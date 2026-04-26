package com.deckapp.core.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CollectionDao {

    @Query("SELECT * FROM collections ORDER BY name ASC")
    fun getAllCollections(): Flow<List<CollectionEntity>>

    @Query("""
        SELECT c.*, (SELECT COUNT(*) FROM collection_resource_refs WHERE collectionId = c.id) as resourceCount
        FROM collections c
        ORDER BY name ASC
    """)
    fun getAllCollectionsWithCount(): Flow<List<CollectionWithCount>>

    @Query("SELECT * FROM collections WHERE id = :id")
    fun getCollectionById(id: Long): Flow<CollectionEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCollection(collection: CollectionEntity): Long

    @Update
    suspend fun updateCollection(collection: CollectionEntity)

    @Query("DELETE FROM collections WHERE id = :id")
    suspend fun deleteCollection(id: Long)

    @Query("UPDATE collections SET imageUrl = :imageUrl WHERE id = :id")
    suspend fun updateCollectionImage(id: Long, imageUrl: String?)

    // --- Relaciones de Recursos ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResourceRef(ref: CollectionResourceCrossRef)

    @Query("DELETE FROM collection_resource_refs WHERE collectionId = :collectionId AND resourceId = :resourceId AND resourceType = :resourceType")
    suspend fun removeResourceFromCollection(collectionId: Long, resourceId: Long, resourceType: String)

    @Query("DELETE FROM collection_resource_refs WHERE collectionId = :collectionId")
    suspend fun clearCollection(collectionId: Long)

    @Transaction
    @Query("""
        SELECT * FROM card_stacks 
        WHERE id IN (SELECT resourceId FROM collection_resource_refs WHERE collectionId = :collectionId AND resourceType = 'DECK')
        ORDER BY sortOrder ASC, createdAt DESC
    """)
    fun getDecksInCollection(collectionId: Long): Flow<List<CardStackEntity>>

    @Transaction
    @Query("""
        SELECT * FROM random_tables 
        WHERE id IN (SELECT resourceId FROM collection_resource_refs WHERE collectionId = :collectionId AND resourceType = 'TABLE')
        ORDER BY sortOrder ASC, name ASC
    """)
    fun getTablesInCollection(collectionId: Long): Flow<List<RandomTableEntity>>
}
