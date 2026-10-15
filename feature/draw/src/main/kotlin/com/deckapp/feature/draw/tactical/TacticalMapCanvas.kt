package com.deckapp.feature.draw.tactical

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.deckapp.core.domain.usecase.CalculateTacticalDistanceUseCase
import com.deckapp.core.model.*
import kotlin.math.*

@Composable
fun TacticalMapCanvas(
    uiState: TacticalUiState,
    distanceCalculator: CalculateTacticalDistanceUseCase,
    onTokenSelected: (Long?) -> Unit,
    onTokenMoved: (tokenId: Long, newCellX: Float, newCellY: Float) -> Unit,
    onPinClicked: (Long) -> Unit,
    onMapClicked: (cellX: Float, cellY: Float) -> Unit,
    onAoEPositionChanged: ((origin: Offset, angle: Float) -> Unit)? = null,
    onGuidedCalibrationPoint: ((Offset) -> Unit)? = null,
    onTerrainCellInteracted: ((col: Int, row: Int) -> Unit)? = null,
    onFogCellInteracted: ((col: Int, row: Int) -> Unit)? = null,
    onAddRouteWaypoint: ((cellX: Float, cellY: Float) -> Unit)? = null,
    onClearCameraFocus: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    var zoom by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    val map = uiState.map ?: return

    // Auto-foco suave de cámara en el token seleccionado o con turno activo (Atlas VTT)
    LaunchedEffect(uiState.focusCameraOnTokenId) {
        val tokenId = uiState.focusCameraOnTokenId ?: return@LaunchedEffect
        val token = uiState.tokens.find { it.id == tokenId } ?: return@LaunchedEffect
        val tokenWorldX = (token.cellX * map.cellSizePx) + map.gridOffsetX
        val tokenWorldY = (token.cellY * map.cellSizePx) + map.gridOffsetY
        panOffset = Offset(-tokenWorldX * zoom + 400f, -tokenWorldY * zoom + 300f)
        onClearCameraFocus()
    }

    // Estado local de dragging interactivo
    var draggingToken by remember { mutableStateOf<TacticalToken?>(null) }
    var dragWorldPos by remember { mutableStateOf(Offset.Zero) }
    var dragStartCell by remember { mutableStateOf<Pair<Float, Float>?>(null) }
    var currentDragDistanceFt by remember { mutableIntStateOf(0) }

    // Medición libre interactiva
    var measureStartPos by remember { mutableStateOf<Offset?>(null) }
    var measureCurrentPos by remember { mutableStateOf<Offset?>(null) }

    // AoE interactivo local
    var aoeLocalOrigin by remember { mutableStateOf(Offset.Zero) }
    var aoeLocalAngle by remember { mutableFloatStateOf(0f) }

    val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
        zoom = (zoom * zoomChange).coerceIn(0.25f, 4.0f)
        panOffset += offsetChange
    }

    Box(modifier = modifier.fillMaxSize()) {
        // 1. Imagen del Battlemap (renderizada con Coil y sincronizada con pan/zoom)
        if (map.imageUri.isNotBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(map.imageUri)
                    .crossfade(true)
                    .build(),
                contentDescription = "Battlemap Background",
                contentScale = ContentScale.None,
                alignment = Alignment.TopStart,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationX = panOffset.x
                        translationY = panOffset.y
                        scaleX = zoom
                        scaleY = zoom
                        transformOrigin = TransformOrigin(0f, 0f)
                    }
            )
        }

        // 2. Canvas táctico interactivo
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .transformable(state = transformState, enabled = uiState.activeTool == TacticalTool.NAVIGATE)
                .pointerInput(uiState.activeTool, uiState.tokens, uiState.isGuidedCalibrationActive) {
                    detectTapGestures(
                        onTap = { screenOffset ->
                            val worldX = (screenOffset.x - panOffset.x) / zoom
                            val worldY = (screenOffset.y - panOffset.y) / zoom
                            val worldPos = Offset(worldX, worldY)

                            if (uiState.isGuidedCalibrationActive) {
                                onGuidedCalibrationPoint?.invoke(worldPos)
                                return@detectTapGestures
                            }

                            val cellX = (worldX - map.gridOffsetX) / map.cellSizePx
                            val cellY = (worldY - map.gridOffsetY) / map.cellSizePx

                            if (uiState.activeTool == TacticalTool.TERRAIN) {
                                val col = floor(cellX).toInt()
                                val row = floor(cellY).toInt()
                                onTerrainCellInteracted?.invoke(col, row)
                                return@detectTapGestures
                            }

                            if (uiState.activeTool == TacticalTool.FOG) {
                                val col = floor(cellX).toInt()
                                val row = floor(cellY).toInt()
                                onFogCellInteracted?.invoke(col, row)
                                return@detectTapGestures
                            }

                            if (uiState.activeTool == TacticalTool.ROUTE) {
                                val clickedToken = uiState.tokens.find { token ->
                                    val tokenPxX = (token.cellX * map.cellSizePx) + map.gridOffsetX
                                    val tokenPxY = (token.cellY * map.cellSizePx) + map.gridOffsetY
                                    val radius = (token.sizeInCells * map.cellSizePx * 0.5f)
                                    hypot(worldX - tokenPxX, worldY - tokenPxY) <= radius
                                }
                                if (clickedToken != null && clickedToken.id != uiState.selectedTokenId) {
                                    onTokenSelected(clickedToken.id)
                                } else {
                                    val targetCellX = floor(cellX) + 0.5f
                                    val targetCellY = floor(cellY) + 0.5f
                                    onAddRouteWaypoint?.invoke(targetCellX, targetCellY)
                                }
                                return@detectTapGestures
                            }

                            // Comprobar pines
                            val clickedPin = uiState.pins.find { pin ->
                                val pinPxX = (pin.cellX * map.cellSizePx) + map.gridOffsetX
                                val pinPxY = (pin.cellY * map.cellSizePx) + map.gridOffsetY
                                hypot(worldX - pinPxX, worldY - pinPxY) <= (map.cellSizePx * 0.4f)
                            }
                            if (clickedPin != null) {
                                onPinClicked(clickedPin.id)
                                return@detectTapGestures
                            }

                            // Comprobar tokens
                            val clickedToken = uiState.tokens.find { token ->
                                val tokenPxX = (token.cellX * map.cellSizePx) + map.gridOffsetX
                                val tokenPxY = (token.cellY * map.cellSizePx) + map.gridOffsetY
                                val radius = (token.sizeInCells * map.cellSizePx * 0.5f)
                                hypot(worldX - tokenPxX, worldY - tokenPxY) <= radius
                            }

                            if (clickedToken != null) {
                                onTokenSelected(clickedToken.id)
                            } else {
                                onTokenSelected(null)
                                if (uiState.activeTool == TacticalTool.AOE) {
                                    aoeLocalOrigin = worldPos
                                    onAoEPositionChanged?.invoke(worldPos, aoeLocalAngle)
                                } else {
                                    onMapClicked(cellX, cellY)
                                }
                            }
                        }
                    )
                }
                .pointerInput(uiState.activeTool, uiState.tokens, zoom, panOffset) {
                    if (uiState.activeTool == TacticalTool.TERRAIN) {
                        detectDragGestures(
                            onDragStart = { start ->
                                val worldX = (start.x - panOffset.x) / zoom
                                val worldY = (start.y - panOffset.y) / zoom
                                val col = floor((worldX - map.gridOffsetX) / map.cellSizePx).toInt()
                                val row = floor((worldY - map.gridOffsetY) / map.cellSizePx).toInt()
                                onTerrainCellInteracted?.invoke(col, row)
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                val worldX = (change.position.x - panOffset.x) / zoom
                                val worldY = (change.position.y - panOffset.y) / zoom
                                val col = floor((worldX - map.gridOffsetX) / map.cellSizePx).toInt()
                                val row = floor((worldY - map.gridOffsetY) / map.cellSizePx).toInt()
                                onTerrainCellInteracted?.invoke(col, row)
                            }
                        )
                    } else if (uiState.activeTool == TacticalTool.FOG) {
                        detectDragGestures(
                            onDragStart = { start ->
                                val worldX = (start.x - panOffset.x) / zoom
                                val worldY = (start.y - panOffset.y) / zoom
                                val col = floor((worldX - map.gridOffsetX) / map.cellSizePx).toInt()
                                val row = floor((worldY - map.gridOffsetY) / map.cellSizePx).toInt()
                                onFogCellInteracted?.invoke(col, row)
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                val worldX = (change.position.x - panOffset.x) / zoom
                                val worldY = (change.position.y - panOffset.y) / zoom
                                val col = floor((worldX - map.gridOffsetX) / map.cellSizePx).toInt()
                                val row = floor((worldY - map.gridOffsetY) / map.cellSizePx).toInt()
                                onFogCellInteracted?.invoke(col, row)
                            }
                        )
                    } else if (uiState.activeTool == TacticalTool.MEASURE) {
                        detectDragGestures(
                            onDragStart = { start ->
                                val worldX = (start.x - panOffset.x) / zoom
                                val worldY = (start.y - panOffset.y) / zoom
                                measureStartPos = Offset(worldX, worldY)
                                measureCurrentPos = Offset(worldX, worldY)
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                measureCurrentPos = measureCurrentPos?.let { it + (dragAmount / zoom) }
                            },
                            onDragEnd = {
                                measureStartPos = null
                                measureCurrentPos = null
                            },
                            onDragCancel = {
                                measureStartPos = null
                                measureCurrentPos = null
                            }
                        )
                    } else if (uiState.activeTool == TacticalTool.AOE) {
                        detectDragGestures(
                            onDragStart = { start ->
                                val worldX = (start.x - panOffset.x) / zoom
                                val worldY = (start.y - panOffset.y) / zoom
                                aoeLocalOrigin = Offset(worldX, worldY)
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val currentDragWorld = Offset(
                                    (change.position.x - panOffset.x) / zoom,
                                    (change.position.y - panOffset.y) / zoom
                                )
                                val dx = currentDragWorld.x - aoeLocalOrigin.x
                                val dy = currentDragWorld.y - aoeLocalOrigin.y
                                aoeLocalAngle = (atan2(dy, dx) * 180f / PI.toFloat())
                                onAoEPositionChanged?.invoke(aoeLocalOrigin, aoeLocalAngle)
                            }
                        )
                    } else if (uiState.activeTool != TacticalTool.NAVIGATE && uiState.activeTool != TacticalTool.ROUTE) {
                        detectDragGestures(
                            onDragStart = { startOffset ->
                                val worldX = (startOffset.x - panOffset.x) / zoom
                                val worldY = (startOffset.y - panOffset.y) / zoom

                                val touchedToken = uiState.tokens.find { token ->
                                    val tokenPxX = (token.cellX * map.cellSizePx) + map.gridOffsetX
                                    val tokenPxY = (token.cellY * map.cellSizePx) + map.gridOffsetY
                                    val radius = (token.sizeInCells * map.cellSizePx * 0.5f)
                                    hypot(worldX - tokenPxX, worldY - tokenPxY) <= radius
                                }

                                if (touchedToken != null) {
                                    draggingToken = touchedToken
                                    dragWorldPos = Offset(worldX, worldY)
                                    dragStartCell = Pair(touchedToken.cellX, touchedToken.cellY)
                                    onTokenSelected(touchedToken.id)
                                }
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                draggingToken?.let { token ->
                                    dragWorldPos += (dragAmount / zoom)

                                    val targetCellX = floor((dragWorldPos.x - map.gridOffsetX) / map.cellSizePx) + (token.sizeInCells / 2f)
                                    val targetCellY = floor((dragWorldPos.y - map.gridOffsetY) / map.cellSizePx) + (token.sizeInCells / 2f)

                                    dragStartCell?.let { start ->
                                        val dist = distanceCalculator(
                                            fromX = start.first,
                                            fromY = start.second,
                                            toX = targetCellX,
                                            toY = targetCellY,
                                            gridType = map.gridType,
                                            diagonalRule = map.diagonalRule,
                                            scaleFtPerCell = map.scaleFtPerCell,
                                            terrainMap = uiState.terrainMap
                                        )
                                        currentDragDistanceFt = dist.distanceFt
                                    }
                                }
                            },
                            onDragEnd = {
                                draggingToken?.let { token ->
                                    val snappedCellX = floor((dragWorldPos.x - map.gridOffsetX) / map.cellSizePx) + (token.sizeInCells / 2f)
                                    val snappedCellY = floor((dragWorldPos.y - map.gridOffsetY) / map.cellSizePx) + (token.sizeInCells / 2f)
                                    onTokenMoved(token.id, snappedCellX, snappedCellY)
                                }
                                draggingToken = null
                                dragStartCell = null
                                currentDragDistanceFt = 0
                            },
                            onDragCancel = {
                                draggingToken = null
                                dragStartCell = null
                                currentDragDistanceFt = 0
                            }
                        )
                    }
                }
        ) {
            // Aplicar transformaciones de cámara (Pan & Zoom)
            drawContext.canvas.save()
            drawContext.canvas.translate(panOffset.x, panOffset.y)
            drawContext.canvas.scale(zoom, zoom)

            // Fondo neutro si no hay imagen
            if (map.imageUri.isBlank()) {
                drawRect(
                    color = Color(0xFF141416),
                    size = Size(size.width / zoom + abs(panOffset.x) * 2, size.height / zoom + abs(panOffset.y) * 2),
                    topLeft = Offset(-abs(panOffset.x), -abs(panOffset.y))
                )
            }

            // Terreno táctico (Difícil, Muros, Peligros)
            drawTacticalTerrain(
                map = map,
                terrainMap = uiState.terrainMap,
                zoom = zoom
            )

            // Grilla táctica
            if (map.gridType != GridType.NONE) {
                drawTacticalGrid(
                    map = map,
                    viewSize = size,
                    zoom = zoom,
                    panOffset = panOffset
                )
            }

            // Niebla de Guerra (Fog of War)
            if (uiState.isFogEnabled) {
                drawTacticalFog(
                    map = map,
                    revealedCells = uiState.revealedCells,
                    isFogDmView = uiState.isFogDmView,
                    viewSize = size,
                    zoom = zoom,
                    panOffset = panOffset
                )
            }

            // Áreas persistentes de conjuros (Persistent AoE)
            uiState.persistentAoEs.forEach { aoe ->
                drawAoETemplate(
                    shape = aoe.shape,
                    origin = aoe.originWorld,
                    radiusPx = (aoe.sizeFt.toFloat() / map.scaleFtPerCell) * map.cellSizePx,
                    angleDegrees = aoe.angleDegrees,
                    fillColor = Color(aoe.colorHex),
                    strokeColor = Color(aoe.colorHex or 0xFF000000),
                    zoom = zoom,
                    label = "${aoe.sizeFt} ft",
                    textMeasurer = textMeasurer
                )
            }

            // Plantilla AoE activa
            if (uiState.activeTool == TacticalTool.AOE) {
                val effectiveOrigin = if (aoeLocalOrigin != Offset.Zero) {
                    aoeLocalOrigin
                } else if (uiState.selectedToken != null) {
                    val tok = uiState.selectedToken!!
                    Offset((tok.cellX * map.cellSizePx) + map.gridOffsetX, (tok.cellY * map.cellSizePx) + map.gridOffsetY)
                } else {
                    Offset(map.gridOffsetX + map.cellSizePx * 3, map.gridOffsetY + map.cellSizePx * 3)
                }

                val radiusPx = (uiState.aoeRadiusFt.toFloat() / map.scaleFtPerCell) * map.cellSizePx
                drawAoETemplate(
                    shape = uiState.activeAoeShape,
                    origin = effectiveOrigin,
                    radiusPx = radiusPx,
                    angleDegrees = aoeLocalAngle,
                    fillColor = Color(0x38FF9800),
                    strokeColor = Color(0xFFFF9800),
                    zoom = zoom,
                    label = "${uiState.aoeRadiusFt} ft",
                    textMeasurer = textMeasurer
                )
            }

            // Medición libre
            if (measureStartPos != null && measureCurrentPos != null) {
                val start = measureStartPos!!
                val current = measureCurrentPos!!

                val fromCellX = (start.x - map.gridOffsetX) / map.cellSizePx
                val fromCellY = (start.y - map.gridOffsetY) / map.cellSizePx
                val toCellX = (current.x - map.gridOffsetX) / map.cellSizePx
                val toCellY = (current.y - map.gridOffsetY) / map.cellSizePx

                val distResult = distanceCalculator(
                    fromX = fromCellX,
                    fromY = fromCellY,
                    toX = toCellX,
                    toY = toCellY,
                    gridType = map.gridType,
                    diagonalRule = map.diagonalRule,
                    scaleFtPerCell = map.scaleFtPerCell,
                    terrainMap = uiState.terrainMap
                )

                drawLine(
                    color = Color(0xFFFFEB3B),
                    start = start,
                    end = current,
                    strokeWidth = 3f / zoom,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f / zoom, 8f / zoom))
                )

                val midX = (start.x + current.x) / 2f
                val midY = (start.y + current.y) / 2f
                drawMeasureBadge(
                    textMeasurer = textMeasurer,
                    text = "${distResult.distanceFt} ft (${distResult.cells.toInt()} c)",
                    center = Offset(midX, midY),
                    zoom = zoom
                )
            }

            // Calibración guiada 3x3 celdas
            if (uiState.isGuidedCalibrationActive && uiState.guidedCalibrationFirstPoint != null) {
                val p1 = uiState.guidedCalibrationFirstPoint!!
                drawCircle(
                    color = Color(0xFF00E5FF),
                    radius = 8f / zoom,
                    center = p1
                )
            }

            // Ruta táctica planificada (Tool.ROUTE)
            drawRoutePath(
                map = map,
                waypoints = uiState.routeWaypoints,
                currentDistanceFt = uiState.currentRouteDistanceFt,
                selectedToken = uiState.selectedToken,
                zoom = zoom,
                textMeasurer = textMeasurer
            )

            // Drag Ruler al mover un token
            if (draggingToken != null && dragStartCell != null) {
                val startPxX = (dragStartCell!!.first * map.cellSizePx) + map.gridOffsetX
                val startPxY = (dragStartCell!!.second * map.cellSizePx) + map.gridOffsetY
                val snappedCellX = floor((dragWorldPos.x - map.gridOffsetX) / map.cellSizePx) + (draggingToken!!.sizeInCells / 2f)
                val snappedCellY = floor((dragWorldPos.y - map.gridOffsetY) / map.cellSizePx) + (draggingToken!!.sizeInCells / 2f)
                val targetPxX = (snappedCellX * map.cellSizePx) + map.gridOffsetX
                val targetPxY = (snappedCellY * map.cellSizePx) + map.gridOffsetY

                drawCircle(
                    color = Color(0x5500E5FF),
                    radius = (draggingToken!!.sizeInCells * map.cellSizePx * 0.48f),
                    center = Offset(targetPxX, targetPxY)
                )

                drawLine(
                    color = Color(0xFF00E5FF),
                    start = Offset(startPxX, startPxY),
                    end = Offset(targetPxX, targetPxY),
                    strokeWidth = 3f / zoom,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f / zoom, 6f / zoom))
                )

                val midX = (startPxX + targetPxX) / 2f
                val midY = (startPxY + targetPxY) / 2f
                drawMeasureBadge(
                    textMeasurer = textMeasurer,
                    text = "$currentDragDistanceFt ft",
                    center = Offset(midX, midY),
                    zoom = zoom
                )
            }

            // Pines de secretos/habitaciones
            uiState.pins.forEach { pin ->
                val pxX = (pin.cellX * map.cellSizePx) + map.gridOffsetX
                val pxY = (pin.cellY * map.cellSizePx) + map.gridOffsetY
                val pinRadius = map.cellSizePx * 0.28f

                drawCircle(
                    color = Color(pin.colorHex),
                    radius = pinRadius,
                    center = Offset(pxX, pxY)
                )
                drawCircle(
                    color = Color.White,
                    radius = pinRadius,
                    center = Offset(pxX, pxY),
                    style = Stroke(width = 2f / zoom)
                )

                val initial = if (pin.isSecret) "?" else "!"
                val pinFontSize = (13f / zoom).coerceIn(8f, 24f).sp
                val textLayout = textMeasurer.measure(
                    text = initial,
                    style = TextStyle(
                        color = Color.Black,
                        fontSize = pinFontSize,
                        fontWeight = FontWeight.Bold
                    )
                )
                drawText(
                    textLayoutResult = textLayout,
                    topLeft = Offset(pxX - textLayout.size.width / 2f, pxY - textLayout.size.height / 2f)
                )
            }

            // Tokens
            uiState.tokens.forEach { token ->
                val isBeingDragged = token.id == draggingToken?.id
                val center = if (isBeingDragged) {
                    dragWorldPos
                } else {
                    Offset(
                        (token.cellX * map.cellSizePx) + map.gridOffsetX,
                        (token.cellY * map.cellSizePx) + map.gridOffsetY
                    )
                }

                drawTacticalToken(
                    token = token,
                    center = center,
                    cellSize = map.cellSizePx,
                    isSelected = token.id == uiState.selectedTokenId,
                    isActiveTurn = token.id == uiState.activeCombatTurnTokenId,
                    isTargeted = token.id == uiState.pendingAttackTargetTokenId,
                    zoom = zoom,
                    textMeasurer = textMeasurer
                )
            }

            // Línea de Puntería Táctica (Targeting Line)
            val attackerToken = uiState.targetingAttackerToken
            if (attackerToken != null) {
                val attackerCenter = Offset(
                    (attackerToken.cellX * map.cellSizePx) + map.gridOffsetX,
                    (attackerToken.cellY * map.cellSizePx) + map.gridOffsetY
                )

                val targetToken = uiState.pendingAttackTargetToken ?: uiState.selectedToken.takeIf { it?.id != attackerToken.id }
                if (targetToken != null) {
                    val targetCenter = Offset(
                        (targetToken.cellX * map.cellSizePx) + map.gridOffsetX,
                        (targetToken.cellY * map.cellSizePx) + map.gridOffsetY
                    )

                    drawLine(
                        color = Color(0xFFFF5252),
                        start = attackerCenter,
                        end = targetCenter,
                        strokeWidth = 3f / zoom,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f / zoom, 10f / zoom), 0f)
                    )

                    val distResult = distanceCalculator(
                        fromX = attackerToken.cellX,
                        fromY = attackerToken.cellY,
                        toX = targetToken.cellX,
                        toY = targetToken.cellY,
                        scaleFtPerCell = map.scaleFtPerCell,
                        gridType = map.gridType,
                        terrainMap = uiState.terrainMap,
                        fromElevationFt = attackerToken.elevationFt,
                        toElevationFt = targetToken.elevationFt
                    )
                    val elevDiff = distResult.elevationDiffFt
                    val targetText = if (elevDiff > 0) {
                        "Objetivo: ${distResult.distance3dFt} ft (Δh: ${elevDiff}ft)"
                    } else {
                        "Objetivo: ${distResult.distanceFt} ft"
                    }
                    val midPoint = Offset((attackerCenter.x + targetCenter.x) / 2f, (attackerCenter.y + targetCenter.y) / 2f)

                    drawMeasureBadge(
                        textMeasurer = textMeasurer,
                        text = targetText,
                        center = midPoint,
                        zoom = zoom
                    )
                }
            }

            drawContext.canvas.restore()
        }
    }
}

