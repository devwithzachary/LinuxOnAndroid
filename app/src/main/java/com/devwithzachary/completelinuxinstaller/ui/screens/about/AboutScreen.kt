package com.devwithzachary.completelinuxinstaller.ui.screens.about

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DeveloperBoard
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Tag
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.devwithzachary.completelinuxinstaller.BuildConfig
import com.devwithzachary.completelinuxinstaller.R
import com.devwithzachary.completelinuxinstaller.engine.UpdateCheckResult
import com.devwithzachary.completelinuxinstaller.ui.designsystem.LoaHeroContainer
import com.devwithzachary.completelinuxinstaller.ui.designsystem.LoaMonospaceChip
import com.devwithzachary.completelinuxinstaller.ui.designsystem.LoaProgressRing
import com.devwithzachary.completelinuxinstaller.ui.designsystem.LoaSectionHeader
import com.devwithzachary.completelinuxinstaller.ui.designsystem.LoaSegmentedGroup
import com.devwithzachary.completelinuxinstaller.ui.designsystem.LoaStatusBadge
import com.devwithzachary.completelinuxinstaller.ui.designsystem.LoaStatusTone
import com.devwithzachary.completelinuxinstaller.ui.theme.LinuxOnAndroidTheme

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AboutScreen(
    onCheckForUpdatesClick: () -> Unit = {},
    isCheckingForUpdates: Boolean = false,
    updateCheckResult: UpdateCheckResult? = null,
    onViewAppOverview: () -> Unit = {},
    onBack: (() -> Unit)? = null
) {
    val uriHandler = LocalUriHandler.current
    val websiteUrl = stringResource(R.string.website_url)
    val websiteDomain = stringResource(R.string.website_domain)
    val discordUrl = stringResource(R.string.discord_url)
    val patreonUrl = stringResource(R.string.patreon_url)
    val buymeacoffeeUrl = stringResource(R.string.buymeacoffee_url)
    val githubIssuesUrl = stringResource(R.string.github_issues_url)

    val appTitle = stringResource(R.string.app_title)
    val appTagline = stringResource(R.string.app_tagline)
    val aboutInfoTitle = stringResource(R.string.about_info_title)
    val labelAppVersion = stringResource(R.string.label_app_version)
    val labelBuildTarget = stringResource(R.string.label_build_target)
    val labelLinuxDistro = stringResource(R.string.label_linux_distro)
    val labelContainerEngine = stringResource(R.string.label_container_engine)
    val labelDeveloper = stringResource(R.string.label_developer)
    val checkUpdatesTitle = if (isCheckingForUpdates) {
        stringResource(R.string.github_updates_status_checking)
    } else {
        stringResource(R.string.github_updates_manual_check_about)
    }
    val upToDateText = (updateCheckResult as? UpdateCheckResult.UpToDate)?.let {
        stringResource(R.string.github_updates_status_up_to_date, it.currentVersion)
    }
    val errorText = (updateCheckResult as? UpdateCheckResult.Error)?.let {
        stringResource(R.string.github_updates_status_error, it.message)
    }

    val discordTitle = stringResource(R.string.discord_card_title)
    val discordDesc = stringResource(R.string.discord_card_text)
    val alphaTitle = stringResource(R.string.alpha_card_title)
    val alphaDesc = stringResource(R.string.alpha_card_text)
    val patreonTitle = stringResource(R.string.btn_join_patreon)
    val buyMeACoffeeTitle = stringResource(R.string.btn_buy_me_a_coffee)
    val patreonDesc = stringResource(R.string.patreon_support_text)

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_about)) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Back to settings"
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                LoaHeroContainer(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_launcher_logo),
                            contentDescription = appTitle,
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                        )

                        Text(
                            text = appTitle,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = appTagline,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.widthIn(max = 560.dp)
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            itemVerticalAlignment = Alignment.CenterVertically
                        ) {
                            LoaMonospaceChip(
                                text = "v${BuildConfig.VERSION_NAME}",
                                leadingIcon = Icons.Rounded.Tag
                            )
                            LoaMonospaceChip(
                                text = websiteDomain,
                                leadingIcon = Icons.Rounded.Language,
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                onClick = {
                                    try {
                                        uriHandler.openUri(websiteUrl)
                                    } catch (_: Exception) {
                                    }
                                }
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LoaSectionHeader(title = aboutInfoTitle)

                    LoaSegmentedGroup {
                        row(
                            title = labelAppVersion,
                            leadingIcon = Icons.Rounded.Tag,
                            trailingContent = {
                                LoaMonospaceChip(text = "v${BuildConfig.VERSION_NAME}")
                            }
                        )
                        row(
                            title = labelBuildTarget,
                            leadingIcon = Icons.Rounded.DeveloperBoard,
                            trailingContent = {
                                LoaMonospaceChip(text = "Release (ARM64-v8a)")
                            }
                        )
                        row(
                            title = labelLinuxDistro,
                            leadingIcon = Icons.Rounded.Terminal,
                            trailingContent = {
                                LoaMonospaceChip(text = "7 Supported Distros")
                            }
                        )
                        row(
                            title = labelContainerEngine,
                            leadingIcon = Icons.Rounded.Memory,
                            trailingContent = {
                                LoaMonospaceChip(text = "PRoot 5.3")
                            }
                        )
                        row(
                            title = labelDeveloper,
                            leadingIcon = Icons.Rounded.Person,
                            trailingContent = {
                                LoaMonospaceChip(text = "DevWithZachary")
                            }
                        )
                        if (!BuildConfig.IS_PLAY_STORE) {
                            row(
                                title = checkUpdatesTitle,
                                subtitle = upToDateText ?: errorText,
                                leadingIcon = Icons.Rounded.CloudDownload,
                                enabled = !isCheckingForUpdates,
                                trailingContent = {
                                    when {
                                        isCheckingForUpdates -> LoaProgressRing(
                                            progress = null,
                                            size = 20.dp,
                                            strokeWidth = 3.dp
                                        )
                                        updateCheckResult is UpdateCheckResult.UpToDate -> LoaStatusBadge(
                                            label = "Up to date",
                                            tone = LoaStatusTone.Running
                                        )
                                        updateCheckResult is UpdateCheckResult.Error -> LoaStatusBadge(
                                            label = "Error",
                                            tone = LoaStatusTone.Error
                                        )
                                        else -> Icon(
                                            imageVector = Icons.Rounded.ChevronRight,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                onClick = onCheckForUpdatesClick
                            )
                        }
                        row(
                            title = "App Overview & Architecture",
                            subtitle = "View PRoot user-space virtualization introduction",
                            leadingIcon = Icons.Rounded.Info,
                            trailingContent = {
                                Icon(
                                    imageVector = Icons.Rounded.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            onClick = onViewAppOverview
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LoaSectionHeader(title = "Community & Support")

                    LoaSegmentedGroup {
                        row(
                            title = discordTitle,
                            subtitle = discordDesc,
                            subtitleMaxLines = 3,
                            leadingIcon = Icons.Rounded.Forum,
                            trailingContent = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            onClick = {
                                try {
                                    uriHandler.openUri(discordUrl)
                                } catch (_: Exception) {
                                }
                            }
                        )
                        row(
                            title = alphaTitle,
                            subtitle = alphaDesc,
                            subtitleMaxLines = 3,
                            leadingIcon = Icons.Rounded.BugReport,
                            trailingContent = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            onClick = {
                                try {
                                    uriHandler.openUri(githubIssuesUrl)
                                } catch (_: Exception) {
                                }
                            }
                        )
                        if (!BuildConfig.IS_PLAY_STORE) {
                            row(
                                title = patreonTitle,
                                subtitle = patreonDesc,
                                subtitleMaxLines = 3,
                                leadingIcon = Icons.Rounded.Favorite,
                                trailingContent = {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                onClick = {
                                    try {
                                        uriHandler.openUri(patreonUrl)
                                    } catch (_: Exception) {
                                    }
                                }
                            )
                            row(
                                title = buyMeACoffeeTitle,
                                leadingIcon = Icons.Rounded.LocalCafe,
                                trailingContent = {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                onClick = {
                                    try {
                                        uriHandler.openUri(buymeacoffeeUrl)
                                    } catch (_: Exception) {
                                    }
                                }
                            )
                        }
                    }
                }

                AboutCreditsSection()

                AboutChangelogSection()
            }
        }
    }
}

@Preview(
    name = "About - Light",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Composable
private fun AboutScreenLightPreview() {
    LinuxOnAndroidTheme(darkTheme = false, dynamicColor = false) {
        AboutScreen(onBack = {})
    }
}

@Preview(
    name = "About - Dark",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun AboutScreenDarkPreview() {
    LinuxOnAndroidTheme(darkTheme = true, dynamicColor = false) {
        AboutScreen(onBack = {})
    }
}
