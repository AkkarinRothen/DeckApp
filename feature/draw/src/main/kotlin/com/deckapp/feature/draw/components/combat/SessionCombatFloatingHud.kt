package com.deckapp.feature.draw.components.combat

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deckapp.core.model.*

/**
 * Mini-Tracker de Combate e Iniciativa Flotante (Session Combat Floating HUD).
 * Permite seguir y controlar el combate (cola de turnos, HP de participantes, condiciones y rondas)
 * directamente sobre cualquier vista (Mapa Táctico, Tablero Bento, etc.) sin cambiar de pantalla.
 */
@Composable
fun SessionCombatFloatingHud(
    encounter: Encounter?,
    players: List<PlayerInitiativeParticipant> = emptyList(),
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    onNextTurn: () -> Unit,
    onChoirCharacterCommand: (Long, com.deckapp.core.model.ChoirCharacterCommand) -> Unit,
    onChoirStateChange: (Long, ChoirCombatCommand) -> Unit,
    onApplyDamage: (creatureId: Long, delta: Int) -> Unit,
    onToggleCondition: (creatureId: Long, condition: Condition) -> Unit,
    onEndCombat: () -> Unit,
    onCopyRoundToDiary: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (encounter == null || (encounter.creatures.isEmpty() && players.isEmpty())) return

    val haptic = LocalHapticFeedback.current

    // Unificar criaturas y jugadores en lista ordenada de iniciativa
    val unifiedList = remember(encounter.creatures, players) {
        com.deckapp.core.domain.usecase.OrderCombatParticipantsUseCase(encounter.creatures, players)
    }

    val activeIndex = encounter.currentTurnIndex.coerceIn(0, (unifiedList.size - 1).coerceAtLeast(0))
    val activeParticipant = unifiedList.getOrNull(activeIndex)
    val isPlayerTurn = activeParticipant is UnifiedParticipant.Player

    val activeBorderColor = if (isPlayerTurn) Color(0xFF4FC3F7) else Color(0xFFFF5252)

    Column(
        modifier = modifier.widthIn(max = 350.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── Pastilla Inteligente Superior Central (Collapsed View) ───────────
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xEB161920),
            border = BorderStroke(1.2.dp, activeBorderColor.copy(alpha = 0.85f)),
            shadowElevation = 8.dp,
            modifier = Modifier
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onToggleExpanded()
                }
        ) {
            Row(
                modifier = Modifier.padding(start = 12.dp, top = 6.dp, bottom = 6.dp, end = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Icono temático de espadas
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = activeBorderColor,
                    modifier = Modifier.size(18.dp)
                )

                // Texto de Ronda y Turno
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "R${encounter.currentRound}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "·",
                            color = Color.White.copy(alpha = 0.6f)
                        )
                        Text(
                            text = activeParticipant?.name ?: "Sin participantes",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Mini barra o estado de HP si es criatura
                    if (activeParticipant is UnifiedParticipant.Creature) {
                        val c = activeParticipant.data
                        val hpPercent = (c.currentHp.toFloat() / c.maxHp.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
                        val hpColor = when {
                            hpPercent <= 0f -> MaterialTheme.colorScheme.error
                            hpPercent <= 0.35f -> Color(0xFFFFB300)
                            else -> Color(0xFF4CAF50)
                        }
                        Text(
                            text = if (ChoirProfile.fromNotes(c.notes) != null) {
                                val wounds = ChoirCombatState.fromNotes(c.notes)
                                "${wounds.major} mayores · ${wounds.minor} menores"
                            } else if (com.deckapp.core.domain.usecase.ChoirCharacterCodec.read(c.notes) != null) {
                                val sheet = com.deckapp.core.domain.usecase.ChoirCharacterCodec.read(c.notes)!!
                                "${sheet.major}/4 mayores · ${sheet.minor}/8 menores"
                            } else "${c.currentHp}/${c.maxHp} HP",
                            style = MaterialTheme.typography.labelSmall,
                            color = hpColor,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 10.sp
                        )
                    } else if (isPlayerTurn) {
                        Text(
                            text = "Turno de Jugador",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF4FC3F7),
                            fontSize = 10.sp
                        )
                    }
                }

                Spacer(Modifier.width(2.dp))

                // Botón directo para avanzar turno en 1 toque
                Surface(
                    shape = CircleShape,
                    color = activeBorderColor.copy(alpha = 0.2f),
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onNextTurn()
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Siguiente Turno",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Colapsar" else "Expandir",
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // ── Panel Flotante Expandido ──────────────────────────────────────────
        AnimatedVisibility(
            visible = isExpanded,
            enter = slideInVertically(initialOffsetY = { -it / 3 }, animationSpec = tween(220)) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it / 3 }, animationSpec = tween(180)) + fadeOut()
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xF2161920),
                border = BorderStroke(1.dp, Color(0x33FFFFFF)),
                shadowElevation = 12.dp,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .width(340.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Cabecera del Panel
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = encounter.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Ronda ${encounter.currentRound} · ${unifiedList.size} Combatientes",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Button(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onNextTurn()
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(30.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = activeBorderColor)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Turno", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            IconButton(
                                onClick = onToggleExpanded,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cerrar",
                                    tint = Color.White.copy(alpha = 0.6f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // Cola de Iniciativa
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(unifiedList.indices.toList(), key = { unifiedList[it].stableId }) { index ->
                            val participant = unifiedList[index]
                            val isCurrentTurn = index == activeIndex

                            val choirCreature = (participant as? UnifiedParticipant.Creature)?.data
                            if (choirCreature != null && ChoirProfile.fromNotes(choirCreature.notes) != null) {
                                com.deckapp.core.ui.components.ChoirCombatControls(choirCreature,
                                    { onChoirStateChange(choirCreature.id, it) }, isActive = isCurrentTurn)
                            } else if (choirCreature != null && com.deckapp.core.domain.usecase.ChoirCharacterCodec.read(choirCreature.notes) != null) {
                                com.deckapp.core.ui.components.ChoirCharacterCombatControls(choirCreature,
                                    { onChoirCharacterCommand(choirCreature.id, it) }, isActive = isCurrentTurn)
                            } else CombatParticipantItemRow(
                                participant = participant,
                                isCurrentTurn = isCurrentTurn,
                                onApplyDamage = { delta ->
                                    if (participant is UnifiedParticipant.Creature) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onApplyDamage(participant.data.id, delta)
                                    }
                                },
                                onToggleCondition = { condition ->
                                    if (participant is UnifiedParticipant.Creature) {
                                        onToggleCondition(participant.data.id, condition)
                                    }
                                }
                            )
                        }
                    }

                    // Botones Inferiores: Registro en Diario y Finalización
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onCopyRoundToDiary,
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 6.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Copiar Asalto", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }

                        TextButton(
                            onClick = onEndCombat,
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            contentPadding = PaddingValues(vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Concluir", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Fila individual para un participante en la cola de iniciativa del HUD.
 */
@Composable
private fun CombatParticipantItemRow(
    participant: UnifiedParticipant,
    isCurrentTurn: Boolean,
    onApplyDamage: (delta: Int) -> Unit,
    onToggleCondition: (Condition) -> Unit
) {
    var showConditionMenu by remember { mutableStateOf(false) }

    val isCreature = participant is UnifiedParticipant.Creature
    val creature = (participant as? UnifiedParticipant.Creature)?.data
    val isDead = creature?.let { it.currentHp <= 0 } ?: false

    val cardColor = when {
        isDead -> Color.White.copy(alpha = 0.04f)
        isCurrentTurn -> Color(0x33FFB300)
        else -> Color.White.copy(alpha = 0.08f)
    }

    val borderColor = when {
        isCurrentTurn -> Color(0xFFFFB300)
        isDead -> Color.Transparent
        else -> Color.White.copy(alpha = 0.15f)
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = cardColor,
        border = BorderStroke(if (isCurrentTurn) 1.5.dp else 1.dp, borderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Columna Izquierda: Nombre, Tipo, Iniciativa
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (isCurrentTurn) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFFFB300),
                            modifier = Modifier.size(8.dp)
                        ) {}
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = participant.name,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (isCurrentTurn) FontWeight.Black else FontWeight.Bold,
                                color = if (isDead) Color.White.copy(alpha = 0.4f) else Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (isCurrentTurn) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFFFFB300).copy(alpha = 0.25f),
                                    modifier = Modifier.padding(start = 2.dp)
                                ) {
                                    Text(
                                        text = "TURNO",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFFFFB300),
                                        fontSize = 9.sp,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }

                        Text(
                            text = if (isCreature) "Init ${participant.initiative} · CA ${participant.ac}" else "PJ · Init ${participant.initiative}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 10.sp
                        )
                    }
                }

                // Columna Derecha: HP y controles express
                if (creature != null) {
                    val hpColor = when {
                        creature.currentHp <= 0 -> MaterialTheme.colorScheme.error
                        creature.currentHp * 2 <= creature.maxHp -> Color(0xFFFFB300)
                        else -> Color(0xFF4CAF50)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Badge de HP
                        Text(
                            text = "${creature.currentHp}/${creature.maxHp}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = hpColor,
                            fontSize = 11.sp
                        )

                        // Botones de daño/curación rápida
                        QuickDamagePill(label = "-5", color = MaterialTheme.colorScheme.error) { onApplyDamage(-5) }
                        QuickDamagePill(label = "-1", color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)) { onApplyDamage(-1) }
                        QuickDamagePill(label = "+5", color = Color(0xFF4CAF50)) { onApplyDamage(5) }

                        // Menú de Condiciones
                        Box {
                            IconButton(
                                onClick = { showConditionMenu = true },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MedicalServices,
                                    contentDescription = "Condiciones",
                                    tint = if (creature.conditions.isNotEmpty()) Color(0xFFFFB300) else Color.White.copy(alpha = 0.5f),
                                    modifier = Modifier.size(14.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = showConditionMenu,
                                onDismissRequest = { showConditionMenu = false }
                            ) {
                                Condition.values().take(8).forEach { condition ->
                                    val isApplied = creature.conditions.contains(condition)
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Checkbox(
                                                    checked = isApplied,
                                                    onCheckedChange = null,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Text(condition.label)
                                            }
                                        },
                                        onClick = {
                                            onToggleCondition(condition)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Fila de chips de condiciones si tiene activas
            if (creature != null && creature.conditions.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    creature.conditions.forEach { condition ->
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0x33FFB300),
                            border = BorderStroke(0.5.dp, Color(0xFFFFB300).copy(alpha = 0.6f)),
                            modifier = Modifier.clickable { onToggleCondition(condition) }
                        ) {
                            Text(
                                text = condition.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFFFB300),
                                fontSize = 9.sp,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickDamagePill(label: String, color: Color, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.2f),
        border = BorderStroke(0.5.dp, color.copy(alpha = 0.6f)),
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable { onClick() }
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color,
            fontSize = 10.sp,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
        )
    }
}
