package com.deckapp.feature.draw.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.deckapp.core.model.Npc

@Composable
fun NpcQuickPreviewDialog(
    npc: Npc,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = npc.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 350.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val choir = com.deckapp.core.model.ChoirProfile.fromNotes(npc.notes)
                val character = com.deckapp.core.domain.usecase.ChoirCharacterCodec.read(npc.notes)
                if (character != null) {
                    Text("Inquebrantable · Vigor ${character.vigor} · Heridas ${character.major}/4 y ${character.minor}/8")
                } else if (choir != null) {
                    Text("Choir of Flesh · Defensa ${choir.statistics["CD"]} · Iniciativa fija ${choir.initiative}")
                } else Text(
                    text = "HP: ${npc.currentHp}/${npc.maxHp}  ·  CA: ${npc.armorClass}  ·  Inic: ${if (npc.initiativeBonus >= 0) "+${npc.initiativeBonus}" else npc.initiativeBonus.toString()}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                if (npc.description.isNotBlank()) {
                    Text(
                        text = npc.description,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                if (npc.notes.isNotBlank()) {
                    Text(
                        text = "Notas:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    com.deckapp.core.ui.components.MarkdownText(markdown = npc.notes)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )
}
