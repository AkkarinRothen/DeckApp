package com.deckapp.core.model

/**
 * Modelo de dominio para una Colección (Baúl).
 */
data class DeckCollection(
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val color: Int,               // Color ARGB
    val icon: CollectionIcon = CollectionIcon.CHEST,
    val resourceCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

enum class CollectionIcon {
    CHEST,        // Baúl / Tesoro
    BOOK,         // Grimorio / Reglas
    MAP,          // Cartografía / Viaje
    SKULL,        // Enemigos / Peligro
    BAG,          // Equipo / Inventario
    SWORDS,       // Combate
    MOUNTAIN,     // Mundo / Localizaciones
    PEOPLE,       // NPCs / Personajes
    FOLDER        // Carpeta genérica
}
