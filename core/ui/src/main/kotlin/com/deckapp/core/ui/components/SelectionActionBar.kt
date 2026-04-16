package com.deckapp.core.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SelectionActionBar(
    count: Int,
    onClear: () -> Unit,
    onDelete: () -> Unit,
    onArchive: ((Boolean) -> Unit)? = null,
    onTogglePin: ((Boolean) -> Unit)? = null,
    onAddTag: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        tonalElevation = 8.dp,
        shadowElevation = 8.dp,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            IconButton(onClick = onClear) {
                Icon(Icons.Default.Clear, contentDescription = "Cancelar")
            }
            Text(
                text = "$count seleccionados",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            
            if (onAddTag != null) {
                IconButton(onClick = onAddTag) {
                    Icon(Icons.Default.Tag, contentDescription = "Etiquetar")
                }
            }

            if (onTogglePin != null) {
                IconButton(onClick = { onTogglePin(true) }) {
                    Icon(Icons.Default.PushPin, contentDescription = "Fijar")
                }
            }

            if (onArchive != null) {
                IconButton(onClick = { onArchive(true) }) {
                    Icon(Icons.Default.Archive, contentDescription = "Archivar")
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete, 
                    contentDescription = "Eliminar",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
