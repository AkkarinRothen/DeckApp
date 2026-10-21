package com.deckapp.feature.draw.model.state

import com.deckapp.core.model.*
import com.deckapp.feature.draw.delegates.PlaymatDelegate

/**
 * Contrato agrupado de acciones y callbacks para el Tapete 2D (Playmat).
 * Elimina el prop-drilling de más de 40 lambdas individuales en SessionScreen y PlaymatCanvasView.
 */
data class PlaymatActions(
    val onPlaceCardOnPlaymat: (Long, Float, Float) -> Unit = { _, _, _ -> },
    val onMoveCard: (Long, Float, Float, String?) -> Unit = { _, _, _, _ -> },
    val onRotateCard: (Long, Int) -> Unit = { _, _ -> },
    val onFlipCard: (Long) -> Unit = {},
    val onReturnToHand: (Long) -> Unit = {},
    val onDiscardCard: (Long) -> Unit = {},
    val onSelectCard: (Long?) -> Unit = {},
    val onToggleHandTray: () -> Unit = {},
    val onSelectSurface: (String) -> Unit = {},
    val onCreateSurface: (String, PlaymatTheme, PlaymatTemplateType) -> Unit = { _, _, _ -> },
    val onDeleteSurface: (String) -> Unit = {},
    val onSetTheme: (PlaymatTheme) -> Unit = {},
    val onSetTemplate: (PlaymatTemplateType) -> Unit = {},
    val onUpdateCamera: (Float, Float, Float) -> Unit = { _, _, _ -> },
    val onQuickDraw: (Long, Float, Float) -> Unit = { _, _, _ -> },
    val onAddStickyNote: (String, Float, Float) -> Unit = { _, _, _ -> },
    val onMoveStickyNote: (String, Float, Float) -> Unit = { _, _, _ -> },
    val onDeleteStickyNote: (String) -> Unit = {},
    val onCardDetailClick: (Long) -> Unit = {},
    val onSendCardToTacticalMap: (Card, Boolean) -> Unit = { _, _ -> },
    val onMoveDeck: (Long, Float, Float) -> Unit = { _, _, _ -> },
    val onMoveDiscardPile: (Long, Float, Float) -> Unit = { _, _, _ -> },
    val onDrawFromDeck: (Long, Float, Float, String?) -> Unit = { _, _, _, _ -> },
    val onReshuffleDeck: (Long) -> Unit = {},
    val onAddTableChip: (Long, String, String, Float, Float) -> Unit = { _, _, _, _, _ -> },
    val onCreateConnection: (String, String) -> Unit = { _, _ -> },
    val onUpdateConnection: (String, String, String) -> Unit = { _, _, _ -> },
    val onRemoveConnection: (String) -> Unit = {},
    val onAddToken: (name: String, imagePath: String?, npcId: Long?, x: Float, y: Float, maxHp: Int, currentHp: Int, tokenType: PlaymatTokenType, ringColorHex: String) -> Unit = { _, _, _, _, _, _, _, _, _ -> },
    val onMoveToken: (String, Float, Float) -> Unit = { _, _, _ -> },
    val onUpdateTokenHp: (String, Int) -> Unit = { _, _ -> },
    val onUpdateTokenType: (String, PlaymatTokenType) -> Unit = { _, _ -> },
    val onRemoveToken: (String) -> Unit = {},
    val onAddClock: (title: String, segments: Int, colorHex: String, x: Float, y: Float) -> Unit = { _, _, _, _, _ -> },
    val onUpdateClockProgress: (clockId: String, filledSegments: Int) -> Unit = { _, _ -> },
    val onMoveClock: (clockId: String, x: Float, y: Float) -> Unit = { _, _, _ -> },
    val onRemoveClock: (clockId: String) -> Unit = {},
    val onSetBackgroundMap: (uri: String?, preset: PlaymatAtmosphericPreset) -> Unit = { _, _ -> },
    val onSetBackgroundTransform: (opacity: Float, scale: Float) -> Unit = { _, _ -> },
    val onToggleBackgroundLock: () -> Unit = {},
    val onSetGridConfig: (gridType: PlaymatGridType, gridStyle: PlaymatGridStyle, cellSizeDp: Float, snapToGrid: Boolean) -> Unit = { _, _, _, _ -> },
    val onToggleZenMode: () -> Unit = {},
    val onToggleSessionDrawer: () -> Unit = {},
    val onSetSessionDrawerTab: (Int) -> Unit = {},
    val onAddManualLog: (String) -> Unit = {},
    val onSaveNotes: (String) -> Unit = {},
    val onClearLogs: () -> Unit = {},
    val onToggleFilterCurrentScene: () -> Unit = {}
)

/**
 * Conecta cómodamente un [PlaymatDelegate] a un contrato de [PlaymatActions].
 */
