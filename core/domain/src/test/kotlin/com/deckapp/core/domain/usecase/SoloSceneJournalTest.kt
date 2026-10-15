package com.deckapp.core.domain.usecase

import com.deckapp.core.domain.repository.SessionRepository
import com.deckapp.core.model.*
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class SoloSceneJournalTest {
    private val context = SoloSceneContext("Posada del río", "Encontrar al mensajero", "La sala está vacía.\nHay una carta.", "¿Es una trampa?")
    private val scene = Scene(id = 12, sessionId = 4, title = "La carta", content = context.toSceneContent(), sortOrder = 2)

    @Test fun `el contexto multilinea conserva todos los campos al recuperar una escena`() {
        assertEquals(context, scene.soloContext())
    }

    @Test fun `una escena libre del planificador conserva su contenido como situacion`() {
        val original = "Puerta cerrada\n\n## Enemigos\nDos guardias"
        assertEquals(original, scene.copy(content = original).soloContext().situation)
    }

    @Test fun `el diario guarda la pregunta y los valores de la tirada original`() {
        val result = BoardMythicFateResult(ProbabilityLevel.UNLIKELY, 3, 16, 25, FateResult.YES, true,
            randomEventDescription = "Descubrir · Peligro", question = "¿Es una trampa?", sceneTitle = "La carta", sceneId = 12)
        val text = oracleJournalEntry(result)
        assertTrue(text.contains("¿Es una trampa?"))
        assertTrue(text.contains("La carta"))
        assertTrue(text.contains("Improbable · Caos 3 · d100 16/25"))
        assertTrue(text.contains("Descubrir · Peligro"))
    }

    @Test fun `cerrar guarda contexto consecuencias y pendientes y prepara una escena nueva`() = runTest {
        val repository = mockk<SessionRepository>()
        coEvery { repository.closeSceneAndAppendJournal(any(), any(), any(), any()) } returns 13L
        val closure = SoloSceneClosure("Abrí la carta", "El sello me delata", "¿Quién me sigue?", "El callejón", -1)
        val closed = CloseSoloSceneUseCase(repository)(scene, closure)
        assertEquals(13L, closed.nextSceneId)
        assertTrue(closed.journalEntry.contains("Posada del río"))
        assertTrue(closed.journalEntry.contains("El sello me delata"))
        coVerify(exactly = 1) {
            repository.closeSceneAndAppendJournal(12, 4, closed.journalEntry, match<Scene> {
                it.id == 0L && it.sessionId == 4L && it.title == "El callejón" &&
                    it.soloContext().pendingQuestion == "¿Quién me sigue?" && !it.isCompleted
            })
        }
    }

    @Test fun `se puede cerrar sin crear otra escena`() = runTest {
        val repository = mockk<SessionRepository>()
        coEvery { repository.closeSceneAndAppendJournal(12, 4, any(), null) } returns null
        assertNull(CloseSoloSceneUseCase(repository)(scene, SoloSceneClosure("Fin de la escena")).nextSceneId)
    }

    @Test fun `rechaza cierre vacio o repetido antes de escribir datos`() = runTest {
        val repository = mockk<SessionRepository>()
        val close = CloseSoloSceneUseCase(repository)
        assertTrue(runCatching { close(scene, SoloSceneClosure("  ")) }.exceptionOrNull() is IllegalArgumentException)
        assertTrue(runCatching { close(scene.copy(isCompleted = true), SoloSceneClosure("Fin")) }.exceptionOrNull() is IllegalArgumentException)
        assertTrue(runCatching { close(scene, SoloSceneClosure("Fin", chaosDelta = 2)) }.exceptionOrNull() is IllegalArgumentException)
        coVerify(exactly = 0) { repository.closeSceneAndAppendJournal(any(), any(), any(), any()) }
    }

    @Test fun `un fallo al guardar se propaga sin anunciar un cierre exitoso`() = runTest {
        val repository = mockk<SessionRepository>()
        coEvery { repository.closeSceneAndAppendJournal(any(), any(), any(), any()) } throws IllegalStateException("Disco lleno")
        val failure = runCatching { CloseSoloSceneUseCase(repository)(scene, SoloSceneClosure("Fin")) }.exceptionOrNull()
        assertEquals("Disco lleno", failure?.message)
    }
}
