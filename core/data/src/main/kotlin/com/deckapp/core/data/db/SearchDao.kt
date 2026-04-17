package com.deckapp.core.data.db

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Pojo para resultados de búsqueda simplificados.
 * El [rowid] en FTS coincide con el [id] de la tabla de contenido.
 */
data class SearchResultId(
    val rowid: Long
)

data class EntrySearchResult(
    val rowid: Long,
    val tableId: Long
)

@Dao
interface SearchDao {

    @Query("""
        SELECT rowid FROM cards_fts 
        WHERE cards_fts MATCH :query
    """)
    fun searchCardIds(query: String): Flow<List<SearchResultId>>

    @Query("""
        SELECT rowid FROM card_faces_fts 
        WHERE card_faces_fts MATCH :query
    """)
    fun searchCardFaceIds(query: String): Flow<List<SearchResultId>>

    @Query("""
        SELECT rowid FROM random_tables_fts 
        WHERE random_tables_fts MATCH :query
    """)
    fun searchTableIds(query: String): Flow<List<SearchResultId>>

    @Query("""
        SELECT f.rowid, e.tableId 
        FROM table_entries_fts f
        INNER JOIN table_entries e ON f.rowid = e.id
        WHERE table_entries_fts MATCH :query
    """)
    fun searchTableEntriesWithTableId(query: String): Flow<List<EntrySearchResult>>
}
