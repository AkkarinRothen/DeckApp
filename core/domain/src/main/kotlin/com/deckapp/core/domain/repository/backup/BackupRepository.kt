package com.deckapp.core.domain.repository.backup

import com.deckapp.core.model.backup.FullBackupDto

interface BackupRepository {
    /**
     * Extrae todas las entidades de la base de datos (cartas, mazos, tablas, etc.)
     * y las empaqueta en un único DTO listo para ser serializado a JSON.
     */
    suspend fun createFullBackupDto(): FullBackupDto

    /**
     * Borra absolutamente todos los datos actuales e inserta los datos proporcionados.
     * Esta operación es destructiva y debe ejecutarse en una transacción.
     */
    suspend fun restoreFullBackup(backup: FullBackupDto)
}
