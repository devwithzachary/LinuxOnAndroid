package com.devwithzachary.completelinuxinstaller.ui.screens.terminal

import android.content.Intent
import android.graphics.Paint
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
    val clipboardManager = LocalClipboardManager.current
    val hapticFeedback = LocalHapticFeedback.current

    var selectionStart by remember { mutableStateOf<Pair<Int, Int>?>(null) } // (row, col)
    var selectionEnd by remember { mutableStateOf<Pair<Int, Int>?>(null) }   // (row, col)
    var accumulatedScrollY by remember { mutableFloatStateOf(0f) }

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
    val charWidth = paint.measureText("W")
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

        LaunchedEffect(cols, rows) {
            terminalBridge.updateTerminalSize(cols, rows)
        }

        val selStart = selectionStart
        val selEnd = selectionEnd
        val hasSelection = selStart != null && selEnd != null

        // Normalized linear selection bounds: (fromR, fromC) <= (toR, toC)
        val (fromR, fromC, toR, toC) = remember(selStart, selEnd, cols) {
            if (selStart != null && selEnd != null) {
                val startLinear = selStart.first * cols + selStart.second
                val endLinear = selEnd.first * cols + selEnd.second
                if (startLinear <= endLinear) {
                    listOf(selStart.first, selStart.second, selEnd.first, selEnd.second)
                } else {
                    listOf(selEnd.first, selEnd.second, selStart.first, selStart.second)
                }
            } else {
                listOf(0, 0, 0, 0)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
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
                            val wordRange = terminalBridge.getWordAt(r, c)
                            selectionStart = Pair(r, wordRange.first)
                            selectionEnd = Pair(r, wordRange.second)
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
                            isCtrlOrMeta && event.key == Key.V -> {
                                val clipText = clipboardManager.getText()?.text
                                if (!clipText.isNullOrEmpty()) {
                                    terminalBridge.pasteText(clipText)
                                }
                                true
                            }

                            isCtrlOrMeta && event.isShiftPressed && event.key == Key.C -> {
                                val text = terminalBridge.getScreenText()
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

                        // Linear multi-line selection check
                        val isSelected = hasSelection && when {
                            r < fromR || r > toR -> false
                            fromR == toR -> c in fromC..toC
                            r == fromR -> c >= fromC
                            r == toR -> c <= toC
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

            // Draggable Text Selection Handles
            if (hasSelection) {
                var dragStartAnchor by remember { mutableStateOf(Offset.Zero) }
                var dragEndAnchor by remember { mutableStateOf(Offset.Zero) }

                // Start Selection Handle (top-left of selection)
                val startHandlePos = Offset(fromC * charWidth, (fromR + 1) * charHeight)
                TerminalSelectionHandle(
                    position = startHandlePos,
                    isStart = true,
                    onDragStart = {
                        dragStartAnchor = Offset(fromC * charWidth + charWidth * 0.5f, fromR * charHeight + charHeight * 0.5f)
                    },
                    onDrag = { dragDelta ->
                        val curPixelX = dragStartAnchor.x + dragDelta.x
                        val curPixelY = dragStartAnchor.y + dragDelta.y
                        val newR = (curPixelY / charHeight).toInt().coerceIn(0, rows - 1)
                        val newC = (curPixelX / charWidth).toInt().coerceIn(0, cols - 1)
                        if (selectionStart?.first != newR || selectionStart?.second != newC) {
                            try { hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove) } catch (_: Exception) {}
                            selectionStart = Pair(newR, newC)
                        }
                    },
                    onDragEnd = {}
                )

                // End Selection Handle (bottom-right of selection)
                val endHandlePos = Offset((toC + 1) * charWidth, (toR + 1) * charHeight)
                TerminalSelectionHandle(
                    position = endHandlePos,
                    isStart = false,
                    onDragStart = {
                        dragEndAnchor = Offset(toC * charWidth + charWidth * 0.5f, toR * charHeight + charHeight * 0.5f)
                    },
                    onDrag = { dragDelta ->
                        val curPixelX = dragEndAnchor.x + dragDelta.x
                        val curPixelY = dragEndAnchor.y + dragDelta.y
                        val newR = (curPixelY / charHeight).toInt().coerceIn(0, rows - 1)
                        val newC = (curPixelX / charWidth).toInt().coerceIn(0, cols - 1)
                        if (selectionEnd?.first != newR || selectionEnd?.second != newC) {
                            try { hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove) } catch (_: Exception) {}
                            selectionEnd = Pair(newR, newC)
                        }
                    },
                    onDragEnd = {}
                )
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

                        // Select All Screen Text
                        TextButton(
                            onClick = {
                                selectionStart = Pair(0, 0)
                                selectionEnd = Pair(rows - 1, cols - 1)
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
