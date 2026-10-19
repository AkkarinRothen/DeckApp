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
import com.deckapp.feature.draw.components.threads.ThreadsOverlayTab
import com.deckapp.feature.draw.components.spotlight.SpotlightCategory
import com.deckapp.feature.draw.components.spotlight.SpotlightItem
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
    val showDeckPicker: Boolean = false,
    val showStarterPackDialog: Boolean = false,
    val availableStarterPacks: List<com.deckapp.feature.reference.StarterPackInfo> = emptyList(),
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
    val mainViewMode: SessionViewMode = SessionViewMode.BOARD,
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
    val isHotbarExpanded: Boolean = false,
    val showHotbarActionDialog: Boolean = false,
    val editingHotbarAction: SessionHotbarAction? = null,
    val showSceneFramingDialog: Boolean = false,
    val isClocksOverlayExpanded: Boolean = false,
    val showClockEditDialog: Boolean = false,
    val editingClock: TensionClock? = null,
    val clockImpactAlert: TensionClock? = null,
    val isThreadsOverlayExpanded: Boolean = false,
    val selectedThreadsTab: ThreadsOverlayTab = ThreadsOverlayTab.THREADS,
    val lastRandomMythicPick: String? = null,
    val showThreadEditDialog: Boolean = false,
    val showMythicCharacterEditDialog: Boolean = false,
    val editingMythicThread: MythicThread? = null,
    val editingMythicCharacter: MythicCharacter? = null,
    val isCombatHudExpanded: Boolean = false,
    val showSpotlightDialog: Boolean = false,
    val spotlightQuery: String = "",
    val selectedSpotlightCategory: SpotlightCategory = SpotlightCategory.ALL,
    val spotlightResults: List<SpotlightItem> = emptyList(),
    val selectedSpotlightItem: SpotlightItem? = null,
    val lastSpotlightRollResult: TableRollResult? = null,
    val isSpotlightSearching: Boolean = false,
    val showChoirGuide: Boolean = false,
    val choirGuideSourcePage: Int? = null,
    val choirGuideTableId: Long? = null,
    val choirGuideError: String? = null,
    val showMicrosettingBuilder: Boolean = false,
    val activeMicrosetting: com.deckapp.core.model.MicrosettingCluster? = null,
    val editingMicrosettingBoardItemId: Long? = null,
    val showStoryEngineGuide: Boolean = false,
    val storyEngineGuideTab: com.deckapp.feature.draw.model.StoryEngineGuideTab = com.deckapp.feature.draw.model.StoryEngineGuideTab.DECK_OF_WORLDS,
    val storyEngineGuideStepIndex: Int = 0,
    val selectedPlaymatCardId: Long? = null,
    val showPlaymatHandTray: Boolean = true
) {
    val activePlaymatSurface: com.deckapp.core.model.PlaymatSurface
        get() {
            val surfaces = boardMemory.playmatSurfaces
            if (surfaces.isEmpty()) return com.deckapp.core.model.PlaymatSurface()
            return surfaces.find { it.id == boardMemory.activePlaymatSurfaceId } ?: surfaces.first()
        }
}

enum class SidebarTool {
    NONE, TABLES, DECKS, COMBAT, NOTES, JOURNAL, MYTHIC_CONTEXT, WIKI, NPCS, PLAN, REFERENCES
}

enum class SessionViewMode {
    BOARD, MAP, TACTICAL, MYTHIC_LOG, PLAYMAT
}

enum class SessionBoardTemplatePreset {
    EXPLORATION, NARRATIVE, COMBAT
}



