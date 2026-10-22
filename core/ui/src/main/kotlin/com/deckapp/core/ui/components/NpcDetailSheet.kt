package com.deckapp.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Remove
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
    onUpdateHp: ((Npc, Int) -> Unit)? = null,
    onRollAction: ((label: String, formula: String) -> Unit)? = null,
    isPlayingVoice: Boolean = false,
    onPlayVoice: ((String) -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showFullImage by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var localCurrentHp by remember(npc.id, npc.currentHp) { mutableIntStateOf(npc.currentHp) }

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

            // 3. Bloque Bento: Salud Interactiva (HP / Vigor)
            if (choirProfile == null && npc.maxHp > 0) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val hpPercent = (localCurrentHp.toFloat() / npc.maxHp.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
                        val hpColor by animateColorAsState(
                            targetValue = when {
                                hpPercent > 0.5f -> Color(0xFF4CAF50)
                                hpPercent > 0.2f -> Color(0xFFFFB74D)
                                else -> Color(0xFFEF5350)
                            },
                            label = "hpColor"
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Favorite,
                                    contentDescription = null,
                                    tint = hpColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "Puntos de Golpe",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "$localCurrentHp / ${npc.maxHp}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = hpColor
                            )
                        }

                        LinearProgressIndicator(
                            progress = { hpPercent },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = hpColor,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        // Botones de ajuste interactivo
                        if (onUpdateHp != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                HpAdjustButton("-5") {
                                    val newHp = (localCurrentHp - 5).coerceAtLeast(0)
                                    localCurrentHp = newHp
                                    onUpdateHp(npc, newHp)
                                }
                                HpAdjustButton("-1") {
                                    val newHp = (localCurrentHp - 1).coerceAtLeast(0)
                                    localCurrentHp = newHp
                                    onUpdateHp(npc, newHp)
                                }
                                Spacer(Modifier.weight(1f))
                                HpAdjustButton("+1") {
                                    val newHp = (localCurrentHp + 1).coerceAtMost(npc.maxHp)
                                    localCurrentHp = newHp
                                    onUpdateHp(npc, newHp)
                                }
                                HpAdjustButton("+5") {
                                    val newHp = (localCurrentHp + 5).coerceAtMost(npc.maxHp)
                                    localCurrentHp = newHp
                                    onUpdateHp(npc, newHp)
                                }
                            }
                        }
                    }
                }
            }

            // 4. Resumen Mecánico Bento (Estadísticas y tiradas al frente)
            Surface(
                shape = RoundedCornerShape(14.dp),
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
                                InteractiveAttrBadge("FUE", attr.strength, onRollAction)
                                InteractiveAttrBadge("DES", attr.dexterity, onRollAction)
                                InteractiveAttrBadge("CON", attr.constitution, onRollAction)
                                InteractiveAttrBadge("INT", attr.intelligence, onRollAction)
                                InteractiveAttrBadge("VOL", attr.willpower, onRollAction)
                                InteractiveAttrBadge("PRE", attr.presence, onRollAction)
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
                                StatPill("CA / DEFENSA", npc.armorClass.toString(), Color(0xFF42A5F5))
                                val initLabel = if (npc.initiativeBonus >= 0) "+${npc.initiativeBonus}" else npc.initiativeBonus.toString()
                                StatPill("INICIATIVA", initLabel, Color(0xFFFFB74D))
                                if (onRollAction != null) {
                                    FilledTonalButton(
                                        onClick = {
                                            val sign = if (npc.initiativeBonus >= 0) "+" else ""
                                            onRollAction("Iniciativa de ${npc.name}", "1d20$sign${npc.initiativeBonus}")
                                        },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.Casino, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Tirar Inic", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 5. Acciones de Choque / Ataques (Si es criatura Choir o tiene acciones)
            if (choirProfile != null) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Acciones y Ataques de Criatura",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        if (choirProfile.standard.isNotBlank()) {
                            ChoirActionRow(
                                title = "Ataque Estándar",
                                description = choirProfile.standard,
                                onRoll = onRollAction?.let { roll ->
                                    { roll("Ataque Estándar (${npc.name})", "1d20") }
                                }
                            )
                        }
                        if (choirProfile.special.isNotBlank()) {
                            ChoirActionRow(
                                title = "Ataque Especial",
                                description = choirProfile.special,
                                onRoll = onRollAction?.let { roll ->
                                    { roll("Ataque Especial (${npc.name})", "1d20") }
                                }
                            )
                        }
                        if (choirProfile.support.isNotBlank()) {
                            ChoirActionRow(
                                title = "Acción de Apoyo",
                                description = choirProfile.support,
                                onRoll = null
                            )
                        }
                    }
                }
            }

            // 6. Pestañas de Detalle (Descripción, Notas y Reglas)
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

@Composable
private fun HpAdjustButton(text: String, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
        modifier = Modifier.height(30.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(text, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ChoirActionRow(
    title: String,
    description: String,
    onRoll: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (onRoll != null) {
            Spacer(Modifier.width(8.dp))
            FilledTonalIconButton(
                onClick = onRoll,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Default.Casino,
                    contentDescription = "Tirar $title",
                    modifier = Modifier.size(18.dp)
                )
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
private fun InteractiveAttrBadge(
    name: String,
    score: Int,
    onRollAction: ((String, String) -> Unit)?
) {
    val mod = ChoirCharacterRules.modifier(score)
    val modStr = if (mod >= 0) "+$mod" else "$mod"
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable(enabled = onRollAction != null) {
                onRollAction?.invoke("Tirada de $name", "1d20$modStr")
            }
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Text(name, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("$score ($modStr)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
    }
}
