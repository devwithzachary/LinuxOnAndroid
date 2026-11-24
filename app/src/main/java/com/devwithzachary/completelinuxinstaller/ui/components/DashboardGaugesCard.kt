package com.devwithzachary.completelinuxinstaller.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.devwithzachary.completelinuxinstaller.engine.SystemResourceMetrics
import com.devwithzachary.completelinuxinstaller.ui.designsystem.LoaProgressRing
import com.devwithzachary.completelinuxinstaller.ui.designsystem.LoaStatusBadge
import com.devwithzachary.completelinuxinstaller.ui.designsystem.LoaStatusTone
import com.devwithzachary.completelinuxinstaller.ui.theme.LoaMonoTextStyle
import com.devwithzachary.completelinuxinstaller.ui.theme.LoaSegmentedSingleShape
import java.util.Locale

@Composable
fun DashboardGaugesCard(
    metrics: SystemResourceMetrics,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = LoaSegmentedSingleShape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Live System Resources",
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                LoaStatusBadge(
                    label = if (metrics.isSessionRunning) "Live" else "Standby",
                    tone = if (metrics.isSessionRunning) LoaStatusTone.Running else LoaStatusTone.Stopped
                )
            }

            val ramProgress = metrics.ramUsagePercent.coerceIn(0f, 1f)
            val ramColor = when {
                ramProgress > 0.85f -> MaterialTheme.colorScheme.error
                ramProgress > 0.65f -> MaterialTheme.colorScheme.tertiary
                else -> MaterialTheme.colorScheme.primary
            }

            val diskProgress = if (metrics.storageTotalBytes > 0L) {
                metrics.storageUsagePercent.coerceIn(0f, 1f)
            } else {
                0f
            }
            val diskColor = when {
                diskProgress > 0.85f -> MaterialTheme.colorScheme.error
                diskProgress > 0.65f -> MaterialTheme.colorScheme.tertiary
                else -> MaterialTheme.colorScheme.primary
            }
            val freeGb = if (metrics.storageTotalBytes > 0) {
                String.format(
                    Locale.US,
                    "%.1f",
                    metrics.storageAvailableBytes / (1024.0 * 1024.0 * 1024.0)
                )
            } else {
                "0.0"
            }
            val rootfsUsedText = if (metrics.storageUsedMb >= 1024L) {
                String.format(Locale.US, "%.1f GB", metrics.storageUsedMb / 1024.0)
            } else {
                "${metrics.storageUsedMb} MB"
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ResourceRingColumn(
                    title = "RAM",
                    progress = ramProgress,
                    indicatorColor = ramColor,
                    primaryValue = "${metrics.containerMemoryUsedMb} MB",
                    secondaryValue = if (metrics.systemTotalRamMb > 0) {
                        "${metrics.systemUsedRamMb}/${metrics.systemTotalRamMb} MB"
                    } else {
                        "Device RAM"
                    },
                    modifier = Modifier.weight(1f)
                )

                ResourceRingColumn(
                    title = "Storage",
                    progress = diskProgress,
                    indicatorColor = diskColor,
                    primaryValue = rootfsUsedText,
                    secondaryValue = "$freeGb GB free",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ResourceRingColumn(
    title: String,
    progress: Float,
    indicatorColor: androidx.compose.ui.graphics.Color,
    primaryValue: String,
    secondaryValue: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LoaProgressRing(
            progress = progress,
            size = 72.dp,
            strokeWidth = 8.dp,
            indicatorColor = indicatorColor
        ) {
            Text(
                text = "${(progress * 100).toInt()}%",
                style = LoaMonoTextStyle,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = primaryValue,
                style = LoaMonoTextStyle,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = secondaryValue,
                style = LoaMonoTextStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
