package com.deckapp.feature.draw.components

import com.deckapp.core.model.Encounter
import com.deckapp.core.model.SessionBoardItemType
import com.deckapp.core.model.TableRollResult
import com.deckapp.core.model.TensionClock

internal fun boardBlockSummary(
    type: SessionBoardItemType,
    meta: BoardBlockMeta,
    latestTableRoll: TableRollResult?,
    clock: TensionClock?,
    encounter: Encounter?
): String = when (type) {
    SessionBoardItemType.TABLE -> latestTableRoll?.let { "Último: ${it.resolvedText}" }
        ?: "Sin tiradas recientes"
    SessionBoardItemType.CLOCK -> clock?.let {
        "${it.progress}/${it.segments} segmentos" + if (it.isCompleted) " · Completado" else ""
    } ?: "Sin reloj vinculado"
    SessionBoardItemType.COMBAT -> encounter?.let {
        "${it.name} · Ronda ${it.currentRound} · Turno ${it.currentTurnIndex + 1}"
    } ?: "Sin combate activo"
    SessionBoardItemType.NPCS -> meta.footnote
    SessionBoardItemType.MAP -> meta.footnote
    else -> meta.preview
}.lineSequence().filter { it.isNotBlank() }.joinToString(" · ")
