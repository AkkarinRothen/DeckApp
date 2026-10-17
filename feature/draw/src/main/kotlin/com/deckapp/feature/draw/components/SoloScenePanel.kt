package com.deckapp.feature.draw.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.deckapp.core.domain.usecase.*
import com.deckapp.core.model.*

@Composable
internal fun SoloScenePanel(
    sessionId: Long,
    scenes: List<Scene>,
    selectedScene: Scene?,
    busy: Boolean,
    probability: ProbabilityLevel,
    chaos: Int,
    lastOracleResult: BoardMythicFateResult?,
    onSelect: (Long?) -> Unit,
    onSave: (Scene, () -> Unit) -> Unit,
    onClose: (Scene, SoloSceneClosure, () -> Unit) -> Unit,
    onAsk: (String) -> Unit,
    onAppendToDiary: (String) -> Unit
) {
    var showSelector by remember { mutableStateOf(false) }
    var editingSceneId by rememberSaveable(sessionId) { mutableStateOf<Long?>(null) }
    var closingSceneId by rememberSaveable(sessionId) { mutableStateOf<Long?>(null) }
    val editingScene = if (editingSceneId == 0L) Scene(sessionId = sessionId, title = "", sortOrder = (scenes.maxOfOrNull { it.sortOrder } ?: -1) + 1)
        else scenes.firstOrNull { it.id == editingSceneId }
    val closingScene = scenes.firstOrNull { it.id == closingSceneId }
    var isCollapsed by rememberSaveable(sessionId) { mutableStateOf(true) }
    var detailsExpanded by rememberSaveable(selectedScene?.id) { mutableStateOf(false) }
    val context = selectedScene?.soloContext()

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Cabecera compacta siempre visible
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { isCollapsed = !isCollapsed }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = selectedScene?.title ?: "Sin escena activa",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = androidx.compose.foundation.shape.CircleShape
                        ) {
                            Text(
                                text = "Caos $chaos",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (isCollapsed) {
                        val subtitle = context?.location?.ifBlank { null }
                            ?: context?.objective?.ifBlank { null }
                            ?: if (selectedScene != null) "Toca para ver u oracular" else "Toca para crear escena"
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Selector de escena
                Box {
                    IconButton(onClick = { showSelector = true }, enabled = !busy, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.ExpandMore, contentDescription = "Elegir escena")
                    }
                    DropdownMenu(expanded = showSelector, onDismissRequest = { showSelector = false }) {
                        scenes.filterNot { it.isCompleted }.forEach { scene ->
                            DropdownMenuItem(text = { Text(scene.title) }, onClick = { onSelect(scene.id); showSelector = false })
                        }
                        DropdownMenuItem(text = { Text("Crear escena") }, onClick = {
                            editingSceneId = 0L
                            showSelector = false
                        })
                    }
                }

                // Botón expandir/plegar acordeón
                IconButton(
                    onClick = { isCollapsed = !isCollapsed },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isCollapsed) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                        contentDescription = if (isCollapsed) "Expandir escena" else "Plegar escena"
                    )
                }
            }

            // Contenido desplegado en acordeón
            androidx.compose.animation.AnimatedVisibility(visible = !isCollapsed) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (context != null) {
                        val overview = listOf(context.location, context.objective).filter { it.isNotBlank() }.joinToString(" · ")
                        if (overview.isNotBlank()) Text(overview, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        if (detailsExpanded && context.situation.isNotBlank()) Text(context.situation, style = MaterialTheme.typography.bodySmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
                        if (context.pendingQuestion.isNotBlank()) Text("Pregunta: ${context.pendingQuestion}", style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text("${probability.displayLabel()} · Factor de Caos $chaos", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                FilledTonalButton(onClick = { onAsk(context.pendingQuestion) },
                                    enabled = !busy && context.pendingQuestion.isNotBlank(), modifier = Modifier.heightIn(min = 40.dp)) { Text("Preguntar") }
                            }
                            item {
                                TextButton(onClick = { editingSceneId = selectedScene?.id }, enabled = !busy,
                                    modifier = Modifier.heightIn(min = 40.dp)) { Text("Editar escena") }
                            }
                            item {
                                TextButton(onClick = { closingSceneId = selectedScene?.id }, enabled = !busy,
                                    modifier = Modifier.heightIn(min = 40.dp)) { Text("Cerrar escena") }
                            }
                            item {
                                TextButton(onClick = { detailsExpanded = !detailsExpanded }, modifier = Modifier.heightIn(min = 40.dp)) {
                                    Text(if (detailsExpanded) "Menos contexto" else "Ver contexto")
                                }
                            }
                        }
                    } else {
                        TextButton(onClick = {
                            editingSceneId = 0L
                        }, enabled = !busy, modifier = Modifier.heightIn(min = 40.dp)) { Text("Crear escena") }
                    }
                    if (lastOracleResult != null && lastOracleResult.sceneId == selectedScene?.id && lastOracleResult.question.isNotBlank()) {
                        Text("${lastOracleResult.question} → ${lastOracleResult.answerLabel}", style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2, overflow = TextOverflow.Ellipsis)
                        TextButton(onClick = { onAppendToDiary(oracleJournalEntry(lastOracleResult)) },
                            enabled = !busy, modifier = Modifier.heightIn(min = 40.dp)) { Text("Guardar consulta en el diario") }
                    }
                    if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                }
            }
        }
    }
    editingScene?.let { scene ->
        SoloSceneEditor(scene, busy, onDismiss = { if (!busy) editingSceneId = null },
            onSave = { updated -> onSave(updated) { editingSceneId = null } })
    }
    closingScene?.let { scene ->
        SoloSceneCloseDialog(scene, busy, onDismiss = { if (!busy) closingSceneId = null },
            onClose = { closure -> onClose(scene, closure) { closingSceneId = null } })
    }
}

