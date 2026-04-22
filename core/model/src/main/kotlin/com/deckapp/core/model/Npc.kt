package com.deckapp.core.model

import java.util.UUID

/**
 * Representa un Personaje No Jugador (NPC) persistente en la biblioteca.
 * Sirve como plantilla para las criaturas de los encuentros.
 */
data class Npc(
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val imagePath: String? = null,
    val voiceSamplePath: String? = null,
    val maxHp: Int = 10,
    val currentHp: Int = 10,
    val armorClass: Int = 10,
    val initiativeBonus: Int = 0,
    val notes: String = "",
    val tags: List<Tag> = emptyList(),
    val isMonster: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
