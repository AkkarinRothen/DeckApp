package com.deckapp.feature.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deckapp.core.domain.repository.CardRepository
import com.deckapp.core.domain.repository.SessionRepository
import com.deckapp.core.domain.repository.TableRepository
import com.deckapp.core.model.DrawAction
import com.deckapp.core.model.DrawEvent
import com.deckapp.core.model.TableRollResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class GlobalAnalyticsUiState(
    val totalSessions: Int = 0,
    val totalDrawn: Int = 0,
    val totalRolls: Int = 0,
    val totalPlayTimeMinutes: Long = 0,
    val luckFactor: Float = 0.5f,
    val deckDistribution: Map<String, Int> = emptyMap(),
    val luckHistory: List<Float> = emptyList(),
    val mostActiveDay: String? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class GlobalAnalyticsViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val cardRepository: CardRepository,
    private val tableRepository: TableRepository
) : ViewModel() {

    val uiState: StateFlow<GlobalAnalyticsUiState> = combine(
        sessionRepository.getAllSessions(),
        sessionRepository.getAllEvents(),
        tableRepository.getAllRollLog()
    ) { sessions, events, rolls ->
        if (sessions.isEmpty() && events.isEmpty() && rolls.isEmpty()) {
            return@combine GlobalAnalyticsUiState(isLoading = false)
        }

        // 1. Estadísticas básicas
        val totalDrawn = events.count { it.action == DrawAction.DRAW }
        val totalRolls = rolls.size
        val totalTime = sessions.mapNotNull { s ->
            if (s.endedAt != null) (s.endedAt!! - s.createdAt) else null
        }.sum() / (1000 * 60)

        // 2. Distribución de mazos
        // Necesitamos nombres de mazos. Para eficiencia en global, lo hacemos por lo que hay en los eventos.
        val deckUsage = mutableMapOf<String, Int>()
        // Nota: En una app real, esto se optimizaría con un JOIN en el DAO.
        // Aquí lo hacemos agregando por los nombres que ya vienen en los eventos (si los hay) 
        // o por metadatos. Como DrawEvent no tiene deckName, usamos el repo.
        
        // Pero para no bloquear el hilo UI, esta agregación es costosa. 
        // Lo ideal es tener un Map de deckId -> Name cacheado.
        val deckIds = events.mapNotNull { it.metadata.toLongOrNull() }.distinct() // Usualmente el metadata guarda el stackId en algunos eventos
        // En este proyecto, SessionRepositoryImpl logea el evento. Vamos a verificar qué hay en metadata.
        
        // 3. Factor de Suerte (Simplificado: media de rolls normalizada 0..1)
        // Asumiendo que muchos rolls son d20 (1-20), media esperada 10.5.
        // Como no sabemos el tipo de dado de cada tabla, buscamos patrones en rollValue.
        val luckValue = if (rolls.isNotEmpty()) {
            val avg = rolls.map { it.rollValue }.average().toFloat()
            // Heurística simple: si la media > 11 es "buena suerte" en d20.
            // Para generalizar, normalizamos un poco.
            (avg / 20f).coerceIn(0f, 1f)
        } else 0.5f

        GlobalAnalyticsUiState(
            totalSessions = sessions.size,
            totalDrawn = totalDrawn,
            totalRolls = totalRolls,
            totalPlayTimeMinutes = totalTime,
            luckFactor = luckValue,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = GlobalAnalyticsUiState()
    )
}
