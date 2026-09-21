package com.devwithzachary.completelinuxinstaller.model.distros

import com.devwithzachary.completelinuxinstaller.model.DistroCatalog.COMMON_DOCKER_WRAPPER
import com.devwithzachary.completelinuxinstaller.model.DistroCatalog.UDOCKER_INSTALL_PIPELINE
import com.devwithzachary.completelinuxinstaller.model.DistroDefinition
import com.devwithzachary.completelinuxinstaller.model.PackageManagerType
import com.devwithzachary.completelinuxinstaller.model.SystemArchitecture

val ArchDistro = DistroDefinition(
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
                "sed -i 's/^DownloadUser/#DownloadUser/; s/^#DisableSandbox/DisableSandbox/; s/^SigLevel.*/SigLevel = Never/; s/^LocalFileSigLevel.*/LocalFileSigLevel = Never/' /etc/pacman.conf 2>/dev/null || true; " +
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
            "sed -i 's/^DownloadUser/#DownloadUser/; s/^#DisableSandbox/DisableSandbox/; s/^SigLevel.*/SigLevel = Never/; s/^LocalFileSigLevel.*/LocalFileSigLevel = Never/' /etc/pacman.conf 2>/dev/null || true && " +
                "pacman -Syy --noconfirm xfce4 xfce4-terminal tigervnc curl ca-certificates python && " +
                "rm -f /etc/tigervnc/vncserver-config-defaults 2>/dev/null || true; " +
                "mkdir -p /root/.vnc /etc/skel/.vnc /etc/vnc /tmp/.X11-unix /tmp/.ICE-unix && chmod 1777 /tmp/.X11-unix /tmp/.ICE-unix 2>/dev/null || true; " +
                "(echo arch | vncpasswd -f > /root/.vnc/passwd 2>/dev/null || echo arch | tigervncpasswd -f > /root/.vnc/passwd 2>/dev/null || true) && chmod 600 /root/.vnc/passwd 2>/dev/null || true; " +
                "(echo arch | vncpasswd -f > /etc/skel/.vnc/passwd 2>/dev/null || echo arch | tigervncpasswd -f > /etc/skel/.vnc/passwd 2>/dev/null || true) && chmod 600 /etc/skel/.vnc/passwd 2>/dev/null || true; " +
                "printf '#!/bin/sh\\nunset SESSION_MANAGER\\nunset DBUS_SESSION_BUS_ADDRESS\\nexport XDG_SESSION_TYPE=x11\\nexport XDG_CURRENT_DESKTOP=XFCE\\nexport DESKTOP_SESSION=xfce\\nexport NO_AT_BRIDGE=1\\nexport GDK_BACKEND=x11\\nif command -v dbus-launch >/dev/null 2>&1; then\\n    eval \$(dbus-launch --sh-syntax --exit-with-session)\\nfi\\nxfsettingsd --daemon 2>/dev/null || true\\nxfwm4 --daemon 2>/dev/null || xfwm4 &\\nxfce4-panel &\\nThunar --daemon 2>/dev/null &\\nexec startxfce4\\n' > /etc/vnc/xstartup && chmod 755 /etc/vnc/xstartup && cp /etc/vnc/xstartup /root/.vnc/xstartup && cp /etc/vnc/xstartup /etc/skel/.vnc/xstartup && chmod 755 /root/.vnc/xstartup /etc/skel/.vnc/xstartup; " +
                "printf 'securitytypes=None,VncAuth\\ngeometry=1280x720\\nlocalhost=no\\nalwaysshared=1\\n' > /etc/vnc/config && chmod 644 /etc/vnc/config && cp /etc/vnc/config /root/.vnc/config && cp /etc/vnc/config /etc/skel/.vnc/config; " +
                "for u in /home/*; do if [ -d \"\$u\" ]; then mkdir -p \"\$u/.vnc\" \"\$u/.config/tigervnc\" && cp /etc/vnc/xstartup \"\$u/.vnc/xstartup\" && cp /etc/vnc/config \"\$u/.vnc/config\" && (echo arch | vncpasswd -f > \"\$u/.vnc/passwd\" 2>/dev/null || echo arch | tigervncpasswd -f > \"\$u/.vnc/passwd\" 2>/dev/null || true) && (echo arch | vncpasswd -f > \"\$u/.config/tigervnc/passwd\" 2>/dev/null || echo arch | tigervncpasswd -f > \"\$u/.config/tigervnc/passwd\" 2>/dev/null || true) && chmod 755 \"\$u/.vnc/xstartup\" && chmod -R 777 \"\$u/.vnc\" \"\$u/.config\" 2>/dev/null || true; chmod 600 \"\$u/.vnc/passwd\" \"\$u/.config/tigervnc/passwd\" 2>/dev/null || true; fi; done"
        },
        "python_dev" to { _ ->
            "sed -i 's/^DownloadUser/#DownloadUser/; s/^#DisableSandbox/DisableSandbox/; s/^SigLevel.*/SigLevel = Never/; s/^LocalFileSigLevel.*/LocalFileSigLevel = Never/' /etc/pacman.conf 2>/dev/null || true && pacman -Syy --noconfirm python python-pip git base-devel neovim curl wget ca-certificates"
        },
        "node_dev" to { _ ->
            "sed -i 's/^DownloadUser/#DownloadUser/; s/^#DisableSandbox/DisableSandbox/; s/^SigLevel.*/SigLevel = Never/; s/^LocalFileSigLevel.*/LocalFileSigLevel = Never/' /etc/pacman.conf 2>/dev/null || true && pacman -Syy --noconfirm nodejs npm yarn git base-devel neovim curl wget ca-certificates"
        },
        "android_dev" to { _ ->
            "sed -i 's/^DownloadUser/#DownloadUser/; s/^#DisableSandbox/DisableSandbox/; s/^SigLevel.*/SigLevel = Never/; s/^LocalFileSigLevel.*/LocalFileSigLevel = Never/' /etc/pacman.conf 2>/dev/null || true && pacman -Syy --noconfirm jdk17-openjdk android-tools gradle git curl wget unzip ca-certificates"
        },
        "nginx_web" to { _ ->
            "sed -i 's/^DownloadUser/#DownloadUser/; s/^#DisableSandbox/DisableSandbox/; s/^SigLevel.*/SigLevel = Never/; s/^LocalFileSigLevel.*/LocalFileSigLevel = Never/' /etc/pacman.conf 2>/dev/null || true && pacman -Syy --noconfirm nginx sqlite curl ca-certificates && (sed -i 's/\\b80\\b/8080/g' /etc/nginx/nginx.conf 2>/dev/null || true) && mkdir -p /run /var/log/nginx /var/lib/nginx && chmod -R 777 /run /var/log/nginx /var/lib/nginx 2>/dev/null || true"
        },
        "openssh_server" to { port ->
            "sed -i 's/^DownloadUser/#DownloadUser/; s/^#DisableSandbox/DisableSandbox/; s/^SigLevel.*/SigLevel = Never/; s/^LocalFileSigLevel.*/LocalFileSigLevel = Never/' /etc/pacman.conf 2>/dev/null || true && pacman -Syy --noconfirm openssh ca-certificates && mkdir -p /run/sshd /var/run/sshd /var/empty && [ -e /dev/ptmx ] || (mknod -m 666 /dev/ptmx c 5 2 2>/dev/null || ln -s /dev/pts/ptmx /dev/ptmx 2>/dev/null || true) && chmod 666 /dev/ptmx 2>/dev/null || true && ssh-keygen -A 2>/dev/null || true && (sed -i 's/^#\\?PasswordAuthentication.*/PasswordAuthentication yes/' /etc/ssh/sshd_config 2>/dev/null || true) && (sed -i 's/^#\\?PermitRootLogin.*/PermitRootLogin yes/' /etc/ssh/sshd_config 2>/dev/null || true) && echo \"Port $port\" >> /etc/ssh/ssh_config && chmod 600 /etc/ssh/ssh_host_*_key 2>/dev/null || true && chmod 755 /etc/ssh /run/sshd /var/run/sshd /var/empty 2>/dev/null || true"
        },
        "code_server" to { _ ->
            "sed -i 's/^DownloadUser/#DownloadUser/; s/^#DisableSandbox/DisableSandbox/; s/^SigLevel.*/SigLevel = Never/; s/^LocalFileSigLevel.*/LocalFileSigLevel = Never/' /etc/pacman.conf 2>/dev/null || true && pacman -Syy --noconfirm curl ca-certificates git procps-ng && (curl -fsSL https://code-server.dev/install.sh | sh -s -- --method=standalone --prefix=/usr/local || curl -fsSL https://code-server.dev/install.sh | sh)"
        },
        "web_terminal" to { _ ->
            "sed -i 's/^DownloadUser/#DownloadUser/; s/^#DisableSandbox/DisableSandbox/; s/^SigLevel.*/SigLevel = Never/; s/^LocalFileSigLevel.*/LocalFileSigLevel = Never/' /etc/pacman.conf 2>/dev/null || true && pacman -Syy --noconfirm curl ca-certificates && (pacman -S --noconfirm ttyd || true) && if ! command -v ttyd >/dev/null 2>&1; then ARCH=\$(uname -m); case \"\$ARCH\" in aarch64|arm64) TTYD_BIN=\"ttyd.aarch64\" ;; x86_64|amd64) TTYD_BIN=\"ttyd.x86_64\" ;; armv7*|armhf) TTYD_BIN=\"ttyd.armhf\" ;; *) TTYD_BIN=\"ttyd.aarch64\" ;; esac; (curl -fsSL -o /usr/local/bin/ttyd \"https://github.com/tsl0922/ttyd/releases/download/1.7.7/\$TTYD_BIN\" || wget -qO /usr/local/bin/ttyd \"https://github.com/tsl0922/ttyd/releases/download/1.7.7/\$TTYD_BIN\") && chmod 755 /usr/local/bin/ttyd || true; fi"
        },
        "docker_tools" to { _ ->
            "sed -i 's/^DownloadUser/#DownloadUser/; s/^#DisableSandbox/DisableSandbox/; s/^SigLevel.*/SigLevel = Never/; s/^LocalFileSigLevel.*/LocalFileSigLevel = Never/' /etc/pacman.conf 2>/dev/null || true && pacman -Syy --noconfirm curl ca-certificates tar python python-pip && (pacman -S --noconfirm docker docker-compose || true) && " +
                "$UDOCKER_INSTALL_PIPELINE && $COMMON_DOCKER_WRAPPER"
        }
    ),
    softwarePackageLaunchCommands = mapOf(
        "xfce_desktop" to { _ ->
            "rm -f /etc/tigervnc/vncserver-config-defaults 2>/dev/null || true; mkdir -p /tmp/.X11-unix /tmp/.ICE-unix /root/.vnc && chmod 1777 /tmp/.X11-unix /tmp/.ICE-unix 2>/dev/null || true; [ -f /root/.vnc/passwd ] || (echo arch | vncpasswd -f > /root/.vnc/passwd 2>/dev/null || echo arch | tigervncpasswd -f > /root/.vnc/passwd 2>/dev/null || true); chmod 600 /root/.vnc/passwd 2>/dev/null || true; for u in /home/*; do if [ -d \"\$u\" ]; then mkdir -p \"\$u/.config/tigervnc\" \"\$u/.vnc\" && chmod -R 777 \"\$u/.vnc\" \"\$u/.config\" 2>/dev/null || true; [ -f \"\$u/.config/tigervnc/passwd\" ] || (echo arch | vncpasswd -f > \"\$u/.config/tigervnc/passwd\" 2>/dev/null || echo arch | tigervncpasswd -f > \"\$u/.config/tigervnc/passwd\" 2>/dev/null || true); [ -f \"\$u/.vnc/passwd\" ] || cp \"\$u/.config/tigervnc/passwd\" \"\$u/.vnc/passwd\" 2>/dev/null || true; chmod 600 \"\$u/.vnc/passwd\" \"\$u/.config/tigervnc/passwd\" 2>/dev/null || true; fi; done; vncserver -kill :1 2>/dev/null || true; rm -f /tmp/.X1-lock /tmp/.X11-unix/X1 2>/dev/null; vncserver :1 -xstartup /etc/vnc/xstartup -geometry 1280x720 -depth 24 -SecurityTypes None,VncAuth -UseBlacklist=0 --I-KNOW-THIS-IS-INSECURE"
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
