package com.deckapp.core.data.db

import androidx.room.*

/**
 * Representa una Colección (Baúl/Carpeta) para organizar recursos.
 */
@Entity(tableName = "collections")
data class CollectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val color: Int,               // Color ARGB para personalización visual
    val iconName: String = "",    // Referencia al nombre del icono (Chest, Map, Skull, etc.)
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

/**
 * Relación muchos-a-muchos entre Colecciones y Recursos (Mazos o Tablas).
 */
@Entity(
    tableName = "collection_resource_refs",
    primaryKeys = ["collectionId", "resourceId", "resourceType"],
    indices = [
        Index(value = ["collectionId"]),
        Index(value = ["resourceId", "resourceType"])
    ]
)
data class CollectionResourceCrossRef(
    val collectionId: Long,
    val resourceId: Long,
    val resourceType: String // "DECK" o "TABLE"
)

/**
 * Objeto para cargar una colección con el conteo de sus recursos.
 */
data class CollectionWithCount(
    @Embedded val collection: CollectionEntity,
    @ColumnInfo(name = "resourceCount") val resourceCount: Int
)
