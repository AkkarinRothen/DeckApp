package com.deckapp.feature.tables.library

import com.deckapp.core.model.RandomTable
import com.deckapp.core.model.Tag
import com.deckapp.core.model.TableRollResult

data class TableLibraryUiState(
    val tables: List<RandomTable> = emptyList(),
    val filteredTables: List<RandomTable> = emptyList(),
    val allTags: List<Tag> = emptyList(),
    val selectedTagIds: Set<Long> = emptySet(),
    val selectedTableIds: Set<Long> = emptySet(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val activeTable: RandomTable? = null,
    val activeRollResult: TableRollResult? = null,
    val isRolling: Boolean = false,
    val snackbarMessage: String? = null
)
