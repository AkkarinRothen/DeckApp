package com.deckapp.feature.draw.tactical

import com.deckapp.core.domain.repository.*
import com.deckapp.core.domain.usecase.CalculateTacticalDistanceUseCase
import com.deckapp.core.model.*
import com.deckapp.feature.draw.SessionUiState
import com.deckapp.feature.draw.delegates.TacticalMapDelegate
import io.mockk.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class TacticalRouteMovementTest {

    private class Fixture(scope: CoroutineScope) {
        val maps = MutableStateFlow(listOf(TacticalMap(id = 1, name = "Dungeon", imageUri = "", sessionId = 10, scaleFtPerCell = 5)))
        val tokens = MutableStateFlow(listOf(TacticalToken(id = 2, mapId = 1, name = "Guerrero", cellX = 2f, cellY = 2f, speedFt = 30)))
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
            every { combat.getActiveEncounter(10) } returns flowOf(null)
            coEvery { repo.updateTokenPosition(any(), any(), any()) } answers {
                val id = firstArg<Long>(); val x = secondArg<Float>(); val y = arg<Float>(2)
                tokens.value = tokens.value.map { if (it.id == id) it.copy(cellX = x, cellY = y) else it }
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
    fun `selecting ROUTE tool with selected token initializes start waypoint`() = runTest {
        val f = Fixture(backgroundScope)
        runCurrent()

        f.delegate.selectToken(2L)
        runCurrent()
        f.delegate.setTacticalTool(TacticalTool.ROUTE)
        runCurrent()

        val tactical = f.state.value.tacticalState
        assertEquals(TacticalTool.ROUTE, tactical.activeTool)
        assertEquals(listOf(Pair(2f, 2f)), tactical.routeWaypoints)
        assertEquals(0, tactical.currentRouteDistanceFt)
    }

    @Test
    fun `adding and removing route waypoints updates total distance reactively`() = runTest {
        val f = Fixture(backgroundScope)
        runCurrent()

        f.delegate.selectToken(2L)
        f.delegate.setTacticalTool(TacticalTool.ROUTE)
        runCurrent()

        // Añadir waypoint en celda (4, 2) -> 2 celdas hacia la derecha = 10 ft
        f.delegate.addRouteWaypoint(4f, 2f)
        runCurrent()

        var tactical = f.state.value.tacticalState
        assertEquals(2, tactical.routeWaypoints.size)
        assertEquals(10, tactical.currentRouteDistanceFt)

        // Añadir segundo waypoint en celda (4, 5) -> 3 celdas abajo = 15 ft más (total 25 ft)
        f.delegate.addRouteWaypoint(4f, 5f)
        runCurrent()

        tactical = f.state.value.tacticalState
        assertEquals(3, tactical.routeWaypoints.size)
        assertEquals(25, tactical.currentRouteDistanceFt)

        // Deshacer último waypoint vuelve a 10 ft
        f.delegate.removeLastRouteWaypoint()
        runCurrent()

        tactical = f.state.value.tacticalState
        assertEquals(2, tactical.routeWaypoints.size)
        assertEquals(10, tactical.currentRouteDistanceFt)
    }

    @Test
    fun `committing route movement updates token position and accumulates turn budget`() = runTest {
        val f = Fixture(backgroundScope)
        runCurrent()

        f.delegate.selectToken(2L)
        f.delegate.setTacticalTool(TacticalTool.ROUTE)
        runCurrent()

        // Ruta de 4 celdas = 20 ft
        f.delegate.addRouteWaypoint(6f, 2f)
        runCurrent()

        assertEquals(20, f.state.value.tacticalState.currentRouteDistanceFt)

        f.delegate.commitRouteMovement()
        runCurrent()

        // Token movido a (6.5f, 2.5f)
        val token = f.tokens.value.first()
        assertEquals(6.5f, token.cellX, 0.01f)
        assertEquals(2.5f, token.cellY, 0.01f)

        // Presupuesto de turno consumió 20 ft
        val turnBudget = f.state.value.tacticalState.tokenMovedThisTurnFt[2L]
        assertEquals(20, turnBudget)

        // Reset de presupuesto
        f.delegate.resetTokenTurnMovement(2L)
        runCurrent()
        assertNull(f.state.value.tacticalState.tokenMovedThisTurnFt[2L])
    }
}
