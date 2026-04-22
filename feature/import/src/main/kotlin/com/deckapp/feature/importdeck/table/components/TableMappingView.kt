package com.deckapp.feature.importdeck.table.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import kotlin.math.abs

@Composable
fun TableMappingView(
    bitmap: Bitmap,
    anchors: List<Float>,
    onAddAnchor: (Float) -> Unit,
    onRemoveAnchor: (Float) -> Unit,
    onConfirm: () -> Unit,
    isVisionProcessing: Boolean = false,
    onVisionRecognize: (() -> Unit)? = null
) {
    // Estado de transformación (Zoom y Pan)
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val state = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 4f)
        offset += offsetChange
    }

    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    val colorScheme = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Ajustar Columnas",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Toca para definir dónde terminan las columnas.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant
                )
            }
            if (scale > 1f) {
                IconButton(onClick = { scale = 1f; offset = Offset.Zero }) {
                    Icon(Icons.Default.ZoomIn, "Reset Zoom", tint = colorScheme.primary)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black.copy(alpha = 0.05f))
                .onGloballyPositioned { containerSize = it.size }
                .transformable(state = state)
                .pointerInput(containerSize) {
                    detectTapGestures { tapOffset ->
                        if (containerSize == IntSize.Zero) return@detectTapGestures
                        
                        val bitmapSize = Size(bitmap.width.toFloat(), bitmap.height.toFloat())
                        val imageBounds = calculateImageBounds(containerSize.toSize(), bitmapSize)
                        
                        // 1. Revertir Zoom y Pan en el tapOffset
                        val centerX = containerSize.width / 2f
                        val centerY = containerSize.height / 2f
                        val relativeX = (tapOffset.x - centerX - offset.x) / scale + centerX
                        val relativeY = (tapOffset.y - centerY - offset.y) / scale + centerY
                        
                        // 2. Comprobar si está dentro de imageBounds
                        if (imageBounds.contains(Offset(relativeX, relativeY))) { 
                            val xNormalized = (relativeX - imageBounds.left) / imageBounds.width
                            
                            val threshold = 0.04f / scale
                            val existing = anchors.find { abs(it - xNormalized) < threshold }
                            if (existing != null) {
                                onRemoveAnchor(existing)
                            } else if (xNormalized in 0f..1f) {
                                onAddAnchor(xNormalized)
                            }
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y
                    )
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
                    
                    val w = imageBounds.width
                    val h = imageBounds.height
                    val left = imageBounds.left
                    val top = imageBounds.top
                    
                    val sortedAnchors = (listOf(0f) + anchors + listOf(1f)).sorted()
                    
                    for (i in 0 until sortedAnchors.size - 1) {
                        val start = left + sortedAnchors[i] * w
                        val end = left + sortedAnchors[i+1] * w
                        
                        if (i % 2 != 0) {
                            drawRect(
                                color = colorScheme.primary.copy(alpha = 0.08f),
                                topLeft = Offset(start, top),
                                size = Size(end - start, h)
                            )
                        }
                    }

                    anchors.forEach { x ->
                        val xPos = left + x * w
                        drawLine(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.Transparent, colorScheme.primary, Color.Transparent),
                                startY = top,
                                endY = top + h
                            ),
                            start = Offset(xPos, top),
                            end = Offset(xPos, top + h),
                            strokeWidth = (2.dp / scale).toPx()
                        )
                        drawCircle(
                            color = colorScheme.primary,
                            radius = (4.dp / scale).toPx(),
                            center = Offset(xPos, top + 10.dp.toPx() / scale)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        
        Surface(
            tonalElevation = 2.dp,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.AutoFixHigh, null, tint = colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Text(
                    "Las líneas rojas separan las columnas. Si el texto está muy junto, haz zoom para separar con precisión.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        if (onVisionRecognize != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(18.dp),
                    enabled = !isVisionProcessing
                ) {
                    Icon(Icons.Default.Check, null)
                    Spacer(Modifier.width(8.dp))
                    Text("OCR")
                }
                Button(
                    onClick = onVisionRecognize,
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(18.dp),
                    enabled = !isVisionProcessing,
                    colors = ButtonDefaults.buttonColors(containerColor = colorScheme.tertiary)
                ) {
                    if (isVisionProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = colorScheme.onTertiary
                        )
                    } else {
                        Icon(Icons.Default.RemoveRedEye, null)
                        Spacer(Modifier.width(8.dp))
                        Text("IA Vision")
                    }
                }
            }
        } else {
            Button(
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary)
            ) {
                Icon(Icons.Default.Check, null)
                Spacer(Modifier.width(12.dp))
                Text("Procesar Columnas", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

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
