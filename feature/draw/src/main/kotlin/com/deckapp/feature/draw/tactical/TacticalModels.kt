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
    val mapBreadcrumbs: List<Pair<Long, String>> = emptyList(),
    val focusCameraOnTokenId: Long? = null,
    val showMapSelectorSheet: Boolean = false,
    val showCreateMapDialog: Boolean = false,
    val mapToRename: TacticalMap? = null,
    val mapToLinkScene: TacticalMap? = null,
    val mapToDelete: TacticalMap? = null,
    // Vínculos de Aventura en Pines
    val availableTables: List<RandomTable> = emptyList(),
    val availableEncounters: List<Encounter> = emptyList(),
    val availableWikiEntries: List<WikiEntry> = emptyList(),
    val previewWikiEntry: WikiEntry? = null,
    // Sincronización Táctica de Combate, Iniciativa y Ataques
    val activeEncounter: Encounter? = null,
    val targetingAttackerTokenId: Long? = null,
    val pendingAttackTargetTokenId: Long? = null,
    val attackResolution: TacticalAttackResolution? = null,
    val showCustomAttackDialog: Boolean = false,
    // Panel Lateral Rápido Multitool (Mesa Híbrida)
    val isSidePanelOpen: Boolean = false,
    val activeSidePanelTab: TacticalSidePanelTab = TacticalSidePanelTab.NOTES_WIKI,
    val sidePanelDiceCount: Int = 1,
    val sidePanelDiceSides: Int = 20,
    val sidePanelDiceModifier: Int = 0,
    val sidePanelLastDiceResult: DiceRollResult? = null,
    val sidePanelRecentRolls: List<DiceRollResult> = emptyList(),
    val sidePanelMythicChaos: Int = 5,
    val sidePanelMythicOdds: ProbabilityLevel = ProbabilityLevel.FIFTY_FIFTY,
    val sidePanelLastFateResult: Pair<FateResult, Boolean>? = null,
    val sidePanelLastEventResult: String? = null,
    val wikiSearchQuery: String = ""
) {
    val selectedToken: TacticalToken? get() = tokens.find { it.id == selectedTokenId }
    val selectedPin: TacticalMapPin? get() = pins.find { it.id == selectedPinId }
    val targetingAttackerToken: TacticalToken? get() = tokens.find { it.id == targetingAttackerTokenId }
    val pendingAttackTargetToken: TacticalToken? get() = tokens.find { it.id == pendingAttackTargetTokenId }

    val activeCombatTurnTokenId: Long?
        get() {
            val encounter = activeEncounter ?: return null
            if (encounter.creatures.isEmpty()) return null
            val activeCreature = encounter.creatures.getOrNull(encounter.currentTurnIndex.coerceIn(0, encounter.creatures.size - 1)) ?: return null
            return tokens.find { it.encounterCreatureId == activeCreature.id || (it.npcId != null && it.npcId == activeCreature.npcId) }?.id
        }
}

enum class TacticalSidePanelTab {
    NOTES_WIKI,
    DICE,
    MYTHIC,
    COMBAT
}

data class TacticalAttackOption(
    val name: String,
    val bonus: Int,
    val damageFormula: String
)

data class TacticalAttackResolution(
    val attackerTokenId: Long,
    val attackerName: String,
    val targetTokenId: Long,
    val targetName: String,
    val targetAc: Int,
    val attackName: String,
    val attackBonus: Int,
    val d20Roll: Int,
    val totalAttackRoll: Int,
    val isCriticalHit: Boolean,
    val isCriticalMiss: Boolean,
    val isHit: Boolean,
    val damageFormula: String,
    val damageRoll: Int,
    val distanceFt: Int,
    val elevationDiffFt: Int = 0,
    val advantageLabel: String? = null,
    val isDamageApplied: Boolean = false
)
