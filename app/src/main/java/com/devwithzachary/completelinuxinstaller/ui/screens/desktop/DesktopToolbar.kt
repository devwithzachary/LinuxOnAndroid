package com.devwithzachary.completelinuxinstaller.ui.screens.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.FitScreen
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Mouse
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.ZoomOutMap
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devwithzachary.completelinuxinstaller.engine.desktop.DesktopResolution
import com.devwithzachary.completelinuxinstaller.engine.desktop.DesktopTouchMode
import com.devwithzachary.completelinuxinstaller.engine.desktop.DesktopUiState
import com.devwithzachary.completelinuxinstaller.engine.desktop.VncKeyMapper
import com.devwithzachary.completelinuxinstaller.ui.designsystem.LoaBottomSheet
import com.devwithzachary.completelinuxinstaller.ui.designsystem.LoaSectionHeader
import com.devwithzachary.completelinuxinstaller.ui.designsystem.LoaSegmentedGroup
import com.devwithzachary.completelinuxinstaller.ui.theme.LoaMonoTextStyle

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
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.statusBarsPadding()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(MaterialTheme.colorScheme.secondary, CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = state.activeContainerName ?: "Linux Desktop",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
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
                        text = "$resDimensions - ${state.touchMode.label} - ${state.selectedUser}",
                        style = LoaMonoTextStyle.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onToggleKeyboard,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Keyboard,
                        contentDescription = "Toggle Keyboard",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(
                    onClick = onToggleTouchMode,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = if (state.touchMode == DesktopTouchMode.DIRECT_TOUCH) {
                            Icons.Rounded.TouchApp
                        } else {
                            Icons.Rounded.Mouse
                        },
                        contentDescription = "Toggle Touch Mode",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(
                    onClick = onToggleFitToScreen,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = if (state.isFitToScreen) {
                            Icons.Rounded.FitScreen
                        } else {
                            Icons.Rounded.ZoomOutMap
                        },
                        contentDescription = "Toggle Fit Screen",
                        tint = if (state.isFitToScreen) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                }

                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = "Desktop Options",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Display Resolution") },
                            leadingIcon = { Icon(Icons.Rounded.AspectRatio, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                showResolutionDialog = true
                            }
                        )
                        if (state.availableUsers.size > 1) {
                            DropdownMenuItem(
                                text = { Text("Switch User (${state.selectedUser})") },
                                leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    showUserDialog = true
                                }
                            )
                        }
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Restart Desktop") },
                            leadingIcon = { Icon(Icons.Rounded.Refresh, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onRestartSession()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Stop Desktop Session", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.PowerSettingsNew,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            },
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
        LoaBottomSheet(
            onDismissRequest = { showResolutionDialog = false },
            title = "Select Desktop Resolution",
            subtitle = "Choose a preset or automatic screen space"
        ) {
            LoaSegmentedGroup {
                DesktopResolution.DEFAULT_PRESETS.forEach { res ->
                    val isSelected = if (res.isAutomatic) {
                        state.resolution.isAutomatic
                    } else {
                        !state.resolution.isAutomatic &&
                            state.resolution.width == res.width &&
                            state.resolution.height == res.height
                    }
                    val itemLabel = if (res.isAutomatic && effectiveResolution != null) {
                        val scaleSuffix = if (state.uiScalePercent != 100) " @ ${state.uiScalePercent}%" else ""
                        "Automatic (${effectiveResolution.width}x${effectiveResolution.height}$scaleSuffix)"
                    } else {
                        res.label
                    }
                    item { shape ->
                        Surface(
                            onClick = {
                                onChangeResolution(res)
                                if (!res.isAutomatic) {
                                    showResolutionDialog = false
                                }
                            },
                            shape = shape,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainer
                            },
                            tonalElevation = 0.dp,
                            shadowElevation = 0.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = itemLabel,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (state.resolution.isAutomatic) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LoaSectionHeader(title = "UI Size / Display Scale")
                    Text(
                        text = "Renders UI larger on high-res displays while preserving aspect ratio and screen space.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.widthIn(max = 560.dp)
                    )
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        DesktopResolution.SCALE_PRESETS.chunked(3).forEach { rowPresets ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowPresets.forEach { scale ->
                                    FilterChip(
                                        selected = state.uiScalePercent == scale,
                                        onClick = { onChangeUiScale(scale) },
                                        label = {
                                            Text(
                                                text = "$scale%",
                                                style = LoaMonoTextStyle,
                                                maxLines = 1
                                            )
                                        },
                                        modifier = Modifier.weight(1f)
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
                Button(
                    onClick = { showResolutionDialog = false },
                    modifier = Modifier
                        .widthIn(max = 320.dp)
                        .fillMaxWidth()
                ) {
                    Text("Close")
                }
            }
        }
    }

    if (showUserDialog) {
        LoaBottomSheet(
            onDismissRequest = { showUserDialog = false },
            title = "Switch Desktop User",
            subtitle = "Restart desktop session as a different user"
        ) {
            LoaSegmentedGroup {
                state.availableUsers.forEach { user ->
                    val isSelected = state.selectedUser == user
                    item { shape ->
                        Surface(
                            onClick = {
                                showUserDialog = false
                                if (user != state.selectedUser) {
                                    onSwitchUser(user)
                                }
                            },
                            shape = shape,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainer
                            },
                            tonalElevation = 0.dp,
                            shadowElevation = 0.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = user,
                                    style = LoaMonoTextStyle,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(20.dp)
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
                    onClick = { showUserDialog = false },
                    modifier = Modifier.widthIn(max = 320.dp)
                ) {
                    Text("Cancel")
                }
            }
        }
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
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
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
            KeyPillButton(label = "ESC", onClick = { onSendKey(VncKeyMapper.KEY_ESCAPE) })
            KeyPillButton(label = "TAB", onClick = { onSendKey(VncKeyMapper.KEY_TAB) })

            KeyPillButton(
                label = "CTRL",
                isActive = state.isCtrlActive,
                onClick = onToggleCtrl
            )

            KeyPillButton(
                label = "ALT",
                isActive = state.isAltActive,
                onClick = onToggleAlt
            )

            KeyPillButton(
                label = "SHIFT",
                isActive = state.isShiftActive,
                onClick = onToggleShift
            )

            KeyPillButton(label = "SUPER", onClick = { onSendKey(VncKeyMapper.KEY_SUPER_LEFT) })
            KeyPillButton(label = "DEL", onClick = { onSendKey(VncKeyMapper.KEY_DELETE) })

            KeyPillButton(label = "◀", onClick = { onSendKey(VncKeyMapper.KEY_LEFT) })
            KeyPillButton(label = "▲", onClick = { onSendKey(VncKeyMapper.KEY_UP) })
            KeyPillButton(label = "▼", onClick = { onSendKey(VncKeyMapper.KEY_DOWN) })
            KeyPillButton(label = "▶", onClick = { onSendKey(VncKeyMapper.KEY_RIGHT) })

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
    val bgColor = if (isActive) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerHighest
    }
    val textColor = if (isActive) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Surface(
        onClick = onClick,
        color = bgColor,
        contentColor = textColor,
        shape = RoundedCornerShape(6.dp),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = modifier.height(32.dp)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = LoaMonoTextStyle.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}
