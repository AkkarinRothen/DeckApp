package com.deckapp.feature.session

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.deckapp.core.model.DrawAction
import com.deckapp.core.model.SessionLogEventType
import com.deckapp.core.ui.components.DeckCard
import com.deckapp.core.ui.components.MarkdownText
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionHistoryScreen(
    onBack: () -> Unit,
    viewModel: SessionHistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Historial de sesión") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    SessionHeader(uiState)
                }

                item {
                    Text(
                        text = "Actividad y Registro",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                item {
                    HistoryFilterBar(
                        searchQuery = uiState.searchQuery,
                        selectedCategory = uiState.categoryFilter,
                        onCategorySelected = viewModel::setCategoryFilter,
                        onSearchChange = viewModel::setSearchQuery
                    )
                }

                if (uiState.filteredTimeline.isEmpty()) {
                    item {
                        Text(
                            "No hay eventos que coincidan con los filtros seleccionados.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    items(uiState.filteredTimeline, key = { event ->
                        when (event) {
                            is TimelineEvent.CardEvent -> "card_${event.event.id}_${event.timestamp}"
                            is TimelineEvent.TableEvent -> "table_${event.result.id}_${event.timestamp}"
                            is TimelineEvent.LogEvent -> "log_${event.entry.id}_${event.timestamp}"
                        }
                    }) { event ->
                        TimelineItem(event)
                    }
                }

                if (!uiState.session?.dmNotes.isNullOrBlank()) {
                    item {
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "Notas del DM",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Box(Modifier.padding(16.dp)) {
                                MarkdownText(
                                    markdown = uiState.session?.dmNotes ?: "",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(32.dp)) }
            }
        }
    }
}

