package com.devwithzachary.completelinuxinstaller.ui.screens.terminal

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.DpOffset
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
    fontSizeSp: Int = 13,
    fontFamilyName: String = "Monospace"
) {
    val density = LocalDensity.current
    val fontSizePx = with(density) { fontSizeSp.sp.toPx() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val clipboardManager = LocalClipboardManager.current

    var showContextMenu by remember { mutableStateOf(false) }
    var menuOffset by remember { mutableStateOf(DpOffset.Zero) }

    var selectionStart by remember { mutableStateOf<Pair<Int, Int>?>(null) } // (row, col)
    var selectionEnd by remember { mutableStateOf<Pair<Int, Int>?>(null) }   // (row, col)
    var showSelectPortionDialog by remember { mutableStateOf(false) }
    var accumulatedScrollY by remember { mutableFloatStateOf(0f) }

    val selectedTypeface = remember(fontFamilyName) {
        when (fontFamilyName) {
            "JetBrains Mono" -> Typeface.create("monospace", Typeface.BOLD)
            "Sans Serif" -> Typeface.create("sans-serif", Typeface.NORMAL)
            "Serif" -> Typeface.create("serif", Typeface.NORMAL)
            "Cursive" -> Typeface.create("cursive", Typeface.NORMAL)
            "Casual" -> Typeface.create("casual", Typeface.NORMAL)
            "Wingdings" -> Typeface.create("monospace", Typeface.NORMAL)
            else -> Typeface.MONOSPACE
        }
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

        var isSelecting by remember { mutableStateOf(false) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(cols, rows) {
                    detectDragGestures(
                        onDragStart = { _ ->
                            accumulatedScrollY = 0f
                            isSelecting = false
                        },
                        onDrag = { change, dragAmount ->
                            // Check if gesture is primarily horizontal text selection vs vertical scrolling
                            val absX = kotlin.math.abs(dragAmount.x)
                            val absY = kotlin.math.abs(dragAmount.y)

                            if (!isSelecting && absX > absY * 1.5f && selectionStart == null) {
                                isSelecting = true
                                val offset = change.position
                                val c = (offset.x / charWidth).toInt().coerceIn(0, cols - 1)
                                val r = (offset.y / charHeight).toInt().coerceIn(0, rows - 1)
                                selectionStart = Pair(r, c)
                                selectionEnd = Pair(r, c)
                            }

                            if (isSelecting || selectionStart != null) {
                                val offset = change.position
                                val c = (offset.x / charWidth).toInt().coerceIn(0, cols - 1)
                                val r = (offset.y / charHeight).toInt().coerceIn(0, rows - 1)
                                selectionEnd = Pair(r, c)
                            } else {
                                // Pure Vertical Scroll Mode - No selection highlighting interference
                                accumulatedScrollY += dragAmount.y
                                val threshold = charHeight * 0.8f
                                if (accumulatedScrollY > threshold) {
                                    val lines = (accumulatedScrollY / charHeight).toInt()
                                    terminalBridge.scrollUp(lines)
                                    accumulatedScrollY %= charHeight
                                } else if (accumulatedScrollY < -threshold) {
                                    val lines = (-accumulatedScrollY / charHeight).toInt()
                                    terminalBridge.scrollDown(lines)
                                    accumulatedScrollY %= charHeight
                                }
                            }
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            selectionStart = null
                            selectionEnd = null
                            focusRequester.requestFocus()
                            keyboardController?.show()
                            onTapTerminal()
                        },
                        onLongPress = { offset ->
                            menuOffset = DpOffset(offset.x.toDp(), offset.y.toDp())
                            showContextMenu = true
                        }
                    )
                }
        ) {
            // Context Menu for Long-Press (Copy / Paste / Select Portion)
            DropdownMenu(
                expanded = showContextMenu,
                onDismissRequest = { showContextMenu = false },
                offset = menuOffset
            ) {
                DropdownMenuItem(
                    text = { Text("📋 Paste") },
                    onClick = {
                        showContextMenu = false
                        val clipText = clipboardManager.getText()?.text
                        if (!clipText.isNullOrEmpty()) {
                            terminalBridge.pasteText(clipText)
                        }
                    }
                )
                DropdownMenuItem(
                    text = { Text("✂️ Select Portion...") },
                    onClick = {
                        showContextMenu = false
                        showSelectPortionDialog = true
                    }
                )
                DropdownMenuItem(
                    text = { Text("📄 Copy Screen Text") },
                    onClick = {
                        showContextMenu = false
                        val text = terminalBridge.getScreenText()
                        if (text.isNotEmpty()) {
                            clipboardManager.setText(AnnotatedString(text))
                        }
                    }
                )
                DropdownMenuItem(
                    text = { Text("📜 Copy All Output") },
                    onClick = {
                        showContextMenu = false
                        val text = terminalBridge.getAllTerminalText()
                        if (text.isNotEmpty()) {
                            clipboardManager.setText(AnnotatedString(text))
                        }
                    }
                )
            }

            // Invisible BasicTextField to capture all software and physical keyboard inputs directly into PTY
            BasicTextField(
                value = textFieldValue,
                onValueChange = { newValue ->
                    val oldText = lastText
                    val newText = newValue.text

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
                    keyboardType = KeyboardType.Password,
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

                // Selection calculation
                val selStart = selectionStart
                val selEnd = selectionEnd
                val minR = if (selStart != null && selEnd != null) minOf(selStart.first, selEnd.first) else -1
                val maxR = if (selStart != null && selEnd != null) maxOf(selStart.first, selEnd.first) else -1
                val minC = if (selStart != null && selEnd != null) minOf(selStart.second, selEnd.second) else -1
                val maxC = if (selStart != null && selEnd != null) maxOf(selStart.second, selEnd.second) else -1

                for (r in 0 until renderRows) {
                    val rowY = r * charHeight
                    val rowChars = emulator.getRenderRow(r)
                    val actualCols = rowChars.size

                    for (c in 0 until actualCols) {
                        val cell: TerminalChar = rowChars[c]
                        val cellX = c * charWidth

                        val isSelected = minR != -1 && r in minR..maxR &&
                                (r > minR || c >= minC) && (r < maxR || c <= maxC)

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

                        paint.isFakeBoldText = cell.bold
                        paint.isUnderlineText = cell.underline

                        if (cell.ch != ' ') {
                            val charStr = if (fontFamilyName == "Wingdings") toWingdingsChar(cell.ch) else cell.ch.toString()
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

            // Floating Selection Bar when Drag-Selecting Text
            if (selectionStart != null && selectionEnd != null) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 12.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFF2D2D2D),
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                val start = selectionStart
                                val end = selectionEnd
                                if (start != null && end != null) {
                                    val text = terminalBridge.getSelectedText(start.first, start.second, end.first, end.second)
                                    if (text.isNotEmpty()) {
                                        clipboardManager.setText(AnnotatedString(text))
                                    }
                                }
                                selectionStart = null
                                selectionEnd = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007ACC)),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy Selection", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy Selection", fontSize = 12.sp)
                        }

                        IconButton(
                            onClick = {
                                selectionStart = null
                                selectionEnd = null
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Clear Selection", tint = Color.LightGray)
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

    // Modal Dialog for Select Portion mode
    if (showSelectPortionDialog) {
        val fullTerminalText = remember { terminalBridge.getAllTerminalText() }
        AlertDialog(
            onDismissRequest = { showSelectPortionDialog = false },
            title = {
                Text(
                    "Select & Copy Output Portion",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 350.dp)
                        .background(Color(0xFF141414), shape = RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    SelectionContainer {
                        Text(
                            text = fullTerminalText.ifEmpty { "Terminal output is empty" },
                            color = Color(0xFFE0E0E0),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            modifier = Modifier.verticalScroll(rememberScrollState())
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(fullTerminalText))
                        showSelectPortionDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007ACC))
                ) {
                    Text("Copy All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSelectPortionDialog = false }) {
                    Text("Done", color = Color(0xFF81D4FA))
                }
            },
            containerColor = Color(0xFF2D2D2D)
        )
    }
}

private fun toWingdingsChar(ch: Char): String {
    return when (ch) {
        'a' -> "✌"
        'b' -> "👌"
        'c' -> "👍"
        'd' -> "👎"
        'e' -> "👈"
        'f' -> "👉"
        'g' -> "👆"
        'h' -> "👇"
        'i' -> "🖐"
        'j' -> "☺"
        'k' -> "😐"
        'l' -> "☹"
        'm' -> "💣"
        'n' -> "☠"
        'o' -> "⚐"
        'p' -> "⚑"
        'q' -> "✈"
        'r' -> "☼"
        's' -> "💧"
        't' -> "❄"
        'u' -> "🕇"
        'v' -> "🕈"
        'w' -> "✠"
        'x' -> "✡"
        'y' -> "☸"
        'z' -> "☯"
        'A' -> "✌"
        'B' -> "👌"
        'C' -> "👍"
        'D' -> "👎"
        'E' -> "👈"
        'F' -> "👉"
        'G' -> "👆"
        'H' -> "👇"
        'I' -> "🖐"
        'J' -> "☺"
        'K' -> "😐"
        'L' -> "☹"
        'M' -> "💣"
        'N' -> "☠"
        'O' -> "⚐"
        'P' -> "⚑"
        'Q' -> "✈"
        'R' -> "☼"
        'S' -> "💧"
        'T' -> "❄"
        'U' -> "🕇"
        'V' -> "🕈"
        'W' -> "✠"
        'X' -> "✡"
        'Y' -> "☸"
        'Z' -> "☯"
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

