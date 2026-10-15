package com.deckapp.feature.settings.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.deckapp.core.ui.components.DeckButton
import com.deckapp.core.ui.components.DeckCard
import com.deckapp.core.ui.components.DeckOutlinedButton
import com.deckapp.core.ui.components.DeckTextButton

/** A real dialog lets the user inspect fields, cards, focus and actions in the chosen theme. */
@Composable
fun ThemeComponentsPreview() {
    var open by rememberSaveable { mutableStateOf(false) }
    var campaign by rememberSaveable { mutableStateOf("El bosque de las brumas") }
    DeckTextButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) {
        Text("Probar tarjetas, campos y ventanas")
    }
    if (open) {
        AlertDialog(
            onDismissRequest = { open = false },
            title = { Text("Vista previa del tema") },
            text = {
                Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Probá el campo y los controles con el tema elegido.")
                    DeckCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Notas de aventura", style = MaterialTheme.typography.titleMedium)
                            Text("Un viejo mapa señala un refugio junto al río.", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    OutlinedTextField(value = campaign, onValueChange = { campaign = it },
                        label = { Text("Nombre de ejemplo") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    DeckOutlinedButton(onClick = { campaign = "El bosque de las brumas" }) { Text("Restablecer ejemplo") }
                    DeckButton(onClick = {}, enabled = false) { Text("Acción deshabilitada") }
                }
            },
            confirmButton = { DeckButton(onClick = { open = false }) { Text("Listo") } }
        )
    }
}