private fun DrawScope.drawAoETemplate(
    shape: TacticalMeasureShape,
    origin: Offset,
    radiusPx: Float,
    angleDegrees: Float,
    fillColor: Color,
    strokeColor: Color,
    zoom: Float,
    label: String,
    textMeasurer: TextMeasurer
) {
    val strokeWidth = 2.5f / zoom

    when (shape) {
        TacticalMeasureShape.CIRCLE -> {
            drawCircle(
                color = fillColor,
                radius = radiusPx,
                center = origin
            )
            drawCircle(
                color = strokeColor,
                radius = radiusPx,
                center = origin,
                style = Stroke(width = strokeWidth)
            )
            drawMeasureBadge(
                textMeasurer = textMeasurer,
                text = "Radio $label",
                center = Offset(origin.x, origin.y - radiusPx),
                zoom = zoom
            )
        }
        TacticalMeasureShape.CONE -> {
            val rad = angleDegrees * (PI.toFloat() / 180f)
            val halfSpread = 26.56f * (PI.toFloat() / 180f) // Cono de 53° estándar D&D
            val leftRad = rad - halfSpread
            val rightRad = rad + halfSpread

            val pLeft = Offset(origin.x + radiusPx * cos(leftRad), origin.y + radiusPx * sin(leftRad))
            val pRight = Offset(origin.x + radiusPx * cos(rightRad), origin.y + radiusPx * sin(rightRad))

            val conePath = Path().apply {
                moveTo(origin.x, origin.y)
                lineTo(pLeft.x, pLeft.y)
                lineTo(pRight.x, pRight.y)
                close()
            }

            drawPath(conePath, color = fillColor)
            drawPath(conePath, color = strokeColor, style = Stroke(width = strokeWidth))

            val labelCenter = Offset(origin.x + (radiusPx * 0.7f) * cos(rad), origin.y + (radiusPx * 0.7f) * sin(rad))
            drawMeasureBadge(textMeasurer, "Cono $label", labelCenter, zoom)
        }
        TacticalMeasureShape.LINE -> {
            val rad = angleDegrees * (PI.toFloat() / 180f)
            val perpRad = rad + (PI.toFloat() / 2f)
            val halfWidthPx = (radiusPx * 0.1f).coerceIn(10f, 30f) // ~5 pies de ancho

            val dx = radiusPx * cos(rad)
            val dy = radiusPx * sin(rad)
            val px = halfWidthPx * cos(perpRad)
            val py = halfWidthPx * sin(perpRad)

            val linePath = Path().apply {
                moveTo(origin.x - px, origin.y - py)
                lineTo(origin.x + dx - px, origin.y + dy - py)
                lineTo(origin.x + dx + px, origin.y + dy + px)
                lineTo(origin.x + px, origin.y + py)
                close()
            }

            drawPath(linePath, color = fillColor)
            drawPath(linePath, color = strokeColor, style = Stroke(width = strokeWidth))
            drawMeasureBadge(textMeasurer, "Línea $label", Offset(origin.x + dx / 2f, origin.y + dy / 2f), zoom)
        }
        TacticalMeasureShape.BOX -> {
            val half = radiusPx
            val rect = androidx.compose.ui.geometry.Rect(
                origin.x - half,
                origin.y - half,
                origin.x + half,
                origin.y + half
            )
            drawRect(
                color = fillColor,
                topLeft = rect.topLeft,
                size = rect.size
            )
            drawRect(
                color = strokeColor,
                topLeft = rect.topLeft,
                size = rect.size,
                style = Stroke(width = strokeWidth)
            )
            drawMeasureBadge(textMeasurer, "Caja ${label}", Offset(origin.x, origin.y - half), zoom)
        }
    }
}

