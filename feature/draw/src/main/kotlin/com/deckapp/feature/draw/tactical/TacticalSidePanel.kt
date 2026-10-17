package com.deckapp.feature.draw.tactical

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deckapp.core.model.*

@Composable
fun TacticalSidePanel(
    uiState: TacticalUiState,
    dmNotes: String,
    onNotesChanged: (String) -> Unit,
    onTabSelected: (TacticalSidePanelTab) -> Unit,
    onSetDice: (count: Int, sides: Int, modifier: Int) -> Unit,
    onRollDice: () -> Unit,
    onRollFateCheck: (ProbabilityLevel, Int) -> Unit,
    onRollRandomEvent: () -> Unit,
    onWikiSearchQueryChanged: (String) -> Unit,
    onAdvanceCombatTurn: () -> Unit,
    onSelectToken: (Long) -> Unit,
    onClose: () -> Unit,
    onFocusPinCoordinates: (cellX: Float, cellY: Float) -> Unit = { _, _ -> },
    onPinClicked: (Long) -> Unit = {},
    onToggleStickyPin: (Long) -> Unit = {},
    onTogglePinLocked: (Long) -> Unit = {},
    onEditPin: (TacticalMapPin) -> Unit = {},
    onDeletePin: (Long) -> Unit = {},
    onToggleShowSecretPins: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxHeight(),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.98f),
        tonalElevation = 6.dp,
        shadowElevation = 12.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Cabecera: Título, Pestañas principales y botón Cerrar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DashboardCustomize,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Mesa Híbrida",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(onClick = onClose, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Cerrar Panel", modifier = Modifier.size(20.dp))
                }
            }

            // Barra de Pestañas
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val tabs = listOf(
                    TacticalSidePanelTab.NOTES_WIKI to ("Notas & Wiki" to Icons.AutoMirrored.Filled.MenuBook),
                    TacticalSidePanelTab.PINS to ("Pines (${uiState.pins.size})" to Icons.Default.LocationOn),
                    TacticalSidePanelTab.DICE to ("Dados" to Icons.Default.Casino),
                    TacticalSidePanelTab.MYTHIC to ("Mythic" to Icons.Default.AutoAwesome),
                    TacticalSidePanelTab.COMBAT to ("Combate" to Icons.Default.SportsKabaddi)
                )

                tabs.forEach { (tab, meta) ->
                    val isSelected = uiState.activeSidePanelTab == tab
                    FilterChip(
                        selected = isSelected,
                        onClick = { onTabSelected(tab) },
                        label = { Text(meta.first, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        leadingIcon = {
                            Icon(meta.second, contentDescription = null, modifier = Modifier.size(14.dp))
                        },
                        modifier = Modifier.height(32.dp)
                    )
                }
            }

            HorizontalDivider()

            // Contenido según la pestaña activa
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (uiState.activeSidePanelTab) {
                    TacticalSidePanelTab.NOTES_WIKI -> {
                        SidePanelNotesAndWikiTab(
                            dmNotes = dmNotes,
                            onNotesChanged = onNotesChanged,
                            wikiEntries = uiState.availableWikiEntries,
                            searchQuery = uiState.wikiSearchQuery,
                            onSearchQueryChanged = onWikiSearchQueryChanged
                        )
                    }
                    TacticalSidePanelTab.PINS -> {
                        SidePanelPinsTab(
                            pins = uiState.pins,
                            showSecretPins = uiState.showSecretPins,
                            lockedPinIds = uiState.lockedPinIds,
                            stickyPinId = uiState.stickyPinId,
                            onFocusPinCoordinates = onFocusPinCoordinates,
                            onPinClicked = onPinClicked,
                            onToggleStickyPin = onToggleStickyPin,
                            onTogglePinLocked = onTogglePinLocked,
                            onEditPin = onEditPin,
                            onDeletePin = onDeletePin,
                            onToggleShowSecretPins = onToggleShowSecretPins
                        )
                    }
                    TacticalSidePanelTab.DICE -> {
                        SidePanelDiceTab(
                            diceCount = uiState.sidePanelDiceCount,
                            diceSides = uiState.sidePanelDiceSides,
                            diceModifier = uiState.sidePanelDiceModifier,
                            lastResult = uiState.sidePanelLastDiceResult,
                            recentRolls = uiState.sidePanelRecentRolls,
                            onSetDice = onSetDice,
                            onRollDice = onRollDice
                        )
                    }
                    TacticalSidePanelTab.MYTHIC -> {
                        SidePanelMythicTab(
                            chaosFactor = uiState.sidePanelMythicChaos,
                            odds = uiState.sidePanelMythicOdds,
                            lastFateResult = uiState.sidePanelLastFateResult,
                            lastEventResult = uiState.sidePanelLastEventResult,
                            onRollFateCheck = onRollFateCheck,
                            onRollRandomEvent = onRollRandomEvent
                        )
                    }
                    TacticalSidePanelTab.COMBAT -> {
                        SidePanelCombatTab(
                            activeEncounter = uiState.activeEncounter,
                            tokens = uiState.tokens,
                            activeTurnTokenId = uiState.activeCombatTurnTokenId,
                            onAdvanceTurn = onAdvanceCombatTurn,
                            onSelectToken = onSelectToken
                        )
                    }
                }
            }
        }
    }
}

