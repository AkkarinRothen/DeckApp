package com.deckapp.feature.hexploration.components

import android.content.ClipData
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deckapp.core.model.HexPoi
import com.deckapp.core.model.HexTile
import com.deckapp.core.model.PoiType
import kotlin.math.*

enum class HexCanvasMode { DESIGN, SESSION }

internal const val HEX_SIZE_DEFAULT = 80f   // radius in px at scale=1
private const val FOG_ALPHA = 0.75f

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HexGridCanvas(
    tiles: List<HexTile>,
    pois: List<HexPoi>,
    mode: HexCanvasMode,
    selectedTile: HexTile?,
    onTileClick: (HexTile) -> Unit,
    onTileLongPress: (HexTile) -> Unit,
    onEmptySpaceClick: ((Int, Int) -> Unit)? = null,
    onExploreTile: ((HexTile) -> Unit)? = null,
    onStateChanged: ((Float, Offset) -> Unit)? = null,
    onTimeSkip: (() -> Unit)? = null,
    partyLocation: Pair<Int, Int>? = null,
    onMoveParty: ((Int, Int) -> Unit)? = null,
    showCoordinates: Boolean = false,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    
    val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale = (scale * zoomChange).coerceIn(0.2f, 5f)
        offset += offsetChange
    }

    // Time-Skip Logic: Gesto circular
    var lastAngle by remember { mutableFloatStateOf(0f) }
    var totalRotation by remember { mutableFloatStateOf(0f) }
    var isRotating by remember { mutableStateOf(false) }

    // Rascado para explorar
    var scratchedTile by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var scratchPoints by remember { mutableStateOf<List<Offset>>(emptyList()) }

    // Sincronizar estado con el exterior
    LaunchedEffect(scale, offset) {
        onStateChanged?.invoke(scale, offset)
    }

    val tileMap = remember(tiles) { tiles.associateBy { it.q to it.r } }
    val poiMap = remember(pois) { pois.groupBy { it.tileQ to it.tileR } }

    Canvas(
        modifier = modifier
            .fillMaxSize()
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
                            onTileLongPress(tile)
                        }
                    }
                )
            }
            .pointerInput(mode, onTimeSkip) {
                if (mode == HexCanvasMode.SESSION && onTimeSkip != null) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.changes.size == 2) {
                                val change1 = event.changes[0]
                                val change2 = event.changes[1]
                                val center = (change1.position + change2.position) / 2f
                                val currentVector = change1.position - center
                                val angle = Math.toDegrees(atan2(currentVector.y.toDouble(), currentVector.x.toDouble())).toFloat()

                                if (!isRotating) {
                                    lastAngle = angle
                                    isRotating = true
                                    totalRotation = 0f
                                } else {
                                    var delta = angle - lastAngle
                                    if (delta > 180f) delta -= 360f
                                    if (delta < -180f) delta += 360f
                                    totalRotation += delta
                                    lastAngle = angle

                                    if (abs(totalRotation) > 300f) {
                                        onTimeSkip()
                                        totalRotation = 0f
                                        isRotating = false
                                    }
                                }
                            } else {
                                isRotating = false
                            }
                        }
                    }
                }
            }
            .pointerInput(mode, tileMap, scale, offset) {
                if (mode == HexCanvasMode.SESSION) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val position = event.changes.first().position
                            val worldPos = screenToWorld(position, offset, scale)
                            val (q, r) = worldToAxial(worldPos, HEX_SIZE_DEFAULT)
                            val tile = tileMap[q to r]

                            if (tile != null && !tile.isExplored) {
                                if (scratchedTile != (q to r)) {
                                    scratchedTile = q to r
                                    scratchPoints = emptyList()
                                }
                                
                                if (event.changes.first().pressed) {
                                    scratchPoints = scratchPoints + position
                                    if (scratchPoints.size > 15) {
                                        onExploreTile?.invoke(tile)
                                        scratchedTile = null
                                        scratchPoints = emptyList()
                                    }
                                }
                            } else {
                                scratchedTile = null
                                scratchPoints = emptyList()
                            }
                        }
                    }
                }
            }
    ) {
        val hexSize = HEX_SIZE_DEFAULT * scale
        val viewportBounds = ViewportBounds(
            left = -offset.x / scale,
            top = -offset.y / scale,
            right = (-offset.x + size.width) / scale,
            bottom = (-offset.y + size.height) / scale
        )

        val margin = HEX_SIZE_DEFAULT * 3
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

            val isExplored = mode == HexCanvasMode.DESIGN || tile.isExplored
            if (isExplored) {
                if (showCoordinates) {
                    drawTerrainLabel("${tile.q},${tile.r}", screenCenter.copy(y = screenCenter.y - hexSize * 0.45f), hexSize, textMeasurer)
                }
                if (tile.terrainLabel.isNotBlank()) {
                    drawTerrainLabel(tile.terrainLabel, screenCenter, hexSize, textMeasurer)
                }
            }
        }

        partyLocation?.let { (q, r) ->
            val center = axialToPixel(q, r, HEX_SIZE_DEFAULT)
            val screenCenter = worldToScreen(center, offset, scale)
            drawPartyToken(screenCenter, hexSize)
        }

        if (selectedTile != null && partyLocation != null && (selectedTile.q != partyLocation.first || selectedTile.r != partyLocation.second)) {
            val startCenter = axialToPixel(partyLocation.first, partyLocation.second, HEX_SIZE_DEFAULT)
            val endCenter = axialToPixel(selectedTile.q, selectedTile.r, HEX_SIZE_DEFAULT)
            
            val startScreen = worldToScreen(startCenter, offset, scale)
            val endScreen = worldToScreen(endCenter, offset, scale)
            
            drawLine(
                color = Color.White.copy(alpha = 0.6f),
                start = startScreen,
                end = endScreen,
                strokeWidth = 2f * scale,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f * scale, 10f * scale))
            )
        }
    }
}

