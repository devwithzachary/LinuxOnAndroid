package com.devwithzachary.completelinuxinstaller.engine

import android.system.Os
import android.util.Log
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TerminalSession(
    val id: String,
    initialTitle: String,
    val containerId: String = "ubuntu_default",
    val containerName: String = "Ubuntu",
    val loginUser: String = "root"
) {
    companion object {
        private const val TAG = "TerminalSession"
    }

    private val sessionScope = CoroutineScope(Dispatchers.IO + Job())
    private val writeDispatcher = Dispatchers.IO.limitedParallelism(1)

    private val _title = MutableStateFlow(initialTitle)
    val title: StateFlow<String> = _title.asStateFlow()

    fun setTitle(newTitle: String) {
        _title.value = newTitle
    }

    val emulator = TerminalEmulator(cols = 80, rows = 24)

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _refreshTrigger = MutableStateFlow(0L)
    val refreshTrigger: StateFlow<Long> = _refreshTrigger.asStateFlow()

    private var ptyProcess: PtyProcess? = null
    private var sessionJob: Job? = null

    fun startSession(
        pRootEngine: PRootEngine,
        rootfsDir: File = pRootEngine.rootfsDir,
        defaultShell: String? = null
    ) {
        if (_isRunning.value) return

        sessionJob = sessionScope.launch {
            try {
                _isRunning.value = true
                val customConfig = PRootConfig(
                    rootfsDir = rootfsDir,
                    tmpDir = pRootEngine.tmpDir
                )

                val hasRealBash = (File(rootfsDir, "bin/bash").exists() && File(rootfsDir, "bin/bash").length() > 1000) ||
                                  (File(rootfsDir, "usr/bin/bash").exists() && File(rootfsDir, "usr/bin/bash").length() > 1000)
                val effectiveShell = when {
                    defaultShell != null && (File(rootfsDir, defaultShell.removePrefix("/")).exists() || defaultShell == "/bin/sh") -> defaultShell
                    hasRealBash -> if (File(rootfsDir, "bin/bash").exists()) "/bin/bash" else "/usr/bin/bash"
                    File(rootfsDir, "bin/ash").exists() -> "/bin/ash"
                    File(rootfsDir, "bin/sh").exists() -> "/bin/sh"
                    else -> "/bin/sh"
                }

                val cmdList = pRootEngine.buildPRootCommand(
                    config = customConfig,
                    command = listOf(effectiveShell, "-l"),
                    loginUser = loginUser
                )
                val cmdPath = cmdList[0]
                val args = cmdList.toTypedArray()

                val hostName = ContainerManager.formatContainerHostname(containerName)
                val envMap = pRootEngine.getEnvironmentVariables(loginUser = loginUser).toMutableMap()
                envMap["TERM"] = "xterm-256color"
                envMap["COLORTERM"] = "truecolor"
                envMap["PROOT_NO_SECCOMP"] = "1"
                envMap["PROOT_FORCE_SETID"] = "1"
                envMap["PROOT_LINK2SYMLINK"] = "1"
                envMap["HOSTNAME"] = hostName
                envMap["PATH"] = "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"

                val envArray = envMap.map { "${it.key}=${it.value}" }.toTypedArray()

                val outPid = IntArray(1)
                val masterFd = PtyNative.createSubprocess(
                    cmdPath = cmdPath,
                    args = args,
                    env = envArray,
                    cwdPath = rootfsDir.absolutePath,
                    cols = emulator.cols,
                    rows = emulator.rows,
                    outPid = outPid
                )

                if (masterFd < 0) {
                    Log.e(TAG, "Failed to create PTY subprocess for session $id")
                    _isRunning.value = false
                    return@launch
                }

                val proc = PtyProcess(masterFdInt = masterFd, pid = outPid[0])
                ptyProcess = proc

                Log.d(TAG, "PTY Subprocess [$id - ${title.value}] launched successfully with PID ${outPid[0]}")

                val buffer = ByteArray(4096)
                while (_isRunning.value) {
                    val bytesRead = try {
                        proc.inputStream.read(buffer)
                    } catch (_: Exception) {
                        -1
                    }

                    if (bytesRead <= 0) break

                    emulator.appendBytes(buffer, bytesRead)
                    _refreshTrigger.value = System.currentTimeMillis()
                }

                PtyNative.waitForProcess(outPid[0])
            } catch (e: Exception) {
                Log.e(TAG, "Error in PTY session $id", e)
            } finally {
                _isRunning.value = false
                ptyProcess = null
                _refreshTrigger.value = System.currentTimeMillis()
            }
        }
    }

    fun stopSession() {
        _isRunning.value = false
        try {
            ptyProcess?.let { proc ->
                try {
                    android.system.Os.kill(proc.pid, android.system.OsConstants.SIGTERM)
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {}
        sessionJob?.cancel()
        ptyProcess = null
        _refreshTrigger.value = System.currentTimeMillis()
    }

    fun updateTerminalSize(cols: Int, rows: Int) {
        if (cols > 0 && rows > 0 && (emulator.cols != cols || emulator.rows != rows)) {
            emulator.resize(cols, rows)
            ptyProcess?.updateWindowSize(cols, rows)
            _refreshTrigger.value = System.currentTimeMillis()
        }
    }

    fun sendInput(input: String) {
        emulator.scrollToBottom()
        sessionScope.launch(writeDispatcher) {
            try {
                val proc = ptyProcess
                if (proc != null && _isRunning.value) {
                    val bytes = input.toByteArray(Charsets.UTF_8)
                    Os.write(proc.parcelFd.fileDescriptor, bytes, 0, bytes.size)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error writing to PTY stream for session $id", e)
            }
        }
    }

    fun scrollUp(lines: Int = 3) {
        emulator.scrollUp(lines)
        _refreshTrigger.value = System.currentTimeMillis()
    }

    fun scrollDown(lines: Int = 3) {
        emulator.scrollDown(lines)
        _refreshTrigger.value = System.currentTimeMillis()
    }

    fun scrollToBottom() {
        emulator.scrollToBottom()
        _refreshTrigger.value = System.currentTimeMillis()
    }

    fun pasteText(text: String) {
        if (text.isNotEmpty()) {
            val formatted = text.replace("\r\n", "\r").replace("\n", "\r")
            sendInput(formatted)
        }
    }

    fun sendKeyShortcut(key: String) {
        val trimmed = key.trim()
        if (trimmed.isEmpty()) return

        when (trimmed) {
            "Paste" -> return
            "Ctrl+C" -> return sendInput("\u0003")
            "Ctrl+Z" -> return sendInput("\u001A")
            "Ctrl+D" -> return sendInput("\u0004")
            "Tab" -> return sendInput("\t")
            "Esc", "ESC" -> return sendInput("\u001B")
            "▲", "Up" -> return sendInput("\u001B[A")
            "▼", "Down" -> return sendInput("\u001B[B")
            "◄", "Left" -> return sendInput("\u001B[D")
            "►", "Right" -> return sendInput("\u001B[C")
            "Enter", "Return" -> return sendInput("\r")
            "Backspace" -> return sendInput("\u007F")
        }

        if (trimmed.startsWith("Ctrl+", ignoreCase = true) && trimmed.length == 6) {
            val char = trimmed[5]
            val upperChar = char.uppercaseChar()
            if (upperChar in 'A'..'Z') {
                val ctrlByte = (upperChar.code - 'A'.code + 1).toChar().toString()
                sendInput(ctrlByte)
                return
            }
        }

        if (trimmed.startsWith("Alt+", ignoreCase = true) && trimmed.length == 5) {
            val char = trimmed[4]
            sendInput("\u001B" + char)
            return
        }

        if (trimmed.length <= 2 && !trimmed.contains(" ")) {
            sendInput(trimmed)
            return
        }

        sendInput(trimmed + "\n")
    }

    fun getScreenText(): String = emulator.getVisibleText()
    fun getAllTerminalText(): String = emulator.getAllText()
}
