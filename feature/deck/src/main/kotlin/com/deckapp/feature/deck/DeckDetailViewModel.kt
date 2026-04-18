package com.deckapp.feature.deck

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deckapp.core.domain.repository.CardRepository
import com.deckapp.core.domain.usecase.DuplicateDeckUseCase
import com.deckapp.core.domain.usecase.ExportDeckToZipUseCase
import com.deckapp.core.domain.usecase.MergeDecksUseCase
import com.deckapp.core.model.Card
import com.deckapp.core.model.CardStack
import android.net.Uri
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DeckDetailUiState(
    val deck: CardStack? = null,
    val cards: List<Card> = emptyList(),
    val filteredCards: List<Card> = emptyList(),
    val availableSuits: List<String> = emptyList(),
    val suitFilter: String? = null,
    val isLoading: Boolean = true,
    val duplicatedDeckId: Long? = null,   // non-null tras duplicar → navegación
    val isDuplicating: Boolean = false,
    val isExporting: Boolean = false,
    val isMerging: Boolean = false,
    val showMergeDialog: Boolean = false,
    val availableDecks: List<CardStack> = emptyList(),
    val errorMessage: String? = null,
    val exportSuccessMessage: String? = null,
    val showConfigSheet: Boolean = false,
    val isReorderMode: Boolean = false
)

