package com.deckapp.feature.draw.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.deckapp.core.ui.components.GameSystemsSelector

import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import com.deckapp.core.model.MythicSession

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionConfigSheet(
    selectedSystems: List<String>,
    onSystemsChanged: (List<String>) -> Unit,
    availableSystems: List<String>,
    linkedMythicSession: MythicSession?,
    allMythicSessions: List<MythicSession>,
    onLinkMythic: (Long?) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Configuración de la Sesión",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            // Sección Vinculación Mythic
            MythicLinkSection(
                currentSession = linkedMythicSession,
                allSessions = allMythicSessions,
                onLink = onLinkMythic
            )

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
}

@Composable
private fun MythicLinkSection(
    currentSession: MythicSession?,
    allSessions: List<MythicSession>,
    onLink: (Long?) -> Unit
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
