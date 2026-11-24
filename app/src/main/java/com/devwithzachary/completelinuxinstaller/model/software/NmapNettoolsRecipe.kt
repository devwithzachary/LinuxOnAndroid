package com.devwithzachary.completelinuxinstaller.model.software

import com.devwithzachary.completelinuxinstaller.model.PackageManagerType
import com.devwithzachary.completelinuxinstaller.model.SoftwareCategory

val NmapNettoolsRecipe = SoftwareRecipe(
    id = "nmap_nettools",
    name = "Nmap",
    description = "Network discovery, port scanning, and security auditing utility",
    category = SoftwareCategory.NETWORKING,
    iconName = "Security",
    expectedBinaries = listOf("usr/bin/nmap"),
    packagesByManager = mapOf(
        PackageManagerType.APT to listOf("nmap"),
        PackageManagerType.DNF to listOf("nmap"),
        PackageManagerType.APK to listOf("nmap"),
        PackageManagerType.PACMAN to listOf("nmap")
    ),
    unsupportedDistros = setOf("void_rolling"),
    postInstallNotes = "Run 'nmap -sT <host>' (use TCP connect scans in PRoot without raw sockets)."
)

