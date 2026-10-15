package com.deckapp.feature.draw.tactical

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deckapp.core.model.GridType
import com.deckapp.core.model.Scene
import com.deckapp.core.model.TacticalMap

@Composable
fun TacticalCreateMapDialog(
    scenes: List<Scene>,
    activeSceneId: Long?,
    onDismiss: () -> Unit,
    onCreateMap: (name: String, imageUri: String, gridType: GridType, cellSizePx: Float, scaleFtPerCell: Int, sceneId: Long?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf("") }
    var selectedGridType by remember { mutableStateOf(GridType.SQUARE) }
    var scaleFtPerCell by remember { mutableIntStateOf(5) }
    var selectedSceneId by remember { mutableStateOf(activeSceneId) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { imageUri = it.toString() }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuevo Mapa Táctico") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Nombre del mapa
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre del Mapa") },
                    placeholder = { Text("Ej: Cripta del Nigromante, Taberna...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Imagen de fondo
                Text("Imagen de Batalla (Opcional)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Elegir Imagen", fontSize = 12.sp)
                    }

                    if (imageUri.isNotBlank()) {
                        IconButton(onClick = { imageUri = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Quitar imagen", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                if (imageUri.isNotBlank()) {
                    Text(
                        text = "Imagen seleccionada",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Tipo de Grilla
                Text("Tipo de Cuadrícula", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedGridType == GridType.SQUARE,
                        onClick = { selectedGridType = GridType.SQUARE },
                        label = { Text("Cuadrada", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Square, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    )
                    FilterChip(
                        selected = selectedGridType == GridType.HEX_FLAT,
                        onClick = { selectedGridType = GridType.HEX_FLAT },
                        label = { Text("Hexagonal", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Hexagon, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    )
                    FilterChip(
                        selected = selectedGridType == GridType.NONE,
                        onClick = { selectedGridType = GridType.NONE },
                        label = { Text("Sin Grilla", fontSize = 12.sp) }
                    )
                }

                // Escala de pies por casilla
                Text("Escala por Casilla", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(5, 10, 15).forEach { scale ->
                        FilterChip(
                            selected = scaleFtPerCell == scale,
                            onClick = { scaleFtPerCell = scale },
                            label = { Text("${scale} ft", fontSize = 12.sp) }
                        )
                    }
                }

                // Vincular a Escena
                if (scenes.isNotEmpty()) {
                    Text("Asociar a Escena del Planificador", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedSceneId == null,
                            onClick = { selectedSceneId = null },
                            label = { Text("Ninguna", fontSize = 11.sp) }
                        )
                        scenes.forEach { scene ->
                            FilterChip(
                                selected = selectedSceneId == scene.id,
                                onClick = { selectedSceneId = scene.id },
                                label = { Text(scene.title, fontSize = 11.sp) },
                                leadingIcon = { Icon(Icons.Default.Movie, contentDescription = null, modifier = Modifier.size(12.dp)) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            FilledTonalButton(
                onClick = {
                    val finalName = name.ifBlank { "Nuevo Mapa" }
                    onCreateMap(finalName, imageUri, selectedGridType, 70f, scaleFtPerCell, selectedSceneId)
                    onDismiss()
                }
            ) {
                Text("Crear Mapa")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun TacticalRenameMapDialog(
    map: TacticalMap,
    onDismiss: () -> Unit,
    onRenameConfirm: (Long, String) -> Unit
) {
    var newName by remember { mutableStateOf(map.name) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Renombrar Mapa") },
        text = {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text("Nombre") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            FilledTonalButton(
                onClick = {
                    if (newName.isNotBlank()) {
                        onRenameConfirm(map.id, newName)
                    }
                    onDismiss()
                }
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun TacticalLinkSceneDialog(
    map: TacticalMap,
    scenes: List<Scene>,
    onDismiss: () -> Unit,
    onLinkConfirm: (Long, Long?) -> Unit
) {
    var selectedSceneId by remember { mutableStateOf(map.sceneId) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Vincular Mapa a Escena") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Selecciona qué escena narrativa activará o sugerirá este mapa:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )

                Surface(
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                    color = if (selectedSceneId == null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedSceneId = null }
                ) {
                    Text(
                        "Ninguna (Sin escena vinculada)",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                scenes.forEach { scene ->
                    val isSelected = selectedSceneId == scene.id
                    Surface(
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedSceneId = scene.id }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Movie, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text(scene.title, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        },
        confirmButton = {
            FilledTonalButton(
                onClick = {
                    onLinkConfirm(map.id, selectedSceneId)
                    onDismiss()
                }
            ) {
                Text("Vincular")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
