package com.devwithzachary.completelinuxinstaller.engine

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class RootfsMigrationTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testReadVersion_nonExistentRootfs_returnsNull() {
        val rootfsDir = tempFolder.newFolder("empty_rootfs")
        val version = RootfsMigrationManager.readVersion(rootfsDir)
        assertNull("Empty directory without bin or usr should return null version", version)
    }

    @Test
    fun testReadVersion_legacyRootfs_returnsLegacyVersion1() {
        val rootfsDir = tempFolder.newFolder("legacy_rootfs")
        File(rootfsDir, "bin").mkdirs()
        File(rootfsDir, "usr").mkdirs()

        val version = RootfsMigrationManager.readVersion(rootfsDir)
        assertNotNull("Legacy rootfs with bin/usr must be recognized", version)
        assertEquals(RootfsMigrationManager.LEGACY_VERSION_CODE, version?.versionCode)
        assertEquals(RootfsMigrationManager.LEGACY_VERSION_NAME, version?.versionName)
    }

    @Test
    fun testWriteAndReadVersion_persistsMetadataAccurately() {
        val rootfsDir = tempFolder.newFolder("versioned_rootfs")
        val info = RootfsVersionInfo(
            versionCode = 8,
            versionName = "1.1.1",
            installedAt = 1723630000000L,
            lastUpgradedAt = 1723635000000L
        )

        RootfsMigrationManager.writeVersion(rootfsDir, info)
        val readInfo = RootfsMigrationManager.readVersion(rootfsDir)

        assertNotNull("Written version must be readable", readInfo)
        assertEquals(8, readInfo?.versionCode)
        assertEquals("1.1.1", readInfo?.versionName)
        assertEquals(1723630000000L, readInfo?.installedAt)
        assertEquals(1723635000000L, readInfo?.lastUpgradedAt)
    }

    @Test
    fun testGetPendingMigrations_resolvesCorrectSteps() {
        val allPending = RootfsMigrationManager.getPendingMigrations(currentVersionCode = 1, targetVersionCode = 10)
        assertEquals(8, allPending.size)
        assertEquals(listOf(2, 3, 4, 5, 6, 7, 8, 10), allPending.map { it.targetVersionCode })

        val partialPending = RootfsMigrationManager.getPendingMigrations(currentVersionCode = 5, targetVersionCode = 10)
        assertEquals(4, partialPending.size)
        assertEquals(listOf(6, 7, 8, 10), partialPending.map { it.targetVersionCode })

        val upToDate = RootfsMigrationManager.getPendingMigrations(currentVersionCode = 10, targetVersionCode = 10)
        assertTrue("No migrations should be pending when current equals target", upToDate.isEmpty())
    }

    @Test
    fun testExecuteMigrations_appliesAllFixesToRootfs() {
        val rootfsDir = tempFolder.newFolder("migration_test_rootfs")
        File(rootfsDir, "usr/bin").mkdirs()
        File(rootfsDir, "usr/sbin").mkdirs()
        File(rootfsDir, "etc").mkdirs()
        File(rootfsDir, "usr/bin/sudo").createNewFile()

        val pending = RootfsMigrationManager.getPendingMigrations(currentVersionCode = 1, targetVersionCode = 10)

        val logs = mutableListOf<String>()
        for (step in pending) {
            val success = step.execute(null, rootfsDir) { logs.add(it) }
            assertTrue("Migration step ${step.name} must succeed", success)
        }

        // Verify DNS and hosts (v2)
        val resolv = File(rootfsDir, "etc/resolv.conf")
        assertTrue("resolv.conf must exist", resolv.exists())
        assertTrue(resolv.readText().contains("8.8.8.8"))

        // Verify APT sandboxing (v3)
        val aptConf = File(rootfsDir, "etc/apt/apt.conf.d/99linuxonandroid")
        assertTrue("apt config must exist", aptConf.exists())
        assertTrue(aptConf.readText().contains("APT::Sandbox::User \"root\""))

        // Verify policy-rc.d (v4)
        val policyRcD = File(rootfsDir, "usr/sbin/policy-rc.d")
        assertTrue("policy-rc.d must exist", policyRcD.exists())
        assertTrue(policyRcD.readText().contains("exit 101"))

        // Verify PAM permit rules (v7)
        val pamSudo = File(rootfsDir, "etc/pam.d/sudo")
        assertTrue("pam.d/sudo must exist", pamSudo.exists())
        assertTrue(pamSudo.readText().contains("pam_permit.so"))

        // Verify OpenSSH config (v8)
        val sshConf = File(rootfsDir, "etc/ssh/sshd_config.d/00-linuxonandroid.conf")
        assertTrue("00-linuxonandroid.conf must exist", sshConf.exists())
        assertTrue(sshConf.readText().contains("Port 2222"))

        // Verify OS Release, LSB Release & Environment variables (v10)
        val osRelease = File(rootfsDir, "etc/os-release")
        assertTrue("os-release must exist", osRelease.exists())
        assertTrue(osRelease.readText().contains("UBUNTU_CODENAME=resolute"))
        assertTrue(osRelease.readText().contains("VERSION_CODENAME=resolute"))

        val lsbRelease = File(rootfsDir, "etc/lsb-release")
        assertTrue("lsb-release must exist", lsbRelease.exists())
        assertTrue(lsbRelease.readText().contains("DISTRIB_CODENAME=resolute"))

        val envFile = File(rootfsDir, "etc/environment")
        assertTrue("environment must exist", envFile.exists())
        assertTrue(envFile.readText().contains("UBUNTU_CODENAME=\"resolute\""))
        assertTrue(envFile.readText().contains("VERSION_CODENAME=\"resolute\""))

        val profileScript = File(rootfsDir, "etc/profile.d/00-linuxonandroid-env.sh")
        assertTrue("00-linuxonandroid-env.sh must exist", profileScript.exists())
        assertTrue(profileScript.canExecute())
    }

    @Test
    fun testHasRootfsImprovements_handlesVersionsWithoutChanges() {
        // App is on build 10, container is on build 10 -> no improvements
        assertFalse(RootfsMigrationManager.hasRootfsImprovements(currentVersionCode = 10, targetVersionCode = 10))

        // Hypothetical future app build 11 without any added migrations beyond 10
        assertFalse(RootfsMigrationManager.hasRootfsImprovements(currentVersionCode = 10, targetVersionCode = 11))

        // Legacy container on build 1 -> has improvements up to build 10
        assertTrue(RootfsMigrationManager.hasRootfsImprovements(currentVersionCode = 1, targetVersionCode = 10))
    }

    @Test
    fun testPackageVersionTracking_readsAndWritesAccurately() {
        val rootfsDir = tempFolder.newFolder("pkg_version_test_rootfs")

        // Initially empty
        val initialMap = RootfsMigrationManager.readPackageVersions(rootfsDir)
        assertTrue(initialMap.isEmpty())
        assertNull(RootfsMigrationManager.getPackageVersion(rootfsDir, "openssh_server"))

        // Write package versions
        RootfsMigrationManager.writePackageVersion(rootfsDir, "openssh_server", 3)
        RootfsMigrationManager.writePackageVersion(rootfsDir, "xfce_desktop", 2)

        val updatedMap = RootfsMigrationManager.readPackageVersions(rootfsDir)
        assertEquals(2, updatedMap.size)
        assertEquals(3, RootfsMigrationManager.getPackageVersion(rootfsDir, "openssh_server"))
        assertEquals(2, RootfsMigrationManager.getPackageVersion(rootfsDir, "xfce_desktop"))
    }

    @Test
    fun testPackageInstalledTracking_isolatedFromUnrelatedBinaryFiles() {
        val rootfsDir = tempFolder.newFolder("pkg_tracking_isolation_rootfs")
        val binDir = File(rootfsDir, "usr/bin").apply { mkdirs() }

        // Simulate an unrelated script creating python3
        File(binDir, "python3").createNewFile()

        // Without python_dev registered in linuxonandroid_packages, it should not be considered installed
        val packages = RootfsMigrationManager.readPackageVersions(rootfsDir)
        assertFalse("python_dev should NOT be recognized as installed just because /usr/bin/python3 exists", packages.containsKey("python_dev"))

        // Once formally registered by 1-click installer:
        RootfsMigrationManager.writePackageVersion(rootfsDir, "python_dev", 2)
        val updatedPackages = RootfsMigrationManager.readPackageVersions(rootfsDir)
        assertTrue("python_dev must now be recognized as installed", updatedPackages.containsKey("python_dev"))
    }

    @Test
    fun testDnsResolvConfPersistence_writesAndParsesCorrectly() {
        val rootfsDir = tempFolder.newFolder("dns_test_rootfs")
        val etcDir = File(rootfsDir, "etc").apply { mkdirs() }
        val resolvConf = File(etcDir, "resolv.conf")

        val dnsServers = listOf("1.1.1.1", "1.0.0.1", "8.8.8.8")
        resolvConf.writeText(dnsServers.joinToString("\n") { "nameserver $it" } + "\n")

        val parsed = resolvConf.readLines()
            .filter { it.trim().startsWith("nameserver") }
            .map { it.removePrefix("nameserver").trim() }

        assertEquals(listOf("1.1.1.1", "1.0.0.1", "8.8.8.8"), parsed)
    }
}
