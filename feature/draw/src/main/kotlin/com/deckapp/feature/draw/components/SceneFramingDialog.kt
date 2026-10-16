package com.deckapp.feature.draw.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.deckapp.core.domain.usecase.mythic.SceneCheckResult
import com.deckapp.core.model.CardStack
import com.deckapp.core.model.Npc
import com.deckapp.core.model.RandomTable
import com.deckapp.core.model.Scene
import com.deckapp.feature.draw.scene.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SceneFramingDialog(
    currentChaos: Int,
    activeScene: Scene?,
    availableNpcs: List<Npc>,
    availableTables: List<RandomTable>,
    availableDecks: List<CardStack>,
    onDismiss: () -> Unit,
    onConfirmFraming: (draft: SceneFramingDraft, closePreviousScene: Boolean, previousSceneSummary: String, chaosDelta: Int) -> Unit
) {
    var step by remember { mutableStateOf(if (activeScene != null && !activeScene.isCompleted) 0 else 1) }

    // Estado del Cierre de Escena Previa (Paso 0)
    var closePrevious by remember { mutableStateOf(activeScene != null && !activeScene.isCompleted) }
    var prevSummary by remember { mutableStateOf("") }
    var chaosDelta by remember { mutableStateOf(0) }

    // Estado del Encuadre (Paso 1)
    var title by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var objective by remember { mutableStateOf("") }
    var expectation by remember { mutableStateOf("") }

    // Estado del Chequeo (Paso 2)
    var chaosFactor by remember { mutableStateOf(currentChaos.coerceIn(1, 9)) }
    var checkOutcome by remember { mutableStateOf<SceneCheckOutcome?>(null) }
    var isRollingCheck by remember { mutableStateOf(false) }

    // Estado de la Situación y Vínculos (Paso 3)
    var adjustedSituation by remember { mutableStateOf("") }
    var selectedNpcId by remember { mutableStateOf<Long?>(null) }
    var selectedTableId by remember { mutableStateOf<Long?>(null) }
    var selectedDeckId by remember { mutableStateOf<Long?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFA161920),
            tonalElevation = 8.dp,
            border = BorderStroke(1.dp, Color(0x33FFFFFF)),
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Cabecera con Título y Cerrar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.HistoryEdu,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = "Encuadre de Escena",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Cerrar", tint = Color.LightGray)
                    }
                }

                // Indicador de Pasos (Stepper)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val stepsList = if (activeScene != null && !activeScene.isCompleted) {
                        listOf(0 to "Cierre", 1 to "Intención", 2 to "Chequeo", 3 to "Arranque")
                    } else {
                        listOf(1 to "Intención", 2 to "Chequeo", 3 to "Arranque")
                    }

                    stepsList.forEachIndexed { index, (sIndex, sLabel) ->
                        val isActive = step == sIndex
                        val isDone = step > sIndex

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isActive -> MaterialTheme.colorScheme.primary
                                            isDone -> Color(0xFF4CAF50)
                                            else -> Color(0x33FFFFFF)
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isDone) {
                                    Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
                                } else {
                                    Text(
                                        text = (index + 1).toString(),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (isActive) Color.White else Color.Gray
                                    )
                                }
                            }
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = sLabel,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = if (isActive) MaterialTheme.colorScheme.primary else Color.Gray
                            )
                            if (index < stepsList.size - 1) {
                                Spacer(Modifier.width(8.dp))
                                Box(modifier = Modifier.width(16.dp).height(1.dp).background(Color(0x33FFFFFF)))
                                Spacer(Modifier.width(8.dp))
                            }
                        }
                    }
                }

                Divider(color = Color(0x22FFFFFF), thickness = 1.dp)

                // Contenido dinámico según el paso
                AnimatedContent(targetState = step, label = "StepTransition") { currentStep ->
                    when (currentStep) {
                        0 -> {
                            // Paso 0: Cierre de Escena Previa
                            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0x33FFA726),
                                    border = BorderStroke(1.dp, Color(0x66FFA726)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(Icons.Default.WarningAmber, null, tint = Color(0xFFFFA726))
                                        Column {
                                            Text(
                                                text = "Escena en curso: \"${activeScene?.title}\"",
                                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                                color = Color.White
                                            )
                                            Text(
                                                text = "¿Deseas cerrar esta escena antes de arrancar la nueva?",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.LightGray
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FilterChip(
                                        selected = closePrevious,
                                        onClick = { closePrevious = true },
                                        label = { Text("Cerrar y resumir") },
                                        leadingIcon = { if (closePrevious) Icon(Icons.Default.Check, null) }
                                    )
                                    FilterChip(
                                        selected = !closePrevious,
                                        onClick = { closePrevious = false },
                                        label = { Text("Mantener abierta") }
                                    )
                                }

                                if (closePrevious) {
                                    OutlinedTextField(
                                        value = prevSummary,
                                        onValueChange = { prevSummary = it },
                                        label = { Text("Resumen de conclusión") },
                                        placeholder = { Text("¿Qué ocurrió al final de la escena?") },
                                        minLines = 2,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    // Balance de Caos (-1, 0, +1)
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = "Balance de Caos resultante",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = Color.LightGray
                                        )
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            FilterChip(
                                                selected = chaosDelta == -1,
                                                onClick = { chaosDelta = -1 },
                                                label = { Text("Control (-1)") }
                                            )
                                            FilterChip(
                                                selected = chaosDelta == 0,
                                                onClick = { chaosDelta = 0 },
                                                label = { Text("Neutro (0)") }
                                            )
                                            FilterChip(
                                                selected = chaosDelta == 1,
                                                onClick = { chaosDelta = 1 },
                                                label = { Text("Descontrol (+1)") }
                                            )
                                        }
                                    }
                                }

                                Button(
                                    onClick = {
                                        chaosFactor = (chaosFactor + chaosDelta).coerceIn(1, 9)
                                        step = 1
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Continuar al Encuadre ➔")
                                }
                            }
                        }

                        1 -> {
                            // Paso 1: Intención & Expectativa
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedTextField(
                                    value = title,
                                    onValueChange = { title = it },
                                    label = { Text("Título de la Escena *") },
                                    placeholder = { Text("ej. Infiltración en el Campamento Orco") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = location,
                                    onValueChange = { location = it },
                                    label = { Text("Lugar / Entorno") },
                                    placeholder = { Text("ej. Claro en el bosque al pie de las colinas") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = objective,
                                    onValueChange = { objective = it },
                                    label = { Text("Objetivo") },
                                    placeholder = { Text("ej. Rescatar al prisionero sin dar la alarma") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = expectation,
                                    onValueChange = { expectation = it },
                                    label = { Text("Expectativa Inicial") },
                                    placeholder = { Text("¿Qué esperás encontrar al comenzar la escena?") },
                                    minLines = 2,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    if (activeScene != null && !activeScene.isCompleted) {
                                        TextButton(onClick = { step = 0 }) {
                                            Text("⬅ Volver")
                                        }
                                    } else {
                                        Spacer(Modifier.width(1.dp))
                                    }

                                    Button(
                                        onClick = {
                                            if (checkOutcome == null) {
                                                checkOutcome = evaluateSceneCheck(chaosFactor)
                                                adjustedSituation = when (checkOutcome?.result) {
                                                    SceneCheckResult.NORMAL -> expectation
                                                    SceneCheckResult.ALTERED -> "$expectation\n\n[Giro de Escena]: ${checkOutcome?.alterationSuggestion}"
                                                    SceneCheckResult.INTERRUPTED -> "¡Escena Interrumpida!\n[Evento Aleatorio]: ${checkOutcome?.eventFocus} · ${checkOutcome?.eventMeaning}"
                                                    null -> expectation
                                                }
                                            }
                                            step = 2
                                        },
                                        enabled = title.isNotBlank()
                                    ) {
                                        Text("Siguiente: Chequeo ➔")
                                    }
                                }
                            }
                        }

                        2 -> {
                            // Paso 2: Chequeo de Escena Mythic (d10 vs Caos)
                            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                // Factor de Caos
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Factor de Caos actual:",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.LightGray
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = {
                                                if (chaosFactor > 1) {
                                                    chaosFactor--
                                                    checkOutcome = evaluateSceneCheck(chaosFactor)
                                                }
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.Remove, null, tint = Color.LightGray)
                                        }
                                        Text(
                                            text = chaosFactor.toString(),
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 8.dp)
                                        )
                                        IconButton(
                                            onClick = {
                                                if (chaosFactor < 9) {
                                                    chaosFactor++
                                                    checkOutcome = evaluateSceneCheck(chaosFactor)
                                                }
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.Add, null, tint = Color.LightGray)
                                        }
                                    }
                                }

                                // Tarjeta de Resultado del Chequeo
                                checkOutcome?.let { outcome ->
                                    val (badgeColor, badgeIcon) = when (outcome.result) {
                                        SceneCheckResult.NORMAL -> Color(0xFF4CAF50) to Icons.Default.CheckCircle
                                        SceneCheckResult.ALTERED -> Color(0xFFFFA726) to Icons.Default.Tune
                                        SceneCheckResult.INTERRUPTED -> Color(0xFFEF5350) to Icons.Default.Bolt
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = badgeColor.copy(alpha = 0.15f),
                                        border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.5f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Icon(badgeIcon, null, tint = badgeColor, modifier = Modifier.size(20.dp))
                                                    Text(
                                                        text = "ESCENA ${outcome.displayResult}",
                                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                                        color = badgeColor
                                                    )
                                                }
                                                Text(
                                                    text = "d10: ${outcome.roll} vs Caos ${outcome.chaosFactor}",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = Color.LightGray
                                                )
                                            }

                                            when (outcome.result) {
                                                SceneCheckResult.NORMAL -> {
                                                    Text(
                                                        text = "La escena arranca tal como fue planeada. Tu objetivo y expectativa se mantienen intactos.",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = Color.LightGray
                                                    )
                                                }
                                                SceneCheckResult.ALTERED -> {
                                                    Text(
                                                        text = "Giro sugerido:",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                        color = Color.White
                                                    )
                                                    Text(
                                                        text = outcome.alterationSuggestion.orEmpty(),
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = Color.White
                                                    )
                                                }
                                                SceneCheckResult.INTERRUPTED -> {
                                                    Text(
                                                        text = "¡La escena esperada NO ocurre! Ha surgido un evento imprevisto:",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = Color.LightGray
                                                    )
                                                    Surface(
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = Color(0x33EF5350),
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Column(modifier = Modifier.padding(8.dp)) {
                                                            Text(
                                                                text = "Foco: ${outcome.eventFocus}",
                                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                                color = Color(0xFFFFCDD2)
                                                            )
                                                            Text(
                                                                text = "Significado: ${outcome.eventMeaning}",
                                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                                color = Color.White
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            // Botón para re-tirar si el DM lo desea
                                            TextButton(
                                                onClick = {
                                                    checkOutcome = evaluateSceneCheck(chaosFactor)
                                                    adjustedSituation = when (checkOutcome?.result) {
                                                        SceneCheckResult.NORMAL -> expectation
                                                        SceneCheckResult.ALTERED -> "$expectation\n\n[Giro de Escena]: ${checkOutcome?.alterationSuggestion}"
                                                        SceneCheckResult.INTERRUPTED -> "¡Escena Interrumpida!\n[Evento Aleatorio]: ${checkOutcome?.eventFocus} · ${checkOutcome?.eventMeaning}"
                                                        null -> expectation
                                                    }
                                                },
                                                modifier = Modifier.align(Alignment.End)
                                            ) {
                                                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text("Tirar de nuevo")
                                            }
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    TextButton(onClick = { step = 1 }) {
                                        Text("⬅ Volver")
                                    }
                                    Button(onClick = { step = 3 }) {
                                        Text("Siguiente: Situación ➔")
                                    }
                                }
                            }
                        }

                        3 -> {
                            // Paso 3: Situación y Despliegue
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedTextField(
                                    value = adjustedSituation,
                                    onValueChange = { adjustedSituation = it },
                                    label = { Text("Situación Inicial Adaptada") },
                                    minLines = 3,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                // Selectores opcionales de recursos
                                if (availableNpcs.isNotEmpty()) {
                                    var expandedNpcs by remember { mutableStateOf(false) }
                                    ExposedDropdownMenuBox(
                                        expanded = expandedNpcs,
                                        onExpandedChange = { expandedNpcs = !expandedNpcs }
                                    ) {
                                        OutlinedTextField(
                                            value = availableNpcs.find { it.id == selectedNpcId }?.name ?: "Ninguno",
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text("Vincular PNJ Clave (Opcional)") },
                                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedNpcs) },
                                            modifier = Modifier.menuAnchor().fillMaxWidth()
                                        )
                                        ExposedDropdownMenu(
                                            expanded = expandedNpcs,
                                            onDismissRequest = { expandedNpcs = false }
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text("Ninguno") },
                                                onClick = { selectedNpcId = null; expandedNpcs = false }
                                            )
                                            availableNpcs.forEach { npc ->
                                                DropdownMenuItem(
                                                    text = { Text(npc.name) },
                                                    onClick = { selectedNpcId = npc.id; expandedNpcs = false }
                                                )
                                            }
                                        }
                                    }
                                }

                                if (availableTables.isNotEmpty()) {
                                    var expandedTables by remember { mutableStateOf(false) }
                                    ExposedDropdownMenuBox(
                                        expanded = expandedTables,
                                        onExpandedChange = { expandedTables = !expandedTables }
                                    ) {
                                        OutlinedTextField(
                                            value = availableTables.find { it.id == selectedTableId }?.name ?: "Ninguna",
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text("Vincular Tabla de Escena (Opcional)") },
                                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTables) },
                                            modifier = Modifier.menuAnchor().fillMaxWidth()
                                        )
                                        ExposedDropdownMenu(
                                            expanded = expandedTables,
                                            onDismissRequest = { expandedTables = false }
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text("Ninguna") },
                                                onClick = { selectedTableId = null; expandedTables = false }
                                            )
                                            availableTables.forEach { table ->
                                                DropdownMenuItem(
                                                    text = { Text(table.name) },
                                                    onClick = { selectedTableId = table.id; expandedTables = false }
                                                )
                                            }
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(onClick = { step = 2 }) {
                                        Text("⬅ Volver")
                                    }

                                    Button(
                                        onClick = {
                                            val draft = SceneFramingDraft(
                                                title = title,
                                                location = location,
                                                objective = objective,
                                                expectation = expectation,
                                                adjustedSituation = adjustedSituation,
                                                checkOutcome = checkOutcome,
                                                linkedNpcId = selectedNpcId,
                                                linkedTableId = selectedTableId,
                                                linkedDeckId = selectedDeckId
                                            )
                                            onConfirmFraming(draft, closePrevious, prevSummary, chaosDelta)
                                            onDismiss()
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary
                                        )
                                    ) {
                                        Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("🎬 Comenzar Escena")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
