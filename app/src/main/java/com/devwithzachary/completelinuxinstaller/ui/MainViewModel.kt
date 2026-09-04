package com.devwithzachary.completelinuxinstaller.ui

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.devwithzachary.completelinuxinstaller.engine.DownloadState
import com.devwithzachary.completelinuxinstaller.engine.DiagnosticsManager
import com.devwithzachary.completelinuxinstaller.engine.GitHubRelease
import com.devwithzachary.completelinuxinstaller.engine.GitHubReleaseManager
import com.devwithzachary.completelinuxinstaller.engine.InstallStepState
import com.devwithzachary.completelinuxinstaller.engine.PRootEngine
import com.devwithzachary.completelinuxinstaller.engine.RootfsManager
import com.devwithzachary.completelinuxinstaller.engine.SoftwareInstaller
import com.devwithzachary.completelinuxinstaller.engine.SystemMonitorManager
import com.devwithzachary.completelinuxinstaller.engine.SystemResourceMetrics
import com.devwithzachary.completelinuxinstaller.engine.TerminalBridge
import com.devwithzachary.completelinuxinstaller.engine.UpdateCheckResult
import com.devwithzachary.completelinuxinstaller.model.InstallStatus
import com.devwithzachary.completelinuxinstaller.model.LinuxDistribution
import com.devwithzachary.completelinuxinstaller.model.SoftwareCategory
import com.devwithzachary.completelinuxinstaller.model.SoftwarePackage
import com.devwithzachary.completelinuxinstaller.theme.TerminalTheme
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

import com.devwithzachary.completelinuxinstaller.R
import com.devwithzachary.completelinuxinstaller.engine.ContainerManager
import com.devwithzachary.completelinuxinstaller.engine.RootfsMigrationManager
import com.devwithzachary.completelinuxinstaller.engine.RootfsVersionInfo
import com.devwithzachary.completelinuxinstaller.engine.UpgradeState
import com.devwithzachary.completelinuxinstaller.model.ContainerInstance
import com.devwithzachary.completelinuxinstaller.service.PRootForegroundService
import com.devwithzachary.completelinuxinstaller.ui.screens.terminal.TerminalFonts

enum class InitStep(val stringResId: Int) {
    VERIFYING_BINARIES(R.string.splash_init_verifying_binaries),
    CHECKING_FILESYSTEM(R.string.splash_init_checking_filesystem),
    PREPARING_ENVIRONMENT(R.string.splash_init_preparing)
}

private const val INIT_SLOW_THRESHOLD_MS = 10_000L
private const val TAG = "MainViewModel"

data class DashboardUiState(
    val isInitializing: Boolean = true,
    val initStep: InitStep = InitStep.VERIFYING_BINARIES,
    val initElapsedMs: Long = 0L,
    val splashDismissed: Boolean = false,
    val isInstalled: Boolean = false,
    val storageUsedMb: Long = 0L,
    val distroName: String = "Ubuntu 26.04 LTS (ARM64/x86_64)",
    val bindSdCard: Boolean = true,
    val isRunning: Boolean = false,
    val isVncInstalled: Boolean = false,
    val isNginxInstalled: Boolean = false,
    val isSshInstalled: Boolean = false,
    val sshPort: Int = 2222,
    val rootfsVersion: RootfsVersionInfo? = null,
    val isUpgradeAvailable: Boolean = false,
    val dnsServers: List<String> = listOf("8.8.8.8", "1.1.1.1", "8.8.4.4"),
    val containerUsers: List<String> = emptyList(),
    val containers: List<com.devwithzachary.completelinuxinstaller.model.ContainerInstance> = emptyList(),
    val defaultContainerId: String = com.devwithzachary.completelinuxinstaller.engine.ContainerManager.DEFAULT_CONTAINER_ID
) {
    val isInitSlow: Boolean get() = initElapsedMs >= INIT_SLOW_THRESHOLD_MS
}

sealed class BackupState {
    data object Idle : BackupState()
    data class Processing(val message: String, val progressPercent: Int = -1) : BackupState()
    data class Success(val message: String) : BackupState()
    data class Error(val message: String) : BackupState()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val pRootEngine = PRootEngine(application)
    val rootfsManager = RootfsManager(application, pRootEngine)
    val containerManager = ContainerManager(application)
    val softwareInstaller = SoftwareInstaller(pRootEngine)
    val terminalBridge = TerminalBridge(pRootEngine)
    val diagnosticsManager = DiagnosticsManager(application, pRootEngine, rootfsManager, containerManager)
    val systemMonitorManager = SystemMonitorManager(application, pRootEngine, rootfsManager, containerManager)
    val gitHubReleaseManager = GitHubReleaseManager(application)

    val containers: StateFlow<List<com.devwithzachary.completelinuxinstaller.model.ContainerInstance>> = containerManager.containers
    val defaultContainerId: StateFlow<String> = containerManager.defaultContainerId
    val sessions: StateFlow<List<com.devwithzachary.completelinuxinstaller.engine.TerminalSession>> = terminalBridge.sessions
    val activeSessionId: StateFlow<String?> = terminalBridge.activeSessionId

    private val _isGitHubUpdateCheckEnabled = MutableStateFlow(gitHubReleaseManager.isUpdateCheckEnabled())
    val isGitHubUpdateCheckEnabled: StateFlow<Boolean> = _isGitHubUpdateCheckEnabled.asStateFlow()

    private val _updateCheckResult = MutableStateFlow<UpdateCheckResult?>(null)
    val updateCheckResult: StateFlow<UpdateCheckResult?> = _updateCheckResult.asStateFlow()

    private val _isCheckingForUpdates = MutableStateFlow(false)
    val isCheckingForUpdates: StateFlow<Boolean> = _isCheckingForUpdates.asStateFlow()

