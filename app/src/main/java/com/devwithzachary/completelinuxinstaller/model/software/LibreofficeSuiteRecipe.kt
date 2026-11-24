package com.devwithzachary.completelinuxinstaller.model.software

import com.devwithzachary.completelinuxinstaller.model.PackageManagerType
import com.devwithzachary.completelinuxinstaller.model.SoftwareCategory

val LibreofficeSuiteRecipe = SoftwareRecipe(
    id = "libreoffice_suite",
    name = "LibreOffice",
    description = "Full desktop office suite for documents, spreadsheets, and slides",
    category = SoftwareCategory.PRODUCTIVITY,
    iconName = "Description",
    expectedBinaries = listOf("usr/bin/libreoffice"),
    isBeta = true,
    packagesByManager = mapOf(
        PackageManagerType.APT to listOf("libreoffice"),
        PackageManagerType.DNF to listOf("libreoffice"),
        PackageManagerType.APK to listOf("libreoffice"),
        PackageManagerType.PACMAN to listOf("libreoffice-fresh"),
        PackageManagerType.XBPS to listOf("libreoffice")
    ),
    postInstallNotes = "Open Writer, Calc, or Impress from the XFCE Applications menu."
)

