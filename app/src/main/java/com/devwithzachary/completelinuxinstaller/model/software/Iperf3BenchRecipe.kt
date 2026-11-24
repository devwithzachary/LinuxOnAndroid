package com.devwithzachary.completelinuxinstaller.model.software

import com.devwithzachary.completelinuxinstaller.model.PackageManagerType
import com.devwithzachary.completelinuxinstaller.model.SoftwareCategory

val Iperf3BenchRecipe = SoftwareRecipe(
    id = "iperf3_bench",
    name = "iPerf3",
    description = "Network bandwidth and throughput benchmarking tool",
    category = SoftwareCategory.NETWORKING,
    iconName = "Security",
    expectedBinaries = listOf("usr/bin/iperf3"),
    packagesByManager = mapOf(
        PackageManagerType.APT to listOf("iperf3"),
        PackageManagerType.DNF to listOf("iperf3"),
        PackageManagerType.APK to listOf("iperf3"),
        PackageManagerType.PACMAN to listOf("iperf3"),
        PackageManagerType.XBPS to listOf("iperf3")
    ),
    postInstallNotes = "Run 'iperf3 -s' for server mode or 'iperf3 -c <host>' to benchmark."
)

