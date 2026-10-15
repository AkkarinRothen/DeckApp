package com.deckapp.feature.draw.components.blocks

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ClockSegmentsDisplay(
    progress: Int,
    segments: Int,
    sizeDp: Dp,
    strokeWidthDp: Dp,
    accentColorHex: String?,
    isCompleted: Boolean,
    modifier: Modifier = Modifier
) {
    val parsedColor = remember(accentColorHex) {
        if (!accentColorHex.isNullOrBlank()) {
            try {
                Color(android.graphics.Color.parseColor(accentColorHex))
            } catch (e: Exception) {
                null
            }
        } else null
    }

    val primaryColor = if (isCompleted) MaterialTheme.colorScheme.error else (parsedColor ?: MaterialTheme.colorScheme.primary)
    val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val dividerColor = MaterialTheme.colorScheme.surface

    Canvas(modifier = modifier.size(sizeDp)) {
        val strokeWidth = strokeWidthDp.toPx()
        val diameter = size.minDimension - strokeWidth
        val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
        val arcSize = Size(diameter, diameter)

        // Fondo del círculo
        drawArc(
            color = trackColor,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth)
        )

        // Arcos de progreso
        val totalSegments = segments.coerceAtLeast(1)
        val segmentAngle = 360f / totalSegments
        val filledCount = progress.coerceIn(0, totalSegments)

        for (i in 0 until filledCount) {
            val startAngle = -90f + (i * segmentAngle) + 2f
            val sweep = (segmentAngle - 4f).coerceAtLeast(1f)
            drawArc(
                color = primaryColor,
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        // Líneas divisorias entre segmentos
        val radius = diameter / 2
        val centerOffset = Offset(size.width / 2, size.height / 2)
        for (i in 0 until totalSegments) {
            val angleRad = Math.toRadians((-90f + i * segmentAngle).toDouble())
            val innerR = radius - strokeWidth / 2
            val outerR = radius + strokeWidth / 2
            val start = Offset(
                (centerOffset.x + innerR * cos(angleRad)).toFloat(),
                (centerOffset.y + innerR * sin(angleRad)).toFloat()
            )
            val end = Offset(
                (centerOffset.x + outerR * cos(angleRad)).toFloat(),
                (centerOffset.y + outerR * sin(angleRad)).toFloat()
            )
            drawLine(
                color = dividerColor,
                start = start,
                end = end,
                strokeWidth = 2.dp.toPx()
            )
        }
    }
}
