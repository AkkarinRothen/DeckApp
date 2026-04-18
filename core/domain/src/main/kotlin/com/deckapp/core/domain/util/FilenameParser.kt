package com.deckapp.core.domain.util

/**
 * Parsea nombres de archivo de imágenes de cartas para extraer metadatos.
 * Mejorado para ignorar ruido de escaneo y manejar formatos más flexibles.
 */
object FilenameParser {

    data class CardMetadata(
        val title: String,
        val value: Int? = null,
        val suit: String? = null
    )

    private val scanPrefixes = listOf("Scan_", "IMG_", "DSC_", "Carta_", "Copy of ", "Copia de ")
    private val numberRegex = Regex("""\b\d{1,3}\b""") // Solo números de 1-3 dígitos aislados

    fun parse(filename: String): CardMetadata {
        val originalNoExt = filename.substringBeforeLast('.')
        var cleanName = originalNoExt
        
        // 1. Limpiar prefijos de ruido
        scanPrefixes.forEach { prefix ->
            if (cleanName.startsWith(prefix, ignoreCase = true)) {
                cleanName = cleanName.substring(prefix.length)
            }
        }

        // 2. Extraer valor numérico si existe (primer número de 1-3 dígitos)
        val valueMatch = numberRegex.find(cleanName)
        val value = valueMatch?.value?.toIntOrNull()
        
        // Remover el valor del nombre para el título
        if (valueMatch != null) {
            cleanName = cleanName.removeRange(valueMatch.range).trim()
        }

        // 3. Dividir por separadores comunes
        val parts = cleanName.split(Regex("""[_\-\s]+"""))
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        if (parts.isEmpty()) {
            return CardMetadata(
                title = originalNoExt.titleCase(),
                value = value
            )
        }

        return when {
            parts.size >= 2 -> CardMetadata(
                title = parts.dropLast(1).joinToString(" ") { it.titleCase() },
                value = value,
                suit = parts.last().titleCase()
            )
            else -> CardMetadata(
                title = parts.first().titleCase(),
                value = value
            )
        }
    }

    fun isImage(filename: String): Boolean {
        val ext = filename.substringAfterLast('.', "").lowercase()
        return ext in listOf("jpg", "jpeg", "png", "webp", "bmp", "gif")
    }

    private fun String.titleCase(): String =
        if (isEmpty()) "" else this.lowercase().replaceFirstChar { it.uppercase() }
}
