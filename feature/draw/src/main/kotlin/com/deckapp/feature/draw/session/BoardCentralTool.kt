package com.deckapp.feature.draw.session

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deckapp.core.model.SessionBoardItemType
import com.deckapp.core.model.SessionBoardViewLabels
import com.deckapp.core.ui.session.SessionCentralConnector
import com.deckapp.feature.draw.SessionViewModel
import com.deckapp.feature.draw.ChoirGuideNotes
import com.deckapp.feature.draw.SidebarTool
import com.deckapp.feature.draw.SessionViewMode
import com.deckapp.core.domain.usecase.boardInspirationCandidates
import com.deckapp.feature.draw.components.BoardInspirationTray
import com.deckapp.feature.draw.components.SoloScenePanel
import com.deckapp.feature.draw.components.SessionBoardTab

@OptIn(ExperimentalMaterial3Api::class)
val BoardCentralTool = SessionCentralConnector(
    id = "board",
    label = "Mesa de Juego",
    icon = Icons.Default.Dashboard,
    content = { context ->
        val viewModel: SessionViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        var isBoardEditMode by rememberSaveable { mutableStateOf(false) }
        
        SessionBoardTab(
            boardItems = uiState.sessionBoardItems,
            fullBoardItems = uiState.fullBoardItems,
            boardGroups = uiState.boardGroups,
            activeBoardView = uiState.activeBoardView,
            viewLabels = uiState.session?.boardViewLabels ?: SessionBoardViewLabels(),
            tablesInSession = uiState.tablesInSession,
            allTables = uiState.allTables,
            recentTableRolls = uiState.recentTableRolls,
            pinnedTableRolls = uiState.pinnedTableRolls,
            onPinTableRoll = viewModel::pinTableRoll,
            onUnpinTableRoll = viewModel::unpinTableRoll,
            recentDiceRolls = uiState.recentDiceRolls,
            notesPreview = ChoirGuideNotes.visible(uiState.dmNotes).take(300),
            notesContent = ChoirGuideNotes.visible(uiState.dmNotes),
            isSavingNotes = uiState.isSavingNotes,
            onUpdateNotes = viewModel::updateNotes,
            npcCount = uiState.npcCount,
            npcs = uiState.npcs,
            activeEncounter = uiState.activeEncounter,
            playerParticipants = uiState.playerParticipants,
            permanentPlayers = uiState.permanentPlayers,
            onApplyDamage = viewModel::applyDamage,
            onNextTurn = viewModel::nextTurn,
            onEndEncounter = viewModel::endEncounter,
            onAddPlayer = viewModel::addPlayerParticipant,
            onRemovePlayer = viewModel::removePlayerParticipant,
            onSavePermanentPlayer = viewModel::savePermanentPlayer,
            onDeletePermanentPlayer = viewModel::deletePermanentPlayer,
            onToggleCondition = viewModel::toggleCondition,
            wikiEntryCount = uiState.wikiEntries.size,
            wikiEntries = uiState.wikiEntries,
            referenceFocusTitle = null,
            referencePreview = null,
            referencePinnedCount = 0,
            referenceSearchQuery = "",
            referenceSearchCount = 0,
            referenceSearchPreview = null,
            linkedMapName = uiState.hplState?.mapName,
            linkedMapTileCount = uiState.hplState?.tiles?.size ?: 0,
            linkedMapPoiCount = uiState.hplState?.pois?.size ?: 0,
            linkedMapDayNumber = uiState.hplState?.currentDay?.dayNumber,
            linkedMapPartyLocation = uiState.hplState?.partyLocation?.let { (q, r) -> "hex $q,$r" },
            hexTiles = uiState.hplState?.tiles ?: emptyList(),
            hexPois = uiState.hplState?.pois ?: emptyList(),
            partyLocation = uiState.hplState?.partyLocation,
            onMoveParty = viewModel::moveParty,
            onExploreTile = viewModel::toggleTileExplored,
            onReconnoiterTile = viewModel::toggleTileReconnoitered,
            onMapTile = viewModel::toggleTileMapped,
            linkedHexMapId = uiState.linkedHexMapId,
            linkedMythicSession = uiState.linkedMythicSession,
            mythicState = uiState.mythicState,
            tensionClocks = uiState.tensionClocks,
            allHexMaps = uiState.allHexMaps,
            onLinkHexMap = viewModel::linkHexMap,
            onLinkResourceId = viewModel::setBoardBlockResourceId,
            onUpdateNpcHp = viewModel::updateNpcHp,
            onNpcClick = context.onNpcClick,
            onWikiClick = context.onWikiClick,
            onUpdateWikiEntry = viewModel::updateWikiEntry,
            isEditing = isBoardEditMode,
            onToggleEditing = { isBoardEditMode = !isBoardEditMode },
            onAddBlock = { type, resourceId, targetGroupId ->
                viewModel.addBoardBlock(type = type, resourceId = resourceId, targetGroupId = targetGroupId)
            },
            onCreateClock = viewModel::createClockAndAddToBoard,
            onTickClock = viewModel::tickClock,
            onUpdateClockDescriptionAndProgress = viewModel::updateClockDescriptionAndProgress,
            onOpenBlock = { item ->
                when (item.type) {
                    SessionBoardItemType.TABLE -> viewModel.toggleLeftSidebar(SidebarTool.TABLES)
                    SessionBoardItemType.COMBAT -> viewModel.toggleLeftSidebar(SidebarTool.COMBAT)
                    SessionBoardItemType.NOTES -> viewModel.toggleRightSidebar(SidebarTool.NOTES)
                    SessionBoardItemType.NPCS -> viewModel.toggleRightSidebar(SidebarTool.NPCS)
                    SessionBoardItemType.REFERENCE -> viewModel.toggleRightSidebar(SidebarTool.REFERENCES)
                    SessionBoardItemType.MYTHIC -> viewModel.toggleRightSidebar(SidebarTool.MYTHIC_CONTEXT)
                    SessionBoardItemType.MAP -> viewModel.setMainViewMode(SessionViewMode.MAP)
                    SessionBoardItemType.WIKI_MAP -> viewModel.toggleRightSidebar(SidebarTool.PLAN)
                    SessionBoardItemType.DECK -> viewModel.toggleLeftSidebar(SidebarTool.DECKS)
                    SessionBoardItemType.MICROSETTING -> viewModel.openMicrosettingBuilder(item)
                    else -> {}
                }
            },
            onQuickAction = { item ->
                when (item.type) {
                    SessionBoardItemType.TABLE -> item.resourceId?.let { viewModel.rollActiveTable(it) }
                    SessionBoardItemType.COMBAT -> viewModel.toggleLeftSidebar(SidebarTool.COMBAT)
                    SessionBoardItemType.NOTES -> viewModel.showQuickNote()
                    SessionBoardItemType.MICROSETTING -> viewModel.generateMicrosetting(item.id)
                    else -> {}
                }
            },
            onRemoveBlock = viewModel::removeBoardBlock,
            onMoveUp = { viewModel.moveBoardBlock(it, -1) },
            onMoveDown = { viewModel.moveBoardBlock(it, 1) },
            onMoveTo = { fromId, toId -> viewModel.moveBoardBlockTo(fromId, toId) },
            onSetSize = viewModel::setBoardBlockSize,
            onSetDisplayMode = viewModel::setBoardBlockDisplayMode,
            onSetTitleOverride = viewModel::setBoardBlockTitleOverride,
            onSetActiveBoardView = viewModel::setActiveBoardView,
            onDuplicateBoardView = { targetView -> viewModel.duplicateBoardView(targetView) },
            onUpdateBoardViewLabel = viewModel::updateBoardViewLabel,
            onClearBoardView = viewModel::clearBoardView,
            onUpdateBoardViewLabels = viewModel::updateBoardViewLabels,
            onApplyBoardTemplate = viewModel::applyBoardTemplate,
            onRollTable = { tableId ->
                uiState.sessionBoardItems.filter { it.type == SessionBoardItemType.TABLE && it.resourceId == tableId }
                    .forEach { viewModel.markBoardBlockUsed(it.id) }
                viewModel.rollActiveTable(tableId)
            },
            onTogglePinBlock = viewModel::togglePinBlock,
            onToggleCollapseBlock = viewModel::toggleCollapseBlock,
            onSetAllBlocksCollapsedInView = { _, collapsed -> viewModel.setAllBlocksCollapsedInView(collapsed) },
            onTagClicked = viewModel::handleSmartTag,
            onDiceClicked = { formula, adv, dis, extra -> viewModel.rollSmartDice(formula, adv, dis, extra) },
            onCreateGroup = { name, color, _, _ -> viewModel.createBoardGroup(name, color) },
            onRenameGroup = { groupId, name, _, _, _ -> viewModel.renameBoardGroup(groupId, name) },
            onDeleteGroup = viewModel::deleteBoardGroup,
            onToggleGroupCollapsed = { groupId, _ -> viewModel.toggleBoardGroupCollapsed(groupId) },
            onMoveBoardGroup = viewModel::moveBoardGroup,
            onAssignToGroup = viewModel::assignItemToGroup,
            onMergeItems = viewModel::mergeBoardItems,
            deckRefs = uiState.deckRefs,
            deckNames = uiState.deckNames,
            allDecks = uiState.allDecks,
            handByDeck = uiState.handByDeck,
            deckCardCounts = uiState.deckCardCounts,
            deckAspectRatios = uiState.deckAspectRatios,
            deckBackImages = uiState.deckBackImages,
            onDrawCardFromDeck = { deckId ->
                uiState.sessionBoardItems.filter { it.type == SessionBoardItemType.DECK && it.resourceId == deckId }
                    .forEach { viewModel.markBoardBlockUsed(it.id) }
                viewModel.drawCardFromDeck(deckId)
            },
            onOpenDrawBySuit = { deckId ->
                uiState.sessionBoardItems.filter { it.type == SessionBoardItemType.DECK && it.resourceId == deckId }
                    .forEach { viewModel.markBoardBlockUsed(it.id) }
                viewModel.openDrawBySuitForDeck(deckId)
            },
            spreadResultsByDeck = uiState.spreadResultsByDeck,
            onDrawSpread = { deckId, spread ->
                if (spread == com.deckapp.core.model.CardSpread.TAROKKA_CROSS) {
                    uiState.sessionBoardItems.filter { it.type == SessionBoardItemType.DECK && it.resourceId == deckId }
                        .forEach { viewModel.markBoardBlockUsed(it.id) }
                    viewModel.drawTarokkaCard(deckId)
                }
            },
            onClearSpread = viewModel::clearTarokkaSpread,
            onShuffleDeck = viewModel::shuffleSingleDeck,
            onShuffleOnly = { deckId -> viewModel.shuffleDeckOnly(deckId) },
            onShuffleDiscards = { deckId -> viewModel.shuffleSingleDeck(deckId) },
            onResetDeck = { deckId -> viewModel.resetSingleDeck(deckId) },
            onDiscardCard = { cardId -> viewModel.discardCard(cardId) },
            onRevealCard = { cardId -> viewModel.revealCard(cardId) },
            onCardClick = context.onCardClick,
            onRotateCard = { cardId -> viewModel.rotateCard(cardId) },
            piledByDeck = uiState.pile.groupBy { it.stackId }.mapValues { it.value.size },
            sessionId = uiState.session?.id ?: 0L,
            isSidebarOpen = context.isSidebarOpen,
            boardMythicChaos = uiState.boardMythicChaos,
            boardMythicProbability = uiState.boardMythicProbability,
            boardMythicLastResult = uiState.boardMythicLastResult,
            boardMythicHistory = uiState.boardMythicHistory,
            boardMythicLastEvent = uiState.boardMythicLastEvent,
            onSetBoardMythicProbability = viewModel::setBoardMythicProbability,
            onUpdateBoardMythicChaos = viewModel::updateBoardMythicChaos,
            onRollBoardMythicFate = viewModel::rollBoardMythicFate,
            onRollBoardMythicEvent = { viewModel.rollBoardMythicRandomEvent() },
            onAskBoardMythicQuestion = { probability, question -> viewModel.rollBoardMythicFate(probability, question = question) },
            workspaceMemory = uiState.boardMemory,
            savedBoardTemplates = uiState.savedBoardTemplates,
            onSetBoardPlayMode = viewModel::setBoardPlayMode,
            onUndoBoardChange = viewModel::undoBoardChange,
            onSaveBoardTemplate = viewModel::saveCurrentBoardTemplate,
            onApplySavedBoardTemplate = viewModel::applySavedBoardTemplate,
            onDeleteSavedBoardTemplate = viewModel::deleteSavedBoardTemplate,
            onMarkBoardBlockUsed = viewModel::markBoardBlockUsed,
            onWorkspaceLayoutChanged = viewModel::setWorkspaceLayout,
            workspaceActions = {
                val candidates = remember(uiState.recentTableRolls, uiState.boardMythicHistory, uiState.handByDeck, uiState.deckNames, uiState.scenes, uiState.focusedSceneId) {
                    boardInspirationCandidates(uiState.recentTableRolls, uiState.boardMythicHistory,
                        uiState.handByDeck.values.flatten(), uiState.deckNames, viewModel.activeSoloScene()?.title.orEmpty())
                }
                BoardInspirationTray(
                    entries = uiState.boardMemory.inspiration,
                    candidates = candidates,
                    busy = uiState.isSendingInspiration || uiState.isSoloSceneBusy,
                    onCapture = viewModel::captureBoardInspiration,
                    onStatus = viewModel::setBoardInspirationStatus,
                    onSendToDiary = viewModel::sendBoardInspirationToDiary
                )
            },
            soloSceneHeader = {
                SoloScenePanel(
                    sessionId = uiState.sessionId,
                    scenes = uiState.scenes,
                    selectedScene = viewModel.activeSoloScene(),
                    busy = uiState.isSoloSceneBusy,
                    probability = uiState.boardMythicProbability,
                    chaos = uiState.linkedMythicSession?.chaosFactor ?: uiState.boardMythicChaos,
                    lastOracleResult = uiState.boardMythicLastResult,
                    onSelect = viewModel::focusScene,
                    onSave = viewModel::saveSoloScene,
                    onClose = viewModel::closeSoloScene,
                    onAsk = { question -> viewModel.rollBoardMythicFate(question = question) },
                    onAppendToDiary = viewModel::appendToNotes
                )
            },
            onAppendToNotes = viewModel::appendToNotes,
            onGenerateMicrosetting = { viewModel.generateMicrosetting(it) },
            onRotateMicrosettingSlot = { itemId, slot -> viewModel.rotateMicrosettingSlot(slot, itemId) },
            onRedrawMicrosettingSlot = { itemId, slot -> viewModel.redrawMicrosettingSlot(slot, itemId) },
            modifier = Modifier.fillMaxSize()
        )

        if (uiState.showDrawBySuitSheet) {
            ModalBottomSheet(onDismissRequest = viewModel::closeDrawBySuitSheet) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Robar por palo", style = MaterialTheme.typography.titleLarge,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    Text(uiState.deckNames[uiState.selectedDeckId] ?: "Mazo",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (uiState.availableSuitsForDraw.isEmpty()) {
                        Text("No quedan cartas con palo disponible.", modifier = Modifier.padding(vertical = 12.dp))
                    } else {
                        uiState.availableSuitsForDraw.forEach { suit ->
                            FilledTonalButton(
                                onClick = { viewModel.drawCardBySuit(suit) },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text(suit) }
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                }
            }
        }
    }
)
