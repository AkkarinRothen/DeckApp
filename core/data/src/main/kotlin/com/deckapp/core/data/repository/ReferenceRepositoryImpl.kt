package com.deckapp.core.data.repository

import androidx.room.withTransaction
import com.deckapp.core.data.db.*
import com.deckapp.core.domain.repository.ReferenceRepository
import com.deckapp.core.model.ReferenceTable
import com.deckapp.core.model.SystemRule
import com.deckapp.core.model.Tag
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReferenceRepositoryImpl @Inject constructor(
    private val db: DeckAppDatabase,
    private val tableDao: ReferenceTableDao,
    private val ruleDao: SystemRuleDao,
    private val randomTableDao: RandomTableDao
) : ReferenceRepository {

    override fun getAllReferenceTables(): Flow<List<ReferenceTable>> =
        tableDao.getAllTablesWithRows().map { entities ->
            entities.map { it.table.toDomain(rows = it.rows) }
        }

    override fun getTablesBySystem(system: String): Flow<List<ReferenceTable>> =
        tableDao.getTablesWithRowsBySystem(system).map { entities ->
            entities.map { it.table.toDomain(rows = it.rows) }
        }

    override suspend fun getReferenceTableWithRows(id: Long): ReferenceTable? {
        val withRows = tableDao.getTableWithRows(id) ?: return null
        val tags = tableDao.getTagsForTable(id).map { it.toDomain() }
        return withRows.table.toDomain(rows = withRows.rows, tags = tags)
    }

    override fun searchReferenceTables(query: String): Flow<List<ReferenceTable>> =
        tableDao.searchTablesWithRows(query).map { entities ->
            entities.map { it.table.toDomain(rows = it.rows) }
        }

    override suspend fun saveReferenceTable(table: ReferenceTable): Long {
        val entity = table.toEntity()
        val id = tableDao.insertTable(entity)
        val tableId = if (id == -1L) {
            tableDao.updateTable(entity)
            entity.id
        } else id

        tableDao.deleteRowsForTable(tableId)
        tableDao.insertRows(table.rows.map { it.toEntity(tableId) })
        return tableId
    }

    override suspend fun deleteReferenceTable(tableId: Long) {
        tableDao.deleteTable(tableId)
    }

    override suspend fun updateTablePinned(id: Long, pinned: Boolean) {
        tableDao.updatePinned(id, pinned)
    }

    override fun getAllSystemRules(): Flow<List<SystemRule>> =
        ruleDao.getAllRules().map { entities ->
            entities.map { it.toDomain() }
        }

    override fun getRulesBySystem(system: String): Flow<List<SystemRule>> =
        ruleDao.getRulesBySystem(system).map { entities ->
            entities.map { it.toDomain() }
        }

    override fun searchSystemRules(query: String): Flow<List<SystemRule>> =
        ruleDao.searchRules(query).map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun getRuleById(id: Long): SystemRule? {
        val entity = ruleDao.getRuleById(id) ?: return null
        val tags = ruleDao.getTagsForRule(id).map { it.toDomain() }
        return entity.toDomain(tags = tags)
    }

    override suspend fun saveSystemRule(rule: SystemRule): Long {
        val entity = rule.toEntity()
        val id = ruleDao.insertRule(entity)
        if (id == -1L) {
            ruleDao.updateRule(entity)
            return entity.id
        }
        return id
    }

    override suspend fun deleteSystemRule(ruleId: Long) {
        ruleDao.deleteRule(ruleId)
    }

    override suspend fun updateRulePinned(id: Long, pinned: Boolean) {
        ruleDao.updatePinned(id, pinned)
    }

    override fun getDistinctSystems(): Flow<List<String>> =
        combine(tableDao.getDistinctSystems(), ruleDao.getDistinctSystems()) { tableSystems, ruleSystems ->
            (tableSystems + ruleSystems).distinct().sorted()
        }

    override suspend fun addTagToReferenceTable(tableId: Long, tagId: Long) {
        tableDao.addTagToTable(ReferenceTableTagCrossRef(tableId, tagId))
    }

    override suspend fun removeTagFromReferenceTable(tableId: Long, tagId: Long) {
        tableDao.removeTagFromTable(ReferenceTableTagCrossRef(tableId, tagId))
    }

    override suspend fun addTagToSystemRule(ruleId: Long, tagId: Long) {
        ruleDao.addTagToRule(SystemRuleTagCrossRef(ruleId, tagId))
    }

    override suspend fun removeTagFromSystemRule(ruleId: Long, tagId: Long) {
        ruleDao.removeTagFromRule(SystemRuleTagCrossRef(ruleId, tagId))
    }

    override fun getInstalledPackNames(): Flow<Set<String>> = combine(
        tableDao.getDistinctSourcePacks(),
        ruleDao.getDistinctSourcePacks(),
        randomTableDao.getDistinctSourcePacks()
    ) { t, r, rt -> (t + r + rt).toSet() }

    override suspend fun importStarterPack(backup: com.deckapp.core.model.backup.FullBackupDto, packName: String) {
        db.withTransaction {
            val gameSystems = (backup.referenceTables.map { it.gameSystem } +
                               backup.systemRules.map { it.gameSystem }).toSet()
            gameSystems.forEach { system ->
                tableDao.deleteOrphansByGameSystem(system)
                ruleDao.deleteOrphansByGameSystem(system)
            }

            val tableIdMap = mutableMapOf<Long, Long>()

            backup.referenceTables.forEach { dto ->
                val existing = tableDao.getTableByPackAndName(packName, dto.name)
                val entity = ReferenceTableEntity(
                    id = existing?.id ?: 0,
                    name = dto.name,
                    description = dto.description,
                    gameSystem = dto.gameSystem,
                    category = dto.category,
                    columnsJson = dto.columnsJson,
                    isPinned = dto.isPinned,
                    sortOrder = dto.sortOrder,
                    createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                    sourcePack = packName
                )
                val tableId = if (existing == null) {
                    tableDao.insertTable(entity)
                } else {
                    tableDao.updateTable(entity)
                    existing.id
                }
                tableIdMap[dto.id] = tableId
                tableDao.deleteRowsForTable(tableId)
            }

            backup.referenceRows.forEach { dto ->
                val newTableId = tableIdMap[dto.tableId]
                if (newTableId != null) {
                    tableDao.insertRows(listOf(
                        ReferenceRowEntity(
                            id = 0,
                            tableId = newTableId,
                            cellsJson = dto.cellsJson,
                            sortOrder = dto.sortOrder
                        )
                    ))
                }
            }

            backup.systemRules.forEach { dto ->
                val existing = ruleDao.getRuleByPackAndTitle(packName, dto.title)
                val entity = SystemRuleEntity(
                    id = existing?.id ?: 0,
                    title = dto.title,
                    content = dto.content,
                    gameSystem = dto.gameSystem,
                    category = dto.category,
                    isPinned = dto.isPinned,
                    sortOrder = dto.sortOrder,
                    lastUpdated = System.currentTimeMillis(),
                    sourcePack = packName
                )
                if (existing == null) ruleDao.insertRule(entity) else ruleDao.updateRule(entity)
            }

            // 3. Tablas Aleatorias: preserve database identities when reinstalling.
            val randomTableIdMap = importRandomTables(backup, packName, replaceExisting = true)

            // 4. NPCs / Criaturas del paquete
            if (backup.npcs.isNotEmpty()) {
                val tagDao = db.tagDao()
                val npcDao = db.npcDao()
                val existingTag = tagDao.getTagByName(packName)
                val tagId = existingTag?.id ?: tagDao.insertTag(
                    TagEntity(
                        id = 0,
                        name = packName,
                        color = 0xFF8B0000.toInt()
                    )
                )

                backup.npcs.forEach { dto ->
                    val existing = npcDao.getNpcByName(dto.name)
                    val npcId = if (existing != null) {
                        npcDao.upsertNpc(
                            existing.copy(
                                description = dto.description,
                                imagePath = dto.imagePath ?: existing.imagePath,
                                maxHp = dto.maxHp,
                                currentHp = dto.currentHp,
                                armorClass = dto.armorClass,
                                initiativeBonus = dto.initiativeBonus,
                                notes = dto.notes,
                                isMonster = dto.isMonster
                            )
                        )
                        existing.id
                    } else {
                        npcDao.upsertNpc(
                            NpcEntity(
                                id = 0,
                                name = dto.name,
                                description = dto.description,
                                imagePath = dto.imagePath,
                                maxHp = dto.maxHp,
                                currentHp = dto.currentHp,
                                armorClass = dto.armorClass,
                                initiativeBonus = dto.initiativeBonus,
                                notes = dto.notes,
                                isMonster = dto.isMonster,
                                createdAt = System.currentTimeMillis()
                            )
                        )
                    }
                    npcDao.insertCrossRef(NpcTagCrossRef(npcId = npcId, tagId = tagId))
                }
            }

            // 5. Tactical Maps & Pins del paquete
            if (backup.tacticalMaps.isNotEmpty()) {
                val tacticalMapDao = db.tacticalMapDao()
                val mapIdMap = mutableMapOf<Long, Long>()
                val existingMaps = tacticalMapDao.getAllMapsSync().filter { it.sessionId == null }

                backup.tacticalMaps.forEach { dto ->
                    val existing = existingMaps.find { it.name == dto.name }
                    val mapEntity = TacticalMapEntity(
                        id = existing?.id ?: 0,
                        name = dto.name,
                        imageUri = dto.imageUri,
                        gridType = dto.gridType,
                        cellSizePx = dto.cellSizePx,
                        gridOffsetX = dto.gridOffsetX,
                        gridOffsetY = dto.gridOffsetY,
                        gridColorHex = dto.gridColorHex,
                        gridOpacity = dto.gridOpacity,
                        scaleFtPerCell = dto.scaleFtPerCell,
                        diagonalRule = dto.diagonalRule,
                        sessionId = null,
                        sceneId = null,
                        fogOfWarMaskJson = dto.fogOfWarMaskJson,
                        terrainMaskJson = dto.terrainMaskJson,
                        areasJson = dto.areasJson,
                        createdAt = existing?.createdAt ?: System.currentTimeMillis()
                    )
                    val mapId = tacticalMapDao.upsertMap(mapEntity)
                    mapIdMap[dto.id] = mapId
                    existing?.let { tacticalMapDao.deletePinsByMapId(it.id) }
                }

                if (backup.tacticalMapPins.isNotEmpty()) {
                    val jsonSerializer = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                    backup.tacticalMapPins.forEach { pinDto ->
                        val newMapId = mapIdMap[pinDto.mapId]
                        if (newMapId != null) {
                            val remappedTargetTableId = pinDto.targetTableId?.let { randomTableIdMap[it] ?: it }
                            val remappedTableLinksJson = try {
                                val links = jsonSerializer.decodeFromString<List<com.deckapp.core.model.PinTableLink>>(pinDto.tableLinksJson)
                                val remappedLinks = links.map { link ->
                                    link.copy(tableId = randomTableIdMap[link.tableId] ?: link.tableId)
                                }
                                jsonSerializer.encodeToString(remappedLinks)
                            } catch (_: Exception) {
                                pinDto.tableLinksJson
                            }

                            tacticalMapDao.upsertPin(
                                TacticalMapPinEntity(
                                    id = 0,
                                    mapId = newMapId,
                                    cellX = pinDto.cellX,
                                    cellY = pinDto.cellY,
                                    title = pinDto.title,
                                    description = pinDto.description,
                                    iconName = pinDto.iconName,
                                    colorHex = pinDto.colorHex,
                                    isSecret = pinDto.isSecret,
                                    explorationState = pinDto.explorationState,
                                    targetTableId = remappedTargetTableId,
                                    targetEncounterId = pinDto.targetEncounterId,
                                    targetWikiEntryId = pinDto.targetWikiEntryId,
                                    targetMapId = pinDto.targetMapId?.let { mapIdMap[it] ?: it },
                                    discoveriesJson = pinDto.discoveriesJson,
                                    tableLinksJson = remappedTableLinksJson
                                )
                            )
                        }
                    }
                }
            }

            // 6. Decks & Cards del paquete
            if (backup.decks.isNotEmpty()) {
                val cardStackDao = db.cardStackDao()
                val cardDao = db.cardDao()
                val cardFaceDao = db.cardFaceDao()
                val tagDao = db.tagDao()

                val existingTag = tagDao.getTagByName(packName)
                val packTagId = existingTag?.id ?: tagDao.insertTag(
                    TagEntity(
                        id = 0,
                        name = packName,
                        color = 0xFF2E7D32.toInt()
                    )
                )

                val deckIdMap = mutableMapOf<Long, Long>()
                val existingStacks = cardStackDao.getAllStacksSync()
                // A Deck of Worlds reimport must retain card IDs, draws and session history.
                val preserveStoryCards = packName == "story_engine_deck_of_worlds.json"
                val existingStoryByImage = mutableMapOf<String, Pair<CardEntity, CardFaceEntity>>()
                if (preserveStoryCards) {
                    backup.decks.mapNotNull { dto -> existingStacks.find { it.name == dto.name }?.id }
                        .forEach { stackId ->
                            cardDao.getCardsForStackSync(stackId).forEach { card ->
                                val face = cardFaceDao.getFacesForCardSync(card.id).firstOrNull()
                                face?.let { existingFace ->
                                    existingFace.imagePath?.let { path ->
                                        existingStoryByImage[path] = card to existingFace
                                    }
                                }
                            }
                        }
                }

                backup.decks.forEach { dto ->
                    val existing = existingStacks.find { it.name == dto.name }
                    val stackEntity = CardStackEntity(
                        id = existing?.id ?: 0,
                        name = dto.name,
                        type = dto.type,
                        description = dto.description,
                        coverImagePath = dto.coverImagePath,
                        sourceFolderPath = dto.sourceFolderPath,
                        defaultContentMode = dto.defaultContentMode,
                        drawMode = dto.drawMode,
                        drawFaceDown = dto.drawFaceDown,
                        backImagePath = dto.backImagePath,
                        displayCount = dto.displayCount,
                        aspectRatio = dto.aspectRatio,
                        isArchived = dto.isArchived,
                        sortOrder = dto.sortOrder,
                        createdAt = existing?.createdAt ?: System.currentTimeMillis()
                    )
                    val stackId = if (existing == null) {
                        cardStackDao.insertStack(stackEntity)
                    } else {
                        cardStackDao.updateStack(stackEntity)
                        existing.id
                    }
                    deckIdMap[dto.id] = stackId
                    tagDao.insertStackTagRef(CardStackTagCrossRef(stackId, packTagId))

                    if (existing != null && !preserveStoryCards) {
                        cardFaceDao.deleteFacesForStack(existing.id)
                        cardDao.deleteCardsByStackId(existing.id)
                    }
                }

                val cardIdMap = mutableMapOf<Long, Long>()
                backup.cards.forEach { cardDto ->
                    val newStackId = deckIdMap[cardDto.stackId] ?: return@forEach
                    val sourceFace = backup.cardFaces.firstOrNull { it.cardId == cardDto.id }
                    val preserved = if (preserveStoryCards) sourceFace?.imagePath?.let(existingStoryByImage::get) else null
                    if (preserved != null) {
                        val oldCard = preserved.first
                        cardDao.updateCard(oldCard.copy(
                            stackId = newStackId,
                            originDeckId = cardDto.originDeckId?.let { deckIdMap[it] } ?: newStackId,
                            title = if (oldCard.title.matches(Regex(".*#[0-9]+"))) cardDto.title else oldCard.title,
                            suit = cardDto.suit,
                            sortOrder = cardDto.sortOrder
                        ))
                        cardIdMap[cardDto.id] = oldCard.id
                        tagDao.insertCardTagRef(CardTagCrossRef(oldCard.id, packTagId))
                        return@forEach
                    }
                    val cardEntity = CardEntity(
                        id = 0,
                        stackId = newStackId,
                        originDeckId = cardDto.originDeckId?.let { deckIdMap[it] } ?: newStackId,
                        title = cardDto.title,
                        suit = cardDto.suit,
                        value = cardDto.value,
                        currentFaceIndex = cardDto.currentFaceIndex,
                        currentRotation = cardDto.currentRotation,
                        isReversed = cardDto.isReversed,
                        isDrawn = cardDto.isDrawn,
                        isRevealed = cardDto.isRevealed,
                        sortOrder = cardDto.sortOrder,
                        linkedTableId = cardDto.linkedTableId?.let { randomTableIdMap[it] ?: it },
                        dmNotes = cardDto.dmNotes,
                        lastDrawnAt = cardDto.lastDrawnAt
                    )
                    val cardId = cardDao.insertCard(cardEntity)
                    cardIdMap[cardDto.id] = cardId
                    tagDao.insertCardTagRef(CardTagCrossRef(cardId, packTagId))
                }

                backup.cardFaces.forEach { faceDto ->
                    val newCardId = cardIdMap[faceDto.cardId] ?: return@forEach
                    val preservedFace = if (preserveStoryCards) faceDto.imagePath?.let(existingStoryByImage::get)?.second else null
                    if (preservedFace != null) {
                        cardFaceDao.updateFace(preservedFace.copy(
                            name = if (preservedFace.name.matches(Regex(".*#[0-9]+"))) faceDto.name else preservedFace.name,
                            contentMode = faceDto.contentMode,
                            zonesJson = if (preservedFace.zonesJson == "[]") faceDto.zonesJson else preservedFace.zonesJson
                        ))
                        return@forEach
                    }
                    val faceEntity = CardFaceEntity(
                        id = 0,
                        cardId = newCardId,
                        faceIndex = faceDto.faceIndex,
                        name = faceDto.name,
                        imagePath = faceDto.imagePath,
                        contentMode = faceDto.contentMode,
                        zonesJson = faceDto.zonesJson,
                        reversedImagePath = faceDto.reversedImagePath
                    )
                    cardFaceDao.insertFace(faceEntity)
                }
            }
        }
    }

    private suspend fun importRandomTables(
        backup: com.deckapp.core.model.backup.FullBackupDto,
        packName: String,
        replaceExisting: Boolean
    ): Map<Long, Long> {
        val randomTableIdMap = mutableMapOf<Long, Long>()
        val writtenTableIds = mutableSetOf<Long>()
        backup.randomTables.forEach { dto ->
            val existing = if (replaceExisting) randomTableDao.getTableByPackAndName(packName, dto.name)
                else randomTableDao.getOwnedTableByPackAndName(packName, dto.name)
            if (existing != null && !replaceExisting) {
                randomTableIdMap[dto.id] = existing.id
                return@forEach
            }
            val entity = RandomTableEntity(
                id = existing?.id ?: 0,
                bundleId = null,
                name = dto.name,
                category = dto.category,
                description = dto.description,
                rollFormula = dto.rollFormula,
                rollMode = dto.rollMode,
                isNoRepeat = dto.isNoRepeat,
                isPinned = dto.isPinned,
                sourceType = "JSON",
                sourceName = packName,
                isBuiltIn = true,
                sortOrder = dto.sortOrder,
                createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                sourcePack = packName
            )
            val tableId = if (existing == null) {
                randomTableDao.insertTable(entity)
            } else {
                randomTableDao.updateTable(entity)
                existing.id
            }
            randomTableIdMap[dto.id] = tableId
            writtenTableIds += tableId
            randomTableDao.deleteEntriesForTable(tableId)
        }

        backup.tableEntries.forEach { dto ->
            val newTableId = randomTableIdMap[dto.tableId]
            if (newTableId != null && newTableId in writtenTableIds) {
                randomTableDao.insertEntries(listOf(
                    TableEntryEntity(
                        id = 0,
                        tableId = newTableId,
                        minRoll = dto.minRoll,
                        maxRoll = dto.maxRoll,
                        weight = dto.weight,
                        text = dto.text,
                        subTableRef = dto.subTableRef,
                        subTableId = dto.subTableId?.let { randomTableIdMap[it] },
                        sortOrder = dto.sortOrder
                    )
                ))
            }
        }
        return randomTableIdMap
    }

    override suspend fun updateStarterPackArtwork(artwork: com.deckapp.core.model.backup.FullBackupDto, packName: String,
        previous: com.deckapp.core.model.backup.FullBackupDto?) {
        db.withTransaction {
            // Recheck ownership in the transaction so an uninstall cannot resurrect the pack.
            val ownedNpcs = db.npcDao().getNpcsByTagName(packName).associateBy { it.name }
            if (ownedNpcs.isEmpty() && ruleDao.getAllRulesSync().none { it.sourcePack == packName } &&
                tableDao.getAllTablesSync().none { it.sourcePack == packName }) return@withTransaction
            val oldProfiles = previous?.npcs?.associateBy { it.name }.orEmpty()
            // Detect an old library from its content, including restored old backups.
            // Once upgraded, intentionally deleted templates stay deleted on later launches.
            val needsBestiaryUpgrade = ownedNpcs.values.none {
                com.deckapp.core.model.ChoirProfile.fromNotes(it.notes) != null
            }
            val tagDao = db.tagDao()
            val tagId = tagDao.getTagByName(packName)?.id ?: tagDao.insertTag(
                TagEntity(name = packName, color = 0xFF8B0000.toInt()))
            artwork.npcs.forEach { dto ->
                val existing = ownedNpcs[dto.name]
                if (existing == null) {
                    if (!needsBestiaryUpgrade) return@forEach
                    // Insert a separate owned template rather than claiming a personal namesake.
                    run {
                        val id = db.npcDao().upsertNpc(NpcEntity(
                            name = dto.name, description = dto.description, imagePath = dto.imagePath,
                            maxHp = dto.maxHp, currentHp = dto.currentHp, armorClass = dto.armorClass,
                            initiativeBonus = dto.initiativeBonus, notes = dto.notes,
                            isMonster = dto.isMonster, createdAt = System.currentTimeMillis()))
                        db.npcDao().insertCrossRef(NpcTagCrossRef(id, tagId))
                    }
                    return@forEach
                }
                val old = oldProfiles[dto.name]
                val packagedPortrait = existing.imagePath.isNullOrBlank() ||
                    existing.imagePath?.startsWith("file:///android_asset/starter_packs/choir_of_flesh/") == true
                val notes = com.deckapp.core.domain.usecase.MergeChoirProfileUseCase.notes(existing.notes, dto.notes, old?.notes)
                val updated = existing.copy(
                    imagePath = if (packagedPortrait) dto.imagePath ?: existing.imagePath else existing.imagePath,
                    notes = notes,
                    description = if (existing.description == old?.description) dto.description else existing.description
                )
                if (updated != existing) db.npcDao().upsertNpc(updated)
            }
            // Existing encounters retain their own wound state, personal notes and numeric fields.
            // Only profiles linked to an NPC owned by this pack are eligible for this upgrade.
            val profilesById = ownedNpcs.mapNotNull { (name, npc) ->
                artwork.npcs.find { it.name == name }?.let { npc.id to it }
            }.toMap()
            val encounterDao = db.encounterDao()
            if (profilesById.isNotEmpty()) {
                encounterDao.getCreaturesByNpcIds(profilesById.keys.toList()).forEach { creature ->
                    val dto = profilesById[creature.npcId] ?: return@forEach
                    val notes = com.deckapp.core.domain.usecase.MergeChoirProfileUseCase.notes(
                        creature.notes, dto.notes, oldProfiles[dto.name]?.notes)
                    if (notes != creature.notes) encounterDao.updateCreature(creature.copy(notes = notes))
                }
            }
            artwork.systemRules.forEach { dto ->
                val existing = ruleDao.getRuleByPackAndTitle(packName, dto.title)
                if (existing == null) {
                    ruleDao.insertRule(SystemRuleEntity(
                        title = dto.title, content = dto.content, gameSystem = dto.gameSystem,
                        category = dto.category, sortOrder = dto.sortOrder, sourcePack = packName,
                        lastUpdated = System.currentTimeMillis()
                    ))
                } else if (existing.content == previous?.systemRules?.find { it.title == dto.title }?.content) {
                    ruleDao.updateRule(existing.copy(content = dto.content, lastUpdated = System.currentTimeMillis()))
                }
            }
            // Older installed versions can predate the pack's random tables.
            importRandomTables(artwork, packName, replaceExisting = false)
        }
    }

    override suspend fun removeStarterPack(packName: String) {
        db.withTransaction {
            tableDao.deleteByPack(packName)
            ruleDao.deleteByPack(packName)
            randomTableDao.deleteByPack(packName)
            db.npcDao().deleteNpcsByTagName(packName)
            val tag = db.tagDao().getTagByName(packName)
            if (tag != null) {
                val taggedStackIds = db.tagDao().getStackIdsForTag(tag.id)
                taggedStackIds.forEach { stackId ->
                    db.cardFaceDao().deleteFacesForStack(stackId)
                    db.cardDao().deleteCardsByStackId(stackId)
                    db.tagDao().deleteTagsForStack(stackId)
                    db.cardStackDao().deleteStack(stackId)
                }
                db.tagDao().deleteTag(tag.id)
            }
            val packSlug = packName.lowercase().replace(" ", "_")
            val tacticalMapDao = db.tacticalMapDao()
            val packMaps = tacticalMapDao.getAllMapsSync().filter { it.sessionId == null && it.imageUri.contains(packSlug) }
            packMaps.forEach { map ->
                tacticalMapDao.deletePinsByMapId(map.id)
                tacticalMapDao.deleteTokensByMapId(map.id)
                tacticalMapDao.deleteMap(map.id)
            }
        }
    }
}
