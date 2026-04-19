package com.deckapp.feature.draw

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deckapp.core.domain.repository.CardRepository
import com.deckapp.core.domain.repository.EncounterRepository
import com.deckapp.core.domain.repository.SessionRepository
import com.deckapp.core.domain.usecase.*
import com.deckapp.core.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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
    val isSimplifiedMode: Boolean = false
)



@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SessionViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val cardRepository: CardRepository,
    private val tableRepository: com.deckapp.core.domain.repository.TableRepository,
    private val encounterRepository: EncounterRepository,
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
    private val settingsRepository: com.deckapp.core.domain.repository.SettingsRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val sessionId: Long = checkNotNull(savedStateHandle["sessionId"])
    private var peekCardId: Long?
        get() = savedStateHandle.get<Long>("peekCardId")
        set(value) { savedStateHandle["peekCardId"] = value }

    private val _uiState = MutableStateFlow(SessionUiState())
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

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
                        dmNotes = session.dmNotes ?: "",
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
    }

    /** Selecciona el mazo activo para la siguiente acción ROBAR. */
    fun selectDeck(stackId: Long) {
        _uiState.update { it.copy(selectedDeckId = stackId) }
    }

    /** Actualiza el tab activo del workspace de sesión. */
    fun setActiveTab(tab: Int) {
        _uiState.update { it.copy(activeTab = tab) }
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

    /** Stub — lógica de tirada activa implementada en Sprint 5. */
    fun rollActiveTable() { /* Implementado en feature:tables */ }

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
            sessionRepository.updateDmNotes(sessionId, updated)
        }
    }

    /** Actualiza las notas de la sesión (llamado desde el editor con debounce en la UI). */
    fun updateNotes(notes: String) {
        if (notes == _uiState.value.dmNotes) return
        
        _uiState.update { it.copy(dmNotes = notes, isSavingNotes = true) }
        viewModelScope.launch {
            sessionRepository.updateDmNotes(sessionId, notes)
            // Pequeña pausa para que el indicador sea perceptible si la DB es muy rápida
            delay(400)
            _uiState.update { it.copy(isSavingNotes = false) }
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

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
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
}
