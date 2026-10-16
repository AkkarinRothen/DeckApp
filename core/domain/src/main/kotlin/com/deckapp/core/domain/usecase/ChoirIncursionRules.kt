package com.deckapp.core.domain.usecase

import com.deckapp.core.model.*

/**
 * Reglas canónicas del Tablero de Incursión y Gestión del Asentamiento (Choir of Flesh).
 */
object ChoirIncursionRules {

    fun advance(incursion: ChoirIncursion): ChoirIncursion {
        if (incursion.isFinished) return incursion
        val nextIndex = (incursion.currentNodeIndex + 1).coerceAtMost(8)
        if (nextIndex == incursion.currentNodeIndex) return incursion

        val updatedNodes = incursion.nodes.map { node ->
            when (node.index) {
                incursion.currentNodeIndex -> {
                    if (node.state == ChoirNodeState.CURRENT) node.copy(state = ChoirNodeState.CLEARED)
                    else node
                }
                nextIndex -> node.copy(state = ChoirNodeState.CURRENT)
                else -> node
            }
        }

        val targetNode = updatedNodes.find { it.index == nextIndex }
        val addedSong = targetNode?.songDelta ?: 1
        val newSong = (incursion.songLevel + addedSong).coerceIn(0, 10)

        return incursion.copy(
            currentNodeIndex = nextIndex,
            songLevel = newSong,
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

        val isBossFinal = incursion.currentNodeIndex == 8
        val newStatus = if (isBossFinal) ChoirIncursionStatus.EXTRACTED_SUCCESS else incursion.status

        return incursion.copy(
            nodes = updatedNodes,
            lootCollected = newLoot,
            status = newStatus
        )
    }

    fun fleeCurrentNode(incursion: ChoirIncursion): ChoirIncursion {
        if (incursion.isFinished) return incursion
        val updatedNodes = incursion.nodes.map { node ->
            if (node.index == incursion.currentNodeIndex) {
                node.copy(state = ChoirNodeState.FLED)
            } else node
        }
        return incursion.copy(
            nodes = updatedNodes,
            songLevel = (incursion.songLevel + 1).coerceAtMost(10)
        )
    }

    fun extractToSettlement(
        incursion: ChoirIncursion,
        settlement: ChoirSettlement
    ): Pair<ChoirIncursion, ChoirSettlement> {
        val extractedIncursion = incursion.copy(status = ChoirIncursionStatus.EXTRACTED_SUCCESS)
        val logEntry = "Incursión '${incursion.title}' finalizada con éxito. Nodos explorados: ${incursion.currentNodeIndex}/8. Botín: ${if (incursion.lootCollected.isBlank()) "Ninguno" else incursion.lootCollected}."

        val updatedSettlement = settlement.copy(
            morale = (settlement.morale + 1).coerceAtMost(10),
            log = settlement.log + logEntry
        )
        return extractedIncursion to updatedSettlement
    }

    fun restShort(
        character: ChoirCharacter,
        settlement: ChoirSettlement? = null
    ): Pair<ChoirCharacter, ChoirSettlement?> {
        val maxVigor = ChoirCharacterRules.vigorMaximum(character)
        val hasRations = character.rations > 0 || (settlement != null && settlement.stockRations > 0)

        val updatedChar = if (character.rations > 0) {
            character.copy(rations = character.rations - 1, vigor = maxVigor)
        } else {
            character.copy(vigor = maxVigor)
        }

        val updatedSettlement = if (character.rations == 0 && settlement != null && settlement.stockRations > 0) {
            settlement.copy(stockRations = settlement.stockRations - 1)
        } else settlement

        return updatedChar to updatedSettlement
    }

    fun restLong(
        character: ChoirCharacter,
        settlement: ChoirSettlement,
        treatmentSuccess: Boolean = true
    ): Pair<ChoirCharacter, ChoirSettlement> {
        val maxVigor = ChoirCharacterRules.vigorMaximum(character)
        val rationsToSpend = 2.coerceAtMost(settlement.stockRations)
        val medicalToSpend = 1.coerceAtMost(settlement.stockMedicalSupplies)

        val updatedSettlement = settlement.copy(
            stockRations = settlement.stockRations - rationsToSpend,
            stockMedicalSupplies = settlement.stockMedicalSupplies - medicalToSpend
        )

        val newAnguish = (character.anguish - 2).coerceAtLeast(0)
        val newTrauma = if (treatmentSuccess && character.trauma > 0) character.trauma - 1 else character.trauma

        val updatedChar = character.copy(
            vigor = maxVigor,
            anguish = newAnguish,
            trauma = newTrauma,
            major = 0,
            minor = 0
        )

        return updatedChar to updatedSettlement
    }
}
