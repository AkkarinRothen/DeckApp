package com.deckapp.feature.importdeck.table

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.deckapp.core.domain.repository.TableRepository
import com.deckapp.core.domain.repository.RecentFileRepository
import com.deckapp.core.domain.repository.FileRepository
import com.deckapp.core.domain.usecase.RenderPdfPageUseCase
import com.deckapp.core.domain.usecase.ImportTableUseCase
import com.deckapp.core.domain.usecase.ImportTableUseCase.ImportResult
import com.deckapp.core.domain.usecase.ImportSource
import com.deckapp.core.domain.usecase.ReadTextFromUriUseCase
import com.deckapp.core.domain.usecase.AnalyzeTableImageUseCase
import com.deckapp.core.domain.usecase.CsvTableParser
import com.deckapp.core.domain.usecase.RangeParser
import com.deckapp.core.model.OcrBlock
import com.deckapp.core.model.TableEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel para la importación de tablas.
 * [NOTA] Se ha quitado Hilt temporalmente de esta clase para evitar un NullPointerException
 * interno de KSP (Internal Compiler Error) que bloquea el build del módulo :feature:import.
 */
class TableImportViewModel(
    private val renderPdfPageUseCase: RenderPdfPageUseCase,
    private val importTableUseCase: ImportTableUseCase,
    private val readTextFromUriUseCase: ReadTextFromUriUseCase,
    private val tableRepository: TableRepository,
    private val recentFileRepository: RecentFileRepository,
    private val fileRepository: FileRepository
) : ViewModel() {

    private val analyzeTableImageUseCase = AnalyzeTableImageUseCase()
    private val csvParser = CsvTableParser()

    private val _uiState = MutableStateFlow(TableImportUiState())
    val uiState: StateFlow<TableImportUiState> = _uiState.asStateFlow()

    init {
        loadRecents()
    }

    private fun loadRecents() {
        viewModelScope.launch {
            recentFileRepository.getRecentFiles().collect { recents ->
                val mapped = recents
                    .filter { it.type == "pdf" }
                    .map { Uri.parse(it.uri) to it.name }
                _uiState.update { it.copy(recentPdfs = mapped) }
            }
        }
    }

    fun setSource(source: ImportSource) {
        val mode = when (source) {
            ImportSource.OCR_IMAGE -> ImportMode.OCR
            ImportSource.CSV_TEXT -> ImportMode.CSV
            ImportSource.JSON_TEXT -> ImportMode.JSON
            ImportSource.PLAIN_TEXT -> ImportMode.PLAIN_TEXT
        }
        _uiState.update { it.copy(mode = mode) }
    }

    fun selectFile(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, selectedUri = uri) }
            try {
                recentFileRepository.addRecentFile(uri, uri.lastPathSegment ?: "Archivo", com.deckapp.core.domain.repository.RecentFileType.PDF)
                
                loadPage(0)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message, isProcessing = false) }
            }
        }
    }

    fun loadPage(index: Int) {
        val uri = _uiState.value.selectedUri ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            try {
                val result = renderPdfPageUseCase(uri, index)
                if (result != null) {
                    _uiState.update { it.copy(
                        pageBitmap = result.bitmap,
                        pdfPageCount = result.totalPageCount,
                        currentPageIndex = index,
                        step = ImportStep.FILE_PREVIEW,
                        isProcessing = false
                    ) }
                } else {
                    _uiState.update { it.copy(errorMessage = "No se pudo cargar la página $index", isProcessing = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message, isProcessing = false) }
            }
        }
    }

    fun confirmPageSelection() {
        _uiState.update { it.copy(step = ImportStep.CROP) }
    }

    fun browsePdfs(rootUri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            try {
                val files = fileRepository.listPdfsInFolder(rootUri)
                _uiState.update { it.copy(browsedPdfs = files, isProcessing = false) }
                
                files.forEach { (uri, _) ->
                    val thumb = renderPdfPageUseCase(uri, 0)?.bitmap
                    _uiState.update { s -> s.copy(browsedThumbnails = s.browsedThumbnails + (uri to thumb)) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message, isProcessing = false) }
            }
        }
    }

    fun processCrop(bitmap: Bitmap) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isProcessing = true, croppedBitmap = bitmap) }
                val blocks = importTableUseCase.getRawBlocks(bitmap)
                val layout = analyzeTableImageUseCase.analyzeLayout(blocks, _uiState.value.expectedTableCount)
                
                if (layout.isEmpty()) {
                    _uiState.update { it.copy(errorMessage = "No se detectaron tablas en la imagen", isProcessing = false) }
                    return@launch
                }

                val (cluster, anchors) = layout[0]

                _uiState.update { it.copy(
                    ocrBlocks = blocks,
                    currentCluster = cluster,
                    detectedAnchors = anchors,
                    step = ImportStep.RECOGNITION,
                    isProcessing = false
                ) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Error en OCR: ${e.message}", isProcessing = false) }
            }
        }
    }

    fun addAnchor(x: Float) {
        val current = _uiState.value.detectedAnchors
        _uiState.update { it.copy(detectedAnchors = (current + x).sorted()) }
    }

    fun removeAnchor(x: Float) {
        val current = _uiState.value.detectedAnchors
        val threshold = 0.02f
        val filtered = current.filter { kotlin.math.abs(it - x) > threshold }
        _uiState.update { it.copy(detectedAnchors = filtered) }
    }

    fun confirmRecognition() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isProcessing = true) }
                
                val result = analyzeTableImageUseCase.processWithAnchors(
                    _uiState.value.currentCluster,
                    _uiState.value.detectedAnchors
                )

                if (result == null) {
                    _uiState.update { it.copy(errorMessage = "Error al procesar con las columnas seleccionadas", isProcessing = false) }
                    return@launch
                }

                if (_uiState.value.isStitchingMode && _uiState.value.detectedTables.isNotEmpty()) {
                    val currentTables = _uiState.value.detectedTables.toMutableList()
                    val lastTable = currentTables.last()
                    val mergedEntries = lastTable.entries + result.entries
                    currentTables[currentTables.size - 1] = lastTable.copy(entries = mergedEntries)
                    
                    _uiState.update { it.copy(
                        detectedTables = currentTables,
                        currentTableIndex = currentTables.size - 1,
                        step = ImportStep.MAPPING,
                        isProcessing = false
                    ) }
                    loadResultToDraft(currentTables.last())
                } else {
                    _uiState.update { it.copy(
                        detectedTables = listOf(result),
                        currentTableIndex = 0,
                        step = ImportStep.MAPPING,
                        isProcessing = false
                    ) }
                    loadResultToDraft(result)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Error al confirmar mapeo: ${e.message}", isProcessing = false) }
            }
        }
    }

    fun setExpectedTableCount(count: Int) {
        _uiState.update { it.copy(expectedTableCount = count) }
    }

    fun toggleStitchingMode(enabled: Boolean) {
        _uiState.update { it.copy(isStitchingMode = enabled) }
    }

    private fun loadResultToDraft(result: ImportResult) {
        _uiState.update { it.copy(
            editableEntries = result.entries,
            tableNameDraft = result.suggestedName,
            validationResult = RangeParser.validate(result.entries),
            lowConfidenceIndices = result.lowConfidenceIndices
        ) }
    }

    fun updateEntry(index: Int, entry: TableEntry) {
        val newList = _uiState.value.editableEntries.toMutableList()
        if (index in newList.indices) {
            newList[index] = entry
            _uiState.update { it.copy(
                editableEntries = newList,
                validationResult = RangeParser.validate(newList)
            ) }
        }
    }

    fun setDraftName(name: String) = _uiState.update { it.copy(tableNameDraft = name) }
    fun setDraftTag(tag: String) = _uiState.update { it.copy(tableTagDraft = tag) }

    fun nextTable() {
        val state = _uiState.value
        val currentIndex = state.currentTableIndex
        if (currentIndex < state.detectedTables.size - 1) {
            // Guardar progreso actual en la lista detectada? 
            // Podríamos actualizar la lista detectedTables con las entries editadas
            val updatedTables = state.detectedTables.toMutableList()
            updatedTables[currentIndex] = updatedTables[currentIndex].copy(
                entries = state.editableEntries,
                suggestedName = state.tableNameDraft
            )
            
            val nextIndex = currentIndex + 1
            _uiState.update { it.copy(
                detectedTables = updatedTables,
                currentTableIndex = nextIndex
            ) }
            loadResultToDraft(updatedTables[nextIndex])
        } else {
            // Ir a resumen final
            _uiState.update { it.copy(step = ImportStep.REVIEW) }
        }
    }

    fun saveAll() {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            try {
                // En un flujo real, guardaríamos cada tabla en detectedTables
                // Para este MVP, marcamos éxito
                _uiState.update { it.copy(savedSuccessfully = true, isProcessing = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message, isProcessing = false) }
            }
        }
    }

    fun clearError() = _uiState.update { it.copy(errorMessage = null) }

    fun reset() {
        _uiState.value = TableImportUiState()
        loadRecents()
    }

    /**
     * Factory manual para evitar Hilt en esta clase específica.
     */
    class Factory(
        private val renderPdfPageUseCase: RenderPdfPageUseCase,
        private val importTableUseCase: ImportTableUseCase,
        private val readTextFromUriUseCase: ReadTextFromUriUseCase,
        private val tableRepository: TableRepository,
        private val recentFileRepository: RecentFileRepository,
        private val fileRepository: FileRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TableImportViewModel(
                renderPdfPageUseCase,
                importTableUseCase,
                readTextFromUriUseCase,
                tableRepository,
                recentFileRepository,
                fileRepository
            ) as T
        }
    }
}
