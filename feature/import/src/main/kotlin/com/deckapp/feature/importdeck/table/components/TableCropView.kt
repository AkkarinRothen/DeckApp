package com.deckapp.feature.importdeck.table.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.magnifier
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import kotlin.math.max
import kotlin.math.min

import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path as AndroidPath

enum class HandleType {
    TOP_LEFT, TOP_RIGHT, BOTTOM_RIGHT, BOTTOM_LEFT,
    MOVE, NONE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TableCropView(
    bitmap: Bitmap,
    expectedTableCount: Int,
    isStitching: Boolean,
    onSetExpectedTableCount: (Int) -> Unit,
    onToggleStitching: (Boolean) -> Unit,
    onCropConfirmed: (Bitmap) -> Unit
) {
    // Estado del cuadrilátero (en coordenadas normalizadas 0.0 - 1.0 relativo a la imagen)
    var quadPoints by remember { 
        mutableStateOf(
            listOf(
                Offset(0.1f, 0.1f), // TL
                Offset(0.9f, 0.1f), // TR
                Offset(0.9f, 0.5f), // BR
                Offset(0.1f, 0.5f)  // BL
            )
        )
    }
    
    // Tamaño del contenedor de la imagen para convertir coordenadas
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    
    // Feedback táctil: posición actual del dedo para la lupa
    var magnifierPosition by remember { mutableStateOf(Offset.Unspecified) }
    
    var activeHandle by remember { mutableStateOf(HandleType.NONE) }
    
    val colorScheme = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (isStitching) "Recortar siguiente sección" else "Enmarca la tabla claramente",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Ajusta las esquinas para corregir la perspectiva",
            style = MaterialTheme.typography.bodySmall,
            color = colorScheme.onSurfaceVariant
        )
        
        Spacer(Modifier.height(16.dp))
        
