package com.deckapp.feature.draw.scene

import com.deckapp.core.domain.usecase.mythic.MythicReferenceData
import com.deckapp.core.domain.usecase.mythic.SceneCheckResult
import kotlin.random.Random

/**
 * Resultado evaluado del chequeo de escena Mythic (d10 vs Caos).
 */
data class SceneCheckOutcome(
    val chaosFactor: Int,
    val roll: Int,
    val result: SceneCheckResult,
    val alterationSuggestion: String? = null,
    val eventFocus: String? = null,
    val eventMeaning: String? = null
) {
    val displayResult: String
        get() = when (result) {
            SceneCheckResult.NORMAL -> "NORMAL"
            SceneCheckResult.ALTERED -> "ALTERADA"
            SceneCheckResult.INTERRUPTED -> "INTERRUMPIDA"
        }

    val summaryExplanation: String
        get() = when (result) {
            SceneCheckResult.NORMAL -> "La escena comienza tal como fue planeada."
            SceneCheckResult.ALTERED -> alterationSuggestion ?: "La situación se altera o intensifica."
            SceneCheckResult.INTERRUPTED -> "¡Escena Interrumpida! $eventFocus: $eventMeaning"
        }
}

val ALTERATION_PRESETS = listOf(
    "Giro de Urgencia: La situación apremia o el tiempo límite se reduce drásticamente.",
    "Giro de Entorno: El clima, terreno o visibilidad se vuelven adversos de improvisto.",
    "Giro de Presencia: Un PNJ inesperado o una patrulla enemiga irrumpe en el lugar.",
    "Giro de Obstáculo: El acceso principal está bloqueado o fuertemente custodiado.",
    "Giro de Vínculo: Un objeto clave, pista o elemento de la escena falta o fue cambiado."
)

fun evaluateSceneCheck(chaosFactor: Int, forcedRoll: Int? = null): SceneCheckOutcome {
    val roll = forcedRoll ?: Random.nextInt(1, 11)
    val result = when {
        roll > chaosFactor -> SceneCheckResult.NORMAL
        roll % 2 == 0 -> SceneCheckResult.ALTERED
        else -> SceneCheckResult.INTERRUPTED
    }

    val alteration = if (result == SceneCheckResult.ALTERED) {
        ALTERATION_PRESETS.random()
    } else null

    val (focus, meaning) = if (result == SceneCheckResult.INTERRUPTED) {
        val focusRoll = Random.nextInt(1, 101)
        val f = MythicReferenceData.getEventFocus(focusRoll)
        val action = MythicReferenceData.actionKeywords.random()
        val subject = MythicReferenceData.subjectKeywords.random()
        f to "$action · $subject"
    } else (null to null)

    return SceneCheckOutcome(
        chaosFactor = chaosFactor,
        roll = roll,
        result = result,
        alterationSuggestion = alteration,
        eventFocus = focus,
        eventMeaning = meaning
    )
}

/**
 * Borrador para el asistente de encuadre de escenas.
 */
data class SceneFramingDraft(
    val title: String,
    val location: String = "",
    val objective: String = "",
    val expectation: String = "",
    val adjustedSituation: String = "",
    val checkOutcome: SceneCheckOutcome? = null,
    val linkedNpcId: Long? = null,
    val linkedTableId: Long? = null,
    val linkedDeckId: Long? = null
)

/**
 * Da formato de entrada en Markdown para el Diario de Sesión.
 */
fun buildFramedSceneJournalEntry(
    sceneNumber: Int,
    draft: SceneFramingDraft
): String = buildString {
    appendLine("## 🎬 Escena $sceneNumber: ${draft.title.trim()}")
    
    draft.checkOutcome?.let { check ->
        appendLine("> **Chequeo de Caos:** Caos ${check.chaosFactor} · d10: ${check.roll} → **${check.displayResult}**")
        when (check.result) {
            SceneCheckResult.NORMAL -> appendLine("> *La escena comienza según lo planeado.*")
            SceneCheckResult.ALTERED -> appendLine("> **Giro:** ${check.alterationSuggestion}")
            SceneCheckResult.INTERRUPTED -> appendLine("> **Evento:** ${check.eventFocus} · *${check.eventMeaning}*")
        }
    }

    if (draft.location.isNotBlank()) {
        appendLine("\n### Lugar\n${draft.location.trim()}")
    }
    if (draft.objective.isNotBlank()) {
        appendLine("\n### Objetivo\n${draft.objective.trim()}")
    }
    val situation = draft.adjustedSituation.ifBlank { draft.expectation }
    if (situation.isNotBlank()) {
        appendLine("\n### Situación Inicial\n${situation.trim()}")
    }
}
