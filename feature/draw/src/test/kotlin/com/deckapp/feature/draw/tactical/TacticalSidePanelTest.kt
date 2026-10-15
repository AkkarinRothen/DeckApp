package com.deckapp.feature.draw.tactical

import com.deckapp.core.domain.repository.EncounterRepository
import com.deckapp.core.domain.repository.TacticalMapRepository
import com.deckapp.core.domain.usecase.CalculateTacticalDistanceUseCase
import com.deckapp.core.model.*
import com.deckapp.feature.draw.SessionUiState
import com.deckapp.feature.draw.delegates.TacticalMapDelegate
import io.mockk.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class TacticalSidePanelTest {

    private class Fixture(scope: CoroutineScope) {
        val maps = MutableStateFlow(listOf(TacticalMap(id = 1, name = "Arena", imageUri = "", sessionId = 10, scaleFtPerCell = 5)))
        val creature1 = EncounterCreature(id = 101L, encounterId = 50L, name = "Paladín", maxHp = 40, currentHp = 40, armorClass = 18, sortOrder = 0)
        val creature2 = EncounterCreature(id = 102L, encounterId = 50L, name = "Goblin", maxHp = 10, currentHp = 10, armorClass = 12, sortOrder = 1)
        val encounter = MutableStateFlow<Encounter?>(
            Encounter(id = 50L, name = "Combate Cueva", creatures = listOf(creature1, creature2), currentRound = 1, currentTurnIndex = 0)
        )

        val token1 = TacticalToken(id = 1L, mapId = 1, name = "Paladín", cellX = 2f, cellY = 2f, currentHp = 40, maxHp = 40, armorClass = 18, encounterCreatureId = 101L)
        val token2 = TacticalToken(id = 2L, mapId = 1, name = "Goblin", cellX = 5f, cellY = 2f, currentHp = 10, maxHp = 10, armorClass = 12, encounterCreatureId = 102L)
        val tokens = MutableStateFlow(listOf(token1, token2))

        val state = MutableStateFlow(SessionUiState(sessionId = 10))
        val repo = mockk<TacticalMapRepository>(relaxed = true)
        val combat = mockk<EncounterRepository>(relaxed = true)
        val distanceCalculator = CalculateTacticalDistanceUseCase()
        val delegate: TacticalMapDelegate

        init {
            every { repo.getTacticalMapsBySessionId(10) } returns maps
            every { repo.getTacticalMapById(any()) } answers {
                val id = firstArg<Long>(); maps.map { list -> list.find { it.id == id } }
            }
            every { repo.getTokensByMapId(any()) } answers {
                val id = firstArg<Long>(); tokens.map { list -> list.filter { it.mapId == id } }
            }
            every { repo.getTokenById(any()) } answers {
                val id = firstArg<Long>(); tokens.map { list -> list.find { it.id == id } }
            }
            every { repo.getPinsByMapId(any()) } returns flowOf(emptyList())
            every { combat.getActiveEncounter(10) } returns encounter
            coEvery { combat.saveEncounter(any()) } answers {
                val enc = firstArg<Encounter>()
                encounter.value = enc
                enc.id
            }

            delegate = TacticalMapDelegate(
                sessionId = 10,
                _uiState = state,
                scope = scope,
                tacticalMapRepository = repo,
                encounterRepository = combat,
                distanceCalculator = distanceCalculator
            )
        }
    }

    @Test
    fun testToggleSidePanel() = runTest {
        val f = Fixture(backgroundScope)
        runCurrent()

        assertFalse(f.state.value.tacticalState.isSidePanelOpen)

        f.delegate.toggleSidePanel()
        runCurrent()
        assertTrue(f.state.value.tacticalState.isSidePanelOpen)

        f.delegate.setSidePanelOpen(false)
        runCurrent()
        assertFalse(f.state.value.tacticalState.isSidePanelOpen)

        f.delegate.setSidePanelOpen(true)
        runCurrent()
        assertTrue(f.state.value.tacticalState.isSidePanelOpen)
    }

    @Test
    fun testSwitchSidePanelTabs() = runTest {
        val f = Fixture(backgroundScope)
        runCurrent()

        assertEquals(TacticalSidePanelTab.NOTES_WIKI, f.state.value.tacticalState.activeSidePanelTab)

        f.delegate.setSidePanelTab(TacticalSidePanelTab.DICE)
        runCurrent()
        assertEquals(TacticalSidePanelTab.DICE, f.state.value.tacticalState.activeSidePanelTab)

        f.delegate.setSidePanelTab(TacticalSidePanelTab.MYTHIC)
        runCurrent()
        assertEquals(TacticalSidePanelTab.MYTHIC, f.state.value.tacticalState.activeSidePanelTab)

        f.delegate.setSidePanelTab(TacticalSidePanelTab.COMBAT)
        runCurrent()
        assertEquals(TacticalSidePanelTab.COMBAT, f.state.value.tacticalState.activeSidePanelTab)
    }

    @Test
    fun testSidePanelDiceConfigurationAndRoll() = runTest {
        val f = Fixture(backgroundScope)
        runCurrent()

        f.delegate.setSidePanelDice(count = 2, sides = 6, modifier = 3)
        runCurrent()

        assertEquals(2, f.state.value.tacticalState.sidePanelDiceCount)
        assertEquals(6, f.state.value.tacticalState.sidePanelDiceSides)
        assertEquals(3, f.state.value.tacticalState.sidePanelDiceModifier)

        f.delegate.rollSidePanelDice()
        runCurrent()

        val lastRoll = f.state.value.tacticalState.sidePanelLastDiceResult
        assertNotNull(lastRoll)
        assertTrue(lastRoll!!.total in (2 * 1 + 3)..(2 * 6 + 3))
        assertEquals("2d6+3", lastRoll.expression)
        assertEquals(1, f.state.value.tacticalState.sidePanelRecentRolls.size)

        // Otro lanzamiento
        f.delegate.rollSidePanelDice()
        runCurrent()
        assertEquals(2, f.state.value.tacticalState.sidePanelRecentRolls.size)
    }

    @Test
    fun testSidePanelMythicFateCheckAndRandomEvent() = runTest {
        val f = Fixture(backgroundScope)
        runCurrent()

        f.delegate.rollSidePanelFateCheck(probability = ProbabilityLevel.LIKELY, chaos = 5, forcedRoll = 40)
        runCurrent()

        val fateResult = f.state.value.tacticalState.sidePanelLastFateResult
        assertNotNull(fateResult)
        assertNotNull(fateResult!!.first)
        assertEquals(5, f.state.value.tacticalState.sidePanelMythicChaos)
        assertEquals(ProbabilityLevel.LIKELY, f.state.value.tacticalState.sidePanelMythicOdds)

        // Evento aleatorio
        f.delegate.rollSidePanelRandomEvent()
        runCurrent()

        val eventResult = f.state.value.tacticalState.sidePanelLastEventResult
        assertNotNull(eventResult)
        assertTrue(eventResult!!.contains(" · "))
    }

    @Test
    fun testSidePanelWikiSearchQuery() = runTest {
        val f = Fixture(backgroundScope)
        runCurrent()

        assertEquals("", f.state.value.tacticalState.wikiSearchQuery)

        f.delegate.setWikiSearchQuery("Cueva de los Lamentos")
        runCurrent()

        assertEquals("Cueva de los Lamentos", f.state.value.tacticalState.wikiSearchQuery)
    }

    @Test
    fun testSidePanelAdvanceCombatTurn() = runTest {
        val f = Fixture(backgroundScope)
        runCurrent()

        // Turno inicial 0
        assertEquals(0, f.encounter.value?.currentTurnIndex)

        f.delegate.advanceCombatTurn()
        runCurrent()

        // Avanza al turno 1
        assertEquals(1, f.encounter.value?.currentTurnIndex)
    }
}
