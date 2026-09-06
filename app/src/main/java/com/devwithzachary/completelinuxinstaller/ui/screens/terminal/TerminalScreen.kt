package com.devwithzachary.completelinuxinstaller.ui.screens.terminal

import android.app.Activity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devwithzachary.completelinuxinstaller.engine.TerminalBridge
import com.devwithzachary.completelinuxinstaller.model.ContainerInstance
import com.devwithzachary.completelinuxinstaller.ui.components.EditHotkeysDialog
import com.devwithzachary.completelinuxinstaller.ui.components.ExtraKeysRow
import com.devwithzachary.completelinuxinstaller.util.HotkeyManager

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun TerminalScreen(
    terminalBridge: TerminalBridge,
    containers: List<ContainerInstance> = emptyList(),
    defaultContainerId: String = "ubuntu_default",
    onStartSession: () -> Unit,
    onStopSession: () -> Unit,
    onCreateTab: (containerId: String?, user: String?, title: String?) -> Unit = { _, _, _ -> },
    onSwitchTab: (sessionId: String) -> Unit = {},
    onCloseTab: (sessionId: String) -> Unit = {},
    onRenameTab: (sessionId: String, newTitle: String) -> Unit = { _, _ -> },
    defaultLoginUser: String = "root",
    fontSizeSp: Int = 13,
    fontFamilyName: String = TerminalFonts.DEFAULT_FONT,
    isKeepScreenOnEnabled: Boolean = true
) {
    val context = LocalContext.current
    val view = LocalView.current
    val clipboardManager = LocalClipboardManager.current

    val sessions by terminalBridge.sessions.collectAsStateWithLifecycle()
    val activeSessionId by terminalBridge.activeSessionId.collectAsStateWithLifecycle()
    val isRunning by terminalBridge.isRunning.collectAsStateWithLifecycle()
    val refreshTrigger by terminalBridge.refreshTrigger.collectAsStateWithLifecycle()

    val activeSession = sessions.find { it.id == activeSessionId } ?: sessions.firstOrNull()

    // Keep display awake during active sessions
    DisposableEffect(isKeepScreenOnEnabled, isRunning) {
        val shouldKeepScreenOn = isKeepScreenOnEnabled && isRunning
        if (shouldKeepScreenOn) {
            view.keepScreenOn = true
        }
        onDispose {
            if (shouldKeepScreenOn) {
                view.keepScreenOn = false
            }
        }
    }

    val window = remember(view) { (view.context as? Activity)?.window }
    val insetsController = remember(window, view) { window?.let { WindowCompat.getInsetsController(it, view) } }

    DisposableEffect(window, insetsController) {
        val prevStatusBarColor = window?.statusBarColor
        val prevLightStatusBars = insetsController?.isAppearanceLightStatusBars

        window?.statusBarColor = android.graphics.Color.parseColor("#242424")
        insetsController?.isAppearanceLightStatusBars = false

        onDispose {
            if (window != null && prevStatusBarColor != null) {
                window.statusBarColor = prevStatusBarColor
            }
            if (insetsController != null && prevLightStatusBars != null) {
                insetsController.isAppearanceLightStatusBars = prevLightStatusBars
            }
        }
    }

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    var customHotkeys by remember { mutableStateOf(HotkeyManager.getHotkeys(context)) }
    var showEditHotkeysDialog by remember { mutableStateOf(false) }
    var showNewTabDialog by remember { mutableStateOf(false) }
    var sessionToRename by remember { mutableStateOf<Pair<String, String>?>(null) }

    var isCtrlActive by remember { mutableStateOf(false) }
    var isAltActive by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (sessions.isEmpty()) {
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
            color = Color(0xFF242424)
        ) {
            Column(modifier = Modifier.statusBarsPadding()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
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
                                    color = if (isRunning) Color(0xFF4CAF50) else Color(0xFFE57373),
                                    shape = CircleShape
                                )
                        )
                        val promptSymbol = if (defaultLoginUser == "root") "#" else "$"
                        val activeTitle = activeSession?.let { it.title.value } ?: "Terminal"
                        Text(
                            text = if (isRunning) "$activeTitle $promptSymbol" else "$activeTitle (Stopped)",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
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
                                tint = Color(0xFF81D4FA),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(onClick = { showEditHotkeysDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Edit Hotkeys",
                                tint = Color(0xFFFFB74D),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(onClick = {
                            focusRequester.requestFocus()
                            keyboardController?.show()
                        }) {
                            Icon(
                                imageVector = Icons.Default.Keyboard,
                                contentDescription = "Show Keyboard",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        if (isRunning) {
                            IconButton(onClick = {
                                isCtrlActive = false
                                isAltActive = false
                                onStopSession()
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Stop,
                                    contentDescription = "Stop Terminal",
                                    tint = Color(0xFFE57373),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        } else {
                            IconButton(onClick = onStartSession) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Start Terminal",
                                    tint = Color(0xFF81C784),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                // Tab Strip for Multiple Concurrent Terminal Windows
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF1E1E1E)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LazyRow(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            items(sessions, key = { it.id }) { session ->
                                val isSelected = session.id == activeSessionId
                                val sessionRunning by session.isRunning.collectAsStateWithLifecycle()
                                val sessionTitle by session.title.collectAsStateWithLifecycle()

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) Color(0xFF333333) else Color(0xFF262626),
                                    border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4CAF50)) else null,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .combinedClickable(
                                            onClick = {
                                                onSwitchTab(session.id)
                                                terminalBridge.switchActiveSession(session.id)
                                                focusRequester.requestFocus()
                                            },
                                            onLongClick = {
                                                sessionToRename = Pair(session.id, sessionTitle)
                                            }
                                        )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .background(
                                                    color = if (sessionRunning) Color(0xFF4CAF50) else Color(0xFF9E9E9E),
                                                    shape = CircleShape
                                                )
                                        )

                                        Text(
                                            text = sessionTitle,
                                            color = if (isSelected) Color.White else Color(0xFFB0B0B0),
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            fontFamily = FontFamily.Monospace,
                                            maxLines = 1
                                        )

                                        IconButton(
                                            onClick = {
                                                onCloseTab(session.id)
                                                terminalBridge.closeSession(session.id)
                                            },
                                            modifier = Modifier.size(16.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Close Tab",
                                                tint = Color(0xFFB0B0B0),
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // + New Tab Button
                        IconButton(
                            onClick = {
                                if (containers.size > 1) {
                                    showNewTabDialog = true
                                } else {
                                    val container = containers.find { it.id == defaultContainerId } ?: containers.firstOrNull()
                                    onCreateTab(container?.id, container?.defaultUser ?: defaultLoginUser, null)
                                }
                            },
                            modifier = Modifier
                                .size(28.dp)
                                .background(Color(0xFF333333), shape = RoundedCornerShape(6.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "New Terminal Tab",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
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
            isCtrlActive = isCtrlActive,
            isAltActive = isAltActive,
            onConsumeModifiers = {
                isCtrlActive = false
                isAltActive = false
            },
            modifier = Modifier.weight(1f),
            fontSizeSp = fontSizeSp,
            fontFamilyName = fontFamilyName
        )

        // Touch Navigation & Quick Command Keys Ribbon (Positioned directly above keyboard)
        ExtraKeysRow(
            keys = customHotkeys,
            onPaste = {
                val clipText = clipboardManager.getText()?.text
                if (!clipText.isNullOrEmpty()) {
                    terminalBridge.pasteText(clipText)
                }
            },
            isCtrlActive = isCtrlActive,
            onToggleCtrl = {
                isCtrlActive = !isCtrlActive
                focusRequester.requestFocus()
                keyboardController?.show()
            },
            isAltActive = isAltActive,
            onToggleAlt = {
                isAltActive = !isAltActive
                focusRequester.requestFocus()
                keyboardController?.show()
            },
            onKeyClick = { key ->
                if (key == "Paste") {
                    val clipText = clipboardManager.getText()?.text
                    if (!clipText.isNullOrEmpty()) {
                        terminalBridge.pasteText(clipText)
                    }
                } else {
                    if (isCtrlActive || isAltActive) {
                        if (key.length == 1) {
                            terminalBridge.sendModifiedChar(key[0], isCtrlActive, isAltActive)
                        } else {
                            terminalBridge.sendKeyShortcut(key)
                        }
                        isCtrlActive = false
                        isAltActive = false
                    } else {
                        terminalBridge.sendKeyShortcut(key)
                    }
                }
            }
        )
    }

    // New Tab Selector Dialog
    if (showNewTabDialog) {
        AlertDialog(
            onDismissRequest = { showNewTabDialog = false },
            title = { Text("Open New Terminal Tab", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Select container rootfs for this session:", style = MaterialTheme.typography.bodyMedium)
                    containers.forEach { container ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    showNewTabDialog = false
                                    onCreateTab(container.id, container.defaultUser, "${container.name} (${sessionTabCount(sessions, container.id) + 1})")
                                },
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
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
                                            .clip(CircleShape)
                                            .background(Color(container.colorHex))
                                    )
                                    Column {
                                        Text(container.name, fontWeight = FontWeight.Bold)
                                        Text(container.distroName, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNewTabDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Rename Tab Dialog
    sessionToRename?.let { (sessionId, currentTitle) ->
        var newTitleText by remember { mutableStateOf(currentTitle) }
        AlertDialog(
            onDismissRequest = { sessionToRename = null },
            title = { Text("Rename Tab", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newTitleText,
                    onValueChange = { newTitleText = it },
                    label = { Text("Tab Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (newTitleText.isNotBlank()) {
                        onRenameTab(sessionId, newTitleText.trim())
                        terminalBridge.renameSession(sessionId, newTitleText.trim())
                    }
                    sessionToRename = null
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { sessionToRename = null }) {
                    Text("Cancel")
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

private fun sessionTabCount(sessions: List<com.devwithzachary.completelinuxinstaller.engine.TerminalSession>, containerId: String): Int {
    return sessions.count { it.containerId == containerId }
}
