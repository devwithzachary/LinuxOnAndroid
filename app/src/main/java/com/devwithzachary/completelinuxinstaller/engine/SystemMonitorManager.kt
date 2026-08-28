package com.devwithzachary.completelinuxinstaller.engine

import android.app.ActivityManager
import android.content.Context
import android.os.Process
import android.os.StatFs
import android.system.Os
import android.system.OsConstants
import java.io.File
import java.net.InetSocketAddress
import java.net.Socket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ContainerProcessInfo(
    val pid: Int,
    val name: String,
    val user: String,
    val rssMb: Double,
    val state: String,
    val cmdline: String
)

data class ListeningPortInfo(
    val port: Int,
    val protocol: String = "TCP",
    val serviceName: String,
    val isWebAccessible: Boolean = false,
    val processName: String? = null
)

data class SystemResourceMetrics(
    val containerMemoryUsedMb: Long = 0L,
    val systemTotalRamMb: Long = 0L,
    val systemAvailableRamMb: Long = 0L,
    val storageUsedMb: Long = 0L,
    val storageTotalBytes: Long = 0L,
    val storageAvailableBytes: Long = 0L,
    val processes: List<ContainerProcessInfo> = emptyList(),
    val listeningPorts: List<ListeningPortInfo> = emptyList(),
    val isSessionRunning: Boolean = false
) {
    val systemUsedRamMb: Long
        get() = (systemTotalRamMb - systemAvailableRamMb).coerceAtLeast(0L)

    val ramUsagePercent: Float
        get() = if (systemTotalRamMb > 0) (systemUsedRamMb.toFloat() / systemTotalRamMb.toFloat()).coerceIn(0f, 1f) else 0f

    val storageUsagePercent: Float
        get() = if (storageTotalBytes > 0) {
            val usedBytes = (storageTotalBytes - storageAvailableBytes).coerceAtLeast(0L)
            (usedBytes.toFloat() / storageTotalBytes.toFloat()).coerceIn(0f, 1f)
        } else 0f
}

