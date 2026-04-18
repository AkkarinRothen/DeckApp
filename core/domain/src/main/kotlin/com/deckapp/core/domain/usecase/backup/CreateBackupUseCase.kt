package com.deckapp.core.domain.usecase.backup

import android.net.Uri
import com.deckapp.core.domain.repository.FileRepository
import com.deckapp.core.domain.repository.backup.BackupRepository
import com.deckapp.core.model.backup.BackupCounts
import com.deckapp.core.model.backup.BackupManifest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

class CreateBackupUseCase @Inject constructor(
    private val backupRepository: BackupRepository,
    private val fileRepository: FileRepository
) {
    /**
     * Orquesta la creación del backup completo.
     * @param outputUri La URI donde el usuario eligió guardar el ZIP.
     */
    suspend operator fun invoke(outputUri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // 1. Extraer todos los datos desde la DB en un DTO gigante (independiente de Room)
            val fullBackup = backupRepository.createFullBackupDto()

            // 2. Serializar la base de datos a JSON
            val databaseJson = Json { prettyPrint = false }.encodeToString(fullBackup)

            // 3. Crear el manifiesto con los metadatos y conteos
            val manifest = BackupManifest(
                version = 1,
                appVersion = "1.0.0", // TODO: Podría extraerse dinámicamente
                deviceModel = android.os.Build.MODEL,
                dbVersion = 26, // Match con DeckAppDatabase.version
                counts = BackupCounts(
                    decks = fullBackup.decks.size,
                    cards = fullBackup.cards.size,
                    randomTables = fullBackup.randomTables.size,
                    npcs = fullBackup.npcs.size,
                    wikiEntries = fullBackup.wikiEntries.size,
                    sessions = fullBackup.sessions.size
                )
            )
            // Serializar el manifiesto bonito para que sea legible
            val manifestJson = Json { prettyPrint = true }.encodeToString(manifest)

            // 4. Delegar al repositorio la creación física del ZIP (JSON + Imágenes)
            fileRepository.createFullBackupZip(
                outputUri = outputUri,
                manifestJson = manifestJson,
                databaseJson = databaseJson
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
