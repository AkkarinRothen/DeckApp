package com.deckapp.core.data.repository

import com.deckapp.core.data.db.*
import com.deckapp.core.domain.repository.EncounterRepository
import com.deckapp.core.model.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
class EncounterRepositoryImpl @Inject constructor(
    private val encounterDao: EncounterDao,
    private val combatLogDao: CombatLogDao
) : EncounterRepository {

    override fun getAllEncounters(): Flow<List<Encounter>> {
        return encounterDao.getAllEncounters().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getEncounterById(id: Long): Flow<Encounter?> {
        return encounterDao.getEncounterById(id).flatMapLatest { encounterEntity ->
            if (encounterEntity == null) flowOf(null)
            else {
                encounterDao.getCreaturesForEncounter(encounterEntity.id).map { creatures ->
                    encounterEntity.toDomain(creatures.map { it.toDomain() })
                }
            }
        }
    }

    override fun getActiveEncounter(sessionId: Long): Flow<Encounter?> {
        return encounterDao.getActiveEncounterForSession(sessionId).flatMapLatest { encounterEntity ->
            if (encounterEntity == null) flowOf(null)
            else {
                encounterDao.getCreaturesForEncounter(encounterEntity.id).map { creatures ->
                    encounterEntity.toDomain(creatures.map { it.toDomain() })
                }
            }
        }
    }

    override suspend fun getCreatureById(id: Long): EncounterCreature? {
        return encounterDao.getCreatureById(id)?.toDomain()
    }

    override suspend fun saveEncounter(encounter: Encounter): Long {
        val encounterId = encounterDao.insertEncounter(encounter.toEntity())
        val creatures = encounter.creatures.map { it.copy(encounterId = encounterId).toEntity() }
        if (creatures.isNotEmpty()) {
            encounterDao.insertCreatures(creatures)
        }
        return encounterId
    }

    override suspend fun deleteEncounter(encounterId: Long) {
        encounterDao.deleteEncounter(encounterId)
    }

    override suspend fun updateCreature(creature: EncounterCreature) {
        encounterDao.updateCreature(creature.toEntity())
    }

    override suspend fun deleteCreature(creatureId: Long) {
        encounterDao.deleteCreature(creatureId)
    }

    override suspend fun startEncounterInSession(encounterId: Long, sessionId: Long): Encounter {
        // 1. Obtener encuentro original (one-shot)
        val originalEncounterEntity = encounterDao.getEncounterById(encounterId).first() 
            ?: throw IllegalArgumentException("Encounter $encounterId not found")
        val originalCreatures = encounterDao.getCreaturesForEncounter(encounterId).first()

        // 2. Desactivar encuentros previos en la sesión
        encounterDao.deactivateAllEncountersForSession(sessionId)

        // 3. Crear copia del encuentro para la sesión
        val newEncounterId = encounterDao.insertEncounter(
            originalEncounterEntity.copy(
                id = 0,
                linkedSessionId = sessionId,
                isActive = true,
                currentRound = 1,
                currentTurnIndex = 0,
                createdAt = System.currentTimeMillis()
            )
        )

        // 4. Copiar criaturas (reseteando HP y tirando iniciativa)
        val newCreatures = originalCreatures.map { creature ->
            creature.copy(
                id = 0,
                encounterId = newEncounterId,
                currentHp = creature.maxHp, // Reset HP
                initiativeRoll = (1..20).random() // Auto-roll initiative
            )
        }
        encounterDao.insertCreatures(newCreatures)

        // 5. Devolver el encuentro mapeado
        return encounterDao.getEncounterById(newEncounterId).first()!!.toDomain(
            newCreatures.map { it.toDomain() }
        )
    }

    override fun getLogForEncounter(encounterId: Long): Flow<List<CombatLogEntry>> {
        return combatLogDao.getLogForEncounter(encounterId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun recordLog(entry: CombatLogEntry) {
        combatLogDao.insertEntry(entry.toEntity())
    }
}