private fun DrawScope.drawTacticalGrid(
    map: TacticalMap,
    viewSize: Size,
    zoom: Float,
    panOffset: Offset
) {
    val gridColor = Color(map.gridColorHex).copy(alpha = map.gridOpacity)
    val cellSize = map.cellSizePx
    val strokeWidth = (1.2f / zoom).coerceAtLeast(0.8f)

    if (map.gridType == GridType.SQUARE) {
        val startX = -panOffset.x / zoom - ((-panOffset.x / zoom - map.gridOffsetX) % cellSize) - cellSize
        val endX = (-panOffset.x + viewSize.width) / zoom + cellSize
        val startY = -panOffset.y / zoom - ((-panOffset.y / zoom - map.gridOffsetY) % cellSize) - cellSize
        val endY = (-panOffset.y + viewSize.height) / zoom + cellSize

        var x = startX
        while (x <= endX) {
            drawLine(
                color = gridColor,
                start = Offset(x, startY),
                end = Offset(x, endY),
                strokeWidth = strokeWidth
            )
            x += cellSize
        }

        var y = startY
        while (y <= endY) {
            drawLine(
                color = gridColor,
                start = Offset(startX, y),
                end = Offset(endX, y),
                strokeWidth = strokeWidth
            )
            y += cellSize
        }
    }
}

