package com.deckapp.feature.dice

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiceRollerBottomSheet(
    onDismiss: () -> Unit,
    viewModel: DiceRollerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current

    Column(modifier = Modifier.padding(16.dp).padding(bottom = 32.dp)) {
        Text("Rodillo de Dados", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        // Selector de dados NdM
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            DiceSelector("Cantidad", uiState.count, 1, 20) { viewModel.updateCount(it) }
            Text("d", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            DiceSelector("Caras", uiState.sides, 2, 100) { viewModel.updateSides(it) }
            Text("+", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            DiceSelector("Mod.", uiState.modifier, -50, 50) { viewModel.updateModifier(it) }
        }

        Spacer(Modifier.height(24.dp))

        // Resultado actual con animación sutil
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .background(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.shapes.medium),
            contentAlignment = Alignment.Center
        ) {
            if (uiState.lastResult != null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        uiState.lastResult!!.total.toString(),
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        uiState.lastResult!!.expression,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                    )
                }
            } else {
                Text("Listo para tirar", color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.5f))
            }
        }

        Spacer(Modifier.height(24.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { 
                    viewModel.rollDice()
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                },
                modifier = Modifier.weight(1f).height(56.dp)
            ) {
                Text("Lanzar")
            }
            
            OutlinedButton(
                onClick = { viewModel.clearHistory() },
                modifier = Modifier.height(56.dp)
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Limpiar historial")
            }
        }

        Spacer(Modifier.height(24.dp))

        // Historial reciente
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.History, null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text("Historial reciente", style = MaterialTheme.typography.labelLarge)
        }
        
        Spacer(Modifier.height(8.dp))
        
        LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
            items(uiState.history) { roll ->
                ListItem(
                    headlineContent = { Text("${roll.total}", fontWeight = FontWeight.Bold) },
                    supportingContent = { Text(roll.expression) },
                    trailingContent = { Text(roll.timestampLabel, style = MaterialTheme.typography.bodySmall) }
                )
                HorizontalDivider(thickness = 0.5.dp)
            }
        }
    }
}

@Composable
private fun DiceSelector(label: String, value: Int, min: Int, max: Int, onValueChange: (Int) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall)
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { if (value > min) onValueChange(value - 1) }) { Text("-", fontSize = 20.sp) }
            Text(value.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            TextButton(onClick = { if (value < max) onValueChange(value + 1) }) { Text("+", fontSize = 20.sp) }
        }
    }
}
