package com.deckapp.feature.hexploration

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deckapp.core.domain.repository.CardRepository
import com.deckapp.core.domain.repository.ReferenceRepository
import com.deckapp.core.domain.repository.SessionRepository
import com.deckapp.core.domain.usecase.DiscardCardUseCase
import com.deckapp.core.domain.usecase.DrawCardUseCase
import com.deckapp.core.domain.usecase.GetAllTablesUseCase
import com.deckapp.core.domain.usecase.ResetDeckUseCase
import com.deckapp.core.domain.usecase.RollTableUseCase
import com.deckapp.core.domain.usecase.hex.ExploreHexUseCase
import com.deckapp.core.domain.usecase.hex.GetHexDaysUseCase
import com.deckapp.core.domain.usecase.hex.GetHexMapWithTilesUseCase
import com.deckapp.core.domain.usecase.hex.LinkHexMapToSessionUseCase
import com.deckapp.core.domain.usecase.hex.LogHexActivityUseCase
import com.deckapp.core.domain.usecase.hex.MapHexUseCase
import com.deckapp.core.domain.usecase.hex.MovePartyUseCase
import com.deckapp.core.domain.usecase.hex.ReconnoiterHexUseCase
import com.deckapp.core.domain.usecase.hex.StartNewHexDayUseCase
import com.deckapp.core.model.Card
import com.deckapp.core.model.CardStack
import com.deckapp.core.model.HexDay
import com.deckapp.core.model.HexPoi
import com.deckapp.core.model.HexSessionResources
import com.deckapp.core.model.HexTile
import com.deckapp.core.model.RandomTable
import com.deckapp.core.model.SystemRule
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
    val maxActivitiesPerDay: Int = 8,
    val todayLog: List<com.deckapp.core.model.HexActivityEntry> = emptyList(),
    val partyLocation: Pair<Int, Int>? = null,
    val selectedTile: HexTile? = null,
    val showTileSheet: Boolean = false,
    val activeSessionId: Long? = null,
    val lastRollResult: TableRollResult? = null,
    val rollResultContext: String = "",
    val showRollResultDialog: Boolean = false,
    val recentRolls: List<TableRollResult> = emptyList(),
    val showTablesSheet: Boolean = false,
    val showRulesSheet: Boolean = false,
    val allTables: List<RandomTable> = emptyList(),
    val allRules: List<SystemRule> = emptyList(),
    val rulesSearchQuery: String = "",
    val weatherTableId: Long? = null,
    val travelEventTableId: Long? = null,
    val terrainTableConfig: String = "{}",
    val isLoading: Boolean = true,
    val sessionResources: HexSessionResources = HexSessionResources(),
    val pinnedTables: List<RandomTable> = emptyList(),
    val pinnedDecks: List<CardStack> = emptyList(),
    val pinnedRules: List<SystemRule> = emptyList(),
    val allDecks: List<CardStack> = emptyList(),
    val drawnCardsByDeck: Map<Long, List<Card>> = emptyMap(),
    val lastDrawnCard: Card? = null,
    val showLastDrawnCard: Boolean = false,
    val showResourcesPanel: Boolean = false
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
    private val sessionRepository: SessionRepository,
    private val movePartyUseCase: MovePartyUseCase,
    private val getHexDaysUseCase: GetHexDaysUseCase,
    private val logHexActivityUseCase: LogHexActivityUseCase,
    private val getAllTablesUseCase: GetAllTablesUseCase,
    private val referenceRepository: ReferenceRepository,
    private val drawCardUseCase: DrawCardUseCase,
    private val discardCardUseCase: DiscardCardUseCase,
    private val resetDeckUseCase: ResetDeckUseCase,
    private val cardRepository: CardRepository
) : ViewModel() {

    private val mapId: Long = savedStateHandle["mapId"] ?: 0L

    private val _uiState = MutableStateFlow(HexMapSessionUiState(mapId = mapId))
    val uiState: StateFlow<HexMapSessionUiState> = _uiState.asStateFlow()

    init {
        combine(
            getHexMapWithTilesUseCase(mapId).filterNotNull(),
            sessionRepository.getActiveSession()
        ) { mapData, activeSession ->
            val resources = parseSessionResources(mapData.map.sessionResources)
            _uiState.update {
                it.copy(
                    mapName = mapData.map.name,
                    tiles = mapData.tiles,
                    pois = mapData.pois,
                    maxActivitiesPerDay = mapData.map.maxActivitiesPerDay,
                    partyLocation = if (mapData.map.partyQ != null && mapData.map.partyR != null)
                        mapData.map.partyQ!! to mapData.map.partyR!!
                    else null,
                    activeSessionId = activeSession?.id,
                    weatherTableId = mapData.map.weatherTableId,
                    travelEventTableId = mapData.map.travelEventTableId,
                    terrainTableConfig = mapData.map.terrainTableConfig,
                    sessionResources = resources,
                    isLoading = false
                )
            }
            updatePinnedResources()
            if (activeSession != null && mapData.map.sessionId != activeSession.id) {
                linkHexMapToSessionUseCase(mapId, activeSession.id)
            }
        }.launchIn(viewModelScope)

        observeDays()

        getAllTablesUseCase()
            .onEach { tables ->
                _uiState.update { it.copy(allTables = tables) }
                updatePinnedResources()
            }
            .launchIn(viewModelScope)

        referenceRepository.getAllSystemRules()
            .onEach { rules ->
                _uiState.update { it.copy(allRules = rules) }
                updatePinnedResources()
            }
            .launchIn(viewModelScope)

        cardRepository.getAllDecks()
            .onEach { decks ->
                _uiState.update { it.copy(allDecks = decks) }
                updatePinnedResources()
            }
            .launchIn(viewModelScope)

        cardRepository.getDrawnCards()
            .onEach { drawnCards ->
                val pinnedDeckIds = _uiState.value.pinnedDecks.map { it.id }.toSet()
                val byDeck = drawnCards
                    .filter { it.stackId in pinnedDeckIds }
                    .groupBy { it.stackId }
                _uiState.update { it.copy(drawnCardsByDeck = byDeck) }
            }
            .launchIn(viewModelScope)
    }

    private fun updatePinnedResources() {
        val state = _uiState.value
        val res = state.sessionResources
        _uiState.update {
            it.copy(
                pinnedTables = it.allTables.filter { t -> t.id in res.tableIds },
                pinnedDecks = it.allDecks.filter { d -> d.id in res.deckIds },
                pinnedRules = it.allRules.filter { r -> r.id in res.ruleIds }
            )
        }
    }

    private fun observeDays() {
        getHexDaysUseCase(mapId)
            .onEach { days ->
                val latest = days.maxByOrNull { it.dayNumber }
                _uiState.update { it.copy(
                    currentDay = latest,
                    activitiesUsedToday = latest?.activitiesLog?.size ?: 0,
                    todayLog = latest?.activitiesLog ?: emptyList()
                ) }
            }
            .launchIn(viewModelScope)
    }

    fun selectTile(tile: HexTile) {
        _uiState.update { it.copy(selectedTile = tile, showTileSheet = true) }
    }

    fun dismissTileSheet() {
        _uiState.update { it.copy(showTileSheet = false, selectedTile = null) }
    }

    fun explore(tile: HexTile) {
        if (tile.isExplored) return
        val state = _uiState.value
        if (state.activitiesUsedToday >= state.maxActivitiesPerDay) return
        viewModelScope.launch {
            val dayId = ensureDayExists()
            exploreHexUseCase(tile, dayId)
            autoRollTerrainIfConfigured(tile)
        }
        dismissTileSheet()
    }

    fun reconnoiter(tile: HexTile) {
        if (tile.isReconnoitered) return
        val state = _uiState.value
        if (state.activitiesUsedToday >= state.maxActivitiesPerDay) return
        viewModelScope.launch {
            val dayId = ensureDayExists()
            reconnoiterHexUseCase(tile, dayId)
            autoRollTerrainIfConfigured(tile)
        }
        dismissTileSheet()
    }

    fun mapTile(tile: HexTile) {
        if (tile.isMapped || !tile.isReconnoitered) return
        val state = _uiState.value
        if (state.activitiesUsedToday >= state.maxActivitiesPerDay) return
        viewModelScope.launch {
            val dayId = ensureDayExists()
            mapHexUseCase(tile, dayId)
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
            val weatherId = _uiState.value.weatherTableId
            if (weatherId != null) autoRoll(weatherId, "Clima del día")
        }
    }

    fun rollPoiTable(tableId: Long) {
        viewModelScope.launch {
            autoRoll(tableId, "Tabla POI")
        }
        dismissTileSheet()
    }

    fun moveParty(q: Int, r: Int) {
        val state = _uiState.value
        if (state.tiles.none { it.q == q && it.r == r }) return
        viewModelScope.launch {
            movePartyUseCase(mapId, q, r)
            _uiState.update { it.copy(partyLocation = q to r) }
            val dayId = ensureDayExists()
            logHexActivityUseCase(
                dayId,
                com.deckapp.core.model.HexActivityEntry(
                    type = com.deckapp.core.model.HexActivityType.TRAVEL,
                    description = "Viaje a ($q, $r)",
                    tileQ = q,
                    tileR = r
                )
            )
            val travelId = _uiState.value.travelEventTableId
            if (travelId != null) autoRoll(travelId, "Evento de viaje")
        }
    }

    fun dismissRollDialog() {
        _uiState.update { it.copy(showRollResultDialog = false, lastRollResult = null) }
    }

    fun showTablesSheet() = _uiState.update { it.copy(showTablesSheet = true) }
    fun dismissTablesSheet() = _uiState.update { it.copy(showTablesSheet = false) }
    fun showRulesSheet() = _uiState.update { it.copy(showRulesSheet = true) }
    fun dismissRulesSheet() = _uiState.update { it.copy(showRulesSheet = false) }
    fun onRulesSearchChanged(q: String) = _uiState.update { it.copy(rulesSearchQuery = q) }

    fun rollTableManually(tableId: Long) {
        viewModelScope.launch {
            val context = _uiState.value.allTables.find { it.id == tableId }?.name ?: ""
            autoRoll(tableId, context)
        }
    }

    fun showResourcesPanel() = _uiState.update { it.copy(showResourcesPanel = true) }
    fun dismissResourcesPanel() = _uiState.update { it.copy(showResourcesPanel = false) }
    fun dismissLastDrawnCard() = _uiState.update { it.copy(showLastDrawnCard = false, lastDrawnCard = null) }

    fun drawCardFromDeck(deckId: Long) {
        viewModelScope.launch {
            val sessionId = _uiState.value.activeSessionId ?: return@launch
            val deck = _uiState.value.pinnedDecks.find { it.id == deckId } ?: return@launch
            val card = drawCardUseCase(sessionId, deckId, deck.drawMode)
            if (card != null) {
                _uiState.update { it.copy(lastDrawnCard = card, showLastDrawnCard = true) }
            }
        }
    }

    fun discardCard(cardId: Long) {
        viewModelScope.launch {
            val sessionId = _uiState.value.activeSessionId ?: return@launch
            discardCardUseCase(sessionId, cardId)
            _uiState.update { it.copy(showLastDrawnCard = false, lastDrawnCard = null) }
        }
    }

    fun resetDeck(deckId: Long) {
        viewModelScope.launch {
            val sessionId = _uiState.value.activeSessionId ?: return@launch
            resetDeckUseCase(sessionId, deckId)
        }
    }

    private suspend fun autoRoll(tableId: Long, context: String) {
        if (_uiState.value.showRollResultDialog) return
        val result = rollTableUseCase(tableId, _uiState.value.activeSessionId)
        _uiState.update { it.copy(
            lastRollResult = result,
            rollResultContext = context,
            showRollResultDialog = true,
            recentRolls = (listOf(result) + it.recentRolls).take(3)
        ) }
    }

    private suspend fun autoRollTerrainIfConfigured(tile: HexTile) {
        if (tile.terrainLabel.isBlank()) return
        val tableId = parseTerrainConfig(_uiState.value.terrainTableConfig)[tile.terrainLabel] ?: return
        autoRoll(tableId, "Encuentro: ${tile.terrainLabel}")
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
