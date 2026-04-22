package com.deckapp.feature.hexploration

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.style.TextOverflow
import com.deckapp.core.model.HexPoi
import com.deckapp.core.model.HexTile
import com.deckapp.core.model.PoiType
import com.deckapp.core.model.RandomTable
import com.deckapp.core.model.TerrainBrush
import com.deckapp.feature.hexploration.components.HexCanvasMode
import com.deckapp.feature.hexploration.components.HexGridCanvas
import com.deckapp.feature.hexploration.components.TerrainBrushToolbar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HexMapEditorScreen(
    onStartSession: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: HexMapEditorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (uiState.showAddPoiDialog) {
        AddPoiDialog(
            name = uiState.newPoiName,
            type = uiState.newPoiType,
            description = uiState.newPoiDescription,
            onNameChange = viewModel::onPoiNameChange,
            onTypeChange = viewModel::onPoiTypeChange,
            onDescChange = viewModel::onPoiDescChange,
            onConfirm = viewModel::savePoi,
            onDismiss = viewModel::dismissAddPoiDialog
        )
    }

    if (uiState.showAddBrushDialog) {
        AddBrushDialog(
            onConfirm = viewModel::addCustomBrush,
            onDismiss = viewModel::dismissAddBrushDialog
        )
    }

    if (uiState.showWeatherTablePicker) {
        HexTablePickerDialog(
            tables = uiState.allTables,
            onDismiss = viewModel::dismissWeatherTablePicker,
            onTableSelected = { viewModel.setWeatherTable(it) },
            onClear = { viewModel.setWeatherTable(null) }
        )
    }

    if (uiState.showTravelTablePicker) {
        HexTablePickerDialog(
            tables = uiState.allTables,
            onDismiss = viewModel::dismissTravelTablePicker,
            onTableSelected = { viewModel.setTravelTable(it) },
            onClear = { viewModel.setTravelTable(null) }
        )
    }

    var showMapSettingsSheet by remember { mutableStateOf(false) }
    var showResourcesSheet by remember { mutableStateOf(false) }

    val mapSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    if (showMapSettingsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showMapSettingsSheet = false },
            sheetState = mapSheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            MapSettingsSheet(
                mapNotes = uiState.mapNotes,
                maxActivitiesPerDay = uiState.maxActivitiesPerDay,
                weatherTableName = uiState.allTables.find { it.id == uiState.weatherTableId }?.name,
                travelTableName = uiState.allTables.find { it.id == uiState.travelEventTableId }?.name,
                terrainTableConfig = uiState.terrainTableConfig,
                allTables = uiState.allTables,
                brushes = uiState.brushes,
                onNotesChange = viewModel::updateMapNotes,
                onMaxActivitiesChange = viewModel::updateMaxActivities,
                onPickWeatherTable = viewModel::showWeatherTablePicker,
                onClearWeatherTable = { viewModel.setWeatherTable(null) },
                onPickTravelTable = viewModel::showTravelTablePicker,
                onClearTravelTable = { viewModel.setTravelTable(null) },
                onSetTerrainTable = viewModel::setTerrainTable
            )
        }
    }

    if (showResourcesSheet) {
        ModalBottomSheet(
            onDismissRequest = { showResourcesSheet = false },
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            MapResourcesSheet(
                sessionResources = uiState.sessionResources,
                allTables = uiState.allTables,
                allDecks = uiState.allDecks,
                allRules = uiState.allRules,
                onToggleTable = viewModel::toggleTableInResources,
                onToggleDeck = viewModel::toggleDeckInResources,
                onToggleRule = viewModel::toggleRuleInResources
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.mapName, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::undo, enabled = uiState.canUndo) {
                        Icon(
                            Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Deshacer",
                            tint = if (uiState.canUndo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                    }
                    IconButton(onClick = viewModel::redo, enabled = uiState.canRedo) {
                        Icon(
                            Icons.AutoMirrored.Filled.Redo,
                            contentDescription = "Rehacer",
                            tint = if (uiState.canRedo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                    }
                    IconButton(onClick = { showResourcesSheet = true }) {
                        Icon(Icons.AutoMirrored.Filled.LibraryBooks, contentDescription = "Recursos de sesión")
                    }
                    IconButton(onClick = { showMapSettingsSheet = true }) {
                        Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = "Configuración del mapa")
                    }
                    IconButton(onClick = viewModel::toggleCoordinates) {
                        Icon(
                            Icons.Default.GridOn,
                            contentDescription = "Toggle coordenadas",
                            tint = if (uiState.showCoordinates) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = viewModel::addRing) {
                        Icon(Icons.Default.Layers, contentDescription = "Agregar Anillo")
                    }
                    FilledTonalButton(
                        onClick = { onStartSession(uiState.mapId) },
                        modifier = Modifier.padding(end = 8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Iniciar")
                    }
                }
            )
        },
        bottomBar = {
            Column {
                // Inline Details Panel
                AnimatedVisibility(
                    visible = uiState.showTileSheet && uiState.selectedTile != null,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    if (uiState.selectedTile != null) {
                        EditorTileDetails(
                            tile = uiState.selectedTile!!,
                            pois = uiState.pois.filter { it.tileQ == uiState.selectedTile!!.q && it.tileR == uiState.selectedTile!!.r },
                            onDismiss = viewModel::dismissTileSheet,
                            onAddPoi = { viewModel.showAddPoiDialog() },
                            onDeletePoi = viewModel::deletePoi,
                            onDmNotesChange = { viewModel.updateTileNotes(uiState.selectedTile!!, it, uiState.selectedTile!!.playerNotes) },
                            onPlayerNotesChange = { viewModel.updateTileNotes(uiState.selectedTile!!, uiState.selectedTile!!.dmNotes, it) }
                        )
                    }
                }
                TerrainBrushToolbar(
                    brushes = uiState.brushes,
                    activeBrush = uiState.activeBrush,
                    onBrushSelect = viewModel::selectBrush,
                    onAddBrush = { viewModel.showAddBrushDialog() }
                )
            }
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        HexGridCanvas(
            tiles = uiState.tiles,
            pois = uiState.pois,
            mode = HexCanvasMode.DESIGN,
            selectedTile = uiState.selectedTile,
            onTileClick = viewModel::onTileClick,
            onTileLongPress = viewModel::onTileLongPress,
            onEmptySpaceClick = viewModel::onEmptySpaceClick,
            showCoordinates = uiState.showCoordinates,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        )
    }
}

@Composable
private fun EditorTileDetails(
    tile: HexTile,
    pois: List<HexPoi>,
    onDismiss: () -> Unit,
    onAddPoi: () -> Unit,
    onDeletePoi: (Long) -> Unit,
    onDmNotesChange: (String) -> Unit,
    onPlayerNotesChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 400.dp)
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp, top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Hex (${tile.q}, ${tile.r}) — ${tile.terrainLabel.ifBlank { "Sin etiqueta" }}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Cerrar")
            }
        }
        
        Text(
            "Coste de movimiento: ${if (tile.terrainCost == 0) "Infranqueable" else "${tile.terrainCost} actividad(es)"}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        HorizontalDivider()

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("POIs", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            IconButton(onClick = onAddPoi) {
                Icon(Icons.Default.Add, contentDescription = "Agregar POI")
            }
        }

        if (pois.isEmpty()) {
            Text(
                "Sin puntos de interés en este hex",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            pois.forEach { poi ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(poi.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text(poi.type.name.lowercase().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { onDeletePoi(poi.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Borrar POI",
                            tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        HorizontalDivider()

        Text(
            text = "Notas del DJ",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
        OutlinedTextField(
            value = tile.dmNotes,
            onValueChange = onDmNotesChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Solo visible para el DJ...") },
            textStyle = MaterialTheme.typography.bodySmall,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Notas para Jugadores",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.secondary
        )
        OutlinedTextField(
            value = tile.playerNotes,
            onValueChange = onPlayerNotesChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Visible para todos los jugadores...") },
            textStyle = MaterialTheme.typography.bodySmall,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MapResourcesSheet(
    sessionResources: com.deckapp.core.model.HexSessionResources,
    allTables: List<RandomTable>,
    allDecks: List<com.deckapp.core.model.CardStack>,
    allRules: List<com.deckapp.core.model.SystemRule>,
    onToggleTable: (Long) -> Unit,
    onToggleDeck: (Long) -> Unit,
    onToggleRule: (Long) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    
    val filteredTables = remember(allTables, searchQuery) {
        allTables.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }
    val filteredDecks = remember(allDecks, searchQuery) {
        allDecks.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }
    val filteredRules = remember(allRules, searchQuery) {
        allRules.filter { it.title.contains(searchQuery, ignoreCase = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 600.dp)
            .padding(bottom = 16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Recursos de sesión",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Configura qué herramientas tendrás a mano durante la exploración",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(Modifier.height(16.dp))
            
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Buscar recursos...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = if (searchQuery.isNotEmpty()) {
                    { IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Close, null) } }
                } else null,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
        }

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            divider = {}
        ) {
            val tabs = listOf(
                "Mazos" to sessionResources.deckIds.size,
                "Tablas" to sessionResources.tableIds.size,
                "Reglas" to sessionResources.ruleIds.size
            )
            tabs.forEachIndexed { index, (label, count) ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(label)
                            if (count > 0) {
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(start = 6.dp)
                                ) {
                                    Text(count.toString(), color = MaterialTheme.colorScheme.onPrimary)
                                }
                            }
                        }
                    }
                )
            }
        }

        HorizontalDivider(thickness = 0.5.dp)

        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> ResourceGrid(
                    items = filteredDecks,
                    selectedIds = sessionResources.deckIds,
                    onToggle = onToggleDeck,
                    emptyMessage = "No se encontraron mazos",
                    itemContent = { deck -> ResourceCardContent(deck.name, "Mazo") }
                )
                1 -> ResourceGrid(
                    items = filteredTables,
                    selectedIds = sessionResources.tableIds,
                    onToggle = onToggleTable,
                    emptyMessage = "No se encontraron tablas",
                    itemContent = { table -> ResourceCardContent(table.name, table.category) }
                )
                2 -> ResourceGrid(
                    items = filteredRules,
                    selectedIds = sessionResources.ruleIds,
                    onToggle = onToggleRule,
                    emptyMessage = "No se encontraron reglas",
                    itemContent = { rule -> ResourceCardContent(rule.title, rule.category) }
                )
            }
        }
    }
}

@Composable
private fun <T> ResourceGrid(
    items: List<T>,
    selectedIds: List<Long>,
    onToggle: (Long) -> Unit,
    emptyMessage: String,
    itemContent: @Composable (T) -> Unit
) {
    if (items.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(emptyMessage, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
            columns = androidx.compose.foundation.lazy.grid.GridCells.Adaptive(160.dp),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(items.size) { index ->
                val item = items[index]
                val id = when (item) {
                    is com.deckapp.core.model.RandomTable -> item.id
                    is com.deckapp.core.model.CardStack -> item.id
                    is com.deckapp.core.model.SystemRule -> item.id
                    else -> 0L
                }
                val isSelected = id in selectedIds
                
                Card(
                    onClick = { onToggle(id) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer 
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
                ) {
                    Box {
                        itemContent(item)
                        if (isSelected) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResourceCardContent(title: String, subtitle: String?) {
    Column(modifier = Modifier.padding(12.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MapSettingsSheet(
    mapNotes: String,
    maxActivitiesPerDay: Int,
    weatherTableName: String?,
    travelTableName: String?,
    terrainTableConfig: String,
    allTables: List<RandomTable>,
    brushes: List<TerrainBrush>,
    onNotesChange: (String) -> Unit,
    onMaxActivitiesChange: (Int) -> Unit,
    onPickWeatherTable: () -> Unit,
    onClearWeatherTable: () -> Unit,
    onPickTravelTable: () -> Unit,
    onClearTravelTable: () -> Unit,
    onSetTerrainTable: (String, Long?) -> Unit
) {
    var notes by remember(mapNotes) { mutableStateOf(mapNotes) }
    var showTerrainSection by remember { mutableStateOf(false) }
    val terrainConfig: Map<String, Long> = remember(terrainTableConfig) { parseTerrainConfig(terrainTableConfig) }
    val terrainLabels: List<String> = remember(brushes) {
        brushes.filter { it.cost >= 0 && it.label.isNotBlank() }.map { it.label }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 32.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column {
            Text("Configuración del mapa", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
        }

        Column {
            Text("Actividades por día: $maxActivitiesPerDay", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Slider(
                value = maxActivitiesPerDay.toFloat(),
                onValueChange = { onMaxActivitiesChange(it.toInt()) },
                valueRange = 1f..30f,
                steps = 28,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Column {
            HorizontalDivider()
            Spacer(Modifier.height(4.dp))
            Text("Tablas automáticas", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        }

        TableLinkRow(
            label = "Clima (inicio de día)",
            currentTableName = weatherTableName,
            onPick = onPickWeatherTable,
            onClear = onClearWeatherTable
        )

        TableLinkRow(
            label = "Eventos de viaje (movimiento)",
            currentTableName = travelTableName,
            onPick = onPickTravelTable,
            onClear = onClearTravelTable
        )

        if (terrainLabels.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showTerrainSection = !showTerrainSection },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Tablas de encuentro por terreno",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    if (showTerrainSection) "▲" else "▼",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (showTerrainSection) {
                terrainLabels.forEach { labelStr: String ->
                    TerrainConfigRow(
                        label = labelStr,
                        terrainConfig = terrainConfig,
                        allTables = allTables,
                        onSetTerrainTable = onSetTerrainTable
                    )
                }
            }
        }

        NotesSection(
            notes = mapNotes,
            onNotesChange = onNotesChange
        )
    }
}

@Composable
private fun AddBrushDialog(
    onConfirm: (label: String, cost: Int, color: Long) -> Unit,
    onDismiss: () -> Unit
) {
    var label by remember { mutableStateOf("") }
    var cost by remember { mutableStateOf(1) }
    var selectedColorIndex by remember { mutableStateOf(0) }
    val paletteColors = listOf(
        0xFF7CB87BL, 0xFF4A90D9L, 0xFF8B7355L, 0xFF9E9E9EL,
        0xFFD4C875L, 0xFF3A7D44L, 0xFF8B0000L, 0xFF9370DBL,
        0xFFFF6B35L, 0xFF2C3E50L
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuevo terreno") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Nombre del terreno") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Coste de movimiento: $cost", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Slider(
                    value = cost.toFloat(),
                    onValueChange = { cost = it.toInt() },
                    valueRange = 0f..3f,
                    steps = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Color:", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    paletteColors.forEachIndexed { idx, color ->
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(color))
                                .then(if (idx == selectedColorIndex)
                                    Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                else Modifier)
                                .clickable { selectedColorIndex = idx }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(label, cost, paletteColors[selectedColorIndex]) },
                enabled = label.isNotBlank()
            ) { Text("Agregar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun ResourceToggleRow(
    name: String,
    subtitle: String?,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.bodyMedium)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        androidx.compose.material3.Checkbox(checked = isSelected, onCheckedChange = { onToggle() })
    }
}

@Composable
private fun NotesSection(
    notes: String,
    onNotesChange: (String) -> Unit
) {
    var notesState by remember(notes) { mutableStateOf(notes) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        HorizontalDivider()
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = notesState,
            onValueChange = { notesState = it },
            label = { Text("Notas del mapa (DM)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            maxLines = 8
        )
        TextButton(onClick = { onNotesChange(notesState) }) { Text("Guardar notas") }
    }
}

@Composable
private fun TerrainConfigRow(
    label: String,
    terrainConfig: Map<String, Long>,
    allTables: List<RandomTable>,
    onSetTerrainTable: (String, Long?) -> Unit
) {
    val linkedId = terrainConfig[label]
    val linkedName = allTables.find { it.id == linkedId }?.name
    var showPickerForTerrain by remember { mutableStateOf(false) }
    if (showPickerForTerrain) {
        HexTablePickerDialog(
            tables = allTables,
            onDismiss = { showPickerForTerrain = false },
            onTableSelected = { id ->
                onSetTerrainTable(label, id)
                showPickerForTerrain = false
            },
            onClear = {
                onSetTerrainTable(label, null)
                showPickerForTerrain = false
            }
        )
    }
    TableLinkRow(
        label = label,
        currentTableName = linkedName,
        onPick = { showPickerForTerrain = true },
        onClear = { onSetTerrainTable(label, null) }
    )
}

@Composable
private fun TableLinkRow(
    label: String,
    currentTableName: String?,
    onPick: () -> Unit,
    onClear: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(
                currentTableName ?: "Sin tabla",
                style = MaterialTheme.typography.bodySmall,
                color = if (currentTableName != null) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        TextButton(onClick = onPick) { Text("Cambiar") }
        if (currentTableName != null) {
            IconButton(onClick = onClear) {
                Icon(Icons.Default.Close, contentDescription = "Quitar tabla",
                    modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun HexTablePickerDialog(
    tables: List<RandomTable>,
    onDismiss: () -> Unit,
    onTableSelected: (Long) -> Unit,
    onClear: () -> Unit = {}
) {
    var search by remember { mutableStateOf("") }
    val filtered = remember(search, tables) {
        if (search.isBlank()) tables
        else tables.filter { it.name.contains(search, ignoreCase = true) }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Seleccionar tabla") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    label = { Text("Buscar...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                LazyColumn(modifier = Modifier.height(300.dp)) {
                    items(filtered) { table ->
                        Text(
                            text = table.name,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onTableSelected(table.id) }
                                .padding(vertical = 8.dp, horizontal = 4.dp)
                        )
                        HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun AddPoiDialog(
    name: String,
    type: PoiType,
    description: String,
    onNameChange: (String) -> Unit,
    onTypeChange: (PoiType) -> Unit,
    onDescChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    var typeMenuExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Agregar POI") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text("Nombre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Box {
                    OutlinedTextField(
                        value = type.name.lowercase().replaceFirstChar { it.uppercase() },
                        onValueChange = {},
                        label = { Text("Tipo") },
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth().clickable { typeMenuExpanded = true }
                    )
                    DropdownMenu(expanded = typeMenuExpanded, onDismissRequest = { typeMenuExpanded = false }) {
                        PoiType.entries.forEach { poiType ->
                            DropdownMenuItem(
                                text = { Text(poiType.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                onClick = { onTypeChange(poiType); typeMenuExpanded = false }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = description,
                    onValueChange = onDescChange,
                    label = { Text("Descripción (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = name.isNotBlank()) { Text("Agregar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
