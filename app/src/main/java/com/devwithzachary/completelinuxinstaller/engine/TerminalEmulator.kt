package com.devwithzachary.completelinuxinstaller.engine

import androidx.compose.ui.graphics.Color

data class TerminalChar(
    val ch: Char = ' ',
    val fgColor: Color = Color(0xFFE0E0E0),
    val bgColor: Color = Color.Transparent,
    val bold: Boolean = false,
    val underline: Boolean = false,
    val reverse: Boolean = false
)

class TerminalEmulator(
    var cols: Int = 80,
    var rows: Int = 24,
    val maxScrollback: Int = 2000
) {
    // Primary & Alternate Screen Buffers
    private var primaryGrid = Array(rows) { Array(cols) { TerminalChar() } }
    private var altGrid = Array(rows) { Array(cols) { TerminalChar() } }
    var grid = primaryGrid
        private set

    val scrollback = mutableListOf<Array<TerminalChar>>()

    var cursorX = 0
        private set
    var cursorY = 0
        private set
    var cursorVisible = true
        private set
    var appCursorKeys = false
        private set

    private var savedCursorX = 0
    private var savedCursorY = 0

    // Top and Bottom Scrolling Margins (DECSTBM)
    var scrollTop = 0
        private set
    var scrollBottom = rows - 1
        private set

    private var currentFg: Color = Color(0xFFE0E0E0)
    private var currentBg: Color = Color.Transparent
    private var isBold = false
    private var isUnderline = false
    private var isReverse = false

    private var inAltBuffer = false

    // ANSI Escape Sequence Parser State
    private enum class State { NORMAL, ESCAPE, CSI, OSC, CHARSET }
    private var state = State.NORMAL
    private val csiParams = StringBuilder()

    // 16 Standard ANSI Colors
    private val ansiColors = arrayOf(
        Color(0xFF000000), // Black
        Color(0xFFCD0000), // Red
        Color(0xFF00CD00), // Green
        Color(0xFFCDCD00), // Yellow
        Color(0xFF0000EE), // Blue
        Color(0xFFCD00CD), // Magenta
        Color(0xFF00CDCD), // Cyan
        Color(0xFFE5E5E5), // White
        Color(0xFF7F7F7F), // Bright Black
        Color(0xFFFF0000), // Bright Red
        Color(0xFF00FF00), // Bright Green
        Color(0xFFFFFF00), // Bright Yellow
        Color(0xFF5C5CFF), // Bright Blue
        Color(0xFFFF00FF), // Bright Magenta
        Color(0xFF00FFFF), // Bright Cyan
        Color(0xFFFFFFFF)  // Bright White
    )

    fun resize(newCols: Int, newRows: Int) {
        if (newCols <= 0 || newRows <= 0) return
        val oldCols = cols
        val oldRows = rows
        cols = newCols
        rows = newRows

        primaryGrid = Array(rows) { r ->
            Array(cols) { c ->
                if (r < oldRows && c < oldCols) grid[r][c] else TerminalChar()
            }
        }
        altGrid = Array(rows) { Array(cols) { TerminalChar() } }
        grid = if (inAltBuffer) altGrid else primaryGrid

        scrollTop = 0
        scrollBottom = rows - 1

        cursorX = cursorX.coerceIn(0, cols - 1)
        cursorY = cursorY.coerceIn(0, rows - 1)
    }

    @Synchronized
    fun appendBytes(buffer: ByteArray, length: Int) {
        val text = String(buffer, 0, length, Charsets.UTF_8)
        for (ch in text) {
            processChar(ch)
        }
    }

    private fun processChar(ch: Char) {
        when (state) {
            State.NORMAL -> {
                when (ch) {
                    '\u001B' -> state = State.ESCAPE
                    '\r' -> cursorX = 0
                    '\n' -> lineFeed()
                    '\b' -> if (cursorX > 0) cursorX--
                    '\t' -> cursorX = ((cursorX / 8) + 1) * 8
                    '\u0007' -> {} // Bell
                    else -> {
                        if (ch >= ' ') {
                            if (cursorX >= cols) {
                                cursorX = 0
                                lineFeed()
                            }
                            grid[cursorY][cursorX] = TerminalChar(
                                ch = ch,
                                fgColor = if (isReverse) currentBg.takeIf { it != Color.Transparent } ?: Color.Black else currentFg,
                                bgColor = if (isReverse) currentFg else currentBg,
                                bold = isBold,
                                underline = isUnderline,
                                reverse = isReverse
                            )
                            cursorX++
                        }
                    }
                }
            }

            State.ESCAPE -> {
                when (ch) {
                    '[' -> {
                        state = State.CSI
                        csiParams.clear()
                    }
                    ']' -> {
                        state = State.OSC
                    }
                    '\\' -> { // ST (String Terminator: ESC \)
                        state = State.NORMAL
                    }
                    '(', ')', '*', '+' -> { // Designate Character Set G0..G3 (e.g. ESC ( B)
                        state = State.CHARSET
                    }
                    '7' -> { // DECSC - Save cursor
                        savedCursorX = cursorX
                        savedCursorY = cursorY
                        state = State.NORMAL
                    }
                    '8' -> { // DECRC - Restore cursor
                        cursorX = savedCursorX.coerceIn(0, cols - 1)
                        cursorY = savedCursorY.coerceIn(0, rows - 1)
                        state = State.NORMAL
                    }
                    'D' -> { // Index (IND)
                        lineFeed()
                        state = State.NORMAL
                    }
                    'M' -> { // Reverse Index (RI)
                        reverseIndex()
                        state = State.NORMAL
                    }
                    'E' -> { // Next Line (NEL)
                        cursorX = 0
                        lineFeed()
                        state = State.NORMAL
                    }
                    else -> state = State.NORMAL
                }
            }

            State.CHARSET -> {
                state = State.NORMAL
            }

            State.CSI -> {
                if (ch in '0'..'9' || ch == ';' || ch == '?' || ch == '>') {
                    csiParams.append(ch)
                } else {
                    handleCsiCommand(ch)
                    state = State.NORMAL
                }
            }

            State.OSC -> {
                when (ch) {
                    '\u0007' -> state = State.NORMAL
                    '\u001B' -> state = State.ESCAPE
                }
            }
        }
    }

    private fun lineFeed() {
        if (cursorY < scrollBottom) {
            cursorY++
        } else if (cursorY == scrollBottom) {
            // Scroll line within top & bottom scrolling margins [scrollTop .. scrollBottom]
            if (!inAltBuffer && scrollTop == 0) {
                scrollback.add(grid[0].copyOf())
                if (scrollback.size > maxScrollback) {
                    scrollback.removeAt(0)
                }
            }
            for (r in scrollTop until scrollBottom) {
                grid[r] = grid[r + 1]
            }
            grid[scrollBottom] = Array(cols) { TerminalChar() }
        } else if (cursorY < rows - 1) {
            cursorY++
        }
    }

    private fun reverseIndex() {
        if (cursorY > scrollTop) {
            cursorY--
        } else if (cursorY == scrollTop) {
            for (r in scrollBottom downTo scrollTop + 1) {
                grid[r] = grid[r - 1]
            }
            grid[scrollTop] = Array(cols) { TerminalChar() }
        }
    }

    private fun handleCsiCommand(cmd: Char) {
        val paramStr = csiParams.toString()
        val isPrivate = paramStr.startsWith("?")
        val cleanParams = if (isPrivate) paramStr.substring(1) else paramStr
        val args = if (cleanParams.isEmpty()) emptyList() else cleanParams.split(";").mapNotNull { it.toIntOrNull() }

        fun getArg(index: Int, default: Int): Int = args.getOrNull(index) ?: default

        when (cmd) {
            'm' -> handleSgr(args)
            'H', 'f' -> { // Cursor position
                val r = (getArg(0, 1) - 1).coerceIn(0, rows - 1)
                val c = (getArg(1, 1) - 1).coerceIn(0, cols - 1)
                cursorY = r
                cursorX = c
            }
            'A' -> cursorY = (cursorY - getArg(0, 1)).coerceAtLeast(0) // Up
            'B' -> cursorY = (cursorY + getArg(0, 1)).coerceAtMost(rows - 1) // Down
            'C' -> cursorX = (cursorX + getArg(0, 1)).coerceAtMost(cols - 1) // Forward
            'D' -> cursorX = (cursorX - getArg(0, 1)).coerceAtLeast(0) // Back
            'G', '`' -> cursorX = (getArg(0, 1) - 1).coerceIn(0, cols - 1) // Cursor Horizontal Absolute
            'd' -> cursorY = (getArg(0, 1) - 1).coerceIn(0, rows - 1) // Vertical Line Position Absolute
            's' -> { // Save cursor position
                savedCursorX = cursorX
                savedCursorY = cursorY
            }
            'u' -> { // Restore cursor position
                cursorX = savedCursorX.coerceIn(0, cols - 1)
                cursorY = savedCursorY.coerceIn(0, rows - 1)
            }
            'r' -> { // DECSTBM - Set Top and Bottom Margins
                val top = (getArg(0, 1) - 1).coerceIn(0, rows - 1)
                val bottom = (getArg(1, rows) - 1).coerceIn(0, rows - 1)
                if (top < bottom) {
                    scrollTop = top
                    scrollBottom = bottom
                } else {
                    scrollTop = 0
                    scrollBottom = rows - 1
                }
                cursorY = scrollTop
                cursorX = 0
            }
            'J' -> { // Erase in display
                val mode = getArg(0, 0)
                when (mode) {
                    0 -> { // Clear cursor to end
                        clearRange(cursorY, cursorX, rows - 1, cols - 1)
                    }
                    1 -> { // Clear start to cursor
                        clearRange(0, 0, cursorY, cursorX)
                    }
                    2, 3 -> { // Clear whole screen
                        clearRange(0, 0, rows - 1, cols - 1)
                    }
                }
            }
            'K' -> { // Erase in line
                val mode = getArg(0, 0)
                when (mode) {
                    0 -> for (c in cursorX until cols) grid[cursorY][c] = TerminalChar()
                    1 -> for (c in 0..cursorX) grid[cursorY][c] = TerminalChar()
                    2 -> for (c in 0 until cols) grid[cursorY][c] = TerminalChar()
                }
            }
            'L' -> { // Insert Lines
                val count = getArg(0, 1).coerceIn(1, rows - cursorY)
                for (r in scrollBottom downTo cursorY + count) {
                    grid[r] = grid[r - count]
                }
                for (r in cursorY until cursorY + count) {
                    grid[r] = Array(cols) { TerminalChar() }
                }
            }
            'M' -> { // Delete Lines
                val count = getArg(0, 1).coerceIn(1, rows - cursorY)
                for (r in cursorY until scrollBottom - count + 1) {
                    grid[r] = grid[r + count]
                }
                for (r in scrollBottom - count + 1..scrollBottom) {
                    grid[r] = Array(cols) { TerminalChar() }
                }
            }
            'P' -> { // Delete Characters
                val count = getArg(0, 1).coerceIn(1, cols - cursorX)
                val line = grid[cursorY]
                for (c in cursorX until cols - count) {
                    line[c] = line[c + count]
                }
                for (c in cols - count until cols) {
                    line[c] = TerminalChar()
                }
            }
            '@' -> { // Insert Characters
                val count = getArg(0, 1).coerceIn(1, cols - cursorX)
                val line = grid[cursorY]
                for (c in cols - 1 downTo cursorX + count) {
                    line[c] = line[c - count]
                }
                for (c in cursorX until cursorX + count) {
                    line[c] = TerminalChar()
                }
            }
            'X' -> { // Erase Characters
                val count = getArg(0, 1).coerceIn(1, cols - cursorX)
                val line = grid[cursorY]
                for (c in cursorX until cursorX + count) {
                    line[c] = TerminalChar()
                }
            }
            'h' -> {
                val arg = getArg(0, 0)
                if (isPrivate) {
                    when (arg) {
                        1 -> appCursorKeys = true
                        25 -> cursorVisible = true
                        1049, 47 -> { // Enable alt screen buffer
                            inAltBuffer = true
                            grid = altGrid
                            clearRange(0, 0, rows - 1, cols - 1)
                        }
                    }
                } else if (arg == 1) {
                    appCursorKeys = true
                }
            }
            'l' -> {
                val arg = getArg(0, 0)
                if (isPrivate) {
                    when (arg) {
                        1 -> appCursorKeys = false
                        25 -> cursorVisible = false
                        1049, 47 -> { // Disable alt screen buffer
                            inAltBuffer = false
                            grid = primaryGrid
                        }
                    }
                } else if (arg == 1) {
                    appCursorKeys = false
                }
            }
        }
    }

    private fun clearRange(r1: Int, c1: Int, r2: Int, c2: Int) {
        for (r in r1..r2) {
            val startC = if (r == r1) c1 else 0
            val endC = if (r == r2) c2 else cols - 1
            for (c in startC..endC) {
                grid[r][c] = TerminalChar()
            }
        }
    }

    private fun handleSgr(args: List<Int>) {
        if (args.isEmpty()) {
            resetSgr()
            return
        }

        var i = 0
        while (i < args.size) {
            when (val code = args[i]) {
                0 -> resetSgr()
                1 -> isBold = true
                2 -> {} // Dim
                4 -> isUnderline = true
                7 -> isReverse = true
                22 -> isBold = false
                24 -> isUnderline = false
                27 -> isReverse = false
                in 30..37 -> currentFg = ansiColors[code - 30]
                39 -> currentFg = Color(0xFFE0E0E0)
                in 40..47 -> currentBg = ansiColors[code - 40]
                49 -> currentBg = Color.Transparent
                in 90..97 -> currentFg = ansiColors[code - 90 + 8]
                in 100..107 -> currentBg = ansiColors[code - 100 + 8]
                38, 48 -> {
                    if (i + 2 < args.size && args[i + 1] == 5) {
                        // 256 colors
                        val colorIdx = args[i + 2]
                        val parsed = parse256Color(colorIdx)
                        if (code == 38) currentFg = parsed else currentBg = parsed
                        i += 2
                    } else if (i + 4 < args.size && args[i + 1] == 2) {
                        // Truecolor RGB
                        val r = args[i + 2]
                        val g = args[i + 3]
                        val b = args[i + 4]
                        val parsed = Color(r, g, b)
                        if (code == 38) currentFg = parsed else currentBg = parsed
                        i += 4
                    }
                }
            }
            i++
        }
    }

    private fun resetSgr() {
        currentFg = Color(0xFFE0E0E0)
        currentBg = Color.Transparent
        isBold = false
        isUnderline = false
        isReverse = false
    }

    private fun parse256Color(index: Int): Color {
        return when {
            index in 0..15 -> ansiColors[index]
            index in 16..231 -> {
                val n = index - 16
                val r = (n / 36) * 51
                val g = ((n % 36) / 6) * 51
                val b = (n % 6) * 51
                Color(r, g, b)
            }
            index in 232..255 -> {
                val gray = (index - 232) * 10 + 8
                Color(gray, gray, gray)
            }
            else -> Color(0xFFE0E0E0)
        }
    }
}
