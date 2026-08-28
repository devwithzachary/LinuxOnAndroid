package com.devwithzachary.completelinuxinstaller.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticsManagerTest {

    private fun fullInfo() = DiagnosticsInfo(
        appVersionName = "1.3.0",
        appVersionCode = 10,
        deviceManufacturer = "Google",
        deviceModel = "Pixel 8",
        androidRelease = "14",
        androidSdkInt = 34,
        deviceAbi = "arm64-v8a",
        rootfsPath = "/data/user/0/com.example/files/ubuntu_rootfs",
        rootfsInstalled = true,
        storageUsedMb = 1536L,
        storageSource = "fresh scan",
        totalStorageBytes = 128_849_018_880L,
        freeStorageBytes = 64_424_509_440L,
        totalRamMb = 8192L,
        freeRamMb = 2048L,
        sessionRunning = true,
        containerVersionName = "1.2.0",
        containerVersionCode = 9,
        distroPrettyName = "Ubuntu 26.04 LTS",
        integrityChecks = listOf(
            "rootfs directory" to true,
            "bin/sh" to false
        ),
        timestampMs = 0L
    )

    @Test
    fun testFullReport_containsAllSections() {
        val report = DiagnosticsManager.buildReport(fullInfo())

        assertTrue(report.contains("LinuxOnAndroid Debug Report"))
        assertTrue(report.contains("--- App ---"))
        assertTrue(report.contains("--- Device ---"))
        assertTrue(report.contains("--- Container ---"))
        assertTrue(report.contains("--- Storage ---"))
        assertTrue(report.contains("--- Memory ---"))
        assertTrue(report.contains("--- Integrity Checks ---"))
    }

    @Test
    fun testFullReport_containsValues() {
        val report = DiagnosticsManager.buildReport(fullInfo())

        assertTrue(report.contains("App version: 1.3.0 (Build 10)"))
        assertTrue(report.contains("Google Pixel 8"))
        assertTrue(report.contains("Android version: 14 (SDK 34)"))
        assertTrue(report.contains("ABI: arm64-v8a"))
        assertTrue(report.contains("/data/user/0/com.example/files/ubuntu_rootfs"))
        assertTrue(report.contains("Status: Installed"))
        assertTrue(report.contains("Distribution: Ubuntu 26.04 LTS"))
        assertTrue(report.contains("Container build: v1.2.0 (Build 9)"))
        assertTrue(report.contains("Session running: Yes"))
        assertTrue(report.contains("RootFS size: 1536 MB (fresh scan)"))
        assertTrue(report.contains("Volume total: 128.8 GB"))
        assertTrue(report.contains("Volume free: 64.4 GB"))
        assertTrue(report.contains("Total RAM: 8192 MB"))
        assertTrue(report.contains("Available RAM: 2048 MB"))
        assertTrue(report.contains("rootfs directory: OK"))
        assertTrue(report.contains("bin/sh: MISSING"))
    }

    @Test
    fun testNotInstalled_reportDegradesGracefully() {
        val info = fullInfo().copy(
            rootfsInstalled = false,
            storageUsedMb = null,
            storageSource = "",
            containerVersionName = null,
            containerVersionCode = null,
            distroPrettyName = null,
            sessionRunning = false
        )
        val report = DiagnosticsManager.buildReport(info)

        assertTrue(report.contains("Status: Not installed"))
        assertTrue(report.contains("Distribution: Unknown"))
        assertTrue(report.contains("Container build: None"))
        assertTrue(report.contains("Session running: No"))
        assertTrue(report.contains("RootFS size: unavailable"))
    }

    @Test
    fun testLegacyContainerWithoutVersionFile() {
        val info = fullInfo().copy(
            containerVersionName = null,
            containerVersionCode = null
        )

        val report = DiagnosticsManager.buildReport(info)

        assertTrue(report.contains("Container build: Legacy v1.0.0"))
        assertFalse(report.contains("(Build 9)"))
    }

    @Test
    fun testNullOptionalFieldsRenderAsUnavailable() {
        val info = fullInfo().copy(
            totalStorageBytes = null,
            freeStorageBytes = null,
            totalRamMb = null,
            freeRamMb = null,
            storageUsedMb = null,
            storageSource = ""
        )

        val report = DiagnosticsManager.buildReport(info)

        assertTrue(report.contains("RootFS size: unavailable"))
        assertTrue(report.contains("Volume total: unavailable"))
        assertTrue(report.contains("Volume free: unavailable"))
        assertTrue(report.contains("Total RAM: unavailable"))
        assertTrue(report.contains("Available RAM: unavailable"))
    }

    @Test
    fun testStartupSection_onlyWhenMeasured() {
        val without = DiagnosticsManager.buildReport(fullInfo())
        assertFalse(without.contains("Startup"))

        val with = DiagnosticsManager.buildReport(fullInfo().copy(startupElapsedMs = 1234L))
        assertTrue(with.contains("Startup duration: 1234 ms"))
    }

    @Test
    fun testCachedSizeSourceIsLabeled() {
        val report = DiagnosticsManager.buildReport(fullInfo().copy(storageSource = "cached"))

        assertTrue(report.contains("RootFS size: 1536 MB (cached)"))
    }

    @Test
    fun testTimestampFormatting() {
        val report = DiagnosticsManager.buildReport(fullInfo().copy(timestampMs = 0L))

        assertEquals(true, Regex("Generated: \\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2} UTC").containsMatchIn(report))
    }
}