        // ── Contenedor Interactivo ──────────────────────────────────
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black.copy(alpha = 0.05f))
                .onGloballyPositioned { containerSize = it.size }
                .magnifier(
                    sourceCenter = { magnifierPosition },
                    magnifierCenter = {
                        if (magnifierPosition.isSpecified) {
                            magnifierPosition.copy(y = magnifierPosition.y - 120f)
                        } else Offset.Unspecified
                    },
                    zoom = 2f,
                    cornerRadius = 48.dp,
                    size = androidx.compose.ui.unit.DpSize(100.dp, 100.dp)
                )
                .pointerInput(containerSize) {
                    if (containerSize == IntSize.Zero) return@pointerInput
                    
                    val bitmapSize = Size(bitmap.width.toFloat(), bitmap.height.toFloat())
                    val imageBounds = calculateImageBounds(containerSize.toSize(), bitmapSize)
                    
                    detectDragGestures(
                        onDragStart = { offset -> 
                            magnifierPosition = offset
                            activeHandle = getHandleAtQuad(offset, quadPoints, imageBounds)
                        },
                        onDragEnd = { 
                            magnifierPosition = Offset.Unspecified
                            activeHandle = HandleType.NONE
                        },
                        onDragCancel = { 
                            magnifierPosition = Offset.Unspecified
                            activeHandle = HandleType.NONE
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            magnifierPosition = change.position
                            quadPoints = updateQuad(quadPoints, activeHandle, dragAmount, imageBounds)
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
            
            Canvas(modifier = Modifier.fillMaxSize()) {
                val bitmapSize = Size(bitmap.width.toFloat(), bitmap.height.toFloat())
                val imageBounds = calculateImageBounds(size, bitmapSize)
                
                // Convertir puntos normalizados a pixeles de pantalla
                val points = quadPoints.map { 
                    Offset(
                        imageBounds.left + it.x * imageBounds.width,
                        imageBounds.top + it.y * imageBounds.height
                    )
                }

                // 1. Scrim (oscurecemos fuera del cuadrilátero)
                val quadPath = Path().apply {
                    moveTo(points[0].x, points[0].y)
                    lineTo(points[1].x, points[1].y)
                    lineTo(points[2].x, points[2].y)
                    lineTo(points[3].x, points[3].y)
                    close()
                }
                
                val scrimPath = Path().apply {
                    addRect(imageBounds)
                    addPath(quadPath)
                    fillType = PathFillType.EvenOdd
                }
                drawPath(scrimPath, Color.Black.copy(alpha = 0.6f))
                
                // 2. Bordes del cuadrilátero
                drawPath(
                    path = quadPath,
                    color = colorScheme.primary,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )

                // 3. Manejadores
                val handleRadius = 12.dp.toPx()
                points.forEachIndexed { index, center ->
                    val isSelected = activeHandle.ordinal == index
                    drawCircle(
                        color = Color.White, 
                        radius = if (isSelected) handleRadius * 1.2f else handleRadius, 
                        center = center
                    )
                    drawCircle(
                        color = colorScheme.primary, 
                        radius = if (isSelected) handleRadius * 1.2f else handleRadius, 
                        center = center, 
                        style = Stroke(2.dp.toPx())
                    )
                }
            }
        }
        
        Spacer(Modifier.height(24.dp))
        
        // ── Acciones y Controles Premium ──────────────────────────
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = colorScheme.surfaceVariant.copy(alpha = 0.3f),
            border = androidx.compose.foundation.BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Configuración de Celdas", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text("Detecta tablas automáticamente o fuerza un número.", style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurfaceVariant)
                    }
                    
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        FilterChip(
                            selected = expectedTableCount > 0,
                            onClick = { expanded = true },
                            label = { Text(if (expectedTableCount == 0) "Auto" else "$expectedTableCount Tablas") },
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) }
                        )
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            DropdownMenuItem(
                                text = { Text("Automático") },
                                onClick = { onSetExpectedTableCount(0); expanded = false }
                            )
                            (1..5).forEach { i ->
                                DropdownMenuItem(
                                    text = { Text("Forzar $i tabla${if (i > 1) "s" else ""}") },
                                    onClick = { onSetExpectedTableCount(i); expanded = false }
                                )
                            }
                        }
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.3f))
                Spacer(Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Modo Unión (Stitching)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text("Útil para unir tablas cortadas en varias páginas.", style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = isStitching,
                        onCheckedChange = onToggleStitching,
                        thumbContent = if (isStitching) {
                            { Icon(Icons.Default.Check, null, Modifier.size(SwitchDefaults.IconSize)) }
                        } else null
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = {
                val cropped = performPerspectiveCrop(bitmap, quadPoints)
                onCropConfirmed(cropped)
            },
            modifier = Modifier.fillMaxWidth().height(60.dp),
            shape = RoundedCornerShape(18.dp),
            contentPadding = PaddingValues(horizontal = 24.dp)
        ) {
            Icon(Icons.Default.Check, null)
            Spacer(Modifier.width(12.dp))
            Text("Analizar Selección", style = MaterialTheme.typography.titleMedium)
        }
        
    }
}

// ── Lógica de Cálculo ────────────────────────────────────────────────────────

private fun calculateImageBounds(containerSize: Size, bitmapSize: Size): Rect {
    val containerRatio = containerSize.width / containerSize.height
    val bitmapRatio = bitmapSize.width / bitmapSize.height
    
    val (drawWidth, drawHeight) = if (bitmapRatio > containerRatio) {
        containerSize.width to (containerSize.width / bitmapRatio)
    } else {
        (containerSize.height * bitmapRatio) to containerSize.height
    }
    
    val left = (containerSize.width - drawWidth) / 2
    val top = (containerSize.height - drawHeight) / 2
    
    return Rect(left, top, left + drawWidth, top + drawHeight)
}

