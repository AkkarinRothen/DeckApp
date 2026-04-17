package com.deckapp.core.domain.usecase

import com.deckapp.core.domain.repository.CardRepository
import com.deckapp.core.domain.repository.TableRepository
import com.deckapp.core.domain.repository.CollectionRepository
import com.deckapp.core.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class GlobalSearchUseCase @Inject constructor(
    private val cardRepository: CardRepository,
    private val tableRepository: TableRepository,
    private val collectionRepository: CollectionRepository
) {
    operator fun invoke(query: String): Flow<List<SearchMatch>> {
        if (query.length < 2) return kotlinx.coroutines.flow.flowOf(emptyList())

        return combine(
            cardRepository.searchCards(query),
            tableRepository.searchTables(query),
            tableRepository.searchEntries(query),
            collectionRepository.searchCollections(query)
        ) { cards, tables, entries, collections ->
            val matches = mutableListOf<SearchMatch>()

            // 1. Colecciones (Baúl) - Más prioridad
            collections.take(10).forEach { col ->
                matches.add(
                    SearchMatch(
                        id = col.id,
                        type = SearchResultType.BAUL,
                        title = col.name,
                        subtitle = "Baúl · ${col.resourceCount} recursos",
                        snippet = col.description.take(60)
                    )
                )
            }

            // 2. Mazos
            cards.take(20).forEach { card ->
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

            // 3. Tablas
            tables.take(20).forEach { table ->
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

            // 4. Entradas de tabla (Agregadas por tabla)
            entries.take(50).forEach { (tableId, text) ->
                matches.add(
                    SearchMatch(
                        id = tableId,
                        type = SearchResultType.TABLE,
                        title = "Coincidencia en tabla",
                        subtitle = "Contenido de tabla",
                        snippet = text.take(100),
                        parentId = tableId
                    )
                )
            }

            matches.distinctBy { "${it.type}_${it.id}" }
        }
    }
}
