package com.devwithzachary.completelinuxinstaller.model.distros

import com.devwithzachary.completelinuxinstaller.model.DistroCatalog.COMMON_DOCKER_WRAPPER
import com.devwithzachary.completelinuxinstaller.model.DistroCatalog.COMMON_VNCSERVER_WRAPPER
import com.devwithzachary.completelinuxinstaller.model.DistroCatalog.UDOCKER_INSTALL_PIPELINE
import com.devwithzachary.completelinuxinstaller.model.DistroDefinition
import com.devwithzachary.completelinuxinstaller.model.PackageManagerType
import com.devwithzachary.completelinuxinstaller.model.SystemArchitecture

val FedoraDistro = DistroDefinition(
    id = "fedora_44",
    name = "Fedora 44",
    version = "44",
    tag = "Leading-Edge & RPM",
    description = "Modern, innovative Linux distribution featuring the DNF package manager and RPM ecosystem.",
    packageManager = PackageManagerType.DNF,
    defaultShell = "/bin/bash",
    candidateShells = listOf("/bin/bash", "/usr/bin/bash", "/bin/sh"),
    downloadSizeMb = 142,
    installedSizeMb = 480,
    colorHex = 0xFF51A2DA,
    downloadUrls = mapOf(
        SystemArchitecture.ARM64 to "https://download.fedoraproject.org/pub/fedora/linux/releases/44/Container/aarch64/images/Fedora-WSL-Base-44-1.7.aarch64.wsl",
        SystemArchitecture.X86_64 to "https://download.fedoraproject.org/pub/fedora/linux/releases/44/Container/x86_64/images/Fedora-WSL-Base-44-1.7.x86_64.wsl",
        SystemArchitecture.ARMV7 to "https://download.fedoraproject.org/pub/fedora/linux/releases/44/Container/aarch64/images/Fedora-WSL-Base-44-1.7.aarch64.wsl"
    ),
    checksumUrls = mapOf(
        SystemArchitecture.ARM64 to "https://download.fedoraproject.org/pub/fedora/linux/releases/44/Container/aarch64/images/Fedora-Container-44-1.7-aarch64-CHECKSUM",
        SystemArchitecture.X86_64 to "https://download.fedoraproject.org/pub/fedora/linux/releases/44/Container/x86_64/images/Fedora-Container-44-1.7-x86_64-CHECKSUM",
        SystemArchitecture.ARMV7 to "https://download.fedoraproject.org/pub/fedora/linux/releases/44/Container/aarch64/images/Fedora-Container-44-1.7-aarch64-CHECKSUM"
    ),
    sha256Checksums = mapOf(
        SystemArchitecture.ARM64 to "1a3220262dc918b07d08410278205fb35e92c28a53455382028eea1a980476ee",
        SystemArchitecture.X86_64 to "2e5b153ba4b639952bf546be577fc19b832fe8944caa7de342b32f10da7d319a",
        SystemArchitecture.ARMV7 to "1a3220262dc918b07d08410278205fb35e92c28a53455382028eea1a980476ee"
    ),
    firstLaunchScriptBuilder = { rootPassword, username, userPassword, _ ->
        "export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin; " +
                "rm -f /usr/lib64/rpm-plugins/selinux.so /usr/lib/rpm-plugins/selinux.so 2>/dev/null || true; " +
                "mkdir -p /etc/sudoers.d /etc/pam.d /etc/selinux /home/$username /run /var/run /var/log 2>/dev/null; " +
                "(grep -q ^$username: /etc/passwd || echo \"$username:x:1000:1000:$username:/home/$username:/bin/bash\" >> /etc/passwd); " +
                "(grep -q ^$username: /etc/group || echo \"$username:x:1000:\" >> /etc/group); " +
                "(grep -q ^wheel: /etc/group && sed -i 's/^wheel:.*/&,$username/' /etc/group || echo \"wheel:x:10:root,$username\" >> /etc/group); " +
                "(grep -q ^$username: /etc/shadow || echo \"$username:*:19700:0:99999:7:::\" >> /etc/shadow); " +
                "chown -R 1000:1000 /home/$username 2>/dev/null || true; " +
                "chmod 644 /etc/shadow /etc/shadow- /etc/passwd /etc/group 2>/dev/null || true; " +
                "echo \"$username ALL=(ALL:ALL) NOPASSWD:ALL\" > /etc/sudoers.d/$username && chmod 0440 /etc/sudoers.d/$username; " +
                "printf 'auth sufficient pam_permit.so\\naccount sufficient pam_permit.so\\nsession sufficient pam_permit.so\\npassword sufficient pam_permit.so\\n' > /etc/pam.d/su 2>/dev/null || true; " +
                "cp /etc/pam.d/su /etc/pam.d/su-l 2>/dev/null || true; " +
                "([ -f /etc/selinux/config ] && sed -i 's/^SELINUX=.*/SELINUX=disabled/' /etc/selinux/config 2>/dev/null || printf 'SELINUX=disabled\\nSELINUXTYPE=targeted\\n' > /etc/selinux/config 2>/dev/null || true); " +
                "echo \"root:$rootPassword\" | chpasswd 2>/dev/null; passwd -u root 2>/dev/null || true; " +
                "echo \"$username:$userPassword\" | chpasswd 2>/dev/null; passwd -u $username 2>/dev/null || true; " +
                "chown -R 0:0 /etc/sudoers /etc/sudoers.d /usr/bin/sudo /usr/lib/sudo 2>/dev/null || true; chmod 4755 /usr/bin/sudo 2>/dev/null || true"
    },
    softwarePackageCommands = mapOf(
        "xfce_desktop" to { _ ->
            "export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin; " +
                    "([ -f /etc/selinux/config ] && sed -i 's/^SELINUX=.*/SELINUX=disabled/' /etc/selinux/config 2>/dev/null || true); " +
                    "rm -f /usr/lib64/rpm-plugins/selinux.so /usr/lib/rpm-plugins/selinux.so 2>/dev/null || true; " +
                    "dnf install -y xfce4-session xfwm4 xfce4-panel xfdesktop xfce4-terminal thunar xfce4-settings xfce4-appfinder adwaita-icon-theme rsvg-pixbuf-loader dejavu-sans-fonts dbus-x11 tigervnc-server curl ca-certificates perl python3 && " +
                    "(dnf install -y dnfdragora 2>/dev/null || true) && " +
                    "$COMMON_VNCSERVER_WRAPPER && " +
                    "rm -f /etc/tigervnc/vncserver-config-defaults 2>/dev/null || true; " +
                    "mkdir -p /root/.vnc /etc/skel/.vnc /etc/vnc /tmp/.X11-unix /tmp/.ICE-unix && " +
                    "chmod 1777 /tmp/.X11-unix /tmp/.ICE-unix 2>/dev/null || true; " +
                    "(echo fedora | vncpasswd -f > /root/.vnc/passwd 2>/dev/null || echo fedora | tigervncpasswd -f > /root/.vnc/passwd 2>/dev/null || true) && " +
                    "chmod 600 /root/.vnc/passwd 2>/dev/null || true; " +
                    "(echo fedora | vncpasswd -f > /etc/skel/.vnc/passwd 2>/dev/null || echo fedora | tigervncpasswd -f > /etc/skel/.vnc/passwd 2>/dev/null || true) && " +
                    "chmod 600 /etc/skel/.vnc/passwd 2>/dev/null || true; " +
                    "cat << 'EOF' > /usr/bin/bwrap\n" +
                    "#!/bin/sh\n" +
                    "if [ \"\$1\" = \"--version\" ] || [ \"\$1\" = \"-v\" ]; then\n" +
                    "    echo \"bubblewrap 0.11.0 (PRoot shim)\"\n" +
                    "    exit 0\n" +
                    "fi\n" +
                    "while [ \$# -gt 0 ]; do\n" +
                    "    case \"\$1\" in\n" +
                    "        --)\n" +
                    "            shift\n" +
                    "            [ -n \"\$1\" ] && exec \"\$@\"\n" +
                    "            exit 0\n" +
                    "            ;;\n" +
                    "        /*|./*)\n" +
                    "            if [ -x \"\$1\" ] && [ ! -d \"\$1\" ]; then\n" +
                    "                exec \"\$@\"\n" +
                    "            fi\n" +
                    "            ;;\n" +
                    "    esac\n" +
                    "    shift\n" +
                    "done\n" +
                    "exit 0\n" +
                    "EOF\n" +
                    "chmod 755 /usr/bin/bwrap 2>/dev/null || true; cp /usr/bin/bwrap /usr/local/bin/bwrap 2>/dev/null || true; chmod 755 /usr/local/bin/bwrap 2>/dev/null || true; " +
                    "cat << 'EOF' > /etc/vnc/xstartup\n" +
                    "#!/bin/sh\n" +
                    "export PATH=\"/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/usr/lib/xfce4/xfconf:/usr/lib64/xfce4/xfconf:/usr/libexec/xfce4/xfconf:\$PATH\"\n" +
                    "unset SESSION_MANAGER\n" +
                    "unset DBUS_SESSION_BUS_ADDRESS\n" +
                    "export XDG_SESSION_TYPE=x11\n" +
                    "export XDG_CURRENT_DESKTOP=XFCE\n" +
                    "export DESKTOP_SESSION=xfce\n" +
                    "export NO_AT_BRIDGE=1\n" +
                    "export GDK_BACKEND=x11\n" +
                    "export GTK_OVERLAY_SCROLLING=0\n" +
                    "export GLYCIN_DISABLE_SANDBOX=1\n" +
                    "export GLYCIN_ENABLE_SANDBOX=0\n" +
                    "export LIBGL_ALWAYS_SOFTWARE=1\n" +
                    "[ -r \$HOME/.Xresources ] && xrdb \$HOME/.Xresources 2>/dev/null || true\n" +
                    "if command -v dbus-launch >/dev/null 2>&1; then\n" +
                    "    eval \$(dbus-launch --sh-syntax)\n" +
                    "fi\n" +
                    "if command -v xfconfd >/dev/null 2>&1; then\n" +
                    "    xfconfd &\n" +
                    "elif [ -x /usr/lib64/xfce4/xfconf/xfconfd ]; then\n" +
                    "    /usr/lib64/xfce4/xfconf/xfconfd &\n" +
                    "elif [ -x /usr/lib/xfce4/xfconf/xfconfd ]; then\n" +
                    "    /usr/lib/xfce4/xfconf/xfconfd &\n" +
                    "elif [ -x /usr/libexec/xfce4/xfconf/xfconfd ]; then\n" +
                    "    /usr/libexec/xfce4/xfconf/xfconfd &\n" +
                    "fi\n" +
                    "xsetroot -solid \"#1e293b\" 2>/dev/null || true\n" +
                    "xfconf-query -c xfwm4 -p /general/use_compositing -n -t bool -s false 2>/dev/null || true\n" +
                    "if command -v startxfce4 >/dev/null 2>&1; then\n" +
                    "    exec startxfce4\n" +
                    "elif command -v xfce4-session >/dev/null 2>&1; then\n" +
                    "    exec xfce4-session\n" +
                    "else\n" +
                    "    xfsettingsd --daemon 2>/dev/null || true\n" +
                    "    xfwm4 --compositor=off &\n" +
                    "    xfce4-panel &\n" +
                    "    Thunar --daemon 2>/dev/null &\n" +
                    "    if command -v xfdesktop >/dev/null 2>&1; then\n" +
                    "        exec xfdesktop\n" +
                    "    else\n" +
                    "        exec xterm\n" +
                    "    fi\n" +
                    "fi\n" +
                    "EOF\n" +
                    "chmod 755 /etc/vnc/xstartup && cp /etc/vnc/xstartup /root/.vnc/xstartup && cp /etc/vnc/xstartup /etc/skel/.vnc/xstartup && chmod 755 /root/.vnc/xstartup /etc/skel/.vnc/xstartup; " +
                    "printf 'securitytypes=VncAuth\\ngeometry=1280x720\\nlocalhost=yes\\nalwaysshared=1\\n' > /etc/vnc/config && chmod 644 /etc/vnc/config && cp /etc/vnc/config /root/.vnc/config && cp /etc/vnc/config /etc/skel/.vnc/config && " +
                    "for u in /home/*; do if [ -d \"\$u\" ]; then " +
                        "mkdir -p \"\$u/.vnc\" \"\$u/.config/tigervnc\" && " +
                        "cp /etc/vnc/xstartup \"\$u/.vnc/xstartup\" && " +
                        "cp /etc/vnc/config \"\$u/.vnc/config\" && " +
                        "(echo fedora | vncpasswd -f > \"\$u/.vnc/passwd\" 2>/dev/null || echo fedora | tigervncpasswd -f > \"\$u/.vnc/passwd\" 2>/dev/null || true) && " +
                        "(echo fedora | vncpasswd -f > \"\$u/.config/tigervnc/passwd\" 2>/dev/null || echo fedora | tigervncpasswd -f > \"\$u/.config/tigervnc/passwd\" 2>/dev/null || true) && " +
                        "chmod 755 \"\$u/.vnc/xstartup\" && chmod -R 777 \"\$u/.vnc\" \"\$u/.config\" 2>/dev/null || true; " +
                        "chmod 600 \"\$u/.vnc/passwd\" \"\$u/.config/tigervnc/passwd\" 2>/dev/null || true; " +
                    "fi; done"
        },
        "python_dev" to { _ ->
            "export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin; " +
                    "([ -f /etc/selinux/config ] && sed -i 's/^SELINUX=.*/SELINUX=disabled/' /etc/selinux/config 2>/dev/null || true) && " +
                    "dnf install -y python3 python3-pip python3-devel git gcc gcc-c++ make neovim curl wget ca-certificates"
        },
        "node_dev" to { _ ->
            "export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin; " +
                    "([ -f /etc/selinux/config ] && sed -i 's/^SELINUX=.*/SELINUX=disabled/' /etc/selinux/config 2>/dev/null || true) && " +
                    "dnf install -y nodejs npm git gcc gcc-c++ make neovim curl wget ca-certificates"
        },
        "android_dev" to { _ ->
            "export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin; " +
                    "([ -f /etc/selinux/config ] && sed -i 's/^SELINUX=.*/SELINUX=disabled/' /etc/selinux/config 2>/dev/null || true) && " +
                    "dnf install -y java-17-openjdk-headless git curl wget unzip ca-certificates"
        },
        "nginx_web" to { _ ->
            "export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin; " +
                    "([ -f /etc/selinux/config ] && sed -i 's/^SELINUX=.*/SELINUX=disabled/' /etc/selinux/config 2>/dev/null || true) && " +
                    "dnf install -y nginx sqlite curl ca-certificates && " +
                    "(sed -i 's/\\b80\\b/8080/g' /etc/nginx/nginx.conf /etc/nginx/conf.d/*.conf 2>/dev/null || true) && " +
                    "(sed -i 's/^\\s*user\\s\\+nginx/#user nginx/' /etc/nginx/nginx.conf 2>/dev/null || true) && " +
                    "mkdir -p /run /var/log/nginx /var/lib/nginx && chmod -R 777 /run /var/log/nginx /var/lib/nginx 2>/dev/null || true"
        },
        "openssh_server" to { port ->
            "export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin; " +
                    "([ -f /etc/selinux/config ] && sed -i 's/^SELINUX=.*/SELINUX=disabled/' /etc/selinux/config 2>/dev/null || true) && " +
                    "dnf install -y openssh-server ca-certificates && " +
                    "mkdir -p /run/sshd /var/run/sshd /var/empty /etc/ssh/sshd_config.d && " +
                    "[ -e /dev/ptmx ] || (mknod -m 666 /dev/ptmx c 5 2 2>/dev/null || ln -s /dev/pts/ptmx /dev/ptmx 2>/dev/null || true) && chmod 666 /dev/ptmx 2>/dev/null || true && " +
                    "ssh-keygen -A 2>/dev/null || true && " +
                    "echo \"Port $port\" > /etc/ssh/sshd_config.d/00-linuxonandroid.conf && " +
                    "echo \"PermitRootLogin yes\" >> /etc/ssh/sshd_config.d/00-linuxonandroid.conf && " +
                    "echo \"PasswordAuthentication yes\" >> /etc/ssh/sshd_config.d/00-linuxonandroid.conf && " +
                    "echo \"KbdInteractiveAuthentication yes\" >> /etc/ssh/sshd_config.d/00-linuxonandroid.conf && " +
                    "echo \"UsePAM no\" >> /etc/ssh/sshd_config.d/00-linuxonandroid.conf && " +
                    "echo \"StrictModes no\" >> /etc/ssh/sshd_config.d/00-linuxonandroid.conf && " +
                    "echo \"SetEnv PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin\" >> /etc/ssh/sshd_config.d/00-linuxonandroid.conf && " +
                    "echo \"Subsystem sftp internal-sftp\" >> /etc/ssh/sshd_config.d/00-linuxonandroid.conf && " +
                    "(sed -i 's/^Subsystem.*sftp/#&/' /etc/ssh/sshd_config 2>/dev/null || true) && " +
                    "(sed -i 's/^#\\?UsePAM.*/UsePAM no/' /etc/ssh/sshd_config 2>/dev/null || true) && " +
                    "(sed -i 's/^#\\?PasswordAuthentication.*/PasswordAuthentication yes/' /etc/ssh/sshd_config 2>/dev/null || true) && " +
                    "(sed -i 's/^#\\?PermitRootLogin.*/PermitRootLogin yes/' /etc/ssh/sshd_config 2>/dev/null || true) && " +
                    "(sed -i 's/^#\\?Port .*/Port $port/' /etc/ssh/sshd_config 2>/dev/null || echo \"Port $port\" >> /etc/ssh/sshd_config) && " +
                    "chmod 600 /etc/ssh/ssh_host_*_key 2>/dev/null || true && " +
                    "chmod 755 /etc/ssh /run/sshd /var/run/sshd /var/empty 2>/dev/null || true"
        },
        "code_server" to { _ ->
            "export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin; " +
                    "([ -f /etc/selinux/config ] && sed -i 's/^SELINUX=.*/SELINUX=disabled/' /etc/selinux/config 2>/dev/null || true) && " +
                    "dnf install -y curl ca-certificates git procps-ng && " +
                    "(curl -fsSL https://code-server.dev/install.sh | sh || curl -fsSL https://code-server.dev/install.sh | sh -s -- --method=standalone --prefix=/usr/local)"
        },
        "web_terminal" to { _ ->
            "export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin; " +
                    "([ -f /etc/selinux/config ] && sed -i 's/^SELINUX=.*/SELINUX=disabled/' /etc/selinux/config 2>/dev/null || true) && " +
                    "(dnf install -y --setopt=keepcache=0 ttyd curl ca-certificates procps-ng || dnf install -y --setopt=keepcache=0 curl ca-certificates procps-ng) && " +
                    "if ! command -v ttyd >/dev/null 2>&1; then ${com.devwithzachary.completelinuxinstaller.model.SoftwarePackage.TTYD_FALLBACK_DOWNLOAD_CMD}; fi; dnf clean all"
        },
        "docker_tools" to { _ ->
            "export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin; " +
                    "([ -f /etc/selinux/config ] && sed -i 's/^SELINUX=.*/SELINUX=disabled/' /etc/selinux/config 2>/dev/null || true) && " +
                    "(dnf install -y --setopt=keepcache=0 docker-cli docker-compose python3 python3-pip curl ca-certificates tar || dnf install -y --setopt=keepcache=0 python3 python3-pip curl ca-certificates tar || true) && " +
                    "$UDOCKER_INSTALL_PIPELINE && $COMMON_DOCKER_WRAPPER && dnf clean all"
        }
    ),
    softwarePackageLaunchCommands = mapOf(
        "xfce_desktop" to { _ ->
            "export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin; " +
                    "([ -f /usr/bin/vncserver ] && grep -q 'LinuxOnAndroid' /usr/bin/vncserver 2>/dev/null || ($COMMON_VNCSERVER_WRAPPER)); " +
                    "rm -f /etc/tigervnc/vncserver-config-defaults 2>/dev/null || true; " +
                    "mkdir -p /tmp/.X11-unix /tmp/.ICE-unix /root/.vnc && chmod 1777 /tmp/.X11-unix /tmp/.ICE-unix 2>/dev/null || true; " +
                    "[ -f /root/.vnc/passwd ] || (echo fedora | vncpasswd -f > /root/.vnc/passwd 2>/dev/null || echo fedora | tigervncpasswd -f > /root/.vnc/passwd 2>/dev/null || true); chmod 600 /root/.vnc/passwd 2>/dev/null || true; " +
                    "for u in /home/*; do if [ -d \"\$u\" ]; then " +
                        "mkdir -p \"\$u/.config/tigervnc\" \"\$u/.vnc\" && chmod -R 777 \"\$u/.vnc\" \"\$u/.config\" 2>/dev/null || true; " +
                        "[ -f \"\$u/.config/tigervnc/passwd\" ] || (echo fedora | vncpasswd -f > \"\$u/.config/tigervnc/passwd\" 2>/dev/null || echo fedora | tigervncpasswd -f > \"\$u/.config/tigervnc/passwd\" 2>/dev/null || true); " +
                        "[ -f \"\$u/.vnc/passwd\" ] || cp \"\$u/.config/tigervnc/passwd\" \"\$u/.vnc/passwd\" 2>/dev/null || true; " +
                        "chmod 600 \"\$u/.vnc/passwd\" \"\$u/.config/tigervnc/passwd\" 2>/dev/null || true; " +
                    "fi; done; " +
                    "vncserver -kill :1 2>/dev/null || true; " +
                    "rm -f /tmp/.X1-lock /tmp/.X11-unix/X1 2>/dev/null; " +
                    "vncserver :1 -xstartup /etc/vnc/xstartup -geometry 1280x720 -depth 24 -SecurityTypes VncAuth -UseBlacklist=0 -localhost"
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
        "xfce_desktop" to 7
    )
)
