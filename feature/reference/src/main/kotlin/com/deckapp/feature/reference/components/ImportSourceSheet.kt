package com.deckapp.feature.reference.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportSourceSheet(
    onDismiss: () -> Unit,
    onCsvSelected: (String) -> Unit,
    onMarkdownPaste: (String) -> Unit,
    onImageSelected: (android.net.Uri) -> Unit,
    modifier: Modifier = Modifier
) {
    var showMarkdownDialog by remember { mutableStateOf(false) }
    var markdownText by remember { mutableStateOf("") }

    val csvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            onCsvSelected(it.toString())
            onDismiss()
        }
    }

    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            onImageSelected(it)
            onDismiss()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Importar datos de tabla",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(16.dp)
            )

            ListItem(
                headlineContent = { Text("Archivo CSV / TSV") },
                supportingContent = { Text("Importar desde archivos .csv o .tsv") },
                leadingContent = { Icon(Icons.Default.TableChart, contentDescription = null) },
                modifier = Modifier.clickable {
                    csvLauncher.launch(arrayOf("text/comma-separated-values", "text/plain", "application/csv"))
                }
            )

            ListItem(
                headlineContent = { Text("Pegar Markdown") },
                supportingContent = { Text("Copiar y pegar una tabla de texto") },
                leadingContent = { Icon(Icons.Default.Description, contentDescription = null) },
                modifier = Modifier.clickable {
                    showMarkdownDialog = true
                }
            )

            ListItem(
                headlineContent = { Text("Desde imagen (OCR)") },
                supportingContent = { Text("Reconocer tabla usando IA (Gemini)") },
                leadingContent = { Icon(Icons.Default.Image, contentDescription = null) },
                modifier = Modifier.clickable {
                    imageLauncher.launch("image/*")
                }
            )
        }
    }

    if (showMarkdownDialog) {
        AlertDialog(
            onDismissRequest = { showMarkdownDialog = false },
            title = { Text("Pegar tabla Markdown") },
            text = {
                OutlinedTextField(
                    value = markdownText,
                    onValueChange = { markdownText = it },
                    placeholder = { Text("| Col 1 | Col 2 |\n|-------|-------|\n| Val 1 | Val 2 |") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    maxLines = 10
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onMarkdownPaste(markdownText)
                        showMarkdownDialog = false
                        onDismiss()
                    },
                    enabled = markdownText.isNotBlank()
                ) {
                    Text("Importar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMarkdownDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
