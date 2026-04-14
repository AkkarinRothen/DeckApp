package com.deckapp.feature.tables

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deckapp.core.domain.repository.TableRepository
import com.deckapp.core.domain.usecase.RollTableUseCase
import com.deckapp.core.model.RandomTable
import com.deckapp.core.model.TableEntry
import com.deckapp.core.model.TableRollMode
import com.deckapp.core.model.TableRollResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TableEditorUiState(
    val name: String = "",
    val description: String = "",
    val category: String = "",
    val rollFormula: String = "1d6",
    val rollMode: TableRollMode = TableRollMode.RANGE,
    val entries: List<TableEntry> = emptyList(),
    val isBuiltIn: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isDirty: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val previewResult: TableRollResult? = null,
    val isNewTable: Boolean = true
)

@HiltViewModel
class TableEditorViewModel @Inject constructor(
    private val tableRepository: TableRepository,
    private val rollTableUseCase: RollTableUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val tableId: Long = savedStateHandle["tableId"] ?: -1L

    private val _uiState = MutableStateFlow(TableEditorUiState(isNewTable = tableId == -1L))
    val uiState: StateFlow<TableEditorUiState> = _uiState.asStateFlow()

    init {
        if (tableId != -1L) {
            loadTable(tableId)
        }
    }

    private fun loadTable(id: Long) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val table = tableRepository.getTableWithEntries(id)
            if (table != null) {
                _uiState.update { _ ->
                    TableEditorUiState(
                        name = table.name,
                        description = table.description,
                        category = table.category,
                        rollFormula = table.rollFormula,
                        rollMode = table.rollMode,
                        entries = table.entries,
                        isBuiltIn = table.isBuiltIn,
                        isLoading = false,
                        isNewTable = false
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Tabla no encontrada") }
            }
        }
    }

    fun setName(name: String) = _uiState.update { it.copy(name = name, isDirty = true) }
    fun setDescription(desc: String) = _uiState.update { it.copy(description = desc, isDirty = true) }
    fun setCategory(cat: String) = _uiState.update { it.copy(category = cat, isDirty = true) }
    fun setRollFormula(formula: String) = _uiState.update { it.copy(rollFormula = formula, isDirty = true) }
    fun setRollMode(mode: TableRollMode) = _uiState.update { it.copy(rollMode = mode, isDirty = true) }

    fun addEntry() {
        val state = _uiState.value
        val lastMax = state.entries.maxOfOrNull { it.maxRoll } ?: 0
        val (_, maxPossible) = RollTableUseCase.getDiceRange(state.rollFormula)
        val newMin = (lastMax + 1).coerceAtMost(maxPossible)
        val newEntry = TableEntry(
            minRoll = newMin,
            maxRoll = newMin,
            text = "",
            sortOrder = state.entries.size
        )
        _uiState.update { it.copy(entries = it.entries + newEntry, isDirty = true) }
    }

    fun updateEntry(index: Int, entry: TableEntry) {
        val entries = _uiState.value.entries.toMutableList()
        if (index in entries.indices) {
            entries[index] = entry
            _uiState.update { it.copy(entries = entries, isDirty = true) }
        }
    }

    fun removeEntry(index: Int) {
        val entries = _uiState.value.entries.toMutableList()
        if (index in entries.indices) {
            entries.removeAt(index)
            _uiState.update { it.copy(entries = entries, isDirty = true) }
        }
    }

    fun previewRoll() {
        val state = _uiState.value
        if (state.entries.isEmpty()) return
        val table = buildTableFromState(tableId.coerceAtLeast(0))
        viewModelScope.launch {
            tableRepository.saveTable(table).let { savedId ->
                val result = rollTableUseCase(savedId, sessionId = null)
                _uiState.update { it.copy(previewResult = result) }
            }
        }
    }

    fun clearPreview() = _uiState.update { it.copy(previewResult = null) }

    fun save() {
        val state = _uiState.value
        if (state.name.isBlank()) {
            _uiState.update { it.copy(errorMessage = "El nombre no puede estar vacío") }
            return
        }
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            tableRepository.saveTable(buildTableFromState(tableId.coerceAtLeast(0)))
            _uiState.update { it.copy(isSaving = false, isDirty = false, successMessage = "Tabla guardada") }
        }
    }

    fun clearMessages() = _uiState.update { it.copy(errorMessage = null, successMessage = null) }

    private fun buildTableFromState(id: Long) = RandomTable(
        id = id,
        name = _uiState.value.name.trim(),
        description = _uiState.value.description.trim(),
        category = _uiState.value.category.trim(),
        rollFormula = _uiState.value.rollFormula.trim(),
        rollMode = _uiState.value.rollMode,
        entries = _uiState.value.entries,
        isBuiltIn = _uiState.value.isBuiltIn
    )
}
