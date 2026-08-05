package com.devwithzachary.completelinuxinstaller.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devwithzachary.completelinuxinstaller.ui.DashboardUiState
import com.devwithzachary.completelinuxinstaller.ui.components.PatreonBanner
import com.devwithzachary.completelinuxinstaller.ui.components.SystemStatusCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    state: DashboardUiState,
    onInstallClick: () -> Unit,
    onOpenTerminalClick: () -> Unit,
    onStopSessionClick: () -> Unit,
    onRunPresetClick: (String) -> Unit
) {
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Complete Linux Installer",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Badge(
                        containerColor = if (state.isInstalled) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                    ) {
                        Text(
                            text = if (state.isInstalled) "READY" else "NOT INSTALLED",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                }
                Text(
                    text = "PRoot-based Linux image manager for non-rooted Android devices.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }

        if (!state.isInstalled) {
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
                            text = "Ubuntu 26.04 LTS rootfs is not initialized yet.",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    Button(
                        onClick = onInstallClick,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Install Barebones Ubuntu RootFS")
                    }
                }
            }
        } else {
            // Quick Launcher Actions Grid
            Text(
                text = "Quick Actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onOpenTerminalClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.Terminal, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Terminal")
                }

                if (state.isRunning) {
                    OutlinedButton(
                        onClick = onStopSessionClick,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Stop")
                    }
                } else {
                    Button(
                        onClick = onOpenTerminalClick,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Start Shell")
                    }
                }
            }

            // System Status Card
            SystemStatusCard(state = state)

            // Preset Quick Triggers (Only shown if at least one service package is installed)
            val hasAnyServiceInstalled = state.isVncInstalled || state.isNginxInstalled || state.isSshInstalled

            if (hasAnyServiceInstalled) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "One-Touch Service Launchers",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )

                        if (state.isVncInstalled) {
                            OutlinedButton(
                                onClick = { onRunPresetClick("vncserver :1 -geometry 1280x720") },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Computer, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Start TigerVNC Desktop Server (Port 5901)")
                            }
                        }

                        if (state.isNginxInstalled) {
                            OutlinedButton(
                                onClick = { onRunPresetClick("service nginx start") },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Dns, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Start NGINX HTTP Web Server (Port 80)")
                            }
                        }

                        if (state.isSshInstalled) {
                            OutlinedButton(
                                onClick = { onRunPresetClick("/usr/sbin/sshd -p 2222") },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Security, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Start OpenSSH Server (Port 2222)")
                            }
                        }
                    }
                }
            }
        }
    }
}
