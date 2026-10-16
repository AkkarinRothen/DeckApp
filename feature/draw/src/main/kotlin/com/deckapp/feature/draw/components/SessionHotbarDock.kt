package com.deckapp.feature.draw.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deckapp.core.model.HotbarActionType
import com.deckapp.core.model.SessionHotbarAction

/**
 * Dock Flotante de Atajos y Macros Rápidas (Action Hotbar) para la mesa de juego.
 * Permite ejecutar tiradas de dados, consultas de oráculo, tablas aleatorias y robo de cartas con 1 toque.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SessionHotbarDock(
    actions: List<SessionHotbarAction>,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    onExecuteAction: (SessionHotbarAction) -> Unit,
    onAddAction: () -> Unit,
    onEditAction: (SessionHotbarAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Contenido Expandido de la Barra
        AnimatedVisibility(
            visible = isExpanded,
            enter = slideInVertically(initialOffsetY = { it / 2 }, animationSpec = tween(220)) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it / 2 }, animationSpec = tween(180)) + fadeOut()
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
                    .shadow(elevation = 14.dp, shape = RoundedCornerShape(20.dp))
                    .clip(RoundedCornerShape(20.dp))
                    .border(
                        BorderStroke(
                            width = 1.dp,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.25f),
                                    Color.White.copy(alpha = 0.08f)
                                )
                            )
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ),
                color = Color(0xEB161920),
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    // Cabecera de la barra
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = Color(0xFFFFD54F),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Atajos Rápidos",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                ),
                                color = Color(0xFFECEFF1)
                            )
                            Badge(
                                containerColor = Color(0xFF263238),
                                contentColor = Color(0xFFB0BEC5)
                            ) {
                                Text(
                                    text = "${actions.size}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Botón Añadir Atajo
                            IconButton(
                                onClick = onAddAction,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Añadir Atajo",
                                    tint = Color(0xFF81C784),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            // Botón Colapsar Barra
                            IconButton(
                                onClick = onToggleExpanded,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Ocultar Atajos",
                                    tint = Color(0xFF90A4AE),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Fila desplazable de Chips de Acción
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        items(actions, key = { it.id }) { action ->
                            HotbarActionChip(
                                action = action,
                                onClick = { onExecuteAction(action) },
                                onLongClick = { onEditAction(action) }
                            )
                        }

                        item {
                            // Botón extra para agregar al final
                            Surface(
                                onClick = onAddAction,
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0x3337474F),
                                border = BorderStroke(1.dp, Color(0x55546E7A)),
                                modifier = Modifier.height(42.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = Color(0xFF81C784),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Crear",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                        color = Color(0xFFB0BEC5)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Pastilla Botón de Toggle cuando está colapsado (o siempre visible de forma sutil)
        if (!isExpanded) {
            Surface(
                onClick = onToggleExpanded,
                shape = CircleShape,
                color = Color(0xEB1A1D24),
                border = BorderStroke(
                    1.dp,
                    Brush.verticalGradient(
                        listOf(Color(0x55FFD54F), Color(0x22FFD54F))
                    )
                ),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .height(36.dp)
                    .clip(CircleShape)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = Color(0xFFFFD54F),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Atajos (${actions.size})",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFFECEFF1)
                    )
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Expandir Atajos",
                        tint = Color(0xFFB0BEC5),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/** Chip individual de atajo rápido con feedback visual y soporte para toque simple o sostenido */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HotbarActionChip(
    action: SessionHotbarAction,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val (typeColor, defaultIcon) = when (action.type) {
        HotbarActionType.DICE_ROLL -> Pair(Color(0xFF5C6BC0), Icons.Default.Casino)
        HotbarActionType.ORACLE_QUERY -> Pair(Color(0xFFAB47BC), Icons.Default.AutoAwesome)
        HotbarActionType.RANDOM_TABLE -> Pair(Color(0xFFFFA000), Icons.Default.FormatListNumbered)
        HotbarActionType.DRAW_CARD -> Pair(Color(0xFF26A69A), Icons.Default.Style)
    }

    val chipColor = action.colorHex?.let { Color(it.toULong()) } ?: typeColor

    Surface(
        modifier = Modifier
            .height(42.dp)
            .shadow(4.dp, shape = RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .border(
                BorderStroke(1.dp, chipColor.copy(alpha = 0.55f)),
                shape = RoundedCornerShape(12.dp)
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        color = chipColor.copy(alpha = 0.22f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(chipColor.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = defaultIcon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }

            Column(
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = action.label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    ),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (action.payload.isNotBlank()) {
                    Text(
                        text = formatPayloadSubtext(action),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 9.sp,
                            color = Color.White.copy(alpha = 0.70f)
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

private fun formatPayloadSubtext(action: SessionHotbarAction): String {
    return when (action.type) {
        HotbarActionType.DICE_ROLL -> action.payload
        HotbarActionType.ORACLE_QUERY -> when (action.payload) {
            "50_50" -> "50/50"
            "RANDOM_EVENT" -> "Evento"
            "NEAR_SURE" -> "Seguro"
            "UNLIKELY" -> "Improbable"
            else -> action.payload
        }
        HotbarActionType.RANDOM_TABLE -> "Tabla"
        HotbarActionType.DRAW_CARD -> "Robar"
    }
}
