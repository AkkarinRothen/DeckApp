package com.deckapp.feature.draw.components

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import com.deckapp.core.model.*
import kotlinx.coroutines.delay

@Composable
fun BoardItemRenderer(
    item: SessionBoardItem,
    uiContext: BoardItemUiContext,
    compactInFolder: Boolean = false,
    activeDragTargetId: Long? = null,
    activeMergeTargetId: Long? = null,
    onMergeTargetChange: (Long?) -> Unit = {},
    onMergeItems: (Long, Long) -> Unit = { _, _ -> },
    orderedItems: List<SessionBoardItem> = emptyList(),
    boardBounds: Map<Long, Rect> = emptyMap(),
    onOpenBlock: (SessionBoardItem) -> Unit = {},
    onRemoveBlock: (Long) -> Unit = {},
    onMoveUp: (Long) -> Unit = {},
    onMoveDown: (Long) -> Unit = {},
    onMoveTo: (Long, Long) -> Unit = { _, _ -> },
    onDragStart: () -> Unit = {},
    onDragEnd: () -> Unit = {},
    onDragTargetChange: (Long?) -> Unit = {},
    activeDragTargetGroupId: Long? = null,
    onDragTargetGroupChange: (Long?) -> Unit = {},
    groupBounds: Map<Long, Rect> = emptyMap(),
    onTogglePin: () -> Unit = {},
    onToggleCollapseBlock: () -> Unit = {},
    onUpdateDicePresets: (Long, List<DicePreset>) -> Unit = { _, _ -> },
    onAssignToGroupId: (Long?) -> Unit = {},
    onConfigClick: () -> Unit = {},
    onQuickAction: () -> Unit = {},
    onBoundsChanged: (Rect) -> Unit = {},
    dropZoneBounds: Rect? = null,
    isHoveringDropZone: Boolean = false,
    onHoverDropZoneChange: (Boolean) -> Unit = {},
    dragEnabled: Boolean = true,
    index: Int = 0,
    modifier: Modifier = Modifier
) {
    val isFastRender = index > 12
    var visible by remember(item.id, index) { mutableStateOf(isFastRender) }
    if (!isFastRender && !visible) {
        LaunchedEffect(item.id) {
            delay(index * 15L)
            visible = true
        }
    }

    val latestTableRoll = remember(item.id, item.type, item.resourceId, uiContext.recentTableRolls) {
        if (item.type == SessionBoardItemType.TABLE) {
            uiContext.recentTableRolls.firstOrNull { it.tableId == item.resourceId }
        } else {
            null
        }
    }

    val tableRecentRolls = remember(item.id, item.type, item.resourceId, uiContext.recentTableRolls) {
        if (item.type == SessionBoardItemType.TABLE) {
            uiContext.recentTableRolls.filter { it.tableId == item.resourceId }
        } else {
            emptyList()
        }
    }

    val table = remember(item.id, item.type, item.resourceId, uiContext.tablesInSession, uiContext.allTables) {
        if (item.type == SessionBoardItemType.TABLE) {
            (uiContext.tablesInSession + uiContext.allTables)
                .distinctBy { it.id }
                .firstOrNull { it.id == item.resourceId }
        } else {
            null
        }
    }

    val linkedWikiEntry = remember(item.id, item.type, uiContext.wikiEntries) {
        if (item.type == SessionBoardItemType.WIKI_MAP) {
            uiContext.wikiEntries.find { it.id == item.resourceId }
        } else null
    }

    val renderContent: @Composable (Modifier) -> Unit = { targetModifier ->
        RenderBlockCardContent(
            item = item,
            uiContext = uiContext,
            compactInFolder = compactInFolder,
            table = table,
            latestTableRoll = latestTableRoll,
            tableRecentRolls = tableRecentRolls,
            linkedWikiEntry = linkedWikiEntry,
            activeDragTargetId = activeDragTargetId,
            activeMergeTargetId = activeMergeTargetId,
            onMergeTargetChange = onMergeTargetChange,
            onMergeItems = onMergeItems,
            orderedItems = orderedItems,
            boardBounds = boardBounds,
            onOpenBlock = onOpenBlock,
            onRemoveBlock = onRemoveBlock,
            onMoveUp = onMoveUp,
            onMoveDown = onMoveDown,
            onMoveTo = onMoveTo,
            onDragStart = onDragStart,
            onDragEnd = onDragEnd,
            onDragTargetChange = onDragTargetChange,
            activeDragTargetGroupId = activeDragTargetGroupId,
            onDragTargetGroupChange = onDragTargetGroupChange,
            groupBounds = groupBounds,
            onTogglePin = onTogglePin,
            onToggleCollapseBlock = onToggleCollapseBlock,
            onUpdateDicePresets = onUpdateDicePresets,
            onAssignToGroupId = onAssignToGroupId,
            onConfigClick = onConfigClick,
            onQuickAction = onQuickAction,
            onBoundsChanged = onBoundsChanged,
            dropZoneBounds = dropZoneBounds,
            isHoveringDropZone = isHoveringDropZone,
            onHoverDropZoneChange = onHoverDropZoneChange,
            dragEnabled = dragEnabled,
            modifier = targetModifier
        )
    }

    if (isFastRender || visible) {
        if (!isFastRender) {
            androidx.compose.animation.AnimatedVisibility(
                visible = visible,
                enter = fadeIn(animationSpec = tween(300)) + 
                        slideInVertically(initialOffsetY = { it / 8 }, animationSpec = tween(400)),
                modifier = modifier
            ) {
                renderContent(Modifier)
            }
        } else {
            renderContent(modifier)
        }
    }
}

