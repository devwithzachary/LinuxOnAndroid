package com.devwithzachary.completelinuxinstaller.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devwithzachary.completelinuxinstaller.R
import com.devwithzachary.completelinuxinstaller.engine.SystemResourceMetrics
import com.devwithzachary.completelinuxinstaller.model.ContainerInstance
import com.devwithzachary.completelinuxinstaller.ui.DashboardUiState
import com.devwithzachary.completelinuxinstaller.ui.components.DashboardGaugesCard
import com.devwithzachary.completelinuxinstaller.ui.components.PatreonBanner
import com.devwithzachary.completelinuxinstaller.ui.screens.container.ContainerDetailTab
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
    onContainerClick: (String, ContainerDetailTab) -> Unit = { _, _ -> },
    onSetDefaultContainerClick: (String) -> Unit = {}
) {
    val windowSizeClass = rememberWindowSizeClass()

    if (windowSizeClass.isMediumOrExpanded && state.isInstalled && state.containers.isNotEmpty()) {
        // Large Screen & Desktop 2-Column Responsive Dashboard Layout
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Left Column: Containers List & Management
            Column(
                modifier = Modifier
                    .weight(1.15f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Section Header: Installed RootFS Containers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Widgets,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Installed RootFS Containers",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "${state.containers.size} installed",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // List of Container Cards with Right-Click Context Menu Support
                state.containers.forEach { container ->
                    val isDefault = container.id == state.defaultContainerId
                    val isRunning = state.isRunning && (isDefault || container.isDefault)
                    ContainerCard(
                        container = container,
                        isDefault = isDefault,
                        isRunning = isRunning,
                        cMetrics = containerMetrics[container.id],
                        fallbackMetrics = metrics,
                        onContainerClick = onContainerClick,
                        onOpenContainerTerminalClick = onOpenContainerTerminalClick,
                        onSetDefaultContainerClick = onSetDefaultContainerClick
                    )
                }

                // Add More Container Button
                OutlinedButton(
                    onClick = onInstallClick,
                    modifier = Modifier.fillMaxWidth().handHover(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.AddCircleOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Install Another Linux Distribution")
                }
            }

            // Right Column: Hero Banner, Live Gauges, Quick Terminal Launcher & Community
            Column(
                modifier = Modifier
                    .weight(0.85f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Hero Header Card
                HeroHeaderCard()

                // Quick Terminal Launcher Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Terminal,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Terminal Session",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Surface(
                                shape = CircleShape,
                                color = if (state.isRunning) Color(0xFF4CAF50).copy(alpha = 0.15f) else MaterialTheme.colorScheme.outlineVariant
                            ) {
                                Text(
                                    text = if (state.isRunning) "RUNNING" else "STOPPED",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (state.isRunning) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Text(
                            text = "Press Ctrl+Alt+T or click below to launch the PRoot terminal console.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = onOpenTerminalClick,
                            modifier = Modifier.fillMaxWidth().handHover(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Open Terminal (Ctrl+Alt+T)")
                        }
                    }
                }

                // Live System Resource Gauges Card
                DashboardGaugesCard(metrics = metrics)

                // Patreon Community Banner
                PatreonBanner()
            }
        }
    } else {
        // Compact Phone Layout (or when no container is installed yet)
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PatreonBanner()

            // Hero Header Card
            HeroHeaderCard()

            if (!state.isInstalled || state.containers.isEmpty()) {
                // Setup Required Action Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = "No Linux distribution rootfs installed yet.",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        Button(
                            onClick = onInstallClick,
                            modifier = Modifier.fillMaxWidth().handHover(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Install a Linux Distribution")
                        }
                    }
                }
            } else {
                // Section Header: Installed RootFS Containers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Widgets,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Installed RootFS Containers",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                // List of Container Cards
                state.containers.forEach { container ->
                    val isDefault = container.id == state.defaultContainerId
                    val isRunning = state.isRunning && (isDefault || container.isDefault)
                    ContainerCard(
                        container = container,
                        isDefault = isDefault,
                        isRunning = isRunning,
                        cMetrics = containerMetrics[container.id],
                        fallbackMetrics = metrics,
                        onContainerClick = onContainerClick,
                        onOpenContainerTerminalClick = onOpenContainerTerminalClick,
                        onSetDefaultContainerClick = onSetDefaultContainerClick
                    )
                }

                // Bottom Add More Container Button
                OutlinedButton(
                    onClick = onInstallClick,
                    modifier = Modifier.fillMaxWidth().handHover(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.AddCircleOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Install Another Linux Distribution")
                }
            }
        }
    }
}

