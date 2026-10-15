package com.deckapp.feature.draw.tactical

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deckapp.core.domain.usecase.CalculateTacticalDistanceUseCase
import com.deckapp.core.model.*

@Composable
fun TacticalMapView(
    uiState: TacticalUiState,
    npcs: List<Npc> = emptyList(),
    activeEncounter: Encounter? = null,
    distanceCalculator: CalculateTacticalDistanceUseCase,
    onToolSelected: (TacticalTool) -> Unit,
    onAoeShapeSelected: (TacticalMeasureShape) -> Unit,
    onAoeRadiusSelected: (Int) -> Unit,
    onPersistAoE: () -> Unit,
    onClearPersistentAoEs: () -> Unit,
    onAoEPositionChanged: (origin: Offset, angle: Float) -> Unit,
    onTokenSelected: (Long?) -> Unit,
    onTokenMoved: (tokenId: Long, newCellX: Float, newCellY: Float) -> Unit,
    onTokenHpChanged: (tokenId: Long, newHp: Int) -> Unit,
    onTokenConditionToggled: (tokenId: Long, condition: Condition) -> Unit,
    onTokenSizeChanged: (tokenId: Long, newSize: Float) -> Unit,
    onTokenVisibilityToggled: (tokenId: Long) -> Unit,
    onOpenNpc: ((Long) -> Unit)? = null,
    onDeleteToken: (tokenId: Long) -> Unit,
    onPinClicked: (Long) -> Unit,
    onMapClicked: (cellX: Float, cellY: Float) -> Unit,
    onAddToken: (name: String, faction: TokenFaction, hp: Int, ac: Int, size: Float) -> Unit,
    onAddTokenFromNpc: (Npc) -> Unit,
    onSyncEncounter: (Encounter) -> Unit,
    onUpdateGridCalibration: (cellSize: Float, offsetX: Float, offsetY: Float) -> Unit,
    onUpdateMapImage: (String) -> Unit,
    onStartGuidedCalibration: () -> Unit,
    onGuidedCalibrationPoint: (Offset) -> Unit,
    onCreateDefaultMap: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddTokenDialog by remember { mutableStateOf(false) }
    var showCalibrationDialog by remember { mutableStateOf(false) }
    var showMapSettingsDialog by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { onUpdateMapImage(it.toString()) }
    }

    if (uiState.map == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Map,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                )
                Text(
                    text = "NO HAY MAPA TÁCTICO ACTIVO",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Inicia un mapa táctico interactivo con grilla y tokens para tu sesión",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
                Button(onClick = onCreateDefaultMap) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Crear Mapa de Batalla Rápido")
                }
            }
        }
        return
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Canvas interactivo
        TacticalMapCanvas(
            uiState = uiState,
            distanceCalculator = distanceCalculator,
            onTokenSelected = onTokenSelected,
            onTokenMoved = onTokenMoved,
            onPinClicked = onPinClicked,
            onMapClicked = onMapClicked,
            onAoEPositionChanged = onAoEPositionChanged,
            onGuidedCalibrationPoint = onGuidedCalibrationPoint,
            modifier = Modifier.fillMaxSize()
        )

        // Banner informativo de calibración guiada
        if (uiState.isGuidedCalibrationActive) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Straighten, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                    Text(
                        text = if (uiState.guidedCalibrationFirstPoint == null)
                            "Toca la esquina superior de un bloque 3x3"
                        else
                            "Toca la esquina opuesta del bloque 3x3",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
        }

        // Mini-HUD del token seleccionado
        AnimatedVisibility(
            visible = uiState.selectedToken != null && !uiState.isGuidedCalibrationActive,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            uiState.selectedToken?.let { token ->
                TacticalTokenHud(
                    token = token,
                    onHpChanged = { newHp -> onTokenHpChanged(token.id, newHp) },
                    onConditionToggled = { cond -> onTokenConditionToggled(token.id, cond) },
                    onSizeChanged = { newSize -> onTokenSizeChanged(token.id, newSize) },
                    onVisibilityToggled = { onTokenVisibilityToggled(token.id) },
                    onOpenNpc = onOpenNpc,
                    onDeleteToken = { onDeleteToken(token.id) },
                    onClose = { onTokenSelected(null) }
                )
            }
        }

        // Barra de herramientas flotante inferior
        TacticalToolbar(
            activeTool = uiState.activeTool,
            activeAoeShape = uiState.activeAoeShape,
            aoeRadiusFt = uiState.aoeRadiusFt,
            onToolSelected = onToolSelected,
            onAoeShapeSelected = onAoeShapeSelected,
            onAoeRadiusSelected = onAoeRadiusSelected,
            onPersistAoE = onPersistAoE,
            onAddTokenClicked = { showAddTokenDialog = true },
            onCalibrateGridClicked = { showCalibrationDialog = true },
            onOpenMapSettingsClicked = { showMapSettingsDialog = true },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    // Diálogo de configuración del mapa y fondo
    if (showMapSettingsDialog && uiState.map != null) {
        val currentMap = uiState.map
        var imagePathInput by remember { mutableStateOf(currentMap.imageUri) }

        AlertDialog(
            onDismissRequest = { showMapSettingsDialog = false },
            title = { Text("Configuración del Mapa") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Imagen de fondo del Battlemap:", style = MaterialTheme.typography.labelMedium)
                    OutlinedTextField(
                        value = imagePathInput,
                        onValueChange = { imagePathInput = it },
                        label = { Text("Ruta o URI de imagen") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Button(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Elegir Imagen del Dispositivo")
                    }

                    HorizontalDivider()

                    Text("Calibración Guiada de Grilla:", style = MaterialTheme.typography.labelMedium)
                    OutlinedButton(
                        onClick = {
                            showMapSettingsDialog = false
                            onStartGuidedCalibration()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CropFree, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Calibrar tocando área 3x3")
                    }

                    if (uiState.persistentAoEs.isNotEmpty()) {
                        OutlinedButton(
                            onClick = {
                                onClearPersistentAoEs()
                                showMapSettingsDialog = false
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.LayersClear, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Limpiar Áreas AoE Fijadas (${uiState.persistentAoEs.size})")
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateMapImage(imagePathInput)
                        showMapSettingsDialog = false
                    }
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMapSettingsDialog = false }) {
                    Text("Cerrar")
                }
            }
        )
    }

    // Diálogo de adición de token: Crear nuevo o importar desde PNJ/Encuentro
    if (showAddTokenDialog) {
        var selectedTab by remember { mutableIntStateOf(0) } // 0=Nuevo, 1=Desde PNJ
        var tokenName by remember { mutableStateOf("") }
        var faction by remember { mutableStateOf(TokenFaction.ENEMY) }
        var hp by remember { mutableIntStateOf(10) }
        var ac by remember { mutableIntStateOf(12) }
        var size by remember { mutableFloatStateOf(1f) }
        var npcSearchQuery by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddTokenDialog = false },
            title = { Text("Añadir Token al Mapa") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Botón para sincronizar encuentro si está activo
                    if (activeEncounter != null && activeEncounter.creatures.isNotEmpty()) {
                        FilledTonalButton(
                            onClick = {
                                onSyncEncounter(activeEncounter)
                                showAddTokenDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Importar Combatientes del Encuentro (${activeEncounter.creatures.size})", fontSize = 12.sp)
                        }
                    }

                    TabRow(selectedTabIndex = selectedTab) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Personalizado") }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("Desde PNJ (${npcs.size})") }
                        )
                    }

                    if (selectedTab == 0) {
                        OutlinedTextField(
                            value = tokenName,
                            onValueChange = { tokenName = it },
                            label = { Text("Nombre") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text("Facción:", style = MaterialTheme.typography.labelMedium)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            TokenFaction.entries.forEach { f ->
                                FilterChip(
                                    selected = faction == f,
                                    onClick = { faction = f },
                                    label = {
                                        Text(
                                            when (f) {
                                                TokenFaction.PLAYER -> "PJ"
                                                TokenFaction.ALLY -> "Aliado"
                                                TokenFaction.ENEMY -> "Hostil"
                                                TokenFaction.NEUTRAL -> "Neutro"
                                            }
                                        )
                                    }
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = hp.toString(),
                                onValueChange = { hp = it.toIntOrNull() ?: 10 },
                                label = { Text("HP") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = ac.toString(),
                                onValueChange = { ac = it.toIntOrNull() ?: 10 },
                                label = { Text("CA") },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Text("Tamaño:", style = MaterialTheme.typography.labelMedium)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(1f to "1x1", 2f to "2x2", 3f to "3x3", 4f to "4x4").forEach { (s, label) ->
                                FilterChip(
                                    selected = size == s,
                                    onClick = { size = s },
                                    label = { Text(label) }
                                )
                            }
                        }
                    } else {
                        // Lista de PNJs de la campaña
                        OutlinedTextField(
                            value = npcSearchQuery,
                            onValueChange = { npcSearchQuery = it },
                            label = { Text("Buscar PNJ...") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        val filteredNpcs = npcs.filter {
                            it.name.contains(npcSearchQuery, ignoreCase = true) ||
                            it.description.contains(npcSearchQuery, ignoreCase = true)
                        }

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 240.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(filteredNpcs) { npc ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onAddTokenFromNpc(npc)
                                            showAddTokenDialog = false
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(npc.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                            Text(npc.description, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                        }
                                        Text(
                                            "${npc.maxHp} HP / ${npc.armorClass} CA",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (selectedTab == 0) {
                    Button(
                        onClick = {
                            val finalName = tokenName.ifBlank { "Token ${uiState.tokens.size + 1}" }
                            onAddToken(finalName, faction, hp, ac, size)
                            showAddTokenDialog = false
                        }
                    ) {
                        Text("Colocar")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTokenDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Diálogo de calibración manual de grilla
    if (showCalibrationDialog && uiState.map != null) {
        val currentMap = uiState.map
        var cellSize by remember { mutableFloatStateOf(currentMap.cellSizePx) }
        var offsetX by remember { mutableFloatStateOf(currentMap.gridOffsetX) }
        var offsetY by remember { mutableFloatStateOf(currentMap.gridOffsetY) }

        AlertDialog(
            onDismissRequest = { showCalibrationDialog = false },
            title = { Text("Calibrar Grilla") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Tamaño de Celda: ${cellSize.toInt()} px")
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalButton(onClick = { cellSize = (cellSize - 5f).coerceAtLeast(20f) }) { Text("-5") }
                        FilledTonalButton(onClick = { cellSize = (cellSize - 1f).coerceAtLeast(20f) }) { Text("-1") }
                        FilledTonalButton(onClick = { cellSize += 1f }) { Text("+1") }
                        FilledTonalButton(onClick = { cellSize += 5f }) { Text("+5") }
                    }

                    Text("Desplazamiento X / Y:")
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalButton(onClick = { offsetX -= 2f }) { Text("X -2") }
                        FilledTonalButton(onClick = { offsetX += 2f }) { Text("X +2") }
                        FilledTonalButton(onClick = { offsetY -= 2f }) { Text("Y -2") }
                        FilledTonalButton(onClick = { offsetY += 2f }) { Text("Y +2") }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateGridCalibration(cellSize, offsetX, offsetY)
                        showCalibrationDialog = false
                    }
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCalibrationDialog = false }) {
                    Text("Cerrar")
                }
            }
        )
    }
}
