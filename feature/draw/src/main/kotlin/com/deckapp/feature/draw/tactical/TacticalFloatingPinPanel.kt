package com.deckapp.feature.draw.tactical

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deckapp.core.model.*

/**
 * Mini-panel flotante in-situ para pines tácticos (inspirado en Atlas VTT).
 * Permite leer descripciones en Markdown, tirar fórmulas de dados inline (ej. 2d6+3),
 * alternar estados de secreto y ejecutar acciones directas (viaje entre mapas, spawn de encuentros, tiradas de tablas).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TacticalFloatingPinPanel(
    pin: TacticalMapPin,
    availableMaps: List<TacticalMap>,
    availableTables: List<RandomTable>,
    availableEncounters: List<Encounter>,
    availableWikiEntries: List<WikiEntry>,
    onDismiss: () -> Unit,
    onEnterSubMap: (Long) -> Unit,
    onSpawnEncounter: (encounterId: Long, cellX: Float, cellY: Float) -> Unit,
    onRollPinTable: (Long) -> Unit,
    onToggleSecret: (Long) -> Unit,
    onEditPin: (TacticalMapPin) -> Unit,
    onRollInlineFormula: (formula: String, label: String) -> Unit,
    isSticky: Boolean = false,
    isMinimized: Boolean = false,
    isLocked: Boolean = false,
    onToggleSticky: () -> Unit = {},
    onToggleMinimized: () -> Unit = {},
    onToggleLocked: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val targetMap = remember(pin.targetMapId, availableMaps) {
        availableMaps.find { it.id == pin.targetMapId }
    }
    val targetEncounter = remember(pin.targetEncounterId, availableEncounters) {
        availableEncounters.find { it.id == pin.targetEncounterId }
    }
    val targetTable = remember(pin.targetTableId, availableTables) {
        availableTables.find { it.id == pin.targetTableId }
    }

    // Extracción de fórmulas de dados en la descripción (ej: 1d20+3, 2d6, 3d8+4)
    val diceRegex = remember { Regex("""\b(\d+d\d+([+-]\d+)?)\b""", RegexOption.IGNORE_CASE) }
    val inlineDiceMatches = remember(pin.description) {
        diceRegex.findAll(pin.description).map { it.value }.distinct().toList()
    }

    if (isMinimized) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.96f),
            tonalElevation = 6.dp,
            shadowElevation = 8.dp,
            modifier = modifier
                .padding(12.dp)
                .clickable { onToggleMinimized() }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(pin.colorHex)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Text(
                    text = pin.title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                IconButton(
                    onClick = onToggleMinimized,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.UnfoldMore,
                        contentDescription = "Expandir",
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar",
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
        return
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.96f),
        tonalElevation = 8.dp,
        shadowElevation = 10.dp,
        modifier = modifier
            .widthIn(min = 280.dp, max = 350.dp)
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(14.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Cabecera: Icono semántico + Título + Sticky / Lock / Minimizar / Cerrar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(pin.colorHex)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (pin.iconName) {
                            "stairs", "portal", "map" -> Icons.Default.MeetingRoom
                            "door" -> Icons.Default.DoorSliding
                            "chest" -> Icons.Default.Inventory2
                            "trap" -> Icons.Default.Warning
                            "skull" -> Icons.Default.Dangerous
                            else -> Icons.Default.Info
                        },
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = pin.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "(${pin.cellX.toInt()}, ${pin.cellY.toInt()})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        if (pin.isSecret) {
                            Text(
                                text = "• Secreto DM",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }

                // Botón Candado (Bloquear movimiento)
                IconButton(
                    onClick = onToggleLocked,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = if (isLocked) "Desbloquear" else "Bloquear posición",
                        tint = if (isLocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Botón Chincheta (Sticky Note)
                IconButton(
                    onClick = onToggleSticky,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = if (isSticky) "Desfijar de pantalla" else "Fijar nota en pantalla",
                        tint = if (isSticky) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Botón Minimizar a píldora
                IconButton(
                    onClick = onToggleMinimized,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.UnfoldLess,
                        contentDescription = "Minimizar",
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            HorizontalDivider()

            // Descripción de la sala o secreto
            if (pin.description.isNotBlank()) {
                Text(
                    text = pin.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            } else {
                Text(
                    text = "Sin descripción adicional.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            // Fórmulas de dados detectadas en el texto
            if (inlineDiceMatches.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Tiradas rápidas detectadas:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        inlineDiceMatches.forEach { formula ->
                            FilterChip(
                                selected = false,
                                onClick = { onRollInlineFormula(formula, pin.title) },
                                label = { Text(formula, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Casino,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            )
                        }
                    }
                }
            }

            // Descubrimientos de tablas previas si existen
            if (pin.discoveries.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Historial de descubrimientos (${pin.discoveries.size}):",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.outline
                        )
                        pin.discoveries.takeLast(2).forEach { disc ->
                            Text(
                                text = "• ${disc.resolvedText}",
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            HorizontalDivider()

            // Acciones directas integradas estilo Atlas VTT
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                // 1. Entrar a Sub-mapa anidado
                if (targetMap != null) {
                    Button(
                        onClick = { onEnterSubMap(targetMap.id) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Entrar a ${targetMap.name}", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }

                // 2. Desplegar Encuentro en racimo
                if (targetEncounter != null) {
                    FilledTonalButton(
                        onClick = { onSpawnEncounter(targetEncounter.id, pin.cellX, pin.cellY) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Icon(Icons.Default.SportsKabaddi, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onErrorContainer)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Desplegar ${targetEncounter.name}",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // 3. Tirar en tabla vinculada
                if (targetTable != null) {
                    OutlinedButton(
                        onClick = { onRollPinTable(pin.id) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Casino, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Tirar en ${targetTable.name}", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }

                // Fila inferior de utilidades (Secreto / Editar)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { onToggleSecret(pin.id) },
                        contentPadding = PaddingValues(horizontal = 6.dp)
                    ) {
                        Icon(
                            imageVector = if (pin.isSecret) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(if (pin.isSecret) "Hacer público" else "Hacer secreto", fontSize = 11.sp)
                    }

                    TextButton(
                        onClick = { onEditPin(pin) },
                        contentPadding = PaddingValues(horizontal = 6.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Editar", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
