package com.devwithzachary.completelinuxinstaller.ui.designsystem

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.devwithzachary.completelinuxinstaller.ui.theme.LinuxOnAndroidTheme
import com.devwithzachary.completelinuxinstaller.ui.theme.LoaMonoTextStyle
import com.devwithzachary.completelinuxinstaller.ui.theme.LoaSheetShape

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun DesignSystemGallery() {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "LinuxOnAndroid Design System",
                style = MaterialTheme.typography.headlineMedium
            )

            LoaSectionHeader(
                title = "Active Distribution",
                actionLabel = "Manage",
                onActionClick = {}
            )

            LoaHeroContainer {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Ubuntu 24.04 LTS",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        LoaStatusBadge(
                            label = "Running",
                            tone = LoaStatusTone.Running
                        )
                    }

                    LoaProgressRing(progress = 0.72f) {
                        Text(
                            text = "72%",
                            style = LoaMonoTextStyle,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LoaMonospaceChip(
                        text = "arm64",
                        leadingIcon = Icons.Rounded.Memory
                    )
                    LoaMonospaceChip(text = ":5900")
                    LoaMonospaceChip(
                        text = "/data/local/ubuntu",
                        leadingIcon = Icons.Rounded.Folder
                    )
                }
            }

            LoaSectionHeader(title = "Status Badges & Progress Rings")

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LoaStatusBadge(label = "Running", tone = LoaStatusTone.Running)
                LoaStatusBadge(label = "Stopped", tone = LoaStatusTone.Stopped)
                LoaStatusBadge(label = "Downloading", tone = LoaStatusTone.Downloading)
                LoaStatusBadge(label = "Warning", tone = LoaStatusTone.Warning)
                LoaStatusBadge(label = "Error", tone = LoaStatusTone.Error)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LoaProgressRing(progress = 0.45f) {
                    Text(
                        text = "45%",
                        style = LoaMonoTextStyle
                    )
                }
                LoaProgressRing(progress = null) {
                    Icon(
                        imageVector = Icons.Rounded.Terminal,
                        contentDescription = null
                    )
                }
            }

            LoaSectionHeader(title = "Segmented List Group")

            LoaSegmentedGroup {
                row(
                    title = "Keep Session Alive",
                    subtitle = "Prevent Android from stopping background services",
                    leadingIcon = Icons.Rounded.Settings,
                    trailingContent = {
                        Switch(checked = true, onCheckedChange = {})
                    },
                    onClick = {}
                )
                row(
                    title = "VNC Display Server",
                    subtitle = "XFCE desktop over local loopback",
                    leadingIcon = Icons.Rounded.Computer,
                    trailingContent = {
                        LoaMonospaceChip(text = ":5900")
                    },
                    onClick = {}
                )
                row(
                    title = "Shell Environment",
                    subtitle = "Configure default login user and path",
                    leadingIcon = Icons.Rounded.Terminal,
                    trailingContent = {
                        Icon(
                            imageVector = Icons.Rounded.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    onClick = {}
                )
            }

            LoaSegmentedGroup {
                row(
                    title = "Single Item Group",
                    subtitle = "Uses full 20dp corner radius",
                    leadingIcon = Icons.Rounded.Folder,
                    onClick = {}
                )
            }

            LoaSectionHeader(title = "Bottom Sheet Container")

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = LoaSheetShape,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                contentColor = MaterialTheme.colorScheme.onSurface,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    BottomSheetDefaults.DragHandle()
                    LoaBottomSheetContent(
                        title = "Configure SSH Port",
                        subtitle = "Choose the local port used for OpenSSH connections"
                    ) {
                        LoaMonospaceChip(text = "127.0.0.1:2222")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Spacer(modifier = Modifier.weight(1f))
                            FilledTonalButton(onClick = {}) {
                                Text(text = "Cancel")
                            }
                            Button(onClick = {}) {
                                Text(text = "Apply")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(
    name = "Design System - Light",
    showBackground = true,
    widthDp = 412,
    heightDp = 1280,
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Composable
private fun DesignSystemLightPreview() {
    LinuxOnAndroidTheme(darkTheme = false, dynamicColor = false) {
        DesignSystemGallery()
    }
}

@Preview(
    name = "Design System - Dark",
    showBackground = true,
    widthDp = 412,
    heightDp = 1280,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun DesignSystemDarkPreview() {
    LinuxOnAndroidTheme(darkTheme = true, dynamicColor = false) {
        DesignSystemGallery()
    }
}
