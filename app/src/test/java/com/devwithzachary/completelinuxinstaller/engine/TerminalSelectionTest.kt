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
}