@Composable
private fun SoloSceneEditor(scene: Scene, busy: Boolean, onDismiss: () -> Unit, onSave: (Scene) -> Unit) {
    val context = remember(scene) { scene.soloContext() }
    var title by rememberSaveable(scene.id) { mutableStateOf(scene.title) }
    var location by rememberSaveable(scene.id) { mutableStateOf(context.location) }
    var objective by rememberSaveable(scene.id) { mutableStateOf(context.objective) }
    var situation by rememberSaveable(scene.id) { mutableStateOf(context.situation) }
    var question by rememberSaveable(scene.id) { mutableStateOf(context.pendingQuestion) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (scene.id == 0L) "Crear escena" else "Editar escena") },
        text = {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SceneField("Título", title, !busy) { title = it }
                SceneField("Lugar", location, !busy) { location = it }
                SceneField("Objetivo del personaje", objective, !busy) { objective = it }
                SceneField("Situación", situation, !busy) { situation = it }
                SceneField("Pregunta pendiente", question, !busy) { question = it }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(scene.copy(title = title.trim(),
                content = SoloSceneContext(location, objective, situation, question).toSceneContent())) },
                enabled = title.isNotBlank() && !busy) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text("Cancelar") } }
    )
}

@Composable
private fun SoloSceneCloseDialog(scene: Scene, busy: Boolean, onDismiss: () -> Unit, onClose: (SoloSceneClosure) -> Unit) {
    var summary by rememberSaveable(scene.id) { mutableStateOf("") }
    var consequences by rememberSaveable(scene.id) { mutableStateOf("") }
    var pending by rememberSaveable(scene.id) { mutableStateOf(scene.soloContext().pendingQuestion) }
    var nextTitle by rememberSaveable(scene.id) { mutableStateOf("") }
    var chaosDelta by rememberSaveable(scene.id) { mutableStateOf(0) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cerrar ${scene.title}") },
        text = {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SceneField("Resumen", summary, !busy) { summary = it }
                SceneField("Consecuencias", consequences, !busy) { consequences = it }
                SceneField("Pendientes", pending, !busy) { pending = it }
                SceneField("Título de la siguiente escena (opcional)", nextTitle, !busy) { nextTitle = it }
                Text("Ajuste de caos (opcional)", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(-1, 0, 1).forEach { delta ->
                        FilterChip(selected = chaosDelta == delta, onClick = { chaosDelta = delta }, enabled = !busy,
                            label = { Text(if (delta == 0) "Igual" else if (delta > 0) "+1" else "−1") }, modifier = Modifier.heightIn(min = 48.dp))
                    }
                }
                Text("El cierre se guardará en el diario de la sesión.", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(onClick = { onClose(SoloSceneClosure(summary, consequences, pending, nextTitle, chaosDelta)) },
                enabled = summary.isNotBlank() && !busy) { Text("Guardar y cerrar") }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text("Cancelar") } }
    )
}

@Composable
private fun SceneField(label: String, value: String, enabled: Boolean, onChange: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = onChange, label = { Text(label) },
        modifier = Modifier.fillMaxWidth(), enabled = enabled, maxLines = 5)
}
