package com.devwithzachary.completelinuxinstaller.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.devwithzachary.completelinuxinstaller.R
import com.devwithzachary.completelinuxinstaller.ui.DashboardUiState
import com.devwithzachary.completelinuxinstaller.ui.designsystem.LoaMonospaceChip
import com.devwithzachary.completelinuxinstaller.ui.designsystem.LoaSectionHeader
import com.devwithzachary.completelinuxinstaller.ui.designsystem.LoaSegmentedGroup
import com.devwithzachary.completelinuxinstaller.ui.designsystem.LoaStatusBadge
import com.devwithzachary.completelinuxinstaller.ui.designsystem.LoaStatusTone

@Composable
fun SystemStatusCard(
    state: DashboardUiState,
    modifier: Modifier = Modifier
) {
    val distroLabel = stringResource(R.string.label_distribution).removeSuffix(":")
    val sdCardLabel = stringResource(R.string.label_sdcard_bound).removeSuffix(":")
    val sdCardDisabled = stringResource(R.string.value_sdcard_disabled)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LoaSectionHeader(title = stringResource(R.string.status_card_title))

        LoaSegmentedGroup {
            row(
                title = distroLabel,
                trailingContent = {
                    Text(
                        text = state.distroName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            )

            row(
                title = sdCardLabel,
                trailingContent = {
                    if (state.bindSdCard) {
                        LoaMonospaceChip(text = "/sdcard")
                    } else {
                        LoaStatusBadge(
                            label = sdCardDisabled,
                            tone = LoaStatusTone.Stopped
                        )
                    }
                }
            )

            if (state.isInstalled) {
                val ver = state.rootfsVersion
                row(
                    title = "Container Version",
                    trailingContent = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            LoaMonospaceChip(
                                text = if (ver != null) {
                                    "v${ver.versionName} (b${ver.versionCode})"
                                } else {
                                    "v1.0.0"
                                }
                            )
                            if (state.isUpgradeAvailable) {
                                LoaStatusBadge(
                                    label = "Update",
                                    tone = LoaStatusTone.Downloading
                                )
                            }
                        }
                    }
                )
            }
        }
    }
}
