package com.deckapp.core.domain.usecase

import com.deckapp.core.model.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class ChoirGuideRulesTest {
    private var event = 0
    private fun resolve(m: ChoirGuideMemory, action: String, roll: Int? = null) =
        ChoirGuideRules.resolve(m, action, roll, "", "event-${++event}", 123L).getOrThrow()
    private fun exploring(step: String = "arrival", turns: Int = 0) = ChoirGuideMemory(progress = ChoirGuideProgress(
        active = ChoirProcedure.EXPLORATION, steps = mapOf(ChoirProcedure.EXPLORATION to step), turnsSpent = turns))
    private fun incursion(actions: Int = 15, step: String = "action") = ChoirGuideMemory(progress = ChoirGuideProgress(
        active = ChoirProcedure.INCURSION, steps = mapOf(ChoirProcedure.INCURSION to step), incursionActions = actions, incursionPrepared = true))

    @Test fun `arrival boundaries require encounter on 1 to 5 and no encounter on 6 to 10`() {
        for (v in 1..10) {
            val m = resolve(exploring(), "arrival_roll", v)
            assertEquals(if (v <= 5) "encounter" else "action", m.progress.steps[ChoirProcedure.EXPLORATION])
            assertEquals(0, m.progress.turnsSpent)
        }
        for (v in listOf(null, 0, 11)) assertTrue(ChoirGuideRules.resolve(exploring(), "arrival_roll", v, "", "e", 0).isFailure)
    }

    @Test fun `last travel turn still checks arrival before day review`() {
        var m = resolve(exploring("destination", 3), "known")
        assertEquals(4, m.progress.turnsSpent)
        assertEquals("arrival", ChoirProcedureCatalog.current(m)?.id)
        m = resolve(m, "arrival_roll", 1)
        assertEquals("encounter", ChoirProcedureCatalog.current(m)?.id)
        m = resolve(m, "encounter_done")
        assertEquals("day_end", ChoirProcedureCatalog.current(m)?.id)
        m = resolve(m, "day_done")
        assertEquals(2, m.progress.day)
        assertEquals(0, m.progress.turnsSpent)
    }

    @Test fun `fork generation does not consume another turn or become an arrival itself`() {
        var m = resolve(exploring("action"), "travel")
        m = resolve(m, "new_point", 6)
        assertEquals("fork", ChoirProcedureCatalog.current(m)?.id)
        assertEquals(0, m.progress.turnsSpent)
        m = resolve(m, "arrived")
        assertEquals(1, m.progress.turnsSpent)
        assertEquals("arrival", ChoirProcedureCatalog.current(m)?.id)
    }

    @Test fun `corruption replaces ordinary travel cost with three turns`() {
        var m = resolve(exploring("destination", 2), "new_point", 5)
        assertEquals(2, m.progress.turnsSpent)
        m = resolve(m, "corruption_done")
        assertEquals(2, m.progress.day)
        assertEquals(1, m.progress.turnsSpent)
        assertEquals("arrival", ChoirProcedureCatalog.current(m)?.id)
    }

    @Test fun `foraging records d4 plus one and spends a single turn`() {
        var m = resolve(exploring("forage"), "forage_success")
        assertEquals(0, m.progress.turnsSpent)
        m = resolve(m, "rations_done", 4)
        assertEquals(1, m.progress.turnsSpent)
        assertTrue(m.lastResolution.contains("5 raciones"))
    }

    @Test fun `preparation requires distinct hazards and five points before start`() {
        var m = ChoirGuideRules.select(ChoirGuideMemory(), ChoirProcedure.INCURSION_SETUP)
        m = resolve(m, "rumors_spent")
        m = resolve(m, "layout_done")
        m = resolve(m, "extraction", 3)
        m = resolve(m, "hazard", 3) // may share extraction
        assertTrue(ChoirGuideRules.resolve(m, "hazard", 3, "", "bad", 0).isFailure)
        m = resolve(m, "hazard", 8)
        repeat(5) { m = resolve(m, "poi") }
        assertEquals("ready", ChoirProcedureCatalog.current(m)?.id)
        m = resolve(m, "begin_incursion")
        assertEquals(ChoirProcedure.INCURSION, m.progress.active)
        assertEquals(15, m.progress.incursionActions)
        assertTrue(m.progress.incursionPrepared)
        m = resolve(m, "move")
        m = ChoirGuideRules.select(m, ChoirProcedure.INCURSION_SETUP)
        assertTrue(ChoirGuideRules.resolve(m, "begin_incursion", null, "", "again", 0).isFailure)
        m = resolve(m, "continue_incursion")
        assertEquals(14, m.progress.incursionActions)
        assertEquals("resolve_move", ChoirProcedureCatalog.current(m)?.id)
    }

    @Test fun `incursion combat is charged once and not again on completion`() {
        var m = resolve(incursion(), "fight")
        assertEquals(14, m.progress.incursionActions)
        assertTrue(ChoirGuideRules.resolve(m, "fight", null, "", "dup", 0).isFailure)
        m = resolve(m, "action_done")
        assertEquals(14, m.progress.incursionActions)
        m = resolve(m, "scavenge")
        m = resolve(m, "action_done")
        m = resolve(m, "loot")
        assertEquals(12, m.progress.incursionActions)
    }

    @Test fun `counter input allows zero and cannot spend more actions than remain`() {
        assertTrue(ChoirGuideRules.resolve(incursion(0), "move", null, "", "bad", 0).isFailure)
        assertTrue(ChoirGuideRules.resolve(incursion(1, "opportunity"), "opportunity_cost", 2, "", "bad", 0).isFailure)
        assertEquals(1, resolve(incursion(1, "opportunity"), "opportunity_cost", 0).progress.incursionActions)
        assertEquals(0, resolve(incursion(step = "resume"), "resume_actions", 0).progress.incursionActions)
        assertTrue(ChoirProcedureCatalog.current(incursion(step = "resume"))!!.isCounter)
    }

    @Test fun `anguish equal threshold causes breakdown and resumes exact prior step`() {
        val base = incursion(9, "resolve_fight").copy(draftRoll = "11", draftNote = "Mi decisión", draftMethod = "dado físico")
        var m = ChoirGuideRules.interrupt(base, ChoirAnguishCheck("a", anguish = 7))
        assertEquals("", m.draftRoll)
        m = resolve(m, "resolve_anguish", 7)
        assertEquals("breakdown", ChoirProcedureCatalog.current(m)?.id)
        assertEquals(base.progress, m.progress)
        m = resolve(m, "breakdown_done")
        assertEquals(base.progress, m.progress)
        assertEquals("resolve_fight", ChoirProcedureCatalog.current(m)?.id)
        assertTrue(m.anguishChecks.isEmpty())
        assertEquals(base.draftRoll, m.draftRoll)
        assertEquals(base.draftNote, m.draftNote)
    }

    @Test fun `anguish above threshold passes without losing other pending checks`() {
        var m = ChoirGuideRules.interrupt(exploring(), ChoirAnguishCheck("a", anguish = 5))
        m = ChoirGuideRules.interrupt(m, ChoirAnguishCheck("b", anguish = 9))
        m = resolve(m, "resolve_anguish", 6)
        assertEquals(listOf("b"), m.anguishChecks.map { it.id })
        assertEquals("arrival", m.progress.steps[ChoirProcedure.EXPLORATION] ?: "arrival")
    }

    @Test fun `observing selected character only queues increases once and never existing initial anguish`() {
        var m = ChoirGuideRules.selectCharacter(ChoirGuideMemory(), 42, 8)
        m = ChoirGuideRules.observeAnguish(m, 42, "Ada", 8, "a")
        assertTrue(m.anguishChecks.isEmpty())
        m = ChoirGuideRules.observeAnguish(m, 43, "Other", 12, "other")
        assertTrue(m.anguishChecks.isEmpty())
        m = ChoirGuideRules.observeAnguish(m, 42, "Ada", 9, "b")
        m = ChoirGuideRules.observeAnguish(m, 42, "Ada", 9, "c")
        assertEquals(1, m.anguishChecks.size)
        m = ChoirGuideRules.observeAnguish(m, 42, "Ada", 4, "d")
        assertEquals(1, m.anguishChecks.size)
    }

    @Test fun `correcting a guide step restores counters but records a correction`() {
        val base = incursion(3)
        val changed = resolve(base, "move")
        val restored = ChoirGuideRules.undo(changed, "undo", 100)
        assertEquals(base.progress, restored.progress)
        assertEquals(changed.revision + 1, restored.revision)
        assertEquals(2, restored.journalOutbox.size)
        assertTrue(restored.lastResolution.contains("manualmente no se revierten"))
    }

    @Test fun `procedure state survives serialization and old board memories gain defaults`() {
        val m = ChoirGuideRules.interrupt(incursion(5), ChoirAnguishCheck("pending", anguish = 4))
            .copy(draftRoll = "12", draftNote = "Destino", journalOutbox = listOf(ChoirGuideJournalEntry("e", "Texto", 1)))
        val memory = BoardSessionMemory(choirGuide = m, focusedSceneId = 123)
        val json = Json { ignoreUnknownKeys = true }
        assertEquals(memory, json.decodeFromString<BoardSessionMemory>(json.encodeToString(memory)))
        assertEquals(ChoirGuideMemory(), json.decodeFromString<BoardSessionMemory>("{\"focusedSceneId\":123}").choirGuide)
    }

    @Test fun `every supported step has a catalog definition and source page`() {
        val steps = mapOf(
            ChoirProcedure.EXPLORATION to listOf("arrival", "encounter", "action", "destination", "landmark", "natural", "fork", "corruption", "forage", "rations", "rest", "rumors", "day_end"),
            ChoirProcedure.INCURSION_SETUP to listOf("rumors", "layout", "extraction", "hazard_one", "hazard_two", "poi", "ready"),
            ChoirProcedure.INCURSION to listOf("action", "resume", "opportunity", "resolve_move", "resolve_fight", "resolve_scavenge", "resolve_loot", "resolve_opportunity", "extraction", "finished")
        )
        steps.forEach { (procedure, ids) -> ids.forEach { id ->
            val spec = ChoirProcedureCatalog.current(ChoirGuideMemory(progress = ChoirGuideProgress(active = procedure, steps = mapOf(procedure to id))))
            assertNotNull("$procedure/$id", spec)
            assertTrue(spec!!.sheet in 1..6)
        } }
    }
}
