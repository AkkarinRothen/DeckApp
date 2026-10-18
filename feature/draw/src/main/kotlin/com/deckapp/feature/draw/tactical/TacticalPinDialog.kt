package com.deckapp.feature.draw.tactical

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deckapp.core.model.*

private data class PinIconOption(val key: String, val label: String, val icon: ImageVector)

private val PIN_ICONS = listOf(
    PinIconOption("info", "Info", TacticalPinIcons.Info),
    PinIconOption("door", "Puerta", TacticalPinIcons.Door),
    PinIconOption("trap", "Trampa", TacticalPinIcons.Trap),
    PinIconOption("chest", "Tesoro", TacticalPinIcons.Chest),
    PinIconOption("skull", "Peligro", TacticalPinIcons.Skull),
    PinIconOption("secret", "Secreto", TacticalPinIcons.Secret),
    PinIconOption("stairs", "Escalera", TacticalPinIcons.Stairs),
    PinIconOption("portal", "Portal", TacticalPinIcons.Portal)
)

private val PIN_COLORS = listOf(
    0xFFFFA000L to "Ámbar",
    0xFFE53935L to "Rojo",
    0xFF43A047L to "Verde",
    0xFF1E88E5L to "Azul",
    0xFF8E24AAL to "Violeta",
    0xFFFB8C00L to "Naranja"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TacticalPinDialog(
    initialPin: TacticalMapPin?,
    defaultCellX: Float,
    defaultCellY: Float,
    mapId: Long,
    availableTables: List<RandomTable> = emptyList(),
    availableEncounters: List<Encounter> = emptyList(),
    availableWikiEntries: List<WikiEntry> = emptyList(),
    availableMaps: List<TacticalMap> = emptyList(),
    onDismiss: () -> Unit,
    onSavePin: (TacticalMapPin) -> Unit
) {
    var title by remember { mutableStateOf(initialPin?.title ?: "") }
    var description by remember { mutableStateOf(initialPin?.description ?: "") }
    var selectedIcon by remember { mutableStateOf(initialPin?.iconName ?: "info") }
    var selectedColorHex by remember { mutableStateOf(initialPin?.colorHex ?: 0xFFFFA000L) }
    var isSecret by remember { mutableStateOf(initialPin?.isSecret ?: true) }
    var explorationState by remember { mutableStateOf(initialPin?.explorationState ?: MapPinExplorationState.UNKNOWN) }
    var tableLinks by remember { mutableStateOf(initialPin?.effectiveTableLinks ?: emptyList()) }
    var targetEncounterId by remember { mutableStateOf(initialPin?.targetEncounterId) }
    var targetWikiEntryId by remember { mutableStateOf(initialPin?.targetWikiEntryId) }
    var targetMapId by remember { mutableStateOf(initialPin?.targetMapId) }

    var showTablePicker by remember { mutableStateOf(false) }
    var showEncounterPicker by remember { mutableStateOf(false) }
    var showWikiPicker by remember { mutableStateOf(false) }
    var showMapPicker by remember { mutableStateOf(false) }

    val linkedEncounterName = remember(targetEncounterId, availableEncounters) {
        availableEncounters.find { it.id == targetEncounterId }?.name
    }
    val linkedWikiTitle = remember(targetWikiEntryId, availableWikiEntries) {
        availableWikiEntries.find { it.id == targetWikiEntryId }?.title
    }
    val linkedMapName = remember(targetMapId, availableMaps) {
        availableMaps.find { it.id == targetMapId }?.name
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (initialPin == null) "Nuevo Pin de Dungeon" else "Editar Pin de Sala")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Título
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Título de la Sala / Pin") },
                    placeholder = { Text("Ej: Habitación 3, Trampa de Foso...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Estado de Exploración
                Text(
                    "Estado de Exploración (Solo / DM)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = explorationState == MapPinExplorationState.UNKNOWN,
                        onClick = { explorationState = MapPinExplorationState.UNKNOWN },
                        label = { Text("Desconocido", fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.HelpOutline, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = explorationState == MapPinExplorationState.DISCOVERED,
                        onClick = { explorationState = MapPinExplorationState.DISCOVERED },
                        label = { Text("Descubierto", fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = explorationState == MapPinExplorationState.EXPLORED,
                        onClick = { explorationState = MapPinExplorationState.EXPLORED },
                        label = { Text("Explorado", fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Vínculos con la Aventura
                Text(
                    "Conexiones con la Aventura",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline
                )

                // Vínculos a Tablas Aleatorias (Múltiples tablas por sala)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Casino, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Text("Tablas de la sala (${tableLinks.size})", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                        TextButton(onClick = { showTablePicker = true }) {
                            Text("+ Añadir tabla", fontSize = 11.sp)
                        }
                    }

                    if (tableLinks.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "Ninguna tabla vinculada a esta sala",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    } else {
                        tableLinks.forEachIndexed { index, link ->
                            val currentTable = availableTables.find { it.id == link.tableId }
                            val tableName = currentTable?.name ?: "Tabla #${link.tableId}"
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = tableName,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(
                                            onClick = { tableLinks = tableLinks.filterIndexed { i, _ -> i != index } },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = "Quitar", modifier = Modifier.size(14.dp))
                                        }
                                    }

                                    // Chips rápidos para asignar rol/etiqueta a la tabla
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text("Rol:", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                        val rolePresets = listOf("Botín", "Encuentro", "Secreto", "Evento", "Trampa")
                                        rolePresets.forEach { preset ->
                                            val isSelected = link.label.equals(preset, ignoreCase = true)
                                            FilterChip(
                                                selected = isSelected,
                                                onClick = {
                                                    val newLabel = if (isSelected) "" else preset
                                                    tableLinks = tableLinks.mapIndexed { i, item ->
                                                        if (i == index) item.copy(label = newLabel) else item
                                                    }
                                                },
                                                label = { Text(preset, fontSize = 9.sp) },
                                                modifier = Modifier.height(26.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Vínculo Encuentro Preparado
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.SportsKabaddi, contentDescription = null, tint = Color(0xFFE53935), modifier = Modifier.size(18.dp))
                            Text(
                                text = linkedEncounterName?.let { "Encuentro: $it" } ?: "Sin encuentro",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (linkedEncounterName != null) FontWeight.Medium else FontWeight.Normal
                            )
                        }
                        if (targetEncounterId != null) {
                            IconButton(onClick = { targetEncounterId = null }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Quitar", modifier = Modifier.size(16.dp))
                            }
                        } else {
                            TextButton(onClick = { showEncounterPicker = true }) {
                                Text("+ Vincular", fontSize = 11.sp)
                            }
                        }
                    }
                }

                // Vínculo Lore / Referencia
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.MenuBook, contentDescription = null, tint = Color(0xFF1E88E5), modifier = Modifier.size(18.dp))
                            Text(
                                text = linkedWikiTitle?.let { "Lore: $it" } ?: "Sin entrada de lore",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (linkedWikiTitle != null) FontWeight.Medium else FontWeight.Normal
                            )
                        }
                        if (targetWikiEntryId != null) {
                            IconButton(onClick = { targetWikiEntryId = null }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Quitar", modifier = Modifier.size(16.dp))
                            }
                        } else {
                            TextButton(onClick = { showWikiPicker = true }) {
                                Text("+ Vincular", fontSize = 11.sp)
                            }
                        }
                    }

                    // Fila de Mapa Anidado (Map-in-Map estilo Atlas VTT)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Map, contentDescription = null, tint = Color(0xFF43A047), modifier = Modifier.size(18.dp))
                            Text(
                                text = linkedMapName?.let { "Mapa: $it" } ?: "Sin sub-mapa",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (linkedMapName != null) FontWeight.Medium else FontWeight.Normal
                            )
                        }
                        if (targetMapId != null) {
                            IconButton(onClick = { targetMapId = null }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Quitar", modifier = Modifier.size(16.dp))
                            }
                        } else {
                            TextButton(onClick = { showMapPicker = true }) {
                                Text("+ Vincular", fontSize = 11.sp)
                            }
                        }
                    }
                }

                // Selector de Iconos
                Text("Icono", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PIN_ICONS.forEach { opt ->
                        val isSelected = selectedIcon.equals(opt.key, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedIcon = opt.key },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = opt.icon,
                                contentDescription = opt.label,
                                tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Selector de Colores
                Text("Color", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PIN_COLORS.forEach { (colorHex, _) ->
                        val isSelected = selectedColorHex == colorHex
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(colorHex))
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Black.copy(alpha = 0.3f),
                                    shape = CircleShape
                                )
                                .clickable { selectedColorHex = colorHex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // Switch de Secreto DM
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Secreto del DM / Solo", style = MaterialTheme.typography.bodyMedium)
                        Text("Ocultar detalles a jugadores en mesa", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    }
                    Switch(
                        checked = isSecret,
                        onCheckedChange = { isSecret = it }
                    )
                }

                // Descripción
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descripción / Secretos / Pistas") },
                    placeholder = { Text("Contenido narrativo de la sala, trampas, dificultad de tiradas o tesoro...") },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            FilledTonalButton(
                onClick = {
                    val finalPin = TacticalMapPin(
                        id = initialPin?.id ?: 0,
                        mapId = mapId,
                        cellX = initialPin?.cellX ?: defaultCellX,
                        cellY = initialPin?.cellY ?: defaultCellY,
                        title = title.ifBlank { "Pin de Sala" },
                        description = description,
                        iconName = selectedIcon,
                        colorHex = selectedColorHex,
                        isSecret = isSecret,
                        explorationState = explorationState,
                        targetTableId = tableLinks.firstOrNull()?.tableId,
                        targetEncounterId = targetEncounterId,
                        targetWikiEntryId = targetWikiEntryId,
                        targetMapId = targetMapId,
                        discoveries = initialPin?.discoveries ?: emptyList(),
                        tableLinks = tableLinks
                    )
                    onSavePin(finalPin)
                    onDismiss()
                }
            ) {
                Text("Guardar Pin")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )

    // Picker modal de Tablas Aleatorias
    if (showTablePicker) {
        ResourcePickerModal(
            title = "Vincular Tabla Aleatoria",
            items = availableTables.map { ResourceItem(it.id, it.name, it.category) },
            onSelect = { selectedTableId ->
                if (tableLinks.none { it.tableId == selectedTableId }) {
                    tableLinks = tableLinks + PinTableLink(tableId = selectedTableId, label = "")
                }
                showTablePicker = false
            },
            onDismiss = { showTablePicker = false }
        )
    }

    // Picker modal de Encuentros
    if (showEncounterPicker) {
        ResourcePickerModal(
            title = "Vincular Encuentro",
            items = availableEncounters.map { ResourceItem(it.id, it.name, "${it.creatures.size} criaturas") },
            onSelect = {
                targetEncounterId = it
                showEncounterPicker = false
            },
            onDismiss = { showEncounterPicker = false }
        )
    }

    // Picker modal de Lore / Wiki
    if (showWikiPicker) {
        ResourcePickerModal(
            title = "Vincular Entrada de Lore",
            items = availableWikiEntries.map { ResourceItem(it.id, it.title, "Wiki / Lore") },
            onSelect = {
                targetWikiEntryId = it
                showWikiPicker = false
            },
            onDismiss = { showWikiPicker = false }
        )
    }

    // Picker modal de Sub-mapas (Map-in-Map)
    if (showMapPicker) {
        ResourcePickerModal(
            title = "Vincular Sub-Mapa (Map-in-Map)",
            items = availableMaps.filter { it.id != mapId }.map { ResourceItem(it.id, it.name, "Mapa Táctico") },
            onSelect = {
                targetMapId = it
                showMapPicker = false
            },
            onDismiss = { showMapPicker = false }
        )
    }
}

private data class ResourceItem(val id: Long, val title: String, val subtitle: String)

@Composable
private fun ResourcePickerModal(
    title: String,
    items: List<ResourceItem>,
    onSelect: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    var filterText by remember { mutableStateOf("") }
    val filtered = remember(filterText, items) {
        if (filterText.isBlank()) items
        else items.filter { it.title.contains(filterText, ignoreCase = true) || it.subtitle.contains(filterText, ignoreCase = true) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 350.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = filterText,
                    onValueChange = { filterText = it },
                    placeholder = { Text("Buscar...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (filtered.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp), contentAlignment = Alignment.Center) {
                        Text("No se encontraron elementos disponibles.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(filtered) { item ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelect(item.id) }
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                    Text(item.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    if (item.subtitle.isNotBlank()) {
                                        Text(item.subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        }
    )
}
