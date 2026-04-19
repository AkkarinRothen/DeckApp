package com.deckapp.feature.hexploration.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import com.deckapp.core.model.HexPoi
import com.deckapp.core.model.HexTile
import com.deckapp.core.model.PoiType
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

enum class HexCanvasMode { DESIGN, SESSION }

private const val HEX_SIZE_DEFAULT = 80f   // radius in px at scale=1
private const val FOG_ALPHA = 0.75f

@Composable
fun HexGridCanvas(
    tiles: List<HexTile>,
    pois: List<HexPoi>,
    mode: HexCanvasMode,
    selectedTile: HexTile?,
    onTileClick: (HexTile) -> Unit,
    onTileLongPress: (HexTile) -> Unit,
    onEmptySpaceClick: ((Int, Int) -> Unit)? = null,
    partyLocation: Pair<Int, Int>? = null,
    onMoveParty: ((Int, Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    
    // Ruler state: temporary destination while dragging or long-pressing
    var rulerTarget by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(0.3f, 4f)
        offset += panChange
    }

    val tileMap = remember(tiles) { tiles.associateBy { it.q to it.r } }
    val poiMap = remember(pois) { pois.groupBy { it.tileQ to it.tileR } }

    Canvas(
        modifier = modifier
            .transformable(state = transformState)
            .pointerInput(tileMap, scale, offset) {
                detectTapGestures(
                    onTap = { tapOffset ->
                        val worldPos = screenToWorld(tapOffset, offset, scale)
                        val (q, r) = worldToAxial(worldPos, HEX_SIZE_DEFAULT)
                        val tile = tileMap[q to r]
                        if (tile != null) {
                            onTileClick(tile)
                        } else {
                            onEmptySpaceClick?.invoke(q, r)
                        }
                    },
                    onLongPress = { tapOffset ->
                        val worldPos = screenToWorld(tapOffset, offset, scale)
                        val (q, r) = worldToAxial(worldPos, HEX_SIZE_DEFAULT)
                        val tile = tileMap[q to r]
                        if (tile != null) {
                            if (mode == HexCanvasMode.SESSION && partyLocation != null) {
                                // If long press on a different tile in session mode, move party
                                onMoveParty?.invoke(q, r)
                            } else {
                                onTileLongPress(tile)
                            }
                        }
                    }
                )
            }
    ) {
        val hexSize = HEX_SIZE_DEFAULT * scale
        val viewportBounds = ViewportBounds(
            left = -offset.x / scale,
            top = -offset.y / scale,
            right = (-offset.x + size.width) / scale,
            bottom = (-offset.y + size.height) / scale
        )

        // Culling: only draw hexes visible in viewport (+1 hex margin)
        val margin = HEX_SIZE_DEFAULT * 2
        val visibleTiles = tiles.filter { tile ->
            val center = axialToPixel(tile.q, tile.r, HEX_SIZE_DEFAULT)
            center.x >= viewportBounds.left - margin &&
                center.x <= viewportBounds.right + margin &&
                center.y >= viewportBounds.top - margin &&
                center.y <= viewportBounds.bottom + margin
        }

        visibleTiles.forEach { tile ->
            val center = axialToPixel(tile.q, tile.r, HEX_SIZE_DEFAULT)
            val screenCenter = worldToScreen(center, offset, scale)
            val tilePois = poiMap[tile.q to tile.r] ?: emptyList()
            val isSelected = selectedTile?.q == tile.q && selectedTile.r == tile.r

            drawHexTile(
                tile = tile,
                center = screenCenter,
                hexSize = hexSize,
                mode = mode,
                isSelected = isSelected,
                pois = tilePois
            )
        }

        // Draw Party Token
        partyLocation?.let { (q, r) ->
            val center = axialToPixel(q, r, HEX_SIZE_DEFAULT)
            val screenCenter = worldToScreen(center, offset, scale)
            drawPartyToken(screenCenter, hexSize)
        }

        // Draw Ruler if a tile is selected (Measure from selected to... nowhere yet, 
        // let's simplify: if selectedTile is active, we can draw distance to party or just highlight distance)
        // For now, let's draw a line between selectedTile and Party if both exist
        if (selectedTile != null && partyLocation != null && (selectedTile.q != partyLocation.first || selectedTile.r != partyLocation.second)) {
            val startCenter = axialToPixel(partyLocation.first, partyLocation.second, HEX_SIZE_DEFAULT)
            val endCenter = axialToPixel(selectedTile.q, selectedTile.r, HEX_SIZE_DEFAULT)
            
            val startScreen = worldToScreen(startCenter, offset, scale)
            val endScreen = worldToScreen(endCenter, offset, scale)
            
            val distance = hexDistance(partyLocation.first, partyLocation.second, selectedTile.q, selectedTile.r)
            
            drawLine(
                color = Color.White.copy(alpha = 0.6f),
                start = startScreen,
                end = endScreen,
                strokeWidth = 2f * scale,
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f * scale, 10f * scale))
            )
            
            // Note: Drawing text in Canvas requires native canvas or a specialized drawText call (available in latest Compose)
            // For simplicity in this environment, we will skip the floating text and let the UI show distance in the sheet
        }
    }
}

