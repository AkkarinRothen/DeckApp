package com.deckapp.feature.importdeck

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deckapp.core.domain.repository.FileRepository
import com.deckapp.core.domain.usecase.ImportDeckUseCase
import com.deckapp.core.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PdfGridTemplate(
    val id: String,
    val name: String,
    val subtitle: String,
    val layoutMode: PdfLayoutMode,
    val gridCols: Int,
    val gridRows: Int,
    val contentMode: CardContentMode,
    val autoTrim: Boolean = true,
    val splitRatio: Float = 0.5f
)

val PDF_PRESET_TEMPLATES = listOf(
    PdfGridTemplate(
        id = "story_engine_2x3",
        name = "Story Engine 2×3 PnP",
        subtitle = "6 cartas cuadradas por hoja (frente/dorso alternado)",
        layoutMode = PdfLayoutMode.ALTERNATING_PAGES,
        gridCols = 2,
        gridRows = 3,
        contentMode = CardContentMode.DOUBLE_SIDED_FULL
    ),
    PdfGridTemplate(
        id = "3x3_alt",
        name = "3×3 Alternado",
        subtitle = "P&P Estándar (frente/dorso seguidos)",
        layoutMode = PdfLayoutMode.ALTERNATING_PAGES,
        gridCols = 3,
        gridRows = 3,
        contentMode = CardContentMode.DOUBLE_SIDED_FULL
    ),
    PdfGridTemplate(
        id = "3x3_single",
        name = "3×3 Solo Frente",
        subtitle = "9 cartas por página, 1 cara",
        layoutMode = PdfLayoutMode.GRID,
        gridCols = 3,
        gridRows = 3,
        contentMode = CardContentMode.IMAGE_ONLY
    ),
    PdfGridTemplate(
        id = "side_by_side_npc",
        name = "Side-by-Side PNJ",
        subtitle = "Mitad izq=arte, mitad der=texto",
        layoutMode = PdfLayoutMode.SIDE_BY_SIDE,
        gridCols = 1,
        gridRows = 1,
        contentMode = CardContentMode.DOUBLE_SIDED_FULL,
        splitRatio = 0.5f
    ),
    PdfGridTemplate(
        id = "1x1_full",
        name = "1×1 Página Completa",
        subtitle = "1 carta por página",
        layoutMode = PdfLayoutMode.GRID,
        gridCols = 1,
        gridRows = 1,
        contentMode = CardContentMode.IMAGE_ONLY
    ),
    PdfGridTemplate(
        id = "first_half_backs",
        name = "1ra Mitad / 2da Mitad",
        subtitle = "Mitad frentes, mitad dorsos",
        layoutMode = PdfLayoutMode.FIRST_HALF_FRONTS,
        gridCols = 3,
        gridRows = 3,
        contentMode = CardContentMode.DOUBLE_SIDED_FULL
    )
)

data class DeckImportUiState(
    val phase: DeckImportPhase = DeckImportPhase.SELECT_SOURCE,
    val source: DeckImportSource? = null,
    val selectedUri: Uri? = null,
    val batchPdfUris: List<Uri> = emptyList(),
    val deckName: String = "",
    val defaultContentMode: CardContentMode = CardContentMode.IMAGE_ONLY,
    // PDF-specific
    val pdfLayoutMode: PdfLayoutMode = PdfLayoutMode.ALTERNATING_PAGES,
    val pdfGridCols: Int = 3,
    val pdfGridRows: Int = 3,
    val pdfSkipPages: Int = 0,
    val pdfStartPage: Int = 1,
    val pdfEndPage: Int? = null,
    val selectedTemplateId: String? = "3x3_alt",
    val pdfPreviewBitmap: android.graphics.Bitmap? = null,
    val pdfPageCount: Int = 0,
    // Almacenamiento
    val availableStorageBytes: Long? = null,
    val storageWarning: String? = null,
    // Vista previa de cartas (máx 6 bitmaps en memoria)
    val previewCardBitmaps: List<android.graphics.Bitmap> = emptyList(),
    val isGeneratingPreview: Boolean = false,
    val pdfAutoTrimCells: Boolean = true,
    val pdfSideBySideSplitRatio: Float = 0.5f,
    val recentPdfs: List<Pair<Uri, String>> = emptyList(),
    val browsedPdfs: List<Pair<Uri, String>> = emptyList(),
    // Progress
    val importProgress: Float = 0f,
    val totalItemsToImport: Int = 0,
    val importedCardCount: Int = 0,
    val isImporting: Boolean = false,
    val importedDeckId: Long? = null,
    val importedDeckCount: Int = 0,
    val errorMessage: String? = null,
    val failedFiles: List<String> = emptyList()
)

