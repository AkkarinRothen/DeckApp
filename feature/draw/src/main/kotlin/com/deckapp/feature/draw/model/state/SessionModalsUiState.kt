package com.deckapp.feature.draw.model.state

import com.deckapp.core.model.MythicCharacter
import com.deckapp.core.model.MythicThread
import com.deckapp.core.model.SessionHotbarAction
import com.deckapp.core.model.TensionClock

/**
 * Sub-estado modular que encapsula la visibilidad y datos transitorios
 * de las ventanas emergentes, diálogos y hojas modales de la sesión.
 */
data class SessionModalsUiState(
    val showQuickNoteDialog: Boolean = false,
    val showResourceManager: Boolean = false,
    val showSessionConfig: Boolean = false,
    val showDeckPicker: Boolean = false,
    val showStarterPackDialog: Boolean = false,
    val showQuickSearch: Boolean = false,
    val showHotbarActionDialog: Boolean = false,
    val editingHotbarAction: SessionHotbarAction? = null,
    val showSceneFramingDialog: Boolean = false,
    val showClockEditDialog: Boolean = false,
    val editingClock: TensionClock? = null,
    val clockImpactAlert: TensionClock? = null,
    val showThreadEditDialog: Boolean = false,
    val showMythicCharacterEditDialog: Boolean = false,
    val editingMythicThread: MythicThread? = null,
    val editingMythicCharacter: MythicCharacter? = null,
    val showChoirGuide: Boolean = false,
    val showMicrosettingBuilder: Boolean = false,
    val showStoryEngineGuide: Boolean = false
)
