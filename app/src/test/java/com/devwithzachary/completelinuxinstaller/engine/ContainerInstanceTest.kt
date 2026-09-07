package com.devwithzachary.completelinuxinstaller.engine

import com.devwithzachary.completelinuxinstaller.model.ContainerInstance
import com.devwithzachary.completelinuxinstaller.model.PackageManagerType
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ContainerInstanceTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testContainerInstance_properties() {
        val rootDir = tempFolder.newFolder("ubuntu_rootfs")
        val container = ContainerInstance(
            id = "test_ubuntu",
            name = "Test Ubuntu",
            distroId = "ubuntu_26_04",
            distroName = "Ubuntu 26.04 LTS",
            rootDirPath = rootDir.absolutePath,
            isDefault = true,
            storageUsedMb = 250L,
            defaultUser = "ubuntu",
            defaultShell = "/bin/bash",
            packageManager = PackageManagerType.APT,
            colorHex = 0xFFE95420
        )

        assertEquals("test_ubuntu", container.id)
        assertEquals("Test Ubuntu", container.name)
        assertEquals(rootDir.absolutePath, container.rootDir.absolutePath)
        assertTrue(container.isDefault)
        assertEquals(250L, container.storageUsedMb)
        assertEquals(PackageManagerType.APT, container.packageManager)
    }

    @Test
    fun testContainerInstance_isInstalled_checksFiles() {
        val rootDir = tempFolder.newFolder("empty_rootfs")
        val container = ContainerInstance(
            id = "c1",
            name = "C1",
            distroId = "alpine_3_20",
            distroName = "Alpine",
            rootDirPath = rootDir.absolutePath
        )

        // Empty dir -> not installed
        assertFalse(container.isInstalled)

        // Only etc/os-release without bin/sh -> still not installed (needs executable shell)
        val etcDir = File(rootDir, "etc").apply { mkdirs() }
        File(etcDir, "os-release").writeText("NAME=Alpine\n")
        assertFalse(container.isInstalled)

        // Add bin/sh -> now installed
        val binDir = File(rootDir, "bin").apply { mkdirs() }
        File(binDir, "sh").createNewFile()

        assertTrue(container.isInstalled)
    }

    @Test
    fun testFormatContainerHostname() {
        assertEquals("Ubuntu2604LTS", ContainerManager.formatContainerHostname("Ubuntu 26.04 LTS"))
        assertEquals("Debian12", ContainerManager.formatContainerHostname("Debian 12"))
        assertEquals("AlpineLinux321", ContainerManager.formatContainerHostname("Alpine Linux 3.21"))
        assertEquals("ArchLinuxARM", ContainerManager.formatContainerHostname("Arch Linux ARM"))
        assertEquals("KaliLinuxCLITools", ContainerManager.formatContainerHostname("Kali Linux CLI Tools"))
        assertEquals("VoidLinux", ContainerManager.formatContainerHostname("Void Linux"))
        assertEquals("MyCustomServer", ContainerManager.formatContainerHostname("my custom-server"))
        assertEquals("localhost", ContainerManager.formatContainerHostname("!!!"))
    }
}
