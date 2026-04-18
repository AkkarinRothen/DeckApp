package com.deckapp.feature.deck

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deckapp.core.domain.repository.CardRepository
import com.deckapp.core.domain.usecase.UpdateCardNotesUseCase
import com.deckapp.core.domain.usecase.UpdateCardStateUseCase
import com.deckapp.core.model.Card
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CardViewUiState(
    val card: Card? = null,
    val isLoading: Boolean = true,
    val isSavingNotes: Boolean = false
)

@HiltViewModel
class CardViewViewModel @Inject constructor(
    private val cardRepository: CardRepository,
    private val updateCardNotesUseCase: UpdateCardNotesUseCase,
    private val updateCardStateUseCase: UpdateCardStateUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val cardId: Long = checkNotNull(savedStateHandle["cardId"])

    private val _isSavingNotes = MutableStateFlow(false)

    val uiState = combine(
        cardRepository.getCardById(cardId),
        _isSavingNotes
    ) { card, isSaving ->
        CardViewUiState(card = card, isLoading = false, isSavingNotes = isSaving)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CardViewUiState()
    )

    fun flipFace() {
        val card = uiState.value.card ?: return
        val nextFaceIndex = (card.currentFaceIndex + 1) % card.faces.size
        viewModelScope.launch {
            updateCardStateUseCase(cardId, UpdateCardStateUseCase.CardStateUpdate(faceIndex = nextFaceIndex))
        }
    }

    fun jumpToFace(index: Int) {
        viewModelScope.launch {
            updateCardStateUseCase(cardId, UpdateCardStateUseCase.CardStateUpdate(faceIndex = index))
        }
    }

    fun rotate90() {
        val card = uiState.value.card ?: return
        val nextRotation = (card.currentRotation + 90) % 360
        viewModelScope.launch {
            updateCardStateUseCase(cardId, UpdateCardStateUseCase.CardStateUpdate(rotation = nextRotation))
        }
    }

    fun setRotation(degrees: Int) {
        viewModelScope.launch {
            updateCardStateUseCase(cardId, UpdateCardStateUseCase.CardStateUpdate(rotation = degrees))
        }
    }

    fun toggleReversed() {
        val card = uiState.value.card ?: return
        viewModelScope.launch {
            updateCardStateUseCase(cardId, UpdateCardStateUseCase.CardStateUpdate(isReversed = !card.isReversed))
        }
    }

    fun toggleRevealed() {
        val card = uiState.value.card ?: return
        viewModelScope.launch {
            updateCardStateUseCase(cardId, UpdateCardStateUseCase.CardStateUpdate(isRevealed = !card.isRevealed))
        }
    }

    fun updateNotes(notes: String) {
        viewModelScope.launch {
            _isSavingNotes.value = true
            // Debounce visual para que el usuario perciba el guardado
            kotlinx.coroutines.delay(800)
            updateCardNotesUseCase(cardId, notes)
            _isSavingNotes.value = false
        }
    }
}
