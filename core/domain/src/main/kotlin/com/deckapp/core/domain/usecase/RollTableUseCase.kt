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
 * - Modos: RANGE (por rango minRoll..maxRoll) y WEIGHTED (peso relativo)
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
        depth: Int = 0
    ): TableRollResult {
        if (depth > 4) {
            return TableRollResult(
                tableId = tableId,
                tableName = "?",
                sessionId = sessionId,
                rollValue = 0,
                resolvedText = "[máx. anidado alcanzado]"
            )
        }

        val table = tableRepository.getTableWithEntries(tableId)
            ?: return TableRollResult(
                tableId = tableId,
                tableName = "Tabla desconocida",
                sessionId = sessionId,
                rollValue = 0,
                resolvedText = "[tabla no encontrada]"
            )

        val rollValue = evaluateDiceFormula(table.rollFormula)
        val entry = pickEntry(table, rollValue)
        
        // 1. Resolver texto base (dados inline y referencias por @Nombre)
        var resolvedText = resolveText(entry?.text ?: "[sin entrada]", sessionId, depth)

        // 2. Resolver sub-tabla vinculada por ID (Referencia directa potente)
        entry?.subTableId?.let { id ->
            val subResult = invoke(id, sessionId, depth + 1)
            resolvedText = "$resolvedText → ${subResult.resolvedText}"
        }

        val result = TableRollResult(
            tableId = tableId,
            tableName = table.name,
            sessionId = sessionId,
            rollValue = rollValue,
            resolvedText = resolvedText
        )
        tableRepository.saveRollResult(result)
        return result
    }

    // ── Selección de entrada ──────────────────────────────────────────────────

    private fun pickEntry(table: RandomTable, rollValue: Int): TableEntry? {
        return when (table.rollMode) {
            TableRollMode.RANGE -> {
                table.entries.firstOrNull { rollValue in it.minRoll..it.maxRoll }
                    ?: table.entries.randomOrNull()
            }
            TableRollMode.WEIGHTED -> {
                val totalWeight = table.entries.sumOf { it.weight }.coerceAtLeast(1)
                var pick = Random.nextInt(totalWeight)
                table.entries.firstOrNull { entry ->
                    pick -= entry.weight
                    pick < 0
                } ?: table.entries.randomOrNull()
            }
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

    // ── Evaluación de fórmula de dado ─────────────────────────────────────────

    companion object {
        private val diceFormulaRegex = Regex("""(\d+)d(\d+)([+\-]\d+)?""", RegexOption.IGNORE_CASE)

        fun evaluateDiceFormula(formula: String): Int {
            val match = diceFormulaRegex.find(formula.trim()) ?: return 1
            val count = match.groupValues[1].toIntOrNull() ?: 1
            val sides = match.groupValues[2].toIntOrNull() ?: 6
            val modifier = match.groupValues[3].toIntOrNull() ?: 0
            return (1..count.coerceAtLeast(1)).sumOf { Random.nextInt(1, sides.coerceAtLeast(2) + 1) } + modifier
        }

        /** Devuelve el rango posible de una fórmula: Pair(min, max). */
        fun getDiceRange(formula: String): Pair<Int, Int> {
            val match = diceFormulaRegex.find(formula.trim()) ?: return 1 to 1
            val count = match.groupValues[1].toIntOrNull() ?: 1
            val sides = match.groupValues[2].toIntOrNull() ?: 6
            val modifier = match.groupValues[3].toIntOrNull() ?: 0
            return (count + modifier) to (count * sides + modifier)
        }
    }
}
