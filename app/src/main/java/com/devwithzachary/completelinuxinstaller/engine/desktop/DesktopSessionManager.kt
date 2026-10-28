package com.devwithzachary.completelinuxinstaller.engine.desktop

import android.content.Context
import android.util.Log
import com.devwithzachary.completelinuxinstaller.model.ContainerInstance
import com.devwithzachary.completelinuxinstaller.engine.ContainerManager
import com.devwithzachary.completelinuxinstaller.engine.InstallStepState
import com.devwithzachary.completelinuxinstaller.engine.PRootConfig
import com.devwithzachary.completelinuxinstaller.engine.PRootEngine
import com.devwithzachary.completelinuxinstaller.engine.RootfsMigrationManager
import com.devwithzachary.completelinuxinstaller.engine.SoftwareInstaller
import com.devwithzachary.completelinuxinstaller.model.DistroCatalog
import com.devwithzachary.completelinuxinstaller.model.SoftwarePackage
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File
import java.net.InetSocketAddress
import java.net.Socket

class DesktopSessionManager(
    private val context: Context,
    private val pRootEngine: PRootEngine,
    private val containerManager: ContainerManager,
    private val softwareInstaller: SoftwareInstaller
) {
    companion object {
        private const val TAG = "DesktopSessionManager"
        private const val PREFS_NAME = "desktop_session_prefs"
        private const val KEY_UI_SCALE_PERCENT = "ui_scale_percent"
        private const val KEY_TOUCH_MODE = "touch_mode"
        const val DEFAULT_VNC_PORT = 5901
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val savedUiScale = prefs.getInt(KEY_UI_SCALE_PERCENT, 100).coerceIn(75, 300)
    private val savedTouchModeName = prefs.getString(KEY_TOUCH_MODE, DesktopTouchMode.TRACKPAD.name)
    private val initialTouchMode = try {
        DesktopTouchMode.valueOf(savedTouchModeName ?: DesktopTouchMode.TRACKPAD.name)
    } catch (_: Exception) {
        DesktopTouchMode.TRACKPAD
    }

    private val managerScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var sessionJob: Job? = null
    private var vncServerProcess: Process? = null

    val vncClient = VncClient()

    private val _desktopState = MutableStateFlow(
        DesktopUiState(
            resolution = DesktopResolution.AUTO.copy(scalePercent = savedUiScale),
            uiScalePercent = savedUiScale,
            touchMode = initialTouchMode
        )
    )
    val desktopState: StateFlow<DesktopUiState> = _desktopState.asStateFlow()

    private val _isDesktopRunning = MutableStateFlow(false)
    val isDesktopRunning: StateFlow<Boolean> = _isDesktopRunning.asStateFlow()

    init {
        // Synchronize VNC connection state with desktop state
        managerScope.launch {
            vncClient.connectionState.collect { vncState ->
                when (vncState) {
                    is VncConnectionState.Connected -> {
                        _isDesktopRunning.value = true
                        _desktopState.update { it.copy(sessionStatus = DesktopStatus.RUNNING, errorMessage = null) }
                    }
                    is VncConnectionState.Disconnected -> {
                        if (_desktopState.value.sessionStatus == DesktopStatus.RUNNING) {
                            _desktopState.update { it.copy(sessionStatus = DesktopStatus.IDLE) }
                            _isDesktopRunning.value = false
                        }
                    }
                    is VncConnectionState.Error -> {
                        if (_desktopState.value.sessionStatus == DesktopStatus.RUNNING || _desktopState.value.sessionStatus == DesktopStatus.STARTING_SERVER) {
                            _desktopState.update { it.copy(sessionStatus = DesktopStatus.ERROR, errorMessage = vncState.message) }
                            _isDesktopRunning.value = false
                        }
                    }
                    is VncConnectionState.Connecting -> {
                        // Keep current starting state
                    }
                }
            }
        }

        val defaultContainer = containerManager.getDefaultContainer()
        if (defaultContainer != null) {
            updateSelectedContainer(defaultContainer.id)
        }
    }

    private var availableCanvasWidth: Int = 1280
    private var availableCanvasHeight: Int = 720

    fun updateAvailableScreenSpace(width: Int, height: Int) {
        if (width > 0 && height > 0) {
            val clampedW = ((width / 2) * 2).coerceIn(320, 3840)
            val clampedH = ((height / 2) * 2).coerceIn(320, 3840)
            availableCanvasWidth = clampedW
            availableCanvasHeight = clampedH
        }
    }

    fun getEffectiveResolution(
        resolution: DesktopResolution = _desktopState.value.resolution,
        scalePercent: Int = _desktopState.value.uiScalePercent
    ): DesktopResolution {
        if (resolution.isAutomatic) {
            val scaleFactor = (scalePercent.coerceIn(75, 300)) / 100f
            val rawW = (availableCanvasWidth / scaleFactor).toInt()
            val rawH = (availableCanvasHeight / scaleFactor).toInt()
            val effW = ((rawW / 2) * 2).coerceIn(320, 3840)
            val effH = ((rawH / 2) * 2).coerceIn(320, 3840)
            val scaleSuffix = if (scalePercent != 100) " @ ${scalePercent}%" else ""
            return DesktopResolution(
                width = effW,
                height = effH,
                label = "Automatic (${effW}x${effH}$scaleSuffix)",
                isAutomatic = true,
                scalePercent = scalePercent
            )
        }
        return resolution
    }

    fun isDesktopInstalled(containerId: String? = null): Boolean {
        val container = resolveContainer(containerId) ?: return false
        val rootfsDir = container.rootDir
        if (!container.isInstalled || !rootfsDir.exists()) return false

        val hasVnc = SoftwarePackage.isBinaryPresent(rootfsDir, "usr/bin/vncserver") ||
                SoftwarePackage.isBinaryPresent(rootfsDir, "usr/bin/tigervncserver") ||
                SoftwarePackage.isBinaryPresent(rootfsDir, "usr/bin/Xvnc")
        val hasXfce = SoftwarePackage.isBinaryPresent(rootfsDir, "usr/bin/startxfce4") ||
                SoftwarePackage.isBinaryPresent(rootfsDir, "usr/bin/xfce4-session") ||
                SoftwarePackage.isBinaryPresent(rootfsDir, "usr/bin/xfwm4")

        val packageVersions = RootfsMigrationManager.readPackageVersions(rootfsDir)
        val isInstalled = (hasVnc && hasXfce) || packageVersions.containsKey("xfce_desktop")
        return isInstalled
    }

    fun isDesktopUpgradeAvailable(containerId: String? = null): Boolean {
        val container = resolveContainer(containerId) ?: return false
        val rootfsDir = container.rootDir
        if (!container.isInstalled || !rootfsDir.exists()) return false

        val packageVersions = RootfsMigrationManager.readPackageVersions(rootfsDir)
        val installedVersion = packageVersions["xfce_desktop"] ?: return false
        val distroDef = DistroCatalog.getById(container.distroId.ifBlank { container.distroName })
        val effectiveVersion = distroDef.getSoftwarePackageVersion("xfce_desktop")
            ?: SoftwarePackage.getPresets().find { it.id == "xfce_desktop" }?.version ?: 1
        return installedVersion < effectiveVersion
    }

    fun resolveContainer(containerId: String? = null): ContainerInstance? {
        return if (!containerId.isNullOrBlank()) {
            containerManager.getContainer(containerId)
        } else {
            containerManager.getDefaultContainer()
        }
    }

    fun getContainerUsers(container: ContainerInstance): List<String> {
        val rootDir = container.rootDir
        val passwdFile = File(rootDir, "etc/passwd")
        val normalUsers = mutableListOf<String>()
        if (passwdFile.exists()) {
            try {
                passwdFile.readLines().forEach { line ->
                    val parts = line.split(":")
                    if (parts.size >= 3) {
                        val name = parts[0]
                        val uid = parts[2].toIntOrNull() ?: -1
                        if (uid >= 1000 && name != "nobody" && name.isNotBlank()) {
                            normalUsers.add(name)
                        }
                    }
                }
            } catch (_: Exception) {}
        }
        val result = mutableListOf<String>()
        if (container.defaultUser.isNotBlank() && container.defaultUser != "root") {
            result.add(container.defaultUser)
        }
        normalUsers.forEach { if (!result.contains(it)) result.add(it) }
        if (!result.contains("root")) {
            result.add("root")
        }
        return result
    }

    fun updateSelectedContainer(containerId: String) {
        val container = containerManager.getContainer(containerId) ?: return
        val installed = isDesktopInstalled(containerId)
        val upgradeAvailable = isDesktopUpgradeAvailable(containerId)
        val users = getContainerUsers(container)
        val defaultUser = container.defaultUser.ifBlank { users.firstOrNull { it != "root" } ?: "root" }
        _desktopState.update {
            it.copy(
                activeContainerId = container.id,
                activeContainerName = container.name,
                isPackagesInstalled = installed,
                hasUpgradeAvailable = upgradeAvailable,
                availableUsers = users,
                selectedUser = if (users.contains(it.selectedUser)) it.selectedUser else defaultUser
            )
        }
    }

    fun setSelectedUser(username: String) {
        if (_desktopState.value.availableUsers.contains(username) || username == "root") {
            _desktopState.update { it.copy(selectedUser = username) }
        }
    }

    fun setResolution(resolution: DesktopResolution) {
        if (resolution.isAutomatic && resolution.scalePercent > 0 && resolution != DesktopResolution.AUTO) {
            val clamped = resolution.scalePercent.coerceIn(75, 300)
            prefs.edit().putInt(KEY_UI_SCALE_PERCENT, clamped).apply()
            _desktopState.update {
                it.copy(
                    resolution = resolution,
                    uiScalePercent = clamped
                )
            }
        } else if (resolution.isAutomatic) {
            val activeScale = _desktopState.value.uiScalePercent
            _desktopState.update {
                it.copy(
                    resolution = resolution.copy(scalePercent = activeScale)
                )
            }
        } else {
            _desktopState.update { it.copy(resolution = resolution) }
        }
    }

    fun setUiScalePercent(percent: Int) {
        val clamped = percent.coerceIn(75, 300)
        prefs.edit().putInt(KEY_UI_SCALE_PERCENT, clamped).apply()
        _desktopState.update {
            it.copy(
                uiScalePercent = clamped,
                resolution = if (it.resolution.isAutomatic) it.resolution.copy(scalePercent = clamped) else it.resolution
            )
        }
        if (_desktopState.value.sessionStatus == DesktopStatus.RUNNING && _desktopState.value.resolution.isAutomatic) {
            restartDesktopSessionWithResolution(getEffectiveResolution(scalePercent = clamped))
        }
    }

    fun setTouchMode(mode: DesktopTouchMode) {
        prefs.edit().putString(KEY_TOUCH_MODE, mode.name).apply()
        _desktopState.update { it.copy(touchMode = mode) }
    }


    fun toggleFitToScreen() {
        _desktopState.update { it.copy(isFitToScreen = !it.isFitToScreen) }
    }

    fun toggleCtrl() {
        val next = !_desktopState.value.isCtrlActive
        _desktopState.update { it.copy(isCtrlActive = next) }
        vncClient.sendKeyEvent(VncKeyMapper.KEY_CONTROL_LEFT, next)
    }

    fun toggleAlt() {
        val next = !_desktopState.value.isAltActive
        _desktopState.update { it.copy(isAltActive = next) }
        vncClient.sendKeyEvent(VncKeyMapper.KEY_ALT_LEFT, next)
    }

    fun toggleShift() {
        val next = !_desktopState.value.isShiftActive
        _desktopState.update { it.copy(isShiftActive = next) }
        vncClient.sendKeyEvent(VncKeyMapper.KEY_SHIFT_LEFT, next)
    }

    fun sendSpecialKey(keysym: Int) {
        managerScope.launch {
            vncClient.sendKeyEvent(keysym, down = true)
            delay(50)
            vncClient.sendKeyEvent(keysym, down = false)

            // Auto-release modifier keys if pressed
            if (_desktopState.value.isCtrlActive) {
                toggleCtrl()
            }
            if (_desktopState.value.isAltActive) {
                toggleAlt()
            }
            if (_desktopState.value.isShiftActive) {
                toggleShift()
            }
        }
    }

    fun sendTextString(text: String) {
        managerScope.launch {
            for (char in text) {
                val keysym = VncKeyMapper.charToKeysym(char)
                vncClient.sendKeyEvent(keysym, down = true)
                delay(10)
                vncClient.sendKeyEvent(keysym, down = false)
                delay(10)
            }
        }
    }

    fun startDesktopSession(
        containerId: String? = null,
        resolution: DesktopResolution? = null,
        loginUser: String? = null
    ) {
        val container = resolveContainer(containerId)
        if (container == null) {
            _desktopState.update {
                it.copy(
                    sessionStatus = DesktopStatus.ERROR,
                    errorMessage = "No Linux container is installed. Install a distribution first."
                )
            }
            return
        }

        val users = getContainerUsers(container)
        val userToUse = loginUser?.ifBlank { null }
            ?: if (_desktopState.value.selectedUser.isNotBlank() && users.contains(_desktopState.value.selectedUser)) {
                _desktopState.value.selectedUser
            } else {
                container.defaultUser.ifBlank { users.firstOrNull { it != "root" } ?: "root" }
            }

        val chosenRes = resolution ?: _desktopState.value.resolution
        val effectiveRes = getEffectiveResolution(chosenRes)
        _desktopState.update {
            it.copy(
                activeContainerId = container.id,
                activeContainerName = container.name,
                resolution = chosenRes,
                selectedUser = userToUse,
                availableUsers = users,
                errorMessage = null
            )
        }

        val installed = isDesktopInstalled(container.id)
        if (!installed) {
            // Automatically run package installation pipeline first
            installDesktopPackages(container.id, autoStartSessionAfter = true)
            return
        }

        launchServerAndConnect(container, effectiveRes, userToUse)
    }

    fun installDesktopPackages(
        containerId: String,
        autoStartSessionAfter: Boolean = true
    ) {
        val container = resolveContainer(containerId) ?: return
        val targetDir = container.rootDir
        val distroDef = DistroCatalog.getById(container.distroId.ifBlank { container.distroName })
        val xfcePreset = SoftwarePackage.getPresetsForContainer(container).find { it.id == "xfce_desktop" }
            ?: SoftwarePackage.getPresets().find { it.id == "xfce_desktop" }!!

        _desktopState.update {
            it.copy(
                activeContainerId = container.id,
                activeContainerName = container.name,
                sessionStatus = DesktopStatus.INSTALLING_PACKAGES,
                packageInstallProgress = "Preparing XFCE 4 desktop installation...",
                packageInstallLogs = "Starting XFCE 4 Desktop environment installation into ${container.name}...\n",
                errorMessage = null
            )
        }

        managerScope.launch {
            try {
                softwareInstaller.installPackage(xfcePreset, targetDir, distroDef).collect { step ->
                    when (step) {
                        is InstallStepState.Progress -> {
                            _desktopState.update { state ->
                                state.copy(
                                    packageInstallProgress = step.logLine,
                                    packageInstallLogs = state.packageInstallLogs + step.logLine + "\n"
                                )
                            }
                        }
                        is InstallStepState.Success -> {
                            _desktopState.update { state ->
                                state.copy(
                                    isPackagesInstalled = true,
                                    packageInstallProgress = "Installation completed successfully!",
                                    packageInstallLogs = state.packageInstallLogs + "Installation completed successfully!\n"
                                )
                            }
                            if (autoStartSessionAfter) {
                                delay(1000)
                                launchServerAndConnect(container, getEffectiveResolution(_desktopState.value.resolution), _desktopState.value.selectedUser)
                            } else {
                                val upgradeAvailable = isDesktopUpgradeAvailable(container.id)
                                _desktopState.update { it.copy(sessionStatus = DesktopStatus.IDLE, hasUpgradeAvailable = upgradeAvailable) }
                            }
                        }
                        is InstallStepState.Error -> {
                            _desktopState.update { state ->
                                state.copy(
                                    sessionStatus = DesktopStatus.ERROR,
                                    errorMessage = "Package installation failed: ${step.errorMessage}",
                                    packageInstallLogs = state.packageInstallLogs + "ERROR: ${step.errorMessage}\n"
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed installing desktop packages", e)
                _desktopState.update {
                    it.copy(
                        sessionStatus = DesktopStatus.ERROR,
                        errorMessage = "Installation error: ${e.message}"
                    )
                }
            }
        }
    }

    private suspend fun prepareVncEnvironment(container: ContainerInstance, cleanPass: String, targetUser: String) {
        val userHome = if (targetUser == "root") "/root" else "/home/$targetUser"
        val prepScript = buildString {
            append("rm -f /etc/tigervnc/vncserver-config-defaults 2>/dev/null || true; ")
            append("mkdir -p /tmp/.X11-unix /tmp/.ICE-unix /root/.vnc /etc/skel/.vnc 2>/dev/null || true; ")
            append("chmod 1777 /tmp/.X11-unix /tmp/.ICE-unix 2>/dev/null || true; ")
            append("vncserver -kill :1 2>/dev/null || true; ")
            append("pkill -9 -f Xvnc 2>/dev/null || true; ")
            append("rm -f /tmp/.X1-lock /tmp/.X11-unix/X1 /tmp/.X11-unix/X* /root/.vnc/*.pid /home/*/.vnc/*.pid 2>/dev/null || true; ")
            append("for u in /home/*; do if [ -d \"\$u\" ]; then mkdir -p \"\$u/.vnc\" \"\$u/.config/tigervnc\" && chmod -R 777 \"\$u/.vnc\" \"\$u/.config\" 2>/dev/null || true; (echo '$cleanPass' | vncpasswd -f > \"\$u/.config/tigervnc/passwd\" 2>/dev/null || true); (echo '$cleanPass' | vncpasswd -f > \"\$u/.vnc/passwd\" 2>/dev/null || true); chmod 600 \"\$u/.vnc/passwd\" \"\$u/.config/tigervnc/passwd\" 2>/dev/null || true; fi; done; ")
            append("(echo '$cleanPass' | vncpasswd -f > /root/.vnc/passwd 2>/dev/null || echo '$cleanPass' | tigervncpasswd -f > /root/.vnc/passwd 2>/dev/null || true); ")
            append("chmod 600 /root/.vnc/passwd 2>/dev/null || true; ")
            append("mkdir -p $userHome/.vnc $userHome/.config/tigervnc 2>/dev/null || true; ")
            append("[ -f /etc/vnc/xstartup ] && cp /etc/vnc/xstartup $userHome/.vnc/xstartup 2>/dev/null || true; ")
            append("chmod 755 $userHome/.vnc/xstartup 2>/dev/null || true; ")
            if (targetUser != "root") {
                append("chown -R $targetUser:$targetUser $userHome/.vnc $userHome/.config 2>/dev/null || true")
            }
        }
        executeOneShotCommand(container, prepScript, timeoutMs = 8000L)
    }

    private fun launchServerAndConnect(
        container: ContainerInstance,
        resolution: DesktopResolution,
        loginUser: String = _desktopState.value.selectedUser
    ) {
        sessionJob?.cancel()
        sessionJob = managerScope.launch {
            try {
                _desktopState.update { it.copy(sessionStatus = DesktopStatus.STARTING_SERVER, errorMessage = null) }

                val cleanPass = container.effectiveVncPassword.ifBlank { "user" }
                val width = resolution.width
                val height = resolution.height

                Log.d(TAG, "Preparing VNC environment for ${container.name} (user: $loginUser)...")
                prepareVncEnvironment(container, cleanPass, loginUser)

                val vncBaseCommand = "vncserver :1 -xstartup /etc/vnc/xstartup -geometry ${width}x${height} -depth 24 -SecurityTypes VncAuth,None -UseBlacklist=0 -localhost"
                Log.d(TAG, "Starting VNC server process as $loginUser: $vncBaseCommand")

                val targetDir = container.rootDir
                val shellPath = when {
                    File(targetDir, "usr/bin/bash").exists() || File(targetDir, "bin/bash").exists() -> "/bin/bash"
                    File(targetDir, "usr/bin/dash").exists() || File(targetDir, "bin/dash").exists() -> "/usr/bin/dash"
                    else -> "/bin/sh"
                }
                val suBin = when {
                    File(targetDir, "usr/bin/su").exists() -> "/usr/bin/su"
                    File(targetDir, "bin/su").exists() -> "/bin/su"
                    else -> "su"
                }
                val fullCommand = if (loginUser != "root") {
                    val innerCmd = "cd /home/$loginUser && export USER=$loginUser && export LOGNAME=$loginUser && export HOME=/home/$loginUser && $vncBaseCommand"
                    "$suBin -s $shellPath $loginUser -c \"$innerCmd\""
                } else {
                    vncBaseCommand
                }

                val cmdList = pRootEngine.buildPRootCommand(
                    config = PRootConfig(
                        rootfsDir = targetDir,
                        tmpDir = pRootEngine.tmpDir,
                        workingDir = if (loginUser == "root") "/root" else "/home/$loginUser"
                    ),
                    command = listOf(shellPath, "-c", fullCommand)
                )

                val pb = ProcessBuilder(cmdList)
                pb.directory(pRootEngine.tmpDir)
                pb.environment().putAll(pRootEngine.getEnvironmentVariables(loginUser = loginUser))
                pb.redirectErrorStream(true)
                val proc = pb.start()
                vncServerProcess = proc

                val capturedLogs = mutableListOf<String>()
                val logJob = launch(Dispatchers.IO) {
                    try {
                        proc.inputStream.bufferedReader().useLines { lines ->
                            lines.forEach { line ->
                                Log.d(TAG, "[TigerVNC] $line")
                                synchronized(capturedLogs) {
                                    if (capturedLogs.size > 50) capturedLogs.removeAt(0)
                                    capturedLogs.add(line)
                                }
                            }
                        }
                    } catch (_: Exception) {}
                }

                // Poll for port 5901 to become available (up to 15 seconds)
                var isPortOpen = false
                val startTime = System.currentTimeMillis()
                while (System.currentTimeMillis() - startTime < 15000L) {
                    if (isTcpPortOpen("127.0.0.1", DEFAULT_VNC_PORT)) {
                        isPortOpen = true
                        break
                    }
                    if (!proc.isAlive && proc.exitValue() != 0) {
                        Log.w(TAG, "PRoot VNC process exited early with failure code: ${proc.exitValue()}")
                        break
                    }
                    delay(250)
                }

                if (!isPortOpen) {
                    val recent = synchronized(capturedLogs) { capturedLogs.joinToString("\n") }
                    val errorDetails = if (!proc.isAlive && proc.exitValue() != 0) {
                        "VNC server exited with code ${proc.exitValue()}.\n$recent"
                    } else {
                        "VNC server did not start on port $DEFAULT_VNC_PORT within 15 seconds.\n$recent"
                    }
                    throw IllegalStateException(errorDetails.trim())
                }

                Log.i(TAG, "VNC server is ready on port $DEFAULT_VNC_PORT! Connecting VncClient...")
                delay(300)
                vncClient.connect("127.0.0.1", DEFAULT_VNC_PORT, cleanPass)

            } catch (e: CancellationException) {
                Log.d(TAG, "Launch session cancelled")
            } catch (e: Exception) {
                Log.e(TAG, "Failed starting desktop session", e)
                _desktopState.update {
                    it.copy(
                        sessionStatus = DesktopStatus.ERROR,
                        errorMessage = e.message ?: "Failed starting desktop server"
                    )
                }
                _isDesktopRunning.value = false
            }
        }
    }

    fun stopDesktopSession() {
        val container = resolveContainer(_desktopState.value.activeContainerId)
        vncClient.disconnect()
        sessionJob?.cancel()

        managerScope.launch {
            _desktopState.update { it.copy(sessionStatus = DesktopStatus.STOPPING) }
            if (container != null) {
                val stopScript = "vncserver -kill :1 2>/dev/null || true; pkill -9 -f Xvnc 2>/dev/null || true; rm -f /tmp/.X1-lock /tmp/.X11-unix/X1 /tmp/.X11-unix/X* /root/.vnc/*.pid /home/*/.vnc/*.pid 2>/dev/null || true"
                executeOneShotCommand(container, stopScript, timeoutMs = 5000L)
            }
            vncServerProcess?.let { proc ->
                if (proc.isAlive) {
                    proc.destroy()
                }
            }
            vncServerProcess = null
            _isDesktopRunning.value = false
            _desktopState.update { it.copy(sessionStatus = DesktopStatus.IDLE, errorMessage = null) }
        }
    }

    fun restartDesktopSession() {
        val container = resolveContainer(_desktopState.value.activeContainerId) ?: return
        val currentRes = getEffectiveResolution(_desktopState.value.resolution)
        val user = _desktopState.value.selectedUser
        managerScope.launch {
            stopDesktopSession()
            delay(800)
            launchServerAndConnect(container, currentRes, user)
        }
    }

    fun restartDesktopSessionWithResolution(resolution: DesktopResolution) {
        val container = resolveContainer(_desktopState.value.activeContainerId) ?: return
        setResolution(resolution)
        updateAvailableScreenSpace(resolution.width, resolution.height)
        val targetRes = getEffectiveResolution(resolution)
        val user = _desktopState.value.selectedUser
        managerScope.launch {
            stopDesktopSession()
            delay(800)
            launchServerAndConnect(container, targetRes, user)
        }
    }

    fun restartDesktopSessionWithUser(username: String) {
        val container = resolveContainer(_desktopState.value.activeContainerId) ?: return
        setSelectedUser(username)
        val currentRes = getEffectiveResolution(_desktopState.value.resolution)
        managerScope.launch {
            stopDesktopSession()
            delay(800)
            launchServerAndConnect(container, currentRes, username)
        }
    }

    private suspend fun executeOneShotCommand(
        container: ContainerInstance,
        command: String,
        timeoutMs: Long = 8000L
    ): Pair<Int, String> = withContext(Dispatchers.IO) {
        val targetDir = container.rootDir
        val shellPath = when {
            File(targetDir, "usr/bin/bash").exists() || File(targetDir, "bin/bash").exists() -> "/bin/bash"
            File(targetDir, "usr/bin/dash").exists() || File(targetDir, "bin/dash").exists() -> "/usr/bin/dash"
            else -> "/bin/sh"
        }
        val cmdList = pRootEngine.buildPRootCommand(
            config = PRootConfig(rootfsDir = targetDir, tmpDir = pRootEngine.tmpDir),
            command = listOf(shellPath, "-c", command)
        )
        val pb = ProcessBuilder(cmdList)
        pb.directory(pRootEngine.tmpDir)
        pb.environment().putAll(pRootEngine.getEnvironmentVariables())
        pb.redirectErrorStream(true)
        val proc = pb.start()

        val outputBuilder = StringBuilder()
        val readerJob = launch {
            try {
                proc.inputStream.bufferedReader().useLines { lines ->
                    lines.forEach { line ->
                        outputBuilder.appendLine(line)
                    }
                }
            } catch (_: Exception) {}
        }

        val completed = withTimeoutOrNull(timeoutMs) {
            val exit = proc.waitFor()
            readerJob.join()
            exit
        }

        if (completed == null) {
            proc.destroy()
            readerJob.cancel()
            Pair(-1, "Command timed out after ${timeoutMs}ms:\n$outputBuilder")
        } else {
            Pair(completed, outputBuilder.toString().trim())
        }
    }

    private fun isTcpPortOpen(host: String, port: Int): Boolean {
        return try {
            Socket().use { s ->
                s.connect(InetSocketAddress(host, port), 200)
                true
            }
        } catch (_: Exception) {
            false
        }
    }
}
