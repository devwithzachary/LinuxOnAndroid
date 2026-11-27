package com.devwithzachary.completelinuxinstaller.ui.screens.whatsnew

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Palette
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
    versionKey = "1.9.1",
    displayVersion = "v1.9.1",
    headline = "Android Green Theme, Cleaner Navigation & Key Fixes",
    subtitle = "Welcome to LinuxOnAndroid 1.9.1! Here is what changed and what was fixed in this release.",
    features = listOf(
        WhatsNewFeature(
            badge = "UPDATED",
            title = "Android Green Classic Theme & Terminal Palette",
            summary = "Updated the LinuxOnAndroid Classic app theme and the default LinuxOnAndroid terminal color scheme to an Android green and dark forest-charcoal palette in both light and dark modes.",
            stepsToFind = listOf(
                "Open Settings -> Appearance to switch between System, Light, and Dark modes.",
                "Turn off Dynamic Color on Android 12+ to use the updated Android green Classic palette.",
                "Select LinuxOnAndroid under Terminal Theme to preview the forest-charcoal palette."
            ),
            icon = Icons.Rounded.Palette
        ),
        WhatsNewFeature(
            badge = "UPDATED",
            title = "Cleaner Navigation & Dashboard Details",
            summary = "Moved About into Settings so the bottom navigation bar stays focused on four tabs (Dashboard, Terminal, Desktop, Settings), and updated Dashboard container subtitles to show architecture and runtime info instead of repeating the distro name.",
            stepsToFind = listOf(
                "Use the 4-tab navigation bar to switch between Dashboard, Terminal, Desktop, and Settings.",
                "Scroll to the bottom of Settings and tap Complete Linux Installer to open About, Credits, and Changelog.",
                "Check container cards on the Dashboard to see architecture and runtime subtitles."
            ),
            icon = Icons.Rounded.Dashboard
        ),
        WhatsNewFeature(
            badge = "FIXED",
            title = "Ubuntu 26.04 Package Upgrade Fix",
            summary = "Fixed package upgrade and installation errors on Ubuntu 26.04 where core shell commands like mkdir and basename could fail during updates.",
            stepsToFind = listOf(
                "Open an existing or new Ubuntu 26.04 container.",
                "Install packages from the Software tab or run apt-get update && apt-get upgrade in the Terminal."
            ),
            icon = Icons.Rounded.Build
        ),
        WhatsNewFeature(
            badge = "FIXED",
            title = "Desktop Diagnostics & Startup Hardening",
            summary = "Graphical desktop sessions now save startup logs and include recent output in Settings debug reports, and the app stays on internal storage to prevent terminal startup linker errors.",
            stepsToFind = listOf(
                "Open the Desktop tab to start a graphical session and inspect ~/.vnc/vncserver_1.log if needed.",
                "Open Settings -> Diagnostics -> Generate Debug Report to include recent desktop logs."
            ),
            icon = Icons.Rounded.BugReport
        )
    )
)
