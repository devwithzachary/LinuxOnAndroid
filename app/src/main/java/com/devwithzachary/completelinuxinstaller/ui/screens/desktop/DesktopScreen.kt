package com.devwithzachary.completelinuxinstaller.ui.screens.desktop

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devwithzachary.completelinuxinstaller.R
import com.devwithzachary.completelinuxinstaller.model.ContainerInstance
import com.devwithzachary.completelinuxinstaller.engine.desktop.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesktopScreen(
    desktopSessionManager: DesktopSessionManager,
    containers: List<ContainerInstance>,
    defaultContainerId: String?,
    onNavigateToWizard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
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

    // Keep estimated screen space updated when orientation/configuration changes
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

            // Ask to restart VNC with new available space only if running AND using Automatic resolution
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

    // Intercept back button when desktop is running: prompt or stop
    BackHandler(enabled = isRunning) {
        desktopSessionManager.stopDesktopSession()
    }

    // Set initial container if none selected
    LaunchedEffect(containers, defaultContainerId) {
        if (state.activeContainerId == null && containers.isNotEmpty()) {
            val target = containers.find { it.id == defaultContainerId } ?: containers.first()
            desktopSessionManager.updateSelectedContainer(target.id)
        }
    }

    if (containers.isEmpty()) {
        NoContainersView(onNavigateToWizard = onNavigateToWizard, modifier = modifier)
    } else if (state.sessionStatus == DesktopStatus.RUNNING || state.sessionStatus == DesktopStatus.STARTING_SERVER) {
        // Active Desktop Viewport
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFF0F172A))
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
                // Top control bar
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
                        val nextMode = if (state.touchMode == DesktopTouchMode.DIRECT_TOUCH)
                            DesktopTouchMode.TRACKPAD
                        else
                            DesktopTouchMode.DIRECT_TOUCH
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

                // Desktop Canvas
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
                                .background(Color(0xFF0F172A).copy(alpha = 0.85f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Starting TigerVNC display server (:1)...",
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Initializing XFCE desktop environment...",
                                    color = Color(0xFF94A3B8),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }

                // Quick Keys Bar
                DesktopQuickKeysRibbon(
                    state = state,
                    onToggleCtrl = { desktopSessionManager.toggleCtrl() },
                    onToggleAlt = { desktopSessionManager.toggleAlt() },
                    onToggleShift = { desktopSessionManager.toggleShift() },
                    onSendKey = { keysym -> desktopSessionManager.sendSpecialKey(keysym) }
                )
            }

            // Hidden BasicTextField to receive soft keyboard inputs
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
        // Desktop Launcher / Setup Screen
        val currentContainer = containers.find { it.id == state.activeContainerId } ?: containers.first()
        val isInstalled = desktopSessionManager.isDesktopInstalled(currentContainer.id)

        if (state.sessionStatus == DesktopStatus.INSTALLING_PACKAGES) {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Installing Desktop Packages",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
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

                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }

                // Full terminal output taking all available space below the header and above bottom tab bar
                val logScrollState = rememberScrollState()
                LaunchedEffect(state.packageInstallLogs) {
                    logScrollState.scrollTo(logScrollState.maxValue)
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                        .verticalScroll(logScrollState)
                ) {
                    Text(
                        text = state.packageInstallLogs.ifBlank { "Initializing package manager..." },
                        color = Color(0xFFE2E8F0),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }
        } else {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Container Selector Card
                ContainerPickerCard(
                    containers = containers,
                    selectedContainerId = currentContainer.id,
                    onSelectContainer = { id -> desktopSessionManager.updateSelectedContainer(id) }
                )

                if (!isInstalled) {
                    // Packages not installed -> Setup Banner
                    DesktopSetupCard(
                        containerName = currentContainer.name,
                        onInstallClick = { desktopSessionManager.installDesktopPackages(currentContainer.id) }
                    )
                } else {
                    // Packages installed -> Ready to launch
                    DesktopLauncherCard(
                        state = state,
                        container = currentContainer,
                        effectiveResolution = desktopSessionManager.getEffectiveResolution(state.resolution),
                        onResolutionChange = { res -> desktopSessionManager.setResolution(res) },
                        onUiScaleChange = { scale -> desktopSessionManager.setUiScalePercent(scale) },
                        onTouchModeChange = { mode -> desktopSessionManager.setTouchMode(mode) },
                        onSelectUser = { user -> desktopSessionManager.setSelectedUser(user) },
                        onStartDesktop = { desktopSessionManager.startDesktopSession(currentContainer.id) },
                        onUpgradeDesktop = { desktopSessionManager.installDesktopPackages(currentContainer.id, autoStartSessionAfter = false) }
                    )
                }

                if (state.sessionStatus == DesktopStatus.ERROR && state.errorMessage != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = state.errorMessage ?: "Unknown error",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium
                            )
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
            icon = { Icon(Icons.Default.ScreenRotation, contentDescription = null) },
            title = { Text("Restart for $orientationName?") },
            text = {
                Text(
                    "The device was rotated to $orientationName mode. Would you like to restart the desktop session with the new available screen space (${newRes.width}x${newRes.height})?"
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.DesktopWindows,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "No Linux Container Installed",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "A Linux rootfs container must be installed before starting a desktop session. Launch the setup wizard to install Debian, Ubuntu, or your preferred distro.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onNavigateToWizard,
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Open Installation Wizard")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContainerPickerCard(
    containers: List<ContainerInstance>,
    selectedContainerId: String,
    onSelectContainer: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedContainer = containers.find { it.id == selectedContainerId } ?: containers.firstOrNull()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Target Distribution",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = selectedContainer?.name ?: "Unknown",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (containers.size > 1) {
                Box {
                    OutlinedButton(onClick = { expanded = true }) {
                        Text("Switch")
                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
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
        }
    }
}

@Composable
private fun DesktopSetupCard(
    containerName: String,
    onInstallClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DesktopWindows,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "XFCE 4 Desktop Environment",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
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
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            FeatureItem(
                icon = Icons.Default.Speed,
                title = "Lightweight & Speedy",
                desc = "Native VNC rendering with zero browser overhead and ultra-low latency."
            )
            FeatureItem(
                icon = Icons.Default.Folder,
                title = "Thunar File Manager & Apps",
                desc = "Browse Linux files, run graphical text editors, utilities, and tools."
            )
            FeatureItem(
                icon = Icons.Default.TouchApp,
                title = "Touch & Trackpad Controls",
                desc = "Intuitive mobile gestures, pinch-to-zoom, right-click, and quick keys."
            )

            Button(
                onClick = onInstallClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(imageVector = Icons.Default.Download, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Install XFCE Desktop", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun DesktopLauncherCard(
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

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color(0xFF10B981).copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "XFCE 4 Desktop Ready",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Ready to start session in ${container.name}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (state.hasUpgradeAvailable && onUpgradeDesktop != null) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Upgrade,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Desktop Upgrade Available",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Includes graphical app installer and desktop improvements.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                        TextButton(onClick = onUpgradeDesktop) {
                            Text("Upgrade", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            HorizontalDivider()

            // Resolution selection
            val scaleSuffix = if (state.resolution.isAutomatic && state.uiScalePercent != 100) " @ ${state.uiScalePercent}%" else ""
            val displayLabel = if (state.resolution.isAutomatic) {
                "Automatic (${effectiveResolution.width}x${effectiveResolution.height}$scaleSuffix)"
            } else {
                state.resolution.label
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Desktop Resolution",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = displayLabel,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { showResMenu = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.AspectRatio, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Change Resolution")
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
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

            if (state.resolution.isAutomatic) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "UI Size / Display Scale",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${state.uiScalePercent}% (${if (state.uiScalePercent == 100) "Native" else "Larger UI"})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        DesktopResolution.SCALE_PRESETS.forEach { scale ->
                            FilterChip(
                                selected = state.uiScalePercent == scale,
                                onClick = { onUiScaleChange(scale) },
                                label = { Text("${scale}%", fontSize = 11.sp, maxLines = 1) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            HorizontalDivider()

            // Control Mode selection
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Control Mode",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (state.touchMode == DesktopTouchMode.TRACKPAD) "Trackpad pointer" else "Direct touch",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
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
                            label = { Text(labelText, maxLines = 1, fontSize = 12.sp) },
                            leadingIcon = if (state.touchMode == mode) {
                                {
                                    Icon(
                                        imageVector = if (mode == DesktopTouchMode.TRACKPAD) Icons.Default.Mouse else Icons.Default.TouchApp,
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

            // Login User selection
            if (state.availableUsers.isNotEmpty()) {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val defaultUser = container.defaultUser.ifBlank { state.availableUsers.firstOrNull { it != "root" } ?: "root" }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Login User",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = state.selectedUser,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
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
                                label = { Text(labelText, maxLines = 1, fontSize = 12.sp) },
                                leadingIcon = if (state.selectedUser == user) {
                                    {
                                        Icon(
                                            imageVector = Icons.Default.Person,
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

            Button(
                onClick = onStartDesktop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Start Desktop Session",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
private fun FeatureItem(
    icon: ImageVector,
    title: String,
    desc: String
) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
