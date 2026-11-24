package com.devwithzachary.completelinuxinstaller.model.software

import com.devwithzachary.completelinuxinstaller.model.PackageManagerType
import com.devwithzachary.completelinuxinstaller.model.SoftwareCategory

val GoDevRecipe = SoftwareRecipe(
    id = "go_dev",
    name = "Go Toolchain",
    description = "Official Go compiler, module tools, and standard library",
    category = SoftwareCategory.DEVELOPMENT,
    iconName = "Code",
    expectedBinaries = listOf("usr/bin/go"),
    packagesByManager = mapOf(
        PackageManagerType.APT to listOf("golang-go"),
        PackageManagerType.DNF to listOf("golang"),
        PackageManagerType.APK to listOf("go"),
        PackageManagerType.PACMAN to listOf("go"),
        PackageManagerType.XBPS to listOf("go")
    ),
    postInstallNotes = "Run 'go version' or 'go mod init' in your project directory."
)

