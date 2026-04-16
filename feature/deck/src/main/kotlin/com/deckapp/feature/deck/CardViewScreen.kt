package com.deckapp.feature.deck

import android.app.Activity
import android.view.WindowManager
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.material.icons.filled.ViewStream
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.material3.LocalContentColor
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
    // V-6: modo "ver todas las caras"
    var showAllFaces by remember { mutableStateOf(false) }
    // V-7: modo brújula para FOUR_EDGE_CUES
    var showCompass by remember { mutableStateOf(false) }
    // Sprint 15: Sheet de notas
    var showNotesSheet by remember { mutableStateOf(false) }
    
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
                        // V-6: toggle "ver todas las caras"
                        if (card.faces.size > 1) {
                            IconButton(onClick = { showAllFaces = !showAllFaces; showCompass = false }) {
                                Icon(
                                    Icons.Default.ViewStream,
                                    contentDescription = "Ver todas las caras",
                                    tint = if (showAllFaces) MaterialTheme.colorScheme.primary
                                           else LocalContentColor.current
                                )
                            }
                        }
                        // V-7: toggle brújula FOUR_EDGE_CUES
                        if (card.activeFace.contentMode == CardContentMode.FOUR_EDGE_CUES) {
                            IconButton(onClick = { showCompass = !showCompass; showAllFaces = false }) {
                                Icon(
                                    Icons.Default.Explore,
                                    contentDescription = "Vista brújula",
                                    tint = if (showCompass) MaterialTheme.colorScheme.primary
                                           else LocalContentColor.current
                                )
                            }
                            if (!showCompass) {
                                IconButton(onClick = { viewModel.rotate90() }) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Rotar orientación")
                                }
                            }
                        }
                        // Botón "Mostrar a jugador"
                        IconButton(onClick = { showPlayerView = true }) {
                            Icon(Icons.Default.Visibility, contentDescription = "Mostrar a jugador")
                        }
                        
                        // Botón de Notas (Sprint 15)
                        IconButton(onClick = { showNotesSheet = true }) {
                            val hasNotes = !card.dmNotes.isNullOrBlank()
                            Icon(
                                imageVector = if (hasNotes) Icons.Default.StickyNote2 else Icons.Default.Edit,
                                contentDescription = "Notas del DM",
                                tint = if (hasNotes) MaterialTheme.colorScheme.tertiary else LocalContentColor.current
                            )
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

                when {
                    // V-6: modo referencia — todas las caras en columna
                    showAllFaces -> AllFacesView(
                        card = card,
                        modifier = Modifier.fillMaxSize().padding(padding)
                    )

                    // V-7: brújula FOUR_EDGE_CUES
                    showCompass && face.contentMode == CardContentMode.FOUR_EDGE_CUES -> {
                        FourEdgeCompassView(
                            face = face,
                            activeRotation = card.currentRotation,
                            onZoneTap = { viewModel.setRotation(it) },
                            modifier = Modifier.fillMaxSize().padding(padding)
                        )
                    }

                    // Vista normal
                    else -> Column(modifier = Modifier.fillMaxSize().padding(padding)) {

                        // ── Área de imagen ────────────────────────────────────────────
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(16.dp)
                                .clip(RoundedCornerShape(12.dp))
                        ) {
                            if (isFlippable) {
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

                                if (face.contentMode == CardContentMode.TOP_BOTTOM_SPLIT) {
                                    HorizontalDivider(
                                        modifier = Modifier.align(Alignment.Center),
                                        thickness = 2.dp,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                    )
                                }
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

                        // ── Panel de zonas ────────────────────────────────────────────
                        when (face.contentMode) {
                            CardContentMode.TOP_BOTTOM_SPLIT -> TopBottomZonePanel(zones = face.zones)
                            CardContentMode.FOUR_QUADRANT -> FourQuadrantZonePanel(
                                zones = face.zones, selectedQuadrant = selectedQuadrant
                            )
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

                        // ── V-5: Tira de miniaturas de caras ──────────────────────────
                        if (card.faces.size > 1) {
                            FaceStrip(
                                card = card,
                                onFaceSelected = { viewModel.jumpToFace(it) },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showNotesSheet && uiState.card != null) {
        CardNotesBottomSheet(
            notes = uiState.card?.dmNotes ?: "",
            onNotesChange = { viewModel.updateNotes(it) },
            onDismiss = { showNotesSheet = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CardNotesBottomSheet(
    notes: String,
    onNotesChange: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    var text by remember { mutableStateOf(notes) }
    
    // Auto-guardado al cerrar o cambiar texto (debounce real se haría en el VM, aqui es simple)
    LaunchedEffect(text) {
        onNotesChange(text)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.StickyNote2,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    "Notas Privadas del DM",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            Spacer(Modifier.height(16.dp))
            
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp),
                placeholder = { Text("Escribe anotaciones para esta carta... (Markdown soportado)") },
                shape = RoundedCornerShape(12.dp)
            )
            
            Spacer(Modifier.height(16.dp))
            
            if (text.isNotBlank()) {
                Text(
                    "Vista previa",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.tertiary
                )
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Box(Modifier.padding(12.dp)) {
                        MarkdownText(markdown = text)
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
// V-5 — Tira de miniaturas de caras
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun FaceStrip(
    card: com.deckapp.core.model.Card,
    onFaceSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(card.faces) { index, face ->
            val isActive = index == card.currentFaceIndex
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(56.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .then(
                        if (isActive) Modifier.border(
                            2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp)
                        ) else Modifier
                    )
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onFaceSelected(index) },
                contentAlignment = Alignment.Center
            ) {
                if (face.imagePath != null) {
                    coil.compose.AsyncImage(
                        model = java.io.File(face.imagePath!!),
                        contentDescription = face.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = (index + 1).toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isActive) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// V-6 — Modo referencia: todas las caras en columna
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun AllFacesView(
    card: com.deckapp.core.model.Card,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        itemsIndexed(card.faces) { index, face ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Cara ${index + 1}: ${face.name}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    CardFaceImage(face = face, title = card.title)
                }
                if (index < card.faces.size - 1) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// V-7 — Vista brújula para FOUR_EDGE_CUES
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun FourEdgeCompassView(
    face: com.deckapp.core.model.CardFace,
    activeRotation: Int,
    onZoneTap: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // zones[0]=Norte (rot 0), zones[1]=Este (rot 90), zones[2]=Sur (rot 180), zones[3]=Oeste (rot 270)
    val labels = listOf("N", "E", "S", "O")
    val rotations = listOf(0, 90, 180, 270)

    Column(
        modifier = modifier.padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Norte
        CompassZoneChip(
            text = face.zones.getOrNull(0)?.text ?: "",
            label = "Norte",
            isActive = activeRotation == 0,
            onClick = { onZoneTap(0) },
            modifier = Modifier.fillMaxWidth(0.7f)
        )

        Row(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Oeste
            CompassZoneChip(
                text = face.zones.getOrNull(3)?.text ?: "",
                label = "Oeste",
                isActive = activeRotation == 270,
                onClick = { onZoneTap(270) },
                modifier = Modifier.weight(1f).fillMaxHeight()
            )

            // Imagen central
            Box(
                modifier = Modifier
                    .weight(2f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp))
            ) {
                CardFaceImage(face = face, title = "")
            }

            // Este
            CompassZoneChip(
                text = face.zones.getOrNull(1)?.text ?: "",
                label = "Este",
                isActive = activeRotation == 90,
                onClick = { onZoneTap(90) },
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
        }

        // Sur
        CompassZoneChip(
            text = face.zones.getOrNull(2)?.text ?: "",
            label = "Sur",
            isActive = activeRotation == 180,
            onClick = { onZoneTap(180) },
            modifier = Modifier.fillMaxWidth(0.7f)
        )
    }
}

@Composable
private fun CompassZoneChip(
    text: String,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = if (isActive) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = if (isActive) 4.dp else 0.dp
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (isActive) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (text.isNotBlank()) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurface,
                    maxLines = 4,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
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
