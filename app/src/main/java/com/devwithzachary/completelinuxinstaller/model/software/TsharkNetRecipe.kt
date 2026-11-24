package com.devwithzachary.completelinuxinstaller.model.software

import com.devwithzachary.completelinuxinstaller.model.PackageManagerType
import com.devwithzachary.completelinuxinstaller.model.SoftwareCategory

val TsharkNetRecipe = SoftwareRecipe(
    id = "tshark_net",
    name = "Wireshark CLI (tshark)",
    description = "Terminal-based network protocol analyzer and packet dissector",
    category = SoftwareCategory.NETWORKING,
    iconName = "Security",
    expectedBinaries = listOf("usr/bin/tshark"),
    packagesByManager = mapOf(
        PackageManagerType.APT to listOf("tshark"),
        PackageManagerType.DNF to listOf("wireshark-cli"),
        PackageManagerType.APK to listOf("tshark"),
        PackageManagerType.PACMAN to listOf("wireshark-cli"),
        PackageManagerType.XBPS to listOf("wireshark")
    ),
    postInstallNotes = "Inspect capture files anytime with 'tshark -r capture.pcap'."
)

