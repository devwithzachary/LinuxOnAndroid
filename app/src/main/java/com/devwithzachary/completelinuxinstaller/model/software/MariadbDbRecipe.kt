package com.devwithzachary.completelinuxinstaller.model.software

import com.devwithzachary.completelinuxinstaller.model.PackageManagerType
import com.devwithzachary.completelinuxinstaller.model.SoftwareCategory

val MariadbDbRecipe = SoftwareRecipe(
    id = "mariadb_db",
    name = "MariaDB (MySQL)",
    description = "Open-source MySQL-compatible relational database server and client",
    category = SoftwareCategory.WEB_SERVER,
    iconName = "Storage",
    expectedBinaries = listOf("usr/bin/mariadb"),
    packagesByManager = mapOf(
        PackageManagerType.APT to listOf("mariadb-server", "mariadb-client"),
        PackageManagerType.DNF to listOf("mariadb-server", "mariadb"),
        PackageManagerType.APK to listOf("mariadb", "mariadb-client"),
        PackageManagerType.PACMAN to listOf("mariadb"),
        PackageManagerType.XBPS to listOf("mariadb", "mariadb-client")
    ),
    preUninstallCommand = "pkill -x mariadbd 2>/dev/null || pkill -x mysqld 2>/dev/null || true",
    postInstallNotes = "Run 'mariadb --version' or initialize with 'mariadb-install-db'."
)

