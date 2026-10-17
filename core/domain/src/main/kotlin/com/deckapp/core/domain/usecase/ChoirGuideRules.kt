package com.deckapp.core.domain.usecase

import com.deckapp.core.model.*

/** State machine for guidance. Manual inventory/character edits are never fabricated. */
object ChoirGuideRules {
    fun select(memory: ChoirGuideMemory, procedure: ChoirProcedure): ChoirGuideMemory {
        require(procedure != ChoirProcedure.ANGUISH) { "Inicia la prueba indicando la Angustia actual." }
        require(memory.anguishChecks.isEmpty()) { "Resuelve primero la prueba de Angustia pendiente; puedes consultar las hojas mientras tanto." }
        val p = memory.progress
        return memory.copy(progress = p.copy(active = procedure,
            steps = p.steps + (procedure to (p.steps[procedure] ?: ChoirProcedureCatalog.initial(procedure)))),
            revision = memory.revision + 1, draftRoll = "", draftNote = "")
    }

    fun selectCharacter(memory: ChoirGuideMemory, id: Long?, anguish: Int?): ChoirGuideMemory = memory.copy(
        selectedCharacterId = id,
        observedAnguish = if (id != null && anguish != null) memory.observedAnguish + (id to anguish.coerceAtLeast(0)) else memory.observedAnguish
    )

    fun observeAnguish(memory: ChoirGuideMemory, id: Long, name: String, value: Int, eventId: String): ChoirGuideMemory {
        if (memory.selectedCharacterId != id) return memory
        val previous = memory.observedAnguish[id]
        if (previous == value) return memory
        val checks = if (previous != null && value > previous)
            memory.anguishChecks + ChoirAnguishCheck(eventId, id, name, value)
        else memory.anguishChecks
        return suspendDraft(memory, memory.copy(observedAnguish = memory.observedAnguish + (id to value), anguishChecks = checks,
            revision = if (checks != memory.anguishChecks) memory.revision + 1 else memory.revision))
    }

    fun interrupt(memory: ChoirGuideMemory, check: ChoirAnguishCheck): ChoirGuideMemory {
        require(check.anguish >= 0) { "La Angustia no puede ser negativa." }
        return suspendDraft(memory, memory.copy(anguishChecks = memory.anguishChecks + check, revision = memory.revision + 1))
    }

    private fun suspendDraft(before: ChoirGuideMemory, after: ChoirGuideMemory): ChoirGuideMemory =
        if (before.anguishChecks.isEmpty() && after.anguishChecks.isNotEmpty()) after.copy(
            suspendedDraft = ChoirGuideDraft(before.draftRoll, before.draftNote, before.draftMethod),
            draftRoll = "", draftNote = "", draftMethod = "dado físico / resolución manual"
        ) else after