private fun DrawScope.drawTacticalToken(
    token: TacticalToken,
    center: Offset,
    cellSize: Float,
    isSelected: Boolean,
    isActiveTurn: Boolean = false,
    isTargeted: Boolean = false,
    zoom: Float,
    textMeasurer: TextMeasurer
) {
    val radius = (token.sizeInCells * cellSize * 0.44f)

    val factionColor = when (token.faction) {
        TokenFaction.PLAYER -> Color(0xFF2196F3)
        TokenFaction.ALLY -> Color(0xFF4CAF50)
        TokenFaction.ENEMY -> Color(0xFFF44336)
        TokenFaction.NEUTRAL -> Color(0xFFFFC107)
    }

    // Sombra de Vuelo en el Suelo (si elevationFt > 0)
    if (token.elevationFt > 0) {
        val shadowOffset = (token.elevationFt.toFloat() / 5f * 2f / zoom).coerceIn(4f / zoom, 24f / zoom)
        drawCircle(
            color = Color(0x55000000),
            radius = radius * 0.95f,
            center = Offset(center.x + shadowOffset, center.y + shadowOffset)
        )
    }

    if (isActiveTurn) {
        // Halo dorado de iniciativa (Turno activo)
        drawCircle(
            color = Color(0xFFFFD700).copy(alpha = 0.35f),
            radius = radius + (11f / zoom),
            center = center
        )
        drawCircle(
            color = Color(0xFFFFD700),
            radius = radius + (5f / zoom),
            center = center,
            style = Stroke(width = 3f / zoom)
        )
    }

    if (isTargeted) {
        // Mira roja de objetivo
        drawCircle(
            color = Color(0xFFFF1744).copy(alpha = 0.35f),
            radius = radius + (9f / zoom),
            center = center
        )
        drawCircle(
            color = Color(0xFFFF1744),
            radius = radius + (4f / zoom),
            center = center,
            style = Stroke(width = 2.5f / zoom, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f / zoom, 6f / zoom), 0f))
        )
    }

    if (isSelected) {
        drawCircle(
            color = Color(0xFF00E5FF).copy(alpha = 0.35f),
            radius = radius + (8f / zoom),
            center = center
        )
        drawCircle(
            color = Color(0xFF00E5FF),
            radius = radius + (3f / zoom),
            center = center,
            style = Stroke(width = 2.5f / zoom)
        )
    }

    // Halo azul cian de vuelo si el token está elevado
    if (token.elevationFt > 0) {
        drawCircle(
            color = Color(0x3300E5FF),
            radius = radius + (5f / zoom),
            center = center
        )
        drawCircle(
            color = Color(0xFF00E5FF),
            radius = radius + (2f / zoom),
            center = center,
            style = Stroke(width = 1.5f / zoom, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f / zoom, 4f / zoom)))
        )
    }

    // Cuerpo base del Token
    drawCircle(
        color = Color(0xFF1E1E24),
        radius = radius,
        center = center
    )

    drawCircle(
        color = factionColor,
        radius = radius,
        center = center,
        style = Stroke(width = (4f / zoom).coerceAtLeast(2.5f))
    )

    val initial = token.name.firstOrNull()?.uppercase() ?: "?"
    val tokenFontSize = (radius * 0.9f / zoom).coerceIn(10f, 36f).sp
    val textLayout = textMeasurer.measure(
        text = initial,
        style = TextStyle(
            color = Color.White,
            fontSize = tokenFontSize,
            fontWeight = FontWeight.Bold
        )
    )
    drawText(
        textLayoutResult = textLayout,
        topLeft = Offset(center.x - textLayout.size.width / 2f, center.y - textLayout.size.height / 2f)
    )

    // Overlay de 0 HP / Derribado o Muerto
    if (token.currentHp <= 0) {
        drawCircle(
            color = Color(0x99212121),
            radius = radius,
            center = center
        )
        val crossSize = radius * 0.55f
        drawLine(
            color = Color(0xFFFF1744),
            start = Offset(center.x - crossSize, center.y - crossSize),
            end = Offset(center.x + crossSize, center.y + crossSize),
            strokeWidth = 3f / zoom
        )
        drawLine(
            color = Color(0xFFFF1744),
            start = Offset(center.x + crossSize, center.y - crossSize),
            end = Offset(center.x - crossSize, center.y + crossSize),
            strokeWidth = 3f / zoom
        )
    }

    // Barra de Vida HP
    val barWidth = radius * 1.6f
    val barHeight = (4.5f / zoom).coerceAtLeast(3f)
    val barTopLeft = Offset(center.x - barWidth / 2f, center.y + radius + (3f / zoom))

    drawRect(
        color = Color(0x99000000),
        topLeft = barTopLeft,
        size = Size(barWidth, barHeight)
    )

    val hpFraction = if (token.maxHp > 0) (token.currentHp.toFloat() / token.maxHp).coerceIn(0f, 1f) else 1f
    val hpColor = when {
        hpFraction > 0.5f -> Color(0xFF4CAF50)
        hpFraction > 0.2f -> Color(0xFFFFC107)
        else -> Color(0xFFF44336)
    }

    drawRect(
        color = hpColor,
        topLeft = barTopLeft,
        size = Size(barWidth * hpFraction, barHeight)
    )

    // Badge Flotante de Elevación (esquina superior derecha)
    if (token.elevationFt != 0) {
        val elevText = if (token.elevationFt > 0) "+${token.elevationFt}ft" else "${token.elevationFt}ft"
        val elevFontSize = (9f / zoom).coerceIn(7f, 13f).sp
        val elevLayout = textMeasurer.measure(
            text = elevText,
            style = TextStyle(
                color = Color.White,
                fontSize = elevFontSize,
                fontWeight = FontWeight.ExtraBold
            )
        )
        val badgeW = elevLayout.size.width + (8f / zoom)
        val badgeH = elevLayout.size.height + (4f / zoom)
        val badgeCenter = Offset(center.x + radius * 0.75f, center.y - radius * 0.75f)
        val badgeTopLeft = Offset(badgeCenter.x - badgeW / 2f, badgeCenter.y - badgeH / 2f)

        drawRoundRect(
            color = Color(0xEE006064),
            topLeft = badgeTopLeft,
            size = Size(badgeW, badgeH),
            cornerRadius = CornerRadius(4f / zoom, 4f / zoom)
        )
        drawRoundRect(
            color = Color(0xFF00E5FF),
            topLeft = badgeTopLeft,
            size = Size(badgeW, badgeH),
            cornerRadius = CornerRadius(4f / zoom, 4f / zoom),
            style = Stroke(width = 1.2f / zoom)
        )
        drawText(
            textLayoutResult = elevLayout,
            topLeft = Offset(badgeTopLeft.x + (4f / zoom), badgeTopLeft.y + (2f / zoom))
        )
    }

    // Micro-badges de Condiciones en el Arco Superior
    if (token.conditions.isNotEmpty()) {
        val condList = token.conditions.toList()
        val maxBadgesToShow = 3
        val displayedConditions = condList.take(maxBadgesToShow)
        val hasExtra = condList.size > maxBadgesToShow
        val totalBadges = displayedConditions.size + (if (hasExtra) 1 else 0)

        val badgeRadius = (6f / zoom).coerceIn(4f, 11f)
        val arcRadius = radius + badgeRadius + (2f / zoom)
        val startAngle = -140f
        val sweepAngle = 100f
        val stepAngle = if (totalBadges > 1) sweepAngle / (totalBadges - 1) else 0f

        displayedConditions.forEachIndexed { index, cond ->
            val angleDeg = if (totalBadges == 1) -90f else startAngle + (stepAngle * index)
            val angleRad = Math.toRadians(angleDeg.toDouble())
            val badgeX = (center.x + arcRadius * kotlin.math.cos(angleRad)).toFloat()
            val badgeY = (center.y + arcRadius * kotlin.math.sin(angleRad)).toFloat()

            val (condColor, condChar) = when (cond) {
                Condition.PRONE -> Color(0xFFFF9800) to "⬇"
                Condition.POISONED -> Color(0xFF4CAF50) to "V"
                Condition.BLINDED -> Color(0xFF9E9E9E) to "C"
                Condition.PARALYZED -> Color(0xFFE91E63) to "P"
                Condition.STUNNED -> Color(0xFFFFEB3B) to "A"
                Condition.UNCONSCIOUS -> Color(0xFFD32F2F) to "💀"
                Condition.INVISIBLE -> Color(0xFF00BCD4) to "I"
                Condition.FRIGHTENED -> Color(0xFF9C27B0) to "!"
                Condition.GRAPPLED -> Color(0xFFFF5722) to "G"
                Condition.RESTRAINED -> Color(0xFF795548) to "R"
                Condition.CHARMED -> Color(0xFFE040FB) to "♥"
                Condition.DEAFENED -> Color(0xFF607D8B) to "S"
                Condition.EXHAUSTED -> Color(0xFFFF6D00) to "E"
                Condition.INCAPACITATED -> Color(0xFFB71C1C) to "X"
                Condition.PETRIFIED -> Color(0xFF546E7A) to "石"
            }

            drawCircle(
                color = Color(0xEE1A1A1A),
                radius = badgeRadius,
                center = Offset(badgeX, badgeY)
            )
            drawCircle(
                color = condColor,
                radius = badgeRadius,
                center = Offset(badgeX, badgeY),
                style = Stroke(width = 1.2f / zoom)
            )
            val condTextLayout = textMeasurer.measure(
                text = condChar,
                style = TextStyle(
                    color = condColor,
                    fontSize = (badgeRadius * 1.3f).sp,
                    fontWeight = FontWeight.Bold
                )
            )
            drawText(
                textLayoutResult = condTextLayout,
                topLeft = Offset(badgeX - condTextLayout.size.width / 2f, badgeY - condTextLayout.size.height / 2f)
            )
        }

        if (hasExtra) {
            val angleDeg = startAngle + sweepAngle
            val angleRad = Math.toRadians(angleDeg.toDouble())
            val badgeX = (center.x + arcRadius * kotlin.math.cos(angleRad)).toFloat()
            val badgeY = (center.y + arcRadius * kotlin.math.sin(angleRad)).toFloat()
            val extraText = "+${condList.size - maxBadgesToShow}"

            drawCircle(
                color = Color(0xEE2A2A2A),
                radius = badgeRadius,
                center = Offset(badgeX, badgeY)
            )
            val extraLayout = textMeasurer.measure(
                text = extraText,
                style = TextStyle(
                    color = Color.White,
                    fontSize = (badgeRadius * 1.1f).sp,
                    fontWeight = FontWeight.Bold
                )
            )
            drawText(
                textLayoutResult = extraLayout,
                topLeft = Offset(badgeX - extraLayout.size.width / 2f, badgeY - extraLayout.size.height / 2f)
            )
        }
    }
}

