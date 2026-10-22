package com.deckapp.core.domain.repository

import android.graphics.Bitmap
import android.net.Uri

/**
 * Abstracción de operaciones de archivo para import.
 * Implementada en :core:data usando ContentResolver + almacenamiento interno de la app.
 */
interface FileRepository {

    suspend fun listImagesInFolder(folderUri: Uri): List<Pair<Uri, String>>
    suspend fun listPdfsInFolder(folderUri: Uri): List<Pair<Uri, String>>

    suspend fun copyImageToInternal(sourceUri: Uri, deckId: Long, fileName: String): String
    
    /** Copia imagen a una categoría general (ej: "npcs") en lugar de un mazo específico. */
    suspend fun copyImageToInternalByCategory(sourceUri: Uri, category: String, fileName: String): String

    /** Copia cualquier tipo de archivo a una categoría general. */
    suspend fun copyFileToInternalByCategory(sourceUri: Uri, category: String, fileName: String): String

    /**
     * Procesa una imagen para mapa táctico:
     * - Re-escala proporcionalmente si ancho o alto superan [maxDimension].
     * - Comprime a WebP con calidad [quality]%.
     * - Guarda en almacenamiento interno bajo `tactical_maps/`.
     * @return Ruta absoluta al archivo WebP resultante.
     */
    suspend fun processTacticalMapImage(
        sourceUri: Uri,
        maxDimension: Int = 2560,
        quality: Int = 85
    ): String

    suspend fun deleteImagesForDeck(deckId: Long)

    /** Borra un archivo específico dada su ruta absoluta en el almacenamiento interno. */
    suspend fun deleteFile(path: String)

    /**
     * Copia todas las imágenes del mazo [sourceDeckId] al directorio del mazo [targetDeckId].
     * @return Mapa de ruta original → ruta nueva (para reasignar imagePath en cartas duplicadas).
     */
    suspend fun duplicateDeckImages(sourceDeckId: Long, targetDeckId: Long): Map<String, String>

    // ── PDF ──────────────────────────────────────────────────────────────────

    /** Retorna la cantidad de páginas del PDF, o 0 si no se puede abrir. */
    suspend fun getPdfPageCount(uri: Uri): Int

    /**
     * Renderiza una página completa del PDF, la guarda en almacenamiento interno y retorna la ruta.
     * @param targetWidth ancho en píxeles del bitmap final (alto se calcula proporcionalmente).
     */
    suspend fun renderPdfPageAndSave(
        uri: Uri, pageIndex: Int,
        deckId: Long, fileName: String,
        targetWidth: Int = 1200
    ): String?

    /**
     * Renderiza una celda de una grilla N×M de una página PDF y la guarda.
     * @param pageRenderWidth ancho en px de la página completa antes de recortar.
     * @param autoTrimCell si true, recorta bordes blancos/vacíos de la celda antes de guardar.
     */
    suspend fun renderPdfGridCellAndSave(
        uri: Uri, pageIndex: Int,
        col: Int, row: Int, totalCols: Int, totalRows: Int,
        deckId: Long, fileName: String,
        pageRenderWidth: Int = 1800,
        autoTrimCell: Boolean = false,
        horizontalSplitRatio: Float = 0.5f
    ): String?

    /**
     * Igual que [renderPdfPageAndSave] pero retorna un Bitmap en memoria (sin guardar).
     * Usado para generar previsualizaciones. El caller es responsable de reciclar el Bitmap.
     */
    suspend fun renderPdfPageToBitmap(uri: Uri, pageIndex: Int, targetWidth: Int = 400): Bitmap?

    /**
     * Igual que [renderPdfGridCellAndSave] pero retorna Bitmap en memoria.
     * Usado para previsualizaciones de grilla.
     * @param autoTrimCell si true, recorta bordes blancos/vacíos de la celda.
     */
    suspend fun renderPdfGridCellToBitmap(
        uri: Uri, pageIndex: Int,
        col: Int, row: Int, totalCols: Int, totalRows: Int,
        pageRenderWidth: Int = 1200,
        autoTrimCell: Boolean = false,
        horizontalSplitRatio: Float = 0.5f
    ): Bitmap?

    // ── ZIP ──────────────────────────────────────────────────────────────────

    /**
     * Descomprime el contenido de un ZIP en un directorio temporal.
     * @return Lista de pares (Uri local temporal, nombre de archivo).
     */
    suspend fun unzipToTemp(zipUri: Uri): List<Pair<Uri, String>>

    /**
     * Comprime el directorio de imágenes del mazo [deckId] en el archivo [outputUri].
     * @param manifestJson JSON opcional con metadatos del mazo para incluir en el ZIP.
     */
    suspend fun zipDeckDirectory(deckId: Long, outputUri: Uri, manifestJson: String? = null): Result<Unit>

    /**
     * Limpia los archivos temporales de la app (caché de PDFs, ZIPs descomprimidos, etc.)
     */
    suspend fun clearCache()

    /**
     * Crea un archivo ZIP completo con el manifiesto, la base de datos en JSON y todas las imágenes.
     * @param outputUri URI destino (SAF)
     * @param manifestJson Contenido del manifest.json
     * @param databaseJson Contenido de deckapp_export.json
     */
    suspend fun createFullBackupZip(outputUri: Uri, manifestJson: String, databaseJson: String): Result<Unit>

    /**
     * Descomprime un archivo de backup en un directorio temporal y devuelve la ruta raíz.
     */
    suspend fun extractFullBackupZipToTemp(zipUri: Uri): String?

    /**
     * Mueve las imágenes restauradas desde el directorio temporal al almacenamiento interno definitivo.
     */
    suspend fun restoreImagesFromTemp(tempDirPath: String)


    /**
     * Verifica si un archivo existe en la ruta absoluta proporcionada.
     */
    suspend fun exists(path: String): Boolean

    /**
     * Lee el contenido de texto de un URI (archivo CSV, JSON, TXT) como String.
     * Usa el ContentResolver del sistema para acceder a archivos SAF.
     */
    suspend fun readTextFromUri(uri: Uri): String

    /**
     * Escribe una cadena de texto en un URI de SAF (JSON, CSV, etc.).
     */
    suspend fun writeTextToUri(uri: Uri, content: String): Result<Unit>

    /**
     * Inspecciona un archivo de backup ZIP sin descomprimirlo en la base de datos,
     * extrayendo el manifiesto y verificando el espacio libre disponible.
     */
    suspend fun inspectBackupZip(zipUri: Uri): Result<BackupInspectionData>

    /**
     * Retorna el espacio disponible en bytes en el almacenamiento interno de la app.
     */
    suspend fun getAvailableStorageBytes(): Long
}

data class BackupInspectionData(
    val manifest: com.deckapp.core.model.backup.BackupManifest,
    val fileSizeBytes: Long,
    val requiredSizeBytes: Long,
    val availableSizeBytes: Long
)
