package com.devwithzachary.completelinuxinstaller.ui.screens.about

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.devwithzachary.completelinuxinstaller.BuildConfig
import com.devwithzachary.completelinuxinstaller.R
import com.devwithzachary.completelinuxinstaller.ui.designsystem.LoaMonospaceChip
import com.devwithzachary.completelinuxinstaller.ui.designsystem.LoaSectionHeader
import com.devwithzachary.completelinuxinstaller.ui.designsystem.LoaSegmentedGroup
import com.devwithzachary.completelinuxinstaller.ui.theme.LoaSegmentedSingleShape

enum class CreditsCategory(val labelRes: Int) {
    ALL(R.string.nav_about),
    CODE(R.string.credits_tab_code),
    ISSUES(R.string.credits_tab_issues),
    PATREON(R.string.credits_tab_patreon)
}

data class ContributionItem(
    val title: String,
    val referenceNumber: Int,
    val isPr: Boolean = false,
    val url: String = if (isPr) {
        "https://github.com/devwithzachary/LinuxOnAndroid/pull/$referenceNumber"
    } else {
        "https://github.com/devwithzachary/LinuxOnAndroid/issues/$referenceNumber"
    }
)

data class Contributor(
    val username: String,
    val displayName: String = username,
    val roleBadge: String,
    val isCodeContributor: Boolean = false,
    val contributions: List<ContributionItem>
) {
    val githubUrl: String = "https://github.com/$username"
}

data class Sponsor(
    val name: String,
    val tier: String,
    val profileUrl: String? = null
)

val CODE_CONTRIBUTORS: List<Contributor> = listOf(
    Contributor(
        username = "bkodenkt",
        displayName = "bkodenkt",
        roleBadge = "German Translation & Code",
        isCodeContributor = true,
        contributions = listOf(
            ContributionItem(
                title = "Added folder and strings-file for German localization",
                referenceNumber = 27,
                isPr = true
            )
        )
    ),
    Contributor(
        username = "sleepy-snowflake",
        displayName = "sleepy-snowflake",
        roleBadge = "Diagnostics, WakeLock & Startup",
        isCodeContributor = true,
        contributions = listOf(
            ContributionItem(
                title = "Add license files for the bundled terminal fonts",
                referenceNumber = 33,
                isPr = true
            ),
            ContributionItem(
                title = "Keep screen on while terminal session is running",
                referenceNumber = 34,
                isPr = true
            ),
            ContributionItem(
                title = "Add startup sanity check with slow-mode splash escape hatch",
                referenceNumber = 35,
                isPr = true
            ),
            ContributionItem(
                title = "Keep screen on while setup wizard is visible",
                referenceNumber = 36,
                isPr = true
            ),
            ContributionItem(
                title = "Add diagnostics debug report generator in settings",
                referenceNumber = 37,
                isPr = true
            )
        )
    )
)

val ISSUE_CONTRIBUTORS: List<Contributor> = listOf(
    Contributor(
        username = "bkodenkt",
        displayName = "bkodenkt",
        roleBadge = "Feature & Bug Pioneer",
        contributions = listOf(
            ContributionItem("User set up at installation does not replace \"ubuntu\" default user", 9),
            ContributionItem("Terminal login always as root, no password challenge when changing users", 10),
            ContributionItem("Adding and moving key-combinations and shortcuts", 11),
            ContributionItem("Console output scrollback buffer & scroll controls", 12),
            ContributionItem("Selecting the SSH port for the quick-start button", 13),
            ContributionItem("Custom DNS server configuration & presets", 14),
            ContributionItem("Copy and Paste in terminal & screen selection", 16),
            ContributionItem("OpenSSH Server one-touch launcher error fix", 21),
            ContributionItem("Keeping the screen alive during terminal and SSH sessions", 25)
        )
    ),
    Contributor(
        username = "hax4dazy",
        displayName = "Hax4dayz",
        roleBadge = "Sudo & Runtime Pioneer",
        contributions = listOf(
            ContributionItem("Interactive Ubuntu sudo & user-space setuid restoration", 7),
            ContributionItem("UBUNTU_CODENAME & VERSION_CODENAME environment exports", 19)
        )
    ),
    Contributor(
        username = "HappyYoyo09",
        displayName = "HappyYoyo09",
        roleBadge = "Input Buffer Pioneer",
        contributions = listOf(
            ContributionItem("Dropped inputs when typing fast (IME stream optimization)", 8)
        )
    ),
    Contributor(
        username = "rayoflight3000",
        displayName = "rayoflight3000",
        roleBadge = "Typography Pioneer",
        contributions = listOf(
            ContributionItem("Terminal character gaps with custom OEM fonts (Monospace font engine)", 31)
        )
    )
)