private fun DrawScope.drawMeasureBadge(
    textMeasurer: TextMeasurer,
    text: String,
    center: Offset,
    zoom: Float
) {
    val badgeFontSize = (11f / zoom).coerceIn(8f, 18f).sp
    val textLayout = textMeasurer.measure(
        text = text,
        style = TextStyle(
            color = Color.White,
            fontSize = badgeFontSize,
            fontWeight = FontWeight.SemiBold
        )
    )

    val padX = 8f / zoom
    val padY = 4f / zoom
    val rectWidth = textLayout.size.width + padX * 2
    val rectHeight = textLayout.size.height + padY * 2
    val rectTopLeft = Offset(center.x - rectWidth / 2f, center.y - rectHeight / 2f)

    drawRoundRect(
        color = Color(0xE6212121),
        topLeft = rectTopLeft,
        size = Size(rectWidth, rectHeight),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f / zoom, 6f / zoom)
    )

    drawText(
        textLayoutResult = textLayout,
        topLeft = Offset(rectTopLeft.x + padX, rectTopLeft.y + padY)
    )
}

private fun DrawScope.drawTacticalTerrain(
    map: TacticalMap,
    terrainMap: Map<String, TacticalTerrainType>,
    zoom: Float
) {
    if (terrainMap.isEmpty()) return
    val cellSize = map.cellSizePx
    val strokeWidth = 1.5f / zoom

    terrainMap.forEach { (coordKey, terrainType) ->
        val parts = coordKey.split(",")
        if (parts.size == 2) {
            val col = parts[0].toIntOrNull() ?: return@forEach
            val row = parts[1].toIntOrNull() ?: return@forEach
            val pxX = (col * cellSize) + map.gridOffsetX
            val pxY = (row * cellSize) + map.gridOffsetY

            when (terrainType) {
                TacticalTerrainType.DIFFICULT -> {
                    drawRect(
                        color = Color(0x55FFA000),
                        topLeft = Offset(pxX, pxY),
                        size = Size(cellSize, cellSize)
                    )
                    drawLine(
                        color = Color(0x99FFA000),
                        start = Offset(pxX, pxY + cellSize),
                        end = Offset(pxX + cellSize, pxY),
                        strokeWidth = strokeWidth
                    )
                }
                TacticalTerrainType.WALL -> {
                    drawRect(
                        color = Color(0xD0263238),
                        topLeft = Offset(pxX, pxY),
                        size = Size(cellSize, cellSize)
                    )
                    drawRect(
                        color = Color(0xFF37474F),
                        topLeft = Offset(pxX, pxY),
                        size = Size(cellSize, cellSize),
                        style = Stroke(width = strokeWidth * 2f)
                    )
                }
                TacticalTerrainType.HAZARD -> {
                    drawRect(
                        color = Color(0x55E53935),
                        topLeft = Offset(pxX, pxY),
                        size = Size(cellSize, cellSize)
                    )
                    drawRect(
                        color = Color(0xAAE53935),
                        topLeft = Offset(pxX, pxY),
                        size = Size(cellSize, cellSize),
                        style = Stroke(width = strokeWidth)
                    )
                }
                TacticalTerrainType.NORMAL -> Unit
            }
        }
    }
}

