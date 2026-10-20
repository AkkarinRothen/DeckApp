package com.deckapp.feature.draw.delegates

import com.deckapp.core.domain.repository.*
import com.deckapp.core.domain.usecase.RollTableUseCase
import com.deckapp.core.model.*
import com.deckapp.feature.draw.SessionUiState
import com.deckapp.feature.draw.components.spotlight.SpotlightCategory
import com.deckapp.feature.draw.components.spotlight.SpotlightItem
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionSpotlightDelegateTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private val tableRepository: TableRepository = mockk(relaxed = true)
    private val npcRepository: NpcRepository = mockk(relaxed = true)
    private val referenceRepository: ReferenceRepository = mockk(relaxed = true)
    private val encounterRepository: EncounterRepository = mockk(relaxed = true)
    private val rollTableUseCase: RollTableUseCase = mockk(relaxed = true)
    private val sessionRepository: SessionRepository = mockk(relaxed = true)

    private val sessionId = 101L

    private val uiState = MutableStateFlow(
        SessionUiState(
            sessionId = sessionId,
            dmNotes = "Nota existente"
        )
    )

    private lateinit var delegate: SessionSpotlightDelegate

    @Before
    fun setUp() {
        coEvery { tableRepository.getAllTables() } returns flowOf(emptyList())
        coEvery { npcRepository.getAllNpcs() } returns flowOf(emptyList())
        coEvery { referenceRepository.getAllSystemRules() } returns flowOf(emptyList())
        coEvery { encounterRepository.getAllEncounters() } returns flowOf(emptyList())

        delegate = SessionSpotlightDelegate(
            sessionId = sessionId,
            _uiState = uiState,
            scope = testScope,
            tableRepository = tableRepository,
            npcRepository = npcRepository,
            referenceRepository = referenceRepository,
            encounterRepository = encounterRepository,
            rollTableUseCase = rollTableUseCase,
            sessionRepository = sessionRepository
        )
    }

    @Test
    fun `openSpotlight sets dialog visible and resets query`() = testScope.runTest {
        delegate.openSpotlight()
        assertTrue(uiState.value.showSpotlightDialog)
        assertEquals("", uiState.value.spotlightQuery)
        assertNull(uiState.value.selectedSpotlightItem)
    }

    @Test
    fun `closeSpotlight hides dialog`() {
        uiState.value = uiState.value.copy(spotlight = uiState.value.spotlight.copy(showDialog = true))
        delegate.closeSpotlight()
        assertFalse(uiState.value.showSpotlightDialog)
    }


    @Test
    fun `updateSpotlightQuery searches after debounce`() = testScope.runTest {
        val sampleTable = RandomTable(
            id = 1L,
            name = "Encuentros en el Pantano",
            category = "Peligros",
            rollFormula = "1d6",
            entries = listOf(TableEntry(id = 1L, tableId = 1L, minRoll = 1, maxRoll = 6, text = "Sanguijuelas"))
        )
        coEvery { tableRepository.getAllTables() } returns flowOf(listOf(sampleTable))

        delegate.updateSpotlightQuery("pantano")
        assertEquals("pantano", uiState.value.spotlightQuery)

        testDispatcher.scheduler.advanceTimeBy(200)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, uiState.value.spotlightResults.size)
        val result = uiState.value.spotlightResults.first() as SpotlightItem.TableResult
        assertEquals("Encuentros en el Pantano", result.title)
    }

    @Test
    fun `selectSpotlightCategory filters by category`() = testScope.runTest {
        delegate.selectSpotlightCategory(SpotlightCategory.TABLES)
        assertEquals(SpotlightCategory.TABLES, uiState.value.selectedSpotlightCategory)
    }

    @Test
    fun `selectSpotlightItem sets selected item`() {
        val item = SpotlightItem.NoteResult(lineNumber = 1, title = "L1", subtitle = "Diario", snippet = "Un misterio")
        delegate.selectSpotlightItem(item)
        assertEquals(item, uiState.value.selectedSpotlightItem)
        assertNull(uiState.value.lastSpotlightRollResult)
    }

    @Test
    fun `rollSpotlightTable rolls table and updates lastSpotlightRollResult`() = testScope.runTest {
        val rollResult = TableRollResult(
            tableId = 5L,
            tableName = "Tesoro",
            rollValue = 4,
            resolvedText = "100 PO",
            timestamp = 1000L,
            sessionId = sessionId
        )
        coEvery { rollTableUseCase(5L, sessionId, persistResult = true) } returns rollResult

        delegate.rollSpotlightTable(5L)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(rollResult, uiState.value.lastSpotlightRollResult)
        coVerify { rollTableUseCase(5L, sessionId, persistResult = true) }
    }

    @Test
    fun `addSpotlightCreatureToCombat adds creature to encounter`() = testScope.runTest {
        coEvery { encounterRepository.saveEncounter(any()) } returns 77L

        delegate.addSpotlightCreatureToCombat(
            name = "Esqueleto",
            hp = 12,
            ac = 13,
            notes = "Resistente a perforante"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { encounterRepository.addCreature(match { it.name == "Esqueleto" && it.maxHp == 12 }) }
        assertTrue(uiState.value.snackbarMessage?.contains("Esqueleto sumado al combate") == true)
    }

    @Test
    fun `copySpotlightToDiary appends text to dmNotes`() = testScope.runTest {
        delegate.copySpotlightToDiary("Pista encontrada: Huellas en el lodo")
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(uiState.value.dmNotes.contains("Pista encontrada: Huellas en el lodo"))
        coVerify { sessionRepository.updateDmNotes(sessionId, any()) }
    }

    @Test
    fun `spotlightUiState encapsulation and synchronization with legacy getters`() {
        assertFalse(uiState.value.showSpotlightDialog)
        assertFalse(uiState.value.spotlight.showDialog)

        delegate.updateSpotlightState {
            it.copy(
                showDialog = true,
                query = "Reglas de sigilo",
                selectedCategory = SpotlightCategory.RULES,
                isSearching = true
            )
        }

        // Verificar sincronización de getters heredados
        assertTrue(uiState.value.showSpotlightDialog)
        assertEquals("Reglas de sigilo", uiState.value.spotlightQuery)
        assertEquals(SpotlightCategory.RULES, uiState.value.selectedSpotlightCategory)
        assertTrue(uiState.value.isSpotlightSearching)

        // Verificar acceso directo a sub-estado modular
        assertEquals("Reglas de sigilo", uiState.value.spotlight.query)
        assertEquals(SpotlightCategory.RULES, uiState.value.spotlight.selectedCategory)
    }
}

