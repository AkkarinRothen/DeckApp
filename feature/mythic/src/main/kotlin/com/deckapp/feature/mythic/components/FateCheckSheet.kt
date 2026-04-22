package com.deckapp.feature.mythic.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.deckapp.core.model.FateResult
import com.deckapp.core.model.ProbabilityLevel
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import android.content.ClipData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FateCheckSheet(
    onPerformRoll: (String, ProbabilityLevel) -> Unit,
    onDismiss: () -> Unit
) {
    var question by remember { mutableStateOf("") }
    var selectedProbability by remember { mutableStateOf(ProbabilityLevel.FIFTY_FIFTY) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Preguntar al Oráculo", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        OutlinedTextField(
            value = question,
            onValueChange = { question = it },
            label = { Text("Pregunta (opcional)") },
            placeholder = { Text("¿Hay guardias en la puerta?") },
            modifier = Modifier.fillMaxWidth()
        )

        Text("Probabilidad", style = MaterialTheme.typography.labelMedium)
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(ProbabilityLevel.entries) { prob ->
                FilterChip(
                    selected = selectedProbability == prob,
                    onClick = { selectedProbability = prob },
                    label = { Text(prob.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }) }
                )
            }
        }

        Button(
            onClick = {
                onPerformRoll(question, selectedProbability)
                onDismiss()
            },
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Icon(Icons.Default.Casino, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("TIRAR")
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FateResultCard(result: FateResult, roll: Int, isRandomEvent: Boolean, eventFocus: String, eventAction: String, eventSubject: String) {
    val color = when (result) {
        FateResult.EXCEPTIONAL_YES, FateResult.YES -> Color(0xFF4CAF50)
        FateResult.EXCEPTIONAL_NO, FateResult.NO -> Color(0xFFF44336)
        else -> MaterialTheme.colorScheme.onSurface
    }

    val resultText = buildString {
        append(result.name.replace("_", " "))
        if (isRandomEvent) {
            append("\nEvento: $eventFocus - $eventAction $eventSubject")
        }
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .dragAndDropSource {
                detectTapGestures(
                    onLongPress = {
                        startTransfer(
                            DragAndDropTransferData(
                                clipData = ClipData.newPlainText("mythic_result", resultText)
                            )
                        )
                    }
                )
            },
        colors = CardDefaults.elevatedCardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    result.name.replace("_", " "),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = color
                )
                Spacer(Modifier.weight(1f))
                Text("Roll: $roll", style = MaterialTheme.typography.labelMedium)
            }

            if (isRandomEvent) {
                Spacer(Modifier.height(12.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("¡EVENTO ALEATORIO!", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                        if (eventFocus.isNotBlank()) {
                            Text("Foco: $eventFocus", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                        if (eventAction.isNotBlank()) {
                            Text("$eventAction $eventSubject", style = MaterialTheme.typography.bodyLarge)
                        } else {
                            Text("Extrayendo significado del destino...", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
