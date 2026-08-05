package com.devwithzachary.completelinuxinstaller.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SoftwarePackageTest {

    @Test
    fun testGetPresets_returnsNonEmptyList() {
        val presets = SoftwarePackage.getPresets()
        assertTrue("Preset package list should not be empty", presets.isNotEmpty())
        assertEquals(5, presets.size)
    }

    @Test
    fun testPresets_haveUniqueIds() {
        val presets = SoftwarePackage.getPresets()
        val ids = presets.map { it.id }
        assertEquals("Package IDs must all be unique", ids.size, ids.toSet().size)
    }

    @Test
    fun testPresets_containRequiredNonInteractiveFlags() {
        val presets = SoftwarePackage.getPresets()
        for (pkg in presets) {
            assertTrue("Package ${pkg.id} must include DEBIAN_FRONTEND=noninteractive in install command", pkg.installCommand.contains("DEBIAN_FRONTEND=noninteractive"))
            assertTrue("Package ${pkg.id} must include non-interactive apt options", pkg.installCommand.contains("apt-get install -y"))
            assertTrue("Package ${pkg.id} must include dpkg recovery flag", pkg.installCommand.contains("dpkg --configure -a"))
            assertTrue("Package ${pkg.id} must include policy-rc.d service block", pkg.installCommand.contains("policy-rc.d"))
            assertTrue("Package ${pkg.id} must include messagebus system user initialization", pkg.installCommand.contains("messagebus:"))
        }
    }

    @Test
    fun testPresets_defineExpectedBinaries() {
        val presets = SoftwarePackage.getPresets()
        for (pkg in presets) {
            assertNotNull("Package ${pkg.id} should have a non-null expectedBinaries list", pkg.expectedBinaries)
            assertFalse("Package ${pkg.id} must define at least 1 expected binary", pkg.expectedBinaries.isEmpty())
            for (binPath in pkg.expectedBinaries) {
                assertFalse("Binary path '$binPath' should not start with a leading slash", binPath.startsWith("/"))
            }
        }
    }

    @Test
    fun testPreset_openssh_server_doesNotBlockForeground() {
        val openssh = SoftwarePackage.getPresets().find { it.id == "openssh_server" }
        assertNotNull("openssh_server preset must exist", openssh)
        openssh?.let {
            assertFalse("installCommand must not start sshd in foreground", it.installCommand.contains("(/usr/sbin/sshd -p 2222 2>/dev/null || true)"))
            assertTrue("launchCommand must exist for openssh_server", it.launchCommand != null && it.launchCommand.contains("sshd -p 2222"))
        }
    }
}
