package com.deckapp.feature.draw.components.blocks

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.deckapp.core.model.DicePreset
import com.deckapp.core.model.DiceRollResult
import com.deckapp.core.model.SessionBoardItem
import com.deckapp.core.model.SessionBoardItemSize
import com.deckapp.feature.draw.components.BoardBlockMeta

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DiceBlockContent(
    item: SessionBoardItem,
    meta: BoardBlockMeta,
    recentDiceRolls: List<DiceRollResult>,
    compactInFolder: Boolean,
    onDiceClicked: (String, Boolean, Boolean, String?) -> Unit,
    onUpdatePresets: (List<DicePreset>) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var formulaInput by remember { mutableStateOf("") }

    var hasAdvantage by remember { mutableStateOf(false) }
    var hasDisadvantage by remember { mutableStateOf(false) }
    val selectedExtraDice by remember { mutableStateOf<String?>(null) }
    
    val latestRoll = recentDiceRolls.firstOrNull()

    fun triggerRoll(formula: String) {
        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
        onDiceClicked(formula, hasAdvantage, hasDisadvantage, selectedExtraDice)
    }

    if (item.size == SessionBoardItemSize.S || compactInFolder) {
        // Layout S: Show latest roll result + fast d20 button
        Row(
            modifier = modifier
                .fillMaxWidth()
                .animateContentSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                if (latestRoll != null) {
                    Text(
                        text = latestRoll.expression,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Total: ${latestRoll.total}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = meta.color
                    )
                } else {
                    Text(
                        text = "Sin tiradas aún",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            FilledIconButton(
                onClick = { triggerRoll("1d20") },
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = meta.color),
                modifier = Modifier.size(36.dp)
            ) {
                Text(
                    text = "d20",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    } else {
        // Layout M and L
        Column(
            modifier = modifier
                .fillMaxWidth()
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Modificadores Rápidos
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                FilterChip(
                    selected = hasAdvantage,
                    onClick = {
                        hasAdvantage = !hasAdvantage
                        if (hasAdvantage) hasDisadvantage = false
                    },
                    label = { Text("Ventaja") }
                )
                FilterChip(
                    selected = hasDisadvantage,
                    onClick = {
                        hasDisadvantage = !hasDisadvantage
                        if (hasDisadvantage) hasAdvantage = false
                    },
                    label = { Text("Desventaja") }
                )
            }

            // Botones de dados comunes
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val commonDice = listOf("1d4", "1d6", "1d8", "1d10", "1d12", "1d20", "1d100")
                commonDice.forEach { dice ->
                    OutlinedButton(
                        onClick = { triggerRoll(dice) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(dice, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            // Input de fórmula libre
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = formulaInput,
                    onValueChange = { formulaInput = it },
                    placeholder = { Text("Ej: 2d6+3", style = MaterialTheme.typography.bodySmall) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(
                        onGo = {
                            if (formulaInput.isNotBlank()) {
                                triggerRoll(formulaInput.trim())
                                formulaInput = ""
                            }
                        }
                    )
                )
                FilledIconButton(
                    onClick = {
                        if (formulaInput.isNotBlank()) {
                            triggerRoll(formulaInput.trim())
                            formulaInput = ""
                        }
                    },
                    enabled = formulaInput.isNotBlank(),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = meta.color)
                ) {
                    Icon(Icons.Default.Casino, contentDescription = "Tirar")
                }
            }

            // Último resultado si existe
            latestRoll?.let { roll ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Último: ${roll.expression}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "= ${roll.total}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = meta.color
                        )
                    }
                }
            }
        }
    }
}
