package com.devwithzachary.completelinuxinstaller.model.software

import com.devwithzachary.completelinuxinstaller.model.PackageManagerType
import com.devwithzachary.completelinuxinstaller.model.SoftwareCategory

val CliPowerToolsRecipe = SoftwareRecipe(
    id = "cli_power_tools",
    name = "CLI Power Tools",
    description = "tmux multiplexer, htop system monitor, and jq JSON processor",
    category = SoftwareCategory.UTILITIES,
    iconName = "Terminal",
    expectedBinaries = listOf("usr/bin/tmux", "usr/bin/htop", "usr/bin/jq"),
    packagesByManager = mapOf(
        PackageManagerType.APT to listOf("tmux", "htop", "jq"),
        PackageManagerType.DNF to listOf("tmux", "htop", "jq"),
        PackageManagerType.APK to listOf("tmux", "htop", "jq"),
        PackageManagerType.PACMAN to listOf("tmux", "htop", "jq"),
        PackageManagerType.XBPS to listOf("tmux", "htop", "jq")
    ),
    postInstallNotes = "Run 'tmux', 'htop', or 'jq' from your terminal."
)

