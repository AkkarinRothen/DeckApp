package com.deckapp.core.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "hex_maps")
data class HexMapEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val rows: Int,
    val cols: Int,
    val sessionId: Long? = null,
    val hexStyle: String = "FLAT_TOP",
    val partyQ: Int? = null,
    val partyR: Int? = null,
    val maxActivitiesPerDay: Int = 8,
    val mapNotes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "hex_tiles",
    primaryKeys = ["mapId", "q", "r"],
    foreignKeys = [
        ForeignKey(
            entity = HexMapEntity::class,
            parentColumns = ["id"],
            childColumns = ["mapId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("mapId")]
)
data class HexTileEntity(
    val mapId: Long,
    val q: Int,
    val r: Int,
    val terrainCost: Int = 1,
    val terrainLabel: String = "",
    val terrainColor: Long = 0xFF7CB87BL,
    val dmNotes: String = "",
    val playerNotes: String = "",
    val isExplored: Boolean = false,
    val isReconnoitered: Boolean = false,
    val isMapped: Boolean = false
)

@Entity(
    tableName = "hex_pois",
    foreignKeys = [
        ForeignKey(
            entity = HexMapEntity::class,
            parentColumns = ["id"],
            childColumns = ["mapId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("mapId")]
)
data class HexPoiEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mapId: Long,
    val tileQ: Int,
    val tileR: Int,
    val name: String,
    val type: String,
    val description: String = "",
    val encounterId: Long? = null,
    val tableId: Long? = null
)

@Entity(
    tableName = "hex_days",
    foreignKeys = [
        ForeignKey(
            entity = HexMapEntity::class,
            parentColumns = ["id"],
            childColumns = ["mapId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("mapId")]
)
data class HexDayEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mapId: Long,
    val dayNumber: Int,
    val activitiesLog: String = "[]",   // JSON array of HexActivityEntry
    val notes: String = ""
)
