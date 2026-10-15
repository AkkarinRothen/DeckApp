package com.deckapp.core.data.repository

import com.deckapp.core.data.db.CollectionDao
import com.deckapp.core.data.db.TagDao
import com.deckapp.core.data.db.SearchDao
import com.deckapp.core.model.DeckCollection
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class CollectionRepositoryImplTest {
    private val dao = mockk<CollectionDao>()
    private val repository = CollectionRepositoryImpl(dao, mockk<TagDao>(), mockk<SearchDao>())

    @Test fun `editing collection updates in place instead of replacing and cascading resource links`() = runBlocking {
        coEvery { dao.updateCollection(any()) } just Runs
        val collection = DeckCollection(id = 7, name = "Bosque", color = 123, imageUrl = "cover.webp", createdAt = 42)
        assertEquals(7L, repository.saveCollection(collection))
        coVerify(exactly = 1) { dao.updateCollection(match { it.id == 7L && it.imageUrl == "cover.webp" && it.createdAt == 42L }) }
        coVerify(exactly = 0) { dao.insertCollection(any()) }
    }

    @Test fun `new collection returns generated id`() = runBlocking {
        coEvery { dao.insertCollection(any()) } returns 12L
        assertEquals(12L, repository.saveCollection(DeckCollection(name = "Tesoro", color = 123)))
        coVerify(exactly = 0) { dao.updateCollection(any()) }
    }
}
