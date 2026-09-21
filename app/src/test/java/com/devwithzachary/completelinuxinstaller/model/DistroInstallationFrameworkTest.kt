package com.devwithzachary.completelinuxinstaller.model

import com.devwithzachary.completelinuxinstaller.engine.ContainerManager
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

class DistroInstallationFrameworkTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testAlpineGuestSymlinkResolution_isRecognizedAsRealRootfs() {
        val rootfs = tempFolder.newFolder("alpine_rootfs")

        // Construct Alpine mini-rootfs layout
        val binDir = File(rootfs, "bin").apply { mkdirs() }
        val sbinDir = File(rootfs, "sbin").apply { mkdirs() }
        val etcDir = File(rootfs, "etc").apply { mkdirs() }
        val usrBinDir = File(rootfs, "usr/bin").apply { mkdirs() }

        // Alpine has a real busybox binary and symlinks to /bin/busybox
        val busybox = File(binDir, "busybox").apply {
            writeText("#!/bin/sh\necho Busybox mock")
            setExecutable(true)
        }
        val apk = File(sbinDir, "apk").apply {
            writeText("#!/bin/sh\necho APK mock")
            setExecutable(true)
        }
        File(etcDir, "alpine-release").writeText("3.21.3\n")

        // Create guest symlink pointing to /bin/busybox
        val binSh = File(binDir, "sh")
        Files.createSymbolicLink(binSh.toPath(), Paths.get("/bin/busybox"))

        val binAsh = File(binDir, "ash")
        Files.createSymbolicLink(binAsh.toPath(), Paths.get("/bin/busybox"))

        // Standard java File.exists() fails because /bin/busybox doesn't exist on host
        assertFalse("Host File.exists() cannot resolve guest-absolute /bin/busybox", binSh.exists())

        // Our guest-aware symlink resolution must correctly find the file
        assertTrue("Guest-aware check must resolve /bin/sh -> /bin/busybox", ContainerManager.fileOrGuestSymlinkExists(rootfs, "bin/sh"))
        assertTrue("Guest-aware check must resolve /bin/ash -> /bin/busybox", ContainerManager.fileOrGuestSymlinkExists(rootfs, "bin/ash"))
        assertTrue("hasValidGuestShell must detect BusyBox symlink shell", ContainerManager.hasValidGuestShell(rootfs))
        assertTrue("hasDistroMarker must detect alpine-release and apk", ContainerManager.hasDistroMarker(rootfs))
        assertTrue("isRealRootfs must validate Alpine rootfs layout", ContainerManager.isRealRootfs(rootfs))

        // ContainerInstance.isInstalled must evaluate to true
        val instance = ContainerInstance(
            id = "test_alpine",
            name = "Alpine Linux 3.21",
            distroId = "alpine_3_21",
            distroName = "Alpine Linux 3.21",
            rootDirPath = rootfs.absolutePath
        )
        assertTrue("ContainerInstance.isInstalled must be true for Alpine", instance.isInstalled)
    }

    @Test
    fun testDebianAndUbuntuLayout_isRecognizedAsRealRootfs() {
        val rootfs = tempFolder.newFolder("debian_rootfs")
        val usrBin = File(rootfs, "usr/bin").apply { mkdirs() }
        val binDir = File(rootfs, "bin").apply { mkdirs() }
        val etcDir = File(rootfs, "etc").apply { mkdirs() }

        File(usrBin, "bash").apply {
            writeText("#!/bin/sh\necho bash")
            setExecutable(true)
        }
        File(etcDir, "debian_version").writeText("12.9\n")
        File(etcDir, "os-release").writeText("ID=debian\nVERSION_ID=12\n")

        // Relative symlink bin/sh -> bash or usr/bin/bash
        val binBash = File(binDir, "bash")
        Files.createSymbolicLink(binBash.toPath(), Paths.get("../usr/bin/bash"))

        assertTrue(ContainerManager.hasValidGuestShell(rootfs))
        assertTrue(ContainerManager.hasDistroMarker(rootfs))
        assertTrue(ContainerManager.isRealRootfs(rootfs))
    }

    @Test
    fun testNonExistentOrEmptyDirectory_isNotRealRootfs() {
        val emptyDir = tempFolder.newFolder("empty_dir")
        assertFalse(ContainerManager.isRealRootfs(emptyDir))

        val nonExistent = File(tempFolder.root, "does_not_exist")
        assertFalse(ContainerManager.isRealRootfs(nonExistent))
    }

    @Test
    fun testAllDistroDownloadUrlsAreWellFormed() {
        for (distro in DistroCatalog.ALL_DISTROS) {
            for (arch in listOf(SystemArchitecture.ARM64, SystemArchitecture.X86_64, SystemArchitecture.ARMV7)) {
                val url = distro.getDownloadUrl(arch)
                assertNotNull("Distro ${distro.name} must have a URL for $arch", url)
                assertTrue("URL must be HTTPS or HTTP: $url", url!!.startsWith("http://") || url.startsWith("https://"))
                assertTrue("URL must point to a supported archive format: $url",
                    url.endsWith(".tar.gz") || url.endsWith(".tar.xz") || url.endsWith(".tar.zst") || url.endsWith(".wsl")
                )
            }
        }
    }
}
