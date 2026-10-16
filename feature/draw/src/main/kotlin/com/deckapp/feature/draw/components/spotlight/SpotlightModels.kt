package com.deckapp.feature.draw.components.spotlight

/**
 * Categorías de filtrado en la paleta de búsqueda Spotlight.
 */
enum class SpotlightCategory(val label: String) {
    ALL("Todos"),
    TABLES("Tablas"),
    NPCS("PNJs"),
    RULES("Reglas"),
    MONSTERS("Monstruos"),
    NOTES("Diario")
}

/**
 * Representa un resultado federado de búsqueda con vista previa y acciones contextuales.
 */
sealed class SpotlightItem {
    abstract val id: String
    abstract val title: String
    abstract val subtitle: String
    abstract val category: SpotlightCategory

    data class TableResult(
        val tableId: Long,
        override val title: String,
        override val subtitle: String,
        val formula: String,
        val rowCount: Int,
        val description: String = ""
    ) : SpotlightItem() {
        override val id: String = "table_$tableId"
        override val category: SpotlightCategory = SpotlightCategory.TABLES
    }

    data class NpcResult(
        val npcId: Long,
        override val title: String,
        override val subtitle: String,
        val notes: String,
        val imagePath: String? = null
    ) : SpotlightItem() {
        override val id: String = "npc_$npcId"
        override val category: SpotlightCategory = SpotlightCategory.NPCS
    }

    data class RuleResult(
        val ruleId: Long,
        override val title: String,
        override val subtitle: String,
        val content: String,
        val gameSystem: String
    ) : SpotlightItem() {
        override val id: String = "rule_$ruleId"
        override val category: SpotlightCategory = SpotlightCategory.RULES
    }

    data class CreatureResult(
        val creatureId: Long,
        override val title: String,
        override val subtitle: String,
        val hp: Int,
        val ac: Int,
        val initiativeBonus: Int = 0,
        val notes: String = ""
    ) : SpotlightItem() {
        override val id: String = "creature_$creatureId"
        override val category: SpotlightCategory = SpotlightCategory.MONSTERS
    }

    data class NoteResult(
        val lineNumber: Int,
        override val title: String,
        override val subtitle: String,
        val snippet: String
    ) : SpotlightItem() {
        override val id: String = "note_$lineNumber"
        override val category: SpotlightCategory = SpotlightCategory.NOTES
    }
}
