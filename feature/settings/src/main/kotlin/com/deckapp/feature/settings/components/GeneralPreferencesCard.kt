package com.deckapp.feature.settings.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import kotlin.math.roundToInt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.deckapp.core.model.GeneralPreferences
import com.deckapp.core.model.StartPage
import com.deckapp.core.ui.components.DeckCard

@Composable
fun GeneralPreferencesCard(preferences: GeneralPreferences, section: String, onChange: (GeneralPreferences) -> Unit) {
    DeckCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            when (section) {
                "General" -> {
                    Text("Pantalla inicial", style = MaterialTheme.typography.titleMedium)
                    Text("Se aplica la próxima vez que abras la app.")
                    Column(Modifier.selectableGroup()) {
                        StartPage.entries.forEach { page ->
                            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).selectable(
                                selected = preferences.startPage == page,
                                role = Role.RadioButton,
                                onClick = { onChange(preferences.copy(startPage = page)) }
                            ), verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = preferences.startPage == page, onClick = null)
                                Spacer(Modifier.width(8.dp))
                                Text(when (page) {
                                    StartPage.LIBRARY -> "Biblioteca"
                                    StartPage.SESSIONS -> "Sesiones"
                                    StartPage.REFERENCES -> "Referencias"
                                })
                            }
                        }
                    }
                }
                "Lectura" -> {
                    Text("Tamaño de texto", style = MaterialTheme.typography.titleMedium)
                    Text("${(preferences.textScale * 100).roundToInt()} % · Se suma al tamaño de texto de Android.")
                    Slider(modifier = Modifier.semantics { contentDescription = "Tamaño de texto" }, value = preferences.textScale, onValueChange = { onChange(preferences.copy(textScale = it)) },
                        valueRange = 1f..1.3f, steps = 2)
                    Text("Vista previa: una carta, una pista y una nueva aventura.", style = MaterialTheme.typography.bodyLarge)
                }
                "Partidas" -> {
                    PreferenceSwitch("Mantener pantalla encendida", "Mientras estés en la mesa de juego.",
                        preferences.keepScreenOn) { onChange(preferences.copy(keepScreenOn = it)) }
                    PreferenceSwitch("Vibración", "Respuesta táctil en cartas, dados y otras acciones.",
                        preferences.hapticsEnabled) { onChange(preferences.copy(hapticsEnabled = it)) }
                }
            }
            Text("Los cambios se guardan automáticamente.", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun PreferenceSwitch(title: String, description: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(
        value = checked, role = Role.Switch, onValueChange = onChange
    ), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(description, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}
