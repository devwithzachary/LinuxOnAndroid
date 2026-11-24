package com.devwithzachary.completelinuxinstaller.model.software

import com.devwithzachary.completelinuxinstaller.model.PackageManagerType
import com.devwithzachary.completelinuxinstaller.model.SoftwareCategory

val PostgresqlDbRecipe = SoftwareRecipe(
    id = "postgresql_db",
    name = "PostgreSQL",
    description = "Advanced open-source relational database client and server",
    category = SoftwareCategory.WEB_SERVER,
    iconName = "Storage",
    expectedBinaries = listOf("usr/bin/psql"),
    packagesByManager = mapOf(
        PackageManagerType.APT to listOf("postgresql"),
        PackageManagerType.DNF to listOf("postgresql-server", "postgresql"),
        PackageManagerType.APK to listOf("postgresql"),
        PackageManagerType.PACMAN to listOf("postgresql"),
        PackageManagerType.XBPS to listOf("postgresql", "postgresql-client")
    ),
    preUninstallCommand = "pkill -x postgres 2>/dev/null || killall -9 postgres 2>/dev/null || true",
    postInstallNotes = "Initialize a cluster with 'initdb' or connect using 'psql'."
)

