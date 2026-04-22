package com.deckapp.feature.session

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deckapp.core.model.Session

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionListScreen(
    onSessionClick: (Long) -> Unit,
    onHistoryClick: (Long) -> Unit,
    onPlannerClick: (Long) -> Unit,
    onAnalyticsClick: () -> Unit,
    onNewSession: () -> Unit,
    viewModel: SessionListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var sessionToDelete by remember { mutableStateOf<Session?>(null) }
    var sessionToRename by remember { mutableStateOf<Session?>(null) }

    // Diálogo de confirmación de borrado
    sessionToDelete?.let { session ->
        AlertDialog(
            onDismissRequest = { sessionToDelete = null },
            title = { Text("Eliminar sesión") },
            text = { Text("¿Eliminar \"${session.name}\"? Se perderá el historial de eventos. Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSession(session.id)
                        sessionToDelete = null
                    }
                ) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { sessionToDelete = null }) { Text("Cancelar") }
            }
        )
    }

    // Diálogo de renombrado
    sessionToRename?.let { session ->
        RenameSessionDialog(
            currentName = session.name,
            onConfirm = { newName ->
                viewModel.renameSession(session.id, newName)
                sessionToRename = null
            },
            onDismiss = { sessionToRename = null }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sesiones") },
                actions = {
                    IconButton(onClick = onAnalyticsClick) {
                        Icon(Icons.Default.TrendingUp, contentDescription = "Analíticas Globales")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNewSession) {
                Icon(Icons.Default.Add, contentDescription = "Nueva sesión")
            }
        }
    ) { padding ->
        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            uiState.activeSessions.isEmpty() && uiState.pastSessions.isEmpty() ->
                EmptySessionsState(onNewSession = onNewSession)

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (uiState.plannedSessions.isNotEmpty()) {
                    item {
                        Text(
                            "Planificadas",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    items(uiState.plannedSessions, key = { it.id }) { session ->
                        SessionCard(
                            session = session,
                            statusText = "Programada",
                            onClick = { onSessionClick(session.id) },
                            onPlanner = { onPlannerClick(session.id) },
                            onRename = { sessionToRename = session },
                            onClone = { viewModel.cloneSession(session.id) },
                            onDelete = { sessionToDelete = session },
                            trailingContent = {
                                Button(
                                    onClick = { viewModel.startSession(session.id) },
                                    contentPadding = PaddingValues(horizontal = 12.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text("Empezar", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        )
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                }

                if (uiState.activeSessions.isNotEmpty()) {
                    item {
                        Text(
                            "Activas",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    items(uiState.activeSessions, key = { it.id }) { session ->
                        SessionCard(
                            session = session,
                            statusText = "En curso",
                            onClick = { onSessionClick(session.id) },
                            onPlanner = { onPlannerClick(session.id) },
                            onRename = { sessionToRename = session },
                            onClone = { viewModel.cloneSession(session.id) },
                            onDelete = { sessionToDelete = session },
                            trailingContent = {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = "Continuar sesión",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        )
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                }

                if (uiState.pastSessions.isNotEmpty()) {
                    item {
                        Text(
                            "Historial",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    items(uiState.pastSessions, key = { it.id }) { session ->
                        SessionCard(
                            session = session,
                            statusText = "Finalizada",
                            onClick = { onHistoryClick(session.id) },
                            onPlanner = { onPlannerClick(session.id) },
                            onRename = { sessionToRename = session },
                            onClone = { viewModel.cloneSession(session.id) },
                            onDelete = { sessionToDelete = session }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionCard(
    session: Session,
    statusText: String,
    onClick: () -> Unit,
    onPlanner: () -> Unit,
    onRename: () -> Unit,
    onClone: () -> Unit,
    onDelete: () -> Unit,
    trailingContent: @Composable () -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = session.name, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            trailingContent()

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Opciones de sesión")
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Planificar") },
                        onClick = {
                            showMenu = false
                            onPlanner()
                        },
                        leadingIcon = { Icon(Icons.Default.HistoryEdu, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Renombrar") },
                        onClick = {
                            showMenu = false
                            onRename()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Duplicar") },
                        onClick = {
                            showMenu = false
                            onClone()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Eliminar", color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            showMenu = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun RenameSessionDialog(
    currentName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Renombrar sesión") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Nombre de la sesión") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (text.isNotBlank()) onConfirm(text) },
                enabled = text.isNotBlank()
            ) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun EmptySessionsState(onNewSession: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Sin sesiones", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            "Creá una sesión para empezar a robar cartas durante tu partida",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onNewSession) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Nueva sesión")
        }
    }
}
