package com.devwithzachary.completelinuxinstaller.ui.screens.terminal

import android.app.Activity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.KeyboardHide
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.StopCircle
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
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
import com.devwithzachary.completelinuxinstaller.ui.designsystem.LoaBottomSheet
import com.devwithzachary.completelinuxinstaller.ui.designsystem.LoaSegmentedGroup
import com.devwithzachary.completelinuxinstaller.ui.theme.LoaMonoTextStyle
import com.devwithzachary.completelinuxinstaller.ui.util.HotkeyManager
import com.devwithzachary.completelinuxinstaller.ui.util.handHover
import com.devwithzachary.completelinuxinstaller.ui.util.isHardwareKeyboardConnected
import com.devwithzachary.completelinuxinstaller.ui.util.onSecondaryClick

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
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
    @Suppress("DEPRECATION")
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
    val headerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val isLightHeader = headerColor.luminance() > 0.5f

    @Suppress("DEPRECATION")
    DisposableEffect(window, insetsController, headerColor) {
        val prevStatusBarColor = window?.statusBarColor
        val prevLightStatusBars = insetsController?.isAppearanceLightStatusBars

        window?.statusBarColor = headerColor.toArgb()
        insetsController?.isAppearanceLightStatusBars = isLightHeader

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
    var showStopConfirmDialog by remember { mutableStateOf(false) }
    var sessionToRename by remember { mutableStateOf<Pair<String, String>?>(null) }
    val isHardwareKeyboard = isHardwareKeyboardConnected()
    var showExtraKeysOverride by remember { mutableStateOf<Boolean?>(null) }
    val showExtraKeys = showExtraKeysOverride ?: !isHardwareKeyboard
    var tabContextMenuSessionId by remember { mutableStateOf<String?>(null) }

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
            .background(MaterialTheme.colorScheme.surface)
            .imePadding()
    ) {
        // Terminal Top Action Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = headerColor,
            contentColor = MaterialTheme.colorScheme.onSurface,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
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
                        modifier = Modifier.weight(1f, fill = false),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    color = if (isRunning) {
                                        MaterialTheme.colorScheme.secondary
                                    } else {
                                        MaterialTheme.colorScheme.error
                                    },
                                    shape = CircleShape
                                )
                        )
                        val promptSymbol = if (defaultLoginUser == "root") "#" else "$"
                        val activeTitle = activeSession?.let { it.title.value } ?: "Terminal"
                        Text(
                            text = if (isRunning) "$activeTitle $promptSymbol" else "$activeTitle (Stopped)",
                            color = MaterialTheme.colorScheme.onSurface,
                            style = LoaMonoTextStyle.copy(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { showExtraKeysOverride = !showExtraKeys },
                                modifier = Modifier
                                    .size(36.dp)
                                    .handHover()
                            ) {
                                Icon(
                                    imageVector = if (showExtraKeys) Icons.Rounded.KeyboardHide else Icons.Rounded.KeyboardArrowUp,
                                    contentDescription = if (showExtraKeys) "Hide On-screen Keys" else "Show On-screen Keys",
                                    tint = if (showExtraKeys) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            IconButton(
                                onClick = {
                                    val clipText = clipboardManager.getText()?.text
                                    if (!clipText.isNullOrEmpty()) {
                                        terminalBridge.pasteText(clipText)
                                    }
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .handHover()
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ContentPaste,
                                    contentDescription = "Paste Clipboard",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            IconButton(
                                onClick = { showEditHotkeysDialog = true },
                                modifier = Modifier
                                    .size(36.dp)
                                    .handHover()
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Tune,
                                    contentDescription = "Edit Hotkeys",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            IconButton(
                                onClick = {
                                    focusRequester.requestFocus()
                                    keyboardController?.show()
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .handHover()
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Keyboard,
                                    contentDescription = "Show Keyboard",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            if (isRunning) {
                                IconButton(
                                    onClick = { showStopConfirmDialog = true },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .handHover()
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Stop,
                                        contentDescription = "Stop Terminal",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            } else {
                                IconButton(
                                    onClick = onStartSession,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .handHover()
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.PlayArrow,
                                        contentDescription = "Start Terminal",
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Tab Strip for Multiple Concurrent Terminal Windows
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp
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

                                val tabContainerColor = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHigh
                                }
                                val tabContentColor = if (isSelected) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }

                                Box {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = tabContainerColor,
                                        contentColor = tabContentColor,
                                        tonalElevation = 0.dp,
                                        shadowElevation = 0.dp,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .handHover()
                                            .combinedClickable(
                                                onClick = {
                                                    onSwitchTab(session.id)
                                                    terminalBridge.switchActiveSession(session.id)
                                                    focusRequester.requestFocus()
                                                },
                                                onLongClick = {
                                                    tabContextMenuSessionId = session.id
                                                }
                                            )
                                            .onSecondaryClick {
                                                tabContextMenuSessionId = session.id
                                            }
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
                                                        color = if (sessionRunning) {
                                                            MaterialTheme.colorScheme.secondary
                                                        } else {
                                                            MaterialTheme.colorScheme.outline
                                                        },
                                                        shape = CircleShape
                                                    )
                                            )

                                            Text(
                                                text = sessionTitle,
                                                color = tabContentColor,
                                                style = LoaMonoTextStyle.copy(
                                                    fontSize = 12.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                ),
                                                maxLines = 1
                                            )

                                            IconButton(
                                                onClick = {
                                                    onCloseTab(session.id)
                                                    terminalBridge.closeSession(session.id)
                                                },
                                                modifier = Modifier
                                                    .size(18.dp)
                                                    .handHover()
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Close,
                                                    contentDescription = "Close $sessionTitle",
                                                    tint = tabContentColor,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }

                                    DropdownMenu(
                                        expanded = tabContextMenuSessionId == session.id,
                                        onDismissRequest = { tabContextMenuSessionId = null }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Rename Tab") },
                                            onClick = {
                                                tabContextMenuSessionId = null
                                                sessionToRename = Pair(session.id, sessionTitle)
                                            },
                                            leadingIcon = {
                                                Icon(Icons.Rounded.Edit, contentDescription = null)
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Close Tab") },
                                            onClick = {
                                                tabContextMenuSessionId = null
                                                onCloseTab(session.id)
                                                terminalBridge.closeSession(session.id)
                                            },
                                            leadingIcon = {
                                                Icon(Icons.Rounded.Close, contentDescription = null)
                                            }
                                        )
                                        if (sessions.size > 1) {
                                            HorizontalDivider()
                                            DropdownMenuItem(
                                                text = { Text("Close Other Tabs") },
                                                onClick = {
                                                    tabContextMenuSessionId = null
                                                    sessions.filter { it.id != session.id }.forEach { other ->
                                                        onCloseTab(other.id)
                                                        terminalBridge.closeSession(other.id)
                                                    }
                                                },
                                                leadingIcon = {
                                                    Icon(Icons.Rounded.Close, contentDescription = null)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        Surface(
                            onClick = {
                                if (containers.size > 1) {
                                    showNewTabDialog = true
                                } else {
                                    val container = containers.find { it.id == defaultContainerId } ?: containers.firstOrNull()
                                    onCreateTab(container?.id, container?.defaultUser ?: defaultLoginUser, null)
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            tonalElevation = 0.dp,
                            shadowElevation = 0.dp,
                            modifier = Modifier
                                .size(30.dp)
                                .handHover()
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = "New Terminal Tab",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Full Interactive Direct-Typing Terminal Window
        FullTerminalView(
            terminalBridge = terminalBridge,
            activeSessionId = activeSessionId,
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
        if (showExtraKeys) {
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
    }

    // New Tab Selector Sheet
    if (showNewTabDialog) {
        LoaBottomSheet(
            onDismissRequest = { showNewTabDialog = false },
            title = "Open New Terminal Tab",
            subtitle = "Select container rootfs for this session"
        ) {
            LoaSegmentedGroup {
                containers.forEach { container ->
                    item { shape ->
                        Surface(
                            onClick = {
                                showNewTabDialog = false
                                onCreateTab(
                                    container.id,
                                    container.defaultUser,
                                    "${container.name} (${sessionTabCount(sessions, container.id) + 1})"
                                )
                            },
                            shape = shape,
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            tonalElevation = 0.dp,
                            shadowElevation = 0.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color(container.colorHex))
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = container.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = container.distroName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                TextButton(
                    onClick = { showNewTabDialog = false },
                    modifier = Modifier.widthIn(max = 320.dp)
                ) {
                    Text("Cancel")
                }
            }
        }
    }

    // Rename Tab Dialog
    sessionToRename?.let { (sessionId, currentTitle) ->
        var newTitleText by remember { mutableStateOf(currentTitle) }
        AlertDialog(
            onDismissRequest = { sessionToRename = null },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            iconContentColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            icon = {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = null
                )
            },
            title = {
                Text(
                    text = "Rename Tab",
                    style = MaterialTheme.typography.headlineSmall
                )
            },
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

    if (showStopConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showStopConfirmDialog = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            iconContentColor = MaterialTheme.colorScheme.error,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            icon = {
                Icon(
                    imageVector = Icons.Rounded.StopCircle,
                    contentDescription = null
                )
            },
            title = {
                Text(
                    text = "Stop Terminal Session?",
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to stop the active terminal session? Any running commands or processes will be terminated.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showStopConfirmDialog = false
                        isCtrlActive = false
                        isAltActive = false
                        onStopSession()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Stop Session")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStopConfirmDialog = false }) {
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

private fun sessionTabCount(
    sessions: List<com.devwithzachary.completelinuxinstaller.engine.TerminalSession>,
    containerId: String
): Int {
    return sessions.count { it.containerId == containerId }
}
