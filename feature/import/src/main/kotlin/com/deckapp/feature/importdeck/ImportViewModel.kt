package com.deckapp.feature.importdeck

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deckapp.core.domain.repository.FileRepository
import com.deckapp.core.domain.usecase.ImportDeckUseCase
import com.deckapp.core.model.CardContentMode
import com.deckapp.core.model.StackType
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ImportSource { FOLDER, PDF, ZIP }

enum class PdfLayoutMode {
    ALTERNATING_PAGES,   // Página 1=frente, página 2=dorso, página 3=frente...
    SIDE_BY_SIDE,        // Frente | Dorso en la misma página (corte vertical)
    GRID,                // N×M cartas por página
    FIRST_HALF_FRONTS    // Primera mitad = frentes, segunda mitad = dorsos
}

data class ImportUiState(
    val phase: ImportPhase = ImportPhase.SELECT_SOURCE,
    val source: ImportSource? = null,
    val selectedUri: Uri? = null,
    val deckName: String = "",
    val defaultContentMode: CardContentMode = CardContentMode.IMAGE_ONLY,
    // PDF-specific
    val pdfLayoutMode: PdfLayoutMode = PdfLayoutMode.ALTERNATING_PAGES,
    val pdfGridCols: Int = 3,
    val pdfGridRows: Int = 3,
    val pdfPreviewBitmap: android.graphics.Bitmap? = null,
    val pdfPageCount: Int = 0,
    // Vista previa de cartas (máx 6 bitmaps en memoria)
    val previewCardBitmaps: List<android.graphics.Bitmap> = emptyList(),
    val isGeneratingPreview: Boolean = false,
    val pdfAutoTrimCells: Boolean = true,
    // Progress
    val importProgress: Float = 0f,
    val importedCardCount: Int = 0,
    val isImporting: Boolean = false,
    val importedDeckId: Long? = null,
    val errorMessage: String? = null
)

enum class ImportPhase {
    SELECT_SOURCE,
    CONFIGURE,
    PREVIEW,
    IMPORTING,
    SUCCESS
}

@HiltViewModel
class ImportViewModel @Inject constructor(
    private val importDeckUseCase: ImportDeckUseCase,
    private val fileRepository: FileRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(ImportUiState())
    val uiState: StateFlow<ImportUiState> = _uiState.asStateFlow()

    fun selectSource(source: ImportSource) {
        _uiState.update { it.copy(source = source) }
    }

    fun onFolderSelected(uri: Uri) {
        // Persist the URI permission so we can read it later
        context.contentResolver.takePersistableUriPermission(
            uri,
            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
        val folderName = uri.lastPathSegment?.substringAfterLast(':') ?: "Nuevo mazo"
        _uiState.update {
            it.copy(
                selectedUri = uri,
                deckName = folderName,
                phase = ImportPhase.CONFIGURE
            )
        }
    }

    fun onPdfSelected(uri: Uri) {
        context.contentResolver.takePersistableUriPermission(
            uri,
            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
        val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "Nuevo mazo"
        val deckName = fileName.removeSuffix(".pdf")
        _uiState.update {
            it.copy(
                selectedUri = uri,
                deckName = deckName,
                phase = ImportPhase.CONFIGURE
            )
        }
        renderPdfFirstPagePreview(uri)
        // Obtener conteo de páginas en background
        viewModelScope.launch(Dispatchers.IO) {
            val count = fileRepository.getPdfPageCount(uri)
            _uiState.update { it.copy(pdfPageCount = count) }
        }
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
                phase = ImportPhase.CONFIGURE
            )
        }
    }

    private fun renderPdfFirstPagePreview(uri: Uri) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val fd = context.contentResolver.openFileDescriptor(uri, "r") ?: return@launch
                val renderer = android.graphics.pdf.PdfRenderer(fd)
                if (renderer.pageCount == 0) {
                    renderer.close()
                    return@launch
                }
                val page = renderer.openPage(0)
                // Escalar a ~600px de ancho manteniendo proporción
                val targetWidth = 600
                val scale = targetWidth.toFloat() / page.width.coerceAtLeast(1)
                val bitmapWidth = targetWidth
                val bitmapHeight = (page.height * scale).toInt().coerceAtLeast(1)
                val bitmap = android.graphics.Bitmap.createBitmap(
                    bitmapWidth, bitmapHeight,
                    android.graphics.Bitmap.Config.ARGB_8888
                )
                // Rellenar fondo blanco (PdfRenderer usa fondo transparente por defecto)
                bitmap.eraseColor(android.graphics.Color.WHITE)
                page.render(bitmap, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                renderer.close()
                _uiState.update { it.copy(pdfPreviewBitmap = bitmap) }
            } catch (e: Throwable) {
                _uiState.update { it.copy(errorMessage = "No se pudo previsualizar el PDF: ${e.message}") }
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
                val autoTrim = state.pdfAutoTrimCells

                when (state.pdfLayoutMode) {
                    PdfLayoutMode.ALTERNATING_PAGES -> {
                        // Mostrar frentes (páginas pares) con la grilla configurada
                        outer@ for (page in 0 until pageCount step 2) {
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
                        outer@ for (page in 0 until pageCount) {
                            for (row in 0 until rows) {
                                for (col in 0 until cols) {
                                    if (bitmaps.size >= maxPreview) break@outer
                                    fileRepository.renderPdfGridCellToBitmap(
                                        uri, page, col, row, totalCols, rows,
                                        autoTrimCell = autoTrim
                                    )?.let { bitmaps += it }
                                    if (bitmaps.size < maxPreview) {
                                        fileRepository.renderPdfGridCellToBitmap(
                                            uri, page, col + cols, row, totalCols, rows,
                                            autoTrimCell = autoTrim
                                        )?.let { bitmaps += it }
                                    }
                                }
                            }
                        }
                    }
                    PdfLayoutMode.GRID -> {
                        outer@ for (page in 0 until pageCount) {
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
                        val half = pageCount / 2
                        outer@ for (page in 0 until half) {
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
                        phase = ImportPhase.PREVIEW
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
        _uiState.update { it.copy(phase = ImportPhase.CONFIGURE) }
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
    fun updatePdfAutoTrimCells(autoTrim: Boolean) = _uiState.update { it.copy(pdfAutoTrimCells = autoTrim) }

    fun startImport() {
        val state = _uiState.value
        val uri = state.selectedUri ?: return
        if (state.deckName.isBlank()) return

        _uiState.update { it.copy(phase = ImportPhase.IMPORTING, isImporting = true) }

        viewModelScope.launch {
            importDeckUseCase(
                uri = uri,
                deckName = state.deckName,
                source = state.source ?: ImportSource.FOLDER,
                defaultContentMode = state.defaultContentMode,
                pdfLayoutMode = state.pdfLayoutMode,
                pdfGridCols = state.pdfGridCols,
                pdfGridRows = state.pdfGridRows,
                pdfAutoTrimCells = state.pdfAutoTrimCells,
                onProgress = { progress, count ->
                    _uiState.update { it.copy(importProgress = progress, importedCardCount = count) }
                }
            ).fold(
                onSuccess = { deckId ->
                    _uiState.update {
                        it.copy(
                            phase = ImportPhase.SUCCESS,
                            isImporting = false,
                            importedDeckId = deckId
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            phase = ImportPhase.CONFIGURE,
                            isImporting = false,
                            errorMessage = error.message
                        )
                    }
                }
            )
        }
    }

    fun clearError() = _uiState.update { it.copy(errorMessage = null) }
}
