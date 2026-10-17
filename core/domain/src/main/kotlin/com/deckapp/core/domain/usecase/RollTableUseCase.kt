package com.deckapp.core.domain.usecase

import com.deckapp.core.domain.repository.TableRepository
import com.deckapp.core.model.RandomTable
import com.deckapp.core.model.TableEntry
import com.deckapp.core.model.TableRollMode
import com.deckapp.core.model.TableRollResult
import javax.inject.Inject
import kotlin.random.Random

/**
 * Tira una tabla aleatoria y persiste el resultado.
 *
 * Soporta:
 * - Fórmulas de dado: "1d6", "2d8+3", "1d100"
 * - Dados inline en el texto de la entrada: "[1d4+1] bandidos"
 * - Sub-tabla de 1 nivel: "@NombreDeTabla" reemplazado por el resultado de esa tabla
 * - Modos: RANGE (rango), WEIGHTED (peso), SEQUENTIAL (siguiente entrada)
 * - isNoRepeat: evita repetir el último resultado en la sesión
 */
class RollTableUseCase @Inject constructor(
    private val tableRepository: TableRepository
) {
    /** Regex para dados inline: [1d6], [2d8+3], [1d4-1] */
    private val inlineDiceRegex = Regex("""\[(\d+)d(\d+)([+\-]\d+)?]""")

    /** Regex para sub-tabla: @NombreTabla */
    private val subTableRegex = Regex("""@([\w\s\u00C0-\u024F]+)""")

    suspend operator fun invoke(
        tableId: Long,
        sessionId: Long?,
        depth: Int = 0,
        persistResult: Boolean = true
    ): TableRollResult {
        val table = tableRepository.getTableWithEntries(tableId)
            ?: return TableRollResult(
                tableId = tableId,
                tableName = "Tabla desconocida",
                sessionId = sessionId,
                rollValue = 0,
                resolvedText = "[tabla no encontrada]"
            )
        
        return roll(table, sessionId, depth, persistResult)
    }

    /**
     * Tira sobre una tabla ya cargada. Útil para previsualizaciones o procesos por lotes.
     */
    suspend fun roll(
        table: RandomTable,
        sessionId: Long?,
        depth: Int = 0,
        persistResult: Boolean = true
    ): TableRollResult {
        if (depth > 4) {
            return TableRollResult(
                tableId = table.id,
                tableName = table.name,
                sessionId = sessionId,
                rollValue = 0,
                resolvedText = "[máx. anidado alcanzado]"
            )
        }

        if (table.rollMode == TableRollMode.MACRO) {
            // Lógica MACRO: tirar en TODAS las entradas (que deben ser sub-tablas)
            val results = table.entries.sortedBy { it.sortOrder }.map { entry ->
                val subTableId = entry.subTableId
                val entryText = if (subTableId != null) {
                    val subRes = invoke(subTableId, sessionId, depth + 1, persistResult = false)
                    subRes.resolvedText
                } else {
                    resolveText(entry.text, sessionId, depth)
                }
                "- $entryText"
            }
            
            val result = TableRollResult(
                tableId = table.id,
                tableName = table.name,
                sessionId = sessionId,
                rollValue = 0,
                resolvedText = results.joinToString("\n")
            )
            
            if (persistResult && table.id > 0) {
                tableRepository.saveRollResult(result)
            }
            return result
        }

        val (entry, rolledRangeValue) = pickEntry(table, sessionId)
        val rollValue = if (table.rollMode == TableRollMode.RANGE) {
            rolledRangeValue ?: entry?.minRoll ?: 0
        } else {
            DiceEvaluator.evaluate(table.rollFormula)
        }
        
        // 1. Resolver texto base
        var resolvedText = resolveText(entry?.text ?: "[sin entrada]", sessionId, depth)

        // 2. Resolver sub-tabla vinculada por ID
        entry?.subTableId?.let { id ->
            val subResult = invoke(id, sessionId, depth + 1, persistResult = false)
            resolvedText = "$resolvedText → ${subResult.resolvedText}"
        }

        val result = TableRollResult(
            tableId = table.id,
            tableName = table.name,
            sessionId = sessionId,
            rollValue = rollValue,
            resolvedText = resolvedText,
            selectedEntryIndex = entry?.let(table.entries::indexOf)?.takeIf { it >= 0 }
        )
        
        if (persistResult && table.id > 0) {
            tableRepository.saveRollResult(result)
        }
        
        return result
    }

    /** Record a die rolled at the table by the player, without rolling the parent table again. */
    suspend fun selectEntry(tableId: Long, entryIndex: Int, sessionId: Long,
                            dieValue: Int? = null): TableRollResult? {
        val table = tableRepository.getTableWithEntries(tableId) ?: return null
        val entry = table.entries.getOrNull(entryIndex) ?: return null
        if (table.rollMode == TableRollMode.RANGE && dieValue != null && dieValue !in entry.minRoll..entry.maxRoll)
            return null
        var resolvedText = resolveText(entry.text, sessionId, 0)
        entry.subTableId?.let { id ->
            val subResult = invoke(id, sessionId, 1, persistResult = false)
            resolvedText = "$resolvedText → ${subResult.resolvedText}"
        }
        val result = TableRollResult(tableId = table.id, tableName = table.name,
            sessionId = sessionId,
            rollValue = if (table.rollMode == TableRollMode.RANGE) dieValue ?: entry.minRoll else entryIndex + 1,
            resolvedText = resolvedText, selectedEntryIndex = entryIndex)
        if (table.id > 0) tableRepository.saveRollResult(result)
        return result
    }

    // ── Selección de entrada ──────────────────────────────────────────────────

    private suspend fun pickEntry(table: RandomTable, sessionId: Long?): Pair<TableEntry?, Int?> {
        if (table.entries.isEmpty()) return null to null

        val lastResults = if (table.isNoRepeat && sessionId != null) {
            tableRepository.getRecentResultsForTable(sessionId, table.id, 5)
        } else emptyList()
        val lastTexts = lastResults.map { it.resolvedText }.toSet()

        return when (table.rollMode) {
            TableRollMode.RANGE -> {
                // En modo rango, el dado manda. Si sale repetido, re-tiramos hasta un límite.
                var entry: TableEntry? = null
                var rolledValue: Int? = null
                repeat(10) {
                    val roll = DiceEvaluator.evaluate(table.rollFormula)
                    rolledValue = roll
                    entry = table.entries.find { roll in it.minRoll..it.maxRoll }
                    if (!table.isNoRepeat || entry == null || entry?.text !in lastTexts) {
                        return entry to roll
                    }
                }
                (entry ?: table.entries.randomOrNull()) to rolledValue
            }
            TableRollMode.WEIGHTED -> {
                val availableEntries = if (table.isNoRepeat) {
                    table.entries.filter { it.text !in lastTexts }.ifEmpty { table.entries }
                } else {
                    table.entries
                }

                val totalWeight = availableEntries.sumOf { it.weight }.coerceAtLeast(1)
                var pick = Random.nextInt(totalWeight)
                (availableEntries.firstOrNull { e ->
                    pick -= e.weight
                    pick < 0
                } ?: availableEntries.randomOrNull()) to null
            }
            TableRollMode.SEQUENTIAL -> {
                val lastResult = if (sessionId != null) {
                    tableRepository.getRecentResultsForTable(sessionId, table.id, 1).firstOrNull()
                } else null
                
                val sorted = table.entries.sortedBy { it.sortOrder }
                if (lastResult == null) {
                    sorted.firstOrNull() to null
                } else {
                    val lastIndex = sorted.indexOfFirst { it.text == lastResult.resolvedText }
                    if (lastIndex == -1 || lastIndex >= sorted.size - 1) {
                        sorted.firstOrNull() to null
                    } else {
                        sorted[lastIndex + 1] to null
                    }
                }
            }
            TableRollMode.MACRO -> null to null // No se usa en pickEntry para macros
        }
    }

    // ── Resolución de texto ───────────────────────────────────────────────────

    private suspend fun resolveText(text: String, sessionId: Long?, depth: Int): String {
        var result = resolveInlineDice(text)
        result = resolveSubTableRefs(result, sessionId, depth)
        return result
    }

    /** Reemplaza [NdM+K] por el resultado evaluado del dado. */
    private fun resolveInlineDice(text: String): String {
        return inlineDiceRegex.replace(text) { match ->
            val count = match.groupValues[1].toIntOrNull() ?: 1
            val sides = match.groupValues[2].toIntOrNull() ?: 6
            val modifier = match.groupValues[3].toIntOrNull() ?: 0
            val roll = (1..count).sumOf { Random.nextInt(1, sides + 1) } + modifier
            roll.toString()
        }
    }

    /** Reemplaza @NombreTabla por el resultado de una tirada de esa tabla (1 nivel). */
    private suspend fun resolveSubTableRefs(text: String, sessionId: Long?, depth: Int): String {
        val matches = subTableRegex.findAll(text).toList()
        if (matches.isEmpty()) return text

        var result = text
        for (match in matches) {
            val tableName = match.groupValues[1].trim()
            val subTable = tableRepository.getTableByName(tableName)
            if (subTable != null) {
                val subResult = invoke(subTable.id, sessionId, depth + 1)
                result = result.replace(match.value, subResult.resolvedText)
            } else {
                result = result.replace(match.value, "[$tableName: no encontrada]")
            }
        }
        return result
    }
}
