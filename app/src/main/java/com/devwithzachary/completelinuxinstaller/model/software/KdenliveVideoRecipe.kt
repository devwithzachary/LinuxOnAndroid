package com.devwithzachary.completelinuxinstaller.model.software

import com.devwithzachary.completelinuxinstaller.model.PackageManagerType
import com.devwithzachary.completelinuxinstaller.model.SoftwareCategory

val KdenliveVideoRecipe = SoftwareRecipe(
    id = "kdenlive_video",
    name = "Kdenlive Video Editor",
    description = "Non-linear multi-track video editor powered by FFmpeg and MLT",
    category = SoftwareCategory.CREATIVE,
    iconName = "Tune",
    expectedBinaries = listOf("usr/bin/kdenlive"),
    isBeta = true,
    packagesByManager = mapOf(
        PackageManagerType.APT to listOf("kdenlive"),
        PackageManagerType.DNF to listOf("kdenlive"),
        PackageManagerType.APK to listOf("kdenlive"),
        PackageManagerType.PACMAN to listOf("kdenlive"),
        PackageManagerType.XBPS to listOf("kdenlive")
    ),
    postInstallNotes = "Open from XFCE Applications -> Multimedia -> Kdenlive."
)

