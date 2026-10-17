package com.deckapp.feature.draw.delegates

import android.content.ContentResolver
import android.net.Uri
import com.deckapp.core.domain.repository.EncounterRepository
import com.deckapp.core.domain.repository.NpcRepository
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

/**
 * Delegado encargado de toda la gestión de Combate, Encuentros, Iniciativa,
 * Tracker de criaturas y reglas del Coro de Carne (Choir of Flesh) en la sesión.
 * Extraído de SessionViewModel para aislar el subsistema táctico y de combate.
 */
class SessionCombatDelegate(
    private val sessionId: Long,
    private val _uiState: MutableStateFlow<SessionUiState>,
    private val scope: CoroutineScope,
    private val encounterRepository: EncounterRepository,
    private val sessionRepository: SessionRepository,
    private val npcRepository: NpcRepository,
    private val applyDamageUseCase: ApplyDamageUseCase,
    private val rollInitiativeUseCase: RollInitiativeUseCase,
    private val calculateInitiativeOrderUseCase: CalculateInitiativeOrderUseCase,
    private val toggleConditionUseCase: ToggleConditionUseCase,
    private val cleanupCombatUseCase: CleanupCombatUseCase,
    private val updateChoirCharacterUseCase: UpdateChoirCharacterUseCase,
    private val updateChoirCombatStateUseCase: UpdateChoirCombatStateUseCase,
    private val notesMutex: Mutex,
    private val onUpdateNotes: (String) -> Unit
) {

    fun updateChoirCharacter(creatureId: Long, command: ChoirCharacterCommand) {
        scope.launch { updateChoirCharacterUseCase(creatureId, command) }
    }

    fun applyChoirCombatAftermath(
        creatureId: Long,
        request: ChoirAftermathRequest,
        onComplete: (ChoirAftermathResult?) -> Unit = {}
    ) {
        scope.launch {
            val result = updateChoirCharacterUseCase.applyAftermath(creatureId, request)
            if (result != null) {
                val updatedNotes = "${_uiState.value.dmNotes}\n\n${result.summaryReport}"
                onUpdateNotes(updatedNotes)
            }
            onComplete(result)
        }
    }

    fun getChoirIncursion(): ChoirIncursion {
        return ChoirIncursionCodec.readIncursion(_uiState.value.dmNotes) ?: ChoirIncursion()
    }

    fun getChoirSettlement(): ChoirSettlement {
        return ChoirIncursionCodec.readSettlement(_uiState.value.dmNotes) ?: ChoirSettlement()
    }

    fun saveChoirIncursion(
        incursion: ChoirIncursion,
        settlement: ChoirSettlement,
        character: ChoirCharacter? = null,
        characterCreatureId: Long? = null
    ) {
        val currentNotes = _uiState.value.dmNotes
        val withIncursion = ChoirIncursionCodec.writeIncursion(currentNotes, incursion)
        val withBoth = ChoirIncursionCodec.writeSettlement(withIncursion, settlement)
        onUpdateNotes(withBoth)
        if (character != null && characterCreatureId != null) {
            scope.launch {
                if (!updateChoirCharacterUseCase.saveSheet(characterCreatureId, character)) {
                    _uiState.update { it.copy(snackbarMessage = "No se pudo guardar el descanso del Inquebrantable") }
                }
            }
        }
    }

    fun updateChoirCombatState(creatureId: Long, command: ChoirCombatCommand) {
        scope.launch {
            updateChoirCombatStateUseCase(creatureId, command)
        }
    }

    fun applyDamage(creatureId: Long, delta: Int) {
        scope.launch {
            applyDamageUseCase(creatureId, delta)
        }
    }

    fun toggleCombatHudExpanded() {
        _uiState.update { it.copy(isCombatHudExpanded = !it.isCombatHudExpanded) }
    }

    fun copyCombatRoundToDiary() {
        val encounter = _uiState.value.activeEncounter ?: return
        scope.launch {
            val sorted = encounter.creatures.sortedBy { it.sortOrder }
            val sb = java.lang.StringBuilder()
            sb.append("\n\n### ⚔️ Asalto ${encounter.currentRound} de Combate: ${encounter.name}\n")
            sorted.forEach { c ->
                val status = if (c.currentHp <= 0) " (Caído/Inconsciente)" else ""
                val conditions = if (c.conditions.isNotEmpty()) " [${c.conditions.joinToString { cond -> cond.label }}]" else ""
                val choir = ChoirProfile.fromNotes(c.notes)
                if (choir != null) {
                    val wounds = ChoirCombatState.fromNotes(c.notes)
                    sb.append("- **${c.name}**: ${wounds.major} heridas mayores, ${wounds.minor} menores$conditions\n")
                } else if (ChoirCharacterCodec.read(c.notes) != null) {
                    val sheet = ChoirCharacterCodec.read(c.notes)!!
                    sb.append("- **${c.name}**: ${sheet.major}/4 mayores, ${sheet.minor}/8 menores; Vigor ${sheet.vigor}; Trauma ${sheet.trauma}$conditions\n")
                } else {
                    sb.append("- **${c.name}**: ${c.currentHp}/${c.maxHp} HP$conditions$status\n")
                }
            }
            val currentNotes = _uiState.value.dmNotes
            val newNotes = (currentNotes + sb.toString()).trim()
            _uiState.update { it.copy(dmNotes = newNotes, snackbarMessage = "Asalto copiado al diario") }
            sessionRepository.updateDmNotes(sessionId, newNotes)
        }
    }

    fun nextTurn() {
        val encounter = _uiState.value.activeEncounter ?: return
        val players = _uiState.value.playerParticipants

        scope.launch {
            val totalParticipants = encounter.creatures.size + players.size
            if (totalParticipants == 0) return@launch

            var nextIndex = encounter.currentTurnIndex + 1
            var nextRound = encounter.currentRound

            if (nextIndex >= totalParticipants) {
                nextIndex = 0
                nextRound++
            }

            encounterRepository.saveEncounter(
                encounter.copy(
                    currentTurnIndex = nextIndex,
                    currentRound = nextRound
                )
            )

            // Registrar log
            if (nextRound > encounter.currentRound) {
                encounterRepository.recordLog(
                    CombatLogEntry(
                        encounterId = encounter.id,
                        message = "Inicio de la Ronda $nextRound",
                        type = CombatLogType.ROUND_START
                    )
                )
            }
        }
    }

    /** Inicia el proceso de combate: tira iniciativa para los que faltan y establece el orden. */
    fun startCombatProcess() {
        val encounter = _uiState.value.activeEncounter ?: return
        scope.launch {
            // 1. Tirar dados para criaturas sin iniciativa (NPCs)
            rollInitiativeUseCase(encounter.id)
            // 2. Establecer el orden determinista (sortOrder)
            calculateInitiativeOrderUseCase(encounter.id)
        }
    }

    /** Cambia el estado de una condición con registro narrativo automático. */
    fun toggleCondition(creatureId: Long, condition: Condition) {
        scope.launch {
            toggleConditionUseCase(creatureId, condition)
        }
    }

    /** Elimina una criatura y recalcula el orden del tracker. */
    fun removeCreature(creatureId: Long) {
        val encounter = _uiState.value.activeEncounter ?: return
        scope.launch {
            cleanupCombatUseCase.removeCreature(encounter.id, creatureId)
        }
    }

    fun addPlayerParticipant(name: String, initiative: Int) {
        _uiState.update {
            it.copy(
                playerParticipants = it.playerParticipants + PlayerInitiativeParticipant(
                    name = name,
                    initiativeTotal = initiative
                )
            )
        }
    }

    fun removePlayerParticipant(id: String) {
        _uiState.update {
            it.copy(
                playerParticipants = it.playerParticipants.filter { p -> p.id != id }
            )
        }
    }

    fun endEncounter() {
        val encounter = _uiState.value.activeEncounter ?: return
        scope.launch {
            val survivors = encounter.creatures
                .filter { it.currentHp > 0 }
                .joinToString(", ") { it.name }
            val summary = """

                --- RESUMEN DE COMBATE: ${encounter.name} ---
                Rondas: ${encounter.currentRound}
                Supervivientes: ${if (survivors.isEmpty()) "Ninguno" else survivors}
                ------------------------------------------
            """.trimIndent()

            val updatedNotes = "${_uiState.value.dmNotes}\n$summary"
            onUpdateNotes(updatedNotes)

            encounterRepository.saveEncounter(encounter.copy(isActive = false))

            if (_uiState.value.activeTab == 5) {
                _uiState.update {
                    it.copy(
                        activeTab = 0,
                        snackbarMessage = "Combate finalizado y resumido en notas",
                        playerParticipants = emptyList()
                    )
                }
            }
        }
    }

    suspend fun getSessionLogText(): String {
        val events = sessionRepository.getEventsForSession(sessionId).first()
        val sessionName = _uiState.value.session?.name ?: "Sesión"
        val sb = StringBuilder()
        sb.append("LOG DE SESIÓN: $sessionName\n")
        sb.append("Generado: ${java.text.SimpleDateFormat("dd/MM/yyyy HH:mm").format(java.util.Date())}\n")
        sb.append("--------------------------------------------------\n\n")

        sb.append("--- ACTIVIDAD DE CARTAS ---\n")
        events.forEach { event ->
            val time = java.text.SimpleDateFormat("HH:mm:ss").format(java.util.Date(event.timestamp))
            sb.append("[$time] ${event.action}: Carta ${event.cardId} ${event.metadata}\n")
        }

        val encounters = encounterRepository.getAllEncounters().first()
            .filter { it.linkedSessionId == sessionId }

        if (encounters.isNotEmpty()) {
            sb.append("\n\n--- HISTORIAL DE COMBATES ---\n")
            encounters.forEach { enc ->
                sb.append("\nENCUENTRO: ${enc.name} (Rondas: ${enc.currentRound})\n")
                val logEntries = encounterRepository.getLogForEncounter(enc.id).first()
                logEntries.reversed().forEach { entry ->
                    val time = java.text.SimpleDateFormat("HH:mm:ss").format(java.util.Date(entry.timestamp))
                    sb.append("[$time] ${entry.message}\n")
                }
            }
        }

        return sb.toString()
    }

    fun exportLog(uri: Uri, contentResolver: ContentResolver) {
        scope.launch {
            try {
                val text = getSessionLogText()
                contentResolver.openOutputStream(uri)?.use { output ->
                    output.write(text.toByteArray())
                }
                _uiState.update { it.copy(snackbarMessage = "Log exportado correctamente") }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Error al exportar log: ${e.message}") }
            }
        }
    }

    fun updateNpcHp(npcId: Long, hp: Int) {
        scope.launch {
            val npc = npcRepository.getNpcById(npcId) ?: return@launch
            npcRepository.saveNpc(npc.copy(currentHp = hp))
        }
    }

    fun createChoirOfFleshCharacter(npc: Npc, sheetMarkdown: String) {
        scope.launch {
            try {
                npcRepository.saveNpc(npc)
                notesMutex.withLock {
                    val currentNotes = _uiState.value.dmNotes
                    val updatedNotes = if (currentNotes.isBlank()) sheetMarkdown else "$currentNotes\n\n$sheetMarkdown"
                    _uiState.update { it.copy(dmNotes = updatedNotes) }
                    sessionRepository.updateDmNotes(sessionId, updatedNotes)
                }
                _uiState.update { it.copy(snackbarMessage = "Inquebrantable forjado: ${npc.name}") }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _uiState.update { it.copy(snackbarMessage = "Error al forjar Inquebrantable: ${e.message}") }
            }
        }
    }

    fun addNpcToCombat(npc: Npc) {
        scope.launch {
            val currentEncounter = _uiState.value.activeEncounter
            val targetEncounterId = currentEncounter?.id ?: encounterRepository.saveEncounter(
                com.deckapp.core.model.Encounter(
                    name = "Combate Rápido",
                    linkedSessionId = sessionId,
                    isActive = true,
                    currentRound = 1
                )
            )

            val maxSort = currentEncounter?.creatures?.maxOfOrNull { it.sortOrder } ?: 0
            val creature = com.deckapp.core.model.EncounterCreature(
                encounterId = targetEncounterId,
                name = npc.name,
                maxHp = npc.maxHp,
                currentHp = npc.currentHp.coerceAtLeast(1),
                armorClass = npc.armorClass,
                notes = npc.notes,
                npcId = npc.id,
                imagePath = npc.imagePath,
                initiativeBonus = npc.initiativeBonus,
                sortOrder = maxSort + 1
            )
            encounterRepository.addCreature(creature)
            _uiState.update { it.copy(snackbarMessage = "${npc.name} sumado al combate") }
        }
    }

    fun logNpcToDiary(npc: Npc) {
        scope.launch {
            notesMutex.withLock {
                val current = _uiState.value.dmNotes
                val entry = "### 👤 PNJ: ${npc.name}\n${npc.description}\n${npc.notes}".trim()
                val updated = if (current.isBlank()) entry else "$current\n\n$entry"
                _uiState.update { it.copy(dmNotes = updated, snackbarMessage = "${npc.name} registrado en el diario") }
                sessionRepository.updateDmNotes(sessionId, updated)
            }
        }
    }
}
