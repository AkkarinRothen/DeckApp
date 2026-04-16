package com.deckapp.feature.tables

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deckapp.core.domain.repository.TableRepository
import com.deckapp.core.domain.repository.SessionRepository
import com.deckapp.core.domain.repository.CardRepository
import com.deckapp.core.domain.usecase.ExportTableUseCase
import com.deckapp.core.domain.usecase.RollTableUseCase
import com.deckapp.core.model.RandomTable
import com.deckapp.core.model.Tag
import com.deckapp.core.model.TableRollResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TablesUiState(
    val tables: List<RandomTable> = emptyList(),
    val filteredTables: List<RandomTable> = emptyList(),
    val allTags: List<Tag> = emptyList(),
    val selectedTagIds: Set<Long> = emptySet(),
    val selectedTableIds: Set<Long> = emptySet(),
    val searchQuery: String = "",
    val activeTable: RandomTable? = null,
    val sessionTableIds: Set<Long> = emptySet(),
    val showAllTables: Boolean = true,
    val lastResult: TableRollResult? = null,
    val recentResults: List<TableRollResult> = emptyList(),
    val isLoading: Boolean = true,
    val isRolling: Boolean = false,
    val snackbarMessage: String? = null
)

@HiltViewModel
class TablesViewModel @Inject constructor(
    private val tableRepository: TableRepository,
    private val sessionRepository: SessionRepository,
    private val cardRepository: CardRepository,
    private val rollTableUseCase: RollTableUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(TablesUiState())
    val uiState: StateFlow<TablesUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        // Combinamos todos los flujos para una reactividad pura y eficiente
        kotlinx.coroutines.flow.combine(
            tableRepository.getAllTables(),
            cardRepository.getAllTags(),
            _uiState.map { it.searchQuery }.distinctUntilChanged(),
            _uiState.map { it.selectedTagIds }.distinctUntilChanged(),
            _uiState.map { it.showAllTables }.distinctUntilChanged(),
            _uiState.map { it.sessionTableIds }.distinctUntilChanged()
        ) { args: Array<Any?> ->
            @Suppress("UNCHECKED_CAST")
            val tables = args[0] as List<RandomTable>
            @Suppress("UNCHECKED_CAST")
            val tags = args[1] as List<Tag>
            val query = args[2] as String
            @Suppress("UNCHECKED_CAST")
            val selectedTagIds = args[3] as Set<Long>
            val showAll = args[4] as Boolean
            @Suppress("UNCHECKED_CAST")
            val sessionTableIds = args[5] as Set<Long>

            val filtered = tables.filter { table ->
                val matchesSession = showAll || table.id in sessionTableIds
                val matchesQuery = table.name.contains(query, ignoreCase = true) ||
                        table.description.contains(query, ignoreCase = true)
                val matchesTags = selectedTagIds.isEmpty() || 
                        table.tags.any { it.id in selectedTagIds }
                matchesSession && matchesQuery && matchesTags
            }.sortedWith(compareByDescending<RandomTable> { it.isPinned }.thenBy { it.name })

            _uiState.value.copy(
                tables = tables,
                filteredTables = filtered,
                allTags = tags,
                isLoading = false
            )
        }.onEach { newState ->
            _uiState.value = newState
        }.launchIn(viewModelScope)
    }

    fun setSession(sessionId: Long?) {
        if (sessionId == null) {
            _uiState.update { it.copy(sessionTableIds = emptySet(), showAllTables = true) }
            return
        }
        /*
        viewModelScope.launch {
            sessionRepository.getTablesForSession(sessionId).collect { tables ->
                _uiState.update { it.copy(
                    sessionTableIds = tables.map { t -> t.id }.toSet(),
                    showAllTables = false
                ) }
            }
        }
        */
    }

    fun setShowAllTables(show: Boolean) = _uiState.update { it.copy(showAllTables = show) }

    @Deprecated("Utilizar loadData() reactivo", ReplaceWith("loadData()"))
    private fun loadTables() {}

    fun filteredTables(): List<RandomTable> = _uiState.value.filteredTables

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

    fun bulkAddTag(tagId: Long) {
        val ids = _uiState.value.selectedTableIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            tableRepository.bulkAddTagToTables(ids, tagId)
            _uiState.update { it.copy(selectedTableIds = emptySet(), snackbarMessage = "Etiqueta añadida a ${ids.size} tablas") }
        }
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

    fun deleteTable(tableId: Long) {
        viewModelScope.launch {
            tableRepository.deleteTable(tableId)
            _uiState.update { it.copy(snackbarMessage = "Tabla eliminada") }
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
