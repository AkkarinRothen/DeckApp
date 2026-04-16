package com.deckapp.feature.tables.tableimport

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deckapp.core.domain.repository.TableRepository
import com.deckapp.core.domain.usecase.CsvTableParser
import com.deckapp.core.domain.usecase.ImportResult
import com.deckapp.core.domain.usecase.ImportSource
import com.deckapp.core.domain.usecase.ImportTableUseCase
import com.deckapp.core.domain.usecase.RangeParser
import com.deckapp.core.domain.usecase.ReadTextFromUriUseCase
import com.deckapp.core.domain.usecase.RenderPdfPageUseCase
import com.deckapp.core.domain.usecase.TextImportParams
import com.deckapp.core.model.RandomTable
import com.deckapp.core.model.TableEntry
import com.deckapp.core.model.TableRollMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── UI States ──────────────────────────────────────────────────────────────────

enum class ImportMode { NONE, OCR, CSV, JSON, PLAIN_TEXT }
enum class ImportStep { SOURCE_SELECTION, FILE_PREVIEW, CROP, MAPPING, REVIEW }

/**
 * Modo de unión cuando el usuario añade páginas adicionales al stitching.
 *
 * [CONTINUE_RANGES]: los rangos de la nueva página se desplazan para continuar
 * desde el máximo de la página anterior. Útil cuando la tabla real sigue en la
 * siguiente página con los mismos números de dado (ej. tabla 1d100 en dos páginas).
 *
 * [APPEND]: las entradas se anexan con `sortOrder` continuo pero sin tocar los
 * valores de `minRoll`/`maxRoll`. Útil cuando cada página es una sección distinta
 * o cuando los rangos ya son correctos tal como vienen del OCR.
 */
enum class StitchingMode { CONTINUE_RANGES, APPEND }

