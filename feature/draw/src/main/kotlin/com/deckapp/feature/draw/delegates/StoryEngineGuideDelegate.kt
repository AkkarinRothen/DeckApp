package com.deckapp.feature.draw.delegates

import com.deckapp.core.domain.repository.SessionRepository
import com.deckapp.core.model.CardStack
import com.deckapp.core.model.SessionDeckRef
import com.deckapp.feature.draw.SessionUiState
import com.deckapp.feature.draw.model.StoryEngineGuideTab
import com.deckapp.feature.draw.model.StoryEngineProcedureCatalog
import com.deckapp.feature.draw.model.StoryEngineStep
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Delegado encargado de gestionar la guía interactiva paso a paso para The Story Engine / Deck of Worlds.
 * Permite avanzar entre fases narrativas y de cartografía modular, robar directamente del mazo
 * asociado al paso (incorporándolo a la sesión si fuera necesario), rotar opciones (1d2 / 1d4)
 * y acceder al generador de microsettings.
 */
class StoryEngineGuideDelegate(
    private val sessionId: Long,
    private val ui: MutableStateFlow<SessionUiState>,
    private val scope: CoroutineScope,
    private val sessionRepository: SessionRepository,
    private val deckDelegate: SessionDeckDelegate,
    private val openMicrosettingBuilder: () -> Unit,
    private val onCardDrawnForStep: ((StoryEngineGuideTab, Int, Long) -> Unit)? = null
) {
    fun open(tab: StoryEngineGuideTab = ui.value.storyEngineGuideTab) {
        ui.update { it.copy(modals = it.modals.copy(showStoryEngineGuide = true), storyEngineGuideTab = tab) }
    }

    fun close() {
        ui.update { it.copy(modals = it.modals.copy(showStoryEngineGuide = false)) }
    }

    fun selectTab(tab: StoryEngineGuideTab) {
        ui.update { it.copy(storyEngineGuideTab = tab, storyEngineGuideStepIndex = 0) }
    }

    fun selectStep(index: Int) {
        ui.update { it.copy(storyEngineGuideStepIndex = index) }
    }

    fun nextStep() {
        val currentTab = ui.value.storyEngineGuideTab
        val maxIndex = when (currentTab) {
            StoryEngineGuideTab.DECK_OF_WORLDS -> StoryEngineProcedureCatalog.deckOfWorldsSteps.size - 1
            StoryEngineGuideTab.NEW_STORY_ENGINE -> StoryEngineProcedureCatalog.newStoryEngineSteps.size - 1
            StoryEngineGuideTab.EXPANSIONS -> 0
        }
        ui.update { state ->
            val next = (state.storyEngineGuideStepIndex + 1).coerceAtMost(maxIndex)
            state.copy(storyEngineGuideStepIndex = next)
        }
    }

    fun prevStep() {
        ui.update { state ->
            val prev = (state.storyEngineGuideStepIndex - 1).coerceAtLeast(0)
            state.copy(storyEngineGuideStepIndex = prev)
        }
    }

    fun findDeckForStep(step: StoryEngineStep): CardStack? {
        val allDecks = ui.value.allDecks
        val sessionDeckIds = ui.value.deckRefs.map { it.stackId }.toSet()
        val sessionDecks = allDecks.filter { it.id in sessionDeckIds }
        val matchingSessionDeck = sessionDecks.firstOrNull { deck ->
            step.deckQueries.any { q -> deck.name.contains(q, ignoreCase = true) }
        }
        if (matchingSessionDeck != null) return matchingSessionDeck

        return allDecks.firstOrNull { deck ->
            step.deckQueries.any { q -> deck.name.contains(q, ignoreCase = true) }
        }
    }

    fun drawStepCard(step: StoryEngineStep) {
        scope.launch {
            val targetDeck = findDeckForStep(step)
            if (targetDeck == null) {
                ui.update { it.copy(snackbarMessage = "No se encontró mazo para \"${step.title}\". Importa el pack en Ajustes.") }
                return@launch
            }
            val handBefore = ui.value.hand.map { it.id }.toSet()
            val isPresentInSession = ui.value.deckRefs.any { it.stackId == targetDeck.id }
            if (!isPresentInSession) {
                sessionRepository.addDeckToSession(SessionDeckRef(sessionId, targetDeck.id))
            }
            deckDelegate.drawCardFromDeck(targetDeck.id)
            val newHand = ui.value.hand
            val drawnCard = newHand.firstOrNull { it.id !in handBefore }
            if (drawnCard != null && onCardDrawnForStep != null) {
                onCardDrawnForStep.invoke(ui.value.storyEngineGuideTab, step.stepIndex, drawnCard.id)
            }
            ui.update { it.copy(snackbarMessage = "Carta robada de ${targetDeck.name} y colocada en el tapete") }
        }
    }

    fun rotateStepCard(step: StoryEngineStep) {
        val targetDeck = findDeckForStep(step)
        val cardToRotate = if (targetDeck != null) {
            ui.value.hand.lastOrNull { it.stackId == targetDeck.id }
        } else {
            ui.value.hand.lastOrNull()
        }
        if (cardToRotate != null) {
            deckDelegate.rotateCard(cardToRotate.id)
            ui.update { it.copy(snackbarMessage = "Pista rotada en ${cardToRotate.title}") }
        } else {
            ui.update { it.copy(snackbarMessage = "No hay cartas de este paso en tu mano para rotar") }
        }
    }

    fun launchMicrosettingBuilder() {
        openMicrosettingBuilder()
    }
}
