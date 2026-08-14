package com.devwithzachary.completelinuxinstaller.ui

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.devwithzachary.completelinuxinstaller.engine.DownloadState
import com.devwithzachary.completelinuxinstaller.engine.InstallStepState
import com.devwithzachary.completelinuxinstaller.engine.PRootEngine
import com.devwithzachary.completelinuxinstaller.engine.RootfsManager
import com.devwithzachary.completelinuxinstaller.engine.SoftwareInstaller
import com.devwithzachary.completelinuxinstaller.engine.TerminalBridge
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
import kotlinx.coroutines.launch

data class DashboardUiState(
    val isInitializing: Boolean = true,
    val isInstalled: Boolean = false,
    val storageUsedMb: Long = 0L,
    val distroName: String = "Ubuntu 26.04 LTS (ARM64/x86_64)",
    val bindSdCard: Boolean = true,
    val isRunning: Boolean = false,
    val isVncInstalled: Boolean = false,
    val isNginxInstalled: Boolean = false,
    val isSshInstalled: Boolean = false,
    val sshPort: Int = 2222,
    val containerUsers: List<String> = emptyList()
)

sealed class BackupState {
    data object Idle : BackupState()
    data class Processing(val message: String, val progressPercent: Int = -1) : BackupState()
    data class Success(val message: String) : BackupState()
    data class Error(val message: String) : BackupState()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val pRootEngine = PRootEngine(application)
    val rootfsManager = RootfsManager(application, pRootEngine)
    val softwareInstaller = SoftwareInstaller(pRootEngine)
    val terminalBridge = TerminalBridge(pRootEngine)

    private val prefs = application.getSharedPreferences("terminal_theme_prefs", Context.MODE_PRIVATE)

    private fun loadSshPort(): Int {
        val port = prefs.getInt("ssh_port", 2222)
        return if (port in 1..65535) port else 2222
    }

    private val _sshPort = MutableStateFlow(loadSshPort())
    val sshPort: StateFlow<Int> = _sshPort.asStateFlow()

    private val _terminalTheme = MutableStateFlow(loadTerminalTheme())
    val terminalTheme: StateFlow<TerminalTheme> = _terminalTheme.asStateFlow()

    private val _terminalFontSize = MutableStateFlow(loadTerminalFontSize())
    val terminalFontSize: StateFlow<Int> = _terminalFontSize.asStateFlow()

    private val _terminalFontFamily = MutableStateFlow(loadTerminalFontFamily())
    val terminalFontFamily: StateFlow<String> = _terminalFontFamily.asStateFlow()

    private val _dashboardState = MutableStateFlow(DashboardUiState(sshPort = loadSshPort()))
    val dashboardState: StateFlow<DashboardUiState> = _dashboardState.asStateFlow()

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    private val _backupState = MutableStateFlow<BackupState>(BackupState.Idle)
    val backupState: StateFlow<BackupState> = _backupState.asStateFlow()

    private val _packages = MutableStateFlow(SoftwarePackage.getPresets(loadSshPort()))
    val packages: StateFlow<List<SoftwarePackage>> = _packages.asStateFlow()

    private val _defaultTerminalUser = MutableStateFlow(loadDefaultTerminalUser())
    val defaultTerminalUser: StateFlow<String> = _defaultTerminalUser.asStateFlow()

