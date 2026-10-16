package com.deckapp.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.deckapp.core.domain.usecase.*
import com.deckapp.core.model.*

@Composable
fun ChoirCharacterCombatControls(creature: EncounterCreature, onCommand: (ChoirCharacterCommand) -> Unit,
    modifier: Modifier = Modifier, isActive: Boolean = false) {
    val s = remember(creature.notes) { ChoirCharacterCodec.read(creature.notes) } ?: return
    Surface(modifier = modifier.fillMaxWidth(), tonalElevation = 2.dp,
        border = if (isActive) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(creature.name, style = MaterialTheme.typography.titleMedium)
            if (isActive) Text("Turno actual", color = MaterialTheme.colorScheme.primary)
            Text("Inquebrantable · Defensa ${ChoirCharacterRules.defense(s)} · Iniciativa ${creature.initiativeTotal ?: "por tirar"}")
            Text("Humanidad ${s.humanity}/20 · Angustia ${s.anguish} · Trauma ${s.trauma}")
            ChoirNumberField("Mayores /4", s.major, 0, 4) { onCommand(if (it > s.major) ChoirCharacterCommand.ADD_MAJOR else ChoirCharacterCommand.REMOVE_MAJOR) }
            ChoirNumberField("Menores /8", s.minor, 0, 8) { onCommand(if (it > s.minor) ChoirCharacterCommand.ADD_MINOR else ChoirCharacterCommand.REMOVE_MINOR) }
            ChoirNumberField("Vigor", s.vigor, 0, ChoirCharacterRules.vigorMaximum(s)) { onCommand(if (it > s.vigor) ChoirCharacterCommand.GAIN_VIGOR else ChoirCharacterCommand.SPEND_VIGOR) }
            Text("Reacción inmediata a la última herida recibida:", style = MaterialTheme.typography.labelLarge)
            OutlinedButton(enabled = s.vigor > 0 && s.major > 0 && s.minor < 8, modifier = Modifier.heightIn(min = 48.dp),
                onClick = { onCommand(ChoirCharacterCommand.LESSEN_MAJOR) }) { Text("1 Vigor: mayor → menor") }
            OutlinedButton(enabled = s.vigor > 0 && s.minor > 0, modifier = Modifier.heightIn(min = 48.dp),
                onClick = { onCommand(ChoirCharacterCommand.NEGATE_MINOR) }) { Text("1 Vigor: negar menor") }
            Text("Las heridas y Vigor se guardan también en la ficha de biblioteca vinculada.", style = MaterialTheme.typography.bodySmall)
            TextButton(enabled = s.undoState != null, modifier = Modifier.heightIn(min = 48.dp),
                onClick = { onCommand(ChoirCharacterCommand.UNDO) }) { Text("Deshacer último cambio") }
            if (ChoirCharacterRules.isDead(s)) Text("Registro completo: muerte.", color = MaterialTheme.colorScheme.error)
        }
    }
}
