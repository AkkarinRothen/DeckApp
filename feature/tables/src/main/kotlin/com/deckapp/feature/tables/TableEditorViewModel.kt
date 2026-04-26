package com.deckapp.feature.tables

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deckapp.core.domain.repository.TableRepository
import com.deckapp.core.domain.repository.CardRepository
import com.deckapp.core.domain.repository.SessionRepository
import com.deckapp.core.domain.usecase.RollTableUseCase
import com.deckapp.core.domain.usecase.DiceEvaluator
import com.deckapp.core.domain.usecase.ValidateTableUseCase
import com.deckapp.core.domain.usecase.GetOrCreateTagUseCase
import com.deckapp.core.model.RandomTable
import com.deckapp.core.model.Tag
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
    val tags: List<Tag> = emptyList(),
    val allTags: List<Tag> = emptyList(),
    val rollFormula: String = "1d6",
    val rollMode: TableRollMode = TableRollMode.RANGE,
    val entries: List<TableEntry> = emptyList(),
    val isBuiltIn: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isDirty: Boolean = false,
    val errorMessage: String? = null,
    val validationErrors: List<ValidateTableUseCase.TableValidationError> = emptyList(),
    val successMessage: String? = null,
    val previewResult: TableRollResult? = null,
    val isNewTable: Boolean = true,
    // Sub-tablas
    val availableTables: List<RandomTable> = emptyList(),
    val pickingEntryIndex: Int? = null
)

