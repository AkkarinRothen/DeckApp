package com.deckapp.feature.mythic.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.deckapp.feature.mythic.MythicSessionUiState
import com.deckapp.feature.mythic.RollHistoryList
import com.deckapp.feature.mythic.MythicHeader

/**
 * Vista central del Oráculo Mythic (Fate Chart y Log).
 */
@Composable
fun MythicCenterView(
    uiState: MythicSessionUiState,
    onFateCheck: () -> Unit,
    onQuickCheck: () -> Unit,
    onSceneChange: (Int) -> Unit,
    onChaosChange: (Int) -> Unit,
    onCheckScene: () -> Unit,
    onFinishScene: () -> Unit,
    modifier: Modifier = Modifier
) {
    val chaosFactor = uiState.session?.chaosFactor ?: 5
    val chaosColor = animateColorAsState(
        targetValue = when {
            chaosFactor <= 3 -> MaterialTheme.colorScheme.primary
            chaosFactor <= 6 -> Color(0xFFFFA726)
            else -> Color(0xFFEF5350)
        },
        label = "ChaosColor"
    ).value

    Column(modifier = modifier.fillMaxSize()) {
        // Cabecera compacta Mítica
        MythicHeader(
            sceneNumber = uiState.session?.sceneNumber ?: 1,
            chaosFactor = chaosFactor,
            chaosColor = chaosColor,
            onSceneChange = onSceneChange,
            onChaosChange = onChaosChange,
            onCheckScene = onCheckScene,
            onFinishScene = onFinishScene
        )

        Spacer(Modifier.height(16.dp))

        // Acciones Rápidas
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onFateCheck,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = chaosColor)
            ) {
                Icon(Icons.Default.QuestionAnswer, null)
                Spacer(Modifier.width(8.dp))
                Text("Preguntar")
            }

            OutlinedButton(
                onClick = onQuickCheck,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = chaosColor)
            ) {
                Icon(Icons.Default.FlashOn, null)
                Spacer(Modifier.width(4.dp))
                Text("50/50")
            }
        }

        Spacer(Modifier.height(24.dp))

        // Log de historial (Área Central)
        Text(
            "Historial de Crónica", 
            modifier = Modifier.padding(horizontal = 16.dp),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        
        Box(modifier = Modifier.weight(1f)) {
            RollHistoryList(uiState.rolls)
        }
    }
}
