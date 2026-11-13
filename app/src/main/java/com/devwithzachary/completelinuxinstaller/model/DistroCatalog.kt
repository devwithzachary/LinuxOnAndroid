package com.devwithzachary.completelinuxinstaller.model

import com.devwithzachary.completelinuxinstaller.engine.desktop.DesktopConfigHelper
import com.devwithzachary.completelinuxinstaller.model.distros.AlpineDistro
import com.devwithzachary.completelinuxinstaller.model.distros.ArchDistro
import com.devwithzachary.completelinuxinstaller.model.distros.DebianDistro
import com.devwithzachary.completelinuxinstaller.model.distros.FedoraDistro
import com.devwithzachary.completelinuxinstaller.model.distros.KaliDistro
import com.devwithzachary.completelinuxinstaller.model.distros.UbuntuDistro
import com.devwithzachary.completelinuxinstaller.model.distros.VoidDistro

enum class PackageManagerType(
    val displayName: String,
    val installPrefix: String,
    val updateCommand: String
) {
    APT("APT", "apt-get install -y", "apt-get update"),
    APK("APK", "apk add", "apk update"),
    PACMAN("Pacman", "pacman -Sy --noconfirm --overwrite \"*\"", "pacman -Sy"),
    DNF("DNF", "dnf install -y", "dnf check-update || true"),
    XBPS("XBPS", "xbps-install -y", "xbps-install -S")
}

fun formatDistroSize(sizeMb: Int): String {
    return if (sizeMb >= 1000) {
        val gb = sizeMb / 1000.0
        val formatted = String.format(java.util.Locale.US, "%.1f", gb).removeSuffix(".0")
        "$formatted GB"
    } else {
        "$sizeMb MB"
    }
}

data class DistroDefinition(
    val id: String,
    val name: String,
    val version: String,
    val tag: String,
    val description: String,
    val packageManager: PackageManagerType,
    val defaultShell: String = "/bin/bash",
    val candidateShells: List<String> = listOf("/bin/bash", "/usr/bin/bash", "/bin/sh"),
    val downloadSizeMb: Int = 33,
    val installedSizeMb: Int = 1500,
    val downloadUrls: Map<SystemArchitecture, String>,
    val checksumUrls: Map<SystemArchitecture, String> = emptyMap(),
    val sha256Checksums: Map<SystemArchitecture, String> = emptyMap(),
    val colorHex: Long = 0xFFE95420, // Default accent
    val isRecommended: Boolean = false,
    val firstLaunchScriptBuilder: (rootPassword: String, username: String, userPassword: String, isArm: Boolean) -> String = { _, _, _, _ -> "" },
    val softwarePackageCommands: Map<String, (sshPort: Int) -> String> = emptyMap(),
    val softwarePackageUninstallCommands: Map<String, () -> String> = emptyMap(),
    val softwarePackageLaunchCommands: Map<String, (sshPort: Int) -> String> = emptyMap(),
    val softwarePackageExpectedBinaries: Map<String, List<String>> = emptyMap(),
    val softwarePackageVersions: Map<String, Int> = emptyMap()
) {
    val expectedSizeMb: Int get() = downloadSizeMb

    val formattedDownloadSize: String get() = formatDistroSize(downloadSizeMb)
    val formattedInstalledSize: String get() = formatDistroSize(installedSizeMb)

    fun getDownloadUrl(arch: SystemArchitecture): String? = downloadUrls[arch]

    fun getChecksumUrl(arch: SystemArchitecture): String? = checksumUrls[arch]

    fun getSha256Checksum(arch: SystemArchitecture): String? = sha256Checksums[arch]

    fun buildFirstLaunchSetupScript(rootPassword: String, username: String, userPassword: String, isArm: Boolean): String {
        return firstLaunchScriptBuilder(rootPassword, username, userPassword, isArm)
    }

    fun getSoftwarePackageInstallCommand(packageId: String, sshPort: Int = 2222): String? {
        return softwarePackageCommands[packageId]?.invoke(sshPort)
    }

    fun getSoftwarePackageUninstallCommand(packageId: String): String {
        return softwarePackageUninstallCommands[packageId]?.invoke()
            ?: DistroCatalog.buildDefaultUninstallCommand(packageId, packageManager)
    }

    fun getSoftwarePackageLaunchCommand(packageId: String, sshPort: Int = 2222, container: ContainerInstance? = null): String? {
        val baseCommand = softwarePackageLaunchCommands[packageId]?.invoke(sshPort) ?: return null
        if (container == null) return baseCommand
        if (packageId == "xfce_desktop") {
            var cmd = baseCommand
            val effectiveVncPass = container.effectiveVncPassword
            if (effectiveVncPass.isNotBlank()) {
                cmd = cmd.replace("echo ubuntu | vncpasswd", "echo '$effectiveVncPass' | vncpasswd")
                    .replace("echo debian | vncpasswd", "echo '$effectiveVncPass' | vncpasswd")
                    .replace("echo arch | vncpasswd", "echo '$effectiveVncPass' | vncpasswd")
                    .replace("echo fedora | vncpasswd", "echo '$effectiveVncPass' | vncpasswd")
                    .replace("echo void | vncpasswd", "echo '$effectiveVncPass' | vncpasswd")
                    .replace("echo kali | vncpasswd", "echo '$effectiveVncPass' | vncpasswd")
                    .replace("echo alpine | vncpasswd", "echo '$effectiveVncPass' | vncpasswd")
                    .replace("echo user | vncpasswd", "echo '$effectiveVncPass' | vncpasswd")
            }
            if (container.vncLanAccess) {
                cmd = cmd.replace("-localhost", "-nolocalhost")
            } else {
                cmd = cmd.replace("-nolocalhost", "-localhost")
            }
            return cmd
        }
        return baseCommand
    }

    fun getSoftwarePackageExpectedBinaries(packageId: String): List<String>? {
        return softwarePackageExpectedBinaries[packageId]
    }

    fun getSoftwarePackageVersion(packageId: String): Int? {
        return softwarePackageVersions[packageId]
    }

    fun toLinuxDistribution(arch: SystemArchitecture): LinuxDistribution {
        val url = getDownloadUrl(arch)
            ?: downloadUrls[SystemArchitecture.ARM64]
            ?: "https://cdimage.ubuntu.com/ubuntu-base/releases/26.04/release/ubuntu-base-26.04-base-arm64.tar.gz"
        return LinuxDistribution(
            id = id,
            name = name,
            version = version,
            architecture = arch,
            downloadUrl = url,
            expectedSizeMb = downloadSizeMb,
            downloadSizeMb = downloadSizeMb,
            installedSizeMb = installedSizeMb,
            description = description
        )
    }
}