    fun resolve(memory: ChoirGuideMemory, action: String, roll: Int?, note: String,
                eventId: String, timestamp: Long): Result<ChoirGuideMemory> = runCatching {
        val spec = requireNotNull(ChoirProcedureCatalog.current(memory)) { "Elige un procedimiento." }
        require(spec.actions.any { it.id == action }) { "La acción no pertenece al paso actual." }
        fun number(max: Int, min: Int = 1): Int = requireNotNull(roll) { "Introduce el resultado antes de confirmar." }
            .also { require(it in min..max) { "El valor debe estar entre $min y $max." } }

        val pending = memory.anguishChecks.firstOrNull()
        if (pending != null) {
            val threshold = pending.characterId?.let { memory.observedAnguish[it] } ?: pending.anguish
            val text: String
            val checks: List<ChoirAnguishCheck>
            if (action == "resolve_anguish") {
                val value = number(20)
                val collapse = value <= threshold
                checks = if (collapse) listOf(pending.copy(anguish = threshold, roll = value)) + memory.anguishChecks.drop(1)
                    else memory.anguishChecks.drop(1)
                text = "${pending.characterName}: prueba de Angustia $value frente a $threshold; ${if (collapse) "Colapso pendiente" else "sin Colapso"}."
            } else {
                val resolvedRoll = pending.roll
                require(resolvedRoll != null && resolvedRoll <= pending.anguish)
                checks = memory.anguishChecks.drop(1)
                text = "${pending.characterName}: Colapso resuelto manualmente. Retomar procedimiento anterior."
            }
            val resolved = journal(memory.copy(anguishChecks = checks), text, note, eventId, timestamp)
            val draft = memory.suspendedDraft
            return@runCatching if (checks.isEmpty() && draft != null) resolved.copy(
                draftRoll = draft.roll, draftNote = draft.note, draftMethod = draft.method, suspendedDraft = null
            ) else resolved
        }

        val before = memory.progress
        var p = before
        val active = requireNotNull(p.active)
        var message = spec.actions.first { it.id == action }.label
        fun go(step: String) { p = p.copy(steps = p.steps + (active to step)) }
        fun completeTurn(next: String, count: Int = 1) {
            val total = p.turnsSpent + count
            // More than a day's travel requires explicit needs review in that procedure.
            p = if (total > 4) p.copy(day = p.day + total / 4, turnsSpent = total % 4)
                else p.copy(turnsSpent = total)
            go(if (p.turnsSpent == 4) "day_end" else next)
        }
        fun spend(cost: Int, next: String) {
            require(cost >= 0 && cost <= p.incursionActions) { "No alcanzan las acciones de incursión ($cost requeridas; ${p.incursionActions} restantes)." }
            p = p.copy(incursionActions = p.incursionActions - cost)
            go(next)
            message += "; coste $cost, quedan ${p.incursionActions}/15 acciones."
        }
        when (active) {
            ChoirProcedure.EXPLORATION -> when (action) {
                "arrival_roll" -> { val value = number(10); go(if (value <= 5) "encounter" else if (p.turnsSpent == 4) "day_end" else "action"); message = "Llegada: d10 $value; ${if (value <= 5) "encuentro pendiente" else "sin encuentro"}." }
                "at_action", "encounter_done" -> go(if (p.turnsSpent == 4) "day_end" else "action")
                "travel" -> go("destination")
                "forage" -> go("forage")
                "rest" -> go("rest")
                "rumors" -> go("rumors")
                "known", "arrived" -> { completeTurn("arrival"); go("arrival") }
                "new_point" -> { val value = number(6); go(when (value) { in 1..3 -> "landmark"; 4 -> "natural"; 5 -> "corruption"; else -> "fork" }); message = "Nuevo destino: d6 $value. ${ChoirProcedureCatalog.current(memory.copy(progress = p))?.title.orEmpty()}" }
                "corruption" -> go("corruption")
                "corruption_done" -> { completeTurn("arrival", 3); go("arrival") }
                "forage_success" -> go("rations")
                "forage_fail", "rest_done", "rumors_success", "rumors_fail" -> completeTurn("action")
                "rations_done" -> { val value = number(4); message = "Forrajeo: d4 $value + 1 = ${value + 1} raciones anotadas manualmente en la ficha."; completeTurn("action") }
                "day_done" -> { p = p.copy(day = p.day + 1, turnsSpent = 0); go("action") }
                else -> error("Paso de exploración no válido.")
            }
            ChoirProcedure.INCURSION_SETUP -> when (action) {
                "rumors_spent" -> go("layout")
                "layout_done" -> go("extraction")
                "extraction" -> { p = p.copy(extractionNode = number(8)); go("hazard_one"); message = "Extracción marcada en zona ${p.extractionNode}." }
                "hazard" -> { val value = number(8); require(value !in p.hazardousNodes) { "Las dos áreas peligrosas deben ser distintas. Vuelve a tirar o introduce otra zona." }; p = p.copy(hazardousNodes = p.hazardousNodes + value); go(if (p.hazardousNodes.size == 1) "hazard_two" else "poi"); message = "Terreno peligroso marcado en zona $value." }
                "poi" -> { require(p.pointsPrepared < 5); p = p.copy(pointsPrepared = p.pointsPrepared + 1); go(if (p.pointsPrepared == 5) "ready" else "poi"); message = "Punto de interés ${p.pointsPrepared}/5 colocado y resuelto manualmente." }
                "begin_incursion" -> { require(!p.incursionPrepared && p.extractionNode != null && p.hazardousNodes.distinct().size == 2 && p.pointsPrepared == 5); p = p.copy(active = ChoirProcedure.INCURSION, incursionActions = 15, incursionPrepared = true, steps = p.steps + (ChoirProcedure.INCURSION to "action")); message = "Incursión iniciada con 15 acciones. Extracción: ${p.extractionNode}; peligros: ${p.hazardousNodes.joinToString()}." }
                "continue_incursion" -> { require(p.incursionPrepared); p = p.copy(active = ChoirProcedure.INCURSION); message = "Incursión retomada sin reiniciar sus ${p.incursionActions} acciones restantes." }
                else -> error("Paso de preparación no válido.")
            }
            ChoirProcedure.INCURSION -> when (action) {
                "prepare" -> { p = p.copy(active = ChoirProcedure.INCURSION_SETUP, steps = p.steps + (ChoirProcedure.INCURSION_SETUP to "rumors"), incursionPrepared = false, extractionNode = null, hazardousNodes = emptyList(), pointsPrepared = 0) }
                "resume" -> go("resume")
                "resume_actions" -> { p = p.copy(incursionActions = number(15, 0), incursionPrepared = true); go("action"); message = "Incursión retomada con ${p.incursionActions}/15 acciones indicadas manualmente." }
                "move" -> spend(1, "resolve_move")
                "fight" -> spend(1, "resolve_fight")
                "scavenge" -> spend(1, "resolve_scavenge")
                "loot" -> spend(1, "resolve_loot")
                "opportunity" -> go("opportunity")
                "opportunity_cost" -> spend(number(15, 0), "resolve_opportunity")
                "action_done" -> go("action")
                "extract" -> go("extraction")
                "extracted" -> go("finished")
                else -> error("Paso de incursión no válido.")
            }
            else -> error("Este procedimiento está disponible como consulta.")
        }
        journal(memory.copy(progress = p, history = (memory.history + before).takeLast(30)),
            "${ChoirProcedureCatalog.info(active).title}: $message", note, eventId, timestamp)
    }

