package com.deckapp.core.model.backup

import kotlinx.serialization.Serializable

@Serializable
data class FullBackupDto(
    val schemaVersion: Int = 1,
    val tags: List<TagBackupDto> = emptyList(),
    val decks: List<CardStackBackupDto> = emptyList(),
    val cards: List<CardBackupDto> = emptyList(),
    val cardFaces: List<CardFaceBackupDto> = emptyList(),
    val cardStackTags: List<CardStackTagBackupDto> = emptyList(),
    val cardTags: List<CardTagBackupDto> = emptyList(),
    val tableBundles: List<TableBundleBackupDto> = emptyList(),
    val randomTables: List<RandomTableBackupDto> = emptyList(),
    val tableEntries: List<TableEntryBackupDto> = emptyList(),
    val randomTableTags: List<RandomTableTagBackupDto> = emptyList(),
    val sessions: List<SessionBackupDto> = emptyList(),
    val sessionDeckRefs: List<SessionDeckRefBackupDto> = emptyList(),
    val sessionTableRefs: List<SessionTableRefBackupDto> = emptyList(),
    val drawEvents: List<DrawEventBackupDto> = emptyList(),
    val encounters: List<EncounterBackupDto> = emptyList(),
    val encounterCreatures: List<EncounterCreatureBackupDto> = emptyList(),
    val combatLog: List<CombatLogEntryBackupDto> = emptyList(),
    val npcs: List<NpcBackupDto> = emptyList(),
    val npcTags: List<NpcTagBackupDto> = emptyList(),
    val wikiCategories: List<WikiCategoryBackupDto> = emptyList(),
    val wikiEntries: List<WikiEntryBackupDto> = emptyList()
)

@Serializable data class TagBackupDto(val id: Long, val name: String, val color: Int)
@Serializable data class CardStackBackupDto(val id: Long, val name: String, val type: String, val description: String, val coverImagePath: String?, val sourceFolderPath: String?, val defaultContentMode: String, val drawMode: String, val drawFaceDown: Boolean, val backImagePath: String?, val displayCount: Boolean, val aspectRatio: String, val isArchived: Boolean, val sortOrder: Int, val createdAt: Long)
@Serializable data class CardBackupDto(val id: Long, val stackId: Long, val originDeckId: Long?, val title: String, val suit: String?, val value: Int?, val currentFaceIndex: Int, val currentRotation: Int, val isReversed: Boolean, val isDrawn: Boolean, val isRevealed: Boolean, val sortOrder: Int, val linkedTableId: Long?, val dmNotes: String?, val lastDrawnAt: Long?)
@Serializable data class CardFaceBackupDto(val id: Long, val cardId: Long, val faceIndex: Int, val name: String, val imagePath: String?, val contentMode: String, val zonesJson: String, val reversedImagePath: String?)

@Serializable data class CardStackTagBackupDto(val stackId: Long, val tagId: Long)
@Serializable data class CardTagBackupDto(val cardId: Long, val tagId: Long)
@Serializable data class RandomTableTagBackupDto(val tableId: Long, val tagId: Long)

@Serializable data class TableBundleBackupDto(val id: Long, val name: String, val description: String, val sourceUri: String?, val createdAt: Long)
@Serializable data class RandomTableBackupDto(val id: Long, val bundleId: Long?, val name: String, val description: String, val rollFormula: String, val rollMode: String, val isNoRepeat: Boolean, val isPinned: Boolean, val sourceType: String, val sourceName: String?, val isBuiltIn: Boolean, val sortOrder: Int, val createdAt: Long)
@Serializable data class TableEntryBackupDto(val id: Long, val tableId: Long, val minRoll: Int, val maxRoll: Int, val weight: Int, val text: String, val subTableRef: String?, val subTableId: Long?, val sortOrder: Int)

@Serializable data class SessionBackupDto(val id: Long, val name: String, val status: String, val scheduledDate: Long?, val summary: String?, val createdAt: Long, val endedAt: Long?, val showCardTitles: Boolean, val dmNotes: String?)
@Serializable data class SessionDeckRefBackupDto(val sessionId: Long, val stackId: Long, val drawModeOverride: String?, val sortOrder: Int)
@Serializable data class SessionTableRefBackupDto(val sessionId: Long, val tableId: Long, val sortOrder: Int)
@Serializable data class DrawEventBackupDto(val id: Long, val sessionId: Long, val cardId: Long?, val action: String, val metadata: String, val timestamp: Long)

@Serializable data class EncounterBackupDto(val id: Long, val name: String, val description: String, val linkedSessionId: Long?, val isActive: Boolean, val currentRound: Int, val currentTurnIndex: Int, val createdAt: Long)
@Serializable data class EncounterCreatureBackupDto(val id: Long, val encounterId: Long, val name: String, val maxHp: Int, val currentHp: Int, val armorClass: Int, val initiativeBonus: Int, val initiativeRoll: Int?, val conditionsJson: String, val notes: String, val sortOrder: Int, val npcId: Long?, val imagePath: String?)
@Serializable data class CombatLogEntryBackupDto(val id: Long, val encounterId: Long, val message: String, val type: String, val timestamp: Long)

@Serializable data class NpcBackupDto(val id: Long, val name: String, val description: String, val imagePath: String?, val maxHp: Int, val currentHp: Int, val armorClass: Int, val initiativeBonus: Int, val notes: String, val isMonster: Boolean, val createdAt: Long)
@Serializable data class NpcTagBackupDto(val npcId: Long, val tagId: Long)

@Serializable data class WikiCategoryBackupDto(val id: Long, val name: String, val iconName: String)
@Serializable data class WikiEntryBackupDto(val id: Long, val title: String, val content: String, val categoryId: Long, val imagePath: String?, val lastUpdated: Long)