object DistroCatalog {

    const val COMMON_DOCKER_WRAPPER =
        "mkdir -p /etc && " +
            "printf '[DEFAULT]\\nvalid_host_env = TERM, PATH, PROOT_TMP_DIR, PROOT_LOADER, PROOT_LOADER32, PROOT_NO_SECCOMP, PROOT_FORCE_SETID, PROOT_LINK2SYMLINK\\n' > /etc/udocker.conf && " +
            "(udocker --allow-root install --force 2>/dev/null || udocker install --force 2>/dev/null || true) && " +
            "mkdir -p /usr/local/bin && " +
            "printf '%s\\n' '#!/bin/sh\n" +
            "if [ -x /usr/bin/docker ] && ([ -n \"\\\$DOCKER_HOST\" ] || [ -S /var/run/docker.sock ]) && /usr/bin/docker info >/dev/null 2>&1; then\n" +
            "  exec /usr/bin/docker \"\\\$@\"\n" +
            "fi\n" +
            "if command -v udocker >/dev/null 2>&1; then\n" +
            "  export PROOT_NO_SECCOMP=1\n" +
            "  if [ -f /usr/local/lib/libproot_loader.so ]; then\n" +
            "    export PROOT_LOADER=/usr/local/lib/libproot_loader.so\n" +
            "  elif [ -f /usr/lib/libproot_loader.so ]; then\n" +
            "    export PROOT_LOADER=/usr/lib/libproot_loader.so\n" +
            "  fi\n" +
            "  if [ \"\\\$(id -u)\" = \"0\" ]; then\n" +
            "    exec udocker --allow-root \"\\\$@\"\n" +
            "  else\n" +
            "    exec udocker \"\\\$@\"\n" +
            "  fi\n" +
            "fi\n" +
            "if [ -x /usr/bin/docker ]; then\n" +
            "  exec /usr/bin/docker \"\\\$@\"\n" +
            "fi\n" +
            "echo \"Error: Neither docker nor udocker could be executed.\" >&2\n" +
            "exit 1' > /usr/local/bin/docker && chmod 755 /usr/local/bin/docker"

