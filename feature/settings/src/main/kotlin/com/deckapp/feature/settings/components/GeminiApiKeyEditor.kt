package com.deckapp.feature.settings.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.deckapp.core.ui.components.*

@Composable
fun GeminiApiKeyEditor(currentKey: String, onKeyChanged: (String) -> Unit, isTesting: Boolean,
    connectionMessage: String?, onTest: () -> Unit) {
    var text by remember(currentKey) { mutableStateOf(currentKey) }
    var visible by remember(currentKey) { mutableStateOf(false) }
    DeckCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Gemini AI (Google)", style = MaterialTheme.typography.titleMedium)
            Text("Al procesar tablas con IA, los datos se envían a Google.", style = MaterialTheme.typography.bodySmall)
            Text(if (currentKey.isBlank()) "Sin clave guardada" else "Clave guardada", style = MaterialTheme.typography.labelLarge)
            OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("Google AI API Key") },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Password
                ),
                visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { visible = !visible }) {
                        Icon(if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (visible) "Ocultar clave" else "Mostrar clave")
                    }
                })
            FlowButtons(text, currentKey, isTesting, onKeyChanged, onTest)
            if (isTesting) LinearProgressIndicator(Modifier.fillMaxWidth())
            connectionMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            Text("Probar conexión envía una solicitud breve a Google y puede consumir cuota.", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowButtons(text: String, currentKey: String, isTesting: Boolean, onKeyChanged: (String) -> Unit, onTest: () -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        DeckButton(onClick = { onKeyChanged(text) }, enabled = text.trim() != currentKey) { Text("Guardar") }
        DeckOutlinedButton(onClick = onTest, enabled = !isTesting && currentKey.isNotBlank() && text.trim() == currentKey) {
            Text(if (isTesting) "Probando…" else "Probar conexión")
        }
        DeckTextButton(onClick = { onKeyChanged("") }, enabled = currentKey.isNotBlank()) { Text("Eliminar clave") }
    }
}
