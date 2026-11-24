package com.devwithzachary.completelinuxinstaller.model.software

import com.devwithzachary.completelinuxinstaller.model.PackageManagerType
import com.devwithzachary.completelinuxinstaller.model.SoftwareCategory

val ZshShellRecipe = SoftwareRecipe(
    id = "zsh_shell",
    name = "Zsh Shell",
    description = "Extended Bourne shell with powerful completion and theming",
    category = SoftwareCategory.UTILITIES,
    iconName = "Terminal",
    expectedBinaries = listOf("usr/bin/zsh"),
    packagesByManager = mapOf(
        PackageManagerType.APT to listOf("zsh"),
        PackageManagerType.DNF to listOf("zsh"),
        PackageManagerType.APK to listOf("zsh"),
        PackageManagerType.PACMAN to listOf("zsh"),
        PackageManagerType.XBPS to listOf("zsh")
    ),
    postInstallNotes = "Launch anytime by running 'zsh'."
)

