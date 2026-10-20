package com.deckapp.feature.draw.components.playmat

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deckapp.core.model.PlaymatConnection
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Renderiza un hilo narrativo entre dos cartas con aspecto físico:
 * chinchetas de anclaje, cuerda elástica con curvatura por gravedad y
 * píldora central interactiva para consultar o editar la relación.
 */
@Composable
fun PlaymatConnectionLine(
    connection: PlaymatConnection,
    fromPos: Offset,
    toPos: Offset,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lineColor = remember(connection.colorHex) {
        try {
            Color(android.graphics.Color.parseColor(connection.colorHex))
        } catch (e: Exception) {
            Color(0xFFE53935)
        }
    }

    val dist = hypot(toPos.x - fromPos.x, toPos.y - fromPos.y)
    val midX = (fromPos.x + toPos.x) / 2f
    val midY = (fromPos.y + toPos.y) / 2f
    val sag = min(dist * 0.10f, 35f)
    val ctrlX = midX
    val ctrlY = midY + sag

    // Punto en t = 0.5 de la curva de Bézier
    val labelX = midX
    val labelY = midY + (sag * 0.5f)

    Box(modifier = modifier.fillMaxSize()) {
        // Lienzo para el hilo elástico y las chinchetas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val path = Path().apply {
                moveTo(fromPos.x, fromPos.y)
                quadraticTo(ctrlX, ctrlY, toPos.x, toPos.y)
            }

            // 1. Sombra arrojada del hilo sobre la superficie
            val shadowPath = Path().apply {
                moveTo(fromPos.x, fromPos.y + 4.dp.toPx())
                quadraticTo(ctrlX, ctrlY + 4.dp.toPx(), toPos.x, toPos.y + 4.dp.toPx())
            }
            drawPath(
                path = shadowPath,
                color = Color.Black.copy(alpha = 0.35f),
                style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
            )

            // 2. Línea principal del hilo
            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
            )

            // 3. Chinchetas en ambos extremos
            drawPushPin(fromPos, lineColor)
            drawPushPin(toPos, lineColor)
        }

        // 4. Píldora interactiva de etiqueta en el centro del hilo
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = labelX.roundToInt(),
                        y = labelY.roundToInt()
                    )
                }
                .wrapContentSize()
        ) {
            // Centrado sobre la coordenada
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
                border = androidx.compose.foundation.BorderStroke(1.5.dp, lineColor),
                modifier = Modifier
                    .offset(x = (-50).dp, y = (-14).dp) // Desplazamiento aproximado para centrado
                    .clickable { onEdit() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(lineColor)
                    )
                    if (connection.label.isNotBlank()) {
                        Text(
                            text = connection.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 11.sp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Editar relación",
                            tint = lineColor,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawPushPin(pos: Offset, pinColor: Color) {
    // Sombra del pin
    drawCircle(
        color = Color.Black.copy(alpha = 0.45f),
        radius = 7.dp.toPx(),
        center = Offset(pos.x + 1.dp.toPx(), pos.y + 2.dp.toPx())
    )

    // Borde metálico exterior
    drawCircle(
        color = Color(0xFF37474F),
        radius = 5.5.dp.toPx(),
        center = pos
    )

    // Cabeza de color
    drawCircle(
        color = pinColor,
        radius = 4.5.dp.toPx(),
        center = pos
    )

    // Brillo especular
    drawCircle(
        color = Color.White.copy(alpha = 0.8f),
        radius = 1.2.dp.toPx(),
        center = Offset(pos.x - 1.5.dp.toPx(), pos.y - 1.5.dp.toPx())
    )
}
