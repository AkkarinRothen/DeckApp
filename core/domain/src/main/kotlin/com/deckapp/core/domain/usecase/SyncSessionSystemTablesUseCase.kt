package com.deckapp.core.domain.usecase

import com.deckapp.core.domain.repository.ReferenceRepository
import com.deckapp.core.domain.repository.SessionRepository
import com.deckapp.core.domain.repository.TableRepository
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

/** Adds installed system tables without removing resources selected by the user. */
class SyncSessionSystemTablesUseCase @Inject constructor(
    private val sessions: SessionRepository,
    private val tables: TableRepository,
    private val references: ReferenceRepository
) {
    operator fun invoke(sessionId: Long) = combine(
        sessions.getSessionById(sessionId).map { it?.gameSystems.orEmpty().filterNot { system -> system == "General" }.toSet() }.distinctUntilChanged(),
        references.getAllSystemRules(),
        references.getAllReferenceTables(),
        tables.getAllTables()
    ) { systems, rules, referenceTables, availableTables ->
        // Pack identifiers are asset filenames; system names are human-readable labels.
        val packs = (rules.filter { it.gameSystem in systems }.mapNotNull { it.sourcePack } +
            referenceTables.filter { it.gameSystem in systems }.mapNotNull { it.sourcePack })
            .filter { it.isNotBlank() }.toSet()
        val isDnd = systems.any { it.contains("D&D", ignoreCase = true) || it.contains("5e", ignoreCase = true) }
        availableTables.filter { table ->
            val sp = table.sourcePack.orEmpty()
            sp in packs || (isDnd && (sp.startsWith("tables_dnd_") || sp.contains("dnd")))
        }.map { it.id }.distinct().sorted()
    }.distinctUntilChanged().onEach { tableIds ->
        val existing = sessions.getTablesForSession(sessionId).first().map { it.id }.toSet()
        tableIds.filterNot { it in existing }.forEach { sessions.addTableToSession(sessionId, it) }
    }
}
