package com.devwithzachary.completelinuxinstaller.ui.screens.about

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.devwithzachary.completelinuxinstaller.R
import com.devwithzachary.completelinuxinstaller.ui.components.ChangelogItem
import com.devwithzachary.completelinuxinstaller.ui.components.InfoRow

@Composable
fun AboutScreen() {
    val uriHandler = LocalUriHandler.current
    val websiteUrl = stringResource(R.string.website_url)
    val patreonUrl = stringResource(R.string.patreon_url)
    val githubIssuesUrl = stringResource(R.string.github_issues_url)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero / Header Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_logo),
                    contentDescription = stringResource(R.string.app_title),
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(16.dp))
                )

                Text(
                    text = stringResource(R.string.app_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = stringResource(R.string.app_tagline),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    onClick = {
                        try {
                            uriHandler.openUri(websiteUrl)
                        } catch (_: Exception) {}
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(R.string.website_domain),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            textDecoration = TextDecoration.Underline
                        )
                    }
                }
            }
        }

        // Support the Project Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.patreon_support_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = stringResource(R.string.patreon_support_text),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(
                    onClick = {
                        try {
                            uriHandler.openUri(patreonUrl)
                        } catch (_: Exception) {}
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.btn_join_patreon))
                }
            }
        }

        // Alpha Status & Bug Reporting Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.BugReport,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.alpha_card_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }

                Text(
                    text = stringResource(R.string.alpha_card_text),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )

                OutlinedButton(
                    onClick = {
                        try {
                            uriHandler.openUri(githubIssuesUrl)
                        } catch (_: Exception) {}
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.BugReport, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.btn_report_github_issue))
                }
            }
        }

        // Version & Build Metadata Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.about_info_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                InfoRow(label = stringResource(R.string.label_app_version), value = "v1.1.0")
                InfoRow(label = stringResource(R.string.label_build_target), value = "Release (ARM64-v8a)")
                InfoRow(label = stringResource(R.string.label_linux_distro), value = "Ubuntu 26.04 LTS (Noble)")
                InfoRow(label = stringResource(R.string.label_virtualization_engine), value = "PRoot 5.3 (Link2Symlink)")
                InfoRow(label = stringResource(R.string.label_developer), value = "DevWithZachary")
            }
        }

        // Changelog History Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.about_changelog_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                ChangelogItem(
                    version = "v1.1.0",
                    date = "August 10, 2026",
                    initialExpanded = true,
                    highlights = listOf(
                        "1-Tap RootFS Container Backup & Restore: Export and import complete Ubuntu rootfs archives (.tar.gz) with real-time extraction and compression progress bars.",
                        "Terminal Color Theme Packs: Added Dracula, Solarized Dark, Monokai, One Dark, and Cyberpunk ANSI color palettes.",
                        "Custom Palette Creator: Edit Foreground, Background, Cursor, Selection Highlight, and 16 ANSI colors with real-time hex input and interactive terminal preview.",
                        "Terminal Scrollback History: Full drag-scroll support up/down through history without accidental text selection.",
                        "Storage Permission UX: Explanatory permission cards in Setup Wizard & Settings with 1-tap grant buttons for device file access.",
                        "Host File Bind Mounts: Expose host /sdcard, /storage/emulated/0, and ~/Downloads inside the Linux container.",
                        "Screen Space Optimization: Streamlined top header space to maximize terminal canvas view.",
                        "Unified Release Script: Created 1-tap ./build_release.sh generating Play Store AAB and signed GitHub/F-Droid APK."
                    )
                )

                ChangelogItem(
                    version = "v1.0.0",
                    date = "August 6, 2026",
                    initialExpanded = false,
                    highlights = listOf(
                        "Official v1.0.0 release now live on the Google Play Store.",
                        "Vim & TUI Navigation: Added Application Cursor Keys mode (DECCKM) and quick-access Esc / directional keys.",
                        "ANSI Progress Bar Fix: Added DECSTBM scrolling margins so apt upgrade and dpkg progress bars remain fixed at the bottom.",
                        "Terminal Control Fixes: Corrected OSC String Terminator (ESC \\) parsing to eliminate stray prompt backslashes.",
                        "Software Hub & UI Improvements: Optimized custom apt package installer hints and single-line prompt constraints.",
                        "Active Development Notice: Reworded in-app development notice with direct GitHub issue submission."
                    )
                )

                ChangelogItem(
                    version = "v0.0.2",
                    date = "August 5, 2026",
                    initialExpanded = false,
                    highlights = listOf(
                        "Extracted and modularized UI component architecture for cleaner maintainability.",
                        "Centralized all user-facing UI text, button labels, and titles into strings.xml resources.",
                        "Added Alpha notice & direct GitHub issue reporting integration for community feedback.",
                        "Cleaned up repository asset structure and removed default leftover template icons."
                    )
                )

                ChangelogItem(
                    version = "v0.0.1",
                    date = "August 4, 2026",
                    initialExpanded = false,
                    highlights = listOf(
                        "2-step setup wizard: Download -> User account & SSH password configuration.",
                        "Multi-user management: Set Root password, create Sudo users, and change user passwords.",
                        "OpenSSH Server configuration on port 2222 with non-root user password authentication.",
                        "1-Click Developer Stacks (Python 3, Node.js, Android Developer Tools) and Desktop/Server presets.",
                        "Modern Jetpack Compose Material 3 dark UI with interactive PTY terminal emulator.",
                        "Patreon support integration and open-source build configuration."
                    )
                )
            }
        }
    }
}
