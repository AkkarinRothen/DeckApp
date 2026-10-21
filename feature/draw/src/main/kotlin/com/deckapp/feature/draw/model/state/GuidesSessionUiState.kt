package com.deckapp.feature.draw.model.state

import androidx.compose.runtime.Immutable
import com.deckapp.core.model.MicrosettingCluster
import com.deckapp.feature.draw.model.StoryEngineGuideTab

/**
 * Estado que encapsula los asistentes y guías de sesión interactivos:
 * - Guía de Procedimientos Choir (fuente, tabla, errores).
 * - Generador de Microsetting / Clúster de ambientación (The Story Engine: Deck of Worlds).
 * - Guía paso a paso The Story Engine & Deck of Worlds (pestaña y paso activo).
 */
@Immutable
data class GuidesSessionUiState(
    val choirGuideSourcePage: Int? = null,
    val choirGuideTableId: Long? = null,
    val choirGuideError: String? = null,
    val activeMicrosetting: MicrosettingCluster? = null,
    val editingMicrosettingBoardItemId: Long? = null,
    val storyEngineGuideTab: StoryEngineGuideTab = StoryEngineGuideTab.DECK_OF_WORLDS,
    val storyEngineGuideStepIndex: Int = 0
)
