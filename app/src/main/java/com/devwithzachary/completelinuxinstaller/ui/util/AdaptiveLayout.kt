package com.devwithzachary.completelinuxinstaller.ui.util

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration

enum class WindowSizeClassType {
    COMPACT,
    MEDIUM,
    EXPANDED;

    val isCompact: Boolean get() = this == COMPACT
    val isMedium: Boolean get() = this == MEDIUM
    val isExpanded: Boolean get() = this == EXPANDED
    val isMediumOrExpanded: Boolean get() = this != COMPACT
}

/**
 * Derives the WindowSizeClassType based on screen width dp in accordance with Material 3 guidelines:
 * - COMPACT: < 600dp (standard phone portrait, narrow split-screen)
 * - MEDIUM: 600dp .. 839dp (7" tablets, foldables, 10" portrait)
 * - EXPANDED: >= 840dp (10" tablets landscape, desktop mode, wide multi-window)
 */
@Composable
fun rememberWindowSizeClass(): WindowSizeClassType {
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    return remember(screenWidthDp) {
        when {
            screenWidthDp < 600 -> WindowSizeClassType.COMPACT
            screenWidthDp < 840 -> WindowSizeClassType.MEDIUM
            else -> WindowSizeClassType.EXPANDED
        }
    }
}

/**
 * Returns true if a physical hardware keyboard is attached (e.g. QWERTY keyboard, tablet keyboard cover, or desktop mode).
 */
@Composable
fun isHardwareKeyboardConnected(): Boolean {
    val configuration = LocalConfiguration.current
    return configuration.keyboard == Configuration.KEYBOARD_QWERTY ||
            configuration.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO
}
