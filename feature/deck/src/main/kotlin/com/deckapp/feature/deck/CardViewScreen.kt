package com.deckapp.feature.deck

import android.app.Activity
import android.view.WindowManager
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.deckapp.core.model.CardContentMode
import com.deckapp.core.model.CardFace
import com.deckapp.core.model.ContentZone
import com.deckapp.core.ui.components.MarkdownText
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun CardViewScreen(
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    viewModel: CardViewViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Estado local: cuadrante seleccionado en modo FOUR_QUADRANT
    var selectedQuadrant by remember { mutableIntStateOf(-1) }
    // Resetear al cambiar de carta o de cara
    LaunchedEffect(uiState.card?.id, uiState.card?.currentFaceIndex) {
        selectedQuadrant = -1
    }

    // Modo "Mostrar a jugador"
    var showPlayerView by remember { mutableStateOf(false) }
    if (showPlayerView && uiState.card != null) {
        PlayerViewOverlay(
            face = uiState.card!!.activeFace,
            cardTitle = uiState.card!!.title,
            onDismiss = { showPlayerView = false }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.card?.title ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    uiState.card?.let { card ->
                        // Botón "Mostrar a jugador" — siempre visible cuando hay carta
                        IconButton(onClick = { showPlayerView = true }) {
                            Icon(Icons.Default.Visibility, contentDescription = "Mostrar a jugador")
                        }
                        if (card.activeFace.contentMode == CardContentMode.FOUR_EDGE_CUES) {
                            IconButton(onClick = { viewModel.rotate90() }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Rotar orientación")
                            }
                        }
                    }
                }
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            uiState.card == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Carta no encontrada")
            }

            else -> {
                val card = uiState.card!!
                val face = card.activeFace
                val isFlippable = face.contentMode == CardContentMode.DOUBLE_SIDED_FULL
                        && card.faces.size > 1

                Column(modifier = Modifier.fillMaxSize().padding(padding)) {

                    // ── Área de imagen ────────────────────────────────────────────
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(16.dp)
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        if (isFlippable) {
                            // DOUBLE_SIDED_FULL: animación de deslizamiento al voltear cara
                            AnimatedContent(
                                targetState = card.currentFaceIndex,
                                transitionSpec = {
                                    if (targetState > initialState)
                                        (slideInHorizontally { it } + fadeIn()) togetherWith
                                        (slideOutHorizontally { -it } + fadeOut())
                                    else
                                        (slideInHorizontally { -it } + fadeIn()) togetherWith
                                        (slideOutHorizontally { it } + fadeOut())
                                },
                                modifier = Modifier.fillMaxSize(),
                                label = "card-face-flip"
                            ) { faceIndex ->
                                CardFaceImage(
                                    face = card.faces.getOrElse(faceIndex) { card.faces.first() },
                                    title = card.title
                                )
                            }
                        } else {
                            CardFaceImage(face = face, title = card.title)

                            // TOP_BOTTOM_SPLIT: línea divisoria central
                            if (face.contentMode == CardContentMode.TOP_BOTTOM_SPLIT) {
                                HorizontalDivider(
                                    modifier = Modifier.align(Alignment.Center),
                                    thickness = 2.dp,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                )
                            }

                            // FOUR_QUADRANT: overlay 2×2 semitransparente
                            if (face.contentMode == CardContentMode.FOUR_QUADRANT) {
                                FourQuadrantOverlay(
                                    selectedQuadrant = selectedQuadrant,
                                    onQuadrantSelected = { q ->
                                        selectedQuadrant = if (selectedQuadrant == q) -1 else q
                                    }
                                )
                            }
                        }
                    }

                    // ── Panel de zonas de contenido ───────────────────────────────
                    when (face.contentMode) {
                        CardContentMode.TOP_BOTTOM_SPLIT -> {
                            TopBottomZonePanel(zones = face.zones)
                        }
                        CardContentMode.FOUR_QUADRANT -> {
                            FourQuadrantZonePanel(
                                zones = face.zones,
                                selectedQuadrant = selectedQuadrant
                            )
                        }
                        else -> {
                            val activeZone = card.activeZone
                            if (activeZone != null && activeZone.text.isNotBlank()) {
                                GenericZonePanel(
                                    zone = activeZone,
                                    contentMode = face.contentMode,
                                    isReversed = card.isReversed,
                                    currentRotation = card.currentRotation,
                                    onToggleReversed = { viewModel.toggleReversed() }
                                )
                            }
                        }
                    }

                    // ── Botón de voltear cara (DOUBLE_SIDED_FULL) ─────────────────
                    if (card.faces.size > 1) {
                        OutlinedButton(
                            onClick = { viewModel.flipFace() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text("Voltear carta  (cara ${card.currentFaceIndex + 1}/${card.faces.size})")
                        }
                    }
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Modo "Mostrar a jugador"
// ──────────────────────────────────────────────────────────────────────────────

/**
 * Overlay full-screen que oculta toda la UI del DM.
 * Activa brillo máximo mientras está visible; lo restaura al cerrarse.
 * Tocar en cualquier lugar cierra el modo.
 */
@Composable
private fun PlayerViewOverlay(face: CardFace, cardTitle: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val window = (context as? Activity)?.window

    DisposableEffect(Unit) {
        val lp = window?.attributes
        lp?.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
        window?.attributes = lp
        onDispose {
            val p = window?.attributes
            p?.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            window?.attributes = p
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        val imagePath = face.imagePath
        if (imagePath != null) {
            AsyncImage(
                model = File(imagePath),
                contentDescription = cardTitle,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        } else {
            Text(
                cardTitle,
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(32.dp)
            )
        }
        // Indicador sutil de toque
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp),
            color = Color.Black.copy(alpha = 0.5f),
            shape = RoundedCornerShape(50)
        ) {
            Text(
                "Tocar para cerrar",
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Subcomponentes privados
// ──────────────────────────────────────────────────────────────────────────────

/** Imagen de una cara de la carta. Placeholder con iniciales si no hay imagen. */
@Composable
private fun CardFaceImage(face: CardFace, title: String) {
    val imagePath = face.imagePath
    if (imagePath != null) {
        AsyncImage(
            model = File(imagePath),
            contentDescription = title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title.take(2).uppercase(),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Overlay 2×2 semitransparente para [CardContentMode.FOUR_QUADRANT].
 * Índices: 0=NO, 1=NE, 2=SO, 3=SE.
 */
@Composable
private fun FourQuadrantOverlay(
    selectedQuadrant: Int,
    onQuadrantSelected: (Int) -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
            QuadrantCell(0, selectedQuadrant, primary, Modifier.weight(1f).fillMaxHeight(), onQuadrantSelected)
            VerticalDivider(color = primary.copy(alpha = 0.4f))
            QuadrantCell(1, selectedQuadrant, primary, Modifier.weight(1f).fillMaxHeight(), onQuadrantSelected)
        }
        HorizontalDivider(color = primary.copy(alpha = 0.4f))
        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
            QuadrantCell(2, selectedQuadrant, primary, Modifier.weight(1f).fillMaxHeight(), onQuadrantSelected)
            VerticalDivider(color = primary.copy(alpha = 0.4f))
            QuadrantCell(3, selectedQuadrant, primary, Modifier.weight(1f).fillMaxHeight(), onQuadrantSelected)
        }
    }
}

@Composable
private fun QuadrantCell(
    index: Int,
    selectedQuadrant: Int,
    primaryColor: Color,
    modifier: Modifier,
    onSelect: (Int) -> Unit
) {
    val isSelected = selectedQuadrant == index
    Box(
        modifier = modifier
            .background(if (isSelected) primaryColor.copy(alpha = 0.25f) else Color.Transparent)
            .clickable { onSelect(index) },
        contentAlignment = Alignment.TopStart
    ) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .padding(6.dp)
                    .size(8.dp)
                    .background(primaryColor, CircleShape)
            )
        }
    }
}

/** Panel de zona para FOUR_QUADRANT: muestra el texto del cuadrante seleccionado. */
@Composable
private fun FourQuadrantZonePanel(zones: List<ContentZone>, selectedQuadrant: Int) {
    val quadrantLabels = listOf("Noroeste", "Noreste", "Suroeste", "Sureste")
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (selectedQuadrant == -1) {
                Text(
                    text = "Toca un cuadrante para ver su contenido",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                val zone = zones.getOrNull(selectedQuadrant)
                Text(
                    text = quadrantLabels.getOrElse(selectedQuadrant) { "Zona" },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(4.dp))
                if (zone != null && zone.text.isNotBlank()) {
                    MarkdownText(
                        markdown = zone.text,
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .heightIn(max = 160.dp)
                    )
                }
            }
        }
    }
}

/** Panel de dos zonas para [CardContentMode.TOP_BOTTOM_SPLIT]. */
@Composable
private fun TopBottomZonePanel(zones: List<ContentZone>) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val labels = listOf("Superior", "Inferior")
            zones.take(2).forEachIndexed { i, zone ->
                if (zone.text.isNotBlank()) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = labels.getOrElse(i) { "Zona ${i + 1}" },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(4.dp))
                            MarkdownText(
                                markdown = zone.text,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState())
                                    .heightIn(max = 120.dp),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Panel genérico para IMAGE_WITH_TEXT, REVERSIBLE, FOUR_EDGE_CUES,
 * LEFT_RIGHT_SPLIT e IMAGE_ONLY con texto.
 */
@Composable
private fun GenericZonePanel(
    zone: ContentZone,
    contentMode: CardContentMode,
    isReversed: Boolean,
    currentRotation: Int,
    onToggleReversed: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // REVERSIBLE: toggle derecho/invertido
            if (contentMode == CardContentMode.REVERSIBLE) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    FilterChip(
                        selected = !isReversed,
                        onClick = { if (isReversed) onToggleReversed() },
                        label = { Text("Derecho") }
                    )
                    Spacer(Modifier.width(8.dp))
                    FilterChip(
                        selected = isReversed,
                        onClick = { if (!isReversed) onToggleReversed() },
                        label = { Text("Invertido") }
                    )
                }
                Spacer(Modifier.height(8.dp))
            }

            // FOUR_EDGE_CUES: etiqueta de dirección activa
            if (contentMode == CardContentMode.FOUR_EDGE_CUES) {
                Text(
                    text = when (currentRotation) {
                        0 -> "Norte"; 90 -> "Este"; 180 -> "Sur"; else -> "Oeste"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(4.dp))
            }

            MarkdownText(
                markdown = zone.text,
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .heightIn(max = 200.dp)
            )
        }
    }
}
