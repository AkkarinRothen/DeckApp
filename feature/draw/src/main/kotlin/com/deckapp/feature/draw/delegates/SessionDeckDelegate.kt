package com.deckapp.feature.draw.delegates

import com.deckapp.core.domain.repository.CardRepository
import com.deckapp.core.domain.repository.SessionRepository
import com.deckapp.core.domain.usecase.*
import com.deckapp.core.model.*
import com.deckapp.feature.draw.SessionUiState
import com.deckapp.feature.draw.model.state.DecksUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Delegado encargado de la gestión de mazos, mano, descarte y operaciones de cartas en la sesión.
 * Extraído de SessionViewModel para aislar la lógica de robo, descarte y barajado.
 */
class SessionDeckDelegate(
    private val sessionId: Long,
    private val _uiState: MutableStateFlow<SessionUiState>,
    private val scope: CoroutineScope,
    private val cardRepository: CardRepository,
    private val sessionRepository: SessionRepository,
    private val drawCardUseCase: DrawCardUseCase,
    private val discardCardUseCase: DiscardCardUseCase,
    private val resetDeckUseCase: ResetDeckUseCase,
    private val shuffleDeckUseCase: ShuffleDeckUseCase,
    private val updateCardStateUseCase: UpdateCardStateUseCase,
    private val undoLastActionUseCase: UndoLastActionUseCase,
    private val getPeekCardId: () -> Long? = { null },
    private val setPeekCardId: (Long?) -> Unit = {}
) {

    /**
     * Helper para mutaciones atómicas del sub-estado [DecksUiState].
     */
    fun updateDeckState(transform: (DecksUiState) -> DecksUiState) {
        _uiState.update { it.copy(decks = transform(it.decks)) }
    }

    /**
     * Selecciona un mazo y roba una carta de él.
     */
    fun drawCardFromDeck(stackId: Long) {
        updateDeckState { it.copy(selectedDeckId = stackId) }
        scope.launch {
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
     * DrawCardUseCase persiste el DrawEvent ANTES de retornar -> crash-safe.
     */
    fun drawCard() {
        val state = _uiState.value
        val deckId = state.selectedDeckId ?: state.deckRefs.firstOrNull()?.stackId ?: return
        val deckRef = state.deckRefs.find { it.stackId == deckId } ?: return
        scope.launch {
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
        scope.launch {
            discardCardUseCase(sessionId, cardId)
        }
    }

    /** Regresa todas las cartas de la pila al mazo (Shuffle back). Activa animación C-5. */
    fun shufflePileBack(stackId: Long? = null) {
        scope.launch {
            updateDeckState { it.copy(isShuffling = true) }

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
            updateDeckState { it.copy(isShuffling = false) }
            if (cardsToReturn.isNotEmpty()) {
                _uiState.update { it.copy(snackbarMessage = "Pila barajada de vuelta al mazo") }
            }
        }
    }

    /** Abre el sheet de "Robar por palo" cargando los palos disponibles del mazo activo. */
    fun openDrawBySuit() {
        val deckId = _uiState.value.selectedDeckId ?: _uiState.value.deckRefs.firstOrNull()?.stackId ?: return
        openDrawBySuit(deckId)
    }

    fun openDrawBySuit(stackId: Long) {
        updateDeckState { it.copy(selectedDeckId = stackId) }
        scope.launch {
            val suits = cardRepository.getCardsForStack(stackId).first()
                .filter { !it.isDrawn }
                .mapNotNull { it.suit }
                .distinct()
                .sorted()
            updateDeckState { it.copy(showDrawBySuitSheet = true, availableSuitsForDraw = suits) }
        }
    }

    /** Roba una carta aleatoria del palo indicado en el mazo activo. */
    fun drawCardBySuit(suit: String) {
        val deckId = _uiState.value.selectedDeckId ?: return
        updateDeckState { it.copy(showDrawBySuitSheet = false) }
        scope.launch {
            val deckRef = _uiState.value.deckRefs.find { it.stackId == deckId } ?: return@launch
            val card = drawCardUseCase(
                sessionId = sessionId,
                deckId = deckId,
                drawMode = DrawMode.RANDOM,
                suit = suit
            )
            if (card == null) {
                _uiState.update { it.copy(snackbarMessage = "No quedan cartas del palo \"$suit\"") }
            }
        }
    }

    fun closeDrawBySuitSheet() {
        updateDeckState { it.copy(showDrawBySuitSheet = false) }
    }

    /** Mueve una carta específica de la pila a la mano. */
    fun returnToHand(cardId: Long) {
        scope.launch {
            updateCardStateUseCase(
                cardId,
                UpdateCardStateUseCase.CardStateUpdate(isDrawn = true, lastDrawnAt = System.currentTimeMillis())
            )
        }
    }

    /** Mueve una carta específica de la pila de vuelta al mazo. */
    fun returnToDeck(cardId: Long) {
        scope.launch {
            updateCardStateUseCase(cardId, UpdateCardStateUseCase.CardStateUpdate(isDrawn = false))
        }
    }

    /** Resetea todos los mazos de la sesión. */
    fun resetDeck() {
        scope.launch {
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
        scope.launch {
            resetDeckUseCase(sessionId, stackId)
            val name = _uiState.value.deckNames[stackId] ?: "Mazo"
            _uiState.update { it.copy(snackbarMessage = "\"$name\" reseteado") }
        }
    }

    /** Deshace la última acción via event log. */
    fun undo() {
        scope.launch {
            val undone = undoLastActionUseCase(sessionId)
            val msg = if (undone) "Acción deshecha" else "Nada que deshacer"
            _uiState.update { it.copy(snackbarMessage = msg) }
        }
    }

    fun undoLastDraw() {
        scope.launch {
            val success = undoLastActionUseCase(sessionId)
            if (success) {
                _uiState.update { it.copy(canUndo = false, snackbarMessage = "Acción deshecha") }
            }
        }
    }

    /** Alterna el modo nocturno. */
    fun toggleNightMode() {
        _uiState.update { it.copy(nightMode = !it.nightMode) }
    }

    /** Alterna la visibilidad de los nombres de las cartas. */
    fun toggleCardTitles() {
        val current = _uiState.value.session?.showCardTitles ?: true
        scope.launch {
            sessionRepository.toggleCardTitles(sessionId, !current)
        }
    }

    /** Ordena la mano actual por valor de carta (Iniciativa). */
    fun sortByValue() {
        val sortedHand = _uiState.value.hand.sortedBy { it.value ?: Int.MAX_VALUE }
        updateDeckState { it.copy(hand = sortedHand) }
    }

    /** Muestra la primera carta disponible del mazo sin modificar su estado (Peek). */
    fun peekTopCard() {
        val deckId = _uiState.value.selectedDeckId ?: _uiState.value.deckRefs.firstOrNull()?.stackId ?: return
        scope.launch {
            val card = cardRepository.getTopCard(deckId)
            if (card == null) {
                _uiState.update { it.copy(snackbarMessage = "No hay cartas disponibles") }
            } else {
                updateDeckState { it.copy(peekCard = card) }
                setPeekCardId(card.id)
            }
        }
    }

    fun clearPeek() {
        updateDeckState { it.copy(peekCard = null) }
        setPeekCardId(null)
    }

    /** Revela una carta que fue robada boca abajo. Loguea FLIP como evento de revelación. */
    fun revealCard(cardId: Long) {
        scope.launch {
            updateCardStateUseCase(cardId, UpdateCardStateUseCase.CardStateUpdate(isRevealed = true))
            sessionRepository.logEvent(
                DrawEvent(
                    sessionId = sessionId,
                    cardId = cardId,
                    action = DrawAction.FLIP,
                    metadata = "reveal"
                )
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
        scope.launch {
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
                _uiState.update {
                    it.copy(snackbarMessage = "Solo quedaban $drawn carta${if (drawn != 1) "s" else ""} disponibles")
                }
            }
        }
    }

    fun toggleDeckInSession(deckId: Long) {
        scope.launch {
            val isPresent = _uiState.value.deckRefs.any { it.stackId == deckId }
            if (isPresent) {
                sessionRepository.removeDeckFromSession(sessionId, deckId)
            } else {
                sessionRepository.addDeckToSession(SessionDeckRef(sessionId, deckId))
            }
        }
    }

    /**
     * Alterna el estado expandido/colapsado de un cluster de mazo en el Workspace Bento.
     * El estado de colapso es efímero (no se persiste en Room).
     */
    fun toggleDeckCollapse(stackId: Long) {
        updateDeckState { state ->
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
        updateDeckState { it.copy(selectedDeckId = stackId) }
    }

    fun shuffleSingleDeck(stackId: Long) {
        shufflePileBack(stackId)
    }

    /** Baraja las cartas disponibles en el mazo sin alterar descartes ni mano. */
    fun shuffleDeckOnly(stackId: Long) {
        scope.launch {
            updateDeckState { it.copy(isShuffling = true) }
            shuffleDeckUseCase(stackId, onlyAvailable = true)
            val name = _uiState.value.deckNames[stackId] ?: "Mazo"
            sessionRepository.logEvent(
                DrawEvent(
                    sessionId = sessionId,
                    cardId = null,
                    action = DrawAction.SHUFFLE_BACK,
                    metadata = "Barajadas disponibles: $stackId"
                )
            )
            delay(500)
            updateDeckState { it.copy(isShuffling = false) }
            _uiState.update { it.copy(snackbarMessage = "\"$name\" barajado") }
        }
    }

    /** Rota una carta a su siguiente orientación según su contentMode. */
    fun rotateCard(cardId: Long) {
        scope.launch {
            val card = cardRepository.getCardById(cardId).first() ?: return@launch
            val nextRotation = when (card.activeFace.contentMode) {
                CardContentMode.FOUR_EDGE_CUES -> (card.currentRotation + 90) % 360
                CardContentMode.TOP_BOTTOM_SPLIT, CardContentMode.REVERSIBLE -> (card.currentRotation + 180) % 360
                else -> (card.currentRotation + 90) % 360
            }
            updateCardStateUseCase(cardId, UpdateCardStateUseCase.CardStateUpdate(rotation = nextRotation))
            sessionRepository.logEvent(
                DrawEvent(
                    sessionId = sessionId,
                    cardId = cardId,
                    action = DrawAction.ROTATE,
                    metadata = "$nextRotation"
                )
            )
        }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
