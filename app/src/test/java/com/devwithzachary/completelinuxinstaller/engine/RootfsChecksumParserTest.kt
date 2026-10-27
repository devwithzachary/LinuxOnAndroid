package com.devwithzachary.completelinuxinstaller.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RootfsChecksumParserTest {

    @Test
    fun testParseExpectedHash_rawHex() {
        val rawSha256 = "d8438b48f657a7da0e0e1819d4e5f082e6c8ea1c1d63dd8c4dddfba6219a557c\n"
        assertEquals(
            "d8438b48f657a7da0e0e1819d4e5f082e6c8ea1c1d63dd8c4dddfba6219a557c",
            RootfsManager.parseExpectedHash(rawSha256, "ubuntu-base.tar.gz")
        )

        val rawMd5 = "fcae6d1945cbe257bbdbdaeec19b4931\r\n"
        assertEquals(
            "fcae6d1945cbe257bbdbdaeec19b4931",
            RootfsManager.parseExpectedHash(rawMd5, "archlinux.tar.gz")
        )
    }

    @Test
    fun testParseExpectedHash_gnuStyleMultiLine() {
        val ubuntuChecksums = """
            # Ubuntu Base 26.04 Checksums
            d8438b48f657a7da0e0e1819d4e5f082e6c8ea1c1d63dd8c4dddfba6219a557c *ubuntu-base-26.04-base-arm64.tar.gz
            0fa1c74167e452932f933f72141551066060c4a45a305f6244dfaaee6c7b94ad *ubuntu-base-26.04-base-armhf.tar.gz
            803975a5b51268615b6d51dcf85b73d740fe2df0ea57e793910c28340d049d9c *ubuntu-base-26.04-base-amd64.tar.gz
        """.trimIndent()

        assertEquals(
            "d8438b48f657a7da0e0e1819d4e5f082e6c8ea1c1d63dd8c4dddfba6219a557c",
            RootfsManager.parseExpectedHash(ubuntuChecksums, "ubuntu-base-26.04-base-arm64.tar.gz")
        )
        assertEquals(
            "0fa1c74167e452932f933f72141551066060c4a45a305f6244dfaaee6c7b94ad",
            RootfsManager.parseExpectedHash(ubuntuChecksums, "ubuntu-base-26.04-base-armhf.tar.gz")
        )
        assertEquals(
            "803975a5b51268615b6d51dcf85b73d740fe2df0ea57e793910c28340d049d9c",
            RootfsManager.parseExpectedHash(ubuntuChecksums, "ubuntu-base-26.04-base-amd64.tar.gz")
        )
    }

    @Test
    fun testParseExpectedHash_debianJenkinsFormat() {
        val debianChecksum = "eddb3bf20ee8aaae4c9e83060a1d4b68ba45a646c0d57549889700b0948967ee  debian-bookworm-rootfs.tar.xz\n"
        assertEquals(
            "eddb3bf20ee8aaae4c9e83060a1d4b68ba45a646c0d57549889700b0948967ee",
            RootfsManager.parseExpectedHash(debianChecksum, "debian-bookworm-rootfs.tar.xz")
        )
    }

    @Test
    fun testParseExpectedHash_fedoraBsdFormat() {
        val fedoraChecksum = """
            # Fedora-Container-44-1.7-aarch64.tar.xz: 104857600 bytes
            SHA256 (Fedora-Container-44-1.7-aarch64.tar.xz) = 75c3ef2fcde7bb5cf0a04918ef6f0dc09ca1e326c54784a0d8e8749fc0db6e3a
        """.trimIndent()

        assertEquals(
            "75c3ef2fcde7bb5cf0a04918ef6f0dc09ca1e326c54784a0d8e8749fc0db6e3a",
            RootfsManager.parseExpectedHash(fedoraChecksum, "Fedora-Container-44-1.7-aarch64.tar.xz")
        )
    }

    @Test
    fun testParseExpectedHash_archArmMd5() {
        val archMd5 = "fcae6d1945cbe257bbdbdaeec19b4931  ArchLinuxARM-aarch64-latest.tar.gz\n"
        assertEquals(
            "fcae6d1945cbe257bbdbdaeec19b4931",
            RootfsManager.parseExpectedHash(archMd5, "ArchLinuxARM-aarch64-latest.tar.gz")
        )
    }

    @Test
    fun testParseExpectedHash_pgpSignedChecksumFile() {
        val pgpSigned = """
            -----BEGIN PGP SIGNED MESSAGE-----
            Hash: SHA256

            934a3174ae5ca46473fa73f8a4e8d356cbbcfd023f7253459c3f15c714aeb1f6  kali-linux-2026-W43-rootfs-minimal-arm64.tar.xz
            -----BEGIN PGP SIGNATURE-----
            Version: GnuPG v2

            iQIcBAEBCAAGBQJk...
            -----END PGP SIGNATURE-----
        """.trimIndent()

        assertEquals(
            "934a3174ae5ca46473fa73f8a4e8d356cbbcfd023f7253459c3f15c714aeb1f6",
            RootfsManager.parseExpectedHash(pgpSigned, "kali-linux-2026-W43-rootfs-minimal-arm64.tar.xz")
        )
    }

    @Test
    fun testParseExpectedHash_notFound() {
        val checksums = """
            d8438b48f657a7da0e0e1819d4e5f082e6c8ea1c1d63dd8c4dddfba6219a557c *ubuntu-base-26.04-base-arm64.tar.gz
            0fa1c74167e452932f933f72141551066060c4a45a305f6244dfaaee6c7b94ad *ubuntu-base-26.04-base-armhf.tar.gz
        """.trimIndent()

        assertNull(RootfsManager.parseExpectedHash(checksums, "nonexistent-distro.tar.gz"))
        assertNull(RootfsManager.parseExpectedHash("", "ubuntu-base-26.04-base-arm64.tar.gz"))
    }
}
