package com.devwithzachary.completelinuxinstaller.engine.desktop

import org.junit.Assert.*
import org.junit.Test

class DesktopSessionManagerTest {

    @Test
    fun testConstants_ports() {
        assertEquals(5901, DesktopSessionManager.DEFAULT_VNC_PORT)
    }

    @Test
    fun testDesktopTouchMode_labels() {
        assertEquals("Touch Screen", DesktopTouchMode.DIRECT_TOUCH.label)
        assertEquals("Trackpad", DesktopTouchMode.TRACKPAD.label)
    }

    @Test
    fun testDesktopUiState_defaults() {
        val state = DesktopUiState()
        assertEquals(DesktopTouchMode.TRACKPAD, state.touchMode)
        assertEquals("root", state.selectedUser)
        assertEquals(listOf("root"), state.availableUsers)
        assertEquals(DesktopResolution.AUTO, state.resolution)
        assertEquals(100, state.uiScalePercent)
    }

    @Test
    fun testVncKeyMapper_additionalKeys() {
        assertEquals(VncKeyMapper.KEY_DELETE, VncKeyMapper.androidKeyCodeToKeysym(android.view.KeyEvent.KEYCODE_FORWARD_DEL))
        assertEquals(VncKeyMapper.KEY_TAB, VncKeyMapper.androidKeyCodeToKeysym(android.view.KeyEvent.KEYCODE_TAB))
        assertEquals(VncKeyMapper.KEY_RETURN, VncKeyMapper.androidKeyCodeToKeysym(android.view.KeyEvent.KEYCODE_NUMPAD_ENTER))
        assertEquals(VncKeyMapper.KEY_LEFT, VncKeyMapper.androidKeyCodeToKeysym(android.view.KeyEvent.KEYCODE_DPAD_LEFT))
        assertEquals(VncKeyMapper.KEY_RIGHT, VncKeyMapper.androidKeyCodeToKeysym(android.view.KeyEvent.KEYCODE_DPAD_RIGHT))
        assertEquals(VncKeyMapper.KEY_PAGE_UP, VncKeyMapper.androidKeyCodeToKeysym(android.view.KeyEvent.KEYCODE_PAGE_UP))
        assertEquals(VncKeyMapper.KEY_PAGE_DOWN, VncKeyMapper.androidKeyCodeToKeysym(android.view.KeyEvent.KEYCODE_PAGE_DOWN))
        assertEquals(VncKeyMapper.KEY_SUPER_LEFT, VncKeyMapper.androidKeyCodeToKeysym(android.view.KeyEvent.KEYCODE_META_LEFT))
    }

    @Test
    fun testDesktopResolution_scalingMath() {
        val baseW = 1080
        val baseH = 2160

        // 100% scale
        val scale100 = 100 / 100f
        val w100 = (((baseW / scale100).toInt() / 2) * 2)
        val h100 = (((baseH / scale100).toInt() / 2) * 2)
        assertEquals(1080, w100)
        assertEquals(2160, h100)

        // 150% scale
        val scale150 = 150 / 100f
        val w150 = (((baseW / scale150).toInt() / 2) * 2)
        val h150 = (((baseH / scale150).toInt() / 2) * 2)
        assertEquals(720, w150)
        assertEquals(1440, h150)
        // Aspect ratio exact match
        assertEquals((baseW.toDouble() / baseH), (w150.toDouble() / h150), 0.001)

        // 200% scale
        val scale200 = 200 / 100f
        val w200 = (((baseW / scale200).toInt() / 2) * 2)
        val h200 = (((baseH / scale200).toInt() / 2) * 2)
        assertEquals(540, w200)
        assertEquals(1080, h200)
        assertEquals((baseW.toDouble() / baseH), (w200.toDouble() / h200), 0.001)
    }

    @Test
    fun testScalePresets() {
        assertEquals(listOf(100, 125, 150, 175, 200), DesktopResolution.SCALE_PRESETS)
    }

    @Test
    fun testDesktopUiState_scaleDefaults() {
        val state = DesktopUiState(uiScalePercent = 150)
        assertEquals(150, state.uiScalePercent)
        assertEquals(DesktopResolution.AUTO, state.resolution)
    }
}
