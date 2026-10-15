package com.deckapp.feature.draw.tactical

import com.deckapp.core.domain.repository.EncounterRepository
import com.deckapp.core.domain.repository.TacticalMapRepository
import com.deckapp.core.domain.usecase.CalculateTacticalDistanceUseCase
import com.deckapp.core.model.*
import com.deckapp.feature.draw.SessionUiState
import com.deckapp.feature.draw.delegates.TacticalMapDelegate
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.hypot
import kotlin.math.roundToInt

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class TacticalElevationConditionsTest {

    private val distanceCalculator = CalculateTacticalDistanceUseCase()

    @Test
    fun `3D distance calculation computes accurate hypotenuse when tokens have elevation`() {
        // Horizontal distance: from (0, 0) to (8, 0) cells at 5ft/cell = 40 ft.
        // Elevation difference: 0 ft to 30 ft.
        // Expected 3D distance: sqrt(40^2 + 30^2) = 50 ft.
        val result = distanceCalculator(
            fromX = 0f,
            fromY = 0f,
            toX = 8f,
            toY = 0f,
            gridType = GridType.SQUARE,
            diagonalRule = DiagonalRule.EQUIDISTANT,
            scaleFtPerCell = 5,
            fromElevationFt = 0,
            toElevationFt = 30
        )

        assertEquals(40, result.distanceFt)
        assertEquals(30, result.elevationDiffFt)
        assertEquals(50, result.distance3dFt)
    }

    @Test
    fun `3D distance equals horizontal distance when elevation difference is zero`() {
        val result = distanceCalculator(
            fromX = 2f,
            fromY = 3f,
            toX = 6f,
            toY = 3f,
            gridType = GridType.SQUARE,
            diagonalRule = DiagonalRule.EQUIDISTANT,
            scaleFtPerCell = 5,
            fromElevationFt = 20,
            toElevationFt = 20
        )

        assertEquals(20, result.distanceFt)
        assertEquals(0, result.elevationDiffFt)
        assertEquals(20, result.distance3dFt)
    }

    @Test
    fun `tactical token model stores elevation and conditions accurately`() {
        val token = TacticalToken(
            id = 1L,
            mapId = 10L,
            name = "Mago Volador",
            elevationFt = 45,
            conditions = setOf(Condition.INVISIBLE, Condition.POISONED)
        )

        assertEquals(45, token.elevationFt)
        assertTrue(token.conditions.contains(Condition.INVISIBLE))
        assertTrue(token.conditions.contains(Condition.POISONED))
        assertFalse(token.conditions.contains(Condition.PRONE))
    }

    @Test
    fun `executeAttack with advantage rolls two d20s and records advantage label`() = runTest {
        val scope = this
        val state = MutableStateFlow(
            SessionUiState(
                sessionId = 1,
                tacticalState = TacticalUiState(
                    map = TacticalMap(id = 1, name = "Cueva", imageUri = "", scaleFtPerCell = 5),
                    tokens = listOf(
                        TacticalToken(id = 10L, mapId = 1, name = "Guerrero", cellX = 0f, cellY = 0f, elevationFt = 0),
                        TacticalToken(id = 20L, mapId = 1, name = "Gárgola", cellX = 4f, cellY = 0f, elevationFt = 15, armorClass = 14)
                    )
                )
            )
        )

        val repo = mockk<TacticalMapRepository>(relaxed = true)
        val encounterRepo = mockk<EncounterRepository>(relaxed = true)

        val delegate = TacticalMapDelegate(
            sessionId = 1,
            scope = scope,
            _uiState = state,
            tacticalMapRepository = repo,
            encounterRepository = encounterRepo,
            distanceCalculator = distanceCalculator
        )

        // Ejecutar ataque con Ventaja (advantageMode = 1)
        delegate.executeAttack(
            attackerTokenId = 10L,
            targetTokenId = 20L,
            attackName = "Flecha",
            attackBonus = 5,
            damageFormula = "1d8+3",
            advantageMode = 1
        )

        val resolution = state.value.tacticalState.attackResolution
        assertNotNull("La resolución del ataque no debe ser nula", resolution)
        assertEquals(10L, resolution!!.attackerTokenId)
        assertEquals(20L, resolution.targetTokenId)
        assertEquals(15, resolution.elevationDiffFt)
        // Distancia horizontal = 20ft, elev = 15ft -> 3D = sqrt(20^2 + 15^2) = 25ft
        assertEquals(25, resolution.distanceFt)
        assertNotNull(resolution.advantageLabel)
        assertTrue(resolution.advantageLabel!!.contains("Ventaja"))
    }

    @Test
    fun `executeAttack with disadvantage rolls two d20s and records disadvantage label`() = runTest {
        val scope = this
        val state = MutableStateFlow(
            SessionUiState(
                sessionId = 1,
                tacticalState = TacticalUiState(
                    map = TacticalMap(id = 1, name = "Arena", imageUri = "", scaleFtPerCell = 5),
                    tokens = listOf(
                        TacticalToken(id = 1L, mapId = 1, name = "Pícaro", cellX = 0f, cellY = 0f),
                        TacticalToken(id = 2L, mapId = 1, name = "Orco", cellX = 2f, cellY = 0f, armorClass = 13)
                    )
                )
            )
        )

        val repo = mockk<TacticalMapRepository>(relaxed = true)
        val encounterRepo = mockk<EncounterRepository>(relaxed = true)

        val delegate = TacticalMapDelegate(
            sessionId = 1,
            scope = scope,
            _uiState = state,
            tacticalMapRepository = repo,
            encounterRepository = encounterRepo,
            distanceCalculator = distanceCalculator
        )

        // Ejecutar ataque con Desventaja (advantageMode = -1)
        delegate.executeAttack(
            attackerTokenId = 1L,
            targetTokenId = 2L,
            attackName = "Daga",
            attackBonus = 3,
            damageFormula = "1d4+1",
            advantageMode = -1
        )

        val resolution = state.value.tacticalState.attackResolution
        assertNotNull(resolution)
        assertNotNull(resolution!!.advantageLabel)
        assertTrue(resolution.advantageLabel!!.contains("Desventaja"))
    }
}
