package com.deckapp.core.domain.usecase

import com.deckapp.core.model.ChoirProfile

/** Chapter V decision tables. NPCs never roll attacks against player AC. */
object ChoirCombatRules {
    private val grades = listOf("Muy Fácil", "Fácil", "Promedio", "Difícil", "Muy Difícil", "Casi Imposible")

    /** Exact dynamic-NO table in Chapter III, not the separate static suggested-NO table. */
    fun dynamicTarget(grade: String, usualModifier: Int): Int? {
        val column = grades.indexOf(grade)
        if (column < 0 || usualModifier !in -5..10) return null
        return 7 + 2 * column + usualModifier
    }

    fun action(profile: ChoirProfile, majorWounds: Int, roll: Int): String {
        require(roll in 1..10)
        require(majorWounds >= 0)
        fun available(text: String) = text.isNotBlank() && !text.trim().startsWith("Ningun", ignoreCase = true)
        val special = profile.special.takeIf(::available) ?: profile.standard
        val support = profile.support.takeIf(::available) ?: special
        return when {
            majorWounds == 0 -> if (roll <= 6) profile.standard else special
            majorWounds == 1 -> when (roll) { in 1..5 -> profile.standard; in 6..8 -> special; else -> support }
            else -> when (roll) { in 1..4 -> profile.standard; in 5..6 -> special; in 7..8 -> support; else -> "Huir" }
        }
    }

    fun target(roll: Int): String {
        require(roll in 1..6)
        return when (roll) {
            1, 2 -> "Más herido; si no corresponde, aleatorizar."
            3, 4 -> "Más cercano; si no está claro, aleatorizar."
            else -> "Objetivo aleatorio."
        }
    }
}
