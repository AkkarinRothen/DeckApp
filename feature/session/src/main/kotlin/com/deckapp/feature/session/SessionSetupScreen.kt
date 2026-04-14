package com.deckapp.feature.session

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deckapp.core.model.DrawMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionSetupScreen(
    onBack: () -> Unit,
    onSessionCreated: (Long) -> Unit,
    viewModel: SessionSetupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.createdSessionId) {
        uiState.createdSessionId?.let { onSessionCreated(it) }
    }

    val selectedCount = uiState.availableDecks.count { it.isSelected }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nueva sesión") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Button(
                    onClick = { viewModel.createSession() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    enabled = uiState.sessionName.isNotBlank() && selectedCount > 0
                ) {
                    Text("Crear sesión con $selectedCount mazo${if (selectedCount != 1) "s" else ""}")
                }
            }
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = uiState.sessionName,
                        onValueChange = { viewModel.updateSessionName(it) },
                        label = { Text("Nombre de la sesión") },
                        placeholder = { Text("ej. Sesión 1 — La Maldición de Strahd") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Mazos disponibles",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (uiState.availableDecks.isEmpty()) {
                    item {
                        Text(
                            "No hay mazos en la biblioteca. Importá uno primero.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(uiState.availableDecks, key = { it.deck.id }) { selection ->
                        DeckSelectionCard(
                            selection = selection,
                            onToggle = { viewModel.toggleDeckSelection(selection.deck.id) },
                            onDrawModeChange = { viewModel.updateDrawMode(selection.deck.id, it) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DeckSelectionCard(
    selection: DeckSelection,
    onToggle: () -> Unit,
    onDrawModeChange: (DrawMode) -> Unit
) {
    Card(
        onClick = onToggle,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (selection.isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = selection.deck.name,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = selection.deck.type.name.lowercase()
                            .replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Checkbox(
                    checked = selection.isSelected,
                    onCheckedChange = { onToggle() }
                )
            }

            // Draw mode selector — shown only when deck is selected
            if (selection.isSelected) {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                Text("Modo de robo", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DrawMode.entries.forEach { mode ->
                        val label = when (mode) {
                            DrawMode.TOP -> "Arriba"
                            DrawMode.BOTTOM -> "Abajo"
                            DrawMode.RANDOM -> "Aleatorio"
                        }
                        FilterChip(
                            selected = selection.drawMode == mode,
                            onClick = { onDrawModeChange(mode) },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
        }
    }
}
