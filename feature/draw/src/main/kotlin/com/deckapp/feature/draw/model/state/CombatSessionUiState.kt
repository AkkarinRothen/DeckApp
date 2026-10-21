package com.deckapp.feature.draw.model.state

import androidx.compose.runtime.Immutable
import com.deckapp.core.model.CombatLogEntry
import com.deckapp.core.model.Encounter
import com.deckapp.core.model.PlayerInitiativeParticipant

/**
 * Estado que encapsula el subsistema de combate, encuentros activos,
 * registro de combate y participantes de iniciativa en la sesión.
 */
@Immutable
data class CombatSessionUiState(
    val hasActiveEncounter: Boolean = false,
    val activeEncounter: Encounter? = null,
    val combatLog: List<CombatLogEntry> = emptyList(),
    val playerParticipants: List<PlayerInitiativeParticipant> = emptyList(),
    val isCombatHudExpanded: Boolean = false
)