@Composable
private fun RenderBlockCardContent(
    item: SessionBoardItem,
    uiContext: BoardItemUiContext,
    compactInFolder: Boolean = false,
    table: RandomTable?,
    latestTableRoll: TableRollResult?,
    tableRecentRolls: List<TableRollResult> = emptyList(),
    linkedWikiEntry: WikiEntry?,
    activeDragTargetId: Long?,
    activeMergeTargetId: Long?,
    onMergeTargetChange: (Long?) -> Unit,
    onMergeItems: (Long, Long) -> Unit,
    orderedItems: List<SessionBoardItem>,
    boardBounds: Map<Long, Rect>,
    onOpenBlock: (SessionBoardItem) -> Unit,
    onRemoveBlock: (Long) -> Unit,
    onMoveUp: (Long) -> Unit,
    onMoveDown: (Long) -> Unit,
    onMoveTo: (Long, Long) -> Unit,
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit,
    onDragTargetChange: (Long?) -> Unit,
    activeDragTargetGroupId: Long?,
    onDragTargetGroupChange: (Long?) -> Unit,
    groupBounds: Map<Long, Rect>,
    onTogglePin: () -> Unit,
    onToggleCollapseBlock: () -> Unit,
    onUpdateDicePresets: (Long, List<DicePreset>) -> Unit,
    onAssignToGroupId: (Long?) -> Unit,
    onConfigClick: () -> Unit,
    onQuickAction: () -> Unit,
    onBoundsChanged: (Rect) -> Unit,
    dropZoneBounds: Rect?,
    isHoveringDropZone: Boolean,
    onHoverDropZoneChange: (Boolean) -> Unit,
    dragEnabled: Boolean,
    modifier: Modifier = Modifier
) {
        BoardBlockCard(
            item = item,
            boardGroups = uiContext.boardGroups,
            tableName = table?.name,
            tableCategory = table?.category,
            tableFormula = table?.rollFormula,
            randomTable = table,
            latestTableRoll = latestTableRoll,
            tableRecentRolls = tableRecentRolls,
            recentDiceRolls = uiContext.recentDiceRolls,
            notesPreview = item.noteContent ?: uiContext.notesPreview,
            notesContent = item.noteContent ?: uiContext.notesContent,
            isSavingNotes = item.noteContent == null && uiContext.isSavingNotes,
            onUpdateNotes = { content ->
                if (item.noteContent == null) uiContext.onUpdateNotes(content)
                else uiContext.onUpdateBlockNote(item.id, content)
            },
            npcCount = uiContext.npcCount,
            activeEncounter = uiContext.activeEncounter,
            playerParticipants = uiContext.playerParticipants,
            permanentPlayers = uiContext.permanentPlayers,
            onApplyDamage = uiContext.onApplyDamage,
            onNextTurn = uiContext.onNextTurn,
            onEndEncounter = uiContext.onEndEncounter,
            onAddPlayer = uiContext.onAddPlayer,
            onRemovePlayer = uiContext.onRemovePlayer,
            onSavePermanentPlayer = uiContext.onSavePermanentPlayer,
            onDeletePermanentPlayer = uiContext.onDeletePermanentPlayer,
            wikiEntryCount = uiContext.wikiEntryCount,
            referenceFocusTitle = uiContext.referenceFocusTitle,
            referencePreview = uiContext.referencePreview,
            referencePinnedCount = uiContext.referencePinnedCount,
            referenceSearchQuery = uiContext.referenceSearchQuery,
            referenceSearchCount = uiContext.referenceSearchCount,
            referenceSearchPreview = uiContext.referenceSearchPreview,
            linkedMapName = uiContext.linkedMapName,
            linkedMapTileCount = uiContext.linkedMapTileCount,
            linkedMapPoiCount = uiContext.linkedMapPoiCount,
            linkedMapDayNumber = uiContext.linkedMapDayNumber,
            linkedMapPartyLocation = uiContext.linkedMapPartyLocation,
            linkedMythicSession = uiContext.linkedMythicSession,
            mythicState = uiContext.mythicState,
            linkedHexMapId = uiContext.linkedHexMapId,
            hexTiles = uiContext.hexTiles,
            hexPois = uiContext.hexPois,
            partyLocation = uiContext.partyLocation,
            onMoveParty = uiContext.onMoveParty,
            onExploreTile = uiContext.onExploreTile,
            onReconnoiterTile = uiContext.onReconnoiterTile,
            onMapTile = uiContext.onMapTile,
            locationName = linkedWikiEntry?.title,
            locationImagePath = linkedWikiEntry?.imagePath,
            locationMapPins = emptyList<MapPin>(),
            npcs = uiContext.npcs,
            wikiEntries = uiContext.wikiEntries,
            onUpdateWikiEntry = uiContext.onUpdateWikiEntry,
            tensionClocks = uiContext.tensionClocks,
            deckNames = uiContext.deckNames,
            handByDeck = uiContext.handByDeck,
            deckCardCounts = uiContext.deckCardCounts,
            deckAspectRatios = uiContext.deckAspectRatios,
            deckBackImages = uiContext.deckBackImages,
            onDrawCardFromDeck = uiContext.onDrawCardFromDeck,
            onShuffleDeck = uiContext.onShuffleDeck,
            onShuffleOnly = uiContext.onShuffleOnly,
            onShuffleDiscards = uiContext.onShuffleDiscards,
            piledByDeck = uiContext.piledByDeck,
            onResetDeck = uiContext.onResetDeck,
            onDiscardCard = uiContext.onDiscardCard,
            onRevealCard = uiContext.onRevealCard,
            onCardClick = uiContext.onCardClick,
            sessionId = uiContext.sessionId,
            onTickClock = uiContext.onTickClock,
            onUpdateClockDescriptionAndProgress = uiContext.onUpdateClockDescriptionAndProgress,
            isEditing = uiContext.isEditing,
            compactInFolder = compactInFolder,
            isDragTarget = activeDragTargetId == item.id,
            isMergeTarget = activeMergeTargetId == item.id,
            npcNames = uiContext.npcNames,
            tableNames = uiContext.tableNames,
            onOpen = { onOpenBlock(item) },
            onRemove = { onRemoveBlock(item.id) },
            onConfigClick = onConfigClick,
            onQuickAction = onQuickAction,
            onTogglePin = onTogglePin,
            onToggleCollapse = onToggleCollapseBlock,
            onTagClicked = uiContext.onTagClicked,
            onDiceClicked = uiContext.onDiceClicked,
            onUpdateNpcHp = uiContext.onUpdateNpcHp,
            onToggleCondition = uiContext.onToggleCondition,
            onNpcClick = uiContext.onNpcClick,
            onUpdateDicePresets = onUpdateDicePresets,
            boardMythicChaos = uiContext.boardMythicChaos,
            boardMythicProbability = uiContext.boardMythicProbability,
            boardMythicLastResult = uiContext.boardMythicLastResult,
            boardMythicHistory = uiContext.boardMythicHistory,
            boardMythicLastEvent = uiContext.boardMythicLastEvent,
            onSetBoardMythicProbability = uiContext.onSetBoardMythicProbability,
            onUpdateBoardMythicChaos = uiContext.onUpdateBoardMythicChaos,
            onRollBoardMythicFate = uiContext.onRollBoardMythicFate,
            onAskBoardMythicQuestion = uiContext.onAskBoardMythicQuestion,
            onRollBoardMythicEvent = uiContext.onRollBoardMythicEvent,
            onAppendToNotes = uiContext.onAppendToNotes,
            onGenerateMicrosetting = uiContext.onGenerateMicrosetting,
            onRotateMicrosettingSlot = uiContext.onRotateMicrosettingSlot,
            onRedrawMicrosettingSlot = uiContext.onRedrawMicrosettingSlot,
            onMoveUp = { onMoveUp(item.id) },
            onMoveDown = { onMoveDown(item.id) },
            onDragStart = onDragStart,
            onDragMove = { sourceId, draggedCenter ->
                val isOverDropZone = dropZoneBounds?.contains(draggedCenter) == true
                if (isOverDropZone) {
                    if (!isHoveringDropZone) {
                        onHoverDropZoneChange(true)
                    }
                    if (activeMergeTargetId != null) onMergeTargetChange(null)
                    if (activeDragTargetId != null) onDragTargetChange(null)
                    if (activeDragTargetGroupId != null) onDragTargetGroupChange(null)
                } else {
                    if (isHoveringDropZone) {
                        onHoverDropZoneChange(false)
                    }
                    val targetId = orderedItems
                        .firstOrNull { candidate ->
                            candidate.id != sourceId && boardBounds[candidate.id]?.contains(draggedCenter) == true
                        }
                        ?.id

                    val targetBounds = targetId?.let { boardBounds[it] }
                    val isMerging = targetBounds != null && targetBounds.center40Percent().contains(draggedCenter)

                    if (isMerging) {
                        if (targetId != activeMergeTargetId) {
                            onMergeTargetChange(targetId)
                        }
                    } else {
                        if (activeMergeTargetId != null) {
                            onMergeTargetChange(null)
                        }

                        if (targetId != null && targetId != activeDragTargetId) {
                            onMoveTo(sourceId, targetId)
                            onDragTargetChange(targetId)
                            onDragTargetGroupChange(null)
                        } else if (targetId == null) {
                            onDragTargetChange(null)
                            val targetGroupId = groupBounds.entries
                                .firstOrNull { entry ->
                                    val isSmart = uiContext.boardGroups.find { it.id == entry.key }?.autoFilterType != null
                                    !isSmart && entry.value.contains(draggedCenter)
                                }
                                ?.key

                            if (targetGroupId != null && targetGroupId != activeDragTargetGroupId) {
                                onDragTargetGroupChange(targetGroupId)
                            } else if (targetGroupId == null && activeDragTargetGroupId != null) {
                                onDragTargetGroupChange(null)
                            }
                        }
                    }
                }
            },
            onDragEnd = {
                if (isHoveringDropZone) {
                    onAssignToGroupId(null)
                    onHoverDropZoneChange(false)
                } else if (activeMergeTargetId != null) {
                    onMergeItems(item.id, activeMergeTargetId!!)
                    onMergeTargetChange(null)
                } else if (activeDragTargetGroupId != null) {
                    onAssignToGroupId(activeDragTargetGroupId)
                    onDragTargetGroupChange(null)
                }
                onDragEnd()
            },
            onRollTable = uiContext.onRollTable,
            pinnedTableRoll = table?.id?.let { uiContext.pinnedTableRolls[it] },
            onPinTableRoll = { roll -> table?.id?.let { uiContext.onPinTableRoll(it, roll) } },
            onUnpinTableRoll = { table?.id?.let { uiContext.onUnpinTableRoll(it) } },
            spreadResult = item.resourceId?.let { uiContext.spreadResultsByDeck[it] },
            onDrawSpread = { spread -> item.resourceId?.let { uiContext.onDrawSpread(it, spread) } },
            onClearSpread = { item.resourceId?.let { uiContext.onClearSpread(it) } },
            onSaveSpreadToNotes = uiContext.onSaveSpreadToNotes,
            onBoundsChanged = onBoundsChanged,
            dragEnabled = dragEnabled
        )
    }

private fun Rect.center40Percent(): Rect {
    val cx = left + width / 2f
    val cy = top + height / 2f
    val innerW = width * 0.40f
    val innerH = height * 0.40f
    return Rect(cx - innerW / 2f, cy - innerH / 2f, cx + innerW / 2f, cy + innerH / 2f)
}
