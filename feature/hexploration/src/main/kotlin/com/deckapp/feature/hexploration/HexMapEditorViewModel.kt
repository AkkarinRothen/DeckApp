package com.deckapp.feature.hexploration

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deckapp.core.domain.repository.CardRepository
import com.deckapp.core.domain.repository.ReferenceRepository
import com.deckapp.core.domain.usecase.GetAllTablesUseCase
import com.deckapp.core.domain.usecase.hex.AddHexPoiUseCase
import com.deckapp.core.domain.usecase.hex.AddHexTileUseCase
import com.deckapp.core.domain.usecase.hex.DeleteHexPoiUseCase
import com.deckapp.core.domain.usecase.hex.DeleteHexTileUseCase
import com.deckapp.core.domain.usecase.hex.ExpandHexMapUseCase
import com.deckapp.core.domain.usecase.hex.GetHexMapWithTilesUseCase
import com.deckapp.core.domain.usecase.hex.UpdateHexMapUseCase
import com.deckapp.core.domain.usecase.hex.UpdateHexTileUseCase
import com.deckapp.core.model.CardStack
import com.deckapp.core.model.HexMap
import com.deckapp.core.model.HexPoi
import com.deckapp.core.model.HexSessionResources
import com.deckapp.core.model.HexTile
import com.deckapp.core.model.PoiType
import com.deckapp.core.model.RandomTable
import com.deckapp.core.model.SystemRule
import com.deckapp.core.model.TerrainBrush
import com.deckapp.core.model.defaultBrushes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject


sealed class HexAction {
    data class TileChanged(val oldTile: HexTile?, val newTile: HexTile?) : HexAction()
    data class PoiAdded(val poi: HexPoi) : HexAction()
    data class PoiDeleted(val poi: HexPoi) : HexAction()
}

data class HexMapEditorUiState(
    val mapId: Long = 0,
    val mapName: String = "",
    val mapNotes: String = "",
    val maxActivitiesPerDay: Int = 8,
    val tiles: List<HexTile> = emptyList(),
    val pois: List<HexPoi> = emptyList(),
    val selectedTile: HexTile? = null,
    val activeBrush: TerrainBrush = defaultBrushes.first(),
    val brushes: List<TerrainBrush> = defaultBrushes,
    val showTileSheet: Boolean = false,
    val showAddPoiDialog: Boolean = false,
    val showAddBrushDialog: Boolean = false,
    val newPoiName: String = "",
    val newPoiType: PoiType = PoiType.LANDMARK,
    val newPoiDescription: String = "",
    val showCoordinates: Boolean = false,
    val currentMap: HexMap? = null,
    val isLoading: Boolean = true,
    val allTables: List<RandomTable> = emptyList(),
    val allDecks: List<CardStack> = emptyList(),
    val allRules: List<SystemRule> = emptyList(),
    val sessionResources: HexSessionResources = HexSessionResources(),
    val weatherTableId: Long? = null,
    val travelEventTableId: Long? = null,
    val terrainTableConfig: String = "{}",
    val showWeatherTablePicker: Boolean = false,
    val showTravelTablePicker: Boolean = false,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false
)

