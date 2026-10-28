package com.devwithzachary.completelinuxinstaller.ui.screens.settings.cards

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devwithzachary.completelinuxinstaller.R
import com.devwithzachary.completelinuxinstaller.engine.UpdateCheckResult
import com.devwithzachary.completelinuxinstaller.ui.screens.settings.CollapsibleSettingsCard

@Composable
fun AppUpdatesCard(
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    isGitHubUpdateCheckEnabled: Boolean,
    onSetGitHubUpdateCheckEnabled: (Boolean) -> Unit,
    isCheckingForUpdates: Boolean,
    updateCheckResult: UpdateCheckResult?,
    onCheckForUpdatesClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    CollapsibleSettingsCard(
        title = stringResource(R.string.github_updates_card_title),
        subtitle = stringResource(R.string.github_updates_card_subtitle),
        icon = Icons.Default.CloudDownload,
        isExpanded = isExpanded,
        onToggleExpand = onToggleExpand,
        modifier = modifier,
        badge = {
            if (updateCheckResult is UpdateCheckResult.UpdateAvailable) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = updateCheckResult.release.tagName,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    ) {
        Text(
            text = stringResource(R.string.github_updates_setting_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { onSetGitHubUpdateCheckEnabled(!isGitHubUpdateCheckEnabled) }
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.github_updates_setting_title),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (isGitHubUpdateCheckEnabled) "Automatic startup & background release checks active" else "Automatic release checks disabled (opt-in)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = isGitHubUpdateCheckEnabled,
                onCheckedChange = { onSetGitHubUpdateCheckEnabled(it) }
            )
        }

        Button(
            onClick = onCheckForUpdatesClick,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isCheckingForUpdates,
            shape = RoundedCornerShape(10.dp)
        ) {
            if (isCheckingForUpdates) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                if (isCheckingForUpdates) stringResource(R.string.github_updates_status_checking)
                else stringResource(R.string.github_updates_btn_check)
            )
        }

        if (updateCheckResult is UpdateCheckResult.UpToDate) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF1E3A1E),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(18.dp))
                    Text(
                        text = stringResource(R.string.github_updates_status_up_to_date, updateCheckResult.currentVersion),
                        color = Color(0xFF4CAF50),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        } else if (updateCheckResult is UpdateCheckResult.Error) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    Text(
                        text = stringResource(R.string.github_updates_status_error, updateCheckResult.message),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
