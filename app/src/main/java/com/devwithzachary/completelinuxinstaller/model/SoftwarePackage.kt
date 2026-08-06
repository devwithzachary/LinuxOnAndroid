package com.devwithzachary.completelinuxinstaller.model

enum class SoftwareCategory(val displayName: String) {
    DEVELOPMENT("Developer Tools"),
    WEB_SERVER("Web & Database"),
    UTILITIES("CLI & System Utilities"),
    NETWORKING("Remote & SSH"),
    DESKTOP_GUI("Desktop Environments")
}

enum class InstallStatus {
    NOT_INSTALLED,
    INSTALLING,
    INSTALLED,
    FAILED
}

data class SoftwarePackage(
    val id: String,
    val name: String,
    val category: SoftwareCategory,
    val description: String,
    val iconName: String,
    val installCommand: String,
    val launchCommand: String? = null,
    val postInstallNotes: String? = null,
    val expectedBinaries: List<String> = emptyList(),
    val status: InstallStatus = InstallStatus.NOT_INSTALLED,
    val progressMessage: String = "",
    val installLogs: String = ""
) {
    companion object {
        private const val DPKG_FLAGS = "-o Dpkg::Options::=\"--force-all\" -o Dpkg::Options::=\"--force-unsafe-io\" -o Dpkg::Use-Pty=0 -o APT::Sandbox::User=root -o Acquire::http::Pipeline-Depth=0 -o Acquire::PDiffs=false"
        private const val NONINT_EXPORT = "export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin; mkdir -p /usr/sbin /etc /var/lib/dbus 2>/dev/null; (grep -q ^messagebus: /etc/group || echo \"messagebus:x:101:\" >> /etc/group); (grep -q ^messagebus: /etc/passwd || echo \"messagebus:x:101:101:D-Bus Message System Daemon:/nonexistent:/bin/false\" >> /etc/passwd); (grep -q ^messagebus: /etc/shadow || echo \"messagebus:*:19700:0:99999:7:::\" >> /etc/shadow); (grep -q ^www-data: /etc/group || echo \"www-data:x:33:\" >> /etc/group); (grep -q ^www-data: /etc/passwd || echo \"www-data:x:33:33:www-data:/var/www:/usr/sbin/nologin\" >> /etc/passwd); (grep -q ^sshd: /etc/group || echo \"sshd:x:102:\" >> /etc/group); (grep -q ^sshd: /etc/passwd || echo \"sshd:x:102:102:Privilege-separated SSH:/run/sshd:/usr/sbin/nologin\" >> /etc/passwd); printf '#!/bin/sh\\nexit 101\\n' > /usr/sbin/policy-rc.d && chmod 755 /usr/sbin/policy-rc.d; if [ ! -f /bin/systemctl ] && [ ! -f /usr/bin/systemctl ]; then printf '#!/bin/sh\\nexit 0\\n' > /usr/bin/systemctl && chmod 755 /usr/bin/systemctl; fi; dbus-uuidgen --ensure 2>/dev/null || true; chmod 755 /usr /usr/local /usr/local/bin /usr/local/sbin /usr/bin /usr/sbin /bin /sbin /etc 2>/dev/null; chmod -R 777 /var/lib/dpkg /var/cache /tmp /var/tmp /.l2s 2>/dev/null; rm -rf /var/lib/dpkg/*-old /var/lib/dpkg/*-new /var/lib/dpkg/lock* /usr/bin/*.dpkg-new /usr/lib/*.dpkg-new 2>/dev/null; mkdir -p /etc/dpkg/dpkg.cfg.d && echo force-all > /etc/dpkg/dpkg.cfg.d/00-linuxonandroid && echo force-unsafe-io >> /etc/dpkg/dpkg.cfg.d/00-linuxonandroid && echo force-overwrite >> /etc/dpkg/dpkg.cfg.d/00-linuxonandroid && echo force-confold >> /etc/dpkg/dpkg.cfg.d/00-linuxonandroid && echo force-confdef >> /etc/dpkg/dpkg.cfg.d/00-linuxonandroid && echo force-depends >> /etc/dpkg/dpkg.cfg.d/00-linuxonandroid; mkdir -p /etc/apt/apt.conf.d && echo 'APT::Sandbox::User \"root\";' > /etc/apt/apt.conf.d/99linuxonandroid && echo 'Acquire::http::Pipeline-Depth \"0\";' >> /etc/apt/apt.conf.d/99linuxonandroid && echo 'Acquire::http::No-Cache \"true\";' >> /etc/apt/apt.conf.d/99linuxonandroid && echo 'Acquire::PDiffs \"false\";' >> /etc/apt/apt.conf.d/99linuxonandroid && echo 'Acquire::ForceIPv4 \"true\";' >> /etc/apt/apt.conf.d/99linuxonandroid; export TMPDIR=/tmp && export TMP=/tmp && export DEBIAN_FRONTEND=noninteractive && export DEBIAN_PRIORITY=critical && export UCF_FORCE_CONFFOLD=1 && export NEEDRESTART_MODE=a"

        fun getPresets(): List<SoftwarePackage> {
            return listOf(
                SoftwarePackage(
                    id = "xfce_desktop",
                    name = "XFCE 4 Desktop & VNC",
                    category = SoftwareCategory.DESKTOP_GUI,
                    description = "Full lightweight graphical desktop environment with XFCE4 and TigerVNC server.",
                    iconName = "DesktopWindows",
                    installCommand = "$NONINT_EXPORT && dpkg --configure -a && apt-get update $DPKG_FLAGS && apt-get install -y $DPKG_FLAGS xfce4 xfce4-terminal dbus-x11 tigervnc-standalone-server tigervnc-xorg-extension tightvncserver novnc websockify curl ca-certificates && mkdir -p /root/.vnc && echo '#!/bin/sh\\nunset SESSION_MANAGER\\nunset DBUS_SESSION_BUS_ADDRESS\\nexec startxfce4' > /root/.vnc/xstartup && chmod +x /root/.vnc/xstartup",
                    launchCommand = "vncserver :1 -geometry 1280x720 -depth 24",
                    postInstallNotes = "VNC Server starts on port 5901 (:1). Connect via any VNC viewer client or noVNC web browser interface.",
                    expectedBinaries = listOf("usr/bin/startxfce4", "usr/bin/vncserver")
                ),
                SoftwarePackage(
                    id = "python_dev",
                    name = "Python 3 Developer Stack",
                    category = SoftwareCategory.DEVELOPMENT,
                    description = "Python 3, pip, venv, Git, C/C++ GCC build-essential, and Neovim.",
                    iconName = "Code",
                    installCommand = "$NONINT_EXPORT && dpkg --configure -a && apt-get update $DPKG_FLAGS && apt-get install -y $DPKG_FLAGS python3 python3-pip python3-venv git build-essential neovim curl wget ca-certificates",
                    postInstallNotes = "Includes Python3, pip3, venv, gcc/g++, git, and neovim.",
                    expectedBinaries = listOf("usr/bin/python3", "usr/bin/pip3", "usr/bin/git", "usr/bin/gcc", "usr/bin/nvim")
                ),
                SoftwarePackage(
                    id = "node_dev",
                    name = "Node.js Developer Stack",
                    category = SoftwareCategory.DEVELOPMENT,
                    description = "Node.js, npm, Yarn, Git, C/C++ GCC build-essential, and Neovim.",
                    iconName = "Code",
                    installCommand = "$NONINT_EXPORT && dpkg --configure -a && apt-get update $DPKG_FLAGS && apt-get install -y $DPKG_FLAGS nodejs npm yarnpkg git build-essential neovim curl wget ca-certificates",
                    postInstallNotes = "Includes Node.js, npm, yarn, gcc/g++, git, and neovim.",
                    expectedBinaries = listOf("usr/bin/node", "usr/bin/npm", "usr/bin/git", "usr/bin/gcc", "usr/bin/nvim")
                ),
                SoftwarePackage(
                    id = "android_dev",
                    name = "Android Developer Tools",
                    category = SoftwareCategory.DEVELOPMENT,
                    description = "OpenJDK 17, Android Platform Tools (adb, fastboot), Gradle, and Git.",
                    iconName = "Android",
                    installCommand = "$NONINT_EXPORT && dpkg --configure -a && apt-get update $DPKG_FLAGS && apt-get install -y $DPKG_FLAGS openjdk-17-jdk-headless android-sdk-platform-tools gradle git curl wget unzip ca-certificates",
                    postInstallNotes = "Includes OpenJDK 17, adb, fastboot, and Gradle for building Android projects.",
                    expectedBinaries = listOf("usr/bin/java", "usr/bin/adb", "usr/bin/gradle", "usr/bin/git")
                ),
                SoftwarePackage(
                    id = "nginx_web",
                    name = "NGINX Web Server & SQLite",
                    category = SoftwareCategory.WEB_SERVER,
                    description = "High-performance HTTP web server and embedded database engine.",
                    iconName = "Dns",
                    installCommand = "$NONINT_EXPORT && dpkg --configure -a && apt-get update $DPKG_FLAGS && apt-get install -y $DPKG_FLAGS nginx sqlite3 curl ca-certificates",
                    launchCommand = "service nginx start",
                    postInstallNotes = "Server starts on port 80 or 8080. Test with 'curl http://localhost'.",
                    expectedBinaries = listOf("usr/sbin/nginx", "usr/bin/sqlite3")
                ),
                SoftwarePackage(
                    id = "openssh_server",
                    name = "OpenSSH Server",
                    category = SoftwareCategory.NETWORKING,
                    description = "SSH daemon allowing remote command line access from PC or LAN devices.",
                    iconName = "Security",
                    installCommand = "$NONINT_EXPORT && dpkg --configure -a && apt-get update $DPKG_FLAGS && apt-get install -y $DPKG_FLAGS openssh-server && mkdir -p /run/sshd /etc/ssh/sshd_config.d && [ -f /etc/ssh/ssh_host_rsa_key ] || ssh-keygen -t rsa -f /etc/ssh/ssh_host_rsa_key -N \"\" && [ -f /etc/ssh/ssh_host_ecdsa_key ] || ssh-keygen -t ecdsa -f /etc/ssh/ssh_host_ecdsa_key -N \"\" && [ -f /etc/ssh/ssh_host_ed25519_key ] || ssh-keygen -t ed25519 -f /etc/ssh/ssh_host_ed25519_key -N \"\" && ssh-keygen -A 2>/dev/null || true && echo \"PermitRootLogin yes\" > /etc/ssh/sshd_config.d/00-linuxonandroid.conf && echo \"PasswordAuthentication yes\" >> /etc/ssh/sshd_config.d/00-linuxonandroid.conf && echo \"KbdInteractiveAuthentication yes\" >> /etc/ssh/sshd_config.d/00-linuxonandroid.conf && echo \"StrictModes no\" >> /etc/ssh/sshd_config.d/00-linuxonandroid.conf && echo \"Port 2222\" >> /etc/ssh/sshd_config.d/00-linuxonandroid.conf && chmod 600 /etc/ssh/ssh_host_*_key 2>/dev/null || true && chmod 755 /etc/ssh /run/sshd 2>/dev/null || true",
                    launchCommand = "mkdir -p /run/sshd && chmod 600 /etc/ssh/ssh_host_*_key 2>/dev/null || true && chmod 755 /etc/ssh /run/sshd 2>/dev/null || true && (killall sshd 2>/dev/null || true) && /usr/sbin/sshd -p 2222",
                    postInstallNotes = "SSH server listening on port 2222. Connect via 'ssh root@<phone-ip> -p 2222' using your Linux password.",
                    expectedBinaries = listOf("usr/sbin/sshd", "usr/bin/ssh-keygen")
                )
            )
        }
    }
}
