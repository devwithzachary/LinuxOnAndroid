package com.devwithzachary.completelinuxinstaller.engine

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UnixCryptTest {

    @Test
    fun testSha512Crypt_standardTestVector() {
        val password = "password"
        val expected = "\$6\$saltstring\$adDbXsJjcDlq2662QPgd.tkSOVmnG9Tt3oXl4HR60SusC3AGjirnDenVZp3DGwLwqy6iYKCzannhaX9DR72nN1"

        assertTrue("Expected password to verify against Ulrich Drepper SHA-512 vector", UnixCrypt.verify(password, expected))
        assertFalse("Wrong password must fail", UnixCrypt.verify("wrongpassword", expected))
    }

    @Test
    fun testSha512Crypt_customRounds() {
        val password = "password"
        val computed = org.apache.commons.codec.digest.Crypt.crypt(password, "\$6\$rounds=1000\$roundsetup")
        assertTrue(computed.startsWith("\$6\$rounds=1000\$roundsetup\$"))
        assertTrue(UnixCrypt.verify(password, computed))
        assertFalse(UnixCrypt.verify("incorrect", computed))
    }

    @Test
    fun testSha256Crypt_standardTestVector() {
        val password = "password"
        val computed = org.apache.commons.codec.digest.Crypt.crypt(password, "\$5\$saltstring")
        println("SHA256 Computed: $computed")
        assertTrue(UnixCrypt.verify(password, computed))
        assertFalse(UnixCrypt.verify("wrong", computed))
    }

    @Test
    fun testMd5Crypt() {
        val password = "password"
        val computed = org.apache.commons.codec.digest.Crypt.crypt(password, "\$1\$saltstri")
        assertTrue(computed.startsWith("\$1\$saltstri\$"))
        assertTrue(UnixCrypt.verify(password, computed))
        assertFalse(UnixCrypt.verify("wrong", computed))
    }

    @Test
    fun testVerify_deviceAlpineHash() {
        // Actual hash from Alpine container on device with password "root"
        val rootHash = "\$6\$/X52jSQlcQ79wsyk\$51iwfwMFPO8Qf8Dzneb5wEuCkfBH4gI1tZfVS8Kj9CgguyoWRr6t0qeOqxsLNjUZSR89Wv1X8Auiek.74RVJT/"
        assertTrue("Root password 'root' must match device Alpine hash", UnixCrypt.verify("root", rootHash))
        assertFalse("Wrong password must fail verification", UnixCrypt.verify("wrongpwd", rootHash))
    }

    @Test
    fun testVerify_unlockedOrEmpty() {
        assertTrue(UnixCrypt.verify("any", ""))
        assertTrue(UnixCrypt.verify("any", "*"))
        assertTrue(UnixCrypt.verify("any", "!"))
        assertTrue(UnixCrypt.verify("any", "!*"))
        assertTrue(UnixCrypt.verify("any", "x"))
    }
}
