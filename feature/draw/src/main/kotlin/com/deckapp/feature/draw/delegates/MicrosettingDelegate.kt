package com.deckapp.feature.draw.delegates

import com.deckapp.core.domain.repository.CardRepository
import com.deckapp.core.model.*
import com.deckapp.feature.draw.SessionUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Gestor interactivo para The Story Engine: Deck of Worlds.
 * Administra la generación de microsettings, rotación de cartas (4 cues),
 * redibujo por ranura y sincronización con el tablero de sesión y notas del DM.
 */
class MicrosettingDelegate(
    private val sessionId: Long,
    private val ui: MutableStateFlow<SessionUiState>,
    private val scope: CoroutineScope,
    private val cardRepository: CardRepository,
    private val notesMutex: Mutex,
    private val onAppendToNotes: (String) -> Unit
) {
    private val mutex = Mutex()

    private inline fun updateGuidesState(crossinline transform: (com.deckapp.feature.draw.model.state.GuidesSessionUiState) -> com.deckapp.feature.draw.model.state.GuidesSessionUiState) {
        ui.update { it.copy(guides = transform(it.guides)) }
    }

    fun openBuilder(item: SessionBoardItem? = null) {
        val existingCluster = item?.noteContent?.let { MicrosettingCodec.decode(it) }
        ui.update { state ->
            state.copy(
                modals = state.modals.copy(showMicrosettingBuilder = true),
                guides = state.guides.copy(
                    activeMicrosetting = existingCluster ?: state.activeMicrosetting,
                    editingMicrosettingBoardItemId = item?.id
                )
            )
        }
        if (ui.value.activeMicrosetting == null && existingCluster == null) {
            generateCluster(item?.id)
        }
    }

    fun closeBuilder() {
        ui.update { it.copy(
            modals = it.modals.copy(showMicrosettingBuilder = false),
            guides = it.guides.copy(editingMicrosettingBoardItemId = null)
        ) }
    }


    fun generateCluster(targetItemId: Long? = ui.value.editingMicrosettingBoardItemId) = scope.launch {
        mutex.withLock {
            val allDecks = ui.value.allDecks
            val items = mutableListOf<MicrosettingCardItem>()

            MicrosettingSlotType.entries.forEach { slot ->
                val stack = findStackForSlot(slot, allDecks) ?: return@forEach
                val cards = cardRepository.getCardsForStack(stack.id).first()
                if (cards.isNotEmpty()) {
                    val card = cards.random()
                    val allCues = card.activeFace.zones.map { it.text }
                    val rotations = optionRotationsFor(card.activeFace.contentMode)
                    val rotation = rotations.random()
                    cardRepository.updateCardRotation(card.id, rotation)
                    val activeCue = allCues.getOrNull(rotations.indexOf(rotation)).orEmpty()
                    items += MicrosettingCardItem(
                        slotType = slot,
                        cardId = card.id,
                        stackId = stack.id,
                        title = card.title,
                        imagePath = card.activeFace.imagePath,
                        rotation = rotation,
                        activeCue = activeCue,
                        allCues = allCues
                    )
                }
            }

            val regionTitle = items.firstOrNull { it.slotType == MicrosettingSlotType.REGION }?.title
            val landmarkTitle = items.firstOrNull { it.slotType == MicrosettingSlotType.LANDMARK }?.title
            val title = listOfNotNull(regionTitle, landmarkTitle).joinToString(" — ").ifBlank { "Microsetting de Mundo" }

            val cluster = MicrosettingCluster(
                id = System.currentTimeMillis().toString(),
                title = title,
                items = items,
                updatedAt = System.currentTimeMillis()
            )

            updateGuidesState { it.copy(activeMicrosetting = cluster) }

            // Si hay un bloque del tablero en edición o target, actualizarlo
            if (targetItemId != null) {
                updateBoardItemNote(targetItemId, cluster)
            }
        }
    }

    fun rotateSlot(
        slotType: MicrosettingSlotType,
        targetItemId: Long? = ui.value.editingMicrosettingBoardItemId
    ) = scope.launch {
        mutex.withLock {
            val current = getClusterForTarget(targetItemId) ?: return@withLock
            val updatedItems = current.items.map { item ->
                if (item.slotType == slotType) {
                    val rotations = item.optionRotations
                    val newRotation = rotations[(rotations.indexOf(item.rotation).coerceAtLeast(0) + 1) % rotations.size]
                    val newCue = item.cueAt(newRotation)
                    cardRepository.updateCardRotation(item.cardId, newRotation)
                    item.copy(rotation = newRotation, activeCue = newCue)
                } else {
                    item
                }
            }
            val updatedCluster = current.copy(items = updatedItems, updatedAt = System.currentTimeMillis())
            updateGuidesState { it.copy(activeMicrosetting = updatedCluster) }
            if (targetItemId != null) {
                updateBoardItemNote(targetItemId, updatedCluster)
            }
        }
    }

    fun setSlotRotation(
        slotType: MicrosettingSlotType,
        rotation: Int,
        targetItemId: Long? = ui.value.editingMicrosettingBoardItemId
    ) = scope.launch {
        mutex.withLock {
            val current = getClusterForTarget(targetItemId) ?: return@withLock
            val updatedItems = current.items.map { item ->
                if (item.slotType == slotType) {
                    val normalizedRotation = (rotation % 360 + 360) % 360
                    if (normalizedRotation !in item.optionRotations) item
                    else {
                        cardRepository.updateCardRotation(item.cardId, normalizedRotation)
                        item.copy(rotation = normalizedRotation, activeCue = item.cueAt(normalizedRotation))
                    }
                } else {
                    item
                }
            }
            val updatedCluster = current.copy(items = updatedItems, updatedAt = System.currentTimeMillis())
            updateGuidesState { it.copy(activeMicrosetting = updatedCluster) }
            if (targetItemId != null) {
                updateBoardItemNote(targetItemId, updatedCluster)
            }
        }
    }

    fun redrawSlot(
        slotType: MicrosettingSlotType,
        targetItemId: Long? = ui.value.editingMicrosettingBoardItemId
    ) = scope.launch {
        mutex.withLock {
            val current = getClusterForTarget(targetItemId) ?: return@withLock
            val allDecks = ui.value.allDecks
            val stack = findStackForSlot(slotType, allDecks) ?: return@withLock
            val cards = cardRepository.getCardsForStack(stack.id).first()
            if (cards.isEmpty()) return@withLock

            // Evitar sacar exactamente la misma carta si hay más de 1
            val currentCardId = current.slot(slotType)?.cardId
            val candidates = if (cards.size > 1) cards.filter { it.id != currentCardId } else cards
            val newCard = candidates.random()
            val allCues = newCard.activeFace.zones.map { it.text }
            val rotations = optionRotationsFor(newCard.activeFace.contentMode)
            val rotation = rotations.random()
            cardRepository.updateCardRotation(newCard.id, rotation)
            val activeCue = allCues.getOrNull(rotations.indexOf(rotation)).orEmpty()

            val newItem = MicrosettingCardItem(
                slotType = slotType,
                cardId = newCard.id,
                stackId = stack.id,
                title = newCard.title,
                imagePath = newCard.activeFace.imagePath,
                rotation = rotation,
                activeCue = activeCue,
                allCues = allCues
            )

            val updatedItems = current.items.filterNot { it.slotType == slotType } + newItem
            val updatedCluster = current.copy(
                items = updatedItems.sortedBy { it.slotType.ordinal },
                updatedAt = System.currentTimeMillis()
            )

            updateGuidesState { it.copy(activeMicrosetting = updatedCluster) }
            if (targetItemId != null) {
                updateBoardItemNote(targetItemId, updatedCluster)
            }
        }
    }

    fun copyToNotes(cluster: MicrosettingCluster? = ui.value.activeMicrosetting) = scope.launch {
        val target = cluster ?: return@launch
        val markdown = MicrosettingCodec.formatMarkdown(target)
        notesMutex.withLock {
            onAppendToNotes(markdown)
        }
        ui.update { it.copy(snackbarMessage = "Microsetting copiado a las notas de sesión") }
    }

    fun saveToBoard(
        cluster: MicrosettingCluster? = ui.value.activeMicrosetting,
        targetItemId: Long? = ui.value.editingMicrosettingBoardItemId,
        onAddBoardBlock: (SessionBoardItemType, Long?) -> Unit
    ) = scope.launch {
        val target = cluster ?: return@launch
        val encoded = MicrosettingCodec.encode(target)
        if (targetItemId != null) {
            updateBoardItemNote(targetItemId, target)
            ui.update { it.copy(snackbarMessage = "Microsetting actualizado en el tablero") }
        } else {
            // Añadir nuevo bloque al tablero
            onAddBoardBlock(SessionBoardItemType.MICROSETTING, null)
            // Se sincronizará el contenido en el bloque recién creado
            ui.update { state ->
                val lastItem = state.fullBoardItems.lastOrNull { it.type == SessionBoardItemType.MICROSETTING }
                if (lastItem != null) {
                    val updated = state.fullBoardItems.map {
                        if (it.id == lastItem.id) it.copy(noteContent = encoded) else it
                    }
                    state.copy(
                        fullBoardItems = updated,
                        sessionBoardItems = updated.filter { it.boardView == state.activeBoardView },
                        snackbarMessage = "Microsetting anclado al tablero"
                    )
                } else {
                    state.copy(snackbarMessage = "Microsetting anclado al tablero")
                }
            }
        }
        closeBuilder()
    }

    private fun getClusterForTarget(targetItemId: Long?): MicrosettingCluster? {
        if (targetItemId != null) {
            val item = ui.value.fullBoardItems.firstOrNull { it.id == targetItemId }
            val decoded = item?.noteContent?.let { MicrosettingCodec.decode(it) }
            if (decoded != null) return decoded
        }
        return ui.value.activeMicrosetting
    }

    private fun updateBoardItemNote(itemId: Long, cluster: MicrosettingCluster) {
        val encoded = MicrosettingCodec.encode(cluster)
        val updated = ui.value.fullBoardItems.map { item ->
            if (item.id == itemId) item.copy(noteContent = encoded) else item
        }
        ui.update { state ->
            state.copy(
                fullBoardItems = updated,
                sessionBoardItems = updated.filter { it.boardView == state.activeBoardView }
            )
        }
    }

    private fun findStackForSlot(slot: MicrosettingSlotType, allDecks: List<CardStack>): CardStack? {
        val queries = when (slot) {
            MicrosettingSlotType.REGION -> listOf("region", "región", "regiones")
            MicrosettingSlotType.LANDMARK -> listOf("landmark", "interés", "interes", "hito", "puntos")
            MicrosettingSlotType.NAMESAKE -> listOf("namesake", "nombre", "apelativo", "nombres")
            MicrosettingSlotType.ORIGIN -> listOf("origin", "origen", "orígenes", "historia")
            MicrosettingSlotType.ATTRIBUTE -> listOf("attribute", "atributo", "atributos", "cultura")
            MicrosettingSlotType.ADVENT -> listOf("advent", "acontecimiento", "acontecimientos", "gancho")
        }
        return allDecks.firstOrNull { deck ->
            val n = deck.name.lowercase()
            n.contains("deck of worlds") && queries.any { n.contains(it) }
        } ?: allDecks.firstOrNull { deck ->
            val n = deck.name.lowercase()
            queries.any { n.contains(it) }
        }
    }

    private fun optionRotationsFor(mode: CardContentMode): List<Int> = when (mode) {
        CardContentMode.TOP_BOTTOM_SPLIT, CardContentMode.LEFT_RIGHT_SPLIT -> listOf(0, 180)
        CardContentMode.FOUR_EDGE_CUES, CardContentMode.FOUR_QUADRANT -> listOf(0, 90, 180, 270)
        else -> listOf(0)
    }
}
