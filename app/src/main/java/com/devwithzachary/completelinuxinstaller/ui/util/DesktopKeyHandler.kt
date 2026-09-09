package com.devwithzachary.completelinuxinstaller.ui.util

import androidx.compose.ui.input.key.*
import com.devwithzachary.completelinuxinstaller.ui.AppScreen

object DesktopKeyHandler {

    /**
     * Checks if a KeyEvent matches a global desktop navigation shortcut.
     * Returns the target AppScreen if matched, or null otherwise.
     */
    fun handleGlobalShortcut(
        event: KeyEvent,
        onNavigate: (AppScreen) -> Unit,
        onNewTerminalTab: () -> Unit = {},
        onCloseTerminalTab: () -> Unit = {},
        onNextTerminalTab: () -> Unit = {},
        onPrevTerminalTab: () -> Unit = {},
        currentScreen: AppScreen
    ): Boolean {
        if (event.type != KeyEventType.KeyDown) return false

        val isCtrl = event.isCtrlPressed || event.isMetaPressed
        val isAlt = event.isAltPressed
        val isShift = event.isShiftPressed

        // Universal Linux shortcut: Ctrl+Alt+T -> Open Terminal from any screen
        if (isCtrl && isAlt && event.key == Key.T) {
            onNavigate(AppScreen.TERMINAL)
            return true
        }

        // Screen shortcuts: Alt+1..4 (or Ctrl+1..4 when not in terminal to avoid clash)
        if (isAlt || (!isCtrl && isAlt)) {
            when (event.key) {
                Key.One, Key.NumPad1 -> {
                    onNavigate(AppScreen.DASHBOARD)
                    return true
                }
                Key.Two, Key.NumPad2 -> {
                    onNavigate(AppScreen.TERMINAL)
                    return true
                }
                Key.Three, Key.NumPad3 -> {
                    onNavigate(AppScreen.SETTINGS)
                    return true
                }
                Key.Four, Key.NumPad4 -> {
                    onNavigate(AppScreen.ABOUT)
                    return true
                }
            }
        }

        // Ctrl + Comma -> Settings
        if (isCtrl && event.key == Key.Comma) {
            onNavigate(AppScreen.SETTINGS)
            return true
        }

        // Terminal tab management shortcuts
        if (currentScreen == AppScreen.TERMINAL) {
            // Ctrl+Shift+T or Ctrl+T -> New Tab
            if ((isCtrl && isShift && event.key == Key.T) || (isCtrl && !isShift && event.key == Key.T && !isAlt)) {
                onNewTerminalTab()
                return true
            }
            // Ctrl+Shift+W -> Close Tab
            if (isCtrl && isShift && event.key == Key.W) {
                onCloseTerminalTab()
                return true
            }
            // Ctrl+Tab -> Next Tab; Ctrl+Shift+Tab -> Prev Tab
            if (isCtrl && event.key == Key.Tab) {
                if (isShift) onPrevTerminalTab() else onNextTerminalTab()
                return true
            }
        }

        return false
    }
}
