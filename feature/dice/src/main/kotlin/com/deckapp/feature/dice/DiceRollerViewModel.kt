package com.deckapp.feature.dice

import androidx.lifecycle.ViewModel
import com.deckapp.core.domain.usecase.DiceEvaluator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class DiceRollResult(
    val expression: String,
    val total: Int,
    val timestamp: Long = System.currentTimeMillis()
) {
    val timestampLabel: String get() = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
}

data class DiceRollerUiState(
    val count: Int = 1,
    val sides: Int = 20,
    val modifier: Int = 0,
    val history: List<DiceRollResult> = emptyList(),
    val lastResult: DiceRollResult? = null
)

@HiltViewModel
class DiceRollerViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(DiceRollerUiState())
    val uiState: StateFlow<DiceRollerUiState> = _uiState.asStateFlow()

    fun updateCount(count: Int) {
        _uiState.update { it.copy(count = count) }
    }

    fun updateSides(sides: Int) {
        _uiState.update { it.copy(sides = sides) }
    }

    fun updateModifier(modifier: Int) {
        _uiState.update { it.copy(modifier = modifier) }
    }

    fun rollDice() {
        val state = _uiState.value
        val formula = "${state.count}d${state.sides}" + 
            (if (state.modifier > 0) "+${state.modifier}" else if (state.modifier < 0) "${state.modifier}" else "")
        
        val total = DiceEvaluator.evaluate(formula)
        val result = DiceRollResult(formula, total)
        
        _uiState.update { s ->
            state.copy(
                lastResult = result,
                history = (listOf(result) + s.history).take(20)
            )
        }
    }

    fun clearHistory() {
        _uiState.update { it.copy(history = emptyList(), lastResult = null) }
    }
}
