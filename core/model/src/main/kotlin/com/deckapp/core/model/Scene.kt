package com.deckapp.core.model

/** 
 * Representa un paso o momento específico dentro de una sesión de juego.
 * Permite estructurar la narrativa en el planificador.
 */
data class Scene(
    val id: Long = 0,
    val sessionId: Long,
    val title: String,
    val content: String = "",
    val isCompleted: Boolean = false,
    val sortOrder: Int = 0,
    // Pro Features (D-2+)
    val linkedTableId: Long? = null,
    val linkedDeckId: Long? = null,
    val imagePath: String? = null,
    val isAlternative: Boolean = false
)
