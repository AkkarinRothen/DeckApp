package com.deckapp.feature.draw.delegates

import com.deckapp.core.domain.repository.EncounterRepository
import com.deckapp.core.domain.repository.NpcRepository
import com.deckapp.core.domain.repository.ReferenceRepository
import com.deckapp.core.domain.repository.SessionRepository
import com.deckapp.core.domain.repository.TableRepository
import com.deckapp.core.domain.usecase.RollTableUseCase
import com.deckapp.core.model.Encounter
import com.deckapp.core.model.EncounterCreature
import com.deckapp.feature.draw.SessionUiState
import com.deckapp.feature.draw.components.spotlight.SpotlightCategory
import com.deckapp.feature.draw.components.spotlight.SpotlightItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Delegado encargado del Spotlight Command Palette (Sprint 64):
 * búsqueda global reactiva/asincrónica de tablas, PNJs, reglas del sistema, criaturas y notas,
 * así como la conversión de resultados a combate y diario.
 */
class SessionSpotlightDelegate(
    private val sessionId: Long,
    private val _uiState: MutableStateFlow<SessionUiState>,
    private val scope: CoroutineScope,
    private val tableRepository: TableRepository,
    private val npcRepository: NpcRepository,
    private val referenceRepository: ReferenceRepository,
    private val encounterRepository: EncounterRepository,
    private val rollTableUseCase: RollTableUseCase,
    private val sessionRepository: SessionRepository
) {
    private var spotlightJob: Job? = null

    fun openGlobalSearch() {
        openSpotlight()
    }

    fun openSpotlight() {
        _uiState.update { 
            it.copy(
                showSpotlightDialog = true,
                spotlightQuery = "",
                selectedSpotlightItem = null,
                lastSpotlightRollResult = null
            ) 
        }
        performSpotlightSearch("", _uiState.value.selectedSpotlightCategory)
    }

    fun closeSpotlight() {
        _uiState.update { it.copy(showSpotlightDialog = false) }
    }

    fun updateSpotlightQuery(query: String) {
        _uiState.update { it.copy(spotlightQuery = query, isSpotlightSearching = true) }
        spotlightJob?.cancel()
        spotlightJob = scope.launch {
            delay(150)
            performSpotlightSearch(query, _uiState.value.selectedSpotlightCategory)
        }
    }

    fun selectSpotlightCategory(category: SpotlightCategory) {
        _uiState.update { it.copy(selectedSpotlightCategory = category, isSpotlightSearching = true) }
        spotlightJob?.cancel()
        spotlightJob = scope.launch {
            performSpotlightSearch(_uiState.value.spotlightQuery, category)
        }
    }

    fun selectSpotlightItem(item: SpotlightItem) {
        _uiState.update { 
            it.copy(
                selectedSpotlightItem = item,
                lastSpotlightRollResult = null
            ) 
        }
    }

    fun rollSpotlightTable(tableId: Long) {
        scope.launch {
            val result = rollTableUseCase(tableId, sessionId, persistResult = true)
            _uiState.update { it.copy(lastSpotlightRollResult = result) }
        }
    }

    fun addSpotlightCreatureToCombat(
        name: String,
        hp: Int,
        ac: Int,
        notes: String,
        npcId: Long? = null,
        imagePath: String? = null,
        initiativeBonus: Int = 0
    ) {
        scope.launch {
            val currentEncounter = _uiState.value.activeEncounter
            val targetEncounterId = if (currentEncounter == null) {
                encounterRepository.saveEncounter(
                    Encounter(
                        name = "Combate de Sesión",
                        linkedSessionId = sessionId,
                        isActive = true,
                        currentRound = 1
                    )
                )
            } else {
                currentEncounter.id
            }

            val maxSort = currentEncounter?.creatures?.maxOfOrNull { it.sortOrder } ?: 0
            val creature = EncounterCreature(
                encounterId = targetEncounterId,
                name = name,
                maxHp = hp,
                currentHp = hp,
                armorClass = ac,
                notes = notes,
                npcId = npcId,
                imagePath = imagePath,
                initiativeBonus = initiativeBonus,
                sortOrder = maxSort + 1
            )
            encounterRepository.addCreature(creature)
            _uiState.update { it.copy(snackbarMessage = "$name sumado al combate") }
        }
    }

    fun addSpotlightNpcToCombat(npcId: Long) {
        scope.launch {
            val npc = npcRepository.getNpcById(npcId) ?: return@launch
            addSpotlightCreatureToCombat(
                name = npc.name,
                hp = npc.maxHp,
                ac = npc.armorClass,
                notes = npc.notes.ifBlank { npc.description },
                npcId = npc.id,
                imagePath = npc.imagePath,
                initiativeBonus = npc.initiativeBonus
            )
        }
    }

    fun copySpotlightToDiary(text: String) {
        scope.launch {
            val currentNotes = _uiState.value.dmNotes
            val newNotes = (currentNotes + "\n\n" + text.trim()).trim()
            _uiState.update { it.copy(dmNotes = newNotes, snackbarMessage = "Copiado al diario") }
            sessionRepository.updateDmNotes(sessionId, newNotes)
        }
    }

    private fun performSpotlightSearch(query: String, category: SpotlightCategory) {
        scope.launch {
            val cleanQuery = query.trim().lowercase()
            val results = mutableListOf<SpotlightItem>()

            // 1. Tablas aleatorias
            if (category == SpotlightCategory.ALL || category == SpotlightCategory.TABLES) {
                runCatching {
                    val allTables = tableRepository.getAllTables().first()
                    val filteredTables = if (cleanQuery.isBlank()) allTables.take(15)
                    else allTables.filter { 
                        it.name.lowercase().contains(cleanQuery) || 
                        it.category.lowercase().contains(cleanQuery) 
                    }
                    filteredTables.forEach { t ->
                        results.add(
                            SpotlightItem.TableResult(
                                tableId = t.id,
                                title = t.name,
                                subtitle = "${t.category} · ${t.rollFormula}",
                                formula = t.rollFormula,
                                rowCount = t.entries.size,
                                description = t.description
                            )
                        )
                    }
                }
            }

            // 2. PNJs
            if (category == SpotlightCategory.ALL || category == SpotlightCategory.NPCS) {
                runCatching {
                    val allNpcs = npcRepository.getAllNpcs().first()
                    val filteredNpcs = if (cleanQuery.isBlank()) allNpcs.take(15)
                    else allNpcs.filter {
                        it.name.lowercase().contains(cleanQuery) ||
                        it.description.lowercase().contains(cleanQuery) ||
                        it.notes.lowercase().contains(cleanQuery)
                    }
                    filteredNpcs.forEach { n ->
                        results.add(
                            SpotlightItem.NpcResult(
                                npcId = n.id,
                                title = n.name,
                                subtitle = if (n.isMonster) "Monstruo / PNJ" else "PNJ",
                                notes = n.description.ifBlank { n.notes },
                                imagePath = n.imagePath
                            )
                        )
                    }
                }
            }

            // 3. Reglas del sistema SRD
            if (category == SpotlightCategory.ALL || category == SpotlightCategory.RULES) {
                runCatching {
                    val allRules = referenceRepository.getAllSystemRules().first()
                    val filteredRules = if (cleanQuery.isBlank()) allRules.take(15)
                    else allRules.filter {
                        it.title.lowercase().contains(cleanQuery) ||
                        it.gameSystem.lowercase().contains(cleanQuery) ||
                        it.category.lowercase().contains(cleanQuery) ||
                        it.content.lowercase().contains(cleanQuery)
                    }
                    filteredRules.forEach { r ->
                        results.add(
                            SpotlightItem.RuleResult(
                                ruleId = r.id,
                                title = r.title,
                                subtitle = "${r.gameSystem} · ${r.category}",
                                content = r.content,
                                gameSystem = r.gameSystem
                            )
                        )
                    }
                }
            }

            // 4. Monstruos y criaturas de encuentros
            if (category == SpotlightCategory.ALL || category == SpotlightCategory.MONSTERS) {
                runCatching {
                    val allEncounters = encounterRepository.getAllEncounters().first()
                    val allCreatures = allEncounters.flatMap { it.creatures }.distinctBy { it.name }
                    val filteredCreatures = if (cleanQuery.isBlank()) allCreatures.take(15)
                    else allCreatures.filter {
                        it.name.lowercase().contains(cleanQuery) ||
                        it.notes.lowercase().contains(cleanQuery)
                    }
                    filteredCreatures.forEach { c ->
                        results.add(
                            SpotlightItem.CreatureResult(
                                creatureId = c.id,
                                title = c.name,
                                subtitle = "HP: ${c.maxHp} · CA: ${c.armorClass}",
                                hp = c.maxHp,
                                ac = c.armorClass,
                                initiativeBonus = c.initiativeBonus,
                                notes = c.notes
                            )
                        )
                    }
                }
            }

            // 5. Diario y notas de la sesión
            if ((category == SpotlightCategory.ALL || category == SpotlightCategory.NOTES) && cleanQuery.isNotBlank()) {
                val notes = _uiState.value.dmNotes
                notes.lines().forEachIndexed { index, line ->
                    if (line.lowercase().contains(cleanQuery) && line.isNotBlank()) {
                        results.add(
                            SpotlightItem.NoteResult(
                                lineNumber = index + 1,
                                title = "Nota L${index + 1}",
                                subtitle = "Diario de Sesión",
                                snippet = line.trim()
                            )
                        )
                    }
                }
            }

            val finalResults = results.take(35)
            val autoSelected = finalResults.firstOrNull()
            _uiState.update { 
                it.copy(
                    spotlightResults = finalResults,
                    selectedSpotlightItem = if (it.selectedSpotlightItem != null && finalResults.any { r -> r.id == it.selectedSpotlightItem.id }) it.selectedSpotlightItem else autoSelected,
                    isSpotlightSearching = false
                ) 
            }
        }
    }
}
