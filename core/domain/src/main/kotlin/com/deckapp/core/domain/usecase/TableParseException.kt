package com.deckapp.core.domain.usecase

/**
 * Jerarquía sellada de errores de parsing de tablas.
 * Permite al ViewModel y a la UI distinguir el tipo de fallo y mostrar un mensaje adecuado.
 */
sealed class TableParseException(message: String, cause: Throwable? = null) : Exception(message, cause) {

    /** El texto JSON está malformado y no puede ser deserializado. */
    class InvalidJson(cause: String) :
        TableParseException("JSON malformado: $cause")

    /** El JSON es válido pero no contiene ningún campo reconocido ('results', 'entries' o 'items'). */
    class UnknownFormat(hint: String) :
        TableParseException("Formato no reconocido. $hint")

    /** El archivo se procesó correctamente pero no produjo ninguna entrada de tabla. */
    class EmptyResult(source: String) :
        TableParseException("No se encontraron entradas en $source.")
}
