package com.deckapp.feature.draw.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.deckapp.core.model.*
import com.deckapp.feature.draw.SessionUiState
import com.deckapp.feature.draw.SessionViewModel
import com.deckapp.feature.draw.components.clocks.ClockEditDialog
import com.deckapp.feature.draw.components.clocks.ClockImpactDialog
import com.deckapp.feature.draw.components.spotlight.SessionSpotlightDialog
import com.deckapp.feature.draw.components.threads.MythicCharacterEditDialog
import com.deckapp.feature.draw.components.threads.ThreadEditDialog
import com.deckapp.core.ui.components.NpcDetailSheet
import com.deckapp.feature.draw.tactical.*
import com.deckapp.feature.tables.TableDetailSheet

/**
 * Agrupa y gestiona el ciclo de vida visual de todas las hojas modales, diálogos y
 * alertas contextuales de la sesión de juego.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDialogsHost(
    uiState: SessionUiState,
    viewModel: SessionViewModel,
    showChoirDialog: Boolean,
    onDismissChoirDialog: () -> Unit,
    showIncursionDialog: Boolean,
    onDismissIncursionDialog: () -> Unit,
    onOpenIncursionFromGuide: () -> Unit,
    peekingWikiEntry: WikiEntry?,
    onDismissWikiPreview: () -> Unit,
    quickViewNpc: Npc?,
    onDismissNpcPreview: () -> Unit,
    peekingTable: RandomTable?,
    onDismissTablePreview: () -> Unit
) {
    if (uiState.showChoirGuide) {
        ChoirProcedureGuide(uiState, viewModel.choirGuideDelegate, onOpenIncursionFromGuide)
    }
    if (uiState.showStoryEngineGuide) {
        StoryEngineProcedureGuide(
            state = uiState,
            delegate = viewModel.storyEngineGuideDelegate
        )
    }
    uiState.choirGuideSourcePage?.let { page ->
        ChoirProcedureSource(page, onPage = { viewModel.choirGuideDelegate.source(it) }, onClose = { viewModel.choirGuideDelegate.source(null) })
    }
    uiState.choirGuideTableId?.let { id ->
        val table = (uiState.tablesInSession + uiState.allTables).firstOrNull { it.id == id }
        if (table != null) {
            TableDetailSheet(
                table = table,
                lastResult = uiState.recentTableRolls.firstOrNull { it.tableId == id },
                recentResults = uiState.recentTableRolls.filter { it.tableId == id },
                isRolling = false,
                onRoll = { viewModel.rollActiveTable(id) },
                onSelectEntry = { index, die -> viewModel.selectTableEntry(id, index, die) },
                onExport = {},
                onDismiss = viewModel.choirGuideDelegate::closeTable
            )
        } else {
            LaunchedEffect(id) { viewModel.choirGuideDelegate.closeTable() }
        }
    }
    if (showChoirDialog) {
        com.deckapp.feature.npcs.dialogs.ChoirOfFleshCharacterDialog(
            onDismiss = onDismissChoirDialog,
            onCharacterCreated = { npc, sheetMarkdown ->
                viewModel.createChoirOfFleshCharacter(npc, sheetMarkdown)
                onDismissChoirDialog()
            }
        )
    }

    if (showIncursionDialog) {
        val incursion = remember(uiState.dmNotes) { viewModel.getChoirIncursion() }
        val settlement = remember(uiState.dmNotes) { viewModel.getChoirSettlement() }
        val activeChar = remember(uiState.activeEncounter) {
            uiState.activeEncounter?.creatures?.firstNotNullOfOrNull { 
                com.deckapp.core.domain.usecase.ChoirCharacterCodec.read(it.notes)
                    ?.let { sheet -> it.id to sheet }
            }
        }
        com.deckapp.core.ui.components.ChoirIncursionDialog(
            initialIncursion = incursion,
            initialSettlement = settlement,
            activeCharacter = activeChar?.second,
            onSave = { inc, set, ch ->
                viewModel.saveChoirIncursion(inc, set, ch, activeChar?.first)
                onDismissIncursionDialog()
            },
            onOpenTacticalMap = {
                viewModel.setMainViewMode(com.deckapp.feature.draw.SessionViewMode.TACTICAL)
            },
            onDismiss = onDismissIncursionDialog
        )
    }

    quickViewNpc?.let { npc ->
        NpcDetailSheet(
            npc = npc,
            onDismiss = onDismissNpcPreview,
            onAddToCombat = viewModel::addNpcToCombat,
            onLogToDiary = viewModel::logNpcToDiary,
            onUpdateHp = { targetNpc, newHp -> viewModel.updateNpcHp(targetNpc.id, newHp) },
            onRollAction = { label, formula -> viewModel.rollSmartDiceWithDescription(formula, label) },
            isPlayingVoice = uiState.playingNpcVoicePath == npc.voiceSamplePath,
            onPlayVoice = viewModel::playNpcVoice
        )
    }

    peekingWikiEntry?.let { wiki ->
        WikiQuickPreviewDialog(
            wikiEntry = wiki,
            onDismiss = onDismissWikiPreview,
            onSendToNotes = { title, content ->
                viewModel.appendToNotes("### [$title]\n$content")
            }
        )
    }

    peekingTable?.let { table ->
        val lastResult = uiState.recentTableRolls.firstOrNull { it.tableId == table.id }
        val recentResults = uiState.recentTableRolls.filter { it.tableId == table.id }
        TableDetailSheet(
            table = table,
            lastResult = lastResult,
            recentResults = recentResults,
            isRolling = false,
            onRoll = { viewModel.rollActiveTable(table.id) },
            onSelectEntry = { index, die -> viewModel.selectTableEntry(table.id, index, die) },
            onExport = {},
            onSendToNotes = { tableName, text ->
                viewModel.appendToNotes("### [$tableName]\n$text")
            },
            onDismiss = onDismissTablePreview
        )
    }

    if (uiState.showSessionConfig) {
        SessionConfigSheet(
            sessionName = uiState.session?.name.orEmpty(),
            onRenameSession = viewModel::renameSession,
            selectedSystems = uiState.session?.gameSystems ?: emptyList(),
            onSystemsChanged = viewModel::updateGameSystems,
            availableSystems = uiState.availableSystems,
            boardViewLabels = uiState.session?.boardViewLabels ?: SessionBoardViewLabels(),
            onBoardViewLabelChanged = viewModel::updateBoardViewLabel,
            linkedMythicSession = uiState.linkedMythicSession,
            allMythicSessions = uiState.allMythicSessions,
            onLinkMythic = viewModel::linkMythicSession,
            onCreateAndLinkMythic = viewModel::createAndLinkMythicSession,
            linkedHexMapId = uiState.linkedHexMapId,
            allHexMaps = uiState.allHexMaps,
            onLinkHexMap = viewModel::linkHexMap,
            onResetFloatingPanels = viewModel::resetFloatingPanels,
            allDecks = uiState.allDecks,
            sessionDeckRefs = uiState.deckRefs,
            onToggleDeck = viewModel::toggleDeckInSession,
            onOpenDeckPicker = viewModel::openDeckPicker,
            onDismiss = viewModel::dismissSessionConfig
        )
    }

    if (uiState.showDeckPicker) {
        SessionDeckPickerSheet(
            allDecks = uiState.allDecks,
            activeDeckIds = uiState.deckRefs.map { it.stackId }.toSet(),
            deckCardCounts = uiState.deckCardCounts,
            cardsInHandByDeck = uiState.handByDeck,
            onToggleDeck = viewModel::toggleDeckInSession,
            onOpenStarterPacks = viewModel::openStarterPackDialog,
            onDismiss = viewModel::closeDeckPicker
        )
    }

    if (uiState.showStarterPackDialog) {
        com.deckapp.feature.reference.StarterPackDialog(
            packs = uiState.availableStarterPacks,
            onDismiss = viewModel::closeStarterPackDialog,
            onInstall = viewModel::installStarterPack,
            onRemove = viewModel::removeStarterPack
        )
    }

    if (uiState.showHotbarActionDialog) {
        val availableTables = uiState.tablesInSession.ifEmpty { uiState.allTables }
        HotbarActionDialog(
            action = uiState.editingHotbarAction,
            availableTables = availableTables,
            availableDecks = uiState.allDecks,
            onDismiss = viewModel::dismissHotbarActionDialog,
            onSave = viewModel::saveHotbarAction,
            onDelete = { actionId -> viewModel.deleteHotbarAction(actionId) }
        )
    }

    if (uiState.showSceneFramingDialog) {
        val activeScene = uiState.scenes.find { it.id == uiState.focusedSceneId && !it.isCompleted }
            ?: uiState.scenes.firstOrNull { !it.isCompleted }
        val currentChaos = uiState.linkedMythicSession?.chaosFactor ?: uiState.boardMythicChaos
        val availableTables = uiState.tablesInSession.ifEmpty { uiState.allTables }

        SceneFramingDialog(
            currentChaos = currentChaos,
            activeScene = activeScene,
            availableNpcs = uiState.npcs,
            availableTables = availableTables,
            availableDecks = uiState.allDecks,
            onDismiss = viewModel::dismissSceneFraming,
            onConfirmFraming = viewModel::confirmSceneFraming
        )
    }

    if (uiState.showClockEditDialog) {
        ClockEditDialog(
            clock = uiState.editingClock,
            onDismiss = viewModel::closeClockEditDialog,
            onSave = viewModel::saveClock
        )
    }

    uiState.clockImpactAlert?.let { completedClock ->
        ClockImpactDialog(
            clock = completedClock,
            onDismiss = viewModel::dismissClockImpactAlert,
            onCopyToDiary = { viewModel.copyClockImpactToDiary(completedClock) },
            onResetClock = { viewModel.resetClock(completedClock.id) },
            onDeleteClock = { viewModel.deleteClock(completedClock.id) }
        )
    }

    if (uiState.showThreadEditDialog) {
        ThreadEditDialog(
            thread = uiState.editingMythicThread,
            onDismiss = viewModel::closeThreadDialog,
            onSave = viewModel::saveMythicThread
        )
    }

    if (uiState.showMythicCharacterEditDialog) {
        MythicCharacterEditDialog(
            character = uiState.editingMythicCharacter,
            onDismiss = viewModel::closeCharacterDialog,
            onSave = viewModel::saveMythicCharacter
        )
    }

    if (uiState.showSpotlightDialog) {
        SessionSpotlightDialog(
            query = uiState.spotlightQuery,
            selectedCategory = uiState.selectedSpotlightCategory,
            results = uiState.spotlightResults,
            selectedItem = uiState.selectedSpotlightItem,
            lastRollResult = uiState.lastSpotlightRollResult,
            isSearching = uiState.isSpotlightSearching,
            onQueryChange = viewModel::updateSpotlightQuery,
            onCategorySelect = viewModel::selectSpotlightCategory,
            onItemSelect = viewModel::selectSpotlightItem,
            onRollTable = viewModel::rollSpotlightTable,
            onAddCreatureToCombat = viewModel::addSpotlightCreatureToCombat,
            onAddNpcToCombat = viewModel::addSpotlightNpcToCombat,
            onCopyToDiary = viewModel::copySpotlightToDiary,
            onDismiss = viewModel::closeSpotlight
        )
    }

    if (uiState.showMicrosettingBuilder) {
        MicrosettingBuilderDialog(
            cluster = uiState.activeMicrosetting,
            onDismiss = viewModel::closeMicrosettingBuilder,
            onGenerateAll = { viewModel.generateMicrosetting(uiState.editingMicrosettingBoardItemId) },
            onRotateSlot = { slot -> viewModel.rotateMicrosettingSlot(slot, uiState.editingMicrosettingBoardItemId) },
            onSetSlotRotation = { slot, rot -> viewModel.setMicrosettingSlotRotation(slot, rot, uiState.editingMicrosettingBoardItemId) },
            onRedrawSlot = { slot -> viewModel.redrawMicrosettingSlot(slot, uiState.editingMicrosettingBoardItemId) },
            onCopyToNotes = { cluster -> viewModel.copyMicrosettingToNotes(cluster) },
            onSaveToBoard = { cluster -> viewModel.saveMicrosettingToBoard(cluster, uiState.editingMicrosettingBoardItemId) }
        )
    }

    if (uiState.tacticalState.showCreatePinDialog) {
        TacticalPinDialog(
            initialPin = uiState.tacticalState.pinToEdit,
            defaultCellX = uiState.tacticalState.pendingPinCoordinates?.first ?: 0f,
            defaultCellY = uiState.tacticalState.pendingPinCoordinates?.second ?: 0f,
            mapId = uiState.tacticalState.map?.id ?: 0L,
            availableTables = uiState.tacticalState.availableTables,
            availableEncounters = uiState.tacticalState.availableEncounters,
            availableWikiEntries = uiState.tacticalState.availableWikiEntries,
            availableMaps = uiState.tacticalState.availableMaps,
            onDismiss = viewModel.tacticalMapDelegate::dismissPinDialogs,
            onSavePin = viewModel.tacticalMapDelegate::savePin
        )
    }

    val selectedPinForSheet = uiState.tacticalState.selectedPin
    if (selectedPinForSheet != null && uiState.tacticalState.showPinDetailDialog) {
        TacticalPinSheet(
            pin = selectedPinForSheet,
            availableTables = uiState.tacticalState.availableTables,
            availableEncounters = uiState.tacticalState.availableEncounters,
            availableWikiEntries = uiState.tacticalState.availableWikiEntries,
            onDismiss = { viewModel.tacticalMapDelegate.selectPin(null) },
            onEditPin = viewModel.tacticalMapDelegate::openEditPin,
            onDeletePin = viewModel.tacticalMapDelegate::deletePin,
            onUpdateExplorationState = { viewModel.tacticalMapDelegate.updatePinExplorationState(selectedPinForSheet.id, it) },
            onRollTable = { viewModel.tacticalMapDelegate.rollPinTable(selectedPinForSheet.id) },
            onRollSpecificTable = { tableId -> viewModel.tacticalMapDelegate.rollPinTable(selectedPinForSheet.id, tableId) },
            onDeleteDiscovery = { viewModel.tacticalMapDelegate.removePinDiscovery(selectedPinForSheet.id, it) },
            onStartEncounter = { encounterId -> viewModel.startEncounterFromScene(encounterId) },
            onPreviewWiki = { wiki -> viewModel.tacticalMapDelegate.setPreviewWikiEntry(wiki) }
        )
    }

    if (uiState.tacticalState.showCreateMapDialog) {
        TacticalCreateMapDialog(
            scenes = uiState.scenes,
            activeSceneId = uiState.focusedSceneId,
            onDismiss = viewModel.tacticalMapDelegate::dismissMultimapDialogs,
            onCreateMap = { name, uri, gridType, cellSize, scaleFt, sceneId ->
                viewModel.tacticalMapDelegate.createNewMap(name, uri, gridType, cellSize, scaleFt, sceneId)
            }
        )
    }

    if (uiState.tacticalState.showMapSelectorSheet) {
        val context = androidx.compose.ui.platform.LocalContext.current
        val isChoir = uiState.session?.gameSystems?.any { it.contains("Choir", ignoreCase = true) } == true
        TacticalMapSelectorSheet(
            availableMaps = uiState.tacticalState.availableMaps,
            currentMapId = uiState.tacticalState.map?.id,
            scenes = uiState.scenes,
            onSelectMap = viewModel.tacticalMapDelegate::switchMap,
            onCreateNewMap = viewModel.tacticalMapDelegate::openCreateMapDialog,
            onLoadSystemMaps = {
                viewModel.tacticalMapDelegate.loadSystemMaps(context, "choir_of_flesh")
            },
            isChoirOfFlesh = isChoir,
            onChangeMapImage = { mapId, uri -> viewModel.tacticalMapDelegate.updateMapImage(uri, mapId) },
            onRenameMap = viewModel.tacticalMapDelegate::openRenameMapDialog,
            onLinkScene = viewModel.tacticalMapDelegate::openLinkSceneDialog,
            onDeleteMap = viewModel.tacticalMapDelegate::deleteMap,
            onDismiss = viewModel.tacticalMapDelegate::dismissMapSelector
        )
    }

    uiState.tacticalState.mapToRename?.let { map ->
        var newMapName by remember(map.id) { mutableStateOf(map.name) }
        AlertDialog(
            onDismissRequest = viewModel.tacticalMapDelegate::dismissMultimapDialogs,
            title = { Text("Renombrar Mapa") },
            text = {
                OutlinedTextField(
                    value = newMapName,
                    onValueChange = { newMapName = it },
                    label = { Text("Nombre del mapa") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newMapName.isNotBlank()) {
                            viewModel.tacticalMapDelegate.renameMap(map.id, newMapName)
                        }
                    }
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel.tacticalMapDelegate::dismissMultimapDialogs) {
                    Text("Cancelar")
                }
            }
        )
    }

    uiState.tacticalState.mapToLinkScene?.let { map ->
        AlertDialog(
            onDismissRequest = viewModel.tacticalMapDelegate::dismissMultimapDialogs,
            title = { Text("Vincular Mapa a Escena") },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (map.sceneId == null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.tacticalMapDelegate.linkMapToScene(map.id, null)
                                }
                        ) {
                            Text(
                                "Sin escena vinculada",
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    items(uiState.scenes, key = { it.id }) { sc ->
                        val isSelected = map.sceneId == sc.id
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.tacticalMapDelegate.linkMapToScene(map.id, sc.id)
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Movie, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text(
                                    sc.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = viewModel.tacticalMapDelegate::dismissMultimapDialogs) {
                    Text("Cerrar")
                }
            }
        )
    }

    uiState.tacticalState.mapToDelete?.let { map ->
        TacticalDeleteMapConfirmDialog(
            map = map,
            onConfirm = { viewModel.tacticalMapDelegate.confirmDeleteMap(map.id) },
            onDismiss = viewModel.tacticalMapDelegate::dismissDeleteMapDialog
        )
    }
}
