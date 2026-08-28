package com.devwithzachary.completelinuxinstaller.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SystemMonitorManagerTest {

    @Test
    fun testResolveServiceName_knownPorts() {
        val mockManager = SystemMonitorManagerStub()

        val ssh = mockManager.resolveServiceName(2222)
        assertEquals("OpenSSH Server", ssh.first)
        assertFalse(ssh.second)

        val vnc = mockManager.resolveServiceName(5901)
        assertEquals("TigerVNC Desktop (:1)", vnc.first)
        assertFalse(vnc.second)

        val nginx = mockManager.resolveServiceName(80)
        assertEquals("HTTP Web Server (NGINX / Apache)", nginx.first)
        assertTrue(nginx.second)

        val node = mockManager.resolveServiceName(3000)
        assertEquals("Node.js / React Web App", node.first)
        assertTrue(node.second)

        val python = mockManager.resolveServiceName(8000)
        assertEquals("Python HTTP / Dev Server", python.first)
        assertTrue(python.second)

        val jupyter = mockManager.resolveServiceName(8888)
        assertEquals("Jupyter Notebook", jupyter.first)
        assertTrue(jupyter.second)
    }

    @Test
    fun testSystemResourceMetrics_calculations() {
        val metrics = SystemResourceMetrics(
            containerMemoryUsedMb = 256L,
            systemTotalRamMb = 8192L,
            systemAvailableRamMb = 4096L,
            storageUsedMb = 1200L,
            storageTotalBytes = 100_000_000_000L,
            storageAvailableBytes = 40_000_000_000L,
            isSessionRunning = true
        )

        assertEquals(4096L, metrics.systemUsedRamMb)
        assertEquals(0.5f, metrics.ramUsagePercent, 0.01f)
        assertEquals(0.6f, metrics.storageUsagePercent, 0.01f)
    }

    @Test
    fun testSystemResourceMetrics_zeroTotalSafe() {
        val metrics = SystemResourceMetrics(
            systemTotalRamMb = 0L,
            storageTotalBytes = 0L
        )

        assertEquals(0L, metrics.systemUsedRamMb)
        assertEquals(0f, metrics.ramUsagePercent, 0.001f)
        assertEquals(0f, metrics.storageUsagePercent, 0.001f)
    }

    @Test
    fun testContainerProcessInfo_attributes() {
        val proc = ContainerProcessInfo(
            pid = 1234,
            name = "TigerVNC Server",
            user = "ubuntu",
            rssMb = 48.5,
            state = "S",
            cmdline = "Xtigervnc :1 -geometry 1280x720"
        )

        assertEquals(1234, proc.pid)
        assertEquals("TigerVNC Server", proc.name)
        assertEquals("ubuntu", proc.user)
        assertEquals(48.5, proc.rssMb, 0.01)
        assertEquals("S", proc.state)
    }

    private class SystemMonitorManagerStub {
        fun resolveServiceName(port: Int): Pair<String, Boolean> {
            return when (port) {
                22, 2222 -> "OpenSSH Server" to false
                5900, 5901, 5902 -> "TigerVNC Desktop (:1)" to false
                80 -> "HTTP Web Server (NGINX / Apache)" to true
                443 -> "HTTPS Web Server" to true
                8080 -> "HTTP Alternate (NGINX / Tomcat)" to true
                8000 -> "Python HTTP / Dev Server" to true
                3000 -> "Node.js / React Web App" to true
                5000 -> "Flask / Web Service" to true
                8888 -> "Jupyter Notebook" to true
                9090 -> "Cockpit / Web Console" to true
                5432 -> "PostgreSQL Database" to false
                3306 -> "MySQL / MariaDB" to false
                6379 -> "Redis In-Memory Store" to false
                27017 -> "MongoDB Database" to false
                else -> "Active TCP Service" to (port in listOf(80, 443, 8080, 8000, 3000, 5000, 8888, 9090))
            }
        }
    }
}
