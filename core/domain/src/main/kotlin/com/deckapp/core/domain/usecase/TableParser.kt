package com.deckapp.core.domain.usecase

import com.deckapp.core.model.TableEntry

/**
 * Contenedor genérico para el resultado de un parseo de tabla.
 */
data class ParsedTableContent(
    val name: String? = null,
    val description: String? = null,
    val entries: List<TableEntry> = emptyList()
)

/**
 * Interfaz común para todos los motores de importación de texto.
 */
interface TableParser {
    /**
     * Devuelve true si el texto parece estar en el formato que este parser maneja.
     */
    fun canParse(rawText: String): Boolean

    /**
     * Parsea el texto y extrae las entradas y metadatos opcionales.
     * @throws TableParseException si el formato es inválido.
     */
    fun parse(rawText: String): ParsedTableContent
}
