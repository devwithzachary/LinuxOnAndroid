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
    var scrollOffset = 0
        private set

    fun scrollUp(lines: Int = 1) {
        if (scrollback.isNotEmpty()) {
            scrollOffset = (scrollOffset + lines).coerceIn(0, scrollback.size)
        }
    }

    fun scrollDown(lines: Int = 1) {
        scrollOffset = (scrollOffset - lines).coerceIn(0, scrollback.size)
    }

    fun scrollToBottom() {
        scrollOffset = 0
    }

    fun getRenderRow(r: Int): Array<TerminalChar> {
        val row = if (scrollOffset == 0 || scrollback.isEmpty()) {
            if (r < grid.size) grid[r] else Array(cols) { TerminalChar() }
        } else {
            val totalHistory = scrollback.size
            val targetIndex = (totalHistory + r) - scrollOffset
            when {
                targetIndex < 0 -> Array(cols) { TerminalChar() }
                targetIndex < totalHistory -> scrollback[targetIndex]
                else -> {
                    val gridIndex = targetIndex - totalHistory
                    if (gridIndex < grid.size) grid[gridIndex] else Array(cols) { TerminalChar() }
                }
            }
        }
        return if (row.size == cols) row else Array(cols) { c -> if (c < row.size) row[c] else TerminalChar() }
    }

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

    var theme: com.devwithzachary.completelinuxinstaller.theme.TerminalTheme = com.devwithzachary.completelinuxinstaller.theme.TerminalTheme.DRACULA
        private set

    private var currentFg: Color = theme.defaultFg
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
    private val ansiColors = Array(16) { i -> theme.ansiColors.getOrElse(i) { Color.White } }

    fun applyTheme(newTheme: com.devwithzachary.completelinuxinstaller.theme.TerminalTheme) {
        theme = newTheme
        for (i in 0 until 16) {
            if (i < newTheme.ansiColors.size) {
                ansiColors[i] = newTheme.ansiColors[i]
            }
        }
        resetSgr()
    }

    fun resize(newCols: Int, newRows: Int) {
        if (newCols <= 0 || newRows <= 0) return
        if (newCols == cols && newRows == rows) return

        val oldCols = cols
        val oldRows = rows
        val oldGrid = grid
        val wasInAlt = inAltBuffer

        fun isRowBlank(row: Array<TerminalChar>): Boolean {
            return row.all { it.ch == ' ' && it.bgColor == Color.Transparent }
        }

        fun resizeRow(row: Array<TerminalChar>): Array<TerminalChar> {
            return if (row.size == newCols) {
                row
            } else {
                Array(newCols) { c ->
                    if (c < row.size) row[c] else TerminalChar()
                }
            }
        }

        if (newCols != oldCols) {
            for (i in scrollback.indices) {
                scrollback[i] = resizeRow(scrollback[i])
            }
        }

        if (!wasInAlt) {
            var lastNonBlankRow = -1
            for (r in oldRows - 1 downTo 0) {
                if (r < oldGrid.size && !isRowBlank(oldGrid[r])) {
                    lastNonBlankRow = r
                    break
                }
            }
            val lastActiveRow = maxOf(cursorY, lastNonBlankRow).coerceIn(0, oldRows - 1)

            val newPrimaryGrid: Array<Array<TerminalChar>>

            if (newRows < oldRows) {
                if (lastActiveRow < newRows) {
                    newPrimaryGrid = Array(newRows) { r ->
                        if (r < oldRows) resizeRow(oldGrid[r]) else Array(newCols) { TerminalChar() }
                    }
                } else {
                    val linesToScroll = lastActiveRow - (newRows - 1)
                    for (r in 0 until linesToScroll) {
                        if (r < oldRows) {
                            scrollback.add(resizeRow(oldGrid[r]))
                            if (scrollback.size > maxScrollback) {
                                scrollback.removeAt(0)
                            }
                        }
                    }
                    newPrimaryGrid = Array(newRows) { r ->
                        val srcRow = r + linesToScroll
                        if (srcRow < oldRows) resizeRow(oldGrid[srcRow]) else Array(newCols) { TerminalChar() }
                    }
                    cursorY = (cursorY - linesToScroll).coerceIn(0, newRows - 1)
                    savedCursorY = (savedCursorY - linesToScroll).coerceIn(0, newRows - 1)
                }
            } else if (newRows > oldRows) {
                val extraRows = newRows - oldRows
                val linesToPull = minOf(extraRows, scrollback.size)
                newPrimaryGrid = Array(newRows) { r ->
                    when {
                        r < linesToPull -> {
                            val sbIndex = scrollback.size - linesToPull + r
                            resizeRow(scrollback[sbIndex])
                        }
                        r < linesToPull + oldRows -> {
                            val srcRow = r - linesToPull
                            if (srcRow < oldRows) resizeRow(oldGrid[srcRow]) else Array(newCols) { TerminalChar() }
                        }
                        else -> {
                            Array(newCols) { TerminalChar() }
                        }
                    }
                }
                repeat(linesToPull) {
                    if (scrollback.isNotEmpty()) {
                        scrollback.removeAt(scrollback.size - 1)
                    }
                }
                cursorY = (cursorY + linesToPull).coerceIn(0, newRows - 1)
                savedCursorY = (savedCursorY + linesToPull).coerceIn(0, newRows - 1)
            } else {
                newPrimaryGrid = Array(newRows) { r ->
                    if (r < oldRows) resizeRow(oldGrid[r]) else Array(newCols) { TerminalChar() }
                }
            }

            primaryGrid = newPrimaryGrid
            altGrid = Array(newRows) { Array(newCols) { TerminalChar() } }
            grid = primaryGrid
        } else {
            val shift = if (cursorY >= newRows) cursorY - (newRows - 1) else 0
            altGrid = Array(newRows) { r ->
                val srcRow = r + shift
                if (srcRow < oldRows) resizeRow(altGrid[srcRow]) else Array(newCols) { TerminalChar() }
            }
            primaryGrid = Array(newRows) { r ->
                if (r < primaryGrid.size) resizeRow(primaryGrid[r]) else Array(newCols) { TerminalChar() }
            }
            grid = altGrid
            cursorY = (cursorY - shift).coerceIn(0, newRows - 1)
            savedCursorY = (savedCursorY - shift).coerceIn(0, newRows - 1)
        }

        cols = newCols
        rows = newRows

        scrollTop = 0
        scrollBottom = rows - 1

        cursorX = cursorX.coerceIn(0, cols - 1)
        savedCursorX = savedCursorX.coerceIn(0, cols - 1)
        if (scrollOffset > 0) {
            scrollOffset = scrollOffset.coerceIn(0, scrollback.size)
        }
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
        currentFg = theme.defaultFg
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

    fun getVisibleText(): String {
        val sb = StringBuilder()
        for (r in 0 until rows) {
            val rowChars = grid[r]
            val rowStr = rowChars.map { it.ch }.joinToString("").trimEnd()
            sb.append(rowStr).append("\n")
        }
        return sb.toString().trimEnd()
    }

    fun getAllText(): String {
        val sb = StringBuilder()
        for (row in scrollback) {
            val rowStr = row.map { it.ch }.joinToString("").trimEnd()
            sb.append(rowStr).append("\n")
        }
        for (r in 0 until rows) {
            val rowChars = grid[r]
            val rowStr = rowChars.map { it.ch }.joinToString("").trimEnd()
            sb.append(rowStr).append("\n")
        }
        return sb.toString().trimEnd()
    }

    fun getWordAt(row: Int, col: Int): Pair<Int, Int> {
        if (rows == 0 || cols == 0) return Pair(0, 0)
        val r = row.coerceIn(0, rows - 1)
        val c = col.coerceIn(0, cols - 1)
        val rowChars = getRenderRow(r)
        val ch = rowChars.getOrNull(c)?.ch ?: ' '

        fun isWordChar(char: Char): Boolean =
            char.isLetterOrDigit() || char == '_' || char == '-' || char == '.' || char == '/' || char == '~' || char == ':' || char == '@' || char == '$'

        if (ch == ' ') {
            return Pair(c, c)
        }

        val isWord = isWordChar(ch)
        var startC = c
        while (startC > 0) {
            val prevChar = rowChars.getOrNull(startC - 1)?.ch ?: ' '
            if (isWord && isWordChar(prevChar)) {
                startC--
            } else if (!isWord && prevChar != ' ' && !isWordChar(prevChar)) {
                startC--
            } else {
                break
            }
        }

        var endC = c
        while (endC < cols - 1) {
            val nextChar = rowChars.getOrNull(endC + 1)?.ch ?: ' '
            if (isWord && isWordChar(nextChar)) {
                endC++
            } else if (!isWord && nextChar != ' ' && !isWordChar(nextChar)) {
                endC++
            } else {
                break
            }
        }

        return Pair(startC, endC)
    }

    fun getSelectedText(startRow: Int, startCol: Int, endRow: Int, endCol: Int): String {
        if (rows == 0 || cols == 0) return ""
        val sR = startRow.coerceIn(0, rows - 1)
        val sC = startCol.coerceIn(0, cols - 1)
        val eR = endRow.coerceIn(0, rows - 1)
        val eC = endCol.coerceIn(0, cols - 1)

        val startLinear = sR * cols + sC
        val endLinear = eR * cols + eC
        val (fromR, fromC) = if (startLinear <= endLinear) Pair(sR, sC) else Pair(eR, eC)
        val (toR, toC) = if (startLinear <= endLinear) Pair(eR, eC) else Pair(sR, sC)

        val sb = StringBuilder()
        for (r in fromR..toR) {
            val rowChars = getRenderRow(r)
            val c1 = if (r == fromR) fromC else 0
            val c2 = if (r == toR) toC else cols - 1

            val lineChars = CharArray(maxOf(0, c2 - c1 + 1))
            for (c in c1..c2) {
                lineChars[c - c1] = rowChars.getOrNull(c)?.ch ?: ' '
            }
            val lineStr = String(lineChars)
            if (r < toR) {
                sb.append(lineStr.trimEnd()).append("\n")
            } else {
                sb.append(lineStr)
            }
        }
        return sb.toString()
    }
}