    fun setGitHubUpdateCheckEnabled(enabled: Boolean) {
        gitHubReleaseManager.setUpdateCheckEnabled(enabled)
        _isGitHubUpdateCheckEnabled.value = enabled
        if (!enabled) {
            _updateCheckResult.value = null
        }
    }

    fun checkForGitHubUpdates(manual: Boolean = false) {
        viewModelScope.launch {
            _isCheckingForUpdates.value = true
            try {
                val result = gitHubReleaseManager.checkForUpdates(force = manual)
                _updateCheckResult.value = result
            } catch (e: Exception) {
                Log.e(TAG, "Error checking for GitHub updates", e)
                if (manual) {
                    _updateCheckResult.value = UpdateCheckResult.Error(e.localizedMessage ?: "Network error")
                }
            } finally {
                _isCheckingForUpdates.value = false
            }
        }
    }

    fun dismissGitHubUpdate(dontAskAgain: Boolean = false, releaseTag: String? = null) {
        if (dontAskAgain) {
            setGitHubUpdateCheckEnabled(false)
        } else if (releaseTag != null) {
            gitHubReleaseManager.dismissRelease(releaseTag)
        }
        _updateCheckResult.value = null
    }

    fun clearUpdateCheckResult() {
        _updateCheckResult.value = null
    }

    private val _systemMetrics = MutableStateFlow(SystemResourceMetrics())
    val systemMetrics: StateFlow<SystemResourceMetrics> = _systemMetrics.asStateFlow()

