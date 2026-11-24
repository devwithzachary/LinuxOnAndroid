package com.devwithzachary.completelinuxinstaller.ui.screens.wizard.steps

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.devwithzachary.completelinuxinstaller.R
import com.devwithzachary.completelinuxinstaller.engine.DownloadState
import com.devwithzachary.completelinuxinstaller.model.DistroDefinition
import com.devwithzachary.completelinuxinstaller.ui.designsystem.LoaHeroContainer
import com.devwithzachary.completelinuxinstaller.ui.designsystem.LoaProgressRing
import com.devwithzachary.completelinuxinstaller.ui.designsystem.LoaSegmentedGroup
import com.devwithzachary.completelinuxinstaller.ui.designsystem.LoaStatusBadge
import com.devwithzachary.completelinuxinstaller.ui.designsystem.LoaStatusTone
import com.devwithzachary.completelinuxinstaller.ui.theme.LoaMonoTextStyle
import com.devwithzachary.completelinuxinstaller.ui.theme.LoaSegmentedSingleShape

@Composable
fun WizardInstallStep(
    selectedDistro: DistroDefinition,
    downloadState: DownloadState,
    onShowLogs: () -> Unit,
    onRetry: () -> Unit,
    onSelectDifferentDistro: () -> Unit = {},
    onCancel: (() -> Unit)? = null,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    var logsExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        when (downloadState) {
            is DownloadState.Success -> {
                LoaHeroContainer {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        LoaStatusBadge(
                            label = "Installed",
                            tone = LoaStatusTone.Running,
                            icon = Icons.Rounded.CheckCircle
                        )

                        LoaProgressRing(
                            progress = 1f,
                            size = 96.dp,
                            strokeWidth = 10.dp,
                            indicatorColor = MaterialTheme.colorScheme.secondary
                        ) {
                            Text(
                                text = "100%",
                                style = LoaMonoTextStyle
                            )
                        }

                        Text(
                            text = "${selectedDistro.name} is Ready",
                            style = MaterialTheme.typography.headlineSmall,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "Rootfs extraction and account setup completed. Launch a terminal session from the Dashboard.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                if (downloadState.logs.isNotEmpty()) {
                    CollapsibleLogsSection(
                        logs = downloadState.logs,
                        expanded = logsExpanded,
                        onToggle = { logsExpanded = !logsExpanded },
                        onOpenFullLogs = onShowLogs
                    )
                }
            }

            is DownloadState.Downloading -> {
                LoaHeroContainer {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        LoaStatusBadge(
                            label = "Downloading",
                            tone = LoaStatusTone.Downloading
                        )

                        LoaProgressRing(
                            progress = (downloadState.progressPercent / 100f).coerceIn(0f, 1f),
                            size = 96.dp,
                            strokeWidth = 10.dp
                        ) {
                            Text(
                                text = "${downloadState.progressPercent}%",
                                style = LoaMonoTextStyle
                            )
                        }

                        Text(
                            text = "Downloading ${selectedDistro.name}",
                            style = MaterialTheme.typography.headlineSmall,
                            textAlign = TextAlign.Center
                        )

                        val downloadedMb = downloadState.bytesDownloaded / (1024 * 1024)
                        val totalMb = downloadState.totalBytes / (1024 * 1024)
                        Text(
                            text = "$downloadedMb MB / ~$totalMb MB",
                            style = LoaMonoTextStyle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            is DownloadState.Extracting -> {
                LoaHeroContainer {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        LoaStatusBadge(
                            label = "Configuring",
                            tone = LoaStatusTone.Downloading
                        )

                        LoaProgressRing(
                            progress = null,
                            size = 96.dp,
                            strokeWidth = 10.dp
                        )

                        Text(
                            text = "Setting Up ${selectedDistro.name}",
                            style = MaterialTheme.typography.headlineSmall,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = downloadState.message,
                            style = LoaMonoTextStyle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                if (downloadState.logs.isNotEmpty()) {
                    CollapsibleLogsSection(
                        logs = downloadState.logs,
                        expanded = logsExpanded,
                        onToggle = { logsExpanded = !logsExpanded },
                        onOpenFullLogs = onShowLogs
                    )
                }
            }

            is DownloadState.Error -> {
                LoaHeroContainer {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        LoaStatusBadge(
                            label = "Failed",
                            tone = LoaStatusTone.Error,
                            icon = Icons.Rounded.Warning
                        )

                        Text(
                            text = "Installation Failed",
                            style = MaterialTheme.typography.headlineSmall,
                            textAlign = TextAlign.Center
                        )

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = LoaSegmentedSingleShape,
                            color = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                            tonalElevation = 0.dp,
                            shadowElevation = 0.dp
                        ) {
                            Text(
                                text = downloadState.message,
                                style = LoaMonoTextStyle,
                                modifier = Modifier.padding(16.dp)
                            )
                        }

                        if (downloadState.logs.isNotEmpty()) {
                            OutlinedButton(onClick = onShowLogs) {
                                Icon(
                                    imageVector = Icons.Rounded.Terminal,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.btn_view_failure_logs))
                            }
                        }

                        if (onCancel != null) {
                            TextButton(onClick = onCancel) {
                                Text("Cancel & Return to Dashboard")
                            }
                        }
                    }
                }
            }

            is DownloadState.Idle -> {
                LoaHeroContainer {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        LoaProgressRing(
                            progress = null,
                            size = 80.dp,
                            strokeWidth = 8.dp
                        )
                        Text(
                            text = "Initializing setup...",
                            style = LoaMonoTextStyle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CollapsibleLogsSection(
    logs: List<String>,
    expanded: Boolean,
    onToggle: () -> Unit,
    onOpenFullLogs: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LoaSegmentedGroup {
            row(
                title = "Live Setup Logs",
                subtitle = "${logs.size} lines captured",
                leadingIcon = Icons.Rounded.Terminal,
                trailingContent = {
                    Icon(
                        imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = if (expanded) "Collapse logs" else "Expand logs",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                onClick = onToggle
            )
        }

        AnimatedVisibility(visible = expanded) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = LoaSegmentedSingleShape,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                contentColor = MaterialTheme.colorScheme.onSurface,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        logs.takeLast(25).forEach { line ->
                            Text(
                                text = line,
                                style = LoaMonoTextStyle,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onOpenFullLogs) {
                            Text("Open Full Logs")
                        }
                    }
                }
            }
        }
    }
}
