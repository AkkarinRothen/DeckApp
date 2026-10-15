package com.deckapp.feature.draw.components.blocks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.deckapp.core.model.Npc
import com.deckapp.core.model.SessionBoardItem
import com.deckapp.core.model.SessionBoardItemSize
import com.deckapp.feature.draw.util.getMode

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun NpcBlockContent(
    item: SessionBoardItem,
    npcCount: Int,
    npcNames: List<String>,
    npcs: List<Npc> = emptyList(),
    compactInFolder: Boolean,
    onDiceClicked: (String) -> Unit = {},
    onUpdateNpcHp: (Long, Int) -> Unit = { _, _ -> },
    onNpcClick: (Long) -> Unit = {},
    onOpen: () -> Unit
) {
    val mode = item.getMode()
    val previewNames = npcNames.take(if (item.size == SessionBoardItemSize.L) 6 else 3)
    val hiddenCount = (npcCount - previewNames.size).coerceAtLeast(0)

    // Modo PNJ Destacado / Mini-Ficha Bento
    if (mode == "featured") {
        val featuredNpc = npcs.find { it.id == item.resourceId }
        if (featuredNpc == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Ningún personaje vinculado.\nConfigura el bloque para asignar uno.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            }
            return
        }

        if (item.size == SessionBoardItemSize.S || compactInFolder) {
            // Layout S compacto para PNJ destacado
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onOpen() }
                ) {
                    Text(
                        text = featuredNpc.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "HP ${featuredNpc.currentHp}/${featuredNpc.maxHp} · CA ${featuredNpc.armorClass}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onOpen,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = "Ver Ficha",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            return
        }

        // Layout M / L: Mini-Ficha Bento completa
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Fila superior: Nombre, Sistema, CA
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onOpen() }
                ) {
                    Text(
                        text = featuredNpc.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = featuredNpc.description.ifBlank { "PNJ" },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Escudo de CA
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "CA",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "${featuredNpc.armorClass}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            // Puntos de Vida y Controles rápidos
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "HP ${featuredNpc.currentHp}/${featuredNpc.maxHp}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(68.dp)
                )

                LinearProgressIndicator(
                    progress = { (featuredNpc.currentHp.toFloat() / featuredNpc.maxHp.toFloat()).coerceIn(0f, 1f) },
                    modifier = Modifier.weight(1f).height(6.dp),
                    color = if (featuredNpc.currentHp.toFloat() / featuredNpc.maxHp.toFloat() < 0.3f) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                FilledTonalIconButton(
                    onClick = { onUpdateNpcHp(featuredNpc.id, (featuredNpc.currentHp - 1).coerceAtLeast(0)) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "-1 HP",
                        modifier = Modifier.size(12.dp)
                    )
                }

                FilledTonalIconButton(
                    onClick = { onUpdateNpcHp(featuredNpc.id, (featuredNpc.currentHp + 1).coerceIn(0, featuredNpc.maxHp)) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "+1 HP",
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            // Atributos y Modificadores (STR, DEX, CON, INT, WIS, CHA)
            val attributes = listOf(
                "STR" to 10,
                "DEX" to (10 + featuredNpc.initiativeBonus * 2),
                "CON" to 10,
                "INT" to 10,
                "WIS" to 10,
                "CHA" to 10
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                maxItemsInEachRow = 6
            ) {
                attributes.forEach { (name, score) ->
                    val mod = (score - 10) / 2
                    val modSign = if (mod >= 0) "+$mod" else "$mod"

                    Surface(
                        onClick = { onDiceClicked("${featuredNpc.name}: Salvación de $name (1d20$modSign)") },
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.width(42.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = name,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = modSign,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Botón de Abrir Ficha Completa
            FilledTonalButton(
                onClick = onOpen,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(
                    imageVector = Icons.Default.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Ver Ficha Completa")
            }
        }
        return
    }

    // Modo Clásico (Lista/Roster)
    if (item.size == SessionBoardItemSize.S || compactInFolder) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$npcCount personajes",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = previewNames.joinToString(", ").ifBlank { "Sin NPCs registrados" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (hiddenCount > 0) {
                    Text(
                        text = "+$hiddenCount mas",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.Groups,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
        }
        return
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            AssistChip(
                onClick = onOpen,
                label = { Text("$npcCount total") }
            )
            AssistChip(
                onClick = onOpen,
                label = { Text("${previewNames.size} visibles") }
            )
            if (hiddenCount > 0) {
                AssistChip(
                    onClick = onOpen,
                    label = { Text("+$hiddenCount mas") }
                )
            }
        }

        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = if (mode == "roster") {
                        "Reparto visible para consulta rapida durante la escena."
                    } else {
                        "Personajes, voces y contexto de campana."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = previewNames.joinToString(", ").ifBlank { "Sin NPCs registrados" },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (previewNames.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                previewNames.forEach { name ->
                    val matchedNpc = npcs.find { it.name.equals(name, ignoreCase = true) }
                    AssistChip(
                        onClick = {
                            if (matchedNpc != null) {
                                onNpcClick(matchedNpc.id)
                            } else {
                                onOpen()
                            }
                        },
                        label = { Text(name, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    )
                }
            }
        }

        FilledTonalButton(
            onClick = onOpen,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.filledTonalButtonColors()
        ) {
            Icon(Icons.Default.PersonSearch, null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (mode == "roster") "Abrir reparto" else "Abrir NPCs")
        }
    }
}
