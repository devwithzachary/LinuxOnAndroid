package com.devwithzachary.completelinuxinstaller.model.distros

import com.devwithzachary.completelinuxinstaller.model.DistroCatalog.COMMON_DOCKER_WRAPPER
import com.devwithzachary.completelinuxinstaller.model.DistroCatalog.UDOCKER_INSTALL_PIPELINE
import com.devwithzachary.completelinuxinstaller.model.DistroDefinition
import com.devwithzachary.completelinuxinstaller.model.PackageManagerType
import com.devwithzachary.completelinuxinstaller.model.SystemArchitecture

val UbuntuDistro = DistroDefinition(
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
    checksumUrls = mapOf(
        SystemArchitecture.ARM64 to "https://cdimage.ubuntu.com/ubuntu-base/releases/26.04/release/SHA256SUMS",
        SystemArchitecture.X86_64 to "https://cdimage.ubuntu.com/ubuntu-base/releases/26.04/release/SHA256SUMS",
        SystemArchitecture.ARMV7 to "https://cdimage.ubuntu.com/ubuntu-base/releases/26.04/release/SHA256SUMS"
    ),
    sha256Checksums = mapOf(
        SystemArchitecture.ARM64 to "b2b46a37324ea1954e93f293fe6d7c2241daf2fc298c4022e6e4caceeed74cab",
        SystemArchitecture.X86_64 to "046fcabb7f16f45a80ae11824664f2a07e01386c6fb1ed9dc1e225a66a6553a2",
        SystemArchitecture.ARMV7 to "414e9d5685ff8a6f4497149544e5aa76129f51aa2b97ccd94d845a9803725b46"
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
            "export DEBIAN_FRONTEND=noninteractive && export DEBIAN_PRIORITY=critical && export UCF_FORCE_CONFFOLD=1 && export NEEDRESTART_MODE=a && dpkg --configure -a && apt-get update -o Dpkg::Options::=\"--force-unsafe-io\" -o Dpkg::Options::=\"--force-overwrite\" -o Dpkg::Options::=\"--force-confdef\" -o Dpkg::Options::=\"--force-confold\" && apt-get install -y -o Dpkg::Options::=\"--force-unsafe-io\" -o Dpkg::Options::=\"--force-overwrite\" -o Dpkg::Options::=\"--force-confdef\" -o Dpkg::Options::=\"--force-confold\" xfce4 xfce4-terminal dbus-x11 tigervnc-standalone-server tigervnc-xorg-extension synaptic curl ca-certificates perl python3 libgdk-pixbuf2.0-bin librsvg2-common adwaita-icon-theme hicolor-icon-theme && (printf '#!/bin/sh\\nexec /usr/sbin/synaptic \"\$@\"\\n' > /usr/bin/synaptic-pkexec && chmod 755 /usr/bin/synaptic-pkexec 2>/dev/null || true) && rm -f /etc/tigervnc/vncserver-config-defaults && mkdir -p /root/.vnc /etc/skel/.vnc /etc/vnc /tmp/.X11-unix /tmp/.ICE-unix && chmod 1777 /tmp/.X11-unix /tmp/.ICE-unix 2>/dev/null || true && echo ubuntu | vncpasswd -f > /root/.vnc/passwd && chmod 600 /root/.vnc/passwd && echo ubuntu | vncpasswd -f > /etc/skel/.vnc/passwd && chmod 600 /etc/skel/.vnc/passwd && cat << 'EOF' > /usr/bin/bwrap\n#!/bin/sh\nif [ \"\$1\" = \"--version\" ] || [ \"\$1\" = \"-v\" ]; then\n    echo \"bubblewrap 0.11.0 (PRoot shim)\"\n    exit 0\nfi\nwhile [ \$# -gt 0 ]; do\n    case \"\$1\" in\n        --)\n            shift\n            [ -n \"\$1\" ] && exec \"\$@\"\n            exit 0\n            ;;\n        /*|./*)\\n            if [ -x \"\$1\" ] && [ ! -d \"\$1\" ]; then\n                exec \"\$@\"\n            fi\n            ;;\n    esac\n    shift\ndone\nexit 0\nEOF\nchmod 755 /usr/bin/bwrap && cp /usr/bin/bwrap /usr/local/bin/bwrap 2>/dev/null || true; chmod 755 /usr/local/bin/bwrap 2>/dev/null || true; cat << 'EOF' > /etc/vnc/xstartup\n#!/bin/sh\nexport PATH=\"/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/usr/lib/xfce4/xfconf:/usr/lib64/xfce4/xfconf:/usr/libexec/xfce4/xfconf:\$PATH\"\nunset SESSION_MANAGER\nunset DBUS_SESSION_BUS_ADDRESS\nexport XDG_SESSION_TYPE=x11\nexport XDG_CURRENT_DESKTOP=XFCE\nexport DESKTOP_SESSION=xfce\nexport NO_AT_BRIDGE=1\nexport GDK_BACKEND=x11\nexport GTK_OVERLAY_SCROLLING=0\nexport GLYCIN_DISABLE_SANDBOX=1\nexport GLYCIN_ENABLE_SANDBOX=0\nexport LIBGL_ALWAYS_SOFTWARE=1\n[ -r \$HOME/.Xresources ] && xrdb \$HOME/.Xresources 2>/dev/null || true\nif command -v dbus-launch >/dev/null 2>&1; then\n    eval \$(dbus-launch --sh-syntax)\nfi\nif command -v xfconfd >/dev/null 2>&1; then\n    xfconfd &\nelif [ -x /usr/lib64/xfce4/xfconf/xfconfd ]; then\n    /usr/lib64/xfce4/xfconf/xfconfd &\nelif [ -x /usr/lib/xfce4/xfconf/xfconfd ]; then\n    /usr/lib/xfce4/xfconf/xfconfd &\nelif [ -x /usr/libexec/xfce4/xfconf/xfconfd ]; then\n    /usr/libexec/xfce4/xfconf/xfconfd &\nfi\nxsetroot -solid \"#1e293b\" 2>/dev/null || true\nxfconf-query -c xfwm4 -p /general/use_compositing -n -t bool -s false 2>/dev/null || true\nif command -v startxfce4 >/dev/null 2>&1; then\n    exec startxfce4\nelif command -v xfce4-session >/dev/null 2>&1; then\n    exec xfce4-session\nelse\n    xfsettingsd --daemon 2>/dev/null || true\n    xfwm4 --compositor=off &\n    xfce4-panel &\n    Thunar --daemon 2>/dev/null &\n    if command -v xfdesktop >/dev/null 2>&1; then\n        exec xfdesktop\n    else\n        exec xterm\n    fi\nfi\nEOF\nchmod 755 /etc/vnc/xstartup && cp /etc/vnc/xstartup /etc/X11/Xtigervnc-session 2>/dev/null || true && chmod 755 /etc/X11/Xtigervnc-session 2>/dev/null || true && cp /etc/vnc/xstartup /root/.vnc/xstartup && cp /etc/vnc/xstartup /etc/skel/.vnc/xstartup && chmod 755 /root/.vnc/xstartup /etc/skel/.vnc/xstartup && printf 'securitytypes=VncAuth\\ngeometry=1280x720\\nlocalhost=yes\\nalwaysshared=1\\n' > /etc/vnc/config && chmod 644 /etc/vnc/config && cp /etc/vnc/config /root/.vnc/config && cp /etc/vnc/config /etc/skel/.vnc/config && for u in /home/*; do if [ -d \"\$u\" ]; then mkdir -p \"\$u/.vnc\" \"\$u/.config/tigervnc\" && cp /etc/vnc/xstartup \"\$u/.vnc/xstartup\" && cp /etc/vnc/config \"\$u/.vnc/config\" && echo ubuntu | vncpasswd -f > \"\$u/.vnc/passwd\" && echo ubuntu | vncpasswd -f > \"\$u/.config/tigervnc/passwd\" && chmod 755 \"\$u/.vnc/xstartup\" && chmod -R 777 \"\$u/.vnc\" \"\$u/.config\" 2>/dev/null || true; chmod 600 \"\$u/.vnc/passwd\" \"\$u/.config/tigervnc/passwd\" 2>/dev/null || true; fi; done 2>/dev/null || true"
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
        },
        "code_server" to { _ ->
            "export DEBIAN_FRONTEND=noninteractive && export DEBIAN_PRIORITY=critical && export UCF_FORCE_CONFFOLD=1 && export NEEDRESTART_MODE=a && dpkg --configure -a && chmod -R 755 /usr/lib/cargo /usr/libexec 2>/dev/null && apt-get update -o Dpkg::Options::=\"--force-unsafe-io\" -o Dpkg::Options::=\"--force-overwrite\" -o Dpkg::Options::=\"--force-confdef\" -o Dpkg::Options::=\"--force-confold\" && apt-get install -y -o Dpkg::Options::=\"--force-unsafe-io\" -o Dpkg::Options::=\"--force-overwrite\" -o Dpkg::Options::=\"--force-confdef\" -o Dpkg::Options::=\"--force-confold\" curl ca-certificates git procps && (curl -fsSL https://code-server.dev/install.sh | sh || curl -fsSL https://code-server.dev/install.sh | sh -s -- --method=standalone --prefix=/usr/local)"
        },
        "web_terminal" to { _ ->
            "export DEBIAN_FRONTEND=noninteractive && export DEBIAN_PRIORITY=critical && export UCF_FORCE_CONFFOLD=1 && export NEEDRESTART_MODE=a && dpkg --configure -a && chmod -R 755 /usr/lib/cargo /usr/libexec 2>/dev/null && apt-get update -o Dpkg::Options::=\"--force-unsafe-io\" -o Dpkg::Options::=\"--force-overwrite\" -o Dpkg::Options::=\"--force-confdef\" -o Dpkg::Options::=\"--force-confold\" && (apt-get install -y -o Dpkg::Options::=\"--force-unsafe-io\" -o Dpkg::Options::=\"--force-overwrite\" -o Dpkg::Options::=\"--force-confdef\" -o Dpkg::Options::=\"--force-confold\" ttyd curl ca-certificates || true) && if ! command -v ttyd >/dev/null 2>&1; then ${com.devwithzachary.completelinuxinstaller.model.SoftwarePackage.TTYD_FALLBACK_DOWNLOAD_CMD}; fi"
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
                "dpkg --configure -a && " +
                "apt-get update -o Dpkg::Options::=\"--force-unsafe-io\" -o Dpkg::Options::=\"--force-overwrite\" -o Dpkg::Options::=\"--force-confdef\" -o Dpkg::Options::=\"--force-confold\" && " +
                "(apt-get install -y -o Dpkg::Options::=\"--force-unsafe-io\" -o Dpkg::Options::=\"--force-overwrite\" -o Dpkg::Options::=\"--force-confdef\" -o Dpkg::Options::=\"--force-confold\" docker.io docker-compose python3 python3-pip curl ca-certificates tar || " +
                "apt-get install -y -o Dpkg::Options::=\"--force-unsafe-io\" -o Dpkg::Options::=\"--force-overwrite\" -o Dpkg::Options::=\"--force-confdef\" -o Dpkg::Options::=\"--force-confold\" docker-cli docker-compose python3 python3-pip curl ca-certificates tar || " +
                "apt-get install -y -o Dpkg::Options::=\"--force-unsafe-io\" -o Dpkg::Options::=\"--force-overwrite\" -o Dpkg::Options::=\"--force-confdef\" -o Dpkg::Options::=\"--force-confold\" python3 python3-pip curl ca-certificates tar || true) && " +
                "$UDOCKER_INSTALL_PIPELINE && $COMMON_DOCKER_WRAPPER"
        }
    )
)
