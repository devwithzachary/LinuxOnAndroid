package com.devwithzachary.completelinuxinstaller.engine

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.StatFs
import com.devwithzachary.completelinuxinstaller.BuildConfig
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

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
    val startupElapsedMs: Long? = null,
    val timestampMs: Long
)

class DiagnosticsManager(
    private val context: Context,
    private val pRootEngine: PRootEngine,
    private val rootfsManager: RootfsManager
) {

    suspend fun collect(sessionRunning: Boolean, startupElapsedMs: Long? = null): DiagnosticsInfo =
        withContext(Dispatchers.IO) {
            val rootfsDir = pRootEngine.rootfsDir
            val installed = safely(false) { rootfsManager.isInstalled() }

            var storageUsedMb: Long? = null
            var storageSource = ""
            if (installed) {
                val fresh = withTimeoutOrNull(FRESH_SCAN_TIMEOUT_MS) { rootfsManager.getStorageUsedMb() }
                if (fresh != null) {
                    storageUsedMb = fresh
                    storageSource = "fresh scan"
                } else {
                    storageUsedMb = safely(0L) { rootfsManager.getCachedStorageUsedMb() }
                    storageSource = "cached"
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

            val containerVersion = if (installed) safely(null) { rootfsManager.getRootfsVersion() } else null

            val integrityChecks = listOf(
                "rootfs directory" to safely(false) { rootfsDir.isDirectory },
                "bin/sh" to safely(false) { File(rootfsDir, "bin/sh").exists() },
                "etc/os-release" to safely(false) { File(rootfsDir, "etc/os-release").exists() },
                "etc/linuxonandroid_version" to safely(false) { File(rootfsDir, "etc/linuxonandroid_version").exists() }
            )

            val distroPrettyName = if (installed) safely(null) { readPrettyName(File(rootfsDir, "etc/os-release")) } else null

            DiagnosticsInfo(
                appVersionName = BuildConfig.VERSION_NAME,
                appVersionCode = BuildConfig.VERSION_CODE,
                deviceManufacturer = Build.MANUFACTURER ?: "unknown",
                deviceModel = Build.MODEL ?: "unknown",
                androidRelease = Build.VERSION.RELEASE ?: "unknown",
                androidSdkInt = Build.VERSION.SDK_INT,
                deviceAbi = Build.SUPPORTED_ABIS?.firstOrNull() ?: "unknown",
                rootfsPath = rootfsDir.absolutePath,
                rootfsInstalled = installed,
                storageUsedMb = storageUsedMb?.takeIf { it > 0L },
                storageSource = storageSource,
                totalStorageBytes = totalStorageBytes,
                freeStorageBytes = freeStorageBytes,
                totalRamMb = totalRamMb,
                freeRamMb = freeRamMb,
                sessionRunning = sessionRunning,
                containerVersionName = containerVersion?.versionName,
                containerVersionCode = containerVersion?.versionCode,
                distroPrettyName = distroPrettyName,
                integrityChecks = integrityChecks,
                startupElapsedMs = startupElapsedMs,
                timestampMs = System.currentTimeMillis()
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
                appendLine()
                appendLine("--- Device ---")
                appendLine("Device: ${info.deviceManufacturer} ${info.deviceModel}")
                appendLine("Android version: ${info.androidRelease} (SDK ${info.androidSdkInt})")
                appendLine("ABI: ${info.deviceAbi}")
                appendLine()
                appendLine("--- Container ---")
                appendLine("Status: ${if (info.rootfsInstalled) "Installed" else "Not installed"}")
                appendLine("Distribution: ${info.distroPrettyName ?: "Unknown"}")
                appendLine("Container build: ${containerBuildLabel(info)}")
                appendLine("Install path: ${info.rootfsPath}")
                appendLine("Session running: ${if (info.sessionRunning) "Yes" else "No"}")
                info.startupElapsedMs?.let {
                    appendLine("Startup duration: $it ms")
                }
                appendLine()
                appendLine("--- Storage ---")
                appendLine("RootFS size: ${sizeLabel(info)}")
                appendLine("Volume total: ${gigabytesLabel(info.totalStorageBytes)}")
                appendLine("Volume free: ${gigabytesLabel(info.freeStorageBytes)}")
                appendLine()
                appendLine("--- Memory ---")
                appendLine("Total RAM: ${megabytesLabel(info.totalRamMb)}")
                appendLine("Available RAM: ${megabytesLabel(info.freeRamMb)}")
                appendLine()
                appendLine("--- Integrity Checks ---")
                for ((name, ok) in info.integrityChecks) {
                    appendLine("$name: ${if (ok) "OK" else "MISSING"}")
                }
            }
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
