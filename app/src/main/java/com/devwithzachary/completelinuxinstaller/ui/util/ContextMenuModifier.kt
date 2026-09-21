package com.devwithzachary.completelinuxinstaller.ui.util

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

/**
 * Modifier that handles both secondary clicks (mouse right-click) and touch long-presses,
 * invoking [onContextMenu] with the local offset of the click/press.
 *
 * Does NOT consume the initial pointer down event upfront, ensuring that regular taps
 * and clicks continue to be dispatched cleanly to any [clickable] or [combinedClickable]
 * handlers attached to the same composable.
 */
fun Modifier.onContextMenu(
    enabled: Boolean = true,
    onContextMenu: (Offset) -> Unit
): Modifier = if (!enabled) this else this.pointerInput(enabled) {
    awaitPointerEventScope {
        while (true) {
            val downEvent = awaitPointerEvent(PointerEventPass.Main)
            val down = downEvent.changes.firstOrNull { it.pressed } ?: continue

            // 1. Mouse right-click (secondary click)
            if (downEvent.buttons.isSecondaryPressed) {
                down.consume()
                onContextMenu(down.position)
                continue
            }

            // 2. Touch or primary click long-press detection without consuming down upfront
            var isLongPress = false
            try {
                withTimeout(viewConfiguration.longPressTimeoutMillis) {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        val change = event.changes.firstOrNull { it.id == down.id }
                        if (change == null || !change.pressed) {
                            // Pointer released before long-press timeout -> tap
                            break
                        }
                        val distance = (change.position - down.position).getDistance()
                        if (distance > viewConfiguration.touchSlop) {
                            // Pointer moved beyond touch slop -> drag / scroll
                            break
                        }
                    }
                }
            } catch (_: TimeoutCancellationException) {
                // Held for longPressTimeoutMillis without moving -> long press
                isLongPress = true
            }

            if (isLongPress) {
                currentEvent.changes.forEach { it.consume() }
                onContextMenu(down.position)
            }
        }
    }
}

/**
 * Modifier that handles mouse right-click (secondary click) without intercepting touch gestures.
 */
fun Modifier.onSecondaryClick(
    enabled: Boolean = true,
    onSecondaryClick: (Offset) -> Unit
): Modifier = if (!enabled) this else this.pointerInput(enabled) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Main)
            if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                val position = event.changes.firstOrNull()?.position ?: Offset.Zero
                event.changes.forEach { it.consume() }
                onSecondaryClick(position)
            }
        }
    }
}

/**
 * Convenience modifier to set the cursor to a pointing hand when hovering.
 */
fun Modifier.handHover(): Modifier = this.pointerHoverIcon(PointerIcon.Hand)

