package com.devwithzachary.completelinuxinstaller.ui.screens.settings.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.devwithzachary.completelinuxinstaller.R
import com.devwithzachary.completelinuxinstaller.engine.UpdateCheckResult
import com.devwithzachary.completelinuxinstaller.ui.designsystem.LoaProgressRing
import com.devwithzachary.completelinuxinstaller.ui.designsystem.LoaSectionHeader
import com.devwithzachary.completelinuxinstaller.ui.designsystem.LoaSegmentedGroup
import com.devwithzachary.completelinuxinstaller.ui.designsystem.LoaStatusBadge
import com.devwithzachary.completelinuxinstaller.ui.designsystem.LoaStatusTone
import com.devwithzachary.completelinuxinstaller.ui.screens.whatsnew.CURRENT_WHATS_NEW_RELEASE

@Composable
fun AppUpdatesCard(
    isGitHubUpdateCheckEnabled: Boolean,
    onSetGitHubUpdateCheckEnabled: (Boolean) -> Unit,
    isCheckingForUpdates: Boolean,
    updateCheckResult: UpdateCheckResult?,
    onCheckForUpdatesClick: () -> Unit,
    isShowWhatsNewOnUpdateEnabled: Boolean = true,
    onSetShowWhatsNewOnUpdate: (Boolean) -> Unit = {},
    onOpenWhatsNew: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val updatesTitle = stringResource(R.string.github_updates_setting_title)
    val checkUpdatesTitle = stringResource(R.string.github_updates_btn_check)
    val checkingLabel = stringResource(R.string.github_updates_status_checking)

    val checkSubtitle = when {
        isCheckingForUpdates -> checkingLabel
        updateCheckResult is UpdateCheckResult.UpToDate -> {
            stringResource(R.string.github_updates_status_up_to_date, updateCheckResult.currentVersion)
        }
        updateCheckResult is UpdateCheckResult.UpdateAvailable -> {
            "New release available: ${updateCheckResult.release.tagName}"
        }
        updateCheckResult is UpdateCheckResult.Error -> {
            stringResource(R.string.github_updates_status_error, updateCheckResult.message)
        }
        else -> stringResource(R.string.github_updates_card_subtitle)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LoaSectionHeader(title = "Updates")

        LoaSegmentedGroup {
            row(
                title = updatesTitle,
                subtitle = if (isGitHubUpdateCheckEnabled) {
                    "Automatic startup & background release checks active"
                } else {
                    "Automatic release checks disabled"
                },
                leadingIcon = Icons.Rounded.NotificationsActive,
                trailingContent = {
                    Switch(
                        checked = isGitHubUpdateCheckEnabled,
                        onCheckedChange = onSetGitHubUpdateCheckEnabled
                    )
                },
                onClick = { onSetGitHubUpdateCheckEnabled(!isGitHubUpdateCheckEnabled) }
            )

            row(
                title = checkUpdatesTitle,
                subtitle = checkSubtitle,
                leadingIcon = Icons.Rounded.SystemUpdate,
                enabled = !isCheckingForUpdates,
                trailingContent = {
                    when {
                        isCheckingForUpdates -> {
                            LoaProgressRing(
                                progress = null,
                                size = 24.dp,
                                strokeWidth = 3.dp
                            )
                        }
                        updateCheckResult is UpdateCheckResult.UpdateAvailable -> {
                            LoaStatusBadge(
                                label = updateCheckResult.release.tagName,
                                tone = LoaStatusTone.Downloading
                            )
                        }
                        updateCheckResult is UpdateCheckResult.UpToDate -> {
                            LoaStatusBadge(
                                label = "Up to Date",
                                tone = LoaStatusTone.Running
                            )
                        }
                        updateCheckResult is UpdateCheckResult.Error -> {
                            LoaStatusBadge(
                                label = "Error",
                                tone = LoaStatusTone.Error
                            )
                        }
                        else -> {
                            Icon(
                                imageVector = Icons.Rounded.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                onClick = onCheckForUpdatesClick
            )

            row(
                title = "Show What's New on Update",
                subtitle = "Automatically display highlights after major app updates",
                leadingIcon = Icons.Rounded.AutoAwesome,
                trailingContent = {
                    Switch(
                        checked = isShowWhatsNewOnUpdateEnabled,
                        onCheckedChange = onSetShowWhatsNewOnUpdate
                    )
                },
                onClick = { onSetShowWhatsNewOnUpdate(!isShowWhatsNewOnUpdateEnabled) }
            )

            row(
                title = "Review What's New",
                subtitle = CURRENT_WHATS_NEW_RELEASE.displayVersion,
                leadingIcon = Icons.Rounded.NewReleases,
                trailingContent = {
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                onClick = onOpenWhatsNew
            )
        }
    }
}
