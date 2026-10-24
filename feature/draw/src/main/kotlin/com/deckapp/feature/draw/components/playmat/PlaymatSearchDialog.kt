package com.deckapp.feature.draw.components.playmat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.geometry.Offset
import com.deckapp.core.model.*

internal enum class PlaymatSearchKind(val label: String, val itemLabel: String) {
    ALL("Todo", "Elemento"), CARD("Cartas", "Carta"), DECK("Mazos", "Mazo"), DISCARD("Descartes", "Descarte"),
    TOKEN("Personajes", "Personaje"), CLOCK("Relojes", "Reloj"), NOTE("Notas", "Nota"), RESULT("Resultados", "Resultado"),
    GROUP("Grupos", "Grupo"), CONNECTION("Conexiones", "Conexión")
}

internal data class PlaymatSearchResult(
    val id: String,
    val title: String,
    val kind: PlaymatSearchKind,
    val context: String,
    val position: Offset? = null,
    val hiddenGroupIds: Set<String> = emptySet(),
    val focusGroupId: String? = null,
    val groupIds: Set<String> = emptySet()
)

internal fun buildPlaymatSearchResults(
    surface: PlaymatSurface,
    cardsById: Map<Long, Card>,
    deckNames: Map<Long, String>
): List<PlaymatSearchResult> {
    val hiddenGroupIds = surface.groups.filter { it.isHidden }.mapTo(mutableSetOf()) { it.id }
    fun hiddenGroup(groupId: String?) = if (groupId != null && groupId in hiddenGroupIds) setOf(groupId) else emptySet()
    fun addIfHidden(target: MutableSet<String>, groupId: String?) {
        if (groupId != null && groupId in hiddenGroupIds) target.add(groupId)
    }
    fun deckName(stackId: Long) = deckNames[stackId]?.takeIf { it.isNotBlank() } ?: "Mazo #$stackId"

    val results = buildList {
        surface.cards.forEach { placement ->
            val card = cardsById[placement.cardId]
            add(
                PlaymatSearchResult(
                    id = "card:${placement.cardId}",
                    title = card?.title?.takeIf { it.isNotBlank() } ?: "Carta #${placement.cardId}",
                    kind = PlaymatSearchKind.CARD,
                    context = card?.let { deckName(it.originDeckId ?: it.stackId) } ?: "Carta en la mesa",
                    position = Offset(placement.x + 60f, placement.y + 90f),
                    hiddenGroupIds = hiddenGroup(placement.clusterId)
                )
            )
        }
        surface.decks.forEach { deck ->
            val name = deckName(deck.stackId)
            add(
                PlaymatSearchResult(
                    id = "deck:${deck.stackId}", title = name, kind = PlaymatSearchKind.DECK,
                    context = "Mazo en la mesa", position = Offset(deck.x + 60f, deck.y + 90f),
                    hiddenGroupIds = hiddenGroup(deck.groupId)
                )
            )
            add(
                PlaymatSearchResult(
                    id = "discard:${deck.stackId}", title = "Descarte · $name", kind = PlaymatSearchKind.DISCARD,
                    context = "Pila de descarte", position = Offset((deck.discardX ?: deck.x + 150f) + 60f, (deck.discardY ?: deck.y) + 90f),
                    hiddenGroupIds = hiddenGroup(deck.discardGroupId)
                )
            )
        }
        surface.tokens.forEach { token ->
            add(
                PlaymatSearchResult(
                    id = "token:${token.id}", title = token.name, kind = PlaymatSearchKind.TOKEN,
                    context = when (token.tokenType) {
                        PlaymatTokenType.ALLY -> "Aliado · PG ${token.currentHp}/${token.maxHp}"
                        PlaymatTokenType.NEUTRAL -> "Neutral · PG ${token.currentHp}/${token.maxHp}"
                        PlaymatTokenType.ENEMY -> "Enemigo · PG ${token.currentHp}/${token.maxHp}"
                    },
                    position = Offset(token.x + 32f, token.y + 32f), hiddenGroupIds = hiddenGroup(token.groupId)
                )
            )
        }
        surface.clocks.forEach { clock ->
            add(
                PlaymatSearchResult(
                    id = "clock:${clock.id}", title = clock.title, kind = PlaymatSearchKind.CLOCK,
                    context = "${clock.filledSegments}/${clock.totalSegments} segmentos",
                    position = Offset(clock.x + 38f, clock.y + 38f), hiddenGroupIds = hiddenGroup(clock.groupId)
                )
            )
        }
        surface.stickyNotes.forEach { note ->
            add(
                PlaymatSearchResult(
                    id = "note:${note.id}", title = note.text.ifBlank { "Nota sin texto" }, kind = PlaymatSearchKind.NOTE,
                    context = "Nota adhesiva", position = Offset(note.x + 50f, note.y + 50f),
                    hiddenGroupIds = hiddenGroup(note.groupId)
                )
            )
        }
        surface.tableChips.forEach { chip ->
            add(
                PlaymatSearchResult(
                    id = "chip:${chip.id}", title = chip.tableName.ifBlank { "Resultado de tabla" },
                    kind = PlaymatSearchKind.RESULT, context = chip.rollResultText,
                    position = Offset(chip.x + 90f, chip.y + 24f), hiddenGroupIds = hiddenGroup(chip.groupId)
                )
            )
        }
        val currentMemberIds = buildSet {
            surface.cards.forEach { add("card:${it.cardId}") }
            surface.decks.forEach {
                add("deck:${it.stackId}")
                if (it.discardX != null || it.discardY != null) add("discard:${it.stackId}")
            }
            surface.tokens.forEach { add("token:${it.id}") }
            surface.clocks.forEach { add("clock:${it.id}") }
            surface.stickyNotes.forEach { add("note:${it.id}") }
            surface.tableChips.forEach { add("chip:${it.id}") }
        }
        surface.groups.forEach { group ->
            val count = group.memberIds.count { it in currentMemberIds }
            add(
                PlaymatSearchResult(
                    id = "group:${group.id}", title = group.name, kind = PlaymatSearchKind.GROUP,
                    context = if (count == 0) "Grupo vacío" else "$count ${if (count == 1) "pieza" else "piezas"}",
                    hiddenGroupIds = hiddenGroup(group.id), focusGroupId = group.id
                )
            )
        }
        val endpointLabels = buildMap {
            surface.cards.forEach { placement ->
                val title = cardsById[placement.cardId]?.title?.takeIf { it.isNotBlank() } ?: "Carta #${placement.cardId}"
                put("card_${placement.cardId}", title)
            }
            surface.tokens.forEach { put("token_${it.id}", it.name) }
            surface.clocks.forEach { put("clock_${it.id}", it.title) }
        }
        val endpointPositions = buildMap {
            surface.cards.forEach { put("card_${it.cardId}", Offset(it.x + 60f, it.y + 18f)) }
            surface.tokens.forEach { put("token_${it.id}", Offset(it.x + 32f, it.y + 32f)) }
            surface.clocks.forEach { put("clock_${it.id}", Offset(it.x + 38f, it.y + 38f)) }
        }
        surface.connections.forEach { connection ->
            val from = connection.effectiveFromId
            val to = connection.effectiveToId
            val fromPosition = endpointPositions[from]
            val toPosition = endpointPositions[to]
            val connectionDistance = if (fromPosition != null && toPosition != null) {
                kotlin.math.hypot(toPosition.x - fromPosition.x, toPosition.y - fromPosition.y)
            } else 0f
            val hiddenEndpoints = buildSet {
                addIfHidden(this, surface.cards.firstOrNull { "card_${it.cardId}" == from }?.clusterId)
                addIfHidden(this, surface.cards.firstOrNull { "card_${it.cardId}" == to }?.clusterId)
                addIfHidden(this, surface.tokens.firstOrNull { "token_${it.id}" == from }?.groupId)
                addIfHidden(this, surface.tokens.firstOrNull { "token_${it.id}" == to }?.groupId)
                addIfHidden(this, surface.clocks.firstOrNull { "clock_${it.id}" == from }?.groupId)
                addIfHidden(this, surface.clocks.firstOrNull { "clock_${it.id}" == to }?.groupId)
            }
            val endpointContext = "${endpointLabels[from] ?: "Elemento"} → ${endpointLabels[to] ?: "Elemento"}"
            add(
                PlaymatSearchResult(
                    id = "connection:${connection.id}", title = connection.label.ifBlank { "Conexión" },
                    kind = PlaymatSearchKind.CONNECTION, context = endpointContext,
                    position = if (fromPosition != null && toPosition != null) {
                        Offset(
                            (fromPosition.x + toPosition.x) / 2f,
                            (fromPosition.y + toPosition.y) / 2f + minOf(connectionDistance * 0.05f, 17.5f)
                        )
                    } else null,
                    hiddenGroupIds = hiddenEndpoints
                )
            )
        }
    }
    return results.map { result ->
        val memberIds = when {
            result.kind != PlaymatSearchKind.CONNECTION -> setOf(result.id)
            else -> {
                val connectionId = result.id.removePrefix("connection:")
                val connection = surface.connections.firstOrNull { it.id == connectionId }
                val endpoints = listOfNotNull(connection?.effectiveFromId, connection?.effectiveToId)
                endpoints.mapNotNullTo(mutableSetOf()) { endpoint ->
                    when {
                        endpoint.startsWith("card_") -> "card:${endpoint.removePrefix("card_")}"
                        endpoint.startsWith("token_") -> "token:${endpoint.removePrefix("token_")}"
                        endpoint.startsWith("clock_") -> "clock:${endpoint.removePrefix("clock_")}"
                        else -> null
                    }
                }
            }
        }
        val groupIds = surface.groups.filter { group -> group.memberIds.any { it in memberIds } }.mapTo(mutableSetOf()) { it.id }
        result.copy(groupIds = groupIds)
    }
}

