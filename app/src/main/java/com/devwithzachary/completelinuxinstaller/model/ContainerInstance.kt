package com.devwithzachary.completelinuxinstaller.model

import java.io.File

data class ContainerInstance(
    val id: String,
    val name: String,
    val distroId: String,
    val distroName: String,
    val rootDirPath: String,
    val installedAt: Long = System.currentTimeMillis(),
    val isDefault: Boolean = false,
    val storageUsedMb: Long = 0L,
    val defaultUser: String = "root",
    val defaultShell: String = "/bin/bash",
    val packageManager: PackageManagerType = PackageManagerType.APT,
    val colorHex: Long = 0xFFE95420
) {
    val rootDir: File get() = File(rootDirPath)

    val isInstalled: Boolean
        get() {
            if (!rootDir.exists() || !rootDir.isDirectory) return false
            val binSh = File(rootDir, "bin/sh")
            val binBash = File(rootDir, "bin/bash")
            val binAsh = File(rootDir, "bin/ash")
            val usrBinSh = File(rootDir, "usr/bin/sh")
            val usrBinBash = File(rootDir, "usr/bin/bash")
            val osRelease = File(rootDir, "etc/os-release")
            val sbinApk = File(rootDir, "sbin/apk")
            val usrBinPacman = File(rootDir, "usr/bin/pacman")
            val usrBinDnf = File(rootDir, "usr/bin/dnf")
            val hasShell = binSh.exists() || binBash.exists() || binAsh.exists() || usrBinSh.exists() || usrBinBash.exists()
            val hasDistroMarker = osRelease.exists() || sbinApk.exists() || usrBinPacman.exists() || usrBinDnf.exists() || File(rootDir, "usr/bin").exists()
            return hasShell && hasDistroMarker
        }
}
