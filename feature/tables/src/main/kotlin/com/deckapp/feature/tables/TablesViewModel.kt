package com.deckapp.feature.tables

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deckapp.core.domain.repository.TableRepository
import com.deckapp.core.domain.usecase.RollTableUseCase
import com.deckapp.core.model.RandomTable
import com.deckapp.core.model.TableEntry
import com.deckapp.core.model.TableRollMode
import com.deckapp.core.model.TableRollResult
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject

data class TablesUiState(
    val tables: List<RandomTable> = emptyList(),
    val categories: List<String> = emptyList(),
    val selectedCategory: String? = null,
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val activeTable: RandomTable? = null,          // tabla con BottomSheet abierto
    val lastResult: TableRollResult? = null,
    val recentResults: List<TableRollResult> = emptyList(),
    val isRolling: Boolean = false,
    val snackbarMessage: String? = null
)

@HiltViewModel
class TablesViewModel @Inject constructor(
    private val tableRepository: TableRepository,
    private val rollTableUseCase: RollTableUseCase,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(TablesUiState())
    val uiState: StateFlow<TablesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch { loadBundledTablesIfNeeded() }

        viewModelScope.launch {
            combine(
                tableRepository.getAllTables(),
                tableRepository.getCategories()
            ) { tables, categories -> tables to categories }
                .collect { (tables, categories) ->
                    _uiState.update { it.copy(tables = tables, categories = categories, isLoading = false) }
                }
        }
    }

    // ── Filtros ───────────────────────────────────────────────────────────────

    fun setSearchQuery(query: String) = _uiState.update { it.copy(searchQuery = query) }

    fun selectCategory(category: String?) = _uiState.update { it.copy(selectedCategory = category) }

    fun filteredTables(): List<RandomTable> {
        val state = _uiState.value
        return state.tables.filter { table ->
            val matchesCategory = state.selectedCategory == null || table.category == state.selectedCategory
            val matchesSearch = state.searchQuery.isBlank() ||
                table.name.contains(state.searchQuery, ignoreCase = true) ||
                table.category.contains(state.searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }
    }

    // ── BottomSheet de detalle ─────────────────────────────────────────────────

    fun openTable(table: RandomTable) {
        _uiState.update { it.copy(activeTable = table, lastResult = null, recentResults = emptyList()) }
    }

    fun closeTable() = _uiState.update { it.copy(activeTable = null, lastResult = null, recentResults = emptyList()) }

    fun loadRecentResults(sessionId: Long?) {
        val tableId = _uiState.value.activeTable?.id ?: return
        if (sessionId == null) return
        viewModelScope.launch {
            tableRepository.getRecentResultsForTable(sessionId, tableId)
                .collect { results -> _uiState.update { it.copy(recentResults = results) } }
        }
    }

    // ── Tirada ────────────────────────────────────────────────────────────────

    fun rollTable(tableId: Long, sessionId: Long?) {
        _uiState.update { it.copy(isRolling = true) }
        viewModelScope.launch {
            val result = rollTableUseCase(tableId, sessionId)
            _uiState.update { it.copy(lastResult = result, isRolling = false) }
        }
    }

    // ── Editor ────────────────────────────────────────────────────────────────

    suspend fun getTableForEdit(tableId: Long): RandomTable? =
        tableRepository.getTableWithEntries(tableId)

    fun saveTable(table: RandomTable) {
        viewModelScope.launch {
            tableRepository.saveTable(table)
            _uiState.update { it.copy(snackbarMessage = "Tabla guardada") }
        }
    }

    fun deleteTable(tableId: Long) {
        viewModelScope.launch {
            tableRepository.deleteTable(tableId)
            _uiState.update { it.copy(snackbarMessage = "Tabla eliminada") }
        }
    }

    fun clearSnackbar() = _uiState.update { it.copy(snackbarMessage = null) }

    // ── Bundled tables ────────────────────────────────────────────────────────

    private suspend fun loadBundledTablesIfNeeded() {
        if (tableRepository.countBuiltInTables() > 0) return
        try {
            val json = context.assets.open("tables/bundled_tables.json")
                .bufferedReader().readText()
            val pack = bundledJson.decodeFromString(BundledTablePack.serializer(), json)
            pack.tables.forEach { bundled ->
                val table = RandomTable(
                    name = bundled.name,
                    description = bundled.description,
                    category = bundled.category,
                    rollFormula = bundled.rollFormula,
                    rollMode = runCatching { TableRollMode.valueOf(bundled.rollMode) }
                        .getOrDefault(TableRollMode.RANGE),
                    entries = bundled.entries.mapIndexed { idx, e ->
                        TableEntry(
                            minRoll = e.minRoll,
                            maxRoll = e.maxRoll,
                            weight = e.weight,
                            text = e.text,
                            subTableRef = e.subTableRef,
                            sortOrder = idx
                        )
                    },
                    isBuiltIn = true
                )
                tableRepository.saveTable(table)
            }
        } catch (e: Exception) {
            // Si no hay assets o hay error de parseo, seguimos sin tablas predefinidas
        }
    }

    companion object {
        private val bundledJson = Json { ignoreUnknownKeys = true }
    }
}

// ── Modelos de deserialización del bundle ─────────────────────────────────────

@Serializable
data class BundledTablePack(val tables: List<BundledTable>)

@Serializable
data class BundledTable(
    val name: String,
    val description: String = "",
    val category: String = "",
    val rollFormula: String = "1d6",
    val rollMode: String = "RANGE",
    val entries: List<BundledEntry>
)

@Serializable
data class BundledEntry(
    val minRoll: Int = 1,
    val maxRoll: Int = 1,
    val weight: Int = 1,
    val text: String,
    val subTableRef: String? = null
)
