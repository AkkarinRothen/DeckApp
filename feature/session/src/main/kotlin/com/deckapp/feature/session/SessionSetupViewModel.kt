package com.deckapp.feature.session

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deckapp.core.domain.repository.CardRepository
import com.deckapp.core.domain.repository.SessionRepository
import com.deckapp.core.domain.repository.ReferenceRepository
import com.deckapp.core.model.CardStack
import com.deckapp.core.model.DrawMode
import com.deckapp.core.model.Session
import com.deckapp.core.model.SessionDeckRef
import com.deckapp.core.model.StackType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DeckSelection(
    val deck: CardStack,
    val drawMode: DrawMode = DrawMode.TOP,
    val isSelected: Boolean = false
)

data class SessionSetupUiState(
    val sessionName: String = "",
    val availableDecks: List<DeckSelection> = emptyList(),
    val selectedGameSystems: List<String> = listOf("General"),
    val isLoading: Boolean = true,
    val createdSessionId: Long? = null
)

@HiltViewModel
class SessionSetupViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val cardRepository: CardRepository,
    private val referenceRepository: ReferenceRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val preselectedDeckId: Long? = savedStateHandle["preselectedDeckId"]

    private val _uiState = MutableStateFlow(SessionSetupUiState())
    val uiState: StateFlow<SessionSetupUiState> = _uiState.asStateFlow()

    private val _availableSystems = MutableStateFlow<List<String>>(emptyList())
    val availableSystems: StateFlow<List<String>> = _availableSystems.asStateFlow()

    init {
        viewModelScope.launch {
            referenceRepository.getDistinctSystems().collect { systems ->
                _availableSystems.value = systems
            }
        }

        viewModelScope.launch {
            cardRepository.getAllDecks()
                .map { decks ->
                    decks
                        .filter { it.type == StackType.DECK }
                        .map { deck ->
                            DeckSelection(
                                deck = deck,
                                isSelected = deck.id == preselectedDeckId
                            )
                        }
                }
                .collect { selections ->
                    _uiState.update { it.copy(availableDecks = selections, isLoading = false) }
                }
        }
    }

    fun updateSessionName(name: String) = _uiState.update { it.copy(sessionName = name) }

    fun updateGameSystems(systems: List<String>) = _uiState.update { 
        it.copy(selectedGameSystems = if (systems.isEmpty()) listOf("General") else systems) 
    }

    fun toggleDeckSelection(deckId: Long) {
        _uiState.update { state ->
            state.copy(
                availableDecks = state.availableDecks.map { selection ->
                    if (selection.deck.id == deckId) selection.copy(isSelected = !selection.isSelected)
                    else selection
                }
            )
        }
    }

    fun updateDrawMode(deckId: Long, mode: DrawMode) {
        _uiState.update { state ->
            state.copy(
                availableDecks = state.availableDecks.map { selection ->
                    if (selection.deck.id == deckId) selection.copy(drawMode = mode)
                    else selection
                }
            )
        }
    }

    fun createSession() {
        val state = _uiState.value
        val selectedDecks = state.availableDecks.filter { it.isSelected }
        if (state.sessionName.isBlank() || selectedDecks.isEmpty()) return

        viewModelScope.launch {
            val session = Session(
                name = state.sessionName,
                gameSystems = state.selectedGameSystems
            )
            val sessionId = sessionRepository.createSession(session)
            selectedDecks.forEachIndexed { index, selection ->
                sessionRepository.addDeckToSession(
                    SessionDeckRef(
                        sessionId = sessionId,
                        stackId = selection.deck.id,
                        drawModeOverride = selection.drawMode,
                        sortOrder = index
                    )
                )
            }
            _uiState.update { it.copy(createdSessionId = sessionId) }
        }
    }
}
