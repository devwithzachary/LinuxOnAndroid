package com.devwithzachary.completelinuxinstaller.model

/**
 * Declarative recipe for a 1-click Software Center package across all supported
 * Linux package managers.
 */
data class SoftwareRecipe(
    val id: String,
    val name: String,
    val description: String,
    val category: SoftwareCategory,
    val iconName: String,
    val expectedBinaries: List<String>,
    val packagesByManager: Map<PackageManagerType, List<String>>,
    val customInstallCommands: Map<PackageManagerType, String> = emptyMap(),
    val customUninstallCommands: Map<PackageManagerType, String> = emptyMap(),
    val launchCommand: String? = null,
    val postInstallNotes: String = ""
) {
    fun toSoftwarePackage(): SoftwarePackage = SoftwarePackage(
        id = id,
        name = name,
        description = description,
        category = category,
        iconName = iconName,
        expectedBinaries = expectedBinaries,
        installCommand = SoftwareRecipeCatalog.buildInstallCommand(id, PackageManagerType.APT) ?: "",
        uninstallCommand = SoftwareRecipeCatalog.buildUninstallCommand(id, PackageManagerType.APT) ?: "",
        launchCommand = launchCommand,
        postInstallNotes = postInstallNotes
    )
}

object SoftwareRecipeCatalog {
    val recipes: List<SoftwareRecipe> = listOf(
        SoftwareRecipe(
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
        ),
        SoftwareRecipe(
            id = "go_dev",
            name = "Go Toolchain",
            description = "Official Go compiler, module tools, and standard library",
            category = SoftwareCategory.DEVELOPMENT,
            iconName = "Code",
            expectedBinaries = listOf("usr/bin/go"),
            packagesByManager = mapOf(
                PackageManagerType.APT to listOf("golang-go"),
                PackageManagerType.DNF to listOf("golang"),
                PackageManagerType.APK to listOf("go"),
                PackageManagerType.PACMAN to listOf("go"),
                PackageManagerType.XBPS to listOf("go")
            ),
            postInstallNotes = "Run 'go version' or 'go init' in your project directory."
        ),
        SoftwareRecipe(
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
        ),
        SoftwareRecipe(
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
        ),
        SoftwareRecipe(
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
            postInstallNotes = "Initialize a cluster with 'initdb' or connect using 'psql'."
        ),
        SoftwareRecipe(
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
            launchCommand = "redis-server --daemonize yes",
            postInstallNotes = "Start with 'redis-server --daemonize yes' and test via 'redis-cli ping'."
        ),
        SoftwareRecipe(
            id = "cli_power_tools",
            name = "CLI Power Tools",
            description = "tmux multiplexer, htop system monitor, and jq JSON processor",
            category = SoftwareCategory.UTILITIES,
            iconName = "Terminal",
            expectedBinaries = listOf("usr/bin/tmux", "usr/bin/htop", "usr/bin/jq"),
            packagesByManager = mapOf(
                PackageManagerType.APT to listOf("tmux", "htop", "jq"),
                PackageManagerType.DNF to listOf("tmux", "htop", "jq"),
                PackageManagerType.APK to listOf("tmux", "htop", "jq"),
                PackageManagerType.PACMAN to listOf("tmux", "htop", "jq"),
                PackageManagerType.XBPS to listOf("tmux", "htop", "jq")
            ),
            postInstallNotes = "Run 'tmux', 'htop', or 'jq' from your terminal."
        ),
        SoftwareRecipe(
            id = "zsh_shell",
            name = "Zsh Shell",
            description = "Extended Bourne shell with powerful completion and theming",
            category = SoftwareCategory.UTILITIES,
            iconName = "Terminal",
            expectedBinaries = listOf("usr/bin/zsh"),
            packagesByManager = mapOf(
                PackageManagerType.APT to listOf("zsh"),
                PackageManagerType.DNF to listOf("zsh"),
                PackageManagerType.APK to listOf("zsh"),
                PackageManagerType.PACMAN to listOf("zsh"),
                PackageManagerType.XBPS to listOf("zsh")
            ),
            postInstallNotes = "Launch anytime by running 'zsh'."
        ),
        SoftwareRecipe(
            id = "ffmpeg_media",
            name = "FFmpeg",
            description = "Complete cross-platform audio and video conversion suite",
            category = SoftwareCategory.UTILITIES,
            iconName = "Tune",
            expectedBinaries = listOf("usr/bin/ffmpeg"),
            packagesByManager = mapOf(
                PackageManagerType.APT to listOf("ffmpeg"),
                PackageManagerType.DNF to listOf("ffmpeg-free"),
                PackageManagerType.APK to listOf("ffmpeg"),
                PackageManagerType.PACMAN to listOf("ffmpeg"),
                PackageManagerType.XBPS to listOf("ffmpeg")
            ),
            postInstallNotes = "Run 'ffmpeg -version' to verify codecs and configuration."
        ),
        SoftwareRecipe(
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
                PackageManagerType.APT to "export DEBIAN_FRONTEND=noninteractive && (apt-get remove -y rclone && apt-get autoremove -y) 2>/dev/null || rm -f /usr/bin/rclone"
            ),
            postInstallNotes = "Run 'rclone config' to link a cloud remote."
        ),
        SoftwareRecipe(
            id = "iperf3_bench",
            name = "iPerf3",
            description = "Network bandwidth and throughput benchmarking tool",
            category = SoftwareCategory.NETWORKING,
            iconName = "Security",
            expectedBinaries = listOf("usr/bin/iperf3"),
            packagesByManager = mapOf(
                PackageManagerType.APT to listOf("iperf3"),
                PackageManagerType.DNF to listOf("iperf3"),
                PackageManagerType.APK to listOf("iperf3"),
                PackageManagerType.PACMAN to listOf("iperf3"),
                PackageManagerType.XBPS to listOf("iperf3")
            ),
            postInstallNotes = "Run 'iperf3 -s' for server mode or 'iperf3 -c <host>' to benchmark."
        ),
        SoftwareRecipe(
            id = "geany_ide",
            name = "Geany IDE",
            description = "Fast and lightweight GTK desktop text editor and IDE",
            category = SoftwareCategory.PRODUCTIVITY,
            iconName = "Description",
            expectedBinaries = listOf("usr/bin/geany"),
            packagesByManager = mapOf(
                PackageManagerType.APT to listOf("geany"),
                PackageManagerType.DNF to listOf("geany"),
                PackageManagerType.APK to listOf("geany"),
                PackageManagerType.PACMAN to listOf("geany"),
                PackageManagerType.XBPS to listOf("geany")
            ),
            postInstallNotes = "Open from XFCE Applications -> Development -> Geany."
        ),
        SoftwareRecipe(
            id = "gimp_editor",
            name = "GIMP",
            description = "GNU Image Manipulation Program for photo editing and graphics",
            category = SoftwareCategory.PRODUCTIVITY,
            iconName = "Palette",
            expectedBinaries = listOf("usr/bin/gimp"),
            packagesByManager = mapOf(
                PackageManagerType.APT to listOf("gimp"),
                PackageManagerType.DNF to listOf("gimp"),
                PackageManagerType.APK to listOf("gimp"),
                PackageManagerType.PACMAN to listOf("gimp"),
                PackageManagerType.XBPS to listOf("gimp")
            ),
            postInstallNotes = "Open from XFCE Applications -> Graphics -> GIMP."
        ),
        SoftwareRecipe(
            id = "libreoffice_suite",
            name = "LibreOffice",
            description = "Full desktop office suite for documents, spreadsheets, and slides",
            category = SoftwareCategory.PRODUCTIVITY,
            iconName = "Description",
            expectedBinaries = listOf("usr/bin/libreoffice"),
            packagesByManager = mapOf(
                PackageManagerType.APT to listOf("libreoffice"),
                PackageManagerType.DNF to listOf("libreoffice"),
                PackageManagerType.APK to listOf("libreoffice"),
                PackageManagerType.PACMAN to listOf("libreoffice-fresh"),
                PackageManagerType.XBPS to listOf("libreoffice")
            ),
            postInstallNotes = "Open Writer, Calc, or Impress from the XFCE Applications menu."
        )
    )

    private val recipesById: Map<String, SoftwareRecipe> = recipes.associateBy { it.id }

    fun getById(packageId: String): SoftwareRecipe? = recipesById[packageId]

    fun buildInstallCommand(packageId: String, packageManager: PackageManagerType): String? {
        val recipe = recipesById[packageId] ?: return null
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
                "pacman -Syu --noconfirm --overwrite \"*\" $pkgList"
            PackageManagerType.XBPS ->
                "xbps-install -Suy && xbps-install -Sy $pkgList"
        }
    }

    fun buildUninstallCommand(packageId: String, packageManager: PackageManagerType): String? {
        val recipe = recipesById[packageId] ?: return null
        recipe.customUninstallCommands[packageManager]?.let { return it }
        val pkgs = recipe.packagesByManager[packageManager]?.takeIf { it.isNotEmpty() } ?: return null
        val pkgList = pkgs.joinToString(" ")
        return when (packageManager) {
            PackageManagerType.APT ->
                "export DEBIAN_FRONTEND=noninteractive && apt-get remove -y $pkgList && apt-get autoremove -y"
            PackageManagerType.DNF ->
                "dnf remove -y $pkgList"
            PackageManagerType.APK ->
                "apk del $pkgList"
            PackageManagerType.PACMAN ->
                "pacman -Rns --noconfirm $pkgList"
            PackageManagerType.XBPS ->
                "xbps-remove -Ry $pkgList"
        }
    }
}
