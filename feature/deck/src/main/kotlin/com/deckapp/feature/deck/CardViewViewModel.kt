package com.deckapp.feature.deck

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deckapp.core.domain.repository.CardRepository
import com.deckapp.core.domain.usecase.UpdateCardNotesUseCase
import com.deckapp.core.domain.usecase.UpdateCardStateUseCase
import com.deckapp.core.model.Card
import com.deckapp.core.model.CardContentMode
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

    fun rotateToNextOption() {
        val card = uiState.value.card ?: return
        val normalizedRotation = ((card.currentRotation % 360) + 360) % 360
        val update = when (card.activeFace.contentMode) {
            CardContentMode.FOUR_EDGE_CUES ->
                UpdateCardStateUseCase.CardStateUpdate(rotation = (normalizedRotation + 90) % 360)

            CardContentMode.TOP_BOTTOM_SPLIT -> {
                val currentOptionRotation = if (normalizedRotation == 180) 180 else 0
                UpdateCardStateUseCase.CardStateUpdate(
                    rotation = if (currentOptionRotation == 0) 180 else 0
                )
            }

            CardContentMode.REVERSIBLE -> {
                val isCurrentlyReversed = card.isReversed || normalizedRotation == 180
                val nextIsReversed = !isCurrentlyReversed
                UpdateCardStateUseCase.CardStateUpdate(
                    rotation = if (nextIsReversed) 180 else 0,
                    isReversed = nextIsReversed
                )
            }

            else -> return
        }
        viewModelScope.launch {
            updateCardStateUseCase(cardId, update)
        }
    }

    fun setRotation(degrees: Int) {
        val card = uiState.value.card ?: return
        val normalizedRotation = ((degrees % 360) + 360) % 360
        val update = when (card.activeFace.contentMode) {
            CardContentMode.FOUR_EDGE_CUES ->
                UpdateCardStateUseCase.CardStateUpdate(rotation = normalizedRotation / 90 * 90)

            CardContentMode.TOP_BOTTOM_SPLIT ->
                UpdateCardStateUseCase.CardStateUpdate(rotation = if (normalizedRotation == 180) 180 else 0)

            CardContentMode.REVERSIBLE -> {
                val isReversed = normalizedRotation == 180
                UpdateCardStateUseCase.CardStateUpdate(
                    rotation = if (isReversed) 180 else 0,
                    isReversed = isReversed
                )
            }

            else -> UpdateCardStateUseCase.CardStateUpdate(rotation = normalizedRotation)
        }
        viewModelScope.launch {
            updateCardStateUseCase(cardId, update)
        }
    }

    fun toggleReversed() {
        val card = uiState.value.card ?: return
        val normalizedRotation = ((card.currentRotation % 360) + 360) % 360
        val isCurrentlyReversed = card.isReversed || normalizedRotation == 180
        val nextIsReversed = !isCurrentlyReversed
        viewModelScope.launch {
            updateCardStateUseCase(
                cardId,
                UpdateCardStateUseCase.CardStateUpdate(
                    rotation = if (nextIsReversed) 180 else 0,
                    isReversed = nextIsReversed
                )
            )
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
