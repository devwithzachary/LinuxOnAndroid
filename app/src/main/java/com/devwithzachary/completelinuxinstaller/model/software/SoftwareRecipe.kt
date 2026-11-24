package com.devwithzachary.completelinuxinstaller.model.software

import com.devwithzachary.completelinuxinstaller.model.PackageManagerType
import com.devwithzachary.completelinuxinstaller.model.SoftwareCategory
import com.devwithzachary.completelinuxinstaller.model.SoftwarePackage

/**
 * Declarative recipe for a 1-click Software Center package across all supported
 * Linux package managers. Each recipe lives in its own file under model/software/.
 */
data class SoftwareRecipe(
    val id: String,
    val name: String,
    val description: String,
    val category: SoftwareCategory,
    val iconName: String,
    val expectedBinaries: List<String>,
    val packagesByManager: Map<PackageManagerType, List<String>> = emptyMap(),
    val customInstallCommands: Map<PackageManagerType, String> = emptyMap(),
    val customUninstallCommands: Map<PackageManagerType, String> = emptyMap(),
    val preUninstallCommand: String? = null,
    val unsupportedDistros: Set<String> = emptySet(),
    val isBeta: Boolean = false,
    val launchCommand: String? = null,
    val postInstallNotes: String = ""
) {
    fun toSoftwarePackage(): SoftwarePackage = SoftwarePackage(
        id = id,
        name = name,
        category = category,
        description = description,
        iconName = iconName,
        installCommand = SoftwareRecipeCatalog.buildInstallCommand(id, PackageManagerType.APT) ?: "",
        uninstallCommand = SoftwareRecipeCatalog.buildUninstallCommand(id, PackageManagerType.APT) ?: "",
        launchCommand = launchCommand,
        postInstallNotes = postInstallNotes,
        expectedBinaries = expectedBinaries,
        unsupportedDistros = unsupportedDistros,
        isBeta = isBeta
    )
}

object SoftwareRecipeCatalog {
    val recipes: List<SoftwareRecipe> = listOf(
        // Developer Stacks
        CCppDevRecipe,
        GoDevRecipe,
        RustDevRecipe,
        PhpDevRecipe,
        RubyDevRecipe,
        VscodeDesktopRecipe,

        // Databases & Backend
        PostgresqlDbRecipe,
        MariadbDbRecipe,
        RedisServerRecipe,

        // CLI & System Utilities
        CliPowerToolsRecipe,
        SystemMonitorRecipe,
        ZshShellRecipe,
        Aria2DownloaderRecipe,
        RcloneSyncRecipe,

        // Creative & Media
        Blender3dRecipe,
        KdenliveVideoRecipe,
        GimpEditorRecipe,
        FfmpegMediaRecipe,

        // Apps & Productivity
        LibreofficeSuiteRecipe,
        GeanyIdeRecipe,
        FirefoxBrowserRecipe,

        // Networking & Security
        Iperf3BenchRecipe,
        NmapNettoolsRecipe,
        TsharkNetRecipe
    )

    private val recipesById: Map<String, SoftwareRecipe> = recipes.associateBy { it.id }

    fun getById(packageId: String): SoftwareRecipe? = recipesById[packageId]

    fun buildInstallCommand(
        packageId: String,
        packageManager: PackageManagerType,
        distroId: String? = null
    ): String? {
        val recipe = recipesById[packageId] ?: return null
        if (distroId != null && distroId in recipe.unsupportedDistros) return null
        recipe.customInstallCommands[packageManager]?.let { return it }
        val pkgs = recipe.packagesByManager[packageManager]?.takeIf { it.isNotEmpty() } ?: return null
        val pkgList = pkgs.joinToString(" ")
        return when (packageManager) {
            PackageManagerType.APT ->
                "export DEBIAN_FRONTEND=noninteractive && apt-get update && apt-get install -y --no-install-recommends $pkgList"
            PackageManagerType.DNF ->
                "dnf install -y $pkgList"
            PackageManagerType.APK ->
                "apk update && apk add --no-cache $pkgList"
            PackageManagerType.PACMAN ->
                "sed -i 's/^DownloadUser/#DownloadUser/; s/^#DisableSandbox/DisableSandbox/' /etc/pacman.conf 2>/dev/null || true && pacman -Syyu --noconfirm --overwrite \"*\" $pkgList"
            PackageManagerType.XBPS ->
                "xbps-install -Suy && xbps-install -Sy $pkgList"
        }
    }

    fun buildUninstallCommand(
        packageId: String,
        packageManager: PackageManagerType,
        distroId: String? = null
    ): String? {
        val recipe = recipesById[packageId] ?: return null
        if (distroId != null && distroId in recipe.unsupportedDistros) return null
        val coreUninstall = recipe.customUninstallCommands[packageManager] ?: run {
            val pkgs = recipe.packagesByManager[packageManager]?.takeIf { it.isNotEmpty() } ?: return null
            val pkgList = pkgs.joinToString(" ")
            when (packageManager) {
                PackageManagerType.APT ->
                    "_pkgs=\$(dpkg-query -W -f='\${Package} \${Status}\\n' $pkgList 2>/dev/null | awk '/install ok installed/{print \$1}'); [ -n \"\$_pkgs\" ] && DEBIAN_FRONTEND=noninteractive apt-get remove -y --purge \$_pkgs 2>/dev/null || true; DEBIAN_FRONTEND=noninteractive apt-get autoremove -y 2>/dev/null || true"
                PackageManagerType.DNF ->
                    "dnf remove -y $pkgList"
                PackageManagerType.APK ->
                    "apk del $pkgList"
                PackageManagerType.PACMAN ->
                    "pacman -Rns --noconfirm $pkgList 2>/dev/null || for p in ${pkgs.reversed().joinToString(" ")}; do pacman -Qq \"\$p\" >/dev/null 2>&1 && (pacman -Rns --noconfirm \"\$p\" 2>/dev/null || pacman -R --noconfirm \"\$p\" 2>/dev/null || true); done; true"
                PackageManagerType.XBPS ->
                    "xbps-remove -Ry $pkgList 2>/dev/null || for p in ${pkgs.reversed().joinToString(" ")}; do xbps-remove -Ry \"\$p\" 2>/dev/null || xbps-remove -y \"\$p\" 2>/dev/null || true; done; true"
            }
        }
        val pre = recipe.preUninstallCommand?.takeIf { it.isNotBlank() }
        return if (pre != null) "($pre); $coreUninstall" else coreUninstall
    }
}

