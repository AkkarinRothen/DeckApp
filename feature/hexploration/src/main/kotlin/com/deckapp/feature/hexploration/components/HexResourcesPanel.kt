package com.deckapp.feature.hexploration.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.deckapp.core.model.Card
import com.deckapp.core.model.CardStack
import com.deckapp.core.model.RandomTable
import com.deckapp.core.model.SystemRule
import com.deckapp.core.model.TableRollResult
import com.deckapp.core.ui.components.CardThumbnail

@Composable
fun HexResourcesPanel(
    pinnedTables: List<RandomTable>,
    pinnedDecks: List<CardStack>,
    pinnedRules: List<SystemRule>,
    allTables: List<RandomTable>,
    allRules: List<SystemRule>,
    searchQuery: String,
    drawnCardsByDeck: Map<Long, List<Card>>,
    recentRolls: List<TableRollResult>,
    onSearchQueryChanged: (String) -> Unit,
    onRollTable: (Long) -> Unit,
    onDrawCard: (Long) -> Unit,
    onDiscardCard: (Long) -> Unit,
    onResetDeck: (Long) -> Unit
) {
    val isSearching = searchQuery.isNotBlank()
    
    val filteredTables = if (isSearching) {
        allTables.filter { it.name.contains(searchQuery, ignoreCase = true) }
    } else {
        pinnedTables
    }

    val filteredRules = if (isSearching) {
        allRules.filter { it.title.contains(searchQuery, ignoreCase = true) || it.content.contains(searchQuery, ignoreCase = true) }
    } else {
        pinnedRules
    }

    if (!isSearching && pinnedTables.isEmpty() && pinnedDecks.isEmpty() && pinnedRules.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Sin recursos configurados", style = MaterialTheme.typography.titleSmall)
            Text(
                "Agrega tablas, mazos y reglas en el editor del mapa.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Buscar tablas o reglas...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )
        }

        if (filteredTables.isNotEmpty()) {
            item {
                val title = if (isSearching) "Resultados de Tablas" else "Tablas (${pinnedTables.size})"
                CollapsibleSection(title = title) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        filteredTables.forEach { table ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    table.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                FilledTonalButton(onClick = { onRollTable(table.id) }) {
                                    Text("Tirar")
                                }
                            }
                        }
                        if (recentRolls.isNotEmpty()) {
                            Spacer(Modifier.height(4.dp))
                            Text("Últimas tiradas", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            recentRolls.forEach { roll ->
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text(roll.tableName, style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(roll.resolvedText, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                }
                HorizontalDivider()
            }
        }

        if (!isSearching && pinnedDecks.isNotEmpty()) {
            item {
                CollapsibleSection(title = "Mazos (${pinnedDecks.size})") {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        pinnedDecks.forEach { deck ->
                            val drawn = drawnCardsByDeck[deck.id] ?: emptyList()
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(deck.name, style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold)
                                        if (drawn.isNotEmpty()) {
                                            Text("${drawn.size} en mano",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    IconButton(onClick = { onResetDeck(deck.id) }) {
                                        Icon(Icons.Default.Refresh, contentDescription = "Resetear mazo",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    FilledTonalButton(onClick = { onDrawCard(deck.id) }) {
                                        Text("Robar")
                                    }
                                }
                                if (drawn.isNotEmpty()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        drawn.forEach { card ->
                                            CardThumbnail(
                                                card = card,
                                                height = 100.dp,
                                                showTitle = true,
                                                showFaceDots = false
                                            )
                                        }
                                    }
                                } else {
                                    Text("Sin cartas robadas",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
                HorizontalDivider()
            }
        }

        if (filteredRules.isNotEmpty()) {
            item {
                val title = if (isSearching) "Resultados de Reglas" else "Reglas (${pinnedRules.size})"
                CollapsibleSection(title = title) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        filteredRules.forEach { rule ->
                            ExpandableRuleCard(rule)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CollapsibleSection(
    title: String,
    content: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(true) }
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { expanded = !expanded }) {
                Icon(
                    if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "Colapsar" else "Expandir"
                )
            }
        }
        if (expanded) {
            Column(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 12.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun ExpandableRuleCard(rule: SystemRule) {
    var expanded by remember { mutableStateOf(false) }
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(rule.title, style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold)
                    if (rule.category.isNotBlank()) {
                        Text(rule.category, style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                TextButton(onClick = { expanded = !expanded }) {
                    Text(if (expanded) "Menos" else "Ver")
                }
            }
            if (expanded && rule.content.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(rule.content, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
