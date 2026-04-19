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
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.sp
import com.deckapp.core.model.CombatLogEntry
import com.deckapp.core.model.Condition
import com.deckapp.core.model.Encounter
import com.deckapp.core.model.EncounterCreature
import com.deckapp.core.model.PlayerInitiativeParticipant
import coil.compose.AsyncImage
import java.util.Locale

@Composable
fun CombatTab(
    encounter: Encounter,
    players: List<PlayerInitiativeParticipant> = emptyList(),
    log: List<CombatLogEntry> = emptyList(),
    onApplyDamage: (Long, Int) -> Unit,
    onNextTurn: () -> Unit,
    onToggleCondition: (Long, Condition) -> Unit,
    onEndEncounter: () -> Unit,
    onAddPlayer: (String, Int) -> Unit,
    onRemovePlayer: (String) -> Unit
) {
    // Unificar criaturas y jugadores en una sola lista para la iniciativa
    val unifiedList = remember(encounter.creatures, players) {
        val list = mutableListOf<UnifiedParticipant>()
        encounter.creatures.forEach { creature ->
            list.add(UnifiedParticipant.Creature(creature))
        }
        players.forEach { player ->
            list.add(UnifiedParticipant.Player(player))
        }

        // Ordenamos prioritariamente por sortOrder (NPCs) e iniciativa (PJs)
        // El objetivo es que coincida con la lógica de NextTurnUseCase.
        list.sortedWith(
            compareBy<UnifiedParticipant> { 
                if (it is UnifiedParticipant.Creature) it.data.sortOrder else -1 
            }.thenByDescending { it.initiative }
        )
    }

    
    var showLog by remember { mutableStateOf(false) }
    var showSummaryDialog by remember { mutableStateOf(false) }
    var showAddPlayerDialog by remember { mutableStateOf(false) }

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
                    IconButton(onClick = { showAddPlayerDialog = true }) {
                        Icon(Icons.Default.PersonAdd, contentDescription = "Añadir Jugador", tint = MaterialTheme.colorScheme.primary)
                    }
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
            modifier = Modifier.weight(1f).fillMaxWidth().imePadding(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(unifiedList, key = { it.stableId }) { participant ->
                val isActive = unifiedList.indexOf(participant) == encounter.currentTurnIndex
                
                when (participant) {
                    is UnifiedParticipant.Creature -> {
                        CombatCreatureItem(
                            creature = participant.data,
                            isActive = isActive,
                            onApplyDamage = { onApplyDamage(participant.data.id, it) },
                            onToggleCondition = { onToggleCondition(participant.data.id, it) },
                            modifier = Modifier.animateItem()
                        )
                    }
                    is UnifiedParticipant.Player -> {
                        CombatPlayerItem(
                            player = participant.data,
                            isActive = isActive,
                            onRemove = { onRemovePlayer(participant.data.id) },
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }
            
            item {
                Spacer(Modifier.height(80.dp))
            }
        }

        if (showAddPlayerDialog) {
            AddPlayerDialog(
                onConfirm = { name, init -> 
                    onAddPlayer(name, init)
                    showAddPlayerDialog = false
                },
                onDismiss = { showAddPlayerDialog = false }
            )
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
    onToggleCondition: (Condition) -> Unit,
    modifier: Modifier = Modifier
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
        modifier = modifier.fillMaxWidth().graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Indicador de Iniciativa / Avatar
                Box(
                    modifier = Modifier.size(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (creature.imagePath != null) {
                        AsyncImage(
                            model = creature.imagePath,
                            contentDescription = null,
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clip(CircleShape)
                        )
                    } else {
                        Surface(
                            shape = CircleShape,
                            color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = (creature.initiativeTotal ?: "—").toString(),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    
                    // Badge de iniciativa si hay avatar
                    if (creature.imagePath != null) {
                        Surface(
                            shape = CircleShape,
                            color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp).align(Alignment.BottomEnd),
                            shadowElevation = 2.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = (creature.initiativeTotal ?: "—").toString(),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
                
                Spacer(Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = creature.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                        color = if (isBloodied) Color(0xFFF44336) else Color.Unspecified
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "AC ${creature.armorClass}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (isBloodied) {
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                color = Color(0xFFF44336),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.height(14.dp)
                            ) {
                                Text(
                                    text = "BLOODIED",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                            }
                        }
                    }
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
                            label = { Text(condition.name.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }, style = MaterialTheme.typography.labelSmall) },
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
            Column(
                modifier = Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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

// --- Player Item ---

@Composable
private fun CombatPlayerItem(
    player: PlayerInitiativeParticipant,
    isActive: Boolean,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(if (isActive) 1.02f else 1f, label = "scale")

    Surface(
        shape = RoundedCornerShape(12.dp),
        tonalElevation = if (isActive) 8.dp else 4.dp,
        border = if (isActive) {
            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.tertiary)
        } else null,
        color = if (isActive) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface,
        modifier = modifier.fillMaxWidth().graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Iniciativa
            Surface(
                shape = CircleShape,
                color = if (isActive) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = player.initiativeTotal.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isActive) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Person, 
                        contentDescription = null, 
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = player.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "Jugador",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Close, contentDescription = "Quitar", tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            }
        }
    }
}

// --- Add Player Dialog ---

@Composable
private fun AddPlayerDialog(
    onConfirm: (String, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var initiative by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Añadir Jugador a Iniciativa") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre del Jugador") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = initiative,
                    onValueChange = { if (it.all { char -> char.isDigit() }) initiative = it },
                    label = { Text("Iniciativa Total") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    if (name.isNotBlank() && initiative.isNotBlank()) {
                        onConfirm(name, initiative.toInt())
                    }
                },
                enabled = name.isNotBlank() && initiative.isNotBlank()
            ) {
                Text("AÑADIR")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCELAR")
            }
        }
    )
}

// --- Unified Participant Helper ---

sealed class UnifiedParticipant {
    abstract val initiative: Int
    abstract val stableId: String

    data class Creature(val data: EncounterCreature) : UnifiedParticipant() {
        override val initiative: Int = data.initiativeTotal ?: 0
        override val stableId: String = "creature_${data.id}"
    }

    data class Player(val data: PlayerInitiativeParticipant) : UnifiedParticipant() {
        override val initiative: Int = data.initiativeTotal
        override val stableId: String = "player_${data.id}"
    }
}


