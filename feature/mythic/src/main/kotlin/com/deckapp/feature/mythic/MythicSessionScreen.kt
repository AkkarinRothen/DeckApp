package com.deckapp.feature.mythic

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deckapp.core.model.*
import com.deckapp.core.domain.usecase.mythic.SceneCheckResult
import com.deckapp.feature.mythic.components.FateCheckSheet
import com.deckapp.feature.mythic.components.FateResultCard
import com.deckapp.feature.mythic.components.MythicConfigSheet

import com.deckapp.core.ui.util.rememberShakeDetector

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.mimeTypes
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import kotlinx.serialization.json.Json
import com.deckapp.core.domain.usecase.SearchResult

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MythicSessionScreen(
    onBack: () -> Unit,
    onShowDiceRoller: () -> Unit = {},
    onNavigateToHelp: () -> Unit = {},
    viewModel: MythicSessionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    
    var showFateSheet by remember { mutableStateOf(false) }
    var showConfigSheet by remember { mutableStateOf(false) }
    var showFinishSceneDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }

    val dndTarget = remember {
        object : DragAndDropTarget {
            override fun onDrop(event: DragAndDropEvent): Boolean {
                val clipData = event.toAndroidDragEvent().clipData
                if (clipData != null && clipData.itemCount > 0) {
                    try {
                        val json = clipData.getItemAt(0).text.toString()
                        val result = Json.decodeFromString<SearchResult>(json)
                        viewModel.linkSearchResult(result)
                        return true
                    } catch (e: Exception) { e.printStackTrace() }
                }
                return false
            }
        }
    }

    // Shake to Roll: Disparar 50/50 por movimiento
    rememberShakeDetector {
        viewModel.quickFateCheck()
    }

    // Determinar el "Color de Caos" (de Primario a Rojo Intenso)
    val chaosFactor = uiState.session?.chaosFactor ?: 5
    val chaosColor = animateColorAsState(
        targetValue = when {
            chaosFactor <= 3 -> MaterialTheme.colorScheme.primary
            chaosFactor <= 6 -> Color(0xFFFFA726) // Naranja
            else -> Color(0xFFEF5350) // Rojo
        },
        label = "ChaosColor"
    ).value

    LaunchedEffect(uiState.sceneCheckResult) {
        uiState.sceneCheckResult?.let { result ->
            val message = when (result) {
                SceneCheckResult.NORMAL -> "Escena NORMAL: Todo sucede como se esperaba."
                SceneCheckResult.ALTERED -> "Escena ALTERADA: Algo cambia ligeramente."
                SceneCheckResult.INTERRUPTED -> "Escena INTERRUMPIDA: ¡Un evento inesperado ocurre!"
            }
            snackbarHostState.showSnackbar(message)
            viewModel.clearSceneCheck()
        }
    }

    if (showFateSheet) {
        ModalBottomSheet(onDismissRequest = { showFateSheet = false }) {
            FateCheckSheet(
                onPerformRoll = viewModel::performFateCheck,
                onDismiss = { showFateSheet = false }
            )
        }
    }

    if (showConfigSheet && uiState.session != null) {
        ModalBottomSheet(onDismissRequest = { showConfigSheet = false }) {
            MythicConfigSheet(
                sessionName = uiState.session!!.name,
                actionTableId = uiState.session!!.actionTableId,
                subjectTableId = uiState.session!!.subjectTableId,
                allTables = uiState.allTables,
                onSaveTables = viewModel::setTables,
                onDeleteSession = {
                    viewModel.deleteSession(uiState.session!!.id)
                    onBack()
                    showConfigSheet = false
                },
                onDismiss = { showConfigSheet = false }
            )
        }
    }

    if (showFinishSceneDialog) {
        FinishSceneDialog(
            onConfirm = { notes, inControl ->
                viewModel.finishScene(notes, inControl)
                showFinishSceneDialog = false
            },
            onDismiss = { showFinishSceneDialog = false }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(uiState.session?.name ?: "Sesión Mythic") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = onShowDiceRoller) {
                        Icon(Icons.Default.Casino, contentDescription = "Rodillo de dados")
                    }
                    IconButton(onClick = onNavigateToHelp) {
                        Icon(Icons.Default.HelpOutline, contentDescription = "Guía Mythic")
                    }
                    IconButton(onClick = { showConfigSheet = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Configuración")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = chaosColor.copy(alpha = 0.1f)
                )
            )
        }
    ) { padding ->
        if (uiState.isLoading || uiState.session == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val session = uiState.session!!
            
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                // Header: Scene & CF
                MythicHeader(
                    sceneNumber = session.sceneNumber,
                    chaosFactor = session.chaosFactor,
                    chaosColor = chaosColor,
                    onSceneChange = viewModel::updateSceneNumber,
                    onChaosChange = viewModel::updateChaosFactor,
                    onCheckScene = viewModel::checkScene,
                    onFinishScene = { showFinishSceneDialog = true }
                )

                Spacer(Modifier.height(8.dp))

                // Fate Check Action
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { showFateSheet = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = chaosColor)
                    ) {
                        Icon(Icons.Default.QuestionAnswer, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Preguntar")
                    }

                    OutlinedButton(
                        onClick = { viewModel.quickFateCheck() },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = chaosColor)
                    ) {
                        Icon(Icons.Default.FlashOn, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("50/50")
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Last Result or Recent History
                if (uiState.rolls.isNotEmpty()) {
                    val lastRoll = uiState.rolls.first()
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Text("Último resultado", style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.height(4.dp))
                        FateResultCard(
                            result = lastRoll.result,
                            roll = lastRoll.roll,
                            isRandomEvent = lastRoll.isRandomEvent,
                            eventFocus = lastRoll.eventFocus,
                            eventAction = lastRoll.eventAction,
                            eventSubject = lastRoll.eventSubject
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Tabs: Characters / Threads / History
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Personajes") })
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Hilos") })
                    Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Historial") })
                }

                Box(modifier = Modifier.weight(1f)) {
                    when (selectedTab) {
                        0 -> CharacterList(
                            characters = uiState.characters, 
                            onAdd = viewModel::addCharacter, 
                            onDelete = viewModel::deleteCharacter,
                            modifier = Modifier.dragAndDropTarget(
                                shouldStartDragAndDrop = { event -> event.mimeTypes().contains("text/plain") },
                                target = dndTarget
                            )
                        )
                        1 -> ThreadList(
                            threads = uiState.threads, 
                            onAdd = viewModel::addThread, 
                            onToggle = viewModel::toggleThreadStatus, 
                            onDelete = viewModel::deleteThread,
                            modifier = Modifier.dragAndDropTarget(
                                shouldStartDragAndDrop = { event -> event.mimeTypes().contains("text/plain") },
                                target = dndTarget
                            )
                        )
                        2 -> RollHistoryList(uiState.rolls)
                    }
                }
            }
        }
    }
}

