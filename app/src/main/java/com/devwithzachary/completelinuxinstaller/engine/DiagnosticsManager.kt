package com.devwithzachary.completelinuxinstaller.engine

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.StatFs
import com.devwithzachary.completelinuxinstaller.BuildConfig
import com.devwithzachary.completelinuxinstaller.model.ContainerInstance
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

data class ContainerDiagnostics(
    val id: String,
    val name: String,
    val distroName: String,
    val rootDirPath: String,
    val isDefault: Boolean,
    val storageUsedMb: Long?,
    val storageSource: String,
    val containerVersionName: String?,
    val containerVersionCode: Int?,
    val distroPrettyName: String?,
    val integrityChecks: List<Pair<String, Boolean>>
)

data class DiagnosticsInfo(
    val appVersionName: String,
    val appVersionCode: Int,
    val deviceManufacturer: String,
    val deviceModel: String,
    val androidRelease: String,
    val androidSdkInt: Int,
    val deviceAbi: String,
    val rootfsPath: String,
    val rootfsInstalled: Boolean,
    val storageUsedMb: Long?,
    val storageSource: String,
    val totalStorageBytes: Long?,
    val freeStorageBytes: Long?,
    val totalRamMb: Long?,
    val freeRamMb: Long?,
    val sessionRunning: Boolean,
    val containerVersionName: String?,
    val containerVersionCode: Int?,
    val distroPrettyName: String?,
    val integrityChecks: List<Pair<String, Boolean>>,
    val containers: List<ContainerDiagnostics> = emptyList(),
    val prootBinaryExists: Boolean = true,
    val startupElapsedMs: Long? = null,
    val timestampMs: Long
)

