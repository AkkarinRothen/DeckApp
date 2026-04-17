package com.deckapp.core.model

/**
 * Representa una coincidencia de búsqueda global.
 */
data class SearchMatch(
    val id: Long,
    val type: SearchResultType,
    val title: String,
    val subtitle: String? = null,
    val snippet: String? = null,
    val parentId: Long? = null,        // ID del mazo o tabla padre si aplica
    val parentName: String? = null     // Nombre del mazo o tabla padre
)

enum class SearchResultType {
    CARD,
    DECK,
    TABLE,
    TABLE_ENTRY,
    BAUL
}
