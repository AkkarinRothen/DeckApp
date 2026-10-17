package com.deckapp.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SportsKabaddi
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.deckapp.core.domain.usecase.ChoirCharacterCodec
import com.deckapp.core.domain.usecase.ChoirCharacterRules
import com.deckapp.core.model.ChoirProfile
import com.deckapp.core.model.Npc

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NpcDetailSheet(
    npc: Npc,
    onDismiss: () -> Unit,
    onAddToCombat: ((Npc) -> Unit)? = null,
    onLogToDiary: ((Npc) -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    isPlayingVoice: Boolean = false,
    onPlayVoice: ((String) -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showFullImage by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }

    val choirProfile = remember(npc.notes) { ChoirProfile.fromNotes(npc.notes) }
    val choirCharacter = remember(npc.notes) { ChoirCharacterCodec.read(npc.notes) }
    val voicePath = npc.voiceSamplePath

    // Diálogo de imagen ampliada a pantalla completa
    if (showFullImage && npc.imagePath != null) {
        Dialog(onDismissRequest = { showFullImage = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                AsyncImage(
                    model = npc.imagePath,
                    contentDescription = "Retrato ampliado de ${npc.name}",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 500.dp)
                )
                IconButton(
                    onClick = { showFullImage = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Identidad y Avatar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .clickable(enabled = npc.imagePath != null) { showFullImage = true },
                    contentAlignment = Alignment.Center
                ) {
                    if (npc.imagePath != null) {
                        AsyncImage(
                            model = npc.imagePath,
                            contentDescription = npc.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(2.dp)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                .padding(2.dp)
                        ) {
                            Icon(
                                Icons.Default.ZoomIn,
                                contentDescription = "Ampliar imagen",
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    } else {
                        Icon(
                            Icons.Default.Shield,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = npc.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    val systemLabel = when {
                        choirCharacter != null -> "Inquebrantable · Choir of Flesh"
                        choirProfile != null -> "Choir of Flesh · Criatura"
                        else -> "Sistema Genérico / d20"
                    }
                    Text(
                        text = systemLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (onEdit != null) {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar PNJ")
                    }
                }
            }

            // 2. Acciones Rápidas (Añadir al combate, Al diario, Voz)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (onAddToCombat != null) {
                    FilledTonalButton(
                        onClick = { onAddToCombat(npc) },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(Icons.Default.SportsKabaddi, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("A Combate", style = MaterialTheme.typography.labelMedium, maxLines = 1)
                    }
                }

                if (onLogToDiary != null) {
                    OutlinedButton(
                        onClick = { onLogToDiary(npc) },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(Icons.Default.HistoryEdu, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Al Diario", style = MaterialTheme.typography.labelMedium, maxLines = 1)
                    }
                }

                if (voicePath != null && onPlayVoice != null) {
                    FilledTonalIconButton(
                        onClick = { onPlayVoice(voicePath) }
                    ) {
                        Icon(
                            if (isPlayingVoice) Icons.Default.Pause else Icons.Default.Mic,
                            contentDescription = if (isPlayingVoice) "Pausar voz" else "Reproducir voz"
                        )
                    }
                }
            }

            // 3. Resumen Mecánico Prioritario (Estadísticas al frente, no desplazadas por descripción)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    when {
                        choirCharacter != null -> {
                            val maxVigor = ChoirCharacterRules.vigorMaximum(choirCharacter)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                StatPill("DEFENSA", ChoirCharacterRules.defense(choirCharacter).toString(), MaterialTheme.colorScheme.primary)
                                StatPill("INIC", ChoirCharacterRules.initiative(choirCharacter).toString(), MaterialTheme.colorScheme.secondary)
                                StatPill("VIGOR", "${choirCharacter.vigor}/$maxVigor", Color(0xFF4CAF50))
                                StatPill("HERIDAS", "${choirCharacter.major}M / ${choirCharacter.minor}m", Color(0xFFEF5350))
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                val attr = choirCharacter.attributes
                                AttrBadge("FUE", attr.strength)
                                AttrBadge("DES", attr.dexterity)
                                AttrBadge("CON", attr.constitution)
                                AttrBadge("INT", attr.intelligence)
                                AttrBadge("VOL", attr.willpower)
                                AttrBadge("PRE", attr.presence)
                            }
                        }
                        choirProfile != null -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                StatPill("DEFENSA (CD)", choirProfile.statistics["CD"] ?: "10", MaterialTheme.colorScheme.primary)
                                StatPill("INICIATIVA", "${choirProfile.initiative} (fija)", MaterialTheme.colorScheme.secondary)
                                StatPill("MOVIMIENTO", "${choirProfile.statistics["Movimiento"] ?: "2"} casillas", Color(0xFF42A5F5))
                            }
                        }
                        else -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                StatPill("HP", "${npc.currentHp}/${npc.maxHp}", Color(0xFFEF5350))
                                StatPill("CA", npc.armorClass.toString(), Color(0xFF42A5F5))
                                StatPill("INICIATIVA", if (npc.initiativeBonus >= 0) "+${npc.initiativeBonus}" else npc.initiativeBonus.toString(), Color(0xFFFFB74D))
                            }
                        }
                    }
                }
            }

            // 4. Pestañas de Detalle (Ficha/Acciones, Descripción, Notas)
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                divider = { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant) }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Descripción") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Notas y Reglas") }
                )
            }

            when (selectedTab) {
                0 -> {
                    val cleanDesc = remember(npc.description) {
                        cleanMarkdownDirectives(npc.description)
                    }
                    if (cleanDesc.isNotBlank()) {
                        MarkdownText(
                            markdown = cleanDesc,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text(
                            "Sin descripción adicional registrada.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                1 -> {
                    val cleanNotes = remember(npc.notes) {
                        cleanMarkdownDirectives(npc.notes)
                    }
                    if (cleanNotes.isNotBlank()) {
                        MarkdownText(
                            markdown = cleanNotes,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text(
                            "Sin notas o reglas específicas registradas.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

private fun cleanMarkdownDirectives(raw: String): String {
    return raw
        .lines()
        .filterNot { it.trim().startsWith("<!--") && it.trim().endsWith("-->") }
        .joinToString("\n")
        .trim()
}

@Composable
private fun StatPill(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color.copy(alpha = 0.9f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun AttrBadge(name: String, score: Int) {
    val mod = ChoirCharacterRules.modifier(score)
    val modStr = if (mod >= 0) "+$mod" else "$mod"
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(name, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("$score ($modStr)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
    }
}
