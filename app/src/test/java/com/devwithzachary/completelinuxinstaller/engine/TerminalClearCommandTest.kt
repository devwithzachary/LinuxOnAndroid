package com.devwithzachary.completelinuxinstaller.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TerminalClearCommandTest {

    private fun rowToString(row: Array<TerminalChar>): String {
        return row.map { it.ch }.joinToString("").trimEnd()
    }

    @Test
    fun testCsi3J_clearsScrollbackAndResetsScrollOffset() {
        val emulator = TerminalEmulator(cols = 40, rows = 5)

        // Generate lines that scroll into scrollback
        for (i in 1..10) {
            val line = "Log line $i\r\n"
            emulator.appendBytes(line.toByteArray(), line.length)
        }

        // Scrollback should have accumulated lines
        assertTrue("Scrollback should have lines", emulator.scrollback.isNotEmpty())
        val initialScrollbackSize = emulator.scrollback.size

        // Scroll up into history
        emulator.scrollUp(3)
        assertTrue("Scroll offset should be > 0", emulator.scrollOffset > 0)

        // Send CSI 3 J (Erase Saved Lines)
        val clearScrollbackSeq = "\u001B[3J"
        emulator.appendBytes(clearScrollbackSeq.toByteArray(), clearScrollbackSeq.length)

        // Verify scrollback is now empty and scrollOffset is 0
        assertEquals(0, emulator.scrollback.size)
        assertEquals(0, emulator.scrollOffset)
    }

    @Test
    fun testClearCommandSequence_clearsScreenAndScrollback() {
        val emulator = TerminalEmulator(cols = 40, rows = 5)

        for (i in 1..10) {
            val line = "Output $i\r\n"
            emulator.appendBytes(line.toByteArray(), line.length)
        }

        assertTrue("Scrollback should have lines before clear", emulator.scrollback.isNotEmpty())

        // Standard Linux clear sequence: \033[H\033[2J\033[3J
        val clearCmd = "\u001B[H\u001B[2J\u001B[3J"
        emulator.appendBytes(clearCmd.toByteArray(), clearCmd.length)

        assertEquals("Cursor row should be 0", 0, emulator.cursorY)
        assertEquals("Cursor col should be 0", 0, emulator.cursorX)
        assertEquals("Scrollback should be empty", 0, emulator.scrollback.size)
        assertEquals("ScrollOffset should be 0", 0, emulator.scrollOffset)

        for (r in 0 until emulator.rows) {
            assertEquals("Screen row $r should be blank", "", rowToString(emulator.getRenderRow(r)))
        }
    }

    @Test
    fun testMultiParamCsi2And3J_clearsScreenAndScrollback() {
        val emulator = TerminalEmulator(cols = 40, rows = 5)

        for (i in 1..8) {
            val line = "Line $i\r\n"
            emulator.appendBytes(line.toByteArray(), line.length)
        }

        assertTrue(emulator.scrollback.isNotEmpty())

        // Compound sequence: \033[2;3J
        val compoundClear = "\u001B[2;3J"
        emulator.appendBytes(compoundClear.toByteArray(), compoundClear.length)

        assertEquals("Scrollback must be empty after 2;3J", 0, emulator.scrollback.size)
        assertEquals("ScrollOffset must be 0 after 2;3J", 0, emulator.scrollOffset)

        for (r in 0 until emulator.rows) {
            assertEquals("Row $r should be cleared", "", rowToString(emulator.getRenderRow(r)))
        }
    }

    @Test
    fun testCsi2J_clearsScreenWithoutClearingScrollback() {
        val emulator = TerminalEmulator(cols = 40, rows = 5)

        // Print lines to push some to scrollback and some on screen
        for (i in 1..8) {
            val line = "Old line $i\r\n"
            emulator.appendBytes(line.toByteArray(), line.length)
        }

        val scrollbackCount = emulator.scrollback.size
        assertTrue(scrollbackCount > 0)

        // CSI 2 J clears visible screen only (preserves scrollback for full-screen apps like nano/less)
        val clearScreenOnly = "\u001B[2J"
        emulator.appendBytes(clearScreenOnly.toByteArray(), clearScreenOnly.length)

        assertEquals("Scrollback should be preserved by 2J alone", scrollbackCount, emulator.scrollback.size)
        assertEquals("ScrollOffset should be reset to active screen", 0, emulator.scrollOffset)

        for (r in 0 until emulator.rows) {
            assertEquals("Visible row $r should be empty", "", rowToString(emulator.getRenderRow(r)))
        }
    }

    @Test
    fun testEscC_resetsTerminalCompletely() {
        val emulator = TerminalEmulator(cols = 40, rows = 5)

        for (i in 1..8) {
            val line = "Line $i\r\n"
            emulator.appendBytes(line.toByteArray(), line.length)
        }

        emulator.scrollUp(2)
        assertTrue(emulator.scrollback.isNotEmpty())
        assertTrue(emulator.scrollOffset > 0)

        // ESC c (RIS - Reset to Initial State)
        val resetSeq = "\u001Bc"
        emulator.appendBytes(resetSeq.toByteArray(), resetSeq.length)

        assertEquals("Cursor row must be 0", 0, emulator.cursorY)
        assertEquals("Cursor col must be 0", 0, emulator.cursorX)
        assertEquals("Scrollback must be cleared", 0, emulator.scrollback.size)
        assertEquals("ScrollOffset must be 0", 0, emulator.scrollOffset)

        for (r in 0 until emulator.rows) {
            assertEquals("Row $r should be empty", "", rowToString(emulator.getRenderRow(r)))
        }
    }

    @Test
    fun testClearTerminal_methodClearsEverything() {
        val emulator = TerminalEmulator(cols = 40, rows = 5)

        for (i in 1..8) {
            val line = "Data $i\r\n"
            emulator.appendBytes(line.toByteArray(), line.length)
        }

        emulator.scrollUp(1)
        assertTrue(emulator.scrollback.isNotEmpty())

        emulator.clearTerminal()

        assertEquals(0, emulator.cursorY)
        assertEquals(0, emulator.cursorX)
        assertEquals(0, emulator.scrollback.size)
        assertEquals(0, emulator.scrollOffset)
        assertEquals("", emulator.getAllText())
    }
}