class SystemMonitorManager(
    private val context: Context,
    private val pRootEngine: PRootEngine,
    private val rootfsManager: RootfsManager
) {

    private val myAppUid: Int = Process.myUid()

    suspend fun collectMetrics(isSessionRunning: Boolean): SystemResourceMetrics = withContext(Dispatchers.IO) {
        var totalRamMb = 0L
        var availRamMb = 0L
        try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memInfo)
            totalRamMb = memInfo.totalMem / (1024 * 1024)
            availRamMb = memInfo.availMem / (1024 * 1024)
        } catch (_: Exception) {
            totalRamMb = 0L
            availRamMb = 0L
        }

        var storageTotal = 0L
        var storageAvail = 0L
        try {
            val statFs = StatFs(context.filesDir.absolutePath)
            storageTotal = statFs.totalBytes
            storageAvail = statFs.availableBytes
        } catch (_: Exception) {}

        val storageUsedMb = try {
            rootfsManager.getCachedStorageUsedMb()
        } catch (_: Exception) {
            0L
        }

        val processes = collectProcesses()
        val containerMemMb = processes.sumOf { it.rssMb }.toLong().coerceAtLeast(getFallbackAppMemoryMb())
        val listeningPorts = collectListeningPorts(rootfsManager.isInstalled())

        SystemResourceMetrics(
            containerMemoryUsedMb = containerMemMb,
            systemTotalRamMb = totalRamMb,
            systemAvailableRamMb = availRamMb,
            storageUsedMb = storageUsedMb,
            storageTotalBytes = storageTotal,
            storageAvailableBytes = storageAvail,
            processes = processes,
            listeningPorts = listeningPorts,
            isSessionRunning = isSessionRunning
        )
    }

    fun collectProcesses(): List<ContainerProcessInfo> {
        val procDir = File("/proc")
        if (!procDir.exists() || !procDir.isDirectory) return emptyList()

        val pageSize = try {
            Os.sysconf(OsConstants._SC_PAGESIZE)
        } catch (_: Throwable) {
            4096L
        }

        val myPid = Process.myPid()
        val result = mutableListOf<ContainerProcessInfo>()

        val pidDirs = procDir.listFiles { file -> file.isDirectory && file.name.all { it.isDigit() } } ?: return emptyList()

        for (pidDir in pidDirs) {
            val pid = pidDir.name.toIntOrNull() ?: continue
            try {
                val statusFile = File(pidDir, "status")
                if (!statusFile.canRead()) continue

                val lines = statusFile.readLines()
                var procName = pidDir.name
                var state = "S"
                var uid = -1

                for (line in lines) {
                    if (line.startsWith("Name:")) {
                        procName = line.substringAfter("Name:").trim()
                    } else if (line.startsWith("State:")) {
                        state = line.substringAfter("State:").trim().take(1)
                    } else if (line.startsWith("Uid:")) {
                        val uidTokens = line.substringAfter("Uid:").trim().split("\\s+".toRegex())
                        uid = uidTokens.firstOrNull()?.toIntOrNull() ?: -1
                    }
                }

                // Only include processes belonging to our app's UID, excluding the host Android process itself
                if (uid != myAppUid || pid == myPid) continue

                var rssMb = 0.0
                val statmFile = File(pidDir, "statm")
                if (statmFile.canRead()) {
                    val statmTokens = statmFile.readText().trim().split("\\s+".toRegex())
                    if (statmTokens.size >= 2) {
                        val rssPages = statmTokens[1].toLongOrNull() ?: 0L
                        rssMb = (rssPages * pageSize) / (1024.0 * 1024.0)
                    }
                }

                var cmdline = ""
                val cmdlineFile = File(pidDir, "cmdline")
                if (cmdlineFile.canRead()) {
                    val raw = cmdlineFile.readBytes()
                    cmdline = String(raw).replace('\u0000', ' ').trim()
                }
                if (cmdline.isBlank()) {
                    cmdline = procName
                }

                // Derive a clean display name and user
                val isRoot = cmdline.contains("proot") || cmdline.contains("root") || procName.contains("proot")
                val displayUser = if (isRoot) "root" else "ubuntu"

                result.add(
                    ContainerProcessInfo(
                        pid = pid,
                        name = sanitizeProcessName(procName, cmdline),
                        user = displayUser,
                        rssMb = (Math.round(rssMb * 10.0) / 10.0),
                        state = state,
                        cmdline = cmdline
                    )
                )
            } catch (_: Exception) {}
        }

        return result.sortedByDescending { it.rssMb }
    }

    private fun sanitizeProcessName(name: String, cmdline: String): String {
        return when {
            cmdline.contains("Xtigervnc") || name == "Xtigervnc" -> "TigerVNC Server"
            cmdline.contains("xfdesktop") -> "XFCE Desktop"
            cmdline.contains("xfwm4") -> "XFCE Window Mgr"
            cmdline.contains("sshd") || name == "sshd" -> "OpenSSH Daemon"
            cmdline.contains("nginx") || name == "nginx" -> "NGINX Web Server"
            cmdline.contains("dbus-daemon") || name == "dbus-daemon" -> "D-Bus Daemon"
            cmdline.contains("pulseaudio") || name == "pulseaudio" -> "PulseAudio"
            cmdline.contains("proot") || name == "proot" -> "PRoot Engine"
            cmdline.contains("bash") || name == "bash" -> "Bash Shell"
            cmdline.contains("zsh") || name == "zsh" -> "Zsh Shell"
            cmdline.contains("python") || name.startsWith("python") -> "Python Process"
            cmdline.contains("node") || name == "node" -> "Node.js Process"
            cmdline.contains("htop") -> "htop"
            cmdline.contains("apt") || cmdline.contains("dpkg") -> "APT / Dpkg"
            else -> name
        }
    }

    fun collectListeningPorts(isInstalled: Boolean): List<ListeningPortInfo> {
        val portsFound = mutableSetOf<Int>()
        val result = mutableListOf<ListeningPortInfo>()

        // 1. Parse /proc/net/tcp and /proc/net/tcp6 for TCP_LISTEN (state 0A)
        for (tcpPath in listOf("/proc/net/tcp", "/proc/net/tcp6")) {
            val tcpFile = File(tcpPath)
            if (tcpFile.canRead()) {
                try {
                    tcpFile.forEachLine { line ->
                        val parts = line.trim().split("\\s+".toRegex())
                        if (parts.size >= 4) {
                            val state = parts[3]
                            if (state.equals("0A", ignoreCase = true)) {
                                val localAddress = parts[1]
                                val portHex = localAddress.substringAfterLast(":")
                                val portInt = portHex.toIntOrNull(16)
                                if (portInt != null && portInt in 1..65535) {
                                    portsFound.add(portInt)
                                }
                            }
                        }
                    }
                } catch (_: Exception) {}
            }
        }

        // 2. Active probe well-known container service ports if container is installed
        if (isInstalled) {
            val rootfsDir = pRootEngine.rootfsDir
            if (com.devwithzachary.completelinuxinstaller.service.ServiceStatusManager.isVncRunning(rootfsDir)) {
                portsFound.add(5901)
            }
            if (com.devwithzachary.completelinuxinstaller.service.ServiceStatusManager.isNginxRunning(rootfsDir)) {
                portsFound.add(80)
            }
            if (com.devwithzachary.completelinuxinstaller.service.ServiceStatusManager.isSshRunning(rootfsDir, 2222)) {
                portsFound.add(2222)
            }

            val probeCandidatePorts = listOf(22, 2222, 5900, 5901, 80, 8080, 3000, 5000, 8000, 8888, 9090)
            for (candidate in probeCandidatePorts) {
                if (candidate !in portsFound && isPortOpenLocally(candidate)) {
                    portsFound.add(candidate)
                }
            }
        }

        for (port in portsFound.sorted()) {
            val (serviceName, isWeb) = resolveServiceName(port)
            result.add(
                ListeningPortInfo(
                    port = port,
                    protocol = "TCP",
                    serviceName = serviceName,
                    isWebAccessible = isWeb
                )
            )
        }

        return result
    }

    fun resolveServiceName(port: Int): Pair<String, Boolean> {
        return when (port) {
            22, 2222 -> "OpenSSH Server" to false
            5900, 5901, 5902 -> "TigerVNC Desktop (:1)" to false
            80 -> "HTTP Web Server (NGINX / Apache)" to true
            443 -> "HTTPS Web Server" to true
            8080 -> "HTTP Alternate (NGINX / Tomcat)" to true
            8000 -> "Python HTTP / Dev Server" to true
            3000 -> "Node.js / React Web App" to true
            5000 -> "Flask / Web Service" to true
            8888 -> "Jupyter Notebook" to true
            9090 -> "Cockpit / Web Console" to true
            5432 -> "PostgreSQL Database" to false
            3306 -> "MySQL / MariaDB" to false
            6379 -> "Redis In-Memory Store" to false
            27017 -> "MongoDB Database" to false
            else -> "Active TCP Service" to (port in listOf(80, 443, 8080, 8000, 3000, 5000, 8888, 9090))
        }
    }

    private fun isPortOpenLocally(port: Int): Boolean {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress("127.0.0.1", port), 50)
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    fun killProcess(pid: Int): Boolean {
        return try {
            Os.kill(pid, OsConstants.SIGKILL)
            true
        } catch (_: Exception) {
            try {
                Process.killProcess(pid)
                true
            } catch (_: Exception) {
                false
            }
        }
    }

    private fun getFallbackAppMemoryMb(): Long {
        val runtime = Runtime.getRuntime()
        val usedBytes = runtime.totalMemory() - runtime.freeMemory()
        return (usedBytes / (1024 * 1024)).coerceAtLeast(1L)
    }
}