private fun DrawScope.drawPartyToken(center: Offset, hexSize: Float) {
    val tokenSize = hexSize * 0.4f
    // Draw a "Shield" shape for the party
    val path = Path().apply {
        moveTo(center.x, center.y - tokenSize)
        lineTo(center.x + tokenSize * 0.8f, center.y - tokenSize * 0.2f)
        lineTo(center.x + tokenSize * 0.6f, center.y + tokenSize * 0.8f)
        quadraticBezierTo(center.x, center.y + tokenSize * 1.1f, center.x - tokenSize * 0.6f, center.y + tokenSize * 0.8f)
        lineTo(center.x - tokenSize * 0.8f, center.y - tokenSize * 0.2f)
        close()
    }
    
    drawPath(path, Color(0xFFE91E63)) // Pink/Red for party
    drawPath(path, Color.White, style = Stroke(width = 2f))
    
    // Minimal "inner shield" decoration
    drawCircle(Color.White.copy(alpha = 0.3f), radius = tokenSize * 0.3f, center = center)
}

fun hexDistance(q1: Int, r1: Int, q2: Int, r2: Int): Int {
    return (kotlin.math.abs(q1 - q2) + 
            kotlin.math.abs(q1 + r1 - q2 - r2) + 
            kotlin.math.abs(r1 - r2)) / 2
}

// --- Geometry helpers ---

// Flat-top hex: vertex i is at angle = 60°*i
private fun hexCorner(center: Offset, size: Float, i: Int): Offset {
    val angleDeg = 60.0 * i
    val angleRad = PI / 180.0 * angleDeg
    return Offset(
        x = center.x + size * cos(angleRad).toFloat(),
        y = center.y + size * sin(angleRad).toFloat()
    )
}

private fun hexPath(center: Offset, size: Float): Path = Path().apply {
    val first = hexCorner(center, size, 0)
    moveTo(first.x, first.y)
    for (i in 1..5) {
        val corner = hexCorner(center, size, i)
        lineTo(corner.x, corner.y)
    }
    close()
}

// Flat-top axial → pixel
fun axialToPixel(q: Int, r: Int, size: Float): Offset {
    val x = size * (3f / 2f * q)
    val y = size * (sqrt(3f) / 2f * q + sqrt(3f) * r)
    return Offset(x, y)
}

// Pixel → axial (flat-top), returns nearest hex (q, r)
private fun worldToAxial(pos: Offset, size: Float): Pair<Int, Int> {
    val q = (2f / 3f * pos.x) / size
    val r = (-1f / 3f * pos.x + sqrt(3f) / 3f * pos.y) / size
    return axialRound(q, r)
}

private fun axialRound(q: Float, r: Float): Pair<Int, Int> {
    val s = -q - r
    var rQ = q.roundToInt()
    var rR = r.roundToInt()
    val rS = s.roundToInt()
    val dq = kotlin.math.abs(rQ - q)
    val dr = kotlin.math.abs(rR - r)
    val ds = kotlin.math.abs(rS - s)
    if (dq > dr && dq > ds) rQ = -rR - rS
    else if (dr > ds) rR = -rQ - rS
    return rQ to rR
}

