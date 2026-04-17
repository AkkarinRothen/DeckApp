package com.deckapp.core.domain.usecase

import android.net.Uri
import com.deckapp.core.domain.repository.CardRepository
import com.deckapp.core.domain.repository.FileRepository
import com.deckapp.core.domain.util.FilenameParser
import com.deckapp.core.model.*
import javax.inject.Inject

/**
 * Importa un mazo al repositorio local.
 *
 * Fuente FOLDER:
 *   - Lee imágenes de la carpeta SAF (uri = tree URI con permiso persistido)
 *   - Copia cada imagen al almacenamiento interno de la app
 *   - Crea un Card + CardFace por imagen
 *   - Parsea metadatos desde el nombre de archivo (NNN_titulo[_palo].ext)
 *   - Usa la primera imagen como portada del mazo
 *
 * Fuente PDF:
 *   - :feature:import renderiza páginas con android.graphics.pdf.PdfRenderer
 *   - Soporta 4 modos: ALTERNATING_PAGES, SIDE_BY_SIDE, GRID, FIRST_HALF_FRONTS
 *   - Cada modo acepta grilla gridCols×gridRows para extraer múltiples cartas por página
 *
 * @param source  ImportSource enum pasado como Any para evitar dependencia circular con :feature:import
 */
class ImportDeckUseCase @Inject constructor(
    private val cardRepository: CardRepository,
    private val fileRepository: FileRepository
) {

    suspend operator fun invoke(
        uri: Uri,
        deckName: String,
        source: Any,
        defaultContentMode: CardContentMode,
        pdfLayoutMode: Any? = null,
        pdfGridCols: Int = 3,
        pdfGridRows: Int = 3,
        pdfSkipPages: Int = 0,
        pdfAutoTrimCells: Boolean = true,
        onProgress: (progress: Float, cardCount: Int) -> Unit = { _, _ -> },
        onFileError: (fileName: String) -> Unit = {}
    ): Result<Long> {
        return try {
            // Crear el stack inicial sin portada para obtener el deckId asignado por Room
            val stack = CardStack(
                name = deckName,
                type = StackType.DECK,
                description = "",
                sourceFolderPath = uri.toString(),
                defaultContentMode = defaultContentMode,
                tags = emptyList(),
                createdAt = System.currentTimeMillis()
            )
            val deckId = cardRepository.saveStack(stack)

            when (source.toString()) {
                "FOLDER" -> {
                    val images = fileRepository.listImagesInFolder(uri)
                    val coverPath = importFromImageList(images, deckId, defaultContentMode, onProgress, onFileError)
                    if (coverPath != null) {
                        cardRepository.updateStack(stack.copy(id = deckId, coverImagePath = coverPath))
                    }
                }
                "ZIP" -> {
                    val images = fileRepository.unzipToTemp(uri)
                    val coverPath = importFromImageList(images, deckId, defaultContentMode, onProgress, onFileError)
                    if (coverPath != null) {
                        cardRepository.updateStack(stack.copy(id = deckId, coverImagePath = coverPath))
                    }
                }
                "PDF" -> {
                    val coverPath = importFromPdf(
                        pdfUri = uri,
                        deckId = deckId,
                        contentMode = defaultContentMode,
                        layoutMode = pdfLayoutMode.toString(),
                        gridCols = pdfGridCols,
                        gridRows = pdfGridRows,
                        skipPages = pdfSkipPages,
                        autoTrimCells = pdfAutoTrimCells,
                        onProgress = onProgress
                    )
                    if (coverPath != null) {
                        cardRepository.updateStack(stack.copy(id = deckId, coverImagePath = coverPath))
                    }
                }
            }

            Result.success(deckId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Extrae cartas de un PDF según el modo de layout.
     *
     * Modos soportados (comparados via toString para evitar dependencia en :feature:import):
     * - ALTERNATING_PAGES: páginas 0,2,4… = frentes (las impares = dorsos, se ignoran en Fase 1)
     * - SIDE_BY_SIDE: cada página se parte por la mitad vertical (izq=frente, der=dorso → 2 caras)
     * - GRID: cada página se divide en [gridCols]×[gridRows] celdas, una carta por celda
     * - FIRST_HALF_FRONTS: las primeras pageCount/2 páginas son los frentes
     *
     * @return Ruta interna de la primera imagen (portada), o null si no se pudo procesar.
     */
    private suspend fun importFromPdf(
        pdfUri: Uri,
        deckId: Long,
        contentMode: CardContentMode,
        layoutMode: String,
        gridCols: Int,
        gridRows: Int,
        skipPages: Int = 0,
        autoTrimCells: Boolean = true,
        onProgress: (Float, Int) -> Unit
    ): String? {
        val pageCount = fileRepository.getPdfPageCount(pdfUri)
        if (pageCount <= skipPages) return null

        var coverPath: String? = null
        var cardIndex = 0

        suspend fun saveCard(imagePath: String, extraFaces: List<CardFace> = emptyList()) {
            val faces = listOf(
                CardFace(name = "Frente", imagePath = imagePath, contentMode = contentMode)
            ) + extraFaces
            val card = Card(
                stackId = deckId,
                originDeckId = deckId,
                title = "Carta ${cardIndex + 1}",
                faces = faces,
                sortOrder = cardIndex
            )
            cardRepository.saveCard(card)
            if (coverPath == null) coverPath = imagePath
            cardIndex++
        }

        when (layoutMode) {

            "ALTERNATING_PAGES" -> {
                // Páginas pares = grilla gridCols×gridRows de frentes
                // Páginas impares = grilla gridCols×gridRows de dorsos
                var pageIndex = skipPages
                while (pageIndex < pageCount) {
                    for (row in 0 until gridRows) {
                        for (col in 0 until gridCols) {
                            val frontPath = fileRepository.renderPdfGridCellAndSave(
                                pdfUri, pageIndex,
                                col, row, gridCols, gridRows,
                                deckId, "card_${cardIndex}_front.jpg",
                                autoTrimCell = autoTrimCells
                            )
                            if (frontPath != null) {
                                val backFaces = if (pageIndex + 1 < pageCount) {
                                    val backPath = fileRepository.renderPdfGridCellAndSave(
                                        pdfUri, pageIndex + 1,
                                        col, row, gridCols, gridRows,
                                        deckId, "card_${cardIndex}_back.jpg",
                                        autoTrimCell = autoTrimCells
                                    )
                                    if (backPath != null) listOf(CardFace("Dorso", backPath, contentMode))
                                    else emptyList()
                                } else emptyList()
                                saveCard(frontPath, backFaces)
                            }
                        }
                    }
                    pageIndex += 2
                    onProgress(pageIndex.toFloat() / pageCount, cardIndex)
                }
            }

            "SIDE_BY_SIDE" -> {
                // La página tiene gridCols*2 columnas totales:
                //   columnas 0..gridCols-1      = grilla de frentes
                //   columnas gridCols..2*gridCols-1 = grilla de dorsos
                val totalCols = gridCols * 2
                for (pageIndex in skipPages until pageCount) {
                    for (row in 0 until gridRows) {
                        for (col in 0 until gridCols) {
                            val frontPath = fileRepository.renderPdfGridCellAndSave(
                                pdfUri, pageIndex,
                                col, row, totalCols, gridRows,
                                deckId, "card_${cardIndex}_front.jpg",
                                autoTrimCell = autoTrimCells
                            )
                            if (frontPath != null) {
                                val backPath = fileRepository.renderPdfGridCellAndSave(
                                    pdfUri, pageIndex,
                                    col + gridCols, row, totalCols, gridRows,
                                    deckId, "card_${cardIndex}_back.jpg",
                                    autoTrimCell = autoTrimCells
                                )
                                val backFaces = if (backPath != null)
                                    listOf(CardFace("Dorso", backPath, contentMode))
                                else emptyList()
                                saveCard(frontPath, backFaces)
                            }
                        }
                    }
                    onProgress((pageIndex + 1).toFloat() / pageCount, cardIndex)
                }
            }

            "GRID" -> {
                for (pageIndex in skipPages until pageCount) {
                    for (row in 0 until gridRows) {
                        for (col in 0 until gridCols) {
                            val path = fileRepository.renderPdfGridCellAndSave(
                                pdfUri, pageIndex,
                                col, row, gridCols, gridRows,
                                deckId, "card_${cardIndex}.jpg",
                                autoTrimCell = autoTrimCells
                            )
                            if (path != null) saveCard(path)
                        }
                    }
                    onProgress((pageIndex + 1).toFloat() / pageCount, cardIndex)
                }
            }

            "FIRST_HALF_FRONTS" -> {
                // Primera mitad = frentes, segunda mitad = dorsos; ambas con grilla gridCols×gridRows
                // Si saltamos páginas, las saltamos del TOTAL, y recalculamos la mitad sobre lo que queda.
                val remainingPages = pageCount - skipPages
                val halfCount = remainingPages / 2
                for (pageOffset in 0 until halfCount) {
                    val pageIndex = skipPages + pageOffset
                    for (row in 0 until gridRows) {
                        for (col in 0 until gridCols) {
                            val frontPath = fileRepository.renderPdfGridCellAndSave(
                                pdfUri, pageIndex,
                                col, row, gridCols, gridRows,
                                deckId, "card_${cardIndex}_front.jpg",
                                autoTrimCell = autoTrimCells
                            )
                            if (frontPath != null) {
                                val backIndex = skipPages + halfCount + pageOffset
                                val backPath = if (backIndex < pageCount) {
                                    fileRepository.renderPdfGridCellAndSave(
                                        pdfUri, backIndex,
                                        col, row, gridCols, gridRows,
                                        deckId, "card_${cardIndex}_back.jpg",
                                        autoTrimCell = autoTrimCells
                                    )
                                } else null
                                val backFaces = if (backPath != null)
                                    listOf(CardFace("Dorso", backPath, contentMode))
                                else emptyList()
                                saveCard(frontPath, backFaces)
                            }
                        }
                    }
                    onProgress((pageOffset + 1).toFloat() / halfCount, cardIndex)
                }
            }

        }

        return coverPath
    }

    /**
     * Procesa una lista de imágenes: las copia al almacenamiento interno
     * y crea una carta por imagen.
     * @return Ruta interna de la primera imagen (para usarla como portada), o null si no hay imágenes.
     */
    private suspend fun importFromImageList(
        images: List<Pair<Uri, String>>,
        deckId: Long,
        contentMode: CardContentMode,
        onProgress: (Float, Int) -> Unit,
        onFileError: (String) -> Unit = {}
    ): String? {
        val total = images.size
        if (total == 0) return null

        var coverPath: String? = null
        var savedCount = 0

        images.forEachIndexed { index, (imageUri, displayName) ->
            try {
                val internalPath = fileRepository.copyImageToInternal(imageUri, deckId, displayName)
                if (savedCount == 0) coverPath = internalPath

                val metadata = FilenameParser.parse(displayName)

                val card = Card(
                    stackId = deckId,
                    originDeckId = deckId,
                    title = metadata.title,
                    suit = metadata.suit,
                    value = metadata.value,
                    faces = listOf(
                        CardFace(
                            name = "Frente",
                            imagePath = internalPath,
                            contentMode = contentMode,
                            zones = emptyList()
                        )
                    ),
                    sortOrder = savedCount,
                    tags = emptyList()
                )
                cardRepository.saveCard(card)
                savedCount++
            } catch (e: Exception) {
                onFileError(displayName)
            }
            onProgress((index + 1).toFloat() / total, savedCount)
        }

        return coverPath
    }
}
