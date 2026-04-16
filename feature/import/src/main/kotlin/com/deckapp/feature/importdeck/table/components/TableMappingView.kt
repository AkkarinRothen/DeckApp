package com.deckapp.feature.importdeck.table.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

@Composable
fun TableMappingView(
    bitmap: Bitmap,
    anchors: List<Float>,
    onAddAnchor: (Float) -> Unit,
    onRemoveAnchor: (Float) -> Unit,
    onConfirm: () -> Unit
) {
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    val colorScheme = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "Mapeo de Columnas",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
            Text(
                "Toca la imagen para marcar dónde terminan las columnas",
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black.copy(alpha = 0.05f))
                .onGloballyPositioned { containerSize = it.size }
                .pointerInput(containerSize) {
                    detectTapGestures { offset ->
                        val xNormalized = offset.x / containerSize.width
                        // Si tocamos cerca de una existente, la borramos, si no, añadimos
                        val threshold = 0.03f
                        val existing = anchors.find { kotlin.math.abs(it - xNormalized) < threshold }
                        if (existing != null) {
                            onRemoveAnchor(existing)
                        } else {
                            onAddAnchor(xNormalized)
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // Fondo recortado
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )

            // Dibujar las anclas (columnas)
            Canvas(modifier = Modifier.fillMaxSize()) {
                anchors.forEach { x ->
                    val xPos = x * size.width
                    drawLine(
                        color = Color.Red,
                        start = Offset(xPos, 0f),
                        end = Offset(xPos, size.height),
                        strokeWidth = 3.dp.toPx()
                    )
                    // Pequeño indicador en la parte superior
                    drawCircle(
                        color = Color.Red,
                        radius = 6.dp.toPx(),
                        center = Offset(xPos, 12.dp.toPx())
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = colorScheme.secondaryContainer.copy(alpha = 0.4f))
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.AutoFixHigh, null, tint = colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Text(
                    "Las líneas indican dónde se separará el texto. Puedes tocar la imagen para corregirlas.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSecondaryContainer
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = onConfirm,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.Check, null)
            Spacer(Modifier.width(12.dp))
            Text("Generar Tabla de Datos", style = MaterialTheme.typography.titleMedium)
        }
    }
}
