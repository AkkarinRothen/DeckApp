package com.deckapp.feature.draw.model.state

import com.deckapp.core.model.SessionLogEntry
import com.deckapp.core.model.SessionNote

/**
 * Estado UI modularizado para la Bitácora de Sesión y las Notas Rápidas.
 */
data class SessionLogUiState(
    val logs: List<SessionLogEntry> = emptyList(),
    val currentNote: SessionNote? = null,
    val isFilterCurrentScene: Boolean = false,
    val noteDraftMarkdown: String = "",
    val isSavingNote: Boolean = false
)
