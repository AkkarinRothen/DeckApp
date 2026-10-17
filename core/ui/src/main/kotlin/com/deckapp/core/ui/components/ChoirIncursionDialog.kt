@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package com.deckapp.core.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.deckapp.core.domain.usecase.ChoirIncursionRules
import com.deckapp.core.model.*
import kotlin.random.Random

private val CrimsonGothic = Color(0xFF2B0A0A)
private val GoldAccent = Color(0xFFD4AF37)
private val DarkCard = Color(0xFF1E0E0E)

@Composable
fun ChoirIncursionDialog(
    initialIncursion: ChoirIncursion,
    initialSettlement: ChoirSettlement,
    activeCharacter: ChoirCharacter? = null,
    onSave: (ChoirIncursion, ChoirSettlement, ChoirCharacter?) -> Unit,
    onDismiss: () -> Unit
) {
    var incursion by remember { mutableStateOf(initialIncursion) }
    var settlement by remember { mutableStateOf(initialSettlement) }
    var character by remember { mutableStateOf(activeCharacter) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Incursión, 1: Asentamiento

    var showLootDialog by remember { mutableStateOf(false) }
    var lootInput by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(16.dp),
            color = CrimsonGothic,
            border = BorderStroke(1.5.dp, GoldAccent.copy(alpha = 0.7f)),
            tonalElevation = 12.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // --- Top Header ---
                Surface(
                    color = Color.Black.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (selectedTab == 0) Icons.Default.Explore else Icons.Default.HomeWork,
                                contentDescription = null,
                                tint = GoldAccent,
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (selectedTab == 0) incursion.title else "Asentamiento: ${settlement.name}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Choir of Flesh · Asistente de Campaña",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = GoldAccent.copy(alpha = 0.8f)
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
                            }
                        }
                    }
                }

                // --- Tab Navigation ---
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Black.copy(alpha = 0.3f),
                    contentColor = GoldAccent,
                    divider = {}
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Route, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Incursión (${incursion.currentNodeIndex}/8)", fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Asentamiento & Refugio", fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                }

                // Status snack / message banner
                AnimatedVisibility(visible = statusMessage != null) {
                    Surface(
                        color = GoldAccent.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, GoldAccent),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(statusMessage ?: "", style = MaterialTheme.typography.bodySmall, color = Color.White)
                            IconButton(onClick = { statusMessage = null }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                // --- Content Body ---
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    if (selectedTab == 0) {
                        IncursionTabContent(
                            incursion = incursion,
                            onAdvance = { destination ->
                                incursion = ChoirIncursionRules.advance(incursion, destination)
                                statusMessage = "Movimiento registrado: ${incursion.currentNode?.title}"
                            },
                            onClearNode = {
                                showLootDialog = true
                            },
                            onFleeNode = {
                                incursion = ChoirIncursionRules.fleeCurrentNode(incursion)
                                statusMessage = "Huida registrada."
                            },
                            onExtract = {
                                val (inc, set) = ChoirIncursionRules.extractToSettlement(incursion, settlement)
                                incursion = inc
                                settlement = set
                                statusMessage = "Extracción registrada en el asentamiento."
                            },
                            onResetIncursion = {
                                incursion = ChoirIncursion(title = "Nueva Incursión al Páramo")
                                statusMessage = "Nueva incursión iniciada en el Campamento Base."
                            }
                        )
                    } else {
                        SettlementTabContent(
                            settlement = settlement,
                            character = character,
                            onRestShort = {
                                if (character != null) {
                                    val (upChar, upSet) = ChoirIncursionRules.restShort(character!!, settlement)
                                    character = upChar
                                    if (upSet != null) settlement = upSet
                                    statusMessage = "Descanso de 6 horas registrado: Vigor restaurado (${upChar.vigor})."
                                }
                            },
                            onRollRumor = {
                                val rumor = "Rumor del Escatón (d10 #${Random.nextInt(1, 11)}): Presagios de carne palpitante bajo el santuario."
                                settlement = settlement.copy(activeRumors = settlement.activeRumors + rumor)
                                statusMessage = "Nuevo rumor registrado."
                            },
                            onAddFacility = { facilityName ->
                                if (facilityName.isNotBlank() && !settlement.facilities.contains(facilityName)) {
                                    settlement = settlement.copy(facilities = settlement.facilities + facilityName.trim())
                                }
                            }
                        )
                    }
                }

                // --- Bottom Action Bar ---
                Surface(
                    color = Color.Black.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("CERRAR", color = Color.White.copy(alpha = 0.7f))
                        }
                        Spacer(Modifier.width(10.dp))
                        Button(
                            onClick = {
                                onSave(incursion, settlement, character)
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GoldAccent,
                                contentColor = Color.Black
                            )
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("GUARDAR ESTADO", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showLootDialog) {
        AlertDialog(
            onDismissRequest = { showLootDialog = false },
            title = { Text("Despejar Nodo ${incursion.currentNodeIndex}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Registra el botín, reliquias o recursos encontrados en este nodo:")
                    OutlinedTextField(
                        value = lootInput,
                        onValueChange = { lootInput = it },
                        label = { Text("Botín encontrado") },
                        placeholder = { Text("Ej: 2 Raciones, Daga de hueso, Fragmento") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    incursion = ChoirIncursionRules.clearCurrentNode(incursion, lootInput)
                    lootInput = ""
                    showLootDialog = false
                    statusMessage = "Nodo despejado exitosamente."
                }) {
                    Text("DESPEJAR NODO")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLootDialog = false }) {
                    Text("CANCELAR")
                }
            }
        )
    }
}