data class TableImportUiState(
    val mode: ImportMode = ImportMode.NONE,
    val step: ImportStep = ImportStep.SOURCE_SELECTION,
    // OCR
    val selectedUri: Uri? = null,
    val isPdf: Boolean = false,
    val pdfPageCount: Int = 0,
    val currentPageIndex: Int = 0,
    val pageBitmap: Bitmap? = null,
    // Texto / Archivo
    val rawText: String = "",
    val csvPreview: CsvTableParser.ParsePreview? = null,
    // Resultado (Multi-tabla)
    val detectedTables: List<ImportResult> = emptyList(),
    val currentTableIndex: Int = 0,
    val editableEntries: List<TableEntry> = emptyList(),
    val tableNameDraft: String = "",
    val tableTagDraft: String = "",
    val validationResult: RangeParser.ValidationResult? = null,
    /** Índices de editableEntries con confianza OCR baja — se resaltan en la pantalla de revisión. */
    val lowConfidenceIndices: Set<Int> = emptySet(),
    // Control
    val isProcessing: Boolean = false,
    val errorMessage: String? = null,
    val snackbarMessage: String? = null,
    val savedSuccessfully: Boolean = false,
    val isStitchingMode: Boolean = false,
    val stitchingMode: StitchingMode = StitchingMode.CONTINUE_RANGES,
    val expectedTableCount: Int = 0, // 0 = Auto
    val suggestedPoints: List<androidx.compose.ui.geometry.Offset>? = null,
    val croppedBitmap: Bitmap? = null
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class TableImportViewModel @Inject constructor(
    private val renderPdfPageUseCase: RenderPdfPageUseCase,
    private val importTableUseCase: ImportTableUseCase,
    private val readTextFromUriUseCase: ReadTextFromUriUseCase,
    private val tableRepository: TableRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TableImportUiState())
    val uiState: StateFlow<TableImportUiState> = _uiState.asStateFlow()

    // ── Source selection ──────────────────────────────────────────────────────

    fun onOcrSelected(uri: Uri, isPdf: Boolean) {
        _uiState.update { it.copy(mode = ImportMode.OCR, selectedUri = uri, isPdf = isPdf, step = ImportStep.CROP) }
        if (isPdf) loadPdfInfo(uri) else _uiState.update { it.copy(step = ImportStep.CROP) }
    }

    fun onFileSelected(uri: Uri) {
        val path = uri.lastPathSegment ?: ""
        // Detección por extensión como primera pasada rápida.
        // Si la extensión no es específica (.txt, sin extensión), se redetecta
        // por contenido en onRawTextLoaded() tras leer el archivo.
        val modeByExtension = when {
            path.endsWith(".json", true) -> ImportMode.JSON
            path.endsWith(".csv", true) || path.endsWith(".tsv", true) -> ImportMode.CSV
            else -> ImportMode.NONE  // NONE = pendiente de detección por contenido
        }
        _uiState.update { it.copy(mode = modeByExtension, selectedUri = uri, step = ImportStep.FILE_PREVIEW, isProcessing = true) }
        loadFileText(uri)
    }

    /** Infiere el modo de importación a partir del contenido cuando la extensión no es concluyente. */
    private fun detectModeFromContent(content: String): ImportMode {
        val trimmed = content.trimStart('\uFEFF').trimStart()
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) return ImportMode.JSON
        val firstLines = content.lines().filter { it.isNotBlank() }.take(3)
        val hasDelimiter = firstLines.any { line -> line.count { it == ',' || it == ';' } >= 1 }
        if (hasDelimiter) return ImportMode.CSV
        return ImportMode.PLAIN_TEXT
    }

    fun onTextPasted(text: String) {
        _uiState.update { it.copy(mode = ImportMode.PLAIN_TEXT, rawText = text) }
        parseText(ImportSource.PLAIN_TEXT, text)
    }

    // ── PDF Rendering ─────────────────────────────────────────────────────────

    private fun loadPdfInfo(uri: Uri) {
        viewModelScope.launch {
            val count = renderPdfPageUseCase.getPageCount(uri)
            _uiState.update { it.copy(pdfPageCount = count) }
            loadPdfPage(0)
        }
    }

    fun loadPdfPage(pageIndex: Int) {
        val uri = _uiState.value.selectedUri ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, suggestedPoints = null) }
            val bitmap = renderPageAdaptiveDpi(uri, pageIndex)
            _uiState.update { it.copy(pageBitmap = bitmap, currentPageIndex = pageIndex, isProcessing = false) }
            if (bitmap != null) suggestTablePoints(bitmap)
        }
    }

    /**
     * Renderiza una página PDF con resolución adaptativa.
     * Primer pass a 800px para estimar la densidad tipográfica de la tabla.
     * Si la altura media de los bloques OCR es pequeña (texto denso), sube a 1800px.
     * Si el texto es grande, se queda en 1200px para no desperdiciar memoria.
     */
    private suspend fun renderPageAdaptiveDpi(uri: android.net.Uri, pageIndex: Int): android.graphics.Bitmap? {
        val preview = renderPdfPageUseCase.renderPage(uri, pageIndex, targetWidth = 800) ?: return null
        return try {
            val blocks = importTableUseCase.getRawBlocks(preview)
            val avgBlockHeight = if (blocks.isEmpty()) 20f
            else blocks.map { it.boundingBox.height }.average().toFloat()
            // Texto pequeño (alta densidad): avgHeight < 15px en un render de 800px
            // equivale a tipografía de ~8–9pt → necesitamos más resolución.
            val targetWidth = when {
                avgBlockHeight < 15f -> 1800
                avgBlockHeight < 25f -> 1200
                else -> 900
            }
            if (targetWidth == 800) preview
            else renderPdfPageUseCase.renderPage(uri, pageIndex, targetWidth) ?: preview
        } catch (e: Exception) {
            // Si el OCR de preview falla, usar resolución estándar
            renderPdfPageUseCase.renderPage(uri, pageIndex, targetWidth = 1200) ?: preview
        }
    }

    private suspend fun suggestTablePoints(bitmap: Bitmap) {
        try {
            val blocks = importTableUseCase.getRawBlocks(bitmap)
            if (blocks.isEmpty()) return

            val bh = bitmap.height.toFloat()
            val bw = bitmap.width.toFloat()

            // Filtrar ruido de página antes de calcular el bounding box sugerido:
            // - Encabezado (top < 5% de la altura): títulos de capítulo, nombre de libro.
            // - Pie de página (bottom > 95%): número de página, copyright.
            // - Bloques de texto muy corto (≤ 3 chars) solos: números de página sueltos.
            val contentBlocks = blocks.filter { block ->
                val topNorm = block.boundingBox.top / bh
                val bottomNorm = block.boundingBox.bottom / bh
                topNorm >= 0.05f && bottomNorm <= 0.95f && block.text.trim().length > 3
            }

            val source = contentBlocks.ifEmpty { blocks } // fallback si la página es solo tabla
            val left = source.minOf { it.boundingBox.left }
            val top = source.minOf { it.boundingBox.top }
            val right = source.maxOf { it.boundingBox.right }
            val bottom = source.maxOf { it.boundingBox.bottom }

            val normalizedPoints = listOf(
                androidx.compose.ui.geometry.Offset(left / bw, top / bh),
                androidx.compose.ui.geometry.Offset(right / bw, top / bh),
                androidx.compose.ui.geometry.Offset(right / bw, bottom / bh),
                androidx.compose.ui.geometry.Offset(left / bw, bottom / bh)
            )
            _uiState.update { it.copy(suggestedPoints = normalizedPoints) }
        } catch (e: Exception) {
            // Ignorar errores en la sugerencia, no es crítica
        }
    }

    // ── File Loading ──────────────────────────────────────────────────────────

    private fun loadFileText(uri: Uri) {
        viewModelScope.launch {
            try {
                val text = readTextFromUriUseCase(uri)
                // Si la extensión no fue concluyente, detectar por contenido ahora
                val resolvedMode = if (_uiState.value.mode == ImportMode.NONE) {
                    detectModeFromContent(text)
                } else {
                    _uiState.value.mode
                }
                _uiState.update { it.copy(rawText = text, mode = resolvedMode, isProcessing = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Error al leer el archivo: ${e.message}", isProcessing = false) }
            }
        }
    }

    fun onRawTextChanged(text: String) {
        _uiState.update { it.copy(rawText = text) }
    }

    fun previewCsv() {
        val text = _uiState.value.rawText
        if (text.isBlank()) return
        try {
            val preview = importTableUseCase.previewCsv(text)
            _uiState.update { it.copy(csvPreview = preview, step = ImportStep.MAPPING) }
        } catch (e: Exception) {
            _uiState.update { it.copy(errorMessage = "Error al previsualizar CSV: ${e.message}") }
        }
    }

    fun applyMappingAndParse(config: CsvTableParser.ParseConfig) {
        val text = _uiState.value.rawText
        viewModelScope.launch {
            try {
                val result = importTableUseCase.fromText(TextImportParams(ImportSource.CSV_TEXT, text, csvConfig = config))
                applyResults(listOf(result))
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    private fun parseText(source: ImportSource, text: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            try {
                val result = importTableUseCase.fromText(TextImportParams(source, text))
                applyResults(listOf(result))
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message, isProcessing = false) }
            }
        }
    }

    // ── OCR Crop ─────────────────────────────────────────────────────────────

    fun onCropFinished(bitmap: Bitmap) {
        val state = _uiState.value
        _uiState.update { it.copy(isProcessing = true, croppedBitmap = bitmap) }
        viewModelScope.launch {
            try {
                val results = importTableUseCase.fromBitmap(bitmap, expectedTableCount = state.expectedTableCount)
                if (results.isEmpty()) {
                    _uiState.update { it.copy(isProcessing = false, errorMessage = "No se detectaron tablas en la imagen.") }
                } else {
                    applyResults(results)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Error al procesar la imagen: ${e.message}", isProcessing = false) }
            }
        }
    }

    fun selectDetectedTable(index: Int) {
        val state = _uiState.value
        if (index in state.detectedTables.indices) {
            val result = state.detectedTables[index]
            val validation = RangeParser.validateIntegrity(result.entries.map { it.minRoll to it.maxRoll })
            _uiState.update { it.copy(
                currentTableIndex = index,
                editableEntries = result.entries,
                tableNameDraft = result.suggestedName.ifBlank { "Tabla ${index + 1}" },
                validationResult = validation
            ) }
        }
    }

    fun startStitching() {
        if (_uiState.value.isPdf && _uiState.value.selectedUri != null) {
            // Si es PDF, volvemos directamente al CROP del mismo archivo
            _uiState.update { it.copy(isStitchingMode = true, step = ImportStep.CROP) }
        } else {
            // Si es imagen o archivo, volvemos a la selección
            _uiState.update { it.copy(isStitchingMode = true, step = ImportStep.SOURCE_SELECTION) }
        }
    }

    // ── Review / Editing ──────────────────────────────────────────────────────

    private fun applyResults(results: List<ImportResult>) {
        _uiState.update { state ->
            val firstResult = results.first()
            val newEntries = if (state.isStitchingMode) {
                val offset = state.editableEntries.size
                when (state.stitchingMode) {
                    StitchingMode.CONTINUE_RANGES -> {
                        // Desplazar rolls para que continúen desde el máximo anterior
                        val lastMax = state.editableEntries.maxOfOrNull { it.maxRoll } ?: 0
                        state.editableEntries + firstResult.entries.map {
                            it.copy(
                                minRoll = it.minRoll + lastMax,
                                maxRoll = it.maxRoll + lastMax,
                                sortOrder = it.sortOrder + offset
                            )
                        }
                    }
                    StitchingMode.APPEND -> {
                        // Respetar los rolls tal como vienen; solo ajustar sortOrder
                        state.editableEntries + firstResult.entries.map {
                            it.copy(sortOrder = it.sortOrder + offset)
                        }
                    }
                }
            } else {
                firstResult.entries
            }

            val validation = RangeParser.validateIntegrity(newEntries.map { it.minRoll to it.maxRoll })
            state.copy(
                detectedTables = results,
                currentTableIndex = 0,
                editableEntries = newEntries,
                tableNameDraft = if (state.isStitchingMode) state.tableNameDraft else firstResult.suggestedName.ifBlank { "Tabla 1" },
                validationResult = validation,
                lowConfidenceIndices = firstResult.lowConfidenceIndices,
                step = ImportStep.REVIEW,
                isProcessing = false,
                isStitchingMode = false
            )
        }
    }

    fun updateTableName(name: String) = _uiState.update { it.copy(tableNameDraft = name) }
    fun updateTableTag(tag: String) = _uiState.update { it.copy(tableTagDraft = tag) }
    fun updateExpectedTableCount(count: Int) = _uiState.update { it.copy(expectedTableCount = count) }
    fun updateStitchingMode(mode: StitchingMode) = _uiState.update { it.copy(stitchingMode = mode) }

    fun updateEntry(index: Int, entry: TableEntry) {
        val updated = _uiState.value.editableEntries.toMutableList()
        if (index < updated.size) {
            updated[index] = entry
            val validation = RangeParser.validateIntegrity(updated.map { it.minRoll to it.maxRoll })
            _uiState.update { it.copy(editableEntries = updated, validationResult = validation) }
        }
    }

    fun deleteEntry(index: Int) {
        val updated = _uiState.value.editableEntries.toMutableList()
        if (index < updated.size) {
            updated.removeAt(index)
            val validation = RangeParser.validateIntegrity(updated.map { it.minRoll to it.maxRoll })
            _uiState.update { it.copy(editableEntries = updated, validationResult = validation) }
        }
    }

    fun moveEntry(fromIndex: Int, toIndex: Int) {
        val updated = _uiState.value.editableEntries.toMutableList()
        if (fromIndex in updated.indices && toIndex in updated.indices) {
            val item = updated.removeAt(fromIndex)
            updated.add(toIndex, item)
            // Actualizar sortOrder basado en la nueva posición
            val reordered = updated.mapIndexed { i, entry -> entry.copy(sortOrder = i) }
            val validation = RangeParser.validateIntegrity(reordered.map { it.minRoll to it.maxRoll })
            _uiState.update { it.copy(editableEntries = reordered, validationResult = validation) }
        }
    }

    fun addEntry() {
        val current = _uiState.value.editableEntries
        val nextRoll = (current.maxOfOrNull { it.maxRoll } ?: 0) + 1
        val newEntry = TableEntry(minRoll = nextRoll, maxRoll = nextRoll, text = "", sortOrder = current.size)
        _uiState.update { it.copy(editableEntries = current + newEntry) }
    }

    // ── Save ──────────────────────────────────────────────────────────────────

    fun saveTable() {
        val state = _uiState.value
        if (state.editableEntries.isEmpty()) return
        _uiState.update { it.copy(isProcessing = true) }
        viewModelScope.launch {
            try {
                val maxRoll = state.editableEntries.maxOf { it.maxRoll }
                val tags = if (state.tableTagDraft.isNotBlank()) {
                    listOf(com.deckapp.core.model.Tag(name = state.tableTagDraft, color = -10354450))
                } else emptyList()

                val table = RandomTable(
                    id = 0L,
                    name = state.tableNameDraft.ifBlank { "Tabla importada" },
                    description = state.detectedTables.getOrNull(state.currentTableIndex)?.suggestedDescription ?: "",
                    tags = tags,
                    rollFormula = state.detectedTables.getOrNull(state.currentTableIndex)?.suggestedFormula ?: RangeParser.inferRollFormula(maxRoll),
                    rollMode = TableRollMode.RANGE,
                    entries = state.editableEntries,
                    isBuiltIn = false
                )
                tableRepository.saveTable(table)
                _uiState.update { it.copy(isProcessing = false, savedSuccessfully = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Error al guardar la tabla: ${e.message}", isProcessing = false) }
            }
        }
    }

    fun clearError() = _uiState.update { it.copy(errorMessage = null) }
    fun clearSnackbar() = _uiState.update { it.copy(snackbarMessage = null) }
    fun goBack() {
        val currentStep = _uiState.value.step
        val prevStep = when (currentStep) {
            ImportStep.SOURCE_SELECTION -> null
            ImportStep.FILE_PREVIEW -> ImportStep.SOURCE_SELECTION
            ImportStep.CROP -> ImportStep.SOURCE_SELECTION
            ImportStep.MAPPING -> ImportStep.FILE_PREVIEW
            ImportStep.REVIEW -> if (_uiState.value.mode == ImportMode.OCR) ImportStep.CROP else ImportStep.FILE_PREVIEW
        }
        if (prevStep != null) {
            _uiState.update { it.copy(step = prevStep) }
        }
    }
}
