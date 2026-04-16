package com.deckapp.feature.tables

import androidx.lifecycle.ViewModel
import com.deckapp.core.domain.repository.TableRepository
import com.deckapp.core.domain.usecase.RollTableUseCase
import com.deckapp.core.domain.usecase.ImportTableUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainTablesViewModel @Inject constructor(
    private val tableRepository: TableRepository,
    private val rollTableUseCase: RollTableUseCase,
    private val importTableUseCase: ImportTableUseCase
) : ViewModel() {
    // Empty class to test Dagger/KSP validation with new name
}