    fun killProcess(pid: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            systemMonitorManager.killProcess(pid)
            refreshSystemMetrics()
        }
    }

    suspend fun refreshSystemMetrics() {
        val metrics = systemMonitorManager.collectMetrics(terminalBridge.isRunning.value)
        _systemMetrics.value = metrics
    }

    fun triggerMetricsRefresh() {
        viewModelScope.launch(Dispatchers.IO) {
            refreshSystemMetrics()
        }
    }

    private fun startSystemMonitorLoop() {
        viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    refreshSystemMetrics()
                } catch (_: Exception) {}
                kotlinx.coroutines.delay(2500)
            }
        }
    }

    private val prefs = application.getSharedPreferences("terminal_theme_prefs", Context.MODE_PRIVATE)

    private fun loadSshPort(): Int {
        val port = prefs.getInt("ssh_port", 2222)
        return if (port in 1..65535) port else 2222
    }

    private val _sshPort = MutableStateFlow(loadSshPort())
    val sshPort: StateFlow<Int> = _sshPort.asStateFlow()

    private val _dnsServers = MutableStateFlow(rootfsManager.getDnsServers())
    val dnsServers: StateFlow<List<String>> = _dnsServers.asStateFlow()

    private val _upgradeState = MutableStateFlow<UpgradeState>(UpgradeState.Idle)
    val upgradeState: StateFlow<UpgradeState> = _upgradeState.asStateFlow()

    private val _terminalTheme = MutableStateFlow(loadTerminalTheme())
    val terminalTheme: StateFlow<TerminalTheme> = _terminalTheme.asStateFlow()

    private val _terminalFontSize = MutableStateFlow(loadTerminalFontSize())
    val terminalFontSize: StateFlow<Int> = _terminalFontSize.asStateFlow()

    private val _terminalFontFamily = MutableStateFlow(loadTerminalFontFamily())
    val terminalFontFamily: StateFlow<String> = _terminalFontFamily.asStateFlow()

    private val _dashboardState = MutableStateFlow(
        DashboardUiState(
            sshPort = loadSshPort(),
            dnsServers = rootfsManager.getDnsServers(),
            storageUsedMb = rootfsManager.getCachedStorageUsedMb()
        )
    )
    val dashboardState: StateFlow<DashboardUiState> = _dashboardState.asStateFlow()

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    private val _backupState = MutableStateFlow<BackupState>(BackupState.Idle)
    val backupState: StateFlow<BackupState> = _backupState.asStateFlow()

    private val _packages = MutableStateFlow(SoftwarePackage.getPresets(loadSshPort()))
    val packages: StateFlow<List<SoftwarePackage>> = _packages.asStateFlow()

    private val _defaultTerminalUser = MutableStateFlow(loadDefaultTerminalUser())
    val defaultTerminalUser: StateFlow<String> = _defaultTerminalUser.asStateFlow()

    private fun loadKeepAliveEnabled(): Boolean {
        return prefs.getBoolean("keep_alive_enabled", true)
    }

    private val _isKeepAliveEnabled = MutableStateFlow(loadKeepAliveEnabled())
    val isKeepAliveEnabled: StateFlow<Boolean> = _isKeepAliveEnabled.asStateFlow()

    fun toggleKeepAlive() {
        val next = !_isKeepAliveEnabled.value
        prefs.edit().putBoolean("keep_alive_enabled", next).apply()
        _isKeepAliveEnabled.value = next
        if (!next) {
            PRootForegroundService.stop(getApplication())
        } else if (terminalBridge.isRunning.value) {
            PRootForegroundService.start(getApplication())
        }
    }

    private fun loadKeepScreenOnEnabled(): Boolean {
        return prefs.getBoolean("terminal_keep_screen_on", true)
    }

    private val _isKeepScreenOnEnabled = MutableStateFlow(loadKeepScreenOnEnabled())
    val isKeepScreenOnEnabled: StateFlow<Boolean> = _isKeepScreenOnEnabled.asStateFlow()

    fun setKeepScreenOnEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("terminal_keep_screen_on", enabled).apply()
        _isKeepScreenOnEnabled.value = enabled
    }

    private val _requestedScreen = MutableStateFlow<AppScreen?>(null)
    val requestedScreen: StateFlow<AppScreen?> = _requestedScreen.asStateFlow()

    fun navigateToScreen(screen: AppScreen) {
        _requestedScreen.value = screen
    }

    fun clearRequestedScreen() {
        _requestedScreen.value = null
    }

    val isSessionRunning = terminalBridge.isRunning

    fun setDnsServers(servers: List<String>, containerId: String? = null) {
        setContainerDns(servers, containerId)
    }

    fun upgradeRootfs(containerId: String? = null) {
        val container = containerId?.let { containerManager.getContainer(it) } ?: containerManager.getDefaultContainer()
        val targetDir = container?.rootDir ?: pRootEngine.rootfsDir
        viewModelScope.launch {
            rootfsManager.upgradeRootfs(targetDir).collect { state ->
                _upgradeState.value = state
                if (state is UpgradeState.Success) {
                    refreshStatus()
                }
            }
        }
    }

    fun dismissUpgradeState() {
        _upgradeState.value = UpgradeState.Idle
    }

    suspend fun generateDebugReport(): String {
        val info = diagnosticsManager.collect(sessionRunning = terminalBridge.isRunning.value)
        return DiagnosticsManager.buildReport(info)
    }

    fun setSshPort(port: Int) {
        val validPort = if (port in 1..65535) port else 2222
        prefs.edit().putInt("ssh_port", validPort).apply()
        _sshPort.value = validPort

        val currentPackages = _packages.value.map { pkg ->
            if (pkg.id == "openssh_server") {
                pkg.copy(
                    launchCommand = SoftwarePackage.buildSshLaunchCommand(validPort),
                    postInstallNotes = SoftwarePackage.buildSshPostInstallNotes(validPort)
                )
            } else {
                pkg
            }
        }
        _packages.value = currentPackages
        _dashboardState.value = _dashboardState.value.copy(sshPort = validPort)
    }

    private fun loadTerminalFontSize(): Int {
        return prefs.getInt("terminal_font_size", 13)
    }

    private fun loadTerminalFontFamily(): String {
        val saved = prefs.getString("terminal_font_family", TerminalFonts.DEFAULT_FONT)
        return TerminalFonts.normalizeFontName(saved)
    }

    fun setTerminalFontSize(size: Int) {
        val clampedSize = size.coerceIn(10, 24)
        prefs.edit().putInt("terminal_font_size", clampedSize).apply()
        _terminalFontSize.value = clampedSize
    }

    fun setTerminalFontFamily(family: String) {
        val normalized = TerminalFonts.normalizeFontName(family)
        prefs.edit().putString("terminal_font_family", normalized).apply()
        _terminalFontFamily.value = normalized
    }

    private fun loadDefaultTerminalUser(): String {
        val savedUser = prefs.getString("default_terminal_user", null)
        if (!savedUser.isNullOrBlank()) {
            return savedUser
        }
        val users = rootfsManager.getContainerUsers()
        return users.firstOrNull() ?: "root"
    }

    fun setDefaultTerminalUser(username: String) {
        prefs.edit().putString("default_terminal_user", username).apply()
        _defaultTerminalUser.value = username
    }

    private var initJob: kotlinx.coroutines.Job? = null
    private var initGeneration = 0

    fun dismissSplash() {
        initGeneration++
        _dashboardState.value = _dashboardState.value.copy(
            isInitializing = false,
            splashDismissed = true
        )
    }

    fun retryInit() {
        _dashboardState.value = _dashboardState.value.copy(
            isInitializing = true,
            splashDismissed = false,
            initElapsedMs = 0L
        )
        startInitialization()
    }

    private fun startInitialization() {
        val gen = ++initGeneration
        val startMs = System.currentTimeMillis()
        initJob?.cancel()
        initJob = viewModelScope.launch {
            val watchdog = launch {
                while (isActive && _dashboardState.value.isInitializing) {
                    kotlinx.coroutines.delay(1000)
                    val elapsed = System.currentTimeMillis() - startMs
                    if (!_dashboardState.value.isInitializing) break
                    _dashboardState.value = _dashboardState.value.copy(
                        initElapsedMs = elapsed
                    )
                }
            }
            try {
                _dashboardState.value = _dashboardState.value.copy(initStep = InitStep.VERIFYING_BINARIES)
                pRootEngine.ensurePRootExecutable()
                kotlinx.coroutines.delay(200)

                _dashboardState.value = _dashboardState.value.copy(initStep = InitStep.CHECKING_FILESYSTEM)
                refreshStatusInternal()
                kotlinx.coroutines.delay(250)

                _dashboardState.value = _dashboardState.value.copy(initStep = InitStep.PREPARING_ENVIRONMENT)
                kotlinx.coroutines.delay(250)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Startup initialization failed", e)
            } finally {
                watchdog.cancel()
                if (gen == initGeneration) {
                    _dashboardState.value = _dashboardState.value.copy(
                        isInitializing = false,
                        initElapsedMs = System.currentTimeMillis() - startMs
                    )

                    // Check for GitHub updates in background (throttled to once every 24h)
                    viewModelScope.launch {
                        kotlinx.coroutines.delay(1500)
                        if (gitHubReleaseManager.isUpdateCheckEnabled()) {
                            checkForGitHubUpdates(manual = false)
                        }
                    }
                }
            }
        }
    }

    init {
        PRootForegroundService.isTerminalActiveProvider = { terminalBridge.isRunning.value }
        PRootForegroundService.rootfsDirProvider = { pRootEngine.rootfsDir }
        PRootForegroundService.sshPortProvider = { _sshPort.value }
        PRootForegroundService.onStopSessionRequested = { stopTerminalSession() }

        startInitialization()
        startSystemMonitorLoop()
    }

    private fun loadTerminalTheme(): TerminalTheme {
        val themeId = prefs.getString("theme_id", "dracula") ?: "dracula"
        if (themeId == "custom") {
            return loadCustomTheme()
        }
        val theme = TerminalTheme.getById(themeId)
        terminalBridge.emulator.applyTheme(theme)
        return theme
    }

    private fun loadCustomTheme(): TerminalTheme {
        val defaultCustom = TerminalTheme.DRACULA.copy(id = "custom", name = "Custom")
        val fgHex = prefs.getString("custom_fg", TerminalTheme.colorToHex(defaultCustom.defaultFg)) ?: ""
        val bgHex = prefs.getString("custom_bg", TerminalTheme.colorToHex(defaultCustom.defaultBg)) ?: ""
        val cursorHex = prefs.getString("custom_cursor", TerminalTheme.colorToHex(defaultCustom.cursorColor)) ?: ""
        val selHex = prefs.getString("custom_selection", TerminalTheme.colorToHex(defaultCustom.selectionColor)) ?: ""
        val ansiHexStr = prefs.getString("custom_ansi", "") ?: ""

        val fg = TerminalTheme.hexToColor(fgHex, defaultCustom.defaultFg)
        val bg = TerminalTheme.hexToColor(bgHex, defaultCustom.defaultBg)
        val cursor = TerminalTheme.hexToColor(cursorHex, defaultCustom.cursorColor)
        val sel = TerminalTheme.hexToColor(selHex, defaultCustom.selectionColor)

        val ansiList = if (ansiHexStr.isNotBlank()) {
            val parts = ansiHexStr.split(",")
            if (parts.size >= 16) {
                parts.take(16).mapIndexed { idx, hex ->
                    TerminalTheme.hexToColor(hex, defaultCustom.ansiColors.getOrElse(idx) { Color.White })
                }
            } else defaultCustom.ansiColors
        } else defaultCustom.ansiColors

        val customTheme = TerminalTheme(
            id = "custom",
            name = "Custom",
            defaultFg = fg,
            defaultBg = bg,
            cursorColor = cursor,
            selectionColor = sel,
            ansiColors = ansiList
        )
        terminalBridge.emulator.applyTheme(customTheme)
        return customTheme
    }

    fun setTerminalTheme(themeId: String) {
        prefs.edit().putString("theme_id", themeId).apply()
        val theme = if (themeId == "custom") loadCustomTheme() else {
            val t = TerminalTheme.getById(themeId)
            terminalBridge.emulator.applyTheme(t)
            t
        }
        _terminalTheme.value = theme
    }

    fun updateCustomTheme(
        fg: Color,
        bg: Color,
        cursorColor: Color,
        selectionColor: Color,
        ansiColors: List<Color>
    ) {
        val ansiHex = ansiColors.joinToString(",") { TerminalTheme.colorToHex(it) }
        prefs.edit()
            .putString("theme_id", "custom")
            .putString("custom_fg", TerminalTheme.colorToHex(fg))
            .putString("custom_bg", TerminalTheme.colorToHex(bg))
            .putString("custom_cursor", TerminalTheme.colorToHex(cursorColor))
            .putString("custom_selection", TerminalTheme.colorToHex(selectionColor))
            .putString("custom_ansi", ansiHex)
            .apply()

        val customTheme = TerminalTheme(
            id = "custom",
            name = "Custom",
            defaultFg = fg,
            defaultBg = bg,
            cursorColor = cursorColor,
            selectionColor = selectionColor,
            ansiColors = ansiColors
        )
        terminalBridge.emulator.applyTheme(customTheme)
        _terminalTheme.value = customTheme
    }

    suspend fun refreshStatusInternal() = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        containerManager.loadAndMigrateContainers()
        val allContainers = containerManager.getAllContainers()
        val defaultContainer = containerManager.getDefaultContainer()
        val defaultDir = defaultContainer?.rootDir ?: pRootEngine.rootfsDir
        val installed = (allContainers.any { it.isInstalled }) || rootfsManager.isInstalled()
        val rootfsDir = defaultDir

        // Sync individual package states against rootfs file system package tracking file and binaries
        val packageVersions = if (installed) RootfsMigrationManager.readPackageVersions(rootfsDir) else emptyMap()

        val hasVnc = installed && (
            SoftwarePackage.isBinaryPresent(rootfsDir, "usr/bin/vncserver") ||
            SoftwarePackage.isBinaryPresent(rootfsDir, "usr/bin/tigervncserver") ||
            SoftwarePackage.isBinaryPresent(rootfsDir, "usr/bin/startxfce4") ||
            packageVersions.containsKey("xfce_desktop")
        )
        val hasNginx = installed && (
            SoftwarePackage.isBinaryPresent(rootfsDir, "usr/sbin/nginx") ||
            SoftwarePackage.isBinaryPresent(rootfsDir, "usr/bin/nginx") ||
            packageVersions.containsKey("nginx_web")
        )
        val hasSsh = installed && (
            SoftwarePackage.isBinaryPresent(rootfsDir, "usr/sbin/sshd") ||
            SoftwarePackage.isBinaryPresent(rootfsDir, "usr/bin/sshd") ||
            packageVersions.containsKey("openssh_server")
        )
        val users = if (installed) rootfsManager.getContainerUsers() else emptyList()

        val syncedPackages = _packages.value.map { pkg ->
            if (pkg.status == InstallStatus.INSTALLING) {
                pkg
            } else if (!installed) {
                pkg.copy(
                    status = InstallStatus.NOT_INSTALLED,
                    hasUpgradeAvailable = false,
                    progressMessage = "",
                    installLogs = ""
                )
            } else {
                val hasExpectedBinaries = pkg.expectedBinaries.isNotEmpty() &&
                        pkg.expectedBinaries.all { SoftwarePackage.isBinaryPresent(rootfsDir, it) }
                val hasAnyExpectedBinary = pkg.expectedBinaries.isEmpty() ||
                        pkg.expectedBinaries.any { SoftwarePackage.isBinaryPresent(rootfsDir, it) }

                val isPkgInstalled = (packageVersions.containsKey(pkg.id) && hasAnyExpectedBinary) ||
                        hasExpectedBinaries ||
                        (pkg.id.startsWith("custom_") && run {
                            val binaryName = pkg.id.removePrefix("custom_")
                            SoftwarePackage.isBinaryPresent(rootfsDir, "usr/bin/$binaryName") ||
                            SoftwarePackage.isBinaryPresent(rootfsDir, "usr/sbin/$binaryName") ||
                            SoftwarePackage.isBinaryPresent(rootfsDir, "bin/$binaryName")
                        })
                val actualStatus = if (isPkgInstalled) InstallStatus.INSTALLED else pkg.status.takeIf { it == InstallStatus.FAILED } ?: InstallStatus.NOT_INSTALLED
                val installedVer = if (isPkgInstalled) (packageVersions[pkg.id] ?: 1) else pkg.version
                val hasUpgrade = isPkgInstalled && (installedVer < pkg.version)

                pkg.copy(status = actualStatus, hasUpgradeAvailable = hasUpgrade)
            }
        }
        _packages.value = syncedPackages

        val defaultUserForContainer = defaultContainer?.defaultUser ?: users.firstOrNull() ?: "root"
        val savedUser = prefs.getString("default_terminal_user", null)
        if (savedUser.isNullOrBlank() || (savedUser != "root" && !users.contains(savedUser))) {
            prefs.edit().putString("default_terminal_user", defaultUserForContainer).apply()
            _defaultTerminalUser.value = defaultUserForContainer
        } else if (defaultContainer != null && defaultContainer.defaultUser.isNotBlank()) {
            _defaultTerminalUser.value = defaultContainer.defaultUser
        }

        val rootfsVersion = if (installed) rootfsManager.getRootfsVersion() else null
        val isUpgradeAvail = if (installed) rootfsManager.isUpgradeAvailable() else false
        val currentDns = rootfsManager.getDnsServers()
        _dnsServers.value = currentDns

        val totalContainerStorage = allContainers.sumOf { it.storageUsedMb }.takeIf { it > 0L }
            ?: defaultContainer?.storageUsedMb?.takeIf { it > 0L }
            ?: (if (installed) rootfsManager.getCachedStorageUsedMb() else 0L)

        _dashboardState.value = _dashboardState.value.copy(
            isInstalled = installed,
            isRunning = terminalBridge.isRunning.value,
            isVncInstalled = hasVnc,
            isNginxInstalled = hasNginx,
            isSshInstalled = hasSsh,
            sshPort = _sshPort.value,
            rootfsVersion = rootfsVersion,
            isUpgradeAvailable = isUpgradeAvail,
            dnsServers = currentDns,
            containerUsers = users,
            storageUsedMb = totalContainerStorage,
            containers = allContainers,
            defaultContainerId = containerManager.defaultContainerId.value,
            distroName = defaultContainer?.distroName ?: "Ubuntu 26.04 LTS"
        )

        if (installed) {
            triggerAsyncStorageCalculation()
        }
    }

    fun getPackagesForContainer(containerId: String): List<SoftwarePackage> {
        val container = containerManager.getContainer(containerId)
        val rootfsDir = container?.rootDir ?: return _packages.value
        val installed = container.isInstalled && rootfsDir.exists()
        val packageVersions = if (installed) RootfsMigrationManager.readPackageVersions(rootfsDir) else emptyMap()

        return _packages.value.map { pkg ->
            if (pkg.status == InstallStatus.INSTALLING) {
                pkg
            } else if (!installed) {
                pkg.copy(status = InstallStatus.NOT_INSTALLED, hasUpgradeAvailable = false)
            } else {
                val hasExpectedBinaries = pkg.expectedBinaries.isNotEmpty() &&
                        pkg.expectedBinaries.all { SoftwarePackage.isBinaryPresent(rootfsDir, it) }
                val hasAnyExpectedBinary = pkg.expectedBinaries.isEmpty() ||
                        pkg.expectedBinaries.any { SoftwarePackage.isBinaryPresent(rootfsDir, it) }

                val isPkgInstalled = (packageVersions.containsKey(pkg.id) && hasAnyExpectedBinary) ||
                        hasExpectedBinaries ||
                        (pkg.id.startsWith("custom_") && run {
                            val binaryName = pkg.id.removePrefix("custom_")
                            SoftwarePackage.isBinaryPresent(rootfsDir, "usr/bin/$binaryName") ||
                            SoftwarePackage.isBinaryPresent(rootfsDir, "usr/sbin/$binaryName") ||
                            SoftwarePackage.isBinaryPresent(rootfsDir, "bin/$binaryName")
                        })
                val actualStatus = if (isPkgInstalled) InstallStatus.INSTALLED else pkg.status.takeIf { it == InstallStatus.FAILED } ?: InstallStatus.NOT_INSTALLED
                val installedVer = if (isPkgInstalled) (packageVersions[pkg.id] ?: 1) else pkg.version
                val hasUpgrade = isPkgInstalled && (installedVer < pkg.version)

                pkg.copy(status = actualStatus, hasUpgradeAvailable = hasUpgrade)
            }
        }
    }

    private var storageCalculationJob: kotlinx.coroutines.Job? = null

    private fun triggerAsyncStorageCalculation() {
        storageCalculationJob?.cancel()
        storageCalculationJob = viewModelScope.launch(Dispatchers.IO) {
            containerManager.refreshContainerStorage(rootfsManager)
            val updatedContainers = containerManager.getAllContainers()
            val totalFreshStorageMb = updatedContainers.sumOf { it.storageUsedMb }.takeIf { it > 0L }
                ?: (containerManager.getDefaultContainer()?.storageUsedMb ?: 0L)
            _dashboardState.value = _dashboardState.value.copy(
                containers = updatedContainers,
                storageUsedMb = totalFreshStorageMb
            )
            refreshSystemMetrics()
        }
    }

    fun refreshStatus() {
        viewModelScope.launch {
            refreshStatusInternal()
        }
    }

    fun installUbuntu() {
        viewModelScope.launch {
            val distro = LinuxDistribution.defaultForArch(android.os.Build.SUPPORTED_ABIS.firstOrNull() ?: "")
            rootfsManager.downloadAndInstallUbuntu(distro).collect { state ->
                _downloadState.value = state
                if (state is DownloadState.Success) {
                    refreshStatus()
                }
            }
        }
    }

    fun installDistro(
        distroDef: com.devwithzachary.completelinuxinstaller.model.DistroDefinition,
        containerName: String = "",
        rootPassword: String = "root",
        username: String = "user",
        userPassword: String = "user"
    ) {
        viewModelScope.launch {
            val containerId = "container_" + System.currentTimeMillis()
            val finalName = containerName.ifBlank { distroDef.name }
            val container = containerManager.createContainerEntry(
                id = containerId,
                name = finalName,
                distroId = distroDef.id,
                defaultUser = username.ifBlank { "user" },
                defaultShell = distroDef.defaultShell
            )
            rootfsManager.downloadAndInstallDistro(
                targetDir = container.rootDir,
                distroDef = distroDef,
                containerName = finalName,
                rootPassword = rootPassword,
                username = username,
                userPassword = userPassword
            ).collect { state ->
                _downloadState.value = state
                if (state is DownloadState.Success) {
                    containerManager.setDefaultContainer(containerId)
                    refreshStatus()
                }
            }
        }
    }

    fun resetWizardState() {
        _downloadState.value = DownloadState.Idle
    }

    fun configureWizardAccounts(rootPassword: String, username: String, userPassword: String) {
        viewModelScope.launch {
            val cleanUsername = username.lowercase().replace(Regex("[^a-z0-9_-]"), "").ifEmpty { "ubuntu" }
            rootfsManager.setRootPassword(rootPassword)
            if (cleanUsername != "ubuntu") {
                rootfsManager.deleteUser("ubuntu")
            }
            rootfsManager.createOrUpdateUser(cleanUsername, userPassword, isSudo = true)
            setDefaultTerminalUser(cleanUsername)
            refreshStatus()
            terminalBridge.startSession(loginUser = cleanUsername)
        }
    }

    fun changeRootPassword(newPassword: String, containerId: String? = null) {
        val container = containerId?.let { containerManager.getContainer(it) } ?: containerManager.getDefaultContainer()
        val targetDir = container?.rootDir ?: pRootEngine.rootfsDir
        viewModelScope.launch {
            rootfsManager.setRootPassword(newPassword, targetDir)
            refreshStatus()
        }
    }

    fun createUser(username: String, password: String, isSudo: Boolean = true, containerId: String? = null) {
        val container = containerId?.let { containerManager.getContainer(it) } ?: containerManager.getDefaultContainer()
        val targetDir = container?.rootDir ?: pRootEngine.rootfsDir
        viewModelScope.launch {
            rootfsManager.createOrUpdateUser(username, password, isSudo, targetDir)
            refreshStatus()
        }
    }

    fun deleteUser(username: String, containerId: String? = null) {
        val container = containerId?.let { containerManager.getContainer(it) } ?: containerManager.getDefaultContainer()
        val targetDir = container?.rootDir ?: pRootEngine.rootfsDir
        viewModelScope.launch {
            rootfsManager.deleteUser(username, targetDir)
            if (_defaultTerminalUser.value == username) {
                val remainingUsers = rootfsManager.getContainerUsers(targetDir).filter { it != username }
                setDefaultTerminalUser(remainingUsers.firstOrNull() ?: "root")
            }
            refreshStatus()
        }
    }

    fun setContainerDefaultUser(username: String, containerId: String? = null) {
        val container = containerId?.let { containerManager.getContainer(it) } ?: containerManager.getDefaultContainer()
        val cId = container?.id ?: containerId ?: return
        containerManager.setContainerDefaultUser(cId, username)
        if (container?.isDefault == true || cId == _dashboardState.value.defaultContainerId) {
            _defaultTerminalUser.value = username
            prefs.edit().putString("default_terminal_user", username).apply()
        }
        refreshStatus()
    }

    fun getContainerUsers(containerId: String? = null): List<String> {
        val container = containerId?.let { containerManager.getContainer(it) } ?: containerManager.getDefaultContainer()
        val targetDir = container?.rootDir ?: pRootEngine.rootfsDir
        return rootfsManager.getContainerUsers(targetDir)
    }

    fun getContainerDns(containerId: String? = null): List<String> {
        val container = containerId?.let { containerManager.getContainer(it) } ?: containerManager.getDefaultContainer()
        val targetDir = container?.rootDir ?: pRootEngine.rootfsDir
        return rootfsManager.getDnsServers(targetDir)
    }

    fun setContainerDns(servers: List<String>, containerId: String? = null) {
        val container = containerId?.let { containerManager.getContainer(it) } ?: containerManager.getDefaultContainer()
        val targetDir = container?.rootDir ?: pRootEngine.rootfsDir
        rootfsManager.setDnsServers(servers, targetDir)
        _dnsServers.value = rootfsManager.getDnsServers(targetDir)
        _dashboardState.value = _dashboardState.value.copy(dnsServers = _dnsServers.value)
    }

    fun createNewTab(
        containerId: String? = null,
        loginUser: String? = null,
        title: String? = null
    ) {
        val targetContainer = if (containerId != null) {
            containerManager.getContainer(containerId)
        } else {
            containerManager.getDefaultContainer()
        }
        val targetId = targetContainer?.id ?: com.devwithzachary.completelinuxinstaller.engine.ContainerManager.DEFAULT_CONTAINER_ID
        val targetName = targetContainer?.name ?: "Ubuntu"
        val rootDir = targetContainer?.rootDir ?: pRootEngine.rootfsDir
        val user = loginUser ?: targetContainer?.defaultUser ?: _defaultTerminalUser.value
        val shell = targetContainer?.defaultShell

        terminalBridge.createSession(
            containerId = targetId,
            containerName = targetName,
            loginUser = user,
            title = title,
            rootfsDir = rootDir,
            defaultShell = shell,
            autoStart = true
        )

        if (_isKeepAliveEnabled.value) {
            PRootForegroundService.start(getApplication())
        }
        refreshStatus()
    }

    fun switchTab(sessionId: String) {
        terminalBridge.switchActiveSession(sessionId)
        refreshStatus()
    }

    fun closeTab(sessionId: String) {
        terminalBridge.closeSession(sessionId)
        if (!terminalBridge.isAnySessionRunning.value) {
            PRootForegroundService.stop(getApplication())
        }
        refreshStatus()
    }

    fun renameTab(sessionId: String, newTitle: String) {
        terminalBridge.renameSession(sessionId, newTitle)
    }

    fun setDefaultContainer(containerId: String) {
        containerManager.setDefaultContainer(containerId)
        refreshStatus()
    }

    fun deleteContainer(containerId: String) {
        viewModelScope.launch {
            val tabs = terminalBridge.sessions.value.filter { it.containerId == containerId }
            tabs.forEach { terminalBridge.closeSession(it.id) }
            containerManager.deleteContainer(containerId)
            refreshStatus()
        }
    }

    fun startTerminalSessionForContainer(containerId: String, loginUser: String? = null) {
        val container = containerManager.getContainer(containerId) ?: containerManager.getDefaultContainer()
        val targetId = container?.id ?: com.devwithzachary.completelinuxinstaller.engine.ContainerManager.DEFAULT_CONTAINER_ID
        val targetName = container?.name ?: "Ubuntu"
        val rootDir = container?.rootDir ?: pRootEngine.rootfsDir
        val user = loginUser ?: container?.defaultUser ?: _defaultTerminalUser.value
        val shell = container?.defaultShell

        val existingSession = terminalBridge.sessions.value.find { it.containerId == targetId }
        if (existingSession != null) {
            terminalBridge.switchActiveSession(existingSession.id)
            if (!existingSession.isRunning.value) {
                existingSession.startSession(pRootEngine, rootDir, shell)
            }
        } else {
            terminalBridge.createSession(
                containerId = targetId,
                containerName = targetName,
                loginUser = user,
                rootfsDir = rootDir,
                defaultShell = shell,
                autoStart = true
            )
        }

        if (_isKeepAliveEnabled.value) {
            PRootForegroundService.start(getApplication())
        }
        refreshStatus()
    }

    fun startTerminalSession() {
        val defaultContainer = containerManager.getDefaultContainer()
        val targetUser = defaultContainer?.defaultUser ?: _defaultTerminalUser.value
        if (defaultContainer != null) {
            startTerminalSessionForContainer(defaultContainer.id, targetUser)
        } else {
            terminalBridge.startSession(loginUser = targetUser)
            if (_isKeepAliveEnabled.value) {
                PRootForegroundService.start(getApplication())
            }
            refreshStatus()
        }
    }

    fun stopTerminalSession() {
        terminalBridge.stopSession()
        if (!terminalBridge.isAnySessionRunning.value) {
            PRootForegroundService.stop(getApplication())
        }
        refreshStatus()
    }

    fun sendTerminalCommand(command: String, containerId: String? = null) {
        val container = containerId?.let { containerManager.getContainer(it) } ?: containerManager.getDefaultContainer()
        val targetId = container?.id ?: com.devwithzachary.completelinuxinstaller.engine.ContainerManager.DEFAULT_CONTAINER_ID
        val targetName = container?.name ?: "Ubuntu"
        val rootDir = container?.rootDir ?: pRootEngine.rootfsDir
        val user = container?.defaultUser ?: _defaultTerminalUser.value
        val shell = container?.defaultShell

        val existingSession = terminalBridge.sessions.value.find { it.containerId == targetId }
        if (existingSession != null) {
            terminalBridge.switchActiveSession(existingSession.id)
            existingSession.queueCommand(command)
            if (!existingSession.isRunning.value) {
                existingSession.startSession(pRootEngine, rootDir, shell)
            }
        } else {
            val session = terminalBridge.createSession(
                containerId = targetId,
                containerName = targetName,
                loginUser = user,
                rootfsDir = rootDir,
                defaultShell = shell,
                autoStart = false
            )
            session.queueCommand(command)
            session.startSession(pRootEngine, rootDir, shell)
        }

        if (_isKeepAliveEnabled.value) {
            PRootForegroundService.start(getApplication())
        }
        refreshStatus()
    }

    fun sendCtrlC() {
        terminalBridge.sendCtrlC()
    }

    fun installCustomPackage(packageName: String, containerId: String? = null) {
        val cleanName = packageName.trim()
        if (cleanName.isEmpty()) return

        val container = containerId?.let { containerManager.getContainer(it) } ?: containerManager.getDefaultContainer()
        val pm = container?.packageManager ?: com.devwithzachary.completelinuxinstaller.model.PackageManagerType.APT

        val pkgId = "custom_" + cleanName.lowercase().replace(" ", "_")
        val currentList = _packages.value.toMutableList()

        val existingIdx = currentList.indexOfFirst { it.id == pkgId }
        val installCmd = when (pm) {
            com.devwithzachary.completelinuxinstaller.model.PackageManagerType.APK -> "apk update && apk add $cleanName"
            com.devwithzachary.completelinuxinstaller.model.PackageManagerType.PACMAN -> "pacman -Sy --noconfirm $cleanName"
            com.devwithzachary.completelinuxinstaller.model.PackageManagerType.DNF -> "dnf install -y $cleanName"
            com.devwithzachary.completelinuxinstaller.model.PackageManagerType.XBPS -> "xbps-install -S && xbps-install -y $cleanName"
            com.devwithzachary.completelinuxinstaller.model.PackageManagerType.APT -> "export DEBIAN_FRONTEND=noninteractive && dpkg --configure -a && apt-get update && apt-get install -y -o Dpkg::Options::=\"--force-overwrite\" $cleanName"
        }

        val customPkg = SoftwarePackage(
            id = pkgId,
            name = "${pm.name} Package: $cleanName",
            category = SoftwareCategory.UTILITIES,
            description = "Custom ${container?.distroName ?: "Linux"} package '$cleanName' installed via ${pm.name.lowercase()}.",
            iconName = "Terminal",
            installCommand = installCmd
        )

        if (existingIdx != -1) {
            currentList[existingIdx] = customPkg
        } else {
            currentList.add(0, customPkg)
        }

        _packages.value = currentList
        installSoftwarePackage(pkgId, containerId)
    }

    fun installSoftwarePackage(packageId: String, containerId: String? = null) {
        if (_isKeepAliveEnabled.value) {
            PRootForegroundService.start(getApplication())
        }
        val currentPackages = _packages.value.toMutableList()
        val index = currentPackages.indexOfFirst { it.id == packageId }
        if (index == -1) return

        val container = containerId?.let { containerManager.getContainer(it) } ?: containerManager.getDefaultContainer()
        val targetDir = container?.rootDir ?: pRootEngine.rootfsDir
        val distroDef = com.devwithzachary.completelinuxinstaller.model.DistroCatalog.getById(container?.distroId ?: container?.distroName ?: "")
        val currentSshPort = _sshPort.value

        val pkg = currentPackages[index]
        val effectiveCommand = distroDef.getSoftwarePackageInstallCommand(pkg.id, currentSshPort) ?: pkg.installCommand
        currentPackages[index] = pkg.copy(
            status = InstallStatus.INSTALLING,
            progressMessage = "Initializing installation...",
            installLogs = "Starting installation of ${pkg.name} into ${container?.name ?: "container"}...\nExecuting script: $effectiveCommand\n"
        )
        _packages.value = currentPackages

        viewModelScope.launch {
            softwareInstaller.installPackage(pkg, targetDir, distroDef, currentSshPort).collect { step ->
                val list = _packages.value.toMutableList()
                val idx = list.indexOfFirst { it.id == packageId }
                if (idx != -1) {
                    when (step) {
                        is InstallStepState.Progress -> {
                            val newLogs = list[idx].installLogs + step.logLine + "\n"
                            list[idx] = list[idx].copy(
                                status = InstallStatus.INSTALLING,
                                progressMessage = step.logLine,
                                installLogs = newLogs
                            )
                        }

                        is InstallStepState.Success -> {
                            val newLogs = list[idx].installLogs + "Installation completed successfully!\n"
                            list[idx] = list[idx].copy(
                                status = InstallStatus.INSTALLED,
                                progressMessage = "Installed! " + (step.notes ?: ""),
                                installLogs = newLogs
                            )
                            refreshStatus()
                        }

                        is InstallStepState.Error -> {
                            val newLogs = list[idx].installLogs + "ERROR: " + step.errorMessage + "\n"
                            list[idx] = list[idx].copy(
                                status = InstallStatus.FAILED,
                                progressMessage = "Failed: " + step.errorMessage,
                                installLogs = newLogs
                            )
                        }
                    }
                    _packages.value = list
                }
            }
        }
    }

    fun toggleBindSdCard() {
        val current = _dashboardState.value.bindSdCard
        _dashboardState.value = _dashboardState.value.copy(bindSdCard = !current)
    }


    fun wipeRootfs() {
        viewModelScope.launch {
            terminalBridge.stopSession()
            rootfsManager.wipeRootfs()
            _downloadState.value = DownloadState.Idle
            _packages.value = SoftwarePackage.getPresets(_sshPort.value)
            refreshStatus()
        }
    }

    fun exportContainer(
        contentResolver: android.content.ContentResolver,
        uri: android.net.Uri,
        containerId: String? = null
    ) {
        val container = containerId?.let { containerManager.getContainer(it) } ?: containerManager.getDefaultContainer()
        val targetDir = container?.rootDir ?: pRootEngine.rootfsDir
        val name = container?.name ?: "RootFS"
        viewModelScope.launch {
            _backupState.value = BackupState.Processing("Exporting $name container archive...", 0)
            try {
                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    val success = rootfsManager.exportContainerToStream(outputStream, targetDir) { msg, percent ->
                        _backupState.value = BackupState.Processing(msg, percent)
                    }
                    if (success) {
                        _backupState.value = BackupState.Success("$name container exported successfully!")
                    } else {
                        _backupState.value = BackupState.Error("Failed to export $name container archive.")
                    }
                } ?: run {
                    _backupState.value = BackupState.Error("Unable to open destination file stream.")
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error exporting container", e)
                _backupState.value = BackupState.Error("Export failed: ${e.localizedMessage}")
            }
        }
    }

    fun importContainer(
        contentResolver: android.content.ContentResolver,
        uri: android.net.Uri,
        containerId: String? = null
    ) {
        val container = containerId?.let { containerManager.getContainer(it) } ?: containerManager.getDefaultContainer()
        val targetDir = container?.rootDir ?: pRootEngine.rootfsDir
        val name = container?.name ?: "RootFS"
        viewModelScope.launch {
            terminalBridge.stopSession()
            _backupState.value = BackupState.Processing("Importing archive into $name container...", 0)
            try {
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    val success = rootfsManager.importContainerFromStream(inputStream, targetDir) { msg, percent ->
                        _backupState.value = BackupState.Processing(msg, percent)
                    }
                    if (success) {
                        refreshStatus()
                        _backupState.value = BackupState.Success("$name container restored successfully!")
                    } else {
                        _backupState.value = BackupState.Error("Failed to import container archive.")
                    }
                } ?: run {
                    _backupState.value = BackupState.Error("Unable to read backup file stream.")
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error importing container", e)
                _backupState.value = BackupState.Error("Import failed: ${e.localizedMessage}")
            }
        }
    }

    fun dismissBackupStatus() {
        _backupState.value = BackupState.Idle
    }
}
