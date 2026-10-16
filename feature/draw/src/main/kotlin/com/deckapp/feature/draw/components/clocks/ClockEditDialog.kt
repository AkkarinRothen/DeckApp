package com.deckapp.feature.draw.components.clocks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.deckapp.core.model.TensionClock

val CLOCK_PRESET_COLORS = listOf(
    "#E53935" to "Alarma / Peligro",
    "#FB8C00" to "Tensión / Sospecha",
    "#43A047" to "Sigilo / Veneno",
    "#8E24AA" to "Arcano / Maldición",
    "#1E88E5" to "Tiempo / Misión",
    "#00ACC1" to "Tecnología / Éter"
)

val CLOCK_PRESET_SEGMENTS = listOf(4, 6, 8, 10, 12)

/**
 * Diálogo modal para la creación y edición integral de Relojes de Tensión y Progreso.
 */
@Composable
fun ClockEditDialog(
    clock: TensionClock?,
    onDismiss: () -> Unit,
    onSave: (TensionClock) -> Unit
) {
    var name by remember(clock) { mutableStateOf(clock?.name.orEmpty()) }
    var segments by remember(clock) { mutableIntStateOf(clock?.segments ?: 6) }
    var progress by remember(clock) { mutableIntStateOf(clock?.progress ?: 0) }
    var selectedColorHex by remember(clock) { mutableStateOf(clock?.colorHex ?: "#E53935") }
    var triggerDescription by remember(clock) { mutableStateOf(clock?.triggerDescription.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = if (clock == null || clock.id == 0L) "Nuevo Reloj de Tensión" else "Editar Reloj",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Vista previa interactiva
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = name.ifBlank { "Nombre del Reloj" },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Progreso: $progress/$segments segmentos",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    CircularClockGraphic(
                        progress = progress,
                        segments = segments,
                        sizeDp = 52.dp,
                        strokeWidthDp = 7.dp,
                        accentColorHex = selectedColorHex,
                        isCompleted = progress >= segments
                    )
                }

                // Nombre del reloj
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre o Amenaza") },
                    placeholder = { Text("Ej. Alarma de la Fortaleza, Veneno, Sigilo") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Selector de Segmentos
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Segmentos ($segments pasos)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CLOCK_PRESET_SEGMENTS.forEach { seg ->
                            val isSelected = seg == segments
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    segments = seg
                                    if (progress > seg) progress = seg
                                },
                                label = { Text("$seg", fontWeight = FontWeight.Bold) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Selector de Color
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Color Temático",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        CLOCK_PRESET_COLORS.forEach { (hex, _) ->
                            val color = runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(Color.Red)
                            val isSelected = selectedColorHex.equals(hex, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColorHex = hex },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Slider de Progreso Actual
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Progreso Inicial",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "$progress / $segments",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = progress.toFloat(),
                        onValueChange = { progress = it.toInt() },
                        valueRange = 0f..segments.toFloat(),
                        steps = segments - 1,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Consecuencia al detonar
                OutlinedTextField(
                    value = triggerDescription,
                    onValueChange = { triggerDescription = it },
                    label = { Text("Detonante / Consecuencia al Completar") },
                    placeholder = { Text("¿Qué ocurre cuando el reloj se llena al 100%? (Ej. La patrulla entra en la sala y ataca con sorpresa)") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalName = name.trim().ifBlank { "Reloj de Tensión" }
                    val updated = (clock ?: TensionClock()).copy(
                        id = if (clock != null && clock.id != 0L) clock.id else System.currentTimeMillis(),
                        name = finalName,
                        segments = segments,
                        progress = progress.coerceIn(0, segments),
                        colorHex = selectedColorHex,
                        triggerDescription = triggerDescription.trim(),
                        isCompleted = progress >= segments
                    )
                    onSave(updated)
                }
            ) {
                Text("Guardar Reloj")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
