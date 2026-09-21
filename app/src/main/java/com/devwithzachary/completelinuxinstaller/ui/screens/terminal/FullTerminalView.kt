package com.devwithzachary.completelinuxinstaller.ui.screens.terminal

import android.content.Intent
import android.graphics.Paint
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devwithzachary.completelinuxinstaller.engine.TerminalBridge
import com.devwithzachary.completelinuxinstaller.engine.TerminalChar
import kotlin.math.max
import kotlin.math.min

@Composable
fun FullTerminalView(
    terminalBridge: TerminalBridge,
    activeSessionId: String? = null,
    refreshTrigger: Long,
    focusRequester: FocusRequester,
    onTapTerminal: () -> Unit,
    modifier: Modifier = Modifier,
    isCtrlActive: Boolean = false,
    isAltActive: Boolean = false,
    onConsumeModifiers: () -> Unit = {},
    fontSizeSp: Int = 13,
    fontFamilyName: String = TerminalFonts.DEFAULT_FONT
) {
    val density = LocalDensity.current
    val context = LocalContext.current
    val fontSizePx = with(density) { fontSizeSp.sp.toPx() }
    val keyboardController = LocalSoftwareKeyboardController.current
    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current
    val hapticFeedback = LocalHapticFeedback.current

    var selectionStart by remember { mutableStateOf<Pair<Int, Int>?>(null) } // (bufferRow, col)
    var selectionEnd by remember { mutableStateOf<Pair<Int, Int>?>(null) }   // (bufferRow, col)
    var isDraggingStartHandle by remember { mutableStateOf(false) }
    var isDraggingEndHandle by remember { mutableStateOf(false) }
    var activeDragPixelY by remember { mutableFloatStateOf(-1f) }
    var activeDragPixelX by remember { mutableFloatStateOf(-1f) }
    var accumulatedScrollY by remember { mutableFloatStateOf(0f) }
    var showContextMenu by remember { mutableStateOf(false) }
    var contextMenuOffset by remember { mutableStateOf(Offset.Zero) }

    val selectedTypeface = remember(fontFamilyName, context) {
        TerminalFonts.getTypeface(context, fontFamilyName, bold = false)
    }
    val boldTypeface = remember(fontFamilyName, context) {
        TerminalFonts.getTypeface(context, fontFamilyName, bold = true)
    }

    val paint = remember(fontSizePx, selectedTypeface) {
        Paint().apply {
            typeface = selectedTypeface
            textSize = fontSizePx
            isAntiAlias = true
        }
    }

    val fontMetrics = paint.fontMetrics
    val isMono = remember(fontFamilyName) { TerminalFonts.isMonospace(fontFamilyName) }
    val charWidth = remember(paint, isMono) {
        if (isMono) {
            paint.measureText("W")
        } else {
            (paint.measureText("a") + paint.measureText("n")) / 2f
        }
    }
    val asciiCharWidths = remember(paint, boldTypeface, isMono) {
        if (isMono) {
            null
        } else {
            // Precalculate ASCII 32..126 widths: index 0..127 normal, index 128..255 bold
            val widths = FloatArray(256)
            for (code in 32..126) {
                widths[code] = paint.measureText(code.toChar().toString())
            }
            val boldPaint = Paint(paint).apply { typeface = boldTypeface }
            for (code in 32..126) {
                widths[code + 128] = boldPaint.measureText(code.toChar().toString())
            }
            widths
        }
    }
    val charHeight = fontMetrics.bottom - fontMetrics.top
    val baselineOffset = -fontMetrics.top

    // Baseline text state used to capture character additions, backspaces, and IME events
    var textFieldValue by remember {
        mutableStateOf(TextFieldValue("", TextRange.Zero))
    }
    var lastText by remember { mutableStateOf("") }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E1E))
    ) {
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }

        val cols = max(20, (widthPx / charWidth).toInt())
        val rows = max(5, (heightPx / charHeight).toInt())

        LaunchedEffect(activeSessionId) {
            selectionStart = null
            selectionEnd = null
            textFieldValue = TextFieldValue("", TextRange.Zero)
            lastText = ""
        }

        LaunchedEffect(activeSessionId, cols, rows) {
            terminalBridge.updateTerminalSize(cols, rows)
        }

        val selStart = selectionStart
        val selEnd = selectionEnd
        val hasSelection = selStart != null && selEnd != null

        // Normalized linear selection bounds in buffer coordinates: (fromBufferR, fromC) <= (toBufferR, toC)
        val isStartFirst = remember(selStart, selEnd, cols) {
            if (selStart != null && selEnd != null) {
                (selStart.first.toLong() * cols + selStart.second) <= (selEnd.first.toLong() * cols + selEnd.second)
            } else {
                true
            }
        }

        val (fromBufferR, fromC, toBufferR, toC) = remember(selStart, selEnd, cols, isStartFirst) {
            if (selStart != null && selEnd != null) {
                if (isStartFirst) {
                    listOf(selStart.first, selStart.second, selEnd.first, selEnd.second)
                } else {
                    listOf(selEnd.first, selEnd.second, selStart.first, selStart.second)
                }
            } else {
                listOf(0, 0, 0, 0)
            }
        }

        // Auto-scroll ticker when dragging a selection handle near the top or bottom edge of the terminal
        val isDraggingAnyHandle = isDraggingStartHandle || isDraggingEndHandle
        LaunchedEffect(isDraggingAnyHandle) {
            if (!isDraggingAnyHandle) return@LaunchedEffect
            while (isActive) {
                val y = activeDragPixelY
                val x = activeDragPixelX
                if (y >= 0f) {
                    val topThreshold = charHeight * 1.5f
                    val bottomThreshold = heightPx - charHeight * 1.5f

                    if (y < topThreshold && terminalBridge.emulator.scrollback.isNotEmpty()) {
                        terminalBridge.scrollUp(1)
                        val screenR = (y / charHeight).toInt().coerceIn(0, rows - 1)
                        val newC = (x / charWidth).toInt().coerceIn(0, cols - 1)
                        val newBufferR = terminalBridge.emulator.screenToBufferRow(screenR)
                        if (isDraggingStartHandle) {
                            if (isStartFirst) selectionStart = Pair(newBufferR, newC) else selectionEnd = Pair(newBufferR, newC)
                        } else if (isDraggingEndHandle) {
                            if (isStartFirst) selectionEnd = Pair(newBufferR, newC) else selectionStart = Pair(newBufferR, newC)
                        }
                    } else if (y > bottomThreshold && terminalBridge.emulator.scrollOffset > 0) {
                        terminalBridge.scrollDown(1)
                        val screenR = (y / charHeight).toInt().coerceIn(0, rows - 1)
                        val newC = (x / charWidth).toInt().coerceIn(0, cols - 1)
                        val newBufferR = terminalBridge.emulator.screenToBufferRow(screenR)
                        if (isDraggingStartHandle) {
                            if (isStartFirst) selectionStart = Pair(newBufferR, newC) else selectionEnd = Pair(newBufferR, newC)
                        } else if (isDraggingEndHandle) {
                            if (isStartFirst) selectionEnd = Pair(newBufferR, newC) else selectionStart = Pair(newBufferR, newC)
                        }
                    }
                }
                delay(100L)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerHoverIcon(PointerIcon.Text)
                .pointerInput(cols, rows) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Main)
                            if (event.type == PointerEventType.Scroll) {
                                val delta = event.changes.firstOrNull()?.scrollDelta?.y ?: 0f
                                if (delta > 0) {
                                    terminalBridge.scrollDown(max(1, (delta * 2).toInt()))
                                } else if (delta < 0) {
                                    terminalBridge.scrollUp(max(1, (-delta * 2).toInt()))
                                }
                            } else if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                                val change = event.changes.firstOrNull()
                                val position = change?.position ?: Offset.Zero
                                change?.consume()
                                contextMenuOffset = position
                                showContextMenu = true
                            }
                        }
                    }
                }
                .pointerInput(cols, rows) {
                    // Direct smooth scrolling with vertical dragging
                    detectDragGestures(
                        onDragStart = {
                            accumulatedScrollY = 0f
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            accumulatedScrollY += dragAmount.y
                            val threshold = charHeight * 0.75f
                            if (accumulatedScrollY > threshold) {
                                val lines = (accumulatedScrollY / charHeight).toInt().coerceAtLeast(1)
                                terminalBridge.scrollUp(lines)
                                accumulatedScrollY %= charHeight
                            } else if (accumulatedScrollY < -threshold) {
                                val lines = (-accumulatedScrollY / charHeight).toInt().coerceAtLeast(1)
                                terminalBridge.scrollDown(lines)
                                accumulatedScrollY %= charHeight
                            }
                        }
                    )
                }
                .pointerInput(cols, rows) {
                    detectTapGestures(
                        onTap = {
                            if (selectionStart != null || selectionEnd != null) {
                                selectionStart = null
                                selectionEnd = null
                            }
                            focusRequester.requestFocus()
                            keyboardController?.show()
                            onTapTerminal()
                        },
                        onLongPress = { offset ->
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            val c = (offset.x / charWidth).toInt().coerceIn(0, cols - 1)
                            val r = (offset.y / charHeight).toInt().coerceIn(0, rows - 1)
                            val bufferR = terminalBridge.emulator.screenToBufferRow(r)
                            val wordRange = terminalBridge.getWordAtBuffer(bufferR, c)
                            selectionStart = Pair(bufferR, wordRange.first)
                            selectionEnd = Pair(bufferR, wordRange.second)
                        }
                    )
                }
        ) {
            // Invisible BasicTextField to capture all soft/physical keyboard inputs directly into PTY
            BasicTextField(
                value = textFieldValue,
                onValueChange = { newValue ->
                    val oldText = lastText
                    val newText = newValue.text

                    if (isCtrlActive || isAltActive) {
                        val addedText = if (newText.length > oldText.length) {
                            newText.substring(oldText.length)
                        } else if (newText.isNotEmpty()) {
                            newText
                        } else {
                            ""
                        }
                        if (addedText.isNotEmpty()) {
                            for (ch in addedText) {
                                terminalBridge.sendModifiedChar(ch, isCtrlActive, isAltActive)
                            }
                            onConsumeModifiers()
                        }
                        textFieldValue = TextFieldValue("", TextRange.Zero)
                        lastText = ""
                        return@BasicTextField
                    }

                    if (newText.contains("\n") || newText.contains("\r")) {
                        terminalBridge.sendInput("\r")
                        textFieldValue = TextFieldValue("", TextRange.Zero)
                        lastText = ""
                    } else if (newText != oldText) {
                        if (newText.length < oldText.length) {
                            val deleteCount = oldText.length - newText.length
                            repeat(deleteCount) {
                                terminalBridge.sendInput("\u007F")
                            }
                            textFieldValue = newValue
                            lastText = newText
                        } else if (newText.startsWith(oldText)) {
                            val addedText = newText.substring(oldText.length)
                            if (addedText.isNotEmpty()) {
                                terminalBridge.sendInput(addedText)
                            }
                            textFieldValue = newValue
                            lastText = newText
                        } else {
                            var prefixLen = 0
                            while (prefixLen < oldText.length && prefixLen < newText.length && oldText[prefixLen] == newText[prefixLen]) {
                                prefixLen++
                            }
                            val deleteCount = oldText.length - prefixLen
                            repeat(deleteCount) {
                                terminalBridge.sendInput("\u007F")
                            }
                            val addedText = newText.substring(prefixLen)
                            if (addedText.isNotEmpty()) {
                                terminalBridge.sendInput(addedText)
                            }
                            textFieldValue = newValue
                            lastText = newText
                        }
                    }
                },
                modifier = Modifier
                    .size(1.dp)
                    .alpha(0.01f)
                    .focusRequester(focusRequester)
                    .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown) {
                        val isCtrlOrMeta = event.isCtrlPressed || event.isMetaPressed
                        if (isCtrlActive || isAltActive) {
                            val codePoint = event.utf16CodePoint
                            if (codePoint > 0 && !Character.isISOControl(codePoint)) {
                                terminalBridge.sendModifiedChar(codePoint.toChar(), isCtrlActive, isAltActive)
                                onConsumeModifiers()
                                return@onPreviewKeyEvent true
                            }
                        }
                        when {
                            isCtrlOrMeta && (event.key == Key.V || (event.isShiftPressed && event.key == Key.V)) -> {
                                val clipText = clipboardManager.getText()?.text
                                if (!clipText.isNullOrEmpty()) {
                                    terminalBridge.pasteText(clipText)
                                }
                                true
                            }

                            isCtrlOrMeta && event.isShiftPressed && event.key == Key.C -> {
                                val text = if (hasSelection) {
                                    terminalBridge.getSelectedText(fromBufferR, fromC, toBufferR, toC)
                                } else {
                                    terminalBridge.getScreenText()
                                }
                                if (text.isNotEmpty()) {
                                    clipboardManager.setText(AnnotatedString(text))
                                }
                                true
                            }

                            event.key == Key.PageUp -> {
                                terminalBridge.scrollUp(rows / 2)
                                true
                            }

                            event.key == Key.PageDown -> {
                                terminalBridge.scrollDown(rows / 2)
                                true
                            }

                            event.key == Key.Enter -> {
                                terminalBridge.sendInput("\r")
                                textFieldValue = TextFieldValue("", TextRange.Zero)
                                lastText = ""
                                true
                            }

                            event.key == Key.Backspace -> {
                                terminalBridge.sendInput("\u007F")
                                if (lastText.isNotEmpty()) {
                                    val updatedText = lastText.dropLast(1)
                                    lastText = updatedText
                                    textFieldValue = TextFieldValue(updatedText, TextRange(updatedText.length))
                                }
                                true
                            }

                            event.key == Key.Tab -> {
                                terminalBridge.sendTab()
                                true
                            }

                            event.key == Key.Escape -> {
                                terminalBridge.sendEsc()
                                true
                            }

                            event.key == Key.DirectionUp -> {
                                terminalBridge.sendArrowUp()
                                true
                            }

                            event.key == Key.DirectionDown -> {
                                terminalBridge.sendArrowDown()
                                true
                            }

                            event.key == Key.DirectionLeft -> {
                                terminalBridge.sendArrowLeft()
                                true
                            }

                            event.key == Key.DirectionRight -> {
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
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Send
                ),
                keyboardActions = KeyboardActions(
                    onSend = {
                        terminalBridge.sendInput("\r")
                        textFieldValue = TextFieldValue("", TextRange.Zero)
                        lastText = ""
                    },
                    onDone = {
                        terminalBridge.sendInput("\r")
                        textFieldValue = TextFieldValue("", TextRange.Zero)
                        lastText = ""
                    },
                    onGo = {
                        terminalBridge.sendInput("\r")
                        textFieldValue = TextFieldValue("", TextRange.Zero)
                        lastText = ""
                    }
                )
            )

            // Terminal Screen & Text Selection Canvas
            Canvas(modifier = Modifier.fillMaxSize()) {
                @Suppress("UNUSED_VARIABLE")
                val sessionKey = activeSessionId
                @Suppress("UNUSED_VARIABLE")
                val renderTick = refreshTrigger

                val emulator = terminalBridge.emulator
                val theme = emulator.theme
                drawRect(color = theme.defaultBg)

                val curX = emulator.cursorX
                val curY = emulator.cursorY
                val cursorVisible = emulator.cursorVisible
                val isScrolledBack = emulator.scrollOffset > 0

                val renderRows = min(rows, emulator.rows)
                val nativeCanvas = drawContext.canvas.nativeCanvas

                for (r in 0 until renderRows) {
                    val rowY = r * charHeight
                    val rowChars = emulator.getRenderRow(r)
                    val actualCols = rowChars.size

                    for (c in 0 until actualCols) {
                        val cell: TerminalChar = rowChars[c]
                        val cellX = c * charWidth

                        val bRow = emulator.screenToBufferRow(r)

                        // Linear multi-line selection check across absolute buffer rows
                        val isSelected = hasSelection && when {
                            bRow < fromBufferR || bRow > toBufferR -> false
                            fromBufferR == toBufferR -> c in fromC..toC
                            bRow == fromBufferR -> c >= fromC
                            bRow == toBufferR -> c <= toC
                            else -> true
                        }

                        if (isSelected) {
                            paint.color = theme.selectionColor.toArgb()
                            nativeCanvas.drawRect(cellX, rowY, cellX + charWidth, rowY + charHeight, paint)
                        } else if (cell.bgColor != Color.Transparent) {
                            paint.color = cell.bgColor.toArgb()
                            nativeCanvas.drawRect(cellX, rowY, cellX + charWidth, rowY + charHeight, paint)
                        }

                        if (!isScrolledBack && cursorVisible && r == curY && c == curX) {
                            paint.color = theme.cursorColor.toArgb()
                            nativeCanvas.drawRect(cellX, rowY, cellX + charWidth, rowY + charHeight, paint)
                            paint.color = theme.defaultBg.toArgb()
                        } else if (isSelected) {
                            paint.color = theme.defaultFg.toArgb()
                        } else {
                            paint.color = cell.fgColor.toArgb()
                        }

                        paint.typeface = if (cell.bold) boldTypeface else selectedTypeface
                        paint.isFakeBoldText = false
                        paint.isUnderlineText = cell.underline

                        if (cell.ch != ' ') {
                            val charStr =
                                if (fontFamilyName == "CyberGlyphs") com.devwithzachary.completelinuxinstaller.theme.CyberGlyphs.transformChar(cell.ch) else cell.ch.toString()
                            if (!isMono) {
                                val chCode = cell.ch.code
                                val charW = if (asciiCharWidths != null && chCode in 32..126) {
                                    if (cell.bold) asciiCharWidths[chCode + 128] else asciiCharWidths[chCode]
                                } else {
                                    paint.measureText(charStr)
                                }
                                if (charW > charWidth) {
                                    paint.textScaleX = charWidth / charW
                                    nativeCanvas.drawText(
                                        charStr,
                                        cellX,
                                        rowY + baselineOffset,
                                        paint
                                    )
                                    paint.textScaleX = 1f
                                } else {
                                    nativeCanvas.drawText(
                                        charStr,
                                        cellX,
                                        rowY + baselineOffset,
                                        paint
                                    )
                                }
                            } else {
                                nativeCanvas.drawText(
                                    charStr,
                                    cellX,
                                    rowY + baselineOffset,
                                    paint
                                )
                            }
                        }
                    }
                }
            }

            // Draggable Text Selection Handles
            if (hasSelection) {
                var dragStartAnchor by remember { mutableStateOf(Offset.Zero) }
                var dragEndAnchor by remember { mutableStateOf(Offset.Zero) }

                val startScreenR = terminalBridge.emulator.bufferToScreenRow(fromBufferR)
                val endScreenR = terminalBridge.emulator.bufferToScreenRow(toBufferR)

                // Start Selection Handle (top-left of selection)
                if (startScreenR in 0 until rows) {
                    val startHandlePos = Offset(fromC * charWidth, (startScreenR + 1) * charHeight)
                    TerminalSelectionHandle(
                        position = startHandlePos,
                        isStart = true,
                        onDragStart = {
                            dragStartAnchor = Offset(fromC * charWidth + charWidth * 0.5f, startScreenR * charHeight + charHeight * 0.5f)
                            activeDragPixelX = dragStartAnchor.x
                            activeDragPixelY = dragStartAnchor.y
                            isDraggingStartHandle = true
                        },
                        onDrag = { dragDelta ->
                            val curPixelX = dragStartAnchor.x + dragDelta.x
                            val curPixelY = dragStartAnchor.y + dragDelta.y
                            activeDragPixelX = curPixelX
                            activeDragPixelY = curPixelY
                            if (curPixelY < charHeight * 1.5f && terminalBridge.emulator.scrollback.isNotEmpty()) {
                                terminalBridge.scrollUp(1)
                            } else if (curPixelY > heightPx - charHeight * 1.5f && terminalBridge.emulator.scrollOffset > 0) {
                                terminalBridge.scrollDown(1)
                            }
                            val screenR = (curPixelY / charHeight).toInt().coerceIn(0, rows - 1)
                            val newC = (curPixelX / charWidth).toInt().coerceIn(0, cols - 1)
                            val newBufferR = terminalBridge.emulator.screenToBufferRow(screenR)
                            val currentTarget = if (isStartFirst) selectionStart else selectionEnd
                            if (currentTarget == null || currentTarget.first != newBufferR || currentTarget.second != newC) {
                                try { hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove) } catch (_: Exception) {}
                                if (isStartFirst) {
                                    selectionStart = Pair(newBufferR, newC)
                                } else {
                                    selectionEnd = Pair(newBufferR, newC)
                                }
                            }
                        },
                        onDragEnd = {
                            isDraggingStartHandle = false
                            activeDragPixelY = -1f
                        }
                    )
                }

                // End Selection Handle (bottom-right of selection)
                if (endScreenR in 0 until rows) {
                    val endHandlePos = Offset((toC + 1) * charWidth, (endScreenR + 1) * charHeight)
                    TerminalSelectionHandle(
                        position = endHandlePos,
                        isStart = false,
                        onDragStart = {
                            dragEndAnchor = Offset(toC * charWidth + charWidth * 0.5f, endScreenR * charHeight + charHeight * 0.5f)
                            activeDragPixelX = dragEndAnchor.x
                            activeDragPixelY = dragEndAnchor.y
                            isDraggingEndHandle = true
                        },
                        onDrag = { dragDelta ->
                            val curPixelX = dragEndAnchor.x + dragDelta.x
                            val curPixelY = dragEndAnchor.y + dragDelta.y
                            activeDragPixelX = curPixelX
                            activeDragPixelY = curPixelY
                            if (curPixelY < charHeight * 1.5f && terminalBridge.emulator.scrollback.isNotEmpty()) {
                                terminalBridge.scrollUp(1)
                            } else if (curPixelY > heightPx - charHeight * 1.5f && terminalBridge.emulator.scrollOffset > 0) {
                                terminalBridge.scrollDown(1)
                            }
                            val screenR = (curPixelY / charHeight).toInt().coerceIn(0, rows - 1)
                            val newC = (curPixelX / charWidth).toInt().coerceIn(0, cols - 1)
                            val newBufferR = terminalBridge.emulator.screenToBufferRow(screenR)
                            val currentTarget = if (isStartFirst) selectionEnd else selectionStart
                            if (currentTarget == null || currentTarget.first != newBufferR || currentTarget.second != newC) {
                                try { hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove) } catch (_: Exception) {}
                                if (isStartFirst) {
                                    selectionEnd = Pair(newBufferR, newC)
                                } else {
                                    selectionStart = Pair(newBufferR, newC)
                                }
                            }
                        },
                        onDragEnd = {
                            isDraggingEndHandle = false
                            activeDragPixelY = -1f
                        }
                    )
                }
            }

            // Floating Selection Toolbar
            AnimatedVisibility(
                visible = hasSelection,
                enter = fadeIn() + slideInVertically { -it / 2 },
                exit = fadeOut() + slideOutVertically { -it / 2 },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFF2D2D2D),
                    shadowElevation = 8.dp,
                    tonalElevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Copy Selected Text
                        FilledTonalButton(
                            onClick = {
                                val s = selectionStart
                                val e = selectionEnd
                                if (s != null && e != null) {
                                    val text = terminalBridge.getSelectedText(s.first, s.second, e.first, e.second)
                                    if (text.isNotEmpty()) {
                                        clipboardManager.setText(AnnotatedString(text))
                                    }
                                }
                                selectionStart = null
                                selectionEnd = null
                                focusRequester.requestFocus()
                            },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = "Copy Selection",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        // Select All Terminal Buffer Text
                        TextButton(
                            onClick = {
                                val total = terminalBridge.emulator.totalBufferRows
                                selectionStart = Pair(0, 0)
                                selectionEnd = Pair((total - 1).coerceAtLeast(0), cols - 1)
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(
                                Icons.Default.SelectAll,
                                contentDescription = "Select All",
                                modifier = Modifier.size(16.dp),
                                tint = Color(0xFF90CAF9)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Select All", fontSize = 12.sp, color = Color(0xFF90CAF9))
                        }

                        // Share Selected Text
                        IconButton(
                            onClick = {
                                val s = selectionStart
                                val e = selectionEnd
                                if (s != null && e != null) {
                                    val text = terminalBridge.getSelectedText(s.first, s.second, e.first, e.second)
                                    if (text.isNotEmpty()) {
                                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                            putExtra(Intent.EXTRA_TEXT, text)
                                            type = "text/plain"
                                        }
                                        val shareIntent = Intent.createChooser(sendIntent, "Share Terminal Text")
                                        context.startActivity(shareIntent)
                                    }
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = "Share Selection",
                                tint = Color.LightGray,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Dismiss / Clear Selection
                        IconButton(
                            onClick = {
                                selectionStart = null
                                selectionEnd = null
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Clear Selection",
                                tint = Color.LightGray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Floating "Scroll to Bottom" Button when scrolled up into history
            if (terminalBridge.emulator.scrollOffset > 0) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 16.dp, end = 16.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF007ACC),
                    shadowElevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier
                            .clickable { terminalBridge.scrollToBottom() }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = "Scroll to Bottom",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Scroll to Bottom (${terminalBridge.emulator.scrollOffset})",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Mouse Right-Click Context Menu
            Box(
                modifier = Modifier
                    .offset(
                        x = with(density) { contextMenuOffset.x.toDp() },
                        y = with(density) { contextMenuOffset.y.toDp() }
                    )
                    .size(1.dp)
            ) {
                DropdownMenu(
                    expanded = showContextMenu,
                    onDismissRequest = { showContextMenu = false }
                ) {
                    val hasTextSelected = hasSelection
                    // Copy
                    DropdownMenuItem(
                        text = { Text("Copy") },
                        leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        onClick = {
                            showContextMenu = false
                            if (hasTextSelected) {
                                val text = terminalBridge.getSelectedText(fromBufferR, fromC, toBufferR, toC)
                                if (text.isNotEmpty()) {
                                    clipboardManager.setText(AnnotatedString(text))
                                }
                                selectionStart = null
                                selectionEnd = null
                            } else {
                                val text = terminalBridge.getScreenText()
                                if (text.isNotEmpty()) {
                                    clipboardManager.setText(AnnotatedString(text))
                                }
                            }
                            focusRequester.requestFocus()
                        }
                    )
                    // Paste
                    val clipText = clipboardManager.getText()?.text
                    DropdownMenuItem(
                        text = { Text("Paste") },
                        enabled = !clipText.isNullOrEmpty(),
                        leadingIcon = { Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        onClick = {
                            showContextMenu = false
                            if (!clipText.isNullOrEmpty()) {
                                terminalBridge.pasteText(clipText)
                            }
                            focusRequester.requestFocus()
                        }
                    )
                    HorizontalDivider()
                    // Select All
                    DropdownMenuItem(
                        text = { Text("Select All") },
                        leadingIcon = { Icon(Icons.Default.SelectAll, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        onClick = {
                            showContextMenu = false
                            val total = terminalBridge.emulator.totalBufferRows
                            selectionStart = Pair(0, 0)
                            selectionEnd = Pair((total - 1).coerceAtLeast(0), cols - 1)
                        }
                    )
                    // Clear Screen / Buffer
                    DropdownMenuItem(
                        text = { Text("Clear Buffer") },
                        leadingIcon = { Icon(Icons.Default.ClearAll, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        onClick = {
                            showContextMenu = false
                            terminalBridge.clearTerminal()
                            terminalBridge.sendInput("\u000c")
                            focusRequester.requestFocus()
                        }
                    )
                    // Reset Terminal
                    DropdownMenuItem(
                        text = { Text("Reset Terminal") },
                        leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        onClick = {
                            showContextMenu = false
                            terminalBridge.sendInput("\u001Bc")
                            focusRequester.requestFocus()
                        }
                    )
                }
            }
        }
    }
}

/**
 * Draggable touch handle placed at the beginning or end of selected terminal text.
 */
@Composable
private fun TerminalSelectionHandle(
    position: Offset,
    isStart: Boolean,
    onDragStart: () -> Unit,
    onDrag: (dragDelta: Offset) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val handleTouchSize = 44.dp
    val handleTouchSizePx = with(LocalDensity.current) { handleTouchSize.toPx() }
    val primaryColor = MaterialTheme.colorScheme.primary

    val currentOnDragStart by rememberUpdatedState(onDragStart)
    val currentOnDrag by rememberUpdatedState(onDrag)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)

    var dragAccumulated by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .offset(
                x = with(LocalDensity.current) { (position.x - (if (isStart) handleTouchSizePx * 0.75f else handleTouchSizePx * 0.25f)).toDp() },
                y = with(LocalDensity.current) { position.y.toDp() }
            )
            .size(handleTouchSize)
            .pointerInput(isStart) {
                detectDragGestures(
                    onDragStart = {
                        dragAccumulated = Offset.Zero
                        currentOnDragStart()
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragAccumulated += dragAmount
                        currentOnDrag(dragAccumulated)
                    },
                    onDragEnd = {
                        currentOnDragEnd()
                    },
                    onDragCancel = {
                        currentOnDragEnd()
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val radius = w * 0.32f
            val anchorX = if (isStart) w * 0.75f else w * 0.25f
            val anchorY = 0f
            val circleCenterX = w * 0.5f
            val circleCenterY = h * 0.55f

            val path = Path().apply {
                if (isStart) {
                    moveTo(anchorX, anchorY)
                    lineTo(anchorX, circleCenterY)
                    arcTo(
                        rect = Rect(circleCenterX - radius, circleCenterY - radius, circleCenterX + radius, circleCenterY + radius),
                        startAngleDegrees = 0f,
                        sweepAngleDegrees = 270f,
                        forceMoveTo = false
                    )
                    close()
                } else {
                    moveTo(anchorX, anchorY)
                    lineTo(anchorX, circleCenterY)
                    arcTo(
                        rect = Rect(circleCenterX - radius, circleCenterY - radius, circleCenterX + radius, circleCenterY + radius),
                        startAngleDegrees = 180f,
                        sweepAngleDegrees = -270f,
                        forceMoveTo = false
                    )
                    close()
                }
            }
            drawPath(path, color = primaryColor)
        }
    }
}
