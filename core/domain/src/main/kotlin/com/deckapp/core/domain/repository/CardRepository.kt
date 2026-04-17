package com.deckapp.core.domain.repository

import com.deckapp.core.model.Card
import com.deckapp.core.model.CardStack
import com.deckapp.core.model.Tag
import kotlinx.coroutines.flow.Flow

interface CardRepository {

    // --- CardStack ---
    fun getAllDecks(): Flow<List<CardStack>>            // solo no-archivados
    fun getArchivedDecks(): Flow<List<CardStack>>
    suspend fun setDeckArchived(deckId: Long, archived: Boolean)
    fun getDeckById(id: Long): Flow<CardStack?>
    suspend fun saveStack(stack: CardStack): Long      // INSERT — solo para stacks nuevos (id=0)
    suspend fun updateStack(stack: CardStack)          // UPDATE — para stacks existentes (no borra cartas)
    suspend fun deleteStack(id: Long)

    // --- Cards ---
    fun getCardsForStack(stackId: Long): Flow<List<Card>>
    fun getCardById(id: Long): Flow<Card?>
    suspend fun saveCard(card: Card): Long
    suspend fun saveCards(cards: List<Card>)
    suspend fun deleteCard(id: Long)
    suspend fun updateCardDrawnState(cardId: Long, isDrawn: Boolean, lastDrawnAt: Long? = null)
    suspend fun updateCardRotation(cardId: Long, rotation: Int)
    suspend fun updateCardReversed(cardId: Long, isReversed: Boolean)
    suspend fun updateCardFaceIndex(cardId: Long, faceIndex: Int)
    suspend fun updateCardRevealed(cardId: Long, isRevealed: Boolean)
    suspend fun updateCardNotes(cardId: Long, notes: String?)
    /** Persiste el nuevo orden de las cartas actualizando el campo sortOrder de cada ID. */
    suspend fun updateCardsSortOrder(orderedIds: List<Long>)
    suspend fun resetDeck(deckId: Long)  // isDrawn=false para todas las cartas del mazo
    fun getDrawnCards(): Flow<List<Card>>  // cartas con isDrawn=true (mano activa)
    fun getPiledCards(sessionId: Long): Flow<List<Card>>  // cartas descartadas en esta sesión
    fun getAvailableCount(stackId: Long): Flow<Int>  // cartas disponibles (isDrawn=false)
    fun getTotalCardCount(stackId: Long): Flow<Int>  // total de cartas del mazo
    suspend fun getTopCard(stackId: Long): Card?      // primera carta disponible sin robarla (Peek)

    // --- Tags ---
    fun getAllTags(): Flow<List<Tag>>
    suspend fun saveTag(tag: Tag): Long
    suspend fun deleteTag(id: Long)
    suspend fun updateTag(tag: Tag)

    // --- Bulk Operations ---
    suspend fun bulkArchiveDecks(ids: List<Long>, archive: Boolean)
    suspend fun bulkDeleteDecks(ids: List<Long>)
    suspend fun bulkAddTagToStacks(stackIds: List<Long>, tagId: Long)
    suspend fun bulkRemoveTagFromStacks(stackIds: List<Long>, tagId: Long)

    // --- Search ---
    fun searchCards(query: String): Flow<List<Card>>
}
