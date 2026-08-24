package com.devwithzachary.completelinuxinstaller.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ServiceStatusManagerTest {

    @Test
    fun testContainerResourceStatus_Idle() {
        val status = ContainerResourceStatus(
            isTerminalActive = false,
            isSshActive = false,
            isVncActive = false,
            isNginxActive = false,
            memoryUsedMb = 0L
        )

        assertFalse(status.hasActiveServices)
        assertEquals("Container Standby", status.buildSummaryText())
    }

    @Test
    fun testContainerResourceStatus_TerminalActive() {
        val status = ContainerResourceStatus(
            isTerminalActive = true,
            isSshActive = false,
            isVncActive = false,
            isNginxActive = false,
            memoryUsedMb = 95L
        )

        assertTrue(status.hasActiveServices)
        assertEquals("Terminal Active • RAM: 95MB", status.buildSummaryText())
    }

    @Test
    fun testContainerResourceStatus_MultipleActiveServices() {
        val status = ContainerResourceStatus(
            isTerminalActive = true,
            isSshActive = true,
            sshPort = 2222,
            isVncActive = true,
            isNginxActive = true,
            memoryUsedMb = 180L
        )

        assertTrue(status.hasActiveServices)
        val text = status.buildSummaryText()
        assertTrue(text.contains("Terminal Active"))
        assertTrue(text.contains("SSH :2222"))
        assertTrue(text.contains("VNC :5901"))
        assertTrue(text.contains("NGINX :80"))
        assertTrue(text.contains("RAM: 180MB"))
    }

    @Test
    fun testContainerResourceStatus_CustomSshPort() {
        val status = ContainerResourceStatus(
            isTerminalActive = false,
            isSshActive = true,
            sshPort = 8022,
            isVncActive = false,
            isNginxActive = false,
            memoryUsedMb = 50L
        )

        assertTrue(status.hasActiveServices)
        assertEquals("SSH :8022 • RAM: 50MB", status.buildSummaryText())
    }

    @Test
    fun testForegroundServiceConstants() {
        assertEquals("com.devwithzachary.completelinuxinstaller.action.START_SERVICE", PRootForegroundService.ACTION_START)
        assertEquals("com.devwithzachary.completelinuxinstaller.action.STOP_SERVICE", PRootForegroundService.ACTION_STOP_SERVICE)
        assertEquals("com.devwithzachary.completelinuxinstaller.action.STOP_SESSION", PRootForegroundService.ACTION_STOP_SESSION)
        assertEquals("NAV_TARGET", PRootForegroundService.EXTRA_NAV_TARGET)
        assertEquals("TERMINAL", PRootForegroundService.NAV_TARGET_TERMINAL)
    }

    @Test
    fun testGetAppMemoryMb_isPositive() {
        val memory = ServiceStatusManager.getAppMemoryMb()
        assertTrue(memory >= 1L)
    }
}
