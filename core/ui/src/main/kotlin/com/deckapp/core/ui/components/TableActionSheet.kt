package com.deckapp.core.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import com.deckapp.core.model.RandomTable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TableActionSheet(
    table: RandomTable,
    onDismiss: () -> Unit,
    onPin: () -> Unit,
    onAddToCollection: () -> Unit,
    onChangeImage: () -> Unit,
    onDelete: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = table.name,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp)
            )
            
            ListItem(
                headlineContent = { Text(if (table.isPinned) "Desanclar" else "Anclar a favoritos") },
                leadingContent = { Icon(if (table.isPinned) Icons.Default.StarOutline else Icons.Default.Star, null) },
                modifier = Modifier.clickable { 
                    onPin()
                    onDismiss()
                }
            )
            
            ListItem(
                headlineContent = { Text("Añadir al Baúl") },
                leadingContent = { Icon(Icons.Default.Inventory, null) },
                modifier = Modifier.clickable { 
                    onAddToCollection()
                    onDismiss()
                }
            )
            
            ListItem(
                headlineContent = { Text("Cambiar Imagen de Portada") },
                leadingContent = { Icon(Icons.Default.Image, null) },
                modifier = Modifier.clickable { 
                    onChangeImage()
                    onDismiss()
                }
            )
            
            ListItem(
                headlineContent = { Text("Eliminar Tabla", color = MaterialTheme.colorScheme.error) },
                leadingContent = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                modifier = Modifier.clickable { 
                    onDelete()
                    onDismiss()
                }
            )
        }
    }
}
