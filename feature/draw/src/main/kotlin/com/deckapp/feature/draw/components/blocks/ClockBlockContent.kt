package com.deckapp.feature.draw.components.blocks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.deckapp.core.model.TensionClock

data class ClockMilestone(val index: Int, val text: String, val isCompleted: Boolean)

/** Parses milestones/tasks from the clock's trigger description using Markdown checklist format. */
fun TensionClock.parseMilestones(): List<ClockMilestone> {
    if (triggerDescription.isBlank()) return emptyList()
    var idx = 0
    return triggerDescription.lines().mapNotNull { line ->
        val trimmed = line.trim()
        if (trimmed.startsWith("- [ ]") || trimmed.startsWith("- [x]")) {
            val completed = trimmed.startsWith("- [x]")
            val text = trimmed.substring(5).trim()
            ClockMilestone(idx++, text, completed)
        } else {
            null
        }
    }
}

/** Toggles the state of a milestone checklist item and returns the new full description text. */
fun TensionClock.toggleMilestone(milestoneIndex: Int): String {
    val lines = triggerDescription.lines().toMutableList()
    var currentMilestoneIdx = 0
    for (i in lines.indices) {
        val trimmed = lines[i].trim()
        if (trimmed.startsWith("- [ ]") || trimmed.startsWith("- [x]")) {
            if (currentMilestoneIdx == milestoneIndex) {
                val isCurrentlyCompleted = trimmed.startsWith("- [x]")
                val leadingWhitespace = lines[i].takeWhile { it.isWhitespace() }
                val taskText = trimmed.substring(5).trim()
                val newStatus = if (isCurrentlyCompleted) "[ ]" else "[x]"
                lines[i] = "$leadingWhitespace- $newStatus $taskText"
                break
            }
            currentMilestoneIdx++
        }
    }
    return lines.joinToString("\n")
}

/** Returns the text of the description ignoring the milestones. */
fun TensionClock.getCleanDescription(): String {
    return triggerDescription.lines().filterNot {
        val trimmed = it.trim()
        trimmed.startsWith("- [ ]") || trimmed.startsWith("- [x]")
    }.joinToString("\n").trim()
}

/**
 * Renderiza el contenido de un bloque de tipo CLOCK en el tablero Bento.
 *
 * - Modo compacto: muestra el gráfico del reloj + progreso como texto.
 * - Modo detallado: muestra descripción, gráfico interactivo y controles precisos (+1, -1, Reset).
 * - En M/L: muestra una Checklist interactiva de hitos. Marcar un hito avanza el reloj automáticamente.
 */
@Composable
fun ClockBlockContent(
    clock: TensionClock?,
    compactInFolder: Boolean,
    onTickClock: (clockId: Long, delta: Int) -> Unit,
    onUpdateClockDescriptionAndProgress: (Long, String, Int) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier
) {
    if (clock == null) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Reloj no vinculado.\nEdita el bloque para asignarlo.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
        return
    }

    val accentColor = clock.colorHex?.runCatching {
        Color(android.graphics.Color.parseColor(this))
    }?.getOrNull() ?: MaterialTheme.colorScheme.primary

    val milestones = clock.parseMilestones()
    val cleanDescription = clock.getCleanDescription()

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (compactInFolder) 6.dp else 10.dp)
    ) {
        // Gráfico del reloj — toque para avanzar (+1 o reset si lleno)
        val clockSize = if (compactInFolder) 64.dp else 88.dp

        Surface(
            shape = CircleShape,
            color = Color.Transparent,
            modifier = Modifier
                .size(clockSize)
                .clickable {
                    val delta = if (clock.isCompleted) -clock.progress else 1
                    onTickClock(clock.id, delta)
                }
        ) {
            ClockSegmentsDisplay(
                progress = clock.progress,
                segments = clock.segments,
                sizeDp = clockSize,
                strokeWidthDp = if (compactInFolder) 9.dp else 12.dp,
                accentColorHex = clock.colorHex,
                isCompleted = clock.isCompleted,
                modifier = Modifier.size(clockSize)
            )
        }

        // Progreso textual
        Text(
            text = "${clock.progress}/${clock.segments}",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = if (clock.isCompleted) MaterialTheme.colorScheme.error else accentColor
        )

        // Descripción del detonante (solo en modo completo, sin hitos)
        if (!compactInFolder && cleanDescription.isNotBlank()) {
            Text(
                text = cleanDescription,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        // Checklist de Hitos (solo en modo completo M/L)
        if (!compactInFolder && milestones.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp, horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Hitos vinculados:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
                milestones.forEach { milestone ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val updatedDesc = clock.toggleMilestone(milestone.index)
                                val diff = if (milestone.isCompleted) -1 else 1
                                val nextProgress = (clock.progress + diff).coerceIn(0, clock.segments)
                                onUpdateClockDescriptionAndProgress(clock.id, updatedDesc, nextProgress)
                            }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = milestone.isCompleted,
                            onCheckedChange = null, // manejado por el Row clickable
                            modifier = Modifier.size(24.dp),
                            colors = CheckboxDefaults.colors(
                                checkedColor = accentColor,
                                uncheckedColor = MaterialTheme.colorScheme.outline
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = milestone.text,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (milestone.isCompleted) {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                }
            }
        }

        // Controles finos (solo en modo completo)
        if (!compactInFolder) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // -1 (min: 0)
                // Si hay hitos, deshabilitar estos botones finos para mantener la consistencia
                FilledTonalIconButton(
                    onClick = { if (clock.progress > 0) onTickClock(clock.id, -1) },
                    enabled = clock.progress > 0 && milestones.isEmpty(),
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "-1", modifier = Modifier.size(16.dp))
                }

                // Reset
                IconButton(
                    onClick = {
                        if (milestones.isNotEmpty()) {
                            // Si tiene hitos, desmarcar todos
                            var currentDesc = clock.triggerDescription
                            milestones.forEach {
                                if (it.isCompleted) {
                                    val tc = TensionClock(id = clock.id, sessionId = clock.sessionId, name = clock.name, triggerDescription = currentDesc, segments = clock.segments, progress = 0)
                                    currentDesc = tc.toggleMilestone(it.index)
                                }
                            }
                            onUpdateClockDescriptionAndProgress(clock.id, currentDesc, 0)
                        } else {
                            onTickClock(clock.id, -clock.progress)
                        }
                    },
                    enabled = clock.progress > 0,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reset", modifier = Modifier.size(16.dp))
                }

                // +1
                FilledTonalIconButton(
                    onClick = { if (!clock.isCompleted) onTickClock(clock.id, 1) },
                    enabled = !clock.isCompleted && milestones.isEmpty(),
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "+1", modifier = Modifier.size(16.dp))
                }
            }

            if (clock.isCompleted) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        "¡Detonante alcanzado!",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
