package com.deckapp.feature.hexploration

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deckapp.core.domain.usecase.hex.AddHexPoiUseCase
import com.deckapp.core.domain.usecase.hex.AddHexTileUseCase
import com.deckapp.core.domain.usecase.hex.DeleteHexPoiUseCase
import com.deckapp.core.domain.usecase.hex.ExpandHexMapUseCase
import com.deckapp.core.domain.usecase.hex.GetHexMapWithTilesUseCase
import com.deckapp.core.domain.usecase.hex.UpdateHexTileUseCase
import com.deckapp.core.model.HexPoi
import com.deckapp.core.model.HexTile
import com.deckapp.core.model.PoiType
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

data class TerrainBrush(
    val cost: Int,         // 1, 2, 3, or 0 (impassable)
    val label: String,
    val color: Long
)

val defaultBrushes = listOf(
    TerrainBrush(1, "Abierto", 0xFF7CB87BL),
    TerrainBrush(2, "Difícil", 0xFF8B7355L),
    TerrainBrush(3, "Muy difícil", 0xFF6B6B6BL),
    TerrainBrush(0, "Infranqueable", 0xFF2D2D2DL),
    TerrainBrush(1, "Agua", 0xFF4A90D9L),
    TerrainBrush(1, "Llanura", 0xFFD4C875L),
    TerrainBrush(2, "Bosque", 0xFF3A7D44L),
    TerrainBrush(3, "Montaña", 0xFF9E9E9EL)
)

data class HexMapEditorUiState(
    val mapId: Long = 0,
    val mapName: String = "",
    val tiles: List<HexTile> = emptyList(),
    val pois: List<HexPoi> = emptyList(),
    val selectedTile: HexTile? = null,
    val activeBrush: TerrainBrush = defaultBrushes.first(),
    val brushes: List<TerrainBrush> = defaultBrushes,
    val showTileSheet: Boolean = false,
    val showAddPoiDialog: Boolean = false,
    val newPoiName: String = "",
    val newPoiType: PoiType = PoiType.LANDMARK,
    val newPoiDescription: String = "",
    val isLoading: Boolean = true
)

@HiltViewModel
class HexMapEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getHexMapWithTilesUseCase: GetHexMapWithTilesUseCase,
    private val updateHexTileUseCase: UpdateHexTileUseCase,
    private val addHexPoiUseCase: AddHexPoiUseCase,
    private val deleteHexPoiUseCase: DeleteHexPoiUseCase,
    private val expandHexMapUseCase: ExpandHexMapUseCase,
    private val addHexTileUseCase: AddHexTileUseCase
) : ViewModel() {

    private val mapId: Long = savedStateHandle["mapId"] ?: 0L

    private val _uiState = MutableStateFlow(HexMapEditorUiState(mapId = mapId))
    val uiState: StateFlow<HexMapEditorUiState> = _uiState.asStateFlow()

    init {
        getHexMapWithTilesUseCase(mapId)
            .filterNotNull()
            .onEach { data ->
                _uiState.update {
                    it.copy(
                        mapName = data.map.name,
                        tiles = data.tiles,
                        pois = data.pois,
                        isLoading = false
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun onTileClick(tile: HexTile) {
        // In design mode, tap applies the active brush
        val painted = tile.copy(
            terrainCost = _uiState.value.activeBrush.cost,
            terrainLabel = _uiState.value.activeBrush.label,
            terrainColor = _uiState.value.activeBrush.color
        )
        viewModelScope.launch { updateHexTileUseCase(painted) }
    }

    fun onEmptySpaceClick(q: Int, r: Int) {
        // Add a single hex manually using the active brush
        val brush = _uiState.value.activeBrush
        viewModelScope.launch {
            addHexTileUseCase(
                mapId = mapId,
                q = q,
                r = r,
                terrainCost = brush.cost,
                terrainLabel = brush.label,
                terrainColor = brush.color
            )
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
            addHexPoiUseCase(
                HexPoi(
                    mapId = state.mapId,
                    tileQ = tile.q,
                    tileR = tile.r,
                    name = state.newPoiName.trim(),
                    type = state.newPoiType,
                    description = state.newPoiDescription.trim()
                )
            )
        }
        dismissAddPoiDialog()
    }

    fun deletePoi(poiId: Long) {
        viewModelScope.launch { deleteHexPoiUseCase(poiId) }
    }
}
