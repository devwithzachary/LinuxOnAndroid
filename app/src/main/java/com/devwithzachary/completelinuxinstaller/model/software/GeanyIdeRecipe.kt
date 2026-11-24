package com.devwithzachary.completelinuxinstaller.model.software

import com.devwithzachary.completelinuxinstaller.model.PackageManagerType
import com.devwithzachary.completelinuxinstaller.model.SoftwareCategory

val GeanyIdeRecipe = SoftwareRecipe(
    id = "geany_ide",
    name = "Geany IDE",
    description = "Fast and lightweight GTK desktop text editor and IDE",
    category = SoftwareCategory.PRODUCTIVITY,
    iconName = "Description",
    expectedBinaries = listOf("usr/bin/geany"),
    isBeta = true,
    packagesByManager = mapOf(
        PackageManagerType.APT to listOf("geany"),
        PackageManagerType.DNF to listOf("geany"),
        PackageManagerType.APK to listOf("geany"),
        PackageManagerType.PACMAN to listOf("geany"),
        PackageManagerType.XBPS to listOf("geany")
    ),
    postInstallNotes = "Open from XFCE Applications -> Development -> Geany."
)

