package com.deckapp.feature.draw.delegates

import androidx.compose.ui.geometry.Offset
import com.deckapp.core.domain.repository.EncounterRepository
import com.deckapp.core.domain.repository.TacticalMapRepository
import com.deckapp.core.domain.repository.TableRepository
import com.deckapp.core.domain.repository.WikiRepository
import com.deckapp.core.domain.usecase.CalculateTacticalDistanceUseCase
import com.deckapp.core.domain.usecase.RollTableUseCase
import com.deckapp.core.model.*
import com.deckapp.feature.draw.SessionUiState
import com.deckapp.feature.draw.tactical.FogBrushMode
import com.deckapp.feature.draw.tactical.PersistentAoE
import com.deckapp.feature.draw.tactical.TacticalTool
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.max

/**
 * Delegado encargado de la gestión del Mapa Táctico de Batalla (VTT) en la sesión.
 * Coordina tokens, sincronización bidireccional con combates, grilla, calibración guiada y plantillas AoE.
 */
class TacticalMapDelegate(
    private val sessionId: Long,
    private val _uiState: MutableStateFlow<SessionUiState>,
    private val scope: CoroutineScope,
    private val tacticalMapRepository: TacticalMapRepository,
    private val encounterRepository: EncounterRepository,
    val distanceCalculator: CalculateTacticalDistanceUseCase,
    private val tableRepository: TableRepository? = null,
    private val rollTableUseCase: RollTableUseCase? = null,
    private val wikiRepository: WikiRepository? = null,
    private val context: android.content.Context? = null
) {

    private fun loadActiveMapPreference(): Long? {
        val prefs = context?.getSharedPreferences("tactical_prefs", android.content.Context.MODE_PRIVATE)
        val id = prefs?.getLong("active_map_$sessionId", -1L) ?: -1L
        return if (id > 0) id else null
    }

    private fun saveActiveMapPreference(mapId: Long?) {
        val prefs = context?.getSharedPreferences("tactical_prefs", android.content.Context.MODE_PRIVATE) ?: return
        if (mapId != null && mapId > 0) {
            prefs.edit().putLong("active_map_$sessionId", mapId).apply()
        } else {
            prefs.edit().remove("active_map_$sessionId").apply()
        }
    }

    private fun mapEdit(action: suspend () -> Unit) {
        scope.launch(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) {
            try {
                withContext(NonCancellable) { action() }
            } catch (failure: Exception) {
                _uiState.update { it.copy(snackbarMessage = failure.message ?: "Error en el mapa táctico") }
            }
        }
    }

    private var currentSelectedMapId: Long? = loadActiveMapPreference()
    private var tokensAndPinsJob: kotlinx.coroutines.Job? = null

    init {
        observeTacticalMap()
        observeActiveCombatSync()
        observeAdventureResources()
    }

    private fun observeAdventureResources() {
        tableRepository?.getAllTables()?.onEach { tables ->
            _uiState.update { state ->
                state.copy(tacticalState = state.tacticalState.copy(availableTables = tables))
            }
        }?.launchIn(scope)

        encounterRepository.getAllEncounters().onEach { encounters ->
            _uiState.update { state ->
                state.copy(tacticalState = state.tacticalState.copy(availableEncounters = encounters))
            }
        }.launchIn(scope)


        wikiRepository?.getAllEntries()?.onEach { entries ->
            _uiState.update { state ->
                state.copy(tacticalState = state.tacticalState.copy(availableWikiEntries = entries))
            }
        }?.launchIn(scope)
    }


    private fun observeTacticalMap() {
        tacticalMapRepository.getTacticalMapsBySessionId(sessionId).onEach { maps ->
            _uiState.update { state ->
                state.copy(
                    tacticalState = state.tacticalState.copy(availableMaps = maps)
                )
            }

            val targetMap = if (currentSelectedMapId != null) {
                maps.find { it.id == currentSelectedMapId } ?: maps.firstOrNull()
            } else {
                maps.firstOrNull()
            }

            if (targetMap != null) {
                currentSelectedMapId = targetMap.id
                val (isFogOn, loadedCells) = parseFogJson(targetMap.fogOfWarMaskJson)
                val loadedTerrain = parseTerrainJson(targetMap.terrainMaskJson)
                _uiState.update { state ->
                    state.copy(
                        tacticalState = state.tacticalState.copy(
                            map = targetMap,
                            isFogEnabled = isFogOn,
                            revealedCells = loadedCells,
                            terrainMap = loadedTerrain
                        )
                    )
                }
                observeTokensAndPins(targetMap.id)
            } else {
                currentSelectedMapId = null
                tokensAndPinsJob?.cancel()
                _uiState.update { state ->
                    state.copy(
                        tacticalState = state.tacticalState.copy(map = null, tokens = emptyList(), pins = emptyList())
                    )
                }
            }
        }.launchIn(scope)
    }

    private fun observeTokensAndPins(mapId: Long) {
        tokensAndPinsJob?.cancel()
        tokensAndPinsJob = scope.launch {
            launch {
                tacticalMapRepository.getTokensByMapId(mapId).collect { tokens ->
                    _uiState.update { state ->
                        state.copy(tacticalState = state.tacticalState.copy(tokens = tokens))
                    }
                }
            }
            launch {
                tacticalMapRepository.getPinsByMapId(mapId).collect { pins ->
                    _uiState.update { state ->
                        state.copy(tacticalState = state.tacticalState.copy(pins = pins))
                    }
                }
            }
        }
    }

    /**
     * Sincronización automática desde el encuentro de combate hacia los tokens del mapa táctico.
     */
    private fun observeActiveCombatSync() {
        encounterRepository.getActiveEncounter(sessionId).onEach { encounter ->
            if (encounter != null) {
                val currentTokens = _uiState.value.tacticalState.tokens
                val creatureMap = encounter.creatures.associateBy { it.id }

                currentTokens.forEach { token ->
                    val creatureId = token.encounterCreatureId
                    if (creatureId != null && creatureMap.containsKey(creatureId)) {
                        val creature = creatureMap[creatureId]!!
                        if (token.currentHp != creature.currentHp || token.conditions != creature.conditions) {
                            tacticalMapRepository.upsertToken(
                                token.copy(
                                    currentHp = creature.currentHp,
                                    maxHp = creature.maxHp,
                                    armorClass = creature.armorClass,
                                    conditions = creature.conditions
                                )
                            )
                        }
                    }
                }
            }
        }.launchIn(scope)
    }

    fun setTacticalTool(tool: TacticalTool) {
        _uiState.update { state ->
            val tactical = state.tacticalState
            val initialWaypoints = if (tool == TacticalTool.ROUTE) {
                tactical.selectedToken?.let { token ->
                    listOf(Pair(token.cellX, token.cellY))
                } ?: emptyList()
            } else {
                tactical.routeWaypoints
            }
            state.copy(
                tacticalState = tactical.copy(
                    activeTool = tool,
                    isGuidedCalibrationActive = false,
                    guidedCalibrationFirstPoint = null,
                    routeWaypoints = initialWaypoints,
                    currentRouteDistanceFt = if (tool == TacticalTool.ROUTE) 0 else tactical.currentRouteDistanceFt
                )
            )
        }
    }

    fun setAoeShape(shape: TacticalMeasureShape) {
        _uiState.update { state ->
            state.copy(tacticalState = state.tacticalState.copy(activeAoeShape = shape))
        }
    }

    fun setAoeRadius(radiusFt: Int) {
        _uiState.update { state ->
            state.copy(tacticalState = state.tacticalState.copy(aoeRadiusFt = radiusFt))
        }
    }

    fun updateAoEPosition(origin: Offset, angle: Float) {
        _uiState.update { state ->
            state.copy(
                tacticalState = state.tacticalState.copy(
                    aoeOrigin = origin,
                    aoeAngleDegrees = angle
                )
            )
        }
    }

    fun persistCurrentAoE() {
        val tactical = _uiState.value.tacticalState
        val map = tactical.map ?: return
        val origin = tactical.aoeOrigin ?: Offset(map.gridOffsetX + map.cellSizePx * 3, map.gridOffsetY + map.cellSizePx * 3)

        val newAoE = PersistentAoE(
            shape = tactical.activeAoeShape,
            originWorld = origin,
            sizeFt = tactical.aoeRadiusFt,
            angleDegrees = tactical.aoeAngleDegrees
        )

        _uiState.update { state ->
            state.copy(
                tacticalState = state.tacticalState.copy(
                    persistentAoEs = state.tacticalState.persistentAoEs + newAoE
                ),
                snackbarMessage = "Plantilla AoE fijada en el mapa"
            )
        }
    }

    fun clearPersistentAoEs() {
        _uiState.update { state ->
            state.copy(tacticalState = state.tacticalState.copy(persistentAoEs = emptyList()))
        }
    }

    fun selectToken(tokenId: Long?) {
        _uiState.update { state ->
            val tactical = state.tacticalState
            val selected = tactical.tokens.find { it.id == tokenId }
            val waypoints = if (tactical.activeTool == TacticalTool.ROUTE && selected != null) {
                listOf(Pair(selected.cellX, selected.cellY))
            } else if (tactical.activeTool == TacticalTool.ROUTE) {
                emptyList()
            } else {
                tactical.routeWaypoints
            }
            state.copy(
                tacticalState = tactical.copy(
                    selectedTokenId = tokenId,
                    routeWaypoints = waypoints,
                    currentRouteDistanceFt = if (tactical.activeTool == TacticalTool.ROUTE) 0 else tactical.currentRouteDistanceFt
                )
            )
        }
    }

    private fun calculateRouteDistance(
        waypoints: List<Pair<Float, Float>>,
        map: TacticalMap?,
        terrainMap: Map<String, TacticalTerrainType>
    ): Int {
        if (waypoints.size < 2) return 0
        val gridType = map?.gridType ?: GridType.SQUARE
        val diagonalRule = map?.diagonalRule ?: DiagonalRule.ALTERNATING_5_10_5
        val scaleFtPerCell = map?.scaleFtPerCell ?: 5

        var total = 0
        for (i in 0 until waypoints.size - 1) {
            val from = waypoints[i]
            val to = waypoints[i + 1]
            val res = distanceCalculator(
                fromX = from.first,
                fromY = from.second,
                toX = to.first,
                toY = to.second,
                gridType = gridType,
                diagonalRule = diagonalRule,
                scaleFtPerCell = scaleFtPerCell,
                terrainMap = terrainMap
            )
            total += res.distanceFt
        }
        return total
    }

    fun addRouteWaypoint(cellX: Float, cellY: Float) {
        _uiState.update { state ->
            val tactical = state.tacticalState
            val newWaypoints = tactical.routeWaypoints + Pair(cellX, cellY)
            val distance = calculateRouteDistance(newWaypoints, tactical.map, tactical.terrainMap)
            state.copy(
                tacticalState = tactical.copy(
                    routeWaypoints = newWaypoints,
                    currentRouteDistanceFt = distance
                )
            )
        }
    }

    fun removeLastRouteWaypoint() {
        _uiState.update { state ->
            val tactical = state.tacticalState
            if (tactical.routeWaypoints.size <= 1) {
                state.copy(
                    tacticalState = tactical.copy(
                        currentRouteDistanceFt = 0
                    )
                )
            } else {
                val newWaypoints = tactical.routeWaypoints.dropLast(1)
                val distance = calculateRouteDistance(newWaypoints, tactical.map, tactical.terrainMap)
                state.copy(
                    tacticalState = tactical.copy(
                        routeWaypoints = newWaypoints,
                        currentRouteDistanceFt = distance
                    )
                )
            }
        }
    }

    fun clearRoute() {
        _uiState.update { state ->
            val tactical = state.tacticalState
            val initial = tactical.selectedToken?.let { token ->
                listOf(Pair(token.cellX, token.cellY))
            } ?: emptyList()
            state.copy(
                tacticalState = tactical.copy(
                    routeWaypoints = initial,
                    currentRouteDistanceFt = 0
                )
            )
        }
    }

    fun commitRouteMovement() {
        val tactical = _uiState.value.tacticalState
        val token = tactical.selectedToken ?: return
        val lastWaypoint = tactical.routeWaypoints.lastOrNull() ?: return
        if (tactical.routeWaypoints.size < 2) return

        val snappedX = kotlin.math.floor(lastWaypoint.first) + (token.sizeInCells / 2f)
        val snappedY = kotlin.math.floor(lastWaypoint.second) + (token.sizeInCells / 2f)

        moveToken(token.id, snappedX, snappedY)

        val previousMoved = tactical.tokenMovedThisTurnFt[token.id] ?: 0
        val totalMoved = previousMoved + tactical.currentRouteDistanceFt

        _uiState.update { state ->
            state.copy(
                tacticalState = state.tacticalState.copy(
                    tokenMovedThisTurnFt = state.tacticalState.tokenMovedThisTurnFt + (token.id to totalMoved),
                    routeWaypoints = emptyList(),
                    currentRouteDistanceFt = 0,
                    activeTool = TacticalTool.SELECT
                )
            )
        }
    }

    fun resetTokenTurnMovement(tokenId: Long) {
        _uiState.update { state ->
            state.copy(
                tacticalState = state.tacticalState.copy(
                    tokenMovedThisTurnFt = state.tacticalState.tokenMovedThisTurnFt - tokenId
                )
            )
        }
    }

    fun moveToken(tokenId: Long, newCellX: Float, newCellY: Float) {
        scope.launch {
            tacticalMapRepository.updateTokenPosition(tokenId, newCellX, newCellY)
        }
    }

    fun updateTokenHp(tokenId: Long, newHp: Int) {
        scope.launch {
            val token = _uiState.value.tacticalState.tokens.find { it.id == tokenId }
            tacticalMapRepository.updateTokenHp(tokenId, newHp)

            // Sincronizar bidireccionalmente con la criatura del encuentro activo
            token?.encounterCreatureId?.let { creatureId ->
                val creature = encounterRepository.getCreatureById(creatureId)
                if (creature != null) {
                    encounterRepository.updateCreature(creature.copy(currentHp = newHp))
                }
            }
        }
    }

    fun toggleCondition(tokenId: Long, condition: Condition) {
        val token = _uiState.value.tacticalState.tokens.find { it.id == tokenId } ?: return
        val currentConditions = token.conditions.toMutableSet()
        if (currentConditions.contains(condition)) {
            currentConditions.remove(condition)
        } else {
            currentConditions.add(condition)
        }
        scope.launch {
            tacticalMapRepository.upsertToken(token.copy(conditions = currentConditions))

            // Sincronizar bidireccionalmente con la criatura del encuentro activo
            token.encounterCreatureId?.let { creatureId ->
                val creature = encounterRepository.getCreatureById(creatureId)
                if (creature != null) {
                    encounterRepository.updateCreature(creature.copy(conditions = currentConditions))
                }
            }
        }
    }

    fun updateTokenSize(tokenId: Long, size: Float) {
        val token = _uiState.value.tacticalState.tokens.find { it.id == tokenId } ?: return
        scope.launch {
            tacticalMapRepository.upsertToken(token.copy(sizeInCells = size))
        }
    }

    fun toggleTokenVisibility(tokenId: Long) {
        val token = _uiState.value.tacticalState.tokens.find { it.id == tokenId } ?: return
        scope.launch {
            tacticalMapRepository.upsertToken(token.copy(isHidden = !token.isHidden))
        }
    }

    fun deleteToken(tokenId: Long) {
        scope.launch {
            tacticalMapRepository.deleteToken(tokenId)
            _uiState.update { state ->
                state.copy(
                    tacticalState = state.tacticalState.copy(
                        selectedTokenId = if (state.tacticalState.selectedTokenId == tokenId) null else state.tacticalState.selectedTokenId
                    )
                )
            }
        }
    }

    fun addToken(name: String, faction: TokenFaction, hp: Int, ac: Int, size: Float) {
        val map = _uiState.value.tacticalState.map ?: return
        val currentTokenCount = _uiState.value.tacticalState.tokens.size
        val cellX = 3f + (currentTokenCount % 5) * 1.5f
        val cellY = 3f + (currentTokenCount / 5) * 1.5f

        val newToken = TacticalToken(
            mapId = map.id,
            name = name,
            faction = faction,
            sizeInCells = size,
            cellX = cellX,
            cellY = cellY,
            currentHp = hp,
            maxHp = hp,
            armorClass = ac
        )

        scope.launch {
            tacticalMapRepository.upsertToken(newToken)
        }
    }

    fun addTokenFromNpc(npc: Npc) {
        val map = _uiState.value.tacticalState.map ?: return
        val currentTokenCount = _uiState.value.tacticalState.tokens.size
        val cellX = 3f + (currentTokenCount % 5) * 1.5f
        val cellY = 3f + (currentTokenCount / 5) * 1.5f

        val faction = if (npc.isMonster) {
            TokenFaction.ENEMY
        } else {
            TokenFaction.NEUTRAL
        }

        val hp = npc.maxHp.coerceAtLeast(1)
        val ac = npc.armorClass.coerceAtLeast(1)
        val tokenSize = 1f

        val newToken = TacticalToken(
            mapId = map.id,
            name = npc.name,
            avatarUri = npc.imagePath,
            faction = faction,
            sizeInCells = tokenSize,
            cellX = cellX,
            cellY = cellY,
            currentHp = hp,
            maxHp = hp,
            armorClass = ac,
            npcId = npc.id
        )

        scope.launch {
            tacticalMapRepository.upsertToken(newToken)
            _uiState.update { it.copy(snackbarMessage = "${npc.name} añadido al mapa táctico") }
        }
    }

    fun updateGridCalibration(cellSize: Float, offsetX: Float, offsetY: Float) {
        val map = _uiState.value.tacticalState.map ?: return
        scope.launch {
            tacticalMapRepository.upsertTacticalMap(
                map.copy(
                    cellSizePx = cellSize,
                    gridOffsetX = offsetX,
                    gridOffsetY = offsetY
                )
            )
        }
    }

    fun updateMapImage(imageUri: String) {
        val map = _uiState.value.tacticalState.map ?: return
        scope.launch {
            tacticalMapRepository.upsertTacticalMap(
                map.copy(imageUri = imageUri)
            )
            _uiState.update { it.copy(snackbarMessage = "Imagen de mapa actualizada") }
        }
    }

    fun startGuidedCalibration() {
        _uiState.update { state ->
            state.copy(
                tacticalState = state.tacticalState.copy(
                    isGuidedCalibrationActive = true,
                    guidedCalibrationFirstPoint = null
                ),
                snackbarMessage = "Toca la esquina superior izquierda de un bloque de 3x3 celdas"
            )
        }
    }

    fun handleGuidedCalibrationPoint(point: Offset) {
        val tactical = _uiState.value.tacticalState
        val map = tactical.map ?: return
        val firstPoint = tactical.guidedCalibrationFirstPoint

        if (firstPoint == null) {
            _uiState.update { state ->
                state.copy(
                    tacticalState = state.tacticalState.copy(guidedCalibrationFirstPoint = point),
                    snackbarMessage = "Punto 1 fijado. Ahora toca la esquina opuesta del bloque 3x3"
                )
            }
        } else {
            val spanX = abs(point.x - firstPoint.x)
            val spanY = abs(point.y - firstPoint.y)
            val computedCellSize = (max(spanX, spanY) / 3f).coerceIn(20f, 400f)
            val computedOffsetX = (firstPoint.x % computedCellSize)
            val computedOffsetY = (firstPoint.y % computedCellSize)

            scope.launch {
                tacticalMapRepository.upsertTacticalMap(
                    map.copy(
                        cellSizePx = computedCellSize,
                        gridOffsetX = computedOffsetX,
                        gridOffsetY = computedOffsetY
                    )
                )
                _uiState.update { state ->
                    state.copy(
                        tacticalState = state.tacticalState.copy(
                            isGuidedCalibrationActive = false,
                            guidedCalibrationFirstPoint = null
                        ),
                        snackbarMessage = "Grilla calibrada a ${computedCellSize.toInt()}px por celda"
                    )
                }
            }
        }
    }

    fun createDefaultMap() {
        scope.launch {
            val newMap = TacticalMap(
                name = "Mapa de Batalla 1",
                imageUri = "",
                cellSizePx = 70f,
                scaleFtPerCell = 5,
                gridType = GridType.SQUARE,
                diagonalRule = DiagonalRule.ALTERNATING_5_10_5,
                sessionId = sessionId
            )
            val mapId = tacticalMapRepository.upsertTacticalMap(newMap)
            val initialHero = TacticalToken(
                mapId = mapId,
                name = "Aventurero",
                faction = TokenFaction.PLAYER,
                cellX = 2.5f,
                cellY = 2.5f,
                currentHp = 25,
                maxHp = 25,
                armorClass = 15,
                speedFt = 30
            )
            tacticalMapRepository.upsertToken(initialHero)
        }
    }

    /**
     * Sincroniza las criaturas de un encuentro activo como tokens tácticos en el mapa.
     */
    fun syncTokensFromEncounter(encounter: Encounter) {
        val map = _uiState.value.tacticalState.map ?: return
        val existingCreatureIds = _uiState.value.tacticalState.tokens.mapNotNull { it.encounterCreatureId }.toSet()

        val tokensToCreate = encounter.creatures.filter { it.id !in existingCreatureIds }.mapIndexed { index, creature ->
            TacticalToken(
                mapId = map.id,
                name = creature.name,
                faction = if (creature.npcId != null) TokenFaction.ENEMY else TokenFaction.PLAYER,
                cellX = 4f + (index % 4) * 2f,
                cellY = 4f + (index / 4) * 2f,
                currentHp = creature.currentHp,
                maxHp = creature.maxHp,
                armorClass = creature.armorClass,
                encounterCreatureId = creature.id,
                npcId = creature.npcId
            )
        }

        if (tokensToCreate.isNotEmpty()) {
            scope.launch {
                tacticalMapRepository.upsertTokens(tokensToCreate)
                _uiState.update { it.copy(snackbarMessage = "${tokensToCreate.size} combatientes importados al mapa táctico") }
            }
        } else {
            _uiState.update { it.copy(snackbarMessage = "Todos los combatientes ya están en el mapa") }
        }
    }

    // ==========================================
    // PINES DE DUNGEON / ROOM KEYING
    // ==========================================

    fun selectPin(pinId: Long?) {
        _uiState.update { state ->
            state.copy(
                tacticalState = state.tacticalState.copy(
                    selectedPinId = pinId,
                    showPinDetailDialog = pinId != null
                )
            )
        }
    }

    fun openCreatePin(cellX: Float, cellY: Float) {
        _uiState.update { state ->
            state.copy(
                tacticalState = state.tacticalState.copy(
                    showCreatePinDialog = true,
                    pendingPinCoordinates = Pair(cellX, cellY),
                    pinToEdit = null
                )
            )
        }
    }

    fun openEditPin(pin: TacticalMapPin) {
        _uiState.update { state ->
            state.copy(
                tacticalState = state.tacticalState.copy(
                    showCreatePinDialog = true,
                    pinToEdit = pin,
                    showPinDetailDialog = false
                )
            )
        }
    }

    fun dismissPinDialogs() {
        _uiState.update { state ->
            state.copy(
                tacticalState = state.tacticalState.copy(
                    showPinDetailDialog = false,
                    showCreatePinDialog = false,
                    pinToEdit = null,
                    pendingPinCoordinates = null
                )
            )
        }
    }

    fun savePin(pin: TacticalMapPin) {
        scope.launch {
            tacticalMapRepository.upsertPin(pin)
            dismissPinDialogs()
            _uiState.update { it.copy(snackbarMessage = "Pin guardado: ${pin.title}") }
        }
    }

    fun deletePin(pinId: Long) {
        scope.launch {
            tacticalMapRepository.deletePin(pinId)
            dismissPinDialogs()
            _uiState.update { it.copy(snackbarMessage = "Pin eliminado") }
        }
    }

    fun updatePinExplorationState(pinId: Long, state: MapPinExplorationState) {
        val mapId = _uiState.value.tacticalState.map?.id ?: return
        mapEdit {
            val pin = tacticalMapRepository.getPinsByMapId(mapId).first().find { it.id == pinId } ?: return@mapEdit
            val updated = pin.copy(explorationState = state)
            tacticalMapRepository.upsertPin(updated)
        }
    }

    fun rollPinTable(pinId: Long) {
        val mapId = _uiState.value.tacticalState.map?.id ?: return
        val pin = _uiState.value.tacticalState.pins.find { it.id == pinId } ?: return
        val tableId = pin.targetTableId ?: return
        val roller = rollTableUseCase ?: return

        scope.launch {
            try {
                val result = roller(tableId = tableId, sessionId = sessionId)
                mapEdit {
                    val currentPin = tacticalMapRepository.getPinsByMapId(mapId).first().find { it.id == pinId } ?: return@mapEdit
                    val newDiscoveries = currentPin.discoveries + result
                    val newExploration = if (currentPin.explorationState == MapPinExplorationState.UNKNOWN) {
                        MapPinExplorationState.DISCOVERED
                    } else {
                        currentPin.explorationState
                    }
                    val updated = currentPin.copy(
                        discoveries = newDiscoveries,
                        explorationState = newExploration
                    )
                    tacticalMapRepository.upsertPin(updated)
                    _uiState.update { it.copy(snackbarMessage = "Tirada en ${currentPin.title}: ${result.resolvedText}") }
                }
            } catch (e: Exception) {

                _uiState.update { it.copy(snackbarMessage = "Error al tirar en tabla: ${e.message}") }
            }
        }
    }

    fun removePinDiscovery(pinId: Long, discoveryIndex: Int) {
        val mapId = _uiState.value.tacticalState.map?.id ?: return
        mapEdit {
            val pin = tacticalMapRepository.getPinsByMapId(mapId).first().find { it.id == pinId } ?: return@mapEdit
            if (discoveryIndex in pin.discoveries.indices) {
                val newDiscoveries = pin.discoveries.toMutableList().apply { removeAt(discoveryIndex) }
                val updated = pin.copy(discoveries = newDiscoveries)
                tacticalMapRepository.upsertPin(updated)
            }
        }
    }

    fun setPreviewWikiEntry(entry: WikiEntry?) {
        _uiState.update { it.copy(tacticalState = it.tacticalState.copy(previewWikiEntry = entry)) }
    }


    // ==========================================
    // NIEBLA DE GUERRA (FOG OF WAR)
    // ==========================================

    fun toggleFog() {
        val currentMap = _uiState.value.tacticalState.map ?: return
        val newEnabled = !_uiState.value.tacticalState.isFogEnabled
        _uiState.update { state ->
            state.copy(tacticalState = state.tacticalState.copy(isFogEnabled = newEnabled))
        }
        saveFogState(currentMap, newEnabled, _uiState.value.tacticalState.revealedCells)
    }

    fun setFogBrushMode(mode: FogBrushMode) {
        _uiState.update { state ->
            state.copy(tacticalState = state.tacticalState.copy(fogBrushMode = mode))
        }
    }

    fun toggleFogDmView() {
        _uiState.update { state ->
            state.copy(tacticalState = state.tacticalState.copy(isFogDmView = !state.tacticalState.isFogDmView))
        }
    }

    fun interactFogCell(col: Int, row: Int) {
        val currentMap = _uiState.value.tacticalState.map ?: return
        val cellKey = "$col,$row"
        val currentRevealed = _uiState.value.tacticalState.revealedCells.toMutableSet()
        val mode = _uiState.value.tacticalState.fogBrushMode

        val changed = if (mode == FogBrushMode.REVEAL) {
            currentRevealed.add(cellKey)
        } else {
            currentRevealed.remove(cellKey)
        }

        if (changed) {
            _uiState.update { state ->
                state.copy(tacticalState = state.tacticalState.copy(revealedCells = currentRevealed))
            }
            saveFogState(currentMap, _uiState.value.tacticalState.isFogEnabled, currentRevealed)
        }
    }

    fun revealAllFog() {
        val currentMap = _uiState.value.tacticalState.map ?: return
        val allCells = mutableSetOf<String>()
        for (c in 0..50) {
            for (r in 0..50) {
                allCells.add("$c,$r")
            }
        }
        _uiState.update { state ->
            state.copy(tacticalState = state.tacticalState.copy(revealedCells = allCells))
        }
        saveFogState(currentMap, _uiState.value.tacticalState.isFogEnabled, allCells)
    }

    fun resetFog() {
        val currentMap = _uiState.value.tacticalState.map ?: return
        _uiState.update { state ->
            state.copy(tacticalState = state.tacticalState.copy(revealedCells = emptySet()))
        }
        saveFogState(currentMap, _uiState.value.tacticalState.isFogEnabled, emptySet())
    }

    private fun saveFogState(map: TacticalMap, isEnabled: Boolean, revealed: Set<String>) {
        scope.launch {
            val json = buildFogJson(isEnabled, revealed)
            tacticalMapRepository.upsertTacticalMap(map.copy(fogOfWarMaskJson = json))
        }
    }

    private fun buildFogJson(isEnabled: Boolean, revealed: Set<String>): String {
        val joined = revealed.joinToString(",")
        return "enabled:$isEnabled;cells:$joined"
    }

    private fun parseFogJson(json: String): Pair<Boolean, Set<String>> {
        if (json.isBlank()) return Pair(false, emptySet())
        var enabled = false
        val cells = mutableSetOf<String>()
        json.split(";").forEach { part ->
            if (part.startsWith("enabled:")) {
                enabled = part.removePrefix("enabled:").toBoolean()
            } else if (part.startsWith("cells:")) {
                val list = part.removePrefix("cells:")
                if (list.isNotBlank()) {
                    list.split(",").filter { it.contains(":") || it.count { c -> c == ',' } == 0 }.forEach {
                        // cada celda es "x,y", al dividir por comas necesitamos pares
                    }
                }
            }
        }
        // Soporte robusto si se serializó con comas o punto y coma
        val regex = Regex("(\\d+),(\\d+)")
        regex.findAll(json).forEach { match ->
            cells.add(match.value)
        }
        val enabledMatch = Regex("enabled:(true|false)").find(json)
        if (enabledMatch != null) {
            enabled = enabledMatch.groupValues[1].toBoolean()
        }
        return Pair(enabled, cells)
    }

    // ==========================================
    // CAPA DE TERRENO DIFÍCIL Y OBSTÁCULOS
    // ==========================================

    fun setTerrainBrush(type: TacticalTerrainType) {
        _uiState.update { state ->
            state.copy(tacticalState = state.tacticalState.copy(activeTerrainBrush = type))
        }
    }

    fun interactTerrainCell(col: Int, row: Int) {
        val currentMap = _uiState.value.tacticalState.map ?: return
        val currentMapTerrain = _uiState.value.tacticalState.terrainMap.toMutableMap()
        val key = "$col,$row"
        val brush = _uiState.value.tacticalState.activeTerrainBrush

        if (brush == TacticalTerrainType.NORMAL) {
            currentMapTerrain.remove(key)
        } else {
            currentMapTerrain[key] = brush
        }

        _uiState.update { state ->
            state.copy(tacticalState = state.tacticalState.copy(terrainMap = currentMapTerrain))
        }
        saveTerrainState(currentMap, currentMapTerrain)
    }

    fun clearAllTerrain() {
        val currentMap = _uiState.value.tacticalState.map ?: return
        _uiState.update { state ->
            state.copy(tacticalState = state.tacticalState.copy(terrainMap = emptyMap()))
        }
        saveTerrainState(currentMap, emptyMap())
        _uiState.update { it.copy(snackbarMessage = "Capa de terreno reiniciada") }
    }

    private fun saveTerrainState(map: TacticalMap, terrain: Map<String, TacticalTerrainType>) {
        scope.launch {
            val json = buildTerrainJson(terrain)
            tacticalMapRepository.upsertTacticalMap(map.copy(terrainMaskJson = json))
        }
    }

    private fun buildTerrainJson(terrain: Map<String, TacticalTerrainType>): String {
        return terrain.entries.joinToString(";") { "${it.key}:${it.value.name}" }
    }

    private fun parseTerrainJson(json: String): Map<String, TacticalTerrainType> {
        if (json.isBlank()) return emptyMap()
        val result = mutableMapOf<String, TacticalTerrainType>()
        json.split(";").forEach { item ->
            val parts = item.split(":")
            if (parts.size == 2) {
                val coord = parts[0].trim()
                val typeName = parts[1].trim()
                val type = runCatching { TacticalTerrainType.valueOf(typeName) }.getOrNull()
                if (type != null && type != TacticalTerrainType.NORMAL) {
                    result[coord] = type
                }
            }
        }
        return result
    }

    // ==========================================
    // GESTIÓN MULTIMAPA Y VÍNCULO CON ESCENAS
    // ==========================================

    fun switchMap(mapId: Long) {
        currentSelectedMapId = mapId
        saveActiveMapPreference(mapId)
        val targetMap = _uiState.value.tacticalState.availableMaps.find { it.id == mapId } ?: return
        val (isFogOn, loadedCells) = parseFogJson(targetMap.fogOfWarMaskJson)
        val loadedTerrain = parseTerrainJson(targetMap.terrainMaskJson)
        _uiState.update { state ->
            state.copy(
                tacticalState = state.tacticalState.copy(
                    map = targetMap,
                    selectedTokenId = null,
                    selectedPinId = null,
                    isFogEnabled = isFogOn,
                    revealedCells = loadedCells,
                    terrainMap = loadedTerrain,
                    showMapSelectorSheet = false
                ),
                snackbarMessage = "Mapa activo: ${targetMap.name}"
            )
        }
        observeTokensAndPins(targetMap.id)
    }

    fun createNewMap(
        name: String,
        imageUri: String,
        gridType: GridType,
        cellSizePx: Float,
        scaleFtPerCell: Int,
        sceneId: Long?
    ) {
        scope.launch {
            val newMap = TacticalMap(
                name = name,
                imageUri = imageUri,
                gridType = gridType,
                cellSizePx = cellSizePx,
                scaleFtPerCell = scaleFtPerCell,
                sessionId = sessionId,
                sceneId = sceneId
            )
            val newId = tacticalMapRepository.upsertTacticalMap(newMap)
            currentSelectedMapId = newId
            dismissMultimapDialogs()
            _uiState.update { it.copy(snackbarMessage = "Mapa '$name' creado") }
        }
    }

    fun renameMap(mapId: Long, newName: String) {
        scope.launch {
            val map = _uiState.value.tacticalState.availableMaps.find { it.id == mapId } ?: return@launch
            tacticalMapRepository.upsertTacticalMap(map.copy(name = newName))
            dismissMultimapDialogs()
            _uiState.update { it.copy(snackbarMessage = "Mapa renombrado a '$newName'") }
        }
    }

    fun linkMapToScene(mapId: Long, sceneId: Long?) {
        scope.launch {
            val map = _uiState.value.tacticalState.availableMaps.find { it.id == mapId } ?: return@launch
            tacticalMapRepository.upsertTacticalMap(map.copy(sceneId = sceneId))
            dismissMultimapDialogs()
            _uiState.update { it.copy(snackbarMessage = "Mapa vinculado con éxito") }
        }
    }

    fun deleteMap(mapId: Long) {
        scope.launch {
            tacticalMapRepository.deleteTacticalMap(mapId)
            if (currentSelectedMapId == mapId) {
                currentSelectedMapId = null
                saveActiveMapPreference(null)
            }
            dismissMultimapDialogs()
            _uiState.update { it.copy(snackbarMessage = "Mapa eliminado") }
        }
    }

    fun openMapSelector() {
        _uiState.update { it.copy(tacticalState = it.tacticalState.copy(showMapSelectorSheet = true)) }
    }

    fun dismissMapSelector() {
        _uiState.update { it.copy(tacticalState = it.tacticalState.copy(showMapSelectorSheet = false)) }
    }

    fun openCreateMapDialog() {
        _uiState.update { it.copy(tacticalState = it.tacticalState.copy(showCreateMapDialog = true, showMapSelectorSheet = false)) }
    }

    fun openRenameMapDialog(map: TacticalMap) {
        _uiState.update { it.copy(tacticalState = it.tacticalState.copy(mapToRename = map, showMapSelectorSheet = false)) }
    }

    fun openLinkSceneDialog(map: TacticalMap) {
        _uiState.update { it.copy(tacticalState = it.tacticalState.copy(mapToLinkScene = map, showMapSelectorSheet = false)) }
    }

    fun dismissMultimapDialogs() {
        _uiState.update {
            it.copy(
                tacticalState = it.tacticalState.copy(
                    showMapSelectorSheet = false,
                    showCreateMapDialog = false,
                    mapToRename = null,
                    mapToLinkScene = null
                )
            )
        }
    }
}
