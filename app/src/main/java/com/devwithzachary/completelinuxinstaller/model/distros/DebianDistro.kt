package com.devwithzachary.completelinuxinstaller.model.distros

import com.devwithzachary.completelinuxinstaller.model.DistroCatalog.COMMON_DOCKER_WRAPPER
import com.devwithzachary.completelinuxinstaller.model.DistroCatalog.UDOCKER_INSTALL_PIPELINE
import com.devwithzachary.completelinuxinstaller.model.DistroDefinition
import com.devwithzachary.completelinuxinstaller.model.PackageManagerType
import com.devwithzachary.completelinuxinstaller.model.SystemArchitecture

val DebianDistro = DistroDefinition(
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
    checksumUrls = mapOf(
        SystemArchitecture.ARM64 to "https://doi-janky.infosiftr.net/job/tianon/job/debuerreotype/job/arm64v8/lastSuccessfulBuild/artifact/bookworm/rootfs.tar.xz.sha256",
        SystemArchitecture.X86_64 to "https://doi-janky.infosiftr.net/job/tianon/job/debuerreotype/job/amd64/lastSuccessfulBuild/artifact/bookworm/rootfs.tar.xz.sha256",
        SystemArchitecture.ARMV7 to "https://doi-janky.infosiftr.net/job/tianon/job/debuerreotype/job/arm32v7/lastSuccessfulBuild/artifact/bookworm/rootfs.tar.xz.sha256"
    ),
    sha256Checksums = mapOf(
        SystemArchitecture.ARM64 to "500eec4019eab9492cd64c1b07f8061d2fa7eac7fa3ef1fff8b9d6400f60334e",
        SystemArchitecture.X86_64 to "03e8ce9c6e07367ce89fa4c6c3a34dde952cf8f51824741e4506f86d8a2928a3",
        SystemArchitecture.ARMV7 to "502c52d27984c146f4b287a3ca51a73ee74864f85ffdb82378180b300b5c59bb"
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
            "export DEBIAN_FRONTEND=noninteractive && export DEBIAN_PRIORITY=critical && export UCF_FORCE_CONFFOLD=1 && export NEEDRESTART_MODE=a && dpkg --configure -a && apt-get update && apt-get install -y --no-install-recommends xfce4 xfce4-terminal dbus-x11 tigervnc-standalone-server tigervnc-tools tigervnc-common synaptic x11-utils curl ca-certificates perl python3 libgdk-pixbuf2.0-bin librsvg2-common adwaita-icon-theme hicolor-icon-theme && (printf '#!/bin/sh\\nexec /usr/sbin/synaptic \"\$@\"\\n' > /usr/bin/synaptic-pkexec && chmod 755 /usr/bin/synaptic-pkexec 2>/dev/null || true) && rm -f /etc/tigervnc/vncserver-config-defaults && mkdir -p /root/.vnc /etc/skel/.vnc /etc/vnc /tmp/.X11-unix /tmp/.ICE-unix && chmod 1777 /tmp/.X11-unix /tmp/.ICE-unix 2>/dev/null || true; (echo debian | vncpasswd -f > /root/.vnc/passwd 2>/dev/null || echo debian | tigervncpasswd -f > /root/.vnc/passwd 2>/dev/null || true) && chmod 600 /root/.vnc/passwd 2>/dev/null || true; (echo debian | vncpasswd -f > /etc/skel/.vnc/passwd 2>/dev/null || echo debian | tigervncpasswd -f > /etc/skel/.vnc/passwd 2>/dev/null || true) && chmod 600 /etc/skel/.vnc/passwd 2>/dev/null || true; printf '#!/bin/sh\\nif [ \"\$1\" = \"--version\" ] || [ \"\$1\" = \"-v\" ]; then\\n    echo \"bubblewrap 0.11.0 (PRoot shim)\"\\n    exit 0\\nfi\\nwhile [ \$# -gt 0 ]; do\\n    case \"\$1\" in\\n        --)\\n            shift\\n            [ -n \"\$1\" ] && exec \"\$@\"\n            exit 0\\n            ;;\\n        /*|./*)\\n            if [ -x \"\$1\" ] && [ ! -d \"\$1\" ]; then\\n                exec \"\$@\"\n            fi\\n            ;;\\n    esac\\n    shift\\ndone\\nexit 0\\n' > /usr/bin/bwrap && chmod 755 /usr/bin/bwrap; cp /usr/bin/bwrap /usr/local/bin/bwrap 2>/dev/null || true; chmod 755 /usr/local/bin/bwrap 2>/dev/null || true; printf '#!/bin/sh\\nexport PATH=\"/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/usr/lib/xfce4/xfconf:/usr/lib64/xfce4/xfconf:/usr/libexec/xfce4/xfconf:\$PATH\"\\nunset SESSION_MANAGER\\nunset DBUS_SESSION_BUS_ADDRESS\\nexport XDG_SESSION_TYPE=x11\\nexport XDG_CURRENT_DESKTOP=XFCE\\nexport DESKTOP_SESSION=xfce\\nexport NO_AT_BRIDGE=1\\nexport GDK_BACKEND=x11\\nexport GTK_OVERLAY_SCROLLING=0\\nexport GLYCIN_DISABLE_SANDBOX=1\\nexport GLYCIN_ENABLE_SANDBOX=0\\nexport LIBGL_ALWAYS_SOFTWARE=1\\n[ -r \$HOME/.Xresources ] && xrdb \$HOME/.Xresources 2>/dev/null || true\\nif command -v dbus-launch >/dev/null 2>&1; then\\n    eval \$(dbus-launch --sh-syntax)\\nfi\\nif command -v xfconfd >/dev/null 2>&1; then\\n    xfconfd &\\nelif [ -x /usr/lib64/xfce4/xfconf/xfconfd ]; then\\n    /usr/lib64/xfce4/xfconf/xfconfd &\\nelif [ -x /usr/lib/xfce4/xfconf/xfconfd ]; then\\n    /usr/lib/xfce4/xfconf/xfconfd &\\nelif [ -x /usr/libexec/xfce4/xfconf/xfconfd ]; then\\n    /usr/libexec/xfce4/xfconf/xfconfd &\\nfi\\nxsetroot -solid \"#1e293b\" 2>/dev/null || true\\nxfconf-query -c xfwm4 -p /general/use_compositing -n -t bool -s false 2>/dev/null || true\\nif command -v startxfce4 >/dev/null 2>&1; then\\n    exec startxfce4\\nelif command -v xfce4-session >/dev/null 2>&1; then\\n    exec xfce4-session\\nelse\\n    xfsettingsd --daemon 2>/dev/null || true\\n    xfwm4 --compositor=off &\\n    xfce4-panel &\\n    Thunar --daemon 2>/dev/null &\\n    if command -v xfdesktop >/dev/null 2>&1; then\\n        exec xfdesktop\\n    else\\n        exec xterm\\n    fi\\nfi\\n' > /etc/vnc/xstartup && chmod 755 /etc/vnc/xstartup && cp /etc/vnc/xstartup /root/.vnc/xstartup && cp /etc/vnc/xstartup /etc/skel/.vnc/xstartup && chmod 755 /root/.vnc/xstartup /etc/skel/.vnc/xstartup; printf 'securitytypes=VncAuth\\ngeometry=1280x720\\nlocalhost=yes\\nalwaysshared=1\\n' > /etc/vnc/config && chmod 644 /etc/vnc/config && cp /etc/vnc/config /root/.vnc/config && cp /etc/vnc/config /etc/skel/.vnc/config; for u in /home/*; do if [ -d \"\$u\" ]; then mkdir -p \"\$u/.vnc\" \"\$u/.config/tigervnc\" && cp /etc/vnc/xstartup \"\$u/.vnc/xstartup\" && cp /etc/vnc/config \"\$u/.vnc/config\" && (echo debian | vncpasswd -f > \"\$u/.vnc/passwd\" 2>/dev/null || echo debian | tigervncpasswd -f > \"\$u/.vnc/passwd\" 2>/dev/null || true) && (echo debian | vncpasswd -f > \"\$u/.config/tigervnc/passwd\" 2>/dev/null || echo debian | tigervncpasswd -f > \"\$u/.config/tigervnc/passwd\" 2>/dev/null || true) && chmod 755 \"\$u/.vnc/xstartup\" && chmod -R 777 \"\$u/.vnc\" \"\$u/.config\" 2>/dev/null || true; chmod 600 \"\$u/.vnc/passwd\" \"\$u/.config/tigervnc/passwd\" 2>/dev/null || true; fi; done"
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
        },
        "code_server" to { _ ->
            "export DEBIAN_FRONTEND=noninteractive && export DEBIAN_PRIORITY=critical && export UCF_FORCE_CONFFOLD=1 && export NEEDRESTART_MODE=a && dpkg --configure -a && apt-get update && apt-get install -y curl ca-certificates git procps && (curl -fsSL https://code-server.dev/install.sh | sh || curl -fsSL https://code-server.dev/install.sh | sh -s -- --method=standalone --prefix=/usr/local)"
        },
        "web_terminal" to { _ ->
            "export DEBIAN_FRONTEND=noninteractive && export DEBIAN_PRIORITY=critical && export UCF_FORCE_CONFFOLD=1 && export NEEDRESTART_MODE=a && dpkg --configure -a && apt-get update && (apt-get install -y ttyd curl ca-certificates || true) && if ! command -v ttyd >/dev/null 2>&1; then ${com.devwithzachary.completelinuxinstaller.model.SoftwarePackage.TTYD_FALLBACK_DOWNLOAD_CMD}; fi"
        },
        "docker_tools" to { _ ->
            "export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin; " +
                "mkdir -p /usr/sbin /etc /var/lib/dbus 2>/dev/null; " +
                "(grep -q ^messagebus: /etc/group || echo \"messagebus:x:101:\" >> /etc/group); " +
                "(grep -q ^messagebus: /etc/passwd || echo \"messagebus:x:101:101:D-Bus Message System Daemon:/nonexistent:/bin/false\" >> /etc/passwd); " +
                "(grep -q ^messagebus: /etc/shadow || echo \"messagebus:*:19700:0:99999:7:::\" >> /etc/shadow); " +
                "(grep -q ^docker: /etc/group || echo \"docker:x:102:\" >> /etc/group); " +
                "printf '#!/bin/sh\\nexit 101\\n' > /usr/sbin/policy-rc.d && chmod 755 /usr/sbin/policy-rc.d; " +
                "chmod -R 755 /usr/lib/cargo /usr/libexec 2>/dev/null; " +
                "export DEBIAN_FRONTEND=noninteractive && export DEBIAN_PRIORITY=critical && export UCF_FORCE_CONFFOLD=1 && export NEEDRESTART_MODE=a && " +
                "dpkg --configure -a && apt-get update && " +
                "(apt-get install -y docker.io docker-compose python3 python3-pip curl ca-certificates tar || " +
                "apt-get install -y docker-cli docker-compose python3 python3-pip curl ca-certificates tar || " +
                "apt-get install -y python3 python3-pip curl ca-certificates tar || true) && " +
                "$UDOCKER_INSTALL_PIPELINE && $COMMON_DOCKER_WRAPPER"
        }
    ),
    softwarePackageLaunchCommands = mapOf(
        "xfce_desktop" to { _ ->
            "rm -f /etc/tigervnc/vncserver-config-defaults 2>/dev/null || true; mkdir -p /tmp/.X11-unix /tmp/.ICE-unix /root/.vnc && chmod 1777 /tmp/.X11-unix /tmp/.ICE-unix 2>/dev/null || true; for u in /home/*; do if [ -d \"\$u\" ]; then mkdir -p \"\$u/.config/tigervnc\" \"\$u/.vnc\" && chmod -R 777 \"\$u/.vnc\" \"\$u/.config\" 2>/dev/null || true; [ -f \"\$u/.config/tigervnc/passwd\" ] || (echo debian | vncpasswd -f > \"\$u/.config/tigervnc/passwd\" 2>/dev/null || echo debian | tigervncpasswd -f > \"\$u/.config/tigervnc/passwd\" 2>/dev/null || true); [ -f \"\$u/.vnc/passwd\" ] || cp \"\$u/.config/tigervnc/passwd\" \"\$u/.vnc/passwd\" 2>/dev/null || true; chmod 600 \"\$u/.vnc/passwd\" \"\$u/.config/tigervnc/passwd\" 2>/dev/null || true; fi; done; vncserver -kill :1 2>/dev/null || true; rm -f /tmp/.X1-lock /tmp/.X11-unix/X1 2>/dev/null; vncserver :1 -xstartup /etc/vnc/xstartup -geometry 1280x720 -depth 24 -SecurityTypes VncAuth -UseBlacklist=0 -localhost"
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
        "xfce_desktop" to 7
    )
)