fun PlaymatDelegate.toActions(
    activeSurfaceIdProvider: () -> String,
    sessionLogDelegate: com.deckapp.feature.draw.delegates.SessionLogDelegate? = null,
    onCardDetailClick: (Long) -> Unit = {},
    onSendCardToTacticalMap: (Card, Boolean) -> Unit = { _, _ -> }
): PlaymatActions = PlaymatActions(
    onPlaceCardOnPlaymat = { cardId, x, y -> placeCardOnPlaymat(cardId, x, y) },
    onMoveCard = { cardId, x, y, slotTag -> moveCard(cardId, x, y, slotTag) },
    onRotateCard = { cardId, delta ->
        rotateCard(cardId, delta)
        sessionLogDelegate?.logCardAction("Carta", "Rotada $delta°")
    },
    onFlipCard = { cardId ->
        flipCard(cardId)
        sessionLogDelegate?.logCardAction("Carta", "Volteada")
    },
    onReturnToHand = { cardId ->
        returnCardToHand(cardId)
        sessionLogDelegate?.logCardAction("Carta", "Devuelta a la mano")
    },
    onDiscardCard = { cardId ->
        discardCard(cardId)
        sessionLogDelegate?.logCardAction("Carta", "Descartada")
    },
    onSelectCard = { cardId -> selectCard(cardId) },
    onToggleHandTray = { toggleHandTray() },
    onSelectSurface = { surfaceId -> switchSurface(surfaceId) },
    onCreateSurface = { name, theme, template -> createSurface(name, theme, template) },
    onDeleteSurface = { surfaceId -> deleteSurface(surfaceId) },
    onSetTheme = { theme -> setSurfaceTheme(activeSurfaceIdProvider(), theme) },
    onSetTemplate = { template -> setSurfaceTemplate(activeSurfaceIdProvider(), template) },
    onUpdateCamera = { x, y, zoom -> updateCamera(x, y, zoom) },
    onQuickDraw = { stackId, x, y ->
        quickDrawToPlaymat(stackId, x, y)
        sessionLogDelegate?.logCardAction("Mazo", "Robo rápido al tapete")
    },
    onAddStickyNote = { text, x, y -> addStickyNote(text, x, y) },
    onMoveStickyNote = { noteId, x, y -> moveStickyNote(noteId, x, y) },
    onDeleteStickyNote = { noteId -> deleteStickyNote(noteId) },
    onCardDetailClick = onCardDetailClick,
    onSendCardToTacticalMap = onSendCardToTacticalMap,
    onMoveDeck = { stackId, x, y -> moveDeck(stackId, x, y) },
    onMoveDiscardPile = { stackId, x, y -> moveDiscardPile(stackId, x, y) },
    onDrawFromDeck = { stackId, x, y, slotTag ->
        drawFromPhysicalDeck(stackId, x, y, true, slotTag)
        sessionLogDelegate?.logCardAction("Mazo", "Carta colocada en mesa")
    },
    onReshuffleDeck = { stackId ->
        reshuffleDeck(stackId)
        sessionLogDelegate?.logCardAction("Mazo", "Mazo barajado")
    },
    onAddTableChip = { tableId, name, resultText, x, y -> addTableChip(tableId, name, resultText, x, y) },
    onCreateConnection = { fromId, toId -> createConnection(fromId, toId) },
    onUpdateConnection = { id, label, color -> updateConnection(id, label, color) },
    onRemoveConnection = { id -> removeConnection(id) },
    onAddToken = { name, imagePath, npcId, x, y, maxHp, currentHp, tokenType, ringColor ->
        addTokenToPlaymat(name, imagePath, npcId, x, y, maxHp, currentHp, tokenType, ringColor)
        sessionLogDelegate?.logTokenHpChange(name, 0, currentHp, maxHp)
    },
    onMoveToken = { tokenId, x, y -> moveToken(tokenId, x, y) },
    onUpdateTokenHp = { tokenId, hp ->
        updateTokenHp(tokenId, hp)
        sessionLogDelegate?.logTokenHpChange("Token", 0, hp, hp)
    },
    onUpdateTokenType = { tokenId, type -> updateTokenType(tokenId, type) },
    onRemoveToken = { tokenId -> removeToken(tokenId) },
    onAddClock = { title, segments, colorHex, x, y -> addClockToPlaymat(title, segments, x, y, colorHex) },
    onUpdateClockProgress = { clockId, progress ->
        updateClockProgress(clockId, progress)
        sessionLogDelegate?.logClockStep("Reloj", progress, progress)
    },
    onMoveClock = { clockId, x, y -> moveClock(clockId, x, y) },
    onRemoveClock = { clockId -> removeClock(clockId) },
    onSetBackgroundMap = { uri, preset -> setBackgroundMap(uri, preset) },
    onSetBackgroundTransform = { opacity, scale -> setBackgroundTransform(opacity = opacity, scale = scale) },
    onToggleBackgroundLock = { toggleBackgroundLock() },
    onSetGridConfig = { gridType, gridStyle, cellSizeDp, snapToGrid ->
        setGridConfig(gridType, gridStyle, cellSizeDp, snapToGrid)
    },
    onToggleZenMode = { sessionLogDelegate?.toggleZenMode() },
    onToggleSessionDrawer = { sessionLogDelegate?.toggleDrawer() },
    onSetSessionDrawerTab = { tab -> sessionLogDelegate?.setDrawerTab(tab) },
    onAddManualLog = { text -> sessionLogDelegate?.addManualNote(text) },
    onSaveNotes = { md -> sessionLogDelegate?.saveSceneNote(md) },
    onClearLogs = { sessionLogDelegate?.clearLogs() },
    onToggleFilterCurrentScene = { sessionLogDelegate?.toggleFilterCurrentScene() }
)
