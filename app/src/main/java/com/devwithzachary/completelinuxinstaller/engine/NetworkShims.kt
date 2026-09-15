package com.devwithzachary.completelinuxinstaller.engine

import android.content.Context
import android.util.Log
import java.io.File
import java.net.Inet4Address
import java.net.NetworkInterface

/**
 * Manages network diagnostic shims and environment configurations for PRoot Linux environments.
 *
 * Android 10+ SELinux rules enforce `neverallow untrusted_app proc_net:file read`, which prevents
 * unprivileged applications from reading `/proc/net/tcp` and accessing `NETLINK_INET_DIAG` sockets.
 * This causes standard utilities (`netstat`, `ss`, `ip addr`) to fail or report zero listening sockets.
 *
 * NetworkShims solves this by:
 * 1. Injecting transparent `/usr/local/bin/{netstat, ss, ip, hostname, vncserver}` shims that attempt
 *    native execution and cleanly fall back to querying active server processes and host network state.
 * 2. Generating `/etc/network/proot_interfaces.conf` populated with active network interface metadata.
 * 3. Updating `/etc/hosts` so `hostname -i` resolves to the device's actual LAN / Wi-Fi IPv4 address.
 */
object NetworkShims {
    private const val TAG = "NetworkShims"

    data class NetInterfaceData(
        val index: Int,
        val name: String,
        val ipv4Addresses: List<String>,
        val ipv6Addresses: List<String>,
        val macAddress: String,
        val mtu: Int,
        val isUp: Boolean,
        val isLoopback: Boolean
    )

