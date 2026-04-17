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

    suspend fun deleteImagesForDeck(deckId: Long)

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
     */
    suspend fun zipDeckDirectory(deckId: Long, outputUri: Uri): Result<Unit>

    /**
     * Limpia los archivos temporales de la app (caché de PDFs, ZIPs descomprimidos, etc.)
     */
    suspend fun clearCache()

    /**
     * Lee el contenido de texto de un URI (archivo CSV, JSON, TXT) como String.
     * Usa el ContentResolver del sistema para acceder a archivos SAF.
     */
    suspend fun readTextFromUri(uri: Uri): String
}
