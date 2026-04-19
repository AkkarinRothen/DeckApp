package com.deckapp.feature.reference

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deckapp.core.data.util.CsvTableParser
import com.deckapp.core.domain.repository.AiReferenceRepository
import com.deckapp.core.domain.repository.ReferenceRepository
import com.deckapp.core.domain.repository.SettingsRepository
import com.deckapp.core.domain.usecase.MarkdownTableParser
import com.deckapp.core.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject

data class ReferenceTableEditorUiState(
    val id: Long = 0,
    val name: String = "",
    val description: String = "",
    val gameSystem: String = "General",
    val category: String = "General",
    val columns: List<ReferenceColumn> = emptyList(),
    val rows: List<ReferenceRow> = emptyList(),
    val isPinned: Boolean = false,
    val isSaving: Boolean = false,
    val isImportLoading: Boolean = false,
    val importPreviewData: ImportPreviewData? = null,
    val importError: String? = null,
    val error: String? = null,
    val distinctSystems: List<String> = emptyList()
)

@HiltViewModel
class ReferenceTableEditorViewModel @Inject constructor(
    private val referenceRepository: ReferenceRepository,
    private val aiReferenceRepository: AiReferenceRepository,
    private val settingsRepository: SettingsRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReferenceTableEditorUiState())
    val uiState: StateFlow<ReferenceTableEditorUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val systems = referenceRepository.getDistinctSystems().first()
            _uiState.update { it.copy(distinctSystems = systems) }
        }
    }

    fun loadTable(tableId: Long, prefilledSystem: String = "") {
        if (tableId == -1L) {
            _uiState.update { it.copy(
                gameSystem = if (prefilledSystem.isNotBlank()) prefilledSystem else "General",
                columns = listOf(ReferenceColumn(0, "Columna 1"))
            ) }
            return
        }

        viewModelScope.launch {
            val table = referenceRepository.getReferenceTableWithRows(tableId)
            if (table != null) {
                _uiState.update { it.copy(
                    id = table.id,
                    name = table.name,
                    description = table.description,
                    gameSystem = table.gameSystem,
                    category = table.category,
                    columns = table.columns,
                    rows = table.rows,
                    isPinned = table.isPinned
                ) }
            }
        }
    }

    fun onNameChanged(name: String) = _uiState.update { it.copy(name = name) }
    fun onDescriptionChanged(desc: String) = _uiState.update { it.copy(description = desc) }
    fun onSystemChanged(system: String) = _uiState.update { it.copy(gameSystem = system) }
    fun onCategoryChanged(category: String) = _uiState.update { it.copy(category = category) }

    fun addColumn() {
        _uiState.update { state ->
            val nextIndex = state.columns.size
            val newColumns = state.columns + ReferenceColumn(nextIndex, "Columna ${nextIndex + 1}")
            val newRows = state.rows.map { it.copy(cells = it.cells + "") }
            state.copy(columns = newColumns, rows = newRows)
        }
    }

    fun removeColumn(index: Int) {
        _uiState.update { state ->
            val newColumns = state.columns.filter { it.index != index }
                .mapIndexed { i, col -> col.copy(index = i) }
            val newRows = state.rows.map { row ->
                row.copy(cells = row.cells.filterIndexed { i, _ -> i != index })
            }
            state.copy(columns = newColumns, rows = newRows)
        }
    }

    fun updateColumnHeader(index: Int, header: String) {
        _uiState.update { state ->
            state.copy(columns = state.columns.map { 
                if (it.index == index) it.copy(header = header) else it 
            })
        }
    }

    fun addRow() {
        _uiState.update { state ->
            val newRow = ReferenceRow(
                tableId = state.id,
                cells = List(state.columns.size) { "" },
                sortOrder = state.rows.size
            )
            state.copy(rows = state.rows + newRow)
        }
    }

    fun removeRow(index: Int) {
        _uiState.update { state ->
            state.copy(rows = state.rows.filterIndexed { i, _ -> i != index })
        }
    }

    fun updateCell(rowIndex: Int, colIndex: Int, value: String) {
        _uiState.update { state ->
            val newRows = state.rows.toMutableList().apply {
                val row = this[rowIndex]
                val newCells = row.cells.toMutableList().apply { this[colIndex] = value }
                this[rowIndex] = row.copy(cells = newCells)
            }
            state.copy(rows = newRows)
        }
    }

    fun save() {
        val state = _uiState.value
        if (state.name.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val table = ReferenceTable(
                id = state.id,
                name = state.name,
                description = state.description,
                gameSystem = state.gameSystem,
                category = state.category,
                columns = state.columns,
                rows = state.rows,
                isPinned = state.isPinned
            )
            referenceRepository.saveReferenceTable(table)
            _uiState.update { it.copy(isSaving = false) }
        }
    }

    // --- Import ---

    fun prepareImportFromCsv(uriString: String) {
        viewModelScope.launch {
            try {
                val uri = Uri.parse(uriString)
                val inputStream = context.contentResolver.openInputStream(uri)
                val csvText = inputStream?.bufferedReader()?.use { it.readText() } ?: ""

                val preview = CsvTableParser.parseAllRows(csvText)
                _uiState.update { it.copy(importPreviewData = preview, importError = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(importError = "Error al leer CSV: ${e.message}") }
            }
        }
    }

    fun prepareImportFromMarkdown(text: String) {
        val preview = MarkdownTableParser.parseReference(text)
        _uiState.update { it.copy(importPreviewData = preview, importError = null) }
    }

    fun prepareImportFromImage(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isImportLoading = true, importError = null) }
            try {
                val apiKey = settingsRepository.getGeminiApiKey()
                if (apiKey.isBlank()) {
                    _uiState.update { it.copy(isImportLoading = false, importError = "API Key de Gemini no configurada") }
                    return@launch
                }

                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (bitmap == null) {
                    _uiState.update { it.copy(isImportLoading = false, importError = "No se pudo cargar la imagen") }
                    return@launch
                }

                val result = aiReferenceRepository.recognizeReferenceTableFromImage(bitmap, apiKey)
                val preview = ImportPreviewData(
                    headers = result.headers,
                    rows = result.rows,
                    source = ReferenceImportSource.OCR_IMAGE
                )
                _uiState.update { it.copy(importPreviewData = preview, isImportLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isImportLoading = false, importError = "Error de OCR: ${e.message}") }
            }
        }
    }

    fun applyImport(headers: List<String>, replace: Boolean) {
        val preview = _uiState.value.importPreviewData ?: return
        
        _uiState.update { state ->
            val newColumns = headers.mapIndexed { i, h -> ReferenceColumn(i, h) }
            val newRowsData = preview.rows.map { cells -> 
                ReferenceRow(cells = cells, tableId = state.id) 
            }

            val finalRows = if (replace) newRowsData else state.rows + newRowsData
            
            state.copy(
                columns = newColumns,
                rows = finalRows,
                importPreviewData = null
            )
        }
    }

    fun cancelImport() = _uiState.update { it.copy(importPreviewData = null, importError = null) }
}
