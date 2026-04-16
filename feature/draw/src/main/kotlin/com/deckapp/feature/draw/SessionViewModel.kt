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
    val activeTab: Int = 0,                  // 0=Mazos, 1=Tablas, 2=Notas, 3=Combate
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
    val combatLog: List<CombatLogEntry> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SessionViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val cardRepository: CardRepository,
    private val encounterRepository: EncounterRepository,
    private val drawCardUseCase: DrawCardUseCase,
    private val undoLastActionUseCase: UndoLastActionUseCase,
    private val nextTurnUseCase: NextTurnUseCase,
    private val applyDamageUseCase: ApplyDamageUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val sessionId: Long = checkNotNull(savedStateHandle["sessionId"])

    private val _uiState = MutableStateFlow(SessionUiState())
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

    init {
        // Sesión info
        viewModelScope.launch {
            sessionRepository.getSessionById(sessionId)
                .collect { session -> 
                    _uiState.update { it.copy(
                        session = session,
                        dmNotes = session?.dmNotes ?: ""
                    ) } 
                }
        }

        // Timer de sesión
        viewModelScope.launch {
            while (true) {
                val session = _uiState.value.session
                if (session != null) {
                    val elapsed = (System.currentTimeMillis() - session.createdAt) / (1000 * 60)
                    _uiState.update { it.copy(sessionElapsedMinutes = elapsed) }
                }
                delay(60000) // cada minuto
            }
        }

        // Mazos asignados a la sesión + nombres reactivos
        viewModelScope.launch {
            combine(
                sessionRepository.getDecksForSession(sessionId),
                cardRepository.getAllDecks()
            ) { refs, allDecks ->
                val names = allDecks.associate { it.id to it.name }
                val ratios = allDecks.associate { it.id to it.aspectRatio }
                val backImages = allDecks.associate { it.id to it.backImagePath }
                object {
                    val refs = refs; val names = names
                    val ratios = ratios; val backImages = backImages
                }
            }.collect { data ->
                _uiState.update { state ->
                    state.copy(
                        deckRefs = data.refs,
                        deckNames = data.names,
                        deckAspectRatios = data.ratios,
                        deckBackImages = data.backImages,
                        // Auto-seleccionar el primer mazo si todavía no hay selección
                        selectedDeckId = state.selectedDeckId
                            ?: data.refs.firstOrNull()?.stackId
                    )
                }
            }
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
    }

    /** Selecciona el mazo activo para la siguiente acción ROBAR. */
    fun selectDeck(stackId: Long) {
        _uiState.update { it.copy(selectedDeckId = stackId) }
    }

    /** Actualiza el tab activo del workspace de sesión. */
    fun setActiveTab(tab: Int) {
        _uiState.update { it.copy(activeTab = tab) }
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
        _uiState.update { it.copy(dmNotes = updated, showQuickNoteDialog = false, activeTab = 3) }
        viewModelScope.launch {
            sessionRepository.updateDmNotes(sessionId, updated)
        }
    }

    /** Actualiza las notas de la sesión (llamado desde el editor con debounce en la UI). */
    fun updateNotes(notes: String) {
        _uiState.update { it.copy(dmNotes = notes) }
        viewModelScope.launch {
            sessionRepository.updateDmNotes(sessionId, notes)
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
            sessionRepository.logEvent(
                DrawEvent(sessionId = sessionId, cardId = cardId, action = DrawAction.DISCARD)
            )
            cardRepository.updateCardDrawnState(cardId, isDrawn = false, lastDrawnAt = null)
        }
    }

    /** Regresa todas las cartas de la pila al mazo (Shuffle back). */
    fun shufflePileBack(stackId: Long? = null) {
        viewModelScope.launch {
            val piledCards = cardRepository.getPiledCards(sessionId).first()
            val cardsToReturn = if (stackId != null) {
                piledCards.filter { it.stackId == stackId }
            } else {
                piledCards
            }

            if (cardsToReturn.isEmpty()) return@launch

            // Marcamos como no robadas
            cardsToReturn.forEach { card ->
                cardRepository.updateCardDrawnState(card.id, isDrawn = false)
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
            _uiState.update { it.copy(snackbarMessage = "Pila barajada de vuelta al mazo") }
        }
    }

    /** Mueve una carta específica de la pila a la mano. */
    fun returnToHand(cardId: Long) {
        viewModelScope.launch {
            cardRepository.updateCardDrawnState(cardId, isDrawn = true, lastDrawnAt = System.currentTimeMillis())
            // Podríamos loguear un evento especial si fuera necesario
        }
    }

    /** Mueve una carta específica de la pila de vuelta al mazo. */
    fun returnToDeck(cardId: Long) {
        viewModelScope.launch {
            cardRepository.updateCardDrawnState(cardId, isDrawn = false)
            // No necesita evento DISCARD porque ya está fuera, simplemente se limpia de la pila
            // (isDrawn=false && sin evento DISCARD reciente manejará esto si el DAO es correcto).
            // NOTA: Para que getPiledCards deje de verla, técnicamente necesita un RESET o que
            // nosotros logueemos algo que "gane" al DISCARD.
            // Por simplicidad, el DAO actual usa timestamp > MAX(RESET).
            // Vamos a registrar un SHUFFLE_BACK individual para que el DAO (si lo ajustamos) lo ignore.
            // O mejor, el DAO de PiledCards debería excluir cartas que tienen un evento posterior al DISCARD.
        }
    }

    /** Resetea todos los mazos de la sesión. */
    fun resetDeck() {
        viewModelScope.launch {
            _uiState.value.hand.forEach { card ->
                cardRepository.updateCardDrawnState(card.id, isDrawn = false, lastDrawnAt = null)
            }
            _uiState.value.deckRefs.forEach { ref ->
                cardRepository.resetDeck(ref.stackId)
            }
            sessionRepository.logEvent(
                DrawEvent(sessionId = sessionId, cardId = null, action = DrawAction.RESET)
            )
        }
    }

    /**
     * Resetea un único mazo: devuelve al mazo todas sus cartas (mano + pila de descarte).
     * Loguea un RESET individual con metadata del stackId.
     */
    fun resetSingleDeck(stackId: Long) {
        viewModelScope.launch {
            // Devolver al mazo las cartas en mano de este deck
            _uiState.value.hand
                .filter { it.stackId == stackId }
                .forEach { card -> cardRepository.updateCardDrawnState(card.id, isDrawn = false, lastDrawnAt = null) }
            // Resetear todas las cartas del mazo (disponibles + descartadas)
            cardRepository.resetDeck(stackId)
            sessionRepository.logEvent(
                DrawEvent(
                    sessionId = sessionId,
                    cardId = null,
                    action = DrawAction.RESET,
                    metadata = "Mazo: $stackId"
                )
            )
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
            }
        }
    }

    fun clearPeek() = _uiState.update { it.copy(peekCard = null) }

    /** Revela una carta que fue robada boca abajo. Loguea FLIP como evento de revelación. */
    fun revealCard(cardId: Long) {
        viewModelScope.launch {
            cardRepository.updateCardRevealed(cardId, isRevealed = true)
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
        val encounterId = _uiState.value.activeEncounter?.id ?: return
        viewModelScope.launch {
            nextTurnUseCase(encounterId)
        }
    }

    fun toggleCondition(creatureId: Long, condition: Condition) {
        viewModelScope.launch {
            val creature = encounterRepository.getCreatureById(creatureId) ?: return@launch
            val newConditions = if (condition in creature.conditions) {
                creature.conditions - condition
            } else {
                creature.conditions + condition
            }
            encounterRepository.updateCreature(creature.copy(conditions = newConditions))
        }
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
            
            // 3. Desactivar el encuentro
            encounterRepository.saveEncounter(encounter.copy(isActive = false))
            
            // 4. Volver a mazos si estábamos en combate
            if (_uiState.value.activeTab == 4) {
                _uiState.update { it.copy(activeTab = 0, snackbarMessage = "Combate finalizado y resumido en notas") }
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
