package com.deckapp.feature.draw.tactical

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.deckapp.core.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TacticalAreasSheet(areas: List<PersistentAoE>, busy: Boolean, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text("Áreas fijadas", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(20.dp))
        LazyColumn(Modifier.fillMaxWidth().heightIn(max = 420.dp).navigationBarsPadding()) {
            items(areas, key = { it.id }) { area ->
                ListItem(
                    headlineContent = { Text(area.areaLabel(), maxLines = 2, overflow = TextOverflow.Ellipsis) },
                    supportingContent = { Text("${area.shape.areaLabel()} · ${area.sizeFt} ft" + if (area.anchorTokenId != null) " · Aura" else "") },
                    trailingContent = { Text("Editar") },
                    modifier = Modifier.clickable(enabled = !busy) { onSelect(area.id) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun TacticalAreaEditor(
    map: TacticalMap, area: PersistentAoE, tokens: List<TacticalToken>, busy: Boolean,
    onSave: (PersistentAoE) -> Unit, onDelete: () -> Unit, onMove: () -> Unit, onDismiss: () -> Unit
) {
    var name by rememberSaveable(map.id, area.id) { mutableStateOf(area.name) }
    var shape by rememberSaveable(map.id, area.id) { mutableStateOf(area.shape) }
    var size by rememberSaveable(map.id, area.id) { mutableStateOf(area.sizeFt.toString()) }
    var angle by rememberSaveable(map.id, area.id) { mutableFloatStateOf(area.angleDegrees) }
    var color by rememberSaveable(map.id, area.id) { mutableLongStateOf(area.colorHex) }
    var anchor by rememberSaveable(map.id, area.id) { mutableStateOf(area.anchorTokenId) }
    val initialOrigin = area.resolvedOrigin(map, tokens)
    var x by rememberSaveable(map.id, area.id) { mutableFloatStateOf(initialOrigin.x) }
    var y by rememberSaveable(map.id, area.id) { mutableFloatStateOf(initialOrigin.y) }
    var showTokens by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val sizeValue = size.toIntOrNull()
    val valid = sizeValue != null && sizeValue in 1..1000 && (anchor == null || tokens.any { it.id == anchor })
    val draft = area.copy(name = name.trim(), shape = shape, sizeFt = sizeValue ?: area.sizeFt,
        originWorld = Offset(x, y), angleDegrees = angle, colorHex = color, anchorTokenId = anchor)
    val dirty = (if (anchor != null) draft.copy(originWorld = area.originWorld) else draft) != area

    ModalBottomSheet(
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        onDismissRequest = { if (!busy) onDismiss() }
    ) {
        Column(Modifier.fillMaxWidth().heightIn(max = 640.dp).imePadding().navigationBarsPadding().padding(horizontal = 20.dp)) {
            Text("Editar área", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(vertical = 12.dp))
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(name, { name = it.take(80) }, label = { Text("Nombre") },
                    placeholder = { Text("Ej.: Nube venenosa") }, singleLine = true, enabled = !busy, modifier = Modifier.fillMaxWidth())
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TacticalMeasureShape.entries.forEach { value ->
                        FilterChip(enabled = !busy, selected = shape == value, onClick = { shape = value }, label = { Text(value.areaLabel()) })
                    }
                }
                OutlinedTextField(size, { size = it.filter(Char::isDigit).take(4) }, label = { Text("Tamaño en pies") },
                    isError = sizeValue == null || sizeValue !in 1..1000,
                    supportingText = { Text(if (shape == TacticalMeasureShape.CIRCLE) "Radio: entre 1 y 1000 pies" else "Alcance/lado: entre 1 y 1000 pies") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, enabled = !busy, modifier = Modifier.fillMaxWidth())
                if (shape != TacticalMeasureShape.CIRCLE) {
                    Text("Orientación: ${angle.toInt()}°")
                    Slider(enabled = !busy, value = angle.coerceIn(0f, 360f), onValueChange = { angle = it }, valueRange = 0f..360f)
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Ámbar" to 0x44FF9800L, "Rojo" to 0x44F44336L, "Azul" to 0x442196F3L,
                        "Verde" to 0x444CAF50L, "Violeta" to 0x449C27B0L).forEach { (label, value) ->
                        FilterChip(enabled = !busy, selected = color == value, onClick = { color = value }, label = { Text(label) })
                    }
                }
                Box {
                    OutlinedButton(enabled = !busy, onClick = { showTokens = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (anchor == null) "Posición fija" else tokens.find { it.id == anchor }?.let { "Seguir a ${it.name}" } ?: "Token no disponible",
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    DropdownMenu(expanded = showTokens, onDismissRequest = { showTokens = false }) {
                        DropdownMenuItem(text = { Text("Posición fija") }, onClick = {
                            val point = draft.resolvedOrigin(map, tokens)
                            x = point.x; y = point.y; anchor = null; showTokens = false
                        })
                        tokens.forEach { token ->
                            DropdownMenuItem(text = { Text(token.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                onClick = { anchor = token.id; showTokens = false })
                        }
                    }
                }
                if (anchor == null) {
                    Text("Ajustar centro de a una casilla")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(enabled = !busy, onClick = { x -= map.cellSizePx }) { Text("←") }
                        OutlinedButton(enabled = !busy, onClick = { y -= map.cellSizePx }) { Text("↑") }
                        OutlinedButton(enabled = !busy, onClick = { y += map.cellSizePx }) { Text("↓") }
                        OutlinedButton(enabled = !busy, onClick = { x += map.cellSizePx }) { Text("→") }
                    }
                }
                OutlinedButton(onClick = onMove, enabled = !dirty && !busy, modifier = Modifier.fillMaxWidth()) { Text("Mover tocando el mapa") }
                if (dirty) Text("Guardá los cambios antes de moverla en el mapa.", style = MaterialTheme.typography.bodySmall)
                else if (anchor != null) Text("Al moverla en el mapa se separa del token.", style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = { confirmDelete = true }, enabled = !busy) { Text("Eliminar esta área", color = MaterialTheme.colorScheme.error) }
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss, enabled = !busy) { Text("Cancelar") }
                Button(onClick = { onSave(draft) }, enabled = valid && !busy && dirty) { Text(if (busy) "Guardando…" else "Guardar") }
            }
        }
    }
    if (confirmDelete) {
        AlertDialog(onDismissRequest = { confirmDelete = false }, title = { Text("Eliminar área") },
            text = { Text("Se eliminará únicamente ${area.areaLabel()}. Podés recuperarla con Deshacer.") },
            confirmButton = { TextButton(onClick = { confirmDelete = false; onDelete() }) { Text("Eliminar") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancelar") } })
    }
}

fun PersistentAoE.areaLabel(): String =
    name.ifBlank { "${shape.areaLabel()} $sizeFt ft" }

fun TacticalMeasureShape.areaLabel(): String = when (this) {
    TacticalMeasureShape.CIRCLE -> "Círculo / Esfera"
    TacticalMeasureShape.CONE -> "Cono"
    TacticalMeasureShape.LINE -> "Línea"
    TacticalMeasureShape.BOX -> "Cubo / Cuadrado"
}

fun PersistentAoE.resolvedOrigin(map: TacticalMap, tokens: List<TacticalToken>): Offset {
    val anchor = anchorTokenId?.let { id -> tokens.find { it.id == id } }
    return if (anchor != null) {
        val cellSize = map.cellSizePx
        Offset(
            anchor.cellX * cellSize + (anchor.sizeInCells * cellSize / 2f),
            anchor.cellY * cellSize + (anchor.sizeInCells * cellSize / 2f)
        )
    } else {
        originWorld
    }
}

