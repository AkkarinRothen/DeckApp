package com.deckapp.core.domain.usecase

import android.net.Uri
import com.deckapp.core.domain.repository.FileRepository
import com.deckapp.core.model.CardContentMode
import com.deckapp.core.model.CardFace
import com.deckapp.core.model.PdfLayoutMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Procesador especializado en la extracción de cartas desde PDFs.
 * Desacopla la lógica de layouts y asegura el procesamiento en hilos de E/S.
 */
class PdfLayoutProcessor @Inject constructor(
    private val fileRepository: FileRepository
) {

    /**
     * Procesa el PDF y extrae las cartas según el layout indicado.
     * @param startPage Página inicial (0-indexed). Si no se especifica, toma skipPages.
     * @param endPage Página final inclusiva (0-indexed). Si es null, procesa hasta la última página.
     * @param saveCard Callback para persistir cada carta detectada.
     */
    suspend fun process(
        pdfUri: Uri,
        deckId: Long,
        contentMode: CardContentMode,
        layoutMode: PdfLayoutMode,
        gridCols: Int,
        gridRows: Int,
        skipPages: Int = 0,
        startPage: Int = 0,
        endPage: Int? = null,
        autoTrimCells: Boolean,
        splitRatio: Float,
        onProgress: (progress: Float, count: Int) -> Unit,
        saveCard: suspend (frontPath: String, backFaces: List<CardFace>, index: Int) -> Unit
    ) = withContext(Dispatchers.IO) {
        val pageCount = fileRepository.getPdfPageCount(pdfUri)
        if (pageCount <= 0) return@withContext

        val effectiveStart = (if (startPage > 0) startPage else skipPages).coerceAtLeast(0)
        val requestedEnd = endPage ?: (pageCount - 1)
        if (requestedEnd < effectiveStart || effectiveStart >= pageCount) return@withContext
        val effectiveEnd = requestedEnd.coerceAtMost(pageCount - 1)
        val totalRangePages = effectiveEnd - effectiveStart + 1

        var cardIndex = 0

        when (layoutMode) {
            PdfLayoutMode.ALTERNATING_PAGES -> {
                var pageIndex = effectiveStart
                while (pageIndex <= effectiveEnd) {
                    for (row in 0 until gridRows) {
                        for (col in 0 until gridCols) {
                            val frontPath = fileRepository.renderPdfGridCellAndSave(
                                uri = pdfUri, pageIndex = pageIndex,
                                col = col, row = row, totalCols = gridCols, totalRows = gridRows,
                                deckId = deckId, fileName = "card_${cardIndex}_front.jpg",
                                pageRenderWidth = 1800, autoTrimCell = autoTrimCells
                            )
                            if (frontPath != null) {
                                val backFaces = if (pageIndex + 1 <= effectiveEnd) {
                                    val backCol = if (gridCols > 1) gridCols - 1 - col else col
                                    val backPath = fileRepository.renderPdfGridCellAndSave(
                                        uri = pdfUri, pageIndex = pageIndex + 1,
                                        col = backCol, row = row, totalCols = gridCols, totalRows = gridRows,
                                        deckId = deckId, fileName = "card_${cardIndex}_back.jpg",
                                        pageRenderWidth = 1800, autoTrimCell = autoTrimCells
                                    )
                                    if (backPath != null) listOf(CardFace("Dorso", backPath, contentMode))
                                    else emptyList()
                                } else emptyList()
                                saveCard(frontPath, backFaces, cardIndex++)
                            }
                        }
                    }
                    pageIndex += 2
                    onProgress(minOf(1f, (pageIndex - effectiveStart).toFloat() / totalRangePages), cardIndex)
                }
            }

            PdfLayoutMode.SIDE_BY_SIDE -> {
                val totalCols = gridCols * 2
                for (pageIndex in effectiveStart..effectiveEnd) {
                    for (row in 0 until gridRows) {
                        for (col in 0 until gridCols) {
                            val frontPath = fileRepository.renderPdfGridCellAndSave(
                                uri = pdfUri, pageIndex = pageIndex,
                                col = col, row = row, totalCols = totalCols, totalRows = gridRows,
                                deckId = deckId, fileName = "card_${cardIndex}_front.jpg",
                                pageRenderWidth = 1800, autoTrimCell = autoTrimCells,
                                horizontalSplitRatio = splitRatio
                            )
                            if (frontPath != null) {
                                val backPath = fileRepository.renderPdfGridCellAndSave(
                                    uri = pdfUri, pageIndex = pageIndex,
                                    col = col + gridCols, row = row, totalCols = totalCols, totalRows = gridRows,
                                    deckId = deckId, fileName = "card_${cardIndex}_back.jpg",
                                    pageRenderWidth = 1800, autoTrimCell = autoTrimCells,
                                    horizontalSplitRatio = splitRatio
                                )
                                val backFaces = if (backPath != null)
                                    listOf(CardFace("Dorso", backPath, contentMode))
                                else emptyList()
                                saveCard(frontPath, backFaces, cardIndex++)
                            }
                        }
                    }
                    onProgress((pageIndex - effectiveStart + 1).toFloat() / totalRangePages, cardIndex)
                }
            }

            PdfLayoutMode.GRID -> {
                for (pageIndex in effectiveStart..effectiveEnd) {
                    for (row in 0 until gridRows) {
                        for (col in 0 until gridCols) {
                            val path = fileRepository.renderPdfGridCellAndSave(
                                uri = pdfUri, pageIndex = pageIndex,
                                col = col, row = row, totalCols = gridCols, totalRows = gridRows,
                                deckId = deckId, fileName = "card_${cardIndex}.jpg",
                                pageRenderWidth = 1800, autoTrimCell = autoTrimCells
                            )
                            if (path != null) {
                                saveCard(path, emptyList(), cardIndex++)
                            }
                        }
                    }
                    onProgress((pageIndex - effectiveStart + 1).toFloat() / totalRangePages, cardIndex)
                }
            }

            PdfLayoutMode.FIRST_HALF_FRONTS -> {
                val halfCount = totalRangePages / 2
                if (halfCount <= 0) return@withContext
                for (pageOffset in 0 until halfCount) {
                    val pageIndex = effectiveStart + pageOffset
                    for (row in 0 until gridRows) {
                        for (col in 0 until gridCols) {
                            val frontPath = fileRepository.renderPdfGridCellAndSave(
                                uri = pdfUri, pageIndex = pageIndex,
                                col = col, row = row, totalCols = gridCols, totalRows = gridRows,
                                deckId = deckId, fileName = "card_${cardIndex}_front.jpg",
                                pageRenderWidth = 1800, autoTrimCell = autoTrimCells
                            )
                            if (frontPath != null) {
                                val backIndex = effectiveStart + halfCount + pageOffset
                                val backPath = if (backIndex <= effectiveEnd) {
                                    val backCol = if (gridCols > 1) gridCols - 1 - col else col
                                    fileRepository.renderPdfGridCellAndSave(
                                        uri = pdfUri, pageIndex = backIndex,
                                        col = backCol, row = row, totalCols = gridCols, totalRows = gridRows,
                                        deckId = deckId, fileName = "card_${cardIndex}_back.jpg",
                                        pageRenderWidth = 1800, autoTrimCell = autoTrimCells
                                    )
                                } else null
                                val backFaces = if (backPath != null)
                                    listOf(CardFace("Dorso", backPath, contentMode))
                                else emptyList()
                                saveCard(frontPath, backFaces, cardIndex++)
                            }
                        }
                    }
                    onProgress((pageOffset + 1).toFloat() / halfCount, cardIndex)
                }
            }
        }
    }
}