private fun getHandleAtQuad(pos: Offset, points: List<Offset>, imageBounds: Rect): HandleType {
    val threshold = 40f
    
    points.forEachIndexed { index, p ->
        val screenPos = Offset(
            imageBounds.left + p.x * imageBounds.width,
            imageBounds.top + p.y * imageBounds.height
        )
        if ((pos - screenPos).getDistance() < threshold) {
            return HandleType.entries[index]
        }
    }
    
    // Si está dentro del polígono, permitir mover
    // (Simplificado: usar el bounding box para el MOVE)
    val minX = points.minOf { it.x } * imageBounds.width + imageBounds.left
    val maxX = points.maxOf { it.x } * imageBounds.width + imageBounds.left
    val minY = points.minOf { it.y } * imageBounds.height + imageBounds.top
    val maxY = points.maxOf { it.y } * imageBounds.height + imageBounds.top
    
    if (pos.x in minX..maxX && pos.y in minY..maxY) return HandleType.MOVE
    
    return HandleType.NONE
}

private fun updateQuad(points: List<Offset>, handle: HandleType, delta: Offset, imageBounds: Rect): List<Offset> {
    val dx = delta.x / imageBounds.width
    val dy = delta.y / imageBounds.height
    
    val newList = points.toMutableList()
    
    when (handle) {
        HandleType.TOP_LEFT -> newList[0] = Offset((newList[0].x + dx).coerceIn(0f, 1f), (newList[0].y + dy).coerceIn(0f, 1f))
        HandleType.TOP_RIGHT -> newList[1] = Offset((newList[1].x + dx).coerceIn(0f, 1f), (newList[1].y + dy).coerceIn(0f, 1f))
        HandleType.BOTTOM_RIGHT -> newList[2] = Offset((newList[2].x + dx).coerceIn(0f, 1f), (newList[2].y + dy).coerceIn(0f, 1f))
        HandleType.BOTTOM_LEFT -> newList[3] = Offset((newList[3].x + dx).coerceIn(0f, 1f), (newList[3].y + dy).coerceIn(0f, 1f))
        HandleType.MOVE -> {
            return points.map { 
                Offset((it.x + dx).coerceIn(0f, 1f), (it.y + dy).coerceIn(0f, 1f))
            }
        }
        HandleType.NONE -> {}
    }
    return newList
}

private fun performPerspectiveCrop(bitmap: Bitmap, points: List<Offset>): Bitmap {
    val w = bitmap.width.toFloat()
    val h = bitmap.height.toFloat()
    
    // Coordenadas de origen (en el bitmap real)
    val src = floatArrayOf(
        points[0].x * w, points[0].y * h, // TL
        points[1].x * w, points[1].y * h, // TR
        points[2].x * w, points[2].y * h, // BR
        points[3].x * w, points[3].y * h  // BL
    )
    
    // Calcular dimensiones del destino
    val widthTop = Offset(src[0] - src[2], src[1] - src[3]).getDistance()
    val widthBottom = Offset(src[4] - src[6], src[5] - src[7]).getDistance()
    val targetWidth = max(widthTop, widthBottom).toInt().coerceAtLeast(100)
    
    val heightLeft = Offset(src[0] - src[6], src[1] - src[7]).getDistance()
    val heightRight = Offset(src[2] - src[4], src[3] - src[5]).getDistance()
    val targetHeight = max(heightLeft, heightRight).toInt().coerceAtLeast(100)
    
    // Coordenadas de destino (rectángulo perfecto)
    val dst = floatArrayOf(
        0f, 0f,                         // TL
        targetWidth.toFloat(), 0f,      // TR
        targetWidth.toFloat(), targetHeight.toFloat(), // BR
        0f, targetHeight.toFloat()      // BL
    )
    
    val matrix = Matrix()
    matrix.setPolyToPoly(src, 0, dst, 0, 4)
    
    val resultBitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(resultBitmap)
    val paint = Paint()
    paint.isAntiAlias = true
    paint.isFilterBitmap = true
    
    canvas.drawBitmap(bitmap, matrix, paint)
    
    return resultBitmap
}
