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
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import kotlin.math.max
import kotlin.math.min

enum class HandleType {
    TOP_LEFT, TOP_CENTER, TOP_RIGHT,
    CENTER_LEFT, CENTER_RIGHT,
    BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT,
    MOVE
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
    // Estado del rectángulo de recorte (en coordenadas normalizadas 0.0 - 1.0)
    var cropRect by remember { mutableStateOf(Rect(0.1f, 0.1f, 0.9f, 0.5f)) }
    
    // Tamaño del contenedor de la imagen para convertir coordenadas
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    
    // Feedback táctil: posición actual del dedo para la lupa
    var magnifierPosition by remember { mutableStateOf(Offset.Unspecified) }
    
    // Draft del bitmap recortado (para previsualización si quisiéramos, pero aquí lo procesamos al final)
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
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
        )
        Text(
            text = "Usa los tiradores para ajustar el área de reconocimiento",
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
                        // Posicionar la lupa un poco arriba del dedo
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
                    
                    detectDragGestures(
                        onDragStart = { offset -> magnifierPosition = offset },
                        onDragEnd = { magnifierPosition = Offset.Unspecified },
                        onDragCancel = { magnifierPosition = Offset.Unspecified },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            magnifierPosition = change.position
                            
                            val width = containerSize.width.toFloat()
                            val height = containerSize.height.toFloat()
                            
                            // Determinar qué estamos moviendo (aquí simplificado: el que esté más cerca oMOVE)
                            // Para esta implementación robusta, regeneramos el rect basado en el drag
                            val currentRect = cropRect
                            val handle = getHandleAt(change.position, currentRect, width, height)
                            
                            cropRect = updateRect(currentRect, handle, dragAmount, width, height)
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            // Imagen de fondo
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
            
            // Canvas para el Scrim (oscurecido fuera) y el Recuadro
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val rect = Rect(
                    cropRect.left * w,
                    cropRect.top * h,
                    cropRect.right * w,
                    cropRect.bottom * h
                )

                // 1. Scrim: Oscurecer lo que está fuera del rect
                val path = Path().apply {
                    addRect(Rect(0f, 0f, w, h))
                    addRect(rect)
                    fillType = PathFillType.EvenOdd
                }
                drawPath(path, Color.Black.copy(alpha = 0.45f))

                // 2. Bordes del recuadro
                drawRect(
                    color = colorScheme.primary,
                    topLeft = rect.topLeft,
                    size = rect.size,
                    style = Stroke(width = 2.dp.toPx())
                )

                // 3. Guías internas (Regla de tercios sutil)
                val thirdW = rect.width / 3
                val thirdH = rect.height / 3
                for (i in 1..2) {
                    drawLine(
                        color = colorScheme.primary.copy(alpha = 0.3f),
                        start = Offset(rect.left + thirdW * i, rect.top),
                        end = Offset(rect.left + thirdW * i, rect.bottom),
                        strokeWidth = 1.dp.toPx()
                    )
                    drawLine(
                        color = colorScheme.primary.copy(alpha = 0.3f),
                        start = Offset(rect.left, rect.top + thirdH * i),
                        end = Offset(rect.right, rect.top + thirdH * i),
                        strokeWidth = 1.dp.toPx()
                    )
                }
                
                // 4. Manejadores (Visual)
                val handleRadius = 6.dp.toPx()
                val handles = listOf(
                    rect.topLeft, rect.topCenter, rect.topRight,
                    rect.centerLeft, rect.centerRight,
                    rect.bottomLeft, rect.bottomCenter, rect.bottomRight
                )
                handles.forEach { center ->
                    drawCircle(color = Color.White, radius = handleRadius, center = center)
                    drawCircle(color = colorScheme.primary, radius = handleRadius, center = center, style = Stroke(2.dp.toPx()))
                }
            }
        }
        
        Spacer(Modifier.height(24.dp))
        
        // ── Acciones y Controles Avanzados ──────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Número de tablas", style = MaterialTheme.typography.labelLarge)
                        Text("Detectar automáticamente vs forzar 1 tabla", style = MaterialTheme.typography.bodySmall)
                    }
                    SingleChoiceSegmentedButtonRow {
                        SegmentedButton(
                            selected = expectedTableCount == 0,
                            onClick = { onSetExpectedTableCount(0) },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                        ) { Text("Auto") }
                        SegmentedButton(
                            selected = expectedTableCount == 1,
                            onClick = { onSetExpectedTableCount(1) },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                        ) { Text("1 Tabla") }
                    }
                }
                
                Divider(Modifier.padding(vertical = 8.dp), alpha = 0.2f)
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Modo Unión (Stitching)", style = MaterialTheme.typography.labelLarge)
                        Text("Añadir este recorte a la tabla actual", style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(
                        checked = isStitching,
                        onCheckedChange = onToggleStitching
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                val cropped = performCrop(bitmap, cropRect)
                onCropConfirmed(cropped)
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            contentPadding = PaddingValues(horizontal = 24.dp)
        ) {
            Icon(Icons.Default.Check, null)
            Spacer(Modifier.width(12.dp))
            Text("Analizar Selección", style = MaterialTheme.typography.titleMedium)
        }
        
    }
}