@Composable
private fun IncursionTabContent(
    incursion: ChoirIncursion,
    onAdvance: (Int) -> Unit,
    onClearNode: () -> Unit,
    onFleeNode: () -> Unit,
    onExtract: () -> Unit,
    onResetIncursion: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // --- Song Level / Tension Bar ---
        Surface(
            color = DarkCard,
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, Color(0xFF5A1414)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.GraphicEq, contentDescription = null, tint = Color(0xFFEF5350), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("La Canción de la Carne", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Text(
                        "${incursion.songLevel}/10 Tensión",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (incursion.songLevel >= 7) Color(0xFFEF5350) else GoldAccent
                    )
                }

                LinearProgressIndicator(
                    progress = { (incursion.songLevel / 10f).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = if (incursion.songLevel >= 7) Color(0xFFD32F2F) else Color(0xFFFFA000),
                    trackColor = Color.Black.copy(alpha = 0.5f)
                )

                Text(
                    text = "La grilla indica las conexiones; resuelve los eventos y efectos con el manual o tus tablas de sesión.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }

        // --- Current Node Hero Card ---
        val currentNode = incursion.currentNode
        if (currentNode != null) {
            Surface(
                color = DarkCard,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.5.dp, GoldAccent),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = GoldAccent,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "${currentNode.index}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                }
                            }
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(currentNode.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("Tipo: ${currentNode.nodeType.name} · Estado: ${currentNode.state.name}", style = MaterialTheme.typography.labelSmall, color = GoldAccent)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = when (currentNode.state) {
                                ChoirNodeState.CURRENT -> Color(0xFF1976D2)
                                ChoirNodeState.CLEARED -> Color(0xFF388E3C)
                                ChoirNodeState.FLED -> Color(0xFFD32F2F)
                                ChoirNodeState.UNEXPLORED -> Color.Gray
                            }
                        ) {
                            Text(
                                text = currentNode.state.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Text(currentNode.description, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.9f))

                    if (currentNode.lootFound.isNotBlank()) {
                        Text("Botín del nodo: ${currentNode.lootFound}", style = MaterialTheme.typography.bodySmall, color = GoldAccent)
                    }

                    // Botones de acción del nodo
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (currentNode.state == ChoirNodeState.CURRENT && !incursion.isFinished) {
                            Button(
                                onClick = onClearNode,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Despejar")
                            }

                            OutlinedButton(
                                onClick = onFleeNode,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF5350))
                            ) {
                                Icon(Icons.Default.DirectionsRun, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Huir")
                            }
                        }

                        if (incursion.canExtract && !incursion.isFinished) {
                            Button(
                                onClick = onExtract,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4527A0))
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Extraerse")
                            }
                        }
                    }
                    if ((currentNode.state == ChoirNodeState.CLEARED || currentNode.state == ChoirNodeState.FLED) && !incursion.isFinished) {
                        Text("Pasajes conectados", color = GoldAccent, style = MaterialTheme.typography.labelLarge)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ChoirIncursionRules.destinations(incursion).forEach { destination ->
                                OutlinedButton(onClick = { onAdvance(destination) }, modifier = Modifier.heightIn(min = 48.dp)) {
                                    Text(if (destination == 0) "Entrada" else "Nodo $destination")
                                }
                            }
                        }
                    }

                    if (incursion.isFinished) {
                        Surface(
                            color = Color(0xFF1B5E20).copy(alpha = 0.3f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Incursión Finalizada (${incursion.status.name})", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                                TextButton(onClick = onResetIncursion) {
                                    Text("NUEVA INCURSIÓN", color = GoldAccent, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- Track de Nodos (0 al 8) ---
        Text("Trayecto de la Incursión (8 Nodos)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = GoldAccent)

        incursion.nodes.forEach { node ->
            val isCurrent = node.index == incursion.currentNodeIndex
            Surface(
                color = if (isCurrent) DarkCard.copy(alpha = 0.9f) else DarkCard.copy(alpha = 0.4f),
                shape = RoundedCornerShape(8.dp),
                border = if (isCurrent) BorderStroke(1.5.dp, GoldAccent) else BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = when (node.state) {
                            ChoirNodeState.CURRENT -> GoldAccent
                            ChoirNodeState.CLEARED -> Color(0xFF388E3C)
                            ChoirNodeState.FLED -> Color(0xFFD32F2F)
                            ChoirNodeState.UNEXPLORED -> Color.DarkGray
                        },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "${node.index}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (node.state == ChoirNodeState.CURRENT) Color.Black else Color.White
                            )
                        }
                    }

                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = node.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                            color = Color.White
                        )
                        Text(
                            text = "${node.nodeType.name} · ${node.description}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (node.lootFound.isNotBlank()) {
                        Icon(Icons.Default.Diamond, contentDescription = "Botín", tint = GoldAccent, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SettlementTabContent(
    settlement: ChoirSettlement,
    character: ChoirCharacter?,
    onRestShort: () -> Unit,
    onRollRumor: () -> Unit,
    onAddFacility: (String) -> Unit
) {
    var newFacilityText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // --- Settlement Vital Stats ---
        Surface(
            color = DarkCard,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Recursos del Asentamiento", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = GoldAccent)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    StatBadge("Moral", "${settlement.morale}/10", Color(0xFF64B5F6))
                    StatBadge("Defensas", "${settlement.defense}/10", Color(0xFF81C784))
                    StatBadge("Raciones", "${settlement.stockRations}", GoldAccent)
                    StatBadge("Suministros", "${settlement.stockMedicalSupplies}", Color(0xFFE57373))
                    StatBadge("Población", "${settlement.population}", Color.White)
                }
            }
        }

        // --- Descanso y Recuperación ---
        Text("Descanso y recuperación", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = GoldAccent)

        Surface(
            color = DarkCard,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (character != null) {
                    Text(
                        "Inquebrantable vinculado: Vigor ${character.vigor}/${com.deckapp.core.domain.usecase.ChoirCharacterRules.vigorMaximum(character)} · Angustia ${character.anguish} · Trauma ${character.trauma}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onRestShort,
                        modifier = Modifier.weight(1f),
                        enabled = character != null,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Descanso ≥6 horas", fontWeight = FontWeight.Bold)
                            Text("Recupera todo el Vigor", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                Text("Para reducir Trauma, usa el procedimiento de cuidado o descanso semanal del manual y registra el resultado en la ficha.", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall)
            }
        }

        // --- Instalaciones del Bastión ---
        Text("Instalaciones y Mejoras", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = GoldAccent)

        Surface(
            color = DarkCard,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    settlement.facilities.forEach { facility ->
                        SuggestionChip(
                            onClick = {},
                            label = { Text(facility, color = Color.White) },
                            colors = SuggestionChipDefaults.suggestionChipColors(containerColor = Color.Black.copy(alpha = 0.4f))
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newFacilityText,
                        onValueChange = { newFacilityText = it },
                        label = { Text("Nueva Instalación") },
                        placeholder = { Text("Ej: Forja de Huesos, Huerto de Hongos") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = {
                        onAddFacility(newFacilityText)
                        newFacilityText = ""
                    }) {
                        Icon(Icons.Default.AddCircle, contentDescription = "Añadir", tint = GoldAccent)
                    }
                }
            }
        }

        // --- Rumores del Páramo ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Rumores Escuchados", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = GoldAccent)
            TextButton(onClick = onRollRumor) {
                Icon(Icons.Default.Casino, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("TIRAR RUMOR d10", color = GoldAccent)
            }
        }

        if (settlement.activeRumors.isEmpty()) {
            Text("No hay rumores activos registrados.", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.5f))
        } else {
            settlement.activeRumors.forEach { rumor ->
                Surface(
                    color = DarkCard.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "• $rumor",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatBadge(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
    }
}
