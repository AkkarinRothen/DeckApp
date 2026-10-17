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
    onApplyDamage: (amount: Int) -> Unit = { dmg -> onHpChanged((token.currentHp - dmg).coerceAtLeast(0)) },
    onApplyHealing: (amount: Int) -> Unit = { heal -> onHpChanged((token.currentHp + heal).coerceAtMost(token.maxHp)) },
    onOpenDamageCalculator: () -> Unit = {},
    onConditionToggled: (Condition) -> Unit,
    onSizeChanged: (newSize: Float) -> Unit,
    onElevationChanged: (newElevationFt: Int) -> Unit = {},
    onVisibilityToggled: () -> Unit,
    onStartAttackTargeting: () -> Unit = {},
    onOpenNpc: ((Long) -> Unit)? = null,
    onDeleteToken: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showConfirmDeleteToken by remember { mutableStateOf(false) }

    if (showConfirmDeleteToken) {
        AlertDialog(
            onDismissRequest = { showConfirmDeleteToken = false },
            title = { Text("Eliminar token") },
            text = { Text("¿Deseas eliminar el token \"${token.name}\" del mapa táctico?") },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDeleteToken = false
                        onDeleteToken()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDeleteToken = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

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

                    // Botón Atacar / Selección de Objetivo
                    Button(
                        onClick = onStartAttackTargeting,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Default.AdsClick, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Atacar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                    IconButton(onClick = { showConfirmDeleteToken = true }, modifier = Modifier.size(34.dp)) {
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

            // Barra de HP con controles rápidos de daño, curación y calculadora táctica
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Daño rápido (-5, -1)
                FilledTonalButton(
                    onClick = { onApplyDamage(5) },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text("-5", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 12.sp)
                }

                FilledTonalButton(
                    onClick = { onApplyDamage(1) },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text("-1", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 12.sp)
                }

                // Indicador visual de HP (Clickeable para abrir la calculadora completa)
                Surface(
                    onClick = onOpenDamageCalculator,
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "${token.currentHp} / ${token.maxHp} HP",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            if (token.tempHp > 0) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF00E5FF).copy(alpha = 0.25f)
                                ) {
                                    Text(
                                        text = "+${token.tempHp}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF00E5FF),
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.Default.Calculate,
                                contentDescription = "Calculadora",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        LinearProgressIndicator(
                            progress = { if (token.maxHp > 0) (token.currentHp.toFloat() / token.maxHp).coerceIn(0f, 1f) else 1f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = when {
                                token.currentHp > token.maxHp * 0.5f -> Color(0xFF4CAF50)
                                token.currentHp > token.maxHp * 0.2f -> Color(0xFFFFC107)
                                else -> Color(0xFFF44336)
                            },
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }

                // Curación rápida (+1, +5)
                FilledTonalButton(
                    onClick = { onApplyHealing(1) },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text("+1", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                FilledTonalButton(
                    onClick = { onApplyHealing(5) },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text("+5", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            // Fila de Elevación y Vuelo
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Flight,
                    contentDescription = "Elevación",
                    modifier = Modifier.size(16.dp),
                    tint = if (token.elevationFt != 0) Color(0xFF00E5FF) else MaterialTheme.colorScheme.outline
                )
                Text(
                    text = "Altura:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (token.elevationFt != 0) Color(0xFF00E5FF).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = if (token.elevationFt > 0) "+${token.elevationFt} ft" else "${token.elevationFt} ft",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (token.elevationFt != 0) Color(0xFF00E5FF) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                if (token.elevationFt != 0) {
                    FilledTonalButton(
                        onClick = { onElevationChanged(0) },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier.height(26.dp)
                    ) {
                        Text("Tierra", fontSize = 10.sp)
                    }
                }

                listOf(-5, 5, 10, 20).forEach { delta ->
                    val sign = if (delta > 0) "+$delta" else "$delta"
                    FilledTonalButton(
                        onClick = { onElevationChanged((token.elevationFt + delta).coerceIn(-100, 500)) },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier.height(26.dp)
                    ) {
                        Text("${sign}ft", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    }
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
