package com.deckapp.feature.draw

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deckapp.core.domain.repository.CardRepository
import com.deckapp.core.domain.repository.EncounterRepository
import com.deckapp.core.domain.repository.NpcRepository
import com.deckapp.core.domain.repository.SessionRepository
import com.deckapp.core.domain.usecase.*
import com.deckapp.core.domain.usecase.hex.*
import com.deckapp.core.model.*
import com.deckapp.feature.hexploration.HexMapSessionUiState
import com.deckapp.feature.mythic.MythicSessionUiState
import com.deckapp.feature.draw.scene.*
import com.deckapp.core.domain.usecase.mythic.SceneCheckResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

data class SessionUiState(
    val session: Session? = null,
    val deckRefs: List<SessionDeckRef> = emptyList(),
    val deckNames: Map<Long, String> = emptyMap(),       // stackId → nombre del mazo
    val deckCardCounts: Map<Long, Int> = emptyMap(),     // stackId → cartas disponibles
    val selectedDeckId: Long? = null,                    // mazo activo para ROBAR
    val hand: List<Card> = emptyList(),
    val pile: List<Card> = emptyList(),
    val isLoading: Boolean = true,
    val canUndo: Boolean = false,
    val snackbarMessage: String? = null,
    val nightMode: Boolean = false,
    val errorMessage: String? = null,
    val peekCard: Card? = null,    // carta visible en modo Peek (no robada)
    val sessionEnded: Boolean = false,
    val activeTab: Int = 0,                  // 0=Mazos, 1=Pilas, 2=Tablas, 3=Ref, 4=Notas, 5=Combate
    val hasActiveEncounter: Boolean = false, // habilita tab Combate (Sprint 7)
    val activeEncounter: Encounter? = null,  // datos del combate en curso
    val dmNotes: String = "",
    val showQuickNoteDialog: Boolean = false,
    val sessionElapsedMinutes: Long = 0,
    val deckAspectRatios: Map<Long, CardAspectRatio> = emptyMap(), // stackId → aspectRatio
    val deckBackImages: Map<Long, String?> = emptyMap(),           // stackId → backImagePath
    // Sprint 14.5 — Workspace Bento
    val collapsedDeckIds: Set<Long> = emptySet(),   // clusters colapsados manualmente por el DM
    val handByDeck: Map<Long, List<Card>> = emptyMap(), // hand agrupada por stackId (derivada)
    // Combat Log
    val combatLog: List<CombatLogEntry> = emptyList(),
    // Sprint 17 — Resource Management
    val allDecks: List<CardStack> = emptyList(),
    val allTables: List<RandomTable> = emptyList(),
    val tablesInSession: List<RandomTable> = emptyList(),
    val showResourceManager: Boolean = false,
    val showSessionConfig: Boolean = false,
    val availableSystems: List<String> = emptyList(),
    // C-5 — Animación de barajar
    val isShuffling: Boolean = false,
    // D-2 — Robar por palo
    val showDrawBySuitSheet: Boolean = false,
    val availableSuitsForDraw: List<String> = emptyList(),
    // Sprint 15 — UX de Notas
    val isSavingNotes: Boolean = false,
    // Sprint 17 — Participantes temporales (PJs)
    val playerParticipants: List<PlayerInitiativeParticipant> = emptyList(),
    val isSimplifiedMode: Boolean = false,
    val linkedMythicSession: MythicSession? = null,
    val allMythicSessions: List<MythicSession> = emptyList(),
    // Universal Hub - Sidebar State
    val activeLeftSidebar: SidebarTool = SidebarTool.NONE,
    val activeRightSidebar: SidebarTool = SidebarTool.NONE,
    val mainViewMode: SessionViewMode = SessionViewMode.MAP,
    val linkedHexMapId: Long? = null,
    val sessionId: Long = 0,
    // Module Aggregate States
    val hplState: HexMapSessionUiState? = null,
    val mythicState: MythicSessionUiState? = null,
    // Sprint 18 — NPCs & Voice
    val npcs: List<Npc> = emptyList(),
    val playingNpcVoicePath: String? = null,
    // Session Planner
    val scenes: List<Scene> = emptyList(),
    val focusedSceneId: Long? = null,
    val wikiEntries: List<WikiEntry> = emptyList(),
    val tacticalState: com.deckapp.feature.draw.tactical.TacticalUiState = com.deckapp.feature.draw.tactical.TacticalUiState(),
    // Bento Grid Workspace & Board
    val activeBoardView: SessionBoardView = SessionBoardView.MAIN,
    val viewLabels: SessionBoardViewLabels = SessionBoardViewLabels(),
    val sessionBoardItems: List<SessionBoardItem> = emptyList(),
    val fullBoardItems: List<SessionBoardItem> = emptyList(),
    val boardGroups: List<SessionBoardGroup> = emptyList(),
    val tensionClocks: List<TensionClock> = emptyList(),
    val allHexMaps: List<HexMap> = emptyList(),
    val recentDiceRolls: List<DiceRollResult> = emptyList(),
    val recentTableRolls: List<TableRollResult> = emptyList(),
    val npcCount: Int = 0,
    val permanentPlayers: List<Npc> = emptyList(),
    val pinnedTableRolls: Map<Long, TableRollResult> = emptyMap(),
    val spreadResultsByDeck: Map<Long, CardSpreadResult> = emptyMap(),
    val showQuickSearch: Boolean = false,
    // Sprint 56 — Bento Solo Journaling & Interactive Mythic Oracle
    val boardMythicChaos: Int = 5,
    val boardMythicProbability: ProbabilityLevel = ProbabilityLevel.FIFTY_FIFTY,
    val boardMythicLastResult: BoardMythicFateResult? = null,
    val boardMythicHistory: List<BoardMythicFateResult> = emptyList(),
    val boardMythicLastEvent: String? = null,
    val isSoloSceneBusy: Boolean = false,
    val boardMemory: BoardSessionMemory = BoardSessionMemory(),
    val isSendingInspiration: Boolean = false,
    val isHotbarExpanded: Boolean = true,
    val showHotbarActionDialog: Boolean = false,
    val editingHotbarAction: SessionHotbarAction? = null,
    val showSceneFramingDialog: Boolean = false
)

enum class SidebarTool {
    NONE, TABLES, DECKS, COMBAT, NOTES, JOURNAL, MYTHIC_CONTEXT, WIKI, NPCS, PLAN
}

enum class SessionViewMode {
    BOARD, MAP, TACTICAL, MYTHIC_LOG
}

enum class SessionBoardTemplatePreset {
    EXPLORATION, NARRATIVE, COMBAT
}