@HiltViewModel
class DeckDetailViewModel @Inject constructor(
    private val cardRepository: CardRepository,
    private val duplicateDeckUseCase: DuplicateDeckUseCase,
    private val mergeDecksUseCase: MergeDecksUseCase,
    private val exportDeckToZipUseCase: ExportDeckToZipUseCase,
    private val getOrCreateTagUseCase: com.deckapp.core.domain.usecase.GetOrCreateTagUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val deckId: Long = checkNotNull(savedStateHandle["deckId"])
    private val _extras = MutableStateFlow(DeckDetailExtras())

    private data class DeckDetailExtras(
        val duplicatedDeckId: Long? = null,
        val isDuplicating: Boolean = false,
        val isExporting: Boolean = false,
        val isMerging: Boolean = false,
        val showMergeDialog: Boolean = false,
        val errorMessage: String? = null,
        val exportSuccessMessage: String? = null,
        val showConfigSheet: Boolean = false,
        val suitFilter: String? = null,
        val isReorderMode: Boolean = false
    )

    val uiState = combine(
        cardRepository.getDeckById(deckId),
        cardRepository.getCardsForStack(deckId),
        cardRepository.getAllDecks(),
        _extras
    ) { deck, cards, allDecks, extras ->
        // Este bloque ahora se ejecutará en Dispatchers.Default gracias a flowOn
        val suits = cards.mapNotNull { it.suit }.distinct().sorted()
        val filtered = if (extras.suitFilter == null) cards
                       else cards.filter { it.suit == extras.suitFilter }
        DeckDetailUiState(
            deck = deck,
            cards = cards,
            filteredCards = filtered,
            availableSuits = suits,
            suitFilter = extras.suitFilter,
            isLoading = false,
            duplicatedDeckId = extras.duplicatedDeckId,
            isDuplicating = extras.isDuplicating,
            isExporting = extras.isExporting,
            isMerging = extras.isMerging,
            showMergeDialog = extras.showMergeDialog,
            availableDecks = allDecks.filter { it.id != deckId },
            errorMessage = extras.errorMessage,
            exportSuccessMessage = extras.exportSuccessMessage,
            showConfigSheet = extras.showConfigSheet,
            isReorderMode = extras.isReorderMode
        )
    }.flowOn(Dispatchers.Default)
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DeckDetailUiState()
    )

    fun duplicateDeck() {
        val id = deckId
        _extras.update { it.copy(isDuplicating = true) }
        viewModelScope.launch {
            val result = duplicateDeckUseCase(id)
            _extras.update {
                it.copy(
                    isDuplicating = false,
                    duplicatedDeckId = result.getOrNull(),
                    errorMessage = result.exceptionOrNull()?.message
                )
            }
        }
    }

    fun showMergeDialog(show: Boolean) {
        _extras.update { it.copy(showMergeDialog = show) }
    }

    fun mergeWithDeck(sourceDeckId: Long) {
        val targetId = deckId
        _extras.update { it.copy(isMerging = true, showMergeDialog = false) }
        viewModelScope.launch {
            val result = mergeDecksUseCase(sourceDeckId = sourceDeckId, targetDeckId = targetId)
            _extras.update {
                it.copy(
                    isMerging = false,
                    errorMessage = result.exceptionOrNull()?.message,
                    exportSuccessMessage = if (result.isSuccess) "Mazo fusionado correctamente" else null
                )
            }
        }
    }

    fun exportToZip(outputUri: Uri) {
        val id = deckId
        _extras.update { it.copy(isExporting = true, errorMessage = null, exportSuccessMessage = null) }
        viewModelScope.launch {
            val result = exportDeckToZipUseCase(id, outputUri)
            _extras.update {
                it.copy(
                    isExporting = false,
                    exportSuccessMessage = if (result.isSuccess) "Mazo exportado correctamente" else null,
                    errorMessage = result.exceptionOrNull()?.message
                )
            }
        }
    }

    fun clearMessages() {
        _extras.update { it.copy(errorMessage = null, exportSuccessMessage = null) }
    }

    /** Llamar desde la UI después de consumir la navegación. */
    fun onDuplicatedNavHandled() {
        _extras.update { it.copy(duplicatedDeckId = null) }
    }

    fun showConfigSheet(show: Boolean) {
        _extras.update { it.copy(showConfigSheet = show) }
    }

    fun updateDrawMode(mode: com.deckapp.core.model.DrawMode) {
        val deck = uiState.value.deck ?: return
        viewModelScope.launch {
            cardRepository.updateStack(deck.copy(drawMode = mode))
        }
    }

    fun updateAspectRatio(ratio: com.deckapp.core.model.CardAspectRatio) {
        val deck = uiState.value.deck ?: return
        viewModelScope.launch {
            cardRepository.updateStack(deck.copy(aspectRatio = ratio))
        }
    }

    fun toggleDrawFaceDown(enabled: Boolean) {
        val deck = uiState.value.deck ?: return
        viewModelScope.launch {
            cardRepository.updateStack(deck.copy(drawFaceDown = enabled))
        }
    }

    fun setBackImage(path: String?) {
        val deck = uiState.value.deck ?: return
        viewModelScope.launch {
            cardRepository.updateStack(deck.copy(backImagePath = path))
        }
    }

    fun updateName(name: String) {
        val deck = uiState.value.deck ?: return
        if (name == deck.name) return
        viewModelScope.launch {
            cardRepository.updateStack(deck.copy(name = name))
        }
    }

    fun updateDescription(description: String) {
        val deck = uiState.value.deck ?: return
        if (description == deck.description) return
        viewModelScope.launch {
            cardRepository.updateStack(deck.copy(description = description))
        }
    }

    fun setCoverImage(path: String?) {
        val deck = uiState.value.deck ?: return
        viewModelScope.launch {
            cardRepository.updateStack(deck.copy(coverImagePath = path))
        }
    }

    fun toggleArchived() {
        val deck = uiState.value.deck ?: return
        viewModelScope.launch {
            cardRepository.setDeckArchived(deck.id, !deck.isArchived)
        }
    }

    fun deleteDeck() {
        val id = deckId
        viewModelScope.launch {
            cardRepository.deleteStack(id)
        }
    }

    fun setSuitFilter(suit: String?) {
        _extras.update { it.copy(suitFilter = suit) }
    }

    fun toggleReorderMode() {
        _extras.update { it.copy(isReorderMode = !it.isReorderMode) }
    }

    fun saveReorder(orderedIds: List<Long>) {
        viewModelScope.launch {
            cardRepository.updateCardsSortOrder(orderedIds)
            _extras.update { it.copy(isReorderMode = false) }
        }
    }

    fun deleteCard(cardId: Long) {
        viewModelScope.launch {
            cardRepository.deleteCard(cardId)
        }
    }

    /** Crea un tag con [name] y lo asigna a este mazo (si no hay uno con ese nombre ya). */
    fun addTag(name: String) {
        val deck = uiState.value.deck ?: return
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        if (deck.tags.any { it.name.equals(trimmed, ignoreCase = true) }) return
        viewModelScope.launch {
            val tag = getOrCreateTagUseCase(trimmed)
            val updatedDeck = deck.copy(
                tags = deck.tags + tag
            )
            // updateStack en lugar de saveStack: @Update no borra las cartas del mazo
            cardRepository.updateStack(updatedDeck)
        }
    }

    /** Desvincula el tag [tagId] de este mazo (no lo elimina globalmente). */
    fun removeTag(tagId: Long) {
        val deck = uiState.value.deck ?: return
        viewModelScope.launch {
            val updatedDeck = deck.copy(tags = deck.tags.filter { it.id != tagId })
            // updateStack en lugar de saveStack: @Update no borra las cartas del mazo
            cardRepository.updateStack(updatedDeck)
        }
    }
}
