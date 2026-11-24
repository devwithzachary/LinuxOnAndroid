package com.devwithzachary.completelinuxinstaller.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.DesktopWindows
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Upgrade
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.devwithzachary.completelinuxinstaller.R
import com.devwithzachary.completelinuxinstaller.model.InstallStatus
import com.devwithzachary.completelinuxinstaller.model.SoftwarePackage
import com.devwithzachary.completelinuxinstaller.ui.designsystem.LoaProgressRing
import com.devwithzachary.completelinuxinstaller.ui.designsystem.LoaStatusBadge
import com.devwithzachary.completelinuxinstaller.ui.designsystem.LoaStatusTone
import com.devwithzachary.completelinuxinstaller.ui.theme.LoaMonoTextStyle
import com.devwithzachary.completelinuxinstaller.ui.theme.LoaSegmentedSingleShape

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SoftwareCard(
    pkg: SoftwarePackage,
    onInstallClick: () -> Unit,
    onUninstallClick: () -> Unit = {},
    onViewLogsClick: () -> Unit,
    onLaunchClick: (String) -> Unit = {},
    onUpgradeClick: () -> Unit = {},
    shape: Shape = LoaSegmentedSingleShape,
    modifier: Modifier = Modifier
) {
    val icon = when (pkg.iconName) {
        "DesktopWindows" -> Icons.Rounded.DesktopWindows
        "Code" -> Icons.Rounded.Code
        "Dns" -> Icons.Rounded.Dns
        "Terminal" -> Icons.Rounded.Terminal
        "Security" -> Icons.Rounded.Security
        "Android" -> Icons.Rounded.Android
        "Storage" -> Icons.Rounded.Storage
        "Build" -> Icons.Rounded.Build
        "Palette" -> Icons.Rounded.Palette
        "Description" -> Icons.Rounded.Description
        "Public" -> Icons.Rounded.Public
        "Tune" -> Icons.Rounded.Tune
        else -> Icons.Rounded.Apps
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (pkg.status == InstallStatus.INSTALLED) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(24.dp)
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            itemVerticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = pkg.name,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (pkg.isBeta) {
                                LoaStatusBadge(
                                    label = "Beta",
                                    tone = LoaStatusTone.Warning
                                )
                            }
                        }
                        Text(
                            text = pkg.category.displayName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                when (pkg.status) {
                    InstallStatus.INSTALLED -> {
                        LoaStatusBadge(
                            label = stringResource(R.string.status_installed),
                            tone = LoaStatusTone.Running
                        )
                    }
                    InstallStatus.INSTALLING, InstallStatus.UNINSTALLING -> {
                        LoaProgressRing(
                            progress = null,
                            size = 24.dp,
                            strokeWidth = 3.dp
                        )
                    }
                    InstallStatus.FAILED -> {
                        LoaStatusBadge(
                            label = stringResource(R.string.status_failed),
                            tone = LoaStatusTone.Error
                        )
                    }
                    InstallStatus.NOT_INSTALLED -> Unit
                }
            }

            Text(
                text = pkg.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (pkg.isBeta) {
                Text(
                    text = "Beta GUI app using software rendering.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }

            if (pkg.postInstallNotes != null && pkg.status == InstallStatus.INSTALLED) {
                Text(
                    text = "Notes: ${pkg.postInstallNotes}",
                    style = LoaMonoTextStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (pkg.status) {
                    InstallStatus.INSTALLING, InstallStatus.UNINSTALLING -> {
                        OutlinedButton(onClick = onViewLogsClick) {
                            Icon(
                                imageVector = Icons.Rounded.Terminal,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "View Logs",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    InstallStatus.NOT_INSTALLED, InstallStatus.FAILED -> {
                        FilledTonalButton(onClick = onInstallClick) {
                            Icon(
                                imageVector = Icons.Rounded.Download,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.btn_install),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    InstallStatus.INSTALLED -> {
                        if (pkg.launchCommand != null) {
                            FilledTonalButton(
                                onClick = { onLaunchClick(pkg.launchCommand) }
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.btn_start_service),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        if (pkg.hasUpgradeAvailable) {
                            OutlinedButton(onClick = onUpgradeClick) {
                                Icon(
                                    imageVector = Icons.Rounded.Upgrade,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Upgrade",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        TextButton(onClick = onViewLogsClick) {
                            Text(
                                text = "Logs",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        IconButton(onClick = onUninstallClick) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = "Uninstall ${pkg.name}",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}
