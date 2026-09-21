package com.devwithzachary.completelinuxinstaller.ui.screens.container.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devwithzachary.completelinuxinstaller.engine.SystemResourceMetrics
import com.devwithzachary.completelinuxinstaller.model.ContainerInstance
import com.devwithzachary.completelinuxinstaller.model.SoftwarePackage
import com.devwithzachary.completelinuxinstaller.ui.components.ActiveProcessTableCard
import com.devwithzachary.completelinuxinstaller.ui.components.DashboardGaugesCard
import com.devwithzachary.completelinuxinstaller.ui.components.NetworkListenerCard
import com.devwithzachary.completelinuxinstaller.ui.util.handHover
import com.devwithzachary.completelinuxinstaller.ui.util.rememberWindowSizeClass

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
    onKillProcess: (pid: Int) -> Unit,
    onRunPresetCommand: (command: String) -> Unit,
    onNavigateToSoftwareTab: () -> Unit,
    onRefreshMetrics: () -> Unit = {}
) {
    val scrollState = rememberScrollState()
    val windowSizeClass = rememberWindowSizeClass()
    var servicePrompt by remember { mutableStateOf<Pair<String, String>?>(null) }

    if (windowSizeClass.isExpanded) {
        // 2-Column Responsive Overview for Tablets & Desktop Mode
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Left Column: Gauges, Services Launcher, Listening Ports
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Live System Resources & Gauges Card
                DashboardGaugesCard(
                    metrics = metrics.copy(storageUsedMb = container.storageUsedMb)
                )

                // 2. One-Touch Container Services
                ServicesCard(
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

                // 3. Open Listening Ports Card
                NetworkListenerCard(
                    ports = metrics.listeningPorts,
                    onRefresh = onRefreshMetrics
                )
            }

            // Right Column: Live Active Container Processes Table
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
            }
        }
    } else {
        // Compact Single Column for Phones
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Live System Resources & Gauges Card
            DashboardGaugesCard(
                metrics = metrics.copy(storageUsedMb = container.storageUsedMb)
            )

            // 2. One-Touch Container Services
            ServicesCard(
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

            // 3. Live Active Container Processes Table (`ps aux`)
            ActiveProcessTableCard(
                processes = metrics.processes,
                onKillProcess = onKillProcess
            )

            // 4. Open Listening Ports Card
            NetworkListenerCard(
                ports = metrics.listeningPorts,
                onRefresh = onRefreshMetrics
            )
        }
    }

    if (servicePrompt != null) {
        val (serviceTitle, _) = servicePrompt!!
        AlertDialog(
            onDismissRequest = { servicePrompt = null },
            icon = { Icon(Icons.Default.Download, contentDescription = null) },
            title = { Text("Service Not Installed") },
            text = {
                Text("$serviceTitle is not installed in ${container.name}.\n\nWould you like to open the Software tab to install it?")
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

@Composable
fun ServicesCard(
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.RocketLaunch,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Container Services & Launchers",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "Launch background servers and graphical desktop sessions inside this rootfs container.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Row 1: Classic Daemons & Desktop GUI
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // VNC Launcher
                ServiceLauncherButton(
                    icon = Icons.Default.DesktopWindows,
                    label = "VNC",
                    isInstalled = isVncInstalled,
                    modifier = Modifier.weight(1f).handHover(),
                    onClick = {
                        if (isVncInstalled) {
                            val vncPkg = SoftwarePackage.getPresets().find { it.id == "xfce_desktop" }
                            vncPkg?.launchCommand?.let { onRunPresetCommand(it) }
                        } else {
                            onPromptService("TigerVNC & XFCE Desktop", "xfce_desktop")
                        }
                    }
                )

                // NGINX Launcher
                ServiceLauncherButton(
                    icon = Icons.Default.Public,
                    label = "NGINX",
                    isInstalled = isNginxInstalled,
                    modifier = Modifier.weight(1f).handHover(),
                    onClick = {
                        if (isNginxInstalled) {
                            val nginxPkg = SoftwarePackage.getPresets().find { it.id == "nginx_web" }
                            nginxPkg?.launchCommand?.let { onRunPresetCommand(it) }
                        } else {
                            onPromptService("NGINX Web Server", "nginx_web")
                        }
                    }
                )

                // SSH Launcher
                ServiceLauncherButton(
                    icon = Icons.Default.VpnKey,
                    label = "SSH",
                    isInstalled = isSshInstalled,
                    modifier = Modifier.weight(1f).handHover(),
                    onClick = {
                        if (isSshInstalled) {
                            val sshPkg = SoftwarePackage.getPresets(sshPort).find { it.id == "openssh_server" }
                            sshPkg?.launchCommand?.let { onRunPresetCommand(it) }
                        } else {
                            onPromptService("OpenSSH Server", "openssh_server")
                        }
                    }
                )
            }

            // Row 2: Browser Workspaces
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // VS Code Launcher
                ServiceLauncherButton(
                    icon = Icons.Default.Code,
                    label = "VS Code",
                    isInstalled = isCodeServerInstalled,
                    modifier = Modifier.weight(1f).handHover(),
                    onClick = {
                        if (isCodeServerInstalled) {
                            val codePkg = SoftwarePackage.getPresets().find { it.id == "code_server" }
                            codePkg?.launchCommand?.let { onRunPresetCommand(it) }
                        } else {
                            onPromptService("VS Code Server (code-server)", "code_server")
                        }
                    }
                )

                // Web Terminal Launcher
                ServiceLauncherButton(
                    icon = Icons.Default.Terminal,
                    label = "Web Terminal",
                    isInstalled = isWebTerminalInstalled,
                    modifier = Modifier.weight(1f).handHover(),
                    onClick = {
                        if (isWebTerminalInstalled) {
                            val ttydPkg = SoftwarePackage.getPresets().find { it.id == "web_terminal" }
                            ttydPkg?.launchCommand?.let { onRunPresetCommand(it) }
                        } else {
                            onPromptService("Browser Web Terminal (ttyd)", "web_terminal")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun ServiceLauncherButton(
    icon: ImageVector,
    label: String,
    isInstalled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    if (isInstalled) {
        FilledTonalButton(
            onClick = onClick,
            modifier = modifier,
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
            shape = RoundedCornerShape(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = label,
                    fontSize = 12.sp,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier,
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
            shape = RoundedCornerShape(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = label,
                    fontSize = 12.sp,
                    maxLines = 1,
                    softWrap = false,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}
