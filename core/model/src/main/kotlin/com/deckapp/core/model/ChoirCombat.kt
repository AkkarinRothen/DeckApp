package com.deckapp.core.model

enum class ChoirCombatCommand { ADD_MAJOR, REMOVE_MAJOR, ADD_MINOR, REMOVE_MINOR, ROLL_ACTION, ROLL_TARGET }

/** Original dynamic target grades and verbatim actions; deliberately no HP/AC conversion. */
data class ChoirProfile(
    val statistics: Map<String, String>,
    val standard: String,
    val special: String,
    val support: String,
    val reference: String
) {
    val initiative: Int get() = statistics["Iniciativa"]?.toIntOrNull() ?: 0
    val traits: String get() = reference.substringAfter("**Acción de Apoyo:** $support", "")
        .substringBefore("Fuente:").trim()

    companion object {
        const val MARKER = "<!-- choir-of-flesh:v2 -->"
        fun fromNotes(notes: String): ChoirProfile? {
            if (!notes.contains(MARKER)) return null
            val reference = notes.substringAfterLast(MARKER).substringBefore("<!-- choir-state:").trim()
            val stats = Regex("(?m)^- \\*\\*(.+?)\\*\\*: (.+)$").findAll(reference)
                .associate { it.groupValues[1] to it.groupValues[2].trim() }
            fun action(label: String) = Regex("(?m)^\\*\\*$label:\\*\\* (.+)$")
                .find(reference)?.groupValues?.get(1).orEmpty()
            if (stats["Iniciativa"]?.toIntOrNull() == null) return null
            return ChoirProfile(stats, action("Ataque Estándar"), action("Ataque Especial"),
                action("Acción de Apoyo"), reference)
        }
    }
}

/** Stored in existing encounter notes, so backups and session copies retain wound tracking. */
data class ChoirCombatState(val major: Int = 0, val minor: Int = 0, val actionRoll: Int = 0, val targetRoll: Int = 0) {
    fun writeTo(notes: String): String = notes.replace(PATTERN, "").trimEnd() +
        "\n\n<!-- choir-state:${major.coerceAtLeast(0)},${minor.coerceAtLeast(0)},$actionRoll,$targetRoll -->"

    companion object {
        private val PATTERN = Regex("<!-- choir-state:(\\d+),(\\d+),(\\d+),(\\d+) -->")
        fun fromNotes(notes: String): ChoirCombatState {
            val values = PATTERN.findAll(notes).lastOrNull()?.groupValues?.drop(1)?.map { it.toIntOrNull() ?: 0 }
                ?: return ChoirCombatState()
            return ChoirCombatState(values[0], values[1], values[2], values[3])
        }
    }
}
