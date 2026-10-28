package com.devwithzachary.completelinuxinstaller.engine.desktop

object DesktopConfigHelper {

    fun buildStandardXstartupScript(): String {
        return """
            #!/bin/sh
            export PATH="/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/usr/lib/xfce4/xfconf:/usr/lib64/xfce4/xfconf:/usr/libexec/xfce4/xfconf:${'$'}PATH"
            unset SESSION_MANAGER
            unset DBUS_SESSION_BUS_ADDRESS
            export XDG_SESSION_TYPE=x11
            export XDG_CURRENT_DESKTOP=XFCE
            export DESKTOP_SESSION=xfce
            export NO_AT_BRIDGE=1
            export GDK_BACKEND=x11
            export GTK_OVERLAY_SCROLLING=0
            export GLYCIN_DISABLE_SANDBOX=1
            export GLYCIN_ENABLE_SANDBOX=0
            export LIBGL_ALWAYS_SOFTWARE=1
            [ -r "${'$'}HOME/.Xresources" ] && xrdb "${'$'}HOME/.Xresources" 2>/dev/null || true

            if command -v dbus-launch >/dev/null 2>&1; then
                eval ${'$'}(dbus-launch --sh-syntax)
            fi

            if command -v xfconfd >/dev/null 2>&1; then
                xfconfd &
            elif [ -x /usr/lib64/xfce4/xfconf/xfconfd ]; then
                /usr/lib64/xfce4/xfconf/xfconfd &
            elif [ -x /usr/lib/xfce4/xfconf/xfconfd ]; then
                /usr/lib/xfce4/xfconf/xfconfd &
            elif [ -x /usr/libexec/xfce4/xfconf/xfconfd ]; then
                /usr/libexec/xfce4/xfconf/xfconfd &
            fi

            xsetroot -solid "#1e293b" 2>/dev/null || true
            xfconf-query -c xfwm4 -p /general/use_compositing -n -t bool -s false 2>/dev/null || true

            if command -v startxfce4 >/dev/null 2>&1; then
                exec startxfce4
            elif command -v xfce4-session >/dev/null 2>&1; then
                exec xfce4-session
            else
                xfsettingsd --daemon 2>/dev/null || true
                xfwm4 --compositor=off &
                xfce4-panel &
                Thunar --daemon 2>/dev/null &
                if command -v xfdesktop >/dev/null 2>&1; then
                    exec xfdesktop
                else
                    exec xterm
                fi
            fi
        """.trimIndent() + "\n"
    }

