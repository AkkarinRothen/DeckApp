package com.deckapp.core.domain.usecase

import com.deckapp.core.model.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Base64

/** Bounded owned block leaves unrelated/user notes untouched. No database migration is needed. */
object ChoirCharacterCodec {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val marker = Regex("<!-- choir-character:v1:([A-Za-z0-9+/=]+) -->")
    private val block = Regex("<!-- choir-character:v1:[A-Za-z0-9+/=]+ -->[\\s\\S]*?<!-- /choir-character -->")
    fun read(notes: String): ChoirCharacter? = block.find(notes)?.value?.let { value -> marker.find(value) }?.let {
        runCatching { json.decodeFromString<ChoirCharacter>(String(Base64.getDecoder().decode(it.groupValues[1]), Charsets.UTF_8)) }.getOrNull()
    }
    fun snapshot(s: ChoirCharacter): String = Base64.getEncoder().encodeToString(json.encodeToString(s.copy(undoState = null)).toByteArray(Charsets.UTF_8))
    fun restore(snapshot: String): ChoirCharacter? = runCatching {
        json.decodeFromString<ChoirCharacter>(String(Base64.getDecoder().decode(snapshot), Charsets.UTF_8)).copy(undoState = null)
    }.getOrNull()
    fun write(notes: String, sheet: ChoirCharacter): String {
        val encoded = Base64.getEncoder().encodeToString(json.encodeToString(sheet).toByteArray(Charsets.UTF_8))
        val owned = "<!-- choir-character:v1:$encoded -->\n${markdown(sheet)}\n<!-- /choir-character -->"
        return if (block.containsMatchIn(notes)) block.replaceFirst(notes, kotlin.text.Regex.escapeReplacement(owned))
            else notes.trimEnd().let { if (it.isEmpty()) owned else "$it\n\n$owned" }
    }
    fun markdown(s: ChoirCharacter): String = buildString {
        append("## Ficha viva · Choir of Flesh\n\n")
        listOf("FUE", "CON", "DES", "INT", "VOL", "PRE").zip(s.attributes.values()).forEach { (label, score) ->
            append("- **$label**: $score (${ChoirCharacterRules.modifier(score)})\n")
        }
        append("\n**Defensa**: ${ChoirCharacterRules.defense(s)} · **Iniciativa**: ${ChoirCharacterRules.initiative(s)} · **Movimiento**: ${s.movement} casillas\n")
        append("**Vigor**: ${s.vigor}/${ChoirCharacterRules.vigorMaximum(s)} · **Humanidad**: ${s.humanity}/20 · **Angustia**: ${s.anguish}\n")
        append("**Heridas**: ${s.major}/4 mayores, ${s.minor}/8 menores · **Trauma**: ${s.trauma}\n")
        append("**Nivel**: ${s.level} · **XP**: ${s.xp} · **Rumores**: ${s.rumors} · **Carga máxima**: ${s.attributes.strength}\n")
        append("**Raciones**: ${s.rations} · **Suministros médicos**: ${s.medicalSupplies}\n\n")
        listOf("Ocupación" to s.occupation, "Pecado" to s.sin, "Fragmento" to s.fragment,
            "Susurro" to s.whisper, "Habilidades" to s.skills, "Dotes" to s.feats,
            "Defectos" to s.drawbacks, "Competencia de arma" to s.weaponProficiency,
            "Competencia de armadura" to s.armorProficiency, "Dones" to s.gifts,
            "Estigmas" to s.stigmata, "Equipo" to s.equipment, "Notas personales" to s.personalNotes)
            .filter { it.second.isNotBlank() }.forEach { (label, value) ->
                append("### $label\n${value.replace("<!-- /choir-character -->", "&lt;!-- /choir-character --&gt;")}\n\n")
            }
    }

    fun legacy(notes: String): ChoirCharacter? {
        if (read(notes) != null || !notes.startsWith("# Inquebrantable:")) return null
        fun score(label: String) = Regex("\\*\\*${Regex.escape(label)}\\*\\*: (\\d+)").find(notes)?.groupValues?.get(1)?.toIntOrNull()
        val scores = listOf("Fuerza (FUE)", "Constitución (CON)", "Destreza (DES)", "Inteligencia (INT)", "Voluntad (VOL)", "Presencia (PRE)")
            .map { score(it) ?: return null }
        val draft = ChoirCharacter(attributes = ChoirAttributes(scores[0], scores[1], scores[2], scores[3], scores[4], scores[5]),
            humanity = score("Humanidad") ?: 20, anguish = score("Angustia") ?: 0, trauma = score("Trauma") ?: 0,
            personalNotes = "Ficha anterior conservada (sus PG/CD y otros valores antiguos no se usan):\n\n$notes")
        return ChoirCharacterRules.normalize(draft.copy(vigor = score("Vigor") ?: ChoirCharacterRules.vigorMaximum(draft)))
    }
}