@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SessionViewModel @Inject constructor(
    private val syncSessionSystemTables: com.deckapp.core.domain.usecase.SyncSessionSystemTablesUseCase,
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
    private val updateChoirCharacterUseCase: com.deckapp.core.domain.usecase.UpdateChoirCharacterUseCase,
    private val updateChoirCombatStateUseCase: com.deckapp.core.domain.usecase.UpdateChoirCombatStateUseCase,
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
    private val fileRepository: com.deckapp.core.domain.repository.FileRepository,
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
        pinnedTableRolls = initialBoardMemory.pinnedTableResults,
        showChoirGuide = initialBoardMemory.choirGuide.anguishChecks.isNotEmpty()
    ))
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

    val choirGuideDelegate = com.deckapp.feature.draw.delegates.ChoirGuideDelegate(
        sessionId, _uiState, viewModelScope, sessionRepository, notesMutex,
        updateMemory = { updateBoardMemory(it) }
    )

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
        fileRepository = fileRepository,
        context = context
    )

    val boardDelegate = com.deckapp.feature.draw.delegates.BoardDelegate(
        sessionId = sessionId,
        _uiState = _uiState,
        scope = viewModelScope,
        sessionRepository = sessionRepository
    )

    val deckDelegate = com.deckapp.feature.draw.delegates.SessionDeckDelegate(
        sessionId = sessionId,
        _uiState = _uiState,
        scope = viewModelScope,
        cardRepository = cardRepository,
        sessionRepository = sessionRepository,
        drawCardUseCase = drawCardUseCase,
        discardCardUseCase = discardCardUseCase,
        resetDeckUseCase = resetDeckUseCase,
        shuffleDeckUseCase = shuffleDeckUseCase,
        updateCardStateUseCase = updateCardStateUseCase,
        undoLastActionUseCase = undoLastActionUseCase,
        getPeekCardId = { peekCardId },
        setPeekCardId = { peekCardId = it }
    )

    val combatDelegate = com.deckapp.feature.draw.delegates.SessionCombatDelegate(
        sessionId = sessionId,
        _uiState = _uiState,
        scope = viewModelScope,
        encounterRepository = encounterRepository,
        sessionRepository = sessionRepository,
        npcRepository = npcRepository,
        applyDamageUseCase = applyDamageUseCase,
        rollInitiativeUseCase = rollInitiativeUseCase,
        calculateInitiativeOrderUseCase = calculateInitiativeOrderUseCase,
        toggleConditionUseCase = toggleConditionUseCase,
        cleanupCombatUseCase = cleanupCombatUseCase,
        updateChoirCharacterUseCase = updateChoirCharacterUseCase,
        updateChoirCombatStateUseCase = updateChoirCombatStateUseCase,
        notesMutex = notesMutex,
        onUpdateNotes = { updateNotes(it) }
    )

    val mythicDelegate = com.deckapp.feature.draw.delegates.SessionMythicDelegate(
        sessionId = sessionId,
        _uiState = _uiState,
        scope = viewModelScope,
        mythicRepository = mythicRepository,
        sessionRepository = sessionRepository,
        rollMythicFateUseCase = rollMythicFateUseCase,
        notesMutex = notesMutex,
        onUpdateNotes = { updateNotes(it) },
        updateBoardMemory = { updateBoardMemory(it) },
        focusScene = { focusScene(it) }
    )

    val spotlightDelegate = com.deckapp.feature.draw.delegates.SessionSpotlightDelegate(
        sessionId = sessionId,
        _uiState = _uiState,
        scope = viewModelScope,
        tableRepository = tableRepository,
        npcRepository = npcRepository,
        referenceRepository = referenceRepository,
        encounterRepository = encounterRepository,
        rollTableUseCase = rollTableUseCase,
        sessionRepository = sessionRepository
    )

    val clocksDelegate = com.deckapp.feature.draw.delegates.SessionClocksDelegate(
        sessionId = sessionId,
        _uiState = _uiState,
        scope = viewModelScope,
        sessionRepository = sessionRepository,
        notesMutex = notesMutex,
        onAddBoardBlock = { type, _, _, resId ->
            boardDelegate.addBoardBlock(type, resId)
        },
        onRollSmartDice = { formula, desc -> rollSmartDiceWithDescription(formula, desc) },
        onRollMythicRandomEvent = { rollBoardMythicRandomEvent() },
        onRollMythicFate = { rollBoardMythicFate(it) },
        onRollTable = { rollActiveTable(it) },
        onDrawCard = { drawCardFromDeck(it) },
        onUpdateBoardMythicChaos = { updateBoardMythicChaos(it) },
        onFocusScene = { focusScene(it) }
    )

    val microsettingDelegate = com.deckapp.feature.draw.delegates.MicrosettingDelegate(
        sessionId = sessionId,
        ui = _uiState,
        scope = viewModelScope,
        cardRepository = cardRepository,
        notesMutex = notesMutex,
        onAppendToNotes = { appendToNotes(it) }
    )

    val storyEngineGuideDelegate = com.deckapp.feature.draw.delegates.StoryEngineGuideDelegate(
        sessionId = sessionId,
        ui = _uiState,
        scope = viewModelScope,
        sessionRepository = sessionRepository,
        deckDelegate = deckDelegate,
        openMicrosettingBuilder = { openMicrosettingBuilder() }
    )

    val playmatDelegate = com.deckapp.feature.draw.delegates.PlaymatDelegate(
        sessionId = sessionId,
        _uiState = _uiState,
        scope = viewModelScope,
        updateBoardMemory = { updateBoardMemory(it) },
        deckDelegate = deckDelegate
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
                        sessionElapsedMinutes = elapsed,
                        tensionClocks = if (session.tensionClocks.isNotEmpty()) session.tensionClocks else it.tensionClocks
                    ) }
                    choirGuideDelegate.retryJournal()
                    choirGuideDelegate.observeCharacters(_uiState.value.npcs)
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

        syncSessionSystemTables(sessionId)
            .catch { _uiState.update { it.copy(snackbarMessage = "No se pudieron cargar las tablas del sistema.") } }
            .launchIn(viewModelScope)

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
                choirGuideDelegate.observeCharacters(npcs)
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
            val nextRight = if (next != SidebarTool.NONE && state.activeRightSidebar != SidebarTool.NONE) SidebarTool.NONE else state.activeRightSidebar
            state.copy(activeLeftSidebar = next, activeRightSidebar = nextRight)
        }
    }

    fun toggleRightSidebar(tool: SidebarTool) {
        _uiState.update { state ->
            val next = if (state.activeRightSidebar == tool) SidebarTool.NONE else tool
            val nextLeft = if (next != SidebarTool.NONE && state.activeLeftSidebar != SidebarTool.NONE) SidebarTool.NONE else state.activeLeftSidebar
            state.copy(activeRightSidebar = next, activeLeftSidebar = nextLeft)
        }
    }

    fun setMainViewMode(mode: SessionViewMode) {
        if (mode == SessionViewMode.PLAYMAT) {
            playmatDelegate.ensureInitialSurface()
        }
        _uiState.update { it.copy(mainViewMode = mode) }
    }

    // --- Mythic Unified Actions ---
    
    fun performMythicFateCheck(question: String, prob: ProbabilityLevel) =
        mythicDelegate.performMythicFateCheck(question, prob)

    fun quickMythicCheck() = mythicDelegate.quickMythicCheck()

    fun updateMythicChaos(delta: Int) = mythicDelegate.updateMythicChaos(delta)

    fun setBoardMythicProbability(prob: ProbabilityLevel) = mythicDelegate.setBoardMythicProbability(prob)

    fun updateBoardMythicChaos(delta: Int) = mythicDelegate.updateBoardMythicChaos(delta)

    fun rollBoardMythicFate(
        probability: ProbabilityLevel = _uiState.value.boardMythicProbability,
        forcedRoll: Int? = null,
        question: String = ""
    ) = mythicDelegate.rollBoardMythicFate(probability, forcedRoll, question)

    fun rollBoardMythicRandomEvent(): String = mythicDelegate.rollBoardMythicRandomEvent()

    fun appendToNotes(textToAppend: String) = mythicDelegate.appendToNotes(textToAppend)

    fun activeSoloScene(): Scene? = mythicDelegate.activeSoloScene()

    fun saveSoloScene(scene: Scene, onSuccess: () -> Unit) =
        mythicDelegate.saveSoloScene(scene, onSuccess)

    fun closeSoloScene(scene: Scene, closure: SoloSceneClosure, onSuccess: () -> Unit) =
        mythicDelegate.closeSoloScene(scene, closure, onSuccess)

    // --- Threats & Threads Tracker (Sprint 62) ---

    suspend fun ensureLinkedMythicSessionId(): Long = mythicDelegate.ensureLinkedMythicSessionId()

    fun toggleThreadsOverlayExpanded() = mythicDelegate.toggleThreadsOverlayExpanded()

    fun selectThreadsTab(tab: ThreadsOverlayTab) = mythicDelegate.selectThreadsTab(tab)

    fun openCreateThreadDialog() = mythicDelegate.openCreateThreadDialog()

    fun openEditThreadDialog(thread: MythicThread) = mythicDelegate.openEditThreadDialog(thread)

    fun closeThreadDialog() = mythicDelegate.closeThreadDialog()

    fun openCreateCharacterDialog() = mythicDelegate.openCreateCharacterDialog()

    fun openEditCharacterDialog(character: MythicCharacter) = mythicDelegate.openEditCharacterDialog(character)

    fun closeCharacterDialog() = mythicDelegate.closeCharacterDialog()

    fun saveMythicThread(id: Long, description: String) = mythicDelegate.saveMythicThread(id, description)

    fun saveMythicCharacter(id: Long, name: String, notes: String) = mythicDelegate.saveMythicCharacter(id, name, notes)

    fun rollRandomThreadOrCharacter() = mythicDelegate.rollRandomThreadOrCharacter()

    fun clearRandomMythicPick() = mythicDelegate.clearRandomMythicPick()

    fun copyLastRandomPickToDiary() = mythicDelegate.copyLastRandomPickToDiary()

    fun copyThreadsSummaryToDiary() = mythicDelegate.copyThreadsSummaryToDiary()

    fun addMythicCharacter(name: String) = mythicDelegate.addMythicCharacter(name)

    fun deleteMythicCharacter(id: Long) = mythicDelegate.deleteMythicCharacter(id)

    fun updateMythicCharacterNotes(id: Long, notes: String) = mythicDelegate.updateMythicCharacterNotes(id, notes)

    fun sendMythicCharacterToNotes(character: MythicCharacter) = mythicDelegate.sendMythicCharacterToNotes(character)

    fun addMythicThread(description: String) = mythicDelegate.addMythicThread(description)

    fun updateMythicThreadDescription(id: Long, description: String) = mythicDelegate.updateMythicThreadDescription(id, description)

    fun toggleMythicThreadStatus(id: Long, isResolved: Boolean) = mythicDelegate.toggleMythicThreadStatus(id, isResolved)

    fun deleteMythicThread(id: Long) = mythicDelegate.deleteMythicThread(id)

    fun sendMythicThreadToNotes(thread: MythicThread) = mythicDelegate.sendMythicThreadToNotes(thread)

    fun sendRawToNotes(text: String) = mythicDelegate.sendRawToNotes(text)

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

    fun captureBoardInspiration(item: BoardInspiration) = mythicDelegate.captureBoardInspiration(item)

    fun setBoardInspirationStatus(id: String, status: InspirationStatus) = mythicDelegate.setBoardInspirationStatus(id, status)

    fun sendBoardInspirationToDiary(id: String) = mythicDelegate.sendBoardInspirationToDiary(id)

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

    fun openDeckPicker() = _uiState.update { it.copy(showDeckPicker = true) }
    fun closeDeckPicker() = _uiState.update { it.copy(showDeckPicker = false) }

    private var wasDeckPickerOpenBeforeStarterPack = false

    fun openStarterPackDialog() {
        wasDeckPickerOpenBeforeStarterPack = _uiState.value.showDeckPicker
        viewModelScope.launch {
            try {
                val availableAssets = context.assets.list("starter_packs") ?: emptyArray()
                val installed = referenceRepository.getInstalledPackNames().first()
                val packInfos = availableAssets.filter { it.endsWith(".json") }.map { fileName ->
                    val displayName = when (fileName) {
                        "choir_of_flesh.json" -> "Choir of Flesh (El Coro de la Carne)"
                        "story_engine_deck_of_worlds.json" -> "The Story Engine: Deck of Worlds"
                        "story_engine_deck_of_worlds_expansions.json" -> "The Story Engine: Deck of Worlds (Expansiones)"
                        "new_story_engine.json" -> "The Story Engine: New Story Engine (Expansiones)"
                        "dnd_5e_2024.json" -> "D&D 5e (2024)"
                        "twilight_2000_4e.json" -> "Twilight 2000 4e"
                        else -> fileName.removeSuffix(".json").replace("_", " ").split(" ")
                            .joinToString(" ") { it.replaceFirstChar { c -> c.titlecase() } }
                    }
                    com.deckapp.feature.reference.StarterPackInfo(
                        assetName = fileName,
                        displayName = displayName,
                        isInstalled = installed.contains(fileName)
                    )
                }
                _uiState.update { it.copy(availableStarterPacks = packInfos, showStarterPackDialog = true, showDeckPicker = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(snackbarMessage = "Error al cargar packs: ${e.message}") }
            }
        }
    }

    fun closeStarterPackDialog() {
        val shouldRestoreDeckPicker = wasDeckPickerOpenBeforeStarterPack
        wasDeckPickerOpenBeforeStarterPack = false
        _uiState.update { it.copy(showStarterPackDialog = false, showDeckPicker = shouldRestoreDeckPicker) }
    }

    fun installStarterPack(assetName: String) {
        viewModelScope.launch {
            try {
                val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                val jsonString = context.assets.open("starter_packs/$assetName").bufferedReader().use { it.readText() }.trimStart('\uFEFF')
                val pack = json.decodeFromString<com.deckapp.core.model.backup.FullBackupDto>(jsonString)
                referenceRepository.importStarterPack(pack, assetName)
                openStarterPackDialog()
                _uiState.update { it.copy(snackbarMessage = "Pack '$assetName' instalado con éxito") }
            } catch (e: Exception) {
                _uiState.update { it.copy(snackbarMessage = "Error al instalar: ${e.message}") }
            }
        }
    }

    fun removeStarterPack(assetName: String) {
        viewModelScope.launch {
            try {
                referenceRepository.removeStarterPack(assetName)
                openStarterPackDialog()
                _uiState.update { it.copy(snackbarMessage = "Pack eliminado") }
            } catch (e: Exception) {
                _uiState.update { it.copy(snackbarMessage = "Error al eliminar: ${e.message}") }
            }
        }
    }

    fun toggleDeckInSession(deckId: Long) = deckDelegate.toggleDeckInSession(deckId)

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
    fun toggleDeckCollapse(stackId: Long) = deckDelegate.toggleDeckCollapse(stackId)

    /**
     * Registra el último cluster con el que interactuó el DM.
     * El FAB usa esta información para saber de qué mazo robar.
     */
    fun setLastInteractedDeck(stackId: Long) = deckDelegate.setLastInteractedDeck(stackId)

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
            recordTableResult(result, manual = false)
        }
    }

    fun selectTableEntry(tableId: Long, entryIndex: Int, dieValue: Int?) {
        viewModelScope.launch {
            val result = rollTableUseCase.selectEntry(tableId, entryIndex, sessionId, dieValue)
            if (result == null) {
                _uiState.update { it.copy(snackbarMessage = "La entrada ya no está disponible") }
            } else recordTableResult(result, manual = true)
        }
    }

    private fun recordTableResult(result: TableRollResult, manual: Boolean) {
        val method = if (manual) "elección manual" else "tirada"
        val time = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
        val entry = "[$time] Tabla ${result.tableName} ($method, ${result.rollValue}): ${result.resolvedText}"
        val notes = _uiState.value.dmNotes.let { if (it.isBlank()) entry else "$it\n$entry" }
        updateNotes(notes)
        _uiState.update { it.copy(
            recentTableRolls = (listOf(result) + it.recentTableRolls).take(30),
            snackbarMessage = "${result.tableName}: ${result.resolvedText}"
        ) }
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
    fun updateNotes(editedNotes: String) {
        val notes = ChoirGuideNotes.preserveReceipts(editedNotes, _uiState.value.dmNotes)
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

    // --- Decks & Cards Operations (Delegated to SessionDeckDelegate) ---

    fun drawCardFromDeck(stackId: Long) = deckDelegate.drawCardFromDeck(stackId)
    fun drawCard() = deckDelegate.drawCard()
    fun discardCard(cardId: Long) = deckDelegate.discardCard(cardId)
    fun shufflePileBack(stackId: Long? = null) = deckDelegate.shufflePileBack(stackId)
    fun openDrawBySuit() = deckDelegate.openDrawBySuit()
    fun drawCardBySuit(suit: String) = deckDelegate.drawCardBySuit(suit)
    fun closeDrawBySuitSheet() = deckDelegate.closeDrawBySuitSheet()
    fun returnToHand(cardId: Long) = deckDelegate.returnToHand(cardId)
    fun returnToDeck(cardId: Long) = deckDelegate.returnToDeck(cardId)
    fun resetDeck() = deckDelegate.resetDeck()
    fun resetSingleDeck(stackId: Long) = deckDelegate.resetSingleDeck(stackId)
    fun undo() = deckDelegate.undo()
    fun undoLastDraw() = deckDelegate.undoLastDraw()
    fun endSession() {
        viewModelScope.launch {
            sessionRepository.endSession(sessionId)
            _uiState.update { it.copy(sessionEnded = true) }
        }
    }
    fun toggleNightMode() = deckDelegate.toggleNightMode()
    fun toggleCardTitles() = deckDelegate.toggleCardTitles()
    fun sortByValue() = deckDelegate.sortByValue()
    fun peekTopCard() = deckDelegate.peekTopCard()
    fun clearPeek() = deckDelegate.clearPeek()
    fun revealCard(cardId: Long) = deckDelegate.revealCard(cardId)
    fun rotateCard(cardId: Long) = deckDelegate.rotateCard(cardId)
    fun dealCards(count: Int) = deckDelegate.dealCards(count)
    fun clearSnackbar() = deckDelegate.clearSnackbar()
    fun clearSnackbarMessage() = deckDelegate.clearSnackbar()
    fun clearError() = deckDelegate.clearError()
    fun clearErrorMessage() = deckDelegate.clearError()
    fun saveCheckpointOnPause() {
        // Estado reactivo y persistido en Room
    }

    fun openGlobalSearch() = spotlightDelegate.openGlobalSearch()

    // --- Spotlight Command Palette (Sprint 64) ---

    fun openSpotlight() = spotlightDelegate.openSpotlight()

    fun closeSpotlight() = spotlightDelegate.closeSpotlight()

    fun updateSpotlightQuery(query: String) = spotlightDelegate.updateSpotlightQuery(query)

    fun selectSpotlightCategory(category: SpotlightCategory) = spotlightDelegate.selectSpotlightCategory(category)

    fun selectSpotlightItem(item: SpotlightItem) = spotlightDelegate.selectSpotlightItem(item)

    fun rollSpotlightTable(tableId: Long) = spotlightDelegate.rollSpotlightTable(tableId)

    fun addSpotlightCreatureToCombat(
        name: String,
        hp: Int,
        ac: Int,
        notes: String,
        npcId: Long? = null,
        imagePath: String? = null,
        initiativeBonus: Int = 0
    ) = spotlightDelegate.addSpotlightCreatureToCombat(name, hp, ac, notes, npcId, imagePath, initiativeBonus)

    fun addSpotlightNpcToCombat(npcId: Long) = spotlightDelegate.addSpotlightNpcToCombat(npcId)

    fun copySpotlightToDiary(text: String) = spotlightDelegate.copySpotlightToDiary(text)

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

    // --- Combat & Encounter Actions (Delegated to SessionCombatDelegate) ---

    fun updateChoirCharacter(creatureId: Long, command: com.deckapp.core.model.ChoirCharacterCommand) =
        combatDelegate.updateChoirCharacter(creatureId, command)

    fun applyChoirCombatAftermath(
        creatureId: Long,
        request: ChoirAftermathRequest,
        onComplete: (ChoirAftermathResult?) -> Unit = {}
    ) = combatDelegate.applyChoirCombatAftermath(creatureId, request, onComplete)

    fun getChoirIncursion(): ChoirIncursion = combatDelegate.getChoirIncursion()

    fun getChoirSettlement(): ChoirSettlement = combatDelegate.getChoirSettlement()

    fun saveChoirIncursion(
        incursion: ChoirIncursion,
        settlement: ChoirSettlement,
        character: ChoirCharacter? = null,
        characterCreatureId: Long? = null
    ) = combatDelegate.saveChoirIncursion(incursion, settlement, character, characterCreatureId)

    fun updateChoirCombatState(creatureId: Long, command: com.deckapp.core.model.ChoirCombatCommand) =
        combatDelegate.updateChoirCombatState(creatureId, command)

    fun applyDamage(creatureId: Long, delta: Int) = combatDelegate.applyDamage(creatureId, delta)

    fun toggleCombatHudExpanded() = combatDelegate.toggleCombatHudExpanded()

    fun copyCombatRoundToDiary() = combatDelegate.copyCombatRoundToDiary()

    fun nextTurn() = combatDelegate.nextTurn()

    fun startCombatProcess() = combatDelegate.startCombatProcess()

    fun toggleCondition(creatureId: Long, condition: Condition) = combatDelegate.toggleCondition(creatureId, condition)

    fun removeCreature(creatureId: Long) = combatDelegate.removeCreature(creatureId)

    fun addPlayerParticipant(name: String, initiative: Int) = combatDelegate.addPlayerParticipant(name, initiative)

    fun removePlayerParticipant(id: String) = combatDelegate.removePlayerParticipant(id)

    fun endEncounter() = combatDelegate.endEncounter()

    suspend fun getSessionLogText(): String = combatDelegate.getSessionLogText()

    fun exportLog(uri: android.net.Uri, contentResolver: android.content.ContentResolver) =
        combatDelegate.exportLog(uri, contentResolver)
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

    // --- Tension Clocks & Overlay (Sprint 61) ---

    fun toggleClocksOverlayExpanded() = clocksDelegate.toggleClocksOverlayExpanded()
    fun openCreateClockDialog() = clocksDelegate.openCreateClockDialog()
    fun openEditClockDialog(clock: TensionClock) = clocksDelegate.openEditClockDialog(clock)
    fun closeClockEditDialog() = clocksDelegate.closeClockEditDialog()
    fun dismissClockImpactAlert() = clocksDelegate.dismissClockImpactAlert()
    fun saveClock(clock: TensionClock) = clocksDelegate.saveClock(clock)
    fun tickClock(clockId: Long, amount: Int) = clocksDelegate.tickClock(clockId, amount)
    fun resetClock(clockId: Long) = clocksDelegate.resetClock(clockId)
    fun deleteClock(clockId: Long) = clocksDelegate.deleteClock(clockId)
    fun triggerClockNow(clock: TensionClock) = clocksDelegate.triggerClockNow(clock)
    fun copyClockImpactToDiary(clock: TensionClock) = clocksDelegate.copyClockImpactToDiary(clock)
    fun updateClockDescriptionAndProgress(clockId: Long, newDescription: String, newProgress: Int) =
        clocksDelegate.updateClockDescriptionAndProgress(clockId, newDescription, newProgress)
    fun createClockAndAddToBoard(
        name: String,
        segments: Int,
        colorHex: String?,
        triggerDescription: String,
        targetGroupId: Long? = null
    ) = clocksDelegate.createClockAndAddToBoard(name, segments, colorHex, triggerDescription, targetGroupId)

    fun renameSession(name: String) {
        viewModelScope.launch {
            _uiState.value.session?.let { current ->
                sessionRepository.renameSession(current.id, name)
            }
        }
    }

    fun createAndLinkMythicSession() = mythicDelegate.createAndLinkMythicSession()

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

    fun startQuickCombat(name: String = "Combate Rápido") {
        viewModelScope.launch {
            val encounter = Encounter(
                name = name,
                linkedSessionId = sessionId,
                isActive = true
            )
            encounterRepository.saveEncounter(encounter)
            setActiveBoardView(SessionBoardView.COMBAT)
        }
    }

    // --- NPCs & Wiki Updates ---

    fun updateNpcHp(npcId: Long, hp: Int) = combatDelegate.updateNpcHp(npcId, hp)

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

    fun shuffleDeckOnly(stackId: Long) = deckDelegate.shuffleDeckOnly(stackId)

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

    fun toggleHotbarExpanded() = clocksDelegate.toggleHotbarExpanded()

    fun openCreateHotbarAction() = clocksDelegate.openCreateHotbarAction()

    fun openEditHotbarAction(action: SessionHotbarAction) = clocksDelegate.openEditHotbarAction(action)

    fun dismissHotbarActionDialog() = clocksDelegate.dismissHotbarActionDialog()

    fun saveHotbarAction(action: SessionHotbarAction) = clocksDelegate.saveHotbarAction(action)

    fun deleteHotbarAction(actionId: String) = clocksDelegate.deleteHotbarAction(actionId)

    fun executeHotbarAction(action: SessionHotbarAction) = clocksDelegate.executeHotbarAction(action)

    // --- Scene Framing Assistant (Sprint 60) ---

    fun openSceneFraming() = clocksDelegate.openSceneFraming()

    fun dismissSceneFraming() = clocksDelegate.dismissSceneFraming()

    fun confirmSceneFraming(
        draft: SceneFramingDraft,
        closePrevious: Boolean,
        prevSummary: String,
        chaosDelta: Int
    ) = clocksDelegate.confirmSceneFraming(draft, closePrevious, prevSummary, chaosDelta)

    // --- Choir of Flesh (Sprint 65) ---
    fun createChoirOfFleshCharacter(npc: Npc, sheetMarkdown: String) =
        combatDelegate.createChoirOfFleshCharacter(npc, sheetMarkdown)

    fun addNpcToCombat(npc: Npc) = combatDelegate.addNpcToCombat(npc)

    fun logNpcToDiary(npc: Npc) = combatDelegate.logNpcToDiary(npc)

    // --- The Story Engine: Deck of Worlds (Microsettings) ---

    fun openMicrosettingBuilder(item: SessionBoardItem? = null) = microsettingDelegate.openBuilder(item)

    fun closeMicrosettingBuilder() = microsettingDelegate.closeBuilder()

    fun generateMicrosetting(targetItemId: Long? = null) = microsettingDelegate.generateCluster(targetItemId)

    fun rotateMicrosettingSlot(slotType: com.deckapp.core.model.MicrosettingSlotType, targetItemId: Long? = null) =
        microsettingDelegate.rotateSlot(slotType, targetItemId)

    fun setMicrosettingSlotRotation(slotType: com.deckapp.core.model.MicrosettingSlotType, rotation: Int, targetItemId: Long? = null) =
        microsettingDelegate.setSlotRotation(slotType, rotation, targetItemId)

    fun redrawMicrosettingSlot(slotType: com.deckapp.core.model.MicrosettingSlotType, targetItemId: Long? = null) =
        microsettingDelegate.redrawSlot(slotType, targetItemId)

    fun copyMicrosettingToNotes(cluster: com.deckapp.core.model.MicrosettingCluster? = null) =
        microsettingDelegate.copyToNotes(cluster)

    fun saveMicrosettingToBoard(cluster: com.deckapp.core.model.MicrosettingCluster? = null, targetItemId: Long? = null) =
        microsettingDelegate.saveToBoard(cluster, targetItemId) { type, resId ->
            boardDelegate.addBoardBlock(type, resId)
        }
}

