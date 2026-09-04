package com.devwithzachary.completelinuxinstaller.ui.screens.about

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.devwithzachary.completelinuxinstaller.R
import com.devwithzachary.completelinuxinstaller.ui.components.ChangelogItem

data class ReleaseChangelog(
    val version: String,
    val date: String,
    val initialExpanded: Boolean = false,
    val highlights: List<String>
)

val APP_CHANGELOG_HISTORY: List<ReleaseChangelog> = listOf(
    ReleaseChangelog(
        version = "v1.5.0",
        date = "September 1, 2026",
        initialExpanded = true,
        highlights = listOf(
            "Tabbed Multi-Window Terminal: Open, run, switch, rename, and manage multiple concurrent interactive terminal tabs within your Linux rootfs environments.",
            "Multi-Rootfs Container Management: Install, run, and manage multiple distinct Linux distributions side-by-side on disk with per-container storage tracking, custom container names, and dynamic shell hostnames (e.g. user@ContainerName).",
            "Multi-Distro Setup Wizard: Extended setup catalog allowing you to choose and install Debian 12, Alpine Linux 3.21 (ultra-fast ~10MB rootfs), Arch Linux ARM, Kali Linux CLI Tools, Void Linux, or Ubuntu 26.04 LTS.",
            "Interactive Dashboard & Per-Container Overview: Real-time container cards displaying installed distributions, RAM and disk usage, plus full per-container system overview screens.",
            "Filesystem & Software Hub Hardening: Safe rootfs symlink and file extraction without recursive deletion, robust cross-distro user account provisioning (/etc/passwd, /etc/group), high-capacity archive extractor for large rootfs distributions (Arch Linux ARM), automated setup wizard reset, accurate per-container storage tracking across hard links, and tailored 1-click software package scripts for all supported distributions.",
            "Multi-Container Debug Diagnostics: Enhanced the system debug report generator to scan all installed rootfs containers, accurately calculating real-time disk sizes and checking required filesystem files.",
            "Ubuntu 26.04 Coreutils & Package Installation Fix: Fixed rust-coreutils binary execution permissions (/usr/lib/cargo/bin/coreutils) and tar hard link resolution preventing python3.14-minimal preinst maintainer script subprocess failures (exit status 127) during 1-click package installs."
        )
    ),
    ReleaseChangelog(
        version = "v1.4.0",
        date = "August 28, 2026",
        initialExpanded = false,
        highlights = listOf(
            "GitHub Release Update Checker & Play Store Migration Guide: Automated in-app GitHub release checks notifying users of cutting-edge updates, with full step-by-step guidance on migrating from Google Play Store builds to GitHub developer-signed APKs.",
            "Real-Time Container Dashboard & System Monitor: Live visual RAM and storage gauges, real-time process table (ps aux) with 1-tap kill confirmation, and active TCP network port listener with 1-tap browser launcher.",
            "Terminal Native Text Selection & Copy/Paste: Long-press to select words with draggable teardrop start/end handles, 1-tap floating Copy/Select All toolbar, pinned keyboard PASTE button, and buttery smooth scrolling.",
            "System Bars & Light Theme Contrast: Dynamic edge-to-edge status bar and navigation bar contrast fixing light-on-light text and providing seamless terminal immersion.",
            "Diagnostics & System Debug Report Generator (PR #37): Added comprehensive diagnostics reporting in Settings, collecting device specs, Android version, container status, storage analysis, memory breakdown, and rootfs binary integrity checks with 1-tap copy/sharing.",
            "Startup Sanity Check & Slow-Mode Escape Hatch (PR #35): Introduced startup watchdog monitoring initialization progress with slow-load warning alerts and instant 'Retry' and 'Continue anyway' recovery options if filesystem checks take longer than expected.",
            "Setup Wizard Screen Keep-Alive (PR #36): Keeps display awake during initial container download and filesystem extraction in the Setup Wizard, preventing sleep timeouts during long downloads.",
            "Community Credits & Contributors Recognition: Updated in-app Credits card recognizing PRs #35, #36, and #37 contributed by @sleepy-snowflake, and welcoming Patreon supporters."
        )
    ),
    ReleaseChangelog(
        version = "v1.3.0",
        date = "August 25, 2026",
        initialExpanded = false,
        highlights = listOf(
            "Persistent Foreground Service & CPU WakeLock: Runs PRoot inside an Android Foreground Service with CPU WakeLock, preventing Doze and Phantom Process Killer from terminating long compiles, SSH, or terminal sessions when minimized.",
            "Full Container Process-Tree RAM Monitoring: Notification shade calculates true Resident Set Size (RSS) memory across all container child processes (PRoot, Xtigervnc, XFCE, D-Bus, sshd, compilers) via /proc/*/statm.",
            "Instant Startup & Asynchronous RootFS Storage Scanning: Replaced blocking recursive Java File.listFiles() disk scans with fast native du -sk (160ms vs 10+s) and non-blocking background calculation, eliminating splash screen lag.",
            "Notification Permission Rationale & Wizard Card: Replaced unprompted startup permission popup with an explicit card in the Setup Wizard on Android 13+ and an informative in-app rationale dialog for existing users.",
            "Keep Screen On While Terminal Is Active (PR #34 / Issue #25): Added display keep-alive setting tied to the terminal view, preventing the phone screen from sleeping mid-session during long builds or tailing logs.",
            "XFCE 4 Desktop & TigerVNC Subsystem: Fixed graphical desktop session startup with direct component launch, user-space Bubblewrap sandbox bypass for GTK image loaders, automatic password generation (vncpasswd -f), and -UseBlacklist=0 rate-limit prevention for RealVNC Viewer.",
            "Terminal Monospace Font Engine (Issue #31 / PR #33): Bundled authentic JetBrains Mono and Ubuntu Mono font assets directly in APK with license text (OFL 1.1 / Ubuntu Font License), fixing character gap issues on Samsung One UI and custom OEM system fonts.",
            "Password Manager & Autocorrect Fix: Configured developer URI input mode without password classification, preventing annoying 1Password/Bitwarden autofill popups while strictly disabling IME autocorrect (e.g. 'ls' -> 'L\'s').",
            "Permanent CTRL & ALT Modifier Keys: Pinned dedicated CTRL and ALT toggle keys at the start of the hotkey ribbon that latch for the next keypress (e.g. CTRL + L for clear screen), simplifying terminal shortcuts.",
            "German Localization (PR #27): Added full German language support across all screens, setup wizard, settings, dialogs, and software hub (contributed by @bkodenkt).",
            "Community Credits & Contributors Section: Added dedicated Credits section to the About screen celebrating code/translation contributors (PRs #27, #33, #34 by @bkodenkt and @sleepy-snowflake), bug hunters & feature pioneers (issues #7, #8, #9, #10, #11, #12, #13, #14, #16, #19, #21, #25, #31), and Patreon sponsors.",
            "Official Discord Community: Added direct 1-tap invite link to the official LinuxOnAndroid Discord community in the About screen.",
            "Expanded Android 6.0+ (API 23) Support: Lowered minimum supported SDK to Android 6.0 (Marshmallow), broadening device support to 2015+ hardware without sacrificing modern Jetpack Compose Material 3 features.",
            "UBUNTU_CODENAME & VERSION_CODENAME Exports (Issue #19): Configured system-wide codename environment variables in /etc/os-release, /etc/environment, /etc/lsb-release, and /etc/profile.d/, simplifying Docker installations and third-party repository setup."
        )
    ),
    ReleaseChangelog(
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
    ),
    ReleaseChangelog(
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
    ),
    ReleaseChangelog(
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
    ),
    ReleaseChangelog(
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
    ),
    ReleaseChangelog(
        version = "v0.0.2",
        date = "August 5, 2026",
        initialExpanded = false,
        highlights = listOf(
            "Extracted and modularized UI component architecture for cleaner maintainability.",
            "Centralized all user-facing UI text, button labels, and titles into strings.xml resources.",
            "Added Alpha notice & direct GitHub issue reporting integration for community feedback.",
            "Cleaned up repository asset structure and removed default leftover template icons."
        )
    ),
    ReleaseChangelog(
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
)

@Composable
fun AboutChangelogSection(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
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

            for (entry in APP_CHANGELOG_HISTORY) {
                ChangelogItem(
                    version = entry.version,
                    date = entry.date,
                    initialExpanded = entry.initialExpanded,
                    highlights = entry.highlights
                )
            }
        }
    }
}
