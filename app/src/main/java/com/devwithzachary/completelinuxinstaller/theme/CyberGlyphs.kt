package com.devwithzachary.completelinuxinstaller.theme

object CyberGlyphs {

    fun transformChar(ch: Char): String {
        return when (ch) {
            'a', 'A' -> "✌"
            'b', 'B' -> "👌"
            'c', 'C' -> "👍"
            'd', 'D' -> "👎"
            'e', 'E' -> "👈"
            'f', 'F' -> "👉"
            'g', 'G' -> "👆"
            'h', 'H' -> "👇"
            'i', 'I' -> "🖐"
            'j', 'J' -> "☺"
            'k', 'K' -> "😐"
            'l', 'L' -> "☹"
            'm', 'M' -> "💣"
            'n', 'N' -> "☠"
            'o', 'O' -> "⚐"
            'p', 'P' -> "⚑"
            'q', 'Q' -> "✈"
            'r', 'R' -> "☼"
            's', 'S' -> "💧"
            't', 'T' -> "❄"
            'u', 'U' -> "🕇"
            'v', 'V' -> "🕈"
            'w', 'W' -> "✠"
            'x', 'X' -> "✡"
            'y', 'Y' -> "☸"
            'z', 'Z' -> "☯"
            '0' -> "⓪"
            '1' -> "①"
            '2' -> "②"
            '3' -> "③"
            '4' -> "④"
            '5' -> "⑤"
            '6' -> "⑥"
            '7' -> "⑦"
            '8' -> "⑧"
            '9' -> "⑨"
            ':' -> "❖"
            '/' -> "✂"
            '-' -> "✦"
            '~' -> "≈"
            '$' -> "💲"
            '#' -> "⌗"
            '@' -> "🌀"
            '.' -> "●"
            ' ' -> " "
            else -> ch.toString()
        }
    }

    fun transformText(text: String): String {
        val sb = StringBuilder(text.length * 2)
        for (i in text.indices) {
            sb.append(transformChar(text[i]))
        }
        return sb.toString()
    }
}
