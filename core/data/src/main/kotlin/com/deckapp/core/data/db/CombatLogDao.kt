package com.deckapp.core.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CombatLogDao {
    @Query("SELECT * FROM combat_log WHERE encounterId = :encounterId ORDER BY timestamp DESC")
    fun getLogForEncounter(encounterId: Long): Flow<List<CombatLogEntryEntity>>

    @Insert
    suspend fun insertEntry(entry: CombatLogEntryEntity)

    @Query("DELETE FROM combat_log WHERE encounterId = :encounterId")
    suspend fun clearLog(encounterId: Long)
}

