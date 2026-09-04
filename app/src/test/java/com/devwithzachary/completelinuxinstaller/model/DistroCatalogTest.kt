package com.devwithzachary.completelinuxinstaller.model

import org.junit.Assert.*
import org.junit.Test

class DistroCatalogTest {

    @Test
    fun testAllDistros_containsExpectedSixDistros() {
        val distros = DistroCatalog.ALL_DISTROS
        assertEquals(6, distros.size)

        val ids = distros.map { it.id }
        assertTrue("Must contain ubuntu_26_04", ids.contains("ubuntu_26_04"))
        assertTrue("Must contain debian_12", ids.contains("debian_12"))
        assertTrue("Must contain alpine_3_21", ids.contains("alpine_3_21"))
        assertTrue("Must contain arch_arm", ids.contains("arch_arm"))
        assertTrue("Must contain kali_rolling", ids.contains("kali_rolling"))
        assertTrue("Must contain void_rolling", ids.contains("void_rolling"))
    }

    @Test
    fun testDistros_haveUniqueIds() {
        val distros = DistroCatalog.ALL_DISTROS
        val ids = distros.map { it.id }
        assertEquals("Distro IDs must be distinct", ids.size, ids.toSet().size)
    }

    @Test
    fun testDistros_haveValidDownloadUrlsForArchitectures() {
        for (distro in DistroCatalog.ALL_DISTROS) {
            val arm64Url = distro.getDownloadUrl(SystemArchitecture.ARM64)
            assertNotNull("Distro ${distro.name} must define an ARM64 download URL", arm64Url)
            assertTrue("Distro ${distro.name} ARM64 URL must be valid", arm64Url!!.startsWith("http"))

            val x86Url = distro.getDownloadUrl(SystemArchitecture.X86_64)
            assertNotNull("Distro ${distro.name} must define an X86_64 download URL", x86Url)
            assertTrue("Distro ${distro.name} X86_64 URL must be valid", x86Url!!.startsWith("http"))
        }
    }

    @Test
    fun testDistros_packageManagersMappedCorrectly() {
        assertEquals(PackageManagerType.APT, DistroCatalog.UBUNTU_26_04.packageManager)
        assertEquals(PackageManagerType.APT, DistroCatalog.DEBIAN_12.packageManager)
        assertEquals(PackageManagerType.APK, DistroCatalog.ALPINE_3_21.packageManager)
        assertEquals(PackageManagerType.PACMAN, DistroCatalog.ARCH_ARM.packageManager)
        assertEquals(PackageManagerType.APT, DistroCatalog.KALI_ROLLING.packageManager)
        assertEquals(PackageManagerType.XBPS, DistroCatalog.VOID_ROLLING.packageManager)
    }

    @Test
    fun testDistros_expectedSizesArePositive() {
        for (distro in DistroCatalog.ALL_DISTROS) {
            assertTrue("Distro ${distro.name} expected size must be positive", distro.expectedSizeMb > 0)
        }
    }

    @Test
    fun testDistros_defaultShellsAreValid() {
        for (distro in DistroCatalog.ALL_DISTROS) {
            assertTrue("Distro ${distro.name} default shell must start with /bin/", distro.defaultShell.startsWith("/bin/"))
        }
    }

    @Test
    fun testDistros_haveFirstLaunchScripts() {
        for (distro in DistroCatalog.ALL_DISTROS) {
            val script = distro.buildFirstLaunchSetupScript("testroot", "testuser", "testpass", true)
            assertTrue("Distro ${distro.name} setup script must not be empty", script.isNotBlank())
            assertTrue("Distro ${distro.name} script must configure testuser", script.contains("testuser"))
            assertTrue("Distro ${distro.name} script must configure sudoers", script.contains("sudoers"))
        }
    }

    @Test
    fun testDistros_haveOneClickSoftwarePackageCommands() {
        val packageIds = listOf("xfce_desktop", "python_dev", "node_dev", "android_dev", "nginx_web", "openssh_server")
        for (distro in DistroCatalog.ALL_DISTROS) {
            for (pkgId in packageIds) {
                val cmd = distro.getSoftwarePackageInstallCommand(pkgId, 2222)
                assertNotNull("Distro ${distro.name} must provide install command for $pkgId", cmd)
                assertTrue("Distro ${distro.name} install command for $pkgId must not be blank", cmd!!.isNotBlank())
            }
        }
    }

    @Test
    fun testUbuntu_installedSizeIs450Mb() {
        assertEquals(450, DistroCatalog.UBUNTU_26_04.installedSizeMb)
        assertEquals("450 MB", DistroCatalog.UBUNTU_26_04.formattedInstalledSize)
        val defaultUbuntu = LinuxDistribution.defaultForArch("aarch64")
        assertEquals(450, defaultUbuntu.installedSizeMb)
    }

    @Test
    fun testDebian12_xfceDesktop_distroSpecificOverrides() {
        val debian = DistroCatalog.DEBIAN_12
        val installCmd = debian.getSoftwarePackageInstallCommand("xfce_desktop")
        assertNotNull("Debian install command must exist", installCmd)
        assertTrue("Debian install command must contain tigervnc-tools", installCmd!!.contains("tigervnc-tools"))
        assertTrue("Debian install command must contain x11-utils", installCmd.contains("x11-utils"))
        assertTrue("Debian install command must create /usr/bin/bwrap via printf", installCmd.contains("> /usr/bin/bwrap"))
        assertTrue("Debian install command must create /etc/vnc/xstartup via printf", installCmd.contains("> /etc/vnc/xstartup"))

        val launchCmd = debian.getSoftwarePackageLaunchCommand("xfce_desktop")
        assertNotNull("Debian must have distro-specific launch command for xfce_desktop", launchCmd)
        assertTrue("Debian launch command must use debian password", launchCmd!!.contains("echo debian | vncpasswd"))
        assertTrue("Debian launch command must support tigervncpasswd fallback", launchCmd.contains("tigervncpasswd"))

        val expectedBinaries = debian.getSoftwarePackageExpectedBinaries("xfce_desktop")
        assertNotNull("Debian must define expected binaries for xfce_desktop", expectedBinaries)
        assertTrue("Debian expected binaries must include vncpasswd", expectedBinaries!!.contains("usr/bin/vncpasswd"))
        assertTrue("Debian expected binaries must include xstartup", expectedBinaries.contains("etc/vnc/xstartup"))

        assertEquals("Debian xfce_desktop version must be 5", 5, debian.getSoftwarePackageVersion("xfce_desktop"))
    }

    @Test
    fun testUbuntu2604_softwarePackageOverridesAreUnchanged() {
        val ubuntu = DistroCatalog.UBUNTU_26_04
        assertNull("Ubuntu should not override launch command by default", ubuntu.getSoftwarePackageLaunchCommand("xfce_desktop"))
        assertNull("Ubuntu should not override expected binaries by default", ubuntu.getSoftwarePackageExpectedBinaries("xfce_desktop"))
        assertNull("Ubuntu should not override version by default", ubuntu.getSoftwarePackageVersion("xfce_desktop"))
    }
}
