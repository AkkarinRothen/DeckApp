package com.deckapp.feature.draw.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deckapp.core.domain.usecase.OrderCombatParticipantsUseCase
import com.deckapp.core.model.*
import com.deckapp.feature.draw.components.clocks.SessionClocksOverlay
import com.deckapp.feature.draw.components.combat.SessionCombatFloatingHud
import com.deckapp.feature.draw.components.threads.SessionThreadsOverlay
import com.deckapp.feature.draw.components.threads.ThreadsOverlayTab

enum class TopHudSection {
    NONE, COMBAT, CLOCKS, THREADS
}

/**
 * Cápsula Unificada Superior (Unified Top HUD) para la mesa de juego.
 * Agrupa Combate, Relojes de Tensión e Hilos/PNJs en una única pastilla compacta
 * con expansión mutuamente exclusiva para eliminar colisiones visuales en móviles.
 */
@Composable
fun SessionTopHudCapsule(
    // Combate
    encounter: Encounter?,
    players: List<PlayerInitiativeParticipant>,
    onNextTurn: () -> Unit,
    onChoirCharacterCommand: (Long, ChoirCharacterCommand) -> Unit,
    onChoirStateChange: (Long, ChoirCombatCommand) -> Unit,
    onApplyDamage: (creatureId: Long, delta: Int) -> Unit,
    onToggleCondition: (creatureId: Long, condition: Condition) -> Unit,
    onEndCombat: () -> Unit,
    onCopyRoundToDiary: () -> Unit,
    // Relojes
    clocks: List<TensionClock>,
    onTickClock: (clockId: Long, delta: Int) -> Unit,
    onAddClock: () -> Unit,
    onEditClock: (TensionClock) -> Unit,
    onResetClock: (clockId: Long) -> Unit,
    onDeleteClock: (clockId: Long) -> Unit,
    onTriggerClockNow: (TensionClock) -> Unit,
    onCopyClockToDiary: (TensionClock) -> Unit,
    // Hilos y PNJs
    threads: List<MythicThread>,
    characters: List<MythicCharacter>,
    selectedThreadsTab: ThreadsOverlayTab,
    lastRandomPick: String?,
    onSelectThreadsTab: (ThreadsOverlayTab) -> Unit,
    onRollRandom: () -> Unit,
    onClearRandomPick: () -> Unit,
    onCopyRandomPickToDiary: () -> Unit,
    onToggleThreadResolved: (threadId: Long, isResolved: Boolean) -> Unit,
    onAddThread: () -> Unit,
    onEditThread: (MythicThread) -> Unit,
    onDeleteThread: (Long) -> Unit,
    onAddCharacter: () -> Unit,
    onEditCharacter: (MythicCharacter) -> Unit,
    onDeleteCharacter: (Long) -> Unit,
    onCopySummaryToDiary: () -> Unit,
    onSendItemToNotes: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var activeSection by rememberSaveable { mutableStateOf(TopHudSection.NONE) }

    val hasActiveCombat = encounter != null && (encounter.creatures.isNotEmpty() || players.isNotEmpty())
    val hasClockAlert = remember(clocks) { clocks.any { it.isCompleted || (it.segments > 0 && it.progress >= it.segments) } }
    val activeThreadsCount = remember(threads) { threads.count { !it.isResolved } }

    val unifiedParticipants = remember(encounter?.creatures, players) {
        if (encounter != null) OrderCombatParticipantsUseCase(encounter.creatures, players) else emptyList()
    }
    val currentParticipant = if (encounter != null && unifiedParticipants.isNotEmpty()) {
        val idx = encounter.currentTurnIndex.coerceIn(0, (unifiedParticipants.size - 1).coerceAtLeast(0))
        unifiedParticipants.getOrNull(idx)
    } else null
    val isPlayerTurn = currentParticipant is UnifiedParticipant.Player

    val hasActiveThreads = activeThreadsCount > 0 || characters.isNotEmpty()
    val hasClocks = clocks.isNotEmpty()

    if (!hasActiveCombat && !hasClockAlert && !hasActiveThreads && !hasClocks) {
        return
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Barra / Pastilla Superior Unificada
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = Color(0xEB161920),
            border = BorderStroke(1.dp, Color(0x33FFFFFF)),
            shadowElevation = 8.dp,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Chip: Combate (si está activo)
                if (hasActiveCombat && encounter != null) {
                    val combatBorderColor = if (isPlayerTurn) Color(0xFF4FC3F7) else Color(0xFFFF5252)
                    TopHudChip(
                        isSelected = activeSection == TopHudSection.COMBAT,
                        icon = Icons.Default.Shield,
                        iconTint = combatBorderColor,
                        label = "R${encounter.currentRound} · ${currentParticipant?.name ?: "Combate"}",
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            activeSection = if (activeSection == TopHudSection.COMBAT) TopHudSection.NONE else TopHudSection.COMBAT
                        },
                        trailingContent = {
                            Surface(
                                shape = CircleShape,
                                color = combatBorderColor.copy(alpha = 0.25f),
                                modifier = Modifier
                                    .size(22.dp)
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
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    )
                }

                // Chip: Hilos y PNJs (solo si hay hilos activos o personajes)
                if (hasActiveThreads) {
                    if (hasActiveCombat && encounter != null) {
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(18.dp)
                                .background(Color(0x33FFFFFF))
                        )
                    }

                    TopHudChip(
                        isSelected = activeSection == TopHudSection.THREADS,
                        icon = Icons.Default.Bookmark,
                        iconTint = MaterialTheme.colorScheme.secondary,
                        label = "Hilos",
                        badge = activeThreadsCount.takeIf { it > 0 }?.toString(),
                        badgeColor = MaterialTheme.colorScheme.secondary,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            activeSection = if (activeSection == TopHudSection.THREADS) TopHudSection.NONE else TopHudSection.THREADS
                        }
                    )
                }

                // Chip: Relojes de Tensión (solo si hay relojes)
                if (hasClocks) {
                    if ((hasActiveCombat && encounter != null) || hasActiveThreads) {
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(18.dp)
                                .background(Color(0x33FFFFFF))
                        )
                    }

                    TopHudChip(
                        isSelected = activeSection == TopHudSection.CLOCKS,
                        icon = Icons.Default.Schedule,
                        iconTint = if (hasClockAlert) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        label = "Relojes",
                        badge = clocks.size.takeIf { it > 0 }?.toString(),
                        badgeColor = if (hasClockAlert) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            activeSection = if (activeSection == TopHudSection.CLOCKS) TopHudSection.NONE else TopHudSection.CLOCKS
                        }
                    )
                }
            }
        }

        // Panel Expandido Exclusivo debajo de la barra
        AnimatedVisibility(
            visible = activeSection != TopHudSection.NONE,
            enter = slideInVertically(initialOffsetY = { -it / 3 }, animationSpec = tween(220)) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it / 3 }, animationSpec = tween(180)) + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .fillMaxWidth(),
                contentAlignment = Alignment.TopCenter
            ) {
                when (activeSection) {
                    TopHudSection.COMBAT -> {
                        if (encounter != null) {
                            SessionCombatFloatingHud(
                                encounter = encounter,
                                players = players,
                                isExpanded = true,
                                onToggleExpanded = { activeSection = TopHudSection.NONE },
                                onNextTurn = onNextTurn,
                                onChoirCharacterCommand = onChoirCharacterCommand,
                                onChoirStateChange = onChoirStateChange,
                                onApplyDamage = onApplyDamage,
                                onToggleCondition = onToggleCondition,
                                onEndCombat = {
                                    activeSection = TopHudSection.NONE
                                    onEndCombat()
                                },
                                onCopyRoundToDiary = onCopyRoundToDiary,
                                showHeaderPill = false
                            )
                        }
                    }
                    TopHudSection.CLOCKS -> {
                        SessionClocksOverlay(
                            clocks = clocks,
                            isExpanded = true,
                            onToggleExpanded = { activeSection = TopHudSection.NONE },
                            onTickClock = onTickClock,
                            onAddClock = onAddClock,
                            onEditClock = onEditClock,
                            onResetClock = onResetClock,
                            onDeleteClock = onDeleteClock,
                            onTriggerClockNow = onTriggerClockNow,
                            onCopyClockToDiary = onCopyClockToDiary,
                            showHeaderPill = false
                        )
                    }
                    TopHudSection.THREADS -> {
                        SessionThreadsOverlay(
                            threads = threads,
                            characters = characters,
                            isExpanded = true,
                            selectedTab = selectedThreadsTab,
                            lastRandomPick = lastRandomPick,
                            onToggleExpanded = { activeSection = TopHudSection.NONE },
                            onSelectTab = onSelectThreadsTab,
                            onRollRandom = onRollRandom,
                            onClearRandomPick = onClearRandomPick,
                            onCopyRandomPickToDiary = onCopyRandomPickToDiary,
                            onToggleThreadResolved = onToggleThreadResolved,
                            onAddThread = onAddThread,
                            onEditThread = onEditThread,
                            onDeleteThread = onDeleteThread,
                            onAddCharacter = onAddCharacter,
                            onEditCharacter = onEditCharacter,
                            onDeleteCharacter = onDeleteCharacter,
                            onCopySummaryToDiary = onCopySummaryToDiary,
                            onSendItemToNotes = onSendItemToNotes,
                            showHeaderPill = false
                        )
                    }
                    TopHudSection.NONE -> {}
                }
            }
        }
    }
}

@Composable
private fun TopHudChip(
    isSelected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    label: String,
    badge: String? = null,
    badgeColor: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit,
    trailingContent: (@Composable () -> Unit)? = null
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) Color(0x44FFFFFF) else Color.Transparent,
        modifier = Modifier.height(34.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(16.dp)
            )

            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (badge != null) {
                Surface(
                    shape = CircleShape,
                    color = badgeColor.copy(alpha = 0.25f),
                    modifier = Modifier.size(18.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = badge,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = badgeColor,
                            fontSize = 9.sp
                        )
                    }
                }
            }

            trailingContent?.invoke()

            Icon(
                imageVector = if (isSelected) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
