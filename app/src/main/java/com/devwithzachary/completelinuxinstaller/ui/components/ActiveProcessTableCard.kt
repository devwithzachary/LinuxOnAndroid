package com.devwithzachary.completelinuxinstaller.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.devwithzachary.completelinuxinstaller.engine.ContainerProcessInfo
import com.devwithzachary.completelinuxinstaller.ui.designsystem.LoaSectionHeader
import com.devwithzachary.completelinuxinstaller.ui.designsystem.LoaStatusBadge
import com.devwithzachary.completelinuxinstaller.ui.designsystem.LoaStatusTone
import com.devwithzachary.completelinuxinstaller.ui.theme.LoaMonoTextStyle
import com.devwithzachary.completelinuxinstaller.ui.theme.LoaSegmentedSingleShape

@Composable
fun ActiveProcessTableCard(
    processes: List<ContainerProcessInfo>,
    onKillProcess: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var processToKill by remember { mutableStateOf<ContainerProcessInfo?>(null) }

    val filteredProcesses = remember(processes, searchQuery) {
        if (searchQuery.isBlank()) {
            processes
        } else {
            processes.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                    it.pid.toString().contains(searchQuery) ||
                    it.cmdline.contains(searchQuery, ignoreCase = true) ||
                    it.user.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LoaSectionHeader(
                title = "Active Processes",
                modifier = Modifier.weight(1f)
            )
            LoaStatusBadge(
                label = "${processes.size} active",
                tone = if (processes.isNotEmpty()) LoaStatusTone.Running else LoaStatusTone.Stopped
            )
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = LoaSegmentedSingleShape,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.onSurface,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "Filter processes (sshd, vnc, nginx)...",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Clear process filter"
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PID",
                        style = LoaMonoTextStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "PROCESS",
                        style = LoaMonoTextStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(2.2f)
                    )
                    Text(
                        text = "RAM",
                        style = LoaMonoTextStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End,
                        modifier = Modifier
                            .weight(1.1f)
                            .padding(end = 8.dp)
                    )
                    Box(
                        modifier = Modifier.width(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "KILL",
                            style = LoaMonoTextStyle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                if (filteredProcesses.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp, horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = if (searchQuery.isNotEmpty()) {
                                "No processes match '$searchQuery'"
                            } else {
                                "No container child processes active"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        val visibleProcesses = filteredProcesses.take(15)
                        visibleProcesses.forEachIndexed { index, proc ->
                            ProcessRowItem(
                                proc = proc,
                                onKillClick = { processToKill = proc }
                            )
                            if (index < visibleProcesses.lastIndex) {
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                            }
                        }
                        if (filteredProcesses.size > 15) {
                            Text(
                                text = "+ ${filteredProcesses.size - 15} more processes",
                                style = LoaMonoTextStyle,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp, start = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    processToKill?.let { proc ->
        AlertDialog(
            onDismissRequest = { processToKill = null },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text(text = "Kill Process?")
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Terminate ${proc.name} with SIGKILL (kill -9)?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "PID ${proc.pid} | ${proc.user} | ${proc.rssMb} MB",
                        style = LoaMonoTextStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onKillProcess(proc.pid)
                        processToKill = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Text("Kill Process")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { processToKill = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ProcessRowItem(
    proc: ContainerProcessInfo,
    onKillClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "${proc.pid}",
                style = LoaMonoTextStyle,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = proc.user,
                style = MaterialTheme.typography.labelSmall,
                color = if (proc.user == "root") {
                    MaterialTheme.colorScheme.tertiary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Column(
            modifier = Modifier.weight(2.2f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = proc.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = proc.cmdline,
                style = LoaMonoTextStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Text(
            text = "${proc.rssMb} MB",
            style = LoaMonoTextStyle,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1.1f)
                .padding(end = 8.dp)
        )

        Box(
            modifier = Modifier.width(48.dp),
            contentAlignment = Alignment.Center
        ) {
            IconButton(onClick = onKillClick) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Kill ${proc.name} PID ${proc.pid}",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
