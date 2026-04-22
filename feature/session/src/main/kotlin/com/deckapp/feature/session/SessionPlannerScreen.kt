package com.deckapp.feature.session

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deckapp.core.model.Scene
import com.deckapp.core.ui.components.MarkdownText
import com.deckapp.core.ui.components.MarkdownToolbar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionPlannerScreen(
    onBack: () -> Unit,
    viewModel: SessionPlannerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(uiState.session?.name ?: "Planificador", style = MaterialTheme.typography.titleMedium)
                        Text("Preparación de Sesión", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.addScene() }) {
                Icon(Icons.Default.Add, contentDescription = "Añadir Escena")
            }
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    SessionPrepHeader(uiState)
                }

                if (uiState.scenes.isEmpty()) {
                    item {
                        EmptyScenesState { viewModel.addScene() }
                    }
                } else {
                    items(uiState.scenes, key = { it.id }) { scene ->
                        ScenePlannerCard(
                            scene = scene,
                            onTitleChange = { viewModel.updateSceneTitle(scene.id, it) },
                            onContentChange = { viewModel.updateSceneContent(scene.id, it) },
                            onDelete = { viewModel.deleteScene(scene.id) },
                            onMoveUp = { viewModel.moveSceneUp(scene.id) },
                            onMoveDown = { viewModel.moveSceneDown(scene.id) },
                            viewModel = viewModel,
                            uiState = uiState
                        )
                    }
                }
                
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun SessionPrepHeader(uiState: PlannerUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Event, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (uiState.session?.scheduledDate != null) "Próxima sesión: ${java.text.SimpleDateFormat("dd/MM/yyyy").format(uiState.session.scheduledDate)}" else "Sin fecha programada",
                    style = MaterialTheme.typography.labelLarge
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "${uiState.scenes.size} Escenas preparadas",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun ScenePlannerCard(
    scene: Scene,
    onTitleChange: (String) -> Unit,
    onContentChange: (String) -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    viewModel: SessionPlannerViewModel,
    uiState: PlannerUiState
) {
    var isExpanded by remember { mutableStateOf(false) }
    var isEditMode by remember { mutableStateOf(false) }
    var textFieldValue by remember(scene.content) { 
        mutableStateOf(TextFieldValue(scene.content)) 
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        (scene.sortOrder + 1).toString(),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                
                Spacer(Modifier.width(12.dp))
                
                if (isEditMode) {
                    OutlinedTextField(
                        value = scene.title,
                        onValueChange = onTitleChange,
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.titleSmall
                    )
                } else {
                    Text(
                        text = scene.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                }

                IconButton(onClick = { isExpanded = !isExpanded }) {
                    Icon(
                        if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Expandir"
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column {
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(Modifier.height(12.dp))

                    if (isEditMode) {
                        MarkdownToolbar(
                            value = textFieldValue,
                            onValueChange = { 
                                textFieldValue = it
                                onContentChange(it.text)
                            }
                        )
                        OutlinedTextField(
                            value = textFieldValue,
                            onValueChange = { 
                                textFieldValue = it
                                onContentChange(it.text)
                            },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp),
                            placeholder = { Text("Desarrollo de la escena...") }
                        )
                    } else {
                        MarkdownText(
                            markdown = if (scene.content.isBlank()) "_Sin notas de preparación._" else scene.content,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(onClick = onMoveUp) { Icon(Icons.Default.ArrowUpward, "Subir") }
                        IconButton(onClick = onMoveDown) { Icon(Icons.Default.ArrowDownward, "Bajar") }
                    IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Borrar", tint = MaterialTheme.colorScheme.error) }
                        
                        Spacer(Modifier.weight(1f))
                        
                        TextButton(onClick = { isEditMode = !isEditMode }) {
                            Icon(if (isEditMode) Icons.Default.Visibility else Icons.Default.Edit, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (isEditMode) "Ver" else "Editar")
                        }
                    }

                    if (isEditMode) {
                        Spacer(Modifier.height(8.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        Spacer(Modifier.height(8.dp))
                        
                        Text("Configuración de Escena", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            var showTablePicker by remember { mutableStateOf(false) }
                            var showDeckPicker by remember { mutableStateOf(false) }

                            if (showTablePicker) {
                                ResourcePickerDialog(
                                    title = "Vincular Tabla",
                                    items = uiState.allTables.map { it.id to it.name },
                                    onSelect = { viewModel.updateSceneTrigger(scene.id, it, scene.linkedDeckId) },
                                    onDismiss = { showTablePicker = false }
                                )
                            }
                            
                            if (showDeckPicker) {
                                ResourcePickerDialog(
                                    title = "Vincular Mazo",
                                    items = uiState.allDecks.map { it.id to it.name },
                                    onSelect = { viewModel.updateSceneTrigger(scene.id, scene.linkedTableId, it) },
                                    onDismiss = { showDeckPicker = false }
                                )
                            }

                            // Link Table
                            FilterChip(
                                selected = scene.linkedTableId != null,
                                onClick = { showTablePicker = true },
                                label = { 
                                    val tableName = uiState.allTables.find { it.id == scene.linkedTableId }?.name ?: "Vincular Tabla"
                                    Text(tableName) 
                                },
                                leadingIcon = { Icon(Icons.Default.Casino, null, Modifier.size(16.dp)) },
                                trailingIcon = if (scene.linkedTableId != null) {
                                    { IconButton(onClick = { viewModel.updateSceneTrigger(scene.id, null, scene.linkedDeckId) }, modifier = Modifier.size(18.dp)) { Icon(Icons.Default.Close, null) } }
                                } else null
                            )
                            // Link Deck
                            FilterChip(
                                selected = scene.linkedDeckId != null,
                                onClick = { showDeckPicker = true },
                                label = { 
                                    val deckName = uiState.allDecks.find { it.id == scene.linkedDeckId }?.name ?: "Vincular Mazo"
                                    Text(deckName) 
                                },
                                leadingIcon = { Icon(Icons.Default.Style, null, Modifier.size(16.dp)) },
                                trailingIcon = if (scene.linkedDeckId != null) {
                                    { IconButton(onClick = { viewModel.updateSceneTrigger(scene.id, scene.linkedTableId, null) }, modifier = Modifier.size(18.dp)) { Icon(Icons.Default.Close, null) } }
                                } else null
                            )
                        }

                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = scene.isAlternative,
                                onCheckedChange = { _ -> viewModel.toggleSceneAlternative(scene.id) }
                            )
                            Text("Camino alternativo / Ramificación", style = MaterialTheme.typography.bodySmall)
                        }
                        
                        OutlinedTextField(
                            value = scene.imagePath ?: "",
                            onValueChange = { viewModel.updateSceneMood(scene.id, it) },
                            label = { Text("URL de Imagen de Ambiente (Mood)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            trailingIcon = { Icon(Icons.Default.Image, null) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyScenesState(onAdd: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.HistoryEdu,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Tu plan está vacío",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            "Añade escenas para guiar tu narrativa.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onAdd) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Comenzar preparación")
        }
    }
}

@Composable
private fun ResourcePickerDialog(
    title: String,
    items: List<Pair<Long, String>>,
    onSelect: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                items(items) { (id, name) ->
                    ListItem(
                        headlineContent = { Text(name) },
                        modifier = Modifier.clickable { 
                            onSelect(id)
                            onDismiss()
                        }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
