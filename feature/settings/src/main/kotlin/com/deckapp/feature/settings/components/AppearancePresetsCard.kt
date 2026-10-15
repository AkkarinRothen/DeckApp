package com.deckapp.feature.settings.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.deckapp.core.model.AppearancePreset
import com.deckapp.core.model.ThemeAccent
import com.deckapp.core.ui.components.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppearancePresetsCard(
    accent: ThemeAccent, presets: List<AppearancePreset>,
    onAccent: (ThemeAccent) -> Unit, onSave: (String) -> Unit,
    onApply: (String) -> Unit, onDelete: (String) -> Unit
) {
    var saving by rememberSaveable { mutableStateOf(false) }
    var deleting by rememberSaveable { mutableStateOf<String?>(null) }
    DeckCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ThemeChapterHeader("Ambiente de campaña")
            Text("Acento", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeAccent.entries.forEach { value ->
                    FilterChip(selected = accent == value, onClick = { onAccent(value) },
                        modifier = Modifier.heightIn(min = 48.dp), label = { Text(when (value) {
                            ThemeAccent.THEME -> "Del tema"; ThemeAccent.FOREST -> "Bosque"
                            ThemeAccent.COPPER -> "Cobre"; ThemeAccent.ARCANE -> "Arcano"
                        }) })
                }
            }
            Text("Guarda tema, acento, textura y densidad con el nombre de tu campaña. Guardar el mismo nombre actualiza su estilo.",
                style = MaterialTheme.typography.bodySmall)
            DeckOutlinedButton(onClick = { saving = true }) { Text("Guardar estilo actual") }
            if (presets.isEmpty()) Text("Todavía no hay estilos guardados.", style = MaterialTheme.typography.bodySmall)
            else LazyColumn(Modifier.fillMaxWidth().heightIn(max = 280.dp)) {
                items(presets, key = { it.id }) { preset ->
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Text(preset.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        DeckTextButton(onClick = { onApply(preset.id) }) { Text("Aplicar") }
                        IconButton(onClick = { deleting = preset.id }) {
                            Icon(Icons.Default.Delete, "Eliminar estilo ${preset.name}")
                        }
                    }
                }
            }
        }
    }
    if (saving) SavePresetDialog(onDismiss = { saving = false }, onSave = { onSave(it); saving = false })
    deleting?.let { id -> AlertDialog(onDismissRequest = { deleting = null },
        title = { Text("Eliminar estilo") }, text = { Text("Se eliminará esta combinación guardada. La apariencia actual se conserva.") },
        confirmButton = { DeckTextButton(onClick = { onDelete(id); deleting = null }) { Text("Eliminar") } },
        dismissButton = { DeckTextButton(onClick = { deleting = null }) { Text("Cancelar") } }) }
}

@Composable
private fun SavePresetDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Guardar estilo de campaña") },
        text = { OutlinedTextField(value = name, onValueChange = { name = it.take(60) },
            label = { Text("Nombre") }, singleLine = true) },
        confirmButton = { DeckButton(onClick = { onSave(name) }, enabled = name.isNotBlank()) { Text("Guardar") } },
        dismissButton = { DeckTextButton(onClick = onDismiss) { Text("Cancelar") } })
}
