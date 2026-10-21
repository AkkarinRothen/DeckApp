package com.deckapp.feature.draw.model.state

/**
 * Estado UI desacoplado y cohesivo para el Tapete 2D (Playmat).
 * Encapsula la selección activa, visibilidad de bandejas e interacción espacial.
 */
data class PlaymatUiState(
    val selectedCardId: Long? = null,
    val showHandTray: Boolean = true,
    val isRulerActive: Boolean = false,
    val isZenMode: Boolean = false,
    val isSessionDrawerOpen: Boolean = false,
    val sessionDrawerTab: Int = 0 // 0: Bitácora en vivo, 1: Notas rápidas
)
