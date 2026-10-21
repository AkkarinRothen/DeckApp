package com.deckapp.feature.draw.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deckapp.feature.draw.SidebarTool
import com.deckapp.core.ui.icons.DeckIcons

/**
 * Barra inferior unificada y categorizada para teléfono vertical (Portrait).
 * Agrupa las herramientas en 3 categorías sin scroll desbordante:
 * 1. 🃏 Mesa (Mazos, Tablas)
 * 2. 📜 Lore & Registro (Notas, Diario, PNJs, Wiki, Plan)
 * 3. ⚔️ Combate & Reglas (Combate, Reglas, Mythic)
 */
@Composable
fun SessionBottomBar(
    activeTool: SidebarTool,
    hasActiveEncounter: Boolean = false,
    hasLinkedMythic: Boolean = false,
    onToolClick: (SidebarTool) -> Unit,
    modifier: Modifier = Modifier
) {
    var showMesaMenu by remember { mutableStateOf(false) }
    var showLoreMenu by remember { mutableStateOf(false) }
    var showRulesMenu by remember { mutableStateOf(false) }

    val isMesaActive = activeTool in listOf(SidebarTool.DECKS, SidebarTool.TABLES)
    val isLoreActive = activeTool in listOf(SidebarTool.NOTES, SidebarTool.JOURNAL, SidebarTool.NPCS, SidebarTool.WIKI, SidebarTool.PLAN)
    val isRulesActive = activeTool in listOf(SidebarTool.COMBAT, SidebarTool.REFERENCES, SidebarTool.MYTHIC_CONTEXT)

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.96f),
        tonalElevation = 6.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .height(52.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // --- 1. CATEGORÍA: MESA & CARTAS ---
            Box {
                CategoryBottomTab(
                    icon = Icons.Default.Style,
                    label = "Mesa",
                    isActive = isMesaActive,
                    activeSublabel = when (activeTool) {
                        SidebarTool.DECKS -> "Mazos"
                        SidebarTool.TABLES -> "Tablas"
                        else -> null
                    },
                    onClick = { showMesaMenu = true }
                )

                DropdownMenu(
                    expanded = showMesaMenu,
                    onDismissRequest = { showMesaMenu = false }
                ) {
                    Text(
                        text = "🃏 Mesa de Juego",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                    DropdownMenuItem(
                        text = { Text("Mazos de Cartas") },
                        leadingIcon = { Icon(Icons.Default.Style, null, tint = MaterialTheme.colorScheme.primary) },
                        trailingIcon = if (activeTool == SidebarTool.DECKS) {
                            { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary) }
                        } else null,
                        onClick = {
                            showMesaMenu = false
                            onToolClick(SidebarTool.DECKS)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Tablas Aleatorias") },
                        leadingIcon = { Icon(Icons.Default.Casino, null, tint = MaterialTheme.colorScheme.tertiary) },
                        trailingIcon = if (activeTool == SidebarTool.TABLES) {
                            { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary) }
                        } else null,
                        onClick = {
                            showMesaMenu = false
                            onToolClick(SidebarTool.TABLES)
                        }
                    )
                }
            }

            VerticalDivider(
                modifier = Modifier.height(24.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )

            // --- 2. CATEGORÍA: LORE & REGISTRO ---
            Box {
                CategoryBottomTab(
                    icon = Icons.Default.AutoStories,
                    label = "Lore",
                    isActive = isLoreActive,
                    activeSublabel = when (activeTool) {
                        SidebarTool.NOTES -> "Notas"
                        SidebarTool.JOURNAL -> "Diario"
                        SidebarTool.NPCS -> "PNJs"
                        SidebarTool.WIKI -> "Wiki"
                        SidebarTool.PLAN -> "Plan"
                        else -> null
                    },
                    onClick = { showLoreMenu = true }
                )

                DropdownMenu(
                    expanded = showLoreMenu,
                    onDismissRequest = { showLoreMenu = false }
                ) {
                    Text(
                        text = "📜 Campaña y Lore",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                    DropdownMenuItem(
                        text = { Text("Notas de Sesión") },
                        leadingIcon = { Icon(Icons.Default.EditNote, null) },
                        trailingIcon = if (activeTool == SidebarTool.NOTES) {
                            { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary) }
                        } else null,
                        onClick = {
                            showLoreMenu = false
                            onToolClick(SidebarTool.NOTES)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Personajes (PNJs)") },
                        leadingIcon = { Icon(Icons.Default.Groups, null, tint = MaterialTheme.colorScheme.secondary) },
                        trailingIcon = if (activeTool == SidebarTool.NPCS) {
                            { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary) }
                        } else null,
                        onClick = {
                            showLoreMenu = false
                            onToolClick(SidebarTool.NPCS)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Wiki del Mundo") },
                        leadingIcon = { Icon(Icons.Default.Public, null) },
                        trailingIcon = if (activeTool == SidebarTool.WIKI) {
                            { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary) }
                        } else null,
                        onClick = {
                            showLoreMenu = false
                            onToolClick(SidebarTool.WIKI)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Diario de Viaje") },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.LibraryBooks, null) },
                        trailingIcon = if (activeTool == SidebarTool.JOURNAL) {
                            { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary) }
                        } else null,
                        onClick = {
                            showLoreMenu = false
                            onToolClick(SidebarTool.JOURNAL)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Plan de Sesión") },
                        leadingIcon = { Icon(Icons.Default.HistoryEdu, null) },
                        trailingIcon = if (activeTool == SidebarTool.PLAN) {
                            { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary) }
                        } else null,
                        onClick = {
                            showLoreMenu = false
                            onToolClick(SidebarTool.PLAN)
                        }
                    )
                }
            }

            VerticalDivider(
                modifier = Modifier.height(24.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )

            // --- 3. CATEGORÍA: COMBATE & REGLAS ---
            Box {
                CategoryBottomTab(
                    icon = if (hasActiveEncounter) Icons.Default.CrisisAlert else Icons.Default.Shield,
                    label = "Combate",
                    isActive = isRulesActive,
                    badge = if (hasActiveEncounter) "!" else null,
                    badgeColor = Color(0xFFFF5252),
                    activeSublabel = when (activeTool) {
                        SidebarTool.COMBAT -> "Tracker"
                        SidebarTool.REFERENCES -> "Reglas"
                        SidebarTool.MYTHIC_CONTEXT -> "Mythic"
                        else -> null
                    },
                    onClick = { showRulesMenu = true }
                )

                DropdownMenu(
                    expanded = showRulesMenu,
                    onDismissRequest = { showRulesMenu = false }
                ) {
                    Text(
                        text = "⚔️ Reglas y Combate",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                    DropdownMenuItem(
                        text = { Text(if (hasActiveEncounter) "Tracker de Combate (Activo)" else "Tracker de Combate") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.CrisisAlert,
                                null,
                                tint = if (hasActiveEncounter) Color(0xFFFF5252) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        trailingIcon = if (activeTool == SidebarTool.COMBAT) {
                            { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary) }
                        } else null,
                        onClick = {
                            showRulesMenu = false
                            onToolClick(SidebarTool.COMBAT)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Compendio & Reglas") },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.MenuBook, null) },
                        trailingIcon = if (activeTool == SidebarTool.REFERENCES) {
                            { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary) }
                        } else null,
                        onClick = {
                            showRulesMenu = false
                            onToolClick(SidebarTool.REFERENCES)
                        }
                    )
                    if (hasLinkedMythic) {
                        DropdownMenuItem(
                            text = { Text("Oráculo Mythic (Hilos & Caos)") },
                            leadingIcon = { Icon(Icons.Default.AutoAwesome, null, tint = MaterialTheme.colorScheme.tertiary) },
                            trailingIcon = if (activeTool == SidebarTool.MYTHIC_CONTEXT) {
                                { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary) }
                            } else null,
                            onClick = {
                                showRulesMenu = false
                                onToolClick(SidebarTool.MYTHIC_CONTEXT)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryBottomTab(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    activeSublabel: String? = null,
    badge: String? = null,
    badgeColor: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit
) {
    val containerColor by animateColorAsState(
        targetValue = if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f) else Color.Transparent,
        animationSpec = tween(180),
        label = "tab_color"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(180),
        label = "content_color"
    )

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = contentColor,
                    modifier = Modifier.size(20.dp)
                )
                if (badge != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 4.dp, y = (-4).dp)
                            .size(12.dp)
                            .background(badgeColor, CircleShape)
                    )
                }
            }

            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = contentColor
                )
                if (activeSublabel != null) {
                    Text(
                        text = activeSublabel,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = contentColor.copy(alpha = 0.8f)
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = contentColor.copy(alpha = 0.7f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
