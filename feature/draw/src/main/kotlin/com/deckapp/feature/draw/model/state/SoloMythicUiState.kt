package com.deckapp.feature.draw.model.state

import com.deckapp.core.model.BoardMythicFateResult
import com.deckapp.core.model.ProbabilityLevel

/**
 * Sub-estado modular que encapsula las operaciones reactivas del Oráculo de Mítica
 * y el flujo de juego en solitario (Solo Journaling) dentro de la Sesión / Bento Board.
 */
data class SoloMythicUiState(
    val chaos: Int = 5,
    val probability: ProbabilityLevel = ProbabilityLevel.FIFTY_FIFTY,
    val lastResult: BoardMythicFateResult? = null,
    val history: List<BoardMythicFateResult> = emptyList(),
    val lastEvent: String? = null,
    val isSoloSceneBusy: Boolean = false,
    val isSendingInspiration: Boolean = false,
    val lastRandomMythicPick: String? = null
)
