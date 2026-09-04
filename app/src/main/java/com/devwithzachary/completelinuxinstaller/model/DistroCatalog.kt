package com.devwithzachary.completelinuxinstaller.model

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

    val UBUNTU_26_04 = DistroDefinition(
        id = "ubuntu_26_04",
        name = "Ubuntu 26.04 LTS",
        version = "26.04",
        tag = "Official LTS",
        description = "Full Ubuntu LTS base rootfs with APT package manager. Ideal for general development and servers.",
        packageManager = PackageManagerType.APT,
        defaultShell = "/bin/bash",
        downloadSizeMb = 33,
        installedSizeMb = 450,
        colorHex = 0xFFE95420,
        isRecommended = true,
        downloadUrls = mapOf(
            SystemArchitecture.ARM64 to "https://cdimage.ubuntu.com/ubuntu-base/releases/26.04/release/ubuntu-base-26.04-base-arm64.tar.gz",
            SystemArchitecture.X86_64 to "https://cdimage.ubuntu.com/ubuntu-base/releases/26.04/release/ubuntu-base-26.04-base-amd64.tar.gz",
            SystemArchitecture.ARMV7 to "https://cdimage.ubuntu.com/ubuntu-base/releases/26.04/release/ubuntu-base-26.04-base-armhf.tar.gz"
        ),
        firstLaunchScriptBuilder = { rootPassword, username, userPassword, isArm ->
            val repoUrl = if (isArm) "http://ports.ubuntu.com/ubuntu-ports" else "http://archive.ubuntu.com/ubuntu"
            val codename = "resolute"
            "chmod -R 777 /var/lib/dpkg /var/cache /tmp /var/tmp /.l2s 2>/dev/null; chmod 777 /usr /etc 2>/dev/null; chmod -R 755 /usr/lib/cargo /usr/libexec 2>/dev/null; " +
                    "rm -rf /var/lib/dpkg/*-old /var/lib/dpkg/*-new /etc/*.lock /etc/*.PID /etc/*~ /etc/apt/sources.list.d/* 2>/dev/null; " +
                    "mkdir -p /usr/sbin /var/lib/dbus /etc/sudoers.d /etc/pam.d /etc/apt/apt.conf.d 2>/dev/null; printf '#!/bin/sh\\nexit 101\\n' > /usr/sbin/policy-rc.d && chmod 755 /usr/sbin/policy-rc.d; " +
                    "echo 'deb $repoUrl $codename main restricted universe multiverse' > /etc/apt/sources.list && " +
                    "echo 'deb $repoUrl $codename-updates main restricted universe multiverse' >> /etc/apt/sources.list && " +
                    "echo 'deb $repoUrl $codename-security main restricted universe multiverse' >> /etc/apt/sources.list; " +
                    "echo 'APT::Sandbox::User \"root\";' > /etc/apt/apt.conf.d/99linuxonandroid && echo 'Acquire::http::Pipeline-Depth \"0\";' >> /etc/apt/apt.conf.d/99linuxonandroid && echo 'Acquire::http::No-Cache \"true\";' >> /etc/apt/apt.conf.d/99linuxonandroid && echo 'Acquire::PDiffs \"false\";' >> /etc/apt/apt.conf.d/99linuxonandroid && echo 'Acquire::ForceIPv4 \"true\";' >> /etc/apt/apt.conf.d/99linuxonandroid; " +
                    "export DEBIAN_FRONTEND=noninteractive; export DEBIAN_PRIORITY=critical; export UCF_FORCE_CONFFOLD=1; export NEEDRESTART_MODE=a; export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin; " +
                    "dpkg --configure -a 2>/dev/null; " +
                    "apt-get update -o APT::Sandbox::User=root -o Acquire::http::Pipeline-Depth=0 -o Acquire::PDiffs=false 2>/dev/null; " +
                    "apt-get install -y --no-install-recommends -o APT::Sandbox::User=root -o Dpkg::Options::=\"--force-unsafe-io\" -o Dpkg::Options::=\"--force-overwrite\" -o Dpkg::Options::=\"--force-confdef\" -o Dpkg::Options::=\"--force-confold\" -o Dpkg::Use-Pty=0 coreutils ca-certificates sudo python3 curl wget net-tools procps nano dialog 2>/dev/null || true; " +
                    "rm -f /etc/*.lock /etc/*.PID /etc/*~; " +
                    "echo \"root:$rootPassword\" | chpasswd 2>/dev/null; passwd -u root 2>/dev/null || true; " +
                    "(grep -q ^$username: /etc/passwd || echo \"$username:x:1000:1000:$username:/home/$username:/bin/bash\" >> /etc/passwd); " +
                    "(grep -q ^$username: /etc/group || echo \"$username:x:1000:\" >> /etc/group); " +
                    "(grep -q ^$username: /etc/shadow || echo \"$username:*:19700:0:99999:7:::\" >> /etc/shadow); " +
                    "mkdir -p /home/$username; echo \"$username:$userPassword\" | chpasswd 2>/dev/null; passwd -u $username 2>/dev/null || true; " +
                    "chmod 644 /etc/shadow /etc/shadow- /etc/passwd /etc/group 2>/dev/null || true; " +
                    "usermod -aG sudo,shadow $username 2>/dev/null || true; chown -R $username:$username /home/$username 2>/dev/null || true; " +
                    "mkdir -p /etc/sudoers.d && echo \"$username ALL=(ALL:ALL) NOPASSWD:ALL\" > /etc/sudoers.d/$username && chmod 0440 /etc/sudoers.d/$username; " +
                    "printf 'auth sufficient pam_permit.so\\naccount sufficient pam_permit.so\\nsession sufficient pam_permit.so\\npassword sufficient pam_permit.so\\n' > /etc/pam.d/su; " +
                    "cp /etc/pam.d/su /etc/pam.d/su-l 2>/dev/null || true; " +
                    "chown -R 0:0 /etc/sudo.conf /etc/sudoers /etc/sudoers.d /usr/bin/sudo /usr/lib/sudo 2>/dev/null || true; chmod 4755 /usr/bin/sudo 2>/dev/null || true"
        },
        softwarePackageCommands = mapOf(
            "xfce_desktop" to { _ ->
                "export DEBIAN_FRONTEND=noninteractive && export DEBIAN_PRIORITY=critical && export UCF_FORCE_CONFFOLD=1 && export NEEDRESTART_MODE=a && dpkg --configure -a && apt-get update -o Dpkg::Options::=\"--force-unsafe-io\" -o Dpkg::Options::=\"--force-overwrite\" -o Dpkg::Options::=\"--force-confdef\" -o Dpkg::Options::=\"--force-confold\" && apt-get install -y -o Dpkg::Options::=\"--force-unsafe-io\" -o Dpkg::Options::=\"--force-overwrite\" -o Dpkg::Options::=\"--force-confdef\" -o Dpkg::Options::=\"--force-confold\" xfce4 xfce4-terminal dbus-x11 tigervnc-standalone-server tigervnc-xorg-extension novnc websockify curl ca-certificates perl python3 libgdk-pixbuf2.0-bin librsvg2-common adwaita-icon-theme hicolor-icon-theme && rm -f /etc/tigervnc/vncserver-config-defaults && mkdir -p /root/.vnc /etc/skel/.vnc /etc/vnc /tmp/.X11-unix /tmp/.ICE-unix && chmod 1777 /tmp/.X11-unix /tmp/.ICE-unix 2>/dev/null || true && echo ubuntu | vncpasswd -f > /root/.vnc/passwd && chmod 600 /root/.vnc/passwd && echo ubuntu | vncpasswd -f > /etc/skel/.vnc/passwd && chmod 600 /etc/skel/.vnc/passwd && cat << 'EOF' > /usr/bin/bwrap\n#!/usr/bin/env python3\nimport sys, os\nargs = sys.argv[1:]\nexec_idx = -1\nfor i, arg in enumerate(args):\n    if arg.startswith(\"/usr/\") and os.path.isfile(arg) and os.access(arg, os.X_OK):\n        exec_idx = i\n        break\nif exec_idx >= 0:\n    os.execv(args[exec_idx], args[exec_idx:])\nelse:\n    sys.exit(0)\nEOF\nchmod 755 /usr/bin/bwrap && cat << 'EOF' > /etc/vnc/xstartup\n#!/bin/sh\nunset SESSION_MANAGER\nunset DBUS_SESSION_BUS_ADDRESS\nexport XDG_SESSION_TYPE=x11\nexport XDG_CURRENT_DESKTOP=XFCE\nexport DESKTOP_SESSION=xfce\nexport NO_AT_BRIDGE=1\nexport GDK_BACKEND=x11\nexport GTK_OVERLAY_SCROLLING=0\nexport GLYCIN_DISABLE_SANDBOX=1\nexport GLYCIN_ENABLE_SANDBOX=0\nexport LIBGL_ALWAYS_SOFTWARE=1\n[ -r \$HOME/.Xresources ] && xrdb \$HOME/.Xresources 2>/dev/null || true\nif command -v dbus-launch >/dev/null 2>&1; then\n    eval \$(dbus-launch --sh-syntax --exit-with-session)\nfi\nxsetroot -solid \"#1e293b\" 2>/dev/null || true\nxfconf-query -c xfwm4 -p /general/use_compositing -n -t bool -s false 2>/dev/null || true\nxfsettingsd --daemon 2>/dev/null || true\nxfwm4 --compositor=off --daemon 2>/dev/null || xfwm4 --compositor=off &\nxfce4-panel &\nThunar --daemon 2>/dev/null &\nif command -v xfdesktop >/dev/null 2>&1; then\n    exec xfdesktop\nelif command -v startxfce4 >/dev/null 2>&1; then\n    exec startxfce4\nelse\n    exec xterm\nfi\nEOF\nchmod 755 /etc/vnc/xstartup && cp /etc/vnc/xstartup /etc/X11/Xtigervnc-session 2>/dev/null || true && chmod 755 /etc/X11/Xtigervnc-session 2>/dev/null || true && cp /etc/vnc/xstartup /root/.vnc/xstartup && cp /etc/vnc/xstartup /etc/skel/.vnc/xstartup && chmod 755 /root/.vnc/xstartup /etc/skel/.vnc/xstartup && printf 'securitytypes=None,VncAuth\\ngeometry=1280x720\\nlocalhost=no\\nalwaysshared=1\\n' > /etc/vnc/config && chmod 644 /etc/vnc/config && cp /etc/vnc/config /root/.vnc/config && cp /etc/vnc/config /etc/skel/.vnc/config && for u in /home/*; do if [ -d \"\$u\" ]; then mkdir -p \"\$u/.vnc\" \"\$u/.config/tigervnc\" && cp /etc/vnc/xstartup \"\$u/.vnc/xstartup\" && cp /etc/vnc/config \"\$u/.vnc/config\" && echo ubuntu | vncpasswd -f > \"\$u/.vnc/passwd\" && echo ubuntu | vncpasswd -f > \"\$u/.config/tigervnc/passwd\" && chmod 755 \"\$u/.vnc/xstartup\" && chmod -R 777 \"\$u/.vnc\" \"\$u/.config\" 2>/dev/null || true; chmod 600 \"\$u/.vnc/passwd\" \"\$u/.config/tigervnc/passwd\" 2>/dev/null || true; fi; done 2>/dev/null || true"
            },
            "python_dev" to { _ ->
                "export DEBIAN_FRONTEND=noninteractive && export DEBIAN_PRIORITY=critical && export UCF_FORCE_CONFFOLD=1 && export NEEDRESTART_MODE=a && dpkg --configure -a && chmod -R 755 /usr/lib/cargo /usr/libexec 2>/dev/null && apt-get update -o Dpkg::Options::=\"--force-unsafe-io\" -o Dpkg::Options::=\"--force-overwrite\" -o Dpkg::Options::=\"--force-confdef\" -o Dpkg::Options::=\"--force-confold\" && apt-get install -y -o Dpkg::Options::=\"--force-unsafe-io\" -o Dpkg::Options::=\"--force-overwrite\" -o Dpkg::Options::=\"--force-confdef\" -o Dpkg::Options::=\"--force-confold\" python3 python3-pip python3-venv git build-essential neovim curl wget ca-certificates"
            },
            "node_dev" to { _ ->
                "export DEBIAN_FRONTEND=noninteractive && export DEBIAN_PRIORITY=critical && export UCF_FORCE_CONFFOLD=1 && export NEEDRESTART_MODE=a && dpkg --configure -a && chmod -R 755 /usr/lib/cargo /usr/libexec 2>/dev/null && apt-get update -o Dpkg::Options::=\"--force-unsafe-io\" -o Dpkg::Options::=\"--force-overwrite\" -o Dpkg::Options::=\"--force-confdef\" -o Dpkg::Options::=\"--force-confold\" && apt-get install -y -o Dpkg::Options::=\"--force-unsafe-io\" -o Dpkg::Options::=\"--force-overwrite\" -o Dpkg::Options::=\"--force-confdef\" -o Dpkg::Options::=\"--force-confold\" nodejs npm yarnpkg git build-essential neovim curl wget ca-certificates"
            },
            "android_dev" to { _ ->
                "export DEBIAN_FRONTEND=noninteractive && export DEBIAN_PRIORITY=critical && export UCF_FORCE_CONFFOLD=1 && export NEEDRESTART_MODE=a && dpkg --configure -a && chmod -R 755 /usr/lib/cargo /usr/libexec 2>/dev/null && apt-get update -o Dpkg::Options::=\"--force-unsafe-io\" -o Dpkg::Options::=\"--force-overwrite\" -o Dpkg::Options::=\"--force-confdef\" -o Dpkg::Options::=\"--force-confold\" && apt-get install -y -o Dpkg::Options::=\"--force-unsafe-io\" -o Dpkg::Options::=\"--force-overwrite\" -o Dpkg::Options::=\"--force-confdef\" -o Dpkg::Options::=\"--force-confold\" openjdk-17-jdk-headless android-sdk-platform-tools gradle git curl wget unzip ca-certificates"
            },
            "nginx_web" to { _ ->
                "export DEBIAN_FRONTEND=noninteractive && export DEBIAN_PRIORITY=critical && export UCF_FORCE_CONFFOLD=1 && export NEEDRESTART_MODE=a && dpkg --configure -a && chmod -R 755 /usr/lib/cargo /usr/libexec 2>/dev/null && apt-get update -o Dpkg::Options::=\"--force-unsafe-io\" -o Dpkg::Options::=\"--force-overwrite\" -o Dpkg::Options::=\"--force-confdef\" -o Dpkg::Options::=\"--force-confold\" && apt-get install -y -o Dpkg::Options::=\"--force-unsafe-io\" -o Dpkg::Options::=\"--force-overwrite\" -o Dpkg::Options::=\"--force-confdef\" -o Dpkg::Options::=\"--force-confold\" nginx sqlite3 curl ca-certificates && (sed -i 's/\\b80\\b/8080/g' /etc/nginx/sites-available/default /etc/nginx/sites-enabled/* /etc/nginx/conf.d/*.conf /etc/nginx/http.d/*.conf /etc/nginx/nginx.conf 2>/dev/null || true) && (sed -i 's/^\\s*user\\s\\+www-data/#user www-data/' /etc/nginx/nginx.conf 2>/dev/null || true) && mkdir -p /run /var/log/nginx /var/lib/nginx && chmod -R 777 /run /var/log/nginx /var/lib/nginx 2>/dev/null || true"
            },
            "openssh_server" to { port ->
                "export DEBIAN_FRONTEND=noninteractive && export DEBIAN_PRIORITY=critical && export UCF_FORCE_CONFFOLD=1 && export NEEDRESTART_MODE=a && dpkg --configure -a && chmod -R 755 /usr/lib/cargo /usr/libexec 2>/dev/null && mkdir -p /run/sshd /var/run/sshd /var/empty /etc/ssh/sshd_config.d && apt-get update -o Dpkg::Options::=\"--force-unsafe-io\" -o Dpkg::Options::=\"--force-overwrite\" -o Dpkg::Options::=\"--force-confdef\" -o Dpkg::Options::=\"--force-confold\" && apt-get install -y -o Dpkg::Options::=\"--force-unsafe-io\" -o Dpkg::Options::=\"--force-overwrite\" -o Dpkg::Options::=\"--force-confdef\" -o Dpkg::Options::=\"--force-confold\" openssh-server ca-certificates && [ -e /dev/ptmx ] || (mknod -m 666 /dev/ptmx c 5 2 2>/dev/null || ln -s /dev/pts/ptmx /dev/ptmx 2>/dev/null || true) && chmod 666 /dev/ptmx 2>/dev/null || true && ssh-keygen -A 2>/dev/null || true && echo \"Port $port\" > /etc/ssh/sshd_config.d/00-linuxonandroid.conf && echo \"PermitRootLogin yes\" >> /etc/ssh/sshd_config.d/00-linuxonandroid.conf && echo \"PasswordAuthentication yes\" >> /etc/ssh/sshd_config.d/00-linuxonandroid.conf && echo \"KbdInteractiveAuthentication yes\" >> /etc/ssh/sshd_config.d/00-linuxonandroid.conf && echo \"UsePAM no\" >> /etc/ssh/sshd_config.d/00-linuxonandroid.conf && echo \"StrictModes no\" >> /etc/ssh/sshd_config.d/00-linuxonandroid.conf && echo \"SetEnv PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin\" >> /etc/ssh/sshd_config.d/00-linuxonandroid.conf && echo \"Subsystem sftp internal-sftp\" >> /etc/ssh/sshd_config.d/00-linuxonandroid.conf && (sed -i 's/^Subsystem.*sftp/#&/' /etc/ssh/sshd_config 2>/dev/null || true) && (sed -i 's/^#\\?UsePAM.*/UsePAM no/' /etc/ssh/sshd_config 2>/dev/null || true) && (sed -i 's/^#\\?PasswordAuthentication.*/PasswordAuthentication yes/' /etc/ssh/sshd_config 2>/dev/null || true) && (sed -i 's/^#\\?PermitRootLogin.*/PermitRootLogin yes/' /etc/ssh/sshd_config 2>/dev/null || true) && (sed -i 's/^session.*pam_loginuid.so/#&/' /etc/pam.d/sshd 2>/dev/null || true) && chmod 600 /etc/ssh/ssh_host_*_key 2>/dev/null || true && chmod 755 /etc/ssh /run/sshd /var/run/sshd /var/empty 2>/dev/null || true"
            }
        )
    )

    val DEBIAN_12 = DistroDefinition(
        id = "debian_12",
        name = "Debian 12",
        version = "12",
        tag = "Ultra-Stable",
        description = "Rock-solid, ultra-stable Debian Bookworm base with vast package repositories and minimal memory footprint.",
        packageManager = PackageManagerType.APT,
        defaultShell = "/bin/bash",
        downloadSizeMb = 27,
        installedSizeMb = 244,
        colorHex = 0xFFA80030,
        downloadUrls = mapOf(
            SystemArchitecture.ARM64 to "https://doi-janky.infosiftr.net/job/tianon/job/debuerreotype/job/arm64v8/lastSuccessfulBuild/artifact/bookworm/rootfs.tar.xz",
            SystemArchitecture.X86_64 to "https://doi-janky.infosiftr.net/job/tianon/job/debuerreotype/job/amd64/lastSuccessfulBuild/artifact/bookworm/rootfs.tar.xz",
            SystemArchitecture.ARMV7 to "https://doi-janky.infosiftr.net/job/tianon/job/debuerreotype/job/arm32v7/lastSuccessfulBuild/artifact/bookworm/rootfs.tar.xz"
        ),
        firstLaunchScriptBuilder = { rootPassword, username, userPassword, _ ->
            "chmod -R 777 /var/lib/dpkg /var/cache /tmp /var/tmp /.l2s 2>/dev/null; chmod 777 /usr /etc 2>/dev/null; " +
                    "rm -rf /var/lib/dpkg/*-old /var/lib/dpkg/*-new /etc/*.lock /etc/*.PID /etc/*~ /etc/apt/sources.list.d/* 2>/dev/null; " +
                    "mkdir -p /usr/sbin /var/lib/dbus /etc/sudoers.d /etc/pam.d /etc/apt/apt.conf.d 2>/dev/null; printf '#!/bin/sh\\nexit 101\\n' > /usr/sbin/policy-rc.d && chmod 755 /usr/sbin/policy-rc.d; " +
                    "echo 'deb http://deb.debian.org/debian bookworm main contrib non-free non-free-firmware' > /etc/apt/sources.list && " +
                    "echo 'deb http://deb.debian.org/debian bookworm-updates main contrib non-free non-free-firmware' >> /etc/apt/sources.list && " +
                    "echo 'deb http://security.debian.org/debian-security bookworm-security main contrib non-free non-free-firmware' >> /etc/apt/sources.list; " +
                    "echo 'APT::Sandbox::User \"root\";' > /etc/apt/apt.conf.d/99linuxonandroid && echo 'Acquire::http::Pipeline-Depth \"0\";' >> /etc/apt/apt.conf.d/99linuxonandroid && echo 'Acquire::http::No-Cache \"true\";' >> /etc/apt/apt.conf.d/99linuxonandroid && echo 'Acquire::PDiffs \"false\";' >> /etc/apt/apt.conf.d/99linuxonandroid && echo 'Acquire::ForceIPv4 \"true\";' >> /etc/apt/apt.conf.d/99linuxonandroid; " +
                    "export DEBIAN_FRONTEND=noninteractive; export DEBIAN_PRIORITY=critical; export UCF_FORCE_CONFFOLD=1; export NEEDRESTART_MODE=a; export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin; " +
                    "dpkg --configure -a 2>/dev/null; " +
                    "apt-get update -o APT::Sandbox::User=root -o Acquire::http::Pipeline-Depth=0 -o Acquire::PDiffs=false 2>/dev/null; " +
                    "apt-get install -y --no-install-recommends -o APT::Sandbox::User=root -o Dpkg::Options::=\"--force-unsafe-io\" -o Dpkg::Options::=\"--force-overwrite\" -o Dpkg::Options::=\"--force-confdef\" -o Dpkg::Options::=\"--force-confold\" -o Dpkg::Use-Pty=0 coreutils ca-certificates sudo python3 curl wget net-tools procps nano dialog 2>/dev/null || true; " +
                    "echo \"root:$rootPassword\" | chpasswd 2>/dev/null; passwd -u root 2>/dev/null || true; " +
                    "(grep -q ^$username: /etc/passwd || echo \"$username:x:1000:1000:$username:/home/$username:/bin/bash\" >> /etc/passwd); " +
                    "(grep -q ^$username: /etc/group || echo \"$username:x:1000:\" >> /etc/group); " +
                    "(grep -q ^$username: /etc/shadow || echo \"$username:*:19700:0:99999:7:::\" >> /etc/shadow); " +
                    "mkdir -p /home/$username; echo \"$username:$userPassword\" | chpasswd 2>/dev/null; passwd -u $username 2>/dev/null || true; " +
                    "chmod 644 /etc/shadow /etc/shadow- /etc/passwd /etc/group 2>/dev/null || true; " +
                    "usermod -aG sudo,shadow $username 2>/dev/null || true; chown -R $username:$username /home/$username 2>/dev/null || true; " +
                    "mkdir -p /etc/sudoers.d && echo \"$username ALL=(ALL:ALL) NOPASSWD:ALL\" > /etc/sudoers.d/$username && chmod 0440 /etc/sudoers.d/$username; " +
                    "printf 'auth sufficient pam_permit.so\\naccount sufficient pam_permit.so\\nsession sufficient pam_permit.so\\npassword sufficient pam_permit.so\\n' > /etc/pam.d/su; " +
                    "cp /etc/pam.d/su /etc/pam.d/su-l 2>/dev/null || true; " +
                    "chown -R 0:0 /etc/sudo.conf /etc/sudoers /etc/sudoers.d /usr/bin/sudo /usr/lib/sudo 2>/dev/null || true; chmod 4755 /usr/bin/sudo 2>/dev/null || true"
        },
        softwarePackageCommands = mapOf(
            "xfce_desktop" to { _ ->
                "export DEBIAN_FRONTEND=noninteractive && export DEBIAN_PRIORITY=critical && export UCF_FORCE_CONFFOLD=1 && export NEEDRESTART_MODE=a && dpkg --configure -a && apt-get update && apt-get install -y --no-install-recommends xfce4 xfce4-terminal dbus-x11 tigervnc-standalone-server tigervnc-tools tigervnc-common x11-utils novnc websockify curl ca-certificates perl python3 libgdk-pixbuf2.0-bin librsvg2-common adwaita-icon-theme hicolor-icon-theme && rm -f /etc/tigervnc/vncserver-config-defaults && mkdir -p /root/.vnc /etc/skel/.vnc /etc/vnc /tmp/.X11-unix /tmp/.ICE-unix && chmod 1777 /tmp/.X11-unix /tmp/.ICE-unix 2>/dev/null || true; (echo debian | vncpasswd -f > /root/.vnc/passwd 2>/dev/null || echo debian | tigervncpasswd -f > /root/.vnc/passwd 2>/dev/null || true) && chmod 600 /root/.vnc/passwd 2>/dev/null || true; (echo debian | vncpasswd -f > /etc/skel/.vnc/passwd 2>/dev/null || echo debian | tigervncpasswd -f > /etc/skel/.vnc/passwd 2>/dev/null || true) && chmod 600 /etc/skel/.vnc/passwd 2>/dev/null || true; printf '#!/usr/bin/env python3\\nimport sys, os\\nargs = sys.argv[1:]\\nexec_idx = -1\\nfor i, arg in enumerate(args):\\n    if arg.startswith(\"/usr/\") and os.path.isfile(arg) and os.access(arg, os.X_OK):\\n        exec_idx = i\\n        break\\nif exec_idx >= 0:\\n    os.execv(args[exec_idx], args[exec_idx:])\\nelse:\\n    sys.exit(0)\\n' > /usr/bin/bwrap && chmod 755 /usr/bin/bwrap; printf '#!/bin/sh\\nunset SESSION_MANAGER\\nunset DBUS_SESSION_BUS_ADDRESS\\nexport XDG_SESSION_TYPE=x11\\nexport XDG_CURRENT_DESKTOP=XFCE\\nexport DESKTOP_SESSION=xfce\\nexport NO_AT_BRIDGE=1\\nexport GDK_BACKEND=x11\\nif command -v dbus-launch >/dev/null 2>&1; then\\n    eval \$(dbus-launch --sh-syntax --exit-with-session)\\nfi\\nxfsettingsd --daemon 2>/dev/null || true\\nxfwm4 --daemon 2>/dev/null || xfwm4 &\\nxfce4-panel &\\nThunar --daemon 2>/dev/null &\\nexec startxfce4\\n' > /etc/vnc/xstartup && chmod 755 /etc/vnc/xstartup && cp /etc/vnc/xstartup /root/.vnc/xstartup && cp /etc/vnc/xstartup /etc/skel/.vnc/xstartup && chmod 755 /root/.vnc/xstartup /etc/skel/.vnc/xstartup; printf 'securitytypes=None,VncAuth\\ngeometry=1280x720\\nlocalhost=no\\nalwaysshared=1\\n' > /etc/vnc/config && chmod 644 /etc/vnc/config && cp /etc/vnc/config /root/.vnc/config && cp /etc/vnc/config /etc/skel/.vnc/config; for u in /home/*; do if [ -d \"\$u\" ]; then mkdir -p \"\$u/.vnc\" \"\$u/.config/tigervnc\" && cp /etc/vnc/xstartup \"\$u/.vnc/xstartup\" && cp /etc/vnc/config \"\$u/.vnc/config\" && (echo debian | vncpasswd -f > \"\$u/.vnc/passwd\" 2>/dev/null || echo debian | tigervncpasswd -f > \"\$u/.vnc/passwd\" 2>/dev/null || true) && (echo debian | vncpasswd -f > \"\$u/.config/tigervnc/passwd\" 2>/dev/null || echo debian | tigervncpasswd -f > \"\$u/.config/tigervnc/passwd\" 2>/dev/null || true) && chmod 755 \"\$u/.vnc/xstartup\" && chmod -R 777 \"\$u/.vnc\" \"\$u/.config\" 2>/dev/null || true; chmod 600 \"\$u/.vnc/passwd\" \"\$u/.config/tigervnc/passwd\" 2>/dev/null || true; fi; done"
            },
            "python_dev" to { _ ->
                "export DEBIAN_FRONTEND=noninteractive && export DEBIAN_PRIORITY=critical && export UCF_FORCE_CONFFOLD=1 && export NEEDRESTART_MODE=a && dpkg --configure -a && apt-get update && apt-get install -y python3 python3-pip python3-venv git build-essential neovim curl wget ca-certificates"
            },
            "node_dev" to { _ ->
                "export DEBIAN_FRONTEND=noninteractive && export DEBIAN_PRIORITY=critical && export UCF_FORCE_CONFFOLD=1 && export NEEDRESTART_MODE=a && dpkg --configure -a && apt-get update && apt-get install -y nodejs npm yarnpkg git build-essential neovim curl wget ca-certificates"
            },
            "android_dev" to { _ ->
                "export DEBIAN_FRONTEND=noninteractive && export DEBIAN_PRIORITY=critical && export UCF_FORCE_CONFFOLD=1 && export NEEDRESTART_MODE=a && dpkg --configure -a && apt-get update && apt-get install -y openjdk-17-jdk-headless android-sdk-platform-tools gradle git curl wget unzip ca-certificates"
            },
            "nginx_web" to { _ ->
                "export DEBIAN_FRONTEND=noninteractive && export DEBIAN_PRIORITY=critical && export UCF_FORCE_CONFFOLD=1 && export NEEDRESTART_MODE=a && dpkg --configure -a && apt-get update && apt-get install -y nginx sqlite3 curl ca-certificates && (sed -i 's/\\b80\\b/8080/g' /etc/nginx/sites-available/default /etc/nginx/sites-enabled/* /etc/nginx/conf.d/*.conf /etc/nginx/http.d/*.conf /etc/nginx/nginx.conf 2>/dev/null || true) && (sed -i 's/^\\s*user\\s\\+www-data/#user www-data/' /etc/nginx/nginx.conf 2>/dev/null || true) && mkdir -p /run /var/log/nginx /var/lib/nginx && chmod -R 777 /run /var/log/nginx /var/lib/nginx 2>/dev/null || true"
            },
            "openssh_server" to { port ->
                "export DEBIAN_FRONTEND=noninteractive && export DEBIAN_PRIORITY=critical && export UCF_FORCE_CONFFOLD=1 && export NEEDRESTART_MODE=a && dpkg --configure -a && apt-get update && apt-get install -y openssh-server ca-certificates && mkdir -p /run/sshd /var/run/sshd /var/empty /etc/ssh/sshd_config.d && [ -e /dev/ptmx ] || (mknod -m 666 /dev/ptmx c 5 2 2>/dev/null || ln -s /dev/pts/ptmx /dev/ptmx 2>/dev/null || true) && chmod 666 /dev/ptmx 2>/dev/null || true && ssh-keygen -A 2>/dev/null || true && echo \"Port $port\" > /etc/ssh/sshd_config.d/00-linuxonandroid.conf && echo \"PermitRootLogin yes\" >> /etc/ssh/sshd_config.d/00-linuxonandroid.conf && echo \"PasswordAuthentication yes\" >> /etc/ssh/sshd_config.d/00-linuxonandroid.conf && echo \"KbdInteractiveAuthentication yes\" >> /etc/ssh/sshd_config.d/00-linuxonandroid.conf && echo \"UsePAM no\" >> /etc/ssh/sshd_config.d/00-linuxonandroid.conf && echo \"StrictModes no\" >> /etc/ssh/sshd_config.d/00-linuxonandroid.conf && echo \"SetEnv PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin\" >> /etc/ssh/sshd_config.d/00-linuxonandroid.conf && echo \"Subsystem sftp internal-sftp\" >> /etc/ssh/sshd_config.d/00-linuxonandroid.conf && (sed -i 's/^Subsystem.*sftp/#&/' /etc/ssh/sshd_config 2>/dev/null || true) && (sed -i 's/^#\\?UsePAM.*/UsePAM no/' /etc/ssh/sshd_config 2>/dev/null || true) && (sed -i 's/^#\\?PasswordAuthentication.*/PasswordAuthentication yes/' /etc/ssh/sshd_config 2>/dev/null || true) && (sed -i 's/^#\\?PermitRootLogin.*/PermitRootLogin yes/' /etc/ssh/sshd_config 2>/dev/null || true) && (sed -i 's/^session.*pam_loginuid.so/#&/' /etc/pam.d/sshd 2>/dev/null || true) && chmod 600 /etc/ssh/ssh_host_*_key 2>/dev/null || true && chmod 755 /etc/ssh /run/sshd /var/run/sshd /var/empty 2>/dev/null || true"
            }
        ),
        softwarePackageLaunchCommands = mapOf(
            "xfce_desktop" to { _ ->
                "rm -f /etc/tigervnc/vncserver-config-defaults 2>/dev/null || true; mkdir -p /tmp/.X11-unix /tmp/.ICE-unix && chmod 1777 /tmp/.X11-unix /tmp/.ICE-unix 2>/dev/null || true; for u in /home/*; do if [ -d \"\$u\" ]; then mkdir -p \"\$u/.config/tigervnc\" \"\$u/.vnc\" && chmod -R 777 \"\$u/.vnc\" \"\$u/.config\" 2>/dev/null || true; [ -f \"\$u/.config/tigervnc/passwd\" ] || (echo debian | vncpasswd -f > \"\$u/.config/tigervnc/passwd\" 2>/dev/null || echo debian | tigervncpasswd -f > \"\$u/.config/tigervnc/passwd\" 2>/dev/null || true); [ -f \"\$u/.vnc/passwd\" ] || cp \"\$u/.config/tigervnc/passwd\" \"\$u/.vnc/passwd\" 2>/dev/null || true; chmod 600 \"\$u/.vnc/passwd\" \"\$u/.config/tigervnc/passwd\" 2>/dev/null || true; fi; done; vncserver -kill :1 2>/dev/null || true; rm -f /tmp/.X1-lock /tmp/.X11-unix/X1 2>/dev/null; vncserver :1 -xstartup /etc/vnc/xstartup -geometry 1280x720 -depth 24 -SecurityTypes None,VncAuth -UseBlacklist=0 --I-KNOW-THIS-IS-INSECURE"
            }
        ),
        softwarePackageExpectedBinaries = mapOf(
            "xfce_desktop" to listOf(
                "usr/bin/startxfce4",
                "usr/bin/vncserver",
                "usr/bin/vncpasswd",
                "etc/vnc/xstartup"
            )
        ),
        softwarePackageVersions = mapOf(
            "xfce_desktop" to 5
        )
    )

    val ALPINE_3_21 = DistroDefinition(
        id = "alpine_3_21",
        name = "Alpine Linux 3.21",
        version = "3.21",
        tag = "Minimalist",
        description = "Ultra-lightweight musl and BusyBox environment. Boots instantly with minimal memory footprint and fast APK package manager.",
        packageManager = PackageManagerType.APK,
        defaultShell = "/bin/sh",
        downloadSizeMb = 3,
        installedSizeMb = 32,
        colorHex = 0xFF0D597F,
        downloadUrls = mapOf(
            SystemArchitecture.ARM64 to "https://dl-cdn.alpinelinux.org/alpine/v3.21/releases/aarch64/alpine-minirootfs-3.21.3-aarch64.tar.gz",
            SystemArchitecture.X86_64 to "https://dl-cdn.alpinelinux.org/alpine/v3.21/releases/x86_64/alpine-minirootfs-3.21.3-x86_64.tar.gz",
            SystemArchitecture.ARMV7 to "https://dl-cdn.alpinelinux.org/alpine/v3.21/releases/armv7/alpine-minirootfs-3.21.3-armv7.tar.gz"
        ),
        firstLaunchScriptBuilder = { rootPassword, username, userPassword, _ ->
            "export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin; " +
                    "mkdir -p /etc/sudoers.d /etc/pam.d /home/$username 2>/dev/null; " +
                    "[ -e /bin/bash ] || ln -sf /bin/sh /bin/bash 2>/dev/null || true; " +
                    "[ -e /usr/bin/bash ] || ln -sf /bin/sh /usr/bin/bash 2>/dev/null || true; " +
                    "(grep -q ^$username: /etc/passwd || echo \"$username:x:1000:1000:$username:/home/$username:/bin/sh\" >> /etc/passwd); " +
                    "(grep -q ^$username: /etc/group || echo \"$username:x:1000:\" >> /etc/group); " +
                    "(grep -q ^wheel: /etc/group && sed -i 's/^wheel:.*/&,$username/' /etc/group || echo \"wheel:x:10:root,$username\" >> /etc/group); " +
                    "(grep -q ^$username: /etc/shadow || echo \"$username:*:19700:0:99999:7:::\" >> /etc/shadow); " +
                    "chown -R 1000:1000 /home/$username 2>/dev/null || true; " +
                    "chmod 644 /etc/shadow /etc/shadow- /etc/passwd /etc/group 2>/dev/null || true; " +
                    "echo \"$username ALL=(ALL:ALL) NOPASSWD:ALL\" > /etc/sudoers.d/$username && chmod 0440 /etc/sudoers.d/$username; " +
                    "printf 'auth sufficient pam_permit.so\\naccount sufficient pam_permit.so\\nsession sufficient pam_permit.so\\npassword sufficient pam_permit.so\\n' > /etc/pam.d/su 2>/dev/null || true; " +
                    "apk update 2>/dev/null; " +
                    "apk add --no-cache bash sudo shadow coreutils curl wget procps nano dialog 2>/dev/null || true; " +
                    "echo \"root:$rootPassword\" | chpasswd 2>/dev/null; passwd -u root 2>/dev/null || true; " +
                    "echo \"$username:$userPassword\" | chpasswd 2>/dev/null; passwd -u $username 2>/dev/null || true"
        },
        softwarePackageCommands = mapOf(
            "xfce_desktop" to { _ ->
                "apk update && apk add --no-cache xfce4 xfce4-terminal dbus tigervnc curl ca-certificates perl python3 && mkdir -p /root/.vnc /etc/skel/.vnc /etc/vnc /tmp/.X11-unix /tmp/.ICE-unix && chmod 1777 /tmp/.X11-unix /tmp/.ICE-unix 2>/dev/null || true && echo alpine | vncpasswd -f > /root/.vnc/passwd && chmod 600 /root/.vnc/passwd && echo alpine | vncpasswd -f > /etc/skel/.vnc/passwd && chmod 600 /etc/skel/.vnc/passwd && cat << 'EOF' > /etc/vnc/xstartup\n#!/bin/sh\nunset SESSION_MANAGER\nunset DBUS_SESSION_BUS_ADDRESS\nexport XDG_SESSION_TYPE=x11\nexport XDG_CURRENT_DESKTOP=XFCE\nexport DESKTOP_SESSION=xfce\nexport NO_AT_BRIDGE=1\nexport GDK_BACKEND=x11\nif command -v dbus-launch >/dev/null 2>&1; then\n    eval \$(dbus-launch --sh-syntax --exit-with-session)\nfi\nxfsettingsd --daemon 2>/dev/null || true\nxfwm4 --daemon 2>/dev/null || xfwm4 &\nxfce4-panel &\nThunar --daemon 2>/dev/null &\nexec startxfce4\nEOF\nchmod 755 /etc/vnc/xstartup && cp /etc/vnc/xstartup /root/.vnc/xstartup && cp /etc/vnc/xstartup /etc/skel/.vnc/xstartup && chmod 755 /root/.vnc/xstartup /etc/skel/.vnc/xstartup && printf 'securitytypes=None,VncAuth\\ngeometry=1280x720\\nlocalhost=no\\nalwaysshared=1\\n' > /etc/vnc/config && chmod 644 /etc/vnc/config && cp /etc/vnc/config /root/.vnc/config && cp /etc/vnc/config /etc/skel/.vnc/config && for u in /home/*; do if [ -d \"\$u\" ]; then mkdir -p \"\$u/.vnc\" \"\$u/.config/tigervnc\" && cp /etc/vnc/xstartup \"\$u/.vnc/xstartup\" && cp /etc/vnc/config \"\$u/.vnc/config\" && echo alpine | vncpasswd -f > \"\$u/.vnc/passwd\" && chmod 755 \"\$u/.vnc/xstartup\" && chmod 600 \"\$u/.vnc/passwd\" 2>/dev/null || true; fi; done 2>/dev/null || true"
            },
            "python_dev" to { _ ->
                "apk update && apk add --no-cache python3 py3-pip git build-base neovim curl wget ca-certificates"
            },
            "node_dev" to { _ ->
                "apk update && apk add --no-cache nodejs npm yarn git build-base neovim curl wget ca-certificates"
            },
            "android_dev" to { _ ->
                "apk update && apk add --no-cache openjdk17-jre git curl wget unzip ca-certificates"
            },
            "nginx_web" to { _ ->
                "apk update && apk add --no-cache nginx sqlite curl ca-certificates && (sed -i 's/\\b80\\b/8080/g' /etc/nginx/http.d/*.conf /etc/nginx/nginx.conf 2>/dev/null || true) && mkdir -p /run /var/log/nginx /var/lib/nginx && chmod -R 777 /run /var/log/nginx /var/lib/nginx 2>/dev/null || true"
            },
            "openssh_server" to { port ->
                "apk update && apk add --no-cache openssh-server openssh ca-certificates && mkdir -p /run/sshd /var/run/sshd /var/empty && ssh-keygen -A 2>/dev/null || true && (sed -i 's/^#\\?PasswordAuthentication.*/PasswordAuthentication yes/' /etc/ssh/sshd_config 2>/dev/null || true) && (sed -i 's/^#\\?PermitRootLogin.*/PermitRootLogin yes/' /etc/ssh/sshd_config 2>/dev/null || true) && echo \"Port $port\" >> /etc/ssh/ssh_config && chmod 600 /etc/ssh/ssh_host_*_key 2>/dev/null || true"
            }
        )
    )

    val ARCH_ARM = DistroDefinition(
        id = "arch_arm",
        name = "Arch Linux ARM",
        version = "Rolling",
        tag = "Bleeding Edge",
        description = "Rolling release distribution featuring the pacman package manager and bleeding-edge packages.",
        packageManager = PackageManagerType.PACMAN,
        defaultShell = "/bin/bash",
        downloadSizeMb = 790,
        installedSizeMb = 2100,
        colorHex = 0xFF1793D1,
        downloadUrls = mapOf(
            SystemArchitecture.ARM64 to "http://os.archlinuxarm.org/os/ArchLinuxARM-aarch64-latest.tar.gz",
            SystemArchitecture.X86_64 to "https://geo.mirror.pkgbuild.com/iso/latest/archlinux-bootstrap-x86_64.tar.zst",
            SystemArchitecture.ARMV7 to "http://os.archlinuxarm.org/os/ArchLinuxARM-armv7-latest.tar.gz"
        ),
        firstLaunchScriptBuilder = { rootPassword, username, userPassword, _ ->
            "export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin; " +
                    "mkdir -p /etc/sudoers.d /etc/pam.d /home/$username 2>/dev/null; " +
                    "(grep -q ^$username: /etc/passwd || echo \"$username:x:1000:1000:$username:/home/$username:/bin/bash\" >> /etc/passwd); " +
                    "(grep -q ^$username: /etc/group || echo \"$username:x:1000:\" >> /etc/group); " +
                    "(grep -q ^wheel: /etc/group && sed -i 's/^wheel:.*/&,$username/' /etc/group || echo \"wheel:x:10:root,$username\" >> /etc/group); " +
                    "(grep -q ^$username: /etc/shadow || echo \"$username:*:19700:0:99999:7:::\" >> /etc/shadow); " +
                    "chown -R 1000:1000 /home/$username 2>/dev/null || true; " +
                    "chmod 644 /etc/shadow /etc/shadow- /etc/passwd /etc/group 2>/dev/null || true; " +
                    "echo \"$username ALL=(ALL:ALL) NOPASSWD:ALL\" > /etc/sudoers.d/$username && chmod 0440 /etc/sudoers.d/$username; " +
                    "printf 'auth sufficient pam_permit.so\\naccount sufficient pam_permit.so\\nsession sufficient pam_permit.so\\npassword sufficient pam_permit.so\\n' > /etc/pam.d/su 2>/dev/null || true; " +
                    "echo \"root:$rootPassword\" | chpasswd 2>/dev/null; passwd -u root 2>/dev/null || true; " +
                    "echo \"$username:$userPassword\" | chpasswd 2>/dev/null; passwd -u $username 2>/dev/null || true"
        },
        softwarePackageCommands = mapOf(
            "xfce_desktop" to { _ ->
                "pacman -Sy --noconfirm xfce4 xfce4-terminal tigervnc curl ca-certificates python && mkdir -p /root/.vnc /etc/skel/.vnc /etc/vnc /tmp/.X11-unix /tmp/.ICE-unix && chmod 1777 /tmp/.X11-unix /tmp/.ICE-unix 2>/dev/null || true && echo arch | vncpasswd -f > /root/.vnc/passwd && chmod 600 /root/.vnc/passwd && cat << 'EOF' > /etc/vnc/xstartup\n#!/bin/sh\nunset SESSION_MANAGER\nunset DBUS_SESSION_BUS_ADDRESS\nexport XDG_SESSION_TYPE=x11\nexport XDG_CURRENT_DESKTOP=XFCE\nexport DESKTOP_SESSION=xfce\nxfsettingsd --daemon 2>/dev/null || true\nxfwm4 --daemon 2>/dev/null || xfwm4 &\nxfce4-panel &\nexec startxfce4\nEOF\nchmod 755 /etc/vnc/xstartup && cp /etc/vnc/xstartup /root/.vnc/xstartup && printf 'securitytypes=None,VncAuth\\ngeometry=1280x720\\nlocalhost=no\\nalwaysshared=1\\n' > /etc/vnc/config && chmod 644 /etc/vnc/config && cp /etc/vnc/config /root/.vnc/config && for u in /home/*; do if [ -d \"\$u\" ]; then mkdir -p \"\$u/.vnc\" \"\$u/.config/tigervnc\" && cp /etc/vnc/xstartup \"\$u/.vnc/xstartup\" && cp /etc/vnc/config \"\$u/.vnc/config\" && echo arch | vncpasswd -f > \"\$u/.vnc/passwd\" && chmod 755 \"\$u/.vnc/xstartup\" && chmod 600 \"\$u/.vnc/passwd\" 2>/dev/null || true; fi; done 2>/dev/null || true"
            },
            "python_dev" to { _ ->
                "pacman -Sy --noconfirm python python-pip git base-devel neovim curl wget ca-certificates"
            },
            "node_dev" to { _ ->
                "pacman -Sy --noconfirm nodejs npm yarn git base-devel neovim curl wget ca-certificates"
            },
            "android_dev" to { _ ->
                "pacman -Sy --noconfirm jdk17-openjdk android-tools gradle git curl wget unzip ca-certificates"
            },
            "nginx_web" to { _ ->
                "pacman -Sy --noconfirm nginx sqlite curl ca-certificates && (sed -i 's/\\b80\\b/8080/g' /etc/nginx/nginx.conf 2>/dev/null || true) && mkdir -p /run /var/log/nginx /var/lib/nginx && chmod -R 777 /run /var/log/nginx /var/lib/nginx 2>/dev/null || true"
            },
            "openssh_server" to { port ->
                "pacman -Sy --noconfirm openssh ca-certificates && mkdir -p /run/sshd /var/run/sshd /var/empty && ssh-keygen -A 2>/dev/null || true && (sed -i 's/^#\\?PasswordAuthentication.*/PasswordAuthentication yes/' /etc/ssh/sshd_config 2>/dev/null || true) && (sed -i 's/^#\\?PermitRootLogin.*/PermitRootLogin yes/' /etc/ssh/sshd_config 2>/dev/null || true) && echo \"Port $port\" >> /etc/ssh/ssh_config && chmod 600 /etc/ssh/ssh_host_*_key 2>/dev/null || true"
            }
        )
    )

    val KALI_ROLLING = DistroDefinition(
        id = "kali_rolling",
        name = "Kali Linux CLI Tools",
        version = "Rolling",
        tag = "Security & Pen-testing",
        description = "Official Kali NetHunter security auditing and network forensics minimal environment with Kali repositories.",
        packageManager = PackageManagerType.APT,
        defaultShell = "/bin/bash",
        downloadSizeMb = 130,
        installedSizeMb = 1076,
        colorHex = 0xFF557C93,
        downloadUrls = mapOf(
            SystemArchitecture.ARM64 to "https://kali.download/nethunter-images/current/rootfs/kali-nethunter-rootfs-minimal-arm64.tar.xz",
            SystemArchitecture.X86_64 to "https://kali.download/nethunter-images/current/rootfs/kali-nethunter-rootfs-minimal-amd64.tar.xz",
            SystemArchitecture.ARMV7 to "https://kali.download/nethunter-images/current/rootfs/kali-nethunter-rootfs-minimal-armhf.tar.xz"
        ),
        firstLaunchScriptBuilder = { rootPassword, username, userPassword, _ ->
            "chmod -R 777 /var/lib/dpkg /var/cache /tmp /var/tmp /.l2s 2>/dev/null; chmod 777 /usr /etc 2>/dev/null; " +
                    "rm -rf /var/lib/dpkg/*-old /var/lib/dpkg/*-new /etc/*.lock /etc/*.PID /etc/*~ /etc/apt/sources.list.d/* 2>/dev/null; " +
                    "mkdir -p /usr/sbin /var/lib/dbus /etc/sudoers.d /etc/pam.d /etc/apt/apt.conf.d 2>/dev/null; printf '#!/bin/sh\\nexit 101\\n' > /usr/sbin/policy-rc.d && chmod 755 /usr/sbin/policy-rc.d; " +
                    "echo 'deb http://http.kali.org/kali kali-rolling main contrib non-free non-free-firmware' > /etc/apt/sources.list; " +
                    "echo 'APT::Sandbox::User \"root\";' > /etc/apt/apt.conf.d/99linuxonandroid && echo 'Acquire::http::Pipeline-Depth \"0\";' >> /etc/apt/apt.conf.d/99linuxonandroid && echo 'Acquire::http::No-Cache \"true\";' >> /etc/apt/apt.conf.d/99linuxonandroid && echo 'Acquire::PDiffs \"false\";' >> /etc/apt/apt.conf.d/99linuxonandroid && echo 'Acquire::ForceIPv4 \"true\";' >> /etc/apt/apt.conf.d/99linuxonandroid; " +
                    "export DEBIAN_FRONTEND=noninteractive; export DEBIAN_PRIORITY=critical; export UCF_FORCE_CONFFOLD=1; export NEEDRESTART_MODE=a; export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin; " +
                    "dpkg --configure -a 2>/dev/null; " +
                    "apt-get update -o APT::Sandbox::User=root -o Acquire::http::Pipeline-Depth=0 -o Acquire::PDiffs=false 2>/dev/null; " +
                    "apt-get install -y --no-install-recommends -o APT::Sandbox::User=root -o Dpkg::Options::=\"--force-unsafe-io\" -o Dpkg::Options::=\"--force-overwrite\" -o Dpkg::Options::=\"--force-confdef\" -o Dpkg::Options::=\"--force-confold\" -o Dpkg::Use-Pty=0 coreutils ca-certificates sudo python3 curl wget net-tools procps nano dialog 2>/dev/null || true; " +
                    "echo \"root:$rootPassword\" | chpasswd 2>/dev/null; passwd -u root 2>/dev/null || true; " +
                    "(grep -q ^$username: /etc/passwd || echo \"$username:x:1000:1000:$username:/home/$username:/bin/bash\" >> /etc/passwd); " +
                    "(grep -q ^$username: /etc/group || echo \"$username:x:1000:\" >> /etc/group); " +
                    "(grep -q ^$username: /etc/shadow || echo \"$username:*:19700:0:99999:7:::\" >> /etc/shadow); " +
                    "mkdir -p /home/$username; echo \"$username:$userPassword\" | chpasswd 2>/dev/null; passwd -u $username 2>/dev/null || true; " +
                    "chmod 644 /etc/shadow /etc/shadow- /etc/passwd /etc/group 2>/dev/null || true; " +
                    "usermod -aG sudo,shadow $username 2>/dev/null || true; chown -R $username:$username /home/$username 2>/dev/null || true; " +
                    "mkdir -p /etc/sudoers.d && echo \"$username ALL=(ALL:ALL) NOPASSWD:ALL\" > /etc/sudoers.d/$username && chmod 0440 /etc/sudoers.d/$username; " +
                    "printf 'auth sufficient pam_permit.so\\naccount sufficient pam_permit.so\\nsession sufficient pam_permit.so\\npassword sufficient pam_permit.so\\n' > /etc/pam.d/su; " +
                    "cp /etc/pam.d/su /etc/pam.d/su-l 2>/dev/null || true; " +
                    "chown -R 0:0 /etc/sudo.conf /etc/sudoers /etc/sudoers.d /usr/bin/sudo /usr/lib/sudo 2>/dev/null || true; chmod 4755 /usr/bin/sudo 2>/dev/null || true"
        },
        softwarePackageCommands = mapOf(
            "xfce_desktop" to { _ ->
                "export DEBIAN_FRONTEND=noninteractive && export DEBIAN_PRIORITY=critical && export UCF_FORCE_CONFFOLD=1 && export NEEDRESTART_MODE=a && dpkg --configure -a && apt-get update && apt-get install -y --no-install-recommends xfce4 xfce4-terminal dbus-x11 tigervnc-standalone-server tigervnc-tools tigervnc-common x11-utils novnc websockify curl ca-certificates perl python3 libgdk-pixbuf2.0-bin librsvg2-common adwaita-icon-theme hicolor-icon-theme && rm -f /etc/tigervnc/vncserver-config-defaults && mkdir -p /root/.vnc /etc/skel/.vnc /etc/vnc /tmp/.X11-unix /tmp/.ICE-unix && chmod 1777 /tmp/.X11-unix /tmp/.ICE-unix 2>/dev/null || true; (echo kali | vncpasswd -f > /root/.vnc/passwd 2>/dev/null || echo kali | tigervncpasswd -f > /root/.vnc/passwd 2>/dev/null || true) && chmod 600 /root/.vnc/passwd 2>/dev/null || true; (echo kali | vncpasswd -f > /etc/skel/.vnc/passwd 2>/dev/null || echo kali | tigervncpasswd -f > /etc/skel/.vnc/passwd 2>/dev/null || true) && chmod 600 /etc/skel/.vnc/passwd 2>/dev/null || true; printf '#!/usr/bin/env python3\\nimport sys, os\\nargs = sys.argv[1:]\\nexec_idx = -1\\nfor i, arg in enumerate(args):\\n    if arg.startswith(\"/usr/\") and os.path.isfile(arg) and os.access(arg, os.X_OK):\\n        exec_idx = i\\n        break\\nif exec_idx >= 0:\\n    os.execv(args[exec_idx], args[exec_idx:])\\nelse:\\n    sys.exit(0)\\n' > /usr/bin/bwrap && chmod 755 /usr/bin/bwrap; printf '#!/bin/sh\\nunset SESSION_MANAGER\\nunset DBUS_SESSION_BUS_ADDRESS\\nexport XDG_SESSION_TYPE=x11\\nexport XDG_CURRENT_DESKTOP=XFCE\\nexport DESKTOP_SESSION=xfce\\nexport NO_AT_BRIDGE=1\\nexport GDK_BACKEND=x11\\nif command -v dbus-launch >/dev/null 2>&1; then\\n    eval \$(dbus-launch --sh-syntax --exit-with-session)\\nfi\\nxfsettingsd --daemon 2>/dev/null || true\\nxfwm4 --daemon 2>/dev/null || xfwm4 &\\nxfce4-panel &\\nThunar --daemon 2>/dev/null &\\nexec startxfce4\\n' > /etc/vnc/xstartup && chmod 755 /etc/vnc/xstartup && cp /etc/vnc/xstartup /root/.vnc/xstartup && cp /etc/vnc/xstartup /etc/skel/.vnc/xstartup && chmod 755 /root/.vnc/xstartup /etc/skel/.vnc/xstartup; printf 'securitytypes=None,VncAuth\\ngeometry=1280x720\\nlocalhost=no\\nalwaysshared=1\\n' > /etc/vnc/config && chmod 644 /etc/vnc/config && cp /etc/vnc/config /root/.vnc/config && cp /etc/vnc/config /etc/skel/.vnc/config; for u in /home/*; do if [ -d \"\$u\" ]; then mkdir -p \"\$u/.vnc\" \"\$u/.config/tigervnc\" && cp /etc/vnc/xstartup \"\$u/.vnc/xstartup\" && cp /etc/vnc/config \"\$u/.vnc/config\" && (echo kali | vncpasswd -f > \"\$u/.vnc/passwd\" 2>/dev/null || echo kali | tigervncpasswd -f > \"\$u/.vnc/passwd\" 2>/dev/null || true) && (echo kali | vncpasswd -f > \"\$u/.config/tigervnc/passwd\" 2>/dev/null || echo kali | tigervncpasswd -f > \"\$u/.config/tigervnc/passwd\" 2>/dev/null || true) && chmod 755 \"\$u/.vnc/xstartup\" && chmod -R 777 \"\$u/.vnc\" \"\$u/.config\" 2>/dev/null || true; chmod 600 \"\$u/.vnc/passwd\" \"\$u/.config/tigervnc/passwd\" 2>/dev/null || true; fi; done"
            },
            "python_dev" to { _ ->
                "export DEBIAN_FRONTEND=noninteractive && export DEBIAN_PRIORITY=critical && export UCF_FORCE_CONFFOLD=1 && export NEEDRESTART_MODE=a && dpkg --configure -a && apt-get update && apt-get install -y python3 python3-pip python3-venv git build-essential neovim curl wget ca-certificates"
            },
            "node_dev" to { _ ->
                "export DEBIAN_FRONTEND=noninteractive && export DEBIAN_PRIORITY=critical && export UCF_FORCE_CONFFOLD=1 && export NEEDRESTART_MODE=a && dpkg --configure -a && apt-get update && apt-get install -y nodejs npm yarnpkg git build-essential neovim curl wget ca-certificates"
            },
            "android_dev" to { _ ->
                "export DEBIAN_FRONTEND=noninteractive && export DEBIAN_PRIORITY=critical && export UCF_FORCE_CONFFOLD=1 && export NEEDRESTART_MODE=a && dpkg --configure -a && apt-get update && apt-get install -y openjdk-17-jdk-headless android-sdk-platform-tools gradle git curl wget unzip ca-certificates"
            },
            "nginx_web" to { _ ->
                "export DEBIAN_FRONTEND=noninteractive && export DEBIAN_PRIORITY=critical && export UCF_FORCE_CONFFOLD=1 && export NEEDRESTART_MODE=a && dpkg --configure -a && apt-get update && apt-get install -y nginx sqlite3 curl ca-certificates && (sed -i 's/\\b80\\b/8080/g' /etc/nginx/sites-available/default /etc/nginx/sites-enabled/* /etc/nginx/conf.d/*.conf /etc/nginx/http.d/*.conf /etc/nginx/nginx.conf 2>/dev/null || true) && (sed -i 's/^\\s*user\\s\\+www-data/#user www-data/' /etc/nginx/nginx.conf 2>/dev/null || true) && mkdir -p /run /var/log/nginx /var/lib/nginx && chmod -R 777 /run /var/log/nginx /var/lib/nginx 2>/dev/null || true"
            },
            "openssh_server" to { port ->
                "export DEBIAN_FRONTEND=noninteractive && export DEBIAN_PRIORITY=critical && export UCF_FORCE_CONFFOLD=1 && export NEEDRESTART_MODE=a && dpkg --configure -a && apt-get update && apt-get install -y openssh-server ca-certificates && mkdir -p /run/sshd /var/run/sshd /var/empty /etc/ssh/sshd_config.d && [ -e /dev/ptmx ] || (mknod -m 666 /dev/ptmx c 5 2 2>/dev/null || ln -s /dev/pts/ptmx /dev/ptmx 2>/dev/null || true) && chmod 666 /dev/ptmx 2>/dev/null || true && ssh-keygen -A 2>/dev/null || true && echo \"Port $port\" > /etc/ssh/sshd_config.d/00-linuxonandroid.conf && echo \"PermitRootLogin yes\" >> /etc/ssh/sshd_config.d/00-linuxonandroid.conf && echo \"PasswordAuthentication yes\" >> /etc/ssh/sshd_config.d/00-linuxonandroid.conf && echo \"KbdInteractiveAuthentication yes\" >> /etc/ssh/sshd_config.d/00-linuxonandroid.conf && echo \"UsePAM no\" >> /etc/ssh/sshd_config.d/00-linuxonandroid.conf && echo \"StrictModes no\" >> /etc/ssh/sshd_config.d/00-linuxonandroid.conf && echo \"SetEnv PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin\" >> /etc/ssh/sshd_config.d/00-linuxonandroid.conf && echo \"Subsystem sftp internal-sftp\" >> /etc/ssh/sshd_config.d/00-linuxonandroid.conf && (sed -i 's/^Subsystem.*sftp/#&/' /etc/ssh/sshd_config 2>/dev/null || true) && (sed -i 's/^#\\?UsePAM.*/UsePAM no/' /etc/ssh/sshd_config 2>/dev/null || true) && (sed -i 's/^#\\?PasswordAuthentication.*/PasswordAuthentication yes/' /etc/ssh/sshd_config 2>/dev/null || true) && (sed -i 's/^#\\?PermitRootLogin.*/PermitRootLogin yes/' /etc/ssh/sshd_config 2>/dev/null || true) && (sed -i 's/^session.*pam_loginuid.so/#&/' /etc/pam.d/sshd 2>/dev/null || true) && chmod 600 /etc/ssh/ssh_host_*_key 2>/dev/null || true && chmod 755 /etc/ssh /run/sshd /var/run/sshd /var/empty 2>/dev/null || true"
            }
        ),
        softwarePackageLaunchCommands = mapOf(
            "xfce_desktop" to { _ ->
                "rm -f /etc/tigervnc/vncserver-config-defaults 2>/dev/null || true; mkdir -p /tmp/.X11-unix /tmp/.ICE-unix && chmod 1777 /tmp/.X11-unix /tmp/.ICE-unix 2>/dev/null || true; for u in /home/*; do if [ -d \"\$u\" ]; then mkdir -p \"\$u/.config/tigervnc\" \"\$u/.vnc\" && chmod -R 777 \"\$u/.vnc\" \"\$u/.config\" 2>/dev/null || true; [ -f \"\$u/.config/tigervnc/passwd\" ] || (echo kali | vncpasswd -f > \"\$u/.config/tigervnc/passwd\" 2>/dev/null || echo kali | tigervncpasswd -f > \"\$u/.config/tigervnc/passwd\" 2>/dev/null || true); [ -f \"\$u/.vnc/passwd\" ] || cp \"\$u/.config/tigervnc/passwd\" \"\$u/.vnc/passwd\" 2>/dev/null || true; chmod 600 \"\$u/.vnc/passwd\" \"\$u/.config/tigervnc/passwd\" 2>/dev/null || true; fi; done; vncserver -kill :1 2>/dev/null || true; rm -f /tmp/.X1-lock /tmp/.X11-unix/X1 2>/dev/null; vncserver :1 -xstartup /etc/vnc/xstartup -geometry 1280x720 -depth 24 -SecurityTypes None,VncAuth -UseBlacklist=0 --I-KNOW-THIS-IS-INSECURE"
            }
        ),
        softwarePackageExpectedBinaries = mapOf(
            "xfce_desktop" to listOf(
                "usr/bin/startxfce4",
                "usr/bin/vncserver",
                "usr/bin/vncpasswd",
                "etc/vnc/xstartup"
            )
        )
    )

    val VOID_ROLLING = DistroDefinition(
        id = "void_rolling",
        name = "Void Linux",
        version = "Rolling",
        tag = "Fast & Independent",
        description = "General-purpose, independent Linux distribution featuring the XBPS package system and fast boot times.",
        packageManager = PackageManagerType.XBPS,
        defaultShell = "/bin/bash",
        downloadSizeMb = 43,
        installedSizeMb = 283,
        colorHex = 0xFF478061,
        downloadUrls = mapOf(
            SystemArchitecture.ARM64 to "https://repo-default.voidlinux.org/live/current/void-aarch64-ROOTFS-20250202.tar.xz",
            SystemArchitecture.X86_64 to "https://repo-default.voidlinux.org/live/current/void-x86_64-ROOTFS-20250202.tar.xz",
            SystemArchitecture.ARMV7 to "https://repo-default.voidlinux.org/live/current/void-armv7l-ROOTFS-20250202.tar.xz"
        ),
        firstLaunchScriptBuilder = { rootPassword, username, userPassword, _ ->
            "export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin; " +
                    "mkdir -p /etc/sudoers.d /etc/pam.d /home/$username 2>/dev/null; " +
                    "(grep -q ^$username: /etc/passwd || echo \"$username:x:1000:1000:$username:/home/$username:/bin/bash\" >> /etc/passwd); " +
                    "(grep -q ^$username: /etc/group || echo \"$username:x:1000:\" >> /etc/group); " +
                    "(grep -q ^wheel: /etc/group && sed -i 's/^wheel:.*/&,$username/' /etc/group || echo \"wheel:x:10:root,$username\" >> /etc/group); " +
                    "(grep -q ^$username: /etc/shadow || echo \"$username:*:19700:0:99999:7:::\" >> /etc/shadow); " +
                    "chown -R 1000:1000 /home/$username 2>/dev/null || true; " +
                    "chmod 644 /etc/shadow /etc/shadow- /etc/passwd /etc/group 2>/dev/null || true; " +
                    "echo \"$username ALL=(ALL:ALL) NOPASSWD:ALL\" > /etc/sudoers.d/$username && chmod 0440 /etc/sudoers.d/$username; " +
                    "printf 'auth sufficient pam_permit.so\\naccount sufficient pam_permit.so\\nsession sufficient pam_permit.so\\npassword sufficient pam_permit.so\\n' > /etc/pam.d/su 2>/dev/null || true; " +
                    "xbps-install -Sy 2>/dev/null || true; " +
                    "xbps-install -y bash sudo coreutils curl wget nano procps 2>/dev/null || true; " +
                    "echo \"root:$rootPassword\" | chpasswd 2>/dev/null; passwd -u root 2>/dev/null || true; " +
                    "echo \"$username:$userPassword\" | chpasswd 2>/dev/null; passwd -u $username 2>/dev/null || true"
        },
        softwarePackageCommands = mapOf(
            "xfce_desktop" to { _ ->
                "xbps-install -Sy && xbps-install -y xfce4 xfce4-terminal tigervnc curl ca-certificates python3 && mkdir -p /root/.vnc /etc/skel/.vnc /etc/vnc /tmp/.X11-unix /tmp/.ICE-unix && chmod 1777 /tmp/.X11-unix /tmp/.ICE-unix 2>/dev/null || true && echo void | vncpasswd -f > /root/.vnc/passwd && chmod 600 /root/.vnc/passwd && cat << 'EOF' > /etc/vnc/xstartup\n#!/bin/sh\nunset SESSION_MANAGER\nunset DBUS_SESSION_BUS_ADDRESS\nexport XDG_SESSION_TYPE=x11\nexport XDG_CURRENT_DESKTOP=XFCE\nexport DESKTOP_SESSION=xfce\nxfsettingsd --daemon 2>/dev/null || true\nxfwm4 --daemon 2>/dev/null || xfwm4 &\nxfce4-panel &\nexec startxfce4\nEOF\nchmod 755 /etc/vnc/xstartup && cp /etc/vnc/xstartup /root/.vnc/xstartup && printf 'securitytypes=None,VncAuth\\ngeometry=1280x720\\nlocalhost=no\\nalwaysshared=1\\n' > /etc/vnc/config && chmod 644 /etc/vnc/config && cp /etc/vnc/config /root/.vnc/config && for u in /home/*; do if [ -d \"\$u\" ]; then mkdir -p \"\$u/.vnc\" \"\$u/.config/tigervnc\" && cp /etc/vnc/xstartup \"\$u/.vnc/xstartup\" && cp /etc/vnc/config \"\$u/.vnc/config\" && echo void | vncpasswd -f > \"\$u/.vnc/passwd\" && chmod 755 \"\$u/.vnc/xstartup\" && chmod 600 \"\$u/.vnc/passwd\" 2>/dev/null || true; fi; done 2>/dev/null || true"
            },
            "python_dev" to { _ ->
                "xbps-install -Sy && xbps-install -y python3 python3-pip git base-devel neovim curl wget ca-certificates"
            },
            "node_dev" to { _ ->
                "xbps-install -Sy && xbps-install -y nodejs npm yarn git base-devel neovim curl wget ca-certificates"
            },
            "android_dev" to { _ ->
                "xbps-install -Sy && xbps-install -y openjdk17-jre android-tools gradle git curl wget unzip ca-certificates"
            },
            "nginx_web" to { _ ->
                "xbps-install -Sy && xbps-install -y nginx sqlite curl ca-certificates && (sed -i 's/\\b80\\b/8080/g' /etc/nginx/nginx.conf 2>/dev/null || true) && mkdir -p /run /var/log/nginx /var/lib/nginx && chmod -R 777 /run /var/log/nginx /var/lib/nginx 2>/dev/null || true"
            },
            "openssh_server" to { port ->
                "xbps-install -Sy && xbps-install -y openssh ca-certificates && mkdir -p /run/sshd /var/run/sshd /var/empty && ssh-keygen -A 2>/dev/null || true && (sed -i 's/^#\\?PasswordAuthentication.*/PasswordAuthentication yes/' /etc/ssh/sshd_config 2>/dev/null || true) && (sed -i 's/^#\\?PermitRootLogin.*/PermitRootLogin yes/' /etc/ssh/sshd_config 2>/dev/null || true) && echo \"Port $port\" >> /etc/ssh/ssh_config && chmod 600 /etc/ssh/ssh_host_*_key 2>/dev/null || true"
            }
        )
    )

    val ALL_DISTROS = listOf(
        UBUNTU_26_04,
        DEBIAN_12,
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
