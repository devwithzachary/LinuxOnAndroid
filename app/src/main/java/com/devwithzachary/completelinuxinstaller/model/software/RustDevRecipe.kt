package com.devwithzachary.completelinuxinstaller.model.software

import com.devwithzachary.completelinuxinstaller.model.PackageManagerType
import com.devwithzachary.completelinuxinstaller.model.SoftwareCategory

val RustDevRecipe = SoftwareRecipe(
    id = "rust_dev",
    name = "Rust & Cargo",
    description = "Rust systems programming compiler and Cargo package manager",
    category = SoftwareCategory.DEVELOPMENT,
    iconName = "Code",
    expectedBinaries = listOf("usr/bin/rustc", "usr/bin/cargo"),
    packagesByManager = mapOf(
        PackageManagerType.APT to listOf("rustc", "cargo"),
        PackageManagerType.DNF to listOf("rust", "cargo"),
        PackageManagerType.APK to listOf("rust", "cargo"),
        PackageManagerType.PACMAN to listOf("rust"),
        PackageManagerType.XBPS to listOf("rust", "cargo")
    ),
    postInstallNotes = "Run 'rustc --version' and 'cargo --version' to verify."
)

