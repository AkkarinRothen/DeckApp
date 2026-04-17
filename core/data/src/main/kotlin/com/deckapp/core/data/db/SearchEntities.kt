package com.deckapp.core.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4

/**
 * FTS4 para búsqueda en cartas.
 * Indexa el título y las notas del DM.
 */
@Fts4(contentEntity = CardEntity::class)
@Entity(tableName = "cards_fts")
data class CardFtsEntity(
    val title: String,
    @ColumnInfo(name = "dm_notes") val dmNotes: String?
)

/**
 * FTS4 para búsqueda en caras de cartas.
 */
@Fts4(contentEntity = CardFaceEntity::class)
@Entity(tableName = "card_faces_fts")
data class CardFaceFtsEntity(
    val name: String
)

/**
 * FTS4 para búsqueda en tablas aleatorias.
 */
@Fts4(contentEntity = RandomTableEntity::class)
@Entity(tableName = "random_tables_fts")
data class RandomTableFtsEntity(
    val name: String,
    val description: String
)

/**
 * FTS4 para búsqueda en entradas de tablas.
 */
@Fts4(contentEntity = TableEntryEntity::class)
@Entity(tableName = "table_entries_fts")
data class TableEntryFtsEntity(
    val text: String
)
