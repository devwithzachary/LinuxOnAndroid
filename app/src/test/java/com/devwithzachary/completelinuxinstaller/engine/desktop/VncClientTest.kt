package com.devwithzachary.completelinuxinstaller.engine.desktop

import org.junit.Assert.*
import org.junit.Test

class VncClientTest {

    @Test
    fun testReverseBits_invertsBitOrder() {
        assertEquals(0x80.toByte(), VncClient.reverseBits(0x01.toByte()))
        assertEquals(0x01.toByte(), VncClient.reverseBits(0x80.toByte()))
        assertEquals(0x00.toByte(), VncClient.reverseBits(0x00.toByte()))
        assertEquals(0xFF.toByte(), VncClient.reverseBits(0xFF.toByte()))
        assertEquals(0xA0.toByte(), VncClient.reverseBits(0x05.toByte()))

        // Symmetric property: reversing twice yields original byte
        val original = 0x5A.toByte()
        val roundtrip = VncClient.reverseBits(VncClient.reverseBits(original))
        assertEquals(original, roundtrip)
    }

    @Test
    fun testEncryptChallenge_validDesEncryption() {
        val challenge = ByteArray(16) { it.toByte() }
        val password = "secretpassword"

        val response1 = VncClient.encryptChallenge(challenge, password)
        assertNotNull(response1)
        assertEquals("Response must be exactly 16 bytes", 16, response1.size)

        // Deterministic output
        val response2 = VncClient.encryptChallenge(challenge, password)
        assertArrayEquals(response1, response2)

        // Different passwords produce different responses
        val diffResponse = VncClient.encryptChallenge(challenge, "otherpass")
        assertFalse("Different passwords must yield different ciphertext", response1.contentEquals(diffResponse))
    }

    @Test
    fun testDesktopResolution_presetsAndCustom() {
        assertEquals(1280, DesktopResolution.HD_720P.width)
        assertEquals(720, DesktopResolution.HD_720P.height)

        assertEquals(1920, DesktopResolution.FHD_1080P.width)
        assertEquals(1080, DesktopResolution.FHD_1080P.height)

        assertEquals(1024, DesktopResolution.XGA_4_3.width)
        assertEquals(768, DesktopResolution.XGA_4_3.height)

        assertTrue(DesktopResolution.DEFAULT_PRESETS.contains(DesktopResolution.AUTO))
        assertTrue(DesktopResolution.DEFAULT_PRESETS.contains(DesktopResolution.HD_720P))
        assertTrue(DesktopResolution.AUTO.isAutomatic)

        // Custom resolution enforces even dimensions
        val custom = DesktopResolution.createCustom(1115, 2483)
        assertEquals(1114, custom.width)
        assertEquals(2482, custom.height)
    }

    @Test
    fun testVncKeyMapper_charToKeysym() {
        assertEquals(0x61, VncKeyMapper.charToKeysym('a'))
        assertEquals(0x41, VncKeyMapper.charToKeysym('A'))
        assertEquals(0x31, VncKeyMapper.charToKeysym('1'))
        assertEquals(VncKeyMapper.KEY_RETURN, VncKeyMapper.charToKeysym('\n'))
        assertEquals(VncKeyMapper.KEY_RETURN, VncKeyMapper.charToKeysym('\r'))
        assertEquals(VncKeyMapper.KEY_TAB, VncKeyMapper.charToKeysym('\t'))
        assertEquals(VncKeyMapper.KEY_BACKSPACE, VncKeyMapper.charToKeysym('\b'))
    }

    @Test
    fun testVncKeyMapper_androidKeyCodeToKeysym() {
        assertEquals(VncKeyMapper.KEY_BACKSPACE, VncKeyMapper.androidKeyCodeToKeysym(android.view.KeyEvent.KEYCODE_DEL))
        assertEquals(VncKeyMapper.KEY_RETURN, VncKeyMapper.androidKeyCodeToKeysym(android.view.KeyEvent.KEYCODE_ENTER))
        assertEquals(VncKeyMapper.KEY_ESCAPE, VncKeyMapper.androidKeyCodeToKeysym(android.view.KeyEvent.KEYCODE_ESCAPE))
        assertEquals(VncKeyMapper.KEY_UP, VncKeyMapper.androidKeyCodeToKeysym(android.view.KeyEvent.KEYCODE_DPAD_UP))
        assertEquals(VncKeyMapper.KEY_DOWN, VncKeyMapper.androidKeyCodeToKeysym(android.view.KeyEvent.KEYCODE_DPAD_DOWN))
        assertEquals(VncKeyMapper.KEY_F1, VncKeyMapper.androidKeyCodeToKeysym(android.view.KeyEvent.KEYCODE_F1))
        assertEquals(VncKeyMapper.KEY_CONTROL_LEFT, VncKeyMapper.androidKeyCodeToKeysym(android.view.KeyEvent.KEYCODE_CTRL_LEFT))
    }

    @Test
    fun testDesktopUiState_defaults() {
        val state = DesktopUiState()
        assertEquals(DesktopStatus.IDLE, state.sessionStatus)
        assertEquals(DesktopTouchMode.TRACKPAD, state.touchMode)
        assertEquals(DesktopResolution.AUTO, state.resolution)
        assertEquals(100, state.uiScalePercent)
        assertEquals(100, DesktopResolution.AUTO.scalePercent)
        assertEquals(listOf(100, 125, 150, 175, 200), DesktopResolution.SCALE_PRESETS)
        assertTrue(state.isFitToScreen)
        assertFalse(state.isCtrlActive)
        assertFalse(state.isAltActive)
        assertFalse(state.isShiftActive)
    }
}
