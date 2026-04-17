package com.deckapp.core.domain.usecase

import com.deckapp.core.domain.repository.TableRepository
import javax.inject.Inject

/**
 * Invierte el texto de las entradas de una tabla manteniendo los rangos de dados intactos.
 *
 * Ejemplo:
 *   1-3: Goblin    →   1-3: Dragon
 *   4-6: Zombie    →   4-6: Zombie
 *   7-9: Dragon    →   7-9: Goblin
 *
 * Los rangos (minRoll/maxRoll) y su orden no cambian — solo se permutan los textos
 * de forma que el resultado más bajo del dado produce el resultado que antes
 * correspondía al más alto, y viceversa.
 *
 * Caso de uso TTRPG habitual: una tabla de peligro ordenada "fácil → difícil"
 * se convierte en "difícil → fácil" sin reescribir las entradas manualmente.
 *
 * Si la tabla tiene menos de 2 entradas no se hace ningún cambio.
 */
class InvertTableRangesUseCase @Inject constructor(
    private val tableRepository: TableRepository
) {
    suspend operator fun invoke(tableId: Long) {
        val table = tableRepository.getTableWithEntries(tableId) ?: return
        val entries = table.entries
        if (entries.size < 2) return

        val sortedEntries = entries.sortedBy { it.minRoll }
        val reversedTexts = sortedEntries.map { it.text }.reversed()

        val invertedEntries = sortedEntries.mapIndexed { index, entry ->
            entry.copy(text = reversedTexts[index])
        }

        tableRepository.saveTable(table.copy(entries = invertedEntries))
    }
}
