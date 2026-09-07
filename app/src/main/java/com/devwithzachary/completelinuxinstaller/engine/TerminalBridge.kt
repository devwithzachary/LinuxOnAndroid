package com.devwithzachary.completelinuxinstaller.engine

import android.util.Log
import java.io.File
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TerminalBridge(private val pRootEngine: PRootEngine? = null) {

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

    private val scope = CoroutineScope(Dispatchers.Default + kotlinx.coroutines.SupervisorJob())

    private val _sessions = MutableStateFlow<List<TerminalSession>>(emptyList())
    val sessions: StateFlow<List<TerminalSession>> = _sessions.asStateFlow()

    private val _activeSessionId = MutableStateFlow<String?>(null)
    val activeSessionId: StateFlow<String?> = _activeSessionId.asStateFlow()

    val fallbackEmulator = TerminalEmulator(cols = 80, rows = 24)

    val emulator: TerminalEmulator
        get() = getActiveSession()?.emulator ?: fallbackEmulator

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val isAnySessionRunning: StateFlow<Boolean> = _sessions.flatMapLatest { list ->
        if (list.isEmpty()) flowOf(false)
        else combine(list.map { it.isRunning }) { array -> array.any { it } }
    }.stateIn(scope, SharingStarted.Eagerly, false)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val isRunning: StateFlow<Boolean> = _activeSessionId.flatMapLatest { id ->
        val session = _sessions.value.find { it.id == id } ?: _sessions.value.firstOrNull()
        session?.isRunning ?: flowOf(false)
    }.stateIn(scope, SharingStarted.Eagerly, false)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val refreshTrigger: StateFlow<Long> = _activeSessionId.flatMapLatest { id ->
        val session = _sessions.value.find { it.id == id } ?: _sessions.value.firstOrNull()
        session?.refreshTrigger ?: flowOf(0L)
    }.stateIn(scope, SharingStarted.Eagerly, 0L)

    fun getActiveSession(): TerminalSession? {
        val id = _activeSessionId.value
        val list = _sessions.value
        return if (id != null) list.find { it.id == id } else list.firstOrNull()
    }

    fun createSession(
        containerId: String = "ubuntu_default",
        containerName: String = "Ubuntu",
        loginUser: String = "root",
        title: String? = null,
        rootfsDir: File? = pRootEngine?.rootfsDir,
        defaultShell: String? = null,
        autoStart: Boolean = true,
        candidateShells: List<String>? = null
    ): TerminalSession {
        val tabNum = _sessions.value.size + 1
        val sessionTitle = title ?: "Tab $tabNum: ${containerName.take(10)}"
        val sessionId = UUID.randomUUID().toString()

        val session = TerminalSession(
            id = sessionId,
            initialTitle = sessionTitle,
            containerId = containerId,
            containerName = containerName,
            loginUser = loginUser
        )

        val updated = _sessions.value + session
        _sessions.value = updated
        _activeSessionId.value = sessionId

        if (autoStart && pRootEngine != null && rootfsDir != null) {
            session.startSession(pRootEngine, rootfsDir, defaultShell, candidateShells)
        }

        return session
    }

    fun switchActiveSession(sessionId: String) {
        val session = _sessions.value.find { it.id == sessionId }
        if (session != null) {
            _activeSessionId.value = sessionId
        }
    }

    fun closeSession(sessionId: String) {
        val list = _sessions.value
        val sessionToClose = list.find { it.id == sessionId } ?: return
        sessionToClose.stopSession()

        val remaining = list.filter { it.id != sessionId }
        _sessions.value = remaining

        if (_activeSessionId.value == sessionId) {
            _activeSessionId.value = remaining.lastOrNull()?.id
        }
    }

    fun closeAllSessions() {
        _sessions.value.forEach { it.stopSession() }
        _sessions.value = emptyList()
        _activeSessionId.value = null
    }

    fun renameSession(sessionId: String, newTitle: String) {
        _sessions.value.find { it.id == sessionId }?.setTitle(newTitle)
    }

    fun startSession(
        loginUser: String? = null,
        containerId: String = "ubuntu_default",
        containerName: String = "Ubuntu",
        rootfsDir: File? = pRootEngine?.rootfsDir,
        defaultShell: String? = null,
        candidateShells: List<String>? = null
    ) {
        val engine = pRootEngine ?: return
        val dir = rootfsDir ?: engine.rootfsDir
        val active = getActiveSession()
        if (active != null) {
            if (!active.isRunning.value) {
                active.startSession(engine, dir, defaultShell, candidateShells)
            }
        } else {
            createSession(
                containerId = containerId,
                containerName = containerName,
                loginUser = loginUser ?: "root",
                rootfsDir = dir,
                defaultShell = defaultShell,
                autoStart = true,
                candidateShells = candidateShells
            )
        }
    }

    fun stopSession() {
        getActiveSession()?.stopSession()
    }

    fun updateTerminalSize(cols: Int, rows: Int) {
        getActiveSession()?.updateTerminalSize(cols, rows)
    }

    fun sendInput(input: String) {
        val active = getActiveSession()
        if (active != null) {
            active.sendInput(input)
        } else {
            val session = createSession(autoStart = true)
            session.sendInput(input)
        }
    }

    fun scrollUp(lines: Int = 3) {
        getActiveSession()?.scrollUp(lines)
    }

    fun scrollDown(lines: Int = 3) {
        getActiveSession()?.scrollDown(lines)
    }

    fun scrollToBottom() {
        getActiveSession()?.scrollToBottom()
    }

    fun pasteText(text: String) {
        getActiveSession()?.pasteText(text)
    }

    fun sendModifiedChar(char: Char, isCtrl: Boolean, isAlt: Boolean) {
        val seq = getModifiedSequence(char, isCtrl, isAlt)
        sendInput(seq)
    }

    fun sendKeyShortcut(key: String) {
        getActiveSession()?.sendKeyShortcut(key)
    }

    fun queueCommand(command: String, sessionId: String? = null) {
        val session = if (sessionId != null) {
            _sessions.value.find { it.id == sessionId }
        } else {
            getActiveSession()
        }
        session?.queueCommand(command)
    }

    fun sendCommand(command: String) {
        val active = getActiveSession()
        if (active != null) {
            active.queueCommand(command)
        } else {
            val session = createSession(autoStart = true)
            session.queueCommand(command)
        }
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
        sendInput("\u001B[A")
    }

    fun sendArrowDown() {
        sendInput("\u001B[B")
    }

    fun sendArrowRight() {
        sendInput("\u001B[C")
    }

    fun sendArrowLeft() {
        sendInput("\u001B[D")
    }

    fun getScreenText(): String = getActiveSession()?.getScreenText() ?: ""

    fun getAllTerminalText(): String = getActiveSession()?.getAllTerminalText() ?: ""

    fun getSelectedText(startRow: Int, startCol: Int, endRow: Int, endCol: Int): String =
        emulator.getSelectedText(startRow, startCol, endRow, endCol)

    fun getWordAt(row: Int, col: Int): Pair<Int, Int> =
        emulator.getWordAt(row, col)
}
