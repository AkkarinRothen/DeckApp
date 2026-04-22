package com.deckapp.feature.mythic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MythicReferenceScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Guía Mythic GME 2e") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                SectionHeader("Interpretación del Oráculo")
                FateInterpretationCard()
            }

            item {
                SectionHeader("Eventos Aleatorios (Dobles)")
                Text(
                    "Si sacas dobles (11, 22... 88) y el valor es MENOR o IGUAL al Factor de Caos actual, ocurre un Evento Aleatorio.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            item {
                SectionHeader("El Factor de Caos")
                ChaosFactorGuide()
            }

            item {
                SectionHeader("Gestión de Escenas")
                SceneGuide()
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun FateInterpretationCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ResultRow("Sí Excepcional", "El resultado es positivo y ocurre algo extra a tu favor.", Color(0xFF2E7D32))
            ResultRow("Sí", "La respuesta es afirmativa.", Color(0xFF4CAF50))
            ResultRow("No", "La respuesta es negativa.", Color(0xFFF44336))
            ResultRow("No Excepcional", "El resultado es negativo y ocurre algo extra en tu contra.", Color(0xFFB71C1C))
        }
    }
}

@Composable
private fun ResultRow(label: String, desc: String, color: Color) {
    Row(verticalAlignment = Alignment.Top) {
        Box(Modifier.size(12.dp).padding(top = 4.dp).background(color, MaterialTheme.shapes.extraSmall))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, fontWeight = FontWeight.Bold, color = color, fontSize = 14.sp)
            Text(desc, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ChaosFactorGuide() {
    Card {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("• Por defecto empieza en 5.", style = MaterialTheme.typography.bodySmall)
            Text("• Si la escena fue CONTROLADA y tranquila: Baja el Factor de Caos (-1).", style = MaterialTheme.typography.bodySmall)
            Text("• Si la escena fue CAÓTICA o fuera de control: Sube el Factor de Caos (+1).", style = MaterialTheme.typography.bodySmall)
            Text("• A mayor Caos, más probable es que el Oráculo diga 'SÍ' y ocurran eventos.", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun SceneGuide() {
    Card {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Al empezar una nueva escena, tira 1d10 contra el Factor de Caos:", style = MaterialTheme.typography.bodySmall)
            Text("• Resultado > Caos: Escena NORMAL.", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            Text("• Resultado <= Caos y PAR: Escena ALTERADA.", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            Text("• Resultado <= Caos e IMPAR: Escena INTERRUMPIDA.", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
        }
    }
}
