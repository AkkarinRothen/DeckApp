package com.deckapp.feature.draw.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.deckapp.core.domain.usecase.ChoirCharacterCodec
import com.deckapp.core.model.ChoirProfile
import com.deckapp.core.model.Npc

private enum class NpcFilterCategory(val label: String) {
    ALL("Todos"),
    CHOIR("Choir of Flesh"),
    DND("D&D / d20"),
    WITH_VOICE("Con Voz")
}

@Composable
fun NpcsTab(
    npcs: List<Npc>,
    playingPath: String?,
    onPlayVoice: (String) -> Unit,
    onNpcClick: ((Long) -> Unit)? = null
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(NpcFilterCategory.ALL) }

    val filteredNpcs = remember(npcs, searchQuery, selectedCategory) {
        npcs.filter { npc ->
            val matchesQuery = searchQuery.isBlank() ||
                npc.name.contains(searchQuery, ignoreCase = true) ||
                npc.description.contains(searchQuery, ignoreCase = true)

            val isChoir = npc.notes.contains("choir", ignoreCase = true) ||
                npc.description.contains("choir", ignoreCase = true) ||
                npc.description.contains("Inquebrantable", ignoreCase = true) ||
                ChoirCharacterCodec.read(npc.notes) != null ||
                ChoirProfile.fromNotes(npc.notes) != null

            val matchesCategory = when (selectedCategory) {
                NpcFilterCategory.ALL -> true
                NpcFilterCategory.CHOIR -> isChoir
                NpcFilterCategory.DND -> !isChoir
                NpcFilterCategory.WITH_VOICE -> npc.voiceSamplePath != null
            }

            matchesQuery && matchesCategory
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Barra de Búsqueda
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Buscar PNJ por nombre...", style = MaterialTheme.typography.bodyMedium) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Limpiar búsqueda", modifier = Modifier.size(18.dp))
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // Filtros de Categoría
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(NpcFilterCategory.values()) { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { selectedCategory = category },
                    label = { Text(category.label, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }

        // Contador y estado
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${filteredNpcs.size} disponibles",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (filteredNpcs.size != npcs.size) {
                Text(
                    text = "de ${npcs.size} totales",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        if (filteredNpcs.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    if (searchQuery.isNotBlank()) "No hay resultados para \"$searchQuery\"" else "No hay PNJs en esta categoría",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredNpcs, key = { it.id }) { npc ->
                    NpcListItem(
                        npc = npc,
                        isPlaying = playingPath == npc.voiceSamplePath,
                        onPlayVoice = { npc.voiceSamplePath?.let(onPlayVoice) },
                        onClick = { onNpcClick?.invoke(npc.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun NpcListItem(
    npc: Npc,
    isPlaying: Boolean,
    onPlayVoice: () -> Unit,
    onClick: () -> Unit = {}
) {
    val choirProfile = remember(npc.notes) { ChoirProfile.fromNotes(npc.notes) }
    val choirCharacter = remember(npc.notes) { ChoirCharacterCodec.read(npc.notes) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                if (npc.imagePath != null) {
                    AsyncImage(
                        model = npc.imagePath,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(Icons.Default.Mic, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(Modifier.width(12.dp))

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    npc.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                // Resumen mecánico rápido
                val quickStat = when {
                    choirCharacter != null -> "Inquebrantable · Vigor ${choirCharacter.vigor} · Def ${choirCharacter.attributes.dexterity}"
                    choirProfile != null -> "Choir · CD ${choirProfile.statistics["CD"] ?: 10} · Init ${choirProfile.initiative}"
                    else -> "HP ${npc.currentHp}/${npc.maxHp} · CA ${npc.armorClass} · Inic ${if (npc.initiativeBonus >= 0) "+${npc.initiativeBonus}" else npc.initiativeBonus.toString()}"
                }
                Text(
                    text = quickStat,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Voice Control
            if (npc.voiceSamplePath != null) {
                IconButton(
                    onClick = onPlayVoice,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (isPlaying) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent
                    )
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                        tint = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
