package com.devwithzachary.completelinuxinstaller.engine

import com.devwithzachary.completelinuxinstaller.model.ContainerInstance
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ContainerExternalStorageTest {

    @Test
    fun testCamelCaseFormattingSingleWord() {
        assertEquals("ubuntu", ContainerManager.formatExternalFolderName("Ubuntu"))
        assertEquals("void", ContainerManager.formatExternalFolderName("Void"))
        assertEquals("alpine", ContainerManager.formatExternalFolderName("alpine"))
    }

    @Test
    fun testCamelCaseFormattingMultiWord() {
        assertEquals("ubuntuContainer", ContainerManager.formatExternalFolderName("Ubuntu Container"))
        assertEquals("voidLinux", ContainerManager.formatExternalFolderName("Void Linux"))
        assertEquals("myCoolContainer", ContainerManager.formatExternalFolderName("my cool container"))
    }

    @Test
    fun testCamelCaseFormattingWithSpecialCharsAndNumbers() {
        assertEquals("ubuntu2204", ContainerManager.formatExternalFolderName("Ubuntu 22.04"))
        assertEquals("debianBookworm", ContainerManager.formatExternalFolderName("Debian (Bookworm)"))
        assertEquals("archLinuxLatest", ContainerManager.formatExternalFolderName("Arch-Linux_Latest!"))
    }

    @Test
    fun testCamelCaseFormattingEmptyAndBlankFallbacks() {
        assertEquals("container", ContainerManager.formatExternalFolderName(""))
        assertEquals("container", ContainerManager.formatExternalFolderName("   "))
        assertEquals("container", ContainerManager.formatExternalFolderName("---///"))
    }

    @Test
    fun testDisambiguateCollisionsWithNumbers() {
        val existing = setOf("ubuntuContainer", "ubuntuContainer2")
        val result = ContainerManager.formatExternalFolderName("Ubuntu Container", existing)
        assertEquals("ubuntuContainer3", result)
    }

    @Test
    fun testDisambiguateFirstCollision() {
        val existing = setOf("ubuntu")
        val result = ContainerManager.formatExternalFolderName("Ubuntu", existing)
        assertEquals("ubuntu2", result)
    }

    @Test
    fun testContainerInstanceHoldsExternalFolderName() {
        val instance = ContainerInstance(
            id = "test_ubuntu",
            name = "Ubuntu Container",
            distroId = "ubuntu_26_04",
            distroName = "Ubuntu 26.04",
            rootDirPath = "/data/data/com.devwithzachary.completelinuxinstaller/files/containers/test_ubuntu/rootfs",
            externalFolderName = "ubuntuContainer"
        )
        assertEquals("ubuntuContainer", instance.externalFolderName)
    }

    @Test
    fun testJsonSerializationRoundTrip() {
        val instance = ContainerInstance(
            id = "test_void",
            name = "Void Linux",
            distroId = "void_glibc",
            distroName = "Void Linux (GLIBC)",
            rootDirPath = "/tmp/rootfs",
            externalFolderName = "voidLinux"
        )

        val obj = JSONObject().apply {
            put("id", instance.id)
            put("name", instance.name)
            put("distroId", instance.distroId)
            put("distroName", instance.distroName)
            put("rootDirPath", instance.rootDirPath)
            put("externalFolderName", instance.externalFolderName)
        }

        val parsedFolder = obj.optString("externalFolderName", "")
        assertEquals("voidLinux", parsedFolder)
    }

    @Test
    fun testLegacyJsonWithoutExternalFolderDefaultsToEmpty() {
        val legacyJson = """
            {
                "id": "legacy_ubuntu",
                "name": "Ubuntu 26.04",
                "distroId": "ubuntu_26_04",
                "rootDirPath": "/tmp/rootfs"
            }
        """.trimIndent()

        val obj = JSONObject(legacyJson)
        val parsedFolder = obj.optString("externalFolderName", "")
        assertEquals("", parsedFolder)
    }
}
