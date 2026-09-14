package com.devwithzachary.completelinuxinstaller.ui.screens.terminal

import android.content.Context
import android.graphics.Typeface
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.res.ResourcesCompat
import com.devwithzachary.completelinuxinstaller.R

object TerminalFonts {
    const val JETBRAINS_MONO = "JetBrains Mono"
    const val UBUNTU_MONO = "Ubuntu Mono"
    const val MONOSPACE = "Monospace"
    const val CURSIVE = "Cursive"
    const val CASUAL = "Casual"
    const val SERIF = "Serif"
    const val SANS_SERIF = "Sans Serif"
    const val CYBER_GLYPHS = "CyberGlyphs"

    const val DEFAULT_FONT = JETBRAINS_MONO

    val AVAILABLE_FONTS = listOf(
        JETBRAINS_MONO,
        UBUNTU_MONO,
        MONOSPACE,
        CURSIVE,
        CASUAL,
        SERIF,
        SANS_SERIF,
        CYBER_GLYPHS
    )

    val JetBrainsMonoFontFamily: FontFamily by lazy {
        FontFamily(
            Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
            Font(R.font.jetbrains_mono_bold, FontWeight.Bold)
        )
    }

    val UbuntuMonoFontFamily: FontFamily by lazy {
        FontFamily(
            Font(R.font.ubuntu_mono_regular, FontWeight.Normal),
            Font(R.font.ubuntu_mono_bold, FontWeight.Bold)
        )
    }

    val CursiveFontFamily: FontFamily by lazy {
        FontFamily.Cursive
    }

    val CasualFontFamily: FontFamily by lazy {
        try {
            FontFamily(Typeface.create("casual", Typeface.NORMAL))
        } catch (_: Throwable) {
            FontFamily.Cursive
        }
    }

    val SerifFontFamily: FontFamily by lazy {
        FontFamily.Serif
    }

    val SansSerifFontFamily: FontFamily by lazy {
        FontFamily.SansSerif
    }

    /**
     * Cache typefaces to avoid repeatedly parsing font resources on every render frame.
     */
    private val typefaceCache = mutableMapOf<String, Typeface>()

    fun getTypeface(context: Context, fontName: String, bold: Boolean = false): Typeface {
        val cacheKey = "${fontName}_${if (bold) "bold" else "normal"}"
        typefaceCache[cacheKey]?.let { return it }

        val style = if (bold) Typeface.BOLD else Typeface.NORMAL
        val typeface = try {
            when (fontName) {
                UBUNTU_MONO -> {
                    val resId = if (bold) R.font.ubuntu_mono_bold else R.font.ubuntu_mono_regular
                    ResourcesCompat.getFont(context, resId) ?: Typeface.create(Typeface.MONOSPACE, style)
                }
                CURSIVE -> Typeface.create("cursive", style)
                CASUAL -> Typeface.create("casual", style)
                SERIF -> Typeface.create(Typeface.SERIF, style)
                SANS_SERIF -> Typeface.create(Typeface.SANS_SERIF, style)
                JETBRAINS_MONO, MONOSPACE, CYBER_GLYPHS -> {
                    val resId = if (bold) R.font.jetbrains_mono_bold else R.font.jetbrains_mono_regular
                    ResourcesCompat.getFont(context, resId) ?: Typeface.create(Typeface.MONOSPACE, style)
                }
                else -> {
                    val resId = if (bold) R.font.jetbrains_mono_bold else R.font.jetbrains_mono_regular
                    ResourcesCompat.getFont(context, resId) ?: Typeface.create(Typeface.MONOSPACE, style)
                }
            }
        } catch (_: Throwable) {
            Typeface.create(Typeface.MONOSPACE, style)
        }

        typefaceCache[cacheKey] = typeface
        return typeface
    }

    fun getComposeFontFamily(fontName: String): FontFamily {
        return when (fontName) {
            UBUNTU_MONO -> UbuntuMonoFontFamily
            CURSIVE -> CursiveFontFamily
            CASUAL -> CasualFontFamily
            SERIF -> SerifFontFamily
            SANS_SERIF -> SansSerifFontFamily
            JETBRAINS_MONO, MONOSPACE, CYBER_GLYPHS -> JetBrainsMonoFontFamily
            else -> JetBrainsMonoFontFamily
        }
    }

    fun normalizeFontName(fontName: String?): String {
        return when (fontName) {
            UBUNTU_MONO -> UBUNTU_MONO
            JETBRAINS_MONO -> JETBRAINS_MONO
            MONOSPACE -> MONOSPACE
            CURSIVE -> CURSIVE
            CASUAL -> CASUAL
            SERIF -> SERIF
            SANS_SERIF -> SANS_SERIF
            CYBER_GLYPHS -> CYBER_GLYPHS
            else -> DEFAULT_FONT
        }
    }

    fun isMonospace(fontName: String?): Boolean {
        return when (fontName) {
            CURSIVE, CASUAL, SERIF, SANS_SERIF -> false
            UBUNTU_MONO, MONOSPACE, CYBER_GLYPHS, JETBRAINS_MONO -> true
            else -> true
        }
    }
}
