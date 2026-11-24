package com.devwithzachary.completelinuxinstaller.model.software

import com.devwithzachary.completelinuxinstaller.model.PackageManagerType
import com.devwithzachary.completelinuxinstaller.model.SoftwareCategory

val RedisServerRecipe = SoftwareRecipe(
    id = "redis_server",
    name = "Redis",
    description = "In-memory key-value data store, cache, and message broker",
    category = SoftwareCategory.WEB_SERVER,
    iconName = "Storage",
    expectedBinaries = listOf("usr/bin/redis-server"),
    packagesByManager = mapOf(
        PackageManagerType.APT to listOf("redis-server"),
        PackageManagerType.DNF to listOf("redis"),
        PackageManagerType.APK to listOf("redis"),
        PackageManagerType.PACMAN to listOf("redis"),
        PackageManagerType.XBPS to listOf("redis")
    ),
    customUninstallCommands = mapOf(
        PackageManagerType.DNF to "dnf remove -y redis valkey valkey-compat-redis 2>/dev/null || true",
        PackageManagerType.PACMAN to "for p in redis valkey; do pacman -Qq \"\$p\" >/dev/null 2>&1 && (pacman -Rns --noconfirm \"\$p\" 2>/dev/null || pacman -R --noconfirm \"\$p\" 2>/dev/null || true); done; true"
    ),
    preUninstallCommand = "pkill -x redis-server 2>/dev/null || killall -9 redis-server 2>/dev/null || true",
    launchCommand = "redis-server --daemonize yes",
    postInstallNotes = "Start with 'redis-server --daemonize yes' and test via 'redis-cli ping'."
)

