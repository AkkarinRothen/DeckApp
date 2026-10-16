package com.deckapp.core.domain.usecase

import com.deckapp.core.model.*
import kotlin.random.Random

object ChoirCharacterRules {
    fun modifier(score: Int): Int = when {
        score <= 3 -> -3; score <= 5 -> -2; score <= 8 -> -1
        score <= 12 -> 0; score <= 15 -> 1; score <= 17 -> 2; else -> 3
    }
    fun rollAttribute(d6: () -> Int = { Random.nextInt(1, 7) }): Int {
        val dice = List(4) { d6().also { require(it in 1..6) } }
        return dice.sum() - dice.min()
    }
    fun defense(s: ChoirCharacter) = modifier(s.attributes.dexterity) + s.armor + s.defenseOther
    fun initiative(s: ChoirCharacter) = modifier(s.attributes.dexterity) + s.initiativeOther
    fun vigorMaximum(s: ChoirCharacter) = (modifier(s.attributes.constitution) + modifier(s.attributes.willpower)).coerceAtLeast(0)
    fun isDead(s: ChoirCharacter) = s.major >= 4 || s.minor >= 8
    fun normalize(s: ChoirCharacter): ChoirCharacter = s.copy(
        vigor = s.vigor.coerceIn(0, vigorMaximum(s)), humanity = s.humanity.coerceIn(0, 20),
        major = s.major.coerceIn(0, 4), minor = s.minor.coerceIn(0, 8),
        anguish = s.anguish.coerceAtLeast(0), trauma = s.trauma.coerceAtLeast(0))

    fun creationError(name: String, sheet: ChoirCharacter, pool: List<Int>): String? = when {
        name.isBlank() -> "Escribe el nombre del Inquebrantable."
        sheet.attributes.values().any { it !in 3..18 } -> "Los atributos iniciales deben estar entre 3 y 18."
        pool.isNotEmpty() && pool.sorted() != sheet.attributes.values().sorted() -> "Reasigna los seis resultados tirados, o activa la entrada manual."
        sheet.weaponProficiency.isBlank() -> "Elige una competencia de arma."
        sheet.feats.isBlank() -> "Elige la dote inicial."
        sheet.skills.isBlank() -> "Anota tus habilidades elegidas."
        else -> null
    }

    fun apply(s: ChoirCharacter, command: ChoirCharacterCommand): ChoirCharacter = normalize(when (command) {
        ChoirCharacterCommand.ADD_MAJOR -> s.copy(major = s.major + 1)
        ChoirCharacterCommand.REMOVE_MAJOR -> s.copy(major = s.major - 1)
        ChoirCharacterCommand.ADD_MINOR -> s.copy(minor = s.minor + 1)
        ChoirCharacterCommand.REMOVE_MINOR -> s.copy(minor = s.minor - 1)
        ChoirCharacterCommand.GAIN_VIGOR -> s.copy(vigor = s.vigor + 1)
        ChoirCharacterCommand.SPEND_VIGOR -> s.copy(vigor = s.vigor - 1)
        ChoirCharacterCommand.LESSEN_MAJOR -> if (s.vigor > 0 && s.major > 0 && s.minor < 8)
            s.copy(vigor = s.vigor - 1, major = s.major - 1, minor = s.minor + 1) else s
        ChoirCharacterCommand.NEGATE_MINOR -> if (s.vigor > 0 && s.minor > 0)
            s.copy(vigor = s.vigor - 1, minor = s.minor - 1) else s
        ChoirCharacterCommand.UNDO -> s.undoState?.let(ChoirCharacterCodec::restore) ?: s
    })

    fun toNpc(npc: Npc, sheet: ChoirCharacter): Npc {
        val s = normalize(sheet)
        return npc.copy(notes = ChoirCharacterCodec.write(npc.notes, s),
            maxHp = 1, currentHp = if (isDead(s)) 0 else 1,
            armorClass = defense(s), initiativeBonus = initiative(s), isMonster = false)
    }
}