private fun DrawScope.drawPartyToken(center: Offset, hexSize: Float) {
    val tokenSize = hexSize * 0.4f
    val path = Path().apply {
        moveTo(center.x, center.y - tokenSize)
        lineTo(center.x + tokenSize * 0.8f, center.y - tokenSize * 0.2f)
        lineTo(center.x + tokenSize * 0.6f, center.y + tokenSize * 0.8f)
        quadraticTo(center.x, center.y + tokenSize * 1.1f, center.x - tokenSize * 0.6f, center.y + tokenSize * 0.8f)
        lineTo(center.x - tokenSize * 0.8f, center.y - tokenSize * 0.2f)
        close()
    }
    
    drawPath(path, Color(0xFFE91E63))
    drawPath(path, Color.White, style = Stroke(width = 2f))
    drawCircle(Color.White.copy(alpha = 0.3f), radius = tokenSize * 0.3f, center = center)
}

fun hexDistance(q1: Int, r1: Int, q2: Int, r2: Int): Int {
    return (abs(q1 - q2) + 
            abs(q1 + r1 - q2 - r2) + 
            abs(r1 - r2)) / 2
}

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

fun axialToPixel(q: Int, r: Int, size: Float): Offset {
    val x = size * (3f / 2f * q)
    val y = size * (sqrt(3f) / 2f * q + sqrt(3f) * r)
    return Offset(x, y)
}

fun worldToAxial(pos: Offset, size: Float): Pair<Int, Int> {
    val q = (2f / 3f * pos.x) / size
    val r = (-1f / 3f * pos.x + sqrt(3f) / 3f * pos.y) / size
    return axialRound(q, r)
}

private fun axialRound(q: Float, r: Float): Pair<Int, Int> {
    val s = -q - r
    var rQ = q.roundToInt()
    var rR = r.roundToInt()
    val rS = s.roundToInt()
    val dq = abs(rQ - q)
    val dr = abs(rR - r)
    val ds = abs(rS - s)
    if (dq > dr && dq > ds) rQ = -rR - rS
    else if (dr > ds) rR = -rQ - rS
    return rQ to rR
}

fun screenToWorld(screen: Offset, offset: Offset, scale: Float): Offset =
    Offset((screen.x - offset.x) / scale, (screen.y - offset.y) / scale)

fun worldToScreen(world: Offset, offset: Offset, scale: Float): Offset =
    Offset(world.x * scale + offset.x, world.y * scale + offset.y)

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

    if (mode == HexCanvasMode.SESSION && !tile.isExplored) {
        val fogBrush = Brush.radialGradient(
            colors = listOf(Color(0xFF1A1A2E), Color(0xFF0D0D1A)),
            center = center,
            radius = hexSize * 1.2f
        )
        drawPath(path, fogBrush)
        drawPath(path, Color.Black.copy(alpha = 0.4f), style = Stroke(width = 4f))
    } else {
        drawPath(path, terrainColor)
        val highlightBrush = Brush.verticalGradient(
            colors = listOf(Color.White.copy(alpha = 0.1f), Color.Transparent),
            startY = center.y - hexSize,
            endY = center.y + hexSize
        )
        drawPath(path, highlightBrush)
        drawTerrainDecoration(tile, center, hexSize)

        if (mode == HexCanvasMode.SESSION && tile.isReconnoitered && !tile.isMapped) {
            drawPath(path, Color.White.copy(alpha = 0.15f))
        }
        if (tile.isMapped) {
            val goldBrush = Brush.linearGradient(
                colors = listOf(Color(0xFFFFD700).copy(alpha = 0.25f), Color(0xFFFFD700).copy(alpha = 0.05f)),
                start = Offset(center.x - hexSize, center.y - hexSize),
                end = Offset(center.x + hexSize, center.y + hexSize)
            )
            drawPath(path, goldBrush)
        }
    }

    val strokeColor = when {
        isSelected -> Color(0xFFFFD700)
        mode == HexCanvasMode.SESSION && !tile.isExplored -> Color(0xFF2A2A4E).copy(alpha = 0.6f)
        else -> Color.Black.copy(alpha = 0.25f)
    }
    val strokeWidth = if (isSelected) 3f else 1.5f
    drawPath(path, strokeColor, style = Stroke(width = strokeWidth))
    
    if (pois.isNotEmpty() && (mode == HexCanvasMode.DESIGN || tile.isExplored)) {
        val poiColor = poiColor(pois.first().type)
        val hasMultiple = pois.size > 1
        drawCircle(color = poiColor, radius = hexSize * 0.18f, center = center)
        drawCircle(
            color = Color.White,
            radius = if (hasMultiple) hexSize * 0.22f else hexSize * 0.18f,
            center = center,
            style = Stroke(width = if (hasMultiple) 3f else 1.5f)
        )
    }

    if (mode == HexCanvasMode.DESIGN && tile.terrainCost >= 2) {
        val dotRadius = hexSize * 0.05f
        val spacing = dotRadius * 2.5f
        val count = tile.terrainCost.coerceAtMost(3)
        val startX = center.x + hexSize * 0.35f
        val startY = center.y - hexSize * 0.45f
        for (i in 0 until count) {
            drawCircle(
                color = Color.White.copy(alpha = 0.7f),
                radius = dotRadius,
                center = Offset(startX, startY + spacing * i)
            )
        }
    }
}

