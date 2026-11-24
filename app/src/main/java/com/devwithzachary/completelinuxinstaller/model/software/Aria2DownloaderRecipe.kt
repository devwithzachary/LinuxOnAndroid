package com.devwithzachary.completelinuxinstaller.model.software

import com.devwithzachary.completelinuxinstaller.model.PackageManagerType
import com.devwithzachary.completelinuxinstaller.model.SoftwareCategory

val Aria2DownloaderRecipe = SoftwareRecipe(
    id = "aria2_downloader",
    name = "aria2 Downloader",
    description = "Lightweight multi-protocol, multi-connection CLI download utility",
    category = SoftwareCategory.UTILITIES,
    iconName = "Public",
    expectedBinaries = listOf("usr/bin/aria2c"),
    packagesByManager = mapOf(
        PackageManagerType.APT to listOf("aria2"),
        PackageManagerType.DNF to listOf("aria2"),
        PackageManagerType.APK to listOf("aria2"),
        PackageManagerType.PACMAN to listOf("aria2"),
        PackageManagerType.XBPS to listOf("aria2")
    ),
    postInstallNotes = "Run 'aria2c -x 4 <url>' for fast multi-connection downloads."
)

