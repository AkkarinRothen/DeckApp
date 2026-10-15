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
class TacticalCombatSyncTest {

    private class Fixture(scope: CoroutineScope) {
        val maps = MutableStateFlow(listOf(TacticalMap(id = 1, name = "Arena", imageUri = "", sessionId = 10, scaleFtPerCell = 5)))
        val creature1 = EncounterCreature(id = 101L, encounterId = 50L, name = "Guerrero", maxHp = 30, currentHp = 30, armorClass = 16, sortOrder = 0)
        val creature2 = EncounterCreature(id = 102L, encounterId = 50L, name = "Orco", maxHp = 20, currentHp = 20, armorClass = 13, sortOrder = 1)
        val encounter = MutableStateFlow<Encounter?>(
            Encounter(id = 50L, name = "Emboscada", creatures = listOf(creature1, creature2), currentRound = 1, currentTurnIndex = 0)
        )

        val token1 = TacticalToken(id = 1L, mapId = 1, name = "Guerrero", cellX = 2f, cellY = 2f, currentHp = 30, maxHp = 30, armorClass = 16, encounterCreatureId = 101L)
        val token2 = TacticalToken(id = 2L, mapId = 1, name = "Orco", cellX = 5f, cellY = 2f, currentHp = 20, maxHp = 20, armorClass = 13, encounterCreatureId = 102L)
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
            coEvery { combat.getCreatureById(102L) } returns creature2
            coEvery { combat.getCreatureById(101L) } returns creature1
            coEvery { repo.updateTokenHp(any(), any()) } answers {
                val id = firstArg<Long>(); val hp = secondArg<Int>()
                tokens.value = tokens.value.map { if (it.id == id) it.copy(currentHp = hp) else it }
            }
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
    fun `executing attack calculates d20 vs AC, hit or miss and generates resolution`() = runTest {
        val f = Fixture(backgroundScope)
        runCurrent()

        // Ataque exitoso: d20 = 12 + 4 = 16 vs CA 13 (Orco) -> Impacto
        f.delegate.executeAttack(
            attackerTokenId = 1L,
            targetTokenId = 2L,
            attackName = "Espada Larga",
            attackBonus = 4,
            damageFormula = "1d8+3",
            forcedD20 = 12,
            forcedDamage = 7
        )
        runCurrent()

        val hitResolution = f.state.value.tacticalState.attackResolution
        assertNotNull(hitResolution)
        assertEquals(true, hitResolution!!.isHit)
        assertEquals(false, hitResolution.isCriticalHit)
        assertEquals(16, hitResolution.totalAttackRoll)
        assertEquals(7, hitResolution.damageRoll)
        assertEquals(15, hitResolution.distanceFt) // 3 celdas = 15 ft
        assertEquals(false, hitResolution.isDamageApplied)

        // Ataque fallido: d20 = 5 + 4 = 9 vs CA 13 -> Fallo
        f.delegate.executeAttack(
            attackerTokenId = 1L,
            targetTokenId = 2L,
            attackName = "Espada Larga",
            attackBonus = 4,
            damageFormula = "1d8+3",
            forcedD20 = 5,
            forcedDamage = 0
        )
        runCurrent()

        val missResolution = f.state.value.tacticalState.attackResolution
        assertNotNull(missResolution)
        assertEquals(false, missResolution!!.isHit)
        assertEquals(0, missResolution.damageRoll)
    }

    @Test
    fun `critical hit and critical miss are detected correctly`() = runTest {
        val f = Fixture(backgroundScope)
        runCurrent()

        // Pifia natural 1 -> Fallo crítico
        f.delegate.executeAttack(
            attackerTokenId = 1L,
            targetTokenId = 2L,
            attackName = "Ataque",
            attackBonus = 20, // Incluso con bono enorme, 1 natural falla
            damageFormula = "1d6",
            forcedD20 = 1,
            forcedDamage = 0
        )
        runCurrent()

        val pifia = f.state.value.tacticalState.attackResolution
        assertNotNull(pifia)
        assertEquals(true, pifia!!.isCriticalMiss)
        assertEquals(false, pifia.isHit)

        // Crítico natural 20 -> Impacto crítico
        f.delegate.executeAttack(
            attackerTokenId = 1L,
            targetTokenId = 2L,
            attackName = "Ataque",
            attackBonus = 0,
            damageFormula = "1d6",
            forcedD20 = 20,
            forcedDamage = 12
        )
        runCurrent()

        val critico = f.state.value.tacticalState.attackResolution
        assertNotNull(critico)
        assertEquals(true, critico!!.isCriticalHit)
        assertEquals(true, critico.isHit)
    }

    @Test
    fun `applying attack damage reduces target token HP and syncs to creature in combat`() = runTest {
        val f = Fixture(backgroundScope)
        runCurrent()

        f.delegate.executeAttack(
            attackerTokenId = 1L,
            targetTokenId = 2L,
            attackName = "Hachazo",
            attackBonus = 5,
            damageFormula = "1d12+3",
            forcedD20 = 15,
            forcedDamage = 8
        )
        runCurrent()

        val resolution = f.state.value.tacticalState.attackResolution!!
        f.delegate.applyAttackDamage(resolution)
        runCurrent()

        // Orco tenía 20 HP, recibe 8 de daño -> 12 HP
        val orco = f.tokens.value.find { it.id == 2L }!!
        assertEquals(12, orco.currentHp)
        assertEquals(true, f.state.value.tacticalState.attackResolution!!.isDamageApplied)

        // Verificamos que se sincronizó con el EncounterCreature
        coVerify { f.combat.updateCreature(match { it.id == 102L && it.currentHp == 12 }) }
    }

    @Test
    fun `advancing combat turn advances currentTurnIndex and resets turn movement budget`() = runTest {
        val f = Fixture(backgroundScope)
        runCurrent()

        // Seleccionar token 2 (Orco) y planificar ruta de movimiento
        f.delegate.selectToken(2L)
        f.delegate.setTacticalTool(TacticalTool.ROUTE)
        runCurrent()

        f.delegate.addRouteWaypoint(7f, 2f)
        runCurrent()
        f.delegate.commitRouteMovement()
        runCurrent()

        assertNotNull(f.state.value.tacticalState.tokenMovedThisTurnFt[2L])

        f.delegate.advanceCombatTurn()
        runCurrent()

        val enc = f.encounter.value!!
        assertEquals(1, enc.currentTurnIndex) // Pasó al turno de Orco (índice 1)
        
        // El nuevo turno reinicia el presupuesto del combatiente activo
        val orcoMoved = f.state.value.tacticalState.tokenMovedThisTurnFt[2L]
        assertEquals(null, orcoMoved) // Reseteado
    }
}
