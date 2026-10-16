package com.deckapp.feature.npcs.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deckapp.core.domain.usecase.*
import com.deckapp.core.model.Npc
import com.deckapp.core.ui.components.*
import com.deckapp.feature.npcs.ChoirCharacterCreationViewModel

@Composable
fun ChoirOfFleshCharacterDialog(
    onDismiss: () -> Unit,
    onCharacterCreated: (Npc, String) -> Unit,
    viewModel: ChoirCharacterCreationViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val s = state.sheet
    val dismiss = { viewModel.reset(); onDismiss() }
    Dialog(onDismissRequest = dismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxWidth(0.96f).fillMaxHeight(0.94f), shape = MaterialTheme.shapes.large) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Crear Inquebrantable", style = MaterialTheme.typography.headlineSmall)
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ChoirTextField("Nombre", state.name, viewModel::name)
                    Text("Tira seis veces 4d6, descartando el menor. Puedes reasignar los resultados entre atributos o usar entrada manual.")
                    Button(onClick = viewModel::roll, modifier = Modifier.heightIn(min = 48.dp)) { Text("Tirar 6 × 4d6, descartar menor") }
                    if (state.pool.isNotEmpty()) {
                        Text("Resultados que debes asignar: ${state.pool.joinToString()}")
                        TextButton(onClick = viewModel::manual, modifier = Modifier.heightIn(min = 48.dp)) { Text("Usar entrada manual") }
                    }
                    ChoirCharacterFields(s, viewModel::sheet, creation = true)
                    Text("Trasfondos del manual · D10", style = MaterialTheme.typography.titleMedium)
                    ChoirBackgroundPicker("Ocupación", s.occupation, ChoirCreationCatalog.occupations,
                        { viewModel.sheet(s.copy(occupation = it)) }, { viewModel.randomChoice(ChoirCreationCatalog.occupations) })
                    ChoirBackgroundPicker("Pecado", s.sin, ChoirCreationCatalog.sins,
                        { viewModel.sheet(s.copy(sin = it)) }, { viewModel.randomChoice(ChoirCreationCatalog.sins) })
                    ChoirBackgroundPicker("Fragmento", s.fragment, ChoirCreationCatalog.fragments,
                        { viewModel.sheet(s.copy(fragment = it)) }, { viewModel.randomChoice(ChoirCreationCatalog.fragments) })
                    ChoirBackgroundPicker("Susurro", s.whisper, ChoirCreationCatalog.whispers,
                        { viewModel.sheet(s.copy(whisper = it)) }, { viewModel.randomChoice(ChoirCreationCatalog.whispers) })
                    ChoirCharacterDetails(s, viewModel::sheet, backgrounds = false)
                    Text("Inicio: Humanidad 20, Angustia 0, sin heridas ni Trauma. Vigor ${s.vigor}. Anota los consumibles adquiridos; la ocupación no asigna un arma automáticamente.")
                    viewModel.error()?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = dismiss, modifier = Modifier.heightIn(min = 48.dp)) { Text("Cancelar") }
                    Button(enabled = viewModel.error() == null, modifier = Modifier.heightIn(min = 48.dp), onClick = {
                        viewModel.create()?.let { npc ->
                            onCharacterCreated(npc, "# Inquebrantable: ${npc.name}\n\n${ChoirCharacterCodec.markdown(s)}")
                            viewModel.reset()
                        }
                    }) { Text("Crear personaje") }
                }
            }
        }
    }
}

@Composable
private fun ChoirBackgroundPicker(label: String, value: String, choices: List<ChoirBackgroundChoice>, onSelect: (String) -> Unit, random: () -> String) {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f)) {
                OutlinedButton(onClick = { expanded = true }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Elegir $label") }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    choices.forEach { choice -> DropdownMenuItem(text = { Text(choice.title) }, onClick = { onSelect(choice.text); expanded = false }) }
                }
            }
            TextButton(onClick = { onSelect(random()) }, modifier = Modifier.heightIn(min = 48.dp)) { Text("D10") }
        }
        MarkdownText(markdown = value)
    }
}
