package com.deckapp.core.domain.usecase

import com.deckapp.core.model.*

/**
 * Reglas canónicas del Tablero de Incursión y Gestión del Asentamiento (Choir of Flesh).
 */
object ChoirIncursionRules {
    /** Conexiones impresas en Incursion Grid_clean.pdf. */
    private val paths = mapOf(
        0 to setOf(1, 3), 1 to setOf(0, 2, 4), 2 to setOf(1, 5),
        3 to setOf(0, 4, 6), 4 to setOf(1, 3, 5, 7), 5 to setOf(2, 4, 8),
        6 to setOf(3, 7), 7 to setOf(4, 6, 8), 8 to setOf(5, 7)
    )

    fun destinations(incursion: ChoirIncursion): List<Int> =
        if (incursion.isFinished) emptyList() else paths[incursion.currentNodeIndex].orEmpty()
            .filter { target -> incursion.nodes.any { it.index == target } }.sorted()

    fun advance(incursion: ChoirIncursion): ChoirIncursion =
        destinations(incursion).firstOrNull()?.let { advance(incursion, it) } ?: incursion

    fun advance(incursion: ChoirIncursion, destination: Int): ChoirIncursion {
        if (incursion.isFinished) return incursion
        if (destination !in destinations(incursion)) return incursion

        val updatedNodes = incursion.nodes.map { node ->
            when (node.index) {
                incursion.currentNodeIndex -> {
                    if (node.state == ChoirNodeState.CURRENT) node.copy(state = ChoirNodeState.CLEARED)
                    else node
                }
                destination -> node.copy(state = ChoirNodeState.CURRENT)
                else -> node
            }
        }

        return incursion.copy(
            currentNodeIndex = destination,
            nodes = updatedNodes
        )
    }

    fun clearCurrentNode(incursion: ChoirIncursion, lootFound: String = ""): ChoirIncursion {
        if (incursion.isFinished) return incursion
        val updatedNodes = incursion.nodes.map { node ->
            if (node.index == incursion.currentNodeIndex) {
                node.copy(
                    state = ChoirNodeState.CLEARED,
                    lootFound = if (lootFound.isNotBlank()) lootFound else node.lootFound
                )
            } else node
        }

        val newLoot = if (lootFound.isNotBlank()) {
            if (incursion.lootCollected.isBlank()) lootFound.trim()
            else "${incursion.lootCollected.trim()}, ${lootFound.trim()}"
        } else incursion.lootCollected

        return incursion.copy(
            nodes = updatedNodes,
            lootCollected = newLoot
        )
    }

    fun fleeCurrentNode(incursion: ChoirIncursion): ChoirIncursion {
        if (incursion.isFinished) return incursion
        val updatedNodes = incursion.nodes.map { node ->
            if (node.index == incursion.currentNodeIndex) {
                node.copy(state = ChoirNodeState.FLED)
            } else node
        }
        return incursion.copy(nodes = updatedNodes)
    }

    fun extractToSettlement(
        incursion: ChoirIncursion,
        settlement: ChoirSettlement
    ): Pair<ChoirIncursion, ChoirSettlement> {
        val extractedIncursion = incursion.copy(status = ChoirIncursionStatus.EXTRACTED_SUCCESS)
        val logEntry = "Incursión '${incursion.title}' finalizada con éxito. Nodos explorados: ${incursion.currentNodeIndex}/8. Botín: ${if (incursion.lootCollected.isBlank()) "Ninguno" else incursion.lootCollected}."

        val updatedSettlement = settlement.copy(log = settlement.log + logEntry)
        return extractedIncursion to updatedSettlement
    }

    fun restShort(
        character: ChoirCharacter,
        settlement: ChoirSettlement? = null
    ): Pair<ChoirCharacter, ChoirSettlement?> {
        val maxVigor = ChoirCharacterRules.vigorMaximum(character)
        return character.copy(vigor = maxVigor) to settlement
    }

}
