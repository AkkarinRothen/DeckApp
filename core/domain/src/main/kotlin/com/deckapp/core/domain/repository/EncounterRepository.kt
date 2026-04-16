package com.deckapp.core.domain.repository

import com.deckapp.core.model.CombatLogEntry
import com.deckapp.core.model.Encounter
import com.deckapp.core.model.EncounterCreature
import kotlinx.coroutines.flow.Flow

interface EncounterRepository {
    fun getAllEncounters(): Flow<List<Encounter>>
    fun getEncounterById(id: Long): Flow<Encounter?>
    fun getActiveEncounter(sessionId: Long): Flow<Encounter?>
    suspend fun getCreatureById(id: Long): EncounterCreature?
    
    suspend fun saveEncounter(encounter: Encounter): Long
    suspend fun deleteEncounter(encounterId: Long)
    
    suspend fun updateCreature(creature: EncounterCreature)
    suspend fun deleteCreature(creatureId: Long)
    
    /**
     * Inicia un encuentro en una sesión específica.
     * Realiza una copia profunda del encuentro y sus criaturas.
     */
    suspend fun startEncounterInSession(encounterId: Long, sessionId: Long): Encounter

    // --- Combat Log ---
    fun getLogForEncounter(encounterId: Long): Flow<List<CombatLogEntry>>
    suspend fun recordLog(entry: CombatLogEntry)
}