    const val UDOCKER_INSTALL_PIPELINE =
        "(pip3 install --break-system-packages --no-cache-dir udocker || pip install --break-system-packages --no-cache-dir udocker || python3 -m pip install --break-system-packages --no-cache-dir udocker || (curl -fsSL https://github.com/indigo-dc/udocker/releases/download/1.3.17/udocker-1.3.17.tar.gz | tar -xz -C /tmp && cd /tmp/udocker-1.3.17 && python3 setup.py install --prefix=/usr/local && rm -rf /tmp/udocker-1.3.17) || true)"

    const val COMMON_VNCSERVER_WRAPPER = DesktopConfigHelper.COMMON_VNCSERVER_WRAPPER

    val UBUNTU_26_04 = UbuntuDistro
    val DEBIAN_13 = DebianDistro
    val DEBIAN_12 = DebianDistro
    val ALPINE_3_24 = AlpineDistro
    val ALPINE_3_21 = AlpineDistro
    val ARCH_ARM = ArchDistro
    val KALI_ROLLING = KaliDistro
    val VOID_ROLLING = VoidDistro
    val FEDORA_45 = FedoraDistro
    val FEDORA_44 = FedoraDistro

    val ALL_DISTROS = listOf(
        UBUNTU_26_04,
        DEBIAN_13,
        FEDORA_45,
        ALPINE_3_24,
        ARCH_ARM,
        KALI_ROLLING,
        VOID_ROLLING
    )

    fun getById(id: String): DistroDefinition {
        val normalizedId = when (id) {
            "debian_12" -> "debian_13"
            "fedora_44" -> "fedora_45"
            "alpine_3_21" -> "alpine_3_24"
            else -> id
        }
        return ALL_DISTROS.find { it.id == normalizedId || it.name.equals(id, ignoreCase = true) } ?: UBUNTU_26_04
    }

