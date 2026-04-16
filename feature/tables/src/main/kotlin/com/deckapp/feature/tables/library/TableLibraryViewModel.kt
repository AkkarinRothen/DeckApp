package com.deckapp.feature.tables.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deckapp.core.domain.repository.CardRepository
import com.deckapp.core.domain.repository.TableRepository
import com.deckapp.core.domain.usecase.RollTableUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TableLibraryViewModel @Inject constructor(
    private val tableRepository: TableRepository,
    private val cardRepository: CardRepository,
    private val rollTableUseCase: RollTableUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(TableLibraryUiState())
    val uiState: StateFlow<TableLibraryUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        combine(
            tableRepository.getAllTables(),
            cardRepository.getAllTags(),
            _uiState.map { it.searchQuery }.distinctUntilChanged(),
            _uiState.map { it.selectedTagIds }.distinctUntilChanged()
        ) { tables, tags, query, selectedTagIds ->
            val filtered = tables.filter { table ->
                val matchesQuery = table.name.contains(query, ignoreCase = true) ||
                        table.description.contains(query, ignoreCase = true)
                val matchesTags = selectedTagIds.isEmpty() || 
                        table.tags.any { it.id in selectedTagIds }
                matchesQuery && matchesTags
            }.sortedWith(compareByDescending<com.deckapp.core.model.RandomTable> { it.isPinned }.thenByDescending { it.createdAt })
 
            _uiState.value.copy(
                tables = tables,
                filteredTables = filtered,
                allTags = tags,
                isLoading = false
            )
        }.onEach { newState ->
            _uiState.update { newState }
        }.launchIn(viewModelScope)
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

    fun clearTagFilters() {
        _uiState.update { it.copy(selectedTagIds = emptySet()) }
    }
 
    fun clearFilters() {
        _uiState.update { it.copy(searchQuery = "", selectedTagIds = emptySet()) }
    }
 
    // --- Modo de Selección ---
 
    fun toggleTableSelection(tableId: Long) {
        _uiState.update { state ->
            val current = state.selectedTableIds
            val updated = if (tableId in current) current - tableId else current + tableId
            state.copy(selectedTableIds = updated)
        }
    }
 
    fun clearSelection() {
        _uiState.update { it.copy(selectedTableIds = emptySet()) }
    }

    fun togglePin(tableId: Long, isPinned: Boolean) {
        viewModelScope.launch {
            tableRepository.updatePinnedState(tableId, !isPinned)
        }
    }

    fun deleteTable(tableId: Long) {
        viewModelScope.launch {
            tableRepository.deleteTable(tableId)
            _uiState.update { it.copy(snackbarMessage = "Tabla eliminada") }
        }
    }

    fun quickRoll(tableId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isRolling = true) }
            val table = tableRepository.getTableWithEntries(tableId) ?: return@launch
            val result = rollTableUseCase(tableId, sessionId = null)
            _uiState.update { it.copy(
                activeTable = table,
                activeRollResult = result,
                isRolling = false
            ) }
        }
    }

    fun clearRollResult() {
        _uiState.update { it.copy(activeTable = null, activeRollResult = null) }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun duplicateTable(tableId: Long) {
        viewModelScope.launch {
            val table = tableRepository.getTableWithEntries(tableId) ?: return@launch
            val newTable = table.copy(
                id = 0,
                name = "${table.name} (Copia)",
                isPinned = false,
                createdAt = System.currentTimeMillis()
            )
            tableRepository.saveTable(newTable)
            _uiState.update { it.copy(snackbarMessage = "Tabla duplicada") }
        }
    }
 
    // --- Operaciones Masivas ---
 
    fun bulkDelete() {
        val ids = _uiState.value.selectedTableIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            tableRepository.bulkDeleteTables(ids)
            _uiState.update { it.copy(selectedTableIds = emptySet(), snackbarMessage = "Eliminadas ${ids.size} tablas") }
        }
    }
 
    fun bulkUpdatePinned(pinned: Boolean) {
        val ids = _uiState.value.selectedTableIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            tableRepository.bulkUpdatePinnedState(ids, pinned)
            _uiState.update { it.copy(selectedTableIds = emptySet(), snackbarMessage = "Se actualizaron ${ids.size} tablas") }
        }
    }
 
    fun bulkAddTag(tagId: Long) {
        val ids = _uiState.value.selectedTableIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            tableRepository.bulkAddTagToTables(ids, tagId)
            _uiState.update { it.copy(selectedTableIds = emptySet(), snackbarMessage = "Etiqueta añadida a ${ids.size} tablas") }
        }
    }
}
