package com.devwithzachary.completelinuxinstaller.model.software

import com.devwithzachary.completelinuxinstaller.model.PackageManagerType
import com.devwithzachary.completelinuxinstaller.model.SoftwareCategory

val CCppDevRecipe = SoftwareRecipe(
    id = "c_cpp_dev",
    name = "C/C++ Toolchain (Clang)",
    description = "Clang compiler, CMake build system, and GDB debugger",
    category = SoftwareCategory.DEVELOPMENT,
    iconName = "Build",
    expectedBinaries = listOf("usr/bin/clang", "usr/bin/cmake"),
    packagesByManager = mapOf(
        PackageManagerType.APT to listOf("clang", "cmake", "gdb"),
        PackageManagerType.DNF to listOf("clang", "cmake", "gdb"),
        PackageManagerType.APK to listOf("clang", "cmake", "gdb"),
        PackageManagerType.PACMAN to listOf("clang", "cmake", "gdb"),
        PackageManagerType.XBPS to listOf("clang", "cmake", "gdb")
    ),
    postInstallNotes = "Run 'clang --version' or 'cmake --version' to get started."
)

