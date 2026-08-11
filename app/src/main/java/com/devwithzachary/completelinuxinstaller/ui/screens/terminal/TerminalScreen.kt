package com.devwithzachary.completelinuxinstaller.ui.screens.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devwithzachary.completelinuxinstaller.engine.TerminalBridge
import com.devwithzachary.completelinuxinstaller.ui.components.EditHotkeysDialog
import com.devwithzachary.completelinuxinstaller.ui.components.ExtraKeysRow
import com.devwithzachary.completelinuxinstaller.util.HotkeyManager

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TerminalScreen(
    terminalBridge: TerminalBridge,
    onStartSession: () -> Unit,
    onStopSession: () -> Unit,
    defaultLoginUser: String = "root"
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val isRunning by terminalBridge.isRunning.collectAsStateWithLifecycle()
    val refreshTrigger by terminalBridge.refreshTrigger.collectAsStateWithLifecycle()

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    var customHotkeys by remember { mutableStateOf(HotkeyManager.getHotkeys(context)) }
    var showEditHotkeysDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!isRunning) {
            onStartSession()
        }
        focusRequester.requestFocus()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E1E))
            .imePadding()
    ) {
        // Terminal Top Action Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF2D2D2D)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(
                                color = if (isRunning) Color(0xFF4CAF50) else Color.Red,
                                shape = RoundedCornerShape(5.dp)
                            )
                    )
                    val promptSymbol = if (defaultLoginUser == "root") "#" else "$"
                    Text(
                        text = if (isRunning) "$defaultLoginUser@ubuntu:~$promptSymbol (Active PTY)" else "Terminal (Stopped)",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        val clipText = clipboardManager.getText()?.text
                        if (!clipText.isNullOrEmpty()) {
                            terminalBridge.pasteText(clipText)
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.ContentPaste,
                            contentDescription = "Paste Clipboard",
                            tint = Color(0xFF81D4FA)
                        )
                    }

                    IconButton(onClick = { showEditHotkeysDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Edit Hotkeys",
                            tint = Color(0xFFFFB74D)
                        )
                    }

                    IconButton(onClick = {
                        focusRequester.requestFocus()
                        keyboardController?.show()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Keyboard,
                            contentDescription = "Show Keyboard",
                            tint = Color.White
                        )
                    }

                    if (isRunning) {
                        IconButton(onClick = onStopSession) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = "Stop Terminal",
                                tint = Color(0xFFE57373)
                            )
                        }
                    } else {
                        IconButton(onClick = onStartSession) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Start Terminal",
                                tint = Color(0xFF81C784)
                            )
                        }
                    }
                }
            }
        }

        // Full Interactive Direct-Typing Terminal Window
        FullTerminalView(
            terminalBridge = terminalBridge,
            refreshTrigger = refreshTrigger,
            focusRequester = focusRequester,
            onTapTerminal = {
                focusRequester.requestFocus()
                keyboardController?.show()
            },
            modifier = Modifier.weight(1f)
        )

        // Touch Navigation & Quick Command Keys Ribbon (Positioned directly above keyboard)
        ExtraKeysRow(
            keys = customHotkeys,
            onKeyClick = { key ->
                when (key) {
                    "Paste" -> {
                        val clipText = clipboardManager.getText()?.text
                        if (!clipText.isNullOrEmpty()) {
                            terminalBridge.pasteText(clipText)
                        }
                    }
                    "Ctrl+C" -> terminalBridge.sendCtrlC()
                    "Ctrl+Z" -> terminalBridge.sendCtrlZ()
                    "Ctrl+D" -> terminalBridge.sendCtrlD()
                    "Tab" -> terminalBridge.sendTab()
                    "Esc" -> terminalBridge.sendEsc()
                    "▲" -> terminalBridge.sendArrowUp()
                    "▼" -> terminalBridge.sendArrowDown()
                    "◄" -> terminalBridge.sendArrowLeft()
                    "►" -> terminalBridge.sendArrowRight()
                    else -> {
                        if (key.length <= 2 && !key.contains(" ")) {
                            terminalBridge.sendInput(key)
                        } else {
                            terminalBridge.sendCommand(key)
                        }
                    }
                }
            }
        )
    }

    if (showEditHotkeysDialog) {
        EditHotkeysDialog(
            currentHotkeys = customHotkeys,
            onSaveHotkeys = { updatedList ->
                HotkeyManager.saveHotkeys(context, updatedList)
                customHotkeys = updatedList
            },
            onResetDefaults = {
                val defaults = HotkeyManager.resetHotkeys(context)
                customHotkeys = defaults
            },
            onDismiss = { showEditHotkeysDialog = false }
        )
    }
}