@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SessionViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val cardRepository: CardRepository,
    private val mythicRepository: com.deckapp.core.domain.repository.MythicRepository,
    private val tableRepository: com.deckapp.core.domain.repository.TableRepository,
    private val encounterRepository: EncounterRepository,
    private val npcRepository: NpcRepository,
    private val wikiRepository: com.deckapp.core.domain.repository.WikiRepository,
    private val referenceRepository: com.deckapp.core.domain.repository.ReferenceRepository,
    private val drawCardUseCase: DrawCardUseCase,
    private val discardCardUseCase: DiscardCardUseCase,
    private val resetDeckUseCase: ResetDeckUseCase,
    private val shuffleDeckUseCase: ShuffleDeckUseCase,
    private val updateCardStateUseCase: UpdateCardStateUseCase,
    private val undoLastActionUseCase: UndoLastActionUseCase,
    private val nextTurnUseCase: NextTurnUseCase,
    private val applyDamageUseCase: ApplyDamageUseCase,
    private val rollInitiativeUseCase: RollInitiativeUseCase,
    private val calculateInitiativeOrderUseCase: CalculateInitiativeOrderUseCase,
    private val toggleConditionUseCase: ToggleConditionUseCase,
    private val cleanupCombatUseCase: CleanupCombatUseCase,
    private val rollTableUseCase: RollTableUseCase,
    private val settingsRepository: com.deckapp.core.domain.repository.SettingsRepository,
    // HPL & Mythic Integration
    private val getHexMapWithTilesUseCase: GetHexMapWithTilesUseCase,
    private val getHexDaysUseCase: GetHexDaysUseCase,
    private val rollMythicFateUseCase: com.deckapp.core.domain.usecase.mythic.RollMythicFateUseCase,
    private val sceneCheckUseCase: com.deckapp.core.domain.usecase.mythic.SceneCheckUseCase,
    private val hexRepository: com.deckapp.core.domain.repository.HexRepository,
    private val tacticalMapRepository: com.deckapp.core.domain.repository.TacticalMapRepository,
    private val calculateTacticalDistanceUseCase: com.deckapp.core.domain.usecase.CalculateTacticalDistanceUseCase,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val voicePlayer by lazy { com.deckapp.feature.npcs.AudioPlayer(context) }

    private val sessionId: Long = checkNotNull(savedStateHandle["sessionId"])
    private val notesMutex = Mutex()
    private var peekCardId: Long?
        get() = savedStateHandle.get<Long>("peekCardId")
        set(value) { savedStateHandle["peekCardId"] = value }

    private val initialBoardMemory = settingsRepository.getBoardSessionMemory(sessionId)
    private val _uiState = MutableStateFlow(SessionUiState(
        sessionId = sessionId, boardMemory = initialBoardMemory,
        focusedSceneId = initialBoardMemory.focusedSceneId,
        pinnedTableRolls = initialBoardMemory.pinnedTableResults
    ))
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

    val tacticalMapDelegate = com.deckapp.feature.draw.delegates.TacticalMapDelegate(
        sessionId = sessionId,
        _uiState = _uiState,
        scope = viewModelScope,
        tacticalMapRepository = tacticalMapRepository,
        encounterRepository = encounterRepository,
        distanceCalculator = calculateTacticalDistanceUseCase,
        tableRepository = tableRepository,
        rollTableUseCase = rollTableUseCase,
        wikiRepository = wikiRepository,
        context = context
    )

    val boardDelegate = com.deckapp.feature.draw.delegates.BoardDelegate(
        sessionId = sessionId,
        _uiState = _uiState,
        scope = viewModelScope,
        sessionRepository = sessionRepository
    )

    // Un flujo que emite la hora actual cada minuto para cálculos reactivos
    private val minuteTicker = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(60000)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), System.currentTimeMillis())

    init {
        // Re-load peek card if exists in saved state
        peekCardId?.let { id ->
            viewModelScope.launch {
                cardRepository.getCardById(id).first()?.let { card ->
                    _uiState.update { it.copy(peekCard = card) }
                }
            }
        }

        // Combine session data and ticker
        viewModelScope.launch {
            combine(
                sessionRepository.getSessionById(sessionId),
                minuteTicker
            ) { session, now ->
                if (session != null) {
                    val elapsed = (now - session.createdAt) / (1000 * 60)
                    _uiState.update { it.copy(
                        session = session,
                        dmNotes = if (it.isSoloSceneBusy || it.isSavingNotes) it.dmNotes else session.dmNotes.orEmpty(),
                        sessionElapsedMinutes = elapsed
                    ) }
                }
            }.collect()
        }

        // Combine decks and their metadata
        viewModelScope.launch {
            combine(
                sessionRepository.getDecksForSession(sessionId),
                cardRepository.getAllDecks()
            ) { refs, allDecks ->
                val names = allDecks.associate { it.id to it.name }
                val ratios = allDecks.associate { it.id to it.aspectRatio }
                val backImages = allDecks.associate { it.id to it.backImagePath }
                
                _uiState.update { state ->
                    state.copy(
                        deckRefs = refs,
                        deckNames = names,
                        deckAspectRatios = ratios,
                        deckBackImages = backImages,
                        selectedDeckId = state.selectedDeckId ?: refs.firstOrNull()?.stackId
                    )
                }
            }.collect()
        }

        // Conteo reactivo de cartas disponibles por mazo
        viewModelScope.launch {
            sessionRepository.getDecksForSession(sessionId)
                .flatMapLatest { refs ->
                    if (refs.isEmpty()) flowOf(emptyMap())
                    else combine(
                        refs.map { ref ->
                            cardRepository.getAvailableCount(ref.stackId)
                                .map { count -> ref.stackId to count }
                        }
                    ) { pairs -> pairs.toMap() }
                }
                .collect { counts ->
                    _uiState.update { it.copy(deckCardCounts = counts, isLoading = false) }
                }
        }

        // Mano: cartas con isDrawn = true + agrupación por mazo para el Workspace
        viewModelScope.launch {
            cardRepository.getDrawnCards()
                .collect { drawn ->
                    _uiState.update {
                        it.copy(
                            hand = drawn,
                            handByDeck = drawn.groupBy { card -> card.stackId }
                        )
                    }
                }
        }

        // Pila de descarte
        viewModelScope.launch {
            cardRepository.getPiledCards(sessionId)
                .collect { pile -> _uiState.update { it.copy(pile = pile) } }
        }

        // canUndo reactivo
        viewModelScope.launch {
            sessionRepository.getEventsForSession(sessionId)
                .map { it.isNotEmpty() }
                .collect { canUndo -> _uiState.update { it.copy(canUndo = canUndo) } }
        }

        // --- Combat Tracker ---
        viewModelScope.launch {
            encounterRepository.getActiveEncounter(sessionId)
                .flatMapLatest { encounter ->
                    if (encounter == null) flowOf(null to emptyList<CombatLogEntry>())
                    else encounterRepository.getLogForEncounter(encounter.id)
                        .map { log -> encounter to log }
                }
                .collect { (encounter, log) ->
                    _uiState.update { it.copy(
                        activeEncounter = encounter,
                        hasActiveEncounter = encounter != null,
                        combatLog = log
                    ) }
                }
        }

        // Tablas asignadas a la sesión
        viewModelScope.launch {
            sessionRepository.getTablesForSession(sessionId)
                .collect { tables ->
                    _uiState.update { it.copy(tablesInSession = tables) }
                }
        }

        // Todos los recursos disponibles (para el diálogo de gestión)
        viewModelScope.launch {
            combine(
                cardRepository.getAllDecks(),
                tableRepository.getAllTables(),
                referenceRepository.getDistinctSystems()
            ) { decks, tables, systems ->
                Triple(decks, tables, systems)
            }.collect { (decks, tables, systems) ->
                _uiState.update { it.copy(
                    allDecks = decks,
                    allTables = tables,
                    availableSystems = systems
                ) }
            }
        }

        // Cargar estado de modo simplificado
        _uiState.update { it.copy(isSimplifiedMode = settingsRepository.getSimplifiedModeEnabled()) }

        // Vinculación Mythic
        viewModelScope.launch {
            sessionRepository.getSessionById(sessionId)
                .flatMapLatest { session ->
                    if (session?.linkedMythicSessionId != null) {
                        mythicRepository.getSessionById(session.linkedMythicSessionId!!)
                    } else flowOf(null)
                }
                .collect { mythicSession ->
                    _uiState.update { it.copy(linkedMythicSession = mythicSession) }
                }
        }

        viewModelScope.launch {
            mythicRepository.getSessions().collect { sessions ->
                _uiState.update { it.copy(allMythicSessions = sessions) }
            }
        }

        // Recuperar mapa vinculado si existe (buscando mapas que pertenezcan a esta sesión)
        viewModelScope.launch {
            hexRepository.getHexMaps()
                .flatMapLatest { maps ->
                    val linkedMap = maps.find { it.sessionId == sessionId }
                    if (linkedMap != null) {
                        getHexMapWithTilesUseCase(linkedMap.id)
                            .map { mapData -> mapData to linkedMap.id }
                    } else flowOf(null to null)
                }
                .collect { (mapData, mapId) ->
                    if (mapData != null && mapId != null) {
                        _uiState.update { it.copy(
                            linkedHexMapId = mapId,
                            hplState = HexMapSessionUiState(
                                mapId = mapId,
                                mapName = mapData.map.name,
                                tiles = mapData.tiles,
                                pois = mapData.pois,
                                isLoading = false
                            )
                        ) }
                    } else {
                        _uiState.update { it.copy(linkedHexMapId = null, hplState = null) }
                    }
                }
        }

        // Recuperar Mythic vinculado y sus datos (Personajes, Hilos, Rolls)
        viewModelScope.launch {
            sessionRepository.getSessionById(sessionId)
                .flatMapLatest { session ->
                    if (session?.linkedMythicSessionId != null) {
                        val mythicId = session.linkedMythicSessionId!!
                        combine(
                            mythicRepository.getCharacters(mythicId),
                            mythicRepository.getThreads(mythicId),
                            mythicRepository.getRolls(mythicId)
                        ) { chars, threads, rolls ->
                            Triple(chars, threads, rolls)
                        }.map { data -> data to mythicId }
                    } else flowOf(null to null)
                }
                .collect { (data, mythicId) ->
                    if (data != null && mythicId != null) {
                        val (chars, threads, rolls) = data
                        _uiState.update { state ->
                            state.copy(
                                mythicState = MythicSessionUiState(
                                    session = state.linkedMythicSession,
                                    characters = chars,
                                    threads = threads,
                                    rolls = rolls,
                                    isLoading = false
                                )
                            )
                        }
                    } else {
                        _uiState.update { it.copy(mythicState = null) }
                    }
                }
        }

        // Recuperar NPCs de la biblioteca
        // Recuperar todos los hex maps disponibles
        viewModelScope.launch {
            hexRepository.getHexMaps().collect { maps ->
                _uiState.update { it.copy(allHexMaps = maps) }
            }
        }

        // Recuperar NPCs de la biblioteca
        viewModelScope.launch {
            npcRepository.getAllNpcs().collect { npcs ->
                _uiState.update { it.copy(npcs = npcs, npcCount = npcs.size) }
            }
        }

        // Recuperar Escenas del planificador para seguimiento en vivo
        viewModelScope.launch {
            sessionRepository.getScenesForSession(sessionId).collect { scenes ->
                _uiState.update { it.copy(scenes = scenes) }
            }
        }

        // Recuperar entradas de la Wiki para detección de Smart Tags
        viewModelScope.launch {
            wikiRepository.getAllEntries().collect { entries ->
                _uiState.update { it.copy(wikiEntries = entries) }
            }
        }
    }

    fun linkMythicSession(mythicSessionId: Long?) {
        viewModelScope.launch {
            sessionRepository.updateLinkedMythicSession(sessionId, mythicSessionId)
        }
    }

    /** Selecciona el mazo activo para la siguiente acción ROBAR. */
    fun selectDeck(stackId: Long) {
        _uiState.update { it.copy(selectedDeckId = stackId) }
    }

    /** Actualiza el tab activo del workspace de sesión. */
    fun setActiveTab(tab: Int) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun toggleLeftSidebar(tool: SidebarTool) {
        _uiState.update { state ->
            val next = if (state.activeLeftSidebar == tool) SidebarTool.NONE else tool
            state.copy(activeLeftSidebar = next)
        }
    }

    fun toggleRightSidebar(tool: SidebarTool) {
        _uiState.update { state ->
            val next = if (state.activeRightSidebar == tool) SidebarTool.NONE else tool
            state.copy(activeRightSidebar = next)
        }
    }

    fun setMainViewMode(mode: SessionViewMode) {
        _uiState.update { it.copy(mainViewMode = mode) }
    }

    // --- Mythic Unified Actions ---
    
    fun performMythicFateCheck(question: String, prob: ProbabilityLevel) {
        val mythicId = _uiState.value.linkedMythicSession?.id ?: return
        viewModelScope.launch {
            rollMythicFateUseCase(mythicId, question, prob)
        }
    }

    fun quickMythicCheck() = performMythicFateCheck("Consulta rápida", ProbabilityLevel.FIFTY_FIFTY)

    fun updateMythicChaos(delta: Int) {
        val session = _uiState.value.linkedMythicSession ?: return
        val next = (session.chaosFactor + delta).coerceIn(1, 9)
        viewModelScope.launch {
            mythicRepository.updateChaosFactor(session.id, next)
        }
    }

    fun setBoardMythicProbability(prob: ProbabilityLevel) {
        _uiState.update { it.copy(boardMythicProbability = prob) }
    }

    fun updateBoardMythicChaos(delta: Int) {
        val currentChaos = _uiState.value.linkedMythicSession?.chaosFactor ?: _uiState.value.boardMythicChaos
        val next = (currentChaos + delta).coerceIn(1, 9)
        _uiState.update { it.copy(boardMythicChaos = next) }
        _uiState.value.linkedMythicSession?.let { session ->
            viewModelScope.launch {
                mythicRepository.updateChaosFactor(session.id, next)
            }
        }
    }

    fun rollBoardMythicFate(
        probability: ProbabilityLevel = _uiState.value.boardMythicProbability,
        forcedRoll: Int? = null,
        question: String = ""
    ) {
        val chaos = _uiState.value.linkedMythicSession?.chaosFactor ?: _uiState.value.boardMythicChaos
        val rollValue = forcedRoll ?: kotlin.random.Random.nextInt(1, 101)
        val fateCheckUseCase = com.deckapp.core.domain.usecase.mythic.FateCheckUseCase()
        val (result, isRandomEvent) = fateCheckUseCase(probability, chaos, rollValue)

        val fateChart = arrayOf(
            intArrayOf(95, 97, 99, 99, 99, 99, 99, 99, 99),
            intArrayOf(75, 83, 90, 95, 97, 99, 99, 99, 99),
            intArrayOf(55, 65, 75, 83, 90, 95, 97, 99, 99),
            intArrayOf(35, 45, 55, 65, 75, 83, 90, 95, 97),
            intArrayOf(25, 35, 45, 55, 65, 75, 83, 90, 95),
            intArrayOf(15, 25, 35, 45, 55, 65, 75, 83, 90),
            intArrayOf(10, 15, 25, 35, 45, 55, 65, 75, 83),
            intArrayOf(5, 10, 15, 25, 35, 45, 55, 65, 75),
            intArrayOf(3, 5, 10, 15, 25, 35, 45, 55, 65)
        )
        val targetThreshold = fateChart[probability.ordinal][chaos - 1]

        val randomEventDesc = if (isRandomEvent) {
            val action = com.deckapp.core.domain.usecase.mythic.MythicReferenceData.actionKeywords.random()
            val subject = com.deckapp.core.domain.usecase.mythic.MythicReferenceData.subjectKeywords.random()
            "$action · $subject"
        } else null

        val fateItem = BoardMythicFateResult(
            odds = probability,
            chaos = chaos,
            roll = rollValue,
            targetThreshold = targetThreshold,
            fateResult = result,
            isRandomEvent = isRandomEvent,
            randomEventDescription = randomEventDesc,
            question = question.trim(),
            sceneTitle = activeSoloScene()?.title.orEmpty(),
            sceneId = activeSoloScene()?.id
        )

        _uiState.update { state ->
            state.copy(
                boardMythicProbability = probability,
                boardMythicChaos = chaos,
                boardMythicLastResult = fateItem,
                boardMythicHistory = (listOf(fateItem) + state.boardMythicHistory).take(10)
            )
        }

        _uiState.value.linkedMythicSession?.let { session ->
            viewModelScope.launch {
                mythicRepository.saveRoll(
                    MythicRoll(
                        sessionId = session.id,
                        question = question.trim().ifBlank { "Consulta Bento" },
                        probability = probability,
                        chaosFactor = chaos,
                        roll = rollValue,
                        result = result,
                        isRandomEvent = isRandomEvent,
                        eventAction = randomEventDesc ?: "",
                        sceneNumber = session.sceneNumber
                    )
                )
            }
        }
    }

    fun rollBoardMythicRandomEvent(): String {
        val action = com.deckapp.core.domain.usecase.mythic.MythicReferenceData.actionKeywords.random()
        val subject = com.deckapp.core.domain.usecase.mythic.MythicReferenceData.subjectKeywords.random()
        val eventText = "$action · $subject"
        _uiState.update { it.copy(boardMythicLastEvent = eventText) }
        return eventText
    }

    fun appendToNotes(textToAppend: String) {
        val current = _uiState.value.dmNotes
        val updated = if (current.isBlank()) textToAppend else "$current\n\n$textToAppend"
        updateNotes(updated)
    }

    fun activeSoloScene(): Scene? = _uiState.value.let { state ->
        state.scenes.firstOrNull { it.id == state.focusedSceneId && !it.isCompleted }
            ?: state.scenes.firstOrNull { !it.isCompleted }
    }

    fun saveSoloScene(scene: Scene, onSuccess: () -> Unit) {
        if (_uiState.value.isSoloSceneBusy || scene.title.isBlank() || scene.sessionId != sessionId) return
        _uiState.update { it.copy(isSoloSceneBusy = true) }
        viewModelScope.launch {
            try {
                val id = sessionRepository.upsertScene(scene)
                focusScene(id)
                onSuccess()
            } catch (error: Exception) {
                if (error is kotlinx.coroutines.CancellationException) throw error
                _uiState.update { it.copy(snackbarMessage = "No se pudo guardar la escena: ${error.message}") }
            } finally {
                _uiState.update { it.copy(isSoloSceneBusy = false) }
            }
        }
    }

    fun closeSoloScene(scene: Scene, closure: SoloSceneClosure, onSuccess: () -> Unit) {
        if (_uiState.value.isSoloSceneBusy || scene.sessionId != sessionId) return
        _uiState.update { it.copy(isSoloSceneBusy = true) }
        viewModelScope.launch {
            try {
                notesMutex.withLock {
                    sessionRepository.updateDmNotes(sessionId, _uiState.value.dmNotes)
                    val closed = CloseSoloSceneUseCase(sessionRepository)(scene, closure)
                    _uiState.update { state ->
                        state.copy(
                            dmNotes = listOf(state.dmNotes, closed.journalEntry).filter { it.isNotBlank() }.joinToString("\n\n"),
                            focusedSceneId = closed.nextSceneId
                        )
                    }
                    // Persist edits made while the close transaction was running as well.
                    val savedNotes = _uiState.value.dmNotes
                    sessionRepository.updateDmNotes(sessionId, savedNotes)
                    _uiState.update { state -> if (state.dmNotes == savedNotes) state.copy(isSavingNotes = false) else state }
                }
                updateBoardMemory { it.copy(focusedSceneId = _uiState.value.focusedSceneId) }
                if (closure.chaosDelta != 0) updateBoardMythicChaos(closure.chaosDelta)
                onSuccess()
                _uiState.update { it.copy(snackbarMessage = "Escena cerrada y guardada en el diario") }
            } catch (error: Exception) {
                if (error is kotlinx.coroutines.CancellationException) throw error
                _uiState.update { it.copy(snackbarMessage = "No se pudo cerrar la escena: ${error.message}") }
            } finally {
                _uiState.update { it.copy(isSoloSceneBusy = false) }
            }
        }
    }

    fun addMythicCharacter(name: String) {
        val mythicId = _uiState.value.linkedMythicSession?.id ?: return
        if (name.isBlank()) return
        viewModelScope.launch {
            mythicRepository.addCharacter(MythicCharacter(sessionId = mythicId, name = name))
        }
    }

    fun deleteMythicCharacter(id: Long) {
        viewModelScope.launch {
            mythicRepository.deleteCharacter(id)
        }
    }

    fun updateMythicCharacterNotes(id: Long, notes: String) {
        viewModelScope.launch {
            mythicRepository.updateCharacterNotes(id, notes)
        }
    }

    fun sendMythicCharacterToNotes(character: MythicCharacter) {
        viewModelScope.launch {
            val addition = "\n\n[Personaje Mítico: ${character.name}]\n${character.notes}".trim()
            val newNotes = (_uiState.value.dmNotes + "\n\n" + addition).trim()
            _uiState.update { it.copy(dmNotes = newNotes) }
            notesMutex.withLock {
                if (_uiState.value.dmNotes == newNotes) sessionRepository.updateDmNotes(sessionId, newNotes)
            }
        }
    }

    fun addMythicThread(description: String) {
        val mythicId = _uiState.value.linkedMythicSession?.id ?: return
        if (description.isBlank()) return
        viewModelScope.launch {
            mythicRepository.addThread(MythicThread(sessionId = mythicId, description = description))
        }
    }

    fun updateMythicThreadDescription(id: Long, description: String) {
        // Optional thread note update
    }

    fun toggleMythicThreadStatus(id: Long, isResolved: Boolean) {
        viewModelScope.launch {
            mythicRepository.updateThreadStatus(id, isResolved)
        }
    }

    fun deleteMythicThread(id: Long) {
        viewModelScope.launch {
            mythicRepository.deleteThread(id)
        }
    }

    fun sendMythicThreadToNotes(thread: MythicThread) {
        viewModelScope.launch {
            val addition = "\n\n[Hilo Mítico: ${thread.description}]"
            val newNotes = (_uiState.value.dmNotes + "\n\n" + addition).trim()
            _uiState.update { it.copy(dmNotes = newNotes) }
            notesMutex.withLock {
                if (_uiState.value.dmNotes == newNotes) sessionRepository.updateDmNotes(sessionId, newNotes)
            }
        }
    }

    fun focusScene(sceneId: Long?) {
        updateBoardMemory { it.copy(focusedSceneId = sceneId) }
    }

    private fun updateBoardMemory(transform: (BoardSessionMemory) -> BoardSessionMemory) {
        val memory = transform(_uiState.value.boardMemory)
        settingsRepository.setBoardSessionMemory(sessionId, memory)
        _uiState.update { it.copy(boardMemory = memory, focusedSceneId = memory.focusedSceneId,
            pinnedTableRolls = memory.pinnedTableResults) }
    }

    fun setWorkspaceLayout(view: SessionBoardView, layout: BoardViewMemory) {
        updateBoardMemory { it.copy(views = it.views + (view.name to layout)) }
    }

    fun captureBoardInspiration(item: BoardInspiration) {
        updateBoardMemory { it.captureInspiration(item) }
    }

    fun setBoardInspirationStatus(id: String, status: InspirationStatus) {
        updateBoardMemory { it.setInspirationStatus(id, status) }
    }

    fun sendBoardInspirationToDiary(id: String) {
        if (_uiState.value.isSendingInspiration || _uiState.value.isSoloSceneBusy) return
        val item = _uiState.value.boardMemory.inspiration.firstOrNull { it.id == id } ?: return
        if (item.status == InspirationStatus.USED) return
        _uiState.update { it.copy(isSendingInspiration = true, isSavingNotes = true) }
        viewModelScope.launch {
            var savedNotes: String? = null
            try {
                notesMutex.withLock {
                    val entry = item.toDiaryEntry()
                    _uiState.update { state ->
                        val notes = if (entry in state.dmNotes) state.dmNotes
                            else listOf(state.dmNotes, entry).filter { it.isNotBlank() }.joinToString("\n\n")
                        state.copy(dmNotes = notes)
                    }
                    savedNotes = _uiState.value.dmNotes
                    sessionRepository.updateDmNotes(sessionId, savedNotes.orEmpty())
                }
                setBoardInspirationStatus(id, InspirationStatus.USED)
                _uiState.update { it.copy(snackbarMessage = "Inspiración guardada en el diario") }
            } catch (error: Exception) {
                if (error is kotlinx.coroutines.CancellationException) throw error
                _uiState.update { it.copy(snackbarMessage = "No se pudo guardar: ${error.message}") }
            } finally {
                _uiState.update { it.copy(isSendingInspiration = false,
                    isSavingNotes = if (it.dmNotes == savedNotes || savedNotes == null) false else it.isSavingNotes) }
            }
        }
    }

    fun prepareSceneForBoard(scene: Scene) {
        boardDelegate.prepareSceneResources(scene)
    }

    fun prepareSceneForBoard(sceneId: Long) {
        val scene = _uiState.value.scenes.find { it.id == sceneId }
        if (scene != null) {
            boardDelegate.prepareSceneResources(scene)
        } else {
            boardDelegate.addBoardBlock(
                type = SessionBoardItemType.NOTES,
                resourceIdOverride = sceneId
            )
        }
    }

    // --- HPL Unified Actions ---

    fun startHplNewDay() {
        val mapId = _uiState.value.linkedHexMapId ?: return
        viewModelScope.launch {
            // Logica básica de nuevo día
            val newDayId = com.deckapp.core.domain.usecase.hex.StartNewHexDayUseCase(hexRepository)(mapId)
            // (En una implementación real usaríamos el UseCase inyectado pero para demo basta así)
        }
    }

    fun openResourceManager() = _uiState.update { it.copy(showResourceManager = true) }
    fun dismissResourceManager() = _uiState.update { it.copy(showResourceManager = false) }

    fun openSessionConfig() = _uiState.update { it.copy(showSessionConfig = true) }
    fun dismissSessionConfig() = _uiState.update { it.copy(showSessionConfig = false) }

    fun toggleDeckInSession(deckId: Long) {
        viewModelScope.launch {
            val isPresent = _uiState.value.deckRefs.any { it.stackId == deckId }
            if (isPresent) {
                sessionRepository.removeDeckFromSession(sessionId, deckId)
            } else {
                sessionRepository.addDeckToSession(SessionDeckRef(sessionId, deckId))
            }
        }
    }

    fun toggleTableInSession(tableId: Long) {
        viewModelScope.launch {
            val isPresent = _uiState.value.tablesInSession.any { it.id == tableId }
            if (isPresent) {
                sessionRepository.removeTableFromSession(sessionId, tableId)
            } else {
                sessionRepository.addTableToSession(sessionId, tableId)
            }
        }
    }

    /**
     * Alterna el estado expandido/colapsado de un cluster de mazo en el Workspace Bento.
     * El estado de colapso es efímero (no se persiste en Room).
     */
    fun toggleDeckCollapse(stackId: Long) {
        _uiState.update { state ->
            val current = state.collapsedDeckIds
            val updated = if (stackId in current) current - stackId else current + stackId
            state.copy(collapsedDeckIds = updated)
        }
    }

    /**
     * Registra el último cluster con el que interactuó el DM.
     * El FAB usa esta información para saber de qué mazo robar.
     */
    fun setLastInteractedDeck(stackId: Long) {
        _uiState.update { it.copy(selectedDeckId = stackId) }
    }

    /** Conserva un resultado visible al retomar la mesa sin alterar el historial de tiradas. */
    fun pinTableRoll(tableId: Long, result: TableRollResult) {
        if (result.tableId != tableId || result.sessionId != sessionId) return
        updateBoardMemory { it.copy(pinnedTableResults = it.pinnedTableResults + (tableId to result)) }
    }

    fun unpinTableRoll(tableId: Long) {
        updateBoardMemory { it.copy(pinnedTableResults = it.pinnedTableResults - tableId) }
    }

    /** Tira una tabla aleatoria vinculada a un recurso (ej: carta) y muestra el resultado. */
    fun rollActiveTable(tableId: Long) {
        viewModelScope.launch {
            val result = rollTableUseCase(tableId, sessionId)
            _uiState.update { it.copy(snackbarMessage = "${result.tableName}: ${result.resolvedText}") }
        }
    }

    /** Abre el dialog de nota rápida. */
    fun showQuickNote() {
        _uiState.update { it.copy(showQuickNoteDialog = true) }
    }

    /** Cierra el dialog de nota rápida sin guardar. */
    fun dismissQuickNote() {
        _uiState.update { it.copy(showQuickNoteDialog = false) }
    }

    /**
     * Añade una nota rápida con timestamp al texto de notas existente y navega al tab Notas.
     * Formato: "[HH:mm] texto\n"
     */
    fun addQuickNote(text: String) {
        if (text.isBlank()) {
            _uiState.update { it.copy(showQuickNoteDialog = false) }
            return
        }
        val time = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
            .format(java.util.Date())
        val entry = "[$time] $text"
        val current = _uiState.value.dmNotes
        val updated = if (current.isBlank()) entry else "$current\n$entry"
        _uiState.update { it.copy(dmNotes = updated, showQuickNoteDialog = false, activeTab = 4) }
        viewModelScope.launch {
            notesMutex.withLock {
                if (_uiState.value.dmNotes == updated) sessionRepository.updateDmNotes(sessionId, updated)
            }
        }
    }

    /** Actualiza las notas de la sesión (llamado desde el editor con debounce en la UI). */
    fun updateNotes(notes: String) {
        if (notes == _uiState.value.dmNotes) return
        
        _uiState.update { it.copy(dmNotes = notes, isSavingNotes = true) }
        viewModelScope.launch {
            notesMutex.withLock {
                if (_uiState.value.dmNotes == notes) sessionRepository.updateDmNotes(sessionId, notes)
            }
            // Pequeña pausa para que el indicador sea perceptible si la DB es muy rápida
            delay(400)
            _uiState.update { if (it.dmNotes == notes) it.copy(isSavingNotes = false) else it }
        }
    }

    /** Actualiza los sistemas de juego de la sesión. */
    fun updateGameSystems(systems: List<String>) {
        viewModelScope.launch {
            sessionRepository.updateGameSystems(sessionId, systems)
        }
    }

    /**
     * Selecciona un mazo y roba una carta de él.
     */
    fun drawCardFromDeck(stackId: Long) {
        _uiState.update { it.copy(selectedDeckId = stackId) }
        viewModelScope.launch {
            val deckRef = _uiState.value.deckRefs.find { it.stackId == stackId } ?: return@launch
            val card = drawCardUseCase(
                deckId = stackId,
                sessionId = sessionId,
                drawMode = deckRef.drawModeOverride ?: DrawMode.RANDOM
            )
            if (card == null) {
                val deckName = _uiState.value.deckNames[stackId] ?: "Mazo"
                _uiState.update { it.copy(snackbarMessage = "\"$deckName\" no tiene más cartas disponibles") }
            }
        }
    }

    /**
     * Roba una carta del mazo seleccionado.
     * DrawCardUseCase persiste el DrawEvent ANTES de retornar → crash-safe.
     */
    fun drawCard() {
        val state = _uiState.value
        val deckId = state.selectedDeckId ?: state.deckRefs.firstOrNull()?.stackId ?: return
        val deckRef = state.deckRefs.find { it.stackId == deckId } ?: return
        viewModelScope.launch {
            val card = drawCardUseCase(
                deckId = deckRef.stackId,
                sessionId = sessionId,
                drawMode = deckRef.drawModeOverride ?: DrawMode.RANDOM
            )
            if (card == null) {
                val deckName = state.deckNames[deckId] ?: "Mazo"
                _uiState.update { it.copy(snackbarMessage = "\"$deckName\" no tiene más cartas disponibles") }
            }
        }
    }

    /** Descarta una carta de la mano a la pila. */
    fun discardCard(cardId: Long) {
        viewModelScope.launch {
            discardCardUseCase(sessionId, cardId)
        }
    }

    /** Regresa todas las cartas de la pila al mazo (Shuffle back). Activa animación C-5. */
    fun shufflePileBack(stackId: Long? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isShuffling = true) }

            val piledCards = cardRepository.getPiledCards(sessionId).first()
            val cardsToReturn = if (stackId != null) {
                piledCards.filter { it.stackId == stackId }
            } else {
                piledCards
            }

            if (cardsToReturn.isNotEmpty()) {
                // Marcamos como no robadas usando el Use Case unificado
                cardsToReturn.forEach { card ->
                    updateCardStateUseCase(card.id, UpdateCardStateUseCase.CardStateUpdate(isDrawn = false))
                }
                
                // Barajamos el mazo o mazos afectados para que el orden sea fresco
                if (stackId != null) {
                    shuffleDeckUseCase(stackId, onlyAvailable = true)
                } else {
                    cardsToReturn.map { it.stackId }.distinct().forEach { id ->
                        shuffleDeckUseCase(id, onlyAvailable = true)
                    }
                }

                // Registramos el evento de barajado
                sessionRepository.logEvent(
                    DrawEvent(
                        sessionId = sessionId,
                        cardId = null,
                        action = DrawAction.SHUFFLE_BACK,
                        metadata = if (stackId != null) "Mazo: $stackId" else "Toda la pila"
                    )
                )
            }

            // Tiempo mínimo de animación: 900ms
            delay(900)
            _uiState.update { it.copy(isShuffling = false, snackbarMessage = if (cardsToReturn.isNotEmpty()) "Pila barajada de vuelta al mazo" else null) }
        }
    }

    // ── D-2: Robar por palo ──────────────────────────────────────────────────

    /** Abre el sheet de "Robar por palo" cargando los palos disponibles del mazo activo. */
    fun openDrawBySuit() {
        val deckId = _uiState.value.selectedDeckId ?: return
        viewModelScope.launch {
            val suits = cardRepository.getCardsForStack(deckId).first()
                .filter { !it.isDrawn }
                .mapNotNull { it.suit }
                .distinct()
                .sorted()
            _uiState.update { it.copy(showDrawBySuitSheet = true, availableSuitsForDraw = suits) }
        }
    }

    /** Roba una carta aleatoria del palo indicado en el mazo activo. */
    fun drawCardBySuit(suit: String) {
        val deckId = _uiState.value.selectedDeckId ?: return
        _uiState.update { it.copy(showDrawBySuitSheet = false) }
        viewModelScope.launch {
            val available = cardRepository.getCardsForStack(deckId).first()
                .filter { !it.isDrawn && it.suit == suit }
            if (available.isEmpty()) {
                _uiState.update { it.copy(snackbarMessage = "No quedan cartas del palo \"$suit\"") }
                return@launch
            }
            val card = available.random()
            val deck = cardRepository.getDeckById(deckId).first()
            val faceDown = deck?.drawFaceDown == true
            
            updateCardStateUseCase(
                cardId = card.id, 
                update = UpdateCardStateUseCase.CardStateUpdate(
                    isDrawn = true, 
                    lastDrawnAt = System.currentTimeMillis(),
                    isRevealed = !faceDown
                )
            )

            sessionRepository.logEvent(
                DrawEvent(sessionId = sessionId, cardId = card.id, action = DrawAction.DRAW)
            )
        }
    }

    fun closeDrawBySuitSheet() {
        _uiState.update { it.copy(showDrawBySuitSheet = false) }
    }

    /** Mueve una carta específica de la pila a la mano. */
    fun returnToHand(cardId: Long) {
        viewModelScope.launch {
            updateCardStateUseCase(cardId, UpdateCardStateUseCase.CardStateUpdate(isDrawn = true, lastDrawnAt = System.currentTimeMillis()))
        }
    }

    /** Mueve una carta específica de la pila de vuelta al mazo. */
    fun returnToDeck(cardId: Long) {
        viewModelScope.launch {
            updateCardStateUseCase(cardId, UpdateCardStateUseCase.CardStateUpdate(isDrawn = false))
        }
    }

    /** Resetea todos los mazos de la sesión. */
    fun resetDeck() {
        viewModelScope.launch {
            _uiState.value.deckRefs.forEach { ref ->
                resetDeckUseCase(sessionId, ref.stackId)
            }
        }
    }

    /**
     * Resetea un único mazo: devuelve al mazo todas sus cartas (mano + pila de descarte).
     * Loguea un RESET individual con metadata del stackId.
     */
    fun resetSingleDeck(stackId: Long) {
        viewModelScope.launch {
            resetDeckUseCase(sessionId, stackId)
            val name = _uiState.value.deckNames[stackId] ?: "Mazo"
            _uiState.update { it.copy(snackbarMessage = "\"$name\" reseteado") }
        }
    }

    /** Deshace la última acción via event log. */
    fun undo() {
        viewModelScope.launch {
            val undone = undoLastActionUseCase(sessionId)
            val msg = if (undone) "Acción deshecha" else "Nada que deshacer"
            _uiState.update { it.copy(snackbarMessage = msg) }
        }
    }

    /** Finaliza la sesión marcándola como inactiva en Room. */
    fun endSession() {
        viewModelScope.launch {
            sessionRepository.endSession(sessionId)
            _uiState.update { it.copy(sessionEnded = true) }
        }
    }

    /** Alterna el modo nocturno. */
    fun toggleNightMode() {
        _uiState.update { it.copy(nightMode = !it.nightMode) }
    }

    /** Alterna la visibilidad de los nombres de las cartas. */
    fun toggleCardTitles() {
        val current = _uiState.value.session?.showCardTitles ?: true
        viewModelScope.launch {
            sessionRepository.toggleCardTitles(sessionId, !current)
        }
    }

    /** Ordena la mano actual por valor de carta (Iniciativa). */
    fun sortByValue() {
        val sortedHand = _uiState.value.hand.sortedBy { it.value ?: Int.MAX_VALUE }
        _uiState.update { it.copy(hand = sortedHand) }
    }

    /** Muestra la primera carta disponible del mazo sin modificar su estado (Peek). */
    fun peekTopCard() {
        val deckId = _uiState.value.selectedDeckId ?: _uiState.value.deckRefs.firstOrNull()?.stackId ?: return
        viewModelScope.launch {
            val card = cardRepository.getTopCard(deckId)
            if (card == null) {
                _uiState.update { it.copy(snackbarMessage = "No hay cartas disponibles") }
            } else {
                _uiState.update { it.copy(peekCard = card) }
                peekCardId = card.id
            }
        }
    }

    fun clearPeek() {
        _uiState.update { it.copy(peekCard = null) }
        peekCardId = null
    }

    /** Revela una carta que fue robada boca abajo. Loguea FLIP como evento de revelación. */
    fun revealCard(cardId: Long) {
        viewModelScope.launch {
            updateCardStateUseCase(cardId, UpdateCardStateUseCase.CardStateUpdate(isRevealed = true))
            sessionRepository.logEvent(
                DrawEvent(sessionId = sessionId, cardId = cardId, action = DrawAction.FLIP,
                    metadata = "reveal")
            )
        }
    }

    /**
     * Roba [count] cartas del mazo seleccionado en secuencia.
     * Si el mazo se queda sin cartas antes de completar, notifica con un snackbar.
     */
    fun dealCards(count: Int) {
        val state = _uiState.value
        val deckId = state.selectedDeckId ?: state.deckRefs.firstOrNull()?.stackId ?: return
        val deckRef = state.deckRefs.find { it.stackId == deckId } ?: return
        viewModelScope.launch {
            var drawn = 0
            repeat(count) {
                val card = drawCardUseCase(
                    deckId = deckRef.stackId,
                    sessionId = sessionId,
                    drawMode = deckRef.drawModeOverride ?: DrawMode.RANDOM
                )
                if (card != null) drawn++
            }
            if (drawn < count) {
                _uiState.update { it.copy(snackbarMessage = "Solo quedaban $drawn carta${if (drawn != 1) "s" else ""} disponibles") }
            }
        }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun clearSnackbarMessage() = clearSnackbar()

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun clearErrorMessage() = clearError()

    fun saveCheckpointOnPause() {
        // Estado reactivo y persistido en Room
    }

    fun undoLastDraw() {
        viewModelScope.launch {
            val success = undoLastActionUseCase(sessionId)
            if (success) {
                _uiState.update { it.copy(canUndo = false, snackbarMessage = "Acción deshecha") }
            }
        }
    }

    fun openGlobalSearch() {
        _uiState.update { it.copy(showQuickSearch = true) }
    }

    // --- NPCs & Voice ---

    fun playNpcVoice(path: String) {
        if (_uiState.value.playingNpcVoicePath == path) {
            stopNpcVoice()
            return
        }
        
        stopNpcVoice()
        _uiState.update { it.copy(playingNpcVoicePath = path) }
        voicePlayer.playPath(path) {
            _uiState.update { it.copy(playingNpcVoicePath = null) }
        }
    }

    fun stopNpcVoice() {
        voicePlayer.stop()
        _uiState.update { it.copy(playingNpcVoicePath = null) }
    }

    override fun onCleared() {
        super.onCleared()
        voicePlayer.stop()
    }

    // --- Combat Actions ---

    fun applyDamage(creatureId: Long, delta: Int) {
        viewModelScope.launch {
            applyDamageUseCase(creatureId, delta)
        }
    }

    fun nextTurn() {
        val encounter = _uiState.value.activeEncounter ?: return
        val players = _uiState.value.playerParticipants
        
        viewModelScope.launch {
            // Unificar para calcular el total de participantes
            val totalParticipants = encounter.creatures.size + players.size
            if (totalParticipants == 0) return@launch
            
            var nextIndex = encounter.currentTurnIndex + 1
            var nextRound = encounter.currentRound
            
            if (nextIndex >= totalParticipants) {
                nextIndex = 0
                nextRound++
            }
            
            encounterRepository.saveEncounter(
                encounter.copy(
                    currentTurnIndex = nextIndex,
                    currentRound = nextRound
                )
            )
            
            // Registrar log
            if (nextRound > encounter.currentRound) {
                encounterRepository.recordLog(
                    com.deckapp.core.model.CombatLogEntry(
                        encounterId = encounter.id,
                        message = "Inicio de la Ronda $nextRound",
                        type = com.deckapp.core.model.CombatLogType.ROUND_START
                    )
                )
            }
        }
    }

    /** Inicia el proceso de combate: tira iniciativa para los que faltan y establece el orden. */
    fun startCombatProcess() {
        val encounter = _uiState.value.activeEncounter ?: return
        viewModelScope.launch {
            // 1. Tirar dados para criaturas sin iniciativa (NPCs)
            rollInitiativeUseCase(encounter.id)
            // 2. Establecer el orden determinista (sortOrder)
            calculateInitiativeOrderUseCase(encounter.id)
        }
    }

    /** Cambia el estado de una condición con registro narrativo automático. */
    fun toggleCondition(creatureId: Long, condition: Condition) {
        viewModelScope.launch {
            toggleConditionUseCase(creatureId, condition)
        }
    }

    /** Elimina una criatura y recalcula el orden del tracker. */
    fun removeCreature(creatureId: Long) {
        val encounter = _uiState.value.activeEncounter ?: return
        viewModelScope.launch {
            cleanupCombatUseCase.removeCreature(encounter.id, creatureId)
        }
    }

    fun addPlayerParticipant(name: String, initiative: Int) {
        _uiState.update { it.copy(
            playerParticipants = it.playerParticipants + PlayerInitiativeParticipant(name = name, initiativeTotal = initiative)
        ) }
    }

    fun removePlayerParticipant(id: String) {
        _uiState.update { it.copy(
            playerParticipants = it.playerParticipants.filter { p -> p.id != id }
        ) }
    }

    fun endEncounter() {
        val encounter = _uiState.value.activeEncounter ?: return
        viewModelScope.launch {
            // 1. Generar Resumen
            val survivors = encounter.creatures
                .filter { it.currentHp > 0 }
                .joinToString(", ") { it.name }
            val summary = """
                
                --- RESUMEN DE COMBATE: ${encounter.name} ---
                Rondas: ${encounter.currentRound}
                Supervivientes: ${if (survivors.isEmpty()) "Ninguno" else survivors}
                ------------------------------------------
            """.trimIndent()
            
            // 2. Anexar a notas
            val updatedNotes = "${_uiState.value.dmNotes}\n$summary"
            updateNotes(updatedNotes)
            
            // 3. Desactivar el encuentro y limpiar PJs temporales
            encounterRepository.saveEncounter(encounter.copy(isActive = false))
            
            // 4. Volver a mazos si estábamos en combate
            if (_uiState.value.activeTab == 5) {
                _uiState.update { it.copy(
                    activeTab = 0, 
                    snackbarMessage = "Combate finalizado y resumido en notas",
                    playerParticipants = emptyList() // Limpiamos PJs al terminar
                ) }
            }
        }
    }

    /** Genera un log de texto con los eventos de la sesión. */
    suspend fun getSessionLogText(): String {
        val events = sessionRepository.getEventsForSession(sessionId).first()
        val sessionName = _uiState.value.session?.name ?: "Sesión"
        val sb = StringBuilder()
        sb.append("LOG DE SESIÓN: $sessionName\n")
        sb.append("Generado: ${java.text.SimpleDateFormat("dd/MM/yyyy HH:mm").format(java.util.Date())}\n")
        sb.append("--------------------------------------------------\n\n")

        sb.append("--- ACTIVIDAD DE CARTAS ---\n")
        events.forEach { event ->
            val time = java.text.SimpleDateFormat("HH:mm:ss").format(java.util.Date(event.timestamp))
            sb.append("[$time] ${event.action}: Carta ${event.cardId} ${event.metadata}\n")
        }
        
        // Sprint 16: Logs de Combate
        val encounters = encounterRepository.getAllEncounters().first()
            .filter { it.linkedSessionId == sessionId }
        
        if (encounters.isNotEmpty()) {
            sb.append("\n\n--- HISTORIAL DE COMBATES ---\n")
            encounters.forEach { enc ->
                sb.append("\nENCUENTRO: ${enc.name} (Rondas: ${enc.currentRound})\n")
                val logEntries = encounterRepository.getLogForEncounter(enc.id).first()
                logEntries.reversed().forEach { entry ->
                    val time = java.text.SimpleDateFormat("HH:mm:ss").format(java.util.Date(entry.timestamp))
                    sb.append("[$time] ${entry.message}\n")
                }
            }
        }
        
        return sb.toString()
    }

    /** Escribe el log de la sesión en el URI proporcionado. */
    fun exportLog(uri: android.net.Uri, contentResolver: android.content.ContentResolver) {
        viewModelScope.launch {
            try {
                val text = getSessionLogText()
                contentResolver.openOutputStream(uri)?.use { output ->
                    output.write(text.toByteArray())
                }
                _uiState.update { it.copy(snackbarMessage = "Log exportado correctamente") }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Error al exportar log: ${e.message}") }
            }
        }
    }
    fun toggleSceneCompletion(sceneId: Long, completed: Boolean) {
        viewModelScope.launch {
            sessionRepository.updateSceneCompletion(sceneId, completed)
        }
    }

    // --- Scene Triggers (D-2+) ---

    fun rollSceneTable(tableId: Long) {
        rollActiveTable(tableId)
    }

    fun drawSceneCard(deckId: Long) {
        drawCardFromDeck(deckId)
    }

    // --- Permanent Players ---

    fun savePermanentPlayer(player: Npc) {
        viewModelScope.launch {
            npcRepository.saveNpc(player)
            _uiState.update { state ->
                val updated = state.permanentPlayers.filter { it.id != player.id } + player
                state.copy(permanentPlayers = updated)
            }
        }
    }

    fun deletePermanentPlayer(player: Npc) {
        viewModelScope.launch {
            npcRepository.deleteNpc(player.id)
            _uiState.update { state ->
                state.copy(permanentPlayers = state.permanentPlayers.filter { it.id != player.id })
            }
        }
    }

    // --- Hex Map Exploration Actions ---

    fun linkHexMap(mapId: Long?) {
        _uiState.update { it.copy(linkedHexMapId = mapId) }
    }

    fun moveParty(q: Int, r: Int) {
        val mapId = _uiState.value.linkedHexMapId ?: return
        viewModelScope.launch {
            hexRepository.updatePartyLocation(mapId, q, r)
        }
    }

    fun toggleTileExplored(tile: HexTile) {
        viewModelScope.launch {
            hexRepository.upsertHexTile(tile.copy(isExplored = !tile.isExplored))
        }
    }

    fun toggleTileReconnoitered(tile: HexTile) {
        viewModelScope.launch {
            hexRepository.upsertHexTile(tile.copy(isReconnoitered = !tile.isReconnoitered))
        }
    }

    fun toggleTileMapped(tile: HexTile) {
        viewModelScope.launch {
            hexRepository.upsertHexTile(tile.copy(isMapped = !tile.isMapped))
        }
    }

    // --- Tension Clocks ---

    fun tickClock(clockId: Long, amount: Int) {
        _uiState.update { state ->
            state.copy(
                tensionClocks = state.tensionClocks.map { clock ->
                    if (clock.id == clockId) {
                        val newProgress = (clock.progress + amount).coerceIn(0, clock.segments)
                        clock.copy(progress = newProgress)
                    } else clock
                }
            )
        }
    }

    fun updateClockDescriptionAndProgress(clockId: Long, newDescription: String, newProgress: Int) {
        _uiState.update { state ->
            state.copy(
                tensionClocks = state.tensionClocks.map { clock ->
                    if (clock.id == clockId) {
                        clock.copy(
                            triggerDescription = newDescription,
                            progress = newProgress.coerceIn(0, clock.segments)
                        )
                    } else clock
                }
            )
        }
    }

    fun createClockAndAddToBoard(
        name: String,
        segments: Int,
        colorHex: String?,
        triggerDescription: String,
        targetGroupId: Long? = null
    ) {
        val clockId = System.currentTimeMillis()
        val newClock = TensionClock(
            id = clockId,
            sessionId = sessionId,
            name = name,
            segments = segments,
            progress = 0,
            colorHex = colorHex,
            triggerDescription = triggerDescription
        )
        _uiState.update { it.copy(tensionClocks = it.tensionClocks + newClock) }
        addBoardBlock(
            type = SessionBoardItemType.CLOCK,
            size = SessionBoardItemSize.S,
            titleOverride = name,
            resourceId = clockId
        )
    }

    fun renameSession(name: String) {
        viewModelScope.launch {
            _uiState.value.session?.let { current ->
                sessionRepository.renameSession(current.id, name)
            }
        }
    }

    fun createAndLinkMythicSession() {
        viewModelScope.launch {
            val sessionName = "Mítico - ${_uiState.value.session?.name ?: "Sesión"}"
            val newSessionId = mythicRepository.saveSession(MythicSession(name = sessionName))
            linkMythicSession(newSessionId)
        }
    }

    fun resetFloatingPanels() {
        _uiState.update { it.copy(activeLeftSidebar = SidebarTool.NONE, activeRightSidebar = SidebarTool.NONE) }
    }

    fun sendResultToQuickNotes(tableName: String, resultText: String) {
        viewModelScope.launch {
            val currentNotes = _uiState.value.dmNotes
            val addition = "\n\n[$tableName]: $resultText"
            val newNotes = (currentNotes + addition).trim()
            _uiState.update { it.copy(dmNotes = newNotes) }
            notesMutex.withLock {
                if (_uiState.value.dmNotes == newNotes) sessionRepository.updateDmNotes(sessionId, newNotes)
            }
        }
    }

    fun startEncounterFromScene(encounterId: Long) {
        viewModelScope.launch {
            encounterRepository.getEncounterById(encounterId).firstOrNull()?.let { encounter ->
                encounterRepository.saveEncounter(encounter.copy(linkedSessionId = sessionId, isActive = true))
                setActiveBoardView(SessionBoardView.COMBAT)
            }
        }
    }

    // --- NPCs & Wiki Updates ---

    fun updateNpcHp(npcId: Long, hp: Int) {
        viewModelScope.launch {
            val npc = npcRepository.getNpcById(npcId) ?: return@launch
            npcRepository.saveNpc(npc.copy(currentHp = hp))
        }
    }

    fun updateWikiEntry(entry: WikiEntry) {
        viewModelScope.launch {
            wikiRepository.saveEntry(entry)
        }
    }

    // --- Smart Dice & Tags ---

    fun rollSmartDice(
        formula: String,
        advantage: Boolean = false,
        disadvantage: Boolean = false,
        extraDice: String? = null
    ) {
        viewModelScope.launch {
            try {
                val cleaned = formula.trim()
                var (actualFormula, description) = if (cleaned.contains(":")) {
                    val parts = cleaned.split(":")
                    val dicePart = parts.last().trim()
                    val descPart = parts.dropLast(1).joinToString(":").trim()
                    dicePart to descPart
                } else {
                    cleaned to ""
                }

                var modDesc = ""
                if (advantage && (actualFormula.contains("1d20") || actualFormula.contains("d20"))) {
                    actualFormula = actualFormula.replace("1d20", "2d20kh1").replace("d20", "2d20kh1")
                    modDesc = " con Ventaja"
                } else if (disadvantage && (actualFormula.contains("1d20") || actualFormula.contains("d20"))) {
                    actualFormula = actualFormula.replace("1d20", "2d20kl1").replace("d20", "2d20kl1")
                    modDesc = " con Desventaja"
                }

                var total = DiceEvaluator.evaluate(actualFormula)
                var extraDetail = ""
                if (!extraDice.isNullOrBlank()) {
                    val extraRoll = DiceEvaluator.evaluate(extraDice)
                    total += extraRoll
                    extraDetail = " + $extraDice ($extraRoll)"
                }

                val fullDesc = if (description.isNotBlank()) description + modDesc else "Tirada$modDesc"
                val displayFormula = if (extraDetail.isNotBlank()) "$actualFormula$extraDetail" else actualFormula
                val newRoll = DiceRollResult(
                    expression = if (description.isNotBlank()) "$description ($displayFormula)" else displayFormula,
                    total = total
                )

                _uiState.update { state ->
                    val updatedRolls = (listOf(newRoll) + state.recentDiceRolls).take(10)
                    state.copy(
                        snackbarMessage = "$fullDesc ($displayFormula): $total",
                        recentDiceRolls = updatedRolls
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(snackbarMessage = "Fórmula incorrecta: $formula") }
            }
        }
    }

    fun rollSmartDiceWithDescription(formula: String, description: String) {
        viewModelScope.launch {
            try {
                val total = DiceEvaluator.evaluate(formula)
                val displayFormula = if (description.isNotBlank()) "$description ($formula)" else formula
                val newRoll = DiceRollResult(expression = displayFormula, total = total)
                _uiState.update { state ->
                    val updatedRolls = (listOf(newRoll) + state.recentDiceRolls).take(10)
                    state.copy(
                        snackbarMessage = "$description ($formula): $total",
                        recentDiceRolls = updatedRolls
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(snackbarMessage = "Fórmula incorrecta: $formula") }
            }
        }
    }

    fun handleSmartTag(tag: String) {
        _uiState.update { it.copy(snackbarMessage = "Etiqueta: $tag") }
    }

    // --- Decks Actions ---

    fun shuffleSingleDeck(stackId: Long) {
        shufflePileBack(stackId)
    }

    // --- Board Bento Actions (Delegated to BoardDelegate) ---

    fun addBoardBlock(
        type: SessionBoardItemType,
        size: SessionBoardItemSize = SessionBoardItemSize.M,
        displayMode: String? = null,
        titleOverride: String? = null,
        resourceId: Long? = null
    ) = boardDelegate.addBoardBlock(type, resourceId)

    fun removeBoardBlock(itemId: Long) = boardDelegate.removeBoardBlock(itemId)
    fun moveBoardBlock(itemId: Long, direction: Int) = boardDelegate.moveBoardBlock(itemId, direction)
    fun moveBoardBlockTo(fromId: Long, toId: Long) = boardDelegate.moveBoardBlockTo(fromId, toId)
    fun setBoardBlockSize(itemId: Long, size: SessionBoardItemSize) = boardDelegate.setBoardBlockSize(itemId, size)
    fun setBoardBlockDisplayMode(itemId: Long, displayMode: String?) = boardDelegate.setBoardBlockDisplayMode(itemId, displayMode)
    fun setBoardBlockTitleOverride(itemId: Long, title: String?) = boardDelegate.setBoardBlockTitleOverride(itemId, title)
    fun setBoardBlockResourceId(itemId: Long, resourceId: Long?) = boardDelegate.setBoardBlockResourceId(itemId, resourceId)
    fun setActiveBoardView(view: SessionBoardView) = boardDelegate.setActiveBoardView(view)
    fun duplicateBoardView(target: SessionBoardView) = boardDelegate.duplicateBoardView(target)
    fun updateBoardViewLabel(view: SessionBoardView, label: String) = boardDelegate.updateBoardViewLabel(view, label)
    fun clearBoardView(view: SessionBoardView) = boardDelegate.clearBoardView(view)
    fun updateBoardViewLabels(labels: SessionBoardViewLabels) = boardDelegate.updateBoardViewLabels(labels)
    fun applyBoardTemplate(template: SessionBoardTemplatePreset) = boardDelegate.applyBoardTemplate(template)
    fun togglePinBlock(itemId: Long) = boardDelegate.togglePinBlock(itemId)
    fun toggleCollapseBlock(itemId: Long) = boardDelegate.toggleCollapseBlock(itemId)
    fun setAllBlocksCollapsedInView(collapsed: Boolean) = boardDelegate.setAllBlocksCollapsedInView(_uiState.value.activeBoardView, collapsed)
    fun createBoardGroup(name: String, colorHex: String? = null) = boardDelegate.createBoardGroup(name, colorHex, null, null)
    fun renameBoardGroup(groupId: Long, newName: String) = boardDelegate.renameBoardGroup(groupId, newName, null, null, null)
    fun deleteBoardGroup(groupId: Long) = boardDelegate.deleteBoardGroup(groupId)
    fun toggleBoardGroupCollapsed(groupId: Long) {
        val group = _uiState.value.boardGroups.find { it.id == groupId } ?: return
        boardDelegate.toggleBoardGroupCollapsed(groupId, !group.isCollapsed)
    }
    fun assignItemToGroup(itemId: Long, groupId: Long?) = boardDelegate.assignItemToGroup(itemId, groupId)
    fun mergeBoardItems(sourceId: Long, targetId: Long, onGroupCreated: (SessionBoardGroup) -> Unit = {}) =
        boardDelegate.mergeBoardItems(sourceId, targetId, onGroupCreated)

    // --- Session Hotbar Actions (Sprint 59) ---

    fun toggleHotbarExpanded() {
        _uiState.update { it.copy(isHotbarExpanded = !it.isHotbarExpanded) }
    }

    fun openCreateHotbarAction() {
        _uiState.update { it.copy(showHotbarActionDialog = true, editingHotbarAction = null) }
    }

    fun openEditHotbarAction(action: SessionHotbarAction) {
        _uiState.update { it.copy(showHotbarActionDialog = true, editingHotbarAction = action) }
    }

    fun dismissHotbarActionDialog() {
        _uiState.update { it.copy(showHotbarActionDialog = false, editingHotbarAction = null) }
    }

    fun saveHotbarAction(action: SessionHotbarAction) {
        val currentActions = _uiState.value.session?.hotbarActions ?: defaultHotbarActions()
        val index = currentActions.indexOfFirst { it.id == action.id }
        val updatedActions = if (index >= 0) {
            currentActions.toMutableList().apply { set(index, action) }
        } else {
            currentActions + action
        }
        _uiState.update { state ->
            state.copy(
                session = state.session?.copy(hotbarActions = updatedActions),
                showHotbarActionDialog = false,
                editingHotbarAction = null
            )
        }
        viewModelScope.launch {
            sessionRepository.updateHotbarActions(sessionId, updatedActions)
        }
    }

    fun deleteHotbarAction(actionId: String) {
        val currentActions = _uiState.value.session?.hotbarActions ?: defaultHotbarActions()
        val updatedActions = currentActions.filterNot { it.id == actionId }
        _uiState.update { state ->
            state.copy(
                session = state.session?.copy(hotbarActions = updatedActions),
                showHotbarActionDialog = false,
                editingHotbarAction = null
            )
        }
        viewModelScope.launch {
            sessionRepository.updateHotbarActions(sessionId, updatedActions)
        }
    }

    fun executeHotbarAction(action: SessionHotbarAction) {
        when (action.type) {
            HotbarActionType.DICE_ROLL -> {
                rollSmartDiceWithDescription(action.payload, action.label)
            }
            HotbarActionType.ORACLE_QUERY -> {
                when (action.payload.uppercase()) {
                    "RANDOM_EVENT" -> rollBoardMythicRandomEvent()
                    "CERTAIN" -> rollBoardMythicFate(ProbabilityLevel.CERTAIN)
                    "NEAR_SURE", "NEARLY_CERTAIN" -> rollBoardMythicFate(ProbabilityLevel.NEARLY_CERTAIN)
                    "VERY_LIKELY" -> rollBoardMythicFate(ProbabilityLevel.VERY_LIKELY)
                    "LIKELY" -> rollBoardMythicFate(ProbabilityLevel.LIKELY)
                    "UNLIKELY" -> rollBoardMythicFate(ProbabilityLevel.UNLIKELY)
                    "VERY_UNLIKELY" -> rollBoardMythicFate(ProbabilityLevel.VERY_UNLIKELY)
                    "NEAR_IMPOSSIBLE", "NEARLY_IMPOSSIBLE" -> rollBoardMythicFate(ProbabilityLevel.NEARLY_IMPOSSIBLE)
                    "IMPOSSIBLE" -> rollBoardMythicFate(ProbabilityLevel.IMPOSSIBLE)
                    else -> rollBoardMythicFate(ProbabilityLevel.FIFTY_FIFTY)
                }
            }
            HotbarActionType.RANDOM_TABLE -> {
                val tableId = action.payload.toLongOrNull()
                if (tableId != null) {
                    rollActiveTable(tableId)
                } else {
                    _uiState.update { it.copy(snackbarMessage = "Tabla no vinculada: ${action.label}") }
                }
            }
            HotbarActionType.DRAW_CARD -> {
                val deckId = action.payload.toLongOrNull() ?: _uiState.value.selectedDeckId
                if (deckId != null) {
                    drawCardFromDeck(deckId)
                } else {
                    _uiState.update { it.copy(snackbarMessage = "No hay mazo seleccionado para robar") }
                }
            }
        }
    }

    // --- Scene Framing Assistant (Sprint 60) ---

    fun openSceneFraming() {
        _uiState.update { it.copy(showSceneFramingDialog = true) }
    }

    fun dismissSceneFraming() {
        _uiState.update { it.copy(showSceneFramingDialog = false) }
    }

    fun confirmSceneFraming(
        draft: SceneFramingDraft,
        closePrevious: Boolean,
        prevSummary: String,
        chaosDelta: Int
    ) {
        viewModelScope.launch {
            try {
                val currentActiveScene = _uiState.value.scenes.find { it.id == _uiState.value.focusedSceneId && !it.isCompleted }
                    ?: _uiState.value.scenes.firstOrNull { !it.isCompleted }

                if (closePrevious && currentActiveScene != null) {
                    val closure = SoloSceneClosure(
                        summary = prevSummary.ifBlank { "Conclusión de la escena." },
                        chaosDelta = chaosDelta
                    )
                    notesMutex.withLock {
                        sessionRepository.updateDmNotes(sessionId, _uiState.value.dmNotes)
                        val closed = CloseSoloSceneUseCase(sessionRepository)(currentActiveScene, closure)
                        _uiState.update { state ->
                            state.copy(
                                dmNotes = listOf(state.dmNotes, closed.journalEntry).filter { it.isNotBlank() }.joinToString("\n\n")
                            )
                        }
                    }
                    if (closure.chaosDelta != 0) {
                        updateBoardMythicChaos(closure.chaosDelta)
                    }
                }

                val sceneCount = _uiState.value.scenes.size + 1
                val newScene = Scene(
                    sessionId = sessionId,
                    title = draft.title,
                    content = SoloSceneContext(
                        location = draft.location,
                        objective = draft.objective,
                        situation = draft.adjustedSituation.ifBlank { draft.expectation },
                        pendingQuestion = ""
                    ).toSceneContent(),
                    sortOrder = (_uiState.value.scenes.maxOfOrNull { it.sortOrder } ?: 0) + 1,
                    linkedTableId = draft.linkedTableId,
                    linkedDeckId = draft.linkedDeckId
                )

                val newSceneId = sessionRepository.upsertScene(newScene)
                focusScene(newSceneId)

                // Registro estructurado en Markdown en el diario
                val journalEntry = buildFramedSceneJournalEntry(sceneCount, draft)
                notesMutex.withLock {
                    val currentNotes = _uiState.value.dmNotes
                    val updatedNotes = if (currentNotes.isBlank()) journalEntry else "$currentNotes\n\n$journalEntry"
                    _uiState.update { it.copy(dmNotes = updatedNotes, showSceneFramingDialog = false) }
                    sessionRepository.updateDmNotes(sessionId, updatedNotes)
                }

                val snackMsg = when (draft.checkOutcome?.result) {
                    SceneCheckResult.INTERRUPTED -> "¡Escena Interrumpida! Evento: ${draft.checkOutcome.eventFocus}"
                    SceneCheckResult.ALTERED -> "Escena Alterada: ${draft.title}"
                    else -> "Escena \"${draft.title}\" iniciada"
                }
                _uiState.update { it.copy(snackbarMessage = snackMsg) }

            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _uiState.update { it.copy(snackbarMessage = "Error al encuadrar escena: ${e.message}", showSceneFramingDialog = false) }
            }
        }
    }
}