class DiagnosticsManager(
    private val context: Context,
    private val pRootEngine: PRootEngine,
    private val rootfsManager: RootfsManager,
    private val containerManager: ContainerManager? = null
) {

    suspend fun collect(sessionRunning: Boolean, startupElapsedMs: Long? = null): DiagnosticsInfo =
        withContext(Dispatchers.IO) {
            val installedContainers = containerManager?.getAllContainers() ?: emptyList()
            val hasMultipleContainers = installedContainers.isNotEmpty()

            val containerDiagnosticsList = mutableListOf<ContainerDiagnostics>()
            for (container in installedContainers) {
                val dir = File(container.rootDirPath)
                val isInstalled = dir.exists() && dir.isDirectory

                var sizeMb: Long? = null
                var source = ""
                if (isInstalled) {
                    val fresh = withTimeoutOrNull(FRESH_SCAN_TIMEOUT_MS) { rootfsManager.getStorageUsedMbForDir(dir) }
                    if (fresh != null && fresh > 0L) {
                        sizeMb = fresh
                        source = "fresh scan"
                    } else if (container.storageUsedMb > 0L) {
                        sizeMb = container.storageUsedMb
                        source = "cached"
                    }
                }

                val version = if (isInstalled) safely(null) { rootfsManager.getRootfsVersion(dir) } else null
                val prettyName = if (isInstalled) safely(null) { readPrettyName(File(dir, "etc/os-release")) } else null

                val checks = checkContainerIntegrity(dir)

                containerDiagnosticsList.add(
                    ContainerDiagnostics(
                        id = container.id,
                        name = container.name,
                        distroName = container.distroName,
                        rootDirPath = container.rootDirPath,
                        isDefault = container.isDefault,
                        storageUsedMb = sizeMb,
                        storageSource = source,
                        containerVersionName = version?.versionName,
                        containerVersionCode = version?.versionCode,
                        distroPrettyName = prettyName,
                        integrityChecks = checks
                    )
                )
            }

            // Legacy fallback inspection for default rootfsDir
            val legacyDir = pRootEngine.rootfsDir
            val legacyInstalled = safely(false) { rootfsManager.isInstalled() }
            var legacyStorageMb: Long? = null
            var legacySource = ""
            if (legacyInstalled) {
                val fresh = withTimeoutOrNull(FRESH_SCAN_TIMEOUT_MS) { rootfsManager.getStorageUsedMb() }
                if (fresh != null) {
                    legacyStorageMb = fresh
                    legacySource = "fresh scan"
                } else {
                    legacyStorageMb = safely(0L) { rootfsManager.getCachedStorageUsedMb() }
                    legacySource = "cached"
                }
            }

            var totalStorageBytes: Long? = null
            var freeStorageBytes: Long? = null
            safely(null) {
                val stat = StatFs(context.filesDir.absolutePath)
                totalStorageBytes = stat.totalBytes
                freeStorageBytes = stat.availableBytes
            }

            var totalRamMb: Long? = null
            var freeRamMb: Long? = null
            safely(null) {
                val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                val memoryInfo = ActivityManager.MemoryInfo()
                activityManager.getMemoryInfo(memoryInfo)
                totalRamMb = memoryInfo.totalMem / BYTES_PER_MB
                freeRamMb = memoryInfo.availMem / BYTES_PER_MB
            }

            val defaultContainer = containerDiagnosticsList.find { it.isDefault } ?: containerDiagnosticsList.firstOrNull()
            val primaryDir = if (defaultContainer != null) File(defaultContainer.rootDirPath) else legacyDir
            val primaryInstalled = if (defaultContainer != null) defaultContainer.storageUsedMb != null else legacyInstalled
            val primaryStorageMb = defaultContainer?.storageUsedMb ?: legacyStorageMb
            val primarySource = defaultContainer?.storageSource ?: legacySource
            val primaryVersionName = defaultContainer?.containerVersionName ?: (if (legacyInstalled) safely(null) { rootfsManager.getRootfsVersion() }?.versionName else null)
            val primaryVersionCode = defaultContainer?.containerVersionCode ?: (if (legacyInstalled) safely(null) { rootfsManager.getRootfsVersion() }?.versionCode else null)
            val primaryPrettyName = defaultContainer?.distroPrettyName ?: (if (legacyInstalled) safely(null) { readPrettyName(File(legacyDir, "etc/os-release")) } else null)
            val primaryIntegrity = if (defaultContainer != null) defaultContainer.integrityChecks else checkContainerIntegrity(legacyDir)

            val prootExec = pRootEngine.prootBinary
            val prootOk = prootExec.exists() && prootExec.canExecute()

            DiagnosticsInfo(
                appVersionName = BuildConfig.VERSION_NAME,
                appVersionCode = BuildConfig.VERSION_CODE,
                deviceManufacturer = Build.MANUFACTURER ?: "unknown",
                deviceModel = Build.MODEL ?: "unknown",
                androidRelease = Build.VERSION.RELEASE ?: "unknown",
                androidSdkInt = Build.VERSION.SDK_INT,
                deviceAbi = Build.SUPPORTED_ABIS?.firstOrNull() ?: "unknown",
                rootfsPath = primaryDir.absolutePath,
                rootfsInstalled = primaryInstalled,
                storageUsedMb = primaryStorageMb?.takeIf { it > 0L },
                storageSource = primarySource,
                totalStorageBytes = totalStorageBytes,
                freeStorageBytes = freeStorageBytes,
                totalRamMb = totalRamMb,
                freeRamMb = freeRamMb,
                sessionRunning = sessionRunning,
                containerVersionName = primaryVersionName,
                containerVersionCode = primaryVersionCode,
                distroPrettyName = primaryPrettyName,
                integrityChecks = primaryIntegrity,
                containers = containerDiagnosticsList,
                prootBinaryExists = prootOk,
                startupElapsedMs = startupElapsedMs,
                timestampMs = System.currentTimeMillis()
            )
        }

    private fun checkContainerIntegrity(dir: File): List<Pair<String, Boolean>> {
        val hasShell = File(dir, "bin/sh").exists() ||
                File(dir, "bin/bash").exists() ||
                File(dir, "bin/ash").exists() ||
                File(dir, "usr/bin/sh").exists() ||
                File(dir, "usr/bin/bash").exists()

        val hasOsRelease = File(dir, "etc/os-release").exists() ||
                File(dir, "usr/lib/os-release").exists() ||
                File(dir, "etc/issue").exists()

        return listOf(
            "rootfs directory" to safely(false) { dir.exists() && dir.isDirectory },
            "shell (bin/sh, bash, ash)" to safely(false) { hasShell },
            "etc/os-release" to safely(false) { hasOsRelease },
            "etc/resolv.conf" to safely(false) { File(dir, "etc/resolv.conf").exists() },
            "etc/passwd" to safely(false) { File(dir, "etc/passwd").exists() },
            "etc/group" to safely(false) { File(dir, "etc/group").exists() },
            "etc/hosts" to safely(false) { File(dir, "etc/hosts").exists() },
            "version metadata" to safely(false) { File(dir, "etc/linuxonandroid_version").exists() }
        )
    }

    private fun readPrettyName(osReleaseFile: File): String? {
        if (!osReleaseFile.exists()) return null
        return Regex("""PRETTY_NAME=["']?([^"'\n]+)["']?""").find(osReleaseFile.readText())?.groupValues?.get(1)?.trim()
    }

    private fun <T> safely(default: T, block: () -> T): T = try {
        block()
    } catch (_: Exception) {
        default
    }

    companion object {
        private const val FRESH_SCAN_TIMEOUT_MS = 5_000L
        private const val BYTES_PER_MB = 1024L * 1024L

        fun buildReport(info: DiagnosticsInfo): String {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss 'UTC'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            return buildString {
                appendLine("=== LinuxOnAndroid Debug Report ===")
                appendLine("Generated: ${dateFormat.format(Date(info.timestampMs))}")
                appendLine("Review before sharing. This report contains no passwords or personal files.")
                appendLine()
                appendLine("--- App ---")
                appendLine("App version: ${info.appVersionName} (Build ${info.appVersionCode})")
                appendLine("PRoot binary: ${if (info.prootBinaryExists) "Available & Executable" else "Missing / Not Executable"}")
                appendLine()
                appendLine("--- Device ---")
                appendLine("Device: ${info.deviceManufacturer} ${info.deviceModel}")
                appendLine("Android version: ${info.androidRelease} (SDK ${info.androidSdkInt})")
                appendLine("ABI: ${info.deviceAbi}")
                appendLine()
                appendLine("--- Session & Host Resources ---")
                appendLine("Session running: ${if (info.sessionRunning) "Yes" else "No"}")
                info.startupElapsedMs?.let {
                    appendLine("Startup duration: $it ms")
                }
                appendLine("Volume total: ${gigabytesLabel(info.totalStorageBytes)}")
                appendLine("Volume free: ${gigabytesLabel(info.freeStorageBytes)}")
                appendLine("Total RAM: ${megabytesLabel(info.totalRamMb)}")
                appendLine("Available RAM: ${megabytesLabel(info.freeRamMb)}")
                appendLine()

                if (info.containers.isNotEmpty()) {
                    appendLine("--- Installed RootFS Containers (${info.containers.size}) ---")
                    info.containers.forEachIndexed { index, container ->
                        val defaultTag = if (container.isDefault) " [DEFAULT]" else ""
                        appendLine("[${index + 1}] ${container.name}$defaultTag")
                        appendLine("  ID: ${container.id}")
                        appendLine("  Distribution: ${container.distroPrettyName ?: container.distroName}")
                        appendLine("  Path: ${container.rootDirPath}")
                        appendLine("  Size: ${containerSizeLabel(container)}")
                        appendLine("  Build: ${containerVersionLabel(container)}")
                        appendLine("  Integrity Checks:")
                        for ((checkName, ok) in container.integrityChecks) {
                            appendLine("    • $checkName: ${if (ok) "OK" else "MISSING"}")
                        }
                        appendLine()
                    }
                } else {
                    appendLine("--- Container ---")
                    appendLine("Status: ${if (info.rootfsInstalled) "Installed" else "Not installed"}")
                    appendLine("Distribution: ${info.distroPrettyName ?: "Unknown"}")
                    appendLine("Container build: ${containerBuildLabel(info)}")
                    appendLine("Install path: ${info.rootfsPath}")
                    appendLine("RootFS size: ${sizeLabel(info)}")
                    appendLine()
                    appendLine("--- Integrity Checks ---")
                    for ((name, ok) in info.integrityChecks) {
                        appendLine("$name: ${if (ok) "OK" else "MISSING"}")
                    }
                    appendLine()
                }
            }
        }

        private fun containerVersionLabel(c: ContainerDiagnostics): String = when {
            c.containerVersionName == null -> "Legacy v1.0.0"
            else -> "v${c.containerVersionName} (Build ${c.containerVersionCode})"
        }

        private fun containerSizeLabel(c: ContainerDiagnostics): String {
            val usedMb = c.storageUsedMb ?: return "unavailable"
            val source = c.storageSource.takeIf { it.isNotBlank() } ?: return "$usedMb MB"
            return "$usedMb MB ($source)"
        }

        private fun containerBuildLabel(info: DiagnosticsInfo): String = when {
            !info.rootfsInstalled -> "None"
            info.containerVersionName == null -> "Legacy v1.0.0"
            else -> "v${info.containerVersionName} (Build ${info.containerVersionCode})"
        }

        private fun sizeLabel(info: DiagnosticsInfo): String {
            val usedMb = info.storageUsedMb ?: return "unavailable"
            val source = info.storageSource.takeIf { it.isNotBlank() } ?: return "$usedMb MB"
            return "$usedMb MB ($source)"
        }

        private fun gigabytesLabel(bytes: Long?): String =
            bytes?.let { String.format(Locale.US, "%.1f GB", it / 1_000_000_000.0) } ?: "unavailable"

        private fun megabytesLabel(mb: Long?): String = mb?.let { "$it MB" } ?: "unavailable"
    }
}
