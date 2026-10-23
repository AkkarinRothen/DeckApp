@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.deckapp.feature.draw.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.deckapp.core.model.*
import com.deckapp.feature.draw.SessionBoardTemplatePreset
import com.deckapp.feature.mythic.MythicSessionUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SessionBoardTab(
    boardItems: List<SessionBoardItem>,
    fullBoardItems: List<SessionBoardItem> = boardItems,
    boardGroups: List<SessionBoardGroup>,
    activeBoardView: SessionBoardView,
    viewLabels: SessionBoardViewLabels,
    tablesInSession: List<RandomTable>,
    allTables: List<RandomTable>,
    recentTableRolls: List<TableRollResult>,
    recentDiceRolls: List<DiceRollResult> = emptyList(),
    notesPreview: String = "",
    notesContent: String = "",
    isSavingNotes: Boolean = false,
    onUpdateNotes: (String) -> Unit = {},
    npcCount: Int = 0,
    npcs: List<Npc> = emptyList(),
    activeEncounter: Encounter? = null,
    playerParticipants: List<PlayerInitiativeParticipant> = emptyList(),
    permanentPlayers: List<Npc> = emptyList(),
    onApplyDamage: (Long, Int) -> Unit = { _, _ -> },
    onNextTurn: () -> Unit = {},
    onEndEncounter: () -> Unit = {},
    onAddPlayer: (String, Int) -> Unit = { _, _ -> },
    onRemovePlayer: (String) -> Unit = {},
    onSavePermanentPlayer: (Npc) -> Unit = {},
    onDeletePermanentPlayer: (Npc) -> Unit = {},
    onToggleCondition: (Long, Condition) -> Unit = { _, _ -> },
    wikiEntryCount: Int = 0,
    wikiEntries: List<WikiEntry> = emptyList(),
    referenceFocusTitle: String? = null,
    referencePreview: String? = null,
    referencePinnedCount: Int = 0,
    referenceSearchQuery: String = "",
    referenceSearchCount: Int = 0,
    referenceSearchPreview: String? = null,
    linkedMapName: String? = null,
    linkedMapTileCount: Int = 0,
    linkedMapPoiCount: Int = 0,
    linkedMapDayNumber: Int? = null,
    linkedMapPartyLocation: String? = null,
    linkedMythicSession: MythicSession? = null,
    mythicState: MythicSessionUiState? = null,
    linkedHexMapId: Long? = null,
    hexTiles: List<HexTile> = emptyList(),
    hexPois: List<HexPoi> = emptyList(),
    partyLocation: Pair<Int, Int>? = null,
    onMoveParty: (Int, Int) -> Unit = { _, _ -> },
    onExploreTile: (HexTile) -> Unit = {},
    onReconnoiterTile: (HexTile) -> Unit = {},
    onMapTile: (HexTile) -> Unit = {},
    tensionClocks: List<TensionClock> = emptyList(),
    allHexMaps: List<HexMap> = emptyList(),
    onLinkHexMap: (Long?) -> Unit = {},
    onLinkResourceId: (Long, Long?) -> Unit = { _, _ -> },
    onUpdateNpcHp: (Long, Int) -> Unit = { _, _ -> },
    onNpcClick: (Long) -> Unit = {},
    onWikiClick: (Long) -> Unit = {},
    onUpdateWikiEntry: (WikiEntry) -> Unit = {},
    isEditing: Boolean,
    onToggleEditing: () -> Unit,
    onAddBlock: (SessionBoardItemType, Long?, Long?) -> Unit,
    onCreateClock: ((name: String, segments: Int, colorHex: String?, triggerDescription: String, targetGroupId: Long?) -> Unit)? = null,
    onTickClock: (Long, Int) -> Unit = { _, _ -> },
    onUpdateClockDescriptionAndProgress: (Long, String, Int) -> Unit = { _, _, _ -> },
    onOpenBlock: (SessionBoardItem) -> Unit = {},
    onQuickAction: (SessionBoardItem) -> Unit = {},
    onRemoveBlock: (Long) -> Unit,
    onMoveUp: (Long) -> Unit = {},
    onMoveDown: (Long) -> Unit = {},
    onMoveTo: (Long, Long) -> Unit = { _, _ -> },
    onSetSize: (Long, SessionBoardItemSize) -> Unit = { _, _ -> },
    onSetDisplayMode: (Long, String?) -> Unit = { _, _ -> },
    onSetTitleOverride: (Long, String?) -> Unit = { _, _ -> },
    onSetActiveBoardView: (SessionBoardView) -> Unit,
    onDuplicateBoardView: (SessionBoardView) -> Unit = {},
    onUpdateBoardViewLabel: (SessionBoardView, String) -> Unit = { _, _ -> },
    onClearBoardView: (SessionBoardView) -> Unit = {},
    onUpdateBoardViewLabels: (SessionBoardViewLabels) -> Unit = {},
    onApplyBoardTemplate: (SessionBoardTemplatePreset) -> Unit = {},
    onRollTable: (Long) -> Unit = {},
    onTogglePinBlock: (Long) -> Unit = {},
    onToggleCollapseBlock: (Long) -> Unit = {},
    onSetAllBlocksCollapsedInView: (SessionBoardView, Boolean) -> Unit = { _, _ -> },
    onTagClicked: (String) -> Unit = {},
    onDiceClicked: (String, Boolean, Boolean, String?) -> Unit = { _, _, _, _ -> },
    onCreateGroup: (String, String?, String?, String?) -> Unit = { _, _, _, _ -> },
    onRenameGroup: (Long, String, String?, String?, String?) -> Unit = { _, _, _, _, _ -> },
    onDeleteGroup: (Long) -> Unit = {},
    onToggleGroupCollapsed: (Long, Boolean) -> Unit = { _, _ -> },
    onSetGroupLayoutMode: (Long, SessionBoardLayoutMode) -> Unit = { _, _ -> },
    onAssignToGroup: (Long, Long?) -> Unit = { _, _ -> },
    onMergeItems: (Long, Long, (SessionBoardGroup) -> Unit) -> Unit = { _, _, _ -> },
    deckRefs: List<SessionDeckRef> = emptyList(),
    deckNames: Map<Long, String> = emptyMap(),
    allDecks: List<CardStack> = emptyList(),
    handByDeck: Map<Long, List<Card>> = emptyMap(),
    deckCardCounts: Map<Long, Int> = emptyMap(),
    deckAspectRatios: Map<Long, CardAspectRatio> = emptyMap(),
    deckBackImages: Map<Long, String?> = emptyMap(),
    onDrawCardFromDeck: (Long) -> Unit = {},
    onOpenDrawBySuit: (Long) -> Unit = {},
    onShuffleDeck: (Long) -> Unit = {},
    onShuffleOnly: (Long) -> Unit = onShuffleDeck,
    onShuffleDiscards: (Long) -> Unit = onShuffleDeck,
    onResetDeck: (Long) -> Unit = {},
    onDiscardCard: (Long) -> Unit = {},
    onRevealCard: (Long) -> Unit = {},
    onCardClick: (Long) -> Unit = {},
    onRotateCard: (Long) -> Unit = {},
    piledByDeck: Map<Long, Int> = emptyMap(),
    pinnedTableRolls: Map<Long, TableRollResult> = emptyMap(),
    onPinTableRoll: (Long, TableRollResult) -> Unit = { _, _ -> },
    onUnpinTableRoll: (Long) -> Unit = {},
    spreadResultsByDeck: Map<Long, CardSpreadResult> = emptyMap(),
    onDrawSpread: (Long, CardSpread) -> Unit = { _, _ -> },
    onClearSpread: (Long) -> Unit = {},
    onSaveSpreadToNotes: (CardSpreadResult) -> Unit = {},
    chainedRollsByTable: Map<Long, List<TableRollResult>> = emptyMap(),
    onRollSubTable: (Long, Long) -> Unit = { _, _ -> },
    onClearSubTableRoll: (Long, Long) -> Unit = { _, _ -> },
    sessionId: Long = 0L,
    isSidebarOpen: Boolean = false,
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
    savedBoardTemplates: List<SavedBoardTemplate> = emptyList(),
    onSetBoardPlayMode: (BoardPlayMode) -> Unit = {},
    onUndoBoardChange: () -> Unit = {},
    onMoveBoardGroup: (Long, Int) -> Unit = { _, _ -> },
    onSaveBoardTemplate: (String) -> Unit = {},
    onApplySavedBoardTemplate: (SavedBoardTemplate) -> Unit = {},
    onDeleteSavedBoardTemplate: (String) -> Unit = {},
    onMarkBoardBlockUsed: (Long) -> Unit = {},
    onWorkspaceLayoutChanged: (SessionBoardView, com.deckapp.core.model.BoardViewMemory) -> Unit = { _, _ -> },
    workspaceActions: @Composable () -> Unit = {},
    soloSceneHeader: @Composable () -> Unit = {},
    onAppendToNotes: (String) -> Unit = {},
    onGenerateMicrosetting: (Long) -> Unit = {},
    onRotateMicrosettingSlot: (Long, MicrosettingSlotType) -> Unit = { _, _ -> },
    onRedrawMicrosettingSlot: (Long, MicrosettingSlotType) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val orderedItems = remember(boardItems) {
        boardItems.sortedWith(compareBy<SessionBoardItem> { it.position }.thenBy { it.id })
    }

    var showPicker by rememberSaveable { mutableStateOf(false) }
    var pickerTargetGroupId by rememberSaveable { mutableStateOf<Long?>(null) }
    var showCreateGroupDialog by rememberSaveable { mutableStateOf(false) }
    var showRenameGroupDialog by remember { mutableStateOf<SessionBoardGroup?>(null) }
    var showConfigItem by remember { mutableStateOf<SessionBoardItem?>(null) }

    var groupNameInput by remember { mutableStateOf("") }
    var groupAccentColorHexInput by remember { mutableStateOf<String?>(null) }
    var groupIconNameInput by remember { mutableStateOf<String?>(null) }
    var groupAutoFilterTypeInput by remember { mutableStateOf<String?>(null) }

    var showRenameViewDialog by remember { mutableStateOf<SessionBoardView?>(null) }
    var newViewLabelInput by remember { mutableStateOf("") }
    var showConfigureWorkspacesDialog by rememberSaveable { mutableStateOf(false) }
    var showConfirmClearDialog by remember { mutableStateOf<SessionBoardView?>(null) }
    var showOverview by rememberSaveable { mutableStateOf(false) }
    var saveTemplateDialog by remember { mutableStateOf(false) }
    var templateName by remember { mutableStateOf("") }
    var showSavedTemplatesMenu by remember { mutableStateOf(false) }
    val effectiveEditing = isEditing && workspaceMemory.boardMode == BoardPlayMode.PREPARATION

    fun resetGroupEditor() {
        groupNameInput = ""
        groupAccentColorHexInput = null
        groupIconNameInput = null
        groupAutoFilterTypeInput = null
    }

    val orderedViews = remember(viewLabels) { viewLabels.getOrderedViews() }
    val pagerState = rememberPagerState(
        initialPage = orderedViews.indexOf(activeBoardView).coerceAtLeast(0),
        pageCount = { orderedViews.size }
    )
    val boardNavigationScope = rememberCoroutineScope()

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

    var focusedGroupId by rememberSaveable { mutableStateOf<Long?>(null) }
    val isFocusedGroupListMode = remember(focusedGroupId, boardGroups) {
        boardGroups.find { it.id == focusedGroupId }?.preferredLayoutMode == SessionBoardLayoutMode.LIST
    }

    var isFolderSearchActive by rememberSaveable { mutableStateOf(false) }
    var folderSearchQuery by rememberSaveable { mutableStateOf("") }
    var selectedFolderFilter by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(focusedGroupId) {
        isFolderSearchActive = false
        folderSearchQuery = ""
        selectedFolderFilter = null
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (focusedGroupId != null) {
                val focusedGroup = boardGroups.find { it.id == focusedGroupId }
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(onClick = { focusedGroupId = null }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                            }
                            Text(
                                text = focusedGroup?.name ?: "Carpeta",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val isGridLayout = focusedGroup?.preferredLayoutMode != SessionBoardLayoutMode.LIST
                            IconButton(onClick = {
                                focusedGroup?.let { group ->
                                    onSetGroupLayoutMode(
                                        group.id,
                                        if (isGridLayout) SessionBoardLayoutMode.LIST else SessionBoardLayoutMode.GRID
                                    )
                                }
                            }) {
                                Icon(
                                    imageVector = if (isGridLayout) Icons.AutoMirrored.Filled.ViewList else Icons.Default.ViewModule,
                                    contentDescription = if (isGridLayout) "Cambiar a lista compacta" else "Cambiar a cuadrícula compacta"
                                )
                            }
                        IconButton(onClick = {
                            pickerTargetGroupId = focusedGroupId
                            showPicker = true
                        }) {
                            Icon(Icons.Default.Add, contentDescription = "Agregar a carpeta")
                        }
                        }
                    }
                }
            } else {
                SessionBoardTopTabsBar(
                    orderedViews = orderedViews,
                    currentPage = pagerState.currentPage,
                    viewLabels = viewLabels,
                    boardItems = boardItems,
                    isEditing = effectiveEditing,
                    onSelectView = onSetActiveBoardView,
                    onRenameViewRequested = { view, label ->
                        newViewLabelInput = label
                        showRenameViewDialog = view
                    },
                    onDuplicateViewRequested = onDuplicateBoardView,
                    onClearViewRequested = { view ->
                        showConfirmClearDialog = view
                    },
                    onApplyBoardTemplate = onApplyBoardTemplate,
                    onToggleEditing = onToggleEditing,
                    onConfigureViews = { showConfigureWorkspacesDialog = true },
                    editingAllowed = workspaceMemory.boardMode == BoardPlayMode.PREPARATION
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(workspaceMemory.boardMode == BoardPlayMode.PREPARATION,
                    onClick = { onSetBoardPlayMode(BoardPlayMode.PREPARATION) }, label = { Text("Preparar") })
                FilterChip(workspaceMemory.boardMode == BoardPlayMode.PLAY,
                    onClick = { onSetBoardPlayMode(BoardPlayMode.PLAY) }, label = { Text("Partida") })
                TextButton(onClick = { showOverview = true }) { Text("Resumen (${boardGroups.size})") }
                IconButton(onClick = onUndoBoardChange, enabled = workspaceMemory.boardUndoHistory.isNotEmpty()) {
                    Icon(Icons.Default.Undo, contentDescription = workspaceMemory.boardUndoHistory.lastOrNull()?.let { "Deshacer ${it.label}" } ?: "Deshacer")
                }
                Box {
                    TextButton(onClick = { showSavedTemplatesMenu = true }) { Text("Plantillas") }
                    DropdownMenu(expanded = showSavedTemplatesMenu, onDismissRequest = { showSavedTemplatesMenu = false }) {
                        if (savedBoardTemplates.isEmpty()) {
                            DropdownMenuItem(text = { Text("Sin plantillas guardadas") }, onClick = { showSavedTemplatesMenu = false }, enabled = false)
                        } else savedBoardTemplates.forEach { template ->
                            DropdownMenuItem(
                                text = { Text(template.name) },
                                onClick = { showSavedTemplatesMenu = false; onApplySavedBoardTemplate(template) },
                                trailingIcon = {
                                    IconButton(onClick = { onDeleteSavedBoardTemplate(template.id) }) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Eliminar plantilla")
                                    }
                                }
                            )
                        }
                        if (effectiveEditing) {
                            HorizontalDivider()
                            DropdownMenuItem(text = { Text("Guardar vista actual…") }, onClick = {
                                showSavedTemplatesMenu = false
                                templateName = ""
                                saveTemplateDialog = true
                            })
                        }
                    }
                }
            }

            SessionBoardPagerContent(
                orderedViews = orderedViews,
                pagerState = pagerState,
                activeBoardView = activeBoardView,
                onSetActiveBoardView = onSetActiveBoardView,
                boardItems = boardItems,
                orderedItems = orderedItems,
                boardGroups = boardGroups,
                viewLabels = viewLabels,
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
                onUpdateNotes = onUpdateNotes,
                npcCount = npcCount,
                npcs = npcs,
                activeEncounter = activeEncounter,
                playerParticipants = playerParticipants,
                permanentPlayers = permanentPlayers,
                onApplyDamage = onApplyDamage,
                onNextTurn = onNextTurn,
                onEndEncounter = onEndEncounter,
                onAddPlayer = onAddPlayer,
                onRemovePlayer = onRemovePlayer,
                onSavePermanentPlayer = onSavePermanentPlayer,
                onDeletePermanentPlayer = onDeletePermanentPlayer,
                wikiEntryCount = wikiEntryCount,
                wikiEntries = wikiEntries,
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
                onMoveParty = onMoveParty,
                onExploreTile = onExploreTile,
                onReconnoiterTile = onReconnoiterTile,
                onMapTile = onMapTile,
                tensionClocks = tensionClocks,
                deckRefs = deckRefs,
                deckNames = deckNames,
                handByDeck = handByDeck,
                deckCardCounts = deckCardCounts,
                deckAspectRatios = deckAspectRatios,
                deckBackImages = deckBackImages,
                onDrawCardFromDeck = onDrawCardFromDeck,
                onOpenDrawBySuit = onOpenDrawBySuit,
                spreadResultsByDeck = spreadResultsByDeck,
                onDrawSpread = onDrawSpread,
                onClearSpread = onClearSpread,
                onSaveSpreadToNotes = onSaveSpreadToNotes,
                onShuffleDeck = onShuffleDeck,
                onShuffleOnly = onShuffleOnly,
                onShuffleDiscards = onShuffleDiscards,
                onCardClick = onCardClick,
                piledByDeck = piledByDeck,
                onResetDeck = onResetDeck,
                onDiscardCard = onDiscardCard,
                onRevealCard = onRevealCard,
                onRotateCard = onRotateCard,
                sessionId = sessionId,
                isEditing = effectiveEditing,
                focusedGroupId = focusedGroupId,
                isFocusedGroupListMode = isFocusedGroupListMode,
                folderSearchQuery = folderSearchQuery,
                isSidebarOpen = isSidebarOpen,
                onSelectFocusedGroup = { focusedGroupId = it },
                onOpenPickerRequested = { groupId ->
                    pickerTargetGroupId = groupId
                    showPicker = true
                },
                onRenameGroupRequested = { group ->
                    groupNameInput = group.name
                    groupAccentColorHexInput = group.accentColorHex
                    groupIconNameInput = group.iconName
                    groupAutoFilterTypeInput = group.autoFilterType
                    showRenameGroupDialog = group
                },
                onDeleteGroupRequested = { groupId ->
                    onDeleteGroup(groupId)
                },
                onToggleGroupCollapsed = onToggleGroupCollapsed,
                onConfigClick = { showConfigItem = it },
                onQuickAction = { item -> onMarkBoardBlockUsed(item.id); onQuickAction(item) },
                onOpenBlock = { item -> onMarkBoardBlockUsed(item.id); onOpenBlock(item) },
                onRemoveBlock = onRemoveBlock,
                onMoveUp = onMoveUp,
                onMoveDown = onMoveDown,
                onMoveTo = onMoveTo,
                onSetSize = onSetSize,
                onSetDisplayMode = onSetDisplayMode,
                onSetTitleOverride = onSetTitleOverride,
                onMergeItems = { sourceId, targetId ->
                    onMergeItems(sourceId, targetId) { newGroup ->
                        focusedGroupId = newGroup.id
                    }
                },
                onRollTable = onRollTable,
                onTogglePinBlock = onTogglePinBlock,
                onToggleCollapseBlock = onToggleCollapseBlock,
                onTagClicked = onTagClicked,
                onDiceClicked = onDiceClicked,
                onUpdateNpcHp = onUpdateNpcHp,
                onNpcClick = onNpcClick,
                onWikiClick = onWikiClick,
                onUpdateWikiEntry = onUpdateWikiEntry,
                onTickClock = onTickClock,
                onUpdateClockDescriptionAndProgress = onUpdateClockDescriptionAndProgress,
                onAssignToGroup = onAssignToGroup,
                onToggleEditing = onToggleEditing,
                onUpdateBoardViewLabel = onUpdateBoardViewLabel,
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
                workspaceMemory = workspaceMemory,
                onWorkspaceLayoutChanged = onWorkspaceLayoutChanged,
                workspaceActions = workspaceActions,
                soloSceneHeader = soloSceneHeader,
                onAppendToNotes = onAppendToNotes,
                onGenerateMicrosetting = onGenerateMicrosetting,
                onRotateMicrosettingSlot = onRotateMicrosettingSlot,
                onRedrawMicrosettingSlot = onRedrawMicrosettingSlot
            )
        }

        if (showOverview) {
            BoardOverviewDialog(
                groups = boardGroups.filter { it.boardView in orderedViews },
                boardItems = fullBoardItems,
                activityStartedAt = workspaceMemory.activityTrackingStartedAt,
                lastUsedAt = workspaceMemory.blockLastUsedAt,
                itemTitle = { boardItemTitle(it, tablesInSession, allTables, deckNames, wikiEntries) },
                viewLabel = viewLabels::labelFor,
                isEditing = effectiveEditing,
                onMoveGroup = onMoveBoardGroup,
                onJumpToGroup = { group ->
                    showOverview = false
                    focusedGroupId = group.id
                    val page = orderedViews.indexOf(group.boardView).coerceAtLeast(0)
                    boardNavigationScope.launch {
                        pagerState.animateScrollToPage(page)
                        onSetActiveBoardView(group.boardView)
                    }
                },
                onRemoveItem = { onRemoveBlock(it.id) },
                onDismiss = { showOverview = false }
            )
        }

        if (saveTemplateDialog) {
            AlertDialog(
                onDismissRequest = { saveTemplateDialog = false },
                title = { Text("Guardar plantilla") },
                text = {
                    OutlinedTextField(
                        value = templateName,
                        onValueChange = { templateName = it.take(48) },
                        label = { Text("Nombre") },
                        singleLine = true
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        onSaveBoardTemplate(templateName)
                        saveTemplateDialog = false
                    }, enabled = templateName.isNotBlank()) { Text("Guardar") }
                },
                dismissButton = { TextButton(onClick = { saveTemplateDialog = false }) { Text("Cancelar") } }
            )
        }

        if (showPicker) {
            SessionBoardBlockPickerSheet(
                boardItems = boardItems,
                tablesInSession = tablesInSession,
                allTables = allTables,
                recentTableRolls = recentTableRolls,
                linkedHexMapId = linkedHexMapId,
                linkedMythicSession = linkedMythicSession,
                tensionClocks = tensionClocks,
                deckRefs = deckRefs,
                deckNames = deckNames,
                allDecks = allDecks,
                deckCardCounts = deckCardCounts,
                allBoardItems = fullBoardItems,
                boardGroups = boardGroups,
                viewLabels = viewLabels,
                targetGroupId = pickerTargetGroupId,
                targetGroups = boardGroups.filter { it.boardView == activeBoardView },
                onTargetGroupChange = { pickerTargetGroupId = it },
                onRemoveBoardItem = onRemoveBlock,
                onAssignBoardItemsToGroup = { itemIds, groupId -> itemIds.forEach { onAssignToGroup(it, groupId) } },
                onShowOnBoard = { item ->
                    val page = orderedViews.indexOf(item.boardView).coerceAtLeast(0)
                    focusedGroupId = item.groupId
                    boardNavigationScope.launch {
                        pagerState.animateScrollToPage(page)
                        onSetActiveBoardView(item.boardView)
                    }
                    pickerTargetGroupId = null
                    showPicker = false
                },
                wikiEntries = wikiEntries,
                onDismiss = {
                    pickerTargetGroupId = null
                    showPicker = false
                },
                onPick = { type, resId ->
                    onAddBlock(type, resId, pickerTargetGroupId)
                    pickerTargetGroupId = null
                    showPicker = false
                },
                onPickMultipleResources = { tableIds, deckIds ->
                    tableIds.forEach { tableId ->
                        onAddBlock(SessionBoardItemType.TABLE, tableId, pickerTargetGroupId)
                    }
                    deckIds.forEach { deckId ->
                        onAddBlock(SessionBoardItemType.DECK, deckId, pickerTargetGroupId)
                    }
                    pickerTargetGroupId = null
                    showPicker = false
                },
                onCreateGroupClicked = {
                    showPicker = false
                    showCreateGroupDialog = true
                },
                onCreateSmartGroup = { name, filterType ->
                    onCreateGroup(name, null, null, filterType)
                    showPicker = false
                },
                onCreateClock = { name, segments, colorHex, triggerDesc ->
                    onCreateClock?.invoke(name, segments, colorHex, triggerDesc, pickerTargetGroupId)
                    pickerTargetGroupId = null
                    showPicker = false
                }
            )
        }

        if (showCreateGroupDialog) {
            CreateGroupDialog(
                name = groupNameInput,
                accentColorHex = groupAccentColorHexInput,
                iconName = groupIconNameInput,
                onNameChange = { groupNameInput = it },
                onAccentColorHexChange = { groupAccentColorHexInput = it },
                onIconNameChange = { groupIconNameInput = it },
                onDismiss = {
                    resetGroupEditor()
                    showCreateGroupDialog = false
                },
                onConfirm = {
                    if (groupNameInput.isNotBlank()) {
                        onCreateGroup(groupNameInput.trim(), groupAccentColorHexInput, groupIconNameInput, groupAutoFilterTypeInput)
                    }
                    resetGroupEditor()
                    showCreateGroupDialog = false
                }
            )
        }

        if (showRenameGroupDialog != null) {
            val group = showRenameGroupDialog!!
            CreateGroupDialog(
                name = groupNameInput,
                accentColorHex = groupAccentColorHexInput,
                iconName = groupIconNameInput,
                title = "Editar carpeta",
                confirmText = "Guardar",
                onNameChange = { groupNameInput = it },
                onAccentColorHexChange = { groupAccentColorHexInput = it },
                onIconNameChange = { groupIconNameInput = it },
                onDismiss = {
                    resetGroupEditor()
                    showRenameGroupDialog = null
                },
                onConfirm = {
                    if (groupNameInput.isNotBlank()) {
                        onRenameGroup(group.id, groupNameInput.trim(), groupAccentColorHexInput, groupIconNameInput, groupAutoFilterTypeInput)
                    }
                    resetGroupEditor()
                    showRenameGroupDialog = null
                }
            )
        }

        if (showRenameViewDialog != null) {
            val view = showRenameViewDialog!!
            AlertDialog(
                onDismissRequest = { showRenameViewDialog = null },
                title = { Text("Renombrar vista") },
                text = {
                    OutlinedTextField(
                        value = newViewLabelInput,
                        onValueChange = { newViewLabelInput = it },
                        label = { Text("Nombre de la vista") },
                        singleLine = true
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        onUpdateBoardViewLabel(view, newViewLabelInput)
                        showRenameViewDialog = null
                    }) {
                        Text("Guardar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRenameViewDialog = null }) {
                        Text("Cancelar")
                    }
                }
            )
        }

        if (showConfirmClearDialog != null) {
            val view = showConfirmClearDialog!!
            AlertDialog(
                onDismissRequest = { showConfirmClearDialog = null },
                title = { Text("Vaciar vista") },
                text = { Text("¿Estás seguro de que querés quitar todos los bloques de esta vista? Esta acción no se puede deshacer.") },
                confirmButton = {
                    TextButton(onClick = {
                        onClearBoardView(view)
                        showConfirmClearDialog = null
                    }) {
                        Text("Vaciar", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showConfirmClearDialog = null }) {
                        Text("Cancelar")
                    }
                }
            )
        }

        if (showConfigItem != null) {
            val item = showConfigItem!!
            BoardItemConfigDialog(
                item = item,
                boardGroups = boardGroups,
                onDismiss = { showConfigItem = null },
                onSetSize = { size -> onSetSize(item.id, size) },
                onSetDisplayMode = { mode -> onSetDisplayMode(item.id, mode) },
                onAssignToGroup = { onAssignToGroup(item.id, it) },
                onSetTitleOverride = { title -> onSetTitleOverride(item.id, title) }
            )
        }
    }
}

