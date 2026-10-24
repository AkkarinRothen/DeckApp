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
    val decks: com.deckapp.feature.draw.model.state.DecksUiState = com.deckapp.feature.draw.model.state.DecksUiState(),
    val isLoading: Boolean = true,
    val canUndo: Boolean = false,
    val snackbarMessage: String? = null,
    val nightMode: Boolean = false,
    val errorMessage: String? = null,
    val sessionEnded: Boolean = false,
    val activeTab: Int = 0,                  // 0=Mazos, 1=Pilas, 2=Tablas, 3=Ref, 4=Notas, 5=Combate
    val dmNotes: String = "",
    val sessionElapsedMinutes: Long = 0,
    // Sprint 17 — Resource Management
    val resources: com.deckapp.feature.draw.model.state.ResourcesSessionUiState = com.deckapp.feature.draw.model.state.ResourcesSessionUiState(),
    // Sprint 15 — UX de Notas
    val isSavingNotes: Boolean = false,
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
    val npc: com.deckapp.feature.draw.model.state.NpcSessionUiState = com.deckapp.feature.draw.model.state.NpcSessionUiState(),
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
    val savedBoardTemplates: List<SavedBoardTemplate> = emptyList(),
    val tensionClocks: List<TensionClock> = emptyList(),
    val allHexMaps: List<HexMap> = emptyList(),
    val recentDiceRolls: List<DiceRollResult> = emptyList(),
    val recentTableRolls: List<TableRollResult> = emptyList(),
    val pinnedTableRolls: Map<Long, TableRollResult> = emptyMap(),
    // Sprint 56 — Bento Solo Journaling & Interactive Mythic Oracle
    val soloMythic: com.deckapp.feature.draw.model.state.SoloMythicUiState = com.deckapp.feature.draw.model.state.SoloMythicUiState(),
    val boardMemory: BoardSessionMemory = BoardSessionMemory(),
    val isHotbarExpanded: Boolean = false,
    val isClocksOverlayExpanded: Boolean = false,
    val isThreadsOverlayExpanded: Boolean = false,
    val selectedThreadsTab: ThreadsOverlayTab = ThreadsOverlayTab.THREADS,
    val combat: com.deckapp.feature.draw.model.state.CombatSessionUiState = com.deckapp.feature.draw.model.state.CombatSessionUiState(),
    val spotlight: com.deckapp.feature.draw.model.state.SpotlightUiState = com.deckapp.feature.draw.model.state.SpotlightUiState(),
    val guides: com.deckapp.feature.draw.model.state.GuidesSessionUiState = com.deckapp.feature.draw.model.state.GuidesSessionUiState(),
    val modals: com.deckapp.feature.draw.model.state.SessionModalsUiState = com.deckapp.feature.draw.model.state.SessionModalsUiState(),
    val playmat: com.deckapp.feature.draw.model.state.PlaymatUiState = com.deckapp.feature.draw.model.state.PlaymatUiState(),
    val sessionLog: com.deckapp.feature.draw.model.state.SessionLogUiState = com.deckapp.feature.draw.model.state.SessionLogUiState()
) {
    val spreadResultsByDeck: Map<Long, CardSpreadResult>
        get() = boardMemory.tarokkaCardsByDeck.mapNotNull { (deckId, ids) ->
            val byId = (decks.handByDeck[deckId].orEmpty() + decks.pile.filter { it.stackId == deckId })
                .associateBy { it.id }
            val cards = ids.mapNotNull(byId::get)
            if (cards.isEmpty()) null else deckId to CardSpreadResult(CardSpread.TAROKKA_CROSS, cards)
        }.toMap()
    // Retrocompatibilidad con accesores planos delegados a playmat
    val selectedPlaymatCardId: Long? get() = playmat.selectedCardId
    val showPlaymatHandTray: Boolean get() = playmat.showHandTray
    val isZenMode: Boolean get() = playmat.isZenMode
    val isSessionDrawerOpen: Boolean get() = playmat.isSessionDrawerOpen
    val sessionDrawerTab: Int get() = playmat.sessionDrawerTab

    // Retrocompatibilidad con accesores planos delegados a decks
    val deckRefs: List<SessionDeckRef> get() = decks.deckRefs
    val deckNames: Map<Long, String> get() = decks.deckNames
    val deckCardCounts: Map<Long, Int> get() = decks.deckCardCounts
    val selectedDeckId: Long? get() = decks.selectedDeckId
    val hand: List<Card> get() = decks.hand
    val pile: List<Card> get() = decks.pile
    val peekCard: Card? get() = decks.peekCard
    val deckAspectRatios: Map<Long, CardAspectRatio> get() = decks.deckAspectRatios
    val deckBackImages: Map<Long, String?> get() = decks.deckBackImages
    val collapsedDeckIds: Set<Long> get() = decks.collapsedDeckIds
    val handByDeck: Map<Long, List<Card>> get() = decks.handByDeck
    val allDecks: List<CardStack> get() = decks.allDecks
    val isShuffling: Boolean get() = decks.isShuffling
    val showDrawBySuitSheet: Boolean get() = decks.showDrawBySuitSheet
    val availableSuitsForDraw: List<String> get() = decks.availableSuitsForDraw

    // Retrocompatibilidad con accesores planos delegados a soloMythic
    val boardMythicChaos: Int get() = soloMythic.chaos
    val boardMythicProbability: com.deckapp.core.model.ProbabilityLevel get() = soloMythic.probability
    val boardMythicLastResult: com.deckapp.core.model.BoardMythicFateResult? get() = soloMythic.lastResult
    val boardMythicHistory: List<com.deckapp.core.model.BoardMythicFateResult> get() = soloMythic.history
    val boardMythicLastEvent: String? get() = soloMythic.lastEvent
    val isSoloSceneBusy: Boolean get() = soloMythic.isSoloSceneBusy
    val isSendingInspiration: Boolean get() = soloMythic.isSendingInspiration
    val lastRandomMythicPick: String? get() = soloMythic.lastRandomMythicPick


    // Retrocompatibilidad con accesores planos delegados a modals
    val showQuickNoteDialog: Boolean get() = modals.showQuickNoteDialog
    val showResourceManager: Boolean get() = modals.showResourceManager
    val showSessionConfig: Boolean get() = modals.showSessionConfig
    val showDeckPicker: Boolean get() = modals.showDeckPicker
    val showStarterPackDialog: Boolean get() = modals.showStarterPackDialog
    val showQuickSearch: Boolean get() = modals.showQuickSearch
    val showHotbarActionDialog: Boolean get() = modals.showHotbarActionDialog
    val editingHotbarAction: com.deckapp.core.model.SessionHotbarAction? get() = modals.editingHotbarAction
    val showSceneFramingDialog: Boolean get() = modals.showSceneFramingDialog
    val showClockEditDialog: Boolean get() = modals.showClockEditDialog
    val editingClock: com.deckapp.core.model.TensionClock? get() = modals.editingClock
    val clockImpactAlert: com.deckapp.core.model.TensionClock? get() = modals.clockImpactAlert
    val showThreadEditDialog: Boolean get() = modals.showThreadEditDialog
    val showMythicCharacterEditDialog: Boolean get() = modals.showMythicCharacterEditDialog
    val editingMythicThread: com.deckapp.core.model.MythicThread? get() = modals.editingMythicThread
    val editingMythicCharacter: com.deckapp.core.model.MythicCharacter? get() = modals.editingMythicCharacter
    val showChoirGuide: Boolean get() = modals.showChoirGuide
    val showMicrosettingBuilder: Boolean get() = modals.showMicrosettingBuilder
    val showStoryEngineGuide: Boolean get() = modals.showStoryEngineGuide

    // Retrocompatibilidad con accesores planos delegados a spotlight
    val showSpotlightDialog: Boolean get() = spotlight.showDialog
    val spotlightQuery: String get() = spotlight.query
    val selectedSpotlightCategory: SpotlightCategory get() = spotlight.selectedCategory
    val spotlightResults: List<SpotlightItem> get() = spotlight.results
    val selectedSpotlightItem: SpotlightItem? get() = spotlight.selectedItem
    val lastSpotlightRollResult: TableRollResult? get() = spotlight.lastRollResult
    val isSpotlightSearching: Boolean get() = spotlight.isSearching

    // Retrocompatibilidad con accesores planos delegados a combat
    val hasActiveEncounter: Boolean get() = combat.hasActiveEncounter
    val activeEncounter: Encounter? get() = combat.activeEncounter
    val combatLog: List<CombatLogEntry> get() = combat.combatLog
    val playerParticipants: List<PlayerInitiativeParticipant> get() = combat.playerParticipants
    val isCombatHudExpanded: Boolean get() = combat.isCombatHudExpanded

    // Retrocompatibilidad con accesores planos delegados a guides
    val choirGuideSourcePage: Int? get() = guides.choirGuideSourcePage
    val choirGuideTableId: Long? get() = guides.choirGuideTableId
    val choirGuideError: String? get() = guides.choirGuideError
    val activeMicrosetting: com.deckapp.core.model.MicrosettingCluster? get() = guides.activeMicrosetting
    val editingMicrosettingBoardItemId: Long? get() = guides.editingMicrosettingBoardItemId
    val storyEngineGuideTab: com.deckapp.feature.draw.model.StoryEngineGuideTab get() = guides.storyEngineGuideTab
    val storyEngineGuideStepIndex: Int get() = guides.storyEngineGuideStepIndex

    // Retrocompatibilidad con accesores planos delegados a resources
    val allTables: List<RandomTable> get() = resources.allTables
    val tablesInSession: List<RandomTable> get() = resources.tablesInSession
    val availableStarterPacks: List<com.deckapp.feature.reference.StarterPackInfo> get() = resources.availableStarterPacks
    val availableSystems: List<String> get() = resources.availableSystems

    // Retrocompatibilidad con accesores planos delegados a npc
    val npcs: List<Npc> get() = npc.npcs
    val playingNpcVoicePath: String? get() = npc.playingNpcVoicePath
    val npcCount: Int get() = npc.npcCount
    val permanentPlayers: List<Npc> get() = npc.permanentPlayers




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
    private val sessionLogRepository: com.deckapp.core.domain.repository.SessionLogRepository,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val voicePlayer by lazy { com.deckapp.feature.npcs.AudioPlayer(context) }

    private val sessionId: Long = checkNotNull(savedStateHandle["sessionId"])
    private val notesMutex = Mutex()
    private val tarokkaMutex = Mutex()
    private var peekCardId: Long?
        get() = savedStateHandle.get<Long>("peekCardId")
        set(value) { savedStateHandle["peekCardId"] = value }

    private val initialBoardMemory = settingsRepository.getBoardSessionMemory(sessionId)
    private val _uiState = MutableStateFlow(SessionUiState(
        sessionId = sessionId, boardMemory = initialBoardMemory,
        fullBoardItems = initialBoardMemory.boardItems,
        sessionBoardItems = initialBoardMemory.boardItems.filter { it.boardView == SessionBoardView.MAIN },
        boardGroups = initialBoardMemory.boardGroups,
        savedBoardTemplates = settingsRepository.getSavedBoardTemplates(),
        activeBoardView = SessionBoardView.MAIN,
        focusedSceneId = initialBoardMemory.focusedSceneId,
        pinnedTableRolls = initialBoardMemory.pinnedTableResults,
        modals = com.deckapp.feature.draw.model.state.SessionModalsUiState(
            showChoirGuide = initialBoardMemory.choirGuide.anguishChecks.isNotEmpty()
        )
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
        sessionRepository = sessionRepository,
        updateBoardMemory = { updateBoardMemory(it) }
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

    val sessionLogDelegate = com.deckapp.feature.draw.delegates.SessionLogDelegate(
        sessionId = sessionId,
        _uiState = _uiState,
        scope = viewModelScope,
        sessionLogRepository = sessionLogRepository
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
        focusScene = { focusScene(it) },
        onLogOracle = { question, answer, chaos ->
            sessionLogDelegate.logOracleRoll(question, answer, chaos)
        }
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

    val playmatDelegate = com.deckapp.feature.draw.delegates.PlaymatDelegate(
        sessionId = sessionId,
        _uiState = _uiState,
        scope = viewModelScope,
        updateBoardMemory = { updateBoardMemory(it) },
        deckDelegate = deckDelegate,
        onLogTokenHp = { name, delta, newHp, maxHp ->
            sessionLogDelegate.logTokenHpChange(name, delta, newHp, maxHp)
        },
        onLogClockProgress = { title, cur, max ->
            sessionLogDelegate.logClockStep(title, cur, max)
        },
        onLogCardAction = { title, action ->
            sessionLogDelegate.logCardAction(title, action)
        }
    )

    val storyEngineGuideDelegate = com.deckapp.feature.draw.delegates.StoryEngineGuideDelegate(
        sessionId = sessionId,
        ui = _uiState,
        scope = viewModelScope,
        sessionRepository = sessionRepository,
        deckDelegate = deckDelegate,
        openMicrosettingBuilder = { openMicrosettingBuilder() },
        onCardDrawnForStep = { tab, stepIndex, cardId ->
            playmatDelegate.placeStepCardOnPlaymat(tab, stepIndex, cardId)
        }
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
                    _uiState.update { it.copy(decks = it.decks.copy(peekCard = card)) }
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
                        decks = state.decks.copy(
                            deckRefs = refs,
                            deckNames = names,
                            deckAspectRatios = ratios,
                            deckBackImages = backImages,
                            selectedDeckId = state.selectedDeckId ?: refs.firstOrNull()?.stackId
                        )
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
                    _uiState.update { it.copy(decks = it.decks.copy(deckCardCounts = counts), isLoading = false) }
                }
        }

        // Mano: cartas con isDrawn = true + agrupación por mazo para el Workspace
        viewModelScope.launch {
            cardRepository.getDrawnCards()
                .collect { drawn ->
                    _uiState.update {
                        it.copy(
                            decks = it.decks.copy(
                                hand = drawn,
                                handByDeck = drawn.groupBy { card -> card.stackId }
                            )
                        )
                    }
                }
        }

        // Pila de descarte
        viewModelScope.launch {
            cardRepository.getPiledCards(sessionId)
                .collect { pile -> _uiState.update { it.copy(decks = it.decks.copy(pile = pile)) } }
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
                        combat = it.combat.copy(
                            activeEncounter = encounter,
                            hasActiveEncounter = encounter != null,
                            combatLog = log
                        )
                    ) }
                }
        }

        viewModelScope.launch {
            encounterRepository.getAllEncounters().collect { encounters ->
                _uiState.update { it.copy(
                    combat = it.combat.copy(
                        availableEncounters = encounters.filter { enc -> !enc.isActive }
                    )
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
                    _uiState.update { it.copy(resources = it.resources.copy(tablesInSession = tables)) }
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
                    decks = it.decks.copy(allDecks = decks),
                    resources = it.resources.copy(
                        allTables = tables,
                        availableSystems = systems
                    )
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
                _uiState.update { it.copy(npc = it.npc.copy(npcs = npcs, npcCount = npcs.size)) }
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
        _uiState.update { it.copy(decks = it.decks.copy(selectedDeckId = stackId)) }
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
        sessionLogDelegate.onSceneChanged(sceneId)
    }

    private fun updateBoardMemory(transform: (BoardSessionMemory) -> BoardSessionMemory) {
        val memory = transform(_uiState.value.boardMemory)
        val normalizedMemory = if (memory.activityTrackingStartedAt == 0L) memory.copy(activityTrackingStartedAt = System.currentTimeMillis()) else memory
        settingsRepository.setBoardSessionMemory(sessionId, normalizedMemory)
        _uiState.update { it.copy(boardMemory = normalizedMemory, focusedSceneId = normalizedMemory.focusedSceneId,
            pinnedTableRolls = normalizedMemory.pinnedTableResults) }
    }

    fun setWorkspaceLayout(view: SessionBoardView, layout: BoardViewMemory) {
        updateBoardMemory { it.copy(views = it.views + (view.name to layout)) }
    }

    fun markBoardBlockUsed(itemId: Long) {
        updateBoardMemory { it.copy(blockLastUsedAt = it.blockLastUsedAt + (itemId to System.currentTimeMillis())) }
    }

    fun setBoardPlayMode(mode: BoardPlayMode) {
        updateBoardMemory { it.copy(boardMode = mode) }
    }

    fun undoBoardChange() = boardDelegate.undoBoardChange()

    fun moveBoardGroup(groupId: Long, direction: Int) = boardDelegate.moveBoardGroup(groupId, direction)

    fun saveCurrentBoardTemplate(name: String) {
        val trimmed = name.trim().take(48)
        if (trimmed.isEmpty()) return
        val items = _uiState.value.sessionBoardItems.sortedBy { it.position }
        val groups = _uiState.value.boardGroups.filter { it.boardView == _uiState.value.activeBoardView }.sortedBy { it.position }
        val groupIndexes = groups.mapIndexed { index, group -> group.id to index }.toMap()
        val template = SavedBoardTemplate(
            id = java.util.UUID.randomUUID().toString(),
            name = trimmed,
            groups = groups.map { BoardTemplateGroup(it.name, it.accentColorHex, it.iconName, it.preferredLayoutMode, it.position, it.autoFilterType) },
            items = items.map { BoardTemplateItem(it.type, it.resourceId, it.titleOverride, it.position, it.size, it.displayMode, it.groupId?.let(groupIndexes::get)) }
        )
        val updated = (_uiState.value.savedBoardTemplates.filterNot { it.name.equals(trimmed, true) } + template).takeLast(20)
        settingsRepository.setSavedBoardTemplates(updated)
        _uiState.update { it.copy(savedBoardTemplates = updated, snackbarMessage = "Plantilla guardada") }
    }

    fun applySavedBoardTemplate(template: SavedBoardTemplate) = boardDelegate.applySavedBoardTemplate(template)

    fun deleteSavedBoardTemplate(templateId: String) {
        val updated = _uiState.value.savedBoardTemplates.filterNot { it.id == templateId }
        settingsRepository.setSavedBoardTemplates(updated)
        _uiState.update { it.copy(savedBoardTemplates = updated) }
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

    fun openResourceManager() = _uiState.update { it.copy(modals = it.modals.copy(showResourceManager = true)) }
    fun dismissResourceManager() = _uiState.update { it.copy(modals = it.modals.copy(showResourceManager = false)) }

    fun openSessionConfig() = _uiState.update { it.copy(modals = it.modals.copy(showSessionConfig = true)) }
    fun dismissSessionConfig() = _uiState.update { it.copy(modals = it.modals.copy(showSessionConfig = false)) }

    fun openDeckPicker() = _uiState.update { it.copy(modals = it.modals.copy(showDeckPicker = true)) }
    fun closeDeckPicker() = _uiState.update { it.copy(modals = it.modals.copy(showDeckPicker = false)) }

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
                _uiState.update { it.copy(
                    resources = it.resources.copy(availableStarterPacks = packInfos),
                    modals = it.modals.copy(showStarterPackDialog = true, showDeckPicker = false)
                ) }
            } catch (e: Exception) {
                _uiState.update { it.copy(snackbarMessage = "Error al cargar packs: ${e.message}") }
            }
        }
    }

    fun closeStarterPackDialog() {
        val shouldRestoreDeckPicker = wasDeckPickerOpenBeforeStarterPack
        wasDeckPickerOpenBeforeStarterPack = false
        _uiState.update { it.copy(modals = it.modals.copy(showStarterPackDialog = false, showDeckPicker = shouldRestoreDeckPicker)) }
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

    fun reorderSessionDecks(stackIds: List<Long>) = deckDelegate.reorderSessionDecks(stackIds)

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
        _uiState.update { it.copy(modals = it.modals.copy(showQuickNoteDialog = true)) }
    }

    /** Cierra el dialog de nota rápida sin guardar. */
    fun dismissQuickNote() {
        _uiState.update { it.copy(modals = it.modals.copy(showQuickNoteDialog = false)) }
    }

    /**
     * Añade una nota rápida con timestamp al texto de notas existente y navega al tab Notas.
     * Formato: "[HH:mm] texto\n"
     */
    fun addQuickNote(text: String) {
        if (text.isBlank()) {
            _uiState.update { it.copy(modals = it.modals.copy(showQuickNoteDialog = false)) }
            return
        }
        val time = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
            .format(java.util.Date())
        val entry = "[$time] $text"
        val current = _uiState.value.dmNotes
        val updated = if (current.isBlank()) entry else "$current\n$entry"
        _uiState.update { it.copy(dmNotes = updated, modals = it.modals.copy(showQuickNoteDialog = false), activeTab = 4) }
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
    fun resetDeck() {
        deckDelegate.resetDeck()
        updateBoardMemory { it.copy(tarokkaCardsByDeck = emptyMap()) }
    }
    fun resetSingleDeck(stackId: Long) {
        deckDelegate.resetSingleDeck(stackId)
        updateBoardMemory { it.copy(tarokkaCardsByDeck = it.tarokkaCardsByDeck - stackId) }
    }
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
        _uiState.update { it.copy(npc = it.npc.copy(playingNpcVoicePath = path)) }
        voicePlayer.playPath(path) {
            _uiState.update { it.copy(npc = it.npc.copy(playingNpcVoicePath = null)) }
        }
    }

    fun stopNpcVoice() {
        voicePlayer.stop()
        _uiState.update { it.copy(npc = it.npc.copy(playingNpcVoicePath = null)) }
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

    fun drawTarokkaCard(deckId: Long) {
        viewModelScope.launch {
            tarokkaMutex.withLock {
                val current = _uiState.value.boardMemory.tarokkaCardsByDeck[deckId].orEmpty()
                if (current.size >= TAROKKA_POSITIONS.size) return@withLock
                val ref = _uiState.value.deckRefs.firstOrNull { it.stackId == deckId } ?: return@withLock
                val card = drawCardUseCase(
                    sessionId = sessionId,
                    deckId = deckId,
                    drawMode = ref.drawModeOverride ?: DrawMode.RANDOM
                )
                if (card == null) {
                    _uiState.update { it.copy(snackbarMessage = "No hay más cartas disponibles para completar la tirada") }
                    return@withLock
                }
                updateBoardMemory { memory ->
                    val cards = memory.tarokkaCardsByDeck[deckId].orEmpty()
                    if (cards.size >= TAROKKA_POSITIONS.size) memory
                    else memory.copy(tarokkaCardsByDeck = memory.tarokkaCardsByDeck + (deckId to (cards + card.id)))
                }
            }
        }
    }

    fun clearTarokkaSpread(deckId: Long) {
        updateBoardMemory { memory ->
            memory.copy(tarokkaCardsByDeck = memory.tarokkaCardsByDeck - deckId)
        }
    }

    fun openDrawBySuitForDeck(deckId: Long) = deckDelegate.openDrawBySuit(deckId)

    // --- Permanent Players ---

    fun savePermanentPlayer(player: Npc) {
        viewModelScope.launch {
            npcRepository.saveNpc(player)
            _uiState.update { state ->
                val updated = state.permanentPlayers.filter { it.id != player.id } + player
                state.copy(npc = state.npc.copy(permanentPlayers = updated))
            }
        }
    }

    fun deletePermanentPlayer(player: Npc) {
        viewModelScope.launch {
            npcRepository.deleteNpc(player.id)
            _uiState.update { state ->
                state.copy(npc = state.npc.copy(permanentPlayers = state.permanentPlayers.filter { it.id != player.id }))
            }
        }
    }

    // --- Hex Map Exploration Actions ---

    fun linkHexMap(mapId: Long?) {
        viewModelScope.launch {
            if (mapId != null) {
                hexRepository.getHexMap(mapId).firstOrNull()?.let { map ->
                    hexRepository.upsertHexMap(map.copy(sessionId = sessionId))
                    _uiState.update { it.copy(snackbarMessage = "Mapa vinculado: ${map.name}") }
                }
            } else {
                _uiState.value.linkedHexMapId?.let { currentId ->
                    hexRepository.getHexMap(currentId).firstOrNull()?.let { map ->
                        hexRepository.upsertHexMap(map.copy(sessionId = null))
                    }
                }
            }
        }
    }

    fun createQuickHexMap(name: String = "Mapa de la Región") {
        viewModelScope.launch {
            val newMap = com.deckapp.core.model.HexMap(
                name = name,
                sessionId = sessionId,
                rows = 10,
                cols = 10
            )
            hexRepository.upsertHexMap(newMap)
            _uiState.update { it.copy(snackbarMessage = "Nuevo mapa creado y vinculado") }
        }
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

    fun loadAndStartEncounter(encounterId: Long) {
        viewModelScope.launch {
            encounterRepository.getEncounterById(encounterId).firstOrNull()?.let { encounter ->
                encounterRepository.saveEncounter(encounter.copy(linkedSessionId = sessionId, isActive = true))
                setActiveBoardView(SessionBoardView.COMBAT)
                _uiState.update { it.copy(snackbarMessage = "Encuentro iniciado: ${encounter.name}") }
            }
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
                sessionLogDelegate.logDiceRoll(
                    formula = displayFormula,
                    total = total,
                    details = fullDesc
                )
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
                sessionLogDelegate.logDiceRoll(
                    formula = formula,
                    total = total,
                    details = description
                )
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
        resourceId: Long? = null,
        targetGroupId: Long? = null
    ) = boardDelegate.addBoardBlock(type, resourceId, targetGroupId)

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
    fun setBoardGroupLayoutMode(groupId: Long, mode: SessionBoardLayoutMode) = boardDelegate.setBoardGroupLayoutMode(groupId, mode)
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