private fun screenToWorld(screen: Offset, offset: Offset, scale: Float): Offset =
    Offset((screen.x - offset.x) / scale, (screen.y - offset.y) / scale)

private fun worldToScreen(world: Offset, offset: Offset, scale: Float): Offset =
    Offset(world.x * scale + offset.x, world.y * scale + offset.y)

// --- Drawing ---

private data class ViewportBounds(val left: Float, val top: Float, val right: Float, val bottom: Float)

private fun DrawScope.drawHexTile(
    tile: HexTile,
    center: Offset,
    hexSize: Float,
    mode: HexCanvasMode,
    isSelected: Boolean,
    pois: List<HexPoi>
) {
    val path = hexPath(center, hexSize - 1f)
    val terrainColor = Color(tile.terrainColor)

    // Fill
    if (mode == HexCanvasMode.SESSION && !tile.isExplored) {
        // Fog of war: dark overlay
        drawPath(path, Color(0xFF1A1A2E))
        drawPath(path, Color.Black.copy(alpha = FOG_ALPHA))
    } else {
        drawPath(path, terrainColor)
        
        // Advanced Terrain Rendering: Draw abstract icons/decorations
        drawTerrainDecoration(tile, center, hexSize)

        // Reconnoitered tint (lighter, info available)
        if (mode == HexCanvasMode.SESSION && tile.isReconnoitered && !tile.isMapped) {
            drawPath(path, Color.White.copy(alpha = 0.15f))
        }
        // Mapped tint (gold shimmer)
        if (tile.isMapped) {
            drawPath(path, Color(0xFFFFD700).copy(alpha = 0.18f))
        }
    }

    // Stroke
    val strokeColor = when {
        isSelected -> Color(0xFFFFD700)
        mode == HexCanvasMode.SESSION && !tile.isExplored -> Color(0xFF2A2A4E)
        else -> Color.Black.copy(alpha = 0.3f)
    }
    val strokeWidth = if (isSelected) 3f else 1.5f
    drawPath(path, strokeColor, style = Stroke(width = strokeWidth))

    // POI indicator: filled circle in center
    if (pois.isNotEmpty() && (mode == HexCanvasMode.DESIGN || tile.isExplored)) {
        val poiColor = poiColor(pois.first().type)
        drawCircle(color = poiColor, radius = hexSize * 0.18f, center = center)
        drawCircle(color = Color.White, radius = hexSize * 0.18f, center = center, style = Stroke(width = 1.5f))
    }

    // Terrain cost indicator (in design mode, show dots for cost 2 or 3)
    if (mode == HexCanvasMode.DESIGN && tile.terrainCost >= 2) {
        val dotRadius = hexSize * 0.07f
        val spacing = dotRadius * 2.8f
        val count = tile.terrainCost.coerceAtMost(3)
        val startX = center.x - spacing * (count - 1) / 2f
        for (i in 0 until count) {
            drawCircle(
                color = Color.White.copy(alpha = 0.8f),
                radius = dotRadius,
                center = Offset(startX + spacing * i, center.y + hexSize * 0.55f)
            )
        }
    }
}

