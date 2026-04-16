package com.deckapp.feature.deck

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.deckapp.core.model.CardContentMode
import com.deckapp.core.model.CardFace
import com.deckapp.core.model.RandomTable
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardEditorScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: CardEditorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onSaved()
    }

    // Launcher de image picker — devuelve URI de la imagen seleccionada
    var pendingPickFaceIndex by remember { mutableIntStateOf(-1) }
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null && pendingPickFaceIndex >= 0) {
            viewModel.pickFaceImage(pendingPickFaceIndex, uri)
        }
        pendingPickFaceIndex = -1
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (uiState.title.isBlank()) "Nueva carta" else "Editar carta")
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    if (uiState.isSaving || uiState.isPickingImage) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(24.dp)
                                .padding(end = 16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        TextButton(
                            onClick = { viewModel.save() },
                            enabled = uiState.title.isNotBlank()
                        ) {
                            Text("Guardar")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Metadatos ─────────────────────────────────────────────
            OutlinedTextField(
                value = uiState.title,
                onValueChange = { viewModel.updateTitle(it) },
                label = { Text("Nombre de la carta *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = uiState.suit,
                    onValueChange = { viewModel.updateSuit(it) },
                    label = { Text("Palo") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = uiState.value,
                    onValueChange = { viewModel.updateValue(it) },
                    label = { Text("Valor") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            // ── Vínculo con Tabla ─────────────────────────────────────
            TableLinkSection(
                selectedTableId = uiState.linkedTableId,
                availableTables = uiState.availableTables,
                onTableSelect = { viewModel.updateLinkedTable(it) }
            )

            HorizontalDivider()

            // ── Tabs de caras ─────────────────────────────────────────
            Text("Caras", style = MaterialTheme.typography.titleMedium)

            ScrollableTabRow(selectedTabIndex = uiState.selectedFaceIndex) {
                uiState.faces.forEachIndexed { index, face ->
                    Tab(
                        selected = uiState.selectedFaceIndex == index,
                        onClick = { viewModel.selectFace(index) },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(face.name.ifBlank { "Cara ${index + 1}" })
                                // Botón ×: solo visible si hay más de 1 cara
                                if (uiState.faces.size > 1) {
                                    IconButton(
                                        onClick = { viewModel.removeFace(index) },
                                        modifier = Modifier.size(18.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Eliminar cara",
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    )
                }
                Tab(
                    selected = false,
                    onClick = { viewModel.addFace() },
                    text = { Text("+ Añadir") }
                )
            }

            // ── Editor de cara activa ─────────────────────────────────
            val currentFace = uiState.faces.getOrNull(uiState.selectedFaceIndex)
            if (currentFace != null) {
                val faceIndex = uiState.selectedFaceIndex

                FaceEditorSection(
                    face = currentFace,
                    faceIndex = faceIndex,
                    onNameChange = { viewModel.updateFaceName(faceIndex, it) },
                    onPickImage = {
                        pendingPickFaceIndex = faceIndex
                        imagePicker.launch("image/*")
                    },
                    onClearImage = { viewModel.updateFaceImagePath(faceIndex, null) },
                    onContentModeChange = { viewModel.updateFaceContentMode(faceIndex, it) },
                    onZoneTextChange = { zoneIndex, text ->
                        viewModel.updateZoneText(faceIndex, zoneIndex, text)
                    }
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Subcomponentes
// ──────────────────────────────────────────────────────────────────────────────

/**
 * Editor completo de una cara: nombre, imagen, modo de contenido y zonas de texto.
 */
@Composable
private fun FaceEditorSection(
    face: CardFace,
    faceIndex: Int,
    onNameChange: (String) -> Unit,
    onPickImage: () -> Unit,
    onClearImage: () -> Unit,
    onContentModeChange: (CardContentMode) -> Unit,
    onZoneTextChange: (zoneIndex: Int, text: String) -> Unit
) {
    // Nombre de la cara
    OutlinedTextField(
        value = face.name,
        onValueChange = onNameChange,
        label = { Text("Nombre de la cara") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )

    // Preview de imagen + picker
    FaceImagePicker(
        imagePath = face.imagePath,
        onPickImage = onPickImage,
        onClearImage = onClearImage
    )

    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

    // Selector de modo de contenido
    Text(
        "Estructura del contenido",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    ContentModeSelector(
        selected = face.contentMode,
        onSelect = onContentModeChange
    )

    // Editores de zonas de texto
    val zoneLabels = zoneLabelsFor(face.contentMode)
    if (zoneLabels.isNotEmpty()) {
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
        Text(
            "Contenido de texto",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        zoneLabels.forEachIndexed { zoneIndex, label ->
            OutlinedTextField(
                value = face.zones.getOrNull(zoneIndex)?.text ?: "",
                onValueChange = { onZoneTextChange(zoneIndex, it) },
                label = { Text(label) },
                placeholder = { Text("Admite Markdown") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 8
            )
        }
    }
}

/** Preview de la imagen de una cara con botones de cambiar/eliminar. */
@Composable
private fun FaceImagePicker(
    imagePath: String?,
    onPickImage: () -> Unit,
    onClearImage: () -> Unit
) {
    val shape = RoundedCornerShape(8.dp)
    if (imagePath != null) {
        Box(modifier = Modifier.fillMaxWidth()) {
            AsyncImage(
                model = File(imagePath),
                contentDescription = "Imagen de la cara",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(shape)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape),
                contentScale = ContentScale.Crop
            )
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SmallFloatingActionButton(
                    onClick = onPickImage,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text("Cambiar", style = MaterialTheme.typography.labelSmall)
                }
                SmallFloatingActionButton(
                    onClick = onClearImage,
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Quitar imagen",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    } else {
        OutlinedButton(
            onClick = onPickImage,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Seleccionar imagen")
        }
    }
}

/** Selector de radio button para los 8 modos de contenido. */
@Composable
private fun ContentModeSelector(
    selected: CardContentMode,
    onSelect: (CardContentMode) -> Unit
) {
    val modes = listOf(
        CardContentMode.IMAGE_ONLY        to "Solo imagen",
        CardContentMode.IMAGE_WITH_TEXT   to "Imagen + texto",
        CardContentMode.REVERSIBLE        to "Reversible (derecho / invertido)",
        CardContentMode.TOP_BOTTOM_SPLIT  to "División superior / inferior",
        CardContentMode.LEFT_RIGHT_SPLIT  to "División izquierda / derecha",
        CardContentMode.FOUR_EDGE_CUES    to "4 pistas de orientación (Story Engine)",
        CardContentMode.FOUR_QUADRANT     to "4 cuadrantes",
        CardContentMode.DOUBLE_SIDED_FULL to "Dos caras completas e independientes"
    )
    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        modes.forEach { (mode, label) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selected == mode,
                    onClick = { onSelect(mode) }
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    }
}

/** Etiquetas de zonas de texto por modo de contenido. */
private fun zoneLabelsFor(mode: CardContentMode): List<String> = when (mode) {
    CardContentMode.IMAGE_ONLY        -> emptyList()
    CardContentMode.IMAGE_WITH_TEXT   -> listOf("Texto")
    CardContentMode.REVERSIBLE        -> listOf("Texto derecho", "Texto invertido")
    CardContentMode.TOP_BOTTOM_SPLIT  -> listOf("Zona superior", "Zona inferior")
    CardContentMode.LEFT_RIGHT_SPLIT  -> listOf("Zona izquierda", "Zona derecha")
    CardContentMode.FOUR_EDGE_CUES    -> listOf("Norte", "Este", "Sur", "Oeste")
    CardContentMode.FOUR_QUADRANT     -> listOf("Noroeste", "Noreste", "Suroeste", "Sureste")
    CardContentMode.DOUBLE_SIDED_FULL -> listOf("Contenido")
}

/** Sección para vincular una tabla aleatoria a la carta. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TableLinkSection(
    selectedTableId: Long?,
    availableTables: List<RandomTable>,
    onTableSelect: (Long?) -> Unit
) {
    val selectedTable = availableTables.find { it.id == selectedTableId }
    var showDialog by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Acción vinculada", style = MaterialTheme.typography.titleMedium)
        Card(
            onClick = { showDialog = true },
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (selectedTableId != null) 
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) 
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            border = if (selectedTableId != null)
                null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Casino,
                    contentDescription = null,
                    tint = if (selectedTableId != null) 
                        MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = selectedTable?.name ?: "Vincular tabla aleatoria",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (selectedTableId != null) FontWeight.Bold else FontWeight.Normal
                    )
                    Text(
                        text = if (selectedTableId != null) 
                            "Se podrá tirar esta tabla al robar la carta" else "Activa una acción automática al usar esta carta",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (selectedTableId != null) {
                    IconButton(onClick = { onTableSelect(null) }) {
                        Icon(Icons.Default.Close, contentDescription = "Eliminar vínculo")
                    }
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Vincular tabla") },
            text = {
                if (availableTables.isEmpty()) {
                    Text("No hay tablas creadas todavía en la biblioteca.")
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                        items(availableTables) { table ->
                            ListItem(
                                headlineContent = { Text(table.name) },
                                supportingContent = { 
                                    Text(
                                        if (table.tags.isNotEmpty()) table.tags.joinToString(", ") { it.name }
                                        else "Sin etiquetas"
                                    ) 
                                },
                                leadingContent = { 
                                    RadioButton(
                                        selected = table.id == selectedTableId,
                                        onClick = { onTableSelect(table.id); showDialog = false }
                                    ) 
                                },
                                modifier = Modifier.clickable { onTableSelect(table.id); showDialog = false }
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancelar") }
            }
        )
    }
}
