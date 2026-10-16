package com.deckapp.feature.draw.clocks

import com.deckapp.core.model.TensionClock
import com.deckapp.feature.draw.components.blocks.getCleanDescription
import com.deckapp.feature.draw.components.blocks.parseMilestones
import com.deckapp.feature.draw.components.blocks.toggleMilestone
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class SessionClocksTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Test
    fun `TensionClock serializes and deserializes correctly for Room DB and Backups`() {
        val clock = TensionClock(
            id = 12345L,
            sessionId = 1L,
            name = "Alarma de la Ciudadela",
            segments = 6,
            progress = 3,
            colorHex = "#E53935",
            triggerDescription = "Los guardias tocan los cuernos y cierran los portones.",
            isCompleted = false
        )

        val encoded = json.encodeToString(listOf(clock))
        val decoded = json.decodeFromString<List<TensionClock>>(encoded)

        assertEquals(1, decoded.size)
        val loaded = decoded.first()
        assertEquals(12345L, loaded.id)
        assertEquals("Alarma de la Ciudadela", loaded.name)
        assertEquals(6, loaded.segments)
        assertEquals(3, loaded.progress)
        assertEquals("#E53935", loaded.colorHex)
        assertEquals("Los guardias tocan los cuernos y cierran los portones.", loaded.triggerDescription)
        assertFalse(loaded.isCompleted)
    }

    @Test
    fun `Ticking advances progress and clamps properly to segments`() {
        val clock = TensionClock(id = 1L, segments = 4, progress = 2)

        // Advance +1
        val advanced = clock.copy(progress = (clock.progress + 1).coerceIn(0, clock.segments))
        assertEquals(3, advanced.progress)

        // Advance beyond max -> should clamp at 4
        val maxed = advanced.copy(
            progress = (advanced.progress + 5).coerceIn(0, advanced.segments),
            isCompleted = (advanced.progress + 5) >= advanced.segments
        )
        assertEquals(4, maxed.progress)
        assertTrue(maxed.isCompleted)

        // Decrement below 0 -> should clamp at 0
        val decremented = clock.copy(progress = (clock.progress - 10).coerceIn(0, clock.segments))
        assertEquals(0, decremented.progress)
    }

    @Test
    fun `Resetting clock restores progress to 0 and clears completed state`() {
        val completedClock = TensionClock(
            id = 2L,
            segments = 6,
            progress = 6,
            isCompleted = true
        )

        val reset = completedClock.copy(progress = 0, isCompleted = false)
        assertEquals(0, reset.progress)
        assertFalse(reset.isCompleted)
    }

    @Test
    fun `Instant detonation sets clock to 100 percent and completed`() {
        val clock = TensionClock(id = 3L, segments = 8, progress = 1, isCompleted = false)

        val detonated = clock.copy(progress = clock.segments, isCompleted = true)
        assertEquals(8, detonated.progress)
        assertTrue(detonated.isCompleted)
    }

    @Test
    fun `Milestone parsing extracts markdown checklist items and clean description`() {
        val clock = TensionClock(
            name = "Infiltración",
            triggerDescription = """
                Alarma general si el sigilo se rompe.
                - [ ] Evitar patrulla del patio
                - [x] Hackear terminal de seguridad
                - [ ] Entrar al santuario interior
            """.trimIndent()
        )

        val milestones = clock.parseMilestones()
        assertEquals(3, milestones.size)
        assertEquals("Evitar patrulla del patio", milestones[0].text)
        assertFalse(milestones[0].isCompleted)
        assertEquals("Hackear terminal de seguridad", milestones[1].text)
        assertTrue(milestones[1].isCompleted)
        assertEquals("Entrar al santuario interior", milestones[2].text)
        assertFalse(milestones[2].isCompleted)

        val clean = clock.getCleanDescription()
        assertEquals("Alarma general si el sigilo se rompe.", clean)
    }

    @Test
    fun `Toggling milestone flips markdown check state accurately`() {
        val clock = TensionClock(
            triggerDescription = "- [ ] Forzar cerradura"
        )

        val toggled = clock.toggleMilestone(0)
        assertTrue(toggled.contains("- [x] Forzar cerradura"))
    }
}
