package com.devwithzachary.completelinuxinstaller.model.software

import com.devwithzachary.completelinuxinstaller.model.PackageManagerType
import com.devwithzachary.completelinuxinstaller.model.SoftwareCategory

val GimpEditorRecipe = SoftwareRecipe(
    id = "gimp_editor",
    name = "GIMP",
    description = "GNU Image Manipulation Program for photo editing and graphics",
    category = SoftwareCategory.CREATIVE,
    iconName = "Palette",
    expectedBinaries = listOf("usr/bin/gimp"),
    isBeta = true,
    packagesByManager = mapOf(
        PackageManagerType.APT to listOf("gimp"),
        PackageManagerType.DNF to listOf("gimp"),
        PackageManagerType.APK to listOf("gimp"),
        PackageManagerType.PACMAN to listOf("gimp"),
        PackageManagerType.XBPS to listOf("gimp")
    ),
    postInstallNotes = "Open from XFCE Applications -> Graphics -> GIMP."
)

