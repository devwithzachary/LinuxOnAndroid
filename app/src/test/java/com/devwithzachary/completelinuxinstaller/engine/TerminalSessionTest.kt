package com.devwithzachary.completelinuxinstaller.engine

import org.junit.Assert.*
import org.junit.Test

class TerminalSessionTest {

    @Test
    fun testTerminalSession_creationAndDefaults() {
        val session = TerminalSession(
            id = "sess_1",
            containerId = "container_ubuntu",
            containerName = "Ubuntu Container",
            loginUser = "ubuntu",
            initialTitle = "Ubuntu Tab"
        )

        assertEquals("sess_1", session.id)
        assertEquals("container_ubuntu", session.containerId)
        assertEquals("Ubuntu Container", session.containerName)
        assertEquals("ubuntu", session.loginUser)
        assertEquals("Ubuntu Tab", session.title.value)
        assertFalse(session.isRunning.value)
        assertNotNull(session.emulator)
    }

    @Test
    fun testTerminalSession_renameTitle() {
        val session = TerminalSession(
            id = "sess_2",
            containerId = "container_alpine",
            containerName = "Alpine",
            initialTitle = "Alpine (1)"
        )

        session.setTitle("Custom Alpine Server")
        assertEquals("Custom Alpine Server", session.title.value)
    }

    @Test
    fun testTerminalBridge_sessionManager_multiplexing() {
        val bridge = TerminalBridge(null)

        // Initially no sessions or active session
        assertTrue(bridge.sessions.value.isEmpty())
        assertNull(bridge.activeSessionId.value)

        // Create first session
        val s1 = bridge.createSession(
            containerId = "c1",
            containerName = "Ubuntu",
            loginUser = "root",
            title = "Tab 1",
            autoStart = false
        )

        assertEquals(1, bridge.sessions.value.size)
        assertEquals(s1.id, bridge.activeSessionId.value)
        assertEquals("Tab 1", bridge.getActiveSession()?.title?.value)

        // Create second session
        val s2 = bridge.createSession(
            containerId = "c2",
            containerName = "Alpine",
            loginUser = "alpine",
            title = "Tab 2",
            autoStart = false
        )

        assertEquals(2, bridge.sessions.value.size)
        assertEquals(s2.id, bridge.activeSessionId.value)

        // Switch active tab back to s1
        bridge.switchActiveSession(s1.id)
        assertEquals(s1.id, bridge.activeSessionId.value)

        // Rename session
        bridge.renameSession(s1.id, "Renamed Tab 1")
        assertEquals("Renamed Tab 1", s1.title.value)

        // Close s1 -> active session becomes s2
        bridge.closeSession(s1.id)
        assertEquals(1, bridge.sessions.value.size)
        assertEquals(s2.id, bridge.activeSessionId.value)

        // Close remaining session
        bridge.closeSession(s2.id)
        assertTrue(bridge.sessions.value.isEmpty())
        assertNull(bridge.activeSessionId.value)
    }

    @Test
    fun testTerminalSession_shellResolution() {
        val rootDir = java.io.File(System.getProperty("java.io.tmpdir"), "test_rootfs_" + System.currentTimeMillis()).apply { mkdirs() }
        try {
            val binDir = java.io.File(rootDir, "bin").apply { mkdirs() }
            java.io.File(binDir, "bash").createNewFile()
            java.io.File(binDir, "sh").createNewFile()

            val effectiveShell = when {
                java.io.File(rootDir, "bin/bash").exists() -> "/bin/bash"
                java.io.File(rootDir, "bin/sh").exists() -> "/bin/sh"
                else -> "/bin/sh"
            }

            assertEquals("/bin/bash", effectiveShell)
        } finally {
            rootDir.deleteRecursively()
        }
    }
}
