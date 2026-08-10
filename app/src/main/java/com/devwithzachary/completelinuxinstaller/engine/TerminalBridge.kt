package com.devwithzachary.completelinuxinstaller.engine

import android.system.Os
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TerminalBridge(private val pRootEngine: PRootEngine) {

    companion object {
        private const val TAG = "TerminalBridge"
    }

    private val scope = CoroutineScope(Dispatchers.IO + Job())

    val emulator = TerminalEmulator(cols = 80, rows = 24)

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _refreshTrigger = MutableStateFlow(0L)
    val refreshTrigger: StateFlow<Long> = _refreshTrigger.asStateFlow()

    private var ptyProcess: PtyProcess? = null

    fun startSession() {
        if (_isRunning.value) return

        scope.launch {
            try {
                _isRunning.value = true
                val cmdList = pRootEngine.buildPRootCommand()
                val cmdPath = cmdList[0]
                val args = cmdList.toTypedArray()

                val envMap = pRootEngine.getEnvironmentVariables().toMutableMap()
                envMap["TERM"] = "xterm-256color"
                envMap["COLORTERM"] = "truecolor"
                envMap["PROOT_NO_SECCOMP"] = "1"
                envMap["PROOT_FORCE_SETID"] = "1"
                envMap["PATH"] = "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"

                val envArray = envMap.map { "${it.key}=${it.value}" }.toTypedArray()

                val outPid = IntArray(1)
                val masterFd = PtyNative.createSubprocess(
                    cmdPath = cmdPath,
                    args = args,
                    env = envArray,
                    cwdPath = pRootEngine.rootfsDir.absolutePath,
                    cols = emulator.cols,
                    rows = emulator.rows,
                    outPid = outPid
                )

                if (masterFd < 0) {
                    Log.e(TAG, "Failed to create PTY subprocess")
                    _isRunning.value = false
                    return@launch
                }

                val proc = PtyProcess(masterFdInt = masterFd, pid = outPid[0])
                ptyProcess = proc

                Log.d(TAG, "PTY Subprocess launched successfully with PID ${outPid[0]}")

                val buffer = ByteArray(4096)
                while (_isRunning.value) {
                    val bytesRead = try {
                        proc.inputStream.read(buffer)
                    } catch (e: Exception) {
                        -1
                    }

                    if (bytesRead <= 0) break

                    emulator.appendBytes(buffer, bytesRead)
                    _refreshTrigger.value = System.currentTimeMillis()
                }

                PtyNative.waitForProcess(outPid[0])
            } catch (e: Exception) {
                Log.e(TAG, "Error in PTY session", e)
            } finally {
                _isRunning.value = false
                ptyProcess = null
                _refreshTrigger.value = System.currentTimeMillis()
            }
        }
    }

    fun updateTerminalSize(cols: Int, rows: Int) {
        if (cols > 0 && rows > 0 && (emulator.cols != cols || emulator.rows != rows)) {
            emulator.resize(cols, rows)
            ptyProcess?.updateWindowSize(cols, rows)
            _refreshTrigger.value = System.currentTimeMillis()
        }
    }

    fun sendInput(input: String) {
        scope.launch {
            try {
                val proc = ptyProcess
                if (proc != null && _isRunning.value) {
                    val bytes = input.toByteArray(Charsets.UTF_8)
                    Os.write(proc.parcelFd.fileDescriptor, bytes, 0, bytes.size)
                } else if (!_isRunning.value) {
                    startSession()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error writing to PTY stream via POSIX write", e)
            }
        }
    }

    fun pasteText(text: String) {
        if (text.isNotEmpty()) {
            val formatted = text.replace("\r\n", "\r").replace("\n", "\r")
            sendInput(formatted)
        }
    }

    fun getScreenText(): String = emulator.getVisibleText()

    fun getAllTerminalText(): String = emulator.getAllText()

    fun getSelectedText(startRow: Int, startCol: Int, endRow: Int, endCol: Int): String =
        emulator.getSelectedText(startRow, startCol, endRow, endCol)

    fun sendCommand(command: String) {
        sendInput(command + "\n")
    }

    fun sendCtrlC() {
        sendInput("\u0003")
    }

    fun sendCtrlZ() {
        sendInput("\u001A")
    }

    fun sendCtrlD() {
        sendInput("\u0004")
    }

    fun sendTab() {
        sendInput("\t")
    }

    fun sendEsc() {
        sendInput("\u001B")
    }

    fun sendArrowUp() {
        if (emulator.appCursorKeys) sendInput("\u001BOA") else sendInput("\u001B[A")
    }

    fun sendArrowDown() {
        if (emulator.appCursorKeys) sendInput("\u001BOB") else sendInput("\u001B[B")
    }

    fun sendArrowRight() {
        if (emulator.appCursorKeys) sendInput("\u001BOC") else sendInput("\u001B[C")
    }

    fun sendArrowLeft() {
        if (emulator.appCursorKeys) sendInput("\u001BOD") else sendInput("\u001B[D")
    }

    fun stopSession() {
        _isRunning.value = false
        ptyProcess?.destroy()
        ptyProcess = null
        _refreshTrigger.value = System.currentTimeMillis()
    }
}
