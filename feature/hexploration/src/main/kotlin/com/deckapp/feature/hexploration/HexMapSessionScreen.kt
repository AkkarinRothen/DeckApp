package com.deckapp.feature.hexploration

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deckapp.core.model.HexDay
import com.deckapp.core.model.HexPoi
import com.deckapp.core.model.HexTile
import com.deckapp.core.model.MythicSession
import com.deckapp.core.model.RandomTable
import com.deckapp.core.model.CardStack
import com.deckapp.core.model.SystemRule
import com.deckapp.core.model.Card
import com.deckapp.feature.hexploration.components.HexCanvasMode
import com.deckapp.feature.hexploration.components.HexGridCanvas
import com.deckapp.feature.hexploration.components.TerrainBrushToolbar
import com.deckapp.feature.hexploration.components.HexResourcesPanel
import com.deckapp.core.model.TableRollResult

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.mimeTypes
import android.content.ClipData
import kotlinx.serialization.json.Json
import com.deckapp.core.domain.usecase.SearchResult
import com.deckapp.feature.hexploration.components.worldToAxial
import com.deckapp.feature.hexploration.components.screenToWorld
import com.deckapp.feature.hexploration.components.HEX_SIZE_DEFAULT

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HexMapSessionScreen(
    onNavigateToEncounter: (Long) -> Unit,
    onNavigateToEncounterList: () -> Unit,
    onBack: () -> Unit,
    onShowMythicOracle: () -> Unit = {},
    viewModel: HexMapSessionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current
    
    // El mapa necesita rastrear su escala/offset actuales para el Drop coordinado
    var currentScale by remember { mutableFloatStateOf(1f) }
    var currentOffset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }

    val dndTarget = remember {
        object : DragAndDropTarget {
            override fun onDrop(event: DragAndDropEvent): Boolean {
                val clipData = event.toAndroidDragEvent().clipData
                if (clipData != null && clipData.itemCount > 0) {
                    try {
                        val json = clipData.getItemAt(0).text.toString()
                        val result = Json.decodeFromString<SearchResult>(json)
                        
                        // Obtener posición del drop
                        val dropPos = androidx.compose.ui.geometry.Offset(
                            event.toAndroidDragEvent().x,
                            event.toAndroidDragEvent().y
                        )
                        
                        // Convertir a coordenadas axial
                        val worldPos = screenToWorld(dropPos, currentOffset, currentScale)
                        val (q, r) = worldToAxial(worldPos, HEX_SIZE_DEFAULT)
                        
                        viewModel.linkSearchResultToTile(result, q, r)
                        return true
                    } catch (e: Exception) { e.printStackTrace() }
                }
                return false
            }
        }
    }

    // Auto-copiar al portapapeles cuando sale un resultado
    LaunchedEffect(uiState.lastRollResult) {
        uiState.lastRollResult?.let { result ->
            clipboard.setText(AnnotatedString(result.resolvedText))
        }
    }

    var showResourcesPanel by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(uiState.mapName, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        uiState.linkedMythicSession?.let { mythic ->
                            Text(
                                "Mythic: ${mythic.name} (Caos ${mythic.chaosFactor})",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = onShowMythicOracle) {
                        Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = "Oráculo Mythic")
                    }
                    IconButton(onClick = viewModel::showJournalPanel) {
                        Icon(Icons.Default.Book, contentDescription = "Diario")
                    }
                    IconButton(onClick = viewModel::toggleEditMode) {
                        Icon(
                            Icons.Default.Edit, 
                            contentDescription = "Modo Edición",
                            tint = if (uiState.isEditMode) MaterialTheme.colorScheme.primary else LocalContentColor.current
                        )
                    }
                    IconButton(onClick = { showResourcesPanel = true }) {
                        Icon(Icons.Default.Apps, contentDescription = "Recursos")
                    }
                }
            )
        },
        bottomBar = {
            SessionBottomBar(
                currentDay = uiState.currentDay?.dayNumber ?: 0,
                activitiesUsed = uiState.activitiesUsedToday,
                maxActivities = uiState.maxActivitiesPerDay,
                onStartDay = viewModel::startNewDay
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            var contextMenuTile by remember { mutableStateOf<HexTile?>(null) }
            var contextMenuExpanded by remember { mutableStateOf(false) }

            HexGridCanvas(
                tiles = uiState.tiles,
                pois = uiState.pois,
                mode = if (uiState.isEditMode) HexCanvasMode.DESIGN else HexCanvasMode.SESSION,
                selectedTile = uiState.selectedTile,
                partyLocation = uiState.partyLocation,
                onTileClick = { tile ->
                    if (uiState.isEditMode) viewModel.onTileClickInEditMode(tile)
                    else viewModel.selectTile(tile)
                },
                onTileLongPress = { tile ->
                    if (!uiState.isEditMode) {
                        contextMenuTile = tile
                        contextMenuExpanded = true
                    }
                },
                onExploreTile = viewModel::explore,
                onEmptySpaceClick = { q, r ->
                    if (uiState.isEditMode) viewModel.onEmptySpaceClickInEditMode(q, r)
                },
                onStateChanged = { scale, offset ->
                    currentScale = scale
                    currentOffset = offset
                },
                onTimeSkip = {
                    viewModel.startNewDay()
                },
                modifier = Modifier
                    .fillMaxSize()
                    .dragAndDropTarget(
                        shouldStartDragAndDrop = { event ->
                            event.mimeTypes().contains("text/plain")
                        },
                        target = dndTarget
                    )
            )

            // Menú contextual de Long Press
            DropdownMenu(
                expanded = contextMenuExpanded,
                onDismissRequest = { contextMenuExpanded = false }
            ) {
                contextMenuTile?.let { tile ->
                    DropdownMenuItem(
                        text = { Text("Mover equipo aquí") },
                        leadingIcon = { Icon(Icons.Default.DirectionsRun, null) },
                        onClick = {
                            viewModel.moveParty(tile.q, tile.r)
                            contextMenuExpanded = false
                        }
                    )
                    if (!tile.isExplored) {
                        DropdownMenuItem(
                            text = { Text("Revelar hexágono") },
                            leadingIcon = { Icon(Icons.Default.Visibility, null) },
                            onClick = {
                                viewModel.explore(tile)
                                contextMenuExpanded = false
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Ver detalles") },
                        leadingIcon = { Icon(Icons.Default.Info, null) },
                        onClick = {
                            viewModel.selectTile(tile)
                            contextMenuExpanded = false
                        }
                    )
                }
            }

            // Result Overlay for automatic rolls
            if (uiState.showRollResultDialog && uiState.lastRollResult != null) {
                AlertDialog(
                    onDismissRequest = viewModel::dismissRollDialog,
                    title = { Text(uiState.rollResultContext) },
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .dragAndDropSource {
                                    detectTapGestures(
                                        onLongPress = {
                                            startTransfer(
                                                DragAndDropTransferData(
                                                    clipData = ClipData.newPlainText(
                                                        "table_result",
                                                        uiState.lastRollResult!!.resolvedText
                                                    )
                                                )
                                            )
                                        }
                                    )
                                }
                        ) {
                            Text(
                                uiState.lastRollResult!!.resolvedText,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Resultado: ${uiState.lastRollResult!!.rollValue} (Copiado al portapapeles)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = viewModel::dismissRollDialog) { Text("OK") }
                    }
                )
            }

            // Toolbar de pinceles en modo edición
            if (uiState.isEditMode) {
                Box(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)) {
                    Surface(
                        tonalElevation = 8.dp,
                        shadowElevation = 8.dp,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        TerrainBrushToolbar(
                            brushes = uiState.brushes,
                            activeBrush = uiState.activeBrush,
                            onBrushSelect = viewModel::selectBrush
                        )
                    }
                }
            }
        }
    }

    if (uiState.showTileSheet && uiState.selectedTile != null) {
        ModalBottomSheet(onDismissRequest = viewModel::dismissTileSheet) {
            TileSessionDetails(
                tile = uiState.selectedTile!!,
                isPartyHere = uiState.partyLocation == (uiState.selectedTile!!.q to uiState.selectedTile!!.r),
                pois = uiState.pois.filter { it.tileQ == uiState.selectedTile!!.q && it.tileR == uiState.selectedTile!!.r },
                onMoveParty = { viewModel.moveParty(uiState.selectedTile!!.q, uiState.selectedTile!!.r) },
                onExplore = { viewModel.explore(uiState.selectedTile!!) },
                onReconnoiter = { viewModel.reconnoiter(uiState.selectedTile!!) },
                onMap = { viewModel.mapTile(uiState.selectedTile!!) },
                onRollTable = viewModel::rollPoiTable
            )
        }
    }

    if (showResourcesPanel) {
        ModalBottomSheet(
            onDismissRequest = { showResourcesPanel = false },
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(modifier = Modifier.padding(bottom = 32.dp)) {
                MythicLinkSection(
                    currentSession = uiState.linkedMythicSession,
                    allSessions = uiState.allMythicSessions,
                    onLink = {
                        viewModel.linkMythicSession(it)
                        showResourcesPanel = false
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                SessionResourcesPanel(
                    pinnedTables = uiState.pinnedTables,
                    pinnedDecks = uiState.pinnedDecks,
                    pinnedRules = uiState.pinnedRules,
                    allTables = uiState.allTables,
                    allRules = uiState.allRules,
                    searchQuery = uiState.resourceSearchQuery,
                    drawnCardsByDeck = uiState.drawnCardsByDeck,
                    recentRolls = uiState.recentRolls,
                    onSearchQueryChanged = viewModel::onResourceSearchQueryChanged,
                    onRollTable = viewModel::rollTableManually,
                    onDrawCard = viewModel::drawCardFromDeck,
                    onDiscardCard = viewModel::discardCard,
                    onResetDeck = viewModel::resetDeck
                )
            }
        }
    }

    if (uiState.showJournalPanel) {
        ModalBottomSheet(onDismissRequest = viewModel::dismissJournalPanel) {
            JournalPanel(
                days = uiState.allDays,
                onUpdateNotes = viewModel::updateDayNotes,
                onExport = viewModel::exportSummary
            )
        }
    }

    if (uiState.showExportDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissExportDialog,
            title = { Text("Resumen de Campaña") },
            text = {
                OutlinedTextField(
                    value = uiState.exportText,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth().height(300.dp),
                    textStyle = MaterialTheme.typography.bodySmall
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    clipboard.setText(AnnotatedString(uiState.exportText))
                    viewModel.dismissExportDialog()
                }) { Text("Copiar") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissExportDialog) { Text("Cerrar") }
            }
        )
    }
}

