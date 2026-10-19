package com.deckapp.feature.draw.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.deckapp.core.model.*
import com.deckapp.core.ui.components.GameSystemsSelector

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionConfigSheet(
    sessionName: String = "",
    onRenameSession: (String) -> Unit = {},
    selectedSystems: List<String> = emptyList(),
    onSystemsChanged: (List<String>) -> Unit = {},
    availableSystems: List<String> = emptyList(),
    boardViewLabels: SessionBoardViewLabels = SessionBoardViewLabels(),
    onBoardViewLabelChanged: (SessionBoardView, String) -> Unit = { _, _ -> },
    linkedMythicSession: MythicSession? = null,
    allMythicSessions: List<MythicSession> = emptyList(),
    onLinkMythic: (Long?) -> Unit = {},
    onCreateAndLinkMythic: () -> Unit = {},
    linkedHexMapId: Long? = null,
    allHexMaps: List<HexMap> = emptyList(),
    onLinkHexMap: (Long?) -> Unit = {},
    onResetFloatingPanels: () -> Unit = {},
    allDecks: List<CardStack> = emptyList(),
    sessionDeckRefs: List<SessionDeckRef> = emptyList(),
    onToggleDeck: (Long) -> Unit = {},
    onOpenDeckPicker: () -> Unit = {},
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showRenameDialog by remember { mutableStateOf(false) }
    var tempSessionName by remember(sessionName) { mutableStateOf(sessionName) }
    var showHexMapPicker by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Configuración de la Sesión",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onResetFloatingPanels) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reiniciar paneles flotantes")
                }
            }

            // Nombre de la sesión
            OutlinedCard(
                onClick = { showRenameDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Nombre de la sesión",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            sessionName.ifBlank { "Sin nombre" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Icon(Icons.Default.Edit, contentDescription = "Renombrar")
                }
            }

            // Sección Vinculación Mythic
            MythicLinkSection(
                currentSession = linkedMythicSession,
                allSessions = allMythicSessions,
                onLink = onLinkMythic,
                onCreateNew = onCreateAndLinkMythic
            )

            HorizontalDivider()

            // Sección Vinculación Mapa Hex
            val linkedMap = allHexMaps.find { it.id == linkedHexMapId }
            Column {
                Text(
                    "Mapa Hexcrawl Vinculado",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(8.dp))
                OutlinedCard(
                    onClick = { showHexMapPicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Map, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                linkedMap?.name ?: "Sin mapa vinculado",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (linkedMap != null) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                        if (linkedMap != null) {
                            IconButton(onClick = { onLinkHexMap(null) }) {
                                Icon(Icons.Default.LinkOff, null, tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
            HorizontalDivider()

            // Sección Mazos de la Sesión
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Mazos en la Mesa",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "${sessionDeckRefs.size} activos",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "Gestiona qué mazos de tu biblioteca o expansiones están disponibles en la barra lateral para robar cartas.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                OutlinedCard(
                    onClick = onOpenDeckPicker,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Style,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            val activeNames = sessionDeckRefs.mapNotNull { ref ->
                                allDecks.find { it.id == ref.stackId }?.name
                            }
                            Text(
                                text = if (activeNames.isNotEmpty()) activeNames.joinToString(", ") else "Ningún mazo activo",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (activeNames.isNotEmpty()) FontWeight.SemiBold else FontWeight.Normal,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Toca para agregar, quitar o cambiar mazos",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Gestionar mazos",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            HorizontalDivider()

            Text(
                "Sistemas de juego",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "Configura qué sistemas se usan en esta sesión para filtrar las tablas de referencia y reglas.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            GameSystemsSelector(
                selectedSystems = selectedSystems,
                onSystemsChanged = onSystemsChanged,
                availableSystems = availableSystems
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Listo")
            }
        }
    }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Renombrar Sesión") },
            text = {
                OutlinedTextField(
                    value = tempSessionName,
                    onValueChange = { tempSessionName = it },
                    label = { Text("Nombre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRenameSession(tempSessionName.trim())
                        showRenameDialog = false
                    }
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showHexMapPicker) {
        AlertDialog(
            onDismissRequest = { showHexMapPicker = false },
            title = { Text("Seleccionar Mapa Hexcrawl") },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    if (allHexMaps.isEmpty()) {
                        item { Text("No hay mapas Hexcrawl creados.") }
                    }
                    items(allHexMaps) { map ->
                        ListItem(
                            headlineContent = { Text(map.name) },
                            modifier = Modifier.clickable {
                                onLinkHexMap(map.id)
                                showHexMapPicker = false
                            }
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showHexMapPicker = false }) { Text("Cerrar") } }
        )
    }
}

@Composable
private fun MythicLinkSection(
    currentSession: MythicSession?,
    allSessions: List<MythicSession>,
    onLink: (Long?) -> Unit,
    onCreateNew: () -> Unit = {}
) {
    var showPicker by remember { mutableStateOf(false) }

    Column {
        Text("Vinculación Mythic", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Text("Conecta esta sesión a un Oráculo de Mythic para compartir el Factor de Caos e historial.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))

        OutlinedCard(
            onClick = { showPicker = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.AutoMirrored.Filled.MenuBook, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        currentSession?.name ?: "Vincular a una Crónica Mythic",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (currentSession != null) FontWeight.Bold else FontWeight.Normal
                    )
                    if (currentSession != null) {
                        Text("Factor de Caos: ${currentSession.chaosFactor}", style = MaterialTheme.typography.bodySmall)
                    }
                }
                if (currentSession != null) {
                    IconButton(onClick = { onLink(null) }) {
                        Icon(Icons.Default.LinkOff, null, tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }

    if (showPicker) {
        AlertDialog(
            onDismissRequest = { showPicker = false },
            title = { Text("Seleccionar Crónica Mythic") },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    item {
                        ListItem(
                            headlineContent = { Text("Crear nueva crónica Mítica") },
                            leadingContent = { Icon(Icons.Default.Add, null) },
                            modifier = Modifier.clickable {
                                onCreateNew()
                                showPicker = false
                            }
                        )
                    }
                    if (allSessions.isEmpty()) {
                        item { Text("No hay sesiones Mythic creadas.") }
                    }
                    items(allSessions) { mythic ->
                        ListItem(
                            headlineContent = { Text(mythic.name) },
                            supportingContent = { Text("Caos ${mythic.chaosFactor}") },
                            modifier = Modifier.clickable { 
                                onLink(mythic.id)
                                showPicker = false 
                            }
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showPicker = false }) { Text("Cerrar") } }
        )
    }
}