    fun buildDefaultUninstallCommand(packageId: String, pm: PackageManagerType): String {
        fun removePkgs(vararg pkgs: String): String {
            val joined = pkgs.joinToString(" ")
            return when (pm) {
                PackageManagerType.APT -> "DEBIAN_FRONTEND=noninteractive apt-get remove -y --purge $joined 2>/dev/null || true; DEBIAN_FRONTEND=noninteractive apt-get autoremove -y 2>/dev/null || true"
                PackageManagerType.DNF -> "dnf remove -y $joined 2>/dev/null || true"
                PackageManagerType.APK -> "apk del $joined 2>/dev/null || true"
                PackageManagerType.PACMAN -> "pacman -Rns --noconfirm $joined 2>/dev/null || pacman -R --noconfirm $joined 2>/dev/null || true"
                PackageManagerType.XBPS -> "xbps-remove -Ry $joined 2>/dev/null || xbps-remove -y $joined 2>/dev/null || true"
            }
        }

        return when (packageId) {
            "xfce_desktop" -> {
                val stop = "vncserver -kill :1 2>/dev/null || true; (pkill -x Xtigervnc 2>/dev/null || pkill -x Xvnc 2>/dev/null || true)"
                val pkgCmd = when (pm) {
                    PackageManagerType.APT -> removePkgs("xfce4", "xfce4-terminal", "tigervnc-standalone-server", "tigervnc-xorg-extension", "tigervnc-tools", "tigervnc-common", "tightvncserver", "synaptic")
                    PackageManagerType.DNF -> removePkgs("xfce4-session", "xfce4-panel", "xfce4-terminal", "tigervnc-server", "tigervnc", "dnfdragora")
                    PackageManagerType.APK -> removePkgs("xfce4", "xfce4-terminal", "tigervnc")
                    PackageManagerType.PACMAN -> removePkgs("xfce4", "xfce4-terminal", "tigervnc", "octopi")
                    PackageManagerType.XBPS -> removePkgs("xfce4", "xfce4-terminal", "tigervnc", "octoxbps")
                }
                "$stop; $pkgCmd; rm -f /usr/bin/vncserver /usr/bin/startxfce4 /usr/bin/synaptic-pkexec /etc/vnc/xstartup /etc/vnc/config /etc/X11/Xtigervnc-session 2>/dev/null || true"
            }
            "python_dev" -> when (pm) {
                PackageManagerType.APT -> removePkgs("python3-pip", "python3-venv", "build-essential", "neovim")
                PackageManagerType.DNF -> removePkgs("python3-pip", "gcc", "gcc-c++", "neovim")
                PackageManagerType.APK -> removePkgs("py3-pip", "build-base", "neovim")
                PackageManagerType.PACMAN -> removePkgs("python-pip", "base-devel", "neovim")
                PackageManagerType.XBPS -> removePkgs("python3-pip", "base-devel", "neovim")
            }
            "node_dev" -> when (pm) {
                PackageManagerType.APT -> removePkgs("nodejs", "npm", "yarnpkg", "neovim")
                PackageManagerType.DNF -> removePkgs("nodejs", "npm", "neovim")
                PackageManagerType.APK -> removePkgs("nodejs", "npm", "yarn", "neovim")
                PackageManagerType.PACMAN -> removePkgs("nodejs", "npm", "yarn", "neovim")
                PackageManagerType.XBPS -> removePkgs("nodejs", "yarn", "neovim")
            }
            "android_dev" -> when (pm) {
                PackageManagerType.APT -> removePkgs("default-jdk-headless", "openjdk-17-jdk-headless", "android-sdk-platform-tools", "gradle")
                PackageManagerType.DNF -> "${removePkgs("java-latest-openjdk-headless", "java-25-openjdk-headless", "java-17-openjdk-headless", "android-tools")}; rm -rf /opt/gradle-* /usr/bin/gradle /usr/local/bin/gradle 2>/dev/null || true"
                PackageManagerType.APK -> removePkgs("openjdk17-jdk", "openjdk17-jre", "android-tools", "gradle")
                PackageManagerType.PACMAN -> removePkgs("jdk-openjdk", "jdk17-openjdk", "android-tools", "gradle")
                PackageManagerType.XBPS -> removePkgs("openjdk17-jre", "android-tools", "gradle")
            }
            "code_server" -> "(pkill -x code-server 2>/dev/null || killall -9 code-server 2>/dev/null || true); (npm uninstall -g code-server 2>/dev/null || true); ${removePkgs("code-server")}; rm -rf /usr/local/bin/code-server /usr/bin/code-server /usr/lib/code-server /usr/local/lib/code-server* 2>/dev/null || true"
            "nginx_web" -> {
                val stop = "(nginx -s quit 2>/dev/null || pkill -x nginx 2>/dev/null || killall -9 nginx 2>/dev/null || true)"
                val pkgCmd = when (pm) {
                    PackageManagerType.APT -> removePkgs("nginx", "nginx-common", "nginx-core", "sqlite3")
                    else -> removePkgs("nginx", "sqlite")
                }
                "$stop; $pkgCmd"
            }
            "openssh_server" -> {
                val stop = "(pkill -x sshd 2>/dev/null || killall -9 sshd 2>/dev/null || true)"
                val pkgCmd = when (pm) {
                    PackageManagerType.APT, PackageManagerType.DNF -> removePkgs("openssh-server")
                    PackageManagerType.APK -> removePkgs("openssh", "openssh-server")
                    PackageManagerType.PACMAN, PackageManagerType.XBPS -> removePkgs("openssh")
                }
                "$stop; $pkgCmd; rm -f /usr/sbin/sshd /usr/bin/sshd 2>/dev/null || true"
            }
            "web_terminal" -> "(pkill -x ttyd 2>/dev/null || killall -9 ttyd 2>/dev/null || true); ${removePkgs("ttyd")}; rm -f /usr/local/bin/ttyd /usr/bin/ttyd 2>/dev/null || true"
            "docker_tools" -> "(pip3 uninstall -y --break-system-packages udocker 2>/dev/null || pip uninstall -y udocker 2>/dev/null || true); ${removePkgs("docker", "docker.io", "docker-cli", "docker-compose")}; rm -f /usr/local/bin/docker /usr/bin/docker /usr/local/bin/udocker /usr/bin/udocker /etc/udocker.conf 2>/dev/null || true"
            else -> "true"
        }
    }

    fun getForSystemArch(archName: String): SystemArchitecture {
        return when {
            archName.contains("aarch64", ignoreCase = true) || archName.contains(
                "arm64",
                ignoreCase = true
            ) -> SystemArchitecture.ARM64

            archName.contains("x86_64", ignoreCase = true) || archName.contains(
                "amd64",
                ignoreCase = true
            ) -> SystemArchitecture.X86_64

            else -> SystemArchitecture.ARMV7
        }
    }
}
