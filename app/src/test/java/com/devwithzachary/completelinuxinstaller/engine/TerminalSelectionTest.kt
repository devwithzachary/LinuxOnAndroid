package com.devwithzachary.completelinuxinstaller.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class TerminalSelectionTest {

    @Test
    fun testGetWordAt_simpleWord() {
        val emulator = TerminalEmulator(cols = 40, rows = 10)
        val text = "echo \"Hello World\""
        emulator.appendBytes(text.toByteArray(), text.length)

        // Cursor/characters are at row 0: 'e','c','h','o',' ','"','H','e','l','l','o',' '...
        // Index 2 is 'h' in "echo" -> word from 0 to 3
        val wordEcho = emulator.getWordAt(row = 0, col = 2)
        assertEquals(0, wordEcho.first)
        assertEquals(3, wordEcho.second)

        // Index 8 is 'l' in "Hello" -> word from 6 to 10
        val wordHello = emulator.getWordAt(row = 0, col = 8)
        assertEquals(6, wordHello.first)
        assertEquals(10, wordHello.second)
    }

    @Test
    fun testGetWordAt_filePathAndVariable() {
        val emulator = TerminalEmulator(cols = 50, rows = 10)
        val text = "cat /etc/os-release \$VAR"
        emulator.appendBytes(text.toByteArray(), text.length)

        // Index 8 is 't' in "/etc/os-release" -> word from 4 to 18
        val wordPath = emulator.getWordAt(row = 0, col = 8)
        assertEquals(4, wordPath.first)
        assertEquals(18, wordPath.second)

        // Index 21 is 'A' in "$VAR" -> word from 20 to 23
        val wordVar = emulator.getWordAt(row = 0, col = 21)
        assertEquals(20, wordVar.first)
        assertEquals(23, wordVar.second)
    }

    @Test
    fun testGetWordAt_spaceReturnsSingleChar() {
        val emulator = TerminalEmulator(cols = 40, rows = 10)
        val text = "ls -la"
        emulator.appendBytes(text.toByteArray(), text.length)

        // Index 2 is ' '
        val space = emulator.getWordAt(row = 0, col = 2)
        assertEquals(2, space.first)
        assertEquals(2, space.second)
    }

    @Test
    fun testGetSelectedText_singleLine() {
        val emulator = TerminalEmulator(cols = 40, rows = 10)
        val text = "Hello Linux"
        emulator.appendBytes(text.toByteArray(), text.length)

        val selected = emulator.getSelectedText(startRow = 0, startCol = 6, endRow = 0, endCol = 10)
        assertEquals("Linux", selected)

        // Inverted selection order should still yield the exact same text
        val inverted = emulator.getSelectedText(startRow = 0, startCol = 10, endRow = 0, endCol = 6)
        assertEquals("Linux", inverted)
    }

    @Test
    fun testGetSelectedText_multiLineLinear() {
        val emulator = TerminalEmulator(cols = 20, rows = 10)
        val text = "First Line\r\nSecond Line\r\nThird Line"
        emulator.appendBytes(text.toByteArray(), text.length)

        // Select from "Line" on row 0 to "Second" on row 1
        val selected = emulator.getSelectedText(startRow = 0, startCol = 6, endRow = 1, endCol = 5)
        assertEquals("Line\nSecond", selected)
    }

    @Test
    fun testGetSelectedText_whileScrolledBack() {
        val emulator = TerminalEmulator(cols = 20, rows = 3)
        // Write enough lines to push content into scrollback history
        val text = "Line 1\r\nLine 2\r\nLine 3\r\nLine 4\r\nLine 5"
        emulator.appendBytes(text.toByteArray(), text.length)

        // Scroll back 2 lines into history
        emulator.scrollUp(2)

        val visibleText = emulator.getSelectedText(startRow = 0, startCol = 0, endRow = 0, endCol = 5)
        assertEquals("Line 1", visibleText)
    }

    @Test
    fun testGetSelectedText_multiScreenScrollbackAndGrid() {
        val emulator = TerminalEmulator(cols = 20, rows = 3)
        val text = "Line 1\r\nLine 2\r\nLine 3\r\nLine 4\r\nLine 5"
        emulator.appendBytes(text.toByteArray(), text.length)

        // Total buffer rows = 2 in scrollback + 3 in grid = 5 rows total
        assertEquals(5, emulator.totalBufferRows)

        // Select from Line 1 (buffer row 0) to Line 5 (buffer row 4)
        val fullBufferText = emulator.getSelectedText(startRow = 0, startCol = 0, endRow = 4, endCol = 5)
        val expected = "Line 1\nLine 2\nLine 3\nLine 4\nLine 5"
        assertEquals(expected, fullBufferText)
    }

    @Test
    fun testScreenToBufferAndBufferToScreenRow() {
        val emulator = TerminalEmulator(cols = 20, rows = 4)
        // Push 10 lines: 6 will be in scrollback, 4 in grid
        val lines = (1..10).joinToString("\r\n") { "Item $it" }
        emulator.appendBytes(lines.toByteArray(), lines.length)

        assertEquals(6, emulator.scrollback.size)
        assertEquals(10, emulator.totalBufferRows)

        // At scrollOffset = 0 (bottom of terminal):
        // Screen row 0 corresponds to grid[0], which is buffer row 6
        assertEquals(6, emulator.screenToBufferRow(0))
        assertEquals(9, emulator.screenToBufferRow(3))
        assertEquals(0, emulator.bufferToScreenRow(6))
        assertEquals(3, emulator.bufferToScreenRow(9))

        // When scrolled up by 3 lines:
        emulator.scrollUp(3)
        assertEquals(3, emulator.scrollOffset)
        // Screen row 0 corresponds to buffer row 6 - 3 = 3 (in scrollback)
        assertEquals(3, emulator.screenToBufferRow(0))
        assertEquals(6, emulator.screenToBufferRow(3))
        assertEquals(0, emulator.bufferToScreenRow(3))
        assertEquals(3, emulator.bufferToScreenRow(6))
    }

    @Test
    fun testGetWordAtBuffer_inScrollback() {
        val emulator = TerminalEmulator(cols = 30, rows = 3)
        val text = "first_word second_word\r\nline2\r\nline3\r\nline4"
        emulator.appendBytes(text.toByteArray(), text.length)

        // Buffer row 0 is in scrollback ("first_word second_word")
        val word1 = emulator.getWordAtBuffer(bufferRow = 0, col = 3)
        assertEquals(0, word1.first)
        assertEquals(9, word1.second) // "first_word" length 10

        val word2 = emulator.getWordAtBuffer(bufferRow = 0, col = 13)
        assertEquals(11, word2.first)
        assertEquals(21, word2.second) // "second_word"
    }

    @Test
    fun testBufferBoundaryClamping() {
        val emulator = TerminalEmulator(cols = 20, rows = 5)
        emulator.appendBytes("Boundary Test".toByteArray(), 13)

        // Out of bounds rows and cols on a single row should be safely clamped without throwing
        val singleRowText = emulator.getSelectedText(startRow = -10, startCol = -5, endRow = 0, endCol = 999)
        assertEquals("Boundary Test", singleRowText)

        // Clamping across the entire buffer should also work cleanly
        val fullBufferText = emulator.getSelectedText(startRow = -10, startCol = -5, endRow = 999, endCol = 999)
        assertEquals("Boundary Test\n\n\n\n", fullBufferText)
    }
}
