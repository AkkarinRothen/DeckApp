package com.deckapp.feature.draw.components.playmat

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Watches a card before its clickable child: a short tap is left untouched, while moving past
 * touch slop or holding until the long-press timeout claims the gesture for dragging.
 */
internal fun Modifier.playmatHandCardDrag(
    cardId: Long,
    onStart: (Long, Offset, IntSize) -> Unit,
    onMove: (Long, Offset) -> Unit,
    onEnd: (Long, Offset) -> Unit,
    onCancel: (Long) -> Unit
): Modifier = pointerInput(cardId) {
    awaitEachGesture {
        val down = awaitFirstDown(
            requireUnconsumed = false,
            pass = PointerEventPass.Initial
        )
        val pointerId = down.id
        val downPosition = down.position
        var lastPosition = downPosition
        var accumulatedMovement = Offset.Zero
        var dragging = false
        var finished = false
        var completed = false

        try {
            while (!finished && !dragging) {
                val elapsed = (android.os.SystemClock.uptimeMillis() - down.uptimeMillis).coerceAtLeast(0L)
                val timeoutRemaining = (viewConfiguration.longPressTimeoutMillis - elapsed).coerceAtLeast(0L)
                val event = withTimeoutOrNull(timeoutRemaining) {
                    awaitPointerEvent(PointerEventPass.Initial)
                }

                if (event == null) {
                    dragging = true
                    onStart(cardId, downPosition, size)
                    break
                }

                val change = event.changes.firstOrNull { it.id == pointerId }
                if (change == null || !change.pressed) {
                    finished = true
                    break
                }

                accumulatedMovement += change.position - lastPosition
                lastPosition = change.position
                if (accumulatedMovement.getDistance() >= viewConfiguration.touchSlop) {
                    dragging = true
                    onStart(cardId, downPosition, size)
                    change.consume()
                    onMove(cardId, lastPosition)
                }
            }

            if (dragging) {
                var released = false
                while (!released) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val change = event.changes.firstOrNull { it.id == pointerId }
                    if (change == null) {
                        released = true
                    } else {
                        lastPosition = change.position
                        change.consume()
                        if (!change.pressed) {
                            released = true
                        } else {
                            onMove(cardId, lastPosition)
                        }
                    }
                }
                onEnd(cardId, lastPosition)
                completed = true
            }
        } finally {
            if (dragging && !completed) onCancel(cardId)
        }
    }
}