internal fun filterPlaymatSearchResults(
    results: List<PlaymatSearchResult>,
    query: String,
    kind: PlaymatSearchKind
): List<PlaymatSearchResult> {
    val normalizedQuery = query.trim()
    return results.filter { result ->
        (kind == PlaymatSearchKind.ALL || result.kind == kind) &&
            (normalizedQuery.isEmpty() || result.title.contains(normalizedQuery, ignoreCase = true) || result.context.contains(normalizedQuery, ignoreCase = true))
    }
}

@Composable
internal fun PlaymatSearchDialog(
    surface: PlaymatSurface,
    cardsById: Map<Long, Card>,
    deckNames: Map<Long, String>,
    onDismiss: () -> Unit,
    onFocus: (PlaymatSearchResult) -> Unit
) {
    var query by rememberSaveable(surface.id) { mutableStateOf("") }
    var selectedKind by remember(surface.id) { mutableStateOf(PlaymatSearchKind.ALL) }
    val results = remember(surface, cardsById, deckNames) { buildPlaymatSearchResults(surface, cardsById, deckNames) }
    val filteredResults = remember(results, query, selectedKind) { filterPlaymatSearchResults(results, query, selectedKind) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Buscar en el playmat") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().widthIn(min = 280.dp, max = 560.dp).heightIn(max = 560.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Nombre, texto o resultado") },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(PlaymatSearchKind.values().toList(), key = { it.name }) { kind ->
                        FilterChip(
                            selected = selectedKind == kind,
                            onClick = { selectedKind = kind },
                            label = { Text(kind.label) }
                        )
                    }
                }
                if (filteredResults.isEmpty()) {
                    Text("No se encontraron piezas.", style = MaterialTheme.typography.bodyMedium)
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(filteredResults, key = { it.id }) { result ->
                            val hidden = result.hiddenGroupIds.isNotEmpty()
                            ListItem(
                                headlineContent = { Text(result.title, maxLines = 1) },
                                supportingContent = {
                                    Column {
                                        Text("${result.kind.itemLabel} · ${result.context}", maxLines = 2)
                                        if (hidden) Text("Pertenece a un grupo oculto", color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.labelSmall)
                                    }
                                },
                                trailingContent = if (hidden) {
                                    {
                                        TextButton(onClick = { onFocus(result) }, enabled = result.position != null || result.focusGroupId != null) {
                                            Text("Mostrar y enfocar")
                                        }
                                    }
                                } else {
                                    {
                                        AssistChip(
                                            onClick = { onFocus(result) },
                                            enabled = result.position != null || result.focusGroupId != null,
                                            label = { Text("Enfocar") },
                                            leadingIcon = { Icon(Icons.Default.CenterFocusStrong, contentDescription = null) }
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().clickable(enabled = !hidden && (result.position != null || result.focusGroupId != null)) { onFocus(result) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } }
    )
}
