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
    const val CYBER_GLYPHS = "CyberGlyphs"

    const val DEFAULT_FONT = JETBRAINS_MONO

    val AVAILABLE_FONTS = listOf(
        JETBRAINS_MONO,
        UBUNTU_MONO,
        MONOSPACE,
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

    /**
     * Cache typefaces to avoid repeatedly parsing font resources on every render frame.
     */
    private val typefaceCache = mutableMapOf<String, Typeface>()

    fun getTypeface(context: Context, fontName: String, bold: Boolean = false): Typeface {
        val cacheKey = "${fontName}_${if (bold) "bold" else "normal"}"
        typefaceCache[cacheKey]?.let { return it }

        val resId = when (fontName) {
            UBUNTU_MONO -> if (bold) R.font.ubuntu_mono_bold else R.font.ubuntu_mono_regular
            JETBRAINS_MONO, MONOSPACE, CYBER_GLYPHS -> if (bold) R.font.jetbrains_mono_bold else R.font.jetbrains_mono_regular
            else -> if (bold) R.font.jetbrains_mono_bold else R.font.jetbrains_mono_regular
        }

        val typeface = try {
            ResourcesCompat.getFont(context, resId)
                ?: if (bold) Typeface.create(Typeface.MONOSPACE, Typeface.BOLD) else Typeface.MONOSPACE
        } catch (_: Throwable) {
            if (bold) Typeface.create(Typeface.MONOSPACE, Typeface.BOLD) else Typeface.MONOSPACE
        }

        typefaceCache[cacheKey] = typeface
        return typeface
    }

    fun getComposeFontFamily(fontName: String): FontFamily {
        return when (fontName) {
            UBUNTU_MONO -> UbuntuMonoFontFamily
            JETBRAINS_MONO, MONOSPACE, CYBER_GLYPHS -> JetBrainsMonoFontFamily
            else -> JetBrainsMonoFontFamily
        }
    }

    fun normalizeFontName(fontName: String?): String {
        return when (fontName) {
            UBUNTU_MONO -> UBUNTU_MONO
            JETBRAINS_MONO -> JETBRAINS_MONO
            MONOSPACE -> MONOSPACE
            CYBER_GLYPHS -> CYBER_GLYPHS
            else -> DEFAULT_FONT
        }
    }
}