private fun DrawScope.drawTerrainDecoration(tile: HexTile, center: Offset, hexSize: Float) {
    val label = tile.terrainLabel.lowercase()
    val decorationColor = Color.Black.copy(alpha = 0.15f)
    
    when {
        label.contains("bosque") || label.contains("selva") -> {
            // Draw small tree triangles
            val treeSize = hexSize * 0.25f
            drawTree(center.copy(y = center.y - hexSize * 0.2f), treeSize, decorationColor)
            drawTree(center.copy(x = center.x - hexSize * 0.25f, y = center.y + hexSize * 0.15f), treeSize * 0.8f, decorationColor)
            drawTree(center.copy(x = center.x + hexSize * 0.25f, y = center.y + hexSize * 0.15f), treeSize * 0.8f, decorationColor)
        }
        label.contains("montaña") || label.contains("pico") -> {
            // Draw mountain peaks
            val mountainWidth = hexSize * 0.4f
            val mountainHeight = hexSize * 0.35f
            drawMountain(center.copy(y = center.y + hexSize * 0.1f), mountainWidth, mountainHeight, decorationColor)
            drawMountain(center.copy(x = center.x - hexSize * 0.3f, y = center.y + hexSize * 0.2f), mountainWidth * 0.7f, mountainHeight * 0.7f, decorationColor)
        }
        label.contains("agua") || label.contains("mar") || label.contains("lago") -> {
            // Draw waves
            val waveWidth = hexSize * 0.3f
            drawWave(center.copy(x = center.x - waveWidth/2, y = center.y - hexSize * 0.1f), waveWidth, decorationColor)
            drawWave(center.copy(x = center.x - waveWidth/4, y = center.y + hexSize * 0.1f), waveWidth, decorationColor)
        }
        label.contains("colina") -> {
            // Draw curves
            val arcWidth = hexSize * 0.4f
            drawHill(center.copy(y = center.y + hexSize * 0.1f), arcWidth, decorationColor)
        }
        label.contains("llanura") || label.contains("pasto") -> {
            // Draw some grass tufts
            drawGrass(center.copy(x = center.x - hexSize * 0.2f, y = center.y - hexSize * 0.1f), hexSize * 0.15f, decorationColor)
            drawGrass(center.copy(x = center.x + hexSize * 0.1f, y = center.y + hexSize * 0.15f), hexSize * 0.15f, decorationColor)
        }
    }
}

private fun DrawScope.drawTree(center: Offset, size: Float, color: Color) {
    val path = Path().apply {
        moveTo(center.x, center.y - size)
        lineTo(center.x - size / 2, center.y + size / 2)
        lineTo(center.x + size / 2, center.y + size / 2)
        close()
    }
    drawPath(path, color)
}

private fun DrawScope.drawMountain(center: Offset, width: Float, height: Float, color: Color) {
    val path = Path().apply {
        moveTo(center.x, center.y - height)
        lineTo(center.x - width / 2, center.y)
        lineTo(center.x + width / 2, center.y)
        close()
    }
    drawPath(path, color)
}

private fun DrawScope.drawWave(start: Offset, width: Float, color: Color) {
    val path = Path().apply {
        moveTo(start.x, start.y)
        quadraticBezierTo(start.x + width / 4, start.y - width / 8, start.x + width / 2, start.y)
        quadraticBezierTo(start.x + 3 * width / 4, start.y + width / 8, start.x + width, start.y)
    }
    drawPath(path, color, style = Stroke(width = 1.5f))
}

private fun DrawScope.drawHill(center: Offset, width: Float, color: Color) {
    val path = Path().apply {
        moveTo(center.x - width / 2, center.y)
        quadraticBezierTo(center.x, center.y - width / 2, center.x + width / 2, center.y)
    }
    drawPath(path, color, style = Stroke(width = 1.5f))
}

private fun DrawScope.drawGrass(center: Offset, size: Float, color: Color) {
    drawLine(color, center, center.copy(x = center.x - size / 2, y = center.y - size), strokeWidth = 1.5f)
    drawLine(color, center, center.copy(y = center.y - size * 1.2f), strokeWidth = 1.5f)
    drawLine(color, center, center.copy(x = center.x + size / 2, y = center.y - size), strokeWidth = 1.5f)
}

private fun poiColor(type: PoiType): Color = when (type) {
    PoiType.DUNGEON -> Color(0xFF8B0000)
    PoiType.SETTLEMENT -> Color(0xFF4682B4)
    PoiType.LANDMARK -> Color(0xFF9370DB)
    PoiType.HAZARD -> Color(0xFFFF4500)
    PoiType.RESOURCE -> Color(0xFF228B22)
    PoiType.SECRET -> Color(0xFFDAA520)
    PoiType.CUSTOM -> Color(0xFF808080)
}
