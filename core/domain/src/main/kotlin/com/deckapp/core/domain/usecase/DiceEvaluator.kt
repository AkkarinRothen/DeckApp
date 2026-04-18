package com.deckapp.core.domain.usecase

import kotlin.random.Random

/**
 * Evaluador de fórmulas de dados estándar (NdM+K).
 * Extraído para facilitar pruebas unitarias y futura expansión a fórmulas complejas.
 */
object DiceEvaluator {
    private val diceFormulaRegex = Regex("""(\d+)d(\d+)([+\-]\d+)?""", RegexOption.IGNORE_CASE)

    /**
     * Evalúa una fórmula tipo "2d6+3" y devuelve el resultado aleatorio.
     */
    fun evaluate(formula: String): Int {
        val cleaned = formula.trim()
        if (cleaned.isEmpty()) return 1
        
        // Soporte básico para números fijos
        cleaned.toIntOrNull()?.let { return it }

        val match = diceFormulaRegex.find(cleaned) ?: return 1
        val count = match.groupValues[1].toIntOrNull()?.coerceIn(1, 100) ?: 1
        val sides = match.groupValues[2].toIntOrNull()?.coerceIn(1, 1000) ?: 6
        val modifier = match.groupValues[3]?.let { 
            if (it.startsWith("+")) it.substring(1).toIntOrNull() ?: 0
            else it.toIntOrNull() ?: 0
        } ?: 0
        
        return (1..count).sumOf { Random.nextInt(1, sides + 1) } + modifier
    }

    /**
     * Devuelve el rango posible (mínimo y máximo) de una fórmula.
     */
    fun getRange(formula: String): Pair<Int, Int> {
        val cleaned = formula.trim()
        if (cleaned.isEmpty()) return 1 to 1
        
        cleaned.toIntOrNull()?.let { return it to it }

        val match = diceFormulaRegex.find(cleaned) ?: return 1 to 1
        val count = match.groupValues[1].toIntOrNull() ?: 1
        val sides = match.groupValues[2].toIntOrNull() ?: 6
        val modifier = match.groupValues[3]?.let { 
            if (it.startsWith("+")) it.substring(1).toIntOrNull() ?: 0
            else it.toIntOrNull() ?: 0
        } ?: 0
        
        return (count + modifier) to (count * sides + modifier)
    }
}
