package com.deckapp.feature.encounters

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.deckapp.core.model.CombatLogEntry
import com.deckapp.core.model.Condition
import com.deckapp.core.model.Encounter
import com.deckapp.core.model.EncounterCreature

@Composable
fun CombatTab(
    encounter: Encounter,
    log: List<CombatLogEntry> = emptyList(),
    onApplyDamage: (Long, Int) -> Unit,
    onNextTurn: () -> Unit,
    onToggleCondition: (Long, Condition) -> Unit,
    onEndEncounter: () -> Unit
) {
    val sortedCreatures = remember(encounter.creatures) {
        encounter.creatures.sortedByDescending { it.initiativeTotal ?: 0 }
    }
    
    var showLog by remember { mutableStateOf(false) }
    var showSummaryDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        // --- Header de Combate ---
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Ronda ${encounter.currentRound}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = encounter.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { showLog = true }) {
                        Icon(Icons.Default.History, contentDescription = "Historial", tint = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = { showSummaryDialog = true }) {
                        Text("FINALIZAR", color = MaterialTheme.colorScheme.error)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = onNextTurn) {
                        Icon(Icons.Default.SkipNext, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("SIGUIENTE")
                    }
                }
            }
        }

        // --- Lista de Iniciativa ---
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(sortedCreatures, key = { it.id }) { creature ->
                val isActive = sortedCreatures.indexOf(creature) == encounter.currentTurnIndex
                CombatCreatureItem(
                    creature = creature,
                    isActive = isActive,
                    onApplyDamage = { onApplyDamage(creature.id, it) },
                    onToggleCondition = { onToggleCondition(creature.id, it) }
                )
            }
            
            item {
                Spacer(Modifier.height(80.dp))
            }
        }

        if (showLog) {
            CombatLogBottomSheet(
                log = log,
                onDismiss = { showLog = false }
            )
        }

        if (showSummaryDialog) {
            CombatSummaryDialog(
                encounter = encounter,
                onConfirm = onEndEncounter,
                onDismiss = { showSummaryDialog = false }
            )
        }
    }
}

@Composable
private fun CombatCreatureItem(
    creature: EncounterCreature,
    isActive: Boolean,
    onApplyDamage: (Int) -> Unit,
    onToggleCondition: (Condition) -> Unit
) {
    val hpPercentage = creature.currentHp.toFloat() / creature.maxHp.coerceAtLeast(1)
    val hpColor = when {
        hpPercentage > 0.6f -> Color(0xFF4CAF50) // Verde
        hpPercentage > 0.25f -> Color(0xFFFFC107) // Amarillo
        else -> Color(0xFFF44336) // Rojo
    }

    val isBloodied = hpPercentage <= 0.5f && hpPercentage > 0f
    
    // Animación de escala para el item activo
    val scale by animateFloatAsState(if (isActive) 1.02f else 1f, label = "scale")

    Surface(
        shape = RoundedCornerShape(12.dp),
        tonalElevation = if (isActive) 8.dp else 0.dp,
        border = if (isActive) {
            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else if (isBloodied) {
            androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF44336).copy(alpha = 0.5f))
        } else null,
        color = if (isActive) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth().graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Indicador de Iniciativa
                Surface(
                    shape = CircleShape,
                    color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = (creature.initiativeTotal ?: "—").toString(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isActive) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
                
                Spacer(Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = creature.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                    )
                    Text(
                        text = "AC ${creature.armorClass}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${creature.currentHp} / ${creature.maxHp}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    LinearProgressIndicator(
                        progress = { hpPercentage },
                        color = hpColor,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.width(60.dp).height(4.dp).clip(CircleShape)
                    )
                }
            }

            // --- Controles de HP (Solo si está activo o expandido, aquí simplificamos siempre) ---
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                HpButton(label = "-5", onClick = { onApplyDamage(-5) })
                HpButton(label = "-1", onClick = { onApplyDamage(-1) })
                HpButton(label = "+1", onClick = { onApplyDamage(1) }, isHeal = true)
                HpButton(label = "+5", onClick = { onApplyDamage(5) }, isHeal = true)
                
                Spacer(Modifier.weight(1f))
                
                IconButton(onClick = { onApplyDamage(creature.maxHp) }) {
                    Icon(Icons.Default.HealthAndSafety, contentDescription = "Curar todo", tint = Color(0xFF4CAF50))
                }
                IconButton(onClick = { onApplyDamage(-creature.maxHp) }) {
                    Icon(Icons.Default.DeleteForever, contentDescription = "Eliminar", tint = Color(0xFFF44336))
                }
            }

            // --- Condiciones ---
            @OptIn(ExperimentalLayoutApi::class)
            if (creature.conditions.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    creature.conditions.forEach { condition ->
                        AssistChip(
                            onClick = { onToggleCondition(condition) },
                            label = { Text(condition.name.lowercase().capitalize(), style = MaterialTheme.typography.labelSmall) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HpButton(label: String, onClick: () -> Unit, isHeal: Boolean = false) {
    FilledTonalButton(
        onClick = onClick,
        modifier = Modifier.height(32.dp),
        contentPadding = PaddingValues(horizontal = 8.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = if (isHeal) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
            contentColor = if (isHeal) Color(0xFF2E7D32) else Color(0xFFC62828)
        )
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CombatLogBottomSheet(
    log: List<CombatLogEntry>,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Historial de Combate",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(16.dp)
            )
            
            if (log.isEmpty()) {
                Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    Text("No hay eventos registrados", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(log) { entry ->
                        Row(verticalAlignment = Alignment.Top) {
                            val time = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                                .format(java.util.Date(entry.timestamp))
                            
                            Text(
                                text = "[$time]",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = entry.message,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CombatSummaryDialog(
    encounter: Encounter,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val survivors = encounter.creatures.filter { it.currentHp > 0 }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Resumen de Combate") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "El combate ha durado ${encounter.currentRound} rondas.",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Text(
                    text = "Supervivientes:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                
                if (survivors.isEmpty()) {
                    Text("Nadie sobrevivió.", style = MaterialTheme.typography.bodySmall)
                } else {
                    survivors.forEach { 
                        Text("• ${it.name} (${it.currentHp}/${it.maxHp})", style = MaterialTheme.typography.bodySmall)
                    }
                }
                
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Al finalizar, este resumen se anexará a las Notas del DM.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("FINALIZAR")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCELAR")
            }
        }
    )
}
