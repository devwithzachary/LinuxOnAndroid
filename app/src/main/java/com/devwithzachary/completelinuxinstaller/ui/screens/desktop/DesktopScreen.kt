package com.devwithzachary.completelinuxinstaller.ui.screens.desktop

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DesktopWindows
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Mouse
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Upgrade
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devwithzachary.completelinuxinstaller.engine.desktop.DesktopResolution
import com.devwithzachary.completelinuxinstaller.engine.desktop.DesktopSessionManager
import com.devwithzachary.completelinuxinstaller.engine.desktop.DesktopStatus
import com.devwithzachary.completelinuxinstaller.engine.desktop.DesktopTouchMode
import com.devwithzachary.completelinuxinstaller.engine.desktop.DesktopUiState
import com.devwithzachary.completelinuxinstaller.engine.desktop.VncKeyMapper
import com.devwithzachary.completelinuxinstaller.model.ContainerInstance
import com.devwithzachary.completelinuxinstaller.ui.designsystem.LoaHeroContainer
import com.devwithzachary.completelinuxinstaller.ui.designsystem.LoaMonospaceChip
import com.devwithzachary.completelinuxinstaller.ui.designsystem.LoaProgressRing
import com.devwithzachary.completelinuxinstaller.ui.designsystem.LoaSectionHeader
import com.devwithzachary.completelinuxinstaller.ui.designsystem.LoaSegmentedGroup
import com.devwithzachary.completelinuxinstaller.ui.designsystem.LoaStatusBadge
import com.devwithzachary.completelinuxinstaller.ui.designsystem.LoaStatusTone
import com.devwithzachary.completelinuxinstaller.ui.theme.LoaMonoTextStyle
import com.devwithzachary.completelinuxinstaller.ui.theme.LoaSegmentedSingleShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesktopScreen(
    desktopSessionManager: DesktopSessionManager,
    containers: List<ContainerInstance>,
    defaultContainerId: String?,
    onNavigateToWizard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    val state by desktopSessionManager.desktopState.collectAsState()
    val isRunning by desktopSessionManager.isDesktopRunning.collectAsState()

    var showKeyboard by remember { mutableStateOf(false) }

    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val isCompact = configuration.screenWidthDp < 600

    val approxTopBarHeight = statusBarPadding + 48.dp
    val approxBottomNavHeight = if (isCompact) 80.dp else 0.dp
    val approxHotkeysHeight = 44.dp
    val estimatedCanvasWidthPx = with(density) { configuration.screenWidthDp.dp.roundToPx() }
    val estimatedCanvasHeightPx = with(density) {
        (configuration.screenHeightDp.dp - approxTopBarHeight - approxBottomNavHeight - approxHotkeysHeight)
            .coerceAtLeast(320.dp)
            .roundToPx()
    }

    LaunchedEffect(configuration.screenWidthDp, configuration.screenHeightDp, configuration.orientation) {
        if (state.sessionStatus != DesktopStatus.RUNNING) {
            desktopSessionManager.updateAvailableScreenSpace(estimatedCanvasWidthPx, estimatedCanvasHeightPx)
        }
    }

    val currentOrientation = configuration.orientation
    var lastOrientation by remember { mutableStateOf(currentOrientation) }
    var showOrientationRestartDialog by remember { mutableStateOf(false) }
    var pendingOrientationResolution by remember { mutableStateOf<DesktopResolution?>(null) }

    LaunchedEffect(currentOrientation) {
        if (lastOrientation != currentOrientation) {
            lastOrientation = currentOrientation

            if (state.sessionStatus == DesktopStatus.RUNNING && state.resolution.isAutomatic) {
                val newW = estimatedCanvasWidthPx
                val newH = estimatedCanvasHeightPx
                val newRes = DesktopResolution(
                    width = ((newW / 2) * 2).coerceIn(320, 3840),
                    height = ((newH / 2) * 2).coerceIn(320, 3840),
                    label = "Automatic (${newW}x${newH})",
                    isAutomatic = true
                )
                pendingOrientationResolution = newRes
                showOrientationRestartDialog = true
            }
        }
    }

    BackHandler(enabled = isRunning) {
        desktopSessionManager.stopDesktopSession()
    }

    LaunchedEffect(containers, defaultContainerId) {
        if (state.activeContainerId == null && containers.isNotEmpty()) {
            val target = containers.find { it.id == defaultContainerId } ?: containers.first()
            desktopSessionManager.updateSelectedContainer(target.id)
        }
    }

    if (containers.isEmpty()) {
        NoContainersView(onNavigateToWizard = onNavigateToWizard, modifier = modifier)
    } else if (state.sessionStatus == DesktopStatus.RUNNING || state.sessionStatus == DesktopStatus.STARTING_SERVER) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                .onPreviewKeyEvent { keyEvent ->
                    if (keyEvent.type == KeyEventType.KeyDown) {
                        val keysym = VncKeyMapper.androidKeyCodeToKeysym(keyEvent.nativeKeyEvent.keyCode)
                        if (keysym != null) {
                            desktopSessionManager.sendSpecialKey(keysym)
                            return@onPreviewKeyEvent true
                        }
                    }
                    false
                }
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                DesktopTopBar(
                    state = state,
                    effectiveResolution = desktopSessionManager.getEffectiveResolution(state.resolution),
                    onToggleKeyboard = {
                        showKeyboard = !showKeyboard
                        if (showKeyboard) {
                            focusRequester.requestFocus()
                            keyboardController?.show()
                        } else {
                            keyboardController?.hide()
                        }
                    },
                    onToggleTouchMode = {
                        val nextMode = if (state.touchMode == DesktopTouchMode.DIRECT_TOUCH) {
                            DesktopTouchMode.TRACKPAD
                        } else {
                            DesktopTouchMode.DIRECT_TOUCH
                        }
                        desktopSessionManager.setTouchMode(nextMode)
                    },
                    onToggleFitToScreen = { desktopSessionManager.toggleFitToScreen() },
                    onChangeResolution = { res ->
                        desktopSessionManager.setResolution(res)
                        desktopSessionManager.restartDesktopSession()
                    },
                    onChangeUiScale = { scale ->
                        desktopSessionManager.setUiScalePercent(scale)
                    },
                    onSwitchUser = { user -> desktopSessionManager.restartDesktopSessionWithUser(user) },
                    onRestartSession = { desktopSessionManager.restartDesktopSession() },
                    onStopSession = { desktopSessionManager.stopDesktopSession() }
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .onSizeChanged { size ->
                            desktopSessionManager.updateAvailableScreenSpace(size.width, size.height)
                        }
                ) {
                    DesktopCanvasView(
                        vncClient = desktopSessionManager.vncClient,
                        touchMode = state.touchMode,
                        isFitToScreen = state.isFitToScreen
                    )

                    if (state.sessionStatus == DesktopStatus.STARTING_SERVER) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                LoaProgressRing(
                                    progress = null,
                                    size = 48.dp,
                                    strokeWidth = 6.dp
                                )
                                Text(
                                    text = "Starting TigerVNC display server (:1)...",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Initializing XFCE desktop environment...",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }

                DesktopQuickKeysRibbon(
                    state = state,
                    onToggleCtrl = { desktopSessionManager.toggleCtrl() },
                    onToggleAlt = { desktopSessionManager.toggleAlt() },
                    onToggleShift = { desktopSessionManager.toggleShift() },
                    onSendKey = { keysym -> desktopSessionManager.sendSpecialKey(keysym) }
                )
            }

            BasicTextField(
                value = "",
                onValueChange = { newText ->
                    if (newText.isNotEmpty()) {
                        desktopSessionManager.sendTextString(newText)
                    }
                },
                modifier = Modifier
                    .size(1.dp)
                    .alpha(0.001f)
                    .focusRequester(focusRequester)
                    .onKeyEvent { keyEvent ->
                        if (keyEvent.type == KeyEventType.KeyDown) {
                            val keysym = VncKeyMapper.androidKeyCodeToKeysym(keyEvent.nativeKeyEvent.keyCode)
                            if (keysym != null) {
                                desktopSessionManager.sendSpecialKey(keysym)
                                return@onKeyEvent true
                            }
                        }
                        false
                    }
            )
        }
    } else {
        val currentContainer = containers.find { it.id == state.activeContainerId } ?: containers.first()
        val isInstalled = desktopSessionManager.isDesktopInstalled(currentContainer.id)

        if (state.sessionStatus == DesktopStatus.INSTALLING_PACKAGES) {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 640.dp)
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    LoaHeroContainer {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            LoaProgressRing(
                                progress = null,
                                size = 36.dp,
                                strokeWidth = 4.dp
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Installing Desktop Packages",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = state.packageInstallProgress ?: "Setting up XFCE 4 and VNC server...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    minLines = 2,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp),
                            strokeCap = StrokeCap.Round
                        )
                    }

                    val logLines = remember(state.packageInstallLogs) {
                        val raw = state.packageInstallLogs.ifBlank { "Initializing package manager..." }
                        raw.trimEnd('\n').split('\n')
                    }
                    val logListState = rememberLazyListState()
                    LaunchedEffect(logLines.size) {
                        if (logLines.isNotEmpty()) {
                            logListState.scrollToItem(logLines.lastIndex)
                        }
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        shape = MaterialTheme.shapes.medium,
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        LazyColumn(
                            state = logListState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(12.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            items(logLines) { line ->
                                Text(
                                    text = line,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    style = LoaMonoTextStyle
                                )
                            }
                        }
                    }
                }
            }
        } else {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 640.dp)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    ContainerPickerSection(
                        containers = containers,
                        selectedContainerId = currentContainer.id,
                        onSelectContainer = { id -> desktopSessionManager.updateSelectedContainer(id) }
                    )

                    if (!isInstalled) {
                        DesktopSetupSection(
                            containerName = currentContainer.name,
                            onInstallClick = { desktopSessionManager.installDesktopPackages(currentContainer.id) }
                        )
                    } else {
                        DesktopLauncherSection(
                            state = state,
                            container = currentContainer,
                            effectiveResolution = desktopSessionManager.getEffectiveResolution(state.resolution),
                            onResolutionChange = { res -> desktopSessionManager.setResolution(res) },
                            onUiScaleChange = { scale -> desktopSessionManager.setUiScalePercent(scale) },
                            onTouchModeChange = { mode -> desktopSessionManager.setTouchMode(mode) },
                            onSelectUser = { user -> desktopSessionManager.setSelectedUser(user) },
                            onStartDesktop = { desktopSessionManager.startDesktopSession(currentContainer.id) },
                            onUpgradeDesktop = {
                                desktopSessionManager.installDesktopPackages(
                                    currentContainer.id,
                                    autoStartSessionAfter = false
                                )
                            }
                        )
                    }

                    if (state.sessionStatus == DesktopStatus.ERROR && state.errorMessage != null) {
                        Surface(
                            shape = LoaSegmentedSingleShape,
                            color = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                            tonalElevation = 0.dp,
                            shadowElevation = 0.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ErrorOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = state.errorMessage ?: "Unknown error",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.widthIn(max = 560.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showOrientationRestartDialog && pendingOrientationResolution != null) {
        val newRes = pendingOrientationResolution!!
        val orientationName = if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) "landscape" else "portrait"
        AlertDialog(
            onDismissRequest = { showOrientationRestartDialog = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            iconContentColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            icon = { Icon(Icons.Rounded.ScreenRotation, contentDescription = null) },
            title = {
                Text(
                    text = "Restart for $orientationName?",
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            text = {
                Text(
                    text = "The device was rotated to $orientationName mode. Would you like to restart the desktop session with the new available screen space (${newRes.width}x${newRes.height})?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showOrientationRestartDialog = false
                        desktopSessionManager.restartDesktopSessionWithResolution(newRes)
                    }
                ) {
                    Text("Restart Session")
                }
            },
            dismissButton = {
                TextButton(onClick = { showOrientationRestartDialog = false }) {
                    Text("Keep Current")
                }
            }
        )
    }
}

@Composable
private fun NoContainersView(
    onNavigateToWizard: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.widthIn(max = 480.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.DesktopWindows,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "No Linux Container Installed",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            Text(
                text = "A Linux rootfs container must be installed before starting a desktop session. Launch the setup wizard to install Debian, Ubuntu, or your preferred distro.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 560.dp)
            )
            Button(
                onClick = onNavigateToWizard,
                modifier = Modifier
                    .widthIn(max = 320.dp)
                    .fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Open Installation Wizard")
            }
        }
    }
}

@Composable
private fun ContainerPickerSection(
    containers: List<ContainerInstance>,
    selectedContainerId: String,
    onSelectContainer: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedContainer = containers.find { it.id == selectedContainerId } ?: containers.firstOrNull()

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LoaSectionHeader(title = "Target Distribution")

        LoaSegmentedGroup {
            row(
                title = selectedContainer?.name ?: "Unknown",
                subtitle = selectedContainer?.distroName,
                leadingIcon = Icons.Rounded.DesktopWindows,
                trailingContent = if (containers.size > 1) {
                    {
                        Box {
                            FilledTonalButton(onClick = { expanded = true }) {
                                Text("Switch")
                                Icon(
                                    imageVector = Icons.Rounded.ArrowDropDown,
                                    contentDescription = null
                                )
                            }
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                containers.forEach { c ->
                                    DropdownMenuItem(
                                        text = { Text(c.name) },
                                        onClick = {
                                            onSelectContainer(c.id)
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                } else null
            )
        }
    }
}

@Composable
private fun DesktopSetupSection(
    containerName: String,
    onInstallClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        LoaHeroContainer {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DesktopWindows,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "XFCE 4 Desktop Environment",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Not installed in $containerName",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = "To access a full graphical desktop on your phone, XFCE 4 and the TigerVNC display server must be installed inside your container.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.widthIn(max = 560.dp)
            )

            Button(
                onClick = onInstallClick,
                modifier = Modifier
                    .widthIn(max = 320.dp)
                    .fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Rounded.Download,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Install XFCE Desktop")
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            LoaSectionHeader(title = "Included Capabilities")

            LoaSegmentedGroup {
                row(
                    title = "Lightweight & Speedy",
                    subtitle = "Native VNC rendering with zero browser overhead and ultra-low latency.",
                    leadingIcon = Icons.Rounded.Speed
                )
                row(
                    title = "Thunar File Manager & Apps",
                    subtitle = "Browse Linux files, run graphical text editors, utilities, and tools.",
                    leadingIcon = Icons.Rounded.Folder
                )
                row(
                    title = "Touch & Trackpad Controls",
                    subtitle = "Intuitive mobile gestures, pinch-to-zoom, right-click, and quick keys.",
                    leadingIcon = Icons.Rounded.TouchApp
                )
            }
        }
    }
}

@Composable
private fun DesktopLauncherSection(
    state: DesktopUiState,
    container: ContainerInstance,
    effectiveResolution: DesktopResolution,
    onResolutionChange: (DesktopResolution) -> Unit,
    onUiScaleChange: (Int) -> Unit = {},
    onTouchModeChange: (DesktopTouchMode) -> Unit = {},
    onSelectUser: (String) -> Unit = {},
    onStartDesktop: () -> Unit,
    onUpgradeDesktop: (() -> Unit)? = null
) {
    var showResMenu by remember { mutableStateOf(false) }

    val scaleSuffix = if (state.resolution.isAutomatic && state.uiScalePercent != 100) " @ ${state.uiScalePercent}%" else ""
    val displayLabel = if (state.resolution.isAutomatic) {
        "Automatic (${effectiveResolution.width}x${effectiveResolution.height}$scaleSuffix)"
    } else {
        state.resolution.label
    }

    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        LoaHeroContainer {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "XFCE 4 Desktop Ready",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Ready to start session in ${container.name}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                LoaStatusBadge(label = "Ready", tone = LoaStatusTone.Running)
            }

            if (state.hasUpgradeAvailable && onUpgradeDesktop != null) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = MaterialTheme.shapes.medium,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Upgrade,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Desktop Upgrade Available",
                                style = MaterialTheme.typography.labelLarge
                            )
                            Text(
                                text = "Includes graphical app installer and desktop improvements.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        TextButton(onClick = onUpgradeDesktop) {
                            Text("Upgrade")
                        }
                    }
                }
            }

            Button(
                onClick = onStartDesktop,
                modifier = Modifier
                    .widthIn(max = 320.dp)
                    .fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start Desktop Session")
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            LoaSectionHeader(title = "Session Configuration")

            LoaSegmentedGroup {
                // Resolution selection
                item { shape ->
                    Surface(
                        shape = shape,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Desktop Resolution",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                LoaMonospaceChip(text = displayLabel)
                            }

                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = { showResMenu = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(imageVector = Icons.Rounded.AspectRatio, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Change Resolution")
                                    Spacer(modifier = Modifier.weight(1f))
                                    Icon(imageVector = Icons.Rounded.ArrowDropDown, contentDescription = null)
                                }
                                DropdownMenu(
                                    expanded = showResMenu,
                                    onDismissRequest = { showResMenu = false }
                                ) {
                                    DesktopResolution.DEFAULT_PRESETS.forEach { res ->
                                        val itemLabel = if (res.isAutomatic) {
                                            val presetScaleSuffix = if (state.uiScalePercent != 100) " @ ${state.uiScalePercent}%" else ""
                                            "Automatic (${effectiveResolution.width}x${effectiveResolution.height}$presetScaleSuffix) - Screen Space"
                                        } else {
                                            res.label
                                        }
                                        DropdownMenuItem(
                                            text = { Text(itemLabel) },
                                            onClick = {
                                                onResolutionChange(res)
                                                showResMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // UI Scale selection (when Automatic)
                if (state.resolution.isAutomatic) {
                    item { shape ->
                        Surface(
                            shape = shape,
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            tonalElevation = 0.dp,
                            shadowElevation = 0.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "UI Size / Display Scale",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    LoaMonospaceChip(
                                        text = "${state.uiScalePercent}% (${if (state.uiScalePercent == 100) "Native" else "Larger UI"})"
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    DesktopResolution.SCALE_PRESETS.forEach { scale ->
                                        FilterChip(
                                            selected = state.uiScalePercent == scale,
                                            onClick = { onUiScaleChange(scale) },
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

                // Control Mode selection
                item { shape ->
                    Surface(
                        shape = shape,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Control Mode",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                LoaMonospaceChip(
                                    text = if (state.touchMode == DesktopTouchMode.TRACKPAD) "Trackpad pointer" else "Direct touch"
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                DesktopTouchMode.entries.forEach { mode ->
                                    val isDefault = mode == DesktopTouchMode.TRACKPAD
                                    val labelText = if (isDefault) "${mode.label} (Default)" else mode.label
                                    FilterChip(
                                        selected = state.touchMode == mode,
                                        onClick = { onTouchModeChange(mode) },
                                        label = {
                                            Text(
                                                text = labelText,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        },
                                        leadingIcon = if (state.touchMode == mode) {
                                            {
                                                Icon(
                                                    imageVector = if (mode == DesktopTouchMode.TRACKPAD) {
                                                        Icons.Rounded.Mouse
                                                    } else {
                                                        Icons.Rounded.TouchApp
                                                    },
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        } else null,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }

                // Login User selection
                if (state.availableUsers.isNotEmpty()) {
                    item { shape ->
                        val defaultUser = container.defaultUser.ifBlank {
                            state.availableUsers.firstOrNull { it != "root" } ?: "root"
                        }
                        Surface(
                            shape = shape,
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            tonalElevation = 0.dp,
                            shadowElevation = 0.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Login User",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    LoaMonospaceChip(text = state.selectedUser)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    state.availableUsers.forEach { user ->
                                        val isDefault = user == defaultUser
                                        val labelText = if (isDefault) "$user (Default)" else user
                                        FilterChip(
                                            selected = state.selectedUser == user,
                                            onClick = { onSelectUser(user) },
                                            label = {
                                                Text(
                                                    text = labelText,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            },
                                            leadingIcon = if (state.selectedUser == user) {
                                                {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Person,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            } else null,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
