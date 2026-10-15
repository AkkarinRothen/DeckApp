package com.deckapp.core.domain.usecase

import com.deckapp.core.model.*

fun boardInspirationCandidates(
    tableRolls: List<TableRollResult>,
    oracleRolls: List<BoardMythicFateResult>,
    cards: List<Card>,
    deckNames: Map<Long, String>,
    sceneTitle: String
): List<BoardInspiration> {
    val tables = tableRolls.map { roll ->
        BoardInspiration(
            id = "table:${roll.tableId}:${roll.id}:${roll.timestamp}", source = InspirationSource.TABLE,
            title = roll.tableName, text = "[${roll.rollValue}] ${roll.resolvedText}",
            sceneTitle = sceneTitle, timestamp = roll.timestamp
        )
    }
    val oracle = oracleRolls.map { roll ->
        BoardInspiration(
            id = "oracle:${roll.timestamp}:${roll.roll}:${roll.question}", source = InspirationSource.ORACLE,
            title = roll.question.ifBlank { "Consulta al oráculo" }, text = oracleJournalEntry(roll),
            sceneTitle = roll.sceneTitle, timestamp = roll.timestamp
        )
    }
    val cardResults = cards.filter { it.isRevealed }.map { card ->
        val face = card.faces.getOrNull(card.currentFaceIndex) ?: card.faces.firstOrNull()
        val zones = when (face?.contentMode) {
            CardContentMode.REVERSIBLE -> listOfNotNull(face.zones.getOrNull(if (card.isReversed) 1 else 0))
            CardContentMode.FOUR_EDGE_CUES -> listOfNotNull(face.zones.getOrNull(card.currentRotation / 90))
            else -> face?.zones.orEmpty()
        }
        val text = zones.map { it.text }.filter { it.isNotBlank() }.joinToString("\n\n")
        val deck = deckNames[card.originDeckId ?: card.stackId]
        BoardInspiration(
            id = "card:${card.id}:${card.lastDrawnAt}:${card.currentFaceIndex}:${card.currentRotation}:${card.isReversed}",
            source = InspirationSource.CARD, title = card.title,
            text = listOfNotNull(deck?.let { "Mazo: $it" }, text.takeIf { it.isNotBlank() }).joinToString("\n\n").ifBlank { card.title },
            imagePath = if (card.isReversed) face?.reversedImagePath ?: face?.imagePath else face?.imagePath,
            sceneTitle = sceneTitle, timestamp = card.lastDrawnAt ?: 0L
        )
    }
    return (tables + oracle + cardResults).distinctBy { it.id }.sortedByDescending { it.timestamp }
}

fun BoardSessionMemory.captureInspiration(item: BoardInspiration): BoardSessionMemory =
    if (inspiration.any { it.id == item.id }) this else copy(inspiration = listOf(item.copy(status = InspirationStatus.PENDING)) + inspiration)

fun BoardSessionMemory.setInspirationStatus(id: String, status: InspirationStatus): BoardSessionMemory =
    copy(inspiration = inspiration.map { if (it.id == id) it.copy(status = status) else it })

fun BoardInspiration.toDiaryEntry(): String = buildString {
    appendLine("### Inspiración: $title")
    appendLine("Origen: ${when (source) { InspirationSource.TABLE -> "Tabla"; InspirationSource.ORACLE -> "Oráculo"; InspirationSource.CARD -> "Carta" }}")
    if (sceneTitle.isNotBlank()) appendLine("Escena: $sceneTitle")
    append("\n$text")
}
