package com.devwithzachary.completelinuxinstaller.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class TerminalModifierTest {

    @Test
    fun testNoModifiers_returnsUnchanged() {
        assertEquals("a", TerminalBridge.getModifiedSequence('a', isCtrl = false, isAlt = false))
        assertEquals("Z", TerminalBridge.getModifiedSequence('Z', isCtrl = false, isAlt = false))
        assertEquals("1", TerminalBridge.getModifiedSequence('1', isCtrl = false, isAlt = false))
        assertEquals("/", TerminalBridge.getModifiedSequence('/', isCtrl = false, isAlt = false))
    }

    @Test
    fun testCtrlModifier_standardAlphabet() {
        // Ctrl+A -> \u0001
        assertEquals("\u0001", TerminalBridge.getModifiedSequence('a', isCtrl = true, isAlt = false))
        assertEquals("\u0001", TerminalBridge.getModifiedSequence('A', isCtrl = true, isAlt = false))

        // Ctrl+C -> \u0003 (SIGINT)
        assertEquals("\u0003", TerminalBridge.getModifiedSequence('c', isCtrl = true, isAlt = false))
        assertEquals("\u0003", TerminalBridge.getModifiedSequence('C', isCtrl = true, isAlt = false))

        // Ctrl+D -> \u0004 (EOF)
        assertEquals("\u0004", TerminalBridge.getModifiedSequence('d', isCtrl = true, isAlt = false))

        // Ctrl+L -> \u000C (Clear screen)
        assertEquals("\u000C", TerminalBridge.getModifiedSequence('l', isCtrl = true, isAlt = false))
        assertEquals("\u000C", TerminalBridge.getModifiedSequence('L', isCtrl = true, isAlt = false))

        // Ctrl+Z -> \u001A (SIGTSTP)
        assertEquals("\u001A", TerminalBridge.getModifiedSequence('z', isCtrl = true, isAlt = false))
        assertEquals("\u001A", TerminalBridge.getModifiedSequence('Z', isCtrl = true, isAlt = false))
    }

    @Test
    fun testCtrlModifier_specialSymbols() {
        // Ctrl+[ -> Escape (\u001B)
        assertEquals("\u001B", TerminalBridge.getModifiedSequence('[', isCtrl = true, isAlt = false))

        // Ctrl+\ -> \u001C (SIGQUIT)
        assertEquals("\u001C", TerminalBridge.getModifiedSequence('\\', isCtrl = true, isAlt = false))

        // Ctrl+] -> \u001D
        assertEquals("\u001D", TerminalBridge.getModifiedSequence(']', isCtrl = true, isAlt = false))

        // Ctrl+Space -> NUL (\u0000)
        assertEquals("\u0000", TerminalBridge.getModifiedSequence(' ', isCtrl = true, isAlt = false))
        assertEquals("\u0000", TerminalBridge.getModifiedSequence('@', isCtrl = true, isAlt = false))
    }

    @Test
    fun testAltModifier_prependsEscape() {
        // Alt+f -> \u001Bf (Forward word in bash)
        assertEquals("\u001Bf", TerminalBridge.getModifiedSequence('f', isCtrl = false, isAlt = true))

        // Alt+b -> \u001Bb (Backward word in bash)
        assertEquals("\u001Bb", TerminalBridge.getModifiedSequence('b', isCtrl = false, isAlt = true))

        // Alt+. -> \u001B. (Last argument in bash)
        assertEquals("\u001B.", TerminalBridge.getModifiedSequence('.', isCtrl = false, isAlt = true))
    }

    @Test
    fun testCtrlAndAltCombined_prependsEscapeToControlByte() {
        // Ctrl+Alt+a -> \u001B\u0001
        assertEquals("\u001B\u0001", TerminalBridge.getModifiedSequence('a', isCtrl = true, isAlt = true))

        // Ctrl+Alt+l -> \u001B\u000C
        assertEquals("\u001B\u000C", TerminalBridge.getModifiedSequence('l', isCtrl = true, isAlt = true))
    }
}