private fun DrawScope.drawTacticalFog(
    map: TacticalMap,
    revealedCells: Set<String>,
    isFogDmView: Boolean,
    viewSize: Size,
    zoom: Float,
    panOffset: Offset
) {
    val cellSize = map.cellSizePx
    val startCol = kotlin.math.floor((-panOffset.x / zoom - map.gridOffsetX) / cellSize).toInt() - 1
    val endCol = kotlin.math.ceil((-panOffset.x + viewSize.width / zoom - map.gridOffsetX) / cellSize).toInt() + 1
    val startRow = kotlin.math.floor((-panOffset.y / zoom - map.gridOffsetY) / cellSize).toInt() - 1
    val endRow = kotlin.math.ceil((-panOffset.y + viewSize.height / zoom - map.gridOffsetY) / cellSize).toInt() + 1

    val fogColor = if (isFogDmView) Color(0xB3101014) else Color(0xFF000000)

    for (c in startCol..endCol) {
        for (r in startRow..endRow) {
            val key = "$c,$r"
            if (!revealedCells.contains(key)) {
                val pxX = (c * cellSize) + map.gridOffsetX
                val pxY = (r * cellSize) + map.gridOffsetY
                drawRect(
                    color = fogColor,
                    topLeft = Offset(pxX, pxY),
                    size = Size(cellSize, cellSize)
                )
            }
        }
    }
}

