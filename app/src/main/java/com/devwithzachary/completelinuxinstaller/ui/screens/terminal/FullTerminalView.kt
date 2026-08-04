package com.devwithzachary.completelinuxinstaller.ui.screens.terminal

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devwithzachary.completelinuxinstaller.engine.TerminalBridge
import com.devwithzachary.completelinuxinstaller.engine.TerminalChar
import kotlin.math.max

@Composable
fun FullTerminalView(
    terminalBridge: TerminalBridge,
    refreshTrigger: Long,
    focusRequester: FocusRequester,
    onTapTerminal: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val fontSizePx = with(density) { 13.sp.toPx() }

    val paint = remember {
        Paint().apply {
            typeface = Typeface.MONOSPACE
            textSize = fontSizePx
            isAntiAlias = true
        }
    }

    val fontMetrics = paint.fontMetrics
    val charWidth = paint.measureText("W")
    val charHeight = fontMetrics.bottom - fontMetrics.top
    val baselineOffset = -fontMetrics.top

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E1E))
            .focusRequester(focusRequester)
            .focusable()
            .pointerInput(Unit) {
                detectTapGestures {
                    onTapTerminal()
                }
            }
    ) {
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }

        val cols = max(20, (widthPx / charWidth).toInt())
        val rows = max(5, (heightPx / charHeight).toInt())

        LaunchedEffect(cols, rows) {
            terminalBridge.updateTerminalSize(cols, rows)
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            // Explicitly read refreshTrigger inside draw scope to trigger immediate Canvas redraws on PTY output
            @Suppress("UNUSED_VARIABLE")
            val renderTick = refreshTrigger

            val emulator = terminalBridge.emulator
            val grid = emulator.grid
            val curX = emulator.cursorX
            val curY = emulator.cursorY
            val cursorVisible = emulator.cursorVisible

            val actualRows = grid.size
            val actualCols = if (actualRows > 0) grid[0].size else 0

            val canvas = drawContext.canvas.nativeCanvas

            // Always scroll viewport to keep active cursor & command output visible above keyboard
            val visibleRows = rows
            val startRow = (curY - visibleRows + 1).coerceAtLeast(0)
            val endRow = (startRow + visibleRows).coerceAtMost(actualRows)

            for (r in startRow until endRow) {
                val rowY = (r - startRow) * charHeight
                val rowChars = grid[r]
                for (c in 0 until actualCols) {
                    val cell: TerminalChar = rowChars[c]
                    val cellX = c * charWidth

                    // Draw Background
                    if (cell.bgColor != Color.Transparent) {
                        paint.color = cell.bgColor.toArgb()
                        canvas.drawRect(cellX, rowY, cellX + charWidth, rowY + charHeight, paint)
                    }

                    // Draw Cursor Block
                    if (cursorVisible && r == curY && c == curX) {
                        paint.color = Color(0xFF00FF00).toArgb()
                        canvas.drawRect(cellX, rowY, cellX + charWidth, rowY + charHeight, paint)
                        paint.color = Color.Black.toArgb()
                    } else {
                        paint.color = cell.fgColor.toArgb()
                    }

                    paint.isFakeBoldText = cell.bold
                    paint.isUnderlineText = cell.underline

                    if (cell.ch != ' ') {
                        canvas.drawText(
                            cell.ch.toString(),
                            cellX,
                            rowY + baselineOffset,
                            paint
                        )
                    }
                }
            }
        }
    }
}
