package com.devwithzachary.completelinuxinstaller.ui.screens.about

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devwithzachary.completelinuxinstaller.R

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
        roleBadge = "Screen WakeLock & Font Licenses",
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

val PATREON_SPONSORS: List<Sponsor> = emptyList()

@Composable
fun AboutCreditsSection(
    modifier: Modifier = Modifier,
    initialExpanded: Boolean = false
) {
    val uriHandler = LocalUriHandler.current
    val patreonUrl = stringResource(R.string.patreon_url)
    var isSectionExpanded by rememberSaveable { mutableStateOf(initialExpanded) }
    var selectedCategory by remember { mutableStateOf(CreditsCategory.ALL) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Clickable Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { isSectionExpanded = !isSectionExpanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.VolunteerActivism,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.about_credits_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isSectionExpanded) {
                                stringResource(R.string.about_credits_subtitle)
                            } else {
                                "4 contributors • 1 PR • 12 issues"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = { isSectionExpanded = !isSectionExpanded },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isSectionExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isSectionExpanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            AnimatedVisibility(
                visible = isSectionExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {

            // Category Filter Chips
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
                            { Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                }
                item {
                    FilterChip(
                        selected = selectedCategory == CreditsCategory.CODE,
                        onClick = { selectedCategory = CreditsCategory.CODE },
                        label = { Text(stringResource(R.string.credits_tab_code)) },
                        leadingIcon = if (selectedCategory == CreditsCategory.CODE) {
                            { Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                }
                item {
                    FilterChip(
                        selected = selectedCategory == CreditsCategory.ISSUES,
                        onClick = { selectedCategory = CreditsCategory.ISSUES },
                        label = { Text(stringResource(R.string.credits_tab_issues)) },
                        leadingIcon = if (selectedCategory == CreditsCategory.ISSUES) {
                            { Icon(Icons.Default.BugReport, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                }
                item {
                    FilterChip(
                        selected = selectedCategory == CreditsCategory.PATREON,
                        onClick = { selectedCategory = CreditsCategory.PATREON },
                        label = { Text(stringResource(R.string.credits_tab_patreon)) },
                        leadingIcon = if (selectedCategory == CreditsCategory.PATREON) {
                            { Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                }
            }

            HorizontalDivider()

            // Code & PR Contributors
            if (selectedCategory == CreditsCategory.ALL || selectedCategory == CreditsCategory.CODE) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Code,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Code & Pull Requests",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    for (contributor in CODE_CONTRIBUTORS) {
                        ContributorCard(contributor = contributor, onOpenUrl = { url ->
                            try { uriHandler.openUri(url) } catch (_: Exception) {}
                        })
                    }
                }
            }

            // Bug Hunters & Issue Contributors
            if (selectedCategory == CreditsCategory.ALL || selectedCategory == CreditsCategory.ISSUES) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.BugReport,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Bug Hunters & Feature Pioneers",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    for (contributor in ISSUE_CONTRIBUTORS) {
                        ContributorCard(contributor = contributor, onOpenUrl = { url ->
                            try { uriHandler.openUri(url) } catch (_: Exception) {}
                        })
                    }
                }
            }

            // Patreon Sponsors
            if (selectedCategory == CreditsCategory.ALL || selectedCategory == CreditsCategory.PATREON) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Favorite,
                            contentDescription = null,
                            tint = Color(0xFFFF424D),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Patreon Sponsors",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF424D)
                        )
                    }

                    if (PATREON_SPONSORS.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.credits_patreon_empty_title),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = stringResource(R.string.credits_patreon_empty_text),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Button(
                                    onClick = {
                                        try { uriHandler.openUri(patreonUrl) } catch (_: Exception) {}
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(stringResource(R.string.credits_btn_become_sponsor), fontSize = 13.sp)
                                }
                            }
                        }
                    } else {
                        for (sponsor in PATREON_SPONSORS) {
                            SponsorCard(sponsor = sponsor, onOpenUrl = { url ->
                                try { uriHandler.openUri(url) } catch (_: Exception) {}
                            })
                        }
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
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { isExpanded = !isExpanded }
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // Contributor Avatar Badge
                    Box(
                        modifier = Modifier
                            .size(36.dp)
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
                            fontWeight = FontWeight.Bold,
                            color = if (contributor.isCodeContributor) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            }
                        )
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = contributor.displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                onClick = { onOpenUrl(contributor.githubUrl) }
                            ) {
                                Text(
                                    text = "@${contributor.username}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "${contributor.roleBadge} • ${contributor.contributions.size} ${if (contributor.contributions.size == 1) "item" else "items"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Expandable Contribution Items
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    for (item in contributor.contributions) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onOpenUrl(item.url) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (item.isPr) Color(0xFF8957E5).copy(alpha = 0.2f) else Color(0xFF238636).copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = if (item.isPr) "PR #${item.referenceNumber}" else "#${item.referenceNumber}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = if (item.isPr) Color(0xFFB392F0) else Color(0xFF3FB950),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }

                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )

                                Icon(
                                    Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = "Open in GitHub",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
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
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = sponsor.profileUrl != null) {
                sponsor.profileUrl?.let(onOpenUrl)
            }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFF424D).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Favorite,
                    contentDescription = null,
                    tint = Color(0xFFFF424D),
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = sponsor.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = sponsor.tier,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (sponsor.profileUrl != null) {
                Icon(
                    Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
