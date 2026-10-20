package com.deckapp.feature.draw.components.playmat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Diálogo interactivo para configurar y colocar un nuevo Reloj de Progreso (Progress Clock)
 * en la Mesa de Juego (Playmat).
 */
@Composable
fun PlaymatClockCreationDialog(
    onDismissRequest: () -> Unit,
    onConfirm: (title: String, segments: Int, colorHex: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedSegments by remember { mutableIntStateOf(4) }
    var selectedColorHex by remember { mutableStateOf("#E53935") }

    val presetColors = listOf(
        "#E53935" to "Peligro / Alarma",
        "#FB8C00" to "Tensión / Alerta",
        "#8E24AA" to "Magia / Misterio",
        "#1E88E5" to "Tiempo / Misión",
        "#43A047" to "Aliado / Progreso",
        "#546E7A" to "Neutral"
    )

    val quickTitles = listOf(
        "Alerta de Guardias",
        "Tiempo de Escape",
        "Ritual Oscuro",
        "Detección Sigilosa",
        "Llegada de Refuerzos"
    )

    AlertDialog(
        onDismissRequest = onDismissRequest,
        icon = {
            Icon(
                imageVector = Icons.Default.HourglassTop,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text(
                text = "Nuevo Reloj de Progreso",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Campo de Título
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Título o Evento") },
                    placeholder = { Text("Ej: Guardias en alerta") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Chips de sugerencia rápida
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickTitles.take(3).forEach { suggestion ->
                        AssistChip(
                            onClick = { title = suggestion },
                            label = { Text(suggestion, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                // Selector de Segmentos (4, 6 u 8)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Segmentos de Tensión (Blades in the Dark)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(4, 6, 8).forEach { segs ->
                            val isSelected = selectedSegments == segs
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedSegments = segs },
                                label = {
                                    Text(
                                        text = "$segs Segmentos",
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Selector de Color Temático
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Color del Progreso",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        presetColors.forEach { (hex, _) ->
                            val color = try {
                                Color(android.graphics.Color.parseColor(hex))
                            } catch (e: Exception) {
                                Color.Red
                            }
                            val isSelected = selectedColorHex == hex

                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColorHex = hex }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        title.ifBlank { "Reloj de Tensión" },
                        selectedSegments,
                        selectedColorHex
                    )
                }
            ) {
                Text("Colocar en Mesa")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancelar")
            }
        }
    )
}
