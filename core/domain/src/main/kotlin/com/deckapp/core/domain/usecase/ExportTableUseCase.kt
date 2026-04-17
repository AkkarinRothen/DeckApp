package com.deckapp.core.domain.usecase

import com.deckapp.core.model.RandomTable
import com.deckapp.core.model.TableRollMode
import javax.inject.Inject

/**
 * UseCase para exportar tablas a formatos externos.
 */
class ExportTableUseCase @Inject constructor() {

    /**
     * Genera un string en formato CSV.
     * Compatible con Excel y la mayoría de VTTs.
     */
    fun toCsv(table: RandomTable): String {
        return buildString {
            // Header
            if (table.rollMode == TableRollMode.RANGE) {
                append("Min,Max,Text\n")
            } else {
                append("Weight,Text\n")
            }
            
            // Entries
            table.entries.forEach { entry ->
                if (table.rollMode == TableRollMode.RANGE) {
                    append("${entry.minRoll},${entry.maxRoll},\"${entry.text.replace("\"", "\"\"")}\"\n")
                } else {
                    append("${entry.weight},\"${entry.text.replace("\"", "\"\"")}\"\n")
                }
            }
        }
    }

    /**
     * Genera un string en formato Markdown (GFM).
     * Ideal para notas de campaña (Notion, Obsidian, etc).
     */
    fun toMarkdown(table: RandomTable): String {
        return buildString {
            append("### ${table.name}\n\n")
            if (table.description.isNotBlank()) {
                append("${table.description}\n\n")
            }
            
            if (table.rollMode == TableRollMode.RANGE) {
                append("| Rango | Resultado |\n")
                append("| :--- | :--- |\n")
                table.entries.forEach { entry ->
                    val range = if (entry.minRoll == entry.maxRoll) "${entry.minRoll}" else "${entry.minRoll}-${entry.maxRoll}"
                    append("| $range | ${entry.text} |\n")
                }
            } else {
                append("| Peso | Resultado |\n")
                append("| :--- | :--- |\n")
                table.entries.forEach { entry ->
                    append("| ${entry.weight} | ${entry.text} |\n")
                }
            }
            
            append("\n*Fórmula: ${table.rollFormula}*")
        }
    }
}
