package com.deckapp.core.domain.usecase

import com.deckapp.core.domain.repository.MythicRepository
import com.deckapp.core.domain.repository.SessionRepository
import com.deckapp.core.model.FateResult
import com.deckapp.core.model.MythicRoll
import com.deckapp.core.model.ProbabilityLevel
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

class AddQuickNoteUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val mythicRepository: MythicRepository
) {
    suspend operator fun invoke(
        content: String,
        sessionId: Long? = null,
        mythicSessionId: Long? = null
    ) {
        if (content.isBlank()) return

        // 1. Si hay una sesión Mythic activa o especificada, guardar en su log
        val targetMythicId = mythicSessionId ?: sessionId?.let {
            sessionRepository.getSessionById(it).firstOrNull()?.linkedMythicSessionId
        }

        if (targetMythicId != null) {
            val mythicSession = mythicRepository.getSessionById(targetMythicId).firstOrNull()
            if (mythicSession != null) {
                mythicRepository.saveRoll(
                    MythicRoll(
                        sessionId = targetMythicId,
                        question = content,
                        probability = ProbabilityLevel.NARRATIVE,
                        chaosFactor = mythicSession.chaosFactor,
                        roll = 0,
                        result = FateResult.NONE,
                        sceneNumber = mythicSession.sceneNumber
                    )
                )
                // Si solo queremos guardar en Mythic, podemos salir aquí. 
                // Pero a menudo queremos que también esté en las notas de la sesión principal.
            }
        }

        // 2. Guardar en las notas de DM de la sesión estándar (si existe)
        if (sessionId != null) {
            val session = sessionRepository.getSessionById(sessionId).firstOrNull()
            if (session != null) {
                val timestamp = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
                val newNote = "[$timestamp] $content"
                val updatedNotes = if (session.dmNotes.isNullOrBlank()) {
                    newNote
                } else {
                    "${session.dmNotes}\n$newNote"
                }
                sessionRepository.updateDmNotes(sessionId, updatedNotes)
            }
        }
    }
}
