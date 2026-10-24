package com.deckapp.feature.draw.components.playmat

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.layer.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.deckapp.core.model.*
import com.deckapp.feature.draw.model.state.PlaymatActions
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
internal fun PlaymatDialogsHost(
    state: PlaymatDialogState,
    events: PlaymatCanvasEvents
) {
    val actions = state.actions
    val activeSurface = state.activeSurface
    val cameraX = state.cameraX
    val cameraY = state.cameraY
    val cameraZoom = state.cameraZoom
    val onSelectCard = actions.onSelectCard
    val onQuickDraw = actions.onQuickDraw
    val onAddStickyNote = actions.onAddStickyNote
    val onSendCardToTacticalMap = actions.onSendCardToTacticalMap
    val onUpdateConnection = actions.onUpdateConnection
    val onRemoveConnection = actions.onRemoveConnection
    val onAddToken = actions.onAddToken
    val onUpdateTokenHp = actions.onUpdateTokenHp
    val onAddClock = actions.onAddClock
    val onSetBackgroundMap = actions.onSetBackgroundMap
    val onSetBackgroundTransform = actions.onSetBackgroundTransform
    val onToggleBackgroundLock = actions.onToggleBackgroundLock
    val onSetGridConfig = actions.onSetGridConfig
    val showQuickDrawDialog = state.showQuickDrawDialog
    val showStickyNoteDialog = state.showStickyNoteDialog
    val showSendToTacticalDialog = state.showSendToTacticalDialog
    val showTokenPickerDialog = state.showTokenPickerDialog
    val showClockCreationDialog = state.showClockCreationDialog
    val showTacticalSettingsDialog = state.showTacticalSettingsDialog
    val editingHpToken = state.editingHpToken
    val stickyNoteText = state.stickyNoteText
    val editingConnection = state.editingConnection
    val selectedCard = state.selectedCard
// Diálogo "A Mapa Táctico": Selector de Token o Pin
if (showSendToTacticalDialog && selectedCard != null) {
    AlertDialog(
        onDismissRequest = { events.onShowSendToTacticalDialogChanged(false) },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Explore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Enviar a Mapa Táctico")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "¿Cómo deseas representar '${selectedCard.title}' en la cuadrícula táctica?",
                    style = MaterialTheme.typography.bodyMedium
                )

                OutlinedCard(
                    onClick = {
                        events.onShowSendToTacticalDialogChanged(false)
                        onSendCardToTacticalMap(selectedCard, true)
                        onSelectCard(null)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Token de Combate / Miniatura",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = "Crea una ficha neutral para combate con la ilustración de la carta, PV y CA.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                OutlinedCard(
                    onClick = {
                        events.onShowSendToTacticalDialogChanged(false)
                        onSendCardToTacticalMap(selectedCard, false)
                        onSelectCard(null)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Pin de Exploración / PDI",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = "Coloca una chincheta de mapa con el nombre y pistas de la carta como punto de interés.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = { events.onShowSendToTacticalDialogChanged(false) }) {
                Text("Cancelar")
            }
        }
    )
}

// Diálogo de Robo Rápido
if (showQuickDrawDialog) {
    AlertDialog(
        onDismissRequest = { events.onShowQuickDrawDialogChanged(false) },
        title = { Text("Robar Carta al Tapete") },
        text = {
            if (state.deckRefs.isEmpty()) {
                Text("No hay mazos activos en esta sesión. Añade uno desde la biblioteca.")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.deckRefs.forEach { ref ->
                        val deckName = state.deckNames[ref.stackId] ?: "Mazo"
                        val available = state.deckCardCounts[ref.stackId] ?: 0
                        FilledTonalButton(
                            onClick = {
                                events.onShowQuickDrawDialogChanged(false)
                                val centerX = (-cameraX + 350) / cameraZoom
                                val centerY = (-cameraY + 350) / cameraZoom
                                onQuickDraw(ref.stackId, centerX, centerY)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("$deckName ($available disponibles)")
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = { events.onShowQuickDrawDialogChanged(false) }) { Text("Cerrar") }
        }
    )
}

// Diálogo de Nueva Nota Adhesiva
if (showStickyNoteDialog) {
    AlertDialog(
        onDismissRequest = { events.onShowStickyNoteDialogChanged(false) },
        title = { Text("Nueva Nota Adhesiva") },
        text = {
            OutlinedTextField(
                value = stickyNoteText,
                onValueChange = { events.onStickyNoteTextChanged(it) },
                label = { Text("Contenido de la nota") },
                placeholder = { Text("Escribe una pista o recordatorio...") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 4
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    events.onShowStickyNoteDialogChanged(false)
                    val centerX = (-cameraX + 300) / cameraZoom
                    val centerY = (-cameraY + 300) / cameraZoom
                    onAddStickyNote(stickyNoteText, centerX, centerY)
                }
            ) {
                Text("Añadir al Tapete")
            }
        },
        dismissButton = {
            TextButton(onClick = { events.onShowStickyNoteDialogChanged(false) }) { Text("Cancelar") }
        }
    )
}

// Diálogo de Edición de Hilo Narrativo
if (editingConnection != null) {
    PlaymatConnectionEditorDialog(
        connection = editingConnection!!,
        onDismiss = { events.onEditingConnectionChanged(null) },
        onSave = { label, colorHex ->
            onUpdateConnection(editingConnection!!.id, label, colorHex)
            events.onEditingConnectionChanged(null)
        },
        onDelete = {
            onRemoveConnection(editingConnection!!.id)
            events.onEditingConnectionChanged(null)
        }
    )
}

// Diálogo de Selección de Token de Personaje
if (showTokenPickerDialog) {
    PlaymatTokenPickerDialog(
        npcs = state.npcs,
        onDismiss = { events.onShowTokenPickerDialogChanged(false) },
        onSelectNpc = { npc ->
            val centerX = (-cameraX + 300) / cameraZoom
            val centerY = (-cameraY + 300) / cameraZoom
            val type = if (npc.isMonster) PlaymatTokenType.ENEMY else PlaymatTokenType.NEUTRAL
            val ringColor = if (npc.isMonster) "#E53935" else "#FFB300"
            onAddToken(
                npc.name,
                npc.imagePath,
                npc.id,
                centerX,
                centerY,
                npc.maxHp,
                npc.currentHp,
                type,
                ringColor
            )
        },
        onCreateAdHoc = { name, maxHp, type ->
            val centerX = (-cameraX + 300) / cameraZoom
            val centerY = (-cameraY + 300) / cameraZoom
            val ringColor = when (type) {
                PlaymatTokenType.ALLY -> "#4CAF50"
                PlaymatTokenType.NEUTRAL -> "#FFB300"
                PlaymatTokenType.ENEMY -> "#E53935"
            }
            onAddToken(
                name,
                null,
                null,
                centerX,
                centerY,
                maxHp,
                maxHp,
                type,
                ringColor
            )
        }
    )
}

// Diálogo de Ajuste de PV de Token
if (editingHpToken != null) {
    PlaymatTokenHpDialog(
        token = editingHpToken!!,
        onDismiss = { events.onEditingHpTokenChanged(null) },
        onSaveHp = { newHp ->
            onUpdateTokenHp(editingHpToken!!.id, newHp)
            events.onEditingHpTokenChanged(null)
        }
    )
}

// Diálogo de Creación de Reloj de Progreso (Blades in the Dark)
if (showClockCreationDialog) {
    PlaymatClockCreationDialog(
        onDismissRequest = { events.onShowClockCreationDialogChanged(false) },
        onConfirm = { title, segments, colorHex ->
            val centerX = (-cameraX + 300) / cameraZoom
            val centerY = (-cameraY + 300) / cameraZoom
            onAddClock(title, segments, colorHex, centerX, centerY)
            events.onShowClockCreationDialogChanged(false)
        }
    )
}

// Diálogo de Fondo, Battlemaps y Cuadrícula Táctica
if (showTacticalSettingsDialog) {
    PlaymatTacticalSettingsDialog(
        surface = activeSurface,
        onDismissRequest = { events.onShowTacticalSettingsDialogChanged(false) },
        onSetBackgroundMap = onSetBackgroundMap,
        onSetBackgroundTransform = onSetBackgroundTransform,
        onToggleBackgroundLock = onToggleBackgroundLock,
        onSetGridConfig = onSetGridConfig
    )
}
}
