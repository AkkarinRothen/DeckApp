package com.deckapp.core.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import com.deckapp.core.domain.repository.FileRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Implementación de [FileRepository] usando SAF (Storage Access Framework) y
 * almacenamiento interno de la app.
 */
class FileRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : FileRepository {


    // Extensiones de imagen soportadas
    private val imageMimeTypes = setOf(
        "image/jpeg", "image/jpg", "image/png",
        "image/webp", "image/gif", "image/bmp"
    )

    override suspend fun listImagesInFolder(folderUri: Uri): List<Pair<Uri, String>> {
        // ... (resto del método)
        val result = mutableListOf<Pair<Uri, String>>()

        val treeDocId = DocumentsContract.getTreeDocumentId(folderUri)
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(folderUri, treeDocId)

        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE
        )

        context.contentResolver.query(
            childrenUri, projection, null, null,
            "${DocumentsContract.Document.COLUMN_DISPLAY_NAME} ASC"
        )?.use { cursor ->
            val idCol   = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val mimeCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)

            while (cursor.moveToNext()) {
                val mimeType = cursor.getString(mimeCol) ?: continue
                if (mimeType !in imageMimeTypes) continue

                val docId = cursor.getString(idCol) ?: continue
                val displayName = cursor.getString(nameCol) ?: continue

                val documentUri = DocumentsContract.buildDocumentUriUsingTree(folderUri, docId)
                result.add(documentUri to displayName)
            }
        }

        return result
    }

    override suspend fun listPdfsInFolder(folderUri: Uri): List<Pair<Uri, String>> {
        val result = mutableListOf<Pair<Uri, String>>()

        val treeDocId = DocumentsContract.getTreeDocumentId(folderUri)
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(folderUri, treeDocId)

        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE
        )

        context.contentResolver.query(
            childrenUri, projection, null, null,
            "${DocumentsContract.Document.COLUMN_DISPLAY_NAME} ASC"
        )?.use { cursor ->
            val idCol   = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val mimeCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)

            while (cursor.moveToNext()) {
                val mimeType = cursor.getString(mimeCol) ?: continue
                if (mimeType != "application/pdf") continue

                val docId = cursor.getString(idCol) ?: continue
                val displayName = cursor.getString(nameCol) ?: continue

                val documentUri = DocumentsContract.buildDocumentUriUsingTree(folderUri, docId)
                result.add(documentUri to displayName)
            }
        }

        return result
    }

    override suspend fun copyImageToInternal(
        sourceUri: Uri,
        deckId: Long,
        fileName: String
    ): String {
        val deckDir = File(context.filesDir, "decks/$deckId")
        deckDir.mkdirs()
        val destFile = File(deckDir, sanitizeFileName(fileName))

        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output, bufferSize = DEFAULT_BUFFER_SIZE)
            }
        }

        return destFile.absolutePath
    }

    override suspend fun copyImageToInternalByCategory(
        sourceUri: Uri,
        category: String,
        fileName: String
    ): String {
        val categoryDir = File(context.filesDir, category)
        categoryDir.mkdirs()
        val destFile = File(categoryDir, sanitizeFileName(fileName))

        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output, bufferSize = DEFAULT_BUFFER_SIZE)
            }
        }

        return destFile.absolutePath
    }

    override suspend fun copyFileToInternalByCategory(
        sourceUri: Uri,
        category: String,
        fileName: String
    ): String {
        val categoryDir = File(context.filesDir, category)
        categoryDir.mkdirs()
        val destFile = File(categoryDir, sanitizeFileName(fileName))

        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output, bufferSize = DEFAULT_BUFFER_SIZE)
            }
        }

        return destFile.absolutePath
    }

    override suspend fun deleteImagesForDeck(deckId: Long) {
        val deckDir = File(context.filesDir, "decks/$deckId")
        if (deckDir.exists()) deckDir.deleteRecursively()
    }

    override suspend fun deleteFile(path: String) {
        val file = File(path)
        if (file.exists() && file.isFile) {
            file.delete()
        }
    }

    override suspend fun duplicateDeckImages(
        sourceDeckId: Long,
        targetDeckId: Long
    ): Map<String, String> {
        val sourceDir = File(context.filesDir, "decks/$sourceDeckId")
        if (!sourceDir.exists()) return emptyMap()

        val targetDir = File(context.filesDir, "decks/$targetDeckId")
        targetDir.mkdirs()

        val pathMap = mutableMapOf<String, String>()
        sourceDir.listFiles()?.forEach { sourceFile ->
            val destFile = File(targetDir, sourceFile.name)
            sourceFile.copyTo(destFile, overwrite = true)
            pathMap[sourceFile.absolutePath] = destFile.absolutePath
        }
        return pathMap
    }

    // ── PDF ──────────────────────────────────────────────────────────────────

    override suspend fun getPdfPageCount(uri: Uri): Int {
        val fd = context.contentResolver.openFileDescriptor(uri, "r") ?: return 0
        return try {
            val renderer = PdfRenderer(fd)
            val count = renderer.pageCount
            renderer.close()
            count
        } catch (e: Throwable) {
            0
        } finally {
            fd.close()
        }
    }

    override suspend fun renderPdfPageAndSave(
        uri: Uri, pageIndex: Int,
        deckId: Long, fileName: String, targetWidth: Int
    ): String? = try {
        val bitmap = renderPage(uri, pageIndex, targetWidth) ?: return null
        saveBitmapToInternal(bitmap, deckId, fileName)
    } catch (e: Throwable) { null }

    override suspend fun renderPdfGridCellAndSave(
        uri: Uri, pageIndex: Int,
        col: Int, row: Int, totalCols: Int, totalRows: Int,
        deckId: Long, fileName: String, pageRenderWidth: Int,
        autoTrimCell: Boolean,
        horizontalSplitRatio: Float
    ): String? = try {
        val cell = renderGridCell(uri, pageIndex, col, row, totalCols, totalRows, pageRenderWidth, autoTrimCell, horizontalSplitRatio)
            ?: return null
        saveBitmapToInternal(cell, deckId, fileName)
    } catch (e: Throwable) { null }

    override suspend fun renderPdfPageToBitmap(uri: Uri, pageIndex: Int, targetWidth: Int): Bitmap? =
        try { renderPage(uri, pageIndex, targetWidth) } catch (e: Throwable) { null }

    override suspend fun renderPdfGridCellToBitmap(
        uri: Uri, pageIndex: Int,
        col: Int, row: Int, totalCols: Int, totalRows: Int,
        pageRenderWidth: Int,
        autoTrimCell: Boolean,
        horizontalSplitRatio: Float
    ): Bitmap? = try {
        renderGridCell(uri, pageIndex, col, row, totalCols, totalRows, pageRenderWidth, autoTrimCell, horizontalSplitRatio)
    } catch (e: Throwable) { null }

    // ── ZIP ──────────────────────────────────────────────────────────────────

    override suspend fun unzipToTemp(zipUri: Uri): List<Pair<Uri, String>> {
        val result = mutableListOf<Pair<Uri, String>>()
        val tempDir = File(context.cacheDir, "unzip_${UUID.randomUUID()}")
        tempDir.mkdirs()

        context.contentResolver.openInputStream(zipUri)?.use { input ->
            ZipInputStream(BufferedInputStream(input)).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val fileName = File(entry.name).name // Solo el nombre, sin carpetas
                        val destFile = File(tempDir, fileName)
                        
                        // Solo procesamos imágenes y el manifiesto (Sprint 16)
                        val ext = fileName.substringAfterLast(".", "").lowercase()
                        if (ext in setOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "json")) {
                            BufferedOutputStream(FileOutputStream(destFile)).use { bos ->
                                zis.copyTo(bos)
                            }
                            result.add(Uri.fromFile(destFile) to fileName)
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
        }
        return result
    }

    override suspend fun zipDeckDirectory(deckId: Long, outputUri: Uri, manifestJson: String?): Result<Unit> = runCatching {
        val deckDir = File(context.filesDir, "decks/$deckId")
        if (!deckDir.exists()) throw Exception("Deck directory not found")

        context.contentResolver.openOutputStream(outputUri)?.use { output ->
            ZipOutputStream(BufferedOutputStream(output)).use { zos ->
                // Guardar manifiesto si existe (Sprint 16)
                manifestJson?.let { json ->
                    val manifestEntry = ZipEntry("deck_manifest.json")
                    zos.putNextEntry(manifestEntry)
                    zos.write(json.toByteArray())
                    zos.closeEntry()
                }

                deckDir.listFiles()?.forEach { file ->
                    if (file.isFile) {
                        val entry = ZipEntry(file.name)
                        zos.putNextEntry(entry)
                        FileInputStream(file).use { fis ->
                            fis.copyTo(zos)
                        }
                        zos.closeEntry()
                    }
                }
            }
        }
    }

    override suspend fun clearCache() {
        context.cacheDir.deleteRecursively()
        context.cacheDir.mkdirs() // Restaurar el directorio para uso futuro
    }

    override suspend fun createFullBackupZip(outputUri: Uri, manifestJson: String, databaseJson: String): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            context.contentResolver.openOutputStream(outputUri)?.use { output ->
                ZipOutputStream(BufferedOutputStream(output)).use { zos ->
                    // 1. Escribir Manifest
                    zos.putNextEntry(ZipEntry("manifest.json"))
                    zos.write(manifestJson.toByteArray())
                    zos.closeEntry()

                    // 2. Escribir Database
                    val dbEntry = ZipEntry("database/deckapp_export.json")
                    zos.putNextEntry(dbEntry)
                    zos.write(databaseJson.toByteArray())
                    zos.closeEntry()

                    // 3. Empaquetar imágenes (decks, npcs, tables, etc.)
                    val imagesRoot = context.filesDir
                    // Solo tomamos directorios clave para no empaquetar basura
                    val targetDirs = listOf("decks", "npcs", "tables")
                    
                    targetDirs.forEach { dirName ->
                        val dir = File(imagesRoot, dirName)
                        if (dir.exists()) {
                            dir.walkTopDown().filter { it.isFile }.forEach { file ->
                                // Construir ruta relativa ej: "images/decks/1/cover.jpg"
                                val relativePath = "images/${file.toRelativeString(imagesRoot).replace('\\', '/')}"
                                zos.putNextEntry(ZipEntry(relativePath))
                                FileInputStream(file).use { fis -> fis.copyTo(zos) }
                                zos.closeEntry()
                            }
                        }
                    }
                }
            } ?: throw Exception("No se pudo abrir el stream de salida.")
        }
    }

    override suspend fun extractFullBackupZipToTemp(zipUri: Uri): String? = withContext(Dispatchers.IO) {
        val tempDir = File(context.cacheDir, "restore_${UUID.randomUUID()}")
        tempDir.mkdirs()

        try {
            context.contentResolver.openInputStream(zipUri)?.use { input ->
                ZipInputStream(BufferedInputStream(input)).use { zis ->
                    var entry: ZipEntry? = zis.nextEntry
                    while (entry != null) {
                        if (!entry.isDirectory) {
                            val destFile = File(tempDir, entry.name)
                            destFile.parentFile?.mkdirs()
                            BufferedOutputStream(FileOutputStream(destFile)).use { bos ->
                                zis.copyTo(bos)
                            }
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
            }
            tempDir.absolutePath
        } catch (e: Exception) {
            tempDir.deleteRecursively()
            null
        }
    }

    override suspend fun restoreImagesFromTemp(tempDirPath: String) = withContext(Dispatchers.IO) {
        val tempDir = File(tempDirPath)
        val tempImagesDir = File(tempDir, "images")
        if (!tempImagesDir.exists()) return@withContext

        // Limpiar imágenes actuales
        val targetDirs = listOf("decks", "npcs", "tables")
        targetDirs.forEach { dirName ->
            val dir = File(context.filesDir, dirName)
            dir.deleteRecursively()
        }

        // Copiar las nuevas desde temp/images a filesDir/
        tempImagesDir.walkTopDown().filter { it.isFile }.forEach { tempFile ->
            val relativePath = tempFile.toRelativeString(tempImagesDir)
            val finalFile = File(context.filesDir, relativePath)
            finalFile.parentFile?.mkdirs()
            tempFile.copyTo(finalFile, overwrite = true)
        }
    }


    override suspend fun exists(path: String): Boolean {
        return File(path).exists()
    }

    // ── Helpers privados ──────────────────────────────────────────────────────

    /** Renderiza una página completa a Bitmap usando PdfRenderer nativo. */
    private fun renderPage(uri: Uri, pageIndex: Int, targetWidth: Int): Bitmap? {
        val fd = context.contentResolver.openFileDescriptor(uri, "r") ?: return null
        return try {
            val renderer = PdfRenderer(fd)
            if (pageIndex < 0 || pageIndex >= renderer.pageCount) {
                renderer.close()
                return null
            }
            
            val page = renderer.openPage(pageIndex)
            val originalW = page.width
            val originalH = page.height
            
            val scale = targetWidth.toFloat() / originalW.coerceAtLeast(1)
            val bH = (originalH * scale).toInt().coerceAtLeast(1)
            
            val bitmap = Bitmap.createBitmap(targetWidth, bH, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(Color.WHITE)
            
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            
            page.close()
            renderer.close()
            bitmap
        } catch (e: Exception) {
            null
        } finally {
            fd.close()
        }
    }

    /** Renderiza la celda [col],[row] de una grilla [totalCols]×[totalRows] de una página. */
    private fun renderGridCell(
        uri: Uri, pageIndex: Int,
        col: Int, row: Int, totalCols: Int, totalRows: Int,
        pageRenderWidth: Int,
        autoTrim: Boolean = false,
        horizontalSplitRatio: Float = 0.5f
    ): Bitmap? {
        val fullPage = renderPage(uri, pageIndex, pageRenderWidth) ?: return null
        
        val cellW: Int
        val x: Int
        
        if (totalCols % 2 == 0) {
            // Lógica de pares (side-by-side): tratamos cada 2 columnas como una unidad
            val unitW = fullPage.width / (totalCols / 2)
            if (col % 2 == 0) {
                // Parte izquierda (Frente)
                x = (col / 2) * unitW
                cellW = (unitW * horizontalSplitRatio).toInt()
            } else {
                // Parte derecha (Dorso)
                val splitOffset = (unitW * horizontalSplitRatio).toInt()
                x = (col / 2) * unitW + splitOffset
                cellW = unitW - splitOffset
            }
        } else {
            // Grilla estándar equitativa
            cellW = fullPage.width / totalCols
            x = col * cellW
        }

        val cellH = fullPage.height / totalRows
        val y = row * cellH
        val safeW = cellW.coerceAtMost(fullPage.width - x).coerceAtLeast(1)
        val safeH = cellH.coerceAtMost(fullPage.height - y).coerceAtLeast(1)
        val cell = Bitmap.createBitmap(fullPage, x, y, safeW, safeH)
        fullPage.recycle()
        return if (autoTrim) cell.trimWhiteBorders() else cell
    }

    /**
     * Recorta filas y columnas blancas o casi-blancas del borde del bitmap.
     * Útil para eliminar el espacio sobrante en celdas de grilla que no llenan todo el área.
     * No modifica el bitmap original; retorna un nuevo bitmap si hubo recorte, o el mismo si no.
     */
    private fun Bitmap.trimWhiteBorders(threshold: Int = 245): Bitmap {
        val w = width
        val h = height
        if (w <= 2 || h <= 2) return this

        val pixels = IntArray(w * h)
        getPixels(pixels, 0, w, 0, 0, w, h)

        fun isLight(pixel: Int): Boolean =
            Color.red(pixel) >= threshold &&
            Color.green(pixel) >= threshold &&
            Color.blue(pixel) >= threshold

        var top = 0
        topSearch@ for (y in 0 until h) {
            for (x in 0 until w) {
                if (!isLight(pixels[y * w + x])) { top = y; break@topSearch }
            }
        }

        var bottom = h - 1
        bottomSearch@ for (y in h - 1 downTo top) {
            for (x in 0 until w) {
                if (!isLight(pixels[y * w + x])) { bottom = y; break@bottomSearch }
            }
        }

        var left = 0
        leftSearch@ for (x in 0 until w) {
            for (y in top..bottom) {
                if (!isLight(pixels[y * w + x])) { left = x; break@leftSearch }
            }
        }

        var right = w - 1
        rightSearch@ for (x in w - 1 downTo left) {
            for (y in top..bottom) {
                if (!isLight(pixels[y * w + x])) { right = x; break@rightSearch }
            }
        }

        val newW = (right - left + 1).coerceAtLeast(1)
        val newH = (bottom - top + 1).coerceAtLeast(1)
        return if (left > 0 || top > 0 || newW < w || newH < h)
            Bitmap.createBitmap(this, left, top, newW, newH)
        else
            this
    }

    /** Lee la calidad JPEG configurada por el usuario (default 90). */
    private fun getJpegQuality(): Int =
        context.getSharedPreferences("deckapp_settings", android.content.Context.MODE_PRIVATE)
            .getInt("jpeg_quality", 90)

    /** Guarda un Bitmap como JPEG en {filesDir}/decks/{deckId}/{fileName}. Recicla el bitmap. */
    private fun saveBitmapToInternal(bitmap: Bitmap, deckId: Long, fileName: String): String {
        val deckDir = File(context.filesDir, "decks/$deckId")
        deckDir.mkdirs()
        val file = File(deckDir, sanitizeFileName(fileName))
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, getJpegQuality(), out)
        }
        bitmap.recycle()
        return file.absolutePath
    }

    private fun sanitizeFileName(name: String): String =
        name.replace(Regex("[\\\\/:*?\"<>|]"), "_")

    override suspend fun readTextFromUri(uri: Uri): String =
        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
}
