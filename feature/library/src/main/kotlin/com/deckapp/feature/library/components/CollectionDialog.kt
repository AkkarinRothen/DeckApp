package com.deckapp.feature.library.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.deckapp.core.model.CollectionIcon
import com.deckapp.core.model.DeckCollection
import com.deckapp.core.ui.components.*

@Composable
fun CollectionDialog(
    initialName: String = "", initialColor: Int = 0xFF6200EE.toInt(),
    initialIcon: CollectionIcon = CollectionIcon.CHEST, initialImageUrl: String? = null,
    currentImageUri: String? = null, title: String = "Nueva colección", confirmLabel: String = "Crear",
    onDismiss: () -> Unit, onPickImage: () -> Unit = {},
    onConfirm: (String, Int, CollectionIcon, Boolean) -> Unit
) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    var icon by rememberSaveable { mutableStateOf(initialIcon) }
    var color by rememberSaveable { mutableStateOf(initialColor) }
    var automatic by rememberSaveable { mutableStateOf(initialImageUrl == null && currentImageUri == null) }
    LaunchedEffect(currentImageUri) { if (currentImageUri != null) automatic = false }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = {
        Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(name, { name = it }, label = { Text("Nombre") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Box(Modifier.fillMaxWidth().height(180.dp)) {
                CollectionCover(DeckCollection(name = name.ifBlank { "Mi colección" }, color = color, icon = icon,
                    imageUrl = if (automatic) null else currentImageUri ?: initialImageUrl))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Portada automática", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                Switch(automatic, { automatic = it })
            }
            DeckOutlinedButton(onClick = onPickImage) { Text("Elegir imagen de portada") }
            Text("Emblema", style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CollectionIcon.entries.forEach { value -> item {
                    FilterChip(selected = icon == value, onClick = { icon = value }, modifier = Modifier.heightIn(min = 48.dp),
                        label = { Text(value.label()) }, leadingIcon = { Icon(value.toIcon(), null, Modifier.size(20.dp)) })
                } }
            }
            Text("Color", style = MaterialTheme.typography.labelLarge)
            CollectionColorPicker(color) { color = it }
        }
    }, confirmButton = { DeckButton(onClick = { onConfirm(name.trim(), color, icon, automatic) }, enabled = name.isNotBlank()) { Text(confirmLabel) } },
        dismissButton = { DeckTextButton(onClick = onDismiss) { Text("Cancelar") } })
}

@Composable
private fun CollectionColorPicker(selected: Int, onSelect: (Int) -> Unit) {
    val colors = listOf("Borgoña" to 0xFF7B1E2B, "Bosque" to 0xFF28613B, "Cobre" to 0xFF864B24,
        "Arcano" to 0xFF70429A, "Océano" to 0xFF006A78, "Pizarra" to 0xFF495767)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        colors.forEach { (label, value) -> item {
            FilterChip(selected = selected == value.toInt(), onClick = { onSelect(value.toInt()) },
                modifier = Modifier.heightIn(min = 48.dp), label = { Text(label) },
                leadingIcon = { Surface(color = Color(value), shape = MaterialTheme.shapes.small, modifier = Modifier.size(20.dp)) {} })
        } }
    }
}

private fun CollectionIcon.label(): String = when (this) {
    CollectionIcon.CHEST -> "Tesoro"; CollectionIcon.BOOK -> "Libro"; CollectionIcon.MAP -> "Mapa"
    CollectionIcon.SKULL -> "Peligro"; CollectionIcon.BAG -> "Equipo"; CollectionIcon.SWORDS -> "Combate"
    CollectionIcon.MOUNTAIN -> "Mundo"; CollectionIcon.PEOPLE -> "Personajes"; CollectionIcon.FOLDER -> "Carpeta"
    CollectionIcon.ARCANE -> "Magia"; CollectionIcon.COMPASS -> "Exploración"
}
