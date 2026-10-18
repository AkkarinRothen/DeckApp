package com.deckapp.core.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tactical_maps",
    indices = [
        Index("sessionId"),
        Index("sceneId")
    ]
)
data class TacticalMapEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val imageUri: String,
    val gridType: String = "SQUARE",
    val cellSizePx: Float = 70f,
    val gridOffsetX: Float = 0f,
    val gridOffsetY: Float = 0f,
    val gridColorHex: Long = 0x40FFFFFFL,
    val gridOpacity: Float = 0.5f,
    val scaleFtPerCell: Int = 5,
    val diagonalRule: String = "ALTERNATING_5_10_5",
    val sessionId: Long? = null,
    val sceneId: Long? = null,
    val fogOfWarMaskJson: String = "",
    val terrainMaskJson: String = "",
    val areasJson: String = "[]",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "tactical_tokens",
    foreignKeys = [
        ForeignKey(
            entity = TacticalMapEntity::class,
            parentColumns = ["id"],
            childColumns = ["mapId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("mapId"),
        Index("encounterCreatureId"),
        Index("npcId")
    ]
)
data class TacticalTokenEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mapId: Long,
    val name: String,
    val avatarUri: String? = null,
    val faction: String = "ENEMY",
    val sizeInCells: Float = 1f,
    val cellX: Float = 0f,
    val cellY: Float = 0f,
    val currentHp: Int = 10,
    val maxHp: Int = 10,
    val tempHp: Int = 0,
    val armorClass: Int = 10,
    val speedFt: Int = 30,
    val conditionsJson: String = "[]",
    val isHidden: Int = 0,
    val elevationFt: Int = 0,
    val encounterCreatureId: Long? = null,
    val npcId: Long? = null,
    val notes: String = ""
)

@Entity(
    tableName = "tactical_map_pins",
    foreignKeys = [
        ForeignKey(
            entity = TacticalMapEntity::class,
            parentColumns = ["id"],
            childColumns = ["mapId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("mapId")
    ]
)
data class TacticalMapPinEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mapId: Long,
    val cellX: Float,
    val cellY: Float,
    val title: String,
    val description: String = "",
    val iconName: String = "info",
    val colorHex: Long = 0xFFFFA000L,
    val isSecret: Int = 1,
    val explorationState: String = "UNKNOWN",
    val targetTableId: Long? = null,
    val targetEncounterId: Long? = null,
    val targetWikiEntryId: Long? = null,
    val targetMapId: Long? = null,
    val discoveriesJson: String = "[]",
    val tableLinksJson: String = "[]"
)
