package com.devwithzachary.completelinuxinstaller.engine

import android.util.Log
import com.devwithzachary.completelinuxinstaller.model.SoftwarePackage
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class SinglePackageTestResult(
    val packageId: String,
    val packageName: String,
    val passed: Boolean,
    val durationMs: Long,
    val missingBinaries: List<String> = emptyList(),
    val errorMessage: String? = null,
    val logs: String = ""
)

data class PackageTestReport(
    val totalCount: Int,
    val passedCount: Int,
    val failedCount: Int,
    val totalDurationMs: Long,
    val results: List<SinglePackageTestResult>
) {
    val allPassed: Boolean get() = failedCount == 0
}

class PackageTestRunner(
    private val pRootEngine: PRootEngine,
    private val softwareInstaller: SoftwareInstaller
) {
    companion object {
        private const val TAG = "PackageTestRunner"
    }

    suspend fun runSingleTest(pkg: SoftwarePackage): SinglePackageTestResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        Log.i(TAG, "Starting automated test for 1-Click Package: ${pkg.name} (${pkg.id})")

        val logsBuilder = StringBuilder()
        var installError: String? = null
        var isSuccess = false

        try {
            softwareInstaller.installPackage(pkg).collect { state ->
                when (state) {
                    is InstallStepState.Progress -> {
                        logsBuilder.append(state.logLine).append("\n")
                    }
                    is InstallStepState.Success -> {
                        isSuccess = true
                    }
                    is InstallStepState.Error -> {
                        isSuccess = false
                        installError = state.errorMessage
                    }
                }
            }
        } catch (e: Exception) {
            isSuccess = false
            installError = e.message ?: "Unknown exception during installation execution"
        }

        // Verify expected binaries in rootfs
        val rootfsDir = pRootEngine.rootfsDir
        val missing = pkg.expectedBinaries.filter { relPath ->
            !SoftwarePackage.isBinaryPresent(rootfsDir, relPath)
        }

        val finalPassed = isSuccess && missing.isEmpty()
        val duration = System.currentTimeMillis() - startTime
        val logs = logsBuilder.toString()

        Log.i(TAG, "Completed test for ${pkg.id}: passed=$finalPassed, duration=${duration}ms, missingBinaries=$missing")

        SinglePackageTestResult(
            packageId = pkg.id,
            packageName = pkg.name,
            passed = finalPassed,
            durationMs = duration,
            missingBinaries = missing,
            errorMessage = installError ?: if (missing.isNotEmpty()) "Missing expected binaries: $missing" else null,
            logs = logs
        )
    }

    suspend fun runAllTests(packages: List<SoftwarePackage> = SoftwarePackage.getPresets()): PackageTestReport = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val testResults = mutableListOf<SinglePackageTestResult>()

        for (pkg in packages) {
            val result = runSingleTest(pkg)
            testResults.add(result)
        }

        val totalDuration = System.currentTimeMillis() - startTime
        val passed = testResults.count { it.passed }
        val failed = testResults.count { !it.passed }

        PackageTestReport(
            totalCount = testResults.size,
            passedCount = passed,
            failedCount = failed,
            totalDurationMs = totalDuration,
            results = testResults
        )
    }
}
