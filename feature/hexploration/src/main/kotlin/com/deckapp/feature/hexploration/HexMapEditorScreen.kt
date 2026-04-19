package com.deckapp.feature.hexploration

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Layers
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
import com.deckapp.core.model.HexPoi
import com.deckapp.core.model.HexTile
import com.deckapp.core.model.PoiType
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

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    if (uiState.showTileSheet && uiState.selectedTile != null) {
        ModalBottomSheet(
            onDismissRequest = viewModel::dismissTileSheet,
            sheetState = sheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            TileDetailSheet(
                tile = uiState.selectedTile!!,
                pois = uiState.pois.filter { it.tileQ == uiState.selectedTile!!.q && it.tileR == uiState.selectedTile!!.r },
                onSaveNotes = { dm, player -> viewModel.updateTileNotes(uiState.selectedTile!!, dm, player) },
                onAddPoi = viewModel::showAddPoiDialog,
                onDeletePoi = viewModel::deletePoi
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
            TerrainBrushToolbar(
                brushes = uiState.brushes,
                activeBrush = uiState.activeBrush,
                onBrushSelect = viewModel::selectBrush
            )
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
    onBrushSelect: (TerrainBrush) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(vertical = 8.dp),
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
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
private fun TileDetailSheet(
    tile: HexTile,
    pois: List<HexPoi>,
    onSaveNotes: (dmNotes: String, playerNotes: String) -> Unit,
    onAddPoi: () -> Unit,
    onDeletePoi: (Long) -> Unit
) {
    var dmNotes by remember(tile.q, tile.r) { mutableStateOf(tile.dmNotes) }
    var playerNotes by remember(tile.q, tile.r) { mutableStateOf(tile.playerNotes) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Hex (${tile.q}, ${tile.r}) — ${tile.terrainLabel.ifBlank { "Sin etiqueta" }}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Coste de movimiento: ${if (tile.terrainCost == 0) "Infranqueable" else "${tile.terrainCost} actividad(es)"}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        HorizontalDivider()

        OutlinedTextField(
            value = dmNotes,
            onValueChange = { dmNotes = it },
            label = { Text("Notas del DM (privadas)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 4
        )
        OutlinedTextField(
            value = playerNotes,
            onValueChange = { playerNotes = it },
            label = { Text("Notas para jugadores") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 4
        )
        TextButton(onClick = { onSaveNotes(dmNotes, playerNotes) }) {
            Text("Guardar notas")
        }

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
