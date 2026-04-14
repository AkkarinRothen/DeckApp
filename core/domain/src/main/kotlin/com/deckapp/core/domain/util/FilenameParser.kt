package com.deckapp.core.domain.util

/**
 * Parsea nombres de archivo de imágenes de cartas para extraer metadatos.
 *
 * Convención: `NNN_titulo[_palo].ext`
 * - `047_dragon.png`         → value=47,  title="Dragon",        suit=null
 * - `047_dragon_fuego.png`   → value=47,  title="Dragon",        suit="Fuego"
 * - `dragon_fuego.png`       → value=null, title="Dragon",       suit="Fuego"
 * - `dragon.png`             → value=null, title="Dragon",       suit=null
 *
 * Regla de suit: si hay 2+ segmentos de nombre (después del número opcional),
 * el último segmento se interpreta como el palo (suit).
 * Nota Fase 1: el título no puede contener guiones bajos (son separadores).
 */
object FilenameParser {

    data class CardMetadata(
        val title: String,
        val value: Int?,
        val suit: String?
    )

    fun parse(filename: String): CardMetadata {
        val nameWithoutExtension = filename.substringBeforeLast('.')
        val parts = nameWithoutExtension.split('_').filter { it.isNotBlank() }

        if (parts.isEmpty()) return CardMetadata(title = filename, value = null, suit = null)

        // Detectar si el primer segmento es un número (valor de la carta)
        val firstIsNumber = parts.first().all { it.isDigit() }
        val value = if (firstIsNumber) parts.first().toIntOrNull() else null
        val nameParts = if (firstIsNumber) parts.drop(1) else parts

        return when (nameParts.size) {
            0 -> CardMetadata(
                title = filename.substringBeforeLast('.'),
                value = value,
                suit = null
            )
            1 -> CardMetadata(
                title = nameParts[0].titleCase(),
                value = value,
                suit = null
            )
            else -> CardMetadata(
                title = nameParts.dropLast(1).joinToString(" ") { it.titleCase() },
                value = value,
                suit = nameParts.last().titleCase()
            )
        }
    }

    private fun String.titleCase(): String =
        replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}
