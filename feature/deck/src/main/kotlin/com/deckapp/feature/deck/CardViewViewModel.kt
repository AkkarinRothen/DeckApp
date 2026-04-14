package com.deckapp.feature.deck

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deckapp.core.domain.repository.CardRepository
import com.deckapp.core.model.Card
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CardViewUiState(
    val card: Card? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class CardViewViewModel @Inject constructor(
    private val cardRepository: CardRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val cardId: Long = checkNotNull(savedStateHandle["cardId"])

    val uiState = cardRepository.getCardById(cardId)
        .map { CardViewUiState(card = it, isLoading = false) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = CardViewUiState()
        )

    fun flipFace() {
        val card = uiState.value.card ?: return
        val nextFaceIndex = (card.currentFaceIndex + 1) % card.faces.size
        viewModelScope.launch {
            cardRepository.updateCardFaceIndex(cardId, nextFaceIndex)
        }
    }

    fun rotate90() {
        val card = uiState.value.card ?: return
        val nextRotation = (card.currentRotation + 90) % 360
        viewModelScope.launch {
            cardRepository.updateCardRotation(cardId, nextRotation)
        }
    }

    fun toggleReversed() {
        val card = uiState.value.card ?: return
        viewModelScope.launch {
            cardRepository.updateCardReversed(cardId, !card.isReversed)
        }
    }
}
