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
        assertEquals(6, presets.size)
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
    fun testPreset_openssh_server_configuresSecurityAndForeground() {
        val openssh = SoftwarePackage.getPresets().find { it.id == "openssh_server" }
        assertNotNull("openssh_server preset must exist", openssh)
        openssh?.let {
            assertFalse("installCommand must not start sshd in foreground", it.installCommand.contains("(/usr/sbin/sshd -p 2222 2>/dev/null || true)"))
            assertTrue("launchCommand must exist for openssh_server", it.launchCommand != null && it.launchCommand.contains("sshd -p 2222"))
            assertTrue("installCommand must configure UsePAM no", it.installCommand.contains("UsePAM no"))
            assertTrue("installCommand must configure StrictModes no", it.installCommand.contains("StrictModes no"))
        }
    }

    @Test
    fun testBuildSshLaunchCommand_defaultAndCustomPorts() {
        val defaultCmd = SoftwarePackage.buildSshLaunchCommand()
        assertTrue("Default SSH launch command must contain port 2222", defaultCmd.contains("/usr/sbin/sshd -p 2222"))

        val customCmd = SoftwarePackage.buildSshLaunchCommand(8022)
        assertTrue("Custom SSH launch command must contain port 8022", customCmd.contains("/usr/sbin/sshd -p 8022"))
        assertFalse("Custom SSH launch command must not contain port 2222", customCmd.contains("/usr/sbin/sshd -p 2222"))

        val invalidPortCmd = SoftwarePackage.buildSshLaunchCommand(999999)
        assertTrue("Invalid port should fall back to 2222", invalidPortCmd.contains("/usr/sbin/sshd -p 2222"))

        val zeroPortCmd = SoftwarePackage.buildSshLaunchCommand(0)
        assertTrue("Zero port should fall back to 2222", zeroPortCmd.contains("/usr/sbin/sshd -p 2222"))
    }

    @Test
    fun testBuildSshPostInstallNotes_customPorts() {
        val defaultNotes = SoftwarePackage.buildSshPostInstallNotes()
        assertTrue("Default notes must mention port 2222", defaultNotes.contains("-p 2222"))

        val customNotes = SoftwarePackage.buildSshPostInstallNotes(2022)
        assertTrue("Custom notes must mention port 2022", customNotes.contains("-p 2022"))
    }

    @Test
    fun testGetPresets_customSshPort() {
        val presets = SoftwarePackage.getPresets(sshPort = 8022)
        val sshPkg = presets.find { it.id == "openssh_server" }
        assertNotNull("openssh_server must exist", sshPkg)
        sshPkg?.let {
            assertTrue("launchCommand must reflect custom port 8022", it.launchCommand != null && it.launchCommand.contains("/usr/sbin/sshd -p 8022"))
            assertTrue("postInstallNotes must reflect custom port 8022", it.postInstallNotes != null && it.postInstallNotes.contains("-p 8022"))
        }
    }

    @Test
    fun testPreset_xfce_desktop_configuresVncAndStartup() {
        val xfce = SoftwarePackage.getPresets().find { it.id == "xfce_desktop" }
        assertNotNull("xfce_desktop preset must exist", xfce)
        xfce?.let {
            assertTrue("installCommand must configure xstartup", it.installCommand.contains("/etc/vnc/xstartup"))
            assertTrue("installCommand must configure startxfce4", it.installCommand.contains("startxfce4"))
            assertTrue("installCommand must configure dbus-launch", it.installCommand.contains("dbus-launch"))
            assertTrue("installCommand must configure bwrap stub", it.installCommand.contains("/usr/bin/bwrap"))
            assertTrue("launchCommand must exist for xfce_desktop", it.launchCommand != null)
            assertTrue("launchCommand must start display :1", it.launchCommand?.contains("vncserver :1") == true)
            assertTrue("launchCommand must specify SecurityTypes None,VncAuth", it.launchCommand?.contains("-SecurityTypes None,VncAuth") == true)
            assertTrue("launchCommand must specify -UseBlacklist=0", it.launchCommand?.contains("-UseBlacklist=0") == true)
            assertTrue("launchCommand must include --I-KNOW-THIS-IS-INSECURE", it.launchCommand?.contains("--I-KNOW-THIS-IS-INSECURE") == true)
        }
    }
}
