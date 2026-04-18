package com.deckapp.core.domain.usecase.backup

import android.net.Uri
import com.deckapp.core.domain.repository.FileRepository
import com.deckapp.core.domain.repository.backup.BackupRepository
import com.deckapp.core.model.backup.FullBackupDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject

/**
 * Orquestador de la restauración completa del sistema.
 * CUIDADO: Esta operación es destructiva y reemplaza toda la biblioteca actual.
 */
class RestoreBackupUseCase @Inject constructor(
    private val backupRepository: BackupRepository,
    private val fileRepository: FileRepository
) {
    suspend operator fun invoke(zipUri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        var tempDirPath: String? = null
        try {
            // 1. Descomprimir el ZIP en un directorio temporal seguro
            tempDirPath = fileRepository.extractFullBackupZipToTemp(zipUri) 
                ?: return@withContext Result.failure(Exception("No se pudo descomprimir el archivo de backup."))

            // 2. Validar y leer el JSON de la base de datos
            val dbFile = File(tempDirPath, "database/deckapp_export.json")
            if (!dbFile.exists()) {
                return@withContext Result.failure(Exception("El archivo de backup no contiene la base de datos (deckapp_export.json)."))
            }
            
            val dbJson = dbFile.readText()
            val backupDto = Json { 
                ignoreUnknownKeys = true 
            }.decodeFromString<FullBackupDto>(dbJson)

            // 3. Restaurar la Base de Datos en una transacción masiva (Destructivo)
            backupRepository.restoreFullBackup(backupDto)

            // 4. Restaurar la estructura de carpetas de imágenes
            fileRepository.restoreImagesFromTemp(tempDirPath)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            // Siempre limpiar los archivos temporales de restauración
            fileRepository.clearCache()
        }
    }
}
