package com.deckapp.feature.hexploration

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.lazy.LazyColumn
import com.deckapp.core.model.HexPoi
import com.deckapp.core.model.HexTile
import com.deckapp.core.model.PoiType
import com.deckapp.core.model.RandomTable
import com.deckapp.feature.hexploration.components.HexCanvasMode
import com.deckapp.feature.hexploration.components.HexGridCanvas

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
                sessionResources = uiState.sessionResources,
                allDecks = uiState.allDecks,
                allRules = uiState.allRules,
                onNotesChange = viewModel::updateMapNotes,
                onMaxActivitiesChange = viewModel::updateMaxActivities,
                onPickWeatherTable = viewModel::showWeatherTablePicker,
                onClearWeatherTable = { viewModel.setWeatherTable(null) },
                onPickTravelTable = viewModel::showTravelTablePicker,
                onClearTravelTable = { viewModel.setTravelTable(null) },
                onSetTerrainTable = viewModel::setTerrainTable,
                onToggleTable = viewModel::toggleTableInResources,
                onToggleDeck = viewModel::toggleDeckInResources,
                onToggleRule = viewModel::toggleRuleInResources
            )
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
                    IconButton(onClick = { showMapSettingsSheet = true }) {
                        Icon(Icons.Default.Notes, contentDescription = "Configuración del mapa")
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
                            onDeletePoi = viewModel::deletePoi
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
private fun TerrainBrushToolbar(
    brushes: List<TerrainBrush>,
    activeBrush: TerrainBrush,
    onBrushSelect: (TerrainBrush) -> Unit,
    onAddBrush: () -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(vertical = 8.dp),
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onAddBrush() }
                    .padding(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Nuevo terreno",
                        modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.height(4.dp))
                Text("Nuevo", style = MaterialTheme.typography.labelSmall, maxLines = 1)
            }
        }
        items(brushes) { brush ->
            val isActive = brush == activeBrush
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onBrushSelect(brush) }
                    .background(
                        if (isActive) MaterialTheme.colorScheme.primaryContainer
                        else Color.Transparent
                    )
                    .padding(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(brush.color))
                        .then(
                            if (isActive) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            else Modifier
                        )
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = brush.label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun EditorTileDetails(
    tile: HexTile,
    pois: List<HexPoi>,
    onDismiss: () -> Unit,
    onAddPoi: () -> Unit,
    onDeletePoi: (Long) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
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
    sessionResources: com.deckapp.core.model.HexSessionResources,
    allDecks: List<com.deckapp.core.model.CardStack>,
    allRules: List<com.deckapp.core.model.SystemRule>,
    onNotesChange: (String) -> Unit,
    onMaxActivitiesChange: (Int) -> Unit,
    onPickWeatherTable: () -> Unit,
    onClearWeatherTable: () -> Unit,
    onPickTravelTable: () -> Unit,
    onClearTravelTable: () -> Unit,
    onSetTerrainTable: (String, Long?) -> Unit,
    onToggleTable: (Long) -> Unit,
    onToggleDeck: (Long) -> Unit,
    onToggleRule: (Long) -> Unit
) {
    var notes by remember(mapNotes) { mutableStateOf(mapNotes) }
    var showTerrainSection by remember { mutableStateOf(false) }
    val terrainConfig = remember(terrainTableConfig) { parseTerrainConfig(terrainTableConfig) }
    val terrainLabels = remember(brushes) {
        brushes.filter { it.cost >= 0 && it.label.isNotBlank() }.map { it.label }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Configuración del mapa", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
        }
        item {
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
        item {
            HorizontalDivider()
            Spacer(Modifier.height(4.dp))
            Text("Tablas automáticas", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        }
        item {
            TableLinkRow(
                label = "Clima (inicio de día)",
                currentTableName = weatherTableName,
                onPick = onPickWeatherTable,
                onClear = onClearWeatherTable
            )
        }
        item {
            TableLinkRow(
                label = "Eventos de viaje (movimiento)",
                currentTableName = travelTableName,
                onPick = onPickTravelTable,
                onClear = onClearTravelTable
            )
        }
        if (terrainLabels.isNotEmpty()) {
            item {
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
            }
            if (showTerrainSection) {
                items(terrainLabels) { label ->
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
            }
        }
        item {
            HorizontalDivider()
            Spacer(Modifier.height(4.dp))
            Text("Recursos de sesión", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text("Seleccioná qué tendrás a mano durante la sesión",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (allTables.isNotEmpty()) {
            item {
                Text("Tablas", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items(allTables) { table ->
                ResourceToggleRow(
                    name = table.name,
                    subtitle = table.category.takeIf { it.isNotBlank() },
                    isSelected = table.id in sessionResources.tableIds,
                    onToggle = { onToggleTable(table.id) }
                )
            }
        }
        if (allDecks.isNotEmpty()) {
            item {
                Spacer(Modifier.height(4.dp))
                Text("Mazos", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items(allDecks) { deck ->
                ResourceToggleRow(
                    name = deck.name,
                    subtitle = null,
                    isSelected = deck.id in sessionResources.deckIds,
                    onToggle = { onToggleDeck(deck.id) }
                )
            }
        }
        if (allRules.isNotEmpty()) {
            item {
                Spacer(Modifier.height(4.dp))
                Text("Reglas de referencia", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items(allRules) { rule ->
                ResourceToggleRow(
                    name = rule.title,
                    subtitle = rule.category.takeIf { it.isNotBlank() },
                    isSelected = rule.id in sessionResources.ruleIds,
                    onToggle = { onToggleRule(rule.id) }
                )
            }
        }
        item {
            HorizontalDivider()
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notas del mapa (DM)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 8
            )
            TextButton(onClick = { onNotesChange(notes) }) { Text("Guardar notas") }
        }
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
