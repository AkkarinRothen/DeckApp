package com.deckapp.feature.hexploration

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deckapp.core.model.HexActivityEntry
import com.deckapp.core.model.HexPoi
import com.deckapp.core.model.HexTile
import com.deckapp.feature.hexploration.components.HexCanvasMode
import com.deckapp.feature.hexploration.components.HexGridCanvas
import com.deckapp.feature.hexploration.components.hexDistance

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HexMapSessionScreen(
    onNavigateToEncounter: (Long) -> Unit,
    onNavigateToEncounterList: () -> Unit,
    onBack: () -> Unit,
    viewModel: HexMapSessionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (uiState.showRollResultDialog && uiState.lastRollResult != null) {
        val result = uiState.lastRollResult!!
        AlertDialog(
            onDismissRequest = viewModel::dismissRollResult,
            title = { Text(result.tableName) },
            text = {
                Column {
                    Text("Resultado: ${result.rollValue}", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Text(result.resolvedText, style = MaterialTheme.typography.bodyLarge)
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::dismissRollResult) { Text("Cerrar") }
            }
        )
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    if (uiState.showTileSheet && uiState.selectedTile != null) {
        ModalBottomSheet(
            onDismissRequest = viewModel::dismissTileSheet,
            sheetState = sheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            SessionTileSheet(
                tile = uiState.selectedTile!!,
                pois = uiState.pois.filter {
                    it.tileQ == uiState.selectedTile!!.q && it.tileR == uiState.selectedTile!!.r
                },
                partyLocation = uiState.partyLocation,
                activitiesUsed = uiState.activitiesUsedToday,
                maxActivities = uiState.maxActivitiesPerDay,
                todayLog = uiState.todayLog,
                onExplore = { viewModel.explore(uiState.selectedTile!!) },
                onReconnoiter = { viewModel.reconnoiter(uiState.selectedTile!!) },
                onMap = { viewModel.mapTile(uiState.selectedTile!!) },
                onRollTable = viewModel::rollPoiTable,
                onStartEncounter = { encounterId ->
                    if (encounterId != null) onNavigateToEncounter(encounterId)
                    else onNavigateToEncounterList()
                    viewModel.dismissTileSheet()
                }
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(uiState.mapName, maxLines = 1)
                        val dayLabel = if (uiState.currentDay != null)
                            "Día ${uiState.currentDay!!.dayNumber} · ${uiState.activitiesUsedToday}/${uiState.maxActivitiesPerDay} actividades"
                        else "Sin día activo — toca + para empezar"
                        Text(dayLabel, style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = viewModel::startNewDay) {
                Icon(Icons.Default.Add, contentDescription = "Nuevo día")
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
            mode = HexCanvasMode.SESSION,
            selectedTile = uiState.selectedTile,
            onTileClick = viewModel::selectTile,
            onTileLongPress = viewModel::selectTile,
            partyLocation = uiState.partyLocation,
            onMoveParty = viewModel::moveParty,
            modifier = Modifier.fillMaxSize().padding(padding)
        )
    }
}

@Composable
private fun SessionTileSheet(
    tile: HexTile,
    pois: List<HexPoi>,
    partyLocation: Pair<Int, Int>?,
    activitiesUsed: Int,
    maxActivities: Int,
    todayLog: List<HexActivityEntry>,
    onExplore: () -> Unit,
    onReconnoiter: () -> Unit,
    onMap: () -> Unit,
    onRollTable: (Long) -> Unit,
    onStartEncounter: (Long?) -> Unit
) {
    val atLimit = activitiesUsed >= maxActivities
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Hex (${tile.q}, ${tile.r})" + if (tile.terrainLabel.isNotBlank()) " — ${tile.terrainLabel}" else "",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )

            if (partyLocation != null) {
                val dist = hexDistance(partyLocation.first, partyLocation.second, tile.q, tile.r)
                Surface(
                    shape = MaterialTheme.shapes.extraSmall,
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        "Distancia: $dist",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        // Estado del hex
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusChip("Explorado", tile.isExplored)
            StatusChip("Reconocido", tile.isReconnoitered)
            StatusChip("Mapeado", tile.isMapped)
        }

        HorizontalDivider()

        // Acciones de exploración
        Text("Exploración", style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilledTonalButton(
                onClick = onExplore,
                enabled = !tile.isExplored && !atLimit,
                modifier = Modifier.weight(1f)
            ) { Text("Explorar\n(1 act.)", maxLines = 2) }

            FilledTonalButton(
                onClick = onReconnoiter,
                enabled = !tile.isReconnoitered && !atLimit,
                modifier = Modifier.weight(1f)
            ) { Text("Reconocer\n(${tile.terrainCost.coerceAtLeast(1)} act.)", maxLines = 2) }

            FilledTonalButton(
                onClick = onMap,
                enabled = tile.isReconnoitered && !tile.isMapped && !atLimit,
                modifier = Modifier.weight(1f)
            ) { Text("Mapear\n(1 act.)", maxLines = 2) }
        }

        // Notas (visibles solo si explorado)
        if (tile.isExplored && tile.playerNotes.isNotBlank()) {
            HorizontalDivider()
            Text("Notas", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(tile.playerNotes, style = MaterialTheme.typography.bodyMedium)
        }

        // POIs
        if (pois.isNotEmpty() && tile.isExplored) {
            HorizontalDivider()
            Text("Puntos de interés", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            pois.forEach { poi ->
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(poi.name, style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold)
                        if (poi.description.isNotBlank()) {
                            Text(poi.description, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        val tableId = poi.tableId
                        val encounterId = poi.encounterId
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (tableId != null) {
                                Button(
                                    onClick = { onRollTable(tableId) },
                                    modifier = Modifier.weight(1f)
                                ) { Text("Tirar tabla") }
                            }
                            if (encounterId != null || true) {
                                Button(
                                    onClick = { onStartEncounter(encounterId) },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error
                                    )
                                ) { Text("Encuentro") }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
        }

        // Historial del día
        if (todayLog.isNotEmpty()) {
            HorizontalDivider()
            var expanded by remember { mutableStateOf(false) }
            Row(
                modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Historial de hoy (${todayLog.size})",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    if (expanded) "▲" else "▼",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (expanded) {
                todayLog.reversed().forEach { entry ->
                    Text(
                        "· ${entry.description}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusChip(label: String, active: Boolean) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = if (active) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (active) Icon(Icons.Default.Check, contentDescription = null,
                modifier = Modifier.width(14.dp).height(14.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = if (active) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
