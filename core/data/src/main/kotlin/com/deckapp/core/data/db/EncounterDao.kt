package com.deckapp.core.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface EncounterDao {
    @Query("SELECT * FROM encounters ORDER BY createdAt DESC")
    fun getAllEncounters(): Flow<List<EncounterEntity>>

    @Query("SELECT * FROM encounters WHERE id = :id")
    fun getEncounterById(id: Long): Flow<EncounterEntity?>

    @Query("SELECT * FROM encounters WHERE linkedSessionId = :sessionId AND isActive = 1 LIMIT 1")
    fun getActiveEncounterForSession(sessionId: Long): Flow<EncounterEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEncounter(encounter: EncounterEntity): Long

    @Update
    suspend fun updateEncounter(encounter: EncounterEntity)

    @Query("DELETE FROM encounters WHERE id = :id")
    suspend fun deleteEncounter(id: Long)

    @Query("UPDATE encounters SET isActive = 0 WHERE linkedSessionId = :sessionId")
    suspend fun deactivateAllEncountersForSession(sessionId: Long)

    // --- Creatures ---

    @Query("SELECT * FROM encounter_creatures WHERE encounterId = :encounterId ORDER BY sortOrder ASC")
    fun getCreaturesForEncounter(encounterId: Long): Flow<List<EncounterCreatureEntity>>

    @Query("SELECT * FROM encounter_creatures WHERE id = :id")
    suspend fun getCreatureById(id: Long): EncounterCreatureEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCreatures(creatures: List<EncounterCreatureEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCreature(creature: EncounterCreatureEntity): Long

    @Update
    suspend fun updateCreature(creature: EncounterCreatureEntity)

    @Query("DELETE FROM encounter_creatures WHERE id = :id")
    suspend fun deleteCreature(id: Long)

    @Query("DELETE FROM encounter_creatures WHERE encounterId = :encounterId")
    suspend fun deleteCreaturesForEncounter(encounterId: Long)
}
