package com.devwithzachary.completelinuxinstaller.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devwithzachary.completelinuxinstaller.R
import com.devwithzachary.completelinuxinstaller.ui.DashboardUiState

@Composable
fun SystemStatusCard(
    state: DashboardUiState,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.status_card_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stringResource(R.string.label_distribution), style = MaterialTheme.typography.bodyMedium)
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
                Text(stringResource(R.string.label_storage_allocated), style = MaterialTheme.typography.bodyMedium)
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
                Text(stringResource(R.string.label_sdcard_bound), style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = if (state.bindSdCard) stringResource(R.string.value_sdcard_yes) else stringResource(R.string.value_sdcard_disabled),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (state.bindSdCard) Color(0xFF4CAF50) else Color.Gray
                )
            }

            if (state.isInstalled) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Text("Container Version:", style = MaterialTheme.typography.bodyMedium)
                    Row(
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val ver = state.rootfsVersion
                        Text(
                            text = if (ver != null) "v${ver.versionName} (b${ver.versionCode})" else "v1.0.0 (Legacy)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (state.isUpgradeAvailable) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "Update Available",
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
