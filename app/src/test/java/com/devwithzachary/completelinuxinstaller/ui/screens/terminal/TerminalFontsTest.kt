package com.devwithzachary.completelinuxinstaller.ui.screens.terminal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TerminalFontsTest {

    @Test
    fun testAvailableFonts_containsExpectedMonospaceFonts() {
        val fonts = TerminalFonts.AVAILABLE_FONTS
        assertTrue(fonts.contains(TerminalFonts.JETBRAINS_MONO))
        assertTrue(fonts.contains(TerminalFonts.UBUNTU_MONO))
        assertTrue(fonts.contains(TerminalFonts.MONOSPACE))
        assertTrue(fonts.contains(TerminalFonts.CYBER_GLYPHS))
        assertEquals(4, fonts.size)
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
        assertEquals(TerminalFonts.DEFAULT_FONT, TerminalFonts.normalizeFontName("Sans Serif"))
        assertEquals(TerminalFonts.DEFAULT_FONT, TerminalFonts.normalizeFontName("Serif"))
        assertEquals(TerminalFonts.DEFAULT_FONT, TerminalFonts.normalizeFontName("Cursive"))
        assertEquals(TerminalFonts.DEFAULT_FONT, TerminalFonts.normalizeFontName("Casual"))
        assertEquals(TerminalFonts.DEFAULT_FONT, TerminalFonts.normalizeFontName("Comic Sans"))
    }

    @Test
    fun testGetComposeFontFamily_returnsNonNullForSupportedFonts() {
        assertNotNull(TerminalFonts.getComposeFontFamily(TerminalFonts.JETBRAINS_MONO))
        assertNotNull(TerminalFonts.getComposeFontFamily(TerminalFonts.UBUNTU_MONO))
        assertNotNull(TerminalFonts.getComposeFontFamily(TerminalFonts.MONOSPACE))
        assertNotNull(TerminalFonts.getComposeFontFamily(TerminalFonts.CYBER_GLYPHS))
        assertNotNull(TerminalFonts.getComposeFontFamily("Unknown Font"))
    }
}
