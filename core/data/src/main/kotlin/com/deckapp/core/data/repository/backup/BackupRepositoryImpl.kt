package com.deckapp.core.data.repository.backup

import com.deckapp.core.data.db.backup.BackupDao
import com.deckapp.core.domain.repository.backup.BackupRepository
import com.deckapp.core.model.backup.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

import com.deckapp.core.data.db.DeckAppDatabase
import androidx.room.withTransaction
import com.deckapp.core.data.db.*

class BackupRepositoryImpl @Inject constructor(
    private val db: DeckAppDatabase,
    private val backupDao: BackupDao
) : BackupRepository {

    override suspend fun createFullBackupDto(): FullBackupDto = withContext(Dispatchers.IO) {
        FullBackupDto(
            schemaVersion = 1,
            tags = backupDao.getAllTags().map { TagBackupDto(it.id, it.name, it.color) },
            decks = backupDao.getAllDecks().map { CardStackBackupDto(it.id, it.name, it.type, it.description, it.coverImagePath, it.sourceFolderPath, it.defaultContentMode, it.drawMode, it.drawFaceDown, it.backImagePath, it.displayCount, it.aspectRatio, it.isArchived, it.sortOrder, it.createdAt) },
            cards = backupDao.getAllCards().map { CardBackupDto(it.id, it.stackId, it.originDeckId, it.title, it.suit, it.value, it.currentFaceIndex, it.currentRotation, it.isReversed, it.isDrawn, it.isRevealed, it.sortOrder, it.linkedTableId, it.dmNotes, it.lastDrawnAt) },
            cardFaces = backupDao.getAllCardFaces().map { CardFaceBackupDto(it.id, it.cardId, it.faceIndex, it.name, it.imagePath, it.contentMode, it.zonesJson, it.reversedImagePath) },
            cardStackTags = backupDao.getAllCardStackTags().map { CardStackTagBackupDto(it.stackId, it.tagId) },
            cardTags = backupDao.getAllCardTags().map { CardTagBackupDto(it.cardId, it.tagId) },
            randomTableTags = backupDao.getAllRandomTableTags().map { RandomTableTagBackupDto(it.tableId, it.tagId) },
            tableBundles = backupDao.getAllTableBundles().map { TableBundleBackupDto(it.id, it.name, it.description, it.sourceUri, it.createdAt) },
            randomTables = backupDao.getAllRandomTables().map { RandomTableBackupDto(it.id, it.bundleId, it.name, it.description, it.rollFormula, it.rollMode, it.isNoRepeat, it.isPinned, it.sourceType, it.sourceName, it.isBuiltIn, it.sortOrder, it.createdAt) },
            tableEntries = backupDao.getAllTableEntries().map { TableEntryBackupDto(it.id, it.tableId, it.minRoll, it.maxRoll, it.weight, it.text, it.subTableRef, it.subTableId, it.sortOrder) },
            sessions = backupDao.getAllSessions().map { SessionBackupDto(it.id, it.name, it.status, it.scheduledDate, it.summary, it.createdAt, it.endedAt, it.showCardTitles, it.dmNotes) },
            sessionDeckRefs = backupDao.getAllSessionDeckRefs().map { SessionDeckRefBackupDto(it.sessionId, it.stackId, it.drawModeOverride, it.sortOrder) },
            sessionTableRefs = backupDao.getAllSessionTableRefs().map { SessionTableRefBackupDto(it.sessionId, it.tableId, it.sortOrder) },
            drawEvents = backupDao.getAllDrawEvents().map { DrawEventBackupDto(it.id, it.sessionId, it.cardId, it.action, it.metadata, it.timestamp) },
            encounters = backupDao.getAllEncounters().map { EncounterBackupDto(it.id, it.name, it.description, it.linkedSessionId, it.isActive, it.currentRound, it.currentTurnIndex, it.createdAt) },
            encounterCreatures = backupDao.getAllEncounterCreatures().map { EncounterCreatureBackupDto(it.id, it.encounterId, it.name, it.maxHp, it.currentHp, it.armorClass, it.initiativeBonus, it.initiativeRoll, it.conditionsJson, it.notes, it.sortOrder, it.npcId, it.imagePath) },
            combatLog = backupDao.getAllCombatLogEntries().map { CombatLogEntryBackupDto(it.id, it.encounterId, it.message, it.type, it.timestamp) },
            npcs = backupDao.getAllNpcs().map { NpcBackupDto(it.id, it.name, it.description, it.imagePath, it.maxHp, it.currentHp, it.armorClass, it.initiativeBonus, it.notes, it.isMonster, it.createdAt) },
            npcTags = backupDao.getAllNpcTags().map { NpcTagBackupDto(it.npcId, it.tagId) },
            wikiCategories = backupDao.getAllWikiCategories().map { WikiCategoryBackupDto(it.id, it.name, it.iconName) },
            wikiEntries = backupDao.getAllWikiEntries().map { WikiEntryBackupDto(it.id, it.title, it.content, it.categoryId, it.imagePath, it.lastUpdated) }
        )
    }

    override suspend fun restoreFullBackup(backup: FullBackupDto) = withContext(Dispatchers.IO) {
        db.withTransaction {
            // 1. Limpiar base de datos completa
            db.clearAllTables()

            // 2. Insertar Tags
            db.tagDao().insertTags(backup.tags.map { TagEntity(it.id, it.name, it.color) })

            // 3. Insertar Mazos y Cartas
            db.cardStackDao().insertStacks(backup.decks.map { 
                CardStackEntity(it.id, it.name, it.type, it.description, it.coverImagePath, it.sourceFolderPath, it.defaultContentMode, it.drawMode, it.drawFaceDown, it.backImagePath, it.displayCount, it.aspectRatio, it.isArchived, it.sortOrder, it.createdAt) 
            })
            db.cardDao().insertCards(backup.cards.map { 
                CardEntity(it.id, it.stackId, it.originDeckId, it.title, it.suit, it.value, it.currentFaceIndex, it.currentRotation, it.isReversed, it.isDrawn, it.isRevealed, it.sortOrder, it.linkedTableId, it.dmNotes, it.lastDrawnAt) 
            })
            db.cardFaceDao().insertFaces(backup.cardFaces.map { 
                CardFaceEntity(it.id, it.cardId, it.faceIndex, it.name, it.imagePath, it.contentMode, it.zonesJson, it.reversedImagePath) 
            })

            // 4. Insertar Relaciones (Cross-Refs)
            backup.cardStackTags.forEach { db.cardStackDao().insertStackTagCrossRef(CardStackTagCrossRef(it.stackId, it.tagId)) }
            backup.cardTags.forEach { db.cardDao().insertCardTagCrossRef(CardTagCrossRef(it.cardId, it.tagId)) }
            backup.randomTableTags.forEach { db.randomTableDao().insertTableTagCrossRef(RandomTableTagCrossRef(it.tableId, it.tagId)) }

            // 5. Insertar Tablas Aleatorias
            db.tableBundleDao().insertBundles(backup.tableBundles.map { TableBundleEntity(it.id, it.name, it.description, it.sourceUri, it.createdAt) })
            db.randomTableDao().insertTables(backup.randomTables.map { 
                RandomTableEntity(it.id, it.bundleId, it.name, it.description, it.rollFormula, it.rollMode, it.isNoRepeat, it.isPinned, it.sourceType, it.sourceName, it.isBuiltIn, it.sortOrder, it.createdAt) 
            })
            db.randomTableDao().insertEntries(backup.tableEntries.map { 
                TableEntryEntity(it.id, it.tableId, it.minRoll, it.maxRoll, it.weight, it.text, it.subTableRef, it.subTableId, it.sortOrder) 
            })

            // 6. Insertar Sesiones e Historial
            db.sessionDao().insertSessions(backup.sessions.map { 
                SessionEntity(it.id, it.name, it.status, it.scheduledDate, it.summary, it.createdAt, it.endedAt, it.showCardTitles, it.dmNotes) 
            })
            backup.sessionDeckRefs.forEach { db.sessionDao().insertDeckRef(SessionDeckRefEntity(it.sessionId, it.stackId, it.drawModeOverride, it.sortOrder)) }
            backup.sessionTableRefs.forEach { db.sessionDao().insertTableRef(SessionTableRefEntity(it.sessionId, it.tableId, it.sortOrder)) }
            db.drawEventDao().insertEvents(backup.drawEvents.map { DrawEventEntity(it.id, it.sessionId, it.cardId, it.action, it.metadata, it.timestamp) })

            // 7. Insertar Encuentros
            db.encounterDao().insertEncounters(backup.encounters.map { 
                EncounterEntity(it.id, it.name, it.description, it.linkedSessionId, it.isActive, it.currentRound, it.currentTurnIndex, it.createdAt) 
            })
            db.encounterDao().insertCreatures(backup.encounterCreatures.map { 
                EncounterCreatureEntity(it.id, it.encounterId, it.name, it.maxHp, it.currentHp, it.armorClass, it.initiativeBonus, it.initiativeRoll, it.conditionsJson, it.notes, it.sortOrder, it.npcId, it.imagePath) 
            })
            db.encounterDao().insertLogs(backup.combatLog.map { 
                CombatLogEntryEntity(it.id, it.encounterId, it.message, it.type, it.timestamp) 
            })

            // 8. Insertar NPCs y Wiki
            db.npcDao().insertNpcs(backup.npcs.map { 
                NpcEntity(it.id, it.name, it.description, it.imagePath, it.maxHp, it.currentHp, it.armorClass, it.initiativeBonus, it.notes, it.isMonster, it.createdAt) 
            })
            backup.npcTags.forEach { db.npcDao().insertNpcTagCrossRef(NpcTagCrossRef(it.npcId, it.tagId)) }

            db.wikiDao().insertCategories(backup.wikiCategories.map { WikiCategoryEntity(it.id, it.name, it.iconName) })
            db.wikiDao().insertEntries(backup.wikiEntries.map { 
                WikiEntryEntity(it.id, it.title, it.content, it.categoryId, it.imagePath, it.lastUpdated) 
            })
        }
    }
}
