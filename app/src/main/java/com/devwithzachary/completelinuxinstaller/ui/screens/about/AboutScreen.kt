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
    val buymeacoffeeUrl = stringResource(R.string.buymeacoffee_url)
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            try {
                                uriHandler.openUri(patreonUrl)
                            } catch (_: Exception) {}
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.btn_join_patreon))
                    }

                    Button(
                        onClick = {
                            try {
                                uriHandler.openUri(buymeacoffeeUrl)
                            } catch (_: Exception) {}
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.LocalCafe, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.btn_buy_me_a_coffee))
                    }
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

                InfoRow(label = stringResource(R.string.label_app_version), value = "v1.3.0")
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
                    version = "v1.3.0",
                    date = "In Development",
                    initialExpanded = true,
                    highlights = listOf(
                        "Expanded Android 6.0+ (API 23) Support: Lowered minimum supported SDK to Android 6.0 (Marshmallow), broadening device support to 2015+ hardware without sacrificing modern Jetpack Compose Material 3 features.",
                        "UBUNTU_CODENAME & VERSION_CODENAME Exports: Configured system-wide codename environment variables in /etc/os-release, /etc/environment, /etc/lsb-release, and /etc/profile.d/, simplifying Docker installations and third-party repository setup."
                    )
                )

                ChangelogItem(
                    version = "v1.2.0",
                    date = "August 14, 2026",
                    initialExpanded = false,
                    highlights = listOf(
                        "RootFS Incremental Upgrades: Track container build versions and apply incremental improvements, network configurations, and PAM fixes to existing containers with 1-tap without data loss.",
                        "Manifest-Based 1-Click Package Tracking: Authoritative package installation and version tracking via /etc/linuxonandroid_packages, eliminating false-positive binary detections.",
                        "1-Click Software Upgrades: Added 1-tap package upgrade actions in the Software & Packages Hub with balanced 3-way action layout (Upgrade, Start, View Logs).",
                        "Custom DNS Server Configuration: Manage /etc/resolv.conf directly from Settings with quick presets (Google, Cloudflare, Quad9, AdGuard, OpenDNS) and custom IP input.",
                        "Collapsible Settings & Category Tabs: Overhauled Settings with collapsible cards, Category filter tabs (Container, Network, Terminal, Security, Storage), and Expand/Collapse All toggle.",
                        "Terminal Font Selector & CyberGlyphs: Added horizontal scroll chip picker for font families, streamlined developer monospace options, and introduced playful CyberGlyphs symbol typography.",
                        "Performance & Speed Optimizations: Shifted container filesystem inspection to background IO dispatchers, upgraded download/archive streaming to 64KB buffers, and modernized Jetpack Compose APIs."
                    )
                )

                ChangelogItem(
                    version = "v1.1.1",
                    date = "August 12, 2026",
                    initialExpanded = false,
                    highlights = listOf(
                        "Default Terminal User Selector: Added setting to configure default terminal user session, defaulting to your custom regular user.",
                        "Terminal Font Customization: Added custom font size selector and monospace font family picker (Roboto Mono, Fira Code, Source Code Pro, JetBrains Mono).",
                        "Terminal Hotkey Editor & Reordering: Customize, add, remove, and reorder quick hotkey bar buttons with special key combination handling.",
                        "Custom User First-Launch Setup: Setup wizard now creates your custom username directly on installation, cleaning up generic defaults.",
                        "Native Interactive Ubuntu Sudo: Restored standard Ubuntu /usr/bin/sudo with user-space setuid syscall interception (PROOT_FORCE_SETID=1) and PAM permit rules, supporting interactive prompts (sudo apt upgrade, sudo nano, sudo su).",
                        "Soft Keyboard Autocorrect & IME Fix: Suppressed IME word prediction and autocorrect (Gboard/SwiftKey) to prevent CLI terms like 'usr' from changing to 'use', while resolving double-typed letters.",
                        "Sudo Ownership & Setuid Safeguards: Fixed package installer pre-flight hooks to prevent recursive chmod 777 over /usr and /etc, preserving 4755 setuid permissions and 0:0 root ownership.",
                        "SSH Service & Remote Login: Configured OpenSSH Server daemon (Port 2222) with password authentication and SFTP subsystem support.",
                        "Play Store Scoped Storage Compliance: Full Play Store policy compliance with Android Scoped Storage and Storage Access Framework (SAF)."
                    )
                )

                ChangelogItem(
                    version = "v1.1.0",
                    date = "August 10, 2026",
                    initialExpanded = false,
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
