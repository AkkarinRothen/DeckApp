package com.deckapp.core.domain.usecase

import com.deckapp.core.model.ChoirIncursion
import com.deckapp.core.model.ChoirSettlement
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Base64

/**
 * Codificador persistente para Incursiones y Asentamiento en notas Markdown.
 * Almacena el estado en bloques delimitados sin requerir migraciones de base de datos.
 */
object ChoirIncursionCodec {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val incursionMarker = Regex("<!-- choir-incursion:v1:([A-Za-z0-9+/=]+) -->")
    private val incursionBlock = Regex("<!-- choir-incursion:v1:[A-Za-z0-9+/=]+ -->[\\s\\S]*?<!-- /choir-incursion -->")

    private val settlementMarker = Regex("<!-- choir-settlement:v1:([A-Za-z0-9+/=]+) -->")
    private val settlementBlock = Regex("<!-- choir-settlement:v1:[A-Za-z0-9+/=]+ -->[\\s\\S]*?<!-- /choir-settlement -->")

    fun readIncursion(notes: String): ChoirIncursion? = incursionBlock.find(notes)?.value?.let { value ->
        incursionMarker.find(value)
    }?.let {
        runCatching {
            json.decodeFromString<ChoirIncursion>(String(Base64.getDecoder().decode(it.groupValues[1]), Charsets.UTF_8))
        }.getOrNull()
    }

    fun writeIncursion(notes: String, incursion: ChoirIncursion): String {
        val encoded = Base64.getEncoder().encodeToString(json.encodeToString(incursion).toByteArray(Charsets.UTF_8))
        val owned = "<!-- choir-incursion:v1:$encoded -->\n${markdownIncursion(incursion)}\n<!-- /choir-incursion -->"
        return if (incursionBlock.containsMatchIn(notes)) incursionBlock.replaceFirst(notes, Regex.escapeReplacement(owned))
        else notes.trimEnd().let { if (it.isEmpty()) owned else "$it\n\n$owned" }
    }

    fun readSettlement(notes: String): ChoirSettlement? = settlementBlock.find(notes)?.value?.let { value ->
        settlementMarker.find(value)
    }?.let {
        runCatching {
            json.decodeFromString<ChoirSettlement>(String(Base64.getDecoder().decode(it.groupValues[1]), Charsets.UTF_8))
        }.getOrNull()
    }

    fun writeSettlement(notes: String, settlement: ChoirSettlement): String {
        val encoded = Base64.getEncoder().encodeToString(json.encodeToString(settlement).toByteArray(Charsets.UTF_8))
        val owned = "<!-- choir-settlement:v1:$encoded -->\n${markdownSettlement(settlement)}\n<!-- /choir-settlement -->"
        return if (settlementBlock.containsMatchIn(notes)) settlementBlock.replaceFirst(notes, Regex.escapeReplacement(owned))
        else notes.trimEnd().let { if (it.isEmpty()) owned else "$it\n\n$owned" }
    }

    fun markdownIncursion(inc: ChoirIncursion): String = buildString {
        append("### Tablero de Incursión: ${inc.title}\n")
        append("- **Estado**: ${inc.status} · **Nodo Actual**: ${inc.currentNodeIndex}/8 · **Canción de la Carne**: ${inc.songLevel}/10\n")
        append("- **Botín acumulado**: ${if (inc.lootCollected.isBlank()) "Ninguno" else inc.lootCollected}\n\n")
        append("| Nodo | Tipo | Estado | Título |\n")
        append("| --- | --- | --- | --- |\n")
        inc.nodes.forEach { n ->
            append("| ${n.index} | ${n.nodeType} | ${n.state} | ${n.title} |\n")
        }
    }

    fun markdownSettlement(set: ChoirSettlement): String = buildString {
        append("### Asentamiento & Refugio: ${set.name}\n")
        append("- **Moral**: ${set.morale}/10 · **Defensas**: ${set.defense}/10 · **Población**: ${set.population}\n")
        append("- **Raciones en Despensa**: ${set.stockRations} · **Suministros Médicos**: ${set.stockMedicalSupplies}\n")
        append("- **Instalaciones**: ${set.facilities.joinToString(", ")}\n")
        if (set.activeRumors.isNotEmpty()) {
            append("- **Rumores del Páramo**:\n")
            set.activeRumors.forEach { r -> append("  - $r\n") }
        }
    }
}
