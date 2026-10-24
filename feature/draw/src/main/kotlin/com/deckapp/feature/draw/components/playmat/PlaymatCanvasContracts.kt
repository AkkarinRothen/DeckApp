package com.deckapp.feature.draw.components.playmat

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.deckapp.core.model.Card
import com.deckapp.core.model.CardAspectRatio
import com.deckapp.core.model.PlaymatConnection
import com.deckapp.core.model.PlaymatCardPlacement
import com.deckapp.core.model.PlaymatSurface
import com.deckapp.core.model.PlaymatTokenPlacement
import com.deckapp.core.model.Npc
import com.deckapp.core.model.SessionDeckRef
import com.deckapp.core.model.SessionLogEntry
import com.deckapp.feature.draw.model.state.PlaymatActions

/** Values used by the spatial scene. Kept separate from transient overlay and dialog state. */
internal data class PlaymatSceneState(
    val activeSurface: PlaymatSurface,
    val cameraX: Float,
    val cameraY: Float,
    val cameraZoom: Float,
    val deckNames: Map<Long, String>,
    val deckCardCounts: Map<Long, Int>,
    val deckBackImages: Map<Long, String?>,
    val selectedPlaymatCardId: Long?,
    val selectedTokenId: String?,
    val selectedClockId: String?,
    val hoveredSlotTag: String?,
    val connectingSourceEndpointId: String?,
    val cardsById: Map<Long, Card>,
    val endpointPositions: Map<String, Offset>,
    val piledCardsByDeck: Map<Long, List<Card>>,
    val currentTemplateSlots: List<TemplateSlotDef>,
    val actions: PlaymatActions
)

/** Values used by floating controls and session panels. */
internal data class PlaymatOverlayState(
    val activeSurface: PlaymatSurface,
    val allSurfaces: List<PlaymatSurface>,
    val cameraX: Float,
    val cameraY: Float,
    val cameraZoom: Float,
    val canvasBoundsInWindow: Rect,
    val handTrayBoundsInWindow: Rect,
    val handCardDrag: PlaymatHandCardDrag?,
    val isZenMode: Boolean,
    val showDiceOverlay: Boolean,
    val isSessionDrawerOpen: Boolean,
    val sessionDrawerTab: Int,
    val hand: List<Card>,
    val showPlaymatHandTray: Boolean,
    val deckRefs: List<SessionDeckRef>,
    val deckNames: Map<Long, String>,
    val deckCardCounts: Map<Long, Int>,
    val deckAspectRatios: Map<Long, CardAspectRatio>,
    val deckBackImages: Map<Long, String?>,
    val sessionId: Long,
    val sessionLogs: List<SessionLogEntry>,
    val notesMarkdown: String,
    val isFilterCurrentScene: Boolean,
    val currentSceneTitle: String?,
    val isRulerActive: Boolean,
    val rulerStartPos: Offset?,
    val rulerCurrentPos: Offset?,
    val tokenDragStartPos: Offset?,
    val tokenDragCurrentPos: Offset?,
    val selectedTokenId: String?,
    val selectedClockId: String?,
    val connectingSourceEndpointId: String?,
    val cardsById: Map<Long, Card>,
    val placedCardIds: Set<Long>,
    val currentTemplateSlots: List<TemplateSlotDef>,
    val selectedCardPlacement: PlaymatCardPlacement?,
    val selectedCard: Card?,
    val isGroupSelectionMode: Boolean,
    val selectedGroupMemberIds: Set<String>,
    val selectedGroupMemberCount: Int,
    val onCenterGroup: (String) -> Unit,
    val onCenterAtWorld: (Float, Float) -> Unit,
    val actions: PlaymatActions
)

/** Values consumed by conditional dialogs, isolated from scene and overlay state. */
internal data class PlaymatDialogState(
    val activeSurface: PlaymatSurface,
    val cameraX: Float,
    val cameraY: Float,
    val cameraZoom: Float,
    val deckRefs: List<SessionDeckRef>,
    val deckNames: Map<Long, String>,
    val deckCardCounts: Map<Long, Int>,
    val npcs: List<Npc>,
    val showQuickDrawDialog: Boolean,
    val showStickyNoteDialog: Boolean,
    val showSendToTacticalDialog: Boolean,
    val showTokenPickerDialog: Boolean,
    val showClockCreationDialog: Boolean,
    val showTacticalSettingsDialog: Boolean,
    val editingHpToken: PlaymatTokenPlacement?,
    val stickyNoteText: String,
    val editingConnection: PlaymatConnection?,
    val selectedCard: Card?,
    val actions: PlaymatActions
)

/** Events let each stateless layer update state owned by PlaymatCanvasView. */
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
    val onExportSnapshot: () -> Unit,
    val onHandCardDragChanged: (PlaymatHandCardDrag?) -> Unit,
    val onHandTrayBoundsChanged: (Rect) -> Unit,
    val onGroupSelectionModeChanged: (Boolean) -> Unit,
    val onGroupSelectionFinished: () -> Unit
)

/** Temporary pointer state used to render and place a card dragged out of the hand tray. */
internal data class PlaymatHandCardDrag(
    val cardId: Long,
    val pointerInWindow: Offset,
    val grabFraction: Offset
)