private fun DrawScope.drawRoutePath(
    map: TacticalMap,
    waypoints: List<Pair<Float, Float>>,
    currentDistanceFt: Int,
    selectedToken: TacticalToken?,
    zoom: Float,
    textMeasurer: TextMeasurer
) {
    if (waypoints.size < 2) return
    val cellSize = map.cellSizePx

    for (i in 0 until waypoints.size - 1) {
        val p1 = waypoints[i]
        val p2 = waypoints[i + 1]
        val startPx = Offset((p1.first * cellSize) + map.gridOffsetX, (p1.second * cellSize) + map.gridOffsetY)
        val endPx = Offset((p2.first * cellSize) + map.gridOffsetX, (p2.second * cellSize) + map.gridOffsetY)

        drawLine(
            color = Color(0xFF7C4DFF),
            start = startPx,
            end = endPx,
            strokeWidth = 3.5f / zoom,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f / zoom, 6f / zoom))
        )

        drawCircle(
            color = Color(0xFF7C4DFF),
            radius = 6f / zoom,
            center = startPx
        )
    }

    val last = waypoints.last()
    val lastPx = Offset((last.first * cellSize) + map.gridOffsetX, (last.second * cellSize) + map.gridOffsetY)
    drawCircle(
        color = Color(0xFF00E5FF),
        radius = 8f / zoom,
        center = lastPx
    )

    val speed = selectedToken?.speedFt ?: 30
    val isOver = currentDistanceFt > speed
    val badgeText = if (isOver) "$currentDistanceFt ft (> $speed ft)" else "$currentDistanceFt ft"
    drawMeasureBadge(
        textMeasurer = textMeasurer,
        text = badgeText,
        center = Offset(lastPx.x, lastPx.y - 18f / zoom),
        zoom = zoom
    )
}
