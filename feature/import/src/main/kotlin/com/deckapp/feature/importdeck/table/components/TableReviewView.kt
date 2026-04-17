package com.deckapp.feature.importdeck.table.components

import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.deckapp.core.domain.usecase.RangeParser
import com.deckapp.core.model.OcrBlock
import com.deckapp.core.model.TableEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TableReviewView(
    entries: List<TableEntry>,
    tableName: String,
    tableTag: String,
    tableFormula: String = "1d6",
    onEntryChange: (Int, TableEntry) -> Unit,
    onNameChange: (String) -> Unit,
    onTagChange: (String) -> Unit,
    onFormulaChange: (String) -> Unit = {},
    onConfirm: () -> Unit,
    validationResult: RangeParser.ValidationResult?,
    lowConfidenceIndices: Set<Int>,
    confidenceThreshold: Float,
    onThresholdChange: (Float) -> Unit,
    tableProgress: String,
    isAiProcessing: Boolean = false,
    isVisionProcessing: Boolean = false,
    onAiOptimize: () -> Unit = {},
    // Overlay params
    croppedBitmap: Bitmap? = null,
    ocrBlocks: List<OcrBlock> = emptyList(),
    detectedAnchors: List<Float> = emptyList()
) {
    var showOverlay by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Revisando tabla: $tableProgress", style = MaterialTheme.typography.labelMedium)
            
            Row {
                if (isAiProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp).padding(4.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    IconButton(onClick = onAiOptimize) {
                        Icon(Icons.Default.AutoFixHigh, "Optimizar con Gemini IA", tint = MaterialTheme.colorScheme.primary)
                    }
                }

                if (croppedBitmap != null) {
                    IconButton(onClick = { showOverlay = !showOverlay }) {
                        Icon(
                            if (showOverlay) Icons.AutoMirrored.Filled.List else Icons.Default.Image,
                            if (showOverlay) "Ver lista" else "Ver original"
                        )
                    }
                }
                
                if (validationResult?.isValid == true) {
                    Icon(Icons.Default.Check, "Válido", tint = Color.Green, modifier = Modifier.padding(8.dp))
                } else {
                    Icon(Icons.Default.Warning, "Errores en rangos", tint = MaterialTheme.colorScheme.error, modifier = Modifier.padding(8.dp))
                }
            }
        }
        
        Spacer(Modifier.height(8.dp))

        if (showOverlay && croppedBitmap != null) {
            Text("Inspección visual de OCR", style = MaterialTheme.typography.titleSmall)
            Text("Rojo = Baja confianza, Verde = Alta confianza", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
            TableOcrOverlay(
                bitmap = croppedBitmap,
                blocks = ocrBlocks,
                anchors = detectedAnchors,
                lowConfidenceIndices = lowConfidenceIndices,
                modifier = Modifier.weight(1f)
            )
        } else {
            // Slider de confianza
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Umbral de confianza: ${(confidenceThreshold * 100).toInt()}%", 
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f))
                    if (lowConfidenceIndices.isNotEmpty()) {
                        Text("${lowConfidenceIndices.size} avisos", 
                            style = MaterialTheme.typography.bodySmall, 
                            color = MaterialTheme.colorScheme.error)
                    }
                }
                Slider(
                    value = confidenceThreshold,
                    onValueChange = onThresholdChange,
                    valueRange = 0f..1f,
                    modifier = Modifier.height(24.dp)
                )
            }

            Spacer(Modifier.height(8.dp))
            
            if (isVisionProcessing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(4.dp))
                Text(
                    "Vision AI extrayendo entradas...",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(8.dp))
            }

            OutlinedTextField(
                value = tableName,
                onValueChange = onNameChange,
                label = { Text("Nombre de la tabla") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = tableFormula,
                    onValueChange = onFormulaChange,
                    label = { Text("Fórmula de dado") },
                    placeholder = { Text("1d6") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = tableTag,
                    onValueChange = onTagChange,
                    label = { Text("Categoría") },
                    modifier = Modifier.weight(2f)
                )
            }
            
            Spacer(Modifier.height(16.dp))
            
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(entries) { index, entry ->
                    EntryRow(
                        entry = entry,
                        isLowConfidence = index in lowConfidenceIndices,
                        onChange = { onEntryChange(index, it) }
                    )
                }
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        Button(
            onClick = onConfirm,
            modifier = Modifier.fillMaxWidth(),
            enabled = validationResult?.isValid == true || showOverlay
        ) {
            Text(if (showOverlay) "Volver a la lista" else "Continuar")
        }
    }

    // Al hacer click en el botón de confirmar en modo overlay, simplemente volvemos a la lista
    LaunchedEffect(showOverlay) {
        if (!showOverlay && validationResult?.isValid == false) {
            // Opcional: scroll al primer error
        }
    }
}

@Composable
private fun EntryRow(
    entry: TableEntry,
    isLowConfidence: Boolean,
    onChange: (TableEntry) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isLowConfidence) 
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = "${entry.minRoll}",
                onValueChange = { val v = it.toIntOrNull() ?: entry.minRoll; onChange(entry.copy(minRoll = v)) },
                modifier = Modifier.width(60.dp),
                singleLine = true
            )
            Text("-", modifier = Modifier.padding(horizontal = 4.dp))
            OutlinedTextField(
                value = "${entry.maxRoll}",
                onValueChange = { val v = it.toIntOrNull() ?: entry.maxRoll; onChange(entry.copy(maxRoll = v)) },
                modifier = Modifier.width(60.dp),
                singleLine = true
            )
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(
                value = entry.text,
                onValueChange = { onChange(entry.copy(text = it)) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                trailingIcon = {
                    if (isLowConfidence) {
                        Icon(Icons.Default.Warning, "Baja confianza", tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }
    }
}
