package com.deckapp.feature.hexploration

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deckapp.core.domain.repository.CardRepository
import com.deckapp.core.domain.repository.ReferenceRepository
import com.deckapp.core.domain.repository.SessionRepository
import com.deckapp.core.domain.repository.HexRepository
import com.deckapp.core.domain.repository.MythicRepository
import com.deckapp.core.domain.usecase.DiscardCardUseCase
import com.deckapp.core.domain.usecase.DrawCardUseCase
import com.deckapp.core.domain.usecase.GetAllTablesUseCase
import com.deckapp.core.domain.usecase.ResetDeckUseCase
import com.deckapp.core.domain.usecase.RollTableUseCase
import com.deckapp.core.domain.usecase.hex.*
import com.deckapp.core.domain.usecase.AddQuickNoteUseCase
import com.deckapp.core.domain.usecase.SearchResult
import com.deckapp.core.domain.usecase.GlobalSearchResultType
import com.deckapp.core.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject

data class HexMapSessionUiState(
    val mapId: Long = 0,
    val mapName: String = "",
    val tiles: List<HexTile> = emptyList(),
    val pois: List<HexPoi> = emptyList(),
    val currentDay: HexDay? = null,
    val activitiesUsedToday: Int = 0,
    val maxActivitiesPerDay: Int = 8,
    val todayLog: List<HexActivityEntry> = emptyList(),
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
    val showResourcesPanel: Boolean = false,
    val allDays: List<HexDay> = emptyList(),
    val showJournalPanel: Boolean = false,
    val exportText: String = "",
    val showExportDialog: Boolean = false,
    val linkedMythicSession: MythicSession? = null,
    val allMythicSessions: List<MythicSession> = emptyList(),
    // Edit mode support
    val isEditMode: Boolean = false,
    val activeBrush: TerrainBrush = defaultBrushes.first(),
    val brushes: List<TerrainBrush> = defaultBrushes,
    val resourceSearchQuery: String = ""
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
    private val hexRepository: HexRepository,
    private val mythicRepository: MythicRepository,
    private val movePartyUseCase: MovePartyUseCase,
    private val getHexDaysUseCase: GetHexDaysUseCase,
    private val logHexActivityUseCase: LogHexActivityUseCase,
    private val getAllTablesUseCase: GetAllTablesUseCase,
    private val referenceRepository: ReferenceRepository,
    private val drawCardUseCase: DrawCardUseCase,
    private val discardCardUseCase: DiscardCardUseCase,
    private val resetDeckUseCase: ResetDeckUseCase,
    private val cardRepository: CardRepository,
    private val exportSessionSummaryUseCase: ExportSessionSummaryUseCase,
    private val addQuickNoteUseCase: AddQuickNoteUseCase,
    private val updateHexTileUseCase: UpdateHexTileUseCase,
    private val addHexTileUseCase: AddHexTileUseCase,
    private val deleteHexTileUseCase: DeleteHexTileUseCase,
    private val updateHexDayNotesUseCase: UpdateHexDayNotesUseCase
) : ViewModel() {

    private val mapId: Long = savedStateHandle["mapId"] ?: 0L

    private val _uiState = MutableStateFlow(HexMapSessionUiState(mapId = mapId))
    val uiState: StateFlow<HexMapSessionUiState> = _uiState.asStateFlow()

    init {
        getHexMapWithTilesUseCase(mapId)
            .filterNotNull()
            .onEach { mapData ->
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
                        weatherTableId = mapData.map.weatherTableId,
                        travelEventTableId = mapData.map.travelEventTableId,
                        terrainTableConfig = mapData.map.terrainTableConfig,
                        sessionResources = resources,
                        isLoading = false
                    )
                }
                updatePinnedResources()
                
                // Cargar Mythic vinculado
                if (mapData.map.linkedMythicSessionId != null) {
                    loadLinkedMythic(mapData.map.linkedMythicSessionId!!)
                } else {
                    _uiState.update { it.copy(linkedMythicSession = null) }
                }
            }.launchIn(viewModelScope)

        sessionRepository.getActiveSession()
            .onEach { activeSession ->
                _uiState.update { it.copy(activeSessionId = activeSession?.id) }
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

        mythicRepository.getSessions()
            .onEach { sessions ->
                _uiState.update { it.copy(allMythicSessions = sessions) }
            }.launchIn(viewModelScope)

        cardRepository.getDrawnCards()
            .onEach { drawnCards ->
                val sessionId = _uiState.value.activeSessionId
                val pinnedDeckIds = _uiState.value.pinnedDecks.map { it.id }.toSet()
                
                val sessionDrawnCards = if (sessionId != null) {
                    val sessionEvents = sessionRepository.getEventsForSession(sessionId).first()
                    val sessionDrawnIds = sessionEvents
                        .filter { it.action == DrawAction.DRAW }
                        .map { it.cardId }
                        .toSet()
                    drawnCards.filter { it.id in sessionDrawnIds }
                } else {
                    drawnCards
                }

                val byDeck = sessionDrawnCards
                    .filter { it.stackId in pinnedDeckIds }
                    .groupBy { it.stackId }
                _uiState.update { it.copy(drawnCardsByDeck = byDeck) }
            }
            .launchIn(viewModelScope)
    }

    private fun loadLinkedMythic(id: Long) {
        viewModelScope.launch {
            mythicRepository.getSessionById(id).collect { mythic ->
                _uiState.update { it.copy(linkedMythicSession = mythic) }
            }
        }
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
                    todayLog = latest?.activitiesLog ?: emptyList(),
                    allDays = days.sortedByDescending { it.dayNumber }
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
                HexActivityEntry(
                    type = HexActivityType.TRAVEL,
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

    fun showJournalPanel() = _uiState.update { it.copy(showJournalPanel = true) }
    fun dismissJournalPanel() = _uiState.update { it.copy(showJournalPanel = false) }
    fun dismissExportDialog() = _uiState.update { it.copy(showExportDialog = false, exportText = "") }

    fun updateDayNotes(day: HexDay, notes: String) {
        viewModelScope.launch { updateHexDayNotesUseCase(day, notes) }
    }

    fun exportSummary() {
        val text = exportSessionSummaryUseCase(_uiState.value.mapName, _uiState.value.allDays)
        _uiState.update { it.copy(exportText = text, showExportDialog = true) }
    }
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
        val sessionId = _uiState.value.activeSessionId ?: return
        viewModelScope.launch {
            resetDeckUseCase(sessionId, deckId)
        }
    }

    fun linkMythicSession(mythicId: Long?) {
        viewModelScope.launch {
            hexRepository.updateLinkedMythicSession(mapId, mythicId)
        }
    }

    // --- Edit Mode Functions ---

    fun toggleEditMode() {
        _uiState.update { it.copy(isEditMode = !it.isEditMode) }
    }

    fun selectBrush(brush: TerrainBrush) {
        _uiState.update { it.copy(activeBrush = brush) }
    }

    fun onTileClickInEditMode(tile: HexTile) {
        val brush = _uiState.value.activeBrush
        if (brush.cost == -1) {
            viewModelScope.launch { deleteHexTileUseCase(mapId, tile.q, tile.r) }
        } else {
            val painted = tile.copy(
                terrainCost = brush.cost,
                terrainLabel = brush.label,
                terrainColor = brush.color
            )
            viewModelScope.launch { updateHexTileUseCase(painted) }
        }
    }

    fun onEmptySpaceClickInEditMode(q: Int, r: Int) {
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
        }
    }

    fun onResourceSearchQueryChanged(q: String) {
        _uiState.update { it.copy(resourceSearchQuery = q) }
    }

    // --- End Edit Mode ---

    private suspend fun autoRoll(tableId: Long, context: String) {
        if (_uiState.value.showRollResultDialog) return
        val result = rollTableUseCase(tableId, _uiState.value.activeSessionId)
        
        // Registrar en el historial de la sesión automáticamente
        viewModelScope.launch {
            val noteContent = "[$context] Resultado: ${result.resolvedText}"
            addQuickNoteUseCase(
                content = noteContent,
                sessionId = _uiState.value.activeSessionId,
                mythicSessionId = _uiState.value.linkedMythicSession?.id
            )
        }

        _uiState.update { it.copy(
            lastRollResult = result,
            rollResultContext = context,
            showRollResultDialog = true,
            recentRolls = (listOf(result) + it.recentRolls).take(3)
        ) }
    }

    private suspend fun autoRollTerrainIfConfigured(tile: HexTile) {
        if (tile.terrainLabel.isBlank()) return
        val terrainConfig = parseTerrainConfig(_uiState.value.terrainTableConfig)
        val tableId = terrainConfig[tile.terrainLabel] ?: return
        autoRoll(tableId, "Encuentro: ${tile.terrainLabel}")
    }

    fun linkSearchResultToTile(result: SearchResult, q: Int, r: Int) {
        viewModelScope.launch {
            when (result.type) {
                GlobalSearchResultType.NPC -> {
                    // Añadir como un POI tipo Landmark/Enemigo
                    val poi = HexPoi(
                        mapId = mapId,
                        tileQ = q,
                        tileR = r,
                        name = result.title,
                        type = PoiType.LANDMARK,
                        description = "Vinculado desde buscador: ${result.subtitle}"
                    )
                    hexRepository.upsertHexPoi(poi)
                }
                GlobalSearchResultType.TABLE -> {
                    autoRoll(result.id, "Tabla en hex ($q,$r)")
                }
                GlobalSearchResultType.MANUAL, GlobalSearchResultType.RULE, GlobalSearchResultType.WIKI -> {
                    // Crear un POI Landmark que sirva de acceso directo
                    val poi = HexPoi(
                        mapId = mapId,
                        tileQ = q,
                        tileR = r,
                        name = result.title,
                        type = PoiType.LANDMARK,
                        description = "Referencia: ${result.subtitle}. Haz clic para abrir.",
                        tableId = if (result.type == GlobalSearchResultType.TABLE) result.id else null
                        // Nota: En el futuro, podríamos guardar el 'referenciaId' en el POI 
                        // para abrir el visor directamente.
                    )
                    hexRepository.upsertHexPoi(poi)
                }
            }
        }
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

    private fun parseSessionResources(json: String): HexSessionResources {
        return try {
            Json { ignoreUnknownKeys = true }.decodeFromString<HexSessionResources>(json)
        } catch (e: Exception) { HexSessionResources() }
    }

    private fun parseTerrainConfig(json: String): Map<String, Long> {
        return try {
            Json { ignoreUnknownKeys = true }.decodeFromString<Map<String, Long>>(json)
        } catch (e: Exception) { emptyMap() }
    }
}
