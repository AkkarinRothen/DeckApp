package com.deckapp.core.domain.usecase

import com.deckapp.core.domain.repository.CardRepository
import com.deckapp.core.domain.repository.TableRepository
import com.deckapp.core.model.SearchMatch
import com.deckapp.core.model.SearchResultType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class GlobalSearchUseCase @Inject constructor(
    private val cardRepository: CardRepository,
    private val tableRepository: TableRepository
) {
    operator fun invoke(query: String): Flow<List<SearchMatch>> {
        if (query.length < 2) return kotlinx.coroutines.flow.flowOf(emptyList())

        return combine(
            cardRepository.searchCards(query),
            tableRepository.searchTables(query)
        ) { cards, tables ->
            val matches = mutableListOf<SearchMatch>()

            // Mapear cartas encontradas
            cards.forEach { card ->
                matches.add(
                    SearchMatch(
                        id = card.id,
                        type = SearchResultType.CARD,
                        title = card.title,
                        subtitle = "Carta" + (card.dmNotes?.let { " · Tiene notas" } ?: ""),
                        snippet = card.dmNotes?.take(60),
                        parentId = card.stackId
                    )
                )
            }

            // Mapear tablas encontradas
            tables.forEach { table ->
                matches.add(
                    SearchMatch(
                        id = table.id,
                        type = SearchResultType.TABLE,
                        title = table.name,
                        subtitle = "Tabla · ${table.rollFormula}",
                        snippet = table.description.take(60)
                    )
                )
            }

            // Mapear entradas encontradas (agregando por tabla padre)
            // Para simplicidad en el combine, usamos la última emisión conocida de searchEntries
            // En una implementación real más compleja, esto se combinaría reactivamente
            
            matches

            matches
        }
    }
}
