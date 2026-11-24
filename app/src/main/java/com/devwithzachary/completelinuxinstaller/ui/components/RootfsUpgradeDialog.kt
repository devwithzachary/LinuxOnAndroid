package com.devwithzachary.completelinuxinstaller.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Upgrade
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.devwithzachary.completelinuxinstaller.engine.UpgradeState
import com.devwithzachary.completelinuxinstaller.ui.theme.LoaMonoTextStyle

@Composable
fun RootfsUpgradeDialog(
    upgradeState: UpgradeState,
    onDismiss: () -> Unit
) {
    if (upgradeState is UpgradeState.Idle) return

    val isUpgrading = upgradeState is UpgradeState.Upgrading
    val listState = rememberLazyListState()
    val logs = when (upgradeState) {
        is UpgradeState.Upgrading -> upgradeState.logs
        is UpgradeState.Success -> upgradeState.logs
        is UpgradeState.Error -> upgradeState.logs
        is UpgradeState.Idle -> emptyList()
    }

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    AlertDialog(
        onDismissRequest = {
            if (!isUpgrading) {
                onDismiss()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = !isUpgrading,
            dismissOnClickOutside = !isUpgrading
        ),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        icon = {
            Icon(
                imageVector = when (upgradeState) {
                    is UpgradeState.Success -> Icons.Rounded.CheckCircle
                    is UpgradeState.Error -> Icons.Rounded.Error
                    is UpgradeState.Upgrading, is UpgradeState.Idle -> Icons.Rounded.Upgrade
                },
                contentDescription = null,
                tint = when (upgradeState) {
                    is UpgradeState.Success -> MaterialTheme.colorScheme.secondary
                    is UpgradeState.Error -> MaterialTheme.colorScheme.error
                    is UpgradeState.Upgrading, is UpgradeState.Idle -> MaterialTheme.colorScheme.primary
                }
            )
        },
        title = {
            Text(
                text = when (upgradeState) {
                    is UpgradeState.Success -> "RootFS Upgrade Complete"
                    is UpgradeState.Error -> "Upgrade Failed"
                    is UpgradeState.Upgrading -> "Upgrading RootFS Container"
                    is UpgradeState.Idle -> "RootFS Upgrade"
                },
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val subtitle = when (upgradeState) {
                    is UpgradeState.Success -> "All system patches and compatibility fixes applied."
                    is UpgradeState.Error -> upgradeState.message
                    is UpgradeState.Upgrading -> upgradeState.currentStepName
                    is UpgradeState.Idle -> ""
                }
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (upgradeState is UpgradeState.Upgrading) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        LinearProgressIndicator(
                            progress = { (upgradeState.progressPercent / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            strokeCap = StrokeCap.Round
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = upgradeState.currentStepName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${upgradeState.progressPercent}%",
                                style = LoaMonoTextStyle,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 160.dp, max = 280.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    shape = MaterialTheme.shapes.medium,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(logs) { line ->
                            Text(
                                text = line,
                                color = when {
                                    line.startsWith("ERROR") || line.contains("failed", ignoreCase = true) -> MaterialTheme.colorScheme.error
                                    line.startsWith("[Step") || line.contains("successfully", ignoreCase = true) -> MaterialTheme.colorScheme.secondary
                                    line.startsWith("  ->") -> MaterialTheme.colorScheme.onSurface
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                style = LoaMonoTextStyle
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (!isUpgrading) {
                Button(onClick = onDismiss) {
                    Text("Done")
                }
            }
        }
    )
}
