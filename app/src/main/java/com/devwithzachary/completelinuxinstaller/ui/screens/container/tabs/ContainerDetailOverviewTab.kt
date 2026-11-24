package com.devwithzachary.completelinuxinstaller.ui.screens.container.tabs

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
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DesktopWindows
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.devwithzachary.completelinuxinstaller.engine.SystemResourceMetrics
import com.devwithzachary.completelinuxinstaller.model.ContainerInstance
import com.devwithzachary.completelinuxinstaller.model.SoftwarePackage
import com.devwithzachary.completelinuxinstaller.ui.components.ActiveProcessTableCard
import com.devwithzachary.completelinuxinstaller.ui.components.DashboardGaugesCard
import com.devwithzachary.completelinuxinstaller.ui.components.NetworkListenerCard
import com.devwithzachary.completelinuxinstaller.ui.designsystem.LoaHeroContainer
import com.devwithzachary.completelinuxinstaller.ui.designsystem.LoaMonospaceChip
import com.devwithzachary.completelinuxinstaller.ui.designsystem.LoaSectionHeader
import com.devwithzachary.completelinuxinstaller.ui.designsystem.LoaSegmentedGroup
import com.devwithzachary.completelinuxinstaller.ui.designsystem.LoaSegmentedRow
import com.devwithzachary.completelinuxinstaller.ui.designsystem.LoaStatusBadge
import com.devwithzachary.completelinuxinstaller.ui.designsystem.LoaStatusTone
import com.devwithzachary.completelinuxinstaller.ui.util.handHover
import com.devwithzachary.completelinuxinstaller.ui.util.rememberWindowSizeClass
import java.util.Locale