@HiltViewModel
class HexMapEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getHexMapWithTilesUseCase: GetHexMapWithTilesUseCase,
    private val updateHexTileUseCase: UpdateHexTileUseCase,
    private val updateHexMapUseCase: UpdateHexMapUseCase,
    private val addHexPoiUseCase: AddHexPoiUseCase,
    private val deleteHexPoiUseCase: DeleteHexPoiUseCase,
    private val expandHexMapUseCase: ExpandHexMapUseCase,
    private val addHexTileUseCase: AddHexTileUseCase,
    private val deleteHexTileUseCase: DeleteHexTileUseCase,
    private val getAllTablesUseCase: GetAllTablesUseCase,
    private val cardRepository: CardRepository,
    private val referenceRepository: ReferenceRepository
) : ViewModel() {

    private val mapId: Long = savedStateHandle["mapId"] ?: 0L

    private val _uiState = MutableStateFlow(HexMapEditorUiState(mapId = mapId))
    val uiState: StateFlow<HexMapEditorUiState> = _uiState.asStateFlow()

    private val undoStack = mutableListOf<HexAction>()
    private val redoStack = mutableListOf<HexAction>()

    init {
        getHexMapWithTilesUseCase(mapId)
            .filterNotNull()
            .onEach { data ->
                _uiState.update {
                    it.copy(
                        mapName = data.map.name,
                        mapNotes = data.map.mapNotes,
                        maxActivitiesPerDay = data.map.maxActivitiesPerDay,
                        weatherTableId = data.map.weatherTableId,
                        travelEventTableId = data.map.travelEventTableId,
                        terrainTableConfig = data.map.terrainTableConfig,
                        sessionResources = parseSessionResources(data.map.sessionResources),
                        tiles = data.tiles,
                        pois = data.pois,
                        currentMap = data.map,
                        isLoading = false
                    )
                }
            }
            .launchIn(viewModelScope)

        getAllTablesUseCase()
            .onEach { tables -> _uiState.update { it.copy(allTables = tables) } }
            .launchIn(viewModelScope)

        cardRepository.getAllDecks()
            .onEach { decks -> _uiState.update { it.copy(allDecks = decks) } }
            .launchIn(viewModelScope)

        referenceRepository.getAllSystemRules()
            .onEach { rules -> _uiState.update { it.copy(allRules = rules) } }
            .launchIn(viewModelScope)
    }

    fun onTileClick(tile: HexTile) {
        val brush = _uiState.value.activeBrush
        if (brush.cost == -1) {
            viewModelScope.launch { 
                deleteHexTileUseCase(mapId, tile.q, tile.r)
                pushAction(HexAction.TileChanged(tile, null))
            }
        } else {
            val painted = tile.copy(
                terrainCost = brush.cost,
                terrainLabel = brush.label,
                terrainColor = brush.color
            )
            viewModelScope.launch { 
                updateHexTileUseCase(painted)
                pushAction(HexAction.TileChanged(tile, painted))
            }
        }
    }

    fun onEmptySpaceClick(q: Int, r: Int) {
        val brush = _uiState.value.activeBrush
        if (brush.cost == -1) return
        
        viewModelScope.launch {
            addHexTileUseCase(
                mapId = mapId,
                q = q,
                r = r,
                terrainCost = brush.cost,
                terrainLabel = brush.label,
                terrainColor = brush.color
            )
            val newTile = HexTile(mapId = mapId, q = q, r = r, terrainCost = brush.cost, terrainLabel = brush.label, terrainColor = brush.color)
            pushAction(HexAction.TileChanged(null, newTile))
        }
    }

    private fun pushAction(action: HexAction) {
        undoStack.add(action)
        redoStack.clear()
        _uiState.update { it.copy(canUndo = true, canRedo = false) }
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        val action = undoStack.removeAt(undoStack.size - 1)
        redoStack.add(action)
        
        viewModelScope.launch {
            when (action) {
                is HexAction.TileChanged -> {
                    if (action.oldTile == null && action.newTile != null) {
                        deleteHexTileUseCase(mapId, action.newTile.q, action.newTile.r)
                    } else if (action.oldTile != null && action.newTile == null) {
                        addHexTileUseCase(mapId, action.oldTile.q, action.oldTile.r, action.oldTile.terrainCost, action.oldTile.terrainLabel, action.oldTile.terrainColor)
                    } else if (action.oldTile != null && action.newTile != null) {
                        updateHexTileUseCase(action.oldTile)
                    }
                }
                is HexAction.PoiAdded -> deleteHexPoiUseCase(action.poi.id)
                is HexAction.PoiDeleted -> addHexPoiUseCase(action.poi)
            }
            _uiState.update { it.copy(canUndo = undoStack.isNotEmpty(), canRedo = true) }
        }
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        val action = redoStack.removeAt(redoStack.size - 1)
        undoStack.add(action)

        viewModelScope.launch {
            when (action) {
                is HexAction.TileChanged -> {
                    if (action.oldTile == null && action.newTile != null) {
                        addHexTileUseCase(mapId, action.newTile.q, action.newTile.r, action.newTile.terrainCost, action.newTile.terrainLabel, action.newTile.terrainColor)
                    } else if (action.oldTile != null && action.newTile == null) {
                        deleteHexTileUseCase(mapId, action.oldTile.q, action.oldTile.r)
                    } else if (action.oldTile != null && action.newTile != null) {
                        updateHexTileUseCase(action.newTile)
                    }
                }
                is HexAction.PoiAdded -> addHexPoiUseCase(action.poi)
                is HexAction.PoiDeleted -> deleteHexPoiUseCase(action.poi.id)
            }
            _uiState.update { it.copy(canUndo = true, canRedo = redoStack.isNotEmpty()) }
        }
    }

    fun addRing() {
        viewModelScope.launch {
            expandHexMapUseCase(mapId)
        }
    }

    fun onTileLongPress(tile: HexTile) {
        _uiState.update { it.copy(selectedTile = tile, showTileSheet = true) }
    }

    fun dismissTileSheet() {
        _uiState.update { it.copy(showTileSheet = false, selectedTile = null) }
    }

    fun selectBrush(brush: TerrainBrush) {
        _uiState.update { it.copy(activeBrush = brush) }
    }

    fun updateTileNotes(tile: HexTile, dmNotes: String, playerNotes: String) {
        viewModelScope.launch {
            updateHexTileUseCase(tile.copy(dmNotes = dmNotes, playerNotes = playerNotes))
        }
        _uiState.update { it.copy(selectedTile = tile.copy(dmNotes = dmNotes, playerNotes = playerNotes)) }
    }

    fun showAddPoiDialog() = _uiState.update { it.copy(showAddPoiDialog = true) }
    fun dismissAddPoiDialog() = _uiState.update {
        it.copy(showAddPoiDialog = false, newPoiName = "", newPoiDescription = "", newPoiType = PoiType.LANDMARK)
    }
    fun onPoiNameChange(v: String) = _uiState.update { it.copy(newPoiName = v) }
    fun onPoiTypeChange(v: PoiType) = _uiState.update { it.copy(newPoiType = v) }
    fun onPoiDescChange(v: String) = _uiState.update { it.copy(newPoiDescription = v) }

    fun savePoi() {
        val state = _uiState.value
        val tile = state.selectedTile ?: return
        if (state.newPoiName.isBlank()) return
        viewModelScope.launch {
            val poi = HexPoi(
                mapId = state.mapId,
                tileQ = tile.q,
                tileR = tile.r,
                name = state.newPoiName.trim(),
                type = state.newPoiType,
                description = state.newPoiDescription.trim()
            )
            val newId = addHexPoiUseCase(poi)
            pushAction(HexAction.PoiAdded(poi.copy(id = newId)))
        }
        dismissAddPoiDialog()
    }

    fun deletePoi(poiId: Long) {
        val poi = _uiState.value.pois.find { it.id == poiId } ?: return
        viewModelScope.launch { 
            deleteHexPoiUseCase(poiId)
            pushAction(HexAction.PoiDeleted(poi))
        }
    }


    fun updateMapNotes(notes: String) {
        val map = _uiState.value.currentMap ?: return
        viewModelScope.launch { updateHexMapUseCase(map.copy(mapNotes = notes)) }
        _uiState.update { it.copy(mapNotes = notes) }
    }

    fun updateMaxActivities(max: Int) {
        val map = _uiState.value.currentMap ?: return
        val clamped = max.coerceIn(1, 30)
        viewModelScope.launch { updateHexMapUseCase(map.copy(maxActivitiesPerDay = clamped)) }
        _uiState.update { it.copy(maxActivitiesPerDay = clamped) }
    }

    fun toggleCoordinates() = _uiState.update { it.copy(showCoordinates = !it.showCoordinates) }

    fun showAddBrushDialog() = _uiState.update { it.copy(showAddBrushDialog = true) }
    fun dismissAddBrushDialog() = _uiState.update { it.copy(showAddBrushDialog = false) }

    fun addCustomBrush(label: String, cost: Int, color: Long) {
        if (label.isBlank()) return
        val brush = TerrainBrush(cost = cost.coerceIn(0, 3), label = label.trim(), color = color)
        _uiState.update { it.copy(brushes = it.brushes + brush, activeBrush = brush, showAddBrushDialog = false) }
    }

    fun toggleTableInResources(tableId: Long) {
        val current = _uiState.value.sessionResources
        val updated = if (tableId in current.tableIds)
            current.copy(tableIds = current.tableIds - tableId)
        else current.copy(tableIds = current.tableIds + tableId)
        saveSessionResources(updated)
    }

    fun toggleDeckInResources(deckId: Long) {
        val current = _uiState.value.sessionResources
        val updated = if (deckId in current.deckIds)
            current.copy(deckIds = current.deckIds - deckId)
        else current.copy(deckIds = current.deckIds + deckId)
        saveSessionResources(updated)
    }

    fun toggleRuleInResources(ruleId: Long) {
        val current = _uiState.value.sessionResources
        val updated = if (ruleId in current.ruleIds)
            current.copy(ruleIds = current.ruleIds - ruleId)
        else current.copy(ruleIds = current.ruleIds + ruleId)
        saveSessionResources(updated)
    }

    private fun saveSessionResources(resources: HexSessionResources) {
        val map = _uiState.value.currentMap ?: return
        val encoded = encodeSessionResources(resources)
        viewModelScope.launch { updateHexMapUseCase(map.copy(sessionResources = encoded)) }
        _uiState.update { it.copy(sessionResources = resources) }
    }

    fun showWeatherTablePicker() = _uiState.update { it.copy(showWeatherTablePicker = true) }
    fun dismissWeatherTablePicker() = _uiState.update { it.copy(showWeatherTablePicker = false) }
    fun setWeatherTable(tableId: Long?) {
        val map = _uiState.value.currentMap ?: return
        viewModelScope.launch { updateHexMapUseCase(map.copy(weatherTableId = tableId)) }
        _uiState.update { it.copy(weatherTableId = tableId, showWeatherTablePicker = false) }
    }

    fun showTravelTablePicker() = _uiState.update { it.copy(showTravelTablePicker = true) }
    fun dismissTravelTablePicker() = _uiState.update { it.copy(showTravelTablePicker = false) }
    fun setTravelTable(tableId: Long?) {
        val map = _uiState.value.currentMap ?: return
        viewModelScope.launch { updateHexMapUseCase(map.copy(travelEventTableId = tableId)) }
        _uiState.update { it.copy(travelEventTableId = tableId, showTravelTablePicker = false) }
    }

    fun setTerrainTable(terrainLabel: String, tableId: Long?) {
        val map = _uiState.value.currentMap ?: return
        val current = parseTerrainConfig(_uiState.value.terrainTableConfig).toMutableMap()
        if (tableId == null) current.remove(terrainLabel) else current[terrainLabel] = tableId
        val encoded = encodeTerrainConfig(current)
        viewModelScope.launch { updateHexMapUseCase(map.copy(terrainTableConfig = encoded)) }
        _uiState.update { it.copy(terrainTableConfig = encoded) }
    }
}

internal fun parseTerrainConfig(json: String): Map<String, Long> =
    runCatching {
        kotlinx.serialization.json.Json.decodeFromString<Map<String, Long>>(json)
    }.getOrDefault(emptyMap())

private val hexResJson = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }

internal fun parseSessionResources(json: String): HexSessionResources =
    runCatching { hexResJson.decodeFromString(HexSessionResources.serializer(), json) }
        .getOrDefault(HexSessionResources())

internal fun encodeSessionResources(res: HexSessionResources): String =
    runCatching { hexResJson.encodeToString(HexSessionResources.serializer(), res) }
        .getOrDefault("{}")

internal fun encodeTerrainConfig(map: Map<String, Long>): String = buildString {
    append("{")
    map.entries.forEachIndexed { index, (k, v) ->
        if (index > 0) append(",")
        append("\"${k.replace("\"", "\\\"")}\":$v")
    }
    append("}")
}
