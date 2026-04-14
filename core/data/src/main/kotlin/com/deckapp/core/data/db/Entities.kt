package com.deckapp.core.data.db

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

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
    val displayCount: Boolean,
    val aspectRatio: String = "STANDARD", // CardAspectRatio.name
    val createdAt: Long
)

@Entity(
    tableName = "cards",
    foreignKeys = [ForeignKey(
        entity = CardStackEntity::class,
        parentColumns = ["id"],
        childColumns = ["stackId"],
        onDelete = ForeignKey.CASCADE
    )],
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
    val sortOrder: Int
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
    ]
)
data class CardStackTagCrossRef(val stackId: Long, val tagId: Long)

@Entity(
    tableName = "card_tags",
    primaryKeys = ["cardId", "tagId"],
    foreignKeys = [
        ForeignKey(entity = CardEntity::class, parentColumns = ["id"], childColumns = ["cardId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = TagEntity::class, parentColumns = ["id"], childColumns = ["tagId"], onDelete = ForeignKey.CASCADE)
    ]
)
data class CardTagCrossRef(val cardId: Long, val tagId: Long)

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val isActive: Boolean,
    val createdAt: Long,
    val endedAt: Long?,
    val showCardTitles: Boolean = true,
    val dmNotes: String? = null
)

@Entity(
    tableName = "session_deck_refs",
    primaryKeys = ["sessionId", "stackId"],
    foreignKeys = [
        ForeignKey(entity = SessionEntity::class, parentColumns = ["id"], childColumns = ["sessionId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = CardStackEntity::class, parentColumns = ["id"], childColumns = ["stackId"], onDelete = ForeignKey.CASCADE)
    ]
)
data class SessionDeckRefEntity(
    val sessionId: Long,
    val stackId: Long,
    val drawModeOverride: String?,  // DrawMode.name o null
    val sortOrder: Int
)

// ── Random Tables ──────────────────────────────────────────────────────────

@Entity(tableName = "random_tables")
data class RandomTableEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val category: String = "",
    val rollFormula: String = "1d6",
    val rollMode: String = "RANGE",       // TableRollMode.name
    val isBuiltIn: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
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
    foreignKeys = [ForeignKey(
        entity = SessionEntity::class,
        parentColumns = ["id"],
        childColumns = ["sessionId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("sessionId")]
)
data class DrawEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val cardId: Long,
    val action: String,     // DrawAction.name
    val metadata: String,
    val timestamp: Long
)