@Composable
fun OverviewTabContent(
    container: ContainerInstance,
    metrics: SystemResourceMetrics,
    sshPort: Int,
    isVncInstalled: Boolean,
    isNginxInstalled: Boolean,
    isSshInstalled: Boolean,
    isCodeServerInstalled: Boolean,
    isWebTerminalInstalled: Boolean,
    onOpenTerminal: () -> Unit = {},
    onKillProcess: (pid: Int) -> Unit,
    onRunPresetCommand: (command: String) -> Unit,
    onNavigateToSoftwareTab: () -> Unit,
    onRefreshMetrics: () -> Unit = {}
) {
    val windowSizeClass = rememberWindowSizeClass()
    var servicePrompt by remember { mutableStateOf<Pair<String, String>?>(null) }

    if (windowSizeClass.isExpanded) {
        Box(
            modifier = Modifier.fillMaxSize(),
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
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OverviewHeroSection(
                        container = container,
                        metrics = metrics,
                        onOpenTerminal = onOpenTerminal
                    )

                    DashboardGaugesCard(
                        metrics = metrics.copy(storageUsedMb = container.storageUsedMb)
                    )

                    ServicesSegmentedSection(
                        container = container,
                        isVncInstalled = isVncInstalled,
                        isNginxInstalled = isNginxInstalled,
                        isSshInstalled = isSshInstalled,
                        isCodeServerInstalled = isCodeServerInstalled,
                        isWebTerminalInstalled = isWebTerminalInstalled,
                        sshPort = sshPort,
                        onRunPresetCommand = onRunPresetCommand,
                        onPromptService = { title, pkgId -> servicePrompt = Pair(title, pkgId) }
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ActiveProcessTableCard(
                        processes = metrics.processes,
                        onKillProcess = onKillProcess
                    )

                    NetworkListenerCard(
                        ports = metrics.listeningPorts,
                        onRefresh = onRefreshMetrics
                    )
                }
            }
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
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
                OverviewHeroSection(
                    container = container,
                    metrics = metrics,
                    onOpenTerminal = onOpenTerminal
                )

                DashboardGaugesCard(
                    metrics = metrics.copy(storageUsedMb = container.storageUsedMb)
                )

                ServicesSegmentedSection(
                    container = container,
                    isVncInstalled = isVncInstalled,
                    isNginxInstalled = isNginxInstalled,
                    isSshInstalled = isSshInstalled,
                    isCodeServerInstalled = isCodeServerInstalled,
                    isWebTerminalInstalled = isWebTerminalInstalled,
                    sshPort = sshPort,
                    onRunPresetCommand = onRunPresetCommand,
                    onPromptService = { title, pkgId -> servicePrompt = Pair(title, pkgId) }
                )

                ActiveProcessTableCard(
                    processes = metrics.processes,
                    onKillProcess = onKillProcess
                )

                NetworkListenerCard(
                    ports = metrics.listeningPorts,
                    onRefresh = onRefreshMetrics
                )
            }
        }
    }

    servicePrompt?.let { (serviceTitle, _) ->
        AlertDialog(
            onDismissRequest = { servicePrompt = null },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.Download,
                    contentDescription = null
                )
            },
            title = { Text("Service Not Installed") },
            text = {
                Text("$serviceTitle is not installed in ${container.name}. Open the Software tab to install it?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        servicePrompt = null
                        onNavigateToSoftwareTab()
                    }
                ) {
                    Text("Go to Software")
                }
            },
            dismissButton = {
                TextButton(onClick = { servicePrompt = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OverviewHeroSection(
    container: ContainerInstance,
    metrics: SystemResourceMetrics,
    onOpenTerminal: () -> Unit
) {
    val isRunning = metrics.isSessionRunning || metrics.processes.isNotEmpty()

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
                    text = container.name,
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = container.distroName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            LoaStatusBadge(
                label = if (isRunning) "Running" else "Stopped",
                tone = if (isRunning) LoaStatusTone.Running else LoaStatusTone.Stopped
            )
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LoaMonospaceChip(text = "${metrics.containerMemoryUsedMb} MB RAM")
            LoaMonospaceChip(text = "${formatStorageSize(container.storageUsedMb)} disk")
            LoaMonospaceChip(text = if (metrics.processes.isNotEmpty()) "${metrics.processes.size} proc" else "idle")
            LoaMonospaceChip(text = container.defaultUser)
        }

        Button(
            onClick = onOpenTerminal,
            modifier = Modifier
                .widthIn(max = 320.dp)
                .fillMaxWidth()
                .handHover(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
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

private data class ContainerServiceEntry(
    val id: String,
    val title: String,
    val promptTitle: String,
    val endpoint: String,
    val icon: ImageVector,
    val isInstalled: Boolean
)

@Composable
private fun ServicesSegmentedSection(
    container: ContainerInstance,
    isVncInstalled: Boolean,
    isNginxInstalled: Boolean,
    isSshInstalled: Boolean,
    isCodeServerInstalled: Boolean,
    isWebTerminalInstalled: Boolean,
    sshPort: Int,
    onRunPresetCommand: (command: String) -> Unit,
    onPromptService: (title: String, pkgId: String) -> Unit
) {
    val services = remember(
        isVncInstalled,
        isNginxInstalled,
        isSshInstalled,
        isCodeServerInstalled,
        isWebTerminalInstalled,
        sshPort
    ) {
        listOf(
            ContainerServiceEntry(
                id = "xfce_desktop",
                title = "VNC Desktop",
                promptTitle = "TigerVNC & XFCE Desktop",
                endpoint = ":5901",
                icon = Icons.Rounded.DesktopWindows,
                isInstalled = isVncInstalled
            ),
            ContainerServiceEntry(
                id = "nginx_web",
                title = "NGINX Web Server",
                promptTitle = "NGINX Web Server",
                endpoint = ":80",
                icon = Icons.Rounded.Public,
                isInstalled = isNginxInstalled
            ),
            ContainerServiceEntry(
                id = "openssh_server",
                title = "OpenSSH Server",
                promptTitle = "OpenSSH Server",
                endpoint = ":$sshPort",
                icon = Icons.Rounded.VpnKey,
                isInstalled = isSshInstalled
            ),
            ContainerServiceEntry(
                id = "code_server",
                title = "VS Code Server",
                promptTitle = "VS Code Server (code-server)",
                endpoint = ":8080",
                icon = Icons.Rounded.Code,
                isInstalled = isCodeServerInstalled
            ),
            ContainerServiceEntry(
                id = "web_terminal",
                title = "Web Terminal",
                promptTitle = "Browser Web Terminal (ttyd)",
                endpoint = ":7681",
                icon = Icons.Rounded.Terminal,
                isInstalled = isWebTerminalInstalled
            )
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LoaSectionHeader(title = "Services")

        LoaSegmentedGroup {
            services.forEach { service ->
                val triggerLaunch: () -> Unit = {
                    if (service.isInstalled) {
                        val pkg = SoftwarePackage.getPresetsForContainer(container, sshPort)
                            .find { it.id == service.id }
                        pkg?.launchCommand?.let { onRunPresetCommand(it) }
                    } else {
                        onPromptService(service.promptTitle, service.id)
                    }
                }

                item { shape ->
                    LoaSegmentedRow(
                        title = service.title,
                        subtitle = service.endpoint,
                        leadingIcon = service.icon,
                        shape = shape,
                        onClick = triggerLaunch,
                        trailingContent = {
                            if (service.isInstalled) {
                                FilledTonalButton(
                                    onClick = triggerLaunch,
                                    modifier = Modifier.handHover()
                                ) {
                                    Text("Start")
                                }
                            } else {
                                TextButton(
                                    onClick = triggerLaunch,
                                    modifier = Modifier.handHover()
                                ) {
                                    Text("Install")
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

private fun formatStorageSize(mb: Long): String {
    return if (mb >= 1024L) {
        String.format(Locale.US, "%.1f GB", mb / 1024.0)
    } else {
        "$mb MB"
    }
}

@Composable
fun ContainerDetailOverviewTab(
    container: ContainerInstance,
    metrics: SystemResourceMetrics,
    sshPort: Int,
    isVncInstalled: Boolean,
    isNginxInstalled: Boolean,
    isSshInstalled: Boolean,
    isCodeServerInstalled: Boolean,
    isWebTerminalInstalled: Boolean,
    onKillProcess: (pid: Int) -> Unit,
    onRunPresetCommand: (command: String) -> Unit,
    onNavigateToSoftwareTab: () -> Unit,
    onRefreshMetrics: () -> Unit = {}
) = OverviewTabContent(
    container = container,
    metrics = metrics,
    sshPort = sshPort,
    isVncInstalled = isVncInstalled,
    isNginxInstalled = isNginxInstalled,
    isSshInstalled = isSshInstalled,
    isCodeServerInstalled = isCodeServerInstalled,
    isWebTerminalInstalled = isWebTerminalInstalled,
    onKillProcess = onKillProcess,
    onRunPresetCommand = onRunPresetCommand,
    onNavigateToSoftwareTab = onNavigateToSoftwareTab,
    onRefreshMetrics = onRefreshMetrics
)
