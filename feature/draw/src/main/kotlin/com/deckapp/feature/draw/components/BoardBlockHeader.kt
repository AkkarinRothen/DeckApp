package com.deckapp.feature.draw.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
internal fun BoardBlockHeader(
    meta: BoardBlockMeta,
    accent: Color,
    isEditing: Boolean,
    isPinned: Boolean,
    isCollapsed: Boolean,
    isSaving: Boolean,
    showSubtitle: Boolean,
    onExpand: () -> Unit,
    onTogglePin: () -> Unit,
    onToggleCollapse: () -> Unit,
    onConfigure: () -> Unit,
    onRemove: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
    val directCollapse = maxWidth >= 320.dp
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(meta.icon, contentDescription = null, tint = accent, modifier = Modifier.size(24.dp))
        Column(Modifier.weight(1f)) {
            Text(meta.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (showSubtitle && meta.subtitle.isNotBlank()) {
                Text(meta.subtitle, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (isSaving) Icon(Icons.Default.CloudSync, "Guardando", modifier = Modifier.size(16.dp))
        if (isPinned) Icon(Icons.Default.PushPin, "Bloque fijado", tint = accent, modifier = Modifier.size(16.dp))
        if (isEditing) Icon(Icons.Default.DragHandle, "Arrastrar para reordenar", modifier = Modifier.size(20.dp))
        if (!isEditing && directCollapse) {
            IconButton(onClick = onToggleCollapse, modifier = Modifier.size(48.dp)) {
                Icon(if (isCollapsed) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                    contentDescription = if (isCollapsed) "Expandir bloque" else "Contraer bloque")
            }
        }
        Box {
            IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.MoreVert, contentDescription = "Acciones de ${meta.title}")
            }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                if (!isEditing) {
                    DropdownMenuItem(text = { Text(if (isCollapsed) "Expandir contenido" else "Contraer contenido") },
                        leadingIcon = { Icon(if (isCollapsed) Icons.Default.ExpandMore else Icons.Default.ExpandLess, contentDescription = null) },
                        onClick = { menuExpanded = false; onToggleCollapse() })
                    DropdownMenuItem(text = { Text("Ampliar bloque") },
                        leadingIcon = { Icon(Icons.Default.OpenInFull, contentDescription = null) },
                        onClick = { menuExpanded = false; onExpand() })
                }
                DropdownMenuItem(text = { Text(if (isPinned) "Desfijar bloque" else "Fijar bloque") },
                    leadingIcon = { Icon(Icons.Default.PushPin, contentDescription = null) },
                    onClick = { menuExpanded = false; onTogglePin() })
                DropdownMenuItem(text = { Text("Configurar bloque") },
                    leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    onClick = { menuExpanded = false; onConfigure() })
                if (isEditing) {
                    DropdownMenuItem(text = { Text("Quitar de la mesa", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Default.DeleteOutline, contentDescription = null) },
                        onClick = { menuExpanded = false; onRemove() })
                }
            }
        }
    }
    }
}
