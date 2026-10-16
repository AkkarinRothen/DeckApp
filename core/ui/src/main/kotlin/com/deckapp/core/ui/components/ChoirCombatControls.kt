package com.deckapp.core.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.deckapp.core.domain.usecase.ChoirCombatRules
import com.deckapp.core.model.ChoirCombatState
import com.deckapp.core.model.ChoirProfile
import com.deckapp.core.model.EncounterCreature
import com.deckapp.core.model.ChoirCombatCommand
import coil.compose.AsyncImage

/** Same controls in the combat sidebar and floating session tracker. */
@Composable
fun ChoirCombatControls(
    creature: EncounterCreature,
    onStateChange: (ChoirCombatCommand) -> Unit,
    modifier: Modifier = Modifier,
    isActive: Boolean = false
) {
    val profile = remember(creature.notes) { ChoirProfile.fromNotes(creature.notes) } ?: return
    val state = remember(creature.notes) { ChoirCombatState.fromNotes(creature.notes) }
    var showReference by rememberSaveable(creature.id) { mutableStateOf(false) }
    var showTarget by rememberSaveable(creature.id) { mutableStateOf(false) }
    Surface(modifier = modifier.fillMaxWidth(), tonalElevation = 2.dp,
        border = if (isActive) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (creature.imagePath != null) AsyncImage(model = creature.imagePath,
                    contentDescription = creature.name, contentScale = ContentScale.Fit,
                    modifier = Modifier.size(64.dp))
                Text(creature.name, style = MaterialTheme.typography.titleMedium)
            }
            if (isActive) Text("Turno actual", color = MaterialTheme.colorScheme.primary)
            Text("Choir of Flesh · Iniciativa fija ${profile.initiative}", style = MaterialTheme.typography.labelLarge)
            Text("Defensa ${profile.statistics["CD"]} · Ataque ${profile.statistics["Ataque"]} · Precisión ${profile.statistics["Precisión"]}")
            Text("Movimiento ${profile.statistics["Movimiento"]} · ${profile.statistics["Rol"]}")
            if (profile.traits.isNotBlank()) MarkdownText(markdown = profile.traits)
            ChoirWoundCounter("Heridas mayores", state.major,
                { onStateChange(ChoirCombatCommand.REMOVE_MAJOR) },
                { onStateChange(ChoirCombatCommand.ADD_MAJOR) })
            ChoirWoundCounter("Heridas menores", state.minor,
                { onStateChange(ChoirCombatCommand.REMOVE_MINOR) },
                { onStateChange(ChoirCombatCommand.ADD_MINOR) })
            Text("Marca únicamente heridas permitidas por los rasgos. Al sufrir una mayor, consulta la tabla de efectos del tipo de daño.", style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onStateChange(ChoirCombatCommand.ROLL_ACTION) }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text("Acción · d10")
                }
                OutlinedButton(onClick = { onStateChange(ChoirCombatCommand.ROLL_TARGET) }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text("Objetivo · d6")
                }
            }
            if (state.actionRoll in 1..10) {
                Text("d10 = ${state.actionRoll} · ${state.major} heridas mayores")
                MarkdownText(markdown = ChoirCombatRules.action(profile, state.major, state.actionRoll))
            }
            if (state.targetRoll in 1..6) {
                Text("d6 = ${state.targetRoll} · ${ChoirCombatRules.target(state.targetRoll)}")
                Text("Mantén el objetivo hasta que caiga, sea inalcanzable o una acción lo cambie.", style = MaterialTheme.typography.bodySmall)
            }
            Text("El PJ tira contra el grado dinámico. Precisión se aplica como Carga a su defensa.", style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = { showTarget = true }, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("Calcular NO del PJ")
            }
            TextButton(onClick = { showReference = true }, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("Ficha, acciones y rasgos")
            }
        }
    }
    if (showTarget) ChoirTargetDialog(profile) { showTarget = false }
    if (showReference) {
        AlertDialog(
            onDismissRequest = { showReference = false },
            title = { Text(creature.name) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    MarkdownText(markdown = profile.reference)
                }
            },
            confirmButton = { TextButton(onClick = { showReference = false }) { Text("Cerrar") } }
        )
    }
}

@Composable
private fun ChoirWoundCounter(label: String, value: Int, onRemove: () -> Unit, onAdd: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("$label: $value", modifier = Modifier.weight(1f).padding(top = 14.dp))
        OutlinedButton(onClick = onRemove, enabled = value > 0, modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)) { Text("−") }
        Spacer(Modifier.width(4.dp))
        OutlinedButton(onClick = onAdd, modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)) { Text("+") }
    }
}