private fun boardItemTitle(
    item: SessionBoardItem,
    tablesInSession: List<RandomTable>,
    allTables: List<RandomTable>,
    deckNames: Map<Long, String>,
    wikiEntries: List<WikiEntry>
): String = when (item.type) {
    SessionBoardItemType.TABLE -> item.titleOverride ?: (tablesInSession + allTables).firstOrNull { it.id == item.resourceId }?.name ?: "Tabla"
    SessionBoardItemType.DECK -> item.titleOverride ?: deckNames[item.resourceId] ?: "Mazo"
    SessionBoardItemType.WIKI_MAP -> item.titleOverride ?: wikiEntries.firstOrNull { it.id == item.resourceId }?.title ?: "Mapa de wiki"
    SessionBoardItemType.NOTES -> "Notas"
    SessionBoardItemType.NPCS -> "PNJ"
    SessionBoardItemType.COMBAT -> "Combate"
    SessionBoardItemType.MAP -> "Mapa"
    SessionBoardItemType.DICE -> "Dados"
    SessionBoardItemType.MYTHIC -> "Mythic"
    SessionBoardItemType.CLOCK -> item.titleOverride ?: "Reloj"
    SessionBoardItemType.REFERENCE -> "Referencias"
    SessionBoardItemType.MICROSETTING -> item.titleOverride ?: "Microescenario"
}