@Composable
private fun MythicLinkSection(
    currentSession: MythicSession?,
    allSessions: List<MythicSession>,
    onLink: (Long?) -> Unit
) {
    var showPicker by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Vinculación Mythic", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        
        OutlinedCard(
            onClick = { showPicker = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.AutoMirrored.Filled.MenuBook, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        currentSession?.name ?: "Vincular a una Crónica Mythic",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (currentSession != null) FontWeight.Bold else FontWeight.Normal
                    )
                    if (currentSession != null) {
                        Text("Factor de Caos: ${currentSession.chaosFactor}", style = MaterialTheme.typography.bodySmall)
                    }
                }
                if (currentSession != null) {
                    IconButton(onClick = { onLink(null) }) {
                        Icon(Icons.Default.LinkOff, null, tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }

    if (showPicker) {
        AlertDialog(
            onDismissRequest = { showPicker = false },
            title = { Text("Seleccionar Crónica Mythic") },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    if (allSessions.isEmpty()) {
                        item { Text("No hay sesiones Mythic creadas.") }
                    }
                    items(allSessions) { mythic ->
                        ListItem(
                            headlineContent = { Text(mythic.name) },
                            supportingContent = { Text("Caos ${mythic.chaosFactor} • Escena ${mythic.sceneNumber}") },
                            modifier = Modifier.clickable { 
                                onLink(mythic.id)
                                showPicker = false 
                            }
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showPicker = false }) { Text("Cerrar") } }
        )
    }
}

@Composable
private fun SessionBottomBar(
    currentDay: Int,
    activitiesUsed: Int,
    maxActivities: Int,
    onStartDay: () -> Unit
) {
    Surface(tonalElevation = 4.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Día $currentDay", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Actividades: $activitiesUsed / $maxActivities",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (activitiesUsed >= maxActivities) MaterialTheme.colorScheme.error else Color.Unspecified
                )
            }
            Button(onClick = onStartDay) {
                Text("Siguiente Día")
            }
        }
    }
}

