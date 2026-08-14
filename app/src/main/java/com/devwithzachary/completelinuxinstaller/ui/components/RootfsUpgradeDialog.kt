package com.devwithzachary.completelinuxinstaller.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Upgrade
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.devwithzachary.completelinuxinstaller.engine.UpgradeState

@Composable
fun RootfsUpgradeDialog(
    upgradeState: UpgradeState,
    onDismiss: () -> Unit
) {
    if (upgradeState is UpgradeState.Idle) return

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

    Dialog(
        onDismissRequest = {
            if (upgradeState !is UpgradeState.Upgrading) {
                onDismiss()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = upgradeState !is UpgradeState.Upgrading,
            dismissOnClickOutside = upgradeState !is UpgradeState.Upgrading,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.85f)
                .border(1.dp, Color(0xFF30363D), RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF161B22),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = when (upgradeState) {
                                is UpgradeState.Success -> Icons.Default.CheckCircle
                                is UpgradeState.Error -> Icons.Default.Error
                                is UpgradeState.Upgrading, is UpgradeState.Idle -> Icons.Default.Upgrade
                            },
                            contentDescription = null,
                            tint = when (upgradeState) {
                                is UpgradeState.Success -> Color(0xFF4CAF50)
                                is UpgradeState.Error -> MaterialTheme.colorScheme.error
                                is UpgradeState.Upgrading, is UpgradeState.Idle -> Color(0xFF58A6FF)
                            },
                            modifier = Modifier.size(28.dp)
                        )
                        Column {
                            Text(
                                text = when (upgradeState) {
                                    is UpgradeState.Success -> "RootFS Upgrade Complete"
                                    is UpgradeState.Error -> "Upgrade Failed"
                                    is UpgradeState.Upgrading -> "Upgrading RootFS Container..."
                                    is UpgradeState.Idle -> "RootFS Upgrade"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = when (upgradeState) {
                                    is UpgradeState.Success -> "All system patches and compatibility fixes applied"
                                    is UpgradeState.Error -> upgradeState.message
                                    is UpgradeState.Upgrading -> upgradeState.currentStepName
                                    is UpgradeState.Idle -> ""
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF8B949E)
                            )
                        }
                    }

                    if (upgradeState !is UpgradeState.Upgrading) {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color(0xFF8B949E)
                            )
                        }
                    }
                }

                HorizontalDivider(color = Color(0xFF30363D))

                // Progress Indicator
                if (upgradeState is UpgradeState.Upgrading) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        LinearProgressIndicator(
                            progress = { upgradeState.progressPercent / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp),
                            color = Color(0xFF58A6FF),
                            trackColor = Color(0xFF21262D),
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = upgradeState.currentStepName,
                                fontSize = 11.sp,
                                color = Color(0xFF8B949E)
                            )
                            Text(
                                text = "${upgradeState.progressPercent}%",
                                fontSize = 11.sp,
                                color = Color(0xFF58A6FF),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Terminal Log Console
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFF30363D), RoundedCornerShape(8.dp)),
                    color = Color(0xFF0D1117),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(logs) { line ->
                            Text(
                                text = line,
                                color = when {
                                    line.startsWith("ERROR") || line.contains("failed", ignoreCase = true) -> Color(0xFFF85149)
                                    line.startsWith("[Step") || line.contains("successfully", ignoreCase = true) -> Color(0xFF7EE787)
                                    line.startsWith("  ->") -> Color(0xFFE6EDF3)
                                    else -> Color(0xFF8B949E)
                                },
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                // Bottom Action Bar
                if (upgradeState !is UpgradeState.Upgrading) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (upgradeState is UpgradeState.Success) Color(0xFF238636) else MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text("Done")
                        }
                    }
                }
            }
        }
    }
}
