package com.devwithzachary.completelinuxinstaller.service

import java.io.File
import java.net.InetSocketAddress
import java.net.Socket

data class ContainerResourceStatus(
    val isTerminalActive: Boolean = false,
    val isSshActive: Boolean = false,
    val sshPort: Int = 2222,
    val isVncActive: Boolean = false,
    val isNginxActive: Boolean = false,
    val memoryUsedMb: Long = 0L
) {
    val hasActiveServices: Boolean
        get() = isTerminalActive || isSshActive || isVncActive || isNginxActive

    fun buildSummaryText(): String {
        val activeParts = mutableListOf<String>()

        if (isTerminalActive) {
            activeParts.add("Terminal Active")
        }
        if (isSshActive) {
            activeParts.add("SSH :$sshPort")
        }
        if (isVncActive) {
            activeParts.add("VNC :5901")
        }
        if (isNginxActive) {
            activeParts.add("NGINX :80")
        }

        if (activeParts.isEmpty()) {
            activeParts.add("Container Standby")
        }

        val memSuffix = if (memoryUsedMb > 0) " • RAM: ${memoryUsedMb}MB" else ""
        return activeParts.joinToString(" | ") + memSuffix
    }
}

object ServiceStatusManager {

    fun isPortListening(port: Int, timeoutMs: Int = 100): Boolean {
        if (port !in 1..65535) return false
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress("127.0.0.1", port), timeoutMs)
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    fun isPidAlive(pid: Int, expectedName: String? = null): Boolean {
        if (pid <= 0) return false
        val procDir = File("/proc/$pid")
        if (!procDir.exists() || !procDir.isDirectory) return false
        if (expectedName != null) {
            val cmdlineFile = File(procDir, "cmdline")
            val commFile = File(procDir, "comm")
            val cmdline = if (cmdlineFile.canRead()) {
                try { cmdlineFile.readText() } catch (_: Exception) { "" }
            } else ""
            val comm = if (commFile.canRead()) {
                try { commFile.readText().trim() } catch (_: Exception) { "" }
            } else ""
            if (!cmdline.contains(expectedName, ignoreCase = true) && !comm.contains(expectedName, ignoreCase = true)) {
                return false
            }
        }
        return true
    }

    fun isVncRunning(rootfsDir: File?, containerProcesses: List<com.devwithzachary.completelinuxinstaller.engine.ContainerProcessInfo>? = null): Boolean {
        if (rootfsDir == null || !rootfsDir.exists()) {
            return isPortListening(5901)
        }
        if (containerProcesses != null && containerProcesses.any { 
            it.name.contains("TigerVNC", ignoreCase = true) || it.cmdline.contains("Xtigervnc", ignoreCase = true) || it.cmdline.contains("vncserver", ignoreCase = true)
        }) {
            return true
        }
        val lock = File(rootfsDir, "tmp/.X1-lock")
        val x11Unix = File(rootfsDir, "tmp/.X11-unix/X1")
        if (lock.exists()) {
            val pid = try { lock.readText().trim().toIntOrNull() } catch (_: Exception) { null }
            if (pid != null && isPidAlive(pid, "Xtigervnc")) {
                return true
            } else {
                try {
                    lock.delete()
                    x11Unix.delete()
                } catch (_: Exception) {}
            }
        }
        return false
    }

    fun isNginxRunning(rootfsDir: File?, containerProcesses: List<com.devwithzachary.completelinuxinstaller.engine.ContainerProcessInfo>? = null): Boolean {
        if (rootfsDir == null || !rootfsDir.exists()) {
            return isPortListening(80) || isPortListening(8080)
        }
        if (containerProcesses != null && containerProcesses.any { 
            it.name.contains("NGINX", ignoreCase = true) || it.cmdline.contains("nginx", ignoreCase = true)
        }) {
            return isPortListening(80) || isPortListening(8080)
        }
        val pidFile = File(rootfsDir, "run/nginx.pid").let { if (it.exists()) it else File(rootfsDir, "var/run/nginx.pid") }
        if (pidFile.exists()) {
            val pid = try { pidFile.readText().trim().toIntOrNull() } catch (_: Exception) { null }
            if (pid != null && isPidAlive(pid, "nginx")) {
                return true
            } else {
                try { pidFile.delete() } catch (_: Exception) {}
            }
        }
        return false
    }

    fun isSshRunning(rootfsDir: File?, sshPort: Int, containerProcesses: List<com.devwithzachary.completelinuxinstaller.engine.ContainerProcessInfo>? = null): Boolean {
        if (rootfsDir == null || !rootfsDir.exists()) {
            return isPortListening(sshPort)
        }
        if (containerProcesses != null && containerProcesses.any { 
            it.name.contains("OpenSSH", ignoreCase = true) || it.cmdline.contains("sshd", ignoreCase = true)
        }) {
            return isPortListening(sshPort)
        }
        val pidFile = File(rootfsDir, "run/sshd.pid").let { if (it.exists()) it else File(rootfsDir, "var/run/sshd.pid") }
        if (pidFile.exists()) {
            val pid = try { pidFile.readText().trim().toIntOrNull() } catch (_: Exception) { null }
            if (pid != null && isPidAlive(pid, "sshd")) {
                return true
            } else {
                try { pidFile.delete() } catch (_: Exception) {}
            }
        }
        return false
    }

    fun getAppMemoryMb(): Long {
        val runtime = Runtime.getRuntime()
        val usedBytes = runtime.totalMemory() - runtime.freeMemory()
        return (usedBytes / (1024 * 1024)).coerceAtLeast(1L)
    }

    fun getTotalContainerMemoryMb(): Long {
        return try {
            val procDir = File("/proc")
            val pageSize = try {
                android.system.Os.sysconf(android.system.OsConstants._SC_PAGESIZE)
            } catch (_: Throwable) {
                4096L
            }
            var totalRssPages = 0L
            val pids = procDir.listFiles { file -> file.isDirectory && file.name.all { it.isDigit() } }
            pids?.forEach { pidDir ->
                try {
                    val statmFile = File(pidDir, "statm")
                    if (statmFile.canRead()) {
                        val line = statmFile.readText().trim()
                        val tokens = line.split("\\s+".toRegex())
                        if (tokens.size >= 2) {
                            val rssPages = tokens[1].toLongOrNull() ?: 0L
                            totalRssPages += rssPages
                        }
                    }
                } catch (_: Exception) {}
            }
            val totalBytes = totalRssPages * pageSize
            val totalMb = totalBytes / (1024 * 1024)
            if (totalMb > 0) totalMb else getAppMemoryMb()
        } catch (_: Exception) {
            getAppMemoryMb()
        }
    }

    fun checkStatus(
        isTerminalActive: Boolean,
        rootfsDir: File?,
        sshPort: Int
    ): ContainerResourceStatus {
        return ContainerResourceStatus(
            isTerminalActive = isTerminalActive,
            isSshActive = isSshRunning(rootfsDir, sshPort),
            sshPort = sshPort,
            isVncActive = isVncRunning(rootfsDir),
            isNginxActive = isNginxRunning(rootfsDir),
            memoryUsedMb = getTotalContainerMemoryMb()
        )
    }
}
