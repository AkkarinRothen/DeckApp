package com.deckapp.feature.draw.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.deckapp.core.model.*
import com.deckapp.core.ui.util.toCoilModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MicrosettingBuilderDialog(
    cluster: MicrosettingCluster?,
    onDismiss: () -> Unit,
    onGenerateAll: () -> Unit,
    onRotateSlot: (MicrosettingSlotType) -> Unit,
    onSetSlotRotation: (MicrosettingSlotType, Int) -> Unit,
    onRedrawSlot: (MicrosettingSlotType) -> Unit,
    onCopyToNotes: (MicrosettingCluster) -> Unit,
    onSaveToBoard: (MicrosettingCluster) -> Unit,
    onOpenCard: (Long) -> Unit = {}
) {
    var notesInput by remember(cluster?.notes) { mutableStateOf(cluster?.notes.orEmpty()) }
    val currentCluster = cluster?.copy(notes = notesInput)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    text = "Generador de Microsetting",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "The Story Engine: Deck of Worlds",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.Close, contentDescription = "Cerrar")
                            }
                        },
                        actions = {
                            FilledTonalButton(
                                onClick = onGenerateAll,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Icon(Icons.Default.Casino, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Robar Todo")
                            }
                        }
                    )
                },
                bottomBar = {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        tonalElevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { currentCluster?.let { onCopyToNotes(it) } },
                                enabled = currentCluster != null && currentCluster.items.isNotEmpty(),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Copiar a Notas")
                            }

                            Button(
                                onClick = { currentCluster?.let { onSaveToBoard(it) } },
                                enabled = currentCluster != null && currentCluster.items.isNotEmpty(),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.PushPin, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Guardar en Tablero")
                            }
                        }
                    }
                }
            ) { padding ->
                if (currentCluster == null || currentCluster.items.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Public,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Sin cartas de microsetting generadas",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Presiona 'Robar Todo' para extraer una carta de cada mazo. Cada tipo usa sus opciones reales: una, dos o cuatro pistas.",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(onClick = onGenerateAll) {
                                Icon(Icons.Default.Casino, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Generar Microsetting Completo")
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        item {
                            OutlinedTextField(
                                value = notesInput,
                                onValueChange = { notesInput = it },
                                label = { Text("Síntesis del DM / Notas de la Región (opcional)") },
                                placeholder = { Text("Ej: Un páramo de hielo custodiado por autómatas donde una reliquia ancestral fue profanada...") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2,
                                maxLines = 4
                            )
                        }

                        items(currentCluster.items, key = { it.slotType.name }) { cardItem ->
                            MicrosettingSlotDetailCard(
                                item = cardItem,
                                onRotate = { onRotateSlot(cardItem.slotType) },
                                onSelectCue = { rotation -> onSetSlotRotation(cardItem.slotType, rotation) },
                                onRedraw = { onRedrawSlot(cardItem.slotType) },
                                onCardClick = { onOpenCard(cardItem.cardId) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MicrosettingSlotDetailCard(
    item: MicrosettingCardItem,
    onRotate: () -> Unit,
    onSelectCue: (Int) -> Unit,
    onRedraw: () -> Unit,
    onCardClick: () -> Unit
) {
    val slotColor = Color(item.slotType.colorHex)
    val orientationLabels = item.optionLabels
    val rotations = item.optionRotations

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        border = BorderStroke(1.dp, slotColor.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Header: Slot Badge + Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = slotColor.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = item.slotType.displayName.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = slotColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onRotate, enabled = rotations.size > 1, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Siguiente opción",
                            tint = slotColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onRedraw, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.Casino,
                            contentDescription = "Redibujar carta",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Central Area: Artwork + Compass Cue Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Artwork 3x3 square with active rotation
                Box(
                    modifier = Modifier
                        .size(92.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .clickable { onCardClick() }
                ) {
                    AsyncImage(
                        model = toCoilModel(item.imagePath),
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .rotate(item.rotation.toFloat())
                    )
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp),
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.75f)
                    ) {
                        val label = orientationLabels.getOrElse(rotations.indexOf(item.rotation).coerceAtLeast(0)) { "?" }
                        Text(
                            text = label,
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                // Selector de las opciones reales de esta carta.
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    rotations.forEachIndexed { index, rot ->
                        val cueText = item.allCues.getOrNull(index).orEmpty()
                        val isActive = item.rotation == rot
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onSelectCue(rot) },
                            shape = RoundedCornerShape(6.dp),
                            color = if (isActive) slotColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                            border = BorderStroke(
                                width = if (isActive) 1.5.dp else 0.5.dp,
                                color = if (isActive) slotColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = orientationLabels[index],
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = if (isActive) slotColor else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = cueText.ifBlank { "Ver imagen" },
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                    fontStyle = if (isActive) FontStyle.Italic else FontStyle.Normal,
                                    color = if (isActive) slotColor else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isActive) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Activa",
                                        tint = slotColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
