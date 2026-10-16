package com.deckapp.core.domain.usecase

import com.deckapp.core.domain.repository.*
import com.deckapp.core.model.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/** One live NPC sheet shared by all its active encounter representations. */
@Singleton
class UpdateChoirCharacterUseCase @Inject constructor(
    private val npcs: NpcRepository, private val encounters: EncounterRepository
) {
    private val mutex = Mutex()
    suspend fun sync(npcId: Long) = mutex.withLock { syncUnlocked(npcId) }

    private suspend fun syncUnlocked(npcId: Long) {
        val npc = npcs.getNpcById(npcId) ?: return
        val sheet = ChoirCharacterCodec.read(npc.notes) ?: return
        encounters.getAllEncounters().first().filter { it.isActive }.forEach { header ->
            encounters.getEncounterById(header.id).first()?.creatures?.filter { it.npcId == npcId }?.forEach { c ->
                encounters.updateCreature(c.copy(notes = ChoirCharacterCodec.write(c.notes, sheet),
                    maxHp = 1, currentHp = if (ChoirCharacterRules.isDead(sheet)) 0 else 1,
                    armorClass = ChoirCharacterRules.defense(sheet), initiativeBonus = ChoirCharacterRules.initiative(sheet)))
            }
        }
    }

    suspend operator fun invoke(creatureId: Long, command: ChoirCharacterCommand) = mutex.withLock {
        val creature = encounters.getCreatureById(creatureId) ?: return@withLock
        val npc = creature.npcId?.let { npcs.getNpcById(it) }
        val sheet = npc?.let { ChoirCharacterCodec.read(it.notes) } ?: ChoirCharacterCodec.read(creature.notes) ?: return@withLock
        val result = ChoirCharacterRules.apply(sheet, command)
        if (result == sheet) return@withLock
        val changed = if (command == ChoirCharacterCommand.UNDO) result
            else result.copy(undoState = ChoirCharacterCodec.snapshot(sheet))
        if (npc != null) {
            npcs.saveNpc(ChoirCharacterRules.toNpc(npc, changed))
            syncUnlocked(npc.id)
        } else encounters.updateCreature(creature.copy(notes = ChoirCharacterCodec.write(creature.notes, changed),
            currentHp = if (ChoirCharacterRules.isDead(changed)) 0 else 1, maxHp = 1))
        encounters.recordLog(CombatLogEntry(encounterId = creature.encounterId,
            message = "${creature.name}: ${changed.major}/4 heridas mayores, ${changed.minor}/8 menores; Vigor ${changed.vigor} (${command.name})",
            type = CombatLogType.META))
    }
}