    fun buildVncServerScript(): String {
        val d = "$"
        return """
            #!/bin/sh
            # LinuxOnAndroid TigerVNC server wrapper for PRoot environments
            export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:${d}PATH

            ACTION="start"
            DISP=":1"
            GEOM="1280x720"
            DEPTH="24"
            XSTARTUP="/etc/vnc/xstartup"
            SECTYPES="VncAuth"
            LOCALHOST="yes"

            while [ ${d}# -gt 0 ]; do
                case "${d}1" in
                    -list)
                        ACTION="list"
                        shift
                        ;;
                    -kill)
                        ACTION="kill"
                        shift
                        if [ -n "${d}1" ] && [ "${d}1" != "-*" ]; then
                            DISP="${d}1"
                            shift
                        fi
                        ;;
                    -geometry)
                        shift
                        GEOM="${d}1"
                        shift
                        ;;
                    -depth)
                        shift
                        DEPTH="${d}1"
                        shift
                        ;;
                    -xstartup)
                        shift
                        XSTARTUP="${d}1"
                        shift
                        ;;
                    -SecurityTypes)
                        shift
                        SECTYPES="${d}1"
                        shift
                        ;;
                    -localhost)
                        LOCALHOST="yes"
                        shift
                        ;;
                    -nolocalhost)
                        LOCALHOST="no"
                        shift
                        ;;
                    -help|--help|-h)
                        ACTION="help"
                        shift
                        ;;
                    :*)
                        DISP="${d}1"
                        shift
                        ;;
                    *)
                        shift
                        ;;
                esac
            done

            DISP_NUM=${d}(echo "${d}DISP" | tr -d ":")
            [ -z "${d}DISP_NUM" ] && DISP_NUM="1"

            if [ "${d}ACTION" = "help" ]; then
                echo "TigerVNC server wrapper for LinuxOnAndroid (PRoot)"
                echo ""
                echo "Usage:"
                echo "  vncserver [:<display>] [-geometry <width>x<height>] [-depth <depth>] [-SecurityTypes <types>]"
                echo "  vncserver -list"
                echo "  vncserver -kill :<display>"
                echo "  vncserver -help"
                echo ""
                echo "Examples:"
                echo "  vncserver :1"
                echo "  vncserver -geometry 1920x1080"
                echo "  vncserver -list"
                echo "  vncserver -kill :1"
                exit 0
            fi

            if [ "${d}ACTION" = "list" ]; then
                echo "TigerVNC server sessions:"
                echo ""
                printf "%-14s %-11s %s\n" "X DISPLAY #" "RFB PORT" "PROCESS ID"
                FOUND=0

                # 1. Inspect active X11 lock files
                FOUND_DISPS=""
                for lock in /tmp/.X*-lock; do
                    [ -e "${d}lock" ] || continue
                    D_NUM=${d}(basename "${d}lock" | sed 's/\.X//;s/-lock//')
                    PID=${d}(cat "${d}lock" 2>/dev/null | tr -d ' ' | tr -d '\n')
                    if [ -n "${d}PID" ] && ( [ -d "/proc/${d}PID" ] || kill -0 "${d}PID" 2>/dev/null ); then
                        PORT=${d}((5900 + D_NUM))
                        FOUND_DISPS="${d}{FOUND_DISPS}:${d}D_NUM:"
                        printf "%-14s %-11s %s\n" ":${d}D_NUM" "${d}PORT" "${d}PID"
                        FOUND=1
                    fi
                done

                # 2. Inspect running processes for any Xvnc not tracked by locks
                PROCS=${d}( (ps -ef 2>/dev/null || ps aux 2>/dev/null) | grep -v "grep" | grep -v "libproot" | grep -v "printf" | grep "Xvnc" )
                if [ -n "${d}PROCS" ]; then
                    echo "${d}PROCS" | while read -r line; do
                        PID=${d}(echo "${d}line" | awk '{print ${d}2}')
                        D=${d}(echo "${d}line" | grep -E -o ':[0-9]+' | head -n1)
                        D_NUM=${d}(echo "${d}D" | tr -d ':')
                        PORT=""
                        RFB_ARG=${d}(echo "${d}line" | grep -E -o '\-rfbport [0-9]+' | awk '{print ${d}2}')
                        if [ -n "${d}RFB_ARG" ]; then
                            PORT="${d}RFB_ARG"
                        elif [ -n "${d}D_NUM" ]; then
                            PORT=${d}((5900 + D_NUM))
                        fi
                        case "${d}FOUND_DISPS" in
                            *":${d}D_NUM:"*) ;;
                            *)
                                if [ -n "${d}D" ] && [ -n "${d}PORT" ] && [ -n "${d}PID" ]; then
                                    printf "%-14s %-11s %s\n" "${d}D" "${d}PORT" "${d}PID"
                                fi
                                ;;
                        esac
                    done
                fi
                if [ "${d}FOUND" -eq 0 ] && [ -z "${d}PROCS" ]; then
                    echo "No active VNC server sessions found."
                fi
                exit 0
            fi

            if [ "${d}ACTION" = "kill" ]; then
                LOCK="/tmp/.X${d}{DISP_NUM}-lock"
                if [ -e "${d}LOCK" ]; then
                    LPID=${d}(cat "${d}LOCK" 2>/dev/null | tr -d ' ' | tr -d '\n')
                    [ -n "${d}LPID" ] && kill "${d}LPID" 2>/dev/null || true
                fi
                pkill -f "Xvnc :${d}DISP_NUM" 2>/dev/null || killall -9 Xvnc 2>/dev/null || true
                rm -f "/tmp/.X${d}{DISP_NUM}-lock" "/tmp/.X11-unix/X${d}{DISP_NUM}" 2>/dev/null || true
                echo "Killed VNC server on display :${d}DISP_NUM"
                exit 0
            fi

            # ACTION = start
            mkdir -p /tmp/.X11-unix /tmp/.ICE-unix "${d}HOME/.vnc" "${d}HOME/.config/tigervnc" /root/.vnc 2>/dev/null
            chmod 1777 /tmp/.X11-unix /tmp/.ICE-unix 2>/dev/null || true
            pkill -f "Xvnc :${d}DISP_NUM" 2>/dev/null || true
            rm -f "/tmp/.X${d}{DISP_NUM}-lock" "/tmp/.X11-unix/X${d}{DISP_NUM}" 2>/dev/null || true

            PW=""
            if [ -f "${d}HOME/.vnc/passwd" ]; then
                PW="-rfbauth ${d}HOME/.vnc/passwd"
            elif [ -f "${d}HOME/.config/tigervnc/passwd" ]; then
                PW="-rfbauth ${d}HOME/.config/tigervnc/passwd"
            elif [ -f "/root/.vnc/passwd" ]; then
                PW="-rfbauth /root/.vnc/passwd"
            fi

            RFB_PORT=${d}((5900 + DISP_NUM))
            LH_ARG=""
            [ "${d}LOCALHOST" = "yes" ] && LH_ARG="-localhost"
            echo "Starting Xvnc on display :${d}DISP_NUM (${d}GEOM, depth ${d}DEPTH)..."
            Xvnc ":${d}DISP_NUM" -geometry "${d}GEOM" -depth "${d}DEPTH" ${d}PW -SecurityTypes "${d}SECTYPES" -UseBlacklist=0 ${d}LH_ARG >/dev/null 2>&1 &
            sleep 1

            if [ -x "${d}XSTARTUP" ]; then
                DISPLAY=":${d}DISP_NUM" "${d}XSTARTUP" >/dev/null 2>&1 &
            elif [ -x "${d}HOME/.vnc/xstartup" ]; then
                DISPLAY=":${d}DISP_NUM" "${d}HOME/.vnc/xstartup" >/dev/null 2>&1 &
            elif command -v startxfce4 >/dev/null 2>&1; then
                DISPLAY=":${d}DISP_NUM" startxfce4 >/dev/null 2>&1 &
            fi

            LAN_IP=""
            if [ -f "/etc/network/proot_interfaces.conf" ]; then
                LAN_IP=${d}(grep "^PRIMARY_IP=" /etc/network/proot_interfaces.conf 2>/dev/null | cut -d= -f2)
            fi
            if [ -z "${d}LAN_IP" ] && [ -f "/etc/hosts" ]; then
                LAN_IP=${d}(grep -v "^#" /etc/hosts 2>/dev/null | grep -v "^127\." | grep -v "^::1" | awk '{print ${d}1}' | head -n1)
            fi

            echo "VNC Server started on port ${d}RFB_PORT (:${d}DISP_NUM)."
            echo "-> Local connection:  127.0.0.1:${d}RFB_PORT"
            if [ "${d}LOCALHOST" != "yes" ] && [ -n "${d}LAN_IP" ] && [ "${d}LAN_IP" != "127.0.0.1" ]; then
                echo "-> Wi-Fi connection:  ${d}LAN_IP:${d}RFB_PORT"
            fi
            echo "-> List sessions:     vncserver -list"
            echo "-> Stop server:       vncserver -kill :${d}DISP_NUM"
        """.trimIndent() + "\n"
    }

