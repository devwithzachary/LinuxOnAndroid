package com.devwithzachary.completelinuxinstaller.ui.screens.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devwithzachary.completelinuxinstaller.engine.desktop.DesktopResolution
import com.devwithzachary.completelinuxinstaller.engine.desktop.DesktopTouchMode
import com.devwithzachary.completelinuxinstaller.engine.desktop.DesktopUiState
import com.devwithzachary.completelinuxinstaller.engine.desktop.VncKeyMapper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesktopTopBar(
    state: DesktopUiState,
    effectiveResolution: DesktopResolution? = null,
    onToggleKeyboard: () -> Unit,
    onToggleTouchMode: () -> Unit,
    onToggleFitToScreen: () -> Unit,
    onChangeResolution: (DesktopResolution) -> Unit,
    onChangeUiScale: (Int) -> Unit = {},
    onSwitchUser: (String) -> Unit = {},
    onRestartSession: () -> Unit,
    onStopSession: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    var showResolutionDialog by remember { mutableStateOf(false) }
    var showUserDialog by remember { mutableStateOf(false) }

    Surface(
        color = Color(0xFF1E293B),
        contentColor = Color.White,
        tonalElevation = 4.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.statusBarsPadding()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
            // Container badge + status
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(Color(0xFF22C55E), CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = state.activeContainerName ?: "Linux Desktop",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                val activeRes = effectiveResolution ?: state.resolution
                val scaleSuffix = if (state.resolution.isAutomatic && state.uiScalePercent != 100) " (${state.uiScalePercent}%)" else ""
                val resDimensions = if (activeRes.width > 0 && activeRes.height > 0) {
                    "${activeRes.width}x${activeRes.height}$scaleSuffix"
                } else if (state.resolution.width > 0 && state.resolution.height > 0) {
                    "${state.resolution.width}x${state.resolution.height}"
                } else {
                    "Auto$scaleSuffix"
                }
                Text(
                    text = "$resDimensions • ${state.touchMode.label} • ${state.selectedUser}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF94A3B8)
                )
            }

            // Keyboard Toggle
            IconButton(
                onClick = onToggleKeyboard,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Keyboard,
                    contentDescription = "Toggle Keyboard",
                    tint = Color.White
                )
            }

            // Touch Mode Toggle (Direct Touch vs Trackpad)
            IconButton(
                onClick = onToggleTouchMode,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (state.touchMode == DesktopTouchMode.DIRECT_TOUCH) Icons.Default.TouchApp else Icons.Default.Mouse,
                    contentDescription = "Toggle Touch Mode",
                    tint = Color.White
                )
            }

            // Fit to Screen Toggle
            IconButton(
                onClick = onToggleFitToScreen,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (state.isFitToScreen) Icons.Default.FitScreen else Icons.Default.ZoomOutMap,
                    contentDescription = "Toggle Fit Screen",
                    tint = if (state.isFitToScreen) MaterialTheme.colorScheme.primary else Color.White
                )
            }

            // Overflow Menu
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Desktop Options",
                        tint = Color.White
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Display Resolution") },
                        leadingIcon = { Icon(Icons.Default.AspectRatio, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            showResolutionDialog = true
                        }
                    )
                    if (state.availableUsers.size > 1) {
                        DropdownMenuItem(
                            text = { Text("Switch User (${state.selectedUser})") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                showUserDialog = true
                            }
                        )
                    }
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("Restart Desktop") },
                        leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            onRestartSession()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Stop Desktop Session", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Default.PowerSettingsNew, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        onClick = {
                            showMenu = false
                            onStopSession()
                        }
                    )
                }
            }
        }
    }
}

    if (showResolutionDialog) {
        AlertDialog(
            onDismissRequest = { showResolutionDialog = false },
            title = { Text("Select Desktop Resolution") },
            text = {
                Column {
                    DesktopResolution.DEFAULT_PRESETS.forEach { res ->
                        val isSelected = if (res.isAutomatic) {
                            state.resolution.isAutomatic
                        } else {
                            !state.resolution.isAutomatic && state.resolution.width == res.width && state.resolution.height == res.height
                        }
                        val itemLabel = if (res.isAutomatic && effectiveResolution != null) {
                            val scaleSuffix = if (state.uiScalePercent != 100) " @ ${state.uiScalePercent}%" else ""
                            "Automatic (${effectiveResolution.width}x${effectiveResolution.height}$scaleSuffix) - Screen Space"
                        } else {
                            res.label
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    onChangeResolution(res)
                                    if (!res.isAutomatic) {
                                        showResolutionDialog = false
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(itemLabel)
                        }
                    }

                    if (state.resolution.isAutomatic) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "UI Size / Display Scale",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Renders UI larger on high-res displays while preserving aspect ratio and screen space:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            DesktopResolution.SCALE_PRESETS.chunked(3).forEach { rowPresets ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    rowPresets.forEach { scale ->
                                        FilterChip(
                                            selected = state.uiScalePercent == scale,
                                            onClick = { onChangeUiScale(scale) },
                                            label = { Text("${scale}%", fontSize = 11.sp, maxLines = 1) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showResolutionDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (showUserDialog) {
        AlertDialog(
            onDismissRequest = { showUserDialog = false },
            title = { Text("Switch Desktop User") },
            text = {
                Column {
                    Text(
                        text = "Restart desktop session as a different user:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    state.availableUsers.forEach { user ->
                        val isSelected = state.selectedUser == user
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    showUserDialog = false
                                    if (user != state.selectedUser) {
                                        onSwitchUser(user)
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(user, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showUserDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun DesktopQuickKeysRibbon(
    state: DesktopUiState,
    onToggleCtrl: () -> Unit,
    onToggleAlt: () -> Unit,
    onToggleShift: () -> Unit,
    onSendKey: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xFF0F172A),
        contentColor = Color.White,
        tonalElevation = 2.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Esc
            KeyPillButton(label = "ESC", onClick = { onSendKey(VncKeyMapper.KEY_ESCAPE) })
            // Tab
            KeyPillButton(label = "TAB", onClick = { onSendKey(VncKeyMapper.KEY_TAB) })

            // Ctrl (latch)
            KeyPillButton(
                label = "CTRL",
                isActive = state.isCtrlActive,
                onClick = onToggleCtrl
            )

            // Alt (latch)
            KeyPillButton(
                label = "ALT",
                isActive = state.isAltActive,
                onClick = onToggleAlt
            )

            // Shift (latch)
            KeyPillButton(
                label = "SHIFT",
                isActive = state.isShiftActive,
                onClick = onToggleShift
            )

            // Super (Win)
            KeyPillButton(label = "SUPER", onClick = { onSendKey(VncKeyMapper.KEY_SUPER_LEFT) })

            // Delete
            KeyPillButton(label = "DEL", onClick = { onSendKey(VncKeyMapper.KEY_DELETE) })

            // Directional Arrows
            KeyPillButton(label = "◀", onClick = { onSendKey(VncKeyMapper.KEY_LEFT) })
            KeyPillButton(label = "▲", onClick = { onSendKey(VncKeyMapper.KEY_UP) })
            KeyPillButton(label = "▼", onClick = { onSendKey(VncKeyMapper.KEY_DOWN) })
            KeyPillButton(label = "▶", onClick = { onSendKey(VncKeyMapper.KEY_RIGHT) })

            // Enter
            KeyPillButton(label = "ENTER", onClick = { onSendKey(VncKeyMapper.KEY_RETURN) })
        }
    }
}

@Composable
fun KeyPillButton(
    label: String,
    isActive: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isActive) MaterialTheme.colorScheme.primary else Color(0xFF334155)
    val textColor = if (isActive) MaterialTheme.colorScheme.onPrimary else Color.White

    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = bgColor,
            contentColor = textColor
        ),
        shape = RoundedCornerShape(6.dp),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
        modifier = modifier.height(32.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
