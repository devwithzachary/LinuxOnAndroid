package com.devwithzachary.completelinuxinstaller.ui.screens.desktop

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.devwithzachary.completelinuxinstaller.engine.desktop.DesktopTouchMode
import com.devwithzachary.completelinuxinstaller.engine.desktop.VncClient
import kotlin.math.min

@Composable
fun DesktopCanvasView(
    vncClient: VncClient,
    touchMode: DesktopTouchMode,
    isFitToScreen: Boolean,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val viewConfig = LocalViewConfiguration.current
    val refreshTrigger by vncClient.refreshTrigger.collectAsState()

    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // Trackpad virtual cursor state
    var cursorPosition by remember { mutableStateOf(Offset(640f, 360f)) }

    // Double-tap drag state for trackpad
    var lastTapUpTime by remember { mutableLongStateOf(0L) }
    var lastTapUpPos by remember { mutableStateOf(Offset.Zero) }

    // Reset zoom when resolution or fit mode toggles
    LaunchedEffect(isFitToScreen, vncClient.desktopWidth, vncClient.desktopHeight) {
        scale = 1f
        offset = Offset.Zero
        cursorPosition = Offset(vncClient.desktopWidth / 2f, vncClient.desktopHeight / 2f)
    }

    val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 4f)
        if (scale > 1f) {
            offset += offsetChange
        } else {
            offset = Offset.Zero
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .transformable(
                state = transformState,
                enabled = (touchMode == DesktopTouchMode.DIRECT_TOUCH)
            )
    ) {
        val containerWidth = constraints.maxWidth.toFloat()
        val containerHeight = constraints.maxHeight.toFloat()

        val bmpWidth = vncClient.desktopWidth.toFloat().coerceAtLeast(1f)
        val bmpHeight = vncClient.desktopHeight.toFloat().coerceAtLeast(1f)

        // Calculate aspect ratio fit dimensions
        val scaleFactor = min(containerWidth / bmpWidth, containerHeight / bmpHeight)
        val fittedWidth = bmpWidth * scaleFactor
        val fittedHeight = bmpHeight * scaleFactor

        val baseWidth = if (isFitToScreen) fittedWidth else bmpWidth
        val baseHeight = if (isFitToScreen) fittedHeight else bmpHeight

        val drawWidth = baseWidth * scale
        val drawHeight = baseHeight * scale

        // Center on screen + user panning offset
        val drawLeft = ((containerWidth - drawWidth) / 2f) + offset.x
        val drawTop = ((containerHeight - drawHeight) / 2f) + offset.y

        fun screenToDesktop(screenX: Float, screenY: Float): Pair<Int, Int>? {
            if (screenX < drawLeft || screenX > drawLeft + drawWidth ||
                screenY < drawTop || screenY > drawTop + drawHeight
            ) {
                return null
            }
            val normX = (screenX - drawLeft) / drawWidth
            val normY = (screenY - drawTop) / drawHeight
            val dtX = (normX * bmpWidth).toInt().coerceIn(0, (bmpWidth - 1).toInt())
            val dtY = (normY * bmpHeight).toInt().coerceIn(0, (bmpHeight - 1).toInt())
            return Pair(dtX, dtY)
        }

        val canvasModifier = if (touchMode == DesktopTouchMode.DIRECT_TOUCH) {
            Modifier
                .fillMaxSize()
                .pointerInput(drawLeft, drawTop, drawWidth, drawHeight, scale) {
                    detectTapGestures(
                        onTap = { tapOffset ->
                            val coords = screenToDesktop(tapOffset.x, tapOffset.y) ?: return@detectTapGestures
                            // Direct left click: button 1 down then up
                            vncClient.sendPointerEvent(coords.first, coords.second, 1)
                            vncClient.sendPointerEvent(coords.first, coords.second, 0)
                        },
                        onLongPress = { longPressOffset ->
                            val coords = screenToDesktop(longPressOffset.x, longPressOffset.y) ?: return@detectTapGestures
                            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                            // Direct right click: button 4 down then up
                            vncClient.sendPointerEvent(coords.first, coords.second, 4)
                            vncClient.sendPointerEvent(coords.first, coords.second, 0)
                        }
                    )
                }
                .pointerInput(drawLeft, drawTop, drawWidth, drawHeight, scale) {
                    detectDragGestures(
                        onDragStart = { startOffset ->
                            val coords = screenToDesktop(startOffset.x, startOffset.y) ?: return@detectDragGestures
                            vncClient.sendPointerEvent(coords.first, coords.second, 1)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val coords = screenToDesktop(change.position.x, change.position.y) ?: return@detectDragGestures
                            vncClient.sendPointerEvent(coords.first, coords.second, 1)
                        },
                        onDragEnd = {
                            // Release button 1
                            vncClient.sendPointerEvent(0, 0, 0)
                        },
                        onDragCancel = {
                            vncClient.sendPointerEvent(0, 0, 0)
                        }
                    )
                }
        } else {
            Modifier
                .fillMaxSize()
                .pointerInput(touchMode, bmpWidth, bmpHeight) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val downTime = System.currentTimeMillis()
                        var maxPointers = 1
                        var totalDragDist = 0f
                        var isDrag = false
                        var isTwoFingerDrag = false
                        var isHoldingLeftButton = false
                        var prevSinglePos = down.position
                        var prevTwoFingerMidY = 0f

                        val isDoubleTapCandidate = (downTime - lastTapUpTime < 280) &&
                                ((down.position - lastTapUpPos).getDistance() < viewConfig.touchSlop * 2)

                        while (true) {
                            val event = awaitPointerEvent()
                            val pressedChanges = event.changes.filter { it.pressed }
                            val count = pressedChanges.size
                            if (count > maxPointers) {
                                maxPointers = count
                            }

                            if (count == 0) {
                                // All pointers lifted
                                val duration = System.currentTimeMillis() - downTime
                                if (isHoldingLeftButton) {
                                    val cx = cursorPosition.x.toInt()
                                    val cy = cursorPosition.y.toInt()
                                    vncClient.sendPointerEvent(cx, cy, 0)
                                } else if (!isDrag && !isTwoFingerDrag && duration < 350) {
                                    if (maxPointers == 1) {
                                        // Single finger click -> Left click
                                        val cx = cursorPosition.x.toInt()
                                        val cy = cursorPosition.y.toInt()
                                        vncClient.sendPointerEvent(cx, cy, 1)
                                        vncClient.sendPointerEvent(cx, cy, 0)
                                        lastTapUpTime = System.currentTimeMillis()
                                        lastTapUpPos = down.position
                                    } else if (maxPointers >= 2) {
                                        // Two finger click -> Right click!
                                        val cx = cursorPosition.x.toInt()
                                        val cy = cursorPosition.y.toInt()
                                        if (!view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)) {
                                            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                        }
                                        vncClient.sendPointerEvent(cx, cy, 4)
                                        vncClient.sendPointerEvent(cx, cy, 0)
                                        lastTapUpTime = 0L
                                    }
                                }
                                break
                            } else if (count == 1 && maxPointers == 1) {
                                val change = pressedChanges[0]
                                val dragDelta = change.position - prevSinglePos
                                prevSinglePos = change.position
                                totalDragDist += dragDelta.getDistance()

                                if (totalDragDist > viewConfig.touchSlop) {
                                    isDrag = true
                                    if (isDoubleTapCandidate && !isHoldingLeftButton) {
                                        isHoldingLeftButton = true
                                    }
                                }

                                if (isDrag) {
                                    change.consume()
                                    val sensitivity = 1.2f
                                    val nextX = (cursorPosition.x + dragDelta.x * sensitivity).coerceIn(0f, bmpWidth - 1f)
                                    val nextY = (cursorPosition.y + dragDelta.y * sensitivity).coerceIn(0f, bmpHeight - 1f)
                                    cursorPosition = Offset(nextX, nextY)
                                    val buttonMask = if (isHoldingLeftButton) 1 else 0
                                    vncClient.sendPointerEvent(nextX.toInt(), nextY.toInt(), buttonMask)
                                }
                            } else if (count >= 2) {
                                val p1 = pressedChanges[0]
                                val p2 = pressedChanges[1]
                                val midY = (p1.position.y + p2.position.y) / 2f

                                if (prevTwoFingerMidY == 0f) {
                                    prevTwoFingerMidY = midY
                                } else {
                                    val dy = midY - prevTwoFingerMidY
                                    if (kotlin.math.abs(dy) > viewConfig.touchSlop / 2) {
                                        isTwoFingerDrag = true
                                    }
                                    if (isTwoFingerDrag) {
                                        p1.consume()
                                        p2.consume()
                                        val cx = cursorPosition.x.toInt()
                                        val cy = cursorPosition.y.toInt()
                                        if (dy > 14f) {
                                            // Two-finger scroll down (wheel down = mask 16)
                                            vncClient.sendPointerEvent(cx, cy, 16)
                                            vncClient.sendPointerEvent(cx, cy, 0)
                                            prevTwoFingerMidY = midY
                                        } else if (dy < -14f) {
                                            // Two-finger scroll up (wheel up = mask 8)
                                            vncClient.sendPointerEvent(cx, cy, 8)
                                            vncClient.sendPointerEvent(cx, cy, 0)
                                            prevTwoFingerMidY = midY
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
        }

        Box(modifier = canvasModifier) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val trigger = refreshTrigger
                val currentBitmap = vncClient.bitmap

                if (currentBitmap != null && !currentBitmap.isRecycled) {
                    drawImage(
                        image = currentBitmap.asImageBitmap(),
                        srcOffset = IntOffset.Zero,
                        srcSize = IntSize(currentBitmap.width, currentBitmap.height),
                        dstOffset = IntOffset(drawLeft.toInt(), drawTop.toInt()),
                        dstSize = IntSize(drawWidth.toInt(), drawHeight.toInt()),
                        filterQuality = FilterQuality.Low
                    )
                }

                // Draw virtual cursor in Trackpad mode
                if (touchMode == DesktopTouchMode.TRACKPAD) {
                    val cursorScreenX = drawLeft + (cursorPosition.x / bmpWidth) * drawWidth
                    val cursorScreenY = drawTop + (cursorPosition.y / bmpHeight) * drawHeight

                    drawCircle(
                        color = Color.White,
                        radius = 8f,
                        center = Offset(cursorScreenX, cursorScreenY)
                    )
                    drawCircle(
                        color = Color.Black,
                        radius = 8f,
                        center = Offset(cursorScreenX, cursorScreenY),
                        style = Stroke(width = 2f)
                    )
                    drawCircle(
                        color = Color.Black,
                        radius = 2f,
                        center = Offset(cursorScreenX, cursorScreenY)
                    )
                }
            }
        }
    }
}
