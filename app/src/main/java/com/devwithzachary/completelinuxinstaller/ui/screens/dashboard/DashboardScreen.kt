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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devwithzachary.completelinuxinstaller.ui.DashboardUiState

import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

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
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val prefs = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }
    var isBannerDismissed by remember { mutableStateOf(prefs.getBoolean("patreon_banner_dismissed", false)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (!isBannerDismissed) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Support on Patreon",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall
                            )
                        }

                        IconButton(
                            onClick = {
                                prefs.edit().putBoolean("patreon_banner_dismissed", true).apply()
                                isBannerDismissed = true
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss Banner",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Text(
                        text = "Love Complete Linux Installer? Join my Patreon from $1/month to support ongoing development!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )

                    OutlinedButton(
                        onClick = {
                            try {
                                uriHandler.openUri("https://www.patreon.com/cw/DevWithZachary/membership")
                            } catch (_: Exception) {}
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Join my Patreon ($1/mo)")
                    }
                }
            }
        }
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
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "System Environment Status",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Distribution:", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = state.distroName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Storage Allocated:", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "${state.storageUsedMb} MB",
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    LinearProgressIndicator(
                        progress = { (state.storageUsedMb.toFloat() / 2048f).coerceIn(0.05f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("SDCard Bound:", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = if (state.bindSdCard) "Yes (/sdcard)" else "Disabled",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (state.bindSdCard) Color(0xFF4CAF50) else Color.Gray
                        )
                    }
                }
            }

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
