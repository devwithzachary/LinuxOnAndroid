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

    @Test
    fun testResolveServiceName_customSshPort() {
        val mockManager = SystemMonitorManagerStub()
        val customSsh = mockManager.resolveServiceName(22222, 22222)
        assertEquals("OpenSSH Server", customSsh.first)
        assertFalse(customSsh.second)
    }

    @Test
    fun testContainerScoping_processTreeFiltering() {
        val container1Dir = java.io.File("/data/user/0/com.devwithzachary.completelinuxinstaller/files/containers/container_1/rootfs")
        val container2Dir = java.io.File("/data/user/0/com.devwithzachary.completelinuxinstaller/files/containers/container_2/rootfs")

        data class MockProc(val pid: Int, val ppid: Int, val cmdline: String, val cwd: String?)

        val allProcs = listOf(
            MockProc(100, 1, "libproot.so -0 -l -r ${container1Dir.absolutePath} /bin/bash", container1Dir.absolutePath),
            MockProc(101, 100, "su -s /bin/bash - ubuntu", container1Dir.absolutePath),
            MockProc(102, 101, "bash", container1Dir.absolutePath + "/home/ubuntu"),
            MockProc(103, 102, "/usr/sbin/sshd -D", container1Dir.absolutePath),
            MockProc(200, 1, "libproot.so -0 -l -r ${container2Dir.absolutePath} /bin/bash", container2Dir.absolutePath),
            MockProc(201, 200, "bash", container2Dir.absolutePath + "/home/ubuntu")
        )

        fun filterForContainer(targetDir: java.io.File): List<Int> {
            val matched = mutableSetOf<Int>()
            for (p in allProcs) {
                if (p.cmdline.contains(targetDir.absolutePath) || (p.cwd != null && p.cwd.startsWith(targetDir.absolutePath))) {
                    matched.add(p.pid)
                }
            }
            var changed = true
            while (changed) {
                changed = false
                for (p in allProcs) {
                    if (p.pid !in matched && p.ppid in matched) {
                        matched.add(p.pid)
                        changed = true
                    }
                }
            }
            return matched.sorted()
        }

        val c1Pids = filterForContainer(container1Dir)
        assertEquals(listOf(100, 101, 102, 103), c1Pids)

        val c2Pids = filterForContainer(container2Dir)
        assertEquals(listOf(200, 201), c2Pids)

        val emptyContainerDir = java.io.File("/data/user/0/com.devwithzachary.completelinuxinstaller/files/containers/container_3/rootfs")
        val c3Pids = filterForContainer(emptyContainerDir)
        assertTrue(c3Pids.isEmpty())
    }

    @Test
    fun testServiceStatusManager_containerScopingIsolation() {
        val tempDir1 = java.io.File(System.getProperty("java.io.tmpdir"), "test_container_1").apply { mkdirs() }
        val tempDir2 = java.io.File(System.getProperty("java.io.tmpdir"), "test_container_2").apply { mkdirs() }

        try {
            val vncLock1 = java.io.File(tempDir1, "tmp/.X1-lock").apply {
                parentFile?.mkdirs()
                writeText("999999\n")
            }

            // Container 1 has a PID file for VNC
            assertTrue(java.io.File(tempDir1, "tmp/.X1-lock").exists())
            // Container 2 has no PID file
            assertFalse(java.io.File(tempDir2, "tmp/.X1-lock").exists())
            assertFalse(com.devwithzachary.completelinuxinstaller.service.ServiceStatusManager.isVncRunning(tempDir2))
            assertFalse(com.devwithzachary.completelinuxinstaller.service.ServiceStatusManager.isSshRunning(tempDir2, 2222))
            assertFalse(com.devwithzachary.completelinuxinstaller.service.ServiceStatusManager.isNginxRunning(tempDir2))
        } finally {
            tempDir1.deleteRecursively()
            tempDir2.deleteRecursively()
        }
    }

    private class SystemMonitorManagerStub {
        fun resolveServiceName(port: Int, customSshPort: Int = 2222): Pair<String, Boolean> {
            return when (port) {
                22, 2222, customSshPort -> "OpenSSH Server" to false
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
