package com.deckapp.feature.draw.components.playmat

import androidx.compose.ui.geometry.Offset
import com.deckapp.core.model.Card
import com.deckapp.core.model.PlaymatConnection
import com.deckapp.core.model.PlaymatSurface
import com.deckapp.core.model.PlaymatTokenPlacement
import com.deckapp.feature.draw.SessionUiState
import com.deckapp.feature.draw.model.state.PlaymatActions

/** Read-only snapshot of the values shared by the extracted playmat layers. */
internal data class PlaymatCanvasState(
    val uiState: SessionUiState,
    val actions: PlaymatActions,
    val activeSurface: PlaymatSurface,
    val allSurfaces: List<PlaymatSurface>,
    val cameraX: Float,
    val cameraY: Float,
    val cameraZoom: Float,
    val showQuickDrawDialog: Boolean,
    val showStickyNoteDialog: Boolean,
    val showSendToTacticalDialog: Boolean,
    val showDiceOverlay: Boolean,
    val showTokenPickerDialog: Boolean,
    val showClockCreationDialog: Boolean,
    val showTacticalSettingsDialog: Boolean,
    val isRulerActive: Boolean,
    val rulerStartPos: Offset?,
    val rulerCurrentPos: Offset?,
    val tokenDragStartPos: Offset?,
    val tokenDragCurrentPos: Offset?,
    val editingHpToken: PlaymatTokenPlacement?,
    val selectedTokenId: String?,
    val selectedClockId: String?,
    val stickyNoteText: String,
    val hoveredSlotTag: String?,
    val connectingSourceEndpointId: String?,
    val editingConnection: PlaymatConnection?,
    val cardsById: Map<Long, Card>,
    val endpointPositions: Map<String, Offset>,
    val piledCardsByDeck: Map<Long, List<Card>>,
    val placedCardIds: Set<Long>,
    val currentTemplateSlots: List<TemplateSlotDef>,
    val selectedCardPlacement: com.deckapp.core.model.PlaymatCardPlacement?,
    val selectedCard: Card?
)

/** Events that let stateless layers update state owned by PlaymatCanvasView. */
internal data class PlaymatCanvasEvents(
    val onHoveredSlotTagChanged: (String?) -> Unit,
    val onConnectingSourceChanged: (String?) -> Unit,
    val onSelectedTokenChanged: (String?) -> Unit,
    val onSelectedClockChanged: (String?) -> Unit,
    val onTokenDragPositionsChanged: (Offset?, Offset?) -> Unit,
    val onEditingHpTokenChanged: (PlaymatTokenPlacement?) -> Unit,
    val onEditingConnectionChanged: (PlaymatConnection?) -> Unit,
    val onShowQuickDrawDialogChanged: (Boolean) -> Unit,
    val onShowStickyNoteDialogChanged: (Boolean) -> Unit,
    val onShowSendToTacticalDialogChanged: (Boolean) -> Unit,
    val onShowDiceOverlayChanged: (Boolean) -> Unit,
    val onShowTokenPickerDialogChanged: (Boolean) -> Unit,
    val onShowClockCreationDialogChanged: (Boolean) -> Unit,
    val onShowTacticalSettingsDialogChanged: (Boolean) -> Unit,
    val onIsRulerActiveChanged: (Boolean) -> Unit,
    val onRulerPositionsChanged: (Offset?, Offset?) -> Unit,
    val onStickyNoteTextChanged: (String) -> Unit,
    val onResetCamera: () -> Unit,
    val onExportSnapshot: () -> Unit
)
