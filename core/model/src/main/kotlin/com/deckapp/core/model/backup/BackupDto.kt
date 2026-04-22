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
    val wikiEntries: List<WikiEntryBackupDto> = emptyList(),
    val referenceTables: List<ReferenceTableBackupDto> = emptyList(),
    val referenceRows: List<ReferenceRowBackupDto> = emptyList(),
    val referenceTableTags: List<ReferenceTableTagBackupDto> = emptyList(),
    val systemRules: List<SystemRuleBackupDto> = emptyList(),
    val systemRuleTags: List<SystemRuleTagBackupDto> = emptyList()
)

@Serializable data class TagBackupDto(val id: Long = 0L, val name: String = "", val color: Int = 0)
@Serializable data class CardStackBackupDto(val id: Long = 0L, val name: String = "", val type: String = "DECK", val description: String = "", val coverImagePath: String? = null, val sourceFolderPath: String? = null, val defaultContentMode: String = "FIT", val drawMode: String = "RANDOM", val drawFaceDown: Boolean = false, val backImagePath: String? = null, val displayCount: Boolean = true, val aspectRatio: String = "STANDARD", val isArchived: Boolean = false, val sortOrder: Int = 0, val createdAt: Long = 0L)
@Serializable data class CardBackupDto(val id: Long = 0L, val stackId: Long = 0L, val originDeckId: Long? = null, val title: String = "", val suit: String? = null, val value: Int? = null, val currentFaceIndex: Int = 0, val currentRotation: Int = 0, val isReversed: Boolean = false, val isDrawn: Boolean = false, val isRevealed: Boolean = true, val sortOrder: Int = 0, val linkedTableId: Long? = null, val dmNotes: String? = null, val lastDrawnAt: Long? = null)
@Serializable data class CardFaceBackupDto(val id: Long = 0L, val cardId: Long = 0L, val faceIndex: Int = 0, val name: String = "", val imagePath: String? = null, val contentMode: String = "FIT", val zonesJson: String = "[]", val reversedImagePath: String? = null)

@Serializable data class CardStackTagBackupDto(val stackId: Long = 0L, val tagId: Long = 0L)
@Serializable data class CardTagBackupDto(val cardId: Long = 0L, val tagId: Long = 0L)
@Serializable data class RandomTableTagBackupDto(val tableId: Long = 0L, val tagId: Long = 0L)

@Serializable data class TableBundleBackupDto(val id: Long = 0L, val name: String = "", val description: String = "", val sourceUri: String? = null, val createdAt: Long = 0L)
@Serializable data class RandomTableBackupDto(val id: Long = 0L, val bundleId: Long? = null, val name: String = "", val category: String = "General", val description: String = "", val rollFormula: String = "1d6", val rollMode: String = "RANGE", val isNoRepeat: Boolean = false, val isPinned: Boolean = false, val sourceType: String = "MANUAL", val sourceName: String? = null, val isBuiltIn: Boolean = false, val sortOrder: Int = 0, val createdAt: Long = 0L, val sourcePack: String? = null)
@Serializable data class TableEntryBackupDto(val id: Long = 0L, val tableId: Long = 0L, val minRoll: Int = 1, val maxRoll: Int = 1, val weight: Int = 1, val text: String = "", val subTableRef: String? = null, val subTableId: Long? = null, val sortOrder: Int = 0)

@Serializable data class SessionBackupDto(val id: Long = 0L, val name: String = "", val status: String = "COMPLETED", val scheduledDate: Long? = null, val summary: String? = null, val createdAt: Long = 0L, val endedAt: Long? = null, val showCardTitles: Boolean = true, val dmNotes: String? = null, val gameSystemsJson: String = "[\"General\"]")
@Serializable data class SessionDeckRefBackupDto(val sessionId: Long = 0L, val stackId: Long = 0L, val drawModeOverride: String? = null, val sortOrder: Int = 0)
@Serializable data class SessionTableRefBackupDto(val sessionId: Long = 0L, val tableId: Long = 0L, val sortOrder: Int = 0)
@Serializable data class DrawEventBackupDto(val id: Long = 0L, val sessionId: Long = 0L, val cardId: Long? = null, val action: String = "", val metadata: String = "", val timestamp: Long = 0L)

@Serializable data class EncounterBackupDto(val id: Long = 0L, val name: String = "", val description: String = "", val linkedSessionId: Long? = null, val isActive: Boolean = false, val currentRound: Int = 1, val currentTurnIndex: Int = 0, val createdAt: Long = 0L)
@Serializable data class EncounterCreatureBackupDto(val id: Long = 0L, val encounterId: Long = 0L, val name: String = "", val maxHp: Int = 10, val currentHp: Int = 10, val armorClass: Int = 10, val initiativeBonus: Int = 0, val initiativeRoll: Int? = null, val conditionsJson: String = "[]", val notes: String = "", val sortOrder: Int = 0, val npcId: Long? = null, val imagePath: String? = null)
@Serializable data class CombatLogEntryBackupDto(val id: Long = 0L, val encounterId: Long = 0L, val message: String = "", val type: String = "INFO", val timestamp: Long = 0L)

@Serializable data class NpcBackupDto(val id: Long = 0L, val name: String = "", val description: String = "", val imagePath: String? = null, val maxHp: Int = 10, val currentHp: Int = 10, val armorClass: Int = 10, val initiativeBonus: Int = 0, val notes: String = "", val isMonster: Boolean = false, val createdAt: Long = 0L)
@Serializable data class NpcTagBackupDto(val npcId: Long = 0L, val tagId: Long = 0L)

@Serializable data class WikiCategoryBackupDto(val id: Long = 0L, val name: String = "", val iconName: String = "folder")
@Serializable data class WikiEntryBackupDto(val id: Long = 0L, val title: String = "", val content: String = "", val categoryId: Long = 0L, val imagePath: String? = null, val isPinned: Boolean = false, val lastUpdated: Long = 0L)

@Serializable data class ReferenceTableBackupDto(val id: Long = 0L, val name: String = "", val description: String = "", val gameSystem: String = "General", val category: String = "General", val columnsJson: String = "[]", val isPinned: Boolean = false, val sortOrder: Int = 0, val createdAt: Long = 0L, val sourcePack: String? = null)
@Serializable data class ReferenceRowBackupDto(val id: Long = 0L, val tableId: Long = 0L, val cellsJson: String = "[]", val sortOrder: Int = 0)
@Serializable data class ReferenceTableTagBackupDto(val tableId: Long = 0L, val tagId: Long = 0L)
@Serializable data class SystemRuleBackupDto(val id: Long = 0L, val title: String = "", val content: String = "", val gameSystem: String = "General", val category: String = "General", val isPinned: Boolean = false, val sortOrder: Int = 0, val lastUpdated: Long = 0L, val sourcePack: String? = null)
@Serializable data class SystemRuleTagBackupDto(val ruleId: Long = 0L, val tagId: Long = 0L)
