package com.deckapp.core.domain.usecase

/**
 * Utilidades para parsing y normalización de rangos numéricos.
 *
 * Soporta formatos habituales en manuales TTRPG:
 * - "5"          → sola entrada, minRoll=5, maxRoll=5
 * - "1-5"        → minRoll=1, maxRoll=5
 * - "6..10"      → minRoll=6, maxRoll=10
 * - "11 a 20"    → minRoll=11, maxRoll=20
 * - "1 to 5"     → minRoll=1, maxRoll=5
 * - "01-05"      → leading zeros, minRoll=1, maxRoll=5
 * - "d6" style   → devuelve null (no es un rango de tabla)
 */
object RangeParser {

    data class ParsedRange(val min: Int, val max: Int)
    data class ParsedRangeResult(val range: ParsedRange, val consumedLength: Int)

    private val RANGE_PATTERNS = listOf(
        // "1-5", "01-05", "1–5" (em dash), "1—5" (en dash)
        Regex("""^(\d{1,3})\s*[-–—]\s*(\d{1,3})"""),
        // "1..5"
        Regex("""^(\d{1,3})\s*\.\.\s*(\d{1,3})"""),
        // "1 a 5", "1 to 5", "1 to 10"
        Regex("""^(\d{1,3})\s+(?:a|to)\s+(\d{1,3})""", RegexOption.IGNORE_CASE),
        // "1 - 5" (spaces around dash)
        Regex("""^(\d{1,3})\s+-\s+(\d{1,3})""")
    )

    private val SINGLE_NUMBER = Regex("""^(\d{1,3})""")

    // Símbolos basura que suelen aparecer antes de un número en OCR (puntos, asteriscos, ceros mal leídos, etc.)
    // Incluye paréntesis de apertura '(' para soportar formatos como "(1-5)" y "(01–10)" habituales en manuales impresos.
    private val NOISE_PREFIX = Regex("""^[\s\.*_\-)\]~(]*""")

    /**
     * Intenta parsear el texto que empieza por un rango de dados.
     * Devuelve el rango y la longitud consumida si tiene éxito.
     *
     * Manejo especial de rangos percentiles: "00" se normaliza a 100.
     * Ejemplo: "96-00" → ParsedRange(96, 100), convención habitual en tablas OSR.
     */
    fun parse(raw: String): ParsedRangeResult? {
        val noiseMatch = NOISE_PREFIX.find(raw)
        val noiseLength = noiseMatch?.value?.length ?: 0
        val cleaned = raw.substring(noiseLength)
        
        if (cleaned.isEmpty()) return null

        // Rango con dos extremos
        for (pattern in RANGE_PATTERNS) {
            val match = pattern.find(cleaned) ?: continue
            val min = normalizePercentile(match.groupValues[1]) ?: continue
            val max = normalizePercentile(match.groupValues[2]) ?: continue
            val range = if (min <= max) ParsedRange(min, max) else ParsedRange(max, min)
            return ParsedRangeResult(range, noiseLength + match.value.length)
        }

        // Número suelto
        SINGLE_NUMBER.find(cleaned)?.let {
            val value = normalizePercentile(it.groupValues[1]) ?: return null
            return ParsedRangeResult(ParsedRange(value, value), noiseLength + it.value.length)
        }

        return null
    }

    /**
     * Convierte un string numérico a Int, normalizando "00" → 100.
     * Esta convención es estándar en tablas percentiles de TTRPG (OSR, D&D Basic, etc.).
     */
    private fun normalizePercentile(raw: String): Int? {
        val n = raw.toIntOrNull() ?: return null
        return if (n == 0) 100 else n
    }

    /**
     * Dada una lista de entradas con rangos, detecta huecos y solapamientos.
     * Devuelve true si la tabla tiene integridad perfecta (sin huecos ni solapamientos).
     */
    fun validateIntegrity(entries: List<Pair<Int, Int>>): ValidationResult {
        if (entries.isEmpty()) return ValidationResult.empty()
        val sorted = entries.sortedBy { it.first }
        val gaps = mutableListOf<Int>()
        val overlaps = mutableListOf<Int>()
        var expectedNext = sorted[0].first

        for ((min, max) in sorted) {
            when {
                min > expectedNext -> gaps.add(expectedNext)
                min < expectedNext -> overlaps.add(min)
            }
            expectedNext = max + 1
        }
        return ValidationResult(gaps = gaps, overlaps = overlaps)
    }

    data class ValidationResult(
        val gaps: List<Int>,
        val overlaps: List<Int>
    ) {
        val isValid: Boolean get() = gaps.isEmpty() && overlaps.isEmpty()
        companion object {
            fun empty() = ValidationResult(emptyList(), emptyList())
        }
    }

    /**
     * Infiere la fórmula de dado más probable a partir del rango mínimo y máximo detectado.
     *
     * Detecta combinaciones de dados habituales en TTRPG:
     * - min=2, max=12  → "2d6"   (tabla de Encuentros D&D clásica)
     * - min=3, max=18  → "3d6"   (atributos de personaje)
     * - min=2, max=8   → "2d4"
     * - min=2, max=20  → "2d10"
     * Después aplica la heurística estándar de dado único (1d4, 1d6, …, 1d100).
     */
    fun inferRollFormula(minValue: Int, maxValue: Int): String {
        val multiDie = mapOf(
            (2 to 8)  to "2d4",
            (2 to 12) to "2d6",
            (2 to 20) to "2d10",
            (3 to 18) to "3d6"
        )
        multiDie[minValue to maxValue]?.let { return it }
        val standard = listOf(4, 6, 8, 10, 12, 20, 100)
        val die = standard.firstOrNull { it >= maxValue } ?: maxValue
        return "1d$die"
    }

    /** Sobrecarga de compatibilidad cuando solo se conoce el máximo (asume min=1). */
    fun inferRollFormula(maxValue: Int): String = inferRollFormula(1, maxValue)
}
