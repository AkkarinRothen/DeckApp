@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.deckapp.feature.draw.components.blocks

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deckapp.core.model.*
import com.deckapp.feature.draw.components.BoardBlockMeta
import com.deckapp.feature.draw.util.getMode
import kotlinx.coroutines.delay
import java.util.Locale

private data class TtrpgColors(
    val healing: Color = Color(0xFF4CAF50),
    val critical: Color = Color(0xFFFF9800),
    val damage: Color = Color(0xFFE53935),
    val damageContainer: Color = Color(0x33E53935),
    val onDamageContainer: Color = Color(0xFFFF8A80),
    val healingContainer: Color = Color(0x334CAF50),
    val onHealingContainer: Color = Color(0xFFB9F6CA)
)

private val localTtrpgColors = TtrpgColors()
private val MaterialTheme.ttrpgColors: TtrpgColors
    @Composable
    get() = localTtrpgColors

@Composable
fun CombatBlockContent(
    item: SessionBoardItem,
    meta: BoardBlockMeta,
    activeEncounter: Encounter?,
    playerParticipants: List<PlayerInitiativeParticipant>,
    permanentPlayers: List<Npc>,
    compactInFolder: Boolean,
    onApplyDamage: (Long, Int) -> Unit,
    onNextTurn: () -> Unit,
    onEndEncounter: () -> Unit,
    onAddPlayer: (String, Int) -> Unit,
    onRemovePlayer: (String) -> Unit,
    onSavePermanentPlayer: (Npc) -> Unit,
    onDeletePermanentPlayer: (Npc) -> Unit,
    onOpenTracker: () -> Unit,
    npcs: List<Npc> = emptyList(),
    onDiceClicked: (String) -> Unit = {},
    onToggleCondition: (Long, Condition) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val mode = item.getMode()
    if (activeEncounter == null) {
        // Estado sin combate activo
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "No hay combate activo",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            Button(
                onClick = onOpenTracker,
                colors = ButtonDefaults.buttonColors(containerColor = meta.color),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(Icons.Default.CrisisAlert, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Abrir Tracker")
            }
        }
        return
    }

    // Lista unificada y ordenada de la iniciativa
    val unifiedList = remember(activeEncounter.creatures, playerParticipants) {
        val list = mutableListOf<UnifiedParticipant>()
        activeEncounter.creatures.forEach { creature ->
            list.add(UnifiedParticipant.Creature(creature))
        }
        playerParticipants.forEach { player ->
            list.add(UnifiedParticipant.Player(player))
        }

        list.sortedWith(
            compareBy<UnifiedParticipant> { 
                if (it is UnifiedParticipant.Creature) it.data.sortOrder else -1 
            }.thenByDescending { it.initiative }
        )
    }

    val activeParticipant = unifiedList.getOrNull(activeEncounter.currentTurnIndex)
    val activeCombatantName = activeParticipant?.name ?: "—"
    val creatureCount = activeEncounter.creatures.size
    val playerCount = playerParticipants.size
    val bloodiedCreatures = activeEncounter.creatures.count { creature ->
        val maxHp = creature.maxHp.coerceAtLeast(1)
        creature.currentHp in 1 until maxHp && creature.currentHp * 2 <= maxHp
    }
    val downCreatures = activeEncounter.creatures.count { it.currentHp <= 0 }
    
    val activeHpText = if (activeParticipant is UnifiedParticipant.Creature) {
        "HP: ${activeParticipant.data.currentHp}/${activeParticipant.data.maxHp}"
    } else {
        "Jugador"
    }

    val activeCreatureId = (activeParticipant as? UnifiedParticipant.Creature)?.data?.id
    val activeEncounterModeLabel = if (mode == "live") "Modo vivo" else "Vista rapida"

    var damageDialogTarget by remember { mutableStateOf<EncounterCreature?>(null) }
    var showAddPlayerDialog by remember { mutableStateOf(false) }

    if (damageDialogTarget != null) {
        QuickDamageDialog(
            creatureName = damageDialogTarget!!.name,
            onConfirm = { delta ->
                onApplyDamage(damageDialogTarget!!.id, delta)
                damageDialogTarget = null
            },
            onDismiss = { damageDialogTarget = null }
        )
    }

    if (showAddPlayerDialog) {
        BentoAddPlayerDialog(
            permanentPlayers = permanentPlayers,
            onConfirm = { name, initiative ->
                onAddPlayer(name, initiative)
                showAddPlayerDialog = false
            },
            onSavePermanentPlayer = onSavePermanentPlayer,
            onDeletePermanentPlayer = onDeletePermanentPlayer,
            onDismiss = { showAddPlayerDialog = false }
        )
    }

    if (item.size == SessionBoardItemSize.S || compactInFolder) {
        // Tamaño S (Compacto)
        Row(
            modifier = modifier
                .fillMaxWidth()
                .animateContentSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Ronda ${activeEncounter.currentRound} · Turno activo",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = activeCombatantName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = meta.color,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = activeEncounter.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$creatureCount enemigo(s) · $playerCount PJ · $bloodiedCreatures heridos",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            FilledIconButton(
                onClick = onNextTurn,
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = meta.color),
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Default.SkipNext, contentDescription = "Siguiente Turno", tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
    } else if (item.size == SessionBoardItemSize.M) {
        // Tamaño M (Mediano)
        Column(
            modifier = modifier
                .fillMaxWidth()
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = activeEncounter.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Ronda ${activeEncounter.currentRound}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Text(
                    text = activeEncounterModeLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = meta.color,
                    fontWeight = FontWeight.Bold
                )
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CombatSummaryChip(label = "Turno", value = activeCombatantName)
                CombatSummaryChip(label = "Enemigos", value = creatureCount.toString())
                CombatSummaryChip(label = "PJ", value = playerCount.toString())
                CombatSummaryChip(label = "Heridos", value = bloodiedCreatures.toString())
                if (downCreatures > 0) {
                    CombatSummaryChip(label = "Caidos", value = downCreatures.toString())
                }
            }

            ActiveTurnPanel(
                activeParticipant = activeParticipant,
                npcs = npcs,
                onDiceClicked = onDiceClicked,
                onToggleCondition = onToggleCondition,
                metaColor = meta.color
            )

            // Carrusel horizontal de iniciativa
            val listState = rememberLazyListState()
            LaunchedEffect(activeEncounter.currentTurnIndex) {
                if (activeEncounter.currentTurnIndex in unifiedList.indices) {
                    listState.animateScrollToItem(activeEncounter.currentTurnIndex)
                }
            }

            LazyRow(
                state = listState,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                itemsIndexed(unifiedList) { index, participant ->
                    val isActive = index == activeEncounter.currentTurnIndex
                    val ttrpgColors = MaterialTheme.ttrpgColors
                    
                    val cardBg = if (isActive) meta.color.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceContainerLow
                    val cardBorderColor = if (isActive) meta.color else Color.Transparent

                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = cardBg,
                        border = androidx.compose.ui.graphics.SolidColor(cardBorderColor).let { border ->
                            androidx.compose.foundation.BorderStroke(1.dp, border)
                        },
                        modifier = Modifier
                            .widthIn(min = 80.dp, max = 130.dp)
                            .clickable {
                                if (participant is UnifiedParticipant.Creature) {
                                    damageDialogTarget = participant.data
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = participant.initiative.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isActive) meta.color else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Column {
                                Text(
                                    text = participant.name,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (participant is UnifiedParticipant.Creature) {
                                    val hpPercent = participant.data.currentHp.toFloat() / participant.data.maxHp.coerceAtLeast(1)
                                    val hpCol = when {
                                        hpPercent > 0.6f -> ttrpgColors.healing
                                        hpPercent > 0.25f -> ttrpgColors.critical
                                        else -> ttrpgColors.damage
                                    }
                                    Text(
                                        text = "${participant.data.currentHp} HP",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                        color = hpCol,
                                        fontWeight = FontWeight.Bold
                                    )
                                } else {
                                    Text(
                                        text = "PJ",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                        color = MaterialTheme.colorScheme.tertiary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Controles rápidos para el turno activo
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (activeCreatureId != null) {
                        val ttrpgColors = MaterialTheme.ttrpgColors
                        FilledTonalButton(
                            onClick = { onApplyDamage(activeCreatureId, -5) },
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = ttrpgColors.damageContainer,
                                contentColor = ttrpgColors.onDamageContainer
                            )
                        ) {
                            Text("-5 HP", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                        FilledTonalButton(
                            onClick = { onApplyDamage(activeCreatureId, 5) },
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = ttrpgColors.healingContainer,
                                contentColor = ttrpgColors.onHealingContainer
                            )
                        ) {
                            Text("+5 HP", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Text("Turno del jugador", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        onClick = onOpenTracker,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Abrir")
                    }
                    FilledTonalButton(
                        onClick = onNextTurn,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = meta.color.copy(alpha = 0.12f),
                            contentColor = meta.color
                        ),
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(Icons.Default.SkipNext, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Siguiente")
                    }
                }
            }
        }
    } else {
        // Tamaño L (Grande)
        Column(
            modifier = modifier
                .fillMaxWidth()
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header del bloque L
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = activeEncounter.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = meta.color
                    )
                    Text(
                        text = "Ronda ${activeEncounter.currentRound} · Turno de: $activeCombatantName",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onOpenTracker,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, "Abrir tracker", tint = meta.color)
                    }
                    IconButton(
                        onClick = { showAddPlayerDialog = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.PersonAdd, "Añadir PJ", tint = meta.color)
                    }
                    IconButton(
                        onClick = onNextTurn,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.SkipNext, "Siguiente turno", tint = meta.color)
                    }
                    IconButton(
                        onClick = onEndEncounter,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.DeleteForever, "Terminar encuentro", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CombatSummaryChip(label = "Enemigos", value = creatureCount.toString())
                CombatSummaryChip(label = "PJ", value = playerCount.toString())
                CombatSummaryChip(label = "Heridos", value = bloodiedCreatures.toString())
                CombatSummaryChip(label = "Caidos", value = downCreatures.toString())
                CombatSummaryChip(label = "Modo", value = activeEncounterModeLabel)
            }

            ActiveTurnPanel(
                activeParticipant = activeParticipant,
                npcs = npcs,
                onDiceClicked = onDiceClicked,
                onToggleCondition = onToggleCondition,
                metaColor = meta.color
            )

            // Lista vertical completa de iniciativa
            val verticalListState = rememberLazyListState()
            LaunchedEffect(activeEncounter.currentTurnIndex) {
                if (activeEncounter.currentTurnIndex in unifiedList.indices) {
                    verticalListState.animateScrollToItem(activeEncounter.currentTurnIndex)
                }
            }

            LazyColumn(
                state = verticalListState,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 240.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                itemsIndexed(unifiedList) { index, participant ->
                    val isActive = index == activeEncounter.currentTurnIndex
                    val borderStroke = if (isActive) {
                        androidx.compose.foundation.BorderStroke(2.dp, meta.color)
                    } else null

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        tonalElevation = if (isActive) 6.dp else 1.dp,
                        border = borderStroke,
                        color = if (isActive) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Círculo de Iniciativa
                            Surface(
                                shape = CircleShape,
                                color = if (isActive) meta.color else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = participant.initiative.toString(),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(Modifier.width(10.dp))

                            // Nombre e info del combatiente
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = participant.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "AC ${participant.ac}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Vida y barra / o quitar jugador
                            if (participant is UnifiedParticipant.Creature) {
                                val creature = participant.data
                                val hpPercent = creature.currentHp.toFloat() / creature.maxHp.coerceAtLeast(1)
                                val hpCol = when {
                                    hpPercent > 0.6f -> MaterialTheme.ttrpgColors.healing
                                    hpPercent > 0.25f -> MaterialTheme.ttrpgColors.critical
                                    else -> MaterialTheme.ttrpgColors.damage
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // HP textual
                                    Column(
                                        horizontalAlignment = Alignment.End,
                                        modifier = Modifier.clickable {
                                            damageDialogTarget = creature
                                        }
                                    ) {
                                        Text(
                                            text = "${creature.currentHp}/${creature.maxHp}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        LinearProgressIndicator(
                                            progress = { hpPercent },
                                            color = hpCol,
                                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                            modifier = Modifier
                                                .width(50.dp)
                                                .height(3.dp)
                                                .clip(CircleShape)
                                        )
                                    }

                                    // Botones rápidos de HP (+1, -1)
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        FilledTonalIconButton(
                                            onClick = { onApplyDamage(creature.id, -1) },
                                            modifier = Modifier.size(28.dp),
                                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                                containerColor = MaterialTheme.ttrpgColors.damageContainer,
                                                contentColor = MaterialTheme.ttrpgColors.onDamageContainer
                                            )
                                        ) {
                                            Icon(Icons.Default.Remove, null, modifier = Modifier.size(12.dp))
                                        }
                                        FilledTonalIconButton(
                                            onClick = { onApplyDamage(creature.id, 1) },
                                            modifier = Modifier.size(28.dp),
                                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                                containerColor = MaterialTheme.ttrpgColors.healingContainer,
                                                contentColor = MaterialTheme.ttrpgColors.onHealingContainer
                                            )
                                        ) {
                                            Icon(Icons.Default.Add, null, modifier = Modifier.size(12.dp))
                                        }
                                    }
                                }
                            } else if (participant is UnifiedParticipant.Player) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "PJ",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.tertiary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    IconButton(
                                        onClick = { onRemovePlayer(participant.data.id) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Quitar jugador",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CombatSummaryChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun QuickDamageDialog(
    creatureName: String,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Modificar HP: $creatureName") },
        text = {
            OutlinedTextField(
                value = amountText,
                onValueChange = { if (it.all { char -> char.isDigit() }) amountText = it },
                label = { Text("Cantidad") },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                ),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = {
                        val amount = amountText.toIntOrNull() ?: 0
                        onConfirm(-amount) // Daño (negativo)
                    },
                    enabled = amountText.isNotEmpty()
                ) {
                    Text("DAÑO")
                }
                TextButton(
                    onClick = {
                        val amount = amountText.toIntOrNull() ?: 0
                        onConfirm(amount) // Curar (positivo)
                    },
                    enabled = amountText.isNotEmpty()
                ) {
                    Text("CURAR")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCELAR")
            }
        }
    )
}

@Composable
fun BentoAddPlayerDialog(
    permanentPlayers: List<Npc>,
    onConfirm: (String, Int) -> Unit,
    onSavePermanentPlayer: (Npc) -> Unit,
    onDeletePermanentPlayer: (Npc) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var initiative by remember { mutableStateOf("") }
    var selectedPlayer by remember { mutableStateOf<Npc?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Añadir Jugador a Iniciativa") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (permanentPlayers.isNotEmpty()) {
                    Text("Jugadores frecuentes:", style = MaterialTheme.typography.labelSmall)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        permanentPlayers.forEach { player ->
                            FilterChip(
                                selected = selectedPlayer?.id == player.id,
                                onClick = {
                                    if (selectedPlayer?.id == player.id) {
                                        selectedPlayer = null
                                    } else {
                                        selectedPlayer = player
                                        name = player.name
                                    }
                                },
                                label = { Text(player.name) }
                            )
                        }
                    }
                }

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
                        onConfirm(name, initiative.toIntOrNull() ?: 0)
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

private fun Condition.toSpanish(): String = when (this) {
    Condition.BLINDED -> "Cegado"
    Condition.CHARMED -> "Hechizado"
    Condition.DEAFENED -> "Ensordecido"
    Condition.FRIGHTENED -> "Asustado"
    Condition.GRAPPLED -> "Agarrado"
    Condition.INCAPACITATED -> "Incapacitado"
    Condition.INVISIBLE -> "Invisible"
    Condition.PARALYZED -> "Paralizado"
    Condition.PETRIFIED -> "Petrificado"
    Condition.POISONED -> "Envenenado"
    Condition.PRONE -> "Tumbado"
    Condition.RESTRAINED -> "Apresado"
    Condition.STUNNED -> "Aturdido"
    Condition.UNCONSCIOUS -> "Inconsciente"
    Condition.EXHAUSTED -> "Exhausto"
}

@Composable
private fun ActiveTurnPanel(
    activeParticipant: UnifiedParticipant?,
    npcs: List<Npc>,
    onDiceClicked: (String) -> Unit,
    onToggleCondition: (Long, Condition) -> Unit,
    metaColor: Color
) {
    if (activeParticipant == null) return

    val creature = (activeParticipant as? UnifiedParticipant.Creature)?.data
    val npc = creature?.npcId?.let { nid -> npcs.find { it.id == nid } }

    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Cabecera: Nombre y HP
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Turno Activo: ${activeParticipant.name}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (creature != null) "CA ${creature.armorClass} · HP ${creature.currentHp}/${creature.maxHp}" else "Personaje Jugador",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (creature != null) {
                    val hpPercent = creature.currentHp.toFloat() / creature.maxHp.coerceAtLeast(1)
                    val hpColor = when {
                        hpPercent > 0.6f -> MaterialTheme.ttrpgColors.healing
                        hpPercent > 0.25f -> MaterialTheme.ttrpgColors.critical
                        else -> MaterialTheme.ttrpgColors.damage
                    }
                    Box(modifier = Modifier.width(60.dp)) {
                        LinearProgressIndicator(
                            progress = { hpPercent },
                            color = hpColor,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape)
                        )
                    }
                }
            }

            // Acciones / Tiradas rápidas
            if (creature != null) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                Text(
                    text = "Acciones y Tiradas Rápidas",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val bonus = npc?.initiativeBonus ?: creature.initiativeBonus
                    val sign = if (bonus >= 0) "+$bonus" else "$bonus"
                    SuggestionChip(
                        onClick = { onDiceClicked("1d20$sign") },
                        label = { Text("Iniciativa ($sign)", fontSize = 11.sp) },
                        modifier = Modifier.height(28.dp)
                    )
                    SuggestionChip(
                        onClick = { onDiceClicked("1d20") },
                        label = { Text("Ataque 1d20", fontSize = 11.sp) },
                        modifier = Modifier.height(28.dp)
                    )
                    SuggestionChip(
                        onClick = { onDiceClicked("1d6") },
                        label = { Text("Daño 1d6", fontSize = 11.sp) },
                        modifier = Modifier.height(28.dp)
                    )
                    SuggestionChip(
                        onClick = { onDiceClicked("1d8") },
                        label = { Text("Daño 1d8", fontSize = 11.sp) },
                        modifier = Modifier.height(28.dp)
                    )
                }
            }

            // Toggles de condición / estados
            if (creature != null) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Condiciones y Estados",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )

                    var showConditionsMenu by remember { mutableStateOf(false) }
                    Box {
                        TextButton(
                            onClick = { showConditionsMenu = true },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(24.dp)
                        ) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(2.dp))
                            Text("Añadir", style = MaterialTheme.typography.labelSmall)
                        }

                        DropdownMenu(
                            expanded = showConditionsMenu,
                            onDismissRequest = { showConditionsMenu = false }
                        ) {
                            Condition.values().forEach { cond ->
                                val isActive = creature.conditions.contains(cond)
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            if (isActive) {
                                                Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                            } else {
                                                Spacer(Modifier.width(16.dp))
                                            }
                                            Text(cond.toSpanish())
                                        }
                                    },
                                    onClick = {
                                        onToggleCondition(creature.id, cond)
                                        showConditionsMenu = false
                                    }
                                )
                            }
                        }
                    }
                }

                val commonConditions = listOf(
                    Condition.POISONED,
                    Condition.PRONE,
                    Condition.FRIGHTENED,
                    Condition.STUNNED,
                    Condition.UNCONSCIOUS,
                    Condition.INVISIBLE
                )

                if (creature != null) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        commonConditions.forEach { cond ->
                            val isActive = creature.conditions.contains(cond)
                            FilterChip(
                                selected = isActive,
                                onClick = { onToggleCondition(creature.id, cond) },
                                label = { Text(cond.toSpanish(), fontSize = 10.sp) },
                                modifier = Modifier.height(28.dp)
                            )
                        }

                        creature.conditions.forEach { cond ->
                            if (cond !in commonConditions) {
                                FilterChip(
                                    selected = true,
                                    onClick = { onToggleCondition(creature.id, cond) },
                                    label = { Text(cond.toSpanish(), fontSize = 10.sp) },
                                    modifier = Modifier.height(28.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