@Composable
private fun CreateGroupDialog(
    name: String,
    accentColorHex: String?,
    iconName: String?,
    title: String = "Nueva carpeta",
    confirmText: String = "Crear",
    onNameChange: (String) -> Unit,
    onAccentColorHexChange: (String?) -> Unit,
    onIconNameChange: (String?) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text("Nombre de la carpeta") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = name.isNotBlank()
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun BoardItemConfigDialog(
    item: SessionBoardItem,
    boardGroups: List<SessionBoardGroup>,
    onDismiss: () -> Unit,
    onSetSize: (SessionBoardItemSize) -> Unit,
    onSetDisplayMode: (String?) -> Unit,
    onAssignToGroup: (Long?) -> Unit,
    onSetTitleOverride: (String?) -> Unit
) {
    var titleOverride by remember(item.id) { mutableStateOf(item.titleOverride ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configurar bloque") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = titleOverride,
                    onValueChange = { titleOverride = it },
                    label = { Text("Título personalizado (opcional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Tamaño", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SessionBoardItemSize.entries.forEach { size ->
                        FilterChip(
                            selected = item.size == size,
                            onClick = { onSetSize(size) },
                            label = { Text(size.name) }
                        )
                    }
                }

                if (boardGroups.isNotEmpty()) {
                    Text("Carpeta asignada", style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = item.groupId == null,
                            onClick = { onAssignToGroup(null) },
                            label = { Text("Ninguna") }
                        )
                        boardGroups.forEach { group ->
                            FilterChip(
                                selected = item.groupId == group.id,
                                onClick = { onAssignToGroup(group.id) },
                                label = { Text(group.name) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSetTitleOverride(titleOverride.trim().ifEmpty { null })
                onDismiss()
            }) {
                Text("Listo")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )
}

internal fun buildBoardGroupPreviewLines(
    items: List<SessionBoardItem>,
    tablesInSession: List<RandomTable>,
    allTables: List<RandomTable>,
    wikiEntries: List<WikiEntry> = emptyList()
): List<String> {
    val allKnownTables = (tablesInSession + allTables).distinctBy { it.id }
    return items.sortedBy { it.position }.mapNotNull { item ->
        when (item.type) {
            SessionBoardItemType.TABLE -> {
                val tableName = allKnownTables.firstOrNull { it.id == item.resourceId }?.name
                tableName?.let { "Tabla: $it" } ?: "Tabla rápida"
            }
            SessionBoardItemType.NOTES -> "Notas de sesión"
            SessionBoardItemType.MYTHIC -> "Contexto Mythic"
            SessionBoardItemType.MAP -> "Mapa vinculado"
            SessionBoardItemType.WIKI_MAP -> {
                val entry = wikiEntries.find { it.id == item.resourceId }
                entry?.let { "Lugar: ${it.title}" } ?: "Mapa de lugar"
            }
            SessionBoardItemType.DICE -> "Dados y tiradas"
            SessionBoardItemType.NPCS -> "NPCs"
            SessionBoardItemType.COMBAT -> "Combate"
            SessionBoardItemType.REFERENCE -> "Referencia"
            SessionBoardItemType.CLOCK -> "Reloj de progreso"
            SessionBoardItemType.DECK -> "Mazo de cartas"
            SessionBoardItemType.MICROSETTING -> "Microsetting de mundo"
        }
    }
}
