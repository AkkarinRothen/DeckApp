package com.deckapp.feature.importdeck

import android.content.Context
import android.net.Uri
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

data class DeckImportUiState(
    val phase: DeckImportPhase = DeckImportPhase.SELECT_SOURCE,
    val source: DeckImportSource? = null,
    val selectedUri: Uri? = null,
    val deckName: String = "",
    val defaultContentMode: CardContentMode = CardContentMode.IMAGE_ONLY,
    // PDF-specific
    val pdfLayoutMode: PdfLayoutMode = PdfLayoutMode.ALTERNATING_PAGES,
    val pdfGridCols: Int = 3,
    val pdfGridRows: Int = 3,
    val pdfSkipPages: Int = 0,
    val pdfPreviewBitmap: android.graphics.Bitmap? = null,
    val pdfPageCount: Int = 0,
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
        val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "Nuevo mazo"
        val deckName = fileName.removeSuffix(".pdf")
        _uiState.update {
            it.copy(
                selectedUri = uri,
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
            _uiState.update { it.copy(pdfPageCount = count) }
        }
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
                val skip = state.pdfSkipPages
                val autoTrim = state.pdfAutoTrimCells
                val splitRatio = state.pdfSideBySideSplitRatio

                when (state.pdfLayoutMode) {
                    PdfLayoutMode.ALTERNATING_PAGES -> {
                        // Mostrar frentes (páginas pares) con la grilla configurada
                        outer@ for (page in skip until pageCount step 2) {
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
                        outer@ for (page in skip until pageCount) {
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
                        outer@ for (page in skip until pageCount) {
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
                        val remainingPages = pageCount - skip
                        val half = remainingPages / 2
                        outer@ for (pageOffset in 0 until half) {
                            val page = skip + pageOffset
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
                defaultContentMode = if (shouldAutoChange) suggestedContent else currentContent
            )
        }
    }

    fun updatePdfGridCols(cols: Int) = _uiState.update { it.copy(pdfGridCols = cols.coerceAtLeast(1)) }
    fun updatePdfGridRows(rows: Int) = _uiState.update { it.copy(pdfGridRows = rows.coerceAtLeast(1)) }
    fun updatePdfSkipPages(skip: Int) = _uiState.update { it.copy(pdfSkipPages = skip.coerceAtLeast(0)) }
    fun updatePdfAutoTrimCells(autoTrim: Boolean) = _uiState.update { it.copy(pdfAutoTrimCells = autoTrim) }
    fun updatePdfSplitRatio(ratio: Float) = _uiState.update { it.copy(pdfSideBySideSplitRatio = ratio) }

    fun startImport() {
        val state = _uiState.value
        val uri = state.selectedUri ?: return
        if (state.deckName.isBlank()) return

        _uiState.update { it.copy(phase = DeckImportPhase.IMPORTING, isImporting = true) }

        viewModelScope.launch {
            // Calculate total items for better progress reporting
            val total = when (state.source) {
                DeckImportSource.FOLDER -> fileRepository.listImagesInFolder(uri).size
                DeckImportSource.PDF -> {
                    val pages = if (state.pdfPageCount > 0) state.pdfPageCount else fileRepository.getPdfPageCount(uri)
                    val effectivePages = when (state.pdfLayoutMode) {
                        PdfLayoutMode.ALTERNATING_PAGES, PdfLayoutMode.FIRST_HALF_FRONTS -> (pages - state.pdfSkipPages) / 2
                        else -> pages - state.pdfSkipPages
                    }
                    effectivePages.coerceAtLeast(0) * state.pdfGridCols * state.pdfGridRows
                }
                else -> 0
            }
            _uiState.update { it.copy(totalItemsToImport = total) }

            val failedFiles = mutableListOf<String>()
            importDeckUseCase(
                uri = uri,
                deckName = state.deckName,
                source = state.source ?: DeckImportSource.FOLDER,
                defaultContentMode = state.defaultContentMode,
                pdfLayoutMode = state.pdfLayoutMode,
                pdfGridCols = state.pdfGridCols,
                pdfGridRows = state.pdfGridRows,
                pdfSkipPages = state.pdfSkipPages,
                pdfAutoTrimCells = state.pdfAutoTrimCells,
                pdfSplitRatio = state.pdfSideBySideSplitRatio,
                onProgress = { progress, count ->
                    _uiState.update { it.copy(importProgress = progress, importedCardCount = count) }
                },
                onFileError = { fileName ->
                    failedFiles += fileName
                }
            ).fold(
                onSuccess = { deckId ->
                    _uiState.update {
                        it.copy(
                            phase = DeckImportPhase.SUCCESS,
                            isImporting = false,
                            importedDeckId = deckId,
                            failedFiles = failedFiles.toList()
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            phase = DeckImportPhase.CONFIGURE,
                            isImporting = false,
                            errorMessage = error.message,
                            failedFiles = failedFiles.toList()
                        )
                    }
                }
            )
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
