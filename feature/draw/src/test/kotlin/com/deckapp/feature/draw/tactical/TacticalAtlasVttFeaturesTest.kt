package com.deckapp.feature.draw.tactical

import com.deckapp.core.domain.repository.EncounterRepository
import com.deckapp.core.domain.repository.TacticalMapRepository
import com.deckapp.core.domain.usecase.CalculateTacticalDistanceUseCase
import com.deckapp.core.model.*
import com.deckapp.feature.draw.SessionUiState
import com.deckapp.feature.draw.delegates.TacticalMapDelegate
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class TacticalAtlasVttFeaturesTest {

    private val mapRepo = mockk<TacticalMapRepository>(relaxed = true)
    private val encounterRepo = mockk<EncounterRepository>(relaxed = true)
    private val distanceCalculator = CalculateTacticalDistanceUseCase()

    @Test
    fun `TacticalMapPin model stores targetMapId for Map-in-Map nesting`() {
        val pin = TacticalMapPin(
            id = 10,
            mapId = 1,
            cellX = 5f,
            cellY = 8f,
            title = "Entrada a la Mazmorra",
            description = "Escaleras de caracol descendentes hacia la cripta",
            iconName = "stairs",
            targetMapId = 42L
        )

        assertEquals(42L, pin.targetMapId)
        assertEquals("stairs", pin.iconName)
    }

    @Test
    fun `navigateToSubMap pushes current map to breadcrumbs and switches active map`() = runTest {
        val currentMap = TacticalMap(id = 1L, name = "Valle de Phandalin", imageUri = "phandalin.png")
        val subMap = TacticalMap(id = 2L, name = "Cueva del Eco Rojo", imageUri = "cueva.png")

        val uiState = MutableStateFlow(
            SessionUiState(
                tacticalState = TacticalUiState(
                    map = currentMap,
                    availableMaps = listOf(currentMap, subMap),
                    mapBreadcrumbs = emptyList()
                )
            )
        )

        every { mapRepo.getTacticalMapsBySessionId(any()) } returns flowOf(listOf(currentMap, subMap))
        every { mapRepo.getTokensByMapId(any()) } returns flowOf(emptyList())
        every { mapRepo.getPinsByMapId(any()) } returns flowOf(emptyList())
        every { encounterRepo.getActiveEncounter(any()) } returns flowOf(null)

        val delegate = TacticalMapDelegate(
            sessionId = 100L,
            _uiState = uiState,
            scope = this,
            tacticalMapRepository = mapRepo,
            encounterRepository = encounterRepo,
            distanceCalculator = distanceCalculator
        )

        delegate.navigateToSubMap(2L)

        val updated = uiState.value.tacticalState
        assertEquals(2L, updated.map?.id)
        assertEquals(1, updated.mapBreadcrumbs.size)
        assertEquals(1L, updated.mapBreadcrumbs[0].first)
        assertEquals("Valle de Phandalin", updated.mapBreadcrumbs[0].second)
    }

    @Test
    fun `navigateBackToBreadcrumb pops ancestors and navigates to selected parent map`() = runTest {
        val map1 = TacticalMap(id = 1L, name = "Continente", imageUri = "c.png")
        val map2 = TacticalMap(id = 2L, name = "Reino", imageUri = "r.png")
        val map3 = TacticalMap(id = 3L, name = "Castillo", imageUri = "cas.png")

        val uiState = MutableStateFlow(
            SessionUiState(
                tacticalState = TacticalUiState(
                    map = map3,
                    availableMaps = listOf(map1, map2, map3),
                    mapBreadcrumbs = listOf(1L to "Continente", 2L to "Reino")
                )
            )
        )

        every { mapRepo.getTacticalMapsBySessionId(any()) } returns flowOf(listOf(map1, map2, map3))
        every { mapRepo.getTokensByMapId(any()) } returns flowOf(emptyList())
        every { mapRepo.getPinsByMapId(any()) } returns flowOf(emptyList())
        every { encounterRepo.getActiveEncounter(any()) } returns flowOf(null)

        val delegate = TacticalMapDelegate(
            sessionId = 100L,
            _uiState = uiState,
            scope = this,
            tacticalMapRepository = mapRepo,
            encounterRepository = encounterRepo,
            distanceCalculator = distanceCalculator
        )

        delegate.navigateBackToBreadcrumb(1L)

        val updated = uiState.value.tacticalState
        assertEquals(1L, updated.map?.id)
        // Clicking breadcrumb 1L should leave breadcrumbs empty (since 1L was root)
        assertTrue(updated.mapBreadcrumbs.isEmpty())
    }

    @Test
    fun `spawnEncounterOnMap spawns cluster tokens and activates encounter`() = runTest {
        val map = TacticalMap(id = 1L, name = "Arena", imageUri = "arena.png")
        val creatures = listOf(
            EncounterCreature(id = 101L, encounterId = 50L, name = "Orco 1", currentHp = 15, maxHp = 15, armorClass = 13),
            EncounterCreature(id = 102L, encounterId = 50L, name = "Orco 2", currentHp = 15, maxHp = 15, armorClass = 13)
        )
        val encounter = Encounter(id = 50L, name = "Emboscada Orca", creatures = creatures)

        val uiState = MutableStateFlow(
            SessionUiState(
                tacticalState = TacticalUiState(
                    map = map,
                    availableEncounters = listOf(encounter),
                    tokens = emptyList()
                )
            )
        )

        coEvery { mapRepo.upsertToken(any()) } returns 1L
        coEvery { encounterRepo.saveEncounter(any()) } returns 50L

        val delegate = TacticalMapDelegate(
            sessionId = 100L,
            _uiState = uiState,
            scope = this,
            tacticalMapRepository = mapRepo,
            encounterRepository = encounterRepo,
            distanceCalculator = distanceCalculator
        )

        delegate.spawnEncounterOnMap(50L, originCellX = 10f, originCellY = 10f)
        testScheduler.advanceUntilIdle()

        // Verify tokens were persisted
        coVerify(exactly = 2) { mapRepo.upsertToken(any()) }
        coVerify(exactly = 1) { encounterRepo.saveEncounter(match { it.id == 50L && it.isActive }) }

        val finalState = uiState.value.tacticalState
        assertNotNull(finalState.focusCameraOnTokenId)
    }

    @Test
    fun `combat turn navigation advances turn, increments round, and auto-focuses camera`() = runTest {
        val creatures = listOf(
            EncounterCreature(id = 101L, encounterId = 5L, name = "Guerrero", currentHp = 25, maxHp = 25),
            EncounterCreature(id = 102L, encounterId = 5L, name = "Dragón", currentHp = 120, maxHp = 120)
        )
        val encounter = Encounter(id = 5L, name = "Boss", creatures = creatures, currentTurnIndex = 0, currentRound = 1)
        val tokenDragon = TacticalToken(id = 99L, mapId = 1L, name = "Dragón", encounterCreatureId = 102L)

        val uiState = MutableStateFlow(
            SessionUiState(
                tacticalState = TacticalUiState(
                    activeEncounter = encounter,
                    tokens = listOf(tokenDragon)
                )
            )
        )

        coEvery { encounterRepo.saveEncounter(any()) } returns 5L

        val delegate = TacticalMapDelegate(
            sessionId = 100L,
            _uiState = uiState,
            scope = this,
            tacticalMapRepository = mapRepo,
            encounterRepository = encounterRepo,
            distanceCalculator = distanceCalculator
        )

        delegate.advanceCombatTurn()
        testScheduler.advanceUntilIdle()

        coVerify {
            encounterRepo.saveEncounter(match {
                it.currentTurnIndex == 1
            })
        }
        assertEquals(99L, uiState.value.tacticalState.focusCameraOnTokenId)
    }

    @Test
    fun `rollInlineFormula evaluates dice formula and displays result in snackbar`() = runTest {
        val uiState = MutableStateFlow(SessionUiState())

        val delegate = TacticalMapDelegate(
            sessionId = 100L,
            _uiState = uiState,
            scope = this,
            tacticalMapRepository = mapRepo,
            encounterRepository = encounterRepo,
            distanceCalculator = distanceCalculator
        )

        delegate.rollInlineFormula("2d6+4", "Trampa de Flechas")

        val snackbar = uiState.value.snackbarMessage
        assertNotNull(snackbar)
        assertTrue(snackbar!!.startsWith("Trampa de Flechas (2d6+4):"))
    }
}
