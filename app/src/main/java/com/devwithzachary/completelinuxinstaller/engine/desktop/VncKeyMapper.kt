package com.devwithzachary.completelinuxinstaller.engine.desktop

object VncKeyMapper {
    const val KEY_BACKSPACE = 0xFF08
    const val KEY_TAB = 0xFF09
    const val KEY_RETURN = 0xFF0D
    const val KEY_ESCAPE = 0xFF1B
    const val KEY_INSERT = 0xFF63
    const val KEY_DELETE = 0xFFFF
    const val KEY_HOME = 0xFF50
    const val KEY_END = 0xFF57
    const val KEY_PAGE_UP = 0xFF55
    const val KEY_PAGE_DOWN = 0xFF56
    const val KEY_LEFT = 0xFF51
    const val KEY_UP = 0xFF52
    const val KEY_RIGHT = 0xFF53
    const val KEY_DOWN = 0xFF54
    const val KEY_F1 = 0xFFBE
    const val KEY_F2 = 0xFFBF
    const val KEY_F3 = 0xFFC0
    const val KEY_F4 = 0xFFC1
    const val KEY_F5 = 0xFFC2
    const val KEY_F6 = 0xFFC3
    const val KEY_F7 = 0xFFC4
    const val KEY_F8 = 0xFFC5
    const val KEY_F9 = 0xFFC6
    const val KEY_F10 = 0xFFC7
    const val KEY_F11 = 0xFFC8
    const val KEY_F12 = 0xFFC9
    const val KEY_SHIFT_LEFT = 0xFFE1
    const val KEY_SHIFT_RIGHT = 0xFFE2
    const val KEY_CONTROL_LEFT = 0xFFE3
    const val KEY_CONTROL_RIGHT = 0xFFE4
    const val KEY_ALT_LEFT = 0xFFE9
    const val KEY_ALT_RIGHT = 0xFFEA
    const val KEY_SUPER_LEFT = 0xFFEB

    fun charToKeysym(char: Char): Int {
        val code = char.code
        return when {
            code in 0x20..0x7E -> code // standard printable ASCII
            char == '\n' || char == '\r' -> KEY_RETURN
            char == '\t' -> KEY_TAB
            char == '\b' -> KEY_BACKSPACE
            code > 0x7E -> code or 0x01000000 // Unicode direct mapping in X11
            else -> code
        }
    }

    fun androidKeyCodeToKeysym(keyCode: Int): Int? {
        return when (keyCode) {
            android.view.KeyEvent.KEYCODE_DEL -> KEY_BACKSPACE
            android.view.KeyEvent.KEYCODE_FORWARD_DEL -> KEY_DELETE
            android.view.KeyEvent.KEYCODE_TAB -> KEY_TAB
            android.view.KeyEvent.KEYCODE_ENTER,
            android.view.KeyEvent.KEYCODE_NUMPAD_ENTER -> KEY_RETURN
            android.view.KeyEvent.KEYCODE_ESCAPE -> KEY_ESCAPE
            android.view.KeyEvent.KEYCODE_DPAD_UP -> KEY_UP
            android.view.KeyEvent.KEYCODE_DPAD_DOWN -> KEY_DOWN
            android.view.KeyEvent.KEYCODE_DPAD_LEFT -> KEY_LEFT
            android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> KEY_RIGHT
            android.view.KeyEvent.KEYCODE_MOVE_HOME -> KEY_HOME
            android.view.KeyEvent.KEYCODE_MOVE_END -> KEY_END
            android.view.KeyEvent.KEYCODE_PAGE_UP -> KEY_PAGE_UP
            android.view.KeyEvent.KEYCODE_PAGE_DOWN -> KEY_PAGE_DOWN
            android.view.KeyEvent.KEYCODE_INSERT -> KEY_INSERT
            android.view.KeyEvent.KEYCODE_F1 -> KEY_F1
            android.view.KeyEvent.KEYCODE_F2 -> KEY_F2
            android.view.KeyEvent.KEYCODE_F3 -> KEY_F3
            android.view.KeyEvent.KEYCODE_F4 -> KEY_F4
            android.view.KeyEvent.KEYCODE_F5 -> KEY_F5
            android.view.KeyEvent.KEYCODE_F6 -> KEY_F6
            android.view.KeyEvent.KEYCODE_F7 -> KEY_F7
            android.view.KeyEvent.KEYCODE_F8 -> KEY_F8
            android.view.KeyEvent.KEYCODE_F9 -> KEY_F9
            android.view.KeyEvent.KEYCODE_F10 -> KEY_F10
            android.view.KeyEvent.KEYCODE_F11 -> KEY_F11
            android.view.KeyEvent.KEYCODE_F12 -> KEY_F12
            android.view.KeyEvent.KEYCODE_SHIFT_LEFT -> KEY_SHIFT_LEFT
            android.view.KeyEvent.KEYCODE_SHIFT_RIGHT -> KEY_SHIFT_RIGHT
            android.view.KeyEvent.KEYCODE_CTRL_LEFT -> KEY_CONTROL_LEFT
            android.view.KeyEvent.KEYCODE_CTRL_RIGHT -> KEY_CONTROL_RIGHT
            android.view.KeyEvent.KEYCODE_ALT_LEFT -> KEY_ALT_LEFT
            android.view.KeyEvent.KEYCODE_ALT_RIGHT -> KEY_ALT_RIGHT
            android.view.KeyEvent.KEYCODE_META_LEFT -> KEY_SUPER_LEFT
            else -> null
        }
    }
}
