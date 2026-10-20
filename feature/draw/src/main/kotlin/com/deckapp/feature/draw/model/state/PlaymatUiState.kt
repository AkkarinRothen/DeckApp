package com.deckapp.feature.draw.model.state

/**
 * Estado UI desacoplado y cohesivo para el Tapete 2D (Playmat).
 * Encapsula la selección activa, visibilidad de bandejas e interacción espacial.
 */
data class PlaymatUiState(
    val selectedCardId: Long? = null,
    val showHandTray: Boolean = true,
    val isRulerActive: Boolean = false
)
