package com.devwithzachary.completelinuxinstaller.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticsManagerTest {

    private fun fullInfo() = DiagnosticsInfo(
        appVersionName = "1.5.0",
        appVersionCode = 12,
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
        containerVersionName = "1.5.0",
        containerVersionCode = 12,
        distroPrettyName = "Ubuntu 26.04 LTS",
        integrityChecks = listOf(
            "rootfs directory" to true,
            "bin/sh" to false
        ),
        containers = listOf(
            ContainerDiagnostics(
                id = "alpine_1",
                name = "Alpine Linux",
                distroName = "Alpine Linux 3.20",
                rootDirPath = "/data/user/0/com.example/files/containers/alpine_1/rootfs",
                isDefault = true,
                storageUsedMb = 15L,
                storageSource = "fresh scan",
                containerVersionName = "1.5.0",
                containerVersionCode = 12,
                distroPrettyName = "Alpine Linux v3.20",
                integrityChecks = listOf(
                    "rootfs directory" to true,
                    "shell (bin/sh, bash, ash)" to true,
                    "etc/os-release" to true,
                    "etc/resolv.conf" to true,
                    "etc/passwd" to true,
                    "etc/group" to true,
                    "etc/hosts" to true,
                    "version metadata" to true
                )
            ),
            ContainerDiagnostics(
                id = "ubuntu_default",
                name = "Ubuntu 26.04",
                distroName = "Ubuntu 26.04 LTS",
                rootDirPath = "/data/user/0/com.example/files/ubuntu_rootfs",
                isDefault = false,
                storageUsedMb = 1536L,
                storageSource = "cached",
                containerVersionName = "1.4.0",
                containerVersionCode = 11,
                distroPrettyName = "Ubuntu 26.04 LTS",
                integrityChecks = listOf(
                    "rootfs directory" to true,
                    "shell (bin/sh, bash, ash)" to true,
                    "etc/os-release" to true,
                    "etc/resolv.conf" to true,
                    "etc/passwd" to true,
                    "etc/group" to true,
                    "etc/hosts" to true,
                    "version metadata" to false
                )
            )
        ),
        prootBinaryExists = true,
        timestampMs = 0L
    )

    @Test
    fun testMultiContainerReport_containsAllSections() {
        val report = DiagnosticsManager.buildReport(fullInfo())

        assertTrue(report.contains("=== LinuxOnAndroid Debug Report ==="))
        assertTrue(report.contains("--- App ---"))
        assertTrue(report.contains("--- Device ---"))
        assertTrue(report.contains("--- Session & Host Resources ---"))
        assertTrue(report.contains("--- Installed RootFS Containers (2) ---"))
        assertTrue(report.contains("[1] Alpine Linux [DEFAULT]"))
        assertTrue(report.contains("ID: alpine_1"))
        assertTrue(report.contains("Distribution: Alpine Linux v3.20"))
        assertTrue(report.contains("Size: 15 MB (fresh scan)"))
        assertTrue(report.contains("Build: v1.5.0 (Build 12)"))
        assertTrue(report.contains("• shell (bin/sh, bash, ash): OK"))

        assertTrue(report.contains("[2] Ubuntu 26.04"))
        assertTrue(report.contains("ID: ubuntu_default"))
        assertTrue(report.contains("Size: 1536 MB (cached)"))
        assertTrue(report.contains("Build: v1.4.0 (Build 11)"))
        assertTrue(report.contains("• version metadata: MISSING"))
    }

    @Test
    fun testFullReport_containsValues() {
        val report = DiagnosticsManager.buildReport(fullInfo())

        assertTrue(report.contains("App version: 1.5.0 (Build 12)"))
        assertTrue(report.contains("PRoot binary: Available & Executable"))
        assertTrue(report.contains("Google Pixel 8"))
        assertTrue(report.contains("Android version: 14 (SDK 34)"))
        assertTrue(report.contains("ABI: arm64-v8a"))
        assertTrue(report.contains("Session running: Yes"))
        assertTrue(report.contains("Volume total: 128.8 GB"))
        assertTrue(report.contains("Volume free: 64.4 GB"))
        assertTrue(report.contains("Total RAM: 8192 MB"))
        assertTrue(report.contains("Available RAM: 2048 MB"))
    }

    @Test
    fun testLegacyReport_withoutContainersList() {
        val info = fullInfo().copy(containers = emptyList())
        val report = DiagnosticsManager.buildReport(info)

        assertTrue(report.contains("--- Container ---"))
        assertTrue(report.contains("Status: Installed"))
        assertTrue(report.contains("Distribution: Ubuntu 26.04 LTS"))
        assertTrue(report.contains("Container build: v1.5.0 (Build 12)"))
        assertTrue(report.contains("Install path: /data/user/0/com.example/files/ubuntu_rootfs"))
        assertTrue(report.contains("RootFS size: 1536 MB (fresh scan)"))
        assertTrue(report.contains("--- Integrity Checks ---"))
        assertTrue(report.contains("rootfs directory: OK"))
        assertTrue(report.contains("bin/sh: MISSING"))
    }

    @Test
    fun testNotInstalled_reportDegradesGracefully() {
        val info = fullInfo().copy(
            containers = emptyList(),
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
    fun testStartupSection_onlyWhenMeasured() {
        val without = DiagnosticsManager.buildReport(fullInfo())
        assertFalse(without.contains("Startup"))

        val with = DiagnosticsManager.buildReport(fullInfo().copy(startupElapsedMs = 1234L))
        assertTrue(with.contains("Startup duration: 1234 ms"))
    }

    @Test
    fun testTimestampFormatting() {
        val report = DiagnosticsManager.buildReport(fullInfo().copy(timestampMs = 0L))

        assertEquals(true, Regex("Generated: \\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2} UTC").containsMatchIn(report))
    }
}
