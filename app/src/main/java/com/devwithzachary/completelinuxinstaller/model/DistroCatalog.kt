package com.devwithzachary.completelinuxinstaller.model

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
    PACMAN("Pacman", "pacman -Sy --noconfirm", "pacman -Sy"),
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

    fun buildFirstLaunchSetupScript(rootPassword: String, username: String, userPassword: String, isArm: Boolean): String {
        return firstLaunchScriptBuilder(rootPassword, username, userPassword, isArm)
    }

    fun getSoftwarePackageInstallCommand(packageId: String, sshPort: Int = 2222): String? {
        return softwarePackageCommands[packageId]?.invoke(sshPort)
    }

    fun getSoftwarePackageLaunchCommand(packageId: String, sshPort: Int = 2222): String? {
        return softwarePackageLaunchCommands[packageId]?.invoke(sshPort)
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

    const val COMMON_VNCSERVER_WRAPPER =
        "rm -f /usr/bin/vncserver /usr/local/bin/vncserver /usr/local/sbin/vncserver 2>/dev/null || true; " +
            "printf '#!/bin/sh\\n" +
            "# LinuxOnAndroid TigerVNC server wrapper for PRoot environments\\n" +
            "export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:\$PATH\\n" +
            "ACTION=\"start\"; DISP=\":1\"; GEOM=\"1280x720\"; DEPTH=\"24\"; XSTARTUP=\"/etc/vnc/xstartup\"; SECTYPES=\"None,VncAuth\"\\n" +
            "while [ \$# -gt 0 ]; do\\n" +
            "  case \"\$1\" in\\n" +
            "    -list) ACTION=\"list\"; shift ;;\\n" +
            "    -kill) ACTION=\"kill\"; shift; [ -n \"\$1\" ] && [ \"\$1\" != \"-*\" ] && DISP=\"\$1\" && shift ;;\\n" +
            "    -geometry) shift; GEOM=\"\$1\"; shift ;;\\n" +
            "    -depth) shift; DEPTH=\"\$1\"; shift ;;\\n" +
            "    -xstartup) shift; XSTARTUP=\"\$1\"; shift ;;\\n" +
            "    -SecurityTypes) shift; SECTYPES=\"\$1\"; shift ;;\\n" +
            "    -help|--help|-h) ACTION=\"help\"; shift ;;\\n" +
            "    :*) DISP=\"\$1\"; shift ;;\\n" +
            "    *) shift ;;\\n" +
            "  esac\\n" +
            "done\\n" +
            "DISP_NUM=\$(echo \"\$DISP\" | tr -d \":\"); [ -z \"\$DISP_NUM\" ] && DISP_NUM=\"1\"\\n" +
            "if [ \"\$ACTION\" = \"help\" ]; then\\n" +
            "  echo \"TigerVNC server wrapper for LinuxOnAndroid (PRoot)\"\\n" +
            "  echo \"\"\\n" +
            "  echo \"Usage:\"\\n" +
            "  echo \"  vncserver [:<display>] [-geometry <width>x<height>] [-depth <depth>] [-SecurityTypes <types>]\"\\n" +
            "  echo \"  vncserver -list\"\\n" +
            "  echo \"  vncserver -kill :<display>\"\\n" +
            "  exit 0\\n" +
            "fi\\n" +
            "if [ \"\$ACTION\" = \"list\" ]; then\\n" +
            "  echo \"TigerVNC server sessions:\"\\n" +
            "  echo \"\"\\n" +
            "  printf \"%-14s %-11s %s\\\\n\" \"X DISPLAY #\" \"RFB PORT\" \"PROCESS ID\"\\n" +
            "  FOUND=0\\n" +
            "  FOUND_DISPS=\"\"\\n" +
            "  for lock in /tmp/.X*-lock; do\\n" +
            "    [ -e \"\$lock\" ] || continue\\n" +
            "    D_NUM=\$(basename \"\$lock\" | sed 's/\\.X//;s/-lock//')\\n" +
            "    PID=\$(cat \"\$lock\" 2>/dev/null | tr -d ' ' | tr -d '\\n')\\n" +
            "    if [ -n \"\$PID\" ] && ( [ -d \"/proc/\$PID\" ] || kill -0 \"\$PID\" 2>/dev/null ); then\\n" +
            "      PORT=\$((5900 + D_NUM))\\n" +
            "      FOUND_DISPS=\"\${FOUND_DISPS}:\$D_NUM:\"\\n" +
            "      printf \"%-14s %-11s %s\\\\n\" \":\$D_NUM\" \"\$PORT\" \"\$PID\"\\n" +
            "      FOUND=1\\n" +
            "    fi\\n" +
            "  done\\n" +
            "  PROCS=\$( (ps -ef 2>/dev/null || ps aux 2>/dev/null) | grep -v grep | grep -v libproot | grep -v printf | grep Xvnc )\\n" +
            "  if [ -n \"\$PROCS\" ]; then\\n" +
            "    echo \"\$PROCS\" | while read -r line; do\\n" +
            "      PID=\$(echo \"\$line\" | awk '{print \$2}')\\n" +
            "      D=\$(echo \"\$line\" | grep -E -o ':[0-9]+' | head -n1)\\n" +
            "      D_NUM=\$(echo \"\$D\" | tr -d ':')\\n" +
            "      PORT=\"\"\\n" +
            "      RFB_ARG=\$(echo \"\$line\" | grep -E -o '\\-rfbport [0-9]+' | awk '{print \$2}')\\n" +
            "      [ -n \"\$RFB_ARG\" ] && PORT=\"\$RFB_ARG\" || PORT=\$((5900 + D_NUM))\\n" +
            "      case \"\$FOUND_DISPS\" in\\n" +
            "        *\":\$D_NUM:\"*) ;;\\n" +
            "        *) [ -n \"\$D\" ] && [ -n \"\$PORT\" ] && [ -n \"\$PID\" ] && printf \"%-14s %-11s %s\\\\n\" \"\$D\" \"\$PORT\" \"\$PID\" ;;\\n" +
            "      esac\\n" +
            "    done\\n" +
            "  fi\\n" +
            "  if [ \"\$FOUND\" -eq 0 ] && [ -z \"\$PROCS\" ]; then\\n" +
            "    echo \"No active VNC server sessions found.\"\\n" +
            "  fi\\n" +
            "  exit 0\\n" +
            "fi\\n" +
            "if [ \"\$ACTION\" = \"kill\" ]; then\\n" +
            "  LOCK=\"/tmp/.X\${DISP_NUM}-lock\"\\n" +
            "  if [ -e \"\$LOCK\" ]; then\\n" +
            "    LPID=\$(cat \"\$LOCK\" 2>/dev/null | tr -d ' ' | tr -d '\\n')\\n" +
            "    [ -n \"\$LPID\" ] && kill \"\$LPID\" 2>/dev/null || true\\n" +
            "  fi\\n" +
            "  pkill -f \"Xvnc :\$DISP_NUM\" 2>/dev/null || killall -9 Xvnc 2>/dev/null || true\\n" +
            "  rm -f \"/tmp/.X\${DISP_NUM}-lock\" \"/tmp/.X11-unix/X\${DISP_NUM}\" 2>/dev/null || true\\n" +
            "  echo \"Killed VNC server on display :\$DISP_NUM\"; exit 0\\n" +
            "fi\\n" +
            "mkdir -p /tmp/.X11-unix /tmp/.ICE-unix \"\$HOME/.vnc\" \"\$HOME/.config/tigervnc\" /root/.vnc 2>/dev/null; chmod 1777 /tmp/.X11-unix /tmp/.ICE-unix 2>/dev/null || true\\n" +
            "pkill -f \"Xvnc :\$DISP_NUM\" 2>/dev/null || true; rm -f \"/tmp/.X\${DISP_NUM}-lock\" \"/tmp/.X11-unix/X\${DISP_NUM}\" 2>/dev/null || true\\n" +
            "PW=\"\"; if [ -f \"\$HOME/.vnc/passwd\" ]; then PW=\"-rfbauth \$HOME/.vnc/passwd\"; elif [ -f \"\$HOME/.config/tigervnc/passwd\" ]; then PW=\"-rfbauth \$HOME/.config/tigervnc/passwd\"; elif [ -f \"/root/.vnc/passwd\" ]; then PW=\"-rfbauth /root/.vnc/passwd\"; fi\\n" +
            "RFB_PORT=\$((5900 + DISP_NUM))\\n" +
            "echo \"Starting Xvnc on display :\$DISP_NUM (\$GEOM, depth \$DEPTH)...\"\\n" +
            "Xvnc \":\$DISP_NUM\" -geometry \"\$GEOM\" -depth \"\$DEPTH\" \$PW -SecurityTypes \"\$SECTYPES\" -UseBlacklist=0 -ac >/dev/null 2>&1 &\\n" +
            "sleep 1\\n" +
            "if [ -x \"\$XSTARTUP\" ]; then DISPLAY=\":\$DISP_NUM\" \"\$XSTARTUP\" >/dev/null 2>&1 & elif [ -x \"\$HOME/.vnc/xstartup\" ]; then DISPLAY=\":\$DISP_NUM\" \"\$HOME/.vnc/xstartup\" >/dev/null 2>&1 & elif command -v startxfce4 >/dev/null 2>&1; then DISPLAY=\":\$DISP_NUM\" startxfce4 >/dev/null 2>&1 & fi\\n" +
            "LAN_IP=\"\"; [ -f /etc/network/proot_interfaces.conf ] && LAN_IP=\$(grep '^PRIMARY_IP=' /etc/network/proot_interfaces.conf 2>/dev/null | cut -d= -f2); [ -z \"\$LAN_IP\" ] && [ -f /etc/hosts ] && LAN_IP=\$(grep -v '^#' /etc/hosts 2>/dev/null | grep -v '^127\\.' | grep -v '^::1' | awk '{print \$1}' | head -n1)\\n" +
            "echo \"VNC Server started on port \$RFB_PORT (:\$DISP_NUM).\"\\n" +
            "echo \"-> Local connection:  127.0.0.1:\$RFB_PORT\"\\n" +
            "[ -n \"\$LAN_IP\" ] && [ \"\$LAN_IP\" != \"127.0.0.1\" ] && echo \"-> Wi-Fi connection:  \$LAN_IP:\$RFB_PORT\"\\n" +
            "echo \"-> List sessions:     vncserver -list\"\\n" +
            "echo \"-> Stop server:       vncserver -kill :\$DISP_NUM\"\\n' > /usr/bin/vncserver && chmod 755 /usr/bin/vncserver && cp /usr/bin/vncserver /usr/local/bin/vncserver 2>/dev/null || true; chmod 755 /usr/local/bin/vncserver 2>/dev/null || true"

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
