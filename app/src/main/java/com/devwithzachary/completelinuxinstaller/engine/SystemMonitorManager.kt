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
    private val rootfsManager: RootfsManager,
    private val containerManager: ContainerManager? = null,
    private val terminalBridge: TerminalBridge? = null
) {

    private val myAppUid: Int = Process.myUid()

    private data class RawProcInfo(
        val pid: Int,
        val ppid: Int,
        val procName: String,
        val state: String,
        val rssMb: Double,
        val cmdline: String,
        val cwd: String?
    )

    suspend fun collectMetrics(isSessionRunning: Boolean, targetRootDir: File? = null): SystemResourceMetrics = withContext(Dispatchers.IO) {
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

        val storageUsedMb = if (targetRootDir != null) {
            val container = containerManager?.getAllContainers()?.find { it.rootDir.absolutePath == targetRootDir.absolutePath }
            container?.storageUsedMb ?: rootfsManager.getCachedStorageUsedMb()
        } else {
            try {
                val containers = containerManager?.getAllContainers() ?: emptyList()
                if (containers.isNotEmpty()) {
                    val sum = containers.sumOf { it.storageUsedMb }
                    if (sum > 0L) sum else rootfsManager.getCachedStorageUsedMb()
                } else {
                    rootfsManager.getCachedStorageUsedMb()
                }
            } catch (_: Exception) {
                0L
            }
        }

        val processes = collectProcesses(targetRootDir)
        val containerMemMb = if (targetRootDir != null) {
            if (processes.isNotEmpty()) {
                processes.sumOf { it.rssMb }.toLong().coerceAtLeast(1L)
            } else {
                0L
            }
        } else {
            processes.sumOf { it.rssMb }.toLong().coerceAtLeast(getFallbackAppMemoryMb())
        }

        val isInstalled = if (targetRootDir != null) {
            containerManager?.getAllContainers()?.find { it.rootDir.absolutePath == targetRootDir.absolutePath }?.isInstalled == true ||
                (targetRootDir.absolutePath == pRootEngine.rootfsDir.absolutePath && rootfsManager.isInstalled())
        } else {
            rootfsManager.isInstalled() || (containerManager?.getAllContainers()?.any { it.isInstalled } == true)
        }

        val listeningPorts = collectListeningPorts(isInstalled = isInstalled, targetRootDir = targetRootDir)

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

    private fun pathMatchesContainer(path: String?, targetRootDir: File): Boolean {
        if (path == null) return false
        val targetAbs = targetRootDir.absolutePath
        val targetCanon = try { targetRootDir.canonicalPath } catch (_: Exception) { targetAbs }
        val isDefaultLegacy = targetAbs.endsWith("/files/rootfs") || targetCanon.endsWith("/files/rootfs")

        if (isDefaultLegacy) {
            return (path.contains(targetAbs) || path.contains(targetCanon)) && !path.contains("/files/containers/")
        }

        if (path.contains(targetAbs) || path.contains(targetCanon)) return true

        val containerDirName = targetRootDir.name
        if (containerDirName.isNotBlank() && containerDirName != "rootfs" && containerDirName != "files") {
            if (path.contains(containerDirName)) return true
        }

        return false
    }

    fun collectProcesses(targetRootDir: File? = null): List<ContainerProcessInfo> {
        val procDir = File("/proc")
        if (!procDir.exists() || !procDir.isDirectory) return emptyList()

        val pageSize = try {
            Os.sysconf(OsConstants._SC_PAGESIZE)
        } catch (_: Throwable) {
            4096L
        }

        val myPid = Process.myPid()
        val rawList = mutableListOf<RawProcInfo>()

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
                var ppid = 0

                for (line in lines) {
                    if (line.startsWith("Name:")) {
                        procName = line.substringAfter("Name:").trim()
                    } else if (line.startsWith("State:")) {
                        state = line.substringAfter("State:").trim().take(1)
                    } else if (line.startsWith("Uid:")) {
                        val uidTokens = line.substringAfter("Uid:").trim().split("\\s+".toRegex())
                        uid = uidTokens.firstOrNull()?.toIntOrNull() ?: -1
                    } else if (line.startsWith("PPid:")) {
                        ppid = line.substringAfter("PPid:").trim().toIntOrNull() ?: 0
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

                val cwd = try {
                    Os.readlink("/proc/$pid/cwd")
                } catch (_: Throwable) {
                    null
                }

                rawList.add(
                    RawProcInfo(
                        pid = pid,
                        ppid = ppid,
                        procName = procName,
                        state = state,
                        rssMb = rssMb,
                        cmdline = cmdline,
                        cwd = cwd
                    )
                )
            } catch (_: Exception) {}
        }

        val filteredRaw = if (targetRootDir != null) {
            val containerPids = mutableSetOf<Int>()

            // 1. Session process PID matching
            terminalBridge?.sessions?.value?.forEach { session ->
                val targetContainer = containerManager?.getAllContainers()?.find { it.rootDir.absolutePath == targetRootDir.absolutePath }
                val isDefaultRootfs = targetRootDir.absolutePath == pRootEngine.rootfsDir.absolutePath
                val matchesSession = (targetContainer != null && session.containerId == targetContainer.id) ||
                    (isDefaultRootfs && (session.containerId == com.devwithzachary.completelinuxinstaller.engine.ContainerManager.DEFAULT_CONTAINER_ID || session.containerId == "ubuntu_default"))
                if (matchesSession) {
                    session.processPid?.let { containerPids.add(it) }
                }
            }

            // 2. Known PID files in targetRootDir
            listOf(
                File(targetRootDir, "tmp/.X1-lock"),
                File(targetRootDir, "run/sshd.pid"),
                File(targetRootDir, "var/run/sshd.pid"),
                File(targetRootDir, "run/nginx.pid"),
                File(targetRootDir, "var/run/nginx.pid")
            ).forEach { pidFile ->
                if (pidFile.exists()) {
                    val pid = try { pidFile.readText().trim().toIntOrNull() } catch (_: Exception) { null }
                    if (pid != null && pid > 0) containerPids.add(pid)
                }
            }

            // 3. Pass 1: Direct root match on cmdline or cwd
            for (proc in rawList) {
                if (pathMatchesContainer(proc.cmdline, targetRootDir) || pathMatchesContainer(proc.cwd, targetRootDir)) {
                    containerPids.add(proc.pid)
                }
            }

            // 4. Pass 2: Process hierarchy propagation (descendants of proot/su/bash)
            var changed = true
            while (changed) {
                changed = false
                for (proc in rawList) {
                    if (proc.pid !in containerPids && proc.ppid in containerPids) {
                        containerPids.add(proc.pid)
                        changed = true
                    }
                }
            }

            rawList.filter { it.pid in containerPids }
        } else {
            rawList
        }

        return filteredRaw.map { proc ->
            val isRoot = proc.cmdline.contains("proot") || proc.cmdline.contains("root") || proc.procName.contains("proot")
            val displayUser = if (isRoot) "root" else "ubuntu"
            ContainerProcessInfo(
                pid = proc.pid,
                name = sanitizeProcessName(proc.procName, proc.cmdline),
                user = displayUser,
                rssMb = (Math.round(proc.rssMb * 10.0) / 10.0),
                state = proc.state,
                cmdline = proc.cmdline
            )
        }.sortedByDescending { it.rssMb }
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

    fun collectListeningPorts(isInstalled: Boolean = true, targetRootDir: File? = null): List<ListeningPortInfo> {
        val portsFound = mutableSetOf<Int>()
        val result = mutableListOf<ListeningPortInfo>()
        val savedSshPort = try {
            context.getSharedPreferences("terminal_theme_prefs", Context.MODE_PRIVATE).getInt("ssh_port", 2222)
        } catch (_: Exception) { 2222 }

        if (!isInstalled) return emptyList()

        if (targetRootDir != null) {
            val containerProcs = collectProcesses(targetRootDir)
            if (containerProcs.isEmpty()) {
                return emptyList()
            }

            val candidatePorts = mutableSetOf<Int>()

            if (com.devwithzachary.completelinuxinstaller.service.ServiceStatusManager.isVncRunning(targetRootDir, containerProcs)) {
                candidatePorts.add(5901)
            }
            if (com.devwithzachary.completelinuxinstaller.service.ServiceStatusManager.isNginxRunning(targetRootDir, containerProcs)) {
                candidatePorts.add(80)
                candidatePorts.add(8080)
            }
            if (com.devwithzachary.completelinuxinstaller.service.ServiceStatusManager.isSshRunning(targetRootDir, savedSshPort, containerProcs)) {
                candidatePorts.add(savedSshPort)
            }

            for (proc in containerProcs) {
                val cmd = proc.cmdline
                val matches = Regex("""(?:-p|--port|:|\brunserver\s+|\bhttp\.server\s+)\s*(\d{2,5})""").findAll(cmd)
                for (m in matches) {
                    val p = m.groupValues[1].toIntOrNull()
                    if (p != null && p in 1..65535) {
                        candidatePorts.add(p)
                    }
                }
            }

            for (candidate in candidatePorts.sorted()) {
                if (isPortOpenLocally(candidate)) {
                    portsFound.add(candidate)
                }
            }
        } else {
            // Global scan across all containers
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

            val rootDirsToCheck = mutableListOf<File>()
            containerManager?.getAllContainers()?.forEach { c ->
                if (c.isInstalled) rootDirsToCheck.add(c.rootDir)
            }
            val pRootDefaultDir = pRootEngine.rootfsDir
            if (pRootDefaultDir.exists() && rootDirsToCheck.none { it.absolutePath == pRootDefaultDir.absolutePath }) {
                rootDirsToCheck.add(pRootDefaultDir)
            }

            val probeCandidatePorts = mutableSetOf(
                22, 2222, savedSshPort, 5900, 5901, 5902, 80, 443, 8080, 3000, 5000, 8000, 8888, 9090, 5432, 3306, 6379, 27017
            )

            val allProcs = collectProcesses(null)
            for (dir in rootDirsToCheck) {
                if (com.devwithzachary.completelinuxinstaller.service.ServiceStatusManager.isVncRunning(dir, allProcs)) {
                    probeCandidatePorts.add(5901)
                }
                if (com.devwithzachary.completelinuxinstaller.service.ServiceStatusManager.isNginxRunning(dir, allProcs)) {
                    probeCandidatePorts.add(80)
                    probeCandidatePorts.add(8080)
                }
                if (com.devwithzachary.completelinuxinstaller.service.ServiceStatusManager.isSshRunning(dir, savedSshPort, allProcs)) {
                    probeCandidatePorts.add(savedSshPort)
                }
            }

            for (proc in allProcs) {
                val cmd = proc.cmdline
                val matches = Regex("""(?:-p|--port|:|\brunserver\s+|\bhttp\.server\s+)\s*(\d{2,5})""").findAll(cmd)
                for (m in matches) {
                    val p = m.groupValues[1].toIntOrNull()
                    if (p != null && p in 1..65535) {
                        probeCandidatePorts.add(p)
                    }
                }
            }

            for (candidate in probeCandidatePorts.sorted()) {
                if (candidate !in portsFound && isPortOpenLocally(candidate)) {
                    portsFound.add(candidate)
                }
            }
        }

        for (port in portsFound.sorted()) {
            val (serviceName, isWeb) = resolveServiceName(port, savedSshPort)
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

    fun resolveServiceName(port: Int, customSshPort: Int = 2222): Pair<String, Boolean> {
        return when (port) {
            22, 2222, customSshPort -> "OpenSSH Server" to false
            5900, 5901, 5902 -> "TigerVNC Desktop (:${port - 5900})" to false
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
                socket.connect(InetSocketAddress("127.0.0.1", port), 100)
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