val PATREON_SPONSORS: List<Sponsor> = listOf(
    Sponsor(
        name = "Alen",
        tier = "Tinkerer (BuyMeACoffee)"
    ),
    Sponsor(
        name = "Many Fun Adventures",
        tier = "Supporter (BuyMeACoffee)"
    ),
    Sponsor(
        name = "Old PC Gunk (and stuff)",
        tier = "Supporter (Patreon)"
    ),
    Sponsor(
        name = "ShoaffSquatch",
        tier = "Supporter (YouTube)"
    ),
    Sponsor(
        name = "Swamp Gaming Network",
        tier = "Supporter (Patreon)"
    ),
    Sponsor(
        name = "насэр Хорр",
        tier = "Supporter (Patreon)"
    ),
    Sponsor(
        name = "Alcove Electronics Workshop",
        tier = "Tipper (YouTube)"
    ),
    Sponsor(
        name = "Ian Scales",
        tier = "Tipper (YouTube)"
    )
)

@Composable
fun AboutCreditsSection(
    modifier: Modifier = Modifier,
    initialExpanded: Boolean = false
) {
    val uriHandler = LocalUriHandler.current
    val patreonUrl = stringResource(R.string.patreon_url)
    val buymeacoffeeUrl = stringResource(R.string.buymeacoffee_url)
    var isSectionExpanded by rememberSaveable { mutableStateOf(initialExpanded) }
    var selectedCategory by remember { mutableStateOf(CreditsCategory.ALL) }

    val creditsTitle = stringResource(R.string.about_credits_title)
    val totalContributors = remember {
        (CODE_CONTRIBUTORS.map { it.username } + ISSUE_CONTRIBUTORS.map { it.username }).distinct().size
    }
    val totalPrs = remember { CODE_CONTRIBUTORS.flatMap { it.contributions }.count { it.isPr } }
    val totalIssues = remember { ISSUE_CONTRIBUTORS.flatMap { it.contributions }.count { !it.isPr } }
    val summarySubtitle = if (isSectionExpanded) {
        stringResource(R.string.about_credits_subtitle)
    } else {
        "$totalContributors contributors - $totalPrs ${if (totalPrs == 1) "PR" else "PRs"} - $totalIssues ${if (totalIssues == 1) "issue" else "issues"}"
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LoaSectionHeader(title = creditsTitle)

        LoaSegmentedGroup {
            row(
                title = creditsTitle,
                subtitle = summarySubtitle,
                leadingIcon = Icons.Rounded.VolunteerActivism,
                trailingContent = {
                    Icon(
                        imageVector = if (isSectionExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = if (isSectionExpanded) "Collapse $creditsTitle" else "Expand $creditsTitle",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                onClick = { isSectionExpanded = !isSectionExpanded }
            )
        }

        AnimatedVisibility(
            visible = isSectionExpanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = selectedCategory == CreditsCategory.ALL,
                            onClick = { selectedCategory = CreditsCategory.ALL },
                            label = { Text("All") },
                            leadingIcon = if (selectedCategory == CreditsCategory.ALL) {
                                { Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedCategory == CreditsCategory.CODE,
                            onClick = { selectedCategory = CreditsCategory.CODE },
                            label = { Text(stringResource(R.string.credits_tab_code)) },
                            leadingIcon = if (selectedCategory == CreditsCategory.CODE) {
                                { Icon(Icons.Rounded.Code, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedCategory == CreditsCategory.ISSUES,
                            onClick = { selectedCategory = CreditsCategory.ISSUES },
                            label = { Text(stringResource(R.string.credits_tab_issues)) },
                            leadingIcon = if (selectedCategory == CreditsCategory.ISSUES) {
                                { Icon(Icons.Rounded.BugReport, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                    }
                    if (!BuildConfig.IS_PLAY_STORE) {
                        item {
                            FilterChip(
                                selected = selectedCategory == CreditsCategory.PATREON,
                                onClick = { selectedCategory = CreditsCategory.PATREON },
                                label = { Text(stringResource(R.string.credits_tab_patreon)) },
                                leadingIcon = if (selectedCategory == CreditsCategory.PATREON) {
                                    { Icon(Icons.Rounded.Favorite, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null
                            )
                        }
                    }
                }

                if (selectedCategory == CreditsCategory.ALL || selectedCategory == CreditsCategory.CODE) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        LoaSectionHeader(title = "Code & Pull Requests")
                        LoaSegmentedGroup {
                            for (contributor in CODE_CONTRIBUTORS) {
                                item { shape ->
                                    ContributorCard(
                                        contributor = contributor,
                                        shape = shape,
                                        onOpenUrl = { url ->
                                            try { uriHandler.openUri(url) } catch (_: Exception) {}
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                if (selectedCategory == CreditsCategory.ALL || selectedCategory == CreditsCategory.ISSUES) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        LoaSectionHeader(title = "Bug Hunters & Feature Pioneers")
                        LoaSegmentedGroup {
                            for (contributor in ISSUE_CONTRIBUTORS) {
                                item { shape ->
                                    ContributorCard(
                                        contributor = contributor,
                                        shape = shape,
                                        onOpenUrl = { url ->
                                            try { uriHandler.openUri(url) } catch (_: Exception) {}
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                if (!BuildConfig.IS_PLAY_STORE && (selectedCategory == CreditsCategory.ALL || selectedCategory == CreditsCategory.PATREON)) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        LoaSectionHeader(title = stringResource(R.string.credits_tab_patreon))

                        if (PATREON_SPONSORS.isEmpty()) {
                            Surface(
                                shape = LoaSegmentedSingleShape,
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                tonalElevation = 0.dp,
                                shadowElevation = 0.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.credits_patreon_empty_title),
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = stringResource(R.string.credits_patreon_empty_text),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.widthIn(max = 560.dp)
                                    )
                                }
                            }
                        } else {
                            LoaSegmentedGroup {
                                for (sponsor in PATREON_SPONSORS) {
                                    item { shape ->
                                        SponsorCard(
                                            sponsor = sponsor,
                                            shape = shape,
                                            onOpenUrl = { url ->
                                                try { uriHandler.openUri(url) } catch (_: Exception) {}
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            FilledTonalButton(
                                onClick = {
                                    try { uriHandler.openUri(patreonUrl) } catch (_: Exception) {}
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .widthIn(max = 320.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Favorite,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.btn_join_patreon), maxLines = 1)
                            }

                            OutlinedButton(
                                onClick = {
                                    try { uriHandler.openUri(buymeacoffeeUrl) } catch (_: Exception) {}
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .widthIn(max = 320.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.LocalCafe,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.btn_buy_me_a_coffee), maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ContributorCard(
    contributor: Contributor,
    onOpenUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = LoaSegmentedSingleShape
) {
    var isExpanded by remember { mutableStateOf(false) }

    Surface(
        onClick = { isExpanded = !isExpanded },
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                if (contributor.isCodeContributor) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.secondaryContainer
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = contributor.displayName.take(2).uppercase(),
                            style = MaterialTheme.typography.titleSmall,
                            color = if (contributor.isCodeContributor) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            }
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = contributor.displayName,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            LoaMonospaceChip(
                                text = "@${contributor.username}",
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                onClick = { onOpenUrl(contributor.githubUrl) }
                            )
                        }
                        Text(
                            text = "${contributor.roleBadge} - ${contributor.contributions.size} ${if (contributor.contributions.size == 1) "item" else "items"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse ${contributor.displayName}" else "Expand ${contributor.displayName}",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    for (item in contributor.contributions) {
                        Surface(
                            onClick = { onOpenUrl(item.url) },
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            tonalElevation = 0.dp,
                            shadowElevation = 0.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                LoaMonospaceChip(
                                    text = if (item.isPr) "PR #${item.referenceNumber}" else "#${item.referenceNumber}",
                                    containerColor = if (item.isPr) {
                                        MaterialTheme.colorScheme.tertiaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.secondaryContainer
                                    },
                                    contentColor = if (item.isPr) {
                                        MaterialTheme.colorScheme.onTertiaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                    }
                                )

                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )

                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                                    contentDescription = "Open ${if (item.isPr) "PR #${item.referenceNumber}" else "issue #${item.referenceNumber}"}",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SponsorCard(
    sponsor: Sponsor,
    onOpenUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = LoaSegmentedSingleShape
) {
    val rowContent: @Composable () -> Unit = {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.tertiaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Favorite,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = sponsor.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = sponsor.tier,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (sponsor.profileUrl != null) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                    contentDescription = "Open ${sponsor.name} profile",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }

    if (sponsor.profileUrl != null) {
        Surface(
            onClick = { onOpenUrl(sponsor.profileUrl) },
            shape = shape,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            modifier = modifier.fillMaxWidth(),
            content = rowContent
        )
    } else {
        Surface(
            shape = shape,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            modifier = modifier.fillMaxWidth(),
            content = rowContent
        )
    }
}
