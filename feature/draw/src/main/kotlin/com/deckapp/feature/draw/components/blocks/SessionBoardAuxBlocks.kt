package com.deckapp.feature.draw.components.blocks

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.deckapp.core.model.*
import com.deckapp.feature.mythic.MythicSessionUiState
import androidx.compose.runtime.saveable.rememberSaveable
import com.deckapp.core.domain.usecase.oracleJournalEntry

@Composable
fun MapBlockContent(
    linkedHexMapId: Long?,
    linkedMapName: String?,
    linkedMapTileCount: Int,
    linkedMapPoiCount: Int,
    linkedMapDayNumber: Int?,
    linkedMapPartyLocation: String?,
    tiles: List<HexTile>,
    pois: List<HexPoi>,
    partyLocation: Pair<Int, Int>?,
    compactInFolder: Boolean,
    onOpenMap: () -> Unit,
    onMoveParty: (Int, Int) -> Unit,
    onExploreTile: (HexTile) -> Unit,
    onReconnoiterTile: (HexTile) -> Unit,
    onMapTile: (HexTile) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Default.Map,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = linkedMapName ?: "Mapa Hexcrawl",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (linkedMapDayNumber != null) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = "Día $linkedMapDayNumber",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        if (!compactInFolder) {
            Text(
                text = "$linkedMapTileCount casillas exploradas · $linkedMapPoiCount puntos de interés",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!linkedMapPartyLocation.isNullOrBlank()) {
                Text(
                    text = "Grupo: $linkedMapPartyLocation",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }

        OutlinedButton(
            onClick = onOpenMap,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Icon(Icons.Default.Explore, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text("Abrir Hexcrawl", style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
fun LocationMapBlockContent(
    locationName: String?,
    imagePath: String?,
    mapPins: List<MapPin>,
    npcs: List<Npc>,
    wikiEntries: List<WikiEntry>,
    compactInFolder: Boolean,
    size: SessionBoardItemSize,
    onDiceClicked: (String) -> Unit,
    onRollTable: (Long) -> Unit,
    onOpen: () -> Unit,
    onUpdateWikiEntry: (WikiEntry) -> Unit,
    linkedWikiEntry: WikiEntry?,
    onOpenNpc: (Long) -> Unit,
    onOpenWiki: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Default.LocationOn,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = locationName ?: "Ubicación / Mapa Táctico",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }

        if (!compactInFolder) {
            Text(
                text = "${mapPins.size} marcadores · ${npcs.size} NPCs en la zona",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (linkedWikiEntry != null) {
                Surface(
                    onClick = { onOpenWiki(linkedWikiEntry.id) },
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = linkedWikiEntry.title,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        OutlinedButton(
            onClick = onOpen,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Icon(Icons.Default.PinDrop, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text("Ver Ubicación", style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
fun MythicBlockContent(
    item: SessionBoardItem,
    session: MythicSession?,
    mythicState: MythicSessionUiState?,
    compactInFolder: Boolean,
    boardChaos: Int = 5,
    boardOdds: ProbabilityLevel = ProbabilityLevel.FIFTY_FIFTY,
    lastResult: BoardMythicFateResult? = null,
    recentHistory: List<BoardMythicFateResult> = emptyList(),
    lastRandomEvent: String? = null,
    onOddsChanged: (ProbabilityLevel) -> Unit = {},
    onChaosChanged: (delta: Int) -> Unit = {},
    onRollFateCheck: (ProbabilityLevel) -> Unit = {},
    onAskQuestion: ((ProbabilityLevel, String) -> Unit)? = null,
    onRollRandomEvent: () -> Unit = {},
    onSendToNotes: (String) -> Unit = {},
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentChaos = session?.chaosFactor ?: boardChaos
    var showOddsMenu by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }
    var question by rememberSaveable(item.id) { mutableStateOf("") }

    val oddsLabel = when (boardOdds) {
        ProbabilityLevel.IMPOSSIBLE -> "Imposible"
        ProbabilityLevel.NEARLY_IMPOSSIBLE -> "Casi Imposible"
        ProbabilityLevel.VERY_UNLIKELY -> "Muy Improbable"
        ProbabilityLevel.UNLIKELY -> "Improbable"
        ProbabilityLevel.FIFTY_FIFTY -> "50 / 50"
        ProbabilityLevel.LIKELY -> "Probable"
        ProbabilityLevel.VERY_LIKELY -> "Muy Probable"
        ProbabilityLevel.NEARLY_CERTAIN -> "Casi Seguro"
        ProbabilityLevel.CERTAIN -> "Seguro"
        ProbabilityLevel.NARRATIVE -> "Narrativo"
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Cabecera: Título + Control interactivo de Caos
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = session?.name ?: "Oráculo Mítico",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            // Selector interactivo de Caos (- / +)
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 2.dp, vertical = 1.dp)
                ) {
                    IconButton(
                        onClick = { onChaosChanged(-1) },
                        modifier = Modifier.size(22.dp)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Menos Caos", modifier = Modifier.size(12.dp))
                    }
                    Text(
                        text = "Caos $currentChaos",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 2.dp)
                    )
                    IconButton(
                        onClick = { onChaosChanged(+1) },
                        modifier = Modifier.size(22.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Más Caos", modifier = Modifier.size(12.dp))
                    }
                }
            }
        }

        OutlinedTextField(
            value = question,
            onValueChange = { question = it },
            label = { Text("Pregunta al oráculo") },
            placeholder = { Text("¿El guardia me reconoce?") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 3
        )
        // Fila de Controles: Selector de Probabilidad + Botón Preguntar + Botón Chispa
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box {
                OutlinedButton(
                    onClick = { showOddsMenu = true },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(oddsLabel, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.width(2.dp))
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(14.dp))
                }

                DropdownMenu(
                    expanded = showOddsMenu,
                    onDismissRequest = { showOddsMenu = false }
                ) {
                    ProbabilityLevel.values().filter { it != ProbabilityLevel.NARRATIVE }.forEach { lvl ->
                        val label = when(lvl) {
                            ProbabilityLevel.IMPOSSIBLE -> "Imposible"
                            ProbabilityLevel.NEARLY_IMPOSSIBLE -> "Casi Imposible"
                            ProbabilityLevel.VERY_UNLIKELY -> "Muy Improbable"
                            ProbabilityLevel.UNLIKELY -> "Improbable"
                            ProbabilityLevel.FIFTY_FIFTY -> "50 / 50"
                            ProbabilityLevel.LIKELY -> "Probable"
                            ProbabilityLevel.VERY_LIKELY -> "Muy Probable"
                            ProbabilityLevel.NEARLY_CERTAIN -> "Casi Seguro"
                            ProbabilityLevel.CERTAIN -> "Seguro"
                            else -> lvl.name
                        }
                        DropdownMenuItem(
                            text = { Text(label, style = MaterialTheme.typography.bodySmall) },
                            onClick = {
                                onOddsChanged(lvl)
                                showOddsMenu = false
                            }
                        )
                    }
                }
            }

            Button(
                onClick = { onAskQuestion?.invoke(boardOdds, question.trim()) ?: onRollFateCheck(boardOdds) },
                modifier = Modifier.weight(1f).height(32.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Icon(Icons.Default.Casino, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("Preguntar", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }

            FilledTonalIconButton(
                onClick = onRollRandomEvent,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.Bolt, contentDescription = "Chispa / Evento", modifier = Modifier.size(16.dp))
            }
        }

        // Tarjeta de Resultado Principal (si existe tirada)
        if (lastResult != null) {
            if (lastResult.question.isNotBlank()) {
                Text(lastResult.question, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            }
            val isYes = lastResult.fateResult == FateResult.YES || lastResult.fateResult == FateResult.EXCEPTIONAL_YES
            val isExceptional = lastResult.fateResult == FateResult.EXCEPTIONAL_YES || lastResult.fateResult == FateResult.EXCEPTIONAL_NO

            val containerColor = when {
                isExceptional && isYes -> MaterialTheme.colorScheme.primaryContainer
                isYes -> MaterialTheme.colorScheme.secondaryContainer
                isExceptional -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
            val contentColor = when {
                isExceptional && isYes -> MaterialTheme.colorScheme.onPrimaryContainer
                isYes -> MaterialTheme.colorScheme.onSecondaryContainer
                isExceptional -> MaterialTheme.colorScheme.onErrorContainer
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = containerColor,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = lastResult.answerLabel,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = contentColor
                            )
                            Text(
                                text = "d100: ${lastResult.roll} vs ${lastResult.targetThreshold}",
                                style = MaterialTheme.typography.labelSmall,
                                color = contentColor.copy(alpha = 0.8f)
                            )
                        }

                        IconButton(
                            onClick = {
                                onSendToNotes(oracleJournalEntry(lastResult))
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.ContentPasteGo,
                                contentDescription = "Enviar a notas",
                                tint = contentColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    val eventDesc = lastResult.randomEventDescription
                    if (eventDesc != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.85f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Bolt, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.size(12.dp))
                                Text(
                                    text = eventDesc,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }
                }
            }
        }

        // Banner de Chispa / Evento Aleatorio Manual
        if (lastRandomEvent != null) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.size(14.dp))
                        Text(
                            text = "Chispa: $lastRandomEvent",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                    IconButton(
                        onClick = {
                            onSendToNotes("> ⚡ **Chispa**: $lastRandomEvent")
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.ContentPasteGo,
                            contentDescription = "Enviar chispa a notas",
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }

        // Historial Reciente Desplegable
        if (recentHistory.size > 1 && !compactInFolder) {
            TextButton(
                onClick = { showHistory = !showHistory },
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                modifier = Modifier.height(24.dp)
            ) {
                Icon(
                    if (showHistory) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "Tiradas recientes (${recentHistory.size})",
                    style = MaterialTheme.typography.labelSmall
                )
            }

            if (showHistory) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    recentHistory.take(3).forEach { hist ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${hist.question.ifBlank { hist.odds.name.lowercase().replace('_', ' ') }}: ${hist.answerLabel} (${hist.roll})",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { onSendToNotes(oracleJournalEntry(hist)) }, modifier = Modifier.size(48.dp)) {
                                Icon(Icons.Default.ContentPasteGo, contentDescription = "Guardar consulta anterior en el diario", modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }

        // Enlace a vista completa si hay sesión Mythic
        if (session != null && !compactInFolder) {
            TextButton(
                onClick = onOpen,
                modifier = Modifier.align(Alignment.End).height(24.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
            ) {
                Text("Registro de escenas Mythic", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
fun ReferenceBlockContent(
    item: SessionBoardItem,
    focusTitle: String?,
    preview: String?,
    pinnedCount: Int,
    searchQuery: String?,
    searchCount: Int,
    searchPreview: String?,
    wikiEntryCount: Int,
    compactInFolder: Boolean,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.MenuBook,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = focusTitle ?: "Referencia & Reglas",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }

        if (!compactInFolder) {
            if (!preview.isNullOrBlank()) {
                Text(
                    text = preview,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Text(
                    text = "$pinnedCount reglas fijadas · $wikiEntryCount entradas disponibles",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        OutlinedButton(
            onClick = onOpen,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text("Abrir Referencias", style = MaterialTheme.typography.labelMedium)
        }
    }
}
