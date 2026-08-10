package com.devwithzachary.completelinuxinstaller.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

data class TerminalTheme(
    val id: String,
    val name: String,
    val defaultFg: Color,
    val defaultBg: Color,
    val cursorColor: Color,
    val selectionColor: Color,
    val ansiColors: List<Color>
) {
    companion object {
        val DRACULA = TerminalTheme(
            id = "dracula",
            name = "Dracula",
            defaultFg = Color(0xFFF8F8F2),
            defaultBg = Color(0xFF282A36),
            cursorColor = Color(0xFFF8F8F2),
            selectionColor = Color(0xFF44475A),
            ansiColors = listOf(
                Color(0xFF21222C), Color(0xFFFF5555), Color(0xFF50FA7B), Color(0xFFF1FA8C),
                Color(0xFFBD93F9), Color(0xFFFF79C6), Color(0xFF8BE9FD), Color(0xFFF8F8F2),
                Color(0xFF6272A4), Color(0xFFFF6E6E), Color(0xFF69FF94), Color(0xFFFFFFA5),
                Color(0xD6ACFF), Color(0xFFFF92D0), Color(0xFFA4FFFF), Color(0xFFFFFFFF)
            )
        )

        val SOLARIZED_DARK = TerminalTheme(
            id = "solarized_dark",
            name = "Solarized Dark",
            defaultFg = Color(0xFF839496),
            defaultBg = Color(0xFF002B36),
            cursorColor = Color(0xFF839496),
            selectionColor = Color(0xFF073642),
            ansiColors = listOf(
                Color(0xFF073642), Color(0xFFDC322F), Color(0xFF859900), Color(0xFFB58900),
                Color(0xFF268BD2), Color(0xFFD33682), Color(0xFF2AA198), Color(0xFFEEE8D5),
                Color(0xFF002B36), Color(0xFFCB4B16), Color(0xFF586E75), Color(0xFF657B83),
                Color(0xFF839496), Color(0xFF6C71C4), Color(0xFF93A1A1), Color(0xFFFDF6E3)
            )
        )

        val MONOKAI = TerminalTheme(
            id = "monokai",
            name = "Monokai",
            defaultFg = Color(0xFFF8F8F2),
            defaultBg = Color(0xFF272822),
            cursorColor = Color(0xFFF8F8F0),
            selectionColor = Color(0xFF49483E),
            ansiColors = listOf(
                Color(0xFF272822), Color(0xFFF92672), Color(0xFFA6E22E), Color(0xFFE6DB74),
                Color(0xFF66D9EF), Color(0xFFAE81FF), Color(0xFFA1EFE4), Color(0xFFF8F8F2),
                Color(0xFF75715E), Color(0xFFFD5FF1), Color(0xFFA6E22E), Color(0xFFE6DB74),
                Color(0xFF66D9EF), Color(0xFFAE81FF), Color(0xFFA1EFE4), Color(0xFFF8F8F2)
            )
        )

        val ONE_DARK = TerminalTheme(
            id = "one_dark",
            name = "One Dark",
            defaultFg = Color(0xFFABB2BF),
            defaultBg = Color(0xFF282C34),
            cursorColor = Color(0xFF528BFF),
            selectionColor = Color(0xFF3E4451),
            ansiColors = listOf(
                Color(0xFF282C34), Color(0xFFE06C75), Color(0xFF98C379), Color(0xFFE5C07B),
                Color(0xFF61AFEF), Color(0xFFC678DD), Color(0xFF56B6C2), Color(0xFFABB2BF),
                Color(0xFF5C6370), Color(0xFFE06C75), Color(0xFF98C379), Color(0xFFE5C07B),
                Color(0xFF61AFEF), Color(0xFFC678DD), Color(0xFF56B6C2), Color(0xFFFFFFFF)
            )
        )

        val CYBERPUNK = TerminalTheme(
            id = "cyberpunk",
            name = "Cyberpunk",
            defaultFg = Color(0xFF00FF9F),
            defaultBg = Color(0xFF0D0221),
            cursorColor = Color(0xFFFF0055),
            selectionColor = Color(0xFF3A0066),
            ansiColors = listOf(
                Color(0xFF0D0221), Color(0xFFFF0055), Color(0xFF00FF9F), Color(0xFFFFE600),
                Color(0xFF00B8FF), Color(0xFFD900FF), Color(0xFF00F0FF), Color(0xFFE2E2E2),
                Color(0xFF2A085C), Color(0xFFFF3377), Color(0xFF33FFAF), Color(0xFFFFEB33),
                Color(0xFF33C6FF), Color(0xFFE033FF), Color(0xFF33F3FF), Color(0xFFFFFFFF)
            )
        )

        val PRESETS = listOf(DRACULA, SOLARIZED_DARK, MONOKAI, ONE_DARK, CYBERPUNK)

        fun getById(id: String): TerminalTheme {
            return PRESETS.firstOrNull { it.id == id } ?: DRACULA
        }

        fun colorToHex(color: Color): String {
            val argb = color.toArgb()
            return String.format("#%08X", argb)
        }

        fun hexToColor(hex: String, fallback: Color = Color.White): Color {
            return try {
                val cleanHex = hex.trim().replace("#", "")
                val colorInt = when (cleanHex.length) {
                    6 -> (0xFF000000.toInt() or cleanHex.toLong(16).toInt())
                    8 -> cleanHex.toLong(16).toInt()
                    else -> fallback.toArgb()
                }
                Color(colorInt)
            } catch (_: Exception) {
                fallback
            }
        }
    }
}
