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

    private suspend fun syncUnlocked(npcId: Long, originCreatureId: Long? = null) {
        val npc = npcs.getNpcById(npcId) ?: return
        val sheet = ChoirCharacterCodec.read(npc.notes) ?: return
        val updatedIds = mutableSetOf<Long>()
        encounters.getAllEncounters().first().filter { it.isActive }.forEach { header ->
            encounters.getEncounterById(header.id).first()?.creatures?.filter { it.npcId == npcId }?.forEach { c ->
                updateCreature(c, sheet)
                updatedIds += c.id
            }
        }
        if (originCreatureId != null && originCreatureId !in updatedIds) {
            encounters.getCreatureById(originCreatureId)?.takeIf { it.npcId == npcId }?.let { updateCreature(it, sheet) }
        }
    }

    private suspend fun updateCreature(creature: EncounterCreature, sheet: ChoirCharacter) {
        encounters.updateCreature(creature.copy(notes = ChoirCharacterCodec.write(creature.notes, sheet),
            maxHp = 1, currentHp = if (ChoirCharacterRules.isDead(sheet)) 0 else 1,
            armorClass = ChoirCharacterRules.defense(sheet), initiativeBonus = ChoirCharacterRules.initiative(sheet)))
    }

    /** Persists edits made outside combat, such as a rest at the settlement. */
    suspend fun saveSheet(creatureId: Long, edited: ChoirCharacter): Boolean = mutex.withLock {
        val creature = encounters.getCreatureById(creatureId) ?: return@withLock false
        val npc = creature.npcId?.let { npcs.getNpcById(it) }?.takeIf { ChoirCharacterCodec.read(it.notes) != null }
        val previous = npc?.let { ChoirCharacterCodec.read(it.notes) } ?: ChoirCharacterCodec.read(creature.notes)
            ?: return@withLock false
        if (edited == previous) return@withLock true
        val changed = ChoirCharacterRules.normalize(edited.copy(undoState = ChoirCharacterCodec.snapshot(previous)))
        if (npc != null) {
            npcs.saveNpc(ChoirCharacterRules.toNpc(npc, changed))
            syncUnlocked(npc.id, creatureId)
        } else updateCreature(creature, changed)
        true
    }

    suspend operator fun invoke(creatureId: Long, command: ChoirCharacterCommand) = mutex.withLock {
        val creature = encounters.getCreatureById(creatureId) ?: return@withLock
        val npc = creature.npcId?.let { npcs.getNpcById(it) }?.takeIf { ChoirCharacterCodec.read(it.notes) != null }
        val sheet = npc?.let { ChoirCharacterCodec.read(it.notes) } ?: ChoirCharacterCodec.read(creature.notes) ?: return@withLock
        val result = ChoirCharacterRules.apply(sheet, command)
        if (result == sheet) return@withLock
        val changed = if (command == ChoirCharacterCommand.UNDO) result
            else result.copy(undoState = ChoirCharacterCodec.snapshot(sheet))
        if (npc != null) {
            npcs.saveNpc(ChoirCharacterRules.toNpc(npc, changed))
            syncUnlocked(npc.id, creatureId)
        } else updateCreature(creature, changed)
        encounters.recordLog(CombatLogEntry(encounterId = creature.encounterId,
            message = "${creature.name}: ${changed.major}/4 heridas mayores, ${changed.minor}/8 menores; Vigor ${changed.vigor} (${command.name})",
            type = CombatLogType.META))
    }

    suspend fun applyAftermath(creatureId: Long, request: ChoirAftermathRequest): ChoirAftermathResult? = mutex.withLock {
        val creature = encounters.getCreatureById(creatureId) ?: return@withLock null
        val npc = creature.npcId?.let { npcs.getNpcById(it) }?.takeIf { ChoirCharacterCodec.read(it.notes) != null }
        val sheet = npc?.let { ChoirCharacterCodec.read(it.notes) } ?: ChoirCharacterCodec.read(creature.notes) ?: return@withLock null
        val result = ChoirCombatAftermathUseCase.resolve(request.copy(character = sheet))
        val changed = result.updatedCharacter
        if (npc != null) {
            npcs.saveNpc(ChoirCharacterRules.toNpc(npc, changed))
            syncUnlocked(npc.id, creatureId)
        } else {
            updateCreature(creature, changed)
        }
        encounters.recordLog(CombatLogEntry(
            encounterId = creature.encounterId,
            message = "${creature.name} Aftermath: Trauma +${result.traumaGained} (Total: ${changed.trauma}), Heridas limpiadas. XP +${request.xpGained}.",
            type = CombatLogType.META
        ))
        result
    }
}
