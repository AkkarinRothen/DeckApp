package com.deckapp.feature.draw.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import com.deckapp.core.model.CardContentMode
import com.deckapp.core.ui.icons.DeckIcons
import com.deckapp.feature.draw.SessionUiState
import com.deckapp.feature.draw.delegates.StoryEngineGuideDelegate
import com.deckapp.feature.draw.model.StoryEngineGuideTab
import com.deckapp.feature.draw.model.StoryEngineProcedureCatalog
import com.deckapp.feature.draw.model.StoryEngineStep

/**
 * Barra superior contextual para The Story Engine / Deck of Worlds.
 * Muestra el paso actual del procedimiento y permite abrir la guía interactiva completa.
 */
@Composable
fun StoryEngineProcedureBar(
    tab: StoryEngineGuideTab,
    stepIndex: Int,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val steps = when (tab) {
        StoryEngineGuideTab.DECK_OF_WORLDS -> StoryEngineProcedureCatalog.deckOfWorldsSteps
        StoryEngineGuideTab.NEW_STORY_ENGINE -> StoryEngineProcedureCatalog.newStoryEngineSteps
        StoryEngineGuideTab.EXPANSIONS -> emptyList()
    }
    val currentStep = steps.getOrNull(stepIndex) ?: steps.firstOrNull()

    Surface(
        onClick = onOpen,
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Public,
                    contentDescription = null,
                    modifier = Modifier.size(17.dp),
                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${tab.title} · ${currentStep?.title ?: "Guía Narrativa"}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = currentStep?.subtitle ?: "Pulsa para consultar pasos y robar cartas",
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                )
            }

            Spacer(Modifier.width(8.dp))

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Abrir guía de Story Engine",
                tint = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}

