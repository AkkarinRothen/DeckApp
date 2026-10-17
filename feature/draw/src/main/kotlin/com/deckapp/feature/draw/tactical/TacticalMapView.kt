package com.deckapp.feature.draw.tactical

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
    onTokenElevationChanged: (tokenId: Long, elevationFt: Int) -> Unit = { _, _ -> },
    onTokenVisibilityToggled: (tokenId: Long) -> Unit,
    onApplyDamageToToken: (tokenId: Long, amount: Int) -> Unit = { _, _ -> },
    onApplyHealingToToken: (tokenId: Long, amount: Int) -> Unit = { _, _ -> },
    onApplyTempHpToToken: (tokenId: Long, amount: Int) -> Unit = { _, _ -> },
    onOpenDamageCalculator: (tokenId: Long) -> Unit = { _ -> },
    onCloseDamageCalculator: () -> Unit = {},
    onPruneFloatingTexts: () -> Unit = {},
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
    onOpenMapSelector: () -> Unit = {},
    onSwitchMap: (Long) -> Unit = { _ -> },
    onOpenCreateMapDialog: () -> Unit = {},
    onOpenRenameMap: (TacticalMap) -> Unit = { _ -> },
    onDeleteMap: (Long) -> Unit = { _ -> },
    onTerrainBrushSelected: (TacticalTerrainType) -> Unit = { _ -> },
    onTerrainCellInteracted: (col: Int, row: Int) -> Unit = { _, _ -> },
    onClearAllTerrain: () -> Unit = {},
    onToggleFog: () -> Unit = {},
    onFogBrushModeSelected: (FogBrushMode) -> Unit = { _ -> },
    onRevealAllFog: () -> Unit = {},
    onResetFog: () -> Unit = {},
    onToggleFogDmView: () -> Unit = {},
    onFogCellInteracted: (col: Int, row: Int) -> Unit = { _, _ -> },
    onAddRouteWaypoint: (cellX: Float, cellY: Float) -> Unit = { _, _ -> },
    onRemoveLastRouteWaypoint: () -> Unit = {},
    onCommitRouteMovement: () -> Unit = {},
    onClearRoute: () -> Unit = {},
    onResetTokenTurnMovement: (tokenId: Long) -> Unit = { _ -> },
    onStartAttackTargeting: (tokenId: Long) -> Unit = { _ -> },
    onCancelTargeting: () -> Unit = {},
    onExecuteAttack: (attackerId: Long, targetId: Long, attackName: String, attackBonus: Int, damageFormula: String, advantageMode: Int) -> Unit = { _, _, _, _, _, _ -> },
    onApplyAttackDamage: (TacticalAttackResolution) -> Unit = { _ -> },
    onDismissAttackResolution: () -> Unit = {},
    onDismissCustomAttackDialog: () -> Unit = {},
    onAdvanceCombatTurn: () -> Unit = {},
    onPreviousCombatTurn: () -> Unit = {},
    onFocusOnCombatCreature: (Long) -> Unit = {},
    onClearCameraFocus: () -> Unit = {},
    onNavigateBackToBreadcrumb: (Long) -> Unit = {},
    onNavigateToSubMap: (Long) -> Unit = {},
    onSpawnEncounter: (encounterId: Long, cellX: Float, cellY: Float) -> Unit = { _, _, _ -> },
    onRollPinTable: (Long) -> Unit = {},
    onTogglePinSecret: (Long) -> Unit = {},
    onEditPin: (TacticalMapPin) -> Unit = {},
    onRollInlineFormula: (formula: String, label: String) -> Unit = { _, _ -> },
    onDismissPin: () -> Unit = {},
    onPinMoved: (pinId: Long, newCellX: Float, newCellY: Float) -> Unit = { _, _, _ -> },
    onCreatePinFromPreset: (cellX: Float, cellY: Float) -> Unit = { _, _ -> },
    onPinPresetSelected: (TacticalPinPreset) -> Unit = {},
    onResetRoomCounter: () -> Unit = {},
    onToggleShowSecretPins: () -> Unit = {},
    onToggleStickyPin: (Long) -> Unit = {},
    onToggleStickyPinMinimized: () -> Unit = {},
    onTogglePinLocked: (Long) -> Unit = {},
    onDeletePin: (Long) -> Unit = {},
    onFocusPinCoordinates: (cellX: Float, cellY: Float) -> Unit = { _, _ -> },
    dmNotes: String = "",
    onNotesChanged: (String) -> Unit = {},
    onToggleSidePanel: () -> Unit = {},
    onSetSidePanelOpen: (Boolean) -> Unit = {},
    onSetSidePanelTab: (TacticalSidePanelTab) -> Unit = {},
    onSetSidePanelDice: (count: Int, sides: Int, modifier: Int) -> Unit = { _, _, _ -> },
    onRollSidePanelDice: () -> Unit = {},
    onRollSidePanelFateCheck: (ProbabilityLevel, Int) -> Unit = { _, _ -> },
    onRollSidePanelRandomEvent: () -> Unit = {},
    onWikiSearchQueryChanged: (String) -> Unit = {},
    isToolbarMinimized: Boolean = false,
    onToggleToolbarMinimized: () -> Unit = {},
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
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = onCreateDefaultMap) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Crear Mapa Rápido")
                    }
                    if (uiState.availableMaps.isNotEmpty()) {
                        FilledTonalButton(onClick = onOpenMapSelector) {
                            Icon(Icons.Default.Layers, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Elegir Mapa (${uiState.availableMaps.size})")
                        }
                    } else {
                        OutlinedButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Subir Imagen")
                        }
                    }
                }
            }
        }
        return
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isLandscapeOrTablet = maxWidth >= 720.dp

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(end = if (isLandscapeOrTablet && uiState.isSidePanelOpen) 340.dp else 0.dp)
        ) {
        // Canvas interactivo
        TacticalMapCanvas(
            uiState = uiState,
            distanceCalculator = distanceCalculator,
            onTokenSelected = onTokenSelected,
            onTokenMoved = onTokenMoved,
            onPinClicked = onPinClicked,
            onMapClicked = onMapClicked,
            onPinMoved = onPinMoved,
            onCreatePinFromPreset = onCreatePinFromPreset,
            onAoEPositionChanged = onAoEPositionChanged,
            onGuidedCalibrationPoint = onGuidedCalibrationPoint,
            onTerrainCellInteracted = onTerrainCellInteracted,
            onFogCellInteracted = onFogCellInteracted,
            onAddRouteWaypoint = onAddRouteWaypoint,
            onClearCameraFocus = onClearCameraFocus,
            onPruneFloatingTexts = onPruneFloatingTexts,
            modifier = Modifier.fillMaxSize()
        )

        // Cabecera superior del mapa táctico con selector multimapa y presupuesto de movimiento
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
            tonalElevation = 4.dp,
            shadowElevation = 6.dp,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 12.dp, top = 8.dp)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { /* Consumir taps para evitar propagación al canvas subyacente */ }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Chip clickeable para abrir el selector visual de mapas
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    modifier = Modifier.clickable(onClick = onOpenMapSelector)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = "Mapas",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = uiState.map.name.ifBlank { "Mapa de Batalla" },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 130.dp)
                        )
                        Icon(
                            imageVector = Icons.Default.ExpandMore,
                            contentDescription = "Ver todos los mapas",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Acceso directo a calibrar grilla
                IconButton(
                    onClick = { showCalibrationDialog = true },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.GridOn,
                        contentDescription = "Calibrar Grilla",
                        modifier = Modifier.size(15.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Presupuesto de movimiento del token seleccionado
                uiState.selectedToken?.let { tok ->
                    val movedFt = uiState.tokenMovedThisTurnFt[tok.id] ?: 0
                    val speed = tok.speedFt
                    val isExceeded = movedFt > speed
                    VerticalDivider(modifier = Modifier.height(18.dp).padding(horizontal = 2.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isExceeded) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = "Mov: $movedFt / ${speed}ft",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isExceeded) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            if (movedFt > 0) {
                                IconButton(
                                    onClick = { onResetTokenTurnMovement(tok.id) },
                                    modifier = Modifier.size(14.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Reiniciar movimiento de turno",
                                        modifier = Modifier.size(11.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Pila unificada de controles superiores (Apilado vertical limpio para evitar superposiciones)
        val topControlsPadding = if (isLandscapeOrTablet) 8.dp else 52.dp
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = topControlsPadding, start = 8.dp, end = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // 1. Banner informativo de calibración guiada
            if (uiState.isGuidedCalibrationActive) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    tonalElevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Straighten, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.size(16.dp))
                        Text(
                            text = if (uiState.guidedCalibrationFirstPoint == null)
                                "Toca la esquina superior de un bloque 3x3"
                            else
                                "Toca la esquina opuesta del bloque 3x3",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }

            // 2. Indicador de modo Selección de Objetivo (Puntería)
            if (uiState.targetingAttackerTokenId != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AdsClick,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Toca un objetivo en el mapa para ${uiState.targetingAttackerToken?.name ?: "atacar"}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(Modifier.width(4.dp))
                        TextButton(
                            onClick = onCancelTargeting,
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.height(26.dp)
                        ) {
                            Text("Cancelar", fontSize = 11.sp, color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
            }

            // 3. Barra de Navegación de Mapas Anidados (Breadcrumbs estilo Atlas VTT)
            if (uiState.mapBreadcrumbs.isNotEmpty()) {
                TacticalBreadcrumbBar(
                    breadcrumbs = uiState.mapBreadcrumbs,
                    currentMap = uiState.map,
                    onNavigateBack = {
                        val lastCrumb = uiState.mapBreadcrumbs.lastOrNull()
                        if (lastCrumb != null) onNavigateBackToBreadcrumb(lastCrumb.first)
                    },
                    onNavigateToCrumb = onNavigateBackToBreadcrumb
                )
            }

            // 4. Barra de Iniciativa y Turnos de Combate con Auto-Foco
            activeEncounter?.let { encounter ->
                TacticalInitiativeBar(
                    encounter = encounter,
                    onNextTurn = onAdvanceCombatTurn,
                    onPreviousTurn = onPreviousCombatTurn,
                    onSelectCreatureToken = { creatureId ->
                        onFocusOnCombatCreature(creatureId)
                    }
                )
            }

            // 5. Mini-HUD del token seleccionado (situado directamente debajo de la iniciativa sin taparla)
            AnimatedVisibility(
                visible = uiState.selectedToken != null && !uiState.isGuidedCalibrationActive,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut()
            ) {
                uiState.selectedToken?.let { token ->
                    TacticalTokenHud(
                        token = token,
                        onHpChanged = { newHp -> onTokenHpChanged(token.id, newHp) },
                        onApplyDamage = { dmg -> onApplyDamageToToken(token.id, dmg) },
                        onApplyHealing = { heal -> onApplyHealingToToken(token.id, heal) },
                        onOpenDamageCalculator = { onOpenDamageCalculator(token.id) },
                        onConditionToggled = { cond -> onTokenConditionToggled(token.id, cond) },
                        onSizeChanged = { newSize -> onTokenSizeChanged(token.id, newSize) },
                        onElevationChanged = { elev -> onTokenElevationChanged(token.id, elev) },
                        onVisibilityToggled = { onTokenVisibilityToggled(token.id) },
                        onStartAttackTargeting = { onStartAttackTargeting(token.id) },
                        onOpenNpc = onOpenNpc,
                        onDeleteToken = { onDeleteToken(token.id) },
                        onClose = { onTokenSelected(null) }
                    )
                }
            }
        }

        // Mini-panel flotante in-situ de Pin (Atlas VTT) - Se oculta si el drawer lateral de móvil está abierto
        val isMobileSidePanelOpen = !isLandscapeOrTablet && uiState.isSidePanelOpen
        val activeFloatingPin = if (!isMobileSidePanelOpen) {
            uiState.stickyPin ?: if (uiState.showPinDetailDialog) uiState.selectedPin else null
        } else null
        if (activeFloatingPin != null) {
            val panelTopPad = if (uiState.mapBreadcrumbs.isNotEmpty() || activeEncounter != null) 72.dp else 16.dp
            val isSticky = uiState.stickyPinId == activeFloatingPin.id
            TacticalFloatingPinPanel(
                pin = activeFloatingPin,
                availableMaps = uiState.availableMaps,
                availableTables = uiState.availableTables,
                availableEncounters = uiState.availableEncounters,
                availableWikiEntries = uiState.availableWikiEntries,
                onDismiss = {
                    if (isSticky) onToggleStickyPin(activeFloatingPin.id)
                    onDismissPin()
                },
                onEnterSubMap = onNavigateToSubMap,
                onSpawnEncounter = onSpawnEncounter,
                onRollPinTable = onRollPinTable,
                onToggleSecret = onTogglePinSecret,
                onEditPin = onEditPin,
                onRollInlineFormula = onRollInlineFormula,
                isSticky = isSticky,
                isMinimized = isSticky && uiState.isStickyPinMinimized,
                isLocked = uiState.lockedPinIds.contains(activeFloatingPin.id),
                onToggleSticky = { onToggleStickyPin(activeFloatingPin.id) },
                onToggleMinimized = onToggleStickyPinMinimized,
                onToggleLocked = { onTogglePinLocked(activeFloatingPin.id) },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 12.dp, top = panelTopPad)
            )
        }

        // Banner flotante de resolución de ataque
        uiState.attackResolution?.let { resolution ->
            TacticalAttackResolutionBanner(
                resolution = resolution,
                onApplyDamage = onApplyAttackDamage,
                onDismiss = onDismissAttackResolution,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 75.dp)
            )
        }

        // Diálogo de confirmación y lanzamiento de ataque
        if (uiState.showCustomAttackDialog && uiState.targetingAttackerToken != null && uiState.pendingAttackTargetToken != null) {
            val attacker = uiState.targetingAttackerToken!!
            val target = uiState.pendingAttackTargetToken!!
            val distResult = distanceCalculator(
                fromX = attacker.cellX,
                fromY = attacker.cellY,
                toX = target.cellX,
                toY = target.cellY,
                scaleFtPerCell = uiState.map?.scaleFtPerCell ?: 5,
                gridType = uiState.map?.gridType ?: com.deckapp.core.model.GridType.SQUARE,
                terrainMap = uiState.terrainMap,
                fromElevationFt = attacker.elevationFt,
                toElevationFt = target.elevationFt
            )
            val dist = distResult.distance3dFt

            TacticalAttackDialog(
                attacker = attacker,
                target = target,
                distanceFt = dist,
                onDismiss = onDismissCustomAttackDialog,
                onExecuteAttack = { name, bonus, formula, advMode ->
                    onExecuteAttack(attacker.id, target.id, name, bonus, formula, advMode)
                }
            )
        }

        // Barra de herramientas flotante inferior
        TacticalToolbar(
            activeTool = uiState.activeTool,
            activeAoeShape = uiState.activeAoeShape,
            aoeRadiusFt = uiState.aoeRadiusFt,
            isFogEnabled = uiState.isFogEnabled,
            fogBrushMode = uiState.fogBrushMode,
            isFogDmView = uiState.isFogDmView,
            activeTerrainBrush = uiState.activeTerrainBrush,
            routeWaypointsCount = uiState.routeWaypoints.size,
            currentRouteDistanceFt = uiState.currentRouteDistanceFt,
            selectedTokenSpeedFt = uiState.selectedToken?.speedFt,
            activePinPreset = uiState.activePinPreset,
            nextRoomNumber = uiState.nextRoomNumber,
            showSecretPins = uiState.showSecretPins,
            onPinPresetSelected = onPinPresetSelected,
            onResetRoomNumber = onResetRoomCounter,
            onToggleShowSecretPins = onToggleShowSecretPins,
            onOpenDungeonKey = {
                onSetSidePanelTab(TacticalSidePanelTab.PINS)
                onSetSidePanelOpen(true)
            },
            onToolSelected = onToolSelected,
            onAoeShapeSelected = onAoeShapeSelected,
            onAoeRadiusSelected = onAoeRadiusSelected,
            onPersistAoE = onPersistAoE,
            onToggleFog = onToggleFog,
            onFogBrushModeSelected = onFogBrushModeSelected,
            onRevealAllFog = onRevealAllFog,
            onResetFog = onResetFog,
            onToggleFogDmView = onToggleFogDmView,
            onTerrainBrushSelected = onTerrainBrushSelected,
            onClearAllTerrain = onClearAllTerrain,
            onRemoveLastRouteWaypoint = onRemoveLastRouteWaypoint,
            onCommitRouteMovement = onCommitRouteMovement,
            onClearRoute = onClearRoute,
            onAddTokenClicked = { showAddTokenDialog = true },
            onCalibrateGridClicked = { showCalibrationDialog = true },
            onOpenMapSettingsClicked = { showMapSettingsDialog = true },
            isSidePanelOpen = uiState.isSidePanelOpen,
            onToggleSidePanel = onToggleSidePanel,
            isMinimized = isToolbarMinimized,
            onToggleMinimize = onToggleToolbarMinimized,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    // Solapa flotante activadora del panel lateral cuando está cerrado (solo en pantallas anchas / landscape)
    if (!uiState.isSidePanelOpen && isLandscapeOrTablet) {
        Surface(
            shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f),
            tonalElevation = 6.dp,
            shadowElevation = 8.dp,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .clickable { onSetSidePanelOpen(true) }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "Abrir panel lateral",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }

    // Panel Lateral en modo Pantalla Dividida (Landscape / Tablet / Desktop)
    if (isLandscapeOrTablet) {
        AnimatedVisibility(
            visible = uiState.isSidePanelOpen,
            enter = expandHorizontally() + fadeIn(),
            exit = shrinkHorizontally() + fadeOut(),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            TacticalSidePanel(
                uiState = uiState,
                dmNotes = dmNotes,
                onNotesChanged = onNotesChanged,
                onTabSelected = onSetSidePanelTab,
                onSetDice = onSetSidePanelDice,
                onRollDice = onRollSidePanelDice,
                onRollFateCheck = onRollSidePanelFateCheck,
                onRollRandomEvent = onRollSidePanelRandomEvent,
                onWikiSearchQueryChanged = onWikiSearchQueryChanged,
                onAdvanceCombatTurn = onAdvanceCombatTurn,
                onSelectToken = onTokenSelected,
                onClose = { onSetSidePanelOpen(false) },
                onFocusPinCoordinates = onFocusPinCoordinates,
                onPinClicked = onPinClicked,
                onToggleStickyPin = onToggleStickyPin,
                onTogglePinLocked = onTogglePinLocked,
                onEditPin = onEditPin,
                onDeletePin = onDeletePin,
                onToggleShowSecretPins = onToggleShowSecretPins,
                modifier = Modifier.width(340.dp).fillMaxHeight()
            )
        }
    }

        // Panel Lateral en modo Móvil Vertical (Overlay Drawer)
        if (!isLandscapeOrTablet && uiState.isSidePanelOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable { onSetSidePanelOpen(false) }
            )

            AnimatedVisibility(
                visible = uiState.isSidePanelOpen,
                enter = slideInHorizontally { it } + fadeIn(),
                exit = slideOutHorizontally { it } + fadeOut(),
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                TacticalSidePanel(
                    uiState = uiState,
                    dmNotes = dmNotes,
                    onNotesChanged = onNotesChanged,
                    onTabSelected = onSetSidePanelTab,
                    onSetDice = onSetSidePanelDice,
                    onRollDice = onRollSidePanelDice,
                    onRollFateCheck = onRollSidePanelFateCheck,
                    onRollRandomEvent = onRollSidePanelRandomEvent,
                    onWikiSearchQueryChanged = onWikiSearchQueryChanged,
                    onAdvanceCombatTurn = onAdvanceCombatTurn,
                    onSelectToken = onTokenSelected,
                    onClose = { onSetSidePanelOpen(false) },
                    onFocusPinCoordinates = onFocusPinCoordinates,
                    onPinClicked = onPinClicked,
                    onToggleStickyPin = onToggleStickyPin,
                    onTogglePinLocked = onTogglePinLocked,
                    onEditPin = onEditPin,
                    onDeletePin = onDeletePin,
                    onToggleShowSecretPins = onToggleShowSecretPins,
                    modifier = Modifier.widthIn(max = 340.dp).fillMaxHeight()
                )
            }
        }
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

    // Diálogo de Calculadora Táctica de Daño, Curación y PV Temporales
    if (uiState.showDamageCalculatorDialog) {
        val calcTokenId = uiState.damageCalculatorTokenId ?: uiState.selectedTokenId
        val targetToken = uiState.tokens.find { it.id == calcTokenId }
        if (targetToken != null) {
            TacticalDamageCalculatorDialog(
                token = targetToken,
                onApplyDamage = { dmg -> onApplyDamageToToken(targetToken.id, dmg) },
                onApplyHealing = { heal -> onApplyHealingToToken(targetToken.id, heal) },
                onApplyTempHp = { temp -> onApplyTempHpToToken(targetToken.id, temp) },
                onDismiss = onCloseDamageCalculator
            )
        }
    }
}
