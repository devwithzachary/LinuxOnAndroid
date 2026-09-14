package com.devwithzachary.completelinuxinstaller.ui.screens.terminal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TerminalFontsTest {

    @Test
    fun testAvailableFonts_containsExpectedFontsAndCyberGlyphsAtEnd() {
        val fonts = TerminalFonts.AVAILABLE_FONTS
        assertTrue(fonts.contains(TerminalFonts.JETBRAINS_MONO))
        assertTrue(fonts.contains(TerminalFonts.UBUNTU_MONO))
        assertTrue(fonts.contains(TerminalFonts.MONOSPACE))
        assertTrue(fonts.contains(TerminalFonts.CURSIVE))
        assertTrue(fonts.contains(TerminalFonts.CASUAL))
        assertTrue(fonts.contains(TerminalFonts.SERIF))
        assertTrue(fonts.contains(TerminalFonts.SANS_SERIF))
        assertTrue(fonts.contains(TerminalFonts.CYBER_GLYPHS))
        assertEquals(8, fonts.size)
        // Ensure CyberGlyphs is positioned at the very end
        assertEquals(TerminalFonts.CYBER_GLYPHS, fonts.last())
    }

    @Test
    fun testDefaultFont_isJetBrainsMono() {
        assertEquals("JetBrains Mono", TerminalFonts.DEFAULT_FONT)
    }

    @Test
    fun testNormalizeFontName_validFonts_remainUnchanged() {
        for (font in TerminalFonts.AVAILABLE_FONTS) {
            assertEquals(font, TerminalFonts.normalizeFontName(font))
        }
    }

    @Test
    fun testNormalizeFontName_legacyAndInvalidFonts_fallbackToDefault() {
        assertEquals(TerminalFonts.DEFAULT_FONT, TerminalFonts.normalizeFontName(null))
        assertEquals(TerminalFonts.DEFAULT_FONT, TerminalFonts.normalizeFontName(""))
        assertEquals(TerminalFonts.DEFAULT_FONT, TerminalFonts.normalizeFontName("Comic Sans"))
        assertEquals(TerminalFonts.DEFAULT_FONT, TerminalFonts.normalizeFontName("Papyrus"))
        assertEquals(TerminalFonts.DEFAULT_FONT, TerminalFonts.normalizeFontName("Wingdings"))
    }

    @Test
    fun testGetComposeFontFamily_returnsNonNullForSupportedFonts() {
        for (font in TerminalFonts.AVAILABLE_FONTS) {
            assertNotNull(TerminalFonts.getComposeFontFamily(font))
        }
        assertNotNull(TerminalFonts.getComposeFontFamily("Unknown Font"))
    }

    @Test
    fun testIsMonospace_correctlyIdentifiesMonospaceFonts() {
        assertTrue(TerminalFonts.isMonospace(TerminalFonts.JETBRAINS_MONO))
        assertTrue(TerminalFonts.isMonospace(TerminalFonts.UBUNTU_MONO))
        assertTrue(TerminalFonts.isMonospace(TerminalFonts.MONOSPACE))
        assertTrue(TerminalFonts.isMonospace(TerminalFonts.CYBER_GLYPHS))
        assertTrue(TerminalFonts.isMonospace(null))
        assertTrue(TerminalFonts.isMonospace("Unknown"))

        org.junit.Assert.assertFalse(TerminalFonts.isMonospace(TerminalFonts.CURSIVE))
        org.junit.Assert.assertFalse(TerminalFonts.isMonospace(TerminalFonts.CASUAL))
        org.junit.Assert.assertFalse(TerminalFonts.isMonospace(TerminalFonts.SERIF))
        org.junit.Assert.assertFalse(TerminalFonts.isMonospace(TerminalFonts.SANS_SERIF))
    }
}