    val isSessionRunning = terminalBridge.isRunning

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
        return prefs.getString("terminal_font_family", "Monospace") ?: "Monospace"
    }

    fun setTerminalFontSize(size: Int) {
        val clampedSize = size.coerceIn(10, 24)
        prefs.edit().putInt("terminal_font_size", clampedSize).apply()
        _terminalFontSize.value = clampedSize
    }

    fun setTerminalFontFamily(family: String) {
        prefs.edit().putString("terminal_font_family", family).apply()
        _terminalFontFamily.value = family
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

    init {
        viewModelScope.launch {
            pRootEngine.ensurePRootExecutable()
            refreshStatus()
            kotlinx.coroutines.delay(800)
            _dashboardState.value = _dashboardState.value.copy(isInitializing = false)
        }
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

    fun refreshStatus() {
        val installed = rootfsManager.isInstalled()
        val rootfsDir = pRootEngine.rootfsDir

        val hasVnc = installed && File(rootfsDir, "usr/bin/startxfce4").exists() && (
            File(rootfsDir, "usr/bin/vncserver").exists() ||
            File(rootfsDir, "usr/bin/tigervncserver").exists() ||
            File(rootfsDir, "usr/bin/tightvncserver").exists()
        )

        val hasNginx = installed && File(rootfsDir, "usr/sbin/nginx").exists()
        val hasSsh = installed && File(rootfsDir, "usr/sbin/sshd").exists()
        val users = if (installed) rootfsManager.getContainerUsers() else emptyList()

        // Sync individual package states against rootfs file system
        val syncedPackages = _packages.value.map { pkg ->
            if (!installed) {
                pkg.copy(status = InstallStatus.NOT_INSTALLED, progressMessage = "", installLogs = "")
            } else {
                val actualStatus = when (pkg.id) {
                    "xfce_desktop" -> if (File(rootfsDir, "usr/bin/startxfce4").exists() && (File(rootfsDir, "usr/bin/vncserver").exists() || File(rootfsDir, "usr/bin/tigervncserver").exists() || File(rootfsDir, "usr/bin/tightvncserver").exists())) InstallStatus.INSTALLED else InstallStatus.NOT_INSTALLED
                    "python_dev" -> if (File(rootfsDir, "usr/bin/python3").exists()) InstallStatus.INSTALLED else InstallStatus.NOT_INSTALLED
                    "node_dev" -> if (File(rootfsDir, "usr/bin/node").exists()) InstallStatus.INSTALLED else InstallStatus.NOT_INSTALLED
                    "android_dev" -> if (File(rootfsDir, "usr/bin/adb").exists()) InstallStatus.INSTALLED else InstallStatus.NOT_INSTALLED
                    "nginx_web" -> if (File(rootfsDir, "usr/sbin/nginx").exists()) InstallStatus.INSTALLED else InstallStatus.NOT_INSTALLED
                    "openssh_server" -> if (File(rootfsDir, "usr/sbin/sshd").exists()) InstallStatus.INSTALLED else InstallStatus.NOT_INSTALLED
                    else -> {
                        if (pkg.id.startsWith("custom_")) {
                            val binaryName = pkg.id.removePrefix("custom_")
                            if (File(rootfsDir, "usr/bin/$binaryName").exists() || File(rootfsDir, "usr/sbin/$binaryName").exists()) {
                                InstallStatus.INSTALLED
                            } else InstallStatus.NOT_INSTALLED
                        } else pkg.status
                    }
                }
                pkg.copy(status = actualStatus)
            }
        }
        _packages.value = syncedPackages

        val savedUser = prefs.getString("default_terminal_user", null)
        if (savedUser.isNullOrBlank()) {
            val autoDefault = users.firstOrNull() ?: "root"
            _defaultTerminalUser.value = autoDefault
        } else if (savedUser != "root" && !users.contains(savedUser)) {
            val autoDefault = users.firstOrNull() ?: "root"
            prefs.edit().putString("default_terminal_user", autoDefault).apply()
            _defaultTerminalUser.value = autoDefault
        }

        _dashboardState.value = _dashboardState.value.copy(
            isInstalled = installed,
            isRunning = terminalBridge.isRunning.value,
            isVncInstalled = hasVnc,
            isNginxInstalled = hasNginx,
            isSshInstalled = hasSsh,
            sshPort = _sshPort.value,
            containerUsers = users
        )

        // Asynchronously calculate folder disk usage on background thread to prevent UI thread ANR
        if (installed) {
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                val storage = rootfsManager.getStorageUsedMb()
                _dashboardState.value = _dashboardState.value.copy(storageUsedMb = storage)
            }
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

    fun changeRootPassword(newPassword: String) {
        viewModelScope.launch {
            rootfsManager.setRootPassword(newPassword)
            refreshStatus()
        }
    }

    fun createUser(username: String, password: String) {
        viewModelScope.launch {
            rootfsManager.createOrUpdateUser(username, password)
            refreshStatus()
        }
    }

    fun deleteUser(username: String) {
        viewModelScope.launch {
            rootfsManager.deleteUser(username)
            if (_defaultTerminalUser.value == username) {
                val remainingUsers = rootfsManager.getContainerUsers().filter { it != username }
                setDefaultTerminalUser(remainingUsers.firstOrNull() ?: "root")
            }
            refreshStatus()
        }
    }

    fun startTerminalSession() {
        terminalBridge.startSession(loginUser = _defaultTerminalUser.value)
        refreshStatus()
    }

    fun stopTerminalSession() {
        terminalBridge.stopSession()
        refreshStatus()
    }

    fun sendTerminalCommand(command: String) {
        terminalBridge.sendCommand(command)
    }

    fun sendCtrlC() {
        terminalBridge.sendCtrlC()
    }

    fun installCustomPackage(packageName: String) {
        val cleanName = packageName.trim()
        if (cleanName.isEmpty()) return

        val pkgId = "custom_" + cleanName.lowercase().replace(" ", "_")
        val currentList = _packages.value.toMutableList()

        val existingIdx = currentList.indexOfFirst { it.id == pkgId }
        val customPkg = SoftwarePackage(
            id = pkgId,
            name = "Apt Package: $cleanName",
            category = SoftwareCategory.UTILITIES,
            description = "Custom Ubuntu package '$cleanName' installed via apt-get.",
            iconName = "Terminal",
            installCommand = "export DEBIAN_FRONTEND=noninteractive && dpkg --configure -a && apt-get update && apt-get install -y -o Dpkg::Options::=\"--force-overwrite\" $cleanName"
        )

        if (existingIdx != -1) {
            currentList[existingIdx] = customPkg
        } else {
            currentList.add(0, customPkg)
        }

        _packages.value = currentList
        installSoftwarePackage(pkgId)
    }

    fun installSoftwarePackage(packageId: String) {
        val currentPackages = _packages.value.toMutableList()
        val index = currentPackages.indexOfFirst { it.id == packageId }
        if (index == -1) return

        val pkg = currentPackages[index]
        currentPackages[index] = pkg.copy(
            status = InstallStatus.INSTALLING,
            progressMessage = "Initializing installation...",
            installLogs = "Starting installation of ${pkg.name}...\nExecuting script: ${pkg.installCommand}\n"
        )
        _packages.value = currentPackages

        viewModelScope.launch {
            softwareInstaller.installPackage(pkg).collect { step ->
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

    fun exportContainer(contentResolver: android.content.ContentResolver, uri: android.net.Uri) {
        viewModelScope.launch {
            _backupState.value = BackupState.Processing("Exporting RootFS container archive...", 0)
            try {
                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    val success = rootfsManager.exportContainerToStream(outputStream) { msg, percent ->
                        _backupState.value = BackupState.Processing(msg, percent)
                    }
                    if (success) {
                        _backupState.value = BackupState.Success("RootFS container exported successfully!")
                    } else {
                        _backupState.value = BackupState.Error("Failed to export RootFS container archive.")
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

    fun importContainer(contentResolver: android.content.ContentResolver, uri: android.net.Uri) {
        viewModelScope.launch {
            terminalBridge.stopSession()
            _backupState.value = BackupState.Processing("Importing RootFS container archive...", 0)
            try {
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    val success = rootfsManager.importContainerFromStream(inputStream) { msg, percent ->
                        _backupState.value = BackupState.Processing(msg, percent)
                    }
                    if (success) {
                        refreshStatus()
                        _backupState.value = BackupState.Success("RootFS container restored successfully!")
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