    const val COMMON_VNCSERVER_WRAPPER =
        "rm -f /usr/bin/vncserver /usr/local/bin/vncserver /usr/local/sbin/vncserver 2>/dev/null || true; " +
            "printf '#!/bin/sh\\n" +
            "# LinuxOnAndroid TigerVNC server wrapper for PRoot environments\\n" +
            "export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:\$PATH\\n" +
            "ACTION=\"start\"; DISP=\":1\"; GEOM=\"1280x720\"; DEPTH=\"24\"; XSTARTUP=\"/etc/vnc/xstartup\"; SECTYPES=\"VncAuth\"; LOCALHOST=\"yes\"\\n" +
            "while [ \$# -gt 0 ]; do\\n" +
            "  case \"\$1\" in\\n" +
            "    -list) ACTION=\"list\"; shift ;;\\n" +
            "    -kill) ACTION=\"kill\"; shift; [ -n \"\$1\" ] && [ \"\$1\" != \"-*\" ] && DISP=\"\$1\" && shift ;;\\n" +
            "    -geometry) shift; GEOM=\"\$1\"; shift ;;\\n" +
            "    -depth) shift; DEPTH=\"\$1\"; shift ;;\\n" +
            "    -xstartup) shift; XSTARTUP=\"\$1\"; shift ;;\\n" +
            "    -SecurityTypes) shift; SECTYPES=\"\$1\"; shift ;;\\n" +
            "    -localhost) LOCALHOST=\"yes\"; shift ;;\\n" +
            "    -nolocalhost) LOCALHOST=\"no\"; shift ;;\\n" +
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
            "  echo \"  vncserver [:<display>] [-geometry <width>x<height>] [-depth <depth>] [-SecurityTypes <types>] [-localhost|-nolocalhost]\"\\n" +
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
            "    D_NUM=\$(basename \"\$lock\" | sed \"s/\\.X//;s/-lock//\")\\n" +
            "    PID=\$(cat \"\$lock\" 2>/dev/null | tr -d \" \" | tr -d \"\\n\")\\n" +
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
            "      PID=\$(echo \"\$line\" | awk \"{print \\\$2}\")\\n" +
            "      D=\$(echo \"\$line\" | grep -E -o \":[0-9]+\" | head -n1)\\n" +
            "      D_NUM=\$(echo \"\$D\" | tr -d \":\")\\n" +
            "      PORT=\"\"\\n" +
            "      RFB_ARG=\$(echo \"\$line\" | grep -E -o \"\\\\-rfbport [0-9]+\" | awk \"{print \\\$2}\")\\n" +
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
            "    LPID=\$(cat \"\$LOCK\" 2>/dev/null | tr -d \" \" | tr -d \"\\n\")\\n" +
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
            "LH_ARG=\"\"; [ \"\$LOCALHOST\" = \"yes\" ] && LH_ARG=\"-localhost\"\\n" +
            "echo \"Starting Xvnc on display :\$DISP_NUM (\$GEOM, depth \$DEPTH)...\"\\n" +
            "Xvnc \":\$DISP_NUM\" -geometry \"\$GEOM\" -depth \"\$DEPTH\" \$PW -SecurityTypes \"\$SECTYPES\" -UseBlacklist=0 \$LH_ARG >/dev/null 2>&1 &\\n" +
            "sleep 1\\n" +
            "if [ -x \"\$XSTARTUP\" ]; then DISPLAY=\":\$DISP_NUM\" \"\$XSTARTUP\" >/dev/null 2>&1 & elif [ -x \"\$HOME/.vnc/xstartup\" ]; then DISPLAY=\":\$DISP_NUM\" \"\$HOME/.vnc/xstartup\" >/dev/null 2>&1 & elif command -v startxfce4 >/dev/null 2>&1; then DISPLAY=\":\$DISP_NUM\" startxfce4 >/dev/null 2>&1 & fi\\n" +
            "LAN_IP=\"\"; [ -f /etc/network/proot_interfaces.conf ] && LAN_IP=\$(grep \"^PRIMARY_IP=\" /etc/network/proot_interfaces.conf 2>/dev/null | cut -d= -f2); [ -z \"\$LAN_IP\" ] && [ -f /etc/hosts ] && LAN_IP=\$(grep -v \"^#\" /etc/hosts 2>/dev/null | grep -v \"^127\\\\.\" | grep -v \"^::1\" | awk \"{print \\\$1}\" | head -n1)\\n" +
            "echo \"VNC Server started on port \$RFB_PORT (:\$DISP_NUM).\"\\n" +
            "echo \"-> Local connection:  127.0.0.1:\$RFB_PORT\"\\n" +
            "[ \"\$LOCALHOST\" != \"yes\" ] && [ -n \"\$LAN_IP\" ] && [ \"\$LAN_IP\" != \"127.0.0.1\" ] && echo \"-> Wi-Fi connection:  \$LAN_IP:\$RFB_PORT\"\\n" +
            "echo \"-> List sessions:     vncserver -list\"\\n" +
            "echo \"-> Stop server:       vncserver -kill :\$DISP_NUM\"\\n' > /usr/bin/vncserver && chmod 755 /usr/bin/vncserver && cp /usr/bin/vncserver /usr/local/bin/vncserver 2>/dev/null || true; chmod 755 /usr/local/bin/vncserver 2>/dev/null || true"
}
