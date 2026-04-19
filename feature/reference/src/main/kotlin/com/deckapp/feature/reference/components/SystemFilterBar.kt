package com.deckapp.feature.reference.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SystemFilterBar(
    availableSystems: List<String>,
    activeFilters: Set<String>,
    onToggleFilter: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        item {
            FilterChip(
                selected = activeFilters.isEmpty(),
                onClick = { /* Clear all filters handled by UI logic elsewhere or by sending empty list */ },
                label = { Text("Todos") }
            )
        }
        
        items(availableSystems) { system ->
            FilterChip(
                selected = activeFilters.contains(system),
                onClick = { onToggleFilter(system) },
                label = { Text(system) }
            )
        }
    }
}
