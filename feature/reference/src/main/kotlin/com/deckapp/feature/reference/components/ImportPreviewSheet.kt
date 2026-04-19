package com.deckapp.feature.reference.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.deckapp.core.model.ImportPreviewData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportPreviewSheet(
    importData: ImportPreviewData?,
    isLoading: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onConfirm: (headers: List<String>, replace: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var replaceExisting by remember { mutableStateOf(true) }
    var headers by remember(importData) { 
        mutableStateOf(importData?.headers ?: emptyList()) 
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 16.dp)
        ) {
            Text(
                text = "Previsualizar importación",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(16.dp)
            )

            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (error != null) {
                Column(
                    modifier = Modifier.fillMaxWidth().weight(1f).padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(16.dp))
                    Text(text = error, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = onDismiss) { Text("Cerrar") }
                }
            } else if (importData != null) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Encabezados (puedes editarlos):",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    
                    LazyRow(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(headers) { index, header ->
                            OutlinedTextField(
                                value = header,
                                onValueChange = { newValue ->
                                    headers = headers.toMutableList().apply { this[index] = newValue }
                                },
                                modifier = Modifier.width(120.dp),
                                singleLine = true
                            )
                        }
                    }

                    Text(
                        text = "Vista previa (primeras 5 filas):",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(Modifier.height(8.dp))

                    LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp)) {
                        itemsIndexed(importData.rows.take(5)) { rowIndex, row ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (rowIndex % 2 == 0) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(8.dp)
                            ) {
                                row.forEach { cell ->
                                    Text(
                                        text = cell,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = replaceExisting, onClick = { replaceExisting = true })
                        Text("Reemplazar actual", modifier = Modifier.clickable { replaceExisting = true })
                        Spacer(Modifier.width(16.dp))
                        RadioButton(selected = !replaceExisting, onClick = { replaceExisting = false })
                        Text("Añadir al final", modifier = Modifier.clickable { replaceExisting = false })
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancelar")
                    }
                    Button(
                        onClick = { onConfirm(headers, replaceExisting) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Importar ${importData.rows.size} filas")
                    }
                }
            }
        }
    }
}
