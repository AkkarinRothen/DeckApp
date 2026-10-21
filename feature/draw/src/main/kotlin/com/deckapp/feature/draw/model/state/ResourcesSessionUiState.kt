package com.deckapp.feature.draw.model.state

import androidx.compose.runtime.Immutable
import com.deckapp.core.model.RandomTable
import com.deckapp.feature.reference.StarterPackInfo

/**
 * Estado que encapsula los recursos disponibles en la sesión:
 * - Catálogo completo de tablas del sistema y biblioteca.
 * - Tablas asignadas a la sesión actual.
 * - Starter packs disponibles para importar/gestionar.
 * - Sistemas de juego detectados/disponibles.
 */
@Immutable
data class ResourcesSessionUiState(
    val allTables: List<RandomTable> = emptyList(),
    val tablesInSession: List<RandomTable> = emptyList(),
    val availableStarterPacks: List<StarterPackInfo> = emptyList(),
    val availableSystems: List<String> = emptyList()
)
