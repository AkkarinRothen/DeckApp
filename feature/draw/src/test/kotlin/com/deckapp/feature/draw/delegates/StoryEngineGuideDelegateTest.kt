package com.deckapp.feature.draw.delegates

import com.deckapp.core.domain.repository.SessionRepository
import com.deckapp.core.model.Card
import com.deckapp.core.model.CardFace
import com.deckapp.core.model.CardStack
import com.deckapp.core.model.Session
import com.deckapp.core.model.SessionDeckRef
import com.deckapp.core.model.StackType
import com.deckapp.feature.draw.SessionUiState
import com.deckapp.feature.draw.model.StoryEngineGuideTab
import com.deckapp.feature.draw.model.StoryEngineProcedureCatalog
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StoryEngineGuideDelegateTest {

    private val sessionRepository = mockk<SessionRepository>(relaxed = true)
    private val deckDelegate = mockk<SessionDeckDelegate>(relaxed = true)
    private var microsettingBuilderOpened = false

    private val regionDeck = CardStack(id = 101, name = "Regiones Principales", type = StackType.DECK, description = "Deck of Worlds Regions")
    private val landmarkDeck = CardStack(id = 102, name = "Puntos de Interés", type = StackType.DECK, description = "Landmarks")

    private val ui = MutableStateFlow(
        SessionUiState(
            sessionId = 1,
            session = Session(id = 1, name = "Mundo Fantasía", gameSystems = listOf("Story Engine", "Deck of Worlds")),
            decks = com.deckapp.feature.draw.model.state.DecksUiState(
                allDecks = listOf(regionDeck, landmarkDeck),
                deckRefs = listOf(SessionDeckRef(sessionId = 1, stackId = 101)),
                hand = listOf(
                    Card(id = 501, stackId = 101, originDeckId = 101, title = "Pantano Brumoso", faces = listOf(CardFace(name = "Frente"))),
                    Card(id = 502, stackId = 102, originDeckId = 102, title = "Torre en Ruinas", faces = listOf(CardFace(name = "Frente")))
                )
            )
        )
    )

    private fun createDelegate(scope: kotlinx.coroutines.CoroutineScope) = StoryEngineGuideDelegate(
        sessionId = 1,
        ui = ui,
        scope = scope,
        sessionRepository = sessionRepository,
        deckDelegate = deckDelegate,
        openMicrosettingBuilder = { microsettingBuilderOpened = true }
    )

    @Test
    fun `open and close update ui state flags`() = runTest {
        val delegate = createDelegate(this)

        delegate.open(StoryEngineGuideTab.NEW_STORY_ENGINE)
        assertTrue(ui.value.showStoryEngineGuide)
        assertEquals(StoryEngineGuideTab.NEW_STORY_ENGINE, ui.value.storyEngineGuideTab)

        delegate.close()
        assertFalse(ui.value.showStoryEngineGuide)
    }

    @Test
    fun `navigation through steps and tabs respects bounds`() = runTest {
        val delegate = createDelegate(this)

        delegate.selectTab(StoryEngineGuideTab.DECK_OF_WORLDS)
        assertEquals(0, ui.value.storyEngineGuideStepIndex)

        delegate.nextStep()
        assertEquals(1, ui.value.storyEngineGuideStepIndex)

        delegate.selectStep(5)
        assertEquals(5, ui.value.storyEngineGuideStepIndex)

        // Cannot exceed max step index for Deck of Worlds (0..5)
        delegate.nextStep()
        assertEquals(5, ui.value.storyEngineGuideStepIndex)

        delegate.prevStep()
        assertEquals(4, ui.value.storyEngineGuideStepIndex)
    }

    @Test
    fun `drawStepCard adds missing deck to session and draws from it`() = runTest {
        val delegate = createDelegate(this)
        val landmarkStep = StoryEngineProcedureCatalog.deckOfWorldsSteps[1] // Landmark step

        // Landmark deck (102) is in allDecks but not yet in deckRefs in session
        ui.value = ui.value.copy(decks = ui.value.decks.copy(deckRefs = listOf(SessionDeckRef(sessionId = 1, stackId = 101))))

        delegate.drawStepCard(landmarkStep)
        testScheduler.advanceUntilIdle()

        coVerify { sessionRepository.addDeckToSession(match { it.sessionId == 1L && it.stackId == 102L }) }
        verify { deckDelegate.drawCardFromDeck(102L) }
        assertTrue(ui.value.snackbarMessage?.contains("Puntos de Interés") == true)
    }

    @Test
    fun `rotateStepCard delegates card rotation for matching card in hand`() = runTest {
        val delegate = createDelegate(this)
        val landmarkStep = StoryEngineProcedureCatalog.deckOfWorldsSteps[1]

        delegate.rotateStepCard(landmarkStep)

        verify { deckDelegate.rotateCard(502L) }
        assertTrue(ui.value.snackbarMessage?.contains("Torre en Ruinas") == true)
    }

    @Test
    fun `launchMicrosettingBuilder triggers callback`() = runTest {
        val delegate = createDelegate(this)
        assertFalse(microsettingBuilderOpened)

        delegate.launchMicrosettingBuilder()
        assertTrue(microsettingBuilderOpened)
    }

    @Test
    fun `guides substate encapsulation and getter synchronization`() = runTest {
        val delegate = createDelegate(this)

        delegate.open(StoryEngineGuideTab.NEW_STORY_ENGINE)
        assertEquals(StoryEngineGuideTab.NEW_STORY_ENGINE, ui.value.guides.storyEngineGuideTab)
        assertEquals(ui.value.guides.storyEngineGuideTab, ui.value.storyEngineGuideTab)

        delegate.selectStep(3)
        assertEquals(3, ui.value.guides.storyEngineGuideStepIndex)
        assertEquals(ui.value.guides.storyEngineGuideStepIndex, ui.value.storyEngineGuideStepIndex)
    }
}
