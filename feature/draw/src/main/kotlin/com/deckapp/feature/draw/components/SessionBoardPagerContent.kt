package com.deckapp.feature.draw.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed as gridItemsIndexed
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.dp
import com.deckapp.core.model.BoardMythicFateResult
import com.deckapp.core.model.Card
import com.deckapp.core.model.CardAspectRatio
import com.deckapp.core.model.DiceRollResult
import com.deckapp.core.model.Encounter
import com.deckapp.core.model.HexPoi
import com.deckapp.core.model.HexTile
import com.deckapp.core.model.MythicSession
import com.deckapp.core.model.Npc
import com.deckapp.core.model.PlayerInitiativeParticipant
import com.deckapp.core.model.ProbabilityLevel
import com.deckapp.core.model.RandomTable
import com.deckapp.core.model.SessionBoardGroup
import com.deckapp.core.model.SessionBoardItem
import com.deckapp.core.model.SessionBoardItemSize
import com.deckapp.core.model.SessionBoardItemType
import com.deckapp.core.model.SessionBoardView
import com.deckapp.core.model.SessionBoardViewLabels
import com.deckapp.core.model.SessionDeckRef
import com.deckapp.core.model.TableRollResult
import com.deckapp.core.model.TensionClock
import com.deckapp.core.model.WikiEntry
import com.deckapp.feature.mythic.MythicSessionUiState
import kotlinx.coroutines.delay
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Composable
internal fun SessionBoardPagerContent(
    orderedViews: List<SessionBoardView>,
    pagerState: PagerState,
    activeBoardView: SessionBoardView,
    onSetActiveBoardView: (SessionBoardView) -> Unit,
    boardItems: List<SessionBoardItem>,
    orderedItems: List<SessionBoardItem>,
    boardGroups: List<SessionBoardGroup>,
    viewLabels: SessionBoardViewLabels,
    tablesInSession: List<RandomTable>,
    allTables: List<RandomTable>,
    recentTableRolls: List<TableRollResult>,
    recentDiceRolls: List<DiceRollResult>,
    notesPreview: String,
    notesContent: String,
    isSavingNotes: Boolean,
    onUpdateNotes: (String) -> Unit,
    npcCount: Int,
    npcs: List<Npc>,
    activeEncounter: Encounter?,
    playerParticipants: List<PlayerInitiativeParticipant>,
    permanentPlayers: List<Npc>,
    onApplyDamage: (Long, Int) -> Unit,
    onNextTurn: () -> Unit,
    onEndEncounter: () -> Unit,
    onAddPlayer: (String, Int) -> Unit,
    onRemovePlayer: (String) -> Unit,
    onSavePermanentPlayer: (Npc) -> Unit,
    onDeletePermanentPlayer: (Npc) -> Unit,
    wikiEntryCount: Int,
    wikiEntries: List<WikiEntry>,
    referenceFocusTitle: String?,
    referencePreview: String?,
    referencePinnedCount: Int,
    referenceSearchQuery: String,
    referenceSearchCount: Int,
    referenceSearchPreview: String?,
    linkedMapName: String?,
    linkedMapTileCount: Int,
    linkedMapPoiCount: Int,
    linkedMapDayNumber: Int?,
    linkedMapPartyLocation: String?,
    linkedMythicSession: MythicSession?,
    mythicState: MythicSessionUiState?,
    linkedHexMapId: Long?,
    hexTiles: List<HexTile>,
    hexPois: List<HexPoi>,
    partyLocation: Pair<Int, Int>?,
    onMoveParty: (Int, Int) -> Unit,
    onExploreTile: (HexTile) -> Unit,
    onReconnoiterTile: (HexTile) -> Unit,
    onMapTile: (HexTile) -> Unit,
    tensionClocks: List<TensionClock>,
    deckRefs: List<SessionDeckRef>,
    deckNames: Map<Long, String>,
    handByDeck: Map<Long, List<Card>>,
    deckCardCounts: Map<Long, Int>,
    deckAspectRatios: Map<Long, CardAspectRatio>,
    deckBackImages: Map<Long, String?>,
    onDrawCardFromDeck: (Long) -> Unit,
    onShuffleDeck: (Long) -> Unit,
    onResetDeck: (Long) -> Unit,
    onDiscardCard: (Long) -> Unit,
    onRevealCard: (Long) -> Unit,
    sessionId: Long,
    isEditing: Boolean,
    focusedGroupId: Long?,
    isFocusedGroupListMode: Boolean,
    folderSearchQuery: String,
    isSidebarOpen: Boolean,
    onSelectFocusedGroup: (Long) -> Unit,
    onOpenPickerRequested: (Long?) -> Unit,
    onRenameGroupRequested: (SessionBoardGroup) -> Unit,
    onDeleteGroupRequested: (Long) -> Unit,
    onToggleGroupCollapsed: (Long, Boolean) -> Unit,
    onConfigClick: (SessionBoardItem) -> Unit,
    onQuickAction: (SessionBoardItem) -> Unit,
    onOpenBlock: (SessionBoardItem) -> Unit,
    onRemoveBlock: (Long) -> Unit,
    onMoveUp: (Long) -> Unit,
    onMoveDown: (Long) -> Unit,
    onMoveTo: (Long, Long) -> Unit,
    onSetSize: (Long, SessionBoardItemSize) -> Unit,
    onSetDisplayMode: (Long, String?) -> Unit,
    onSetTitleOverride: (Long, String?) -> Unit,
    onMergeItems: (Long, Long) -> Unit,
    onRollTable: (Long) -> Unit,
    onTogglePinBlock: (Long) -> Unit,
    onToggleCollapseBlock: (Long) -> Unit = {},
    onTagClicked: (String) -> Unit,
    onDiceClicked: (String, Boolean, Boolean, String?) -> Unit,
    onUpdateNpcHp: (Long, Int) -> Unit,
    onNpcClick: (Long) -> Unit,
    onWikiClick: (Long) -> Unit,
    onUpdateWikiEntry: (WikiEntry) -> Unit,
    onTickClock: (Long, Int) -> Unit,
    onUpdateClockDescriptionAndProgress: (Long, String, Int) -> Unit,
    onAssignToGroup: (Long, Long?) -> Unit,
    onToggleEditing: () -> Unit,
    onUpdateBoardViewLabel: (SessionBoardView, String) -> Unit,
    pinnedTableRolls: Map<Long, TableRollResult> = emptyMap(),
    onPinTableRoll: (Long, TableRollResult) -> Unit = { _, _ -> },
    onUnpinTableRoll: (Long) -> Unit = {},
    boardMythicChaos: Int = 5,
    boardMythicProbability: ProbabilityLevel = ProbabilityLevel.FIFTY_FIFTY,
    boardMythicLastResult: BoardMythicFateResult? = null,
    boardMythicHistory: List<BoardMythicFateResult> = emptyList(),
    boardMythicLastEvent: String? = null,
    onSetBoardMythicProbability: (ProbabilityLevel) -> Unit = {},
    onUpdateBoardMythicChaos: (Int) -> Unit = {},
    onRollBoardMythicFate: (ProbabilityLevel) -> Unit = {},
    onAskBoardMythicQuestion: (ProbabilityLevel, String) -> Unit = { _, _ -> },
    onRollBoardMythicEvent: () -> Unit = {},
    workspaceMemory: com.deckapp.core.model.BoardSessionMemory = com.deckapp.core.model.BoardSessionMemory(),
    onWorkspaceLayoutChanged: (SessionBoardView, com.deckapp.core.model.BoardViewMemory) -> Unit = { _, _ -> },
    workspaceActions: @Composable () -> Unit = {},
    soloSceneHeader: @Composable () -> Unit = {},
    onAppendToNotes: (String) -> Unit = {}
) {
    val boardBounds = remember { mutableStateMapOf<Long, Rect>() }
    val groupBounds = remember { mutableStateMapOf<Long, Rect>() }
    var activeDragTargetId by remember { mutableStateOf<Long?>(null) }
    var activeDragTargetGroupId by remember { mutableStateOf<Long?>(null) }
    var isDraggingAnyItem by remember { mutableStateOf(false) }

    val uiContext = remember(
        sessionId,
        isEditing,
        boardGroups,
        tablesInSession,
        allTables,
        recentTableRolls,
        pinnedTableRolls,
        onPinTableRoll,
        onUnpinTableRoll,
        recentDiceRolls,
        notesPreview,
        notesContent,
        isSavingNotes,
        npcCount,
        npcs,
        wikiEntries,
        activeEncounter,
        playerParticipants,
        permanentPlayers,
        wikiEntryCount,
        referenceFocusTitle,
        referencePreview,
        referencePinnedCount,
        referenceSearchQuery,
        referenceSearchCount,
        referenceSearchPreview,
        linkedMapName,
        linkedMapTileCount,
        linkedMapPoiCount,
        linkedMapDayNumber,
        linkedMapPartyLocation,
        linkedMythicSession,
        mythicState,
        linkedHexMapId,
        hexTiles,
        hexPois,
        partyLocation,
        tensionClocks,
        deckNames,
        handByDeck,
        deckCardCounts,
        deckAspectRatios,
        deckBackImages,
        boardMythicChaos,
        boardMythicProbability,
        onAskBoardMythicQuestion,
        boardMythicLastResult,
        boardMythicHistory,
        boardMythicLastEvent
    ) {
        BoardItemUiContext(
            sessionId = sessionId,
            isEditing = isEditing,
            boardGroups = boardGroups,
            tablesInSession = tablesInSession,
            allTables = allTables,
            recentTableRolls = recentTableRolls,
            pinnedTableRolls = pinnedTableRolls,
            onPinTableRoll = onPinTableRoll,
            onUnpinTableRoll = onUnpinTableRoll,
            recentDiceRolls = recentDiceRolls,
            notesPreview = notesPreview,
            notesContent = notesContent,
            isSavingNotes = isSavingNotes,
            npcCount = npcCount,
            npcs = npcs,
            npcNames = npcs.map { it.name },
            tableNames = (tablesInSession + allTables).map { it.name }.distinct(),
            wikiEntries = wikiEntries,
            activeEncounter = activeEncounter,
            playerParticipants = playerParticipants,
            permanentPlayers = permanentPlayers,
            wikiEntryCount = wikiEntryCount,
            referenceFocusTitle = referenceFocusTitle,
            referencePreview = referencePreview,
            referencePinnedCount = referencePinnedCount,
            referenceSearchQuery = referenceSearchQuery,
            referenceSearchCount = referenceSearchCount,
            referenceSearchPreview = referenceSearchPreview,
            linkedMapName = linkedMapName,
            linkedMapTileCount = linkedMapTileCount,
            linkedMapPoiCount = linkedMapPoiCount,
            linkedMapDayNumber = linkedMapDayNumber,
            linkedMapPartyLocation = linkedMapPartyLocation,
            linkedMythicSession = linkedMythicSession,
            mythicState = mythicState,
            linkedHexMapId = linkedHexMapId,
            hexTiles = hexTiles,
            hexPois = hexPois,
            partyLocation = partyLocation,
            tensionClocks = tensionClocks,
            deckNames = deckNames,
            handByDeck = handByDeck,
            deckCardCounts = deckCardCounts,
            deckAspectRatios = deckAspectRatios,
            deckBackImages = deckBackImages,
            onUpdateNotes = onUpdateNotes,
            onApplyDamage = onApplyDamage,
            onNextTurn = onNextTurn,
            onEndEncounter = onEndEncounter,
            onAddPlayer = onAddPlayer,
            onRemovePlayer = onRemovePlayer,
            onSavePermanentPlayer = onSavePermanentPlayer,
            onDeletePermanentPlayer = onDeletePermanentPlayer,
            onMoveParty = onMoveParty,
            onExploreTile = onExploreTile,
            onReconnoiterTile = onReconnoiterTile,
            onMapTile = onMapTile,
            onDrawCardFromDeck = onDrawCardFromDeck,
            onShuffleDeck = onShuffleDeck,
            onResetDeck = onResetDeck,
            onDiscardCard = onDiscardCard,
            onRevealCard = onRevealCard,
            onUpdateWikiEntry = onUpdateWikiEntry,
            onRollTable = onRollTable,
            onTagClicked = onTagClicked,
            onDiceClicked = onDiceClicked,
            onUpdateNpcHp = onUpdateNpcHp,
            onNpcClick = onNpcClick,
            onWikiClick = onWikiClick,
            onTickClock = onTickClock,
            onUpdateClockDescriptionAndProgress = onUpdateClockDescriptionAndProgress,
            boardMythicChaos = boardMythicChaos,
            boardMythicProbability = boardMythicProbability,
            boardMythicLastResult = boardMythicLastResult,
            boardMythicHistory = boardMythicHistory,
            boardMythicLastEvent = boardMythicLastEvent,
            onSetBoardMythicProbability = onSetBoardMythicProbability,
            onUpdateBoardMythicChaos = onUpdateBoardMythicChaos,
            onRollBoardMythicFate = onRollBoardMythicFate,
            onAskBoardMythicQuestion = onAskBoardMythicQuestion,
            onRollBoardMythicEvent = onRollBoardMythicEvent,
            onAppendToNotes = onAppendToNotes
        )
    }

    LaunchedEffect(activeBoardView) {
        val targetPage = orderedViews.indexOf(activeBoardView)
        if (pagerState.currentPage != targetPage && targetPage != -1) {
            pagerState.animateScrollToPage(targetPage)
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        val targetView = orderedViews.getOrNull(pagerState.currentPage) ?: SessionBoardView.MAIN
        if (targetView != activeBoardView) {
            onSetActiveBoardView(targetView)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        androidx.compose.foundation.pager.HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = focusedGroupId == null
        ) { pageIndex ->
            val pageView = orderedViews.getOrNull(pageIndex) ?: SessionBoardView.MAIN
            val pageItems = orderedItems.filter { it.boardView == pageView }
            val rootItems = remember(pageItems) {
                pageItems.filter { it.groupId == null }
                    .sortedWith(compareByDescending<SessionBoardItem> { it.isPinned }.thenBy { it.position })
            }
            val viewGroups = boardGroups.filter { it.boardView == pageView }.sortedBy { it.position }

            if (pageItems.isEmpty() && viewGroups.isEmpty()) {
                BoardWorkspaceLayout(
                    sessionId = sessionId,
                    view = pageView,
                    rootItems = emptyList(),
                    isEditing = isEditing,
                    isFolderOpen = focusedGroupId != null,
                    itemTitle = { "" },
                    workspaceMemory = workspaceMemory,
                    onLayoutChanged = onWorkspaceLayoutChanged,
                    workspaceActions = workspaceActions,
                    soloSceneHeader = soloSceneHeader,
                    renderBlock = { _, _ -> }
                ) { _, _ -> Box(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyBoardState(
                        isEditing = isEditing,
                        hasTables = tablesInSession.isNotEmpty() || allTables.isNotEmpty(),
                        hasMap = linkedHexMapId != null,
                        hasMythic = linkedMythicSession != null,
                        onToggleEditing = onToggleEditing,
                        onOpenPicker = { onOpenPickerRequested(null) }
                    )
                } }
            } else {
                val renderWorkspaceBlock: @Composable (SessionBoardItem, Modifier) -> Unit = { item, blockModifier ->
                    BoardItemRenderer(
                        item = item,
                        uiContext = uiContext,
                        compactInFolder = false,
                        activeDragTargetId = activeDragTargetId,
                        activeMergeTargetId = null,
                        onMergeTargetChange = { },
                        onMergeItems = onMergeItems,
                        orderedItems = orderedItems,
                        boardBounds = boardBounds,
                        onOpenBlock = onOpenBlock,
                        onRemoveBlock = onRemoveBlock,
                        onMoveUp = onMoveUp,
                        onMoveDown = onMoveDown,
                        onMoveTo = onMoveTo,
                        onDragStart = { isDraggingAnyItem = true },
                        onDragEnd = {
                            isDraggingAnyItem = false
                            activeDragTargetId = null
                            activeDragTargetGroupId = null
                        },
                        onDragTargetChange = { activeDragTargetId = it },
                        activeDragTargetGroupId = activeDragTargetGroupId,
                        onDragTargetGroupChange = { activeDragTargetGroupId = it },
                        groupBounds = groupBounds,
                        onTogglePin = { onTogglePinBlock(item.id) },
                        onToggleCollapseBlock = { onToggleCollapseBlock(item.id) },
                        onUpdateDicePresets = { itemId, presets ->
                            onSetDisplayMode(itemId, Json.encodeToString(presets))
                        },
                        onBoundsChanged = { bounds -> boardBounds[item.id] = bounds },
                        onAssignToGroupId = { onAssignToGroup(item.id, it) },
                        onConfigClick = { onConfigClick(item) },
                        onQuickAction = { onQuickAction(item) },
                        index = 0,
                        modifier = blockModifier
                    )
                }
                BoardWorkspaceLayout(
                    sessionId = sessionId,
                    view = pageView,
                    rootItems = rootItems,
                    isEditing = isEditing,
                    isFolderOpen = focusedGroupId != null,
                    itemTitle = { item ->
                        item.titleOverride?.takeIf { it.isNotBlank() } ?: when (item.type) {
                            SessionBoardItemType.TABLE -> (tablesInSession + allTables).firstOrNull { it.id == item.resourceId }?.name ?: "Tabla"
                            SessionBoardItemType.NOTES -> "Notas / Diario"
                            SessionBoardItemType.COMBAT -> "Combate"
                            SessionBoardItemType.MYTHIC -> "Oráculo Mythic"
                            SessionBoardItemType.DICE -> "Dados"
                            SessionBoardItemType.CLOCK -> tensionClocks.firstOrNull { it.id == item.resourceId }?.name ?: "Reloj"
                            SessionBoardItemType.NPCS -> "PNJs"
                            SessionBoardItemType.DECK -> deckNames[item.resourceId] ?: "Mazo"
                            SessionBoardItemType.WIKI_MAP -> wikiEntries.firstOrNull { it.id == item.resourceId }?.title ?: "Lugar"
                            SessionBoardItemType.MAP -> linkedMapName ?: "Mapa"
                            SessionBoardItemType.REFERENCE -> "Referencias"
                        }
                    },
                    workspaceMemory = workspaceMemory,
                    onLayoutChanged = onWorkspaceLayoutChanged,
                    workspaceActions = workspaceActions,
                    soloSceneHeader = soloSceneHeader,
                    renderBlock = renderWorkspaceBlock
                ) { columnsCount, workspaceRootItems ->
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(columnsCount),
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 120.dp)
                    ) {
                        if (focusedGroupId == null) {
                            gridItemsIndexed(
                                items = workspaceRootItems,
                                key = { index, item -> if (item.id != 0L) "item_${item.id}" else "item_${item.type}_${item.resourceId}_$index" },
                                span = { _, item ->
                                    val itemSpan = when (item.size) {
                                        SessionBoardItemSize.L -> columnsCount
                                        SessionBoardItemSize.M -> minOf(2, columnsCount)
                                        SessionBoardItemSize.S -> 1
                                    }
                                    GridItemSpan(itemSpan)
                                }
                            ) { index, item ->
                                renderWorkspaceBlock(item, Modifier.animateItem())
                            }

                            gridItemsIndexed(
                                items = viewGroups,
                                key = { index, item -> if (item.id != 0L) "folder_${item.id}" else "folder_${index}_${item.name}" },
                                span = { _, _ -> GridItemSpan(1) }
                            ) { index, group ->
                                val groupItems = if (group.autoFilterType != null) {
                                    pageItems.filter { it.type.name == group.autoFilterType }
                                } else {
                                    pageItems.filter { it.groupId == group.id }
                                }
                                val hasRecentActivity = recentTableRolls.any { roll ->
                                    groupItems.any { it.type == SessionBoardItemType.TABLE && it.resourceId == roll.tableId }
                                }

                                val isFastRender = (rootItems.size + index) > 12
                                var visible by remember(group.id) { mutableStateOf(isFastRender) }
                                if (!isFastRender && !visible) {
                                    LaunchedEffect(group.id) {
                                        delay((rootItems.size + index) * 15L)
                                        visible = true
                                    }
                                }

                                if (visible) {
                                    BoardGroupCard(
                                        group = group,
                                        itemCount = groupItems.size,
                                        previewLines = buildBoardGroupPreviewLines(
                                            items = groupItems,
                                            tablesInSession = tablesInSession,
                                            allTables = allTables,
                                            wikiEntries = wikiEntries
                                        ),
                                        isEditing = isEditing,
                                        isDragTarget = activeDragTargetGroupId == group.id && group.autoFilterType == null,
                                        hasRecentActivity = hasRecentActivity,
                                        onClick = { onSelectFocusedGroup(group.id) },
                                        onQuickAdd = { onOpenPickerRequested(group.id) },
                                        onToggleCollapsed = { onToggleGroupCollapsed(group.id, !group.isCollapsed) },
                                        onRename = { onRenameGroupRequested(group) },
                                        onDelete = { onDeleteGroupRequested(group.id) },
                                        onBoundsChanged = { bounds -> groupBounds[group.id] = bounds },
                                        modifier = Modifier.animateItem()
                                    )
                                }
                            }

                            if (isEditing && pageView == activeBoardView) {
                                item(span = { GridItemSpan(1) }) {
                                    AddBoardPlaceholder(
                                        onOpenPicker = { onOpenPickerRequested(null) }
                                    )
                                }
                            }
                        } else if (pageView == activeBoardView) {
                            val focusedGroup = boardGroups.find { it.id == focusedGroupId }
                            val isSmartFolder = focusedGroup?.autoFilterType != null
                            val rawGroupItems = if (isSmartFolder) {
                                pageItems.filter { it.type.name == focusedGroup?.autoFilterType }
                            } else {
                                pageItems.filter { it.groupId == focusedGroupId }
                            }
                            val groupItems = rawGroupItems.filter { item ->
                                val matchesSearch = if (folderSearchQuery.isBlank()) {
                                    true
                                } else {
                                    val title = when (item.type) {
                                        SessionBoardItemType.TABLE -> {
                                            val table = (tablesInSession + allTables).firstOrNull { it.id == item.resourceId }
                                            item.titleOverride ?: table?.name ?: "Tabla"
                                        }
                                        SessionBoardItemType.CLOCK -> item.titleOverride ?: "Reloj de tensión"
                                        SessionBoardItemType.NOTES -> "Notas de sesión"
                                        SessionBoardItemType.NPCS -> "NPCs"
                                        SessionBoardItemType.COMBAT -> "Combate"
                                        SessionBoardItemType.MAP -> "Mapa"
                                        SessionBoardItemType.MYTHIC -> "Mythic"
                                        SessionBoardItemType.WIKI_MAP -> wikiEntries.find { it.id == item.resourceId }?.title ?: "Mapa"
                                        SessionBoardItemType.DECK -> {
                                            val deckName = deckNames[item.resourceId]
                                            item.titleOverride ?: deckName ?: "Mazo de cartas"
                                        }
                                        else -> item.titleOverride ?: "Bloque"
                                    }
                                    title.contains(folderSearchQuery, ignoreCase = true)
                                }
                                matchesSearch
                            }.sortedBy { it.position }

                            gridItemsIndexed(
                                items = groupItems,
                                key = { index, item -> if (item.id != 0L) "group_item_${item.id}" else "group_item_${item.type}_${item.resourceId}_$index" },
                                span = { _, item ->
                                    val itemSpan = when {
                                        !isEditing && isFocusedGroupListMode -> columnsCount
                                        item.size == SessionBoardItemSize.L -> columnsCount
                                        item.size == SessionBoardItemSize.M -> minOf(2, columnsCount)
                                        else -> 1
                                    }
                                    GridItemSpan(itemSpan)
                                }
                            ) { index, item ->
                                BoardItemRenderer(
                                    item = item,
                                    uiContext = uiContext,
                                    compactInFolder = isFocusedGroupListMode,
                                    activeDragTargetId = activeDragTargetId,
                                    activeMergeTargetId = null,
                                    onMergeTargetChange = { },
                                    onMergeItems = onMergeItems,
                                    orderedItems = orderedItems,
                                    boardBounds = boardBounds,
                                    onOpenBlock = onOpenBlock,
                                    onRemoveBlock = onRemoveBlock,
                                    onMoveUp = onMoveUp,
                                    onMoveDown = onMoveDown,
                                    onMoveTo = onMoveTo,
                                    onDragStart = { isDraggingAnyItem = true },
                                    onDragEnd = {
                                        isDraggingAnyItem = false
                                        activeDragTargetId = null
                                        activeDragTargetGroupId = null
                                    },
                                    onDragTargetChange = { activeDragTargetId = it },
                                    activeDragTargetGroupId = activeDragTargetGroupId,
                                    onDragTargetGroupChange = { activeDragTargetGroupId = it },
                                    groupBounds = groupBounds,
                                    onTogglePin = { onTogglePinBlock(item.id) },
                                    onUpdateDicePresets = { itemId, presets ->
                                        onSetDisplayMode(itemId, Json.encodeToString(presets))
                                    },
                                    onBoundsChanged = { bounds -> boardBounds[item.id] = bounds },
                                    onAssignToGroupId = { onAssignToGroup(item.id, it) },
                                    onConfigClick = { onConfigClick(item) },
                                    onQuickAction = { onQuickAction(item) },
                                    index = index,
                                    modifier = Modifier.animateItem()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

