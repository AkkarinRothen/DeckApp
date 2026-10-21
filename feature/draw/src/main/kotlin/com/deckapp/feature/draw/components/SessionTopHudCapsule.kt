package com.deckapp.feature.draw.components

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
import androidx.compose.ui.graphics.vector.ImageVector
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
 * Franja Slim Adaptativa de Contexto de Sesión (~36dp).
 * No flota sobre el contenido ni tapa los controles centrales.
 * Muestra chips compactos dinámicos solo cuando hay eventos activos:
 * - 🎭 Escena Actual (con acceso rápido al selector de escenas)
 * - ⚔️ Combate Activo (Ronda y turno actual)
 * - ⏰ Relojes de Tensión (Alertas y progreso)
 * - 🧵 Hilos y Personajes Mythic
 * Al tocar un chip, el detalle se abre en un ModalBottomSheet limpio sin invadir el layout.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionTopHudCapsule(
    // Escena actual
    currentSceneTitle: String? = null,
    onOpenScenePicker: (() -> Unit)? = null,
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
    val hasScene = !currentSceneTitle.isNullOrBlank()

    // Si no hay ningún contexto activo, no ocupa espacio
    if (!hasActiveCombat && !hasClockAlert && !hasActiveThreads && !hasClocks && !hasScene) {
        return
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 34.dp, max = 38.dp)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // 1. Chip: Escena Actual (si existe)
        if (hasScene) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(enabled = onOpenScenePicker != null) {
                        onOpenScenePicker?.invoke()
                    }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MovieCreation,
                        contentDescription = "Escena",
                        modifier = Modifier.size(13.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = currentSceneTitle ?: "Escena",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (onOpenScenePicker != null) {
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // 2. Chip: Combate Activo (solo si hay combate)
        if (hasActiveCombat && encounter != null) {
            val combatBorderColor = if (isPlayerTurn) Color(0xFF4FC3F7) else Color(0xFFFF5252)
            TopHudChip(
                isSelected = activeSection == TopHudSection.COMBAT,
                icon = Icons.Default.Shield,
                iconTint = combatBorderColor,
                label = "R${encounter.currentRound} · ${currentParticipant?.name ?: "Combate"}",
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    activeSection = TopHudSection.COMBAT
                },
                trailingContent = {
                    Surface(
                        shape = CircleShape,
                        color = combatBorderColor.copy(alpha = 0.25f),
                        modifier = Modifier
                            .size(20.dp)
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
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            )
        }

        // 3. Chip: Relojes de Tensión (solo si hay relojes)
        if (hasClocks) {
            TopHudChip(
                isSelected = activeSection == TopHudSection.CLOCKS,
                icon = Icons.Default.Schedule,
                iconTint = if (hasClockAlert) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                label = "Relojes",
                badge = clocks.size.toString(),
                badgeColor = if (hasClockAlert) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    activeSection = TopHudSection.CLOCKS
                }
            )
        }

        // 4. Chip: Hilos y PNJs (solo si hay hilos activos)
        if (hasActiveThreads) {
            TopHudChip(
                isSelected = activeSection == TopHudSection.THREADS,
                icon = Icons.Default.Bookmark,
                iconTint = MaterialTheme.colorScheme.secondary,
                label = "Hilos",
                badge = activeThreadsCount.takeIf { it > 0 }?.toString(),
                badgeColor = MaterialTheme.colorScheme.secondary,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    activeSection = TopHudSection.THREADS
                }
            )
        }
    }

    // ModalBottomSheet Detalle: Se abre hacia arriba sin tapar ni superponer el canvas permanentemente
    if (activeSection != TopHudSection.NONE) {
        ModalBottomSheet(
            onDismissRequest = { activeSection = TopHudSection.NONE },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
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
    icon: ImageVector,
    iconTint: Color,
    label: String,
    badge: String? = null,
    badgeColor: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit,
    trailingContent: (@Composable () -> Unit)? = null
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f),
        border = BorderStroke(
            1.dp,
            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        ),
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconTint,
                modifier = Modifier.size(13.dp)
            )

            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (badge != null) {
                Surface(
                    shape = CircleShape,
                    color = badgeColor,
                    modifier = Modifier.size(16.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = badge,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            trailingContent?.invoke()
        }
    }
}
