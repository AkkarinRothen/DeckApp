package com.deckapp.core.data.repository

import com.deckapp.core.data.db.*
import com.deckapp.core.domain.repository.CardRepository
import com.deckapp.core.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CardRepositoryImpl @Inject constructor(
    private val stackDao: CardStackDao,
    private val cardDao: CardDao,
    private val faceDao: CardFaceDao,
    private val tagDao: TagDao,
    private val searchDao: SearchDao
) : CardRepository {

    override fun getAllDecks(): Flow<List<CardStack>> =
        stackDao.getAllDecks().map { entities ->
            entities.map { entity ->
                entity.toDomain(tags = tagDao.getTagsForStack(entity.id).map { it.toDomain() })
            }
        }

    override fun getArchivedDecks(): Flow<List<CardStack>> =
        stackDao.getArchivedDecks().map { entities ->
            entities.map { entity ->
                entity.toDomain(tags = tagDao.getTagsForStack(entity.id).map { it.toDomain() })
            }
        }

    override suspend fun setDeckArchived(deckId: Long, archived: Boolean) =
        stackDao.setArchived(deckId, archived)

    override fun getDeckById(id: Long): Flow<CardStack?> =
        stackDao.getStackById(id).map { entity ->
            entity?.toDomain(tags = tagDao.getTagsForStack(entity.id).map { it.toDomain() })
        }

    override suspend fun saveStack(stack: CardStack): Long {
        val id = stackDao.insertStack(stack.toEntity())
        tagDao.deleteTagsForStack(id)
        stack.tags.forEach { tag ->
            val tagId = tagDao.insertTag(tag.toEntity())
            tagDao.insertStackTagRef(CardStackTagCrossRef(id, tagId))
        }
        return id
    }

    /**
     * Actualiza un stack existente SIN borrar sus cartas.
     * Usa @Update (UPDATE SQL) en lugar de @Insert(REPLACE) para no disparar
     * el CASCADE DELETE sobre la tabla cards.
     */
    override suspend fun updateStack(stack: CardStack) {
        stackDao.updateStack(stack.toEntity())
        tagDao.deleteTagsForStack(stack.id)
        stack.tags.forEach { tag ->
            val tagId = tagDao.insertTag(tag.toEntity())
            tagDao.insertStackTagRef(CardStackTagCrossRef(stack.id, tagId))
        }
    }

    override suspend fun deleteStack(id: Long) = stackDao.deleteStack(id)

    override fun getCardsForStack(stackId: Long): Flow<List<Card>> =
        cardDao.getCardsForStack(stackId).map { cards ->
            cards.map { cardEntity ->
                val faces = faceDao.getFacesForCardSync(cardEntity.id).map { it.toDomain() }
                val tags = tagDao.getTagsForCard(cardEntity.id).map { it.toDomain() }
                cardEntity.toDomain(faces = faces, tags = tags)
            }
        }

    override fun getCardById(id: Long): Flow<Card?> =
        cardDao.getCardById(id).map { cardEntity ->
            cardEntity?.let {
                val faces = faceDao.getFacesForCardSync(it.id).map { f -> f.toDomain() }
                val tags = tagDao.getTagsForCard(it.id).map { t -> t.toDomain() }
                it.toDomain(faces = faces, tags = tags)
            }
        }

    override suspend fun saveCard(card: Card): Long {
        val cardId = cardDao.insertCard(card.toEntity())
        faceDao.deleteFacesForCard(cardId)
        card.faces.forEachIndexed { index, face ->
            faceDao.insertFace(face.toEntity(cardId, index))
        }
        tagDao.deleteTagsForCard(cardId)
        card.tags.forEach { tag ->
            val tagId = tagDao.insertTag(tag.toEntity())
            tagDao.insertCardTagRef(CardTagCrossRef(cardId, tagId))
        }
        return cardId
    }

    override suspend fun saveCards(cards: List<Card>) = cards.forEach { saveCard(it) }

    override suspend fun deleteCard(id: Long) = cardDao.deleteCard(id)

    override suspend fun updateCardDrawnState(cardId: Long, isDrawn: Boolean, lastDrawnAt: Long?) =
        cardDao.updateDrawnState(cardId, isDrawn, lastDrawnAt)

    override suspend fun updateCardRotation(cardId: Long, rotation: Int) =
        cardDao.updateRotation(cardId, rotation)

    override suspend fun updateCardReversed(cardId: Long, isReversed: Boolean) =
        cardDao.updateReversed(cardId, isReversed)

    override suspend fun updateCardFaceIndex(cardId: Long, faceIndex: Int) =
        cardDao.updateFaceIndex(cardId, faceIndex)

    override suspend fun updateCardRevealed(cardId: Long, isRevealed: Boolean) =
        cardDao.updateRevealed(cardId, isRevealed)

    override suspend fun updateCardNotes(cardId: Long, notes: String?) =
        cardDao.updateDmNotes(cardId, notes)

    override suspend fun updateCardsSortOrder(orderedIds: List<Long>) {
        orderedIds.forEachIndexed { index, id ->
            cardDao.updateSortOrder(id, index)
        }
    }

    override suspend fun resetDeck(deckId: Long) = cardDao.resetDeck(deckId)

    override suspend fun getTopCard(stackId: Long): Card? {
        val entity = cardDao.getTopCard(stackId) ?: return null
        val faces = faceDao.getFacesForCardSync(entity.id).map { it.toDomain() }
        val tags = tagDao.getTagsForCard(entity.id).map { it.toDomain() }
        return entity.toDomain(faces = faces, tags = tags)
    }

    override fun getDrawnCards(): Flow<List<Card>> =
        cardDao.getDrawnCards().map { cards ->
            cards.map { cardEntity ->
                val faces = faceDao.getFacesForCardSync(cardEntity.id).map { it.toDomain() }
                val tags = tagDao.getTagsForCard(cardEntity.id).map { it.toDomain() }
                cardEntity.toDomain(faces = faces, tags = tags)
            }
        }

    override fun getPiledCards(sessionId: Long): Flow<List<Card>> =
        cardDao.getPiledCards(sessionId).map { cards ->
            cards.map { cardEntity ->
                val faces = faceDao.getFacesForCardSync(cardEntity.id).map { it.toDomain() }
                val tags = tagDao.getTagsForCard(cardEntity.id).map { it.toDomain() }
                cardEntity.toDomain(faces = faces, tags = tags)
            }
        }

    override fun getAvailableCount(stackId: Long): Flow<Int> =
        cardDao.getAvailableCount(stackId)

    override fun getTotalCardCount(stackId: Long): Flow<Int> =
        cardDao.getTotalCardCount(stackId)

    override fun getAllTags(): Flow<List<Tag>> =
        tagDao.getAllTags().map { it.map { t -> t.toDomain() } }

    override suspend fun saveTag(tag: Tag): Long = tagDao.insertTag(tag.toEntity())

    override suspend fun deleteTag(id: Long) = tagDao.deleteTag(id)

    override suspend fun updateTag(tag: Tag) {
        tagDao.insertTag(tag.toEntity()) // insertTag usa REPLACE on conflict, sirve para update
    }

    override suspend fun bulkArchiveDecks(ids: List<Long>, archive: Boolean) {
        stackDao.bulkSetArchived(ids, archive)
    }

    override suspend fun bulkDeleteDecks(ids: List<Long>) {
        stackDao.bulkDeleteStacks(ids)
    }

    override suspend fun bulkAddTagToStacks(stackIds: List<Long>, tagId: Long) {
        stackIds.forEach { stackId ->
            tagDao.insertStackTagRef(CardStackTagCrossRef(stackId, tagId))
        }
    }

    override suspend fun bulkRemoveTagFromStacks(stackIds: List<Long>, tagId: Long) {
        stackIds.forEach { stackId ->
            tagDao.removeStackTagRef(stackId, tagId)
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    override fun searchCards(query: String): Flow<List<Card>> {
        if (query.isBlank()) return kotlinx.coroutines.flow.flowOf(emptyList())
        val ftsQuery = "$query*"
        
        return combine(
            searchDao.searchCardIds(ftsQuery),
            searchDao.searchCardFaceIds(ftsQuery)
        ) { cardIds, faceIds ->
            (cardIds.map { it.rowid } + faceIds.map { it.rowid }).distinct()
        }.flatMapLatest { ids: List<Long> ->
            if (ids.isEmpty()) kotlinx.coroutines.flow.flowOf(emptyList<Card>())
            else {
                val flows = ids.map { id -> getCardById(id) }
                combine(flows) { cards: Array<Card?> ->
                    cards.filterNotNull()
                }
            }
        }
    }
}
