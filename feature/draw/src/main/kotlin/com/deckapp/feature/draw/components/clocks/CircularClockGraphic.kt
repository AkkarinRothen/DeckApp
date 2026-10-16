package com.deckapp.feature.draw.components.clocks

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

/**
 * Gráfico interactivo y expresivo para un reloj de tensión o progreso (Tension / Progress Clock).
 * Dibuja un arco dividido en sectores discretos con animación suave de avance y centro informativo.
 */
@Composable
fun CircularClockGraphic(
    progress: Int,
    segments: Int,
    sizeDp: Dp = 64.dp,
    strokeWidthDp: Dp = 8.dp,
    accentColorHex: String? = null,
    isCompleted: Boolean = false,
    showCenterText: Boolean = true,
    modifier: Modifier = Modifier
) {
    val totalSegments = segments.coerceAtLeast(1)
    val clampedProgress = progress.coerceIn(0, totalSegments)
    val completed = isCompleted || clampedProgress >= totalSegments

    val baseAccent = remember(accentColorHex) {
        if (!accentColorHex.isNullOrBlank()) {
            runCatching { Color(android.graphics.Color.parseColor(accentColorHex)) }.getOrNull()
        } else null
    } ?: Color(0xFFE53935)

    val targetColor = if (completed) MaterialTheme.colorScheme.error else baseAccent
    val animatedColor by animateColorAsState(targetValue = targetColor, animationSpec = tween(250), label = "clockColor")

    val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    val dividerColor = MaterialTheme.colorScheme.surface

    Box(
        modifier = modifier.size(sizeDp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(sizeDp)) {
            val strokeWidth = strokeWidthDp.toPx()
            val diameter = size.minDimension - strokeWidth
            val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
            val arcSize = Size(diameter, diameter)

            // Pista de fondo
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth)
            )

            // Arcos de progreso para cada sector
            val segmentAngle = 360f / totalSegments
            val gapAngle = if (totalSegments > 8) 2f else 3f

            for (i in 0 until clampedProgress) {
                val startAngle = -90f + (i * segmentAngle) + (gapAngle / 2f)
                val sweep = (segmentAngle - gapAngle).coerceAtLeast(1f)
                drawArc(
                    color = animatedColor,
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            // Separadores radiales
            val radius = diameter / 2f
            val centerOffset = Offset(size.width / 2f, size.height / 2f)
            for (i in 0 until totalSegments) {
                val angleRad = Math.toRadians((-90f + i * segmentAngle).toDouble())
                val innerR = radius - strokeWidth / 2f
                val outerR = radius + strokeWidth / 2f
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

        if (showCenterText) {
            val fontSize = when {
                sizeDp <= 44.dp -> 10.sp
                sizeDp <= 64.dp -> 12.sp
                else -> 14.sp
            }
            Text(
                text = "$clampedProgress/$totalSegments",
                fontSize = fontSize,
                fontWeight = FontWeight.Black,
                color = if (completed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
