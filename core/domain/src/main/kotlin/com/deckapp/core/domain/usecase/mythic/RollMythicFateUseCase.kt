package com.deckapp.core.domain.usecase.mythic

import com.deckapp.core.domain.repository.MythicRepository
import com.deckapp.core.domain.usecase.RollTableUseCase
import com.deckapp.core.model.*
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import kotlin.random.Random

/**
 * Realiza una tirada al Oráculo Mythic, guarda el resultado y resuelve eventos si aplica.
 */
class RollMythicFateUseCase @Inject constructor(
    private val mythicRepository: MythicRepository,
    private val fateCheckUseCase: FateCheckUseCase,
    private val rollTableUseCase: RollTableUseCase
) {

    suspend operator fun invoke(
        sessionId: Long,
        question: String,
        probability: ProbabilityLevel
    ): MythicRoll {
        val session = mythicRepository.getSessionById(sessionId).first() ?: throw IllegalArgumentException("Sesión no encontrada")
        
        val rollValue = Random.nextInt(1, 101)
        val (fateResult, isRandomEvent) = fateCheckUseCase(probability, session.chaosFactor, rollValue)

        var eventFocus = ""
        var eventAction = ""
        var eventSubject = ""

        if (isRandomEvent) {
            // 1. Determinar el Foco
            val focusRoll = Random.nextInt(1, 101)
            var focusLabel = MythicReferenceData.getEventFocus(focusRoll)
            
            // 2. Integración con listas (NPCs e Hilos)
            when {
                focusLabel.contains("NPC", ignoreCase = true) -> {
                    val characters = mythicRepository.getCharacters(sessionId).first()
                    if (characters.isNotEmpty()) {
                        val chosenNpc = characters.random()
                        focusLabel += " (${chosenNpc.name})"
                    }
                }
                focusLabel.contains("Hilo", ignoreCase = true) || focusLabel.contains("Thread", ignoreCase = true) -> {
                    val threads = mythicRepository.getThreads(sessionId).first().filter { !it.isResolved }
                    if (threads.isNotEmpty()) {
                        val chosenThread = threads.random()
                        focusLabel += " (${chosenThread.description})"
                    }
                }
            }
            eventFocus = focusLabel

            // 3. Determinar Acción y Sujeto
            val actionId = session.actionTableId
            eventAction = if (actionId != null) {
                rollTableUseCase(actionId, null).resolvedText
            } else {
                MythicReferenceData.actionKeywords.random()
            }
            
            val subjectId = session.subjectTableId
            eventSubject = if (subjectId != null) {
                rollTableUseCase(subjectId, null).resolvedText
            } else {
                MythicReferenceData.subjectKeywords.random()
            }
        }

        val mythicRoll = MythicRoll(
            sessionId = sessionId,
            question = question,
            probability = probability,
            chaosFactor = session.chaosFactor,
            roll = rollValue,
            result = fateResult,
            isRandomEvent = isRandomEvent,
            eventFocus = eventFocus,
            eventAction = eventAction,
            eventSubject = eventSubject,
            sceneNumber = session.sceneNumber
        )

        val id = mythicRepository.saveRoll(mythicRoll)
        return mythicRoll.copy(id = id)
    }
}
