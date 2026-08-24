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

        fun getModifiedSequence(char: Char, isCtrl: Boolean, isAlt: Boolean): String {
            if (!isCtrl && !isAlt) return char.toString()

            var sequence = if (isCtrl) {
                val upper = char.uppercaseChar()
                when {
                    upper in 'A'..'Z' -> (upper.code - 'A'.code + 1).toChar().toString()
                    char == '@' || char == ' ' || char == '`' -> "\u0000"
                    char == '[' || char == '{' -> "\u001B"
                    char == '\\' || char == '|' -> "\u001C"
                    char == ']' || char == '}' -> "\u001D"
                    char == '^' || char == '~' -> "\u001E"
                    char == '_' || char == '?' -> "\u001F"
                    else -> char.toString()
                }
            } else {
                char.toString()
            }

            if (isAlt) {
                sequence = "\u001B$sequence"
            }
            return sequence
        }
    }

    private val scope = CoroutineScope(Dispatchers.IO + Job())

    val emulator = TerminalEmulator(cols = 80, rows = 24)

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _refreshTrigger = MutableStateFlow(0L)
    val refreshTrigger: StateFlow<Long> = _refreshTrigger.asStateFlow()

    private var ptyProcess: PtyProcess? = null

    fun startSession(loginUser: String? = null) {
        if (_isRunning.value) return

        scope.launch {
            try {
                _isRunning.value = true
                val cmdList = pRootEngine.buildPRootCommand(loginUser = loginUser)
                val cmdPath = cmdList[0]
                val args = cmdList.toTypedArray()

                val envMap = pRootEngine.getEnvironmentVariables(loginUser = loginUser).toMutableMap()
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

    private val writeDispatcher = Dispatchers.IO.limitedParallelism(1)

    fun sendInput(input: String) {
        emulator.scrollToBottom()
        scope.launch(writeDispatcher) {
            try {
                var proc = ptyProcess
                if (proc == null || !_isRunning.value) {
                    if (!_isRunning.value) {
                        startSession()
                    }
                    for (i in 0 until 50) {
                        proc = ptyProcess
                        if (proc != null && _isRunning.value) break
                        kotlinx.coroutines.delay(50)
                    }
                }
                if (proc != null && _isRunning.value) {
                    val bytes = input.toByteArray(Charsets.UTF_8)
                    Os.write(proc.parcelFd.fileDescriptor, bytes, 0, bytes.size)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error writing to PTY stream via POSIX write", e)
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

    fun getScreenText(): String = emulator.getVisibleText()

    fun getAllTerminalText(): String = emulator.getAllText()

    fun getSelectedText(startRow: Int, startCol: Int, endRow: Int, endCol: Int): String =
        emulator.getSelectedText(startRow, startCol, endRow, endCol)

    fun sendKeyShortcut(key: String) {
        val trimmed = key.trim()
        if (trimmed.isEmpty()) return

        // 1. Special Named Keys & Direct Control Mappings
        when (trimmed) {
            "Paste" -> return // Handled in UI layer via clipboard manager
            "Ctrl+C" -> return sendCtrlC()
            "Ctrl+Z" -> return sendCtrlZ()
            "Ctrl+D" -> return sendCtrlD()
            "Tab" -> return sendTab()
            "Esc", "ESC" -> return sendEsc()
            "▲", "Up" -> return sendArrowUp()
            "▼", "Down" -> return sendArrowDown()
            "◄", "Left" -> return sendArrowLeft()
            "►", "Right" -> return sendArrowRight()
            "Enter", "Return" -> return sendInput("\r")
            "Backspace" -> return sendInput("\u007F")
        }

        // 2. Generic Ctrl+<Letter> (e.g., Ctrl+X -> ASCII 24 \u0018 for nano exit)
        if (trimmed.startsWith("Ctrl+", ignoreCase = true) && trimmed.length == 6) {
            val char = trimmed[5]
            val upperChar = char.uppercaseChar()
            if (upperChar in 'A'..'Z') {
                val ctrlByte = (upperChar.code - 'A'.code + 1).toChar().toString()
                sendInput(ctrlByte)
                return
            }
        }

        // 3. Generic Alt+<Char> (e.g., Alt+X -> Escape sequence \u001Bx)
        if (trimmed.startsWith("Alt+", ignoreCase = true) && trimmed.length == 5) {
            val char = trimmed[4]
            sendInput("\u001B" + char)
            return
        }

        // 4. Single Symbols, Escaped Keys, or Short Typed Input without Spaces
        if (trimmed.length <= 2 && !trimmed.contains(" ")) {
            sendInput(trimmed)
            return
        }

        // 5. Multi-character shell commands
        sendCommand(trimmed)
    }

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

    fun sendModifiedChar(char: Char, isCtrl: Boolean, isAlt: Boolean) {
        val seq = getModifiedSequence(char, isCtrl, isAlt)
        sendInput(seq)
    }

    fun stopSession() {
        _isRunning.value = false
        ptyProcess?.destroy()
        ptyProcess = null
        _refreshTrigger.value = System.currentTimeMillis()
    }
}
