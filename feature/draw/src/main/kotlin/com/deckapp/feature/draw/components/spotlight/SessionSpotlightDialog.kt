package com.deckapp.feature.draw.components.spotlight

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.deckapp.core.model.TableRollResult
import com.deckapp.core.ui.icons.DeckIcons

private val SpotlightBg = Color(0xF212141C)
private val CardBg = Color(0xFF1B1E28)
private val SelectedCardBg = Color(0xFF262C3D)
private val BorderColor = Color(0x33FFFFFF)
private val AccentGold = Color(0xFFFFD54F)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionSpotlightDialog(
    query: String,
    selectedCategory: SpotlightCategory,
    results: List<SpotlightItem>,
    selectedItem: SpotlightItem?,
    lastRollResult: TableRollResult?,
    isSearching: Boolean,
    onQueryChange: (String) -> Unit,
    onCategorySelect: (SpotlightCategory) -> Unit,
    onItemSelect: (SpotlightItem) -> Unit,
    onRollTable: (Long) -> Unit,
    onAddCreatureToCombat: (name: String, hp: Int, ac: Int, notes: String) -> Unit,
    onAddNpcToCombat: (Long) -> Unit,
    onCopyToDiary: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(20.dp),
            color = SpotlightBg,
            border = BorderStroke(1.dp, BorderColor),
            shadowElevation = 24.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header: Buscador y botón de cerrar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Buscar",
                        tint = AccentGold,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    TextField(
                        value = query,
                        onValueChange = onQueryChange,
                        placeholder = {
                            Text(
                                "Buscar reglas, monstruos, tablas, PNJs, diario... (Ctrl+K)",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 15.sp
                            )
                        },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = AccentGold,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )
                    if (query.isNotBlank()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Limpiar", tint = Color.White.copy(alpha = 0.7f))
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White.copy(alpha = 0.7f))
                    }
                }

                // Categorías de filtro en chips
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(SpotlightCategory.values()) { category ->
                        val isSelected = category == selectedCategory
                        FilterChip(
                            selected = isSelected,
                            onClick = { onCategorySelect(category) },
                            label = { Text(category.label, fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(
                                    getCategoryIcon(category),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (isSelected) AccentGold else Color.White.copy(alpha = 0.6f)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = Color(0x22FFFFFF),
                                labelColor = Color.White.copy(alpha = 0.7f),
                                selectedContainerColor = Color(0x33FFD54F),
                                selectedLabelColor = AccentGold
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = Color(0x22FFFFFF),
                                selectedBorderColor = AccentGold.copy(alpha = 0.8f)
                            )
                        )
                    }
                }

                if (isSearching) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        color = AccentGold,
                        trackColor = Color(0x22FFFFFF)
                    )
                }

                // Cuerpo: Lista de resultados a la izquierda, Quick Peek a la derecha
                BoxWithConstraints(modifier = Modifier.weight(1f)) {
                    val isWide = maxWidth >= 680.dp

                    if (results.isEmpty() && !isSearching) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.SearchOff,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.3f),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = if (query.isBlank()) "Escribe para buscar en todo el compendio"
                                    else "No se encontraron coincidencias para \"$query\"",
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 14.sp
                                )
                            }
                        }
                    } else if (isWide) {
                        Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            // Columna de lista de resultados
                            LazyColumn(
                                modifier = Modifier
                                    .weight(1.1f)
                                    .fillMaxHeight(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(results, key = { it.id }) { item ->
                                    SpotlightListItem(
                                        item = item,
                                        isSelected = selectedItem?.id == item.id,
                                        onClick = { onItemSelect(item) }
                                    )
                                }
                            }

                            // Panel Quick Peek a la derecha
                            Surface(
                                modifier = Modifier
                                    .weight(1.3f)
                                    .fillMaxHeight(),
                                shape = RoundedCornerShape(14.dp),
                                color = CardBg,
                                border = BorderStroke(1.dp, BorderColor)
                            ) {
                                if (selectedItem != null) {
                                    SpotlightQuickPeekContent(
                                        item = selectedItem,
                                        lastRollResult = lastRollResult,
                                        onRollTable = onRollTable,
                                        onAddCreatureToCombat = onAddCreatureToCombat,
                                        onAddNpcToCombat = onAddNpcToCombat,
                                        onCopyToDiary = onCopyToDiary
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "Selecciona un elemento para ver su vista previa",
                                            color = Color.White.copy(alpha = 0.4f),
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Modo compacto / vertical
                        Column(modifier = Modifier.fillMaxSize()) {
                            LazyColumn(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(results, key = { it.id }) { item ->
                                    SpotlightListItem(
                                        item = item,
                                        isSelected = selectedItem?.id == item.id,
                                        onClick = { onItemSelect(item) }
                                    )
                                }
                            }

                            if (selectedItem != null) {
                                Spacer(Modifier.height(12.dp))
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 240.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    color = CardBg,
                                    border = BorderStroke(1.dp, BorderColor)
                                ) {
                                    SpotlightQuickPeekContent(
                                        item = selectedItem,
                                        lastRollResult = lastRollResult,
                                        onRollTable = onRollTable,
                                        onAddCreatureToCombat = onAddCreatureToCombat,
                                        onAddNpcToCombat = onAddNpcToCombat,
                                        onCopyToDiary = onCopyToDiary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SpotlightListItem(
    item: SpotlightItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val icon = getCategoryIcon(item.category)
    val accentColor = getCategoryColor(item.category)

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) SelectedCardBg else CardBg,
        border = BorderStroke(
            1.dp,
            if (isSelected) AccentGold.copy(alpha = 0.8f) else Color(0x1AFFFFFF)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = accentColor.copy(alpha = 0.2f),
                border = BorderStroke(0.5.dp, accentColor.copy(alpha = 0.4f)),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = item.category.label,
                style = MaterialTheme.typography.labelSmall,
                color = accentColor,
                fontSize = 10.sp,
                modifier = Modifier
                    .background(accentColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun SpotlightQuickPeekContent(
    item: SpotlightItem,
    lastRollResult: TableRollResult?,
    onRollTable: (Long) -> Unit,
    onAddCreatureToCombat: (name: String, hp: Int, ac: Int, notes: String) -> Unit,
    onAddNpcToCombat: (Long) -> Unit,
    onCopyToDiary: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Título de la vista previa
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = getCategoryColor(item.category).copy(alpha = 0.2f)
            ) {
                Text(
                    text = item.category.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = getCategoryColor(item.category),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        Text(
            text = item.subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.6f),
            modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
        )

        HorizontalDivider(color = Color(0x22FFFFFF))

        // Contenido específico según el tipo de elemento
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            when (item) {
                is SpotlightItem.TableResult -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = if (item.description.isNotBlank()) item.description else "Tabla aleatoria con ${item.rowCount} entradas.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Fórmula: ${item.formula}",
                            style = MaterialTheme.typography.labelMedium,
                            color = AccentGold
                        )

                        if (lastRollResult != null && lastRollResult.tableId == item.tableId) {
                            Spacer(Modifier.height(12.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0x334CAF50),
                                border = BorderStroke(1.dp, Color(0xFF4CAF50).copy(alpha = 0.6f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(DeckIcons.Dice, null, modifier = Modifier.size(16.dp), tint = Color(0xFF81C784))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Resultado [dado: ${lastRollResult.rollValue}]:",
                                            style = MaterialTheme.typography.labelSmall, color = Color(0xFF81C784))
                                    }
                                    Text(
                                        text = lastRollResult.resolvedText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
                is SpotlightItem.CreatureResult -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text("HP: ${item.hp}", color = Color(0xFFEF5350), fontWeight = FontWeight.Bold)
                            Text("CA: ${item.ac}", color = Color(0xFF42A5F5), fontWeight = FontWeight.Bold)
                            if (item.initiativeBonus != 0) {
                                val sign = if (item.initiativeBonus > 0) "+" else ""
                                Text("Inic: $sign${item.initiativeBonus}", color = AccentGold, fontWeight = FontWeight.Bold)
                            }
                        }
                        if (item.notes.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = item.notes,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
                is SpotlightItem.NpcResult -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = if (item.notes.isNotBlank()) item.notes else "Sin notas adicionales del PNJ.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
                is SpotlightItem.RuleResult -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        item {
                            Text(
                                text = item.content,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.85f),
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
                is SpotlightItem.NoteResult -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = "Línea ${item.lineNumber}:",
                            style = MaterialTheme.typography.labelSmall,
                            color = AccentGold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = item.snippet,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = Color(0x22FFFFFF))
        Spacer(Modifier.height(10.dp))

        // Barra de acciones contextuales según tipo
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (item) {
                is SpotlightItem.TableResult -> {
                    Button(
                        onClick = { onRollTable(item.tableId) },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGold, contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Casino, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Tirar en Tabla", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    if (lastRollResult != null && lastRollResult.tableId == item.tableId) {
                        OutlinedButton(
                            onClick = {
                                onCopyToDiary("🎲 **Tirada en ${item.title}:** ${lastRollResult.resolvedText} (dado: ${lastRollResult.rollValue})")
                            },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, BorderColor),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Al Diario", fontSize = 12.sp)
                        }
                    }
                }
                is SpotlightItem.CreatureResult -> {
                    Button(
                        onClick = { onAddCreatureToCombat(item.title, item.hp, item.ac, item.notes) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF5350), contentColor = Color.White),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Sumar a Combate", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = {
                            val notesSnippet = if (item.notes.isNotBlank()) " — ${item.notes}" else ""
                            onCopyToDiary("- **${item.title}** (HP: ${item.hp}, CA: ${item.ac})$notesSnippet")
                        },
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, BorderColor),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Al Diario", fontSize = 12.sp)
                    }
                }
                is SpotlightItem.NpcResult -> {
                    Button(
                        onClick = { onAddNpcToCombat(item.npcId) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF26A69A), contentColor = Color.White),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Sumar a Combate", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = {
                            val notesSnippet = if (item.notes.isNotBlank()) " — ${item.notes}" else ""
                            onCopyToDiary("- **${item.title}** (${item.subtitle})$notesSnippet")
                        },
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, BorderColor),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Al Diario", fontSize = 12.sp)
                    }
                }
                is SpotlightItem.RuleResult -> {
                    Button(
                        onClick = {
                            onCopyToDiary("### 📜 ${item.title} (${item.gameSystem})\n${item.content}")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF42A5F5), contentColor = Color.White),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Copiar Regla al Diario", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                is SpotlightItem.NoteResult -> {
                    Button(
                        onClick = {
                            onCopyToDiary(item.snippet)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGold, contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Copiar Fragmento", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private fun getCategoryIcon(category: SpotlightCategory): ImageVector = when (category) {
    SpotlightCategory.ALL -> Icons.Default.AllInclusive
    SpotlightCategory.TABLES -> Icons.Default.Casino
    SpotlightCategory.NPCS -> Icons.Default.Person
    SpotlightCategory.RULES -> Icons.AutoMirrored.Filled.MenuBook
    SpotlightCategory.MONSTERS -> Icons.Default.Security
    SpotlightCategory.NOTES -> Icons.Default.EditNote
}

private fun getCategoryColor(category: SpotlightCategory): Color = when (category) {
    SpotlightCategory.ALL -> Color(0xFFB0BEC5)
    SpotlightCategory.TABLES -> Color(0xFFFFB74D)
    SpotlightCategory.NPCS -> Color(0xFF26A69A)
    SpotlightCategory.RULES -> Color(0xFF42A5F5)
    SpotlightCategory.MONSTERS -> Color(0xFFEF5350)
    SpotlightCategory.NOTES -> Color(0xFFAB47BC)
}