private fun DrawScope.drawTerrainLabel(
    label: String,
    center: Offset,
    hexSize: Float,
    textMeasurer: TextMeasurer
) {
    if (hexSize < 30f) return
    if (center.x < -10000f || center.x > 10000f || center.y < -10000f || center.y > 10000f) return
    if (center.x.isNaN() || center.y.isNaN()) return

    val fontSize = (hexSize * 0.14f).coerceIn(9f, 18f).sp
    val style = TextStyle(fontSize = fontSize, color = Color.White.copy(alpha = 0.85f))
    
    val textLayoutResult = try {
        textMeasurer.measure(
            text = label,
            style = style,
            softWrap = false,
            maxLines = 1
        )
    } catch (e: Exception) { return }

    val textWidth = textLayoutResult.size.width.toFloat()
    val textHeight = textLayoutResult.size.height.toFloat()
    val textX = center.x - textWidth / 2f
    val textY = center.y + hexSize * 0.42f - textHeight / 2f
    
    if (textX.isNaN() || textY.isNaN()) return

    drawIntoCanvas { canvas ->
        canvas.save()
        canvas.translate(textX, textY)
        textLayoutResult.multiParagraph.paint(
            canvas = canvas,
            color = Color.White.copy(alpha = 0.85f)
        )
        canvas.restore()
    }
}

private fun DrawScope.drawTerrainDecoration(tile: HexTile, center: Offset, hexSize: Float) {
    val label = tile.terrainLabel.lowercase()
    val decorationColor = Color.Black.copy(alpha = 0.15f)
    
    when {
        label.contains("bosque") || label.contains("selva") -> {
            val treeSize = hexSize * 0.25f
            drawTree(center.copy(y = center.y - hexSize * 0.2f), treeSize, decorationColor)
            drawTree(center.copy(x = center.x - hexSize * 0.25f, y = center.y + hexSize * 0.15f), treeSize * 0.8f, decorationColor)
            drawTree(center.copy(x = center.x + hexSize * 0.25f, y = center.y + hexSize * 0.15f), treeSize * 0.8f, decorationColor)
        }
        label.contains("montaña") || label.contains("pico") -> {
            val mountainWidth = hexSize * 0.4f
            val mountainHeight = hexSize * 0.35f
            drawMountain(center.copy(y = center.y + hexSize * 0.1f), mountainWidth, mountainHeight, decorationColor)
            drawMountain(center.copy(x = center.x - hexSize * 0.3f, y = center.y + hexSize * 0.2f), mountainWidth * 0.7f, mountainHeight * 0.7f, decorationColor)
        }
        label.contains("agua") || label.contains("mar") || label.contains("lago") -> {
            val waveWidth = hexSize * 0.3f
            drawWave(center.copy(x = center.x - waveWidth/2, y = center.y - hexSize * 0.1f), waveWidth, decorationColor)
            drawWave(center.copy(x = center.x - waveWidth/4, y = center.y + hexSize * 0.1f), waveWidth, decorationColor)
        }
        label.contains("colina") -> {
            val arcWidth = hexSize * 0.4f
            drawHill(center.copy(y = center.y + hexSize * 0.1f), arcWidth, decorationColor)
        }
        label.contains("llanura") || label.contains("pasto") -> {
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
        quadraticTo(start.x + width / 4, start.y - width / 8, start.x + width / 2, start.y)
        quadraticTo(start.x + 3 * width / 4, start.y + width / 8, start.x + width, start.y)
    }
    drawPath(path, color, style = Stroke(width = 1.5f))
}

private fun DrawScope.drawHill(center: Offset, width: Float, color: Color) {
    val path = Path().apply {
        moveTo(center.x - width / 2, center.y)
        quadraticTo(center.x, center.y - width / 2, center.x + width / 2, center.y)
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
