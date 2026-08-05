package com.devwithzachary.completelinuxinstaller.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
}
