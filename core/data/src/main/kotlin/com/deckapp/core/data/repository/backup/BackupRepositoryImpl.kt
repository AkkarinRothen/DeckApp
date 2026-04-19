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
            decks = backupDao.getAllDecks().map { it.toBackupDto() },
            cards = backupDao.getAllCards().map { it.toBackupDto() },
            cardFaces = backupDao.getAllCardFaces().map { it.toBackupDto() },
            cardStackTags = backupDao.getAllCardStackTags().map { CardStackTagBackupDto(it.stackId, it.tagId) },
            cardTags = backupDao.getAllCardTags().map { CardTagBackupDto(it.cardId, it.tagId) },
            randomTableTags = backupDao.getAllRandomTableTags().map { RandomTableTagBackupDto(it.tableId, it.tagId) },
            tableBundles = backupDao.getAllTableBundles().map { TableBundleBackupDto(it.id, it.name, it.description, it.sourceUri, it.createdAt) },
            randomTables = backupDao.getAllRandomTables().map { it.toBackupDto() },
            tableEntries = backupDao.getAllTableEntries().map { it.toBackupDto() },
            sessions = backupDao.getAllSessions().map { it.toBackupDto() },
            sessionDeckRefs = backupDao.getAllSessionDeckRefs().map { SessionDeckRefBackupDto(it.sessionId, it.stackId, it.drawModeOverride, it.sortOrder) },
            sessionTableRefs = backupDao.getAllSessionTableRefs().map { SessionTableRefBackupDto(it.sessionId, it.tableId, it.sortOrder) },
            drawEvents = backupDao.getAllDrawEvents().map { it.toBackupDto() },
            encounters = backupDao.getAllEncounters().map { it.toBackupDto() },
            encounterCreatures = backupDao.getAllEncounterCreatures().map { it.toBackupDto() },
            combatLog = backupDao.getAllCombatLogEntries().map { it.toBackupDto() },
            npcs = backupDao.getAllNpcs().map { it.toBackupDto() },
            npcTags = backupDao.getAllNpcTags().map { NpcTagBackupDto(it.npcId, it.tagId) },
            wikiCategories = backupDao.getAllWikiCategories().map { WikiCategoryBackupDto(it.id, it.name, it.iconName) },
            wikiEntries = backupDao.getAllWikiEntries().map { it.toBackupDto() },
            referenceTables = db.referenceTableDao().getAllTablesSync().map { it.toBackupDto() },
            referenceRows = db.referenceTableDao().getAllRowsSync().map { it.toBackupDto() },
            referenceTableTags = db.referenceTableDao().getAllTagRefsSync().map { ReferenceTableTagBackupDto(it.tableId, it.tagId) },
            systemRules = db.systemRuleDao().getAllRulesSync().map { it.toBackupDto() },
            systemRuleTags = db.systemRuleDao().getAllTagRefsSync().map { SystemRuleTagBackupDto(it.ruleId, it.tagId) }
        )
    }

    override suspend fun restoreFullBackup(backup: FullBackupDto) = withContext(Dispatchers.IO) {
        db.withTransaction {
            db.clearAllTables()

            db.tagDao().insertTags(backup.tags.map { TagEntity(it.id, it.name, it.color) })

            db.cardStackDao().insertStacks(backup.decks.map { 
                CardStackEntity(id = it.id, name = it.name, type = it.type, description = it.description, coverImagePath = it.coverImagePath, sourceFolderPath = it.sourceFolderPath, defaultContentMode = it.defaultContentMode, drawMode = it.drawMode, drawFaceDown = it.drawFaceDown, backImagePath = it.backImagePath, displayCount = it.displayCount, aspectRatio = it.aspectRatio, isArchived = it.isArchived, sortOrder = it.sortOrder, createdAt = it.createdAt) 
            })
            db.cardDao().insertCards(backup.cards.map { 
                CardEntity(id = it.id, stackId = it.stackId, originDeckId = it.originDeckId, title = it.title, suit = it.suit, value = it.value, currentFaceIndex = it.currentFaceIndex, currentRotation = it.currentRotation, isReversed = it.isReversed, isDrawn = it.isDrawn, isRevealed = it.isRevealed, sortOrder = it.sortOrder, linkedTableId = it.linkedTableId, dmNotes = it.dmNotes, lastDrawnAt = it.lastDrawnAt) 
            })
            db.cardFaceDao().insertFaces(backup.cardFaces.map { 
                CardFaceEntity(id = it.id, cardId = it.cardId, faceIndex = it.faceIndex, name = it.name, imagePath = it.imagePath, contentMode = it.contentMode, zonesJson = it.zonesJson, reversedImagePath = it.reversedImagePath) 
            })

            backup.cardStackTags.forEach { db.tagDao().insertStackTagRef(CardStackTagCrossRef(it.stackId, it.tagId)) }
            backup.cardTags.forEach { db.tagDao().insertCardTagRef(CardTagCrossRef(it.cardId, it.tagId)) }
            backup.randomTableTags.forEach { db.tagDao().insertTableTagRef(RandomTableTagCrossRef(it.tableId, it.tagId)) }

            db.tableBundleDao().insertBundles(backup.tableBundles.map { TableBundleEntity(it.id, it.name, it.description, it.sourceUri, it.createdAt) })
            db.randomTableDao().insertTables(backup.randomTables.map { 
                RandomTableEntity(id = it.id, bundleId = it.bundleId, name = it.name, category = it.category, description = it.description, rollFormula = it.rollFormula, rollMode = it.rollMode, isNoRepeat = it.isNoRepeat, isPinned = it.isPinned, sourceType = it.sourceType, sourceName = it.sourceName, isBuiltIn = it.isBuiltIn, sortOrder = it.sortOrder, sourcePack = it.sourcePack, createdAt = it.createdAt) 
            })
            db.randomTableDao().insertEntries(backup.tableEntries.map { 
                TableEntryEntity(id = it.id, tableId = it.tableId, minRoll = it.minRoll, maxRoll = it.maxRoll, weight = it.weight, text = it.text, subTableRef = it.subTableRef, subTableId = it.subTableId, sortOrder = it.sortOrder) 
            })

            db.sessionDao().insertSessions(backup.sessions.map { 
                SessionEntity(id = it.id, name = it.name, status = it.status, scheduledDate = it.scheduledDate, summary = it.summary, createdAt = it.createdAt, endedAt = it.endedAt, showCardTitles = it.showCardTitles, dmNotes = it.dmNotes, gameSystemsJson = it.gameSystemsJson) 
            })
            backup.sessionDeckRefs.forEach { db.sessionDao().insertSessionDeckRef(SessionDeckRefEntity(it.sessionId, it.stackId, it.drawModeOverride, it.sortOrder)) }
            backup.sessionTableRefs.forEach { db.sessionDao().insertSessionTableRef(SessionTableRefEntity(it.sessionId, it.tableId, it.sortOrder)) }
            db.drawEventDao().insertEvents(backup.drawEvents.map { DrawEventEntity(it.id, it.sessionId, it.cardId, it.action, it.metadata, it.timestamp) })

            db.encounterDao().insertEncounters(backup.encounters.map { 
                EncounterEntity(id = it.id, name = it.name, description = it.description, linkedSessionId = it.linkedSessionId, isActive = it.isActive, currentRound = it.currentRound, currentTurnIndex = it.currentTurnIndex, createdAt = it.createdAt) 
            })
            db.encounterDao().insertCreatures(backup.encounterCreatures.map { 
                EncounterCreatureEntity(id = it.id, encounterId = it.encounterId, name = it.name, maxHp = it.maxHp, currentHp = it.currentHp, armorClass = it.armorClass, initiativeBonus = it.initiativeBonus, initiativeRoll = it.initiativeRoll, conditionsJson = it.conditionsJson, notes = it.notes, sortOrder = it.sortOrder, npcId = it.npcId, imagePath = it.imagePath) 
            })
            db.encounterDao().insertCombatLogs(backup.combatLog.map { 
                CombatLogEntryEntity(id = it.id, encounterId = it.encounterId, message = it.message, type = it.type, timestamp = it.timestamp) 
            })

            db.npcDao().insertNpcs(backup.npcs.map { 
                NpcEntity(id = it.id, name = it.name, description = it.description, imagePath = it.imagePath, maxHp = it.maxHp, currentHp = it.currentHp, armorClass = it.armorClass, initiativeBonus = it.initiativeBonus, notes = it.notes, isMonster = it.isMonster, createdAt = it.createdAt) 
            })
            backup.npcTags.forEach { db.npcDao().insertCrossRef(NpcTagCrossRef(it.npcId, it.tagId)) }

            db.wikiDao().insertCategories(backup.wikiCategories.map { WikiCategoryEntity(it.id, it.name, it.iconName) })
            db.wikiDao().insertEntries(backup.wikiEntries.map { 
                WikiEntryEntity(id = it.id, title = it.title, content = it.content, categoryId = it.categoryId, imagePath = it.imagePath, lastUpdated = it.lastUpdated) 
            })

            db.referenceTableDao().insertTables(backup.referenceTables.map {
                ReferenceTableEntity(id = it.id, name = it.name, description = it.description, gameSystem = it.gameSystem, category = it.category, columnsJson = it.columnsJson, isPinned = it.isPinned, sortOrder = it.sortOrder, createdAt = it.createdAt, sourcePack = it.sourcePack)
            })
            db.referenceTableDao().insertRows(backup.referenceRows.map {
                ReferenceRowEntity(id = it.id, tableId = it.tableId, cellsJson = it.cellsJson, sortOrder = it.sortOrder)
            })
            backup.referenceTableTags.forEach { 
                db.referenceTableDao().addTagToTable(ReferenceTableTagCrossRef(it.tableId, it.tagId)) 
            }
            db.systemRuleDao().insertRules(backup.systemRules.map {
                SystemRuleEntity(id = it.id, title = it.title, content = it.content, gameSystem = it.gameSystem, category = it.category, isPinned = it.isPinned, sortOrder = it.sortOrder, lastUpdated = it.lastUpdated, sourcePack = it.sourcePack)
            })
            backup.systemRuleTags.forEach { 
                db.systemRuleDao().addTagToRule(SystemRuleTagCrossRef(it.ruleId, it.tagId)) 
            }
        }
    }
}