@Composable
internal fun MythicHeader(
    sceneNumber: Int,
    chaosFactor: Int,
    chaosColor: Color,
    onSceneChange: (Int) -> Unit,
    onChaosChange: (Int) -> Unit,
    onCheckScene: () -> Unit,
    onFinishScene: () -> Unit
) {
    Surface(
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("ESCENA", style = MaterialTheme.typography.labelSmall)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onSceneChange(-1) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Remove, contentDescription = null)
                    }
                    Text(sceneNumber.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { onSceneChange(1) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Add, contentDescription = null)
                    }
                }
                Row {
                    TextButton(onClick = onFinishScene) {
                        Text("Finalizar")
                    }
                    TextButton(onClick = onCheckScene) {
                        Text("Chequear")
                    }
                }
            }

            VerticalDivider(modifier = Modifier.height(40.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("FACTOR DE CAOS", style = MaterialTheme.typography.labelSmall)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onChaosChange(-1) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Remove, contentDescription = null)
                    }
                    Text(
                        chaosFactor.toString(), 
                        style = MaterialTheme.typography.displaySmall, 
                        fontWeight = FontWeight.Black, 
                        color = chaosColor
                    )
                    IconButton(onClick = { onChaosChange(1) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Add, contentDescription = null)
                    }
                }
            }
        }
    }
}

