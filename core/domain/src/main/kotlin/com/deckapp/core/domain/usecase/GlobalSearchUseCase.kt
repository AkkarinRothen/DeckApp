package com.deckapp.core.domain.usecase

import com.deckapp.core.domain.repository.*
import com.deckapp.core.model.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class SearchResult(
    val id: Long,
    val title: String,
    val type: GlobalSearchResultType,
    val subtitle: String = "",
    val metadata: String = "",
    val parentId: Long? = null
)

enum class GlobalSearchResultType { CARD, TABLE, RULE, MANUAL, NPC, WIKI }

class GlobalSearchUseCase @Inject constructor(
    private val referenceRepository: ReferenceRepository,
    private val manualRepository: ManualRepository,
    private val npcRepository: NpcRepository,
    private val wikiRepository: WikiRepository,
    private val cardRepository: CardRepository
) {
    operator fun invoke(query: String): Flow<List<SearchResult>> {
        if (query.isBlank()) return flowOf(emptyList())

        val ftsQuery = query.toFtsPrefixQuery()
        val tablesFlow = if (ftsQuery.isBlank()) flowOf(emptyList()) else referenceRepository.searchReferenceTables(ftsQuery)
        val rulesFlow = if (ftsQuery.isBlank()) flowOf(emptyList()) else referenceRepository.searchSystemRules(ftsQuery)
        val cardsFlow = if (ftsQuery.isBlank()) flowOf(emptyList()) else cardRepository.searchCards(query)
        val manualsFlow = manualRepository.getAllManuals().map { list -> 
            list.filter { it.title.contains(query, ignoreCase = true) } 
        }
        val npcsFlow = npcRepository.getAllNpcs().map { list -> 
            list.filter { it.name.contains(query, ignoreCase = true) } 
        }
        val wikiFlow = wikiRepository.searchEntries(query)

        val indexedFlow = combine(cardsFlow, tablesFlow, rulesFlow) { cards, tables, rules ->
            Triple(cards, tables, rules)
        }
        return combine(indexedFlow, manualsFlow, npcsFlow, wikiFlow) { indexed, manuals, npcs, wikiEntries ->
            val (cards, tables, rules) = indexed
            val results = mutableListOf<SearchResult>()
            
            cards.forEach { results.add(SearchResult(it.id, it.title, GlobalSearchResultType.CARD, "Carta", parentId = it.stackId)) }
            tables.forEach { results.add(SearchResult(it.id, it.name, GlobalSearchResultType.TABLE, it.gameSystem)) }
            rules.forEach { results.add(SearchResult(it.id, it.title, GlobalSearchResultType.RULE, it.gameSystem)) }
            manuals.forEach { results.add(SearchResult(it.id, it.title, GlobalSearchResultType.MANUAL, it.gameSystem)) }
            npcs.forEach { results.add(SearchResult(it.id, it.name, GlobalSearchResultType.NPC, it.description.take(50))) }
            wikiEntries.forEach { results.add(SearchResult(it.id, it.title, GlobalSearchResultType.WIKI, "Entrada de Wiki")) }
            
            results.distinctBy { "${it.type}:${it.id}" }
                .sortedBy { it.title.lowercase() }
                .take(100)
        }
    }

    private fun String.toFtsPrefixQuery(): String =
        split(Regex("\\s+")).mapNotNull { token ->
            token.filter { it.isLetterOrDigit() || it == '_' }
                .takeIf { it.isNotEmpty() }
                ?.plus("*")
        }.joinToString(" ")
}
