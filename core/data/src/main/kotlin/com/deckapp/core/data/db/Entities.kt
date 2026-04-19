package com.deckapp.core.data.db

import androidx.room.*

@Entity(tableName = "card_stacks")
data class CardStackEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String,               // StackType.name
    val description: String,
    val coverImagePath: String?,
    val sourceFolderPath: String?,
    val defaultContentMode: String, // CardContentMode.name
    val drawMode: String,           // DrawMode.name
    val drawFaceDown: Boolean = false,
    val backImagePath: String? = null,
    val displayCount: Boolean,
    val aspectRatio: String = "STANDARD", // CardAspectRatio.name
    val isArchived: Boolean = false,
    val sortOrder: Int = 0,
    val createdAt: Long
)

@Entity(
    tableName = "cards",
    foreignKeys = [
        ForeignKey(
            entity = CardStackEntity::class,
            parentColumns = ["id"],
            childColumns = ["stackId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CardStackEntity::class,
            parentColumns = ["id"],
            childColumns = ["originDeckId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("stackId"), Index("originDeckId")]
)
data class CardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val stackId: Long,
    val originDeckId: Long?,
    val title: String,
    val suit: String?,
    val value: Int?,
    val currentFaceIndex: Int,
    val currentRotation: Int,
    val isReversed: Boolean,
    val isDrawn: Boolean,
    val isRevealed: Boolean = true,
    val sortOrder: Int,
    val linkedTableId: Long? = null,
    @ColumnInfo(name = "dm_notes") val dmNotes: String? = null,
    @ColumnInfo(name = "last_drawn_at") val lastDrawnAt: Long? = null
)

/**
 * Una cara de una carta.
 * [zonesJson] almacena List<ContentZone> serializado como JSON
 * (evita una tabla adicional de zonas para simplificar el schema en Fase 1).
 */
@Entity(
    tableName = "card_faces",
    foreignKeys = [ForeignKey(
        entity = CardEntity::class,
        parentColumns = ["id"],
        childColumns = ["cardId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("cardId")]
)
data class CardFaceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cardId: Long,
    val faceIndex: Int,
    val name: String,
    val imagePath: String?,
    val contentMode: String,        // CardContentMode.name
    val zonesJson: String,          // JSON de List<ContentZone>
    val reversedImagePath: String?
)

@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val color: Int
)

@Entity(
    tableName = "card_stack_tags",
    primaryKeys = ["stackId", "tagId"],
    foreignKeys = [
        ForeignKey(entity = CardStackEntity::class, parentColumns = ["id"], childColumns = ["stackId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = TagEntity::class, parentColumns = ["id"], childColumns = ["tagId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("tagId")]  // A-3: índice para búsquedas por tag
)
data class CardStackTagCrossRef(val stackId: Long, val tagId: Long)

@Entity(
    tableName = "card_tags",
    primaryKeys = ["cardId", "tagId"],
    foreignKeys = [
        ForeignKey(entity = CardEntity::class, parentColumns = ["id"], childColumns = ["cardId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = TagEntity::class, parentColumns = ["id"], childColumns = ["tagId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("tagId")]  // A-3: índice para búsquedas por tag
)
data class CardTagCrossRef(val cardId: Long, val tagId: Long)

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val status: String,             // SessionStatus.name
    val scheduledDate: Long?,
    val summary: String?,
    val createdAt: Long,
    val endedAt: Long?,
    val showCardTitles: Boolean = true,
    @ColumnInfo(name = "dm_notes") val dmNotes: String? = null,
    val gameSystemsJson: String = "[\"General\"]"
)

@Entity(
    tableName = "session_deck_refs",
    primaryKeys = ["sessionId", "stackId"],
    foreignKeys = [
        ForeignKey(entity = SessionEntity::class, parentColumns = ["id"], childColumns = ["sessionId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = CardStackEntity::class, parentColumns = ["id"], childColumns = ["stackId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("stackId")]
)
data class SessionDeckRefEntity(
    val sessionId: Long,
    val stackId: Long,
    val drawModeOverride: String?,  // DrawMode.name o null
    val sortOrder: Int
)

@Entity(
    tableName = "session_table_refs",
    primaryKeys = ["sessionId", "tableId"],
    foreignKeys = [
        ForeignKey(entity = SessionEntity::class, parentColumns = ["id"], childColumns = ["sessionId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = RandomTableEntity::class, parentColumns = ["id"], childColumns = ["tableId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("tableId")]
)
data class SessionTableRefEntity(
    val sessionId: Long,
    val tableId: Long,
    val sortOrder: Int = 0
)

@Entity(
    tableName = "random_table_tags",
    primaryKeys = ["tableId", "tagId"],
    foreignKeys = [
        ForeignKey(entity = RandomTableEntity::class, parentColumns = ["id"], childColumns = ["tableId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = TagEntity::class, parentColumns = ["id"], childColumns = ["tagId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("tagId")]
)
data class RandomTableTagCrossRef(val tableId: Long, val tagId: Long)

// ── Random Tables ──────────────────────────────────────────────────────────

@Entity(tableName = "table_bundles")
data class TableBundleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val sourceUri: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "random_tables",
    foreignKeys = [
        ForeignKey(
            entity = TableBundleEntity::class,
            parentColumns = ["id"],
            childColumns = ["bundleId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("bundleId")]
)
data class RandomTableEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bundleId: Long? = null,
    val name: String,
    val category: String = "General",
    val description: String = "",
    val rollFormula: String = "1d6",
    val rollMode: String = "RANGE",       // TableRollMode.name
    val isNoRepeat: Boolean = false,
    val isPinned: Boolean = false,
    val sourceType: String = "MANUAL",    // OCR, CSV, JSON, MANUAL
    val sourceName: String? = null,
    val isBuiltIn: Boolean = false,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val sourcePack: String? = null
)

@Entity(
    tableName = "table_entries",
    foreignKeys = [ForeignKey(
        entity = RandomTableEntity::class,
        parentColumns = ["id"],
        childColumns = ["tableId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("tableId")]
)
data class TableEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tableId: Long,
    val minRoll: Int = 1,
    val maxRoll: Int = 1,
    val weight: Int = 1,
    val text: String,
    val subTableRef: String? = null,
    val subTableId: Long? = null,
    val sortOrder: Int = 0
)

@Entity(
    tableName = "table_roll_results",
    foreignKeys = [
        ForeignKey(
            entity = RandomTableEntity::class,
            parentColumns = ["id"],
            childColumns = ["tableId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId"), Index("tableId")]
)
data class TableRollResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tableId: Long,
    val tableName: String,
    val sessionId: Long?,
    val rollValue: Int,
    val resolvedText: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class TableWithEntries(
    @Embedded val table: RandomTableEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "tableId"
    )
    val entries: List<TableEntryEntity>
)

// ── Draw Events ─────────────────────────────────────────────────────────────

@Entity(
    tableName = "draw_events",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CardEntity::class,
            parentColumns = ["id"],
            childColumns = ["cardId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId"), Index("cardId")]
)
data class DrawEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val cardId: Long?,
    val action: String,     // DrawAction.name
    val metadata: String,
    val timestamp: Long
)

// ── Encounters ──────────────────────────────────────────────────────────────

@Entity(
    tableName = "encounters",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["linkedSessionId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("linkedSessionId")]
)
data class EncounterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String,
    val linkedSessionId: Long?,
    val isActive: Boolean,
    val currentRound: Int,
    val currentTurnIndex: Int,
    val createdAt: Long
)

@Entity(
    tableName = "encounter_creatures",
    foreignKeys = [ForeignKey(
        entity = EncounterEntity::class,
        parentColumns = ["id"],
        childColumns = ["encounterId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("encounterId")]
)
data class EncounterCreatureEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val encounterId: Long,
    val name: String,
    val maxHp: Int,
    val currentHp: Int,
    val armorClass: Int,
    val initiativeBonus: Int,
    val initiativeRoll: Int?,
    val conditionsJson: String,   // JSON de Set<Condition>
    val notes: String,
    val sortOrder: Int,
    val npcId: Long? = null,
    val imagePath: String? = null
)

@Entity(
    tableName = "combat_log",
    foreignKeys = [ForeignKey(
        entity = EncounterEntity::class,
        parentColumns = ["id"],
        childColumns = ["encounterId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("encounterId")]
)
data class CombatLogEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val encounterId: Long,
    val message: String,
    val type: String,               // CombatLogType.name
    val timestamp: Long
)

// ── Recent Files ───────────────────────────────────────────────────────────

@Entity(
    tableName = "recent_files",
    indices = [Index(value = ["uri"], unique = true)]
)
data class RecentFileRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uri: String,                // URI del archivo o carpeta
    val name: String,               // Nombre a mostrar
    val type: String,               // "PDF" o "FOLDER"
    val lastAccessed: Long = System.currentTimeMillis()
)
// ── NPCs ───────────────────────────────────────────────────────────────────

@Entity(tableName = "npcs")
data class NpcEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String,
    val imagePath: String?,
    val maxHp: Int,
    val currentHp: Int,
    val armorClass: Int,
    val initiativeBonus: Int,
    val notes: String,
    val isMonster: Boolean,
    val createdAt: Long
)

@Entity(
    tableName = "npc_tags",
    primaryKeys = ["npcId", "tagId"],
    foreignKeys = [
        ForeignKey(entity = NpcEntity::class, parentColumns = ["id"], childColumns = ["npcId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = TagEntity::class, parentColumns = ["id"], childColumns = ["tagId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("tagId")]
)
data class NpcTagCrossRef(val npcId: Long, val tagId: Long)

// ── World Wiki ─────────────────────────────────────────────────────────────

@Entity(tableName = "wiki_categories")
data class WikiCategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val iconName: String
)

@Entity(
    tableName = "wiki_entries",
    foreignKeys = [ForeignKey(
        entity = WikiCategoryEntity::class,
        parentColumns = ["id"],
        childColumns = ["categoryId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("categoryId")]
)
data class WikiEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
    val categoryId: Long,
    val imagePath: String?,
    val lastUpdated: Long
)

// ── Reference Tables ─────────────────────────────────────────────────────────

@Entity(tableName = "reference_tables")
data class ReferenceTableEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val gameSystem: String = "General",
    val category: String = "General",
    val columnsJson: String = "[]",
    val isPinned: Boolean = false,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val sourcePack: String? = null
)

@Entity(
    tableName = "reference_rows",
    foreignKeys = [ForeignKey(
        entity = ReferenceTableEntity::class,
        parentColumns = ["id"],
        childColumns = ["tableId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("tableId")]
)
data class ReferenceRowEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tableId: Long,
    val cellsJson: String = "[]",
    val sortOrder: Int = 0
)

@Entity(
    tableName = "reference_table_tags",
    primaryKeys = ["tableId", "tagId"],
    foreignKeys = [
        ForeignKey(entity = ReferenceTableEntity::class, parentColumns = ["id"], childColumns = ["tableId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = TagEntity::class, parentColumns = ["id"], childColumns = ["tagId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("tagId")]
)
data class ReferenceTableTagCrossRef(val tableId: Long, val tagId: Long)

@Entity(tableName = "system_rules")
data class SystemRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String = "",
    val gameSystem: String = "General",
    val category: String = "General",
    val isPinned: Boolean = false,
    val sortOrder: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis(),
    val sourcePack: String? = null
)

@Entity(
    tableName = "system_rule_tags",
    primaryKeys = ["ruleId", "tagId"],
    foreignKeys = [
        ForeignKey(entity = SystemRuleEntity::class, parentColumns = ["id"], childColumns = ["ruleId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = TagEntity::class, parentColumns = ["id"], childColumns = ["tagId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("tagId")]
)
data class SystemRuleTagCrossRef(val ruleId: Long, val tagId: Long)

data class ReferenceTableWithRows(
    @Embedded val table: ReferenceTableEntity,
    @Relation(parentColumn = "id", entityColumn = "tableId")
    val rows: List<ReferenceRowEntity>
)
