package com.deckapp.feature.draw.tactical

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deckapp.core.model.Condition
import com.deckapp.core.model.TacticalToken
import com.deckapp.core.model.TokenFaction

@Composable
fun TacticalTokenHud(
    token: TacticalToken,
    onHpChanged: (newHp: Int) -> Unit,
    onConditionToggled: (Condition) -> Unit,
    onSizeChanged: (newSize: Float) -> Unit,
    onVisibilityToggled: () -> Unit,
    onOpenNpc: ((Long) -> Unit)? = null,
    onDeleteToken: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f),
        tonalElevation = 8.dp,
        shadowElevation = 12.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Fila superior: Nombre, Facción, CA, Velocidad, Badges y Acciones de Cabecera
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    val factionColor = when (token.faction) {
                        TokenFaction.PLAYER -> Color(0xFF2196F3)
                        TokenFaction.ALLY -> Color(0xFF4CAF50)
                        TokenFaction.ENEMY -> Color(0xFFF44336)
                        TokenFaction.NEUTRAL -> Color(0xFFFFC107)
                    }

                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(factionColor)
                    )

                    Text(
                        text = token.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )

                    // Badge de CA
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "CA",
                                modifier = Modifier.size(13.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${token.armorClass}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Badge de Velocidad
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.DirectionsRun,
                                contentDescription = "Speed",
                                modifier = Modifier.size(13.dp),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = "${token.speedFt}ft",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }

                    // Badge si está sincronizado con iniciativa
                    if (token.encounterCreatureId != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "Encuentro",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Botón para ver ficha completa si tiene PNJ vinculado
                    val linkedNpcId = token.npcId
                    if (linkedNpcId != null && onOpenNpc != null) {
                        FilledTonalButton(
                            onClick = { onOpenNpc(linkedNpcId) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Default.ContactPage, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Ficha", fontSize = 11.sp)
                        }
                    }

                    // Botón de visibilidad / oculto
                    IconButton(
                        onClick = onVisibilityToggled,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = if (token.isHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (token.isHidden) "Oculto" else "Visible",
                            tint = if (token.isHidden) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Botón borrar
                    IconButton(onClick = onDeleteToken, modifier = Modifier.size(34.dp)) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Eliminar Token",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Botón cerrar
                    IconButton(onClick = onClose, modifier = Modifier.size(34.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar HUD",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Barra de HP con controles rápidos de daño y curación
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Daño rápido (-5, -1)
                FilledTonalButton(
                    onClick = { onHpChanged((token.currentHp - 5).coerceAtLeast(0)) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text("-5", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                }

                FilledTonalButton(
                    onClick = { onHpChanged((token.currentHp - 1).coerceAtLeast(0)) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text("-1", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                }

                // Indicador visual de HP
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${token.currentHp} / ${token.maxHp} HP",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    LinearProgressIndicator(
                        progress = { if (token.maxHp > 0) (token.currentHp.toFloat() / token.maxHp).coerceIn(0f, 1f) else 1f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = when {
                            token.currentHp > token.maxHp * 0.5f -> Color(0xFF4CAF50)
                            token.currentHp > token.maxHp * 0.2f -> Color(0xFFFFC107)
                            else -> Color(0xFFF44336)
                        },
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }

                // Curación rápida (+1, +5)
                FilledTonalButton(
                    onClick = { onHpChanged(token.currentHp + 1) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text("+1", fontWeight = FontWeight.Bold)
                }

                FilledTonalButton(
                    onClick = { onHpChanged(token.currentHp + 5) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text("+5", fontWeight = FontWeight.Bold)
                }
            }

            // Fila de Tamaño de Token y Condiciones de combate
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("Tamaño:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)

                listOf(
                    1f to "1x1 (M)",
                    2f to "2x2 (G)",
                    3f to "3x3 (E)",
                    4f to "4x4 (Garg)"
                ).forEach { (size, label) ->
                    FilterChip(
                        selected = token.sizeInCells == size,
                        onClick = { onSizeChanged(size) },
                        label = { Text(label, fontSize = 10.sp) },
                        modifier = Modifier.height(26.dp)
                    )
                }

                VerticalDivider(modifier = Modifier.height(18.dp).padding(horizontal = 2.dp))

                val commonConditions = listOf(
                    Condition.PRONE to "Derribado",
                    Condition.BLINDED to "Cegado",
                    Condition.POISONED to "Envenenado",
                    Condition.PARALYZED to "Paralizado",
                    Condition.FRIGHTENED to "Asustado",
                    Condition.GRAPPLED to "Apresado",
                    Condition.STUNNED to "Aturdido",
                    Condition.UNCONSCIOUS to "Inconsciente",
                    Condition.INVISIBLE to "Invisible"
                )

                commonConditions.forEach { (cond, label) ->
                    val isActive = token.conditions.contains(cond)
                    FilterChip(
                        selected = isActive,
                        onClick = { onConditionToggled(cond) },
                        label = { Text(label, fontSize = 10.sp) },
                        leadingIcon = if (isActive) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(12.dp)) }
                        } else null,
                        modifier = Modifier.height(26.dp)
                    )
                }
            }
        }
    }
}