enum class DeckImportPhase {
    SELECT_SOURCE,
    CONFIGURE,
    PREVIEW,
    IMPORTING,
    SUCCESS
}

@HiltViewModel
class DeckImportViewModel @Inject constructor(
    private val importDeckUseCase: ImportDeckUseCase,
    private val fileRepository: FileRepository,
    private val recentFileRepository: com.deckapp.core.domain.repository.RecentFileRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeckImportUiState())
    val uiState: StateFlow<DeckImportUiState> = _uiState.asStateFlow()

    init {
        loadRecents()
    }

    private fun loadRecents() {
        viewModelScope.launch {
            recentFileRepository.getRecentFiles(10).collect { files ->
                _uiState.update { it.copy(recentPdfs = files.map { f -> f.uri to f.name }) }
            }
        }
    }

    fun selectSource(source: DeckImportSource) {
        _uiState.update { it.copy(source = source) }
    }

    fun onFolderSelected(uri: Uri) {
        context.contentResolver.takePersistableUriPermission(
            uri,
            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
        viewModelScope.launch {
            _uiState.update { it.copy(isImporting = true) } // Usar isImporting o similar para overlay de carga
            try {
                val pdfs = fileRepository.listPdfsInFolder(uri)
                _uiState.update { it.copy(browsedPdfs = pdfs, isImporting = false) }
                // Opcional: Si no hay PDFs, podríamos intentar tratar el resto normalmente 
                // pero el usuario pidió ver miniaturas de PDFs.
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Error al listar PDFs: ${e.message}", isImporting = false) }
            }
        }
        
        // Comportamiento original si se decide seguir con la carpeta directamente:
        // val folderName = uri.lastPathSegment?.substringAfterLast(':') ?: "Nuevo mazo"
        // _uiState.update { it.copy(selectedUri = uri, deckName = folderName, phase = ImportPhase.CONFIGURE) }
    }

    fun onFolderConfirmed(uri: Uri) {
        val folderName = uri.lastPathSegment?.substringAfterLast(':') ?: "Nuevo mazo"
        _uiState.update {
            it.copy(
                selectedUri = uri,
                deckName = folderName,
                phase = DeckImportPhase.CONFIGURE,
                source = DeckImportSource.FOLDER
            )
        }
    }

    fun onPdfSelected(uri: Uri) {
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (e: SecurityException) {
            // Si el URI proviene de un listado de carpeta (tree URI), no se puede tomar persistencia individual
            // pero ya tenemos la persistencia sobre la carpeta madre.
        }
        val fileName = displayName(uri)
        val deckName = fileName.removeSuffix(".pdf")
        _uiState.update {
            it.copy(
                selectedUri = uri,
                batchPdfUris = emptyList(),
                deckName = deckName,
                phase = DeckImportPhase.CONFIGURE,
                source = DeckImportSource.PDF
            )
        }

        // Guardar en recientes
        viewModelScope.launch {
            recentFileRepository.addRecentFile(uri, fileName, com.deckapp.core.domain.repository.RecentFileType.PDF)
        }

        renderPdfFirstPagePreview(uri)
        // Obtener conteo de páginas en background
        viewModelScope.launch(Dispatchers.IO) {
            val count = fileRepository.getPdfPageCount(uri)
            _uiState.update {
                it.copy(
                    pdfPageCount = count,
                    pdfStartPage = 1,
                    pdfEndPage = if (count > 0) count else null
                )
            }
            checkStorage()
        }
    }

    fun onPdfsSelected(uris: List<Uri>) {
        val pdfs = uris.distinct()
        if (pdfs.isEmpty()) return
        pdfs.forEach { uri ->
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
        }
        onPdfSelected(pdfs.first())
        _uiState.update { it.copy(batchPdfUris = pdfs) }
        viewModelScope.launch {
            pdfs.drop(1).forEach { pdfUri ->
                recentFileRepository.addRecentFile(
                    pdfUri,
                    displayName(pdfUri),
                    com.deckapp.core.domain.repository.RecentFileType.PDF
                )
            }
        }
    }

    private fun displayName(uri: Uri): String {
        val fromProvider = runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
        }.getOrNull()
        return fromProvider?.takeIf { it.isNotBlank() }
            ?: uri.lastPathSegment?.substringAfterLast('/')
            ?: "Nuevo mazo.pdf"
    }

    suspend fun renderThumbnail(uri: Uri): android.graphics.Bitmap? {
        return fileRepository.renderPdfPageToBitmap(uri, pageIndex = 0, targetWidth = 300)
    }

    fun onZipSelected(uri: Uri) {
        // En algunos exploradores de archivos el URI no es de árbol, por lo que no se requiere takePersistableUriPermission
        // Pero lo intentamos por consistencia
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
        val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "Nuevo mazo"
        val deckName = fileName.removeSuffix(".zip")
        _uiState.update {
            it.copy(
                selectedUri = uri,
                deckName = deckName,
                phase = DeckImportPhase.CONFIGURE
            )
        }
    }

    private fun renderPdfFirstPagePreview(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val bitmap = fileRepository.renderPdfPageToBitmap(uri, pageIndex = 0, targetWidth = 600)
            if (bitmap != null) {
                _uiState.update { it.copy(pdfPreviewBitmap = bitmap) }
            } else {
                _uiState.update { it.copy(errorMessage = "No se pudo previsualizar el PDF") }
            }
        }
    }

    /**
     * Genera una vista previa de las primeras N cartas según la configuración actual de layout.
     * Los bitmaps se guardan en memoria (no en disco); se reciclan cuando el usuario sale.
     */
    fun generatePreview() {
        val state = _uiState.value
        val uri = state.selectedUri ?: return
        _uiState.update { it.copy(isGeneratingPreview = true, previewCardBitmaps = emptyList()) }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val pageCount = state.pdfPageCount.takeIf { it > 0 }
                    ?: fileRepository.getPdfPageCount(uri)
                val bitmaps = mutableListOf<android.graphics.Bitmap>()
                val maxPreview = 6
                val cols = state.pdfGridCols
                val rows = state.pdfGridRows
                val startIdx = (state.pdfStartPage - 1).coerceAtLeast(0)
                val requestedEnd = state.pdfEndPage?.let { it - 1 } ?: (pageCount - 1)
                if (requestedEnd < startIdx || startIdx >= pageCount) {
                    _uiState.update {
                        it.copy(
                            previewCardBitmaps = emptyList(),
                            isGeneratingPreview = false,
                            phase = DeckImportPhase.PREVIEW
                        )
                    }
                    return@launch
                }
                val endIdx = requestedEnd.coerceAtMost(pageCount - 1)
                val autoTrim = state.pdfAutoTrimCells
                val splitRatio = state.pdfSideBySideSplitRatio

                when (state.pdfLayoutMode) {
                    PdfLayoutMode.ALTERNATING_PAGES -> {
                        // Mostrar frentes con la grilla configurada
                        outer@ for (page in startIdx..endIdx step 2) {
                            for (row in 0 until rows) {
                                for (col in 0 until cols) {
                                    if (bitmaps.size >= maxPreview) break@outer
                                    fileRepository.renderPdfGridCellToBitmap(
                                        uri, page, col, row, cols, rows,
                                        autoTrimCell = autoTrim
                                    )?.let { bitmaps += it }
                                }
                            }
                        }
                    }
                    PdfLayoutMode.SIDE_BY_SIDE -> {
                        // Mostrar pares frente/dorso: izq = cols 0..cols-1, der = cols cols..2*cols-1
                        val totalCols = cols * 2
                        outer@ for (page in startIdx..endIdx) {
                            for (row in 0 until rows) {
                                for (col in 0 until cols) {
                                    if (bitmaps.size >= maxPreview) break@outer
                                    fileRepository.renderPdfGridCellToBitmap(
                                        uri, page, col, row, totalCols, rows,
                                        autoTrimCell = autoTrim,
                                        horizontalSplitRatio = splitRatio
                                    )?.let { bitmaps += it }
                                    if (bitmaps.size < maxPreview) {
                                        fileRepository.renderPdfGridCellToBitmap(
                                            uri, page, col + cols, row, totalCols, rows,
                                            autoTrimCell = autoTrim,
                                            horizontalSplitRatio = splitRatio
                                        )?.let { bitmaps += it }
                                    }
                                }
                            }
                        }
                    }
                    PdfLayoutMode.GRID -> {
                        outer@ for (page in startIdx..endIdx) {
                            for (row in 0 until rows) {
                                for (col in 0 until cols) {
                                    if (bitmaps.size >= maxPreview) break@outer
                                    fileRepository.renderPdfGridCellToBitmap(
                                        uri, page, col, row, cols, rows,
                                        autoTrimCell = autoTrim
                                    )?.let { bitmaps += it }
                                }
                            }
                        }
                    }
                    PdfLayoutMode.FIRST_HALF_FRONTS -> {
                        val totalRange = endIdx - startIdx + 1
                        val half = totalRange / 2
                        outer@ for (pageOffset in 0 until half) {
                            val page = startIdx + pageOffset
                            for (row in 0 until rows) {
                                for (col in 0 until cols) {
                                    if (bitmaps.size >= maxPreview) break@outer
                                    fileRepository.renderPdfGridCellToBitmap(
                                        uri, page, col, row, cols, rows,
                                        autoTrimCell = autoTrim
                                    )?.let { bitmaps += it }
                                }
                            }
                        }
                    }
                }

                _uiState.update {
                    it.copy(
                        previewCardBitmaps = bitmaps,
                        isGeneratingPreview = false,
                        phase = DeckImportPhase.PREVIEW
                    )
                }
            } catch (e: Throwable) {
                _uiState.update {
                    it.copy(
                        isGeneratingPreview = false,
                        errorMessage = "Error al generar preview: ${e.message}"
                    )
                }
            }
        }
    }

    /** Vuelve a la fase de configuración para ajustar el layout. */
    fun backToConfigure() {
        _uiState.update { it.copy(phase = DeckImportPhase.CONFIGURE) }
    }

    fun updateDeckName(name: String) = _uiState.update { it.copy(deckName = name) }
    fun updateDefaultContentMode(mode: CardContentMode) = _uiState.update { it.copy(defaultContentMode = mode) }

    /**
     * Cambia el modo de layout y sugiere automáticamente el ContentMode adecuado:
     * - GRID → IMAGE_ONLY (una cara por carta)
     * - Otros (SIDE_BY_SIDE, ALTERNATING_PAGES, FIRST_HALF_FRONTS) → DOUBLE_SIDED_FULL (2 caras)
     * Solo auto-sugiere si el usuario no cambió el modo manualmente (sigue en el default IMAGE_ONLY).
     */
    fun updatePdfLayoutMode(mode: PdfLayoutMode) {
        val currentContent = _uiState.value.defaultContentMode
        val suggestedContent = when (mode) {
            PdfLayoutMode.GRID -> CardContentMode.IMAGE_ONLY
            else -> CardContentMode.DOUBLE_SIDED_FULL  // layouts que producen 2 caras
        }
        // Auto-cambiar solo si el usuario no eligió algo distinto al default o a la sugerencia previa
        val shouldAutoChange = currentContent == CardContentMode.IMAGE_ONLY
                || currentContent == CardContentMode.DOUBLE_SIDED_FULL
        _uiState.update {
            it.copy(
                pdfLayoutMode = mode,
                defaultContentMode = if (shouldAutoChange) suggestedContent else currentContent,
                selectedTemplateId = null
            )
        }
        checkStorage()
    }

    fun updatePdfGridCols(cols: Int) {
        _uiState.update { it.copy(pdfGridCols = cols.coerceAtLeast(1), selectedTemplateId = null) }
        checkStorage()
    }

    fun updatePdfGridRows(rows: Int) {
        _uiState.update { it.copy(pdfGridRows = rows.coerceAtLeast(1), selectedTemplateId = null) }
        checkStorage()
    }

    fun updatePdfSkipPages(skip: Int) = _uiState.update { it.copy(pdfSkipPages = skip.coerceAtLeast(0)) }

    fun updatePdfStartPage(page: Int) {
        _uiState.update { it.copy(pdfStartPage = page.coerceAtLeast(1)) }
        checkStorage()
    }

    fun updatePdfEndPage(page: Int?) {
        _uiState.update { it.copy(pdfEndPage = page?.coerceAtLeast(1)) }
        checkStorage()
    }

    fun resetPdfPageRange() {
        val total = _uiState.value.pdfPageCount
        _uiState.update { it.copy(pdfStartPage = 1, pdfEndPage = if (total > 0) total else null) }
        checkStorage()
    }

    fun applyPdfTemplate(template: PdfGridTemplate) {
        _uiState.update {
            it.copy(
                selectedTemplateId = template.id,
                pdfLayoutMode = template.layoutMode,
                pdfGridCols = template.gridCols,
                pdfGridRows = template.gridRows,
                defaultContentMode = template.contentMode,
                pdfAutoTrimCells = template.autoTrim,
                pdfSideBySideSplitRatio = template.splitRatio
            )
        }
        checkStorage()
    }

    fun updatePdfAutoTrimCells(autoTrim: Boolean) = _uiState.update { it.copy(pdfAutoTrimCells = autoTrim) }
    fun updatePdfSplitRatio(ratio: Float) = _uiState.update { it.copy(pdfSideBySideSplitRatio = ratio) }

    fun checkStorage() {
        viewModelScope.launch {
            val available = fileRepository.getAvailableStorageBytes()
            val state = _uiState.value
            val estimatedCount = calculateEstimatedCards(state)
            val estimatedBytes = estimatedCount * 350_000L
            val warning = if (available < estimatedBytes && available < 150 * 1024 * 1024L) {
                val availableMb = available / (1024 * 1024)
                val requiredMb = (estimatedBytes / (1024 * 1024)).coerceAtLeast(1)
                "Espacio de almacenamiento bajo: tienes ${availableMb} MB libres y se estiman ~${requiredMb} MB requeridos."
            } else null
            _uiState.update { it.copy(availableStorageBytes = available, storageWarning = warning) }
        }
    }

    fun calculateEstimatedCards(state: DeckImportUiState): Int {
        if (state.source != DeckImportSource.PDF) return 0
        val pageCount = state.pdfPageCount
        if (pageCount <= 0) return 0
        val startIdx = (state.pdfStartPage - 1).coerceAtLeast(0)
        val requestedEnd = state.pdfEndPage?.let { it - 1 } ?: (pageCount - 1)
        if (requestedEnd < startIdx || startIdx >= pageCount) return 0
        val endIdx = requestedEnd.coerceAtMost(pageCount - 1)
        val rangePages = endIdx - startIdx + 1
        val effectivePages = when (state.pdfLayoutMode) {
            PdfLayoutMode.ALTERNATING_PAGES, PdfLayoutMode.FIRST_HALF_FRONTS -> rangePages / 2
            else -> rangePages
        }
        return effectivePages.coerceAtLeast(0) * state.pdfGridCols * state.pdfGridRows
    }

    fun startImport() {
        val state = _uiState.value
        val uri = state.selectedUri ?: return
        if (state.batchPdfUris.isEmpty() && state.deckName.isBlank()) return

        _uiState.update { it.copy(phase = DeckImportPhase.IMPORTING, isImporting = true) }

        viewModelScope.launch {
            val pdfUris = state.batchPdfUris.ifEmpty { listOf(uri) }
            val pageCounts = if (state.source == DeckImportSource.PDF) {
                pdfUris.associateWith { pdfUri ->
                    runCatching { fileRepository.getPdfPageCount(pdfUri) }.getOrDefault(0)
                }
            } else emptyMap()
            val perPdfTotals = pdfUris.associateWith { pdfUri ->
                val pages = pageCounts[pdfUri] ?: state.pdfPageCount
                val startPage = state.pdfStartPage.coerceAtMost(pages.coerceAtLeast(1)).coerceAtLeast(1)
                val endPage = (state.pdfEndPage ?: pages).coerceAtMost(pages).coerceAtLeast(startPage)
                calculateEstimatedCards(
                    state.copy(pdfPageCount = pages, pdfStartPage = startPage, pdfEndPage = endPage)
                )
            }
            val total = when (state.source) {
                DeckImportSource.FOLDER -> fileRepository.listImagesInFolder(uri).size
                DeckImportSource.PDF -> perPdfTotals.values.sum()
                else -> 0
            }
            _uiState.update { it.copy(totalItemsToImport = total) }

            val failedFiles = mutableListOf<String>()
            var firstDeckId: Long? = null
            var successfulDecks = 0
            var successfulCards = 0
            var completedEstimate = 0
            val batch = state.source == DeckImportSource.PDF && state.batchPdfUris.isNotEmpty()
            val sources = if (state.source == DeckImportSource.PDF) pdfUris else listOf(uri)
            for ((index, sourceUri) in sources.withIndex()) {
                val pages = pageCounts[sourceUri] ?: state.pdfPageCount
                val boundedEndPage = state.pdfEndPage?.coerceAtMost(pages)
                val boundedStartPage = (state.pdfStartPage - 1)
                    .coerceAtLeast(0)
                    .coerceAtMost((pages - 1).coerceAtLeast(0))
                val estimatedForFile = perPdfTotals[sourceUri] ?: total
                val fileName = displayName(sourceUri)
                val importName = if (batch) fileName.substringBeforeLast('.', fileName) else state.deckName
                var cardsInCurrentFile = 0
                val result = importDeckUseCase(
                    uri = sourceUri,
                    deckName = importName,
                    source = state.source ?: DeckImportSource.FOLDER,
                    defaultContentMode = state.defaultContentMode,
                    pdfLayoutMode = state.pdfLayoutMode,
                    pdfGridCols = state.pdfGridCols,
                    pdfGridRows = state.pdfGridRows,
                    pdfSkipPages = state.pdfSkipPages,
                    pdfStartPage = boundedStartPage,
                    pdfEndPage = boundedEndPage?.let { (it - 1).coerceAtLeast(0) },
                    pdfAutoTrimCells = state.pdfAutoTrimCells,
                    pdfSplitRatio = state.pdfSideBySideSplitRatio,
                    onProgress = { progress, count ->
                        cardsInCurrentFile = count
                        val overall = if (total > 0) (completedEstimate + progress * estimatedForFile) / total else 0f
                        _uiState.update {
                            it.copy(importProgress = overall.coerceIn(0f, 1f), importedCardCount = successfulCards + count)
                        }
                    },
                    onFileError = { failed -> failedFiles += "$fileName: $failed" }
                )
                result.fold(
                    onSuccess = { deckId ->
                        if (firstDeckId == null) firstDeckId = deckId
                        successfulDecks++
                        successfulCards += cardsInCurrentFile
                    },
                    onFailure = { error -> failedFiles += "$fileName: ${error.message ?: "Error al importar"}" }
                )
                completedEstimate += estimatedForFile
                _uiState.update { it.copy(importProgress = (completedEstimate.toFloat() / total.coerceAtLeast(1)).coerceIn(0f, 1f)) }
            }
            if (firstDeckId != null) {
                _uiState.update {
                    it.copy(
                        phase = DeckImportPhase.SUCCESS,
                        isImporting = false,
                        importedDeckId = firstDeckId,
                        importedDeckCount = successfulDecks,
                        importedCardCount = successfulCards,
                        failedFiles = failedFiles.toList()
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        phase = DeckImportPhase.CONFIGURE,
                        isImporting = false,
                        errorMessage = failedFiles.firstOrNull() ?: "No se pudo importar ningún archivo.",
                        failedFiles = failedFiles.toList()
                    )
                }
            }
        }
    }

    fun clearError() = _uiState.update { it.copy(errorMessage = null) }

    fun clearPreviews() {
        _uiState.value.pdfPreviewBitmap?.recycle()
        _uiState.value.previewCardBitmaps.forEach { it.recycle() }
        _uiState.update { it.copy(pdfPreviewBitmap = null, previewCardBitmaps = emptyList()) }
    }

    override fun onCleared() {
        super.onCleared()
        clearPreviews()
    }
}
