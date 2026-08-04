package com.devwithzachary.completelinuxinstaller.ui.screens.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devwithzachary.completelinuxinstaller.engine.TerminalBridge
import com.devwithzachary.completelinuxinstaller.ui.components.ExtraKeysRow

@Composable
fun TerminalScreen(
    terminalBridge: TerminalBridge,
    onStartSession: () -> Unit,
    onStopSession: () -> Unit
) {
    val isRunning by terminalBridge.isRunning.collectAsStateWithLifecycle()
    val refreshTrigger by terminalBridge.refreshTrigger.collectAsStateWithLifecycle()

    var inputText by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E1E))
    ) {
        // Terminal Top Action Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF2D2D2D)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
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
                    Text(
                        text = if (isRunning) "root@ubuntu:~# (Active PTY)" else "Terminal (Stopped)",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (isRunning) {
                        IconButton(onClick = onStopSession) {
                            Icon(Icons.Default.Stop, contentDescription = "Stop", tint = Color.Red)
                        }
                    } else {
                        IconButton(onClick = onStartSession) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Start", tint = Color(0xFF4CAF50))
                        }
                    }
                }
            }
        }

        // Real Interactive ANSI / PTY Canvas Screen Buffer View
        FullTerminalView(
            terminalBridge = terminalBridge,
            refreshTrigger = refreshTrigger,
            focusRequester = focusRequester,
            onTapTerminal = { focusRequester.requestFocus() },
            modifier = Modifier.weight(1f)
        )

        // Touch Navigation & Command Keys Ribbon
        ExtraKeysRow(
            onKeyClick = { key ->
                when (key) {
                    "Ctrl+C" -> terminalBridge.sendCtrlC()
                    "Ctrl+Z" -> terminalBridge.sendCtrlZ()
                    "Ctrl+D" -> terminalBridge.sendCtrlD()
                    "Tab" -> terminalBridge.sendTab()
                    "Esc" -> terminalBridge.sendEsc()
                    "▲" -> terminalBridge.sendArrowUp()
                    "▼" -> terminalBridge.sendArrowDown()
                    "◄" -> terminalBridge.sendArrowLeft()
                    "►" -> terminalBridge.sendArrowRight()
                    "clear" -> terminalBridge.sendCommand("clear")
                    "htop" -> terminalBridge.sendCommand("htop")
                    "uname -a" -> terminalBridge.sendCommand("uname -a")
                    "df -h" -> terminalBridge.sendCommand("df -h")
                    else -> terminalBridge.sendInput(key)
                }
            }
        )

        // Interactive Command Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF252526)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Type interactive input or bash command...", color = Color.Gray, fontSize = 14.sp) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        autoCorrectEnabled = false,
                        keyboardType = KeyboardType.Ascii,
                        imeAction = ImeAction.Send
                    ),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (inputText.isNotEmpty()) {
                                terminalBridge.sendCommand(inputText)
                                inputText = ""
                            }
                        }
                    ),
                    trailingIcon = {
                        if (inputText.isNotEmpty()) {
                            IconButton(onClick = { inputText = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear Input", tint = Color.Gray)
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF1E1E1E),
                        unfocusedContainerColor = Color(0xFF1E1E1E),
                        focusedBorderColor = Color(0xFF81D4FA),
                        unfocusedBorderColor = Color.DarkGray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        if (inputText.isNotEmpty()) {
                            terminalBridge.sendCommand(inputText)
                            inputText = ""
                        }
                    },
                    modifier = Modifier.background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(8.dp)
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = Color.White
                    )
                }
            }
        }
    }
}
