package com.deckapp.feature.draw.components.clocks

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deckapp.core.model.TensionClock

/**
 * Overlay flotante colapsable de Relojes de Tensión y Progreso (Progress Clocks Overlay).
 * Permite monitorear amenazas, sigilo, alarmas y límites temporales de forma no invasiva
 * desde cualquier vista de la mesa de juego (Bento, Mapas, Diario).
 */
@Composable
fun SessionClocksOverlay(
    clocks: List<TensionClock>,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    onTickClock: (clockId: Long, delta: Int) -> Unit,
    onAddClock: () -> Unit,
    onEditClock: (TensionClock) -> Unit,
    onResetClock: (clockId: Long) -> Unit,
    onDeleteClock: (clockId: Long) -> Unit,
    onTriggerClockNow: (TensionClock) -> Unit,
    onCopyClockToDiary: (TensionClock) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val hasAlert = remember(clocks) { clocks.any { it.isCompleted || (it.segments > 0 && it.progress >= it.segments) } }

    Column(
        modifier = modifier.widthIn(max = 330.dp),
        horizontalAlignment = Alignment.End
    ) {
        // ── Pastilla de Activación (Collapsed / Header Pill) ──────────────────
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xEB161920),
            border = BorderStroke(
                width = 1.dp,
                color = if (hasAlert) MaterialTheme.colorScheme.error.copy(alpha = 0.8f) else Color(0x33FFFFFF)
            ),
            shadowElevation = 6.dp,
            modifier = Modifier
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onToggleExpanded()
                }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = if (hasAlert) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )

                Text(
                    text = "Relojes",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                if (clocks.isNotEmpty()) {
                    Surface(
                        shape = CircleShape,
                        color = if (hasAlert) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                        modifier = Modifier.size(20.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "${clocks.size}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = if (hasAlert) Color.White else MaterialTheme.colorScheme.primary,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Colapsar" else "Expandir",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // ── Panel Flotante Expandido ──────────────────────────────────────────
        AnimatedVisibility(
            visible = isExpanded,
            enter = slideInVertically(initialOffsetY = { -it / 3 }, animationSpec = tween(220)) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it / 3 }, animationSpec = tween(180)) + fadeOut()
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xF2161920),
                border = BorderStroke(1.dp, Color(0x33FFFFFF)),
                shadowElevation = 12.dp,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .width(320.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Barra superior del panel
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Amenazas y Tensión",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            IconButton(
                                onClick = onAddClock,
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Nuevo Reloj",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            IconButton(
                                onClick = onToggleExpanded,
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cerrar",
                                    tint = Color.White.copy(alpha = 0.6f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // Lista de Relojes Activos
                    if (clocks.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color.White.copy(alpha = 0.05f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.4f),
                                    modifier = Modifier.size(32.dp)
                                )
                                Text(
                                    text = "Sin relojes activos",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                                Text(
                                    text = "Usa '+' para seguir sigilo, alarmas, veneno o límites temporales.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 380.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(clocks, key = { it.id }) { clock ->
                                ClockOverlayItemCard(
                                    clock = clock,
                                    onTickClock = { delta ->
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onTickClock(clock.id, delta)
                                    },
                                    onEditClock = { onEditClock(clock) },
                                    onResetClock = { onResetClock(clock.id) },
                                    onDeleteClock = { onDeleteClock(clock.id) },
                                    onTriggerNow = { onTriggerClockNow(clock) },
                                    onCopyToDiary = { onCopyClockToDiary(clock) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Fila individual para un reloj dentro del panel de overlay.
 */
@Composable
private fun ClockOverlayItemCard(
    clock: TensionClock,
    onTickClock: (delta: Int) -> Unit,
    onEditClock: () -> Unit,
    onResetClock: () -> Unit,
    onDeleteClock: () -> Unit,
    onTriggerNow: () -> Unit,
    onCopyToDiary: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val isCompleted = clock.isCompleted || (clock.segments > 0 && clock.progress >= clock.segments)

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isCompleted) Color(0x33E53935) else Color(0x1AFFFFFF),
        border = BorderStroke(
            width = 1.dp,
            color = if (isCompleted) MaterialTheme.colorScheme.error.copy(alpha = 0.6f) else Color(0x22FFFFFF)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Reloj interactivo táctil: pulsar avanza +1 tick
                Surface(
                    shape = CircleShape,
                    color = Color.Transparent,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .clickable { onTickClock(if (isCompleted) -clock.progress else 1) }
                ) {
                    CircularClockGraphic(
                        progress = clock.progress,
                        segments = clock.segments,
                        sizeDp = 44.dp,
                        strokeWidthDp = 6.dp,
                        accentColorHex = clock.colorHex,
                        isCompleted = isCompleted,
                        showCenterText = true
                    )
                }

                // Título y progreso textual
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = clock.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (isCompleted) "¡DETONADO! (${clock.progress}/${clock.segments})" else "${clock.progress} de ${clock.segments} pasos",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isCompleted) MaterialTheme.colorScheme.error else Color.White.copy(alpha = 0.6f),
                        fontWeight = if (isCompleted) FontWeight.Bold else FontWeight.Normal
                    )
                }

                // Controles rápidos: - y +
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(
                        onClick = { onTickClock(-1) },
                        enabled = clock.progress > 0,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Retroceder",
                            tint = if (clock.progress > 0) Color.White.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = { onTickClock(1) },
                        enabled = clock.progress < clock.segments,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Avanzar",
                            tint = if (clock.progress < clock.segments) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Menú contextual desplegable
                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Opciones",
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Editar Reloj") },
                                onClick = {
                                    menuExpanded = false
                                    onEditClock()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Reiniciar (0/${clock.segments})") },
                                onClick = {
                                    menuExpanded = false
                                    onResetClock()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Detonar Ahora (100%)") },
                                onClick = {
                                    menuExpanded = false
                                    onTriggerNow()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.WarningAmber, contentDescription = null, modifier = Modifier.size(18.dp))
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Copiar al Diario") },
                                onClick = {
                                    menuExpanded = false
                                    onCopyToDiary()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp))
                                }
                            )
                            Divider()
                            DropdownMenuItem(
                                text = { Text("Eliminar / Archivar", color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    menuExpanded = false
                                    onDeleteClock()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                }
                            )
                        }
                    }
                }
            }

            // Nota / Consecuencia breve si existe
            if (clock.triggerDescription.isNotBlank()) {
                Text(
                    text = "Detonante: ${clock.triggerDescription}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isCompleted) MaterialTheme.colorScheme.error.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.5f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(start = 2.dp)
                )
            }
        }
    }
}
