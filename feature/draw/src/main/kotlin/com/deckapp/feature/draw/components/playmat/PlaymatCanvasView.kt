package com.deckapp.feature.draw.components.playmat

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.deckapp.core.model.*
import com.deckapp.core.ui.util.toCoilModel
import com.deckapp.feature.draw.SessionUiState
import kotlin.math.roundToInt

/**
 * Vista interactiva principal del Tapete de Cartas espacial 2D (Playmat).
 */
@Composable
fun PlaymatCanvasView(
    uiState: SessionUiState,
    onPlaceCardOnPlaymat: (Long, Float, Float) -> Unit,
    onMoveCard: (Long, Float, Float) -> Unit,
    onRotateCard: (Long, Int) -> Unit,
    onFlipCard: (Long) -> Unit,
    onReturnToHand: (Long) -> Unit,
    onDiscardCard: (Long) -> Unit,
    onSelectCard: (Long?) -> Unit,
    onToggleHandTray: () -> Unit,
    onSelectSurface: (String) -> Unit,
    onCreateSurface: (String, PlaymatTheme, PlaymatTemplateType) -> Unit,
    onDeleteSurface: (String) -> Unit,
    onSetTheme: (PlaymatTheme) -> Unit,
    onSetTemplate: (PlaymatTemplateType) -> Unit,
    onUpdateCamera: (Float, Float, Float) -> Unit,
    onQuickDraw: (Long, Float, Float) -> Unit,
    onAddStickyNote: (String, Float, Float) -> Unit,
    onDeleteStickyNote: (String) -> Unit,
    onCardDetailClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val activeSurface = uiState.activePlaymatSurface
    val allSurfaces = uiState.boardMemory.playmatSurfaces.ifEmpty { listOf(activeSurface) }

    var cameraX by remember(activeSurface.id) { mutableFloatStateOf(activeSurface.cameraX) }
    var cameraY by remember(activeSurface.id) { mutableFloatStateOf(activeSurface.cameraY) }
    var cameraZoom by remember(activeSurface.id) { mutableFloatStateOf(activeSurface.cameraZoom.coerceIn(0.4f, 2.5f)) }

    var showQuickDrawDialog by remember { mutableStateOf(false) }
    var showStickyNoteDialog by remember { mutableStateOf(false) }
    var stickyNoteText by remember { mutableStateOf("") }

    val cardsById = remember(uiState.hand, uiState.pile) {
        (uiState.hand + uiState.pile).associateBy { it.id }
    }

    val placedCardIds = remember(activeSurface.cards) {
        activeSurface.cards.map { it.cardId }.toSet()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(activeSurface.id) {
                detectTransformGestures { _, pan, zoom, _ ->
                    cameraZoom = (cameraZoom * zoom).coerceIn(0.4f, 2.5f)
                    cameraX += pan.x
                    cameraY += pan.y
                    onUpdateCamera(cameraX, cameraY, cameraZoom)
                }
            }
            .pointerInput(activeSurface.id) {
                detectTapGestures(
                    onTap = {
                        onSelectCard(null)
                    }
                )
            }
    ) {
        // 1. Fondo Texturizado / Tapete
        PlaymatBackground(theme = activeSurface.backgroundTheme)

        // 2. Contenido Espacial 2D Transformado (Plantillas, Cartas, Notas)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(cameraX.roundToInt(), cameraY.roundToInt()) }
        ) {
            // Renderizado de Moldes y Plantillas Temáticas
            PlaymatTemplateSilhouettes(
                template = activeSurface.template,
                zoom = cameraZoom
            )

            // Fichas de Tablas
            activeSurface.tableChips.forEach { chip ->
                PlaymatTableChipItem(
                    chip = chip,
                    zoom = cameraZoom
                )
            }

            // Notas Adhesivas
            activeSurface.stickyNotes.forEach { note ->
                PlaymatStickyNoteItem(
                    note = note,
                    zoom = cameraZoom,
                    onDelete = { onDeleteStickyNote(note.id) }
                )
            }

            // Cartas colocadas sobre el tapete
            activeSurface.cards.sortedBy { it.zIndex }.forEach { placement ->
                val card = cardsById[placement.cardId]
                if (card != null) {
                    val isSelected = uiState.selectedPlaymatCardId == placement.cardId
                    val stackId = card.originDeckId ?: card.stackId
                    val backImage = uiState.deckBackImages[stackId]

                    key(placement.cardId) {
                        PlaymatCardItem(
                            placement = placement,
                            card = card,
                            backImage = backImage,
                            isSelected = isSelected,
                            zoom = cameraZoom,
                            onCardClick = {
                                onSelectCard(if (isSelected) null else placement.cardId)
                            },
                            onMove = { dx, dy ->
                                onMoveCard(
                                    placement.cardId,
                                    placement.x + (dx / cameraZoom),
                                    placement.y + (dy / cameraZoom)
                                )
                            }
                        )
                    }
                }
            }
        }

        // 3. Menú Radial Contextual sobre la carta seleccionada
        val selectedCardPlacement = activeSurface.cards.find { it.cardId == uiState.selectedPlaymatCardId }
        if (selectedCardPlacement != null) {
            val cardScreenX = (selectedCardPlacement.x * cameraZoom) + cameraX
            val cardScreenY = (selectedCardPlacement.y * cameraZoom) + cameraY

            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (cardScreenX - 60).roundToInt(),
                            (cardScreenY - 80).roundToInt()
                        )
                    }
            ) {
                PlaymatRadialMenu(
                    visible = true,
                    onRotate = { onRotateCard(selectedCardPlacement.cardId, 90) },
                    onFlip = { onFlipCard(selectedCardPlacement.cardId) },
                    onReturnToHand = { onReturnToHand(selectedCardPlacement.cardId) },
                    onDiscard = { onDiscardCard(selectedCardPlacement.cardId) },
                    onInspect = { onCardDetailClick(selectedCardPlacement.cardId) },
                    onDismiss = { onSelectCard(null) }
                )
            }
        }

        // 4. Barra Superior Flotante del Tapete
        PlaymatToolbar(
            activeSurface = activeSurface,
            allSurfaces = allSurfaces,
            onSelectSurface = onSelectSurface,
            onCreateSurface = onCreateSurface,
            onDeleteSurface = onDeleteSurface,
            onSetTheme = onSetTheme,
            onSetTemplate = onSetTemplate,
            onResetCamera = {
                cameraX = 0f
                cameraY = 0f
                cameraZoom = 1f
                onUpdateCamera(0f, 0f, 1f)
            },
            onAddStickyNote = {
                stickyNoteText = ""
                showStickyNoteDialog = true
            },
            onOpenQuickDraw = { showQuickDrawDialog = true },
            modifier = Modifier.align(Alignment.TopStart)
        )

        // 5. Bandeja Inferior Acoplable de la Mano
        PlaymatHandTray(
            visible = uiState.showPlaymatHandTray,
            hand = uiState.hand,
            placedCardIds = placedCardIds,
            deckNames = uiState.deckNames,
            deckAspectRatios = uiState.deckAspectRatios,
            deckBackImages = uiState.deckBackImages,
            sessionId = uiState.sessionId,
            onToggleVisible = onToggleHandTray,
            onPlaceOnPlaymat = { cardId ->
                // Colocar en el centro visible actual del tapete
                val centerX = (-cameraX + 300) / cameraZoom
                val centerY = (-cameraY + 300) / cameraZoom
                onPlaceCardOnPlaymat(cardId, centerX, centerY)
            },
            onCardClick = onCardDetailClick,
            onDiscardCard = onDiscardCard,
            onRotateCard = { cardId -> onRotateCard(cardId, 90) },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    // Diálogo de Robo Rápido
    if (showQuickDrawDialog) {
        AlertDialog(
            onDismissRequest = { showQuickDrawDialog = false },
            title = { Text("Robar Carta al Tapete") },
            text = {
                if (uiState.deckRefs.isEmpty()) {
                    Text("No hay mazos activos en esta sesión. Añade uno desde la biblioteca.")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        uiState.deckRefs.forEach { ref ->
                            val deckName = uiState.deckNames[ref.stackId] ?: "Mazo"
                            val available = uiState.deckCardCounts[ref.stackId] ?: 0
                            FilledTonalButton(
                                onClick = {
                                    showQuickDrawDialog = false
                                    val centerX = (-cameraX + 350) / cameraZoom
                                    val centerY = (-cameraY + 350) / cameraZoom
                                    onQuickDraw(ref.stackId, centerX, centerY)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("$deckName ($available disponibles)")
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showQuickDrawDialog = false }) { Text("Cerrar") }
            }
        )
    }

    // Diálogo de Nueva Nota Adhesiva
    if (showStickyNoteDialog) {
        AlertDialog(
            onDismissRequest = { showStickyNoteDialog = false },
            title = { Text("Nueva Nota Adhesiva") },
            text = {
                OutlinedTextField(
                    value = stickyNoteText,
                    onValueChange = { stickyNoteText = it },
                    label = { Text("Contenido de la nota") },
                    placeholder = { Text("Escribe una pista o recordatorio...") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showStickyNoteDialog = false
                        val centerX = (-cameraX + 300) / cameraZoom
                        val centerY = (-cameraY + 300) / cameraZoom
                        onAddStickyNote(stickyNoteText, centerX, centerY)
                    }
                ) {
                    Text("Añadir al Tapete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStickyNoteDialog = false }) { Text("Cancelar") }
            }
        )
    }
}

/**
 * Renderizado de carta física colocada en el tapete 2D.
 */
@Composable
private fun PlaymatCardItem(
    placement: PlaymatCardPlacement,
    card: Card,
    backImage: String?,
    isSelected: Boolean,
    zoom: Float,
    onCardClick: () -> Unit,
    onMove: (Float, Float) -> Unit
) {
    val width = 120.dp * zoom
    val height = 180.dp * zoom

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    (placement.x * zoom).roundToInt(),
                    (placement.y * zoom).roundToInt()
                )
            }
            .size(width, height)
            .rotate(placement.rotationDegrees.toFloat())
            .pointerInput(placement.cardId) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onMove(dragAmount.x, dragAmount.y)
                }
            }
            .clickable { onCardClick() }
            .shadow(
                elevation = if (isSelected) 14.dp else 6.dp,
                shape = RoundedCornerShape(8.dp)
            )
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = if (isSelected) 2.5.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                shape = RoundedCornerShape(8.dp)
            )
    ) {
        if (placement.isFaceUp) {
            // Frente de la carta
            val frontImage = card.faces.firstOrNull()?.imagePath ?: card.faces.firstOrNull()?.reversedImagePath
            if (frontImage != null) {
                AsyncImage(
                    model = frontImage.toCoilModel(),
                    contentDescription = card.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = card.title,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = card.faces.firstOrNull()?.name ?: "Frente",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            // Reverso de la carta
            if (backImage != null) {
                AsyncImage(
                    model = backImage.toCoilModel(),
                    contentDescription = "Reverso",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Style,
                        contentDescription = "Reverso",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }
    }
}

/**
 * Renderiza el fondo del tapete con texturas de casino/taberna y rejilla de puntos.
 */
@Composable
private fun PlaymatBackground(theme: PlaymatTheme) {
    val (primaryColor, secondaryColor) = when (theme) {
        PlaymatTheme.FELT_GREEN -> Color(0xFF1B4D3E) to Color(0xFF0F2E25)
        PlaymatTheme.FELT_RED -> Color(0xFF6B1D2F) to Color(0xFF3F101B)
        PlaymatTheme.RUSTIC_WOOD -> Color(0xFF3E2723) to Color(0xFF261715)
        PlaymatTheme.PARCHMENT_LEATHER -> Color(0xFFD7CCC8) to Color(0xFFA1887F)
        PlaymatTheme.DARK_MINIMAL -> Color(0xFF1A1A22) to Color(0xFF101016)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(primaryColor, secondaryColor),
                    radius = 1600f
                )
            )
    ) {
        // Microrejilla de puntos sutiles
        Canvas(modifier = Modifier.fillMaxSize()) {
            val step = 48.dp.toPx()
            val dotRadius = 1.2.dp.toPx()
            val dotColor = Color.White.copy(alpha = 0.08f)

            var x = 0f
            while (x < size.width) {
                var y = 0f
                while (y < size.height) {
                    drawCircle(color = dotColor, radius = dotRadius, center = Offset(x, y))
                    y += step
                }
                x += step
            }
        }
    }
}

/**
 * Renderiza siluetas punteadas magnéticas según la plantilla activa.
 */
@Composable
private fun PlaymatTemplateSilhouettes(
    template: PlaymatTemplateType,
    zoom: Float
) {
    if (template == PlaymatTemplateType.FREEFORM) return

    val slotW = 120.dp * zoom
    val slotH = 180.dp * zoom

    when (template) {
        PlaymatTemplateType.DECK_OF_WORLDS_MICROSETTING -> {
            // Centro: Región (x=300, y=300)
            TemplateSlot(x = 300f * zoom, y = 300f * zoom, width = slotW, height = slotH, label = "Región (Núcleo)")
            // Norte: Punto de Interés
            TemplateSlot(x = 300f * zoom, y = 100f * zoom, width = slotW, height = slotH, label = "Hito / Sitio")
            // Oeste: Nombres
            TemplateSlot(x = 160f * zoom, y = 300f * zoom, width = slotW, height = slotH, label = "Nombres")
            // Noroeste: Orígenes
            TemplateSlot(x = 160f * zoom, y = 100f * zoom, width = slotW, height = slotH, label = "Orígenes")
            // Este: Atributos
            TemplateSlot(x = 440f * zoom, y = 300f * zoom, width = slotW, height = slotH, label = "Atributos")
            // Sureste: Acontecimientos
            TemplateSlot(x = 440f * zoom, y = 500f * zoom, width = slotW, height = slotH, label = "Crisis")
        }
        PlaymatTemplateType.STORY_ENGINE_PREMISE -> {
            TemplateSlot(x = 100f * zoom, y = 200f * zoom, width = slotW, height = slotH, label = "Potenciador")
            TemplateSlot(x = 240f * zoom, y = 200f * zoom, width = slotW, height = slotH, label = "Trasfondo")
            TemplateSlot(x = 380f * zoom, y = 200f * zoom, width = slotW, height = slotH, label = "Misterio")
            TemplateSlot(x = 520f * zoom, y = 200f * zoom, width = slotW, height = slotH, label = "Objeto Único")
        }
        PlaymatTemplateType.TAROT_THREE_CARDS -> {
            TemplateSlot(x = 160f * zoom, y = 240f * zoom, width = slotW, height = slotH, label = "Pasado")
            TemplateSlot(x = 300f * zoom, y = 240f * zoom, width = slotW, height = slotH, label = "Presente")
            TemplateSlot(x = 440f * zoom, y = 240f * zoom, width = slotW, height = slotH, label = "Futuro")
        }
        PlaymatTemplateType.ORACLE_CROSS -> {
            TemplateSlot(x = 300f * zoom, y = 240f * zoom, width = slotW, height = slotH, label = "Centro")
            TemplateSlot(x = 300f * zoom, y = 50f * zoom, width = slotW, height = slotH, label = "Arriba")
            TemplateSlot(x = 300f * zoom, y = 430f * zoom, width = slotW, height = slotH, label = "Abajo")
            TemplateSlot(x = 160f * zoom, y = 240f * zoom, width = slotW, height = slotH, label = "Izquierda")
            TemplateSlot(x = 440f * zoom, y = 240f * zoom, width = slotW, height = slotH, label = "Derecha")
        }
        else -> {}
    }
}

@Composable
private fun TemplateSlot(
    x: Float,
    y: Float,
    width: androidx.compose.ui.unit.Dp,
    height: androidx.compose.ui.unit.Dp,
    label: String
) {
    Box(
        modifier = Modifier
            .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
            .size(width, height)
            .border(
                width = 1.5.dp,
                color = Color.White.copy(alpha = 0.25f),
                shape = RoundedCornerShape(8.dp)
            )
            .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.4f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(4.dp)
        )
    }
}

@Composable
private fun PlaymatStickyNoteItem(
    note: PlaymatStickyNote,
    zoom: Float,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier
            .offset {
                IntOffset(
                    (note.x * zoom).roundToInt(),
                    (note.y * zoom).roundToInt()
                )
            }
            .size(100.dp * zoom, 100.dp * zoom)
            .shadow(4.dp, RoundedCornerShape(4.dp)),
        color = Color(0xFFFFF59D),
        shape = RoundedCornerShape(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = note.text,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = (11 * zoom).sp,
                    color = Color(0xFF212121)
                ),
                maxLines = 5,
                overflow = TextOverflow.Ellipsis
            )
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(20.dp)
                    .align(Alignment.End)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Borrar nota",
                    tint = Color.Black.copy(alpha = 0.4f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun PlaymatTableChipItem(
    chip: PlaymatTableChip,
    zoom: Float
) {
    Surface(
        modifier = Modifier
            .offset {
                IntOffset(
                    (chip.x * zoom).roundToInt(),
                    (chip.y * zoom).roundToInt()
                )
            }
            .shadow(3.dp, RoundedCornerShape(12.dp)),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Casino,
                contentDescription = null,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = "${chip.tableName}: ${chip.rollResultText}",
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
