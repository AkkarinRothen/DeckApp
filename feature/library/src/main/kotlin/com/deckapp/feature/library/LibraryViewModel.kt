package com.deckapp.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deckapp.core.domain.repository.CardRepository
import com.deckapp.core.domain.repository.FileRepository
import com.deckapp.core.domain.usecase.DuplicateDeckUseCase
import com.deckapp.core.domain.usecase.MergeDecksUseCase
import com.deckapp.core.model.CardStack
import com.deckapp.core.model.Tag
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LibraryUiState(
    val allDecks: List<CardStack> = emptyList(),
    val archivedDecks: List<CardStack> = emptyList(),
    val allTags: List<Tag> = emptyList(),
    val searchQuery: String = "",
    val selectedTagIds: Set<Long> = emptySet(),
    val deckCardCounts: Map<Long, Int> = emptyMap(),
    val isLoading: Boolean = true,
    val showArchived: Boolean = false,
    val snackbarMessage: String? = null,
    val mergeSourceDeckId: Long? = null,
    val selectedDeckIds: Set<Long> = emptySet()
) {
    /** Mazos que se muestran según el toggle showArchived. */
    val displayedDecks: List<CardStack>
        get() = if (showArchived) archivedDecks else allDecks

    /** Mazos filtrados por búsqueda y tags sobre el conjunto visible. */
    val filteredDecks: List<CardStack>
        get() = displayedDecks.filter { deck ->
            val matchesSearch = searchQuery.isBlank() ||
                deck.name.contains(searchQuery, ignoreCase = true)
            val matchesTags = selectedTagIds.isEmpty() ||
                deck.tags.any { it.id in selectedTagIds }
            matchesSearch && matchesTags
        }

    /** Mazos disponibles como destino de fusión (excluye el origen). */
    val mergeCandidates: List<CardStack>
        get() = if (mergeSourceDeckId == null) emptyList()
                else allDecks.filter { it.id != mergeSourceDeckId }
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val cardRepository: CardRepository,
    private val fileRepository: FileRepository,
    private val duplicateDeckUseCase: DuplicateDeckUseCase,
    private val mergeDecksUseCase: MergeDecksUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    init {
        // Decks activos + archivados + tags
        viewModelScope.launch {
            combine(
                cardRepository.getAllDecks(),
                cardRepository.getArchivedDecks(),
                cardRepository.getAllTags()
            ) { decks, archived, tags ->
                Triple(decks, archived, tags)
            }.collect { (decks, archived, tags) ->
                _uiState.update { it.copy(allDecks = decks, archivedDecks = archived, allTags = tags, isLoading = false) }
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

    // --- Modo de Selección ---

    fun toggleDeckSelection(deckId: Long) {
        _uiState.update { state ->
            val current = state.selectedDeckIds
            val updated = if (deckId in current) current - deckId else current + deckId
            state.copy(selectedDeckIds = updated)
        }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedDeckIds = emptySet()) }
    }

    /**
     * Elimina el mazo y todas sus imágenes del almacenamiento interno.
     * Room elimina en cascada: cartas → caras → cross-refs de tags.
     */
    fun toggleShowArchived() {
        _uiState.update { it.copy(showArchived = !it.showArchived, searchQuery = "", selectedTagIds = emptySet()) }
    }

    fun archiveDeck(deckId: Long, archive: Boolean) {
        val deckName = (_uiState.value.allDecks + _uiState.value.archivedDecks).find { it.id == deckId }?.name ?: ""
        val msg = if (archive) "\"$deckName\" archivado" else "\"$deckName\" restaurado"
        viewModelScope.launch {
            cardRepository.setDeckArchived(deckId, archive)
            _uiState.update { it.copy(snackbarMessage = msg) }
        }
    }

    fun deleteDeck(deckId: Long) {
        viewModelScope.launch {
            fileRepository.deleteImagesForDeck(deckId)
            cardRepository.deleteStack(deckId)
        }
    }

    fun duplicateDeck(deckId: Long) {
        viewModelScope.launch {
            val deckName = _uiState.value.allDecks.find { it.id == deckId }?.name ?: ""
            duplicateDeckUseCase(deckId).fold(
                onSuccess = { _uiState.update { it.copy(snackbarMessage = "\"$deckName (copia)\" creado") } },
                onFailure = { _uiState.update { it.copy(snackbarMessage = "Error al duplicar el mazo") } }
            )
        }
    }

    fun startMerge(sourceDeckId: Long) {
        _uiState.update { it.copy(mergeSourceDeckId = sourceDeckId) }
    }

    fun confirmMerge(targetDeckId: Long) {
        val sourceId = _uiState.value.mergeSourceDeckId ?: return
        val sourceName = _uiState.value.allDecks.find { it.id == sourceId }?.name ?: ""
        val targetName = _uiState.value.allDecks.find { it.id == targetDeckId }?.name ?: ""
        _uiState.update { it.copy(mergeSourceDeckId = null) }
        viewModelScope.launch {
            mergeDecksUseCase(sourceId, targetDeckId).fold(
                onSuccess = { _uiState.update { it.copy(snackbarMessage = "\"$sourceName\" fusionado en \"$targetName\"") } },
                onFailure = { _uiState.update { it.copy(snackbarMessage = "Error al fusionar los mazos") } }
            )
        }
    }

    fun cancelMerge() {
        _uiState.update { it.copy(mergeSourceDeckId = null) }
    }

    // --- Operaciones Masivas ---

    fun bulkArchive(archive: Boolean) {
        val ids = _uiState.value.selectedDeckIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            cardRepository.bulkArchiveDecks(ids, archive)
            _uiState.update { it.copy(selectedDeckIds = emptySet(), snackbarMessage = "Se procesaron ${ids.size} mazos") }
        }
    }

    fun bulkDelete() {
        val ids = _uiState.value.selectedDeckIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            // Eliminar imágenes físicas primero
            ids.forEach { fileRepository.deleteImagesForDeck(it) }
            cardRepository.bulkDeleteDecks(ids)
            _uiState.update { it.copy(selectedDeckIds = emptySet(), snackbarMessage = "Eliminados ${ids.size} mazos") }
        }
    }

    fun bulkAddTag(tagId: Long) {
        val ids = _uiState.value.selectedDeckIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            cardRepository.bulkAddTagToStacks(ids, tagId)
            _uiState.update { it.copy(selectedDeckIds = emptySet(), snackbarMessage = "Etiqueta añadida a ${ids.size} mazos") }
        }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }
}
