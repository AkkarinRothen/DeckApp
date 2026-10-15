package com.deckapp.core.data.db

import androidx.test.platform.app.InstrumentationRegistry
import com.deckapp.core.data.di.DatabaseModule
import com.deckapp.core.data.repository.backup.BackupRepositoryImpl
import com.deckapp.core.model.backup.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class BackupRestorePreservationTest {
    @Test fun restorePreservesCollectionsAndTableTags() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        check(context.packageName != "com.deckapp.ttrpg")
        context.deleteDatabase(DeckAppDatabase.DATABASE_NAME)
        val db = DatabaseModule.provideDeckAppDatabase(context)
        try {
            val repository = BackupRepositoryImpl(db, db.backupDao())
            val collection = CollectionBackupDto(1, "Campaign", "Keep description", 123, "BOOK", "cover.webp", 42)
            val ref = CollectionResourceBackupDto(1, 1, "NPC")
            repository.restoreFullBackup(FullBackupDto(
                tags = listOf(TagBackupDto(1, "Important", 123)),
                randomTables = listOf(RandomTableBackupDto(id = 1, name = "Weather")),
                randomTableTags = listOf(RandomTableTagBackupDto(1, 1)),
                npcs = listOf(NpcBackupDto(id = 1, name = "Guide")),
                collections = listOf(collection), collectionResourceRefs = listOf(ref)
            ))
            val exported = repository.createFullBackupDto()
            assertEquals(listOf(collection), exported.collections)
            assertEquals(listOf(ref), exported.collectionResourceRefs)
            assertEquals(listOf(RandomTableTagBackupDto(1, 1)), exported.randomTableTags)
            db.openHelper.writableDatabase.query("PRAGMA foreign_key_check").use { assertEquals(0, it.count) }
        } finally { db.close(); context.deleteDatabase(DeckAppDatabase.DATABASE_NAME) }
    }
}
