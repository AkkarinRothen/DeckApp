package com.deckapp.feature.draw.components.playmat

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.deckapp.core.model.PlaymatSurface

@Composable
internal fun PlaymatSelectionLayer(
    surface: PlaymatSurface,
    cameraX: Float,
    cameraY: Float,
    cameraZoom: Float,
    enabled: Boolean,
    selectedIds: Set<String>,
    onSelectionChanged: (Set<String>) -> Unit
) {
    if (!enabled) return
    val spatialIndex = androidx.compose.runtime.remember(surface) { PlaymatSpatialIndex(surface) }
    Canvas(
        Modifier.fillMaxSize().pointerInput(spatialIndex, cameraX, cameraY, cameraZoom, selectedIds) {
            detectTapGestures { point ->
                val x = (point.x - cameraX) / cameraZoom
                val y = (point.y - cameraY) / cameraZoom
                val hit = spatialIndex.hitTest(x, y)?.id
                if (hit != null) onSelectionChanged(if (hit in selectedIds) selectedIds - hit else selectedIds + hit)
            }
        }
    ) {
        val selectedBounds = buildList {
            surface.cards.filter { "card:${it.cardId}" in selectedIds }.forEach { add(it.x to (it.y to (120f to 180f))) }
            surface.tokens.filter { "token:${it.id}" in selectedIds }.forEach { add(it.x to (it.y to (64f to 82f))) }
            surface.clocks.filter { "clock:${it.id}" in selectedIds }.forEach { add(it.x to (it.y to (76f to 100f))) }
            surface.stickyNotes.filter { "note:${it.id}" in selectedIds }.forEach { add(it.x to (it.y to (100f to 100f))) }
            surface.tableChips.filter { "chip:${it.id}" in selectedIds }.forEach { add(it.x to (it.y to (180f to 48f))) }
            surface.decks.forEach { deck ->
                if ("deck:${deck.stackId}" in selectedIds) add(deck.x to (deck.y to (120f to 180f)))
                if ("discard:${deck.stackId}" in selectedIds) add((deck.discardX ?: deck.x + 150f) to ((deck.discardY ?: deck.y) to (120f to 180f)))
            }
        }
        selectedBounds.forEach { (x, rest) ->
            val (y, dimensions) = rest
            val (width, height) = dimensions
            drawRoundRect(
                color = Color(0x4400E5FF),
                topLeft = Offset(cameraX + x * cameraZoom, cameraY + y * cameraZoom),
                size = Size(width * cameraZoom, height * cameraZoom),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f)
            )
            drawRoundRect(
                color = Color(0xFF00E5FF),
                topLeft = Offset(cameraX + x * cameraZoom, cameraY + y * cameraZoom),
                size = Size(width * cameraZoom, height * cameraZoom),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f),
                style = Stroke(width = 3.dp.toPx())
            )
        }
    }
}
