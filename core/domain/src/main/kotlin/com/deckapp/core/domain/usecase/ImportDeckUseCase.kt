package com.deckapp.core.domain.usecase

import android.net.Uri
import com.deckapp.core.domain.repository.CardRepository
import com.deckapp.core.domain.repository.FileRepository
import com.deckapp.core.domain.util.FilenameParser
import com.deckapp.core.model.*
import kotlinx.serialization.json.Json
import java.io.File
import java.net.URI
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
    private val fileRepository: FileRepository,
    private val pdfLayoutProcessor: PdfLayoutProcessor
) {

    suspend operator fun invoke(
        uri: Uri,
        deckName: String,
        source: DeckImportSource,
        defaultContentMode: CardContentMode,
        pdfLayoutMode: PdfLayoutMode? = null,
        pdfGridCols: Int = 3,
        pdfGridRows: Int = 3,
        pdfSkipPages: Int = 0,
        pdfAutoTrimCells: Boolean = true,
        pdfSplitRatio: Float = 0.5f,
        onProgress: (progress: Float, cardCount: Int) -> Unit = { _, _ -> },
        onFileError: (fileName: String) -> Unit = {}
    ): Result<Long> {
        var deckId: Long = 0
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
            deckId = cardRepository.saveStack(stack)

            when (source) {
                DeckImportSource.FOLDER -> {
                    val images = fileRepository.listImagesInFolder(uri)
                    val coverPath = importFromImageList(images, deckId, defaultContentMode, onProgress, onFileError)
                    if (coverPath != null) {
                        cardRepository.updateStack(stack.copy(id = deckId, coverImagePath = coverPath))
                    }
                }
                DeckImportSource.ZIP -> {
                    val allFiles = fileRepository.unzipToTemp(uri)
                    
                    // Buscar el manifiesto (Sprint 16)
                    val manifestFile = allFiles.find { it.second == "deck_manifest.json" }
                    val manifest = manifestFile?.let { (uri, _) ->
                        try {
                            val content = fileRepository.readTextFromUri(uri)
                            Json { ignoreUnknownKeys = true }.decodeFromString<DeckManifest>(content)
                        } catch (e: Exception) { null }
                    }

                    if (manifest != null) {
                        // VALIDACIÓN: Verificar que todos los archivos referenciados existen en el ZIP
                        val missingFiles = manifest.cards.flatMap { c -> c.faces.map { it.fileName } }
                            .filter { fileName -> allFiles.none { it.second == fileName } }
                        
                        if (missingFiles.isNotEmpty()) {
                            throw Exception("ZIP incompleto. Faltan archivos: ${missingFiles.take(3).joinToString(", ")}...")
                        }

                        val coverPath = importFromManifest(allFiles, manifest, deckId, defaultContentMode, onProgress)
                        if (coverPath != null) {
                            cardRepository.updateStack(stack.copy(
                                id = deckId, 
                                coverImagePath = coverPath,
                                description = manifest.description,
                                drawMode = DrawMode.valueOf(manifest.drawMode),
                                aspectRatio = CardAspectRatio.valueOf(manifest.aspectRatio)
                            ))
                        }
                    } else {
                        // Fallback: solo imágenes
                        val images = allFiles.filter { FilenameParser.isImage(it.second) }
                        val coverPath = importFromImageList(images, deckId, defaultContentMode, onProgress, onFileError)
                        if (coverPath != null) {
                            cardRepository.updateStack(stack.copy(id = deckId, coverImagePath = coverPath))
                        }
                    }
                }
                DeckImportSource.PDF -> {
                    val coverPath = importFromPdf(
                        pdfUri = uri,
                        deckId = deckId,
                        contentMode = defaultContentMode,
                        layoutMode = pdfLayoutMode ?: PdfLayoutMode.ALTERNATING_PAGES,
                        gridCols = pdfGridCols,
                        gridRows = pdfGridRows,
                        skipPages = pdfSkipPages,
                        autoTrimCells = pdfAutoTrimCells,
                        splitRatio = pdfSplitRatio,
                        onProgress = onProgress
                    )
                    if (coverPath != null) {
                        cardRepository.updateStack(stack.copy(id = deckId, coverImagePath = coverPath))
                    }
                }
            }

            Result.success(deckId)
        } catch (e: Exception) {
            // ROLLBACK: Si algo falla a mitad del proceso, eliminamos el mazo y sus imágenes parciales
            if (deckId > 0) {
                cardRepository.deleteStack(deckId)
                fileRepository.deleteImagesForDeck(deckId)
            }
            Result.failure(e)
        }
    }

    /**
     * Extrae cartas de un PDF delegando en el procesador especializado.
     */
    private suspend fun importFromPdf(
        pdfUri: Uri,
        deckId: Long,
        contentMode: CardContentMode,
        layoutMode: PdfLayoutMode,
        gridCols: Int,
        gridRows: Int,
        skipPages: Int = 0,
        autoTrimCells: Boolean = true,
        splitRatio: Float = 0.5f,
        onProgress: (Float, Int) -> Unit
    ): String? {
        var coverPath: String? = null

        pdfLayoutProcessor.process(
            pdfUri = pdfUri,
            deckId = deckId,
            contentMode = contentMode,
            layoutMode = layoutMode,
            gridCols = gridCols,
            gridRows = gridRows,
            skipPages = skipPages,
            autoTrimCells = autoTrimCells,
            splitRatio = splitRatio,
            onProgress = onProgress,
            saveCard = { frontPath, backFaces, index ->
                val faces = listOf(
                    CardFace(name = "Frente", imagePath = frontPath, contentMode = contentMode)
                ) + backFaces
                val card = Card(
                    stackId = deckId,
                    originDeckId = deckId,
                    title = "Carta ${index + 1}",
                    faces = faces,
                    sortOrder = index
                )
                cardRepository.saveCard(card)
                if (coverPath == null) coverPath = frontPath
            }
        )

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

    /**
     * Importa cartas usando la información detallada del manifiesto.
     */
    private suspend fun importFromManifest(
        allFiles: List<Pair<Uri, String>>,
        manifest: DeckManifest,
        deckId: Long,
        contentMode: CardContentMode,
        onProgress: (Float, Int) -> Unit
    ): String? {
        val total = manifest.cards.size
        if (total == 0) return null

        var coverPath: String? = null
        var savedCount = 0

        manifest.cards.forEachIndexed { index, cardManifest ->
            val faces = cardManifest.faces.map { faceManifest ->
                val matchingFile = allFiles.find { it.second == faceManifest.fileName }
                val internalPath = matchingFile?.let { (uri, name) ->
                    fileRepository.copyImageToInternal(uri, deckId, name)
                }
                
                CardFace(
                    name = faceManifest.name,
                    imagePath = internalPath,
                    contentMode = CardContentMode.valueOf(faceManifest.contentMode)
                )
            }

            val card = Card(
                stackId = deckId,
                originDeckId = deckId,
                title = cardManifest.title,
                suit = cardManifest.suit,
                value = cardManifest.value?.toIntOrNull(),
                dmNotes = cardManifest.dmNotes,
                faces = faces,
                sortOrder = savedCount
            )

            cardRepository.saveCard(card)
            if (coverPath == null) coverPath = faces.firstOrNull()?.imagePath
            savedCount++
            onProgress((index + 1).toFloat() / total, savedCount)
        }

        return coverPath
    }
}
