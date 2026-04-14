package com.deckapp.feature.draw

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deckapp.core.domain.repository.CardRepository
import com.deckapp.core.domain.repository.SessionRepository
import com.deckapp.core.domain.usecase.DrawCardUseCase
import com.deckapp.core.domain.usecase.UndoLastActionUseCase
import com.deckapp.core.model.*
import com.deckapp.core.model.CardAspectRatio
import dagger.hilt.android.lifecycle.HiltViewModel
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
    val dmNotes: String = "",
    val sessionElapsedMinutes: Long = 0,
    val deckAspectRatios: Map<Long, CardAspectRatio> = emptyMap() // stackId → aspectRatio
)

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val cardRepository: CardRepository,
    private val drawCardUseCase: DrawCardUseCase,
    private val undoLastActionUseCase: UndoLastActionUseCase,
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
                Triple(refs, names, ratios)
            }.collect { (refs, names, ratios) ->
                _uiState.update { state ->
                    state.copy(
                        deckRefs = refs,
                        deckNames = names,
                        deckAspectRatios = ratios,
                        // Auto-seleccionar el primer mazo si todavía no hay selección
                        selectedDeckId = state.selectedDeckId
                            ?: refs.firstOrNull()?.stackId
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

        // Mano: cartas con isDrawn = true
        viewModelScope.launch {
            cardRepository.getDrawnCards()
                .collect { drawn ->
                    _uiState.update { it.copy(hand = drawn) }
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
    }

    /** Selecciona el mazo activo para la siguiente acción ROBAR. */
    fun selectDeck(stackId: Long) {
        _uiState.update { it.copy(selectedDeckId = stackId) }
    }

    /** Actualiza el tab activo del workspace de sesión. */
    fun setActiveTab(tab: Int) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    /** Stub — lógica de tirada activa implementada en Sprint 5. */
    fun rollActiveTable() { /* Implementado en feature:tables */ }

    /** Cambia al tab de notas. */
    fun startQuickNote() {
        _uiState.update { it.copy(activeTab = 2) }
    }

    /** Actualiza las notas de la sesión con debounce se manejaría en la UI, pero el repo persiste. */
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
            cardRepository.updateCardDrawnState(cardId, isDrawn = false)
        }
    }

    /** Resetea todos los mazos de la sesión. */
    fun resetDeck() {
        viewModelScope.launch {
            _uiState.value.hand.forEach { card ->
                cardRepository.updateCardDrawnState(card.id, isDrawn = false)
            }
            _uiState.value.deckRefs.forEach { ref ->
                cardRepository.resetDeck(ref.stackId)
            }
            sessionRepository.logEvent(
                DrawEvent(sessionId = sessionId, cardId = 0L, action = DrawAction.RESET)
            )
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

    /** Genera un log de texto con los eventos de la sesión. */
    suspend fun getSessionLogText(): String {
        val events = sessionRepository.getEventsForSession(sessionId).first()
        val sessionName = _uiState.value.session?.name ?: "Sesión"
        val sb = StringBuilder()
        sb.append("LOG DE SESIÓN: $sessionName\n")
        sb.append("Generado: ${java.text.SimpleDateFormat("dd/MM/yyyy HH:mm").format(java.util.Date())}\n")
        sb.append("--------------------------------------------------\n\n")

        events.forEach { event ->
            val time = java.text.SimpleDateFormat("HH:mm:ss").format(java.util.Date(event.timestamp))
            sb.append("[$time] ${event.action}: Carta ${event.cardId} ${event.metadata}\n")
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
