package com.deckapp.feature.draw.delegates

import com.deckapp.core.domain.repository.SessionRepository
import com.deckapp.core.domain.usecase.*
import com.deckapp.core.model.*
import com.deckapp.feature.draw.SessionUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.random.Random

/** Session-scoped procedure state and an idempotent journal outbox. */
class ChoirGuideDelegate(
    private val sessionId: Long,
    private val ui: MutableStateFlow<SessionUiState>,
    private val scope: CoroutineScope,
    private val sessions: SessionRepository,
    private val notesMutex: Mutex,
    private val updateMemory: ((BoardSessionMemory) -> BoardSessionMemory) -> Unit
) {
    private val mutex = Mutex()
    private fun memory() = ui.value.boardMemory.choirGuide
    private fun save(value: ChoirGuideMemory) = updateMemory { it.copy(choirGuide = value) }
    private fun id() = UUID.randomUUID().toString()

    private inline fun updateGuidesState(crossinline transform: (com.deckapp.feature.draw.model.state.GuidesSessionUiState) -> com.deckapp.feature.draw.model.state.GuidesSessionUiState) {
        ui.update { it.copy(guides = transform(it.guides)) }
    }

    private fun failure(t: Throwable) { updateGuidesState { it.copy(choirGuideError = t.message ?: "No se pudo guardar el procedimiento.") } }

    fun open() { ui.update { it.copy(modals = it.modals.copy(showChoirGuide = true), guides = it.guides.copy(choirGuideError = null)) } }
    fun close() { ui.update { it.copy(modals = it.modals.copy(showChoirGuide = false)) } }
    fun source(page: Int?) { updateGuidesState { it.copy(choirGuideSourcePage = page) } }

    fun select(procedure: ChoirProcedure) = scope.launch {
        mutex.withLock { runCatching { save(ChoirGuideRules.select(memory(), procedure)) }.onFailure(::failure) }
    }

    fun selectCharacter(npcId: Long?) = scope.launch {
        mutex.withLock {
            val sheet = ui.value.npcs.find { it.id == npcId }?.let { ChoirCharacterCodec.read(it.notes) }
            save(ChoirGuideRules.selectCharacter(memory(), if (sheet != null) npcId else null, sheet?.anguish))
        }
    }

    fun observeCharacters(npcs: List<Npc>) = scope.launch {
        mutex.withLock {
            if (ui.value.session?.gameSystems?.any { it.contains("Choir", true) } != true) return@withLock
            val before = memory()
            val npc = npcs.firstOrNull { it.id == before.selectedCharacterId } ?: return@withLock
            val sheet = ChoirCharacterCodec.read(npc.notes) ?: return@withLock
            val after = ChoirGuideRules.observeAnguish(before, npc.id, npc.name, sheet.anguish, id())
            if (after != before) save(after)
            if (after.anguishChecks.size > before.anguishChecks.size) open()
        }
    }

    fun startAnguish(value: Int, revision: Long) = scope.launch {
        mutex.withLock {
            if (revision != memory().revision) return@withLock
            runCatching {
                require(memory().anguishChecks.isEmpty()) { "Resuelve primero la prueba de Angustia que ya está pendiente." }
                val npc = ui.value.npcs.find { it.id == memory().selectedCharacterId }
                val sheet = npc?.let { ChoirCharacterCodec.read(it.notes) }
                save(ChoirGuideRules.interrupt(memory(), ChoirAnguishCheck(id(), npc?.id, npc?.name ?: "Personaje manual", sheet?.anguish ?: value)))
                open()
            }.onFailure(::failure)
        }
    }

    fun draft(roll: String? = null, note: String? = null) = scope.launch {
        mutex.withLock {
            val before = memory()
            updateGuidesState { it.copy(choirGuideError = null) }
            save(before.copy(draftRoll = roll ?: before.draftRoll, draftNote = note ?: before.draftNote,
                draftMethod = if (roll != null) "dado físico / resolución manual" else before.draftMethod))
        }
    }

    fun roll() = scope.launch {
        mutex.withLock {
            val spec = ChoirProcedureCatalog.current(memory()) ?: return@withLock
            if (spec.isCounter) return@withLock
            val sides = spec.die ?: return@withLock
            save(memory().copy(draftRoll = Random.nextInt(1, sides + 1).toString(), draftMethod = "tirada en la app (1d$sides)"))
        }
    }

    fun resolve(action: String, revision: Long) = scope.launch {
        mutex.withLock {
            val before = memory()
            if (before.revision != revision) return@withLock // stale/double tap
            updateGuidesState { it.copy(choirGuideError = null) }
            runCatching {
                save(ChoirGuideRules.resolve(before, action, before.draftRoll.toIntOrNull(), before.draftNote, id(), System.currentTimeMillis()).getOrThrow())
                flushUnlocked()
            }.onFailure(::failure)
        }
    }

    fun undo(revision: Long) = scope.launch {
        mutex.withLock {
            if (memory().revision != revision) return@withLock
            runCatching { save(ChoirGuideRules.undo(memory(), id(), System.currentTimeMillis())); flushUnlocked() }.onFailure(::failure)
        }
    }

    fun restart(procedure: ChoirProcedure, revision: Long) = scope.launch {
        mutex.withLock {
            if (memory().revision != revision) return@withLock
            runCatching { save(ChoirGuideRules.restart(memory(), procedure, id(), System.currentTimeMillis())); flushUnlocked() }.onFailure(::failure)
        }
    }

    fun openTable(title: String) {
        val table = findTable(ui.value, title)
        if (table == null) {
            updateGuidesState { it.copy(choirGuideError = "La tabla «$title» no está integrada. Consulta la referencia del manual; puedes resolver este paso manualmente.") }
        } else ui.update { it.copy(guides = it.guides.copy(choirGuideTableId = table.id), modals = it.modals.copy(showChoirGuide = false)) }
    }

    fun closeTable() { ui.update { it.copy(guides = it.guides.copy(choirGuideTableId = null), modals = it.modals.copy(showChoirGuide = true)) } }
    fun retryJournal() = scope.launch {
        mutex.withLock {
            updateGuidesState { it.copy(choirGuideError = null) }
            runCatching { flushUnlocked() }.onFailure(::failure)
        }
    }

    private suspend fun flushUnlocked() {
        if (ui.value.session == null) return // session observer retries once notes have loaded
        for (entry in memory().journalOutbox.toList()) {
            val marker = "<!-- choir-guide:${entry.id} -->"
            notesMutex.withLock {
                val persisted = sessions.getSessionById(sessionId).first() ?: error("La sesión ya no está disponible.")
                if (marker !in persisted.dmNotes.orEmpty()) {
                    val current = ui.value.dmNotes
                    val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(entry.timestamp))
                    val addition = "[$time] Choir · Procedimientos\n${entry.text}\n$marker"
                    val updated = if (marker in current) current else if (current.isBlank()) addition else "$current\n\n$addition"
                    ui.update { it.copy(dmNotes = updated, isSavingNotes = true) }
                    try { sessions.updateDmNotes(sessionId, updated) }
                    finally { ui.update { if (it.dmNotes == updated) it.copy(isSavingNotes = false) else it } }
                }
            }
            save(memory().copy(journalOutbox = memory().journalOutbox.filterNot { it.id == entry.id }))
        }
    }

    companion object {
        fun findTable(state: SessionUiState, title: String): RandomTable? =
            (state.tablesInSession + state.allTables).distinctBy { it.id }.firstOrNull {
                it.name.equals(title, true) && (it.sourcePack?.contains("Choir", true) == true || it.bundleName?.contains("Choir", true) == true)
            }
    }
}