// ── Lógica de Cálculo ────────────────────────────────────────────────────────

private fun getHandleAt(pos: Offset, rect: Rect, width: Float, height: Float): HandleType {
    val r = Rect(rect.left * width, rect.top * height, rect.right * width, rect.bottom * height)
    val threshold = 40f // Sensibilidad del toque
    
    return when {
        (pos - r.topLeft).getDistance() < threshold -> HandleType.TOP_LEFT
        (pos - r.topRight).getDistance() < threshold -> HandleType.TOP_RIGHT
        (pos - r.bottomLeft).getDistance() < threshold -> HandleType.BOTTOM_LEFT
        (pos - r.bottomRight).getDistance() < threshold -> HandleType.BOTTOM_RIGHT
        (pos - r.topCenter).getDistance() < threshold -> HandleType.TOP_CENTER
        (pos - r.bottomCenter).getDistance() < threshold -> HandleType.BOTTOM_CENTER
        (pos - r.centerLeft).getDistance() < threshold -> HandleType.CENTER_LEFT
        (pos - r.centerRight).getDistance() < threshold -> HandleType.CENTER_RIGHT
        r.contains(pos) -> HandleType.MOVE
        else -> HandleType.MOVE // Por defecto mover si no estamos en un handle
    }
}

private fun updateRect(rect: Rect, handle: HandleType, delta: Offset, width: Float, height: Float): Rect {
    val dx = delta.x / width
    val dy = delta.y / height
    val minSize = 0.05f
    
    return when (handle) {
        HandleType.TOP_LEFT -> rect.copy(
            left = min(rect.right - minSize, max(0f, rect.left + dx)),
            top = min(rect.bottom - minSize, max(0f, rect.top + dy))
        )
        HandleType.TOP_RIGHT -> rect.copy(
            right = max(rect.left + minSize, min(1f, rect.right + dx)),
            top = min(rect.bottom - minSize, max(0f, rect.top + dy))
        )
        HandleType.BOTTOM_LEFT -> rect.copy(
            left = min(rect.right - minSize, max(0f, rect.left + dx)),
            bottom = max(rect.top + minSize, min(1f, rect.bottom + dy))
        )
        HandleType.BOTTOM_RIGHT -> rect.copy(
            right = max(rect.left + minSize, min(1f, rect.right + dx)),
            bottom = max(rect.top + minSize, min(1f, rect.bottom + dy))
        )
        HandleType.TOP_CENTER -> rect.copy(top = min(rect.bottom - minSize, max(0f, rect.top + dy)))
        HandleType.BOTTOM_CENTER -> rect.copy(bottom = max(rect.top + minSize, min(1f, rect.bottom + dy)))
        HandleType.CENTER_LEFT -> rect.copy(left = min(rect.right - minSize, max(0f, rect.left + dx)))
        HandleType.CENTER_RIGHT -> rect.updateRight(max(rect.left + minSize, min(1f, rect.right + dx)))
        HandleType.MOVE -> {
            val newLeft = max(0f, min(1f - rect.width, rect.left + dx))
            val newTop = max(0f, min(1f - rect.height, rect.top + dy))
            Rect(newLeft, newTop, newLeft + rect.width, newTop + rect.height)
        }
    }
}

// Helpers para Rect que no existen en Compose por defecto
private fun Rect.updateRight(newRight: Float) = Rect(left, top, newRight, bottom)

private fun performCrop(bitmap: Bitmap, normalizedRect: Rect): Bitmap {
    val left = (normalizedRect.left * bitmap.width).toInt().coerceIn(0, bitmap.width - 1)
    val top = (normalizedRect.top * bitmap.height).toInt().coerceIn(0, bitmap.height - 1)
    val width = (normalizedRect.width * bitmap.width).toInt().coerceIn(1, bitmap.width - left)
    val height = (normalizedRect.height * bitmap.height).toInt().coerceIn(1, bitmap.height - top)
    
    return Bitmap.createBitmap(bitmap, left, top, width, height)
}
