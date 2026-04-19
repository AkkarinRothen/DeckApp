package com.deckapp.feature.hexploration

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deckapp.core.domain.repository.SessionRepository
import com.deckapp.core.domain.usecase.RollTableUseCase
import com.deckapp.core.domain.usecase.hex.ExploreHexUseCase
import com.deckapp.core.domain.usecase.hex.GetHexMapWithTilesUseCase
import com.deckapp.core.domain.usecase.hex.LinkHexMapToSessionUseCase
import com.deckapp.core.domain.usecase.hex.MapHexUseCase
import com.deckapp.core.domain.usecase.hex.ReconnoiterHexUseCase
import com.deckapp.core.domain.usecase.hex.StartNewHexDayUseCase
import com.deckapp.core.model.HexDay
import com.deckapp.core.model.HexPoi
import com.deckapp.core.model.HexTile
import com.deckapp.core.model.TableRollResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HexMapSessionUiState(
    val mapId: Long = 0,
    val mapName: String = "",
    val tiles: List<HexTile> = emptyList(),
    val pois: List<HexPoi> = emptyList(),
    val currentDay: HexDay? = null,
    val activitiesUsedToday: Int = 0,
    val partyLocation: Pair<Int, Int>? = null,
    val selectedTile: HexTile? = null,
    val showTileSheet: Boolean = false,
    val activeSessionId: Long? = null,
    val lastRollResult: TableRollResult? = null,
    val showRollResultDialog: Boolean = false,
    val isLoading: Boolean = true
)

@HiltViewModel
class HexMapSessionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getHexMapWithTilesUseCase: GetHexMapWithTilesUseCase,
    private val exploreHexUseCase: ExploreHexUseCase,
    private val reconnoiterHexUseCase: ReconnoiterHexUseCase,
    private val mapHexUseCase: MapHexUseCase,
    private val startNewHexDayUseCase: StartNewHexDayUseCase,
    private val linkHexMapToSessionUseCase: LinkHexMapToSessionUseCase,
    private val rollTableUseCase: RollTableUseCase,
    private val sessionRepository: SessionRepository
) : ViewModel() {

    private val mapId: Long = savedStateHandle["mapId"] ?: 0L

    private val _uiState = MutableStateFlow(HexMapSessionUiState(mapId = mapId))
    val uiState: StateFlow<HexMapSessionUiState> = _uiState.asStateFlow()

    init {
        combine(
            getHexMapWithTilesUseCase(mapId).filterNotNull(),
            sessionRepository.getActiveSession()
        ) { mapData, activeSession ->
            val days = mapData.map.let { _ ->
                // days come through the hex repo — re-use the flow below
                emptyList<HexDay>()
            }
            _uiState.update {
                it.copy(
                    mapName = mapData.map.name,
                    tiles = mapData.tiles,
                    pois = mapData.pois,
                    partyLocation = if (mapData.map.partyQ != null && mapData.map.partyR != null)
                        mapData.map.partyQ!! to mapData.map.partyR!!
                    else null,
                    activeSessionId = activeSession?.id,
                    isLoading = false
                )
            }
            // Auto-link map to active session if not already linked
            if (activeSession != null && mapData.map.sessionId != activeSession.id) {
                linkHexMapToSessionUseCase(mapId, activeSession.id)
            }
        }.launchIn(viewModelScope)

        // Observe days separately to track current day and activities
        viewModelScope.launch {
            // Days flow is not directly accessible from the use case here,
            // so we use the repository through the domain layer via the hex map flow
        }
        observeDays()
    }

    private fun observeDays() {
        // We get days via the HexRepository indirectly — inject it as a flow from the use case
        // Since GetHexMapWithTilesUseCase only exposes map+tiles+pois, we need HexRepository.
        // The cleanest approach per architecture: add a GetHexDaysUseCase or use the repository.
        // For now, StartNewHexDayUseCase and activity logging handle state changes,
        // and we observe via a dedicated flow if needed. Days are reflected after each action.
    }

    fun selectTile(tile: HexTile) {
        _uiState.update { it.copy(selectedTile = tile, showTileSheet = true) }
    }

    fun dismissTileSheet() {
        _uiState.update { it.copy(showTileSheet = false, selectedTile = null) }
    }

    fun explore(tile: HexTile) {
        if (tile.isExplored) return
        viewModelScope.launch {
            val dayId = ensureDayExists()
            exploreHexUseCase(tile, dayId)
            _uiState.update { it.copy(activitiesUsedToday = it.activitiesUsedToday + 1) }
        }
        dismissTileSheet()
    }

    fun reconnoiter(tile: HexTile) {
        if (tile.isReconnoitered) return
        viewModelScope.launch {
            val dayId = ensureDayExists()
            reconnoiterHexUseCase(tile, dayId)
            _uiState.update { it.copy(activitiesUsedToday = it.activitiesUsedToday + tile.terrainCost.coerceAtLeast(1)) }
        }
        dismissTileSheet()
    }

    fun mapTile(tile: HexTile) {
        if (tile.isMapped || !tile.isReconnoitered) return
        viewModelScope.launch {
            val dayId = ensureDayExists()
            mapHexUseCase(tile, dayId)
            _uiState.update { it.copy(activitiesUsedToday = it.activitiesUsedToday + 1) }
        }
        dismissTileSheet()
    }

    fun startNewDay() {
        viewModelScope.launch {
            val newDayId = startNewHexDayUseCase(mapId)
            _uiState.update { it.copy(
                currentDay = HexDay(id = newDayId, mapId = mapId, dayNumber = (it.currentDay?.dayNumber ?: 0) + 1),
                activitiesUsedToday = 0
            ) }
        }
    }

    fun rollPoiTable(tableId: Long) {
        viewModelScope.launch {
            val result = rollTableUseCase(tableId, _uiState.value.activeSessionId)
            _uiState.update { it.copy(lastRollResult = result, showRollResultDialog = true) }
        }
        dismissTileSheet()
    }

    fun moveParty(q: Int, r: Int) {
        val targetTile = _uiState.value.tiles.find { it.q == q && it.r == r }
        if (targetTile != null) {
            viewModelScope.launch {
                // For now, update local state. 
                // In a full implementation, we would persist this via a UseCase.
                _uiState.update { it.copy(partyLocation = q to r) }
            }
        }
    }

    fun dismissRollResult() {
        _uiState.update { it.copy(showRollResultDialog = false, lastRollResult = null) }
    }

    private suspend fun ensureDayExists(): Long {
        val day = _uiState.value.currentDay
        if (day != null) return day.id
        val id = startNewHexDayUseCase(mapId)
        _uiState.update { it.copy(
            currentDay = HexDay(id = id, mapId = mapId, dayNumber = 1),
            activitiesUsedToday = 0
        ) }
        return id
    }
}
