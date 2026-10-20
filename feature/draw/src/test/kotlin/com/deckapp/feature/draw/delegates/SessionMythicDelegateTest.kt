package com.deckapp.feature.draw.delegates

import com.deckapp.core.domain.repository.MythicRepository
import com.deckapp.core.domain.repository.SessionRepository
import com.deckapp.core.domain.usecase.mythic.RollMythicFateUseCase
import com.deckapp.core.model.*
import com.deckapp.feature.draw.SessionUiState
import com.deckapp.feature.draw.components.threads.ThreadsOverlayTab
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
class SessionMythicDelegateTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private val mythicRepository: MythicRepository = mockk(relaxed = true)
    private val sessionRepository: SessionRepository = mockk(relaxed = true)
    private val rollMythicFateUseCase: RollMythicFateUseCase = mockk(relaxed = true)
    private val notesMutex = Mutex()

    private val sessionId = 100L
    private var updatedNotesValue: String? = null
    private var focusedSceneId: Long? = null

    private val uiState = MutableStateFlow(
        SessionUiState(
            sessionId = sessionId,
            soloMythic = com.deckapp.feature.draw.model.state.SoloMythicUiState(
                chaos = 5,
                probability = ProbabilityLevel.FIFTY_FIFTY
            ),
            dmNotes = "Diario inicial"
        )
    )

    private lateinit var delegate: SessionMythicDelegate

    @Before
    fun setUp() {
        delegate = SessionMythicDelegate(
            sessionId = sessionId,
            _uiState = uiState,
            scope = testScope,
            mythicRepository = mythicRepository,
            sessionRepository = sessionRepository,
            rollMythicFateUseCase = rollMythicFateUseCase,
            notesMutex = notesMutex,
            onUpdateNotes = { updatedNotesValue = it },
            updateBoardMemory = { transform ->
                val current = uiState.value.boardMemory
                uiState.value = uiState.value.copy(boardMemory = transform(current))
            },
            focusScene = { focusedSceneId = it }
        )
    }

    @Test
    fun `updateBoardMythicChaos updates chaos clamped between 1 and 9`() = testScope.runTest {
        delegate.updateBoardMythicChaos(2)
        assertEquals(7, uiState.value.boardMythicChaos)

        delegate.updateBoardMythicChaos(5)
        assertEquals(9, uiState.value.boardMythicChaos)

        delegate.updateBoardMythicChaos(-10)
        assertEquals(1, uiState.value.boardMythicChaos)
    }

    @Test
    fun `setBoardMythicProbability updates probability in uiState`() {
        delegate.setBoardMythicProbability(ProbabilityLevel.VERY_LIKELY)
        assertEquals(ProbabilityLevel.VERY_LIKELY, uiState.value.boardMythicProbability)
    }

    @Test
    fun `rollBoardMythicFate generates result and records history`() {
        delegate.rollBoardMythicFate(
            probability = ProbabilityLevel.FIFTY_FIFTY,
            forcedRoll = 42,
            question = "¿Hay guardias?"
        )

        assertNotNull(uiState.value.boardMythicLastResult)
        assertEquals(42, uiState.value.boardMythicLastResult?.roll)
        assertEquals("¿Hay guardias?", uiState.value.boardMythicLastResult?.question)
        assertEquals(1, uiState.value.boardMythicHistory.size)
    }

    @Test
    fun `rollBoardMythicRandomEvent generates non-blank keyword event`() {
        val event = delegate.rollBoardMythicRandomEvent()
        assertNotNull(event)
        assertTrue(event.contains("·"))
        assertEquals(event, uiState.value.boardMythicLastEvent)
    }

    @Test
    fun `saveSoloScene persists scene and triggers onSuccess`() = testScope.runTest {
        coEvery { sessionRepository.upsertScene(any()) } returns 33L
        var successCalled = false

        val scene = Scene(sessionId = sessionId, title = "Investigación en la taberna")
        delegate.saveSoloScene(scene) {
            successCalled = true
        }
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(successCalled)
        assertEquals(33L, focusedSceneId)
        coVerify { sessionRepository.upsertScene(scene) }
    }

    @Test
    fun `saveMythicThread adds thread and closes dialog`() = testScope.runTest {
        uiState.value = uiState.value.copy(showThreadEditDialog = true)
        coEvery { mythicRepository.saveSession(any()) } returns 10L

        delegate.saveMythicThread(0L, "Encontrar la llave de plata")
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(uiState.value.showThreadEditDialog)
        coVerify { mythicRepository.addThread(match { it.description == "Encontrar la llave de plata" }) }
    }

    @Test
    fun `captureBoardInspiration and sendBoardInspirationToDiary appends to diary`() = testScope.runTest {
        val inspiration = BoardInspiration(
            id = "insp-1",
            source = InspirationSource.ORACLE,
            title = "Rumor",
            text = "El dragón duerme bajo las ruinas",
            status = InspirationStatus.PENDING
        )

        delegate.captureBoardInspiration(inspiration)
        assertEquals(1, uiState.value.boardMemory.inspiration.size)

        delegate.sendBoardInspirationToDiary("insp-1")
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(uiState.value.dmNotes.contains("El dragón duerme bajo las ruinas"))
        assertEquals(InspirationStatus.USED, uiState.value.boardMemory.inspiration.first().status)
        coVerify { sessionRepository.updateDmNotes(sessionId, any()) }
    }

    @Test
    fun `toggleThreadsOverlayExpanded toggles overlay visibility`() {
        assertFalse(uiState.value.isThreadsOverlayExpanded)
        delegate.toggleThreadsOverlayExpanded()
        assertTrue(uiState.value.isThreadsOverlayExpanded)
        delegate.toggleThreadsOverlayExpanded()
        assertFalse(uiState.value.isThreadsOverlayExpanded)
    }

    @Test
    fun `selectThreadsTab changes selected tab`() {
        delegate.selectThreadsTab(ThreadsOverlayTab.CHARACTERS)
        assertEquals(ThreadsOverlayTab.CHARACTERS, uiState.value.selectedThreadsTab)
    }

    @Test
    fun `soloMythicUiState encapsulation and synchronization with legacy getters`() {
        delegate.updateMythicState {
            it.copy(
                chaos = 8,
                probability = ProbabilityLevel.UNLIKELY,
                lastEvent = "Evento de prueba",
                isSoloSceneBusy = true,
                isSendingInspiration = true,
                lastRandomMythicPick = "Pick 123"
            )
        }

        // Verificar que los getters legacy sincronizan directamente con el sub-estado
        assertEquals(8, uiState.value.boardMythicChaos)
        assertEquals(ProbabilityLevel.UNLIKELY, uiState.value.boardMythicProbability)
        assertEquals("Evento de prueba", uiState.value.boardMythicLastEvent)
        assertTrue(uiState.value.isSoloSceneBusy)
        assertTrue(uiState.value.isSendingInspiration)
        assertEquals("Pick 123", uiState.value.lastRandomMythicPick)

        // Verificar acceso directo al sub-estado modular
        assertEquals(8, uiState.value.soloMythic.chaos)
        assertEquals("Evento de prueba", uiState.value.soloMythic.lastEvent)
    }
}

