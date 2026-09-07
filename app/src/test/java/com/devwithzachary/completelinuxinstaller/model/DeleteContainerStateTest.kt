package com.devwithzachary.completelinuxinstaller.model

import com.devwithzachary.completelinuxinstaller.ui.DeleteContainerState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeleteContainerStateTest {

    @Test
    fun testDeleteContainerState_idle() {
        val state: DeleteContainerState = DeleteContainerState.Idle
        assertEquals(DeleteContainerState.Idle, state)
    }

    @Test
    fun testDeleteContainerState_deleting() {
        val deleting = DeleteContainerState.Deleting(
            containerId = "container_123",
            containerName = "Arch Linux",
            distroName = "Arch Linux ARM",
            statusMessage = "Purging rootfs files, packages, and storage..."
        )

        assertEquals("container_123", deleting.containerId)
        assertEquals("Arch Linux", deleting.containerName)
        assertEquals("Arch Linux ARM", deleting.distroName)
        assertEquals("Purging rootfs files, packages, and storage...", deleting.statusMessage)
    }

    @Test
    fun testDeleteContainerState_deletingDefaultStatusMessage() {
        val deleting = DeleteContainerState.Deleting(
            containerId = "c1",
            containerName = "Ubuntu"
        )

        assertEquals("Deleting container...", deleting.statusMessage)
        assertNull(deleting.distroName)
    }

    @Test
    fun testDeleteContainerState_error() {
        val error = DeleteContainerState.Error(
            containerName = "Debian 12",
            errorMessage = "Failed to completely remove container files."
        )

        assertEquals("Debian 12", error.containerName)
        assertEquals("Failed to completely remove container files.", error.errorMessage)
    }

    @Test
    fun testDeleteContainerState_transitionFlow() {
        val transitions = mutableListOf<DeleteContainerState>()
        transitions.add(DeleteContainerState.Idle)

        transitions.add(
            DeleteContainerState.Deleting(
                containerId = "c1",
                containerName = "Void Linux",
                distroName = "Void Linux",
                statusMessage = "Closing active terminal sessions..."
            )
        )

        transitions.add(
            DeleteContainerState.Deleting(
                containerId = "c1",
                containerName = "Void Linux",
                distroName = "Void Linux",
                statusMessage = "Preparing filesystem permissions..."
            )
        )

        transitions.add(
            DeleteContainerState.Deleting(
                containerId = "c1",
                containerName = "Void Linux",
                distroName = "Void Linux",
                statusMessage = "Purging rootfs files, packages, and storage..."
            )
        )

        transitions.add(
            DeleteContainerState.Deleting(
                containerId = "c1",
                containerName = "Void Linux",
                distroName = "Void Linux",
                statusMessage = "Updating container configuration..."
            )
        )

        transitions.add(DeleteContainerState.Idle)

        assertEquals(6, transitions.size)
        assertTrue(transitions.first() is DeleteContainerState.Idle)
        assertTrue(transitions.last() is DeleteContainerState.Idle)
        assertEquals("Purging rootfs files, packages, and storage...", (transitions[3] as DeleteContainerState.Deleting).statusMessage)
    }
}
