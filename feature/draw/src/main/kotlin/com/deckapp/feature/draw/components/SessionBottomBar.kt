package com.deckapp.feature.draw.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deckapp.feature.draw.SidebarTool

/**
 * Barra inferior unificada de la mesa de sesión para portrait.
 *
 * Fusiona el dock de acciones rápidas con la navegación de herramientas,
 * eliminando los Edge Tabs laterales y el dock flotante en portrait.
 *
 * Layout:
 *   [tabs de herramientas scrollables] | [Dado] [Carta] [Nota] [Buscar]
 */
@Composable
fun SessionBottomBar(
    activeTool: SidebarTool,
    hasActiveEncounter: Boolean,
    hasLinkedMythic: Boolean,
    onToolClick: (SidebarTool) -> Unit,
    onShowDiceRoller: () -> Unit,
    onDrawCard: () -> Unit,
    onShowQuickNote: () -> Unit,
    onShowSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tools = buildList {
        add(SidebarTool.DECKS to (Icons.Default.Style to "Mazos"))
        add(SidebarTool.TABLES to (Icons.Default.Casino to "Tablas"))
        if (hasActiveEncounter) {
            add(SidebarTool.COMBAT to (Icons.Default.CrisisAlert to "Combate"))
        }
        add(SidebarTool.NOTES to (Icons.Default.EditNote to "Notas"))
        add(SidebarTool.WIKI to (Icons.AutoMirrored.Filled.MenuBook to "Wiki"))
        add(SidebarTool.JOURNAL to (Icons.AutoMirrored.Filled.LibraryBooks to "Diario"))
        add(SidebarTool.NPCS to (Icons.Default.Groups to "NPCs"))
        add(SidebarTool.PLAN to (Icons.Default.HistoryEdu to "Plan"))
        if (hasLinkedMythic) {
            add(SidebarTool.MYTHIC_CONTEXT to (Icons.Default.AutoAwesome to "Mythic"))
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp),
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .height(56.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tabs de herramientas scrollables
            LazyRow(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(tools, key = { it.first.name }) { (tool, iconLabel) ->
                    val (icon, label) = iconLabel
                    BottomBarToolTab(
                        icon = icon,
                        label = label,
                        isActive = activeTool == tool,
                        onClick = { onToolClick(tool) }
                    )
                }
            }

            // Separador vertical
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(32.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            )

            // Acciones rápidas fijas
            Row(
                modifier = Modifier.padding(horizontal = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                QuickActionIconButton(
                    icon = Icons.Default.Casino,
                    contentDescription = "Lanzar dado",
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    onClick = onShowDiceRoller
                )
                QuickActionIconButton(
                    icon = Icons.Default.Style,
                    contentDescription = "Robar carta",
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    onClick = onDrawCard
                )
                QuickActionIconButton(
                    icon = Icons.Default.EditNote,
                    contentDescription = "Nota rápida",
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    onClick = onShowQuickNote
                )
                QuickActionIconButton(
                    icon = Icons.Default.Search,
                    contentDescription = "Búsqueda global",
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = onShowSearch
                )
            }
        }
    }
}

@Composable
private fun BottomBarToolTab(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (isActive) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surface.copy(alpha = 0f),
        animationSpec = tween(durationMillis = 200),
        label = "tabBg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(durationMillis = 200),
        label = "tabContent"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        contentColor = contentColor,
        modifier = Modifier.clip(RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(18.dp),
                tint = contentColor
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Clip
            )
        }
    }
}

@Composable
private fun QuickActionIconButton(
    icon: ImageVector,
    contentDescription: String,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = containerColor,
        contentColor = contentColor,
        modifier = Modifier.size(40.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(20.dp),
                tint = contentColor
            )
        }
    }
}
