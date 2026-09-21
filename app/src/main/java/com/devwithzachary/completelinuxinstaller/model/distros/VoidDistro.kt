package com.devwithzachary.completelinuxinstaller.model.distros

import com.devwithzachary.completelinuxinstaller.model.DistroCatalog.COMMON_DOCKER_WRAPPER
import com.devwithzachary.completelinuxinstaller.model.DistroCatalog.COMMON_VNCSERVER_WRAPPER
import com.devwithzachary.completelinuxinstaller.model.DistroCatalog.UDOCKER_INSTALL_PIPELINE
import com.devwithzachary.completelinuxinstaller.model.DistroDefinition
import com.devwithzachary.completelinuxinstaller.model.PackageManagerType
import com.devwithzachary.completelinuxinstaller.model.SystemArchitecture

val VoidDistro = DistroDefinition(
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
                "(xbps-install -Syu xbps -y 2>/dev/null || xbps-install -u xbps -y 2>/dev/null || true); " +
                "xbps-install -y bash sudo coreutils curl wget nano procps 2>/dev/null || true; " +
                "echo \"root:$rootPassword\" | chpasswd 2>/dev/null; passwd -u root 2>/dev/null || true; " +
                "echo \"$username:$userPassword\" | chpasswd 2>/dev/null; passwd -u $username 2>/dev/null || true"
    },
    softwarePackageCommands = mapOf(
        "xfce_desktop" to { _ ->
            "export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin; " +
                "(xbps-install -Syu xbps -y 2>/dev/null || xbps-install -u xbps -y 2>/dev/null || true) && " +
                "xbps-install -y xfce4 xfce4-terminal tigervnc libstdc++ curl ca-certificates python3 && " +
                "$COMMON_VNCSERVER_WRAPPER && " +
                "rm -f /etc/tigervnc/vncserver-config-defaults 2>/dev/null || true; " +
                "mkdir -p /root/.vnc /etc/skel/.vnc /etc/vnc /tmp/.X11-unix /tmp/.ICE-unix && " +
                "chmod 1777 /tmp/.X11-unix /tmp/.ICE-unix 2>/dev/null || true; " +
                "(echo void | vncpasswd -f > /root/.vnc/passwd 2>/dev/null || echo void | tigervncpasswd -f > /root/.vnc/passwd 2>/dev/null || true) && " +
                "chmod 600 /root/.vnc/passwd 2>/dev/null || true; " +
                "(echo void | vncpasswd -f > /etc/skel/.vnc/passwd 2>/dev/null || echo void | tigervncpasswd -f > /etc/skel/.vnc/passwd 2>/dev/null || true) && " +
                "chmod 600 /etc/skel/.vnc/passwd 2>/dev/null || true; " +
                "printf '#!/usr/bin/env python3\\nimport sys, os\\nargs = sys.argv[1:]\\nexec_idx = -1\\nfor i, arg in enumerate(args):\\n    if arg.startswith(\"/usr/\") and os.path.isfile(arg) and os.access(arg, os.X_OK):\\n        exec_idx = i\\n        break\\nif exec_idx >= 0:\\n    os.execv(args[exec_idx], args[exec_idx:])\\nelse:\\n    sys.exit(0)\\n' > /usr/bin/bwrap && chmod 755 /usr/bin/bwrap 2>/dev/null || true; " +
                "printf '#!/bin/sh\\nunset SESSION_MANAGER\\nunset DBUS_SESSION_BUS_ADDRESS\\nexport XDG_SESSION_TYPE=x11\\nexport XDG_CURRENT_DESKTOP=XFCE\\nexport DESKTOP_SESSION=xfce\\nexport NO_AT_BRIDGE=1\\nexport GDK_BACKEND=x11\\nif command -v dbus-launch >/dev/null 2>&1; then\\n    eval \$(dbus-launch --sh-syntax --exit-with-session)\\nfi\\nxfsettingsd --daemon 2>/dev/null || true\\nxfwm4 --daemon 2>/dev/null || xfwm4 &\\nxfce4-panel &\\nThunar --daemon 2>/dev/null &\\nexec startxfce4\\n' > /etc/vnc/xstartup && chmod 755 /etc/vnc/xstartup && cp /etc/vnc/xstartup /root/.vnc/xstartup && cp /etc/vnc/xstartup /etc/skel/.vnc/xstartup && chmod 755 /root/.vnc/xstartup /etc/skel/.vnc/xstartup; " +
                "printf 'securitytypes=None,VncAuth\\ngeometry=1280x720\\nlocalhost=no\\nalwaysshared=1\\n' > /etc/vnc/config && chmod 644 /etc/vnc/config && cp /etc/vnc/config /root/.vnc/config && cp /etc/vnc/config /etc/skel/.vnc/config; " +
                "for u in /home/*; do if [ -d \"\$u\" ]; then " +
                    "mkdir -p \"\$u/.vnc\" \"\$u/.config/tigervnc\" && " +
                    "cp /etc/vnc/xstartup \"\$u/.vnc/xstartup\" && " +
                    "cp /etc/vnc/config \"\$u/.vnc/config\" && " +
                    "(echo void | vncpasswd -f > \"\$u/.vnc/passwd\" 2>/dev/null || echo void | tigervncpasswd -f > \"\$u/.vnc/passwd\" 2>/dev/null || true) && " +
                    "(echo void | vncpasswd -f > \"\$u/.config/tigervnc/passwd\" 2>/dev/null || echo void | tigervncpasswd -f > \"\$u/.config/tigervnc/passwd\" 2>/dev/null || true) && " +
                    "chmod 755 \"\$u/.vnc/xstartup\" && chmod -R 777 \"\$u/.vnc\" \"\$u/.config\" 2>/dev/null || true; " +
                    "chmod 600 \"\$u/.vnc/passwd\" \"\$u/.config/tigervnc/passwd\" 2>/dev/null || true; " +
                "fi; done"
        },
        "python_dev" to { _ ->
            "export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin; " +
                "(xbps-install -Syu xbps -y 2>/dev/null || xbps-install -u xbps -y 2>/dev/null || true) && " +
                "xbps-install -y python3 python3-pip git base-devel neovim curl wget ca-certificates"
        },
        "node_dev" to { _ ->
            "export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin; " +
                "(xbps-install -Syu xbps -y 2>/dev/null || xbps-install -u xbps -y 2>/dev/null || true) && " +
                "xbps-install -y nodejs npm yarn git base-devel neovim curl wget ca-certificates"
        },
        "android_dev" to { _ ->
            "export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin; " +
                "(xbps-install -Syu xbps -y 2>/dev/null || xbps-install -u xbps -y 2>/dev/null || true) && " +
                "xbps-install -y openjdk17-jre android-tools gradle git curl wget unzip ca-certificates"
        },
        "nginx_web" to { _ ->
            "export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin; " +
                "(xbps-install -Syu xbps -y 2>/dev/null || xbps-install -u xbps -y 2>/dev/null || true) && " +
                "xbps-install -y nginx sqlite curl ca-certificates && " +
                "(sed -i 's/\\b80\\b/8080/g' /etc/nginx/nginx.conf 2>/dev/null || true) && " +
                "mkdir -p /run /var/log/nginx /var/lib/nginx && chmod -R 777 /run /var/log/nginx /var/lib/nginx 2>/dev/null || true"
        },
        "openssh_server" to { port ->
            "export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin; " +
                "(xbps-install -Syu xbps -y 2>/dev/null || xbps-install -u xbps -y 2>/dev/null || true) && " +
                "xbps-install -y openssh ca-certificates && " +
                "mkdir -p /run/sshd /var/run/sshd /var/empty && " +
                "[ -e /dev/ptmx ] || (mknod -m 666 /dev/ptmx c 5 2 2>/dev/null || ln -s /dev/pts/ptmx /dev/ptmx 2>/dev/null || true) && chmod 666 /dev/ptmx 2>/dev/null || true && " +
                "ssh-keygen -A 2>/dev/null || true && " +
                "(sed -i 's/^Subsystem.*sftp/#&/' /etc/ssh/sshd_config 2>/dev/null || true) && " +
                "(sed -i 's/^#\\?PasswordAuthentication.*/PasswordAuthentication yes/' /etc/ssh/sshd_config 2>/dev/null || true) && " +
                "(sed -i 's/^#\\?PermitRootLogin.*/PermitRootLogin yes/' /etc/ssh/sshd_config 2>/dev/null || true) && " +
                "(sed -i 's/^#\\?Port .*/Port $port/' /etc/ssh/sshd_config 2>/dev/null || echo \"Port $port\" >> /etc/ssh/sshd_config) && " +
                "chmod 600 /etc/ssh/ssh_host_*_key 2>/dev/null || true && " +
                "chmod 755 /etc/ssh /run/sshd /var/run/sshd /var/empty 2>/dev/null || true"
        },
        "code_server" to { _ ->
            "export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin; " +
                "(xbps-install -Syu xbps -y 2>/dev/null || xbps-install -u xbps -y 2>/dev/null || true) && " +
                "xbps-install -y curl ca-certificates git procps-ng && " +
                "(curl -fsSL https://code-server.dev/install.sh | sh -s -- --method=standalone --prefix=/usr/local || curl -fsSL https://code-server.dev/install.sh | sh)"
        },
        "web_terminal" to { _ ->
            "export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin; " +
                "(xbps-install -Syu xbps -y 2>/dev/null || xbps-install -u xbps -y 2>/dev/null || true) && " +
                "(xbps-install -y ttyd curl ca-certificates || xbps-install -y curl ca-certificates) && " +
                "if ! command -v ttyd >/dev/null 2>&1; then ARCH=\$(uname -m); case \"\$ARCH\" in aarch64|arm64) TTYD_BIN=\"ttyd.aarch64\" ;; x86_64|amd64) TTYD_BIN=\"ttyd.x86_64\" ;; armv7*|armhf) TTYD_BIN=\"ttyd.armhf\" ;; *) TTYD_BIN=\"ttyd.aarch64\" ;; esac; (curl -fsSL -o /usr/local/bin/ttyd \"https://github.com/tsl0922/ttyd/releases/download/1.7.7/\$TTYD_BIN\" || wget -qO /usr/local/bin/ttyd \"https://github.com/tsl0922/ttyd/releases/download/1.7.7/\$TTYD_BIN\") && chmod 755 /usr/local/bin/ttyd || true; fi"
        },
        "docker_tools" to { _ ->
            "export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin; " +
                "(xbps-install -Syu xbps -y 2>/dev/null || xbps-install -u xbps -y 2>/dev/null || true) && " +
                "(xbps-install -y docker-cli docker-compose python3 python3-pip curl ca-certificates tar || xbps-install -y python3 python3-pip curl ca-certificates tar || true) && " +
                "$UDOCKER_INSTALL_PIPELINE && $COMMON_DOCKER_WRAPPER"
        }
    ),
    softwarePackageLaunchCommands = mapOf(
        "xfce_desktop" to { _ ->
            "export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin; " +
                "([ -f /usr/lib/libstdc++.so.6 ] && grep -q CXXABI_1.3.15 /usr/lib/libstdc++.so.6 2>/dev/null || xbps-install -y libstdc++ 2>/dev/null || true); " +
                "([ -f /usr/bin/vncserver ] && grep -q 'LinuxOnAndroid' /usr/bin/vncserver 2>/dev/null || ($COMMON_VNCSERVER_WRAPPER)); " +
                "rm -f /etc/tigervnc/vncserver-config-defaults 2>/dev/null || true; " +
                "mkdir -p /tmp/.X11-unix /tmp/.ICE-unix /root/.vnc && chmod 1777 /tmp/.X11-unix /tmp/.ICE-unix 2>/dev/null || true; " +
                "[ -f /root/.vnc/passwd ] || (echo void | vncpasswd -f > /root/.vnc/passwd 2>/dev/null || echo void | tigervncpasswd -f > /root/.vnc/passwd 2>/dev/null || true); chmod 600 /root/.vnc/passwd 2>/dev/null || true; " +
                "for u in /home/*; do if [ -d \"\$u\" ]; then " +
                    "mkdir -p \"\$u/.config/tigervnc\" \"\$u/.vnc\" && chmod -R 777 \"\$u/.vnc\" \"\$u/.config\" 2>/dev/null || true; " +
                    "[ -f \"\$u/.config/tigervnc/passwd\" ] || (echo void | vncpasswd -f > \"\$u/.config/tigervnc/passwd\" 2>/dev/null || echo void | tigervncpasswd -f > \"\$u/.config/tigervnc/passwd\" 2>/dev/null || true); " +
                    "[ -f \"\$u/.vnc/passwd\" ] || cp \"\$u/.config/tigervnc/passwd\" \"\$u/.vnc/passwd\" 2>/dev/null || true; " +
                    "chmod 600 \"\$u/.vnc/passwd\" \"\$u/.config/tigervnc/passwd\" 2>/dev/null || true; " +
                "fi; done; " +
                "vncserver -kill :1 2>/dev/null || true; " +
                "rm -f /tmp/.X1-lock /tmp/.X11-unix/X1 2>/dev/null; " +
                "vncserver :1 -xstartup /etc/vnc/xstartup -geometry 1280x720 -depth 24 -SecurityTypes None,VncAuth -UseBlacklist=0 --I-KNOW-THIS-IS-INSECURE"
        },
        "openssh_server" to { port ->
            val validPort = if (port in 1..65535) port else 2222
            "(sed -i 's/^Subsystem.*sftp/#&/' /etc/ssh/sshd_config 2>/dev/null || true); mkdir -p /run/sshd /var/run/sshd /var/empty && [ -e /dev/ptmx ] || (mknod -m 666 /dev/ptmx c 5 2 2>/dev/null || ln -s /dev/pts/ptmx /dev/ptmx 2>/dev/null || true) && chmod 666 /dev/ptmx 2>/dev/null || true && ssh-keygen -A 2>/dev/null || true && chmod 755 /etc/ssh /run/sshd /var/run/sshd /var/empty 2>/dev/null || true && (killall -9 sshd 2>/dev/null || true) && (/usr/sbin/sshd -p $validPort 2>/dev/null || /usr/bin/sshd -p $validPort)"
        },
        "nginx_web" to { _ ->
            "mkdir -p /run /var/log/nginx /var/lib/nginx 2>/dev/null && chmod -R 777 /run /var/log/nginx /var/lib/nginx 2>/dev/null || true; nginx 2>/dev/null || /usr/sbin/nginx 2>/dev/null || /usr/bin/nginx 2>/dev/null"
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
