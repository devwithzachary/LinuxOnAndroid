package com.devwithzachary.completelinuxinstaller.model

import org.junit.Assert.*
import org.junit.Test

class DistroCatalogTest {

    @Test
    fun testAllDistros_containsExpectedSevenDistros() {
        val distros = DistroCatalog.ALL_DISTROS
        assertEquals(7, distros.size)

        val ids = distros.map { it.id }
        assertTrue("Must contain ubuntu_26_04", ids.contains("ubuntu_26_04"))
        assertTrue("Must contain debian_12", ids.contains("debian_12"))
        assertTrue("Must contain fedora_44", ids.contains("fedora_44"))
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
        assertEquals(PackageManagerType.DNF, DistroCatalog.FEDORA_44.packageManager)
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
        val packageIds = listOf("xfce_desktop", "python_dev", "node_dev", "android_dev", "nginx_web", "openssh_server", "code_server", "web_terminal")
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
    fun testArchArm_pacmanAndSoftwarePackageOverrides() {
        val arch = DistroCatalog.ARCH_ARM
        val setupScript = arch.buildFirstLaunchSetupScript("root123", "archuser", "user123", true)
        assertTrue("Arch first launch script must sanitize pacman.conf", setupScript.contains("DownloadUser"))
        assertTrue("Arch first launch script must disable sandbox", setupScript.contains("DisableSandbox"))
        assertTrue("Arch first launch script must set SigLevel to Never", setupScript.contains("SigLevel = Never"))

        val xfceInstallCmd = arch.getSoftwarePackageInstallCommand("xfce_desktop")
        assertNotNull("Arch xfce_desktop install command must exist", xfceInstallCmd)
        assertTrue("Arch install command must force refresh databases (-Syy)", xfceInstallCmd!!.contains("-Syy"))
        assertTrue("Arch install command must disable sandbox before pacman", xfceInstallCmd.contains("DisableSandbox"))
        assertTrue("Arch install command must create /etc/vnc/xstartup via printf", xfceInstallCmd.contains("> /etc/vnc/xstartup"))
        assertFalse("Arch install command should not use heredoc", xfceInstallCmd.contains("cat << 'EOF'"))

        val launchCmd = arch.getSoftwarePackageLaunchCommand("xfce_desktop")
        assertNotNull("Arch must define a launch command for xfce_desktop", launchCmd)
        assertTrue("Arch launch command must use arch password", launchCmd!!.contains("echo arch | vncpasswd"))
        assertTrue("Arch launch command must kill previous display", launchCmd.contains("vncserver -kill :1"))
        assertTrue("Arch launch command must launch display :1", launchCmd.contains("vncserver :1"))

        val expectedBinaries = arch.getSoftwarePackageExpectedBinaries("xfce_desktop")
        assertNotNull("Arch must define expected binaries for xfce_desktop", expectedBinaries)
        assertTrue("Arch expected binaries must include startxfce4", expectedBinaries!!.contains("usr/bin/startxfce4"))
        assertTrue("Arch expected binaries must include vncserver", expectedBinaries.contains("usr/bin/vncserver"))
        assertTrue("Arch expected binaries must include vncpasswd", expectedBinaries.contains("usr/bin/vncpasswd"))
        assertTrue("Arch expected binaries must include xstartup", expectedBinaries.contains("etc/vnc/xstartup"))

        assertEquals("Arch xfce_desktop version must be 5", 5, arch.getSoftwarePackageVersion("xfce_desktop"))
    }

    @Test
    fun testKaliRolling_dnsAndSoftwarePackageOverrides() {
        val kali = DistroCatalog.KALI_ROLLING
        val setupScript = kali.buildFirstLaunchSetupScript("root123", "kaliuser", "user123", true)
        assertTrue("Kali setup script must repair resolv.conf", setupScript.contains("nameserver 8.8.8.8"))
        assertTrue("Kali setup script must check for 213.186.33.99", setupScript.contains("213.186.33.99"))

        val xfceInstallCmd = kali.getSoftwarePackageInstallCommand("xfce_desktop")
        assertNotNull("Kali xfce_desktop install command must exist", xfceInstallCmd)
        assertTrue("Kali install command must ensure valid DNS", xfceInstallCmd!!.contains("nameserver 8.8.8.8"))
        assertTrue("Kali install command must check for 213.186.33.99", xfceInstallCmd.contains("213.186.33.99"))
        assertTrue("Kali install command must create /etc/vnc/xstartup via printf", xfceInstallCmd.contains("> /etc/vnc/xstartup"))

        val launchCmd = kali.getSoftwarePackageLaunchCommand("xfce_desktop")
        assertNotNull("Kali must define a launch command for xfce_desktop", launchCmd)
        assertTrue("Kali launch command must use kali password", launchCmd!!.contains("echo kali | vncpasswd"))
        assertTrue("Kali launch command must kill previous display", launchCmd.contains("vncserver -kill :1"))
        assertTrue("Kali launch command must launch display :1", launchCmd.contains("vncserver :1"))

        val expectedBinaries = kali.getSoftwarePackageExpectedBinaries("xfce_desktop")
        assertNotNull("Kali must define expected binaries for xfce_desktop", expectedBinaries)
        assertTrue("Kali expected binaries must include startxfce4", expectedBinaries!!.contains("usr/bin/startxfce4"))
        assertTrue("Kali expected binaries must include vncserver", expectedBinaries.contains("usr/bin/vncserver"))
        assertTrue("Kali expected binaries must include vncpasswd", expectedBinaries.contains("usr/bin/vncpasswd"))
        assertTrue("Kali expected binaries must include xstartup", expectedBinaries.contains("etc/vnc/xstartup"))

        assertEquals("Kali xfce_desktop version must be 5", 5, kali.getSoftwarePackageVersion("xfce_desktop"))
    }

    @Test
    fun testVoidRolling_softwarePackageOverrides() {
        val void = DistroCatalog.VOID_ROLLING
        val setupScript = void.buildFirstLaunchSetupScript("root123", "voiduser", "user123", true)
        assertTrue("Void setup script must update xbps first", setupScript.contains("xbps-install -Syu xbps -y"))

        val xfceInstallCmd = void.getSoftwarePackageInstallCommand("xfce_desktop")
        assertNotNull("Void xfce_desktop install command must exist", xfceInstallCmd)
        assertTrue("Void install command must update xbps first", xfceInstallCmd!!.contains("xbps-install -Syu xbps -y"))
        assertTrue("Void install command must export PATH", xfceInstallCmd.contains("export PATH="))
        assertTrue("Void install command must install libstdc++", xfceInstallCmd.contains("libstdc++"))
        assertTrue("Void install command must deploy PRoot vncserver wrapper", xfceInstallCmd.contains("TigerVNC server wrapper for PRoot environments"))
        assertTrue("Void install command must create /etc/vnc/xstartup via printf", xfceInstallCmd.contains("> /etc/vnc/xstartup"))
        assertTrue("Void install command must use void password", xfceInstallCmd.contains("echo void | vncpasswd"))
        assertFalse("Void install command should not use heredoc", xfceInstallCmd.contains("cat << 'EOF'"))

        val launchCmd = void.getSoftwarePackageLaunchCommand("xfce_desktop")
        assertNotNull("Void must define a launch command for xfce_desktop", launchCmd)
        assertTrue("Void launch command must ensure libstdc++ compatibility", launchCmd!!.contains("CXXABI_1.3.15"))
        assertTrue("Void launch command must deploy PRoot vncserver wrapper if missing", launchCmd.contains("TigerVNC server wrapper for PRoot environments"))
        assertTrue("Void launch command must use void password", launchCmd.contains("echo void | vncpasswd"))
        assertTrue("Void launch command must kill previous display", launchCmd.contains("vncserver -kill :1"))
        assertTrue("Void launch command must launch display :1", launchCmd.contains("vncserver :1"))

        val expectedBinaries = void.getSoftwarePackageExpectedBinaries("xfce_desktop")
        assertNotNull("Void must define expected binaries for xfce_desktop", expectedBinaries)
        assertTrue("Void expected binaries must include startxfce4", expectedBinaries!!.contains("usr/bin/startxfce4"))
        assertTrue("Void expected binaries must include vncserver", expectedBinaries.contains("usr/bin/vncserver"))
        assertTrue("Void expected binaries must include vncpasswd", expectedBinaries.contains("usr/bin/vncpasswd"))
        assertTrue("Void expected binaries must include xstartup", expectedBinaries.contains("etc/vnc/xstartup"))

        assertEquals("Void xfce_desktop version must be 5", 5, void.getSoftwarePackageVersion("xfce_desktop"))

        val sshInstallCmd = void.getSoftwarePackageInstallCommand("openssh_server", 2222)
        assertNotNull("Void openssh install command must exist", sshInstallCmd)
        assertTrue("Void ssh install command must update xbps first", sshInstallCmd!!.contains("xbps-install -Syu xbps -y"))
        assertTrue("Void ssh install command must export PATH", sshInstallCmd.contains("export PATH="))

        val sshLaunchCmd = void.getSoftwarePackageLaunchCommand("openssh_server", 2222)
        assertNotNull("Void ssh launch command must exist", sshLaunchCmd)
        assertTrue("Void ssh launch command must launch sshd on port 2222", sshLaunchCmd!!.contains("sshd -p 2222"))
    }

    @Test
    fun testUbuntu2604_softwarePackageOverridesAreUnchanged() {
        val ubuntu = DistroCatalog.UBUNTU_26_04
        assertNull("Ubuntu should not override launch command by default", ubuntu.getSoftwarePackageLaunchCommand("xfce_desktop"))
        assertNull("Ubuntu should not override expected binaries by default", ubuntu.getSoftwarePackageExpectedBinaries("xfce_desktop"))
        assertNull("Ubuntu should not override version by default", ubuntu.getSoftwarePackageVersion("xfce_desktop"))
    }

    @Test
    fun testAlpine321_candidateShells_prefersShAndAsh() {
        val alpine = DistroCatalog.ALPINE_3_21
        assertEquals("/bin/sh", alpine.defaultShell)
        assertEquals(
            listOf("/bin/sh", "/bin/ash", "/bin/bash", "/usr/bin/bash"),
            alpine.candidateShells
        )
    }

    @Test
    fun testUbuntuDebianAndFedora_candidateShells_useStandardBashHierarchy() {
        val expected = listOf("/bin/bash", "/usr/bin/bash", "/bin/sh")
        assertEquals(expected, DistroCatalog.UBUNTU_26_04.candidateShells)
        assertEquals(expected, DistroCatalog.DEBIAN_12.candidateShells)
        assertEquals(expected, DistroCatalog.FEDORA_44.candidateShells)
    }

    @Test
    fun testAlpine321_firstLaunchScript_doesNotCreateBrokenBusyboxBashSymlink() {
        val alpine = DistroCatalog.ALPINE_3_21
        val script = alpine.buildFirstLaunchSetupScript("root123", "alpine", "alpine123", true)
        // Must clean up any bad symlinks if present and not create broken /bin/bash -> /bin/sh
        assertFalse("Script must not link /bin/sh to /bin/bash", script.contains("ln -sf /bin/sh /bin/bash"))
        assertFalse("Script must not link /bin/sh to /usr/bin/bash", script.contains("ln -sf /bin/sh /usr/bin/bash"))
        assertTrue("Script must link /bin/bash to /usr/bin/bash once bash is installed", script.contains("ln -sf /bin/bash /usr/bin/bash"))
    }

    @Test
    fun testFedora44_configurationAndOverrides() {
        val fedora = DistroCatalog.FEDORA_44
        assertEquals("fedora_44", fedora.id)
        assertEquals("Fedora 44", fedora.name)
        assertEquals("44", fedora.version)
        assertEquals("Leading-Edge & RPM", fedora.tag)
        assertEquals(PackageManagerType.DNF, fedora.packageManager)
        assertEquals("/bin/bash", fedora.defaultShell)
        assertEquals(142, fedora.downloadSizeMb)
        assertEquals(480, fedora.installedSizeMb)
        assertEquals(0xFF51A2DA, fedora.colorHex)

        val arm64Url = fedora.getDownloadUrl(SystemArchitecture.ARM64)
        assertNotNull("Fedora must have ARM64 download URL", arm64Url)
        assertTrue("Fedora ARM64 URL must point to download.fedoraproject.org", arm64Url!!.contains("download.fedoraproject.org"))
        assertTrue("Fedora ARM64 URL must be WSL rootfs", arm64Url.endsWith(".wsl"))

        val x86Url = fedora.getDownloadUrl(SystemArchitecture.X86_64)
        assertNotNull("Fedora must have x86_64 download URL", x86Url)
        assertTrue("Fedora x86_64 URL must point to download.fedoraproject.org", x86Url!!.contains("download.fedoraproject.org"))

        // First launch script
        val script = fedora.buildFirstLaunchSetupScript("fedoraRoot", "fedoraUser", "fedoraPass", true)
        assertTrue("Script must configure fedoraUser in passwd", script.contains("fedoraUser"))
        assertTrue("Script must configure wheel group", script.contains("wheel"))
        assertTrue("Script must configure sudoers.d", script.contains("sudoers.d/fedoraUser"))
        assertTrue("Script must configure PAM su permit", script.contains("/etc/pam.d/su"))

        // Software package overrides
        assertTrue("Fedora setup script must disable SELinux", script.contains("SELINUX=disabled"))

        val xfceInstall = fedora.getSoftwarePackageInstallCommand("xfce_desktop")
        assertNotNull("Fedora xfce install command must exist", xfceInstall)
        assertTrue("Fedora xfce install command must use dnf", xfceInstall!!.contains("dnf install -y"))
        assertTrue("Fedora xfce install command must install tigervnc-server", xfceInstall.contains("tigervnc-server"))
        assertTrue("Fedora xfce install command must disable SELinux", xfceInstall.contains("SELINUX=disabled"))
        assertTrue("Fedora xfce install command must deploy TigerVNC wrapper", xfceInstall.contains("TigerVNC server wrapper for PRoot environments"))
        assertFalse("Fedora wrapper script must not contain escaped dollar parameter \$#", xfceInstall.contains("\\$#"))
        assertFalse("Fedora wrapper script must not contain escaped command substitution \\$(", xfceInstall.contains("\\$("))

        val xfceLaunch = fedora.getSoftwarePackageLaunchCommand("xfce_desktop")
        assertNotNull("Fedora xfce launch command must exist", xfceLaunch)
        assertTrue("Fedora xfce launch command must start vncserver on :1", xfceLaunch!!.contains("vncserver :1"))

        val expectedBinaries = fedora.getSoftwarePackageExpectedBinaries("xfce_desktop")
        assertNotNull("Fedora expected binaries must exist", expectedBinaries)
        assertTrue("Must include startxfce4", expectedBinaries!!.contains("usr/bin/startxfce4"))
        assertTrue("Must include vncserver", expectedBinaries.contains("usr/bin/vncserver"))

        assertEquals("Fedora xfce_desktop version must be 5", 5, fedora.getSoftwarePackageVersion("xfce_desktop"))

        val pythonInstall = fedora.getSoftwarePackageInstallCommand("python_dev")
        assertTrue("Fedora python install must use dnf", pythonInstall!!.contains("dnf install -y python3"))
        assertTrue("Fedora python install must disable SELinux", pythonInstall.contains("SELINUX=disabled"))

        val nodeInstall = fedora.getSoftwarePackageInstallCommand("node_dev")
        assertTrue("Fedora node install must use dnf", nodeInstall!!.contains("dnf install -y nodejs"))

        val androidInstall = fedora.getSoftwarePackageInstallCommand("android_dev")
        assertTrue("Fedora android install must use dnf", androidInstall!!.contains("dnf install -y java-17-openjdk-headless"))

        val nginxInstall = fedora.getSoftwarePackageInstallCommand("nginx_web")
        assertTrue("Fedora nginx install must use dnf", nginxInstall!!.contains("dnf install -y nginx"))
        assertTrue("Fedora nginx install must remap port 80", nginxInstall.contains("8080"))

        val sshInstall = fedora.getSoftwarePackageInstallCommand("openssh_server", 2222)
        assertTrue("Fedora ssh install must use dnf", sshInstall!!.contains("dnf install -y openssh-server"))
        assertTrue("Fedora ssh install must configure port 2222", sshInstall.contains("Port 2222"))
    }

    @Test
    fun testCodeServerPreset_definedAndValid() {
        val presets = SoftwarePackage.getPresets()
        val codePkg = presets.find { it.id == "code_server" }
        assertNotNull("code_server preset must be present", codePkg)
        assertEquals("VS Code Server (code-server)", codePkg!!.name)
        assertEquals(SoftwareCategory.DEVELOPMENT, codePkg.category)
        assertEquals("Code", codePkg.iconName)
        assertTrue("Expected binaries must include usr/bin/code-server", codePkg.expectedBinaries.contains("usr/bin/code-server"))

        val launchCmd = codePkg.launchCommand
        assertNotNull("code_server must have launch command", launchCmd)
        assertTrue("Launch command must specify 0.0.0.0 bind address", launchCmd!!.contains("--bind-addr 0.0.0.0:"))
        assertTrue("Launch command must disable authentication for instant access", launchCmd.contains("--auth none"))
        assertTrue("Launch command must support fallback to 8443 if 8080 is in use", launchCmd.contains("PORT=8443"))
    }

    @Test
    fun testCodeServer_isBinaryPresentAliasDetection() {
        val tempDir = java.nio.file.Files.createTempDirectory("loa_test_rootfs").toFile()
        try {
            val usrLocalBin = java.io.File(tempDir, "usr/local/bin").apply { mkdirs() }
            val dummyBinary = java.io.File(usrLocalBin, "code-server")
            dummyBinary.writeText("#!/bin/sh\nexit 0\n")

            // Even though binary is at usr/local/bin/code-server, checking usr/bin/code-server must resolve to true
            assertTrue(
                "isBinaryPresent for usr/bin/code-server must resolve usr/local/bin/code-server alias",
                SoftwarePackage.isBinaryPresent(tempDir, "usr/bin/code-server")
            )
            assertTrue(
                "isBinaryPresent for usr/local/bin/code-server must be true",
                SoftwarePackage.isBinaryPresent(tempDir, "usr/local/bin/code-server")
            )
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun testCodeServer_distroSpecificInstallCommands() {
        val fedoraCmd = DistroCatalog.FEDORA_44.getSoftwarePackageInstallCommand("code_server")
        assertNotNull(fedoraCmd)
        assertTrue("Fedora code-server install must use dnf", fedoraCmd!!.contains("dnf install -y"))
        assertTrue("Fedora code-server install must run install.sh", fedoraCmd.contains("install.sh"))

        val archCmd = DistroCatalog.ARCH_ARM.getSoftwarePackageInstallCommand("code_server")
        assertNotNull(archCmd)
        assertTrue("Arch code-server install must use standalone prefix to bypass makepkg root restriction", archCmd!!.contains("--method=standalone"))

        val voidCmd = DistroCatalog.VOID_ROLLING.getSoftwarePackageInstallCommand("code_server")
        assertNotNull(voidCmd)
        assertTrue("Void code-server install must use standalone method", voidCmd!!.contains("--method=standalone"))

        val alpineCmd = DistroCatalog.ALPINE_3_21.getSoftwarePackageInstallCommand("code_server")
        assertNotNull(alpineCmd)
        assertTrue("Alpine code-server install must install npm/nodejs or standalone", alpineCmd!!.contains("npm install -g code-server") || alpineCmd.contains("--method=standalone"))
    }

    @Test
    fun testWebTerminal_distroSpecificInstallCommands() {
        val fedoraCmd = DistroCatalog.FEDORA_44.getSoftwarePackageInstallCommand("web_terminal")
        assertNotNull(fedoraCmd)
        assertTrue("Fedora web_terminal install must use dnf", fedoraCmd!!.contains("dnf install -y"))
        assertTrue("Fedora web_terminal install must install ttyd", fedoraCmd.contains("ttyd"))

        val alpineCmd = DistroCatalog.ALPINE_3_21.getSoftwarePackageInstallCommand("web_terminal")
        assertNotNull(alpineCmd)
        assertTrue("Alpine web_terminal install must use apk", alpineCmd!!.contains("apk add"))
        assertTrue("Alpine web_terminal install must install ttyd", alpineCmd.contains("ttyd"))

        val archCmd = DistroCatalog.ARCH_ARM.getSoftwarePackageInstallCommand("web_terminal")
        assertNotNull(archCmd)
        assertTrue("Arch web_terminal install must use pacman", archCmd!!.contains("pacman -S"))
        assertTrue("Arch web_terminal install must install ttyd", archCmd.contains("ttyd"))

        val voidCmd = DistroCatalog.VOID_ROLLING.getSoftwarePackageInstallCommand("web_terminal")
        assertNotNull(voidCmd)
        assertTrue("Void web_terminal install must use xbps", voidCmd!!.contains("xbps-install"))
        assertTrue("Void web_terminal install must install ttyd", voidCmd.contains("ttyd"))

        val ubuntuCmd = DistroCatalog.UBUNTU_26_04.getSoftwarePackageInstallCommand("web_terminal")
        assertNotNull(ubuntuCmd)
        assertTrue("Ubuntu web_terminal install must use apt", ubuntuCmd!!.contains("apt-get install -y"))
        assertTrue("Ubuntu web_terminal install must install ttyd", ubuntuCmd.contains("ttyd"))
    }
}
