package com.devwithzachary.completelinuxinstaller.model.software

import com.devwithzachary.completelinuxinstaller.model.PackageManagerType
import com.devwithzachary.completelinuxinstaller.model.SoftwareCategory

val PhpDevRecipe = SoftwareRecipe(
    id = "php_dev",
    name = "PHP CLI",
    description = "PHP command-line runtime for web backends and scripting",
    category = SoftwareCategory.DEVELOPMENT,
    iconName = "Code",
    expectedBinaries = listOf("usr/bin/php"),
    packagesByManager = mapOf(
        PackageManagerType.APT to listOf("php-cli"),
        PackageManagerType.DNF to listOf("php-cli"),
        PackageManagerType.APK to listOf("php"),
        PackageManagerType.PACMAN to listOf("php"),
        PackageManagerType.XBPS to listOf("php")
    ),
    postInstallNotes = "Run 'php -v' or 'php -S 0.0.0.0:8080' for a local server."
)

