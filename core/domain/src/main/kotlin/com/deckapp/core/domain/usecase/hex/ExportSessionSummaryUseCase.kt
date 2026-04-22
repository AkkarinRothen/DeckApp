package com.deckapp.core.domain.usecase.hex

import com.deckapp.core.model.HexActivityType
import com.deckapp.core.model.HexDay
import javax.inject.Inject

class ExportSessionSummaryUseCase @Inject constructor() {
    operator fun invoke(mapName: String, days: List<HexDay>): String = buildString {
        appendLine("# Diario de $mapName")
        appendLine()
        days.sortedBy { it.dayNumber }.forEach { day ->
            appendLine("## Día ${day.dayNumber}")
            day.activitiesLog.forEach { entry ->
                val icon = when (entry.type) {
                    HexActivityType.TRAVEL       -> "🚶"
                    HexActivityType.EXPLORE      -> "🗺"
                    HexActivityType.RECONNOITER  -> "🔍"
                    HexActivityType.MAP_AREA     -> "📜"
                    HexActivityType.FORTIFY_CAMP -> "⛺"
                    HexActivityType.CUSTOM       -> "•"
                }
                appendLine("- $icon ${entry.description}")
            }
            if (day.notes.isNotBlank()) {
                appendLine()
                appendLine("*${day.notes}*")
            }
            appendLine()
        }
    }
}
