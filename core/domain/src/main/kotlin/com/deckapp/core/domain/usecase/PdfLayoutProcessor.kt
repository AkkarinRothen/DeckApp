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
     * @param saveCard Callback para persistir cada carta detectada.
     */
    suspend fun process(
        pdfUri: Uri,
        deckId: Long,
        contentMode: CardContentMode,
        layoutMode: PdfLayoutMode,
        gridCols: Int,
        gridRows: Int,
        skipPages: Int,
        autoTrimCells: Boolean,
        splitRatio: Float,
        onProgress: (progress: Float, count: Int) -> Unit,
        saveCard: suspend (frontPath: String, backFaces: List<CardFace>, index: Int) -> Unit
    ) = withContext(Dispatchers.IO) {
        val pageCount = fileRepository.getPdfPageCount(pdfUri)
        if (pageCount <= skipPages) return@withContext

        var cardIndex = 0

        when (layoutMode) {
            PdfLayoutMode.ALTERNATING_PAGES -> {
                var pageIndex = skipPages
                while (pageIndex < pageCount) {
                    for (row in 0 until gridRows) {
                        for (col in 0 until gridCols) {
                            val frontPath = fileRepository.renderPdfGridCellAndSave(
                                uri = pdfUri, pageIndex = pageIndex,
                                col = col, row = row, totalCols = gridCols, totalRows = gridRows,
                                deckId = deckId, fileName = "card_${cardIndex}_front.jpg",
                                pageRenderWidth = 1800, autoTrimCell = autoTrimCells
                            )
                            if (frontPath != null) {
                                val backFaces = if (pageIndex + 1 < pageCount) {
                                    val backPath = fileRepository.renderPdfGridCellAndSave(
                                        uri = pdfUri, pageIndex = pageIndex + 1,
                                        col = col, row = row, totalCols = gridCols, totalRows = gridRows,
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
                    onProgress(pageIndex.toFloat() / pageCount, cardIndex)
                }
            }

            PdfLayoutMode.SIDE_BY_SIDE -> {
                val totalCols = gridCols * 2
                for (pageIndex in skipPages until pageCount) {
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
                    onProgress((pageIndex + 1).toFloat() / pageCount, cardIndex)
                }
            }

            PdfLayoutMode.GRID -> {
                for (pageIndex in skipPages until pageCount) {
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
                    onProgress((pageIndex + 1).toFloat() / pageCount, cardIndex)
                }
            }

            PdfLayoutMode.FIRST_HALF_FRONTS -> {
                val remainingPages = pageCount - skipPages
                val halfCount = remainingPages / 2
                for (pageOffset in 0 until halfCount) {
                    val pageIndex = skipPages + pageOffset
                    for (row in 0 until gridRows) {
                        for (col in 0 until gridCols) {
                            val frontPath = fileRepository.renderPdfGridCellAndSave(
                                uri = pdfUri, pageIndex = pageIndex,
                                col = col, row = row, totalCols = gridCols, totalRows = gridRows,
                                deckId = deckId, fileName = "card_${cardIndex}_front.jpg",
                                pageRenderWidth = 1800, autoTrimCell = autoTrimCells
                            )
                            if (frontPath != null) {
                                val backIndex = skipPages + halfCount + pageOffset
                                val backPath = if (backIndex < pageCount) {
                                    fileRepository.renderPdfGridCellAndSave(
                                        uri = pdfUri, pageIndex = backIndex,
                                        col = col, row = row, totalCols = gridCols, totalRows = gridRows,
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
