package com.deckapp.feature.draw.components.playmat

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.deckapp.core.model.PlaymatSurface
import kotlin.math.max

@Composable
internal fun PlaymatMinimap(
    surface: PlaymatSurface,
    cameraX: Float,
    cameraY: Float,
    cameraZoom: Float,
    viewportWidth: Float,
    viewportHeight: Float,
    onCenterAt: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val worldBounds = remember(surface) { playmatContentBounds(surface) }
    Surface(modifier, shape = RoundedCornerShape(14.dp), tonalElevation = 6.dp, shadowElevation = 8.dp) {
        Column(Modifier.padding(8.dp)) {
            Text("Vista general", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(4.dp))
            Canvas(
                Modifier.fillMaxWidth().height(110.dp).pointerInput(worldBounds, surface.id) {
                    detectTapGestures { point ->
                        val scale = minOf(size.width / worldBounds.width, size.height / worldBounds.height)
                        val offsetX = (size.width - worldBounds.width * scale) / 2f
                        val offsetY = (size.height - worldBounds.height * scale) / 2f
                        val worldX = worldBounds.left + (point.x - offsetX) / scale
                        val worldY = worldBounds.top + (point.y - offsetY) / scale
                        onCenterAt(worldX, worldY)
                    }
                }
            ) {
                val scale = minOf(size.width / worldBounds.width, size.height / worldBounds.height)
                val offsetX = (size.width - worldBounds.width * scale) / 2f
                val offsetY = (size.height - worldBounds.height * scale) / 2f
                fun screenX(x: Float) = offsetX + (x - worldBounds.left) * scale
                fun screenY(y: Float) = offsetY + (y - worldBounds.top) * scale
                surface.cards.filterNot { card -> surface.groups.any { it.id == card.clusterId && it.isHidden } }
                    .forEach { drawRect(Color(0xFF80DEEA), Offset(screenX(it.x), screenY(it.y)), androidx.compose.ui.geometry.Size(5f, 8f)) }
                surface.decks.filterNot { deck -> surface.groups.any { it.isHidden && (it.id == deck.groupId || it.id == deck.discardGroupId) } }
                    .forEach { drawRect(Color(0xFFFFCC80), Offset(screenX(it.x), screenY(it.y)), androidx.compose.ui.geometry.Size(6f, 9f)) }
                surface.tokens.filterNot { token -> surface.groups.any { it.id == token.groupId && it.isHidden } }
                    .forEach { drawCircle(Color(0xFFA5D6A7), 3f, Offset(screenX(it.x), screenY(it.y))) }
                surface.clocks.filterNot { clock -> surface.groups.any { it.id == clock.groupId && it.isHidden } }
                    .forEach { drawCircle(Color(0xFFEF9A9A), 3f, Offset(screenX(it.x), screenY(it.y))) }
                surface.stickyNotes.filterNot { note -> surface.groups.any { it.id == note.groupId && it.isHidden } }
                    .forEach { drawRect(Color(0xFFFFF59D), Offset(screenX(it.x), screenY(it.y)), androidx.compose.ui.geometry.Size(5f, 5f)) }
                surface.tableChips.filterNot { chip -> surface.groups.any { it.id == chip.groupId && it.isHidden } }
                    .forEach { drawCircle(Color(0xFFCE93D8), 2f, Offset(screenX(it.x), screenY(it.y))) }
                val visible = Rect(
                    left = -cameraX / cameraZoom,
                    top = -cameraY / cameraZoom,
                    right = (-cameraX + viewportWidth) / cameraZoom,
                    bottom = (-cameraY + viewportHeight) / cameraZoom
                )
                drawRect(
                    Color(0xFF00E5FF),
                    Offset(screenX(visible.left), screenY(visible.top)),
                    androidx.compose.ui.geometry.Size(visible.width * scale, visible.height * scale),
                    style = Stroke(width = 2f)
                )
            }
        }
    }
}

private fun playmatContentBounds(surface: PlaymatSurface): Rect {
    val hiddenGroups = surface.groups.filter { it.isHidden }.map { it.id }.toSet()
    val points = buildList {
        surface.cards.filterNot { it.clusterId in hiddenGroups }.forEach { add(Offset(it.x, it.y)) }
        surface.tokens.filterNot { it.groupId in hiddenGroups }.forEach { add(Offset(it.x, it.y)) }
        surface.clocks.filterNot { it.groupId in hiddenGroups }.forEach { add(Offset(it.x, it.y)) }
        surface.stickyNotes.filterNot { it.groupId in hiddenGroups }.forEach { add(Offset(it.x, it.y)) }
        surface.tableChips.filterNot { it.groupId in hiddenGroups }.forEach { add(Offset(it.x, it.y)) }
        surface.decks.forEach {
            if (it.groupId !in hiddenGroups && it.discardGroupId !in hiddenGroups) add(Offset(it.x, it.y))
            if ((it.discardX != null || it.discardY != null) && it.groupId !in hiddenGroups && it.discardGroupId !in hiddenGroups) {
                add(Offset(it.discardX ?: it.x + 150f, it.discardY ?: it.y))
            }
        }
    }
    if (points.isEmpty()) return Rect(0f, 0f, 600f, 400f)
    val padding = 100f
    val left = points.minOf { it.x } - padding
    val top = points.minOf { it.y } - padding
    val right = points.maxOf { it.x } + padding
    val bottom = points.maxOf { it.y } + padding
    return Rect(left, top, left + max(200f, right - left), top + max(160f, bottom - top))
}
