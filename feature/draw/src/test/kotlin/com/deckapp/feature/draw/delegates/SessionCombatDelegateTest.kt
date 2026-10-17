package com.deckapp.feature.draw.delegates

import com.deckapp.core.domain.repository.EncounterRepository
import com.deckapp.core.domain.repository.NpcRepository
import com.deckapp.core.domain.repository.SessionRepository
import com.deckapp.core.domain.usecase.*
import com.deckapp.core.model.*
import com.deckapp.feature.draw.SessionUiState
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionCombatDelegateTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private val encounterRepository: EncounterRepository = mockk(relaxed = true)
    private val sessionRepository: SessionRepository = mockk(relaxed = true)
    private val npcRepository: NpcRepository = mockk(relaxed = true)
    private val applyDamageUseCase: ApplyDamageUseCase = mockk(relaxed = true)
    private val rollInitiativeUseCase: RollInitiativeUseCase = mockk(relaxed = true)
    private val calculateInitiativeOrderUseCase: CalculateInitiativeOrderUseCase = mockk(relaxed = true)
    private val toggleConditionUseCase: ToggleConditionUseCase = mockk(relaxed = true)
    private val cleanupCombatUseCase: CleanupCombatUseCase = mockk(relaxed = true)
    private val updateChoirCharacterUseCase: UpdateChoirCharacterUseCase = mockk(relaxed = true)
    private val updateChoirCombatStateUseCase: UpdateChoirCombatStateUseCase = mockk(relaxed = true)
    private val notesMutex = Mutex()

    private val sessionId = 200L
    private val encounterId = 50L
    private var updatedNotesValue: String? = null

    private val activeEncounter = Encounter(
        id = encounterId,
        name = "Emboscada del Bosque",
        currentRound = 1,
        currentTurnIndex = 0,
        linkedSessionId = sessionId,
        creatures = listOf(
            EncounterCreature(id = 1L, encounterId = encounterId, name = "Orco Guerrero", currentHp = 15, maxHp = 15, sortOrder = 0),
            EncounterCreature(id = 2L, encounterId = encounterId, name = "Orco Chamán", currentHp = 10, maxHp = 10, sortOrder = 1)
        )
    )

    private val uiState = MutableStateFlow(
        SessionUiState(
            sessionId = sessionId,
            activeEncounter = activeEncounter,
            hasActiveEncounter = true
        )
    )

    private lateinit var delegate: SessionCombatDelegate

    @Before
    fun setUp() {
        updatedNotesValue = null
        delegate = SessionCombatDelegate(
            sessionId = sessionId,
            _uiState = uiState,
            scope = testScope,
            encounterRepository = encounterRepository,
            sessionRepository = sessionRepository,
            npcRepository = npcRepository,
            applyDamageUseCase = applyDamageUseCase,
            rollInitiativeUseCase = rollInitiativeUseCase,
            calculateInitiativeOrderUseCase = calculateInitiativeOrderUseCase,
            toggleConditionUseCase = toggleConditionUseCase,
            cleanupCombatUseCase = cleanupCombatUseCase,
            updateChoirCharacterUseCase = updateChoirCharacterUseCase,
            updateChoirCombatStateUseCase = updateChoirCombatStateUseCase,
            notesMutex = notesMutex,
            onUpdateNotes = { updatedNotesValue = it }
        )
    }

    @Test
    fun `applyDamage calls use case with creatureId and delta`() = runTest(testDispatcher) {
        delegate.applyDamage(1L, -5)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { applyDamageUseCase(1L, -5) }
    }

    @Test
    fun `toggleCondition calls toggleConditionUseCase`() = runTest(testDispatcher) {
        delegate.toggleCondition(1L, Condition.POISONED)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { toggleConditionUseCase(1L, Condition.POISONED) }
    }

    @Test
    fun `startCombatProcess triggers rollInitiative and calculateInitiativeOrder`() = runTest(testDispatcher) {
        delegate.startCombatProcess()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerifyOrder {
            rollInitiativeUseCase(encounterId)
            calculateInitiativeOrderUseCase(encounterId)
        }
    }

    @Test
    fun `removeCreature delegates to cleanupCombatUseCase`() = runTest(testDispatcher) {
        delegate.removeCreature(2L)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { cleanupCombatUseCase.removeCreature(encounterId, 2L) }
    }

    @Test
    fun `addPlayerParticipant and removePlayerParticipant update uiState player list`() {
        assertEquals(0, uiState.value.playerParticipants.size)

        delegate.addPlayerParticipant("Krag", 18)
        assertEquals(1, uiState.value.playerParticipants.size)
        val added = uiState.value.playerParticipants.first()
        assertEquals("Krag", added.name)
        assertEquals(18, added.initiativeTotal)

        delegate.removePlayerParticipant(added.id)
        assertEquals(0, uiState.value.playerParticipants.size)
    }

    @Test
    fun `toggleCombatHudExpanded flips the hud expanded boolean`() {
        assertFalse(uiState.value.isCombatHudExpanded)

        delegate.toggleCombatHudExpanded()
        assertTrue(uiState.value.isCombatHudExpanded)

        delegate.toggleCombatHudExpanded()
        assertFalse(uiState.value.isCombatHudExpanded)
    }

    @Test
    fun `nextTurn advances turn index and increments round on wrap`() = runTest(testDispatcher) {
        delegate.nextTurn()
        testDispatcher.scheduler.advanceUntilIdle()

        // 2 creatures: index 0 -> index 1 (round 1)
        coVerify {
            encounterRepository.saveEncounter(
                match { it.currentTurnIndex == 1 && it.currentRound == 1 }
            )
        }

        // Simular que el encounter se actualizó en uiState
        uiState.value = uiState.value.copy(
            activeEncounter = activeEncounter.copy(currentTurnIndex = 1, currentRound = 1)
        )

        delegate.nextTurn()
        testDispatcher.scheduler.advanceUntilIdle()

        // Index 1 -> wrap around: index 0, round 2
        coVerify {
            encounterRepository.saveEncounter(
                match { it.currentTurnIndex == 0 && it.currentRound == 2 }
            )
        }
    }

    @Test
    fun `updateNpcHp updates the existing NPC in repository`() = runTest(testDispatcher) {
        val existingNpc = Npc(id = 7L, name = "Tabernero", currentHp = 10, maxHp = 10)
        coEvery { npcRepository.getNpcById(7L) } returns existingNpc

        delegate.updateNpcHp(7L, 6)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { npcRepository.saveNpc(match { it.id == 7L && it.currentHp == 6 }) }
    }
}
