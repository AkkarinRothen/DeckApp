package com.deckapp.feature.importdeck.table.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.deckapp.core.model.OcrBlock
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType

/**
 * Componente que muestra el bitmap recortado con los bloques OCR y las columnas detectadas
 * superpuestos. Ayuda al usuario a entender por qué falló el reconocimiento en ciertas áreas.
 */
@Composable
fun TableOcrOverlay(
    bitmap: Bitmap,
    blocks: List<OcrBlock>,
    anchors: List<Float>,
    lowConfidenceIndices: Set<Int>,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(bitmap.width.toFloat() / bitmap.height.toFloat())
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black)
    ) {
        // Imagen original
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Tabla original",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasW = size.width
            val canvasH = size.height

            // 1. Dibujar Bloques OCR
            blocks.forEach { block ->
                val rect = block.boundingBox
                val isLowConf = block.confidence < 0.6f
                
                // ML Kit da coordenadas en píxeles de la imagen original
                // Necesitamos normalizarlas al tamaño del canvas
                val left = (rect.left / bitmap.width) * canvasW
                val top = (rect.top / bitmap.height) * canvasH
                val right = (rect.right / bitmap.width) * canvasW
                val bottom = (rect.bottom / bitmap.height) * canvasH

                val color = if (isLowConf) Color.Red else Color.Green.copy(alpha = 0.6f)
                
                drawRect(
                    color = color,
                    topLeft = Offset(left, top),
                    size = androidx.compose.ui.geometry.Size(right - left, bottom - top),
                    style = Stroke(width = 1.dp.toPx())
                )
                
                if (isLowConf) {
                    drawRect(
                        color = color.copy(alpha = 0.1f),
                        topLeft = Offset(left, top),
                        size = androidx.compose.ui.geometry.Size(right - left, bottom - top)
                    )
                }
            }

            // 2. Dibujar Columnas (Anchors) - Estos vienen normalizados 0.0 a 1.0
            anchors.forEach { x ->
                val posX = x * canvasW
                drawLine(
                    color = colorScheme.primary,
                    start = Offset(posX, 0f),
                    end = Offset(posX, canvasH),
                    strokeWidth = 2.dp.toPx(),
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                )
            }
        }
    }
}