@Composable
private fun SessionHeader(uiState: SessionHistoryUiState) {
    val session = uiState.session ?: return
    val stats = uiState.stats
    val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    val dateText = dateFormat.format(Date(session.createdAt))

    DeckCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(
                        text = session.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = dateText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f),
                    shape = CircleShape
                ) {
                    Text(
                        text = if (session.endedAt != null) "Finalizada" else "Activa",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Main Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatItem(
                    label = "Robadas",
                    value = stats.totalDrawn.toString(),
                    icon = Icons.Default.Style,
                    modifier = Modifier.weight(1f)
                )
                StatItem(
                    label = "Tiradas",
                    value = stats.totalRolls.toString(),
                    icon = Icons.Default.Casino,
                    modifier = Modifier.weight(1f)
                )
                StatItem(
                    label = "Duración",
                    value = "${stats.durationMinutes}m",
                    icon = Icons.Default.Timer,
                    modifier = Modifier.weight(1f)
                )
            }

            if (stats.mostUsedDeckName != null) {
                HorizontalDivider(Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.TrendingUp,
                        null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Mazo más activo: ${stats.mostUsedDeckName}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
        Spacer(Modifier.width(8.dp))
        Column {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun HistoryFilterBar(
    searchQuery: String,
    selectedCategory: HistoryCategoryFilter,
    onCategorySelected: (HistoryCategoryFilter) -> Unit,
    onSearchChange: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            placeholder = { Text("Buscar evento, carta, tabla...") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = if (searchQuery.isNotEmpty()) {
                { IconButton(onClick = { onSearchChange("") }) { Icon(Icons.Default.Close, null) } }
            } else null,
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        // Fila horizontal de chips rápidos (Todos | Dados y Tablas | Cartas | Combate | Relojes)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedCategory == HistoryCategoryFilter.ALL,
                onClick = { onCategorySelected(HistoryCategoryFilter.ALL) },
                label = { Text("Todos") }
            )
            FilterChip(
                selected = selectedCategory == HistoryCategoryFilter.DICE_AND_TABLES,
                onClick = { onCategorySelected(HistoryCategoryFilter.DICE_AND_TABLES) },
                label = { Text("🎲 Dados y Tablas") }
            )
            FilterChip(
                selected = selectedCategory == HistoryCategoryFilter.CARDS,
                onClick = { onCategorySelected(HistoryCategoryFilter.CARDS) },
                label = { Text("🃏 Cartas") }
            )
            FilterChip(
                selected = selectedCategory == HistoryCategoryFilter.COMBAT,
                onClick = { onCategorySelected(HistoryCategoryFilter.COMBAT) },
                label = { Text("⚔️ Combate") }
            )
            FilterChip(
                selected = selectedCategory == HistoryCategoryFilter.CLOCKS,
                onClick = { onCategorySelected(HistoryCategoryFilter.CLOCKS) },
                label = { Text("⏰ Relojes") }
            )
        }

        HorizontalDivider(modifier = Modifier.padding(top = 10.dp))
    }
}

@Composable
private fun TimelineItem(event: TimelineEvent) {
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val timeText = timeFormat.format(Date(event.timestamp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(48.dp)
        ) {
            Text(
                text = timeText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        when (event) {
                            is TimelineEvent.CardEvent -> MaterialTheme.colorScheme.secondary
                            is TimelineEvent.TableEvent -> MaterialTheme.colorScheme.tertiary
                            is TimelineEvent.LogEvent -> when (event.entry.eventType) {
                                SessionLogEventType.DICE -> Color(0xFFFFB74D)
                                SessionLogEventType.CLOCK -> Color(0xFF42A5F5)
                                SessionLogEventType.TOKEN -> Color(0xFFEF5350)
                                else -> MaterialTheme.colorScheme.primary
                            }
                        },
                        CircleShape
                    )
            )
        }

        Spacer(Modifier.width(12.dp))

        Surface(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.surfaceContainer,
            shape = RoundedCornerShape(12.dp)
        ) {
            when (event) {
                is TimelineEvent.CardEvent -> CardEventContent(event)
                is TimelineEvent.TableEvent -> TableEventContent(event)
                is TimelineEvent.LogEvent -> LogEventContent(event)
            }
        }
    }
}

@Composable
private fun CardEventContent(event: TimelineEvent.CardEvent) {
    Row(
        modifier = Modifier.padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val icon = when (event.event.action) {
            DrawAction.DRAW -> Icons.Default.Style
            DrawAction.DISCARD -> Icons.Default.Delete
            DrawAction.FLIP -> Icons.Default.Refresh
            DrawAction.RESET -> Icons.Default.Restore
            DrawAction.PEEK -> Icons.Default.Visibility
            else -> Icons.Default.Info
        }
        val actionText = when (event.event.action) {
            DrawAction.DRAW -> "Robó"
            DrawAction.DISCARD -> "Descartó"
            DrawAction.FLIP -> "Giró"
            DrawAction.RESET -> "Reseteó mazo"
            DrawAction.PEEK -> "Vió tope"
            else -> event.event.action.name
        }

        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.secondary
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = actionText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val cardText = event.card?.title
                ?: event.event.cardId?.takeIf { it > 0 }?.let { "Carta #$it" }
                ?: event.event.metadata?.takeIf { it.isNotBlank() }
            if (cardText != null) {
                Text(
                    text = cardText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        event.card?.activeFace?.imagePath?.let { path ->
            AsyncImage(
                model = File(path),
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Gray.copy(alpha = 0.1f)),
                contentScale = ContentScale.Crop
            )
        }
    }
}

@Composable
private fun TableEventContent(event: TimelineEvent.TableEvent) {
    val result = event.result
    Row(
        modifier = Modifier.padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Casino,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.tertiary
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Tiró ${result.tableName}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = result.resolvedText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
        Surface(
            color = MaterialTheme.colorScheme.tertiary,
            shape = MaterialTheme.shapes.extraSmall
        ) {
            Text(
                text = result.rollValue.toString(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onTertiary,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun LogEventContent(event: TimelineEvent.LogEvent) {
    val entry = event.entry
    val (icon, tint) = when (entry.eventType) {
        SessionLogEventType.DICE -> Icons.Default.Casino to Color(0xFFFFB74D)
        SessionLogEventType.CLOCK -> Icons.Default.Schedule to Color(0xFF42A5F5)
        SessionLogEventType.TOKEN -> Icons.Default.SportsKabaddi to Color(0xFFEF5350)
        SessionLogEventType.ORACLE -> Icons.Default.AutoAwesome to Color(0xFFAB47BC)
        SessionLogEventType.CARD -> Icons.Default.Style to MaterialTheme.colorScheme.secondary
        SessionLogEventType.MANUAL -> Icons.Default.EditNote to MaterialTheme.colorScheme.primary
    }

    Row(
        modifier = Modifier.padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = tint
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (entry.details.isNotBlank()) {
                Text(
                    text = entry.details,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