    /**
     * Inspects active network interfaces on the host Android device.
     */
    fun getActiveInterfaces(): List<NetInterfaceData> {
        val result = mutableListOf<NetInterfaceData>()
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()?.toList().orEmpty()
            for (iface in interfaces) {
                val ipv4List = mutableListOf<String>()
                val ipv6List = mutableListOf<String>()
                try {
                    for (addr in iface.interfaceAddresses) {
                        val hostAddr = addr.address?.hostAddress ?: continue
                        val cleanIp = hostAddr.substringBefore("%")
                        val prefix = addr.networkPrefixLength
                        if (addr.address is Inet4Address) {
                            ipv4List.add("$cleanIp/$prefix")
                        } else {
                            ipv6List.add("$cleanIp/$prefix")
                        }
                    }
                } catch (_: Exception) {}

                val mac = try {
                    iface.hardwareAddress?.joinToString(":") { "%02x".format(it) } ?: "00:00:00:00:00:00"
                } catch (_: Exception) {
                    "00:00:00:00:00:00"
                }

                val mtu = try { iface.mtu } catch (_: Exception) { 1500 }
                val isUp = try { iface.isUp } catch (_: Exception) { true }
                val isLoopback = try { iface.isLoopback } catch (_: Exception) { false }
                val index = try { iface.index } catch (_: Exception) { result.size + 1 }

                result.add(
                    NetInterfaceData(
                        index = index,
                        name = iface.name,
                        ipv4Addresses = ipv4List,
                        ipv6Addresses = ipv6List,
                        macAddress = mac,
                        mtu = mtu,
                        isUp = isUp,
                        isLoopback = isLoopback
                    )
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error enumerating network interfaces: ${e.message}")
        }
        return result
    }

    /**
     * Determines the primary active LAN/Wi-Fi IPv4 address of the device.
     * Prefers Wi-Fi (`wlan`) or Ethernet (`eth`), then other non-loopback interfaces.
     * Falls back to `127.0.0.1` if no network connection is active.
     */
    fun getPrimaryLanIp(interfaces: List<NetInterfaceData> = getActiveInterfaces()): String {
        val preferred = interfaces.firstOrNull { iface ->
            !iface.isLoopback && iface.isUp &&
                (iface.name.startsWith("wlan") || iface.name.startsWith("eth")) &&
                iface.ipv4Addresses.isNotEmpty()
        }
        if (preferred != null) {
            return preferred.ipv4Addresses.first().substringBefore("/")
        }

        val anyOther = interfaces.firstOrNull { iface ->
            !iface.isLoopback && iface.isUp && iface.ipv4Addresses.isNotEmpty()
        }
        if (anyOther != null) {
            return anyOther.ipv4Addresses.first().substringBefore("/")
        }

        return "127.0.0.1"
    }

    /**
     * Ensures all network shims, `/etc/hosts`, and interface metadata are installed in target rootfs.
     */
    fun ensureNetworkShims(targetRootfs: File, context: Context? = null) {
        try {
            val etcDir = File(targetRootfs, "etc").apply { if (!exists()) mkdirs() }
            val netDir = File(etcDir, "network").apply { if (!exists()) mkdirs() }
            val usrLocalBin = File(targetRootfs, "usr/local/bin").apply { if (!exists()) mkdirs() }

            val interfaces = getActiveInterfaces()
            val primaryIp = getPrimaryLanIp(interfaces)

            // 1. Generate /etc/network/proot_interfaces.conf
            val ifaceConf = File(netDir, "proot_interfaces.conf")
            val confContent = buildString {
                appendLine("# Generated by LinuxOnAndroid PRoot Network Helper")
                appendLine("PRIMARY_IP=$primaryIp")
                for (iface in interfaces) {
                    val v4 = iface.ipv4Addresses.joinToString(",")
                    val v6 = iface.ipv6Addresses.joinToString(",")
                    val upStr = if (iface.isUp) "UP" else "DOWN"
                    val loopStr = if (iface.isLoopback) "LOOPBACK" else "BROADCAST"
                    appendLine("IFACE:${iface.index}:${iface.name}:$v4:$v6:${iface.macAddress}:${iface.mtu}:$upStr:$loopStr")
                }
            }
            ifaceConf.writeText(confContent)
            ifaceConf.setReadable(true, false)

            // 2. Update /etc/hosts with the LAN IP mapping
            updateHostsFile(etcDir, primaryIp)

            // 3. Deploy /usr/local/bin/netstat
            installNetstatShim(usrLocalBin)

            // 4. Deploy /usr/local/bin/ss
            installSsShim(usrLocalBin)

            // 5. Deploy /usr/local/bin/ip
            installIpShim(usrLocalBin)

            // 6. Deploy /usr/local/bin/hostname
            installHostnameShim(usrLocalBin)

            // 7. Deploy /usr/local/bin/vncserver
            installVncserverShim(usrLocalBin)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to ensure network shims in rootfs", e)
        }
    }

    private fun updateHostsFile(etcDir: File, primaryIp: String) {
        try {
            val hostsFile = File(etcDir, "hosts")
            val hostnameFile = File(etcDir, "hostname")
            val containerHostName = if (hostnameFile.exists()) {
                hostnameFile.readText().trim().takeIf { it.isNotBlank() } ?: "localhost"
            } else {
                "localhost"
            }

            val existing = if (hostsFile.exists()) hostsFile.readText() else ""
            val lines = existing.lines().toMutableList()

            val filtered = lines.filterNot { line ->
                val trimmed = line.trim()
                !trimmed.startsWith("127.") && !trimmed.startsWith("::1") &&
                    !trimmed.startsWith("#") && trimmed.contains(containerHostName)
            }.toMutableList()

            if (!filtered.any { it.contains("127.0.0.1") && it.contains("localhost") }) {
                filtered.add(0, "127.0.0.1   localhost localhost.localdomain")
            }
            if (!filtered.any { it.contains("::1") && it.contains("localhost") }) {
                filtered.add(1, "::1         localhost ip6-localhost ip6-loopback")
            }

            if (primaryIp != "127.0.0.1" && containerHostName.isNotBlank() && containerHostName != "localhost") {
                filtered.add("$primaryIp   $containerHostName")
            }

            hostsFile.writeText(filtered.joinToString("\n").trimEnd() + "\n")
            hostsFile.setReadable(true, false)
        } catch (_: Exception) {}
    }

    private fun installNetstatShim(usrLocalBin: File) {
        val shim = File(usrLocalBin, "netstat")
        val d = "$"
        val script = """
            #!/bin/sh
            # LinuxOnAndroid netstat wrapper for PRoot environments
            REAL_NETSTAT=""
            if [ -x "/usr/bin/netstat" ] && [ "/usr/bin/netstat" != "${d}0" ]; then
                REAL_NETSTAT="/usr/bin/netstat"
            elif [ -x "/bin/netstat" ] && [ "/bin/netstat" != "${d}0" ]; then
                REAL_NETSTAT="/bin/netstat"
            fi

            if [ -n "${d}REAL_NETSTAT" ]; then
                OUTPUT=$("${d}REAL_NETSTAT" "${d}@" 2>&1)
                EXIT_CODE=${d}?
                case "${d}OUTPUT" in
                    *"AF INET"*|*"cannot open /proc/net"*|*"Permission denied"*)
                        ;;
                    *)
                        printf "%s\n" "${d}OUTPUT"
                        exit "${d}EXIT_CODE"
                        ;;
                esac
            fi

            # Fallback: synthesize listening ports by inspecting active server processes
            printf "Active Internet connections (only servers)\n"
            printf "%-5s %6s %6s %-23s %-23s %-11s %s\n" "Proto" "Recv-Q" "Send-Q" "Local Address" "Foreign Address" "State" "PID/Program name"

            PROCS=${d}(ps -ef 2>/dev/null || ps aux 2>/dev/null)

            # 1. Check Xvnc sessions (via X11 lock files and process list)
            FOUND_VNCS=""
            for lock in /tmp/.X*-lock; do
                [ -e "${d}lock" ] || continue
                D_NUM=${d}(basename "${d}lock" | sed 's/\.X//;s/-lock//')
                PID=${d}(cat "${d}lock" 2>/dev/null | tr -d ' ' | tr -d '\n')
                if [ -n "${d}PID" ] && ( [ -d "/proc/${d}PID" ] || kill -0 "${d}PID" 2>/dev/null ); then
                    PORT=${d}((5900 + D_NUM))
                    FOUND_VNCS="${d}{FOUND_VNCS}:${d}D_NUM:"
                    printf "%-5s %6d %6d %-23s %-23s %-11s %s\n" "tcp" 0 0 "0.0.0.0:${d}PORT" "0.0.0.0:*" "LISTEN" "${d}PID/Xvnc"
                    printf "%-5s %6d %6d %-23s %-23s %-11s %s\n" "tcp6" 0 0 ":::${d}PORT" ":::*" "LISTEN" "${d}PID/Xvnc"
                fi
            done

            # 2. Fallback check for any Xvnc not caught by locks
            echo "${d}PROCS" | grep -v "grep" | grep -v "libproot" | grep -v "printf" | grep "Xvnc" | while read -r line; do
                PID=${d}(echo "${d}line" | awk '{print ${d}2}')
                DISP=${d}(echo "${d}line" | grep -E -o ':[0-9]+' | head -n1 | tr -d ':')
                PORT=""
                RFB_ARG=${d}(echo "${d}line" | grep -E -o '\-rfbport [0-9]+' | awk '{print ${d}2}')
                if [ -n "${d}RFB_ARG" ]; then
                    PORT="${d}RFB_ARG"
                elif [ -n "${d}DISP" ]; then
                    PORT=${d}((5900 + DISP))
                fi
                case "${d}FOUND_VNCS" in
                    *":${d}DISP:"*) ;;
                    *)
                        if [ -n "${d}PORT" ] && [ -n "${d}PID" ]; then
                            printf "%-5s %6d %6d %-23s %-23s %-11s %s\n" "tcp" 0 0 "0.0.0.0:${d}PORT" "0.0.0.0:*" "LISTEN" "${d}PID/Xvnc"
                            printf "%-5s %6d %6d %-23s %-23s %-11s %s\n" "tcp6" 0 0 ":::${d}PORT" ":::*" "LISTEN" "${d}PID/Xvnc"
                        fi
                        ;;
                esac
            done

            # 3. Check sshd processes
            echo "${d}PROCS" | grep -v "grep" | grep "sshd" | head -n1 | while read -r line; do
                PID=${d}(echo "${d}line" | awk '{print ${d}2}')
                if [ -n "${d}PID" ]; then
                    printf "%-5s %6d %6d %-23s %-23s %-11s %s\n" "tcp" 0 0 "0.0.0.0:22" "0.0.0.0:*" "LISTEN" "${d}PID/sshd"
                    printf "%-5s %6d %6d %-23s %-23s %-11s %s\n" "tcp6" 0 0 ":::22" ":::*" "LISTEN" "${d}PID/sshd"
                fi
            done

            # 4. Check nginx processes
            echo "${d}PROCS" | grep -v "grep" | grep "nginx: master" | head -n1 | while read -r line; do
                PID=${d}(echo "${d}line" | awk '{print ${d}2}')
                if [ -n "${d}PID" ]; then
                    printf "%-5s %6d %6d %-23s %-23s %-11s %s\n" "tcp" 0 0 "0.0.0.0:80" "0.0.0.0:*" "LISTEN" "${d}PID/nginx"
                fi
            done

            # 5. Check ttyd processes
            echo "${d}PROCS" | grep -v "grep" | grep "ttyd" | while read -r line; do
                PID=${d}(echo "${d}line" | awk '{print ${d}2}')
                PORT=${d}(echo "${d}line" | grep -o '\(-p\|--port\) [0-9]\+' | awk '{print ${d}2}')
                [ -z "${d}PORT" ] && PORT="7681"
                if [ -n "${d}PID" ]; then
                    printf "%-5s %6d %6d %-23s %-23s %-11s %s\n" "tcp" 0 0 "0.0.0.0:${d}PORT" "0.0.0.0:*" "LISTEN" "${d}PID/ttyd"
                fi
            done

            # 6. Check code-server processes
            echo "${d}PROCS" | grep -v "grep" | grep "code-server" | head -n1 | while read -r line; do
                PID=${d}(echo "${d}line" | awk '{print ${d}2}')
                PORT=${d}(echo "${d}line" | grep -o '\(--port\) [0-9]\+' | awk '{print ${d}2}')
                [ -z "${d}PORT" ] && PORT="8080"
                if [ -n "${d}PID" ]; then
                    printf "%-5s %6d %6d %-23s %-23s %-11s %s\n" "tcp" 0 0 "0.0.0.0:${d}PORT" "0.0.0.0:*" "LISTEN" "${d}PID/node"
                fi
            done
        """.trimIndent() + "\n"
        shim.writeText(script)
        shim.setExecutable(true, false)
        shim.setReadable(true, false)
    }

    private fun installSsShim(usrLocalBin: File) {
        val shim = File(usrLocalBin, "ss")
        val d = "$"
        val script = """
            #!/bin/sh
            # LinuxOnAndroid ss wrapper for PRoot environments
            REAL_SS=""
            if [ -x "/usr/sbin/ss" ] && [ "/usr/sbin/ss" != "${d}0" ]; then
                REAL_SS="/usr/sbin/ss"
            elif [ -x "/usr/bin/ss" ] && [ "/usr/bin/ss" != "${d}0" ]; then
                REAL_SS="/usr/bin/ss"
            fi

            if [ -n "${d}REAL_SS" ]; then
                OUTPUT=$("${d}REAL_SS" "${d}@" 2>&1)
                EXIT_CODE=${d}?
                case "${d}OUTPUT" in
                    *"Cannot open netlink socket"*|*"Permission denied"*)
                        ;;
                    *)
                        printf "%s\n" "${d}OUTPUT"
                        exit "${d}EXIT_CODE"
                        ;;
                esac
            fi

            # Fallback: synthesize listening sockets
            printf "%-8s %-6s %-6s %-22s %-22s %s\n" "State" "Recv-Q" "Send-Q" "Local Address:Port" "Peer Address:Port" "Process"

            PROCS=${d}(ps -ef 2>/dev/null || ps aux 2>/dev/null)

            # Check Xvnc via lock files
            FOUND_VNCS=""
            for lock in /tmp/.X*-lock; do
                [ -e "${d}lock" ] || continue
                D_NUM=${d}(basename "${d}lock" | sed 's/\.X//;s/-lock//')
                PID=${d}(cat "${d}lock" 2>/dev/null | tr -d ' ' | tr -d '\n')
                if [ -n "${d}PID" ] && ( [ -d "/proc/${d}PID" ] || kill -0 "${d}PID" 2>/dev/null ); then
                    PORT=${d}((5900 + D_NUM))
                    FOUND_VNCS="${d}{FOUND_VNCS}:${d}D_NUM:"
                    printf "%-8s %-6d %-6d %-22s %-22s %s\n" "LISTEN" 0 128 "0.0.0.0:${d}PORT" "0.0.0.0:*" "users:((\"Xvnc\",pid=${d}PID,fd=1))"
                    printf "%-8s %-6d %-6d %-22s %-22s %s\n" "LISTEN" 0 128 "[::]:${d}PORT" "[::]:*" "users:((\"Xvnc\",pid=${d}PID,fd=1))"
                fi
            done

            # Check Xvnc via process list
            echo "${d}PROCS" | grep -v "grep" | grep -v "libproot" | grep -v "printf" | grep "Xvnc" | while read -r line; do
                PID=${d}(echo "${d}line" | awk '{print ${d}2}')
                DISP=${d}(echo "${d}line" | grep -E -o ':[0-9]+' | head -n1 | tr -d ':')
                PORT=""
                RFB_ARG=${d}(echo "${d}line" | grep -E -o '\-rfbport [0-9]+' | awk '{print ${d}2}')
                if [ -n "${d}RFB_ARG" ]; then
                    PORT="${d}RFB_ARG"
                elif [ -n "${d}DISP" ]; then
                    PORT=${d}((5900 + DISP))
                fi
                case "${d}FOUND_VNCS" in
                    *":${d}DISP:"*) ;;
                    *)
                        if [ -n "${d}PORT" ] && [ -n "${d}PID" ]; then
                            printf "%-8s %-6d %-6d %-22s %-22s %s\n" "LISTEN" 0 128 "0.0.0.0:${d}PORT" "0.0.0.0:*" "users:((\"Xvnc\",pid=${d}PID,fd=1))"
                            printf "%-8s %-6d %-6d %-22s %-22s %s\n" "LISTEN" 0 128 "[::]:${d}PORT" "[::]:*" "users:((\"Xvnc\",pid=${d}PID,fd=1))"
                        fi
                        ;;
                esac
            done

            # Check sshd
            echo "${d}PROCS" | grep -v "grep" | grep "sshd" | head -n1 | while read -r line; do
                PID=${d}(echo "${d}line" | awk '{print ${d}2}')
                if [ -n "${d}PID" ]; then
                    printf "%-8s %-6d %-6d %-22s %-22s %s\n" "LISTEN" 0 128 "0.0.0.0:22" "0.0.0.0:*" "users:((\"sshd\",pid=${d}PID,fd=3))"
                    printf "%-8s %-6d %-6d %-22s %-22s %s\n" "LISTEN" 0 128 "[::]:22" "[::]:*" "users:((\"sshd\",pid=${d}PID,fd=4))"
                fi
            done
        """.trimIndent() + "\n"
        shim.writeText(script)
        shim.setExecutable(true, false)
        shim.setReadable(true, false)
    }

    private fun installIpShim(usrLocalBin: File) {
        val shim = File(usrLocalBin, "ip")
        val d = "$"
        val script = """
            #!/bin/sh
            # LinuxOnAndroid ip wrapper for PRoot environments
            REAL_IP=""
            if [ -x "/usr/sbin/ip" ] && [ "/usr/sbin/ip" != "${d}0" ]; then
                REAL_IP="/usr/sbin/ip"
            elif [ -x "/bin/ip" ] && [ "/bin/ip" != "${d}0" ]; then
                REAL_IP="/bin/ip"
            fi

            if [ -n "${d}REAL_IP" ]; then
                OUTPUT=$("${d}REAL_IP" "${d}@" 2>&1)
                EXIT_CODE=${d}?
                case "${d}OUTPUT" in
                    *"write error: Socket not connected"*|*"Socket not connected"*)
                        ;;
                    *)
                        printf "%s\n" "${d}OUTPUT"
                        exit "${d}EXIT_CODE"
                        ;;
                esac
            fi

            # Fallback for `ip addr` / `ip a`
            IS_ADDR=0
            if [ ${d}# -eq 0 ]; then
                IS_ADDR=1
            else
                case "${d}1" in
                    a|addr|address) IS_ADDR=1 ;;
                esac
            fi

            if [ "${d}IS_ADDR" -eq 1 ] && [ -f "/etc/network/proot_interfaces.conf" ]; then
                grep "^IFACE:" /etc/network/proot_interfaces.conf 2>/dev/null | while IFS=: read -r prefix idx name v4 v6 mac mtu up loop; do
                    printf "%d: %s: <%s,LOWER_UP> mtu %s state %s\n" "${d}idx" "${d}name" "${d}loop" "${d}mtu" "${d}up"
                    if [ "${d}name" = "lo" ]; then
                        printf "    link/loopback %s\n" "${d}mac"
                        printf "    inet 127.0.0.1/8 scope host lo\n"
                        printf "    inet6 ::1/128 scope host\n"
                    else
                        printf "    link/ether %s\n" "${d}mac"
                        echo "${d}v4" | tr ',' '\n' | while read -r ip; do
                            [ -n "${d}ip" ] && printf "    inet %s scope global %s\n" "${d}ip" "${d}name"
                        done
                        echo "${d}v6" | tr ',' '\n' | while read -r ip; do
                            [ -n "${d}ip" ] && printf "    inet6 %s scope global %s\n" "${d}ip" "${d}name"
                        done
                    fi
                done
                exit 0
            fi

            # Default minimal fallback
            printf "1: lo: <LOOPBACK,UP,LOWER_UP> mtu 65536 state UP\n"
            printf "    link/loopback 00:00:00:00:00:00\n"
            printf "    inet 127.0.0.1/8 scope host lo\n"
            printf "    inet6 ::1/128 scope host\n"
        """.trimIndent() + "\n"
        shim.writeText(script)
        shim.setExecutable(true, false)
        shim.setReadable(true, false)
    }

    private fun installHostnameShim(usrLocalBin: File) {
        val shim = File(usrLocalBin, "hostname")
        val d = "$"
        val script = """
            #!/bin/sh
            # LinuxOnAndroid hostname wrapper for PRoot environments
            REAL_HOSTNAME=""
            if [ -x "/bin/hostname" ] && [ "/bin/hostname" != "${d}0" ]; then
                REAL_HOSTNAME="/bin/hostname"
            elif [ -x "/usr/bin/hostname" ] && [ "/usr/bin/hostname" != "${d}0" ]; then
                REAL_HOSTNAME="/usr/bin/hostname"
            fi

            IS_I=0
            for arg in "${d}@"; do
                if [ "${d}arg" = "-i" ]; then
                    IS_I=1
                    break
                fi
            done

            if [ "${d}IS_I" -eq 1 ]; then
                if [ -n "${d}REAL_HOSTNAME" ]; then
                    OUT=$("${d}REAL_HOSTNAME" "${d}@" 2>/dev/null)
                    case "${d}OUT" in
                        *"127.0.0.1"*|*"::1"*|"")
                            ;;
                        *)
                            printf "%s\n" "${d}OUT"
                            exit 0
                            ;;
                    esac
                fi
                if [ -f "/etc/network/proot_interfaces.conf" ]; then
                    PIP=${d}(grep "^PRIMARY_IP=" /etc/network/proot_interfaces.conf 2>/dev/null | cut -d= -f2)
                    if [ -n "${d}PIP" ] && [ "${d}PIP" != "127.0.0.1" ]; then
                        printf "%s\n" "${d}PIP"
                        exit 0
                    fi
                fi
                if [ -f "/etc/hosts" ]; then
                    HOST_IP=${d}(grep -v "^#" /etc/hosts 2>/dev/null | grep -v "^127\." | grep -v "^::1" | awk '{print ${d}1}' | head -n1)
                    if [ -n "${d}HOST_IP" ]; then
                        printf "%s\n" "${d}HOST_IP"
                        exit 0
                    fi
                fi
            fi

            if [ -n "${d}REAL_HOSTNAME" ]; then
                exec "${d}REAL_HOSTNAME" "${d}@"
            fi
            if [ -f "/etc/hostname" ]; then
                cat /etc/hostname
            else
                echo "localhost"
            fi
        """.trimIndent() + "\n"
        shim.writeText(script)
        shim.setExecutable(true, false)
        shim.setReadable(true, false)
    }

    private fun installVncserverShim(usrLocalBin: File) {
        val shim = File(usrLocalBin, "vncserver")
        val d = "$"
        val script = """
            #!/bin/sh
            # LinuxOnAndroid TigerVNC server wrapper for PRoot environments
            export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:${d}PATH

            ACTION="start"
            DISP=":1"
            GEOM="1280x720"
            DEPTH="24"
            XSTARTUP="/etc/vnc/xstartup"
            SECTYPES="None,VncAuth"

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
            echo "Starting Xvnc on display :${d}DISP_NUM (${d}GEOM, depth ${d}DEPTH)..."
            Xvnc ":${d}DISP_NUM" -geometry "${d}GEOM" -depth "${d}DEPTH" ${d}PW -SecurityTypes "${d}SECTYPES" -UseBlacklist=0 -ac >/dev/null 2>&1 &
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
            if [ -n "${d}LAN_IP" ] && [ "${d}LAN_IP" != "127.0.0.1" ]; then
                echo "-> Wi-Fi connection:  ${d}LAN_IP:${d}RFB_PORT"
            fi
            echo "-> List sessions:     vncserver -list"
            echo "-> Stop server:       vncserver -kill :${d}DISP_NUM"
        """.trimIndent() + "\n"
        shim.writeText(script)
        shim.setExecutable(true, false)
        shim.setReadable(true, false)

        val usrBin = File(usrLocalBin.parentFile, "bin")
        if (usrBin.exists()) {
            val usrBinVnc = File(usrBin, "vncserver")
            try {
                usrBinVnc.writeText(script)
                usrBinVnc.setExecutable(true, false)
                usrBinVnc.setReadable(true, false)
            } catch (_: Exception) {}
        }
    }
}
