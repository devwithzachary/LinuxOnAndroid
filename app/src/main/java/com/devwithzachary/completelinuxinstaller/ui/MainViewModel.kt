package com.devwithzachary.completelinuxinstaller.ui

import android.app.Application
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
import java.io.File
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
    val containerUsers: List<String> = emptyList()
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val pRootEngine = PRootEngine(application)
    val rootfsManager = RootfsManager(application, pRootEngine)
    val softwareInstaller = SoftwareInstaller(pRootEngine)
    val terminalBridge = TerminalBridge(pRootEngine)

    private val _dashboardState = MutableStateFlow(DashboardUiState())
    val dashboardState: StateFlow<DashboardUiState> = _dashboardState.asStateFlow()

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    private val _packages = MutableStateFlow(SoftwarePackage.getPresets())
    val packages: StateFlow<List<SoftwarePackage>> = _packages.asStateFlow()

    val isSessionRunning = terminalBridge.isRunning

    init {
        viewModelScope.launch {
            pRootEngine.ensurePRootExecutable()
            refreshStatus()
            kotlinx.coroutines.delay(800)
            _dashboardState.value = _dashboardState.value.copy(isInitializing = false)
        }
    }

    fun refreshStatus() {
        val installed = rootfsManager.isInstalled()
        val rootfsDir = pRootEngine.rootfsDir

        val hasVnc = installed && (
            File(rootfsDir, "usr/bin/vncserver").exists() ||
            File(rootfsDir, "usr/bin/tigervncserver").exists() ||
            File(rootfsDir, "usr/bin/startxfce4").exists()
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
                    "xfce_desktop" -> if (File(rootfsDir, "usr/bin/startxfce4").exists()) InstallStatus.INSTALLED else pkg.status
                    "python_dev" -> if (File(rootfsDir, "usr/bin/python3").exists()) InstallStatus.INSTALLED else pkg.status
                    "node_dev" -> if (File(rootfsDir, "usr/bin/node").exists()) InstallStatus.INSTALLED else pkg.status
                    "android_dev" -> if (File(rootfsDir, "usr/bin/adb").exists()) InstallStatus.INSTALLED else pkg.status
                    "nginx_web" -> if (File(rootfsDir, "usr/sbin/nginx").exists()) InstallStatus.INSTALLED else pkg.status
                    "openssh_server" -> if (File(rootfsDir, "usr/sbin/sshd").exists()) InstallStatus.INSTALLED else pkg.status
                    else -> {
                        if (pkg.id.startsWith("custom_")) {
                            val binaryName = pkg.id.removePrefix("custom_")
                            if (File(rootfsDir, "usr/bin/$binaryName").exists() || File(rootfsDir, "usr/sbin/$binaryName").exists()) {
                                InstallStatus.INSTALLED
                            } else pkg.status
                        } else pkg.status
                    }
                }
                pkg.copy(status = actualStatus)
            }
        }
        _packages.value = syncedPackages

        _dashboardState.value = _dashboardState.value.copy(
            isInstalled = installed,
            isRunning = terminalBridge.isRunning.value,
            isVncInstalled = hasVnc,
            isNginxInstalled = hasNginx,
            isSshInstalled = hasSsh,
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
            rootfsManager.setRootPassword(rootPassword)
            rootfsManager.createOrUpdateUser(username, userPassword, isSudo = true)
            refreshStatus()
            terminalBridge.startSession()
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
            refreshStatus()
        }
    }

    fun startTerminalSession() {
        terminalBridge.startSession()
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
            _packages.value = SoftwarePackage.getPresets()
            refreshStatus()
        }
    }
}