// ==================== SUB-TABS ====================

@Composable
private fun SidePanelNotesAndWikiTab(
    dmNotes: String,
    onNotesChanged: (String) -> Unit,
    wikiEntries: List<WikiEntry>,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Notas de la Sesión
        Text(
            text = "Notas Rápidas de la Sesión",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        OutlinedTextField(
            value = dmNotes,
            onValueChange = onNotesChanged,
            placeholder = { Text("Anotaciones, eventos, descubrimientos...", fontSize = 12.sp) },
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            textStyle = MaterialTheme.typography.bodySmall
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // Lore & Wiki de la Campaña
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Wiki & Lore de Campaña",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )
            Text(
                text = "${wikiEntries.size} artículos",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChanged,
            placeholder = { Text("Buscar en el wiki...", fontSize = 11.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        val filteredEntries = remember(wikiEntries, searchQuery) {
            if (searchQuery.isBlank()) wikiEntries
            else wikiEntries.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                it.content.contains(searchQuery, ignoreCase = true)
            }
        }

        if (filteredEntries.isEmpty()) {
            Text(
                text = "No se encontraron entradas de wiki",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                filteredEntries.take(10).forEach { entry ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = entry.title,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (entry.content.isNotBlank()) {
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = entry.content,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SidePanelDiceTab(
    diceCount: Int,
    diceSides: Int,
    diceModifier: Int,
    lastResult: DiceRollResult?,
    recentRolls: List<DiceRollResult>,
    onSetDice: (count: Int, sides: Int, modifier: Int) -> Unit,
    onRollDice: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Lanzador de Dados Táctico",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        // Selector de tipo de dado
        val standardDice = listOf(4, 6, 8, 10, 12, 20, 100)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            standardDice.forEach { sides ->
                val isSelected = diceSides == sides
                FilterChip(
                    selected = isSelected,
                    onClick = { onSetDice(diceCount, sides, diceModifier) },
                    label = { Text("d$sides", fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                    modifier = Modifier.height(30.dp)
                )
            }
        }

        // Steppers de Cantidad y Modificador
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cantidad de dados
            Column {
                Text("Cantidad", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilledTonalIconButton(
                        onClick = { onSetDice((diceCount - 1).coerceAtLeast(1), diceSides, diceModifier) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Text("-", fontWeight = FontWeight.Bold)
                    }
                    Text("$diceCount", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    FilledTonalIconButton(
                        onClick = { onSetDice((diceCount + 1).coerceAtMost(20), diceSides, diceModifier) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Text("+", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Modificador
            Column {
                Text("Modificador", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilledTonalIconButton(
                        onClick = { onSetDice(diceCount, diceSides, diceModifier - 1) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Text("-", fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = if (diceModifier >= 0) "+$diceModifier" else "$diceModifier",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    FilledTonalIconButton(
                        onClick = { onSetDice(diceCount, diceSides, diceModifier + 1) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Text("+", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Botón de Tirar
        Button(
            onClick = onRollDice,
            modifier = Modifier.fillMaxWidth().height(42.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.Default.Casino, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Tirar ${diceCount}d$diceSides" + (if (diceModifier > 0) "+$diceModifier" else if (diceModifier < 0) "$diceModifier" else ""),
                fontWeight = FontWeight.Bold
            )
        }

        // Resultado Último
        lastResult?.let { result ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Última Tirada:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(result.expression, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    Text(
                        text = "${result.total}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // Historial Reciente
        if (recentRolls.isNotEmpty()) {
            Text(
                text = "Historial Reciente:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                recentRolls.take(6).forEach { roll ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(roll.expression, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${roll.total}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SidePanelMythicTab(
    chaosFactor: Int,
    odds: ProbabilityLevel,
    lastFateResult: Pair<FateResult, Boolean>?,
    lastEventResult: String?,
    onRollFateCheck: (ProbabilityLevel, Int) -> Unit,
    onRollRandomEvent: () -> Unit
) {
    var selectedOdds by remember { mutableStateOf(odds) }
    var selectedChaos by remember { mutableIntStateOf(chaosFactor) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Oráculo Mythic GME 2e",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        // Factor de Caos
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Factor de Caos: $selectedChaos", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                FilledTonalIconButton(
                    onClick = { selectedChaos = (selectedChaos - 1).coerceAtLeast(1) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Text("-", fontWeight = FontWeight.Bold)
                }
                FilledTonalIconButton(
                    onClick = { selectedChaos = (selectedChaos + 1).coerceAtMost(9) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Text("+", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Nivel de Probabilidad
        Text("Probabilidad:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)

        val oddsList = listOf(
            ProbabilityLevel.CERTAIN to "Seguro",
            ProbabilityLevel.VERY_LIKELY to "Muy Probable",
            ProbabilityLevel.LIKELY to "Probable",
            ProbabilityLevel.FIFTY_FIFTY to "50 / 50",
            ProbabilityLevel.UNLIKELY to "Improbable",
            ProbabilityLevel.VERY_UNLIKELY to "Muy Improbable",
            ProbabilityLevel.IMPOSSIBLE to "Imposible"
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            oddsList.forEach { (prob, label) ->
                val isSelected = selectedOdds == prob
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedOdds = prob },
                    label = { Text(label, fontSize = 10.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                    modifier = Modifier.height(28.dp)
                )
            }
        }

        // Botón Preguntar al Destino (Fate Check)
        Button(
            onClick = { onRollFateCheck(selectedOdds, selectedChaos) },
            modifier = Modifier.fillMaxWidth().height(40.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.Default.HelpOutline, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Preguntar al Destino (d100)", fontWeight = FontWeight.Bold)
        }

        // Resultado Fate Check
        lastFateResult?.let { (fateResult, isRandomEvent) ->
            val (badgeText, badgeColor) = when (fateResult) {
                FateResult.EXCEPTIONAL_YES -> "¡SÍ EXCEPCIONAL!" to Color(0xFFFF9800)
                FateResult.YES -> "SÍ" to Color(0xFF4CAF50)
                FateResult.NO -> "NO" to Color(0xFFE53935)
                FateResult.EXCEPTIONAL_NO -> "¡NO EXCEPCIONAL!" to Color(0xFF880E4F)
                FateResult.NONE -> "---" to Color.Gray
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Resultado del Destino:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    Surface(shape = RoundedCornerShape(8.dp), color = badgeColor) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }

                    if (isRandomEvent) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.errorContainer
                        ) {
                            Text(
                                text = "⚡ ¡EVENTO ALEATORIO DISPARADO!",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // Botón Evento Aleatorio
        FilledTonalButton(
            onClick = onRollRandomEvent,
            modifier = Modifier.fillMaxWidth().height(38.dp)
        ) {
            Icon(Icons.Default.Casino, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Generar Evento (Acción + Tema)", fontSize = 12.sp)
        }

        lastEventResult?.let { event ->
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text("Evento de Significado:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    Text(
                        text = event,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun SidePanelCombatTab(
    activeEncounter: Encounter?,
    tokens: List<TacticalToken>,
    activeTurnTokenId: Long?,
    onAdvanceTurn: () -> Unit,
    onSelectToken: (Long) -> Unit
) {
    if (activeEncounter == null) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.SportsKabaddi, contentDescription = null, modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.outline)
            Spacer(Modifier.height(8.dp))
            Text("No hay combate activo", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
        }
        return
    }

    val sortedCreatures = activeEncounter.creatures.sortedBy { it.sortOrder }
    val currentTurnIndex = activeEncounter.currentTurnIndex.coerceIn(0, (sortedCreatures.size - 1).coerceAtLeast(0))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(activeEncounter.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text("Ronda ${activeEncounter.currentRound}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }

            Button(
                onClick = onAdvanceTurn,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.height(30.dp)
            ) {
                Icon(Icons.Default.SkipNext, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("Siguiente", fontSize = 11.sp)
            }
        }

        HorizontalDivider()

        sortedCreatures.forEachIndexed { index, creature ->
            val isActive = index == currentTurnIndex
            val matchingToken = tokens.find { it.encounterCreatureId == creature.id || (it.npcId != null && it.npcId == creature.npcId) }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                border = if (isActive) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = matchingToken != null) {
                        matchingToken?.let { onSelectToken(it.id) }
                    }
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(creature.name.take(1).uppercase(), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = creature.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("${creature.currentHp}/${creature.maxHp} HP", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                            Text("· CA ${creature.armorClass}", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                        }
                    }

                    if (matchingToken != null) {
                        Icon(
                            imageVector = Icons.Default.GpsFixed,
                            contentDescription = "Fijar en mapa",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SidePanelPinsTab(
    pins: List<TacticalMapPin>,
    showSecretPins: Boolean,
    lockedPinIds: Set<Long>,
    stickyPinId: Long?,
    onFocusPinCoordinates: (cellX: Float, cellY: Float) -> Unit,
    onPinClicked: (Long) -> Unit,
    onToggleStickyPin: (Long) -> Unit,
    onTogglePinLocked: (Long) -> Unit,
    onEditPin: (TacticalMapPin) -> Unit,
    onDeletePin: (Long) -> Unit,
    onToggleShowSecretPins: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") }

    val filteredPins = remember(pins, searchQuery, selectedFilter, showSecretPins) {
        pins.filter { pin ->
            if (pin.isSecret && !showSecretPins) return@filter false

            val matchesSearch = searchQuery.isBlank() ||
                pin.title.contains(searchQuery, ignoreCase = true) ||
                pin.description.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (selectedFilter) {
                "ROOMS" -> pin.iconName == "room" || (pin.title.startsWith("Sala ") || pin.title.all { it.isDigit() })
                "TRAPS" -> pin.iconName == "trap"
                "TREASURES" -> pin.iconName == "chest"
                "COMBAT" -> pin.iconName == "skull"
                "SECRETS" -> pin.isSecret
                else -> true
            }

            matchesSearch && matchesFilter
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Buscador
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Buscar salas, notas...", fontSize = 12.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Clear, contentDescription = "Limpiar", modifier = Modifier.size(14.dp))
                    }
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodySmall
        )

        // Filtros rápidos
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val filterOptions = listOf(
                "ALL" to "Todos (${pins.size})",
                "ROOMS" to "Salas",
                "TRAPS" to "Trampas",
                "TREASURES" to "Tesoros",
                "COMBAT" to "Combate",
                "SECRETS" to "Secretos"
            )
            filterOptions.forEach { (key, label) ->
                FilterChip(
                    selected = selectedFilter == key,
                    onClick = { selectedFilter = key },
                    label = { Text(label, fontSize = 11.sp) }
                )
            }
        }

        // Barra de estado de exploración y toggle de secretos
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val exploredCount = pins.count { it.explorationState == MapPinExplorationState.EXPLORED }
            Text(
                text = "Exploradas: $exploredCount / ${pins.size}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Secretos", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                IconButton(onClick = onToggleShowSecretPins, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = if (showSecretPins) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = "Alternar secretos",
                        tint = if (showSecretPins) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        HorizontalDivider()

        // Lista de pines
        if (filteredPins.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (searchQuery.isNotBlank()) "No se encontraron salas o pines." else "No hay pines en este mapa.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredPins, key = { it.id }) { pin ->
                    val isSticky = stickyPinId == pin.id
                    val isLocked = lockedPinIds.contains(pin.id)

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                        border = if (isSticky) androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFFFD54F)) else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onFocusPinCoordinates(pin.cellX, pin.cellY)
                                onPinClicked(pin.id)
                            }
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(Color(pin.colorHex)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = when {
                                            pin.title.startsWith("Sala ") && pin.title.removePrefix("Sala ").all { it.isDigit() } ->
                                                pin.title.removePrefix("Sala ")
                                            pin.title.isNotBlank() && pin.title.length <= 3 -> pin.title
                                            pin.iconName == "trap" -> "⚠"
                                            pin.iconName == "chest" -> "★"
                                            pin.iconName in listOf("door", "stairs", "portal") -> "⎅"
                                            pin.iconName == "skull" -> "☠"
                                            pin.iconName == "info" -> "i"
                                            pin.isSecret -> "?"
                                            else -> "!"
                                        },
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = pin.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text("(${pin.cellX.toInt()}, ${pin.cellY.toInt()})", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                        Text("• ${pin.explorationState.name}", fontSize = 10.sp, color = when(pin.explorationState) {
                                            MapPinExplorationState.EXPLORED -> MaterialTheme.colorScheme.outline
                                            MapPinExplorationState.DISCOVERED -> MaterialTheme.colorScheme.primary
                                            MapPinExplorationState.UNKNOWN -> MaterialTheme.colorScheme.tertiary
                                        })
                                    }
                                }

                                IconButton(
                                    onClick = { onTogglePinLocked(pin.id) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                        contentDescription = "Bloquear posición",
                                        tint = if (isLocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { onToggleStickyPin(pin.id) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PushPin,
                                        contentDescription = "Fijar nota",
                                        tint = if (isSticky) Color(0xFFFFD54F) else MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { onFocusPinCoordinates(pin.cellX, pin.cellY) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MyLocation,
                                        contentDescription = "Centrar cámara",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            if (pin.description.isNotBlank()) {
                                Text(
                                    text = pin.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
