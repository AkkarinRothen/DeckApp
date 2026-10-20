package com.deckapp.feature.draw.delegates

import com.deckapp.core.domain.repository.SessionRepository
import com.deckapp.core.domain.usecase.mythic.SceneCheckResult
import com.deckapp.core.model.*
import com.deckapp.feature.draw.SessionUiState
import com.deckapp.feature.draw.scene.SceneCheckOutcome
import com.deckapp.feature.draw.scene.SceneFramingDraft
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
class SessionClocksDelegateTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private val sessionRepository: SessionRepository = mockk(relaxed = true)
    private val notesMutex = Mutex()

    private val sessionId = 102L

    private var addedBoardBlock: Pair<SessionBoardItemType, Long>? = null
    private var rolledSmartDice: Pair<String, String>? = null
    private var rolledMythicRandomEvent = false
    private var rolledMythicProbability: ProbabilityLevel? = null
    private var rolledTableId: Long? = null
    private var drawnDeckId: Long? = null
    private var updatedMythicChaosDelta: Int? = null
    private var focusedSceneId: Long? = null

    private val clock1 = TensionClock(
        id = 1L,
        sessionId = sessionId,
        name = "Alarma del Castillo",
        segments = 4,
        progress = 1,
        triggerDescription = "Los guardias tocan los cuernos"
    )

    private val uiState = MutableStateFlow(
        SessionUiState(
            sessionId = sessionId,
            tensionClocks = listOf(clock1),
            session = Session(id = sessionId, name = "Campaña Principal"),
            dmNotes = "Notas previas"
        )
    )

    private lateinit var delegate: SessionClocksDelegate

    @Before
    fun setUp() {
        addedBoardBlock = null
        rolledSmartDice = null
        rolledMythicRandomEvent = false
        rolledMythicProbability = null
        rolledTableId = null
        drawnDeckId = null
        updatedMythicChaosDelta = null
        focusedSceneId = null

        delegate = SessionClocksDelegate(
            sessionId = sessionId,
            _uiState = uiState,
            scope = testScope,
            sessionRepository = sessionRepository,
            notesMutex = notesMutex,
            onAddBoardBlock = { type, _, _, resId -> addedBoardBlock = type to resId },
            onRollSmartDice = { formula, desc -> rolledSmartDice = formula to desc },
            onRollMythicRandomEvent = { rolledMythicRandomEvent = true },
            onRollMythicFate = { rolledMythicProbability = it },
            onRollTable = { rolledTableId = it },
            onDrawCard = { drawnDeckId = it },
            onUpdateBoardMythicChaos = { updatedMythicChaosDelta = it },
            onFocusScene = { focusedSceneId = it }
        )
    }

    @Test
    fun `tickClock advances progress and alerts when completed`() = testScope.runTest {
        delegate.tickClock(1L, 2)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(3, uiState.value.tensionClocks.first().progress)
        assertFalse(uiState.value.tensionClocks.first().isCompleted)
        assertNull(uiState.value.clockImpactAlert)

        delegate.tickClock(1L, 1)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(4, uiState.value.tensionClocks.first().progress)
        assertTrue(uiState.value.tensionClocks.first().isCompleted)
        assertNotNull(uiState.value.clockImpactAlert)
        assertEquals(1L, uiState.value.clockImpactAlert?.id)
        coVerify { sessionRepository.updateTensionClocks(sessionId, any()) }
    }

    @Test
    fun `resetClock zeroes progress and clears completion`() = testScope.runTest {
        delegate.tickClock(1L, 3)
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(uiState.value.tensionClocks.first().isCompleted)

        delegate.resetClock(1L)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(0, uiState.value.tensionClocks.first().progress)
        assertFalse(uiState.value.tensionClocks.first().isCompleted)
        assertNull(uiState.value.clockImpactAlert)
    }

    @Test
    fun `deleteClock removes clock from list`() = testScope.runTest {
        delegate.deleteClock(1L)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(uiState.value.tensionClocks.isEmpty())
        coVerify { sessionRepository.updateTensionClocks(sessionId, emptyList()) }
    }

    @Test
    fun `copyClockImpactToDiary appends consequence and clears alert`() = testScope.runTest {
        val completedClock = clock1.copy(progress = 4, isCompleted = true)
        uiState.value = uiState.value.copy(modals = uiState.value.modals.copy(clockImpactAlert = completedClock))

        delegate.copyClockImpactToDiary(completedClock)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(uiState.value.dmNotes.contains("Los guardias tocan los cuernos"))
        assertNull(uiState.value.clockImpactAlert)
        coVerify { sessionRepository.updateDmNotes(sessionId, any()) }
    }


    @Test
    fun `createClockAndAddToBoard adds clock and triggers board item creation`() = testScope.runTest {
        delegate.createClockAndAddToBoard(
            name = "Rival se acerca",
            segments = 6,
            colorHex = "#FF5555",
            triggerDescription = "El rival entra al templo"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, uiState.value.tensionClocks.size)
        assertNotNull(addedBoardBlock)
        assertEquals(SessionBoardItemType.CLOCK, addedBoardBlock?.first)
    }

    @Test
    fun `executeHotbarAction routes different action types properly`() {
        delegate.executeHotbarAction(SessionHotbarAction(id = "a1", label = "Ataque", type = HotbarActionType.DICE_ROLL, payload = "1d20+5"))
        assertEquals("1d20+5" to "Ataque", rolledSmartDice)

        delegate.executeHotbarAction(SessionHotbarAction(id = "a2", label = "Evento", type = HotbarActionType.ORACLE_QUERY, payload = "RANDOM_EVENT"))
        assertTrue(rolledMythicRandomEvent)

        delegate.executeHotbarAction(SessionHotbarAction(id = "a3", label = "¿Probable?", type = HotbarActionType.ORACLE_QUERY, payload = "LIKELY"))
        assertEquals(ProbabilityLevel.LIKELY, rolledMythicProbability)

        delegate.executeHotbarAction(SessionHotbarAction(id = "a4", label = "Tabla", type = HotbarActionType.RANDOM_TABLE, payload = "88"))
        assertEquals(88L, rolledTableId)

        delegate.executeHotbarAction(SessionHotbarAction(id = "a5", label = "Robar", type = HotbarActionType.DRAW_CARD, payload = "42"))
        assertEquals(42L, drawnDeckId)
    }

    @Test
    fun `confirmSceneFraming creates scene and appends journal entry`() = testScope.runTest {
        coEvery { sessionRepository.upsertScene(any()) } returns 777L

        val draft = SceneFramingDraft(
            title = "Infiltración en la Fortaleza",
            location = "Murallas exteriores",
            objective = "Abrir el portón",
            expectation = "Poca vigilancia",
            checkOutcome = SceneCheckOutcome(
                chaosFactor = 5,
                roll = 8,
                result = SceneCheckResult.NORMAL
            )
        )

        delegate.confirmSceneFraming(
            draft = draft,
            closePrevious = false,
            prevSummary = "",
            chaosDelta = 0
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(777L, focusedSceneId)
        assertTrue(uiState.value.dmNotes.contains("Infiltración en la Fortaleza"))
        coVerify { sessionRepository.upsertScene(any()) }
        coVerify { sessionRepository.updateDmNotes(sessionId, any()) }
    }

    @Test
    fun `sessionModalsUiState encapsulation and synchronization with legacy getters`() {
        assertFalse(uiState.value.showClockEditDialog)
        assertFalse(uiState.value.showHotbarActionDialog)
        assertFalse(uiState.value.showSceneFramingDialog)

        delegate.openCreateClockDialog()
        assertTrue(uiState.value.showClockEditDialog)
        assertTrue(uiState.value.modals.showClockEditDialog)

        delegate.closeClockEditDialog()
        assertFalse(uiState.value.showClockEditDialog)
        assertFalse(uiState.value.modals.showClockEditDialog)

        delegate.openCreateHotbarAction()
        assertTrue(uiState.value.showHotbarActionDialog)
        assertTrue(uiState.value.modals.showHotbarActionDialog)

        delegate.dismissHotbarActionDialog()
        assertFalse(uiState.value.showHotbarActionDialog)
        assertFalse(uiState.value.modals.showHotbarActionDialog)

        delegate.openSceneFraming()
        assertTrue(uiState.value.showSceneFramingDialog)
        assertTrue(uiState.value.modals.showSceneFramingDialog)

        delegate.dismissSceneFraming()
        assertFalse(uiState.value.showSceneFramingDialog)
        assertFalse(uiState.value.modals.showSceneFramingDialog)
    }
}

