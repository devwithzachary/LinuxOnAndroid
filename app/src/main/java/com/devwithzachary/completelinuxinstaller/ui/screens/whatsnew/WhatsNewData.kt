package com.devwithzachary.completelinuxinstaller.ui.screens.whatsnew

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DesktopWindows
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.ui.graphics.vector.ImageVector

data class WhatsNewFeature(
    val badge: String,
    val title: String,
    val summary: String,
    val stepsToFind: List<String>,
    val icon: ImageVector,
    val isBetaBadge: Boolean = false
)

data class WhatsNewRelease(
    val versionKey: String,
    val displayVersion: String,
    val headline: String,
    val subtitle: String,
    val features: List<WhatsNewFeature>
)

object WhatsNewLogic {
    fun shouldAutoShowWhatsNew(
        isEnabled: Boolean,
        lastSeenVersion: String,
        currentVersionKey: String = CURRENT_WHATS_NEW_RELEASE.versionKey
    ): Boolean {
        return isEnabled && lastSeenVersion != currentVersionKey
    }
}

val CURRENT_WHATS_NEW_RELEASE: WhatsNewRelease = WhatsNewRelease(
    versionKey = "1.9.0",
    displayVersion = "v1.9.0",
    headline = "33 1-Click Packages, Beta GUI Apps & More",
    subtitle = "Welcome to LinuxOnAndroid 1.9.0! Here are the key new features in this release and where to find them in the app.",
    features = listOf(
        WhatsNewFeature(
            badge = "NEW CATALOG",
            title = "Expanded Software Center (33 Packages)",
            summary = "Added 24 new 1-click packages across all 7 distributions, including C/C++, Go, Rust, PHP, Ruby, PostgreSQL, MariaDB, Redis, btop/Fastfetch, tmux/htop/jq, Zsh, aria2, Rclone, FFmpeg, iPerf3, Nmap, and tshark, plus instant search and status filters.",
            stepsToFind = listOf(
                "Open any container from the Dashboard.",
                "Switch to the Software tab.",
                "Use the search bar, status chips (All/Installed/Available), or category tabs to browse and install."
            ),
            icon = Icons.Rounded.Apps
        ),
        WhatsNewFeature(
            badge = "BETA",
            title = "Desktop GUI Applications (Beta)",
            summary = "Install VS Code (Desktop), Blender, Kdenlive, GIMP, LibreOffice, Geany IDE, and Firefox with 1 click. These GUI apps currently use software rendering until GPU acceleration arrives, so please report any bugs on GitHub or Discord.",
            stepsToFind = listOf(
                "Open a container's Software tab and look for packages with the amber BETA badge.",
                "Install XFCE Desktop first if you have not already set it up.",
                "Launch the Desktop tab to open your installed GUI apps from the XFCE Applications menu."
            ),
            icon = Icons.Rounded.DesktopWindows,
            isBetaBadge = true
        ),
        WhatsNewFeature(
            badge = "NEW",
            title = "Full 1-Click Package Uninstall",
            summary = "Cleanly uninstall any 1-click Software Center package across all 7 distros. Running services are stopped automatically and generated wrappers are removed with a confirmation prompt and live logs.",
            stepsToFind = listOf(
                "Open a container's Software tab.",
                "Find any Installed package card.",
                "Tap the trash icon on the right side of the card and confirm."
            ),
            icon = Icons.Rounded.DeleteOutline
        ),
        WhatsNewFeature(
            badge = "UPGRADED",
            title = "Updated Distros & Desktop Stability",
            summary = "Upgraded base images to Debian 13 (Trixie), Fedora 45, and Alpine 3.24.4, plus PRoot /dev/shm support, Debian/Ubuntu systemd-sysusers fixes, and Android Phantom Process Killer mitigations for XFCE.",
            stepsToFind = listOf(
                "Tap + New Container on the Dashboard to install the latest Debian 13, Fedora 45, or Alpine 3.24.4.",
                "Open the Desktop tab on existing containers to automatically apply the lighter XFCE startup configuration."
            ),
            icon = Icons.Rounded.RocketLaunch
        )
    )
)
