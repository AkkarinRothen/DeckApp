package com.deckapp.feature.importdeck.table

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.deckapp.core.domain.repository.AiStreamEvent
import com.deckapp.core.domain.repository.TableRepository
import com.deckapp.core.domain.repository.RecentFileRepository
import com.deckapp.core.domain.repository.FileRepository
import com.deckapp.core.domain.repository.RecentFileType
import com.deckapp.core.domain.repository.SettingsRepository
import com.deckapp.core.domain.usecase.RenderPdfPageUseCase
import com.deckapp.core.domain.usecase.ImportTableUseCase
import com.deckapp.core.domain.usecase.ImportResult
import com.deckapp.core.model.TableImportSource
import com.deckapp.core.domain.usecase.TextImportParams
import com.deckapp.core.domain.usecase.ReadTextFromUriUseCase
import com.deckapp.core.domain.usecase.AnalyzeTableImageUseCase
import com.deckapp.core.domain.usecase.CsvTableParser
import com.deckapp.core.domain.usecase.RangeParser
import com.deckapp.core.domain.usecase.TranscribeTableWithAiUseCase
import com.deckapp.core.domain.usecase.RecognizeTableStreamingUseCase
import com.deckapp.core.model.OcrBlock
import com.deckapp.core.model.TableEntry
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.zip.CRC32

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
    private val transcribeTableWithAiUseCase: TranscribeTableWithAiUseCase,
    private val recognizeTableStreamingUseCase: RecognizeTableStreamingUseCase,
    private val recentFileRepository: RecentFileRepository,
    private val fileRepository: FileRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val analyzeTableImageUseCase = AnalyzeTableImageUseCase()
    private val csvParser = CsvTableParser()

    private val AUTO_VISION_THRESHOLD = 0.80f

    private val visionMutex = Mutex()
    private data class VisionCacheEntry(val nameDraft: String, val formulaDraft: String, val entries: List<TableEntry>)
    private var visionCache: Pair<Long, VisionCacheEntry>? = null

    private val _uiState = MutableStateFlow(TableImportUiState())
    val uiState: StateFlow<TableImportUiState> = _uiState.asStateFlow()

    init {
        loadRecents()
    }

    private fun loadRecents() {
        viewModelScope.launch {
            recentFileRepository.getRecentFiles().collect { recents ->
                val mapped = recents
                    .filter { it.type == RecentFileType.PDF }
                    .map { it.uri to it.name }
                _uiState.update { it.copy(recentPdfs = mapped) }
            }
        }
    }

    fun setSource(source: TableImportSource) {
        val mode = when (source) {
            TableImportSource.OCR_IMAGE -> ImportMode.OCR
            TableImportSource.CSV_TEXT -> ImportMode.CSV
            TableImportSource.JSON_TEXT -> ImportMode.JSON
            TableImportSource.PLAIN_TEXT -> ImportMode.PLAIN_TEXT
            TableImportSource.MARKDOWN_TABLE -> ImportMode.MARKDOWN
        }
        _uiState.update { it.copy(mode = mode) }
    }

    fun selectFile(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, selectedUri = uri) }
            try {
                recentFileRepository.addRecentFile(uri, uri.lastPathSegment ?: "Archivo", RecentFileType.PDF)
                loadPage(0)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message, isProcessing = false) }
            }
        }
    }

    fun importTextFile(uri: Uri) {
        val mode = _uiState.value.mode
        val source = when (mode) {
            ImportMode.CSV -> TableImportSource.CSV_TEXT
            ImportMode.JSON -> TableImportSource.JSON_TEXT
            ImportMode.PLAIN_TEXT -> TableImportSource.PLAIN_TEXT
            ImportMode.MARKDOWN -> TableImportSource.MARKDOWN_TABLE
            else -> TableImportSource.PLAIN_TEXT
        }


        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, selectedUri = uri) }
            try {
                val text = readTextFromUriUseCase(uri)
                val result = importTableUseCase.fromText(TextImportParams(source, text))

                _uiState.update { it.copy(
                    detectedTables = listOf(result),
                    currentTableIndex = 0,
                    step = ImportStep.MAPPING,
                    isProcessing = false
                ) }
                loadResultToDraft(result)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Error al importar texto: ${e.message}", isProcessing = false) }
            }
        }
    }

    fun loadPage(index: Int) {
        val uri = _uiState.value.selectedUri ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            try {
                val bitmap = renderPdfPageUseCase.renderPage(uri, index)
                val count = renderPdfPageUseCase.getPageCount(uri)
                if (bitmap != null) {
                    _uiState.update { it.copy(
                        pageBitmap = bitmap,
                        pdfPageCount = count,
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
                    // 400px es suficiente para thumbnails — 1200px era innecesariamente lento.
                    val thumb = renderPdfPageUseCase.renderPage(uri, 0, targetWidth = 400)
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

                val initialTables = layout.map { (cluster, anchors) ->
                    val analysis = analyzeTableImageUseCase.processWithAnchors(cluster, anchors)
                    ImportResult(
                        sourceType = "OCR",
                        entries = analysis?.entries ?: emptyList(),
                        suggestedName = analysis?.suggestedName ?: "",
                        lowConfidenceIndices = analysis?.lowConfidenceIndices ?: emptySet()
                    )
                }

                _uiState.update { it.copy(
                    ocrBlocks = blocks,
                    detectedTables = initialTables,
                    currentTableIndex = 0,
                    currentCluster = layout[0].first,
                    detectedAnchors = layout[0].second,
                    step = ImportStep.RECOGNITION,
                    isProcessing = false
                ) }

                visionCache = null
                if (settingsRepository.getAutoVisionEnabled() && settingsRepository.getGeminiApiKey().isNotBlank()) {
                    val avgConfidence = if (blocks.isNotEmpty()) blocks.map { it.confidence }.average().toFloat() else 1f
                    if (avgConfidence < AUTO_VISION_THRESHOLD) {
                        _uiState.update { it.copy(autoVisionMessage = "OCR con baja confianza (${(avgConfidence * 100).toInt()}%) — mejorando con Vision AI...") }
                        recognizeWithVision()
                    }
                }
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

                val analysis = analyzeTableImageUseCase.processWithAnchors(
                    _uiState.value.currentCluster,
                    _uiState.value.detectedAnchors
                )

                if (analysis == null) {
                    _uiState.update { it.copy(errorMessage = "Error al procesar con las columnas seleccionadas", isProcessing = false) }
                    return@launch
                }

                val result = ImportResult(
                    sourceType = "OCR",
                    entries = analysis.entries,
                    suggestedName = analysis.suggestedName,
                    lowConfidenceIndices = analysis.lowConfidenceIndices
                )

                _uiState.update { it.copy(
                    detectedTables = updatedTablesWithResult(result),
                    step = ImportStep.MAPPING,
                    isProcessing = false
                ) }
                loadResultToDraft(result)
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

    fun optimizeWithAi() {
        val state = _uiState.value
        val rawContext = if (state.ocrBlocks.isNotEmpty()) {
            state.ocrBlocks.joinToString("\n") { it.text }
        } else {
            state.editableEntries.joinToString("\n") { "${it.minRoll}-${it.maxRoll}: ${it.text}" }
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isAiProcessing = true) }
            try {
                val suggestions = transcribeTableWithAiUseCase(rawContext)
                _uiState.update { it.copy(
                    editableEntries = suggestions.entries,
                    isAiProcessing = false,
                    tableNameDraft = suggestions.suggestedName.ifBlank { _uiState.value.tableNameDraft },
                    tableFormulaDraft = suggestions.suggestedFormula.ifBlank { _uiState.value.tableFormulaDraft },
                    validationResult = RangeParser.validateIntegrity(suggestions.entries.map { it.minRoll to it.maxRoll })
                ) }
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isAiProcessing = false,
                    errorMessage = e.message ?: "Error desconocido al procesar con IA"
                ) }
            }
        }
    }

    fun recognizeWithVision() {
        if (visionMutex.isLocked) return
        val bitmap = _uiState.value.croppedBitmap ?: return
        viewModelScope.launch {
            visionMutex.withLock {
                val crc = bitmapCrc32(bitmap)
                val cached = visionCache
                if (cached != null && cached.first == crc) {
                    _uiState.update { it.copy(
                        editableEntries = cached.second.entries,
                        tableNameDraft = cached.second.nameDraft,
                        tableFormulaDraft = cached.second.formulaDraft,
                        step = ImportStep.MAPPING,
                        isVisionProcessing = false,
                        autoVisionMessage = null,
                        validationResult = RangeParser.validateIntegrity(cached.second.entries.map { e -> e.minRoll to e.maxRoll })
                    ) }
                    return@withLock
                }

                _uiState.update { it.copy(
                    isVisionProcessing = true,
                    editableEntries = emptyList(),
                    step = ImportStep.MAPPING
                ) }
                var sortOrder = 0
                try {
                    recognizeTableStreamingUseCase(bitmap, onRetryWait = { delayMs ->
                        val seconds = ((delayMs + 999L) / 1000L).toInt()
                        for (tick in seconds downTo 1) {
                            _uiState.update { it.copy(retryCountdown = tick) }
                            delay(1_000L)
                        }
                        _uiState.update { it.copy(retryCountdown = null) }
                    }).collect { event ->
                        when (event) {
                            is AiStreamEvent.Metadata -> _uiState.update { state -> state.copy(
                                tableNameDraft = event.name.ifBlank { state.tableNameDraft },
                                tableFormulaDraft = event.formula.ifBlank { state.tableFormulaDraft }
                            ) }
                            is AiStreamEvent.Entry -> {
                                val entry = event.entry.copy(sortOrder = sortOrder++)
                                val newEntries = _uiState.value.editableEntries + entry
                                _uiState.update { it.copy(
                                    editableEntries = newEntries,
                                    validationResult = RangeParser.validateIntegrity(newEntries.map { it.minRoll to it.maxRoll })
                                ) }
                            }
                        }
                    }
                    val finalState = _uiState.value
                    visionCache = crc to VisionCacheEntry(
                        nameDraft = finalState.tableNameDraft,
                        formulaDraft = finalState.tableFormulaDraft,
                        entries = finalState.editableEntries
                    )
                    _uiState.update { it.copy(isVisionProcessing = false, retryCountdown = null, autoVisionMessage = null) }
                } catch (e: Exception) {
                    _uiState.update { it.copy(
                        isVisionProcessing = false,
                        retryCountdown = null,
                        autoVisionMessage = null,
                        errorMessage = e.message ?: "Error desconocido con Gemini Vision"
                    ) }
                }
            }
        }
    }

    private fun bitmapCrc32(bitmap: Bitmap): Long {
        val crc = CRC32()
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val buf = ByteArray(pixels.size * 4)
        pixels.forEachIndexed { i, px ->
            buf[i * 4] = (px shr 24).toByte()
            buf[i * 4 + 1] = (px shr 16).toByte()
            buf[i * 4 + 2] = (px shr 8).toByte()
            buf[i * 4 + 3] = px.toByte()
        }
        crc.update(buf)
        return crc.value
    }

    private fun updatedTablesWithResult(result: ImportResult): List<ImportResult> {
        val tables = _uiState.value.detectedTables.toMutableList()
        val idx = _uiState.value.currentTableIndex
        if (idx in tables.indices) tables[idx] = result else tables.add(result)
        return tables
    }

    private fun loadResultToDraft(result: ImportResult) {
        val threshold = _uiState.value.confidenceThreshold
        val entries = result.entries
        val lowConf = entries.mapIndexedNotNull { idx, entry ->
            if (entry.confidence < threshold) idx else null
        }.toSet()

        _uiState.update { it.copy(
            editableEntries = entries,
            tableNameDraft = result.suggestedName,
            tableFormulaDraft = result.suggestedFormula.ifBlank { "1d6" },
            validationResult = RangeParser.validateIntegrity(entries.map { it.minRoll to it.maxRoll }),
            lowConfidenceIndices = lowConf
        ) }
    }

    fun setConfidenceThreshold(threshold: Float) {
        val currentEntries = _uiState.value.editableEntries
        val lowConf = currentEntries.mapIndexedNotNull { idx, entry ->
            if (entry.confidence < threshold) idx else null
        }.toSet()

        _uiState.update { it.copy(
            confidenceThreshold = threshold,
            lowConfidenceIndices = lowConf
        ) }
    }

    fun updateEntry(index: Int, entry: TableEntry) {
        val newList = _uiState.value.editableEntries.toMutableList()
        if (index in newList.indices) {
            newList[index] = entry
            _uiState.update { it.copy(
                editableEntries = newList,
                validationResult = RangeParser.validateIntegrity(newList.map { it.minRoll to it.maxRoll })
            ) }
        }
    }

    fun setDraftName(name: String) = _uiState.update { it.copy(tableNameDraft = name) }
    fun setDraftTag(tag: String) = _uiState.update { it.copy(tableTagDraft = tag) }
    fun setDraftFormula(formula: String) = _uiState.update { it.copy(tableFormulaDraft = formula) }
    fun clearError() = _uiState.update { it.copy(errorMessage = null) }
    fun clearAutoVisionMessage() = _uiState.update { it.copy(autoVisionMessage = null) }

    fun nextTable() {
        val state = _uiState.value
        val currentIndex = state.currentTableIndex
        if (currentIndex < state.detectedTables.size - 1) {
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
            _uiState.update { it.copy(step = ImportStep.REVIEW) }
        }
    }

    fun saveAll() {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            try {
                _uiState.update { it.copy(savedSuccessfully = true, isProcessing = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message, isProcessing = false) }
            }
        }
    }

    fun reset() {
        visionCache = null
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
        private val transcribeTableWithAiUseCase: TranscribeTableWithAiUseCase,
        private val recognizeTableStreamingUseCase: RecognizeTableStreamingUseCase,
        private val recentFileRepository: RecentFileRepository,
        private val fileRepository: FileRepository,
        private val settingsRepository: SettingsRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TableImportViewModel(
                renderPdfPageUseCase,
                importTableUseCase,
                readTextFromUriUseCase,
                tableRepository,
                transcribeTableWithAiUseCase,
                recognizeTableStreamingUseCase,
                recentFileRepository,
                fileRepository,
                settingsRepository
            ) as T
        }
    }
}
