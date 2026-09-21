package com.devwithzachary.completelinuxinstaller.model.distros

import com.devwithzachary.completelinuxinstaller.model.DistroCatalog.COMMON_DOCKER_WRAPPER
import com.devwithzachary.completelinuxinstaller.model.DistroCatalog.UDOCKER_INSTALL_PIPELINE
import com.devwithzachary.completelinuxinstaller.model.DistroDefinition
import com.devwithzachary.completelinuxinstaller.model.PackageManagerType
import com.devwithzachary.completelinuxinstaller.model.SystemArchitecture

val AlpineDistro = DistroDefinition(
    id = "alpine_3_21",
    name = "Alpine Linux 3.21",
    version = "3.21",
    tag = "Minimalist",
    description = "Ultra-lightweight musl and BusyBox environment. Boots instantly with minimal memory footprint and fast APK package manager.",
    packageManager = PackageManagerType.APK,
    defaultShell = "/bin/sh",
    candidateShells = listOf("/bin/sh", "/bin/ash", "/bin/bash", "/usr/bin/bash"),
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
                "([ -L /usr/bin/bash ] && [ \"\$(readlink /usr/bin/bash 2>/dev/null)\" = \"/bin/sh\" ] && rm -f /usr/bin/bash 2>/dev/null || true); " +
                "([ -L /bin/bash ] && [ \"\$(readlink /bin/bash 2>/dev/null)\" = \"/bin/sh\" ] && rm -f /bin/bash 2>/dev/null || true); " +
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
                "[ -x /bin/bash ] && ln -sf /bin/bash /usr/bin/bash 2>/dev/null || true; " +
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
        },
        "code_server" to { _ ->
            "apk update && apk add --no-cache curl ca-certificates git nodejs npm gcompat && (npm install -g code-server --unsafe-perm || curl -fsSL https://code-server.dev/install.sh | sh -s -- --method=standalone --prefix=/usr/local)"
        },
        "web_terminal" to { _ ->
            "apk update && (apk add --no-cache ttyd curl ca-certificates || true) && if ! command -v ttyd >/dev/null 2>&1; then ARCH=\$(uname -m); case \"\$ARCH\" in aarch64|arm64) TTYD_BIN=\"ttyd.aarch64\" ;; x86_64|amd64) TTYD_BIN=\"ttyd.x86_64\" ;; armv7*|armhf) TTYD_BIN=\"ttyd.armhf\" ;; *) TTYD_BIN=\"ttyd.aarch64\" ;; esac; (curl -fsSL -o /usr/local/bin/ttyd \"https://github.com/tsl0922/ttyd/releases/download/1.7.7/\$TTYD_BIN\" || wget -qO /usr/local/bin/ttyd \"https://github.com/tsl0922/ttyd/releases/download/1.7.7/\$TTYD_BIN\") && chmod 755 /usr/local/bin/ttyd || true; fi"
        },
        "docker_tools" to { _ ->
            "apk update && (apk add --no-cache docker-cli docker-cli-compose python3 py3-pip curl ca-certificates tar || apk add --no-cache python3 py3-pip curl ca-certificates tar || true) && " +
                "$UDOCKER_INSTALL_PIPELINE && $COMMON_DOCKER_WRAPPER"
        }
    )
)
