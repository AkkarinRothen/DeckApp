package com.deckapp.core.data.db

import androidx.room.ColumnInfo
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
    @ColumnInfo(defaultValue = "FLAT_TOP")
    val hexStyle: String = "FLAT_TOP",
    val partyQ: Int? = null,
    val partyR: Int? = null,
    @ColumnInfo(defaultValue = "0")
    val isRadial: Boolean = false,
    @ColumnInfo(defaultValue = "8")
    val maxActivitiesPerDay: Int = 8,
    @ColumnInfo(defaultValue = "")
    val mapNotes: String = "",
    val weatherTableId: Long? = null,
    val travelEventTableId: Long? = null,
    @ColumnInfo(defaultValue = "{}")
    val terrainTableConfig: String = "{}",
    @ColumnInfo(defaultValue = "{}")
    val sessionResources: String = "{}",
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
    @ColumnInfo(defaultValue = "1")
    val terrainCost: Int = 1,
    @ColumnInfo(defaultValue = "")
    val terrainLabel: String = "",
    @ColumnInfo(defaultValue = "-2183045")
    val terrainColor: Long = 0xFF7CB87BL,
    @ColumnInfo(defaultValue = "")
    val dmNotes: String = "",
    @ColumnInfo(defaultValue = "")
    val playerNotes: String = "",
    @ColumnInfo(defaultValue = "0")
    val isExplored: Boolean = false,
    @ColumnInfo(defaultValue = "0")
    val isReconnoitered: Boolean = false,
    @ColumnInfo(defaultValue = "0")
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
    @ColumnInfo(defaultValue = "")
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
    @ColumnInfo(defaultValue = "[]")
    val activitiesLog: String = "[]",   // JSON array of HexActivityEntry
    @ColumnInfo(defaultValue = "")
    val notes: String = ""
)
