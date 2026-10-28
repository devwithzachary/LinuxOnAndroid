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
    val DEBIAN_12 = DebianDistro
    val ALPINE_3_21 = AlpineDistro
    val ARCH_ARM = ArchDistro
    val KALI_ROLLING = KaliDistro
    val VOID_ROLLING = VoidDistro
    val FEDORA_44 = FedoraDistro

    val ALL_DISTROS = listOf(
        UBUNTU_26_04,
        DEBIAN_12,
        FEDORA_44,
        ALPINE_3_21,
        ARCH_ARM,
        KALI_ROLLING,
        VOID_ROLLING
    )

    fun getById(id: String): DistroDefinition {
        return ALL_DISTROS.find { it.id == id || it.name.equals(id, ignoreCase = true) } ?: UBUNTU_26_04
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
