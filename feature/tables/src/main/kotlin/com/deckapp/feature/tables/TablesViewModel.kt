package com.deckapp.feature.tables

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deckapp.core.domain.repository.TableRepository
import com.deckapp.core.domain.usecase.ExportTableUseCase
import com.deckapp.core.domain.usecase.RollTableUseCase
import com.deckapp.core.model.RandomTable
import com.deckapp.core.model.TableRollResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TablesUiState(
    val tables: List<RandomTable> = emptyList(),
    val allTags: List<com.deckapp.core.model.Tag> = emptyList(),
    val selectedTagIds: Set<Long> = emptySet(),
    val selectedTableIds: Set<Long> = emptySet(),
    val searchQuery: String = "",
    val activeTable: RandomTable? = null,
    val lastResult: TableRollResult? = null,
    val recentResults: List<TableRollResult> = emptyList(),
    val isLoading: Boolean = true,
    val isRolling: Boolean = false,
    val snackbarMessage: String? = null
)

@HiltViewModel
class TablesViewModel @Inject constructor(
    private val tableRepository: TableRepository,
    private val cardRepository: com.deckapp.core.domain.repository.CardRepository,
    private val rollTableUseCase: RollTableUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(TablesUiState())
    val uiState: StateFlow<TablesUiState> = _uiState.asStateFlow()

    init {
        loadTables()
    }

    private fun loadTables() {
        viewModelScope.launch {
            tableRepository.getAllTables().collect { tables ->
                _uiState.update { it.copy(tables = tables, isLoading = false) }
            }
        }
        viewModelScope.launch {
            cardRepository.getAllTags().collect { tags ->
                _uiState.update { it.copy(allTags = tags) }
            }
        }
    }

    fun filteredTables(): List<RandomTable> {
        val state = _uiState.value
        return state.tables.filter { table ->
            (state.selectedTagIds.isEmpty() || table.tags.any { it.id in state.selectedTagIds }) &&
            (state.searchQuery.isBlank() || table.name.contains(state.searchQuery, ignoreCase = true))
        }.sortedWith(compareByDescending<RandomTable> { it.isPinned }.thenBy { it.name })
    }

    fun setSearchQuery(query: String) = _uiState.update { it.copy(searchQuery = query) }
    
    fun toggleTagFilter(tagId: Long) {
        _uiState.update { state ->
            val current = state.selectedTagIds
            val updated = if (tagId in current) current - tagId else current + tagId
            state.copy(selectedTagIds = updated)
        }
    }

    fun clearFilters() {
        _uiState.update { it.copy(selectedTagIds = emptySet(), searchQuery = "") }
    }

    fun toggleTableSelection(tableId: Long) {
        _uiState.update { state ->
            val current = state.selectedTableIds
            val updated = if (tableId in current) current - tableId else current + tableId
            state.copy(selectedTableIds = updated)
        }
    }

    fun clearSelection() = _uiState.update { it.copy(selectedTableIds = emptySet()) }
    fun openTable(table: RandomTable) = _uiState.update { it.copy(activeTable = table) }
    fun closeTable() = _uiState.update { it.copy(activeTable = null) }

    fun togglePin(table: RandomTable) {
        viewModelScope.launch {
            tableRepository.updatePinnedState(table.id, !table.isPinned)
        }
    }

    // --- Bulk Operations ---

    fun bulkDelete() {
        val ids = _uiState.value.selectedTableIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            tableRepository.bulkDeleteTables(ids)
            _uiState.update { it.copy(selectedTableIds = emptySet(), snackbarMessage = "Eliminadas ${ids.size} tablas") }
        }
    }

    fun bulkTogglePin(pin: Boolean) {
        val ids = _uiState.value.selectedTableIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            tableRepository.bulkUpdatePinnedState(ids, pin)
            _uiState.update { it.copy(selectedTableIds = emptySet()) }
        }
    }

    fun clearSnackbar() = _uiState.update { it.copy(snackbarMessage = null) }

    fun rollTable(tableId: Long, sessionId: Long?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isRolling = true) }
            try {
                val result = rollTableUseCase(tableId, sessionId)
                _uiState.update { it.copy(lastResult = result, isRolling = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isRolling = false) }
            }
        }
    }

    /** Devuelve el JSON de exportación de la tabla activa, o cadena vacía si no hay ninguna. */
    fun getExportJson(): String {
        val table = _uiState.value.activeTable ?: return ""
        return ExportTableUseCase.buildJson(table)
    }

    fun loadRecentResults(sessionId: Long) {
        val tableId = _uiState.value.activeTable?.id ?: return
        viewModelScope.launch {
            tableRepository.getRecentResultsForTable(sessionId, tableId).collect { results ->
                _uiState.update { it.copy(recentResults = results) }
            }
        }
    }
}
