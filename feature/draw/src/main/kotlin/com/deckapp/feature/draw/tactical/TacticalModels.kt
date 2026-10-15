package com.deckapp.feature.draw.tactical

import androidx.compose.ui.geometry.Offset
import com.deckapp.core.model.*

enum class TacticalTool {
    NAVIGATE,
    SELECT,
    MEASURE,
    AOE,
    PIN,
    FOG,
    TERRAIN,
    ROUTE
}

enum class FogBrushMode {
    REVEAL,
    HIDE
}

data class PersistentAoE(
    val id: String = java.util.UUID.randomUUID().toString(),
    val shape: TacticalMeasureShape,
    val originWorld: Offset,
    val sizeFt: Int,
    val angleDegrees: Float = 0f,
    val colorHex: Long = 0x44FF9800L,
    val name: String = "",
    val anchorTokenId: Long? = null
)

data class TacticalUiState(
    val undoLabel: String? = null,
    val isEditingMap: Boolean = false,
    val isCreatingMap: Boolean = false,
    val map: TacticalMap? = null,
    val tokens: List<TacticalToken> = emptyList(),
    val pins: List<TacticalMapPin> = emptyList(),
    val selectedTokenId: Long? = null,
    val selectedPinId: Long? = null,
    val activeTool: TacticalTool = TacticalTool.NAVIGATE,
    // Estado de plantillas AoE
    val activeAoeShape: TacticalMeasureShape = TacticalMeasureShape.CIRCLE,
    val aoeRadiusFt: Int = 20,
    val aoeOrigin: Offset? = null,
    val aoeAngleDegrees: Float = 0f,
    val persistentAoEs: List<PersistentAoE> = emptyList(),
    val selectedAreaId: String? = null,
    val movingAreaId: String? = null,
    // Estado de drag ruler en movimiento de token
    val draggingTokenId: Long? = null,
    val dragStartCell: Pair<Float, Float>? = null,
    val dragCurrentCell: Pair<Float, Float>? = null,
    val dragDistanceFt: Int = 0,
    // Estado de Ruta planificada por waypoints
    val routeWaypoints: List<Pair<Float, Float>> = emptyList(),
    val currentRouteDistanceFt: Int = 0,
    val tokenMovedThisTurnFt: Map<Long, Int> = emptyMap(),
    // Estado de medición libre
    val measureStartOffset: Offset? = null,
    val measureCurrentOffset: Offset? = null,
    val measureDistanceFt: Int = 0,
    // Calibración guiada 3x3
    val isGuidedCalibrationActive: Boolean = false,
    val guidedCalibrationFirstPoint: Offset? = null,
    // Niebla de Guerra (Fog of War)
    val isFogEnabled: Boolean = false,
    val fogBrushMode: FogBrushMode = FogBrushMode.REVEAL,
    val revealedCells: Set<String> = emptySet(), // "col,row"
    val isFogDmView: Boolean = true, // true: semi-transparente (vista DM/Solo), false: totalmente opaco
    // Terreno Difícil y Obstáculos
    val terrainMap: Map<String, TacticalTerrainType> = emptyMap(),
    val activeTerrainBrush: TacticalTerrainType = TacticalTerrainType.DIFFICULT,
    // Diálogos y gestión de Pines
    val showAddTokenDialog: Boolean = false,
    val showGridCalibrationDialog: Boolean = false,
    val showPinDetailDialog: Boolean = false,
    val showCreatePinDialog: Boolean = false,
    val pendingPinCoordinates: Pair<Float, Float>? = null,
    val pinToEdit: TacticalMapPin? = null,
    val showMapSettingsDialog: Boolean = false,
    // Gestión Multimapa de la Sesión y Vínculo con Escenas
    val availableMaps: List<TacticalMap> = emptyList(),
    val showMapSelectorSheet: Boolean = false,
    val showCreateMapDialog: Boolean = false,
    val mapToRename: TacticalMap? = null,
    val mapToLinkScene: TacticalMap? = null,
    // Vínculos de Aventura en Pines
    val availableTables: List<RandomTable> = emptyList(),
    val availableEncounters: List<Encounter> = emptyList(),
    val availableWikiEntries: List<WikiEntry> = emptyList(),
    val previewWikiEntry: WikiEntry? = null
) {
    val selectedToken: TacticalToken? get() = tokens.find { it.id == selectedTokenId }
    val selectedPin: TacticalMapPin? get() = pins.find { it.id == selectedPinId }
}
