package com.deckapp.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deckapp.core.domain.repository.CardRepository
import com.deckapp.core.domain.repository.CollectionRepository
import com.deckapp.core.domain.repository.FileRepository
import com.deckapp.core.domain.repository.TableRepository
import com.deckapp.core.domain.usecase.*
import com.deckapp.core.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    val pendingDeleteName: String? = null,  // non-null = hay un borrado pendiente con undo disponible
    val mergeSourceDeckId: Long? = null,
    val selectedDeckIds: Set<Long> = emptySet(),
    val searchResults: List<SearchMatch> = emptyList(),
    val allTables: List<RandomTable> = emptyList(),
    val allCollections: List<DeckCollection> = emptyList(),
    val activeCollectionId: Long? = null,
    val isLoadingCollections: Boolean = true
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
    private val tableRepository: TableRepository,
    private val collectionRepository: CollectionRepository,
    private val duplicateDeckUseCase: DuplicateDeckUseCase,
    private val mergeDecksUseCase: MergeDecksUseCase,
    private val globalSearchUseCase: GlobalSearchUseCase,
    private val getCollectionsUseCase: GetCollectionsUseCase,
    private val manageCollectionResourceUseCase: ManageCollectionResourceUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    /** Job del borrado diferido en curso. Se cancela si el usuario toca "Deshacer". */
    private var pendingDeleteJob: Job? = null

    init {
        // Observador central para Mazos y Tags, reaccionando a activeCollectionId
        viewModelScope.launch {
            _uiState.map { it.activeCollectionId }
                .distinctUntilChanged()
                .flatMapLatest { colId ->
                    if (colId == null) {
                        // Modo Global: Decks activos + archivados + todos los tags
                        combine(
                            cardRepository.getAllDecks(),
                            cardRepository.getArchivedDecks(),
                            cardRepository.getAllTags()
                        ) { decks, archived, tags -> Triple(decks, archived, tags) }
                    } else {
                        // Modo Colección: Solo decks en la colección + archivados globales (o podrías filtrarlos también)
                        combine(
                            collectionRepository.getDecksInCollection(colId),
                            cardRepository.getArchivedDecks(), // Mantenemos archivados globales por ahora
                            cardRepository.getAllTags()
                        ) { decks, archived, tags -> Triple(decks, archived, tags) }
                    }
                }.collect { (decks, archived, tags) ->
                    _uiState.update { it.copy(allDecks = decks, archivedDecks = archived, allTags = tags, isLoading = false) }
                }
        }

        // Carga de Tablas y Colecciones, reaccionando a activeCollectionId para las tablas
        viewModelScope.launch {
            val tablesFlow = _uiState.map { it.activeCollectionId }
                .distinctUntilChanged()
                .flatMapLatest { colId ->
                    if (colId == null) tableRepository.getAllTables()
                    else collectionRepository.getTablesInCollection(colId)
                }

            combine(
                tablesFlow,
                getCollectionsUseCase()
            ) { tables, collections -> tables to collections }
                .collect { (tables, collections) ->
                    _uiState.update { it.copy(allTables = tables, allCollections = collections, isLoadingCollections = false) }
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

        // Búsqueda Global reactiva
        @OptIn(kotlinx.coroutines.FlowPreview::class)
        viewModelScope.launch {
            _uiState.map { it.searchQuery }
                .distinctUntilChanged()
                .debounce(300)
                .flatMapLatest { query ->
                    globalSearchUseCase(query)
                }
                .collect { matches ->
                    _uiState.update { it.copy(searchResults = matches) }
                }
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

    /**
     * Programa el borrado del mazo con una ventana de 5 segundos para deshacer.
     * El borrado real (imágenes + Room) ocurre después del delay si no se cancela.
     * El mazo sigue visible en la lista durante ese período.
     */
    fun scheduleDeletion(deckId: Long) {
        val deck = (_uiState.value.allDecks + _uiState.value.archivedDecks).find { it.id == deckId }
            ?: return
        pendingDeleteJob?.cancel()
        _uiState.update { it.copy(pendingDeleteName = deck.name) }
        pendingDeleteJob = viewModelScope.launch {
            delay(5_000)
            fileRepository.deleteImagesForDeck(deckId)
            cardRepository.deleteStack(deckId)
            _uiState.update { it.copy(pendingDeleteName = null) }
        }
    }

    /** Cancela el borrado programado — el mazo se mantiene. */
    fun undoDeletion() {
        pendingDeleteJob?.cancel()
        pendingDeleteJob = null
        _uiState.update { it.copy(pendingDeleteName = null) }
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
        addTagToDecks(_uiState.value.selectedDeckIds.toList(), tagId)
    }

    fun addTagToDecks(ids: List<Long>, tagId: Long) {
        if (ids.isEmpty()) return
        viewModelScope.launch {
            cardRepository.bulkAddTagToStacks(ids, tagId)
            _uiState.update { it.copy(
                selectedDeckIds = emptySet(),
                selectedTagIds = it.selectedTagIds + tagId,
                snackbarMessage = "Etiqueta añadida y filtro aplicado"
            ) }
        }
    }

    fun createAndAddTag(ids: List<Long>, name: String) {
        if (ids.isEmpty() || name.isBlank()) return
        viewModelScope.launch {
            // Color por defecto (puedes randomizarlo o usar uno fijo del tema)
            val defaultColor = 0xFF9C27B0.toInt() // Púrpura
            val newTagId = cardRepository.saveTag(Tag(name = name, color = defaultColor))
            cardRepository.bulkAddTagToStacks(ids, newTagId)
            _uiState.update { it.copy(
                selectedDeckIds = emptySet(),
                selectedTagIds = it.selectedTagIds + newTagId,
                snackbarMessage = "Nueva etiqueta \"$name\" creada, añadida y filtrada"
            ) }
        }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    // --- Gestión de Colecciones ---

    fun createCollection(name: String, color: Int, icon: CollectionIcon) {
        viewModelScope.launch {
            val newCollection = DeckCollection(name = name, color = color, icon = icon)
            collectionRepository.saveCollection(newCollection)
            _uiState.update { it.copy(snackbarMessage = "Colección \"$name\" creada") }
        }
    }

    fun deleteCollection(id: Long) {
        viewModelScope.launch {
            collectionRepository.deleteCollection(id)
            _uiState.update { it.copy(snackbarMessage = "Colección eliminada") }
        }
    }

    fun addResourceToCollection(collectionId: Long, resourceId: Long, type: SearchResultType) {
        viewModelScope.launch {
            manageCollectionResourceUseCase.add(collectionId, resourceId, type)
            _uiState.update { it.copy(snackbarMessage = "Añadido al Baúl") }
        }
    }

    fun setActiveCollection(id: Long?) {
        _uiState.update { it.copy(activeCollectionId = id) }
    }
}
