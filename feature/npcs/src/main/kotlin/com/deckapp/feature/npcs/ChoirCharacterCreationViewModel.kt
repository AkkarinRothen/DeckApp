package com.deckapp.feature.npcs

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.deckapp.core.domain.usecase.*
import com.deckapp.core.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import kotlin.random.Random

data class ChoirCreationState(val name: String = "", val sheet: ChoirCharacter = ChoirCharacter(), val pool: List<Int> = emptyList())

@HiltViewModel
class ChoirCharacterCreationViewModel @Inject constructor(private val saved: SavedStateHandle) : ViewModel() {
    private val initial = ChoirCharacter(
        occupation = ChoirCreationCatalog.occupations.first().text, sin = ChoirCreationCatalog.sins.first().text,
        fragment = ChoirCreationCatalog.fragments.first().text, whisper = ChoirCreationCatalog.whispers.first().text)
    private val mutable = MutableStateFlow(ChoirCreationState(
        saved["choirName"] ?: "", saved.get<String>("choirSheet")?.let(ChoirCharacterCodec::read) ?: initial,
        saved.get<IntArray>("choirPool")?.toList().orEmpty()))
    val state = mutable.asStateFlow()
    fun name(value: String) { mutable.update { it.copy(name = value) }; persist() }
    fun sheet(value: ChoirCharacter) {
        val s = ChoirCharacterRules.normalize(value)
        mutable.update { it.copy(sheet = s.copy(vigor = ChoirCharacterRules.vigorMaximum(s))) }; persist()
    }
    fun roll() {
        val dice = List(6) { ChoirCharacterRules.rollAttribute() }
        mutable.update { it.copy(pool = dice) }
        sheet(mutable.value.sheet.copy(attributes = ChoirAttributes(dice[0], dice[1], dice[2], dice[3], dice[4], dice[5])))
    }
    fun manual() { mutable.update { it.copy(pool = emptyList()) }; persist() }
    fun randomChoice(choices: List<ChoirBackgroundChoice>) = choices[Random.nextInt(choices.size)].text
    fun error() = mutable.value.let { ChoirCharacterRules.creationError(it.name, it.sheet, it.pool) }
    fun create(): Npc? {
        if (error() != null) return null
        val draft = mutable.value
        return ChoirCharacterRules.toNpc(Npc(name = draft.name.trim(), description = "Inquebrantable · Choir of Flesh"), draft.sheet)
    }
    fun reset() { mutable.value = ChoirCreationState(sheet = initial); persist() }
    private fun persist() {
        saved["choirName"] = mutable.value.name
        saved["choirSheet"] = ChoirCharacterCodec.write("", mutable.value.sheet)
        saved["choirPool"] = mutable.value.pool.toIntArray()
    }
}
