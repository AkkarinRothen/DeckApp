package com.deckapp.core.domain.usecase

import com.deckapp.core.domain.repository.CardRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Baraja las cartas de un mazo.
 * 
 * Implementa una lógica centralizada para asegurar que:
 * 1. El orden sea aleatorio.
 * 2. Se persista el nuevo orden en el repositorio.
 * 3. Se mantenga la integridad de los índices de ordenamiento (sortOrder).
 */
class ShuffleDeckUseCase @Inject constructor(
    private val cardRepository: CardRepository
) {
    /**
     * @param deckId ID del mazo a barajar.
     * @param onlyAvailable Si es true, las cartas ya robadas mantienen su posición relativa
     * y solo se barajan las que quedan en el mazo. Si es false, se baraja todo.
     */
    suspend operator fun invoke(deckId: Long, onlyAvailable: Boolean = true) {
        val allCards = cardRepository.getCardsForStack(deckId).first()
        if (allCards.isEmpty()) return

        val finalOrderedIds = if (onlyAvailable) {
            val drawnCards = allCards.filter { it.isDrawn }.sortedBy { it.sortOrder }
            val availableIds = allCards.filter { !it.isDrawn }.map { it.id }.shuffled()
            
            // Recomponemos: primero las robadas (ya fuera del mazo) y luego las barajadas
            drawnCards.map { it.id } + availableIds
        } else {
            allCards.map { it.id }.shuffled()
        }

        cardRepository.updateCardsSortOrder(finalOrderedIds)
    }
}
