package com.deckapp.feature.draw.model.state

import androidx.compose.runtime.Immutable
import com.deckapp.core.model.Npc

/**
 * Estado que encapsula los NPCs, clips de voz y jugadores permanentes en la sesión:
 * - Lista de NPCs asociados a la sesión.
 * - Ruta del archivo de audio de voz de NPC en reproducción activa.
 * - Conteo total de NPCs vinculados.
 * - Jugadores/personajes permanentes en el tablero.
 */
@Immutable
data class NpcSessionUiState(
    val npcs: List<Npc> = emptyList(),
    val playingNpcVoicePath: String? = null,
    val npcCount: Int = 0,
    val permanentPlayers: List<Npc> = emptyList()
)