    fun undo(memory: ChoirGuideMemory, eventId: String, timestamp: Long): ChoirGuideMemory {
        val previous = memory.history.lastOrNull() ?: return memory
        require(memory.anguishChecks.isEmpty()) { "Resuelve primero la prueba de Angustia pendiente." }
        return journal(memory.copy(progress = previous, history = memory.history.dropLast(1)),
            "Corrección: se restauró el paso y los contadores anteriores de la guía. Las fichas e inventarios editados manualmente no se revierten.", "", eventId, timestamp)
    }

    fun restart(memory: ChoirGuideMemory, id: ChoirProcedure, eventId: String, timestamp: Long): ChoirGuideMemory {
        val selected = select(memory, id)
        var p = selected.progress.copy(steps = selected.progress.steps + (id to ChoirProcedureCatalog.initial(id)))
        if (id == ChoirProcedure.INCURSION_SETUP) p = p.copy(incursionPrepared = false, extractionNode = null, hazardousNodes = emptyList(), pointsPrepared = 0)
        return journal(selected.copy(progress = p, history = (memory.history + memory.progress).takeLast(30)),
            "Reiniciada la guía: ${ChoirProcedureCatalog.info(id).title}. No se alteraron fichas ni inventarios.", "", eventId, timestamp)
    }

    private fun journal(memory: ChoirGuideMemory, text: String, note: String, id: String, time: Long): ChoirGuideMemory {
        val entry = text + "\nOrigen: ${memory.draftMethod}." + if (note.isNotBlank()) "\n${note.trim()}" else ""
        return memory.copy(revision = memory.revision + 1, draftRoll = "", draftNote = "", draftMethod = "dado físico / resolución manual",
            lastResolution = entry, journalOutbox = memory.journalOutbox + ChoirGuideJournalEntry(id, entry, time))
    }
}
