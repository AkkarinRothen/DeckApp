package com.deckapp.core.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

enum class MicrosettingSlotType(val displayName: String, val colorHex: Long) {
    REGION("Región", 0xFF2E7D32),             // Verde Esmeralda / Bioma base
    LANDMARK("Punto de Interés", 0xFF1565C0), // Azul Zafiro / Sitio o monumento
    NAMESAKE("Nombre", 0xFF7B1FA2),            // Púrpura Místico / Etimología o título
    ORIGIN("Origen", 0xFFE65100),              // Ámbar Profundo / Pasado y catalizador
    ATTRIBUTE("Atributo", 0xFF00838F),         // Turquesa / Rasgo cultural o peculiaridad
    ADVENT("Acontecimiento", 0xFFC62828)       // Rojo Carmesí / Crisis, amenaza o giro
}

@Serializable
data class MicrosettingCardItem(
    val slotType: MicrosettingSlotType,
    val cardId: Long,
    val stackId: Long,
    val title: String,
    val imagePath: String?,
    val rotation: Int = 0, // 0, 90, 180, 270
    val activeCue: String = "",
    val allCues: List<String> = emptyList()
) {
    val optionCount: Int get() = allCues.size.takeIf { it in setOf(1, 2, 4) } ?: 4
    val optionRotations: List<Int> get() = when (optionCount) {
        1 -> listOf(0)
        2 -> listOf(0, 180)
        else -> listOf(0, 90, 180, 270)
    }
    val optionLabels: List<String> get() = when (optionCount) {
        1 -> listOf("Única")
        2 -> listOf("Superior", "Inferior")
        else -> listOf("Norte", "Este", "Sur", "Oeste")
    }
    fun cueAt(rotation: Int): String = allCues.getOrNull(optionRotations.indexOf(rotation)).orEmpty()
}

@Serializable
data class MicrosettingCluster(
    val id: String = "",
    val title: String = "Microsetting de Mundo",
    val items: List<MicrosettingCardItem> = emptyList(),
    val notes: String = "",
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun slot(type: MicrosettingSlotType): MicrosettingCardItem? =
        items.firstOrNull { it.slotType == type }
}

object MicrosettingCodec {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }

    fun encode(cluster: MicrosettingCluster): String = runCatching {
        json.encodeToString(cluster)
    }.getOrDefault("")

    fun decode(raw: String?): MicrosettingCluster? {
        if (raw.isNullOrBlank()) return null
        return runCatching {
            json.decodeFromString<MicrosettingCluster>(raw)
        }.getOrNull()
    }

    fun formatMarkdown(cluster: MicrosettingCluster): String {
        val sb = StringBuilder()
        sb.appendLine("### 🌍 Microsetting: ${cluster.title}")
        if (cluster.notes.isNotBlank()) {
            sb.appendLine("> ${cluster.notes}")
            sb.appendLine()
        }
        MicrosettingSlotType.entries.forEach { slot ->
            val item = cluster.slot(slot)
            if (item != null) {
                val optionIndex = item.optionRotations.indexOf(item.rotation).coerceAtLeast(0)
                val label = item.optionLabels[optionIndex]
                val cueText = if (item.activeCue.isNotBlank()) " · *\"${item.activeCue}\"*" else ""
                sb.appendLine("- **${slot.displayName}:** ${item.title} — $label$cueText")
            }
        }
        return sb.toString().trimEnd()
    }
}
