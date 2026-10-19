package com.deckapp.feature.draw.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.deckapp.core.model.*
import com.deckapp.core.ui.icons.DeckIcons

data class BoardBlockMeta(
    val title: String,
    val subtitle: String = "",
    val icon: ImageVector = DeckIcons.Board,
    val color: Color = Color(0xFF6200EE),
    val preview: String = "",
    val footnote: String = "",
    val accentNote: String? = null,
    val ctaLabel: String = "Tirar"
)

fun boardBlockMeta(
    item: SessionBoardItem,
    tableName: String?,
    tableCategory: String?,
    latestTableRoll: TableRollResult?,
    notesPreview: String,
    npcCount: Int,
    activeEncounter: Encounter?,
    linkedMapDayNumber: Int?,
    linkedMapPartyLocation: String?,
    linkedMythicSession: MythicSession?,
    linkedHexMapId: Long?,
    linkedClock: TensionClock?,
    deckNames: Map<Long, String>,
    handByDeck: Map<Long, List<Card>>,
    deckCardCounts: Map<Long, Int>,
    locationName: String?
): BoardBlockMeta = when (item.type) {
    SessionBoardItemType.TABLE -> {
        val title = tableName ?: item.titleOverride ?: "Tabla"
        val subtitle = tableCategory ?: "Tabla aleatoria"
        val preview = latestTableRoll?.let { "[${it.rollValue}] ${it.resolvedText}" } ?: "Sin tiradas aún"
        BoardBlockMeta(
            title = title,
            subtitle = subtitle,
            icon = DeckIcons.Table,
            color = Color(0xFFE91E63),
            preview = preview,
            footnote = latestTableRoll?.let { "Última tirada guardada" } ?: "Lista para tirar",
            ctaLabel = "Tirar"
        )
    }
    SessionBoardItemType.NOTES -> {
        val title = item.titleOverride ?: "Notas de Sesión"
        BoardBlockMeta(
            title = title,
            subtitle = "Bitácora",
            icon = DeckIcons.Journal,
            color = Color(0xFF4CAF50),
            preview = notesPreview.ifBlank { "Sin notas guardadas" },
            footnote = "Notas persistentes",
            ctaLabel = "Editar"
        )
    }
    SessionBoardItemType.CLOCK -> {
        val title = linkedClock?.name ?: item.titleOverride ?: "Reloj de Tensión"
        val subtitle = linkedClock?.let { "${it.progress}/${it.segments} segmentos" } ?: "Reloj"
        BoardBlockMeta(
            title = title,
            subtitle = subtitle,
            icon = Icons.Default.Schedule,
            color = Color(0xFFFF9800),
            preview = linkedClock?.let { if (it.isCompleted) "¡Reloj completado!" else "${it.progress}/${it.segments} completados" } ?: "Sin reloj vinculado",
            footnote = if (linkedClock?.isCompleted == true) "Completado" else "En progreso",
            ctaLabel = "Avanzar"
        )
    }
    SessionBoardItemType.DICE -> {
        val title = item.titleOverride ?: "Lanzador de Dados"
        BoardBlockMeta(
            title = title,
            subtitle = "Dados rápidos",
            icon = DeckIcons.Dice,
            color = Color(0xFF9C27B0),
            preview = "d4, d6, d8, d10, d12, d20, d100",
            footnote = "Presiona para tirar dados",
            ctaLabel = "Tirar"
        )
    }
    SessionBoardItemType.DECK -> {
        val deckId = item.resourceId ?: 0L
        val name = deckNames[deckId] ?: item.titleOverride ?: "Mazo"
        val inHand = handByDeck[deckId]?.size ?: 0
        val inDeck = deckCardCounts[deckId] ?: 0
        BoardBlockMeta(
            title = name,
            subtitle = "$inHand en mano · $inDeck en mazo",
            icon = DeckIcons.Library,
            color = Color(0xFF3F51B5),
            preview = "$inHand cartas en mano",
            footnote = "Toca para robar carta",
            ctaLabel = "Robar"
        )
    }
    SessionBoardItemType.NPCS -> {
        val title = item.titleOverride ?: "PNJs de la Sesión"
        BoardBlockMeta(
            title = title,
            subtitle = "$npcCount PNJs activos",
            icon = Icons.Default.Group,
            color = Color(0xFF009688),
            preview = "$npcCount PNJs en esta escena",
            footnote = "$npcCount personajes vinculados",
            ctaLabel = "Ver"
        )
    }
    SessionBoardItemType.COMBAT -> {
        val title = activeEncounter?.name ?: item.titleOverride ?: "Rastreador de Combate"
        val subtitle = activeEncounter?.let { "Ronda ${it.currentRound} · Turno ${it.currentTurnIndex + 1}" } ?: "Sin combate"
        BoardBlockMeta(
            title = title,
            subtitle = subtitle,
            icon = Icons.Default.CrisisAlert,
            color = Color(0xFFF44336),
            preview = activeEncounter?.let { "Encuentro activo: ${it.name}" } ?: "No hay combate activo",
            footnote = activeEncounter?.let { "Ronda ${it.currentRound}" } ?: "Combate inactivo",
            ctaLabel = "Turno"
        )
    }
    SessionBoardItemType.MAP -> {
        val hasLinkedMap = linkedHexMapId != null
        val title = locationName ?: item.titleOverride ?: if (hasLinkedMap) "Mapa Hexagonal" else "Sin Mapa Vinculado"
        val day = linkedMapDayNumber?.let { "Día $it" }
        val loc = linkedMapPartyLocation?.let { "Pos: $it" }
        val subtitle = listOfNotNull(day, loc).joinToString(" · ").ifBlank {
            if (hasLinkedMap) "Exploración" else "Toca para vincular"
        }
        BoardBlockMeta(
            title = title,
            subtitle = subtitle,
            icon = DeckIcons.Map,
            color = if (hasLinkedMap) Color(0xFF795548) else Color(0xFF757575),
            preview = if (hasLinkedMap) subtitle else "No hay un mapa asignado a esta sesión",
            footnote = if (hasLinkedMap) "Mapa activo" else "Sin mapa vinculado",
            ctaLabel = if (hasLinkedMap) "Abrir" else "Vincular"
        )
    }
    SessionBoardItemType.MYTHIC -> {
        val title = item.titleOverride ?: "Oráculo Mythic GME"
        val chaos = linkedMythicSession?.chaosFactor ?: 5
        BoardBlockMeta(
            title = title,
            subtitle = "Factor Caos: $chaos",
            icon = Icons.Default.AutoAwesome,
            color = Color(0xFF673AB7),
            preview = "Caos $chaos",
            footnote = "Oráculo narrativo",
            ctaLabel = "Preguntar"
        )
    }
    SessionBoardItemType.REFERENCE -> {
        val title = item.titleOverride ?: "Compendio & Reglas"
        BoardBlockMeta(
            title = title,
            subtitle = "Referencia rápida",
            icon = DeckIcons.Journal,
            color = Color(0xFF607D8B),
            preview = "Reglas y entradas de wiki",
            footnote = "Compendio disponible",
            ctaLabel = "Consultar"
        )
    }
    SessionBoardItemType.WIKI_MAP -> {
        val title = item.titleOverride ?: "Mapa de Wiki"
        BoardBlockMeta(
            title = title,
            subtitle = "Entrada con mapa",
            icon = Icons.Default.Place,
            color = Color(0xFF00BCD4),
            preview = "Puntos de interés",
            footnote = "Entrada vinculada",
            ctaLabel = "Ver"
        )
    }
    SessionBoardItemType.MICROSETTING -> {
        val title = item.titleOverride ?: "Microsetting de Mundo"
        val cluster = com.deckapp.core.model.MicrosettingCodec.decode(item.noteContent)
        val subtitle = if (cluster != null && cluster.items.isNotEmpty()) {
            val reg = cluster.slot(com.deckapp.core.model.MicrosettingSlotType.REGION)?.title
            val ldm = cluster.slot(com.deckapp.core.model.MicrosettingSlotType.LANDMARK)?.title
            listOfNotNull(reg, ldm).joinToString(" · ").ifBlank { "The Story Engine" }
        } else {
            "The Story Engine"
        }
        val preview = if (cluster != null && cluster.items.isNotEmpty()) {
            "${cluster.items.size} cartas conectadas"
        } else {
            "Cluster sin cartas generadas"
        }
        BoardBlockMeta(
            title = title,
            subtitle = subtitle,
            icon = Icons.Default.Public,
            color = Color(0xFF00897B),
            preview = preview,
            footnote = "The Story Engine: Deck of Worlds",
            ctaLabel = "Generar"
        )
    }
}