/**
 * Modal Bottom Sheet interactivo con la guía de procedimientos de The Story Engine y Deck of Worlds.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryEngineProcedureGuide(
    state: SessionUiState,
    delegate: StoryEngineGuideDelegate,
    modifier: Modifier = Modifier
) {
    val currentTab = state.storyEngineGuideTab
    val stepIndex = state.storyEngineGuideStepIndex
    val steps = remember(currentTab) {
        when (currentTab) {
            StoryEngineGuideTab.DECK_OF_WORLDS -> StoryEngineProcedureCatalog.deckOfWorldsSteps
            StoryEngineGuideTab.NEW_STORY_ENGINE -> StoryEngineProcedureCatalog.newStoryEngineSteps
            StoryEngineGuideTab.EXPANSIONS -> emptyList()
        }
    }
    val currentStep = steps.getOrNull(stepIndex) ?: steps.firstOrNull()
    val listState = rememberLazyListState()

    LaunchedEffect(currentTab, stepIndex) {
        listState.animateScrollToItem(0)
    }

    ModalBottomSheet(
        onDismissRequest = delegate::close,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .imePadding()
                .padding(horizontal = 16.dp)
        ) {
            // Cabecera principal
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Public,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "The Story Engine · Guía de Sesión",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = delegate::close) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar guía")
                }
            }

            // Selector de Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StoryEngineGuideTab.values().forEach { tab ->
                    FilterChip(
                        selected = currentTab == tab,
                        onClick = { delegate.selectTab(tab) },
                        label = {
                            Text(
                                text = when (tab) {
                                    StoryEngineGuideTab.DECK_OF_WORLDS -> "Deck of Worlds (6)"
                                    StoryEngineGuideTab.NEW_STORY_ENGINE -> "New Story Engine (4)"
                                    StoryEngineGuideTab.EXPANSIONS -> "Expansiones"
                                },
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    )
                }
            }

            // Selector horizontal de pasos (para Deck of Worlds y New Story Engine)
            if (steps.isNotEmpty()) {
                val scrollState = rememberScrollState()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .horizontalScroll(scrollState),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    steps.forEachIndexed { idx, step ->
                        val isSelected = idx == stepIndex
                        Surface(
                            onClick = { delegate.selectStep(idx) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        ) {
                            Text(
                                text = "${idx + 1}. ${step.title.substringBefore(":")}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // Contenido dinámico
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (currentTab == StoryEngineGuideTab.EXPANSIONS) {
                    item {
                        ExpansionsGuideContent(
                            onOpenMicrosettingBuilder = delegate::launchMicrosettingBuilder
                        )
                    }
                } else if (currentStep != null) {
                    item {
                        StepDetailCard(
                            step = currentStep,
                            state = state,
                            delegate = delegate
                        )
                    }

                    // Botones de navegación inferior
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = delegate::prevStep,
                                enabled = stepIndex > 0
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Paso Anterior")
                            }

                            if (stepIndex < steps.size - 1) {
                                Button(onClick = delegate::nextStep) {
                                    Text("Paso Siguiente")
                                    Spacer(Modifier.width(4.dp))
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            } else {
                                FilledTonalButton(onClick = delegate::launchMicrosettingBuilder) {
                                    Icon(DeckIcons.Board, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Ver en Mesa")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepDetailCard(
    step: StoryEngineStep,
    state: SessionUiState,
    delegate: StoryEngineGuideDelegate
) {
    val matchingDeck = remember(step, state.allDecks, state.deckRefs) {
        delegate.findDeckForStep(step)
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Badges superiores
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Fase ${step.stepIndex + 1} de ${if (step.tab == StoryEngineGuideTab.DECK_OF_WORLDS) 6 else 4}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontWeight = FontWeight.Bold
                    )
                }

                val orientationBadgeText = when (step.contentMode) {
                    CardContentMode.FOUR_EDGE_CUES -> "4 pistas cardinales · 1d4 (Giro 90°)"
                    CardContentMode.TOP_BOTTOM_SPLIT -> "2 opciones · 1d2 (Giro 180°)"
                    CardContentMode.IMAGE_WITH_TEXT -> "Núcleo Central · Fija (0°)"
                    else -> "Multiopción"
                }

                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = orientationBadgeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Título y subtítulo
            Column {
                Text(
                    text = step.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = step.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Descripción principal del paso
            Text(
                text = step.description,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 20.sp
            )

            // Panel de Acciones Interactivas
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Acciones en Mesa:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { delegate.drawStepCard(step) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (matchingDeck != null) "Robar (${matchingDeck.name})" else "Robar Carta",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (step.contentMode != CardContentMode.IMAGE_WITH_TEXT) {
                            OutlinedButton(
                                onClick = { delegate.rotateStepCard(step) }
                            ) {
                                Icon(Icons.Default.RotateRight, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(if (step.contentMode == CardContentMode.FOUR_EDGE_CUES) "Rotar (90°)" else "Rotar (180°)")
                            }
                        }
                    }

                    if (step.tab == StoryEngineGuideTab.DECK_OF_WORLDS) {
                        FilledTonalButton(
                            onClick = delegate::launchMicrosettingBuilder,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(DeckIcons.Board, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Abrir Generador Modular de Microsetting")
                        }
                    }
                }
            }

            // Consejo Creativo
            Surface(
                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = step.creativeTip,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }

            // Preguntas Guía
            if (step.sampleQuestions.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Preguntas para inspirar la escena:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    step.sampleQuestions.forEach { question ->
                        Row(verticalAlignment = Alignment.Top) {
                            Text("• ", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(
                                text = question,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpansionsGuideContent(
    onOpenMicrosettingBuilder: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Arquitectura de Expansiones de Deck of Worlds",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Deck of Worlds se expande en tres dimensiones creativas diseñadas para mezclarse sin fricción:",
                    style = MaterialTheme.typography.bodyMedium
                )

                // 1. Cartographic
                ExpansionCategoryItem(
                    title = "1. Expansiones Cartográficas (Conexión de Mundos)",
                    packs = "Horizons, Coastlines, Wilderness, Depths, Settlements",
                    description = "Añaden nuevas cartas de Región e Hitos especializados. Permiten conectar múltiples microsettings entre sí: cuando los personajes viajan fuera del Pantano inicial, robas una carta de Costa o Frontera para enlazar el nuevo microsetting como un archipiélago continuo."
                )

                // 2. Creative
                ExpansionCategoryItem(
                    title = "2. Expansiones Creativas & Culturales",
                    packs = "Lore Fragments, Keys to the City, Nomads, Ruins & Antiquities",
                    description = "Amplían los pasos de Atributo y Orígenes con pistas de facciones, rituales religiosos, misterios arqueológicos y clanes itinerantes que viajan entre regiones."
                )

                // 3. Genre
                ExpansionCategoryItem(
                    title = "3. Expansiones de Género",
                    packs = "Sci-Fi, Cyberpunk, Gothic Fantasy, Post-Apocalyptic, Steampunk",
                    description = "Inyectan tropos temáticos directos a cualquier paso. Puedes tomar una carta de Región fantástica y rotar una pista de Ciencia Ficción para transformarla en una megaestructura orbital abandonada."
                )

                FilledTonalButton(
                    onClick = onOpenMicrosettingBuilder,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Icon(DeckIcons.Board, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Generar Cluster en Mesa")
                }
            }
        }
    }
}

@Composable
private fun ExpansionCategoryItem(
    title: String,
    packs: String,
    description: String
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Packs: $packs",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
