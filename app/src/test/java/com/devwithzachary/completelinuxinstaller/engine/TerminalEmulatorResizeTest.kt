package com.devwithzachary.completelinuxinstaller.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TerminalEmulatorResizeTest {

    private fun rowToString(row: Array<TerminalChar>): String {
        return row.map { it.ch }.joinToString("").trimEnd()
    }

    @Test
    fun testResize_heightShrinkPushesOldLinesToScrollback() {
        val emulator = TerminalEmulator(cols = 20, rows = 10)
        for (i in 0..7) {
            val line = "line $i\r\n"
            emulator.appendBytes(line.toByteArray(), line.length)
        }
        // At this point, lines 0..7 have been printed.
        // Cursor is at row 8, col 0 (after 8 newlines)
        assertEquals(8, emulator.cursorY)
        assertEquals("line 7", rowToString(emulator.getRenderRow(7)))

        // Shrink height from 10 to 5 (e.g. keyboard opens)
        emulator.resize(newCols = 20, newRows = 5)

        // After resizing to 5 rows:
        // Rows should now contain the latest content up to cursor, with top lines pushed to scrollback
        assertEquals(4, emulator.cursorY)
        // Last active row was 8, newRows is 5 -> linesToScroll = 8 - 4 = 4
        // lines 0..3 pushed to scrollback
        // rows 0..4 should have lines 4, 5, 6, 7, and the empty cursor line 8
        assertEquals(4, emulator.scrollback.size)
        assertEquals("line 0", rowToString(emulator.scrollback[0]))
        assertEquals("line 1", rowToString(emulator.scrollback[1]))
        assertEquals("line 2", rowToString(emulator.scrollback[2]))
        assertEquals("line 3", rowToString(emulator.scrollback[3]))

        assertEquals("line 4", rowToString(emulator.getRenderRow(0)))
        assertEquals("line 5", rowToString(emulator.getRenderRow(1)))
        assertEquals("line 6", rowToString(emulator.getRenderRow(2)))
        assertEquals("line 7", rowToString(emulator.getRenderRow(3)))
    }

    @Test
    fun testResize_heightExpandPullsLinesFromScrollback() {
        val emulator = TerminalEmulator(cols = 20, rows = 10)
        for (i in 0..7) {
            val line = "line $i\r\n"
            emulator.appendBytes(line.toByteArray(), line.length)
        }

        // Shrink to 5
        emulator.resize(newCols = 20, newRows = 5)
        assertEquals(4, emulator.scrollback.size)
        assertEquals(4, emulator.cursorY)

        // Expand back to 10 (e.g. keyboard closes)
        emulator.resize(newCols = 20, newRows = 10)

        // All 4 lines pulled back from scrollback
        assertEquals(0, emulator.scrollback.size)
        assertEquals(8, emulator.cursorY)

        for (i in 0..7) {
            assertEquals("line $i", rowToString(emulator.getRenderRow(i)))
        }
    }

    @Test
    fun testResize_cursorAboveNewHeightDoesNotScroll() {
        val emulator = TerminalEmulator(cols = 20, rows = 10)
        val text = "hello"
        emulator.appendBytes(text.toByteArray(), text.length)
        assertEquals(0, emulator.cursorY)

        // Shrink to 5
        emulator.resize(newCols = 20, newRows = 5)
        assertEquals(0, emulator.scrollback.size)
        assertEquals(0, emulator.cursorY)
        assertEquals("hello", rowToString(emulator.getRenderRow(0)))
    }

    @Test
    fun testResize_widthChangePreservesRowCharacters() {
        val emulator = TerminalEmulator(cols = 20, rows = 5)
        val text = "1234567890"
        emulator.appendBytes(text.toByteArray(), text.length)

        // Expand width to 30
        emulator.resize(newCols = 30, newRows = 5)
        assertEquals(30, emulator.cols)
        assertEquals("1234567890", rowToString(emulator.getRenderRow(0)))

        // Shrink width to 5
        emulator.resize(newCols = 5, newRows = 5)
        assertEquals(5, emulator.cols)
        assertEquals("12345", rowToString(emulator.getRenderRow(0)))
    }
}
