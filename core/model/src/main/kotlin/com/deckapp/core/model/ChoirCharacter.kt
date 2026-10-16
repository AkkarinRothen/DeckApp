package com.deckapp.core.model

import kotlinx.serialization.Serializable

@Serializable
data class ChoirAttributes(
    val strength: Int = 10, val constitution: Int = 10, val dexterity: Int = 10,
    val intelligence: Int = 10, val willpower: Int = 10, val presence: Int = 10
) {
    fun values() = listOf(strength, constitution, dexterity, intelligence, willpower, presence)
}

/** Canonical live player sheet. Stored portably with the NPC's notes and existing backups. */
@Serializable
data class ChoirCharacter(
    val attributes: ChoirAttributes = ChoirAttributes(),
    val armor: Int = 1, val defenseOther: Int = 0, val initiativeOther: Int = 0,
    val movement: Int = 2, val vigor: Int = 0, val humanity: Int = 20,
    val anguish: Int = 0, val trauma: Int = 0, val major: Int = 0, val minor: Int = 0,
    val level: Int = 1, val xp: Int = 0, val rumors: Int = 0,
    val rations: Int = 0, val medicalSupplies: Int = 0,
    val occupation: String = "", val sin: String = "", val fragment: String = "", val whisper: String = "",
    val skills: String = "", val feats: String = "", val drawbacks: String = "",
    val weaponProficiency: String = "", val armorProficiency: String = "Gambesón",
    val gifts: String = "", val stigmata: String = "",
    val equipment: String = "Arma a elección\nGambesón (+1 Defensa)",
    val personalNotes: String = "", val undoState: String? = null
)

enum class ChoirCharacterCommand {
    ADD_MAJOR, REMOVE_MAJOR, ADD_MINOR, REMOVE_MINOR,
    GAIN_VIGOR, SPEND_VIGOR, LESSEN_MAJOR, NEGATE_MINOR, UNDO
}
