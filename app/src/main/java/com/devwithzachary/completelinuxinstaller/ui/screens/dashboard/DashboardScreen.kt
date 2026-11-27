package com.devwithzachary.completelinuxinstaller.ui.screens.dashboard

import android.content.res.Configuration
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.DesktopWindows
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.devwithzachary.completelinuxinstaller.R
import com.devwithzachary.completelinuxinstaller.engine.SystemResourceMetrics
import com.devwithzachary.completelinuxinstaller.model.ContainerInstance
import com.devwithzachary.completelinuxinstaller.ui.DashboardUiState
import com.devwithzachary.completelinuxinstaller.ui.components.DashboardGaugesCard
import com.devwithzachary.completelinuxinstaller.ui.components.PatreonBanner
import com.devwithzachary.completelinuxinstaller.ui.designsystem.LoaHeroContainer
import com.devwithzachary.completelinuxinstaller.ui.designsystem.LoaMonospaceChip
import com.devwithzachary.completelinuxinstaller.ui.designsystem.LoaSectionHeader
import com.devwithzachary.completelinuxinstaller.ui.designsystem.LoaSegmentedGroup
import com.devwithzachary.completelinuxinstaller.ui.designsystem.LoaStatusBadge
import com.devwithzachary.completelinuxinstaller.ui.designsystem.LoaStatusTone
import com.devwithzachary.completelinuxinstaller.ui.screens.container.ContainerDetailTab
import com.devwithzachary.completelinuxinstaller.ui.theme.LinuxOnAndroidTheme
import com.devwithzachary.completelinuxinstaller.ui.theme.LoaMonoTextStyle
import com.devwithzachary.completelinuxinstaller.ui.util.handHover
import com.devwithzachary.completelinuxinstaller.ui.util.onContextMenu
import com.devwithzachary.completelinuxinstaller.ui.util.rememberWindowSizeClass

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    state: DashboardUiState,
    metrics: SystemResourceMetrics = SystemResourceMetrics(),
    containerMetrics: Map<String, SystemResourceMetrics> = emptyMap(),
    onInstallClick: () -> Unit,
    onOpenTerminalClick: () -> Unit,
    onOpenContainerTerminalClick: (String) -> Unit = {},
    onOpenDesktopClick: () -> Unit = {},
    onOpenContainerDesktopClick: (String) -> Unit = {},
    isContainerDesktopSetup: (String) -> Boolean = { false },
    onContainerClick: (String, ContainerDetailTab) -> Unit = { _, _ -> },
    onSetDefaultContainerClick: (String) -> Unit = {}
) {
    val windowSizeClass = rememberWindowSizeClass()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_title),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                actions = {
                    if (state.isInstalled && state.containers.isNotEmpty()) {
                        IconButton(
                            onClick = onInstallClick,
                            modifier = Modifier.handHover()
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = "Install Distribution"
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        if (windowSizeClass.isExpanded && state.isInstalled && state.containers.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.TopCenter
            ) {
                Row(
                    modifier = Modifier
                        .widthIn(max = 1200.dp)
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1.15f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ContainersListSection(
                            state = state,
                            metrics = metrics,
                            containerMetrics = containerMetrics,
                            isContainerDesktopSetup = isContainerDesktopSetup,
                            onInstallClick = onInstallClick,
                            onContainerClick = onContainerClick,
                            onOpenContainerTerminalClick = onOpenContainerTerminalClick,
                            onOpenContainerDesktopClick = onOpenContainerDesktopClick,
                            onSetDefaultContainerClick = onSetDefaultContainerClick
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(0.85f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        DashboardHeroSection(
                            state = state,
                            metrics = metrics,
                            containerMetrics = containerMetrics,
                            isContainerDesktopSetup = isContainerDesktopSetup,
                            onInstallClick = onInstallClick,
                            onOpenTerminalClick = onOpenTerminalClick,
                            onOpenDesktopClick = onOpenDesktopClick
                        )

                        DashboardGaugesCard(metrics = metrics)

                        PatreonBanner()
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState()),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 640.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    DashboardHeroSection(
                        state = state,
                        metrics = metrics,
                        containerMetrics = containerMetrics,
                        isContainerDesktopSetup = isContainerDesktopSetup,
                        onInstallClick = onInstallClick,
                        onOpenTerminalClick = onOpenTerminalClick,
                        onOpenDesktopClick = onOpenDesktopClick
                    )

                    if (state.isInstalled && state.containers.isNotEmpty()) {
                        DashboardGaugesCard(metrics = metrics)

                        ContainersListSection(
                            state = state,
                            metrics = metrics,
                            containerMetrics = containerMetrics,
                            isContainerDesktopSetup = isContainerDesktopSetup,
                            onInstallClick = onInstallClick,
                            onContainerClick = onContainerClick,
                            onOpenContainerTerminalClick = onOpenContainerTerminalClick,
                            onOpenContainerDesktopClick = onOpenContainerDesktopClick,
                            onSetDefaultContainerClick = onSetDefaultContainerClick
                        )
                    }

                    PatreonBanner()
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DashboardHeroSection(
    state: DashboardUiState,
    metrics: SystemResourceMetrics,
    containerMetrics: Map<String, SystemResourceMetrics>,
    isContainerDesktopSetup: (String) -> Boolean,
    onInstallClick: () -> Unit,
    onOpenTerminalClick: () -> Unit,
    onOpenDesktopClick: () -> Unit
) {
    val defaultContainer = state.containers.find { it.id == state.defaultContainerId }
        ?: state.containers.firstOrNull()

    if (!state.isInstalled || defaultContainer == null) {
        LoaHeroContainer {
            LoaStatusBadge(
                label = "Not Installed",
                tone = LoaStatusTone.Stopped
            )

            Text(
                text = "No Linux environment installed",
                style = MaterialTheme.typography.headlineSmall
            )

            Text(
                text = stringResource(R.string.app_tagline_hero),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = onInstallClick,
                modifier = Modifier
                    .widthIn(max = 320.dp)
                    .fillMaxWidth()
                    .handHover()
            ) {
                Icon(
                    imageVector = Icons.Rounded.Download,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Install a Linux Distribution")
            }
        }
        return
    }

    val activeMetrics = containerMetrics[defaultContainer.id]
    val isRunning = state.isRunning
    val ramMb = when {
        activeMetrics != null && activeMetrics.processes.isNotEmpty() -> activeMetrics.containerMemoryUsedMb
        isRunning -> metrics.containerMemoryUsedMb
        else -> 0L
    }
    val procCount = when {
        activeMetrics != null -> activeMetrics.processes.size
        isRunning -> metrics.processes.size
        else -> 0
    }
    val hasDesktop = isContainerDesktopSetup(defaultContainer.id)
    val archLabel = remember { resolveHostArchLabel() }
    val heroSubtitle = remember(defaultContainer.name, defaultContainer.distroName, archLabel) {
        formatContainerSubtitle(defaultContainer, isHero = true, archLabel = archLabel)
    }

    LoaHeroContainer {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = defaultContainer.name,
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = heroSubtitle,
                    style = LoaMonoTextStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            LoaStatusBadge(
                label = if (isRunning) "Running" else "Ready",
                tone = if (isRunning) LoaStatusTone.Running else LoaStatusTone.Stopped
            )
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LoaMonospaceChip(text = "$ramMb MB RAM")
            LoaMonospaceChip(text = "${formatStorageSize(defaultContainer.storageUsedMb)} disk")
            LoaMonospaceChip(text = if (procCount > 0) "$procCount proc" else "idle")
        }

        if (hasDesktop) {
            Row(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onOpenTerminalClick,
                    modifier = Modifier
                        .weight(1f)
                        .handHover(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Terminal,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Terminal",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                FilledTonalButton(
                    onClick = onOpenDesktopClick,
                    modifier = Modifier
                        .weight(1f)
                        .handHover(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DesktopWindows,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Desktop",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        } else {
            Button(
                onClick = onOpenTerminalClick,
                modifier = Modifier
                    .widthIn(max = 320.dp)
                    .fillMaxWidth()
                    .handHover(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Terminal,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Open Terminal",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ContainersListSection(
    state: DashboardUiState,
    metrics: SystemResourceMetrics,
    containerMetrics: Map<String, SystemResourceMetrics>,
    isContainerDesktopSetup: (String) -> Boolean,
    onInstallClick: () -> Unit,
    onContainerClick: (String, ContainerDetailTab) -> Unit,
    onOpenContainerTerminalClick: (String) -> Unit,
    onOpenContainerDesktopClick: (String) -> Unit,
    onSetDefaultContainerClick: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LoaSectionHeader(
            title = "Containers (${state.containers.size})",
            actionLabel = "Add",
            onActionClick = onInstallClick
        )

        LoaSegmentedGroup {
            state.containers.forEach { container ->
                val isDefault = container.id == state.defaultContainerId
                val isRunning = state.isRunning && (isDefault || container.isDefault)
                item { shape ->
                    ContainerSegmentedRow(
                        container = container,
                        shape = shape,
                        isDefault = isDefault,
                        isRunning = isRunning,
                        cMetrics = containerMetrics[container.id],
                        fallbackMetrics = metrics,
                        isDesktopSetup = isContainerDesktopSetup(container.id),
                        onContainerClick = onContainerClick,
                        onOpenContainerTerminalClick = onOpenContainerTerminalClick,
                        onOpenContainerDesktopClick = onOpenContainerDesktopClick,
                        onSetDefaultContainerClick = onSetDefaultContainerClick
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ContainerSegmentedRow(
    container: ContainerInstance,
    shape: Shape,
    isDefault: Boolean,
    isRunning: Boolean,
    cMetrics: SystemResourceMetrics?,
    fallbackMetrics: SystemResourceMetrics,
    isDesktopSetup: Boolean,
    onContainerClick: (String, ContainerDetailTab) -> Unit,
    onOpenContainerTerminalClick: (String) -> Unit,
    onOpenContainerDesktopClick: (String) -> Unit,
    onSetDefaultContainerClick: (String) -> Unit
) {
    var showContextMenu by remember { mutableStateOf(false) }
    val archLabel = remember { resolveHostArchLabel() }
    val rowSubtitle = remember(container.name, container.distroName, archLabel) {
        formatContainerSubtitle(container, isHero = false, archLabel = archLabel)
    }

    val ramText = when {
        cMetrics != null && cMetrics.processes.isNotEmpty() -> "${cMetrics.containerMemoryUsedMb} MB"
        cMetrics == null && isRunning -> "${fallbackMetrics.containerMemoryUsedMb} MB"
        else -> "0 MB"
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        Surface(
            onClick = { onContainerClick(container.id, ContainerDetailTab.OVERVIEW) },
            modifier = Modifier
                .fillMaxWidth()
                .handHover()
                .onContextMenu { showContextMenu = true },
            shape = shape,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.onSurface,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            itemVerticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = container.name,
                                style = MaterialTheme.typography.titleMedium
                            )
                            if (isDefault) {
                                LoaMonospaceChip(
                                    text = "default",
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        Text(
                            text = rowSubtitle,
                            style = LoaMonoTextStyle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = { showContextMenu = true },
                        modifier = Modifier.handHover()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = "Options for ${container.name}"
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FlowRow(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LoaStatusBadge(
                            label = if (isRunning) "Running" else "Stopped",
                            tone = if (isRunning) LoaStatusTone.Running else LoaStatusTone.Stopped
                        )
                        LoaMonospaceChip(text = ramText)
                        LoaMonospaceChip(text = formatStorageSize(container.storageUsedMb))
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        FilledTonalIconButton(
                            onClick = { onOpenContainerTerminalClick(container.id) },
                            modifier = Modifier.handHover()
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Terminal,
                                contentDescription = "Open terminal for ${container.name}"
                            )
                        }

                        if (isDesktopSetup) {
                            FilledTonalIconButton(
                                onClick = { onOpenContainerDesktopClick(container.id) },
                                modifier = Modifier.handHover()
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DesktopWindows,
                                    contentDescription = "Open desktop for ${container.name}"
                                )
                            }
                        }
                    }
                }
            }
        }

        DropdownMenu(
            expanded = showContextMenu,
            onDismissRequest = { showContextMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("Open Terminal") },
                onClick = {
                    showContextMenu = false
                    onOpenContainerTerminalClick(container.id)
                },
                leadingIcon = {
                    Icon(Icons.Rounded.Terminal, contentDescription = null)
                }
            )
            if (isDesktopSetup) {
                DropdownMenuItem(
                    text = { Text("Open Desktop") },
                    onClick = {
                        showContextMenu = false
                        onOpenContainerDesktopClick(container.id)
                    },
                    leadingIcon = {
                        Icon(Icons.Rounded.DesktopWindows, contentDescription = null)
                    }
                )
            }
            DropdownMenuItem(
                text = { Text("Overview & Metrics") },
                onClick = {
                    showContextMenu = false
                    onContainerClick(container.id, ContainerDetailTab.OVERVIEW)
                },
                leadingIcon = {
                    Icon(Icons.Rounded.Analytics, contentDescription = null)
                }
            )
            DropdownMenuItem(
                text = { Text("Software Hub") },
                onClick = {
                    showContextMenu = false
                    onContainerClick(container.id, ContainerDetailTab.SOFTWARE)
                },
                leadingIcon = {
                    Icon(Icons.Rounded.Apps, contentDescription = null)
                }
            )
            DropdownMenuItem(
                text = { Text("Container Settings") },
                onClick = {
                    showContextMenu = false
                    onContainerClick(container.id, ContainerDetailTab.SETTINGS)
                },
                leadingIcon = {
                    Icon(Icons.Rounded.Settings, contentDescription = null)
                }
            )
            if (!isDefault) {
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text("Set as Default Container") },
                    onClick = {
                        showContextMenu = false
                        onSetDefaultContainerClick(container.id)
                    },
                    leadingIcon = {
                        Icon(Icons.Rounded.Star, contentDescription = null)
                    }
                )
            }
        }
    }
}

private fun resolveHostArchLabel(): String {
    val primaryAbi = Build.SUPPORTED_ABIS?.firstOrNull()?.lowercase().orEmpty()
    return when {
        primaryAbi.contains("arm64") || primaryAbi.contains("aarch64") -> "aarch64"
        primaryAbi.contains("x86_64") || primaryAbi.contains("amd64") -> "x86_64"
        primaryAbi.contains("armeabi") || primaryAbi.contains("armv7") -> "armv7l"
        primaryAbi.contains("x86") -> "x86"
        else -> "aarch64"
    }
}

private fun formatContainerSubtitle(
    container: ContainerInstance,
    isHero: Boolean,
    archLabel: String
): String {
    val name = container.name.trim()
    val distro = container.distroName.trim()
    return if (distro.isEmpty() || name.equals(distro, ignoreCase = true)) {
        if (isHero) "Default container • $archLabel • PRoot" else "$archLabel • PRoot"
    } else {
        val hasArchInDistro = distro.contains("arm64", ignoreCase = true) ||
            distro.contains("aarch64", ignoreCase = true) ||
            distro.contains("x86_64", ignoreCase = true) ||
            distro.contains("armv7", ignoreCase = true)
        if (hasArchInDistro) {
            "$distro • PRoot"
        } else {
            "$distro • $archLabel • PRoot"
        }
    }
}

private fun formatStorageSize(mb: Long): String {
    return if (mb >= 1024L) {
        String.format(java.util.Locale.US, "%.1f GB", mb / 1024.0)
    } else {
        "$mb MB"
    }
}

private val previewContainers = listOf(
    ContainerInstance(
        id = "fedora_default",
        name = "Fedora 45",
        distroId = "fedora",
        distroName = "Fedora 45",
        rootDirPath = "/data/user/0/com.devwithzachary.completelinuxinstaller/files/fedora",
        isDefault = true,
        storageUsedMb = 1480L
    ),
    ContainerInstance(
        id = "debian_dev",
        name = "Debian 13",
        distroId = "debian",
        distroName = "Debian 13",
        rootDirPath = "/data/user/0/com.devwithzachary.completelinuxinstaller/files/debian",
        isDefault = false,
        storageUsedMb = 640L
    )
)

@Preview(
    name = "Dashboard Empty - Light",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Composable
private fun DashboardEmptyLightPreview() {
    LinuxOnAndroidTheme(darkTheme = false, dynamicColor = false) {
        DashboardScreen(
            state = DashboardUiState(
                isInitializing = false,
                isInstalled = false,
                containers = emptyList()
            ),
            onInstallClick = {},
            onOpenTerminalClick = {}
        )
    }
}

@Preview(
    name = "Dashboard Empty - Dark",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun DashboardEmptyDarkPreview() {
    LinuxOnAndroidTheme(darkTheme = true, dynamicColor = false) {
        DashboardScreen(
            state = DashboardUiState(
                isInitializing = false,
                isInstalled = false,
                containers = emptyList()
            ),
            onInstallClick = {},
            onOpenTerminalClick = {}
        )
    }
}

@Preview(
    name = "Dashboard Ready - Light",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Composable
private fun DashboardReadyLightPreview() {
    LinuxOnAndroidTheme(darkTheme = false, dynamicColor = false) {
        DashboardScreen(
            state = DashboardUiState(
                isInitializing = false,
                isInstalled = true,
                isRunning = false,
                containers = previewContainers,
                defaultContainerId = "ubuntu_default"
            ),
            metrics = SystemResourceMetrics(
                isSessionRunning = false,
                storageUsedMb = 1480L,
                storageTotalBytes = 128L * 1024 * 1024 * 1024,
                storageAvailableBytes = 42L * 1024 * 1024 * 1024
            ),
            isContainerDesktopSetup = { it == "ubuntu_default" },
            onInstallClick = {},
            onOpenTerminalClick = {}
        )
    }
}

@Preview(
    name = "Dashboard Ready - Dark",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun DashboardReadyDarkPreview() {
    LinuxOnAndroidTheme(darkTheme = true, dynamicColor = false) {
        DashboardScreen(
            state = DashboardUiState(
                isInitializing = false,
                isInstalled = true,
                isRunning = false,
                containers = previewContainers,
                defaultContainerId = "ubuntu_default"
            ),
            metrics = SystemResourceMetrics(
                isSessionRunning = false,
                storageUsedMb = 1480L,
                storageTotalBytes = 128L * 1024 * 1024 * 1024,
                storageAvailableBytes = 42L * 1024 * 1024 * 1024
            ),
            isContainerDesktopSetup = { it == "ubuntu_default" },
            onInstallClick = {},
            onOpenTerminalClick = {}
        )
    }
}

@Preview(
    name = "Dashboard Running - Light",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Composable
private fun DashboardRunningLightPreview() {
    LinuxOnAndroidTheme(darkTheme = false, dynamicColor = false) {
        DashboardScreen(
            state = DashboardUiState(
                isInitializing = false,
                isInstalled = true,
                isRunning = true,
                containers = previewContainers,
                defaultContainerId = "ubuntu_default"
            ),
            metrics = SystemResourceMetrics(
                isSessionRunning = true,
                containerMemoryUsedMb = 512L,
                systemTotalRamMb = 8192L,
                systemAvailableRamMb = 4792L,
                storageUsedMb = 1480L,
                storageTotalBytes = 128L * 1024 * 1024 * 1024,
                storageAvailableBytes = 42L * 1024 * 1024 * 1024
            ),
            isContainerDesktopSetup = { it == "ubuntu_default" },
            onInstallClick = {},
            onOpenTerminalClick = {}
        )
    }
}

@Preview(
    name = "Dashboard Running - Dark",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun DashboardRunningDarkPreview() {
    LinuxOnAndroidTheme(darkTheme = true, dynamicColor = false) {
        DashboardScreen(
            state = DashboardUiState(
                isInitializing = false,
                isInstalled = true,
                isRunning = true,
                containers = previewContainers,
                defaultContainerId = "ubuntu_default"
            ),
            metrics = SystemResourceMetrics(
                isSessionRunning = true,
                containerMemoryUsedMb = 512L,
                systemTotalRamMb = 8192L,
                systemAvailableRamMb = 4792L,
                storageUsedMb = 1480L,
                storageTotalBytes = 128L * 1024 * 1024 * 1024,
                storageAvailableBytes = 42L * 1024 * 1024 * 1024
            ),
            isContainerDesktopSetup = { it == "ubuntu_default" },
            onInstallClick = {},
            onOpenTerminalClick = {}
        )
    }
}