@Composable
private fun CharacterList(
    characters: List<MythicCharacter>,
    onAdd: (String) -> Unit,
    onDelete: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var newName by remember { mutableStateOf("") }

    Column(modifier = modifier.fillMaxSize()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                placeholder = { Text("Nuevo Personaje") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            IconButton(onClick = { onAdd(newName); newName = "" }) {
                Icon(Icons.Default.AddCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
        }
        
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(characters, key = { it.id }) { char ->
                ListItem(
                    headlineContent = { Text(char.name) },
                    trailingContent = {
                        IconButton(onClick = { onDelete(char.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(20.dp))
                        }
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
            }
        }
    }
}

@Composable
private fun ThreadList(
    threads: List<MythicThread>,
    onAdd: (String) -> Unit,
    onToggle: (Long, Boolean) -> Unit,
    onDelete: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var newDesc by remember { mutableStateOf("") }

    Column(modifier = modifier.fillMaxSize()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newDesc,
                onValueChange = { newDesc = it },
                placeholder = { Text("Nuevo Hilo/Trama") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            IconButton(onClick = { onAdd(newDesc); newDesc = "" }) {
                Icon(Icons.Default.AddCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
        }
        
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(threads, key = { it.id }) { thread ->
                ListItem(
                    headlineContent = { 
                        Text(
                            thread.description, 
                            style = if (thread.isResolved) MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant) 
                                    else MaterialTheme.typography.bodyLarge
                        ) 
                    },
                    leadingContent = {
                        Checkbox(checked = thread.isResolved, onCheckedChange = { onToggle(thread.id, it) })
                    },
                    trailingContent = {
                        IconButton(onClick = { onDelete(thread.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(20.dp))
                        }
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
            }
        }
    }
}

@Composable
internal fun RollHistoryList(rolls: List<MythicRoll>) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(rolls, key = { it.id }) { roll ->
            if (roll.probability == ProbabilityLevel.NARRATIVE) {
                // Visualización especial para Notas
                ListItem(
                    overlineContent = { Text("Nota de Escena ${roll.sceneNumber}") },
                    headlineContent = { Text(roll.question, style = MaterialTheme.typography.bodyLarge) },
                    leadingContent = { Icon(Icons.Default.Notes, null, tint = MaterialTheme.colorScheme.secondary) }
                )
            } else {
                val color = when (roll.result) {
                    FateResult.EXCEPTIONAL_YES, FateResult.YES -> Color(0xFF4CAF50)
                    FateResult.EXCEPTIONAL_NO, FateResult.NO -> Color(0xFFF44336)
                    else -> MaterialTheme.colorScheme.onSurface
                }
                ListItem(
                    overlineContent = { Text("Escena ${roll.sceneNumber} • Prob: ${roll.probability.name.replace("_", " ")}") },
                    headlineContent = { Text(roll.question.ifBlank { "Pregunta al Oráculo" }) },
                    supportingContent = {
                        Column {
                            if (roll.isRandomEvent) {
                                Text("¡EVENTO! ${roll.eventFocus}", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                                Text("${roll.eventAction} ${roll.eventSubject}", color = MaterialTheme.colorScheme.error)
                            } else {
                                Text("Roll: ${roll.roll}")
                            }
                        }
                    },
                    trailingContent = {
                        if (roll.result != FateResult.NONE) {
                            Text(roll.result.name.replace("_", " "), color = color, fontWeight = FontWeight.Black)
                        }
                    }
                )
            }
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
        }
    }
}

@Composable
private fun FinishSceneDialog(
    onConfirm: (String, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var notes by remember { mutableStateOf("") }
    var inControl by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Finalizar Escena") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Resumen narrativo (opcional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Column {
                    Text("¿Cómo terminó la escena?", style = MaterialTheme.typography.labelMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = inControl, onClick = { inControl = true })
                        Text("Bajo control (Caos -1)", modifier = Modifier.clickable { inControl = true })
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = !inControl, onClick = { inControl = false })
                        Text("Fuera de control (Caos +1)", modifier = Modifier.clickable { inControl = false })
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(notes, inControl) }) { Text("Finalizar y Seguir") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
