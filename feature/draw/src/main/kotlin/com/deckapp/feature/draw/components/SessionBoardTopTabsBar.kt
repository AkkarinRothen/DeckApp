package com.deckapp.feature.draw.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.deckapp.feature.draw.SessionBoardTemplatePreset
import com.deckapp.core.model.SessionBoardItem
import com.deckapp.core.model.SessionBoardView
import com.deckapp.core.model.SessionBoardViewLabels

@Composable
internal fun SessionBoardTopTabsBar(
    orderedViews: List<SessionBoardView>,
    currentPage: Int,
    viewLabels: SessionBoardViewLabels,
    boardItems: List<SessionBoardItem> = emptyList(),
    isEditing: Boolean,
    onSelectView: (SessionBoardView) -> Unit,
    onRenameViewRequested: (SessionBoardView, String) -> Unit,
    onDuplicateViewRequested: (SessionBoardView) -> Unit,
    onClearViewRequested: (SessionBoardView) -> Unit,
    onApplyBoardTemplate: (SessionBoardTemplatePreset) -> Unit,
    onToggleEditing: () -> Unit,
    onConfigureViews: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Precompute item counts per view for badges
        val itemCountPerView = remember(boardItems) {
            boardItems.groupBy { it.boardView }.mapValues { it.value.size }
        }

        Box(modifier = Modifier.weight(1f)) {
            ScrollableTabRow(
                selectedTabIndex = currentPage,
                edgePadding = 8.dp,
                divider = {},
                // Use transparent indicator — selection is shown via tab background
                indicator = { tabPositions ->
                    if (currentPage < tabPositions.size) {
                        Box(
                            modifier = Modifier
                                .tabIndicatorOffset(tabPositions[currentPage])
                                // transparent — pill background on the Tab itself handles selection
                        )
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                orderedViews.forEachIndexed { index, view ->
                    val label = viewLabels.labelFor(view)
                    val isSelected = currentPage == index
                    val itemCount = itemCountPerView[view] ?: 0
                    var activeTabMenu by remember(view) { mutableStateOf(false) }

                    val tabBg by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surface.copy(alpha = 0f),
                        animationSpec = tween(200),
                        label = "tabBg"
                    )
                    val tabContentColor by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        animationSpec = tween(200),
                        label = "tabContent"
                    )

                    Tab(
                        selected = isSelected,
                        onClick = { onSelectView(view) },
                        selectedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(vertical = 6.dp, horizontal = 2.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(tabBg),
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = label,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = tabContentColor
                                )
                                // Item count badge — only show when there are items
                                if (itemCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(
                                                if (isSelected)
                                                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f)
                                                else
                                                    MaterialTheme.colorScheme.surfaceVariant
                                            )
                                            .padding(horizontal = 5.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = itemCount.toString(),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = tabContentColor
                                        )
                                    }
                                }
                                if (isEditing) {
                                    Box {
                                        IconButton(
                                            onClick = { activeTabMenu = true },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.MoreVert,
                                                contentDescription = "Opciones",
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        DropdownMenu(
                                            expanded = activeTabMenu,
                                            onDismissRequest = { activeTabMenu = false }
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text("Renombrar") },
                                                leadingIcon = { Icon(Icons.Default.Edit, null) },
                                                onClick = {
                                                    activeTabMenu = false
                                                    onRenameViewRequested(view, label)
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Duplicar vista") },
                                                leadingIcon = { Icon(Icons.Default.ContentCopy, null) },
                                                onClick = {
                                                    activeTabMenu = false
                                                    onDuplicateViewRequested(view)
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Limpiar vista") },
                                                leadingIcon = { Icon(Icons.Default.DeleteOutline, null) },
                                                onClick = {
                                                    activeTabMenu = false
                                                    onClearViewRequested(view)
                                                }
                                            )
                                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                                            var showTemplatesMenu by remember(view) { mutableStateOf(false) }
                                            DropdownMenuItem(
                                                text = { Text("Cargar plantilla...") },
                                                leadingIcon = { Icon(Icons.Default.Layers, null) },
                                                onClick = { showTemplatesMenu = true }
                                            )
                                            DropdownMenu(
                                                expanded = showTemplatesMenu,
                                                onDismissRequest = { showTemplatesMenu = false }
                                            ) {
                                                DropdownMenuItem(
                                                    text = { Text("Exploración") },
                                                    onClick = {
                                                        showTemplatesMenu = false
                                                        activeTabMenu = false
                                                        onApplyBoardTemplate(SessionBoardTemplatePreset.EXPLORATION)
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("Narrativa") },
                                                    onClick = {
                                                        showTemplatesMenu = false
                                                        activeTabMenu = false
                                                        onApplyBoardTemplate(SessionBoardTemplatePreset.NARRATIVE)
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("Combate") },
                                                    onClick = {
                                                        showTemplatesMenu = false
                                                        activeTabMenu = false
                                                        onApplyBoardTemplate(SessionBoardTemplatePreset.COMBAT)
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(end = 16.dp)
        ) {
            if (isEditing) {
                IconButton(onClick = onConfigureViews) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Configurar vistas",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            TextButton(
                onClick = onToggleEditing,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = if (isEditing) Icons.Default.Check else Icons.Default.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(if (isEditing) "Hecho" else "Editar")
            }
        }
    }
}
