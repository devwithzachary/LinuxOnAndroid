package com.devwithzachary.completelinuxinstaller.ui.util

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Modifier that handles both secondary clicks (mouse right-click) and touch long-presses,
 * invoking [onContextMenu] with the local offset of the click/press.
 */
fun Modifier.onContextMenu(
    enabled: Boolean = true,
    onContextMenu: (Offset) -> Unit
): Modifier = if (!enabled) this else this.pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Main)
            if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                val position = event.changes.firstOrNull()?.position ?: Offset.Zero
                event.changes.forEach { it.consume() }
                onContextMenu(position)
            }
        }
    }
}.pointerInput(Unit) {
    detectTapGestures(
        onLongPress = { offset ->
            onContextMenu(offset)
        }
    )
}

/**
 * Convenience modifier to set the cursor to a pointing hand when hovering.
 */
fun Modifier.handHover(): Modifier = this.pointerHoverIcon(PointerIcon.Hand)
