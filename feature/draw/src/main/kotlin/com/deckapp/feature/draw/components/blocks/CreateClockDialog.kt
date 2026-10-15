package com.deckapp.feature.draw.components.blocks

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateClockDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, segments: Int, colorHex: String?, triggerDescription: String) -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var segments by rememberSaveable { mutableStateOf(4) }
    var selectedColorHex by rememberSaveable { mutableStateOf<String?>("#E53935") } // Default: Rojo Peligro
    var triggerDescription by rememberSaveable { mutableStateOf("") }

    val segmentOptions = listOf(4, 6, 8, 10, 12)
    val colorPresets = listOf(
        Pair("#E53935", "Peligro"),
        Pair("#FB8C00", "Alerta"),
        Pair("#43A047", "Progreso"),
        Pair("#8E24AA", "Místico"),
        Pair("#1E88E5", "Táctico")
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Nuevo Reloj de Progreso",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Vista previa interactiva en tiempo real del reloj
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ClockSegmentsDisplay(
                        progress = 0,
                        segments = segments,
                        sizeDp = 72.dp,
                        strokeWidthDp = 10.dp,
                        accentColorHex = selectedColorHex,
                        isCompleted = false
                    )
                }

                // Nombre de la tarea o peligro
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre del peligro o tarea") },
                    placeholder = { Text("Ej: Guardias alertados, Infiltración") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Selector de Segmentos
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Segmentos ($segments pasos)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        segmentOptions.forEach { seg ->
                            FilterChip(
                                selected = segments == seg,
                                onClick = { segments = seg },
                                label = {
                                    Text(
                                        "$seg",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (segments == seg) FontWeight.Bold else FontWeight.Normal
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
                        text = "Tono temático",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        colorPresets.forEach { (hex, label) ->
                            val color = remember(hex) {
                                Color(android.graphics.Color.parseColor(hex))
                            }
                            val isSelected = selectedColorHex == hex

                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .clickable { selectedColorHex = hex }
                                    .then(
                                        if (isSelected) {
                                            Modifier.border(2.5.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                        } else {
                                            Modifier.border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = label,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Detonante opcional
                OutlinedTextField(
                    value = triggerDescription,
                    onValueChange = { triggerDescription = it },
                    label = { Text("Detonante o consecuencia (opcional)") },
                    placeholder = { Text("Ej: Los guardias tocan la campana de alarma") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(
                            name.trim(),
                            segments,
                            selectedColorHex,
                            triggerDescription.trim()
                        )
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text("Crear Reloj")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
