package com.deckapp.feature.draw.delegates

import com.deckapp.core.domain.repository.SessionRepository
import com.deckapp.core.model.*
import com.deckapp.feature.draw.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

/**
 * Delegado encargado de toda la lógica del Workspace Bento (Tableros, bloques, vistas y grupos).
 * Extraído de SessionViewModel para reducir su complejidad y aislar la manipulación de la grilla.
 */
class BoardDelegate(
    private val sessionId: Long,
    private val _uiState: MutableStateFlow<SessionUiState>,
    private val scope: CoroutineScope,
    private val sessionRepository: SessionRepository
) {
    private val noteWriteMutex = Mutex()

    init {
        // Inicializar bloques por defecto si el tablero está vacío
        if (_uiState.value.fullBoardItems.isEmpty()) {
            val defaultItems = listOf(
                SessionBoardItem(
                    sessionId = sessionId,
                    type = SessionBoardItemType.NOTES,
                    position = 0,
                    size = SessionBoardItemSize.M,
                    displayMode = "compact",
                    boardView = SessionBoardView.MAIN
                ),
                SessionBoardItem(
                    sessionId = sessionId,
                    type = SessionBoardItemType.TABLE,
                    position = 1,
                    size = SessionBoardItemSize.M,
                    displayMode = "live",
                    boardView = SessionBoardView.MAIN
                ),
                SessionBoardItem(
                    sessionId = sessionId,
                    type = SessionBoardItemType.COMBAT,
                    position = 2,
                    size = SessionBoardItemSize.L,
                    displayMode = "live",
                    boardView = SessionBoardView.MAIN
                ),
                SessionBoardItem(
                    sessionId = sessionId,
                    type = SessionBoardItemType.DICE,
                    position = 3,
                    size = SessionBoardItemSize.S,
                    boardView = SessionBoardView.MAIN
                ),
                SessionBoardItem(
                    sessionId = sessionId,
                    type = SessionBoardItemType.NPCS,
                    position = 4,
                    size = SessionBoardItemSize.M,
                    boardView = SessionBoardView.MAIN
                ),
                SessionBoardItem(
                    sessionId = sessionId,
                    type = SessionBoardItemType.MAP,
                    position = 5,
                    size = SessionBoardItemSize.L,
                    displayMode = "live",
                    boardView = SessionBoardView.MAIN
                )
            )
            _uiState.update { state ->
                state.copy(
                    fullBoardItems = defaultItems,
                    sessionBoardItems = defaultItems.forActiveBoardView(state.activeBoardView)
                )
            }
        }
    }

    fun updateBlockNote(itemId: Long, content: String) {
        val item = _uiState.value.fullBoardItems.find { it.id == itemId } ?: return
        if (item.type != SessionBoardItemType.NOTES || item.sessionId != sessionId) return
        scope.launch {
            noteWriteMutex.lock()
            try {
                val updated = _uiState.value.fullBoardItems.map {
                    if (it.id == itemId) it.copy(noteContent = content, updatedAt = System.currentTimeMillis()) else it
                }
                _uiState.update { it.copy(fullBoardItems = updated, sessionBoardItems = updated.forActiveBoardView(it.activeBoardView)) }
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _uiState.update { it.copy(snackbarMessage = "No se pudo guardar la nota del bloque") }
            } finally {
                noteWriteMutex.unlock()
            }
        }
    }

    fun addBoardBlock(
        type: SessionBoardItemType,
        resourceIdOverride: Long? = null,
        targetGroupId: Long? = null
    ) {
        scope.launch {
            val currentItems = currentBoardItems()
            val resourceId = resourceIdOverride ?: when (type) {
                SessionBoardItemType.TABLE -> {
                    val usedIds = currentItems
                        .filter { it.type == SessionBoardItemType.TABLE }
                        .mapNotNull { it.resourceId }
                        .toSet()
                    _uiState.value.tablesInSession.firstOrNull { it.id !in usedIds }?.id
                        ?: _uiState.value.allTables.firstOrNull { it.id !in usedIds }?.id
                }
                SessionBoardItemType.DECK -> {
                    val usedIds = currentItems
                        .filter { it.type == SessionBoardItemType.DECK }
                        .mapNotNull { it.resourceId }
                        .toSet()
                    _uiState.value.deckRefs.firstOrNull { it.stackId !in usedIds }?.stackId
                        ?: _uiState.value.allDecks.firstOrNull { it.id !in usedIds }?.id
                }
                SessionBoardItemType.CLOCK -> {
                    val usedIds = currentItems
                        .filter { it.type == SessionBoardItemType.CLOCK }
                        .mapNotNull { it.resourceId }
                        .toSet()
                    _uiState.value.tensionClocks.firstOrNull { it.id !in usedIds }?.id
                }
                SessionBoardItemType.WIKI_MAP -> {
                    val usedIds = currentItems
                        .filter { it.type == SessionBoardItemType.WIKI_MAP }
                        .mapNotNull { it.resourceId }
                        .toSet()
                    _uiState.value.wikiEntries.firstOrNull { it.id !in usedIds && !it.imagePath.isNullOrBlank() }?.id
                }
                SessionBoardItemType.MAP -> {
                    _uiState.value.linkedHexMapId ?: _uiState.value.allHexMaps.firstOrNull()?.id
                }
                SessionBoardItemType.MYTHIC -> {
                    _uiState.value.linkedMythicSession?.id ?: _uiState.value.allMythicSessions.firstOrNull()?.id
                }
                else -> null
            }

            val newItemId = (System.currentTimeMillis() % 1000000) + (_uiState.value.fullBoardItems.maxOfOrNull { it.id } ?: 0L) + 1L
            val item = SessionBoardItem(
                id = newItemId,
                sessionId = sessionId,
                type = type,
                resourceId = resourceId,
                position = currentItems.size,
                size = defaultBoardItemSize(type),
                displayMode = defaultBoardDisplayMode(type),
                boardView = currentBoardView(),
                groupId = targetGroupId
            )
            val updated = _uiState.value.fullBoardItems + item
            _uiState.update { state ->
                state.copy(
                    fullBoardItems = updated,
                    sessionBoardItems = updated.forActiveBoardView(state.activeBoardView)
                )
            }
        }
    }

    fun prepareSceneResources(scene: Scene) {
        scope.launch {
            val now = System.currentTimeMillis()
            val currentItems = currentBoardItems()
                .sortedWith(compareBy<SessionBoardItem> { it.position }.thenBy { it.id })
            val nextItems = currentItems.toMutableList()

            fun hasBlock(type: SessionBoardItemType, resourceId: Long? = null): Boolean =
                nextItems.any { item ->
                    item.type == type && (resourceId == null || item.resourceId == resourceId)
                }

            fun addBlock(
                type: SessionBoardItemType,
                resourceId: Long? = null,
                titleOverride: String? = null,
                size: SessionBoardItemSize = defaultBoardItemSize(type),
                displayMode: String? = defaultBoardDisplayMode(type)
            ) {
                if (hasBlock(type, resourceId)) return
                nextItems += SessionBoardItem(
                    id = System.currentTimeMillis() + nextItems.size,
                    sessionId = sessionId,
                    type = type,
                    resourceId = resourceId,
                    titleOverride = titleOverride,
                    position = nextItems.size,
                    size = size,
                    displayMode = displayMode,
                    boardView = currentBoardView()
                )
            }

            scene.linkedTableId?.let { tableId ->
                addBlock(
                    type = SessionBoardItemType.TABLE,
                    resourceId = tableId,
                    size = SessionBoardItemSize.M,
                    displayMode = "live"
                )
            }

            scene.linkedDeckId?.let { deckId ->
                addBlock(
                    type = SessionBoardItemType.DECK,
                    resourceId = deckId,
                    size = SessionBoardItemSize.M,
                    displayMode = "compact"
                )
            }

            if (nextItems.size == currentItems.size) {
                _uiState.update { it.copy(snackbarMessage = "La escena ya está preparada en la mesa") }
                return@launch
            }

            replaceCurrentBoardViewItems(nextItems, now)
            _uiState.update { it.copy(snackbarMessage = "Mesa preparada para: ${scene.title}") }
        }
    }

    fun togglePinBlock(itemId: Long, allBoardItems: List<SessionBoardItem> = _uiState.value.fullBoardItems) {
        val item = allBoardItems.find { it.id == itemId } ?: return
        val updatedItem = item.copy(isPinned = !item.isPinned)
        val updatedList = allBoardItems.map { if (it.id == itemId) updatedItem else it }
        _uiState.update { state ->
            state.copy(
                sessionBoardItems = updatedList.forActiveBoardView(state.activeBoardView),
                fullBoardItems = updatedList
            )
        }
    }

    fun toggleCollapseBlock(itemId: Long, allBoardItems: List<SessionBoardItem> = _uiState.value.fullBoardItems) {
        val item = allBoardItems.find { it.id == itemId } ?: return
        val updatedItem = item.copy(isCollapsed = !item.isCollapsed)
        val updatedList = allBoardItems.map { if (it.id == itemId) updatedItem else it }
        _uiState.update { state ->
            state.copy(
                sessionBoardItems = updatedList.forActiveBoardView(state.activeBoardView),
                fullBoardItems = updatedList
            )
        }
    }

    fun setAllBlocksCollapsedInView(
        boardView: SessionBoardView,
        collapsed: Boolean,
        sessionId: Long = this.sessionId,
        allBoardItems: List<SessionBoardItem> = _uiState.value.fullBoardItems
    ) {
        val updatedList = allBoardItems.map {
            if (it.boardView == boardView) it.copy(isCollapsed = collapsed) else it
        }
        _uiState.update { state ->
            state.copy(
                sessionBoardItems = updatedList.forActiveBoardView(state.activeBoardView),
                fullBoardItems = updatedList
            )
        }
    }

    fun removeBoardBlock(itemId: Long) {
        scope.launch {
            val updated = _uiState.value.fullBoardItems.filterNot { it.id == itemId }
            _uiState.update { state ->
                state.copy(
                    fullBoardItems = updated,
                    sessionBoardItems = updated.forActiveBoardView(state.activeBoardView)
                )
            }
            normalizeBoardPositions()
        }
    }

    fun moveBoardBlock(itemId: Long, direction: Int) {
        scope.launch {
            val viewItems = currentBoardItems().toMutableList()
            val index = viewItems.indexOfFirst { it.id == itemId }
            if (index == -1) return@launch
            val targetIndex = (index + direction).coerceIn(0, viewItems.size - 1)
            if (index == targetIndex) return@launch
            val item = viewItems.removeAt(index)
            viewItems.add(targetIndex, item)
            replaceCurrentBoardViewItems(viewItems)
        }
    }

    fun moveBoardBlockTo(itemId: Long, targetItemId: Long) {
        scope.launch {
            val viewItems = currentBoardItems().toMutableList()
            val fromIndex = viewItems.indexOfFirst { it.id == itemId }
            val toIndex = viewItems.indexOfFirst { it.id == targetItemId }
            if (fromIndex == -1 || toIndex == -1 || fromIndex == toIndex) return@launch
            val item = viewItems.removeAt(fromIndex)
            viewItems.add(toIndex, item)
            replaceCurrentBoardViewItems(viewItems)
        }
    }

    fun setBoardBlockSize(itemId: Long, size: SessionBoardItemSize) {
        scope.launch {
            val updated = _uiState.value.fullBoardItems.map {
                if (it.id == itemId) it.copy(size = size, updatedAt = System.currentTimeMillis()) else it
            }
            _uiState.update { state ->
                state.copy(
                    fullBoardItems = updated,
                    sessionBoardItems = updated.forActiveBoardView(state.activeBoardView)
                )
            }
        }
    }

    fun setBoardBlockDisplayMode(itemId: Long, displayMode: String?) {
        scope.launch {
            val updated = _uiState.value.fullBoardItems.map {
                if (it.id == itemId) it.copy(displayMode = displayMode, updatedAt = System.currentTimeMillis()) else it
            }
            _uiState.update { state ->
                state.copy(
                    fullBoardItems = updated,
                    sessionBoardItems = updated.forActiveBoardView(state.activeBoardView)
                )
            }
        }
    }

    fun setBoardBlockTitleOverride(itemId: Long, titleOverride: String?) {
        scope.launch {
            val updated = _uiState.value.fullBoardItems.map {
                if (it.id == itemId) it.copy(titleOverride = titleOverride, updatedAt = System.currentTimeMillis()) else it
            }
            _uiState.update { state ->
                state.copy(
                    fullBoardItems = updated,
                    sessionBoardItems = updated.forActiveBoardView(state.activeBoardView)
                )
            }
        }
    }

    fun setBoardBlockResourceId(itemId: Long, resourceId: Long?) {
        scope.launch {
            val updated = _uiState.value.fullBoardItems.map {
                if (it.id == itemId) it.copy(resourceId = resourceId, updatedAt = System.currentTimeMillis()) else it
            }
            _uiState.update { state ->
                state.copy(
                    fullBoardItems = updated,
                    sessionBoardItems = updated.forActiveBoardView(state.activeBoardView)
                )
            }
        }
    }

    fun setActiveBoardView(view: SessionBoardView) {
        _uiState.update { state ->
            state.copy(
                activeBoardView = view,
                sessionBoardItems = state.fullBoardItems.forActiveBoardView(view)
            )
        }
    }

    fun duplicateBoardView(targetView: SessionBoardView) {
        scope.launch {
            val currentItems = currentBoardItems()
            val duplicated = currentItems.map { item ->
                item.copy(
                    id = System.currentTimeMillis() + (1..1000).random(),
                    boardView = targetView,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            }
            val filtered = _uiState.value.fullBoardItems.filterNot { it.boardView == targetView }
            val merged = filtered + duplicated
            _uiState.update { state ->
                state.copy(
                    fullBoardItems = merged,
                    sessionBoardItems = merged.forActiveBoardView(state.activeBoardView),
                    snackbarMessage = "Vista duplicada a ${boardViewLabel(targetView)}"
                )
            }
        }
    }

    fun updateBoardViewLabel(boardView: SessionBoardView, label: String) {
        val updatedLabels = _uiState.value.viewLabels.withLabel(boardView, label)
        _uiState.update { state ->
            state.copy(viewLabels = updatedLabels)
        }
    }

    fun updateBoardViewLabels(labels: SessionBoardViewLabels) {
        _uiState.update { state ->
            state.copy(viewLabels = labels)
        }
    }

    fun clearBoardView(boardView: SessionBoardView) {
        scope.launch {
            val filtered = _uiState.value.fullBoardItems.filterNot { it.boardView == boardView }
            _uiState.update { state ->
                state.copy(
                    fullBoardItems = filtered,
                    sessionBoardItems = filtered.forActiveBoardView(state.activeBoardView),
                    snackbarMessage = "Vista ${boardViewLabel(boardView)} limpiada"
                )
            }
        }
    }

    fun applyBoardTemplate(template: SessionBoardTemplatePreset) {
        scope.launch {
            val specs = boardTemplateSpecs(template)
            val currentView = currentBoardView()
            val newItems = specs.mapIndexed { index, spec ->
                SessionBoardItem(
                    id = System.currentTimeMillis() + index,
                    sessionId = sessionId,
                    type = spec.type,
                    position = index,
                    size = spec.size,
                    displayMode = spec.displayMode,
                    boardView = currentView
                )
            }
            val filtered = _uiState.value.fullBoardItems.filterNot { it.boardView == currentView }
            val merged = filtered + newItems
            _uiState.update { state ->
                state.copy(
                    fullBoardItems = merged,
                    sessionBoardItems = newItems,
                    snackbarMessage = "Plantilla ${template.name} aplicada"
                )
            }
        }
    }

    fun createBoardGroup(name: String, colorHex: String?, iconName: String?, notes: String?) {
        scope.launch {
            val newGroup = SessionBoardGroup(
                id = System.currentTimeMillis(),
                sessionId = sessionId,
                boardView = currentBoardView(),
                name = name,
                accentColorHex = colorHex,
                iconName = iconName,
                position = _uiState.value.boardGroups.size
            )
            _uiState.update { state ->
                state.copy(boardGroups = state.boardGroups + newGroup)
            }
        }
    }

    fun renameBoardGroup(groupId: Long, name: String, colorHex: String?, iconName: String?, notes: String?) {
        scope.launch {
            val updated = _uiState.value.boardGroups.map {
                if (it.id == groupId) it.copy(name = name, accentColorHex = colorHex, iconName = iconName) else it
            }
            _uiState.update { state ->
                state.copy(boardGroups = updated)
            }
        }
    }

    fun deleteBoardGroup(groupId: Long) {
        scope.launch {
            val updatedGroups = _uiState.value.boardGroups.filterNot { it.id == groupId }
            val updatedItems = _uiState.value.fullBoardItems.map {
                if (it.groupId == groupId) it.copy(groupId = null) else it
            }
            _uiState.update { state ->
                state.copy(
                    boardGroups = updatedGroups,
                    fullBoardItems = updatedItems,
                    sessionBoardItems = updatedItems.forActiveBoardView(state.activeBoardView)
                )
            }
        }
    }

    fun toggleBoardGroupCollapsed(groupId: Long, collapsed: Boolean) {
        val updated = _uiState.value.boardGroups.map {
            if (it.id == groupId) it.copy(isCollapsed = collapsed) else it
        }
        _uiState.update { state ->
            state.copy(boardGroups = updated)
        }
    }

    fun assignItemToGroup(itemId: Long, groupId: Long?) {
        scope.launch {
            val updated = _uiState.value.fullBoardItems.map {
                if (it.id == itemId) it.copy(groupId = groupId, updatedAt = System.currentTimeMillis()) else it
            }
            _uiState.update { state ->
                state.copy(
                    fullBoardItems = updated,
                    sessionBoardItems = updated.forActiveBoardView(state.activeBoardView)
                )
            }
        }
    }

    fun mergeBoardItems(sourceId: Long, targetId: Long, onGroupCreated: (SessionBoardGroup) -> Unit) {
        scope.launch {
            val targetItem = _uiState.value.fullBoardItems.find { it.id == targetId } ?: return@launch
            val newGroupId = System.currentTimeMillis()
            val createdGroup = SessionBoardGroup(
                id = newGroupId,
                sessionId = sessionId,
                boardView = currentBoardView(),
                name = targetItem.titleOverride ?: "Grupo de bloques",
                position = _uiState.value.boardGroups.size
            )
            val updatedGroups = _uiState.value.boardGroups + createdGroup
            val updatedItems = _uiState.value.fullBoardItems.map {
                if (it.id == sourceId || it.id == targetId) it.copy(groupId = newGroupId, updatedAt = System.currentTimeMillis()) else it
            }
            _uiState.update { state ->
                state.copy(
                    boardGroups = updatedGroups,
                    fullBoardItems = updatedItems,
                    sessionBoardItems = updatedItems.forActiveBoardView(state.activeBoardView)
                )
            }
            normalizeBoardPositions()
            onGroupCreated(createdGroup)
        }
    }

    private suspend fun normalizeBoardPositions() {
        val normalized = currentBoardItems()
            .sortedWith(compareBy<SessionBoardItem> { it.position }.thenBy { it.id })
            .mapIndexed { index, item ->
                item.copy(position = index, updatedAt = System.currentTimeMillis())
            }
        replaceCurrentBoardViewItems(normalized)
    }

    private fun currentBoardView(): SessionBoardView = _uiState.value.activeBoardView

    private fun currentBoardItems(): List<SessionBoardItem> = 
        _uiState.value.fullBoardItems.forActiveBoardView(currentBoardView())

    private suspend fun replaceCurrentBoardViewItems(
        items: List<SessionBoardItem>,
        timestamp: Long = System.currentTimeMillis()
    ) {
        val activeView = currentBoardView()
        val merged = _uiState.value.fullBoardItems
            .filterNot { it.boardView == activeView }
            .plus(
                items.mapIndexed { index, item ->
                    item.copy(
                        position = index,
                        boardView = activeView,
                        updatedAt = timestamp
                    )
                }
            )
        _uiState.update { it.copy(fullBoardItems = merged, sessionBoardItems = items) }
    }

    private fun boardViewLabel(boardView: SessionBoardView): String = when (boardView) {
        SessionBoardView.MAIN -> "Principal"
        SessionBoardView.EXPLORATION -> "Exploracion"
        SessionBoardView.NARRATIVE -> "Narrativa"
        SessionBoardView.COMBAT -> "Combate"
    }

    private fun defaultBoardItemSize(type: SessionBoardItemType): SessionBoardItemSize = when (type) {
        SessionBoardItemType.MAP,
        SessionBoardItemType.WIKI_MAP,
        SessionBoardItemType.MYTHIC,
        SessionBoardItemType.COMBAT -> SessionBoardItemSize.L
        SessionBoardItemType.DICE -> SessionBoardItemSize.S
        else -> SessionBoardItemSize.M
    }

    private fun defaultBoardDisplayMode(type: SessionBoardItemType): String? = when (type) {
        SessionBoardItemType.TABLE -> "live"
        SessionBoardItemType.NOTES -> "compact"
        SessionBoardItemType.MYTHIC -> "compact"
        SessionBoardItemType.MAP,
        SessionBoardItemType.WIKI_MAP -> "live"
        SessionBoardItemType.COMBAT -> "live"
        else -> null
    }

    private data class BoardTemplateSpec(
        val type: SessionBoardItemType,
        val size: SessionBoardItemSize,
        val displayMode: String? = null
    )

    private fun boardTemplateSpecs(template: SessionBoardTemplatePreset): List<BoardTemplateSpec> = when (template) {
        SessionBoardTemplatePreset.EXPLORATION -> listOf(
            BoardTemplateSpec(SessionBoardItemType.MAP, SessionBoardItemSize.L, "live"),
            BoardTemplateSpec(SessionBoardItemType.TABLE, SessionBoardItemSize.M, "live"),
            BoardTemplateSpec(SessionBoardItemType.NOTES, SessionBoardItemSize.M, "compact"),
            BoardTemplateSpec(SessionBoardItemType.REFERENCE, SessionBoardItemSize.M, "context"),
            BoardTemplateSpec(SessionBoardItemType.DICE, SessionBoardItemSize.S, null),
            BoardTemplateSpec(SessionBoardItemType.NPCS, SessionBoardItemSize.M, null)
        )
        SessionBoardTemplatePreset.NARRATIVE -> listOf(
            BoardTemplateSpec(SessionBoardItemType.NOTES, SessionBoardItemSize.L, "expanded"),
            BoardTemplateSpec(SessionBoardItemType.MYTHIC, SessionBoardItemSize.L, "compact"),
            BoardTemplateSpec(SessionBoardItemType.REFERENCE, SessionBoardItemSize.M, "context"),
            BoardTemplateSpec(SessionBoardItemType.NPCS, SessionBoardItemSize.M, null),
            BoardTemplateSpec(SessionBoardItemType.TABLE, SessionBoardItemSize.M, "quick"),
            BoardTemplateSpec(SessionBoardItemType.DICE, SessionBoardItemSize.S, null)
        )
        SessionBoardTemplatePreset.COMBAT -> listOf(
            BoardTemplateSpec(SessionBoardItemType.COMBAT, SessionBoardItemSize.L, "live"),
            BoardTemplateSpec(SessionBoardItemType.TABLE, SessionBoardItemSize.M, "live"),
            BoardTemplateSpec(SessionBoardItemType.DICE, SessionBoardItemSize.S, null),
            BoardTemplateSpec(SessionBoardItemType.NOTES, SessionBoardItemSize.M, "compact"),
            BoardTemplateSpec(SessionBoardItemType.NPCS, SessionBoardItemSize.M, null),
            BoardTemplateSpec(SessionBoardItemType.REFERENCE, SessionBoardItemSize.M, "search")
        )
    }

    private fun List<SessionBoardItem>.forActiveBoardView(boardView: SessionBoardView): List<SessionBoardItem> =
        filter { it.boardView == boardView }
            .sortedWith(compareBy<SessionBoardItem> { it.position }.thenBy { it.id })
}
