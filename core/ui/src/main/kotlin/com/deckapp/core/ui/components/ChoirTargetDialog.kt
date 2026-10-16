package com.deckapp.core.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.deckapp.core.domain.usecase.ChoirCombatRules
import com.deckapp.core.model.ChoirProfile

/** Player-facing check assistant. Mastery and Burden are applied after determining the NO. */
@Composable
internal fun ChoirTargetDialog(profile: ChoirProfile, onDismiss: () -> Unit) {
    val choices = listOf("CD", "Ataque", "Constitución", "Destreza", "Percepción", "Fuerza", "Voluntad")
        .filter { ChoirCombatRules.dynamicTarget(profile.statistics[it].orEmpty(), 0) != null }
    var selected by rememberSaveable { mutableStateOf(choices.firstOrNull().orEmpty()) }
    var modifierInput by rememberSaveable { mutableStateOf("0") }
    var expanded by remember { mutableStateOf(false) }
    val grade = profile.statistics[selected].orEmpty()
    val target = modifierInput.toIntOrNull()?.let { ChoirCombatRules.dynamicTarget(grade, it) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Chequeo del PJ") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box {
                    OutlinedButton(onClick = { expanded = true }, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(when (selected) { "CD" -> "Atacar · Defensa $grade"; "Ataque" -> "Defenderse · Ataque $grade"; else -> "$selected · $grade" })
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        choices.forEach { key -> DropdownMenuItem(
                            text = { Text("$key · ${profile.statistics[key]}") },
                            onClick = { selected = key; expanded = false }
                        ) }
                    }
                }
                OutlinedTextField(value = modifierInput, onValueChange = { modifierInput = it },
                    label = { Text("Modificador habitual del PJ") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    supportingText = { Text("Tabla del manual: −5 a +10. Sin Maestría ni Carga.") })
                Text(target?.let { "Número objetivo dinámico: $it" } ?: "Ingresa un modificador de −5 a +10.", style = MaterialTheme.typography.titleMedium)
                Text("Tira 1d20 + modificador habitual; después aplica Maestría y Carga. El PNJ no tira.")
                if (selected == "Ataque") Text("Aplica Precisión ${profile.statistics["Precisión"]} como Carga a la defensa del PJ.")
                if (selected == "CD") Text("Las heridas menores sufridas por el PNJ otorgan Maestría al atacante por Desgaste; revisa sus rasgos.")
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } }
    )
}
