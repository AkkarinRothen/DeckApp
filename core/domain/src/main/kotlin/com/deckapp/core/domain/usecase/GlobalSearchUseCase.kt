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
    val metadata: String = ""
)

enum class GlobalSearchResultType { TABLE, RULE, MANUAL, NPC, WIKI }

class GlobalSearchUseCase @Inject constructor(
    private val referenceRepository: ReferenceRepository,
    private val manualRepository: ManualRepository,
    private val npcRepository: NpcRepository,
    private val wikiRepository: WikiRepository
) {
    operator fun invoke(query: String): Flow<List<SearchResult>> {
        if (query.isBlank()) return flowOf(emptyList())

        val tablesFlow = referenceRepository.searchReferenceTables(query)
        val rulesFlow = referenceRepository.searchSystemRules(query)
        val manualsFlow = manualRepository.getAllManuals().map { list -> 
            list.filter { it.title.contains(query, ignoreCase = true) } 
        }
        val npcsFlow = npcRepository.getAllNpcs().map { list -> 
            list.filter { it.name.contains(query, ignoreCase = true) } 
        }
        val wikiFlow = wikiRepository.searchEntries(query)

        return combine(
            tablesFlow,
            rulesFlow,
            manualsFlow,
            npcsFlow,
            wikiFlow
        ) { tables, rules, manuals, npcs, wikiEntries ->
            val results = mutableListOf<SearchResult>()
            
            tables.forEach { results.add(SearchResult(it.id, it.name, GlobalSearchResultType.TABLE, it.gameSystem)) }
            rules.forEach { results.add(SearchResult(it.id, it.title, GlobalSearchResultType.RULE, it.gameSystem)) }
            manuals.forEach { results.add(SearchResult(it.id, it.title, GlobalSearchResultType.MANUAL, it.gameSystem)) }
            npcs.forEach { results.add(SearchResult(it.id, it.name, GlobalSearchResultType.NPC, it.description.take(50))) }
            wikiEntries.forEach { results.add(SearchResult(it.id, it.title, GlobalSearchResultType.WIKI, "Entrada de Wiki")) }
            
            results.sortedBy { it.title }
        }
    }
}
