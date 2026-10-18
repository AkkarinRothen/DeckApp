package com.deckapp.feature.draw.components

import com.deckapp.core.ui.icons.DeckIcons

import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.deckapp.core.domain.usecase.ChoirCharacterCodec
import com.deckapp.core.domain.usecase.ChoirProcedureCatalog
import com.deckapp.core.model.*
import com.deckapp.feature.draw.SessionUiState
import com.deckapp.feature.draw.delegates.ChoirGuideDelegate

@Composable
fun ChoirProcedureBar(memory: ChoirGuideMemory, onOpen: () -> Unit) {
    val step = ChoirProcedureCatalog.current(memory) ?: return
    Surface(onClick = onOpen, color = MaterialTheme.colorScheme.secondaryContainer) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(DeckIcons.Guide, contentDescription = null, modifier = Modifier.size(20.dp))
            Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                Text("Ahora: ${step.title}", style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Después: ${step.next}", style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = "Abrir guía de procedimientos")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChoirProcedureGuide(state: SessionUiState, delegate: ChoirGuideDelegate, onOpenIncursion: () -> Unit) {
    val memory = state.boardMemory.choirGuide
    val step = ChoirProcedureCatalog.current(memory)
    var showIndex by rememberSaveable { mutableStateOf(step == null) }
    var reference by rememberSaveable { mutableStateOf<ChoirProcedure?>(null) }
    var confirmUndo by remember { mutableStateOf(false) }
    var confirmRestart by remember { mutableStateOf(false) }
    var chooseCharacter by remember { mutableStateOf(false) }
    var manualAnguish by rememberSaveable { mutableStateOf("") }
    var showAnguish by rememberSaveable { mutableStateOf(false) }
    val characters = remember(state.npcs) { state.npcs.filter { ChoirCharacterCodec.read(it.notes) != null } }
    val character = characters.firstOrNull { it.id == memory.selectedCharacterId }
    val listState = rememberLazyListState()
    LaunchedEffect(memory.revision, showIndex, reference) { listState.scrollToItem(0) }
    LaunchedEffect(memory.anguishChecks.firstOrNull()?.id) {
        if (memory.anguishChecks.isNotEmpty()) { showIndex = false; reference = null }
    }

    ModalBottomSheet(onDismissRequest = delegate::close,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(0.95f).imePadding().padding(horizontal = 16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Choir · Procedimientos", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                IconButton(onClick = delegate::close) { Icon(Icons.Default.Close, "Cerrar procedimientos") }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = !showIndex && reference == null, onClick = { showIndex = false; reference = null }, label = { Text("Guía actual") })
                FilterChip(selected = showIndex || reference != null, onClick = { showIndex = true; reference = null }, label = { Text("Índice y hojas") })
            }
            if (state.choirGuideError != null) {
                Text(state.choirGuideError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = 6.dp).semantics { liveRegion = LiveRegionMode.Polite })
            }
            if (memory.journalOutbox.isNotEmpty()) {
                TextButton(onClick = { delegate.retryJournal() }) {
                    Text("${memory.journalOutbox.size} registro(s) pendiente(s) · Reintentar guardado")
                }
            }
            LazyColumn(Modifier.weight(1f).fillMaxWidth(), state = listState, contentPadding = PaddingValues(bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (reference != null) {
                    val info = ChoirProcedureCatalog.info(reference!!)
                    item {
                        TextButton(onClick = { reference = null }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null); Text("Volver al índice") }
                        Text(info.title, style = MaterialTheme.typography.titleLarge)
                        Text(ChoirProcedureCatalog.referenceText(info.id), modifier = Modifier.padding(vertical = 12.dp))
                        Text(info.reference, style = MaterialTheme.typography.labelMedium)
                        OutlinedButton(onClick = { delegate.source(info.sheet) }) { Text("Abrir hoja original ${info.sheet}") }
                        if (info.id == ChoirProcedure.COMBAT) OutlinedButton(onClick = { delegate.source(4) }) { Text("Continuación: heridas · hoja 4") }
                    }
                } else if (showIndex || step == null) {
                    item { Text("Elige una guía o consulta la regla. El progreso de cada guía se conserva al cambiar de procedimiento.", style = MaterialTheme.typography.bodyMedium) }
                    items(ChoirProcedureCatalog.procedures, key = { it.id }) { info ->
                        OutlinedCard(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(info.title, style = MaterialTheme.typography.titleSmall)
                                Text(info.summary, style = MaterialTheme.typography.bodySmall)
                                Text(info.reference, style = MaterialTheme.typography.labelSmall)
                                if (info.id == ChoirProcedure.ANGUISH) {
                                    TextButton(onClick = { showAnguish = true; showIndex = false; reference = ChoirProcedure.ANGUISH }) { Text("Iniciar prueba de Angustia") }
                                } else if (info.guided) {
                                    TextButton(onClick = { delegate.select(info.id); showIndex = false }) { Text("Abrir / continuar guía") }
                                } else {
                                    TextButton(onClick = { reference = info.id }) { Text("Consultar procedimiento") }
                                }
                                TextButton(onClick = { delegate.source(info.sheet) }) { Text("Ver hoja original") }
                            }
                        }
                    }
                } else {
                    item {
                        val p = memory.progress
                        if (memory.anguishChecks.isNotEmpty()) {
                            Text("Interrupción · ${memory.anguishChecks.size} prueba(s) pendiente(s)", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            Text("Tu paso anterior sigue guardado.", style = MaterialTheme.typography.bodySmall)
                        } else when (p.active) {
                            ChoirProcedure.EXPLORATION -> Text("Día ${p.day} · ${p.turnsSpent}/4 turnos utilizados", style = MaterialTheme.typography.labelLarge)
                            ChoirProcedure.INCURSION -> Text("${p.incursionActions}/15 acciones de incursión", style = MaterialTheme.typography.labelLarge)
                            ChoirProcedure.INCURSION_SETUP -> Text("Preparación · extracción ${p.extractionNode ?: "—"} · peligros ${p.hazardousNodes.joinToString().ifBlank { "—" }} · PoI ${p.pointsPrepared}/5", style = MaterialTheme.typography.labelLarge)
                            else -> Unit
                        }
                        Text("Ahora", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        Text(step.title, style = MaterialTheme.typography.headlineSmall)
                        Text(step.instruction, modifier = Modifier.padding(vertical = 10.dp))
                        Text(step.reference, style = MaterialTheme.typography.labelSmall)
                    }
                    item {
                        OutlinedButton(onClick = { delegate.source(step.sheet) }) { Text("Consultar hoja original ${step.sheet}") }
                        if (memory.progress.active in listOf(ChoirProcedure.INCURSION_SETUP, ChoirProcedure.INCURSION)) {
                            OutlinedButton(onClick = onOpenIncursion) { Text("Abrir tablero de incursión") }
                        }
                        for (title in step.tables) {
                            val table = ChoirGuideDelegate.findTable(state, title)
                            OutlinedButton(onClick = { if (table != null) delegate.openTable(title) else delegate.source(step.sheet) }) {
                                Text(if (table != null) "Consultar tabla: $title" else "$title · consultar referencia")
                            }
                            if (table == null) Text("Esta tabla no está integrada; consulta ${step.reference}. Puedes anotar su resultado manualmente.", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    if (step.die != null) item {
                        OutlinedTextField(value = memory.draftRoll, onValueChange = { delegate.draft(roll = it) },
                            label = { Text(if (step.isCounter) "Valor (0–${step.die})" else "Resultado de 1d${step.die}") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true,
                            modifier = Modifier.fillMaxWidth())
                        if (!step.isCounter) {
                            TextButton(onClick = { delegate.roll() }) { Icon(DeckIcons.Dice, null); Spacer(Modifier.width(6.dp)); Text("Tirar 1d${step.die} en la app") }
                            Text("${memory.draftMethod}. Tirar prepara el resultado; confirmar lo registra.", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    if (step.actions.isNotEmpty()) {
                        item { OutlinedTextField(value = memory.draftNote, onValueChange = { delegate.draft(note = it) }, label = { Text("Resultado / anotación (opcional)") }, minLines = 2, maxLines = 5, modifier = Modifier.fillMaxWidth()) }
                        items(step.actions, key = { it.id }) { action ->
                            Button(onClick = { delegate.resolve(action.id, memory.revision) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(action.label) }
                        }
                    }
                    item {
                        HorizontalDivider()
                        Text("Después", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 10.dp))
                        Text(step.next)
                        if (memory.lastResolution.isNotBlank()) Text("Último registro: ${memory.lastResolution}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 12.dp))
                        if (memory.history.isNotEmpty() && memory.anguishChecks.isEmpty()) TextButton(onClick = { confirmUndo = true }) { Text("Corregir último paso de la guía") }
                        if (memory.progress.active != null && memory.anguishChecks.isEmpty()) TextButton(onClick = { confirmRestart = true }) { Text("Reiniciar este procedimiento") }
                    }
                }
                item {
                    HorizontalDivider()
                    Text("Personaje para avisos de Angustia", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 10.dp))
                    Box {
                        OutlinedButton(onClick = { chooseCharacter = true }) { Text(character?.name ?: "Manual · elegir personaje") }
                        DropdownMenu(expanded = chooseCharacter, onDismissRequest = { chooseCharacter = false }) {
                            DropdownMenuItem(text = { Text("Sin personaje vinculado (manual)") }, onClick = { delegate.selectCharacter(null); chooseCharacter = false })
                            characters.forEach { npc -> DropdownMenuItem(text = { Text(npc.name) }, onClick = { delegate.selectCharacter(npc.id); chooseCharacter = false }) }
                        }
                    }
                    Text("Si aumenta la Angustia de este personaje en la app, se prepara una prueba. Si cambias la ficha en papel, inicia la prueba aquí.", style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = { showAnguish = !showAnguish }) { Text("Prueba de Angustia manual") }
                    if (showAnguish) {
                        val sheet = character?.let { ChoirCharacterCodec.read(it.notes) }
                        if (sheet != null) Text("Angustia actual de ${character?.name}: ${sheet.anguish}")
                        else OutlinedTextField(value = manualAnguish, onValueChange = { manualAnguish = it }, label = { Text("Angustia actual") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        val value = sheet?.anguish ?: manualAnguish.toIntOrNull()
                        Button(enabled = value != null && value >= 0, onClick = { delegate.startAnguish(value!!, memory.revision); showAnguish = false; showIndex = false; reference = null }) { Text("Preparar prueba de Angustia") }
                    }
                }
            }
        }
    }
    if (confirmUndo) AlertDialog(onDismissRequest = { confirmUndo = false }, title = { Text("Corregir guía") },
        text = { Text("Se restaura el paso y los contadores anteriores y se anota la corrección en el diario. Las fichas e inventarios editados manualmente no se revierten.") },
        confirmButton = { TextButton(onClick = { delegate.undo(memory.revision); confirmUndo = false }) { Text("Corregir") } },
        dismissButton = { TextButton(onClick = { confirmUndo = false }) { Text("Cancelar") } })
    if (confirmRestart) AlertDialog(onDismissRequest = { confirmRestart = false }, title = { Text("Reiniciar procedimiento") },
        text = { Text("Volverás al primer paso. Una nueva preparación borra las zonas y PoI de la guía; la grilla, los personajes y el inventario se conservan.") },
        confirmButton = { TextButton(onClick = { memory.progress.active?.let { delegate.restart(it, memory.revision) }; confirmRestart = false }) { Text("Reiniciar") } },
        dismissButton = { TextButton(onClick = { confirmRestart = false }) { Text("Cancelar") } })
}

@Composable
fun ChoirProcedureSource(page: Int, onPage: (Int) -> Unit, onClose: () -> Unit) {
    var scale by remember(page) { mutableFloatStateOf(1f) }
    var offset by remember(page) { mutableStateOf(Offset.Zero) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    val gesture = rememberTransformableState { zoom, pan, _ ->
        scale = (scale * zoom).coerceIn(1f, 6f)
        val limitX = size.width * (scale - 1) / 2
        val limitY = size.height * (scale - 1) / 2
        offset = Offset((offset.x + pan.x).coerceIn(-limitX, limitX), (offset.y + pan.y).coerceIn(-limitY, limitY))
    }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().safeDrawingPadding()) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(enabled = page > 1, onClick = { onPage(page - 1) }) { Icon(Icons.Default.ChevronLeft, "Hoja anterior") }
                    Text("Procedimientos · $page/6", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                    IconButton(enabled = page < 6, onClick = { onPage(page + 1) }) { Icon(Icons.Default.ChevronRight, "Hoja siguiente") }
                    IconButton(onClick = { scale = 1f; offset = Offset.Zero }) { Icon(Icons.Default.FitScreen, "Restablecer zoom") }
                    IconButton(onClick = onClose) { Icon(Icons.Default.Close, "Cerrar hoja original") }
                }
                Text("Pellizca para ampliar y arrastra para leer.", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 12.dp))
                Box(Modifier.weight(1f).fillMaxWidth().clipToBounds().onSizeChanged { size = it }.transformable(gesture)) {
                    AsyncImage(model = "file:///android_asset/choir/procedures/page-$page.webp", contentDescription = "Gameplay Procedures, hoja $page", contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize().graphicsLayer(scaleX = scale, scaleY = scale, translationX = offset.x, translationY = offset.y))
                }
            }
        }
    }
}
