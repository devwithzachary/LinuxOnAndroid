package com.devwithzachary.completelinuxinstaller.model.software

import com.devwithzachary.completelinuxinstaller.model.PackageManagerType
import com.devwithzachary.completelinuxinstaller.model.SoftwareCategory

val RubyDevRecipe = SoftwareRecipe(
    id = "ruby_dev",
    name = "Ruby",
    description = "Ruby interpreter and Gem package ecosystem for scripting and web apps",
    category = SoftwareCategory.DEVELOPMENT,
    iconName = "Code",
    expectedBinaries = listOf("usr/bin/ruby"),
    packagesByManager = mapOf(
        PackageManagerType.APT to listOf("ruby"),
        PackageManagerType.DNF to listOf("ruby"),
        PackageManagerType.APK to listOf("ruby"),
        PackageManagerType.PACMAN to listOf("ruby"),
        PackageManagerType.XBPS to listOf("ruby")
    ),
    postInstallNotes = "Run 'ruby -v' or 'irb' to start an interactive Ruby session."
)

