package com.devwithzachary.completelinuxinstaller.engine

import com.devwithzachary.completelinuxinstaller.model.DistroCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class NetworkShimsTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testEnsureNetworkShimsCreatesAllShimsAndConfigFiles() {
        val rootfsDir = tempFolder.newFolder("rootfs")
        val etcDir = File(rootfsDir, "etc").apply { mkdirs() }
        File(etcDir, "hostname").writeText("UbuntuTest\n")

        NetworkShims.ensureNetworkShims(rootfsDir)

        val usrLocalBin = File(rootfsDir, "usr/local/bin")
        val netstatShim = File(usrLocalBin, "netstat")
        val ssShim = File(usrLocalBin, "ss")
        val ipShim = File(usrLocalBin, "ip")
        val hostnameShim = File(usrLocalBin, "hostname")
        val vncserverShim = File(usrLocalBin, "vncserver")

        assertTrue("netstat shim must exist", netstatShim.exists())
        assertTrue("ss shim must exist", ssShim.exists())
        assertTrue("ip shim must exist", ipShim.exists())
        assertTrue("hostname shim must exist", hostnameShim.exists())
        assertTrue("vncserver shim must exist", vncserverShim.exists())

        assertTrue("netstat shim must be executable", netstatShim.canExecute())
        assertTrue("ss shim must be executable", ssShim.canExecute())
        assertTrue("ip shim must be executable", ipShim.canExecute())
        assertTrue("hostname shim must be executable", hostnameShim.canExecute())
        assertTrue("vncserver shim must be executable", vncserverShim.canExecute())

        val ifaceConf = File(rootfsDir, "etc/network/proot_interfaces.conf")
        assertTrue("proot_interfaces.conf must exist", ifaceConf.exists())
        val ifaceContent = ifaceConf.readText()
        assertTrue("proot_interfaces.conf must declare PRIMARY_IP", ifaceContent.contains("PRIMARY_IP="))

        val hostsFile = File(rootfsDir, "etc/hosts")
        assertTrue("hosts file must exist", hostsFile.exists())
        val hostsContent = hostsFile.readText()
        assertTrue("hosts file must map 127.0.0.1 localhost", hostsContent.contains("127.0.0.1") && hostsContent.contains("localhost"))
    }

    @Test
    fun testNetstatShimIncludesFallbackAndPortParsers() {
        val rootfsDir = tempFolder.newFolder("rootfs_netstat")
        NetworkShims.ensureNetworkShims(rootfsDir)

        val content = File(rootfsDir, "usr/local/bin/netstat").readText()
        assertTrue("Must detect AF INET error", content.contains("AF INET"))
        assertTrue("Must output standard netstat table header", content.contains("Active Internet connections (only servers)"))
        assertTrue("Must parse Xvnc process", content.contains("Xvnc"))
        assertTrue("Must parse -rfbport argument", content.contains("rfbport"))
        assertTrue("Must parse sshd process", content.contains("sshd"))
        assertTrue("Must parse nginx process", content.contains("nginx"))
        assertTrue("Must parse ttyd process", content.contains("ttyd"))
    }

    @Test
    fun testSsShimIncludesFallbackForNetlinkPermissionDenied() {
        val rootfsDir = tempFolder.newFolder("rootfs_ss")
        NetworkShims.ensureNetworkShims(rootfsDir)

        val content = File(rootfsDir, "usr/local/bin/ss").readText()
        assertTrue("Must handle netlink permission denied", content.contains("Cannot open netlink socket"))
        assertTrue("Must output standard ss columns", content.contains("Local Address:Port") && content.contains("Peer Address:Port"))
        assertTrue("Must parse Xvnc sessions", content.contains("Xvnc"))
        assertTrue("Must parse sshd sessions", content.contains("sshd"))
    }

    @Test
    fun testIpShimIncludesFallbackForSocketNotConnected() {
        val rootfsDir = tempFolder.newFolder("rootfs_ip")
        NetworkShims.ensureNetworkShims(rootfsDir)

        val content = File(rootfsDir, "usr/local/bin/ip").readText()
        assertTrue("Must handle Socket not connected error", content.contains("Socket not connected"))
        assertTrue("Must parse proot_interfaces.conf", content.contains("/etc/network/proot_interfaces.conf"))
        assertTrue("Must format ip addr lo fallback", content.contains("link/loopback"))
    }

    @Test
    fun testHostnameShimResolvesPrimaryLanIpForDashI() {
        val rootfsDir = tempFolder.newFolder("rootfs_hostname")
        val etcDir = File(rootfsDir, "etc").apply { mkdirs() }
        File(etcDir, "hostname").writeText("TestHost\n")
        NetworkShims.ensureNetworkShims(rootfsDir)

        val content = File(rootfsDir, "usr/local/bin/hostname").readText()
        assertTrue("Must handle -i flag", content.contains("-i"))
        assertTrue("Must check PRIMARY_IP in proot_interfaces.conf", content.contains("PRIMARY_IP"))
        assertTrue("Must check /etc/hosts for LAN IP", content.contains("/etc/hosts"))
    }

    @Test
    fun testVncserverShimSupportsListKillAndHelp() {
        val rootfsDir = tempFolder.newFolder("rootfs_vnc")
        NetworkShims.ensureNetworkShims(rootfsDir)

        val content = File(rootfsDir, "usr/local/bin/vncserver").readText()
        assertTrue("Must support -list action", content.contains("-list"))
        assertTrue("Must support -kill action", content.contains("-kill"))
        assertTrue("Must support -help action", content.contains("-help"))
        assertTrue("Must display TigerVNC header for -list", content.contains("TigerVNC server sessions:"))
        assertTrue("Must print connection hints", content.contains("-> Local connection:") && content.contains("-> List sessions:"))
        assertTrue("Must clean X locks on kill", content.contains("/tmp/.X\${DISP_NUM}-lock"))
    }

    @Test
    fun testGetPrimaryLanIpSelection() {
        val loOnly = listOf(
            NetworkShims.NetInterfaceData(
                index = 1,
                name = "lo",
                ipv4Addresses = listOf("127.0.0.1/8"),
                ipv6Addresses = listOf("::1/128"),
                macAddress = "00:00:00:00:00:00",
                mtu = 65536,
                isUp = true,
                isLoopback = true
            )
        )
        assertEquals("127.0.0.1", NetworkShims.getPrimaryLanIp(loOnly))

        val withWlan = listOf(
            NetworkShims.NetInterfaceData(
                index = 1,
                name = "lo",
                ipv4Addresses = listOf("127.0.0.1/8"),
                ipv6Addresses = listOf("::1/128"),
                macAddress = "00:00:00:00:00:00",
                mtu = 65536,
                isUp = true,
                isLoopback = true
            ),
            NetworkShims.NetInterfaceData(
                index = 36,
                name = "wlan0",
                ipv4Addresses = listOf("192.168.1.150/24"),
                ipv6Addresses = listOf("fe80::1/64"),
                macAddress = "11:22:33:44:55:66",
                mtu = 1500,
                isUp = true,
                isLoopback = false
            )
        )
        assertEquals("192.168.1.150", NetworkShims.getPrimaryLanIp(withWlan))
    }

    @Test
    fun testCommonVncserverWrapperInDistroCatalogContainsListAndKill() {
        val distro = DistroCatalog.VOID_ROLLING
        val cmdFactory = distro.softwarePackageCommands["xfce_desktop"]
        assertTrue("softwarePackageCommands must contain xfce_desktop", cmdFactory != null)
        val cmd = cmdFactory!!.invoke(1)
        assertTrue("Wrapper must support -list", cmd.contains("-list"))
        assertTrue("Wrapper must support -kill", cmd.contains("-kill"))
        assertTrue("Wrapper must support -help", cmd.contains("-help"))
        assertTrue("Wrapper must show sessions header", cmd.contains("TigerVNC server sessions:"))
        assertTrue("Wrapper must show connection hints", cmd.contains("-> Local connection:"))
    }
}
