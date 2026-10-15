package com.deckapp.core.domain.usecase

import com.deckapp.core.domain.repository.SessionRepository
import com.deckapp.core.model.*
import javax.inject.Inject

private val sceneSections = listOf("Lugar", "Objetivo", "Situación", "Pregunta pendiente")

fun SoloSceneContext.toSceneContent(): String = sceneSections.zip(
    listOf(location, objective, situation, pendingQuestion)
).joinToString("\n\n") { (label, value) -> "### $label\n${value.trim()}" }

fun Scene.soloContext(): SoloSceneContext {
    // Free-form planner scenes remain readable and are preserved as the situation.
    if (!content.startsWith("### Lugar\n")) return SoloSceneContext(situation = content)
    val sections = Regex("(?m)^### (Lugar|Objetivo|Situación|Pregunta pendiente)$").findAll(content).toList()
    val values = sections.mapIndexed { index, match ->
        match.groupValues[1] to content.substring(match.range.last + 1,
            sections.getOrNull(index + 1)?.range?.first ?: content.length).trim()
    }.toMap()
    return SoloSceneContext(values["Lugar"].orEmpty(), values["Objetivo"].orEmpty(),
        values["Situación"].orEmpty(), values["Pregunta pendiente"].orEmpty())
}

fun oracleJournalEntry(result: BoardMythicFateResult): String = buildString {
    if (result.sceneTitle.isNotBlank()) appendLine("> **Escena:** ${result.sceneTitle}")
    if (result.question.isNotBlank()) appendLine("> **Pregunta:** ${result.question}")
    append("> **Oráculo:** ${result.answerLabel} · ${result.odds.displayLabel()} · Caos ${result.chaos} · d100 ${result.roll}/${result.targetThreshold}")
    result.randomEventDescription?.let { append("\n> **Evento:** $it") }
}

fun ProbabilityLevel.displayLabel(): String = when (this) {
    ProbabilityLevel.CERTAIN -> "Seguro"
    ProbabilityLevel.NEARLY_CERTAIN -> "Casi seguro"
    ProbabilityLevel.VERY_LIKELY -> "Muy probable"
    ProbabilityLevel.LIKELY -> "Probable"
    ProbabilityLevel.FIFTY_FIFTY -> "50/50"
    ProbabilityLevel.UNLIKELY -> "Improbable"
    ProbabilityLevel.VERY_UNLIKELY -> "Muy improbable"
    ProbabilityLevel.NEARLY_IMPOSSIBLE -> "Casi imposible"
    ProbabilityLevel.IMPOSSIBLE -> "Imposible"
    ProbabilityLevel.NARRATIVE -> "Narrativo"
}

data class ClosedSoloScene(val journalEntry: String, val nextSceneId: Long?)

class CloseSoloSceneUseCase @Inject constructor(private val repository: SessionRepository) {
    suspend operator fun invoke(scene: Scene, closure: SoloSceneClosure): ClosedSoloScene {
        require(scene.id > 0 && !scene.isCompleted) { "La escena ya está cerrada o todavía no se guardó." }
        require(closure.summary.isNotBlank()) { "Escribí un resumen para cerrar la escena." }
        require(closure.chaosDelta in -1..1) { "El ajuste de caos debe estar entre -1 y 1." }
        val entry = buildString {
            appendLine("## Escena cerrada: ${scene.title}")
            if (scene.content.isNotBlank()) appendLine("\n${scene.content.trim()}\n")
            appendLine("### Resumen\n${closure.summary.trim()}")
            if (closure.consequences.isNotBlank()) appendLine("\n### Consecuencias\n${closure.consequences.trim()}")
            if (closure.pending.isNotBlank()) appendLine("\n### Pendientes\n${closure.pending.trim()}")
        }.trim()
        val next = closure.nextTitle.trim().takeIf { it.isNotEmpty() }?.let { title ->
            Scene(sessionId = scene.sessionId, title = title, sortOrder = scene.sortOrder + 1,
                content = SoloSceneContext(pendingQuestion = closure.pending.trim()).toSceneContent())
        }
        return ClosedSoloScene(entry, repository.closeSceneAndAppendJournal(scene.id, scene.sessionId, entry, next))
    }
}
