package com.devwithzachary.completelinuxinstaller.model.software

import com.devwithzachary.completelinuxinstaller.model.PackageManagerType
import com.devwithzachary.completelinuxinstaller.model.SoftwareCategory

val SystemMonitorRecipe = SoftwareRecipe(
    id = "system_monitor",
    name = "btop & Fastfetch",
    description = "Modern resource monitor (btop) and fast system info tool (fastfetch)",
    category = SoftwareCategory.UTILITIES,
    iconName = "Terminal",
    expectedBinaries = listOf("usr/bin/btop", "usr/bin/fastfetch"),
    packagesByManager = mapOf(
        PackageManagerType.APT to listOf("btop", "fastfetch"),
        PackageManagerType.DNF to listOf("btop", "fastfetch"),
        PackageManagerType.APK to listOf("btop", "fastfetch"),
        PackageManagerType.PACMAN to listOf("btop", "fastfetch"),
        PackageManagerType.XBPS to listOf("btop", "fastfetch")
    ),
    postInstallNotes = "Run 'fastfetch' for system info or 'btop' for live CPU/RAM monitoring."
)