@Composable
private fun HeroHeaderCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.app_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = stringResource(R.string.app_tagline_hero),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun ContainerCard(
    container: ContainerInstance,
    isDefault: Boolean,
    isRunning: Boolean,
    cMetrics: SystemResourceMetrics?,
    fallbackMetrics: SystemResourceMetrics,
    onContainerClick: (String, ContainerDetailTab) -> Unit,
    onOpenContainerTerminalClick: (String) -> Unit,
    onSetDefaultContainerClick: (String) -> Unit
) {
    var showContextMenu by remember { mutableStateOf(false) }

    Box {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .handHover()
                .clickable { onContainerClick(container.id, ContainerDetailTab.OVERVIEW) }
                .onContextMenu { showContextMenu = true },
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Container Title & Badges
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .background(Color(container.colorHex), shape = CircleShape)
                        )
                        Column {
                            Text(
                                text = container.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = container.distroName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isDefault) {
                            Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                Text(
                                    text = "DEFAULT",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        IconButton(
                            onClick = { onContainerClick(container.id, ContainerDetailTab.SETTINGS) },
                            modifier = Modifier.size(26.dp).handHover()
                        ) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = "Container Settings",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

                val ramText = if (cMetrics != null) {
                    if (cMetrics.processes.isNotEmpty()) "${cMetrics.containerMemoryUsedMb} MB" else "Idle (0 MB)"
                } else if (isRunning) {
                    "${fallbackMetrics.containerMemoryUsedMb} MB"
                } else {
                    "Idle (0 MB)"
                }
                val procText = if (cMetrics != null) {
                    if (cMetrics.processes.isNotEmpty()) "${cMetrics.processes.size} active" else "Stopped"
                } else if (isRunning) {
                    "${fallbackMetrics.processes.size} active"
                } else {
                    "Stopped"
                }

                // Resource Usage Overview Grid (RAM, Storage, CPU/Processes, Status)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ContainerMetricItem(
                        label = "RAM Used",
                        value = ramText,
                        icon = Icons.Default.Memory,
                        modifier = Modifier.weight(1f)
                    )

                    ContainerMetricItem(
                        label = "Storage",
                        value = "${container.storageUsedMb} MB",
                        icon = Icons.Default.Storage,
                        modifier = Modifier.weight(1f)
                    )

                    ContainerMetricItem(
                        label = "Processes",
                        value = procText,
                        icon = Icons.Default.Speed,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Card Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = { onOpenContainerTerminalClick(container.id) },
                        modifier = Modifier.weight(1f).handHover(),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Terminal", fontSize = 12.sp)
                    }

                    FilledTonalButton(
                        onClick = { onContainerClick(container.id, ContainerDetailTab.OVERVIEW) },
                        modifier = Modifier.weight(1f).handHover(),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Analytics, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Overview", fontSize = 12.sp)
                    }

                    FilledTonalButton(
                        onClick = { onContainerClick(container.id, ContainerDetailTab.SOFTWARE) },
                        modifier = Modifier.weight(1f).handHover(),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Apps, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Software", fontSize = 12.sp)
                    }
                }
            }
        }

        // Desktop Mouse Right-Click & Long-Press Context Menu
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
                leadingIcon = { Icon(Icons.Default.Terminal, contentDescription = null) }
            )
            DropdownMenuItem(
                text = { Text("Overview & Metrics") },
                onClick = {
                    showContextMenu = false
                    onContainerClick(container.id, ContainerDetailTab.OVERVIEW)
                },
                leadingIcon = { Icon(Icons.Default.Analytics, contentDescription = null) }
            )
            DropdownMenuItem(
                text = { Text("Software Hub") },
                onClick = {
                    showContextMenu = false
                    onContainerClick(container.id, ContainerDetailTab.SOFTWARE)
                },
                leadingIcon = { Icon(Icons.Default.Apps, contentDescription = null) }
            )
            DropdownMenuItem(
                text = { Text("Container Settings") },
                onClick = {
                    showContextMenu = false
                    onContainerClick(container.id, ContainerDetailTab.SETTINGS)
                },
                leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) }
            )
            if (!isDefault) {
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text("Set as Default Container") },
                    onClick = {
                        showContextMenu = false
                        onSetDefaultContainerClick(container.id)
                    },
                    leadingIcon = { Icon(Icons.Default.Star, contentDescription = null) }
                )
            }
        }
    }
}

@Composable
private fun ContainerMetricItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )
    }
}