@HiltViewModel
class TableEditorViewModel @Inject constructor(
    private val tableRepository: TableRepository,
    private val cardRepository: CardRepository,
    private val sessionRepository: SessionRepository,
    private val rollTableUseCase: RollTableUseCase,
    private val validateTableUseCase: ValidateTableUseCase,
    private val getOrCreateTagUseCase: GetOrCreateTagUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val tableId: Long = savedStateHandle["tableId"] ?: -1L
    private val sessionId: Long? = savedStateHandle["sessionId"]

    private val _uiState = MutableStateFlow(TableEditorUiState(isNewTable = tableId == -1L))
    val uiState: StateFlow<TableEditorUiState> = _uiState.asStateFlow()

    init {
        loadAvailableTables()
        loadAllTags()
        if (tableId != -1L) {
            loadTable(tableId)
        } else {
            validate()
        }
    }

    private fun validate() {
        val table = buildTableFromState(tableId.coerceAtLeast(0))
        val report = validateTableUseCase(table)
        _uiState.update { it.copy(validationErrors = report.errors) }
    }

    private fun loadAllTags() {
        viewModelScope.launch {
            cardRepository.getAllTags().collect { tags ->
                _uiState.update { it.copy(allTags = tags) }
            }
        }
    }

    private fun loadAvailableTables() {
        viewModelScope.launch {
            tableRepository.getAllTables().collect { tables ->
                // Filtramos la tabla actual para no referenciarse a sí misma directamente en el selector
                _uiState.update { it.copy(availableTables = tables.filter { t -> t.id != tableId }) }
            }
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
                        tags = table.tags,
                        rollFormula = table.rollFormula,
                        rollMode = table.rollMode,
                        entries = table.entries,
                        isBuiltIn = table.isBuiltIn,
                        isLoading = false,
                        isNewTable = false
                    )
                }
                validate()
            } else {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Tabla no encontrada") }
            }
        }
    }

    fun setName(name: String) {
        _uiState.update { it.copy(name = name, isDirty = true) }
        validate()
    }
    fun setDescription(desc: String) {
        _uiState.update { it.copy(description = desc, isDirty = true) }
        validate()
    }
    
    fun toggleTag(tag: Tag) {
        _uiState.update { state ->
            val updatedTags = if (tag in state.tags) state.tags - tag else state.tags + tag
            state.copy(tags = updatedTags, isDirty = true)
        }
        validate()
    }

    fun createAndAddTag(tagName: String) {
        if (tagName.isBlank()) return
        viewModelScope.launch {
            try {
                val tag = getOrCreateTagUseCase(tagName)
                if (tag !in _uiState.value.tags) {
                    _uiState.update { it.copy(tags = it.tags + tag, isDirty = true) }
                    validate()
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Error al crear etiqueta") }
            }
        }
    }

    fun setRollFormula(formula: String) {
        _uiState.update { it.copy(rollFormula = formula, isDirty = true) }
        validate()
    }
    fun setRollMode(mode: TableRollMode) {
        _uiState.update { it.copy(rollMode = mode, isDirty = true) }
        validate()
    }

    fun addEntry() {
        val state = _uiState.value
        val newEntry = if (state.rollMode == TableRollMode.MACRO) {
            TableEntry(
                minRoll = 0,
                maxRoll = 0,
                text = "",
                sortOrder = state.entries.size
            )
        } else {
            val lastMax = state.entries.maxOfOrNull { it.maxRoll } ?: 0
            val range = DiceEvaluator.getRange(state.rollFormula)
            val maxPossible = range.second
            val newMin = (lastMax + 1).coerceAtMost(maxPossible)
            TableEntry(
                minRoll = newMin,
                maxRoll = newMin,
                text = "",
                sortOrder = state.entries.size
            )
        }
        _uiState.update { it.copy(entries = it.entries + newEntry, isDirty = true) }
        validate()
    }

    fun updateEntry(index: Int, entry: TableEntry) {
        val entries = _uiState.value.entries.toMutableList()
        if (index in entries.indices) {
            entries[index] = entry
            _uiState.update { it.copy(entries = entries, isDirty = true) }
            validate()
        }
    }

    fun removeEntry(index: Int) {
        val entries = _uiState.value.entries.toMutableList()
        if (index in entries.indices) {
            entries.removeAt(index)
            _uiState.update { it.copy(entries = entries, isDirty = true) }
            validate()
        }
    }

    // ── Gestión de Sub-tablas ─────────────────────────────────────────────────

    fun startPickingSubTable(index: Int) {
        _uiState.update { it.copy(pickingEntryIndex = index) }
    }

    fun cancelPicking() {
        _uiState.update { it.copy(pickingEntryIndex = null) }
    }

    fun linkSubTable(table: RandomTable) {
        val index = _uiState.value.pickingEntryIndex ?: return
        val entries = _uiState.value.entries.toMutableList()
        if (index in entries.indices) {
            entries[index] = entries[index].copy(
                subTableId = table.id,
                subTableRef = table.name
            )
            _uiState.update { it.copy(entries = entries, isDirty = true, pickingEntryIndex = null) }
            validate()
        }
    }

    fun unlinkSubTable(index: Int) {
        val entries = _uiState.value.entries.toMutableList()
        if (index in entries.indices) {
            entries[index] = entries[index].copy(
                subTableId = null,
                subTableRef = null
            )
            _uiState.update { it.copy(entries = entries, isDirty = true) }
            validate()
        }
    }

    fun previewRoll() {
        val state = _uiState.value
        if (state.entries.isEmpty()) return
        val table = buildTableFromState(tableId.coerceAtLeast(0))
        viewModelScope.launch {
            // Tiramos directamente sobre el objeto en memoria, sin persistir
            val result = rollTableUseCase.roll(table, sessionId = null, persistResult = false)
            _uiState.update { it.copy(previewResult = result) }
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
            try {
                val newId = tableRepository.saveTable(buildTableFromState(tableId.coerceAtLeast(0)))
                if (state.isNewTable && sessionId != null) {
                    sessionRepository.addTableToSession(sessionId, newId)
                }
                _uiState.update { it.copy(isSaving = false, isDirty = false, successMessage = "Tabla guardada") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, errorMessage = "Error al guardar: ${e.localizedMessage}") }
            }
        }
    }

    fun clearMessages() = _uiState.update { it.copy(errorMessage = null, successMessage = null) }

    private fun buildTableFromState(id: Long) = RandomTable(
        id = id,
        name = _uiState.value.name.trim(),
        description = _uiState.value.description.trim(),
        tags = _uiState.value.tags,
        rollFormula = _uiState.value.rollFormula.trim(),
        rollMode = _uiState.value.rollMode,
        entries = _uiState.value.entries,
        isBuiltIn = _uiState.value.isBuiltIn
    )
}
