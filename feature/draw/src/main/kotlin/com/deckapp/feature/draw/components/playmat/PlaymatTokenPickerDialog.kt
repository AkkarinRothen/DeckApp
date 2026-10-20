package com.deckapp.feature.draw.components.playmat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.deckapp.core.model.Npc
import com.deckapp.core.model.PlaymatTokenType
import com.deckapp.core.ui.util.toCoilModel

/**
 * Diálogo para desplegar un token en el tapete:
 * permite seleccionar un NPC/personaje existente de la campaña o crear un token rápido ad-hoc.
 */
@Composable
fun PlaymatTokenPickerDialog(
    npcs: List<Npc>,
    onDismiss: () -> Unit,
    onSelectNpc: (Npc) -> Unit,
    onCreateAdHoc: (name: String, maxHp: Int, type: PlaymatTokenType) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Campaña, 1 = Rápido

    // Campos formulario rápido
    var adHocName by remember { mutableStateOf("") }
    var adHocHpText by remember { mutableStateOf("10") }
    var adHocType by remember { mutableStateOf(PlaymatTokenType.ALLY) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 520.dp)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Cabecera
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Añadir Token al Tapete",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Pestañas
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("NPCs (${npcs.size})") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Token Rápido") }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Contenido según pestaña
                when (selectedTab) {
                    0 -> {
                        if (npcs.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No hay NPCs registrados en esta campaña.\nPuedes crear uno en la pestaña 'Token Rápido'.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(npcs) { npc ->
                                    NpcTokenPickerRow(
                                        npc = npc,
                                        onSelect = {
                                            onSelectNpc(npc)
                                            onDismiss()
                                        }
                                    )
                                }
                            }
                        }
                    }
                    1 -> {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            OutlinedTextField(
                                value = adHocName,
                                onValueChange = { adHocName = it },
                                label = { Text("Nombre del personaje") },
                                placeholder = { Text("ej. Detective Vance, Guardia...") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = adHocHpText,
                                onValueChange = { adHocHpText = it.filter { ch -> ch.isDigit() } },
                                label = { Text("Puntos de Vida (PV Máx)") },
                                placeholder = { Text("10") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Text(
                                text = "Tipo / Facción",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = adHocType == PlaymatTokenType.ALLY,
                                    onClick = { adHocType = PlaymatTokenType.ALLY },
                                    label = { Text("Aliado (Verde)") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF4CAF50).copy(alpha = 0.25f),
                                        selectedLabelColor = Color(0xFF4CAF50)
                                    )
                                )
                                FilterChip(
                                    selected = adHocType == PlaymatTokenType.NEUTRAL,
                                    onClick = { adHocType = PlaymatTokenType.NEUTRAL },
                                    label = { Text("Neutral (Oro)") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFFFFB300).copy(alpha = 0.25f),
                                        selectedLabelColor = Color(0xFFFFB300)
                                    )
                                )
                                FilterChip(
                                    selected = adHocType == PlaymatTokenType.ENEMY,
                                    onClick = { adHocType = PlaymatTokenType.ENEMY },
                                    label = { Text("Enemigo (Rojo)") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFFE53935).copy(alpha = 0.25f),
                                        selectedLabelColor = Color(0xFFE53935)
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Barra inferior
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cerrar")
                    }
                    if (selectedTab == 1) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val hp = adHocHpText.toIntOrNull() ?: 10
                                onCreateAdHoc(adHocName.ifBlank { "Personaje" }, hp, adHocType)
                                onDismiss()
                            }
                        ) {
                            Text("Colocar Token")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NpcTokenPickerRow(
    npc: Npc,
    onSelect: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Avatar circular
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                if (!npc.imagePath.isNullOrBlank()) {
                    AsyncImage(
                        model = npc.imagePath.toCoilModel(),
                        contentDescription = npc.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = npc.name.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Datos del NPC
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = npc.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${npc.maxHp} PV  •  CA ${npc.armorClass}" + (if (npc.isMonster) "  •  Monstruo" else ""),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Botón añadir
            FilledTonalIconButton(
                onClick = onSelect,
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Añadir a la Mesa",
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
