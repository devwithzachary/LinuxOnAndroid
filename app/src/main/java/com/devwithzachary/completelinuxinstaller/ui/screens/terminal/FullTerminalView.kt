package com.devwithzachary.completelinuxinstaller.ui.screens.terminal

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.*
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
    val keyboardController = LocalSoftwareKeyboardController.current

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

    // Baseline text state used to capture character additions, backspaces, and IME events
    var textFieldValue by remember {
        mutableStateOf(TextFieldValue("  ", TextRange(2)))
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E1E))
            .pointerInput(Unit) {
                detectTapGestures {
                    focusRequester.requestFocus()
                    keyboardController?.show()
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

        // Invisible BasicTextField to capture all software and physical keyboard inputs directly into PTY
        BasicTextField(
            value = textFieldValue,
            onValueChange = { newValue ->
                val oldText = textFieldValue.text
                val newText = newValue.text

                if (newText.length > oldText.length) {
                    val added = newText.substring(oldText.length)
                    if (added.contains("\n")) {
                        terminalBridge.sendInput("\n")
                    } else {
                        terminalBridge.sendInput(added)
                    }
                } else if (newText.length < oldText.length) {
                    // Backspace pressed
                    terminalBridge.sendInput("\u007F")
                }

                // Reset to baseline "  "
                textFieldValue = TextFieldValue("  ", TextRange(2))
            },
            modifier = Modifier
                .size(1.dp)
                .alpha(0.01f)
                .focusRequester(focusRequester)
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown) {
                        when (event.key) {
                            Key.Enter -> {
                                terminalBridge.sendInput("\n")
                                textFieldValue = TextFieldValue("  ", TextRange(2))
                                true
                            }
                            Key.Backspace -> {
                                terminalBridge.sendInput("\u007F")
                                textFieldValue = TextFieldValue("  ", TextRange(2))
                                true
                            }
                            Key.Tab -> {
                                terminalBridge.sendTab()
                                true
                            }
                            Key.Escape -> {
                                terminalBridge.sendEsc()
                                true
                            }
                            Key.DirectionUp -> {
                                terminalBridge.sendArrowUp()
                                true
                            }
                            Key.DirectionDown -> {
                                terminalBridge.sendArrowDown()
                                true
                            }
                            Key.DirectionLeft -> {
                                terminalBridge.sendArrowLeft()
                                true
                            }
                            Key.DirectionRight -> {
                                terminalBridge.sendArrowRight()
                                true
                            }
                            else -> false
                        }
                    } else {
                        false
                    }
                },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Ascii,
                imeAction = ImeAction.Send
            ),
            keyboardActions = KeyboardActions(
                onSend = {
                    terminalBridge.sendInput("\n")
                    textFieldValue = TextFieldValue("  ", TextRange(2))
                },
                onDone = {
                    terminalBridge.sendInput("\n")
                    textFieldValue = TextFieldValue("  ", TextRange(2))
                },
                onGo = {
                    terminalBridge.sendInput("\n")
                    textFieldValue = TextFieldValue("  ", TextRange(2))
                }
            )
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
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

            val visibleRows = rows
            val startRow = (curY - visibleRows + 1).coerceAtLeast(0)
            val endRow = (startRow + visibleRows).coerceAtMost(actualRows)

            for (r in startRow until endRow) {
                val rowY = (r - startRow) * charHeight
                val rowChars = grid[r]
                for (c in 0 until actualCols) {
                    val cell: TerminalChar = rowChars[c]
                    val cellX = c * charWidth

                    if (cell.bgColor != Color.Transparent) {
                        paint.color = cell.bgColor.toArgb()
                        canvas.drawRect(cellX, rowY, cellX + charWidth, rowY + charHeight, paint)
                    }

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
