package com.devwithzachary.completelinuxinstaller.model

import java.io.File
import java.nio.file.Files

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
    val version: Int = 1,
    val hasUpgradeAvailable: Boolean = false,
    val progressMessage: String = "",
    val installLogs: String = ""
) {
    companion object {
        private const val DPKG_FLAGS =
            "-o Dpkg::Options::=\"--force-all\" -o Dpkg::Options::=\"--force-unsafe-io\" -o Dpkg::Use-Pty=0 -o APT::Sandbox::User=root -o Acquire::http::Pipeline-Depth=0 -o Acquire::PDiffs=false"
        private const val NONINT_EXPORT =
            "export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin; mkdir -p /usr/sbin /etc /var/lib/dbus 2>/dev/null; (grep -q ^messagebus: /etc/group || echo \"messagebus:x:101:\" >> /etc/group); (grep -q ^messagebus: /etc/passwd || echo \"messagebus:x:101:101:D-Bus Message System Daemon:/nonexistent:/bin/false\" >> /etc/passwd); (grep -q ^messagebus: /etc/shadow || echo \"messagebus:*:19700:0:99999:7:::\" >> /etc/shadow); (grep -q ^www-data: /etc/group || echo \"www-data:x:33:\" >> /etc/group); (grep -q ^www-data: /etc/passwd || echo \"www-data:x:33:33:www-data:/var/www:/usr/sbin/nologin\" >> /etc/passwd); (grep -q ^sshd: /etc/group || echo \"sshd:x:102:\" >> /etc/group); (grep -q ^sshd: /etc/passwd || echo \"sshd:x:102:102:Privilege-separated SSH:/run/sshd:/usr/sbin/nologin\" >> /etc/passwd); printf '#!/bin/sh\\nexit 101\\n' > /usr/sbin/policy-rc.d && chmod 755 /usr/sbin/policy-rc.d; if [ ! -f /bin/systemctl ] && [ ! -f /usr/bin/systemctl ]; then printf '#!/bin/sh\\nexit 0\\n' > /usr/bin/systemctl && chmod 755 /usr/bin/systemctl; fi; dbus-uuidgen --ensure 2>/dev/null || true; chmod 755 /usr /usr/local /usr/local/bin /usr/local/sbin /usr/bin /usr/sbin /bin /sbin /etc 2>/dev/null; chmod -R 755 /usr/lib/cargo /usr/libexec 2>/dev/null; chmod -R 777 /var/lib/dpkg /var/cache /tmp /var/tmp /.l2s 2>/dev/null; rm -rf /var/lib/dpkg/*-old /var/lib/dpkg/*-new /var/lib/dpkg/lock* /usr/bin/*.dpkg-new /usr/lib/*.dpkg-new 2>/dev/null; mkdir -p /etc/dpkg/dpkg.cfg.d && echo force-all > /etc/dpkg/dpkg.cfg.d/00-linuxonandroid && echo force-unsafe-io >> /etc/dpkg/dpkg.cfg.d/00-linuxonandroid && echo force-overwrite >> /etc/dpkg/dpkg.cfg.d/00-linuxonandroid && echo force-confold >> /etc/dpkg/dpkg.cfg.d/00-linuxonandroid && echo force-confdef >> /etc/dpkg/dpkg.cfg.d/00-linuxonandroid && echo force-depends >> /etc/dpkg/dpkg.cfg.d/00-linuxonandroid; mkdir -p /etc/apt/apt.conf.d && echo 'APT::Sandbox::User \"root\";' > /etc/apt/apt.conf.d/99linuxonandroid && echo 'Acquire::http::Pipeline-Depth \"0\";' >> /etc/apt/apt.conf.d/99linuxonandroid && echo 'Acquire::http::No-Cache \"true\";' >> /etc/apt/apt.conf.d/99linuxonandroid && echo 'Acquire::PDiffs \"false\";' >> /etc/apt/apt.conf.d/99linuxonandroid && echo 'Acquire::ForceIPv4 \"true\";' >> /etc/apt/apt.conf.d/99linuxonandroid; export TMPDIR=/tmp && export TMP=/tmp && export DEBIAN_FRONTEND=noninteractive && export DEBIAN_PRIORITY=critical && export UCF_FORCE_CONFFOLD=1 && export NEEDRESTART_MODE=a; chown -R 0:0 /etc/sudoers /etc/sudoers.d /etc/sudo.conf /usr/bin/sudo /usr/lib/sudo 2>/dev/null || true; chmod 4755 /usr/bin/sudo 2>/dev/null || true; chmod 0440 /etc/sudoers /etc/sudoers.d/* 2>/dev/null || true"

        fun isBinaryPresent(rootfsDir: File, relativePath: String): Boolean {
            val file = File(rootfsDir, relativePath)
            if (file.exists()) return true

            // On Android, absolute symlinks within the container rootfs (e.g. /etc/alternatives/vncserver)
            // point to paths that do not exist on the Android host filesystem.
            // Check if the entry exists as a symbolic link and follow container-relative links.
            try {
                val path = file.toPath()
                if (Files.isSymbolicLink(path)) {
                    var currentPath = path
                    var hops = 0
                    while (hops < 10) {
                        val target = Files.readSymbolicLink(currentPath)
                        val targetFile = if (target.isAbsolute) {
                            File(rootfsDir, target.toString().removePrefix("/"))
                        } else {
                            File(currentPath.toFile().parentFile, target.toString())
                        }
                        if (targetFile.exists()) return true
                        val nextPath = targetFile.toPath()
                        if (Files.isSymbolicLink(nextPath)) {
                            currentPath = nextPath
                            hops++
                        } else {
                            break
                        }
                    }
                    return true
                }
            } catch (_: Exception) {}

            // Handle well-known binary aliases across distributions
            when (relativePath) {
                "usr/bin/vncserver" -> {
                    if (isBinaryPresent(rootfsDir, "usr/bin/tigervncserver") ||
                        isBinaryPresent(rootfsDir, "usr/bin/Xvnc") ||
                        isBinaryPresent(rootfsDir, "usr/bin/Xtigervnc")
                    ) return true
                }
                "usr/sbin/sshd" -> {
                    if (isBinaryPresent(rootfsDir, "usr/bin/sshd")) return true
                }
                "usr/sbin/nginx" -> {
                    if (isBinaryPresent(rootfsDir, "usr/bin/nginx")) return true
                }
            }

            return false
        }

        fun buildNginxLaunchCommand(port: Int = 8080): String {
            val validPort = if (port in 1..65535) port else 8080
            return "(sed -i 's/\\b80\\b/$validPort/g' /etc/nginx/sites-available/default /etc/nginx/sites-enabled/* /etc/nginx/conf.d/*.conf /etc/nginx/http.d/*.conf /etc/nginx/nginx.conf 2>/dev/null || true); (sed -i 's/^\\s*user\\s\\+www-data/#user www-data/' /etc/nginx/nginx.conf 2>/dev/null || true); mkdir -p /run /var/log/nginx /var/lib/nginx 2>/dev/null && chmod -R 777 /run /var/log/nginx /var/lib/nginx 2>/dev/null || true; service nginx start 2>/dev/null || /usr/sbin/service nginx start 2>/dev/null || nginx 2>/dev/null || /usr/sbin/nginx 2>/dev/null || /usr/bin/nginx 2>/dev/null"
        }

        fun buildNginxPostInstallNotes(port: Int = 8080): String {
            val validPort = if (port in 1..65535) port else 8080
            return "NGINX HTTP server listening on port $validPort. Test in your browser or with 'curl http://localhost:$validPort'."
        }

        fun buildSshLaunchCommand(port: Int = 2222): String {
            val validPort = if (port in 1..65535) port else 2222
            return "(sed -i 's/^Subsystem.*sftp/#&/' /etc/ssh/sshd_config 2>/dev/null || true); mkdir -p /run/sshd /var/run/sshd /var/empty && [ -e /dev/ptmx ] || (mknod -m 666 /dev/ptmx c 5 2 2>/dev/null || ln -s /dev/pts/ptmx /dev/ptmx 2>/dev/null || true) && chmod 666 /dev/ptmx 2>/dev/null || true && ssh-keygen -A 2>/dev/null || true && chmod 755 /etc/ssh /run/sshd /var/run/sshd /var/empty 2>/dev/null || true && (killall -9 sshd 2>/dev/null || true) && /usr/sbin/sshd -p $validPort"
        }

        fun buildSshPostInstallNotes(port: Int = 2222): String {
            val validPort = if (port in 1..65535) port else 2222
            return "SSH server listening on port $validPort. Connect via 'ssh <username>@<phone-ip> -p $validPort' using your Linux password."
        }

        fun getPresets(sshPort: Int = 2222): List<SoftwarePackage> {
            val validPort = if (sshPort in 1..65535) sshPort else 2222
            return listOf(
                SoftwarePackage(
                    id = "xfce_desktop",
                    name = "XFCE 4 Desktop & VNC",
                    category = SoftwareCategory.DESKTOP_GUI,
                    description = "Full lightweight graphical desktop environment with XFCE4 and TigerVNC server.",
                    iconName = "DesktopWindows",
                    installCommand = "$NONINT_EXPORT && dpkg --configure -a && apt-get update $DPKG_FLAGS && apt-get install -y $DPKG_FLAGS xfce4 xfce4-terminal dbus-x11 tigervnc-standalone-server tigervnc-xorg-extension novnc websockify curl ca-certificates perl python3 libgdk-pixbuf2.0-bin librsvg2-common adwaita-icon-theme hicolor-icon-theme && rm -f /etc/tigervnc/vncserver-config-defaults && mkdir -p /root/.vnc /etc/skel/.vnc /etc/vnc /tmp/.X11-unix /tmp/.ICE-unix && chmod 1777 /tmp/.X11-unix /tmp/.ICE-unix 2>/dev/null || true && echo ubuntu | vncpasswd -f > /root/.vnc/passwd && chmod 600 /root/.vnc/passwd && echo ubuntu | vncpasswd -f > /etc/skel/.vnc/passwd && chmod 600 /etc/skel/.vnc/passwd && cat << 'EOF' > /usr/bin/bwrap\n#!/usr/bin/env python3\nimport sys, os\nargs = sys.argv[1:]\nexec_idx = -1\nfor i, arg in enumerate(args):\n    if arg.startswith(\"/usr/\") and os.path.isfile(arg) and os.access(arg, os.X_OK):\n        exec_idx = i\n        break\nif exec_idx >= 0:\n    os.execv(args[exec_idx], args[exec_idx:])\nelse:\n    sys.exit(0)\nEOF\nchmod 755 /usr/bin/bwrap && cat << 'EOF' > /etc/vnc/xstartup\n#!/bin/sh\nunset SESSION_MANAGER\nunset DBUS_SESSION_BUS_ADDRESS\nexport XDG_SESSION_TYPE=x11\nexport XDG_CURRENT_DESKTOP=XFCE\nexport DESKTOP_SESSION=xfce\nexport NO_AT_BRIDGE=1\nexport GDK_BACKEND=x11\nexport GTK_OVERLAY_SCROLLING=0\nexport GLYCIN_DISABLE_SANDBOX=1\nexport GLYCIN_ENABLE_SANDBOX=0\nexport LIBGL_ALWAYS_SOFTWARE=1\n[ -r \$HOME/.Xresources ] && xrdb \$HOME/.Xresources 2>/dev/null || true\nif command -v dbus-launch >/dev/null 2>&1; then\n    eval \$(dbus-launch --sh-syntax --exit-with-session)\nfi\nxsetroot -solid \"#1e293b\" 2>/dev/null || true\nxfconf-query -c xfwm4 -p /general/use_compositing -n -t bool -s false 2>/dev/null || true\nxfsettingsd --daemon 2>/dev/null || true\nxfwm4 --compositor=off --daemon 2>/dev/null || xfwm4 --compositor=off &\nxfce4-panel &\nThunar --daemon 2>/dev/null &\nif command -v xfdesktop >/dev/null 2>&1; then\n    exec xfdesktop\nelif command -v startxfce4 >/dev/null 2>&1; then\n    exec startxfce4\nelse\n    exec xterm\nfi\nEOF\nchmod 755 /etc/vnc/xstartup && cp /etc/vnc/xstartup /etc/X11/Xtigervnc-session && chmod 755 /etc/X11/Xtigervnc-session && cp /etc/vnc/xstartup /root/.vnc/xstartup && cp /etc/vnc/xstartup /etc/skel/.vnc/xstartup && chmod 755 /root/.vnc/xstartup /etc/skel/.vnc/xstartup && printf 'securitytypes=None,VncAuth\\ngeometry=1280x720\\nlocalhost=no\\nalwaysshared=1\\n' > /etc/vnc/config && chmod 644 /etc/vnc/config && cp /etc/vnc/config /root/.vnc/config && cp /etc/vnc/config /etc/skel/.vnc/config && for u in /home/*; do if [ -d \"\$u\" ]; then mkdir -p \"\$u/.vnc\" \"\$u/.config/tigervnc\" && cp /etc/vnc/xstartup \"\$u/.vnc/xstartup\" && cp /etc/vnc/config \"\$u/.vnc/config\" && echo ubuntu | vncpasswd -f > \"\$u/.vnc/passwd\" && echo ubuntu | vncpasswd -f > \"\$u/.config/tigervnc/passwd\" && chmod 755 \"\$u/.vnc/xstartup\" && chmod -R 777 \"\$u/.vnc\" \"\$u/.config\" 2>/dev/null || true; chmod 600 \"\$u/.vnc/passwd\" \"\$u/.config/tigervnc/passwd\" 2>/dev/null || true; fi; done 2>/dev/null || true",
                    launchCommand = "rm -f /etc/tigervnc/vncserver-config-defaults 2>/dev/null || true; mkdir -p /tmp/.X11-unix /tmp/.ICE-unix && chmod 1777 /tmp/.X11-unix /tmp/.ICE-unix 2>/dev/null || true; for u in /home/*; do if [ -d \"\$u\" ]; then mkdir -p \"\$u/.config/tigervnc\" && chmod -R 777 \"\$u/.vnc\" \"\$u/.config\" 2>/dev/null || true; [ -f \"\$u/.config/tigervnc/passwd\" ] || (echo ubuntu | vncpasswd -f > \"\$u/.config/tigervnc/passwd\" 2>/dev/null || true); chmod 600 \"\$u/.vnc/passwd\" \"\$u/.config/tigervnc/passwd\" 2>/dev/null || true; fi; done; vncserver -kill :1 2>/dev/null || true; rm -f /tmp/.X1-lock /tmp/.X11-unix/X1 2>/dev/null; vncserver :1 -xstartup /etc/vnc/xstartup -geometry 1280x720 -depth 24 -SecurityTypes None,VncAuth -UseBlacklist=0 --I-KNOW-THIS-IS-INSECURE",
                    postInstallNotes = "VNC Server starts on port 5901 (:1). Connect via RealVNC Viewer, AVNC, or bVNC at localhost:5901 (password: ubuntu, or set encryption to Off for passwordless).",
                    expectedBinaries = listOf("usr/bin/startxfce4", "usr/bin/vncserver"),
                    version = 4
                ),
                SoftwarePackage(
                    id = "python_dev",
                    name = "Python 3 Developer Stack",
                    category = SoftwareCategory.DEVELOPMENT,
                    description = "Python 3, pip, venv, Git, C/C++ GCC build-essential, and Neovim.",
                    iconName = "Code",
                    installCommand = "$NONINT_EXPORT && dpkg --configure -a && apt-get update $DPKG_FLAGS && apt-get install -y $DPKG_FLAGS python3 python3-pip python3-venv git build-essential neovim curl wget ca-certificates",
                    postInstallNotes = "Includes Python3, pip3, venv, gcc/g++, git, and neovim.",
                    expectedBinaries = listOf(
                        "usr/bin/python3",
                        "usr/bin/pip3",
                        "usr/bin/git",
                        "usr/bin/gcc",
                        "usr/bin/nvim"
                    ),
                    version = 2
                ),
                SoftwarePackage(
                    id = "node_dev",
                    name = "Node.js Developer Stack",
                    category = SoftwareCategory.DEVELOPMENT,
                    description = "Node.js, npm, Yarn, Git, C/C++ GCC build-essential, and Neovim.",
                    iconName = "Code",
                    installCommand = "$NONINT_EXPORT && dpkg --configure -a && apt-get update $DPKG_FLAGS && apt-get install -y $DPKG_FLAGS nodejs npm yarnpkg git build-essential neovim curl wget ca-certificates",
                    postInstallNotes = "Includes Node.js, npm, yarn, gcc/g++, git, and neovim.",
                    expectedBinaries = listOf(
                        "usr/bin/node",
                        "usr/bin/npm",
                        "usr/bin/git",
                        "usr/bin/gcc",
                        "usr/bin/nvim"
                    ),
                    version = 2
                ),
                SoftwarePackage(
                    id = "android_dev",
                    name = "Android Developer Tools",
                    category = SoftwareCategory.DEVELOPMENT,
                    description = "OpenJDK 17, Android Platform Tools (adb, fastboot), Gradle, and Git.",
                    iconName = "Android",
                    installCommand = "$NONINT_EXPORT && dpkg --configure -a && apt-get update $DPKG_FLAGS && apt-get install -y $DPKG_FLAGS openjdk-17-jdk-headless android-sdk-platform-tools gradle git curl wget unzip ca-certificates",
                    postInstallNotes = "Includes OpenJDK 17, adb, fastboot, and Gradle for building Android projects.",
                    expectedBinaries = listOf("usr/bin/java", "usr/bin/adb", "usr/bin/gradle", "usr/bin/git"),
                    version = 2
                ),
                SoftwarePackage(
                    id = "nginx_web",
                    name = "NGINX Web Server & SQLite",
                    category = SoftwareCategory.WEB_SERVER,
                    description = "High-performance HTTP web server and embedded database engine.",
                    iconName = "Dns",
                    installCommand = "$NONINT_EXPORT && dpkg --configure -a && apt-get update $DPKG_FLAGS && apt-get install -y $DPKG_FLAGS nginx sqlite3 curl ca-certificates && (sed -i 's/\\b80\\b/8080/g' /etc/nginx/sites-available/default /etc/nginx/sites-enabled/* /etc/nginx/conf.d/*.conf /etc/nginx/http.d/*.conf /etc/nginx/nginx.conf 2>/dev/null || true) && (sed -i 's/^\\s*user\\s\\+www-data/#user www-data/' /etc/nginx/nginx.conf 2>/dev/null || true) && mkdir -p /run /var/log/nginx /var/lib/nginx && chmod -R 777 /run /var/log/nginx /var/lib/nginx 2>/dev/null || true",
                    launchCommand = buildNginxLaunchCommand(),
                    postInstallNotes = buildNginxPostInstallNotes(),
                    expectedBinaries = listOf("usr/sbin/nginx", "usr/bin/sqlite3"),
                    version = 3
                ),
                SoftwarePackage(
                    id = "openssh_server",
                    name = "OpenSSH Server",
                    category = SoftwareCategory.NETWORKING,
                    description = "SSH daemon allowing remote command line access from PC or LAN devices.",
                    iconName = "Security",
                    installCommand = "$NONINT_EXPORT && dpkg --configure -a && apt-get update $DPKG_FLAGS && mkdir -p /run/sshd /var/run/sshd /var/empty /etc/ssh/sshd_config.d && apt-get install -y $DPKG_FLAGS openssh-server ca-certificates && [ -e /dev/ptmx ] || (mknod -m 666 /dev/ptmx c 5 2 2>/dev/null || ln -s /dev/pts/ptmx /dev/ptmx 2>/dev/null || true) && chmod 666 /dev/ptmx 2>/dev/null || true && ssh-keygen -A 2>/dev/null || true && echo \"Port 2222\" > /etc/ssh/sshd_config.d/00-linuxonandroid.conf && echo \"PermitRootLogin yes\" >> /etc/ssh/sshd_config.d/00-linuxonandroid.conf && echo \"PasswordAuthentication yes\" >> /etc/ssh/sshd_config.d/00-linuxonandroid.conf && echo \"KbdInteractiveAuthentication yes\" >> /etc/ssh/sshd_config.d/00-linuxonandroid.conf && echo \"UsePAM no\" >> /etc/ssh/sshd_config.d/00-linuxonandroid.conf && echo \"StrictModes no\" >> /etc/ssh/sshd_config.d/00-linuxonandroid.conf && echo \"SetEnv PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin\" >> /etc/ssh/sshd_config.d/00-linuxonandroid.conf && echo \"Subsystem sftp internal-sftp\" >> /etc/ssh/sshd_config.d/00-linuxonandroid.conf && (sed -i 's/^Subsystem.*sftp/#&/' /etc/ssh/sshd_config 2>/dev/null || true) && (sed -i 's/^#\\?UsePAM.*/UsePAM no/' /etc/ssh/sshd_config 2>/dev/null || true) && (sed -i 's/^#\\?PasswordAuthentication.*/PasswordAuthentication yes/' /etc/ssh/sshd_config 2>/dev/null || true) && (sed -i 's/^#\\?PermitRootLogin.*/PermitRootLogin yes/' /etc/ssh/sshd_config 2>/dev/null || true) && (sed -i 's/^session.*pam_loginuid.so/#&/' /etc/pam.d/sshd 2>/dev/null || true) && chmod 600 /etc/ssh/ssh_host_*_key 2>/dev/null || true && chmod 755 /etc/ssh /run/sshd /var/run/sshd /var/empty 2>/dev/null || true",
                    launchCommand = buildSshLaunchCommand(validPort),
                    postInstallNotes = buildSshPostInstallNotes(validPort),
                    expectedBinaries = listOf("usr/sbin/sshd", "usr/bin/ssh-keygen"),
                    version = 3
                )
            )
        }
    }
}
