package com.devwithzachary.completelinuxinstaller.model.software

import com.devwithzachary.completelinuxinstaller.model.PackageManagerType
import com.devwithzachary.completelinuxinstaller.model.SoftwareCategory

val RcloneSyncRecipe = SoftwareRecipe(
    id = "rclone_sync",
    name = "Rclone",
    description = "Sync files and directories to cloud storage providers",
    category = SoftwareCategory.UTILITIES,
    iconName = "Public",
    expectedBinaries = listOf("usr/bin/rclone"),
    packagesByManager = mapOf(
        PackageManagerType.APT to listOf("rclone"),
        PackageManagerType.DNF to listOf("rclone"),
        PackageManagerType.APK to listOf("rclone"),
        PackageManagerType.PACMAN to listOf("rclone"),
        PackageManagerType.XBPS to listOf("rclone")
    ),
    customInstallCommands = mapOf(
        PackageManagerType.APT to "export DEBIAN_FRONTEND=noninteractive && apt-get update && (apt-get install -y --no-install-recommends rclone || (apt-get install -y --no-install-recommends curl unzip ca-certificates && curl -fsSL https://rclone.org/install.sh | bash))"
    ),
    customUninstallCommands = mapOf(
        PackageManagerType.APT to "export DEBIAN_FRONTEND=noninteractive && (apt-get remove -y rclone && apt-get autoremove -y) 2>/dev/null; rm -f /usr/bin/rclone /usr/local/bin/rclone 2>/dev/null || true"
    ),
    postInstallNotes = "Run 'rclone config' to link a cloud remote."
)

