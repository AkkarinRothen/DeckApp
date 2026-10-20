package com.deckapp.feature.draw.model.state

import com.deckapp.core.model.TableRollResult
import com.deckapp.feature.draw.components.spotlight.SpotlightCategory
import com.deckapp.feature.draw.components.spotlight.SpotlightItem

/**
 * Sub-estado modular que encapsula la búsqueda global, Command Palette
 * y selección de resultados en el Omnibar Spotlight de la sesión.
 */
data class SpotlightUiState(
    val showDialog: Boolean = false,
    val query: String = "",
    val selectedCategory: SpotlightCategory = SpotlightCategory.ALL,
    val results: List<SpotlightItem> = emptyList(),
    val selectedItem: SpotlightItem? = null,
    val lastRollResult: TableRollResult? = null,
    val isSearching: Boolean = false
)
