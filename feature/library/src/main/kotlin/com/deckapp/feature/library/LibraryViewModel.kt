package com.deckapp.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deckapp.core.domain.repository.CardRepository
import com.deckapp.core.domain.repository.FileRepository
import com.deckapp.core.model.CardStack
import com.deckapp.core.model.Tag
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LibraryUiState(
    val allDecks: List<CardStack> = emptyList(),
    val allTags: List<Tag> = emptyList(),
    val searchQuery: String = "",
    val selectedTagIds: Set<Long> = emptySet(),
    val deckCardCounts: Map<Long, Int> = emptyMap(), // stackId → total de cartas
    val isLoading: Boolean = true
) {
    /** Mazos filtrados por búsqueda de nombre y por tags seleccionados. */
    val filteredDecks: List<CardStack>
        get() = allDecks.filter { deck ->
            val matchesSearch = searchQuery.isBlank() ||
                deck.name.contains(searchQuery, ignoreCase = true)
            val matchesTags = selectedTagIds.isEmpty() ||
                deck.tags.any { it.id in selectedTagIds }
            matchesSearch && matchesTags
        }
}

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val cardRepository: CardRepository,
    private val fileRepository: FileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    init {
        // Decks + tags
        viewModelScope.launch {
            combine(
                cardRepository.getAllDecks(),
                cardRepository.getAllTags()
            ) { decks, tags ->
                decks to tags
            }.collect { (decks, tags) ->
                _uiState.update { it.copy(allDecks = decks, allTags = tags, isLoading = false) }
            }
        }

        // Conteo total de cartas por mazo (para badge en portada)
        viewModelScope.launch {
            cardRepository.getAllDecks()
                .flatMapLatest { decks ->
                    if (decks.isEmpty()) flowOf(emptyMap())
                    else combine(
                        decks.map { d -> cardRepository.getTotalCardCount(d.id).map { d.id to it } }
                    ) { pairs -> pairs.toMap() }
                }
                .collect { counts -> _uiState.update { it.copy(deckCardCounts = counts) } }
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun toggleTagFilter(tagId: Long) {
        _uiState.update { state ->
            val current = state.selectedTagIds
            val updated = if (tagId in current) current - tagId else current + tagId
            state.copy(selectedTagIds = updated)
        }
    }

    fun clearFilters() {
        _uiState.update { it.copy(searchQuery = "", selectedTagIds = emptySet()) }
    }

    /**
     * Elimina el mazo y todas sus imágenes del almacenamiento interno.
     * Room elimina en cascada: cartas → caras → cross-refs de tags.
     */
    fun deleteDeck(deckId: Long) {
        viewModelScope.launch {
            fileRepository.deleteImagesForDeck(deckId)
            cardRepository.deleteStack(deckId)
        }
    }
}
