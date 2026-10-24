package com.deckapp.feature.draw.components.playmat

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput

/** Keeps pointer recognition separate while leaving all interaction state in PlaymatCanvasView. */
internal fun Modifier.playmatCanvasGestures(
    isRulerActive: Boolean,
    surfaceId: String,
    cameraX: Float,
    cameraY: Float,
    cameraZoom: Float,
    rulerStartPos: Offset?,
    connectingSourceEndpointId: String?,
    onRulerPositionsChanged: (Offset?, Offset?) -> Unit,
    onTransform: (pan: Offset, zoom: Float) -> Unit,
    onTransformEnd: () -> Unit,
    onCancelConnection: () -> Unit,
    onCanvasTap: (Offset) -> Unit
): Modifier =
    pointerInput(isRulerActive, surfaceId) {
        if (isRulerActive) {
            detectDragGestures(
                onDragStart = { offset ->
                    val canvasX = (offset.x - cameraX) / cameraZoom
                    val canvasY = (offset.y - cameraY) / cameraZoom
                    val position = Offset(canvasX, canvasY)
                    onRulerPositionsChanged(position, position)
                },
                onDrag = { change, _ ->
                    change.consume()
                    val canvasX = (change.position.x - cameraX) / cameraZoom
                    val canvasY = (change.position.y - cameraY) / cameraZoom
                    onRulerPositionsChanged(rulerStartPos, Offset(canvasX, canvasY))
                },
                onDragEnd = { },
                onDragCancel = { onRulerPositionsChanged(null, null) }
            )
        }
    }
        .pointerInput(isRulerActive, surfaceId) {
            if (!isRulerActive) {
                detectTransformGestures { _, pan, zoom, _ -> onTransform(pan, zoom) }
            }
        }
        .pointerInput(surfaceId) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                do {
                    val event = awaitPointerEvent()
                } while (event.changes.any { it.pressed })
                onTransformEnd()
            }
        }
        .pointerInput(surfaceId) {
            detectTapGestures(
                onTap = { position ->
                    when {
                        rulerStartPos != null -> onRulerPositionsChanged(null, null)
                        connectingSourceEndpointId != null -> onCancelConnection()
                        else -> onCanvasTap(position)
                    }
                }
            )
        }