@Composable
private fun TileSessionDetails(
    tile: HexTile,
    isPartyHere: Boolean,
    pois: List<HexPoi>,
    onMoveParty: () -> Unit,
    onExplore: () -> Unit,
    onReconnoiter: () -> Unit,
    onMap: () -> Unit,
    onRollTable: (Long) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp).padding(bottom = 24.dp)) {
        Text("Hex (${tile.q}, ${tile.r})", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(tile.terrainLabel, style = MaterialTheme.typography.bodyMedium)
        
        Spacer(Modifier.height(16.dp))
        
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!isPartyHere) {
                Button(onClick = onMoveParty, modifier = Modifier.weight(1f)) {
                    Text("Mover aquí")
                }
            }
            if (!tile.isExplored) {
                OutlinedButton(onClick = onExplore, modifier = Modifier.weight(1f)) {
                    Text("Explorar")
                }
            }
        }
        
        Spacer(Modifier.height(8.dp))
        
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!tile.isReconnoitered) {
                OutlinedButton(onClick = onReconnoiter, modifier = Modifier.weight(1f)) {
                    Text("Reconocer")
                }
            }
            if (tile.isReconnoitered && !tile.isMapped) {
                OutlinedButton(onClick = onMap, modifier = Modifier.weight(1f)) {
                    Text("Mapear")
                }
            }
        }

        if (pois.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text("Puntos de Interés", style = MaterialTheme.typography.titleSmall)
            pois.forEach { poi ->
                ListItem(
                    headlineContent = { Text(poi.name) },
                    supportingContent = { Text(poi.type.name) },
                    trailingContent = {
                        poi.tableId?.let { tableId ->
                            IconButton(onClick = { onRollTable(tableId) }) {
                                Icon(Icons.Default.Casino, null)
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun SessionResourcesPanel(
    pinnedTables: List<RandomTable>,
    pinnedDecks: List<CardStack>,
    pinnedRules: List<SystemRule>,
    allTables: List<RandomTable>,
    allRules: List<SystemRule>,
    searchQuery: String,
    drawnCardsByDeck: Map<Long, List<Card>>,
    recentRolls: List<TableRollResult>,
    onSearchQueryChanged: (String) -> Unit,
    onRollTable: (Long) -> Unit,
    onDrawCard: (Long) -> Unit,
    onDiscardCard: (Long) -> Unit,
    onResetDeck: (Long) -> Unit
) {
    HexResourcesPanel(
        pinnedTables = pinnedTables,
        pinnedDecks = pinnedDecks,
        pinnedRules = pinnedRules,
        allTables = allTables,
        allRules = allRules,
        searchQuery = searchQuery,
        drawnCardsByDeck = drawnCardsByDeck,
        recentRolls = recentRolls,
        onSearchQueryChanged = onSearchQueryChanged,
        onRollTable = onRollTable,
        onDrawCard = onDrawCard,
        onDiscardCard = onDiscardCard,
        onResetDeck = onResetDeck
    )
}

@Composable
private fun JournalPanel(
    days: List<HexDay>,
    onUpdateNotes: (HexDay, String) -> Unit,
    onExport: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Diario de Campaña", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            IconButton(onClick = onExport) { Icon(Icons.Default.Share, null) }
        }
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(days) { day ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Día ${day.dayNumber}", fontWeight = FontWeight.Bold)
                        day.activitiesLog.forEach { activity ->
                            Text("• ${activity.description}", style = MaterialTheme.typography.bodySmall)
                        }
                        OutlinedTextField(
                            value = day.notes,
                            onValueChange = { onUpdateNotes(day, it) },
                            label = { Text("Notas") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
