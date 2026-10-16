package com.deckapp.feature.draw.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.deckapp.core.model.CardStack
import com.deckapp.core.model.HotbarActionType
import com.deckapp.core.model.RandomTable
import com.deckapp.core.model.SessionHotbarAction

private val HOTBAR_COLORS = listOf(
    0xFF5C6BC0L, // Indigo
    0xFF42A5F5L, // Blue
    0xFF26A69AL, // Teal
    0xFF66BB6AL, // Green
    0xFFFFA726L, // Orange
    0xFFFF7043L, // Deep Orange
    0xFFAB47BCL, // Purple
    0xFFEC407AL  // Pink
)

/** Diálogo modal para crear o editar un atajo o macro rápida en la Hotbar */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HotbarActionDialog(
    action: SessionHotbarAction?,
    availableTables: List<RandomTable>,
    availableDecks: List<CardStack>,
    onDismiss: () -> Unit,
    onSave: (SessionHotbarAction) -> Unit,
    onDelete: ((String) -> Unit)? = null
) {
    val isEditing = action != null
    var selectedType by remember { mutableStateOf(action?.type ?: HotbarActionType.DICE_ROLL) }
    var label by remember { mutableStateOf(action?.label ?: "") }
    var payload by remember {
        mutableStateOf(
            action?.payload ?: when (selectedType) {
                HotbarActionType.DICE_ROLL -> "1d20"
                HotbarActionType.ORACLE_QUERY -> "50_50"
                HotbarActionType.RANDOM_TABLE -> availableTables.firstOrNull()?.id?.toString() ?: ""
                HotbarActionType.DRAW_CARD -> availableDecks.firstOrNull()?.id?.toString() ?: ""
            }
        )
    }
    var selectedColor by remember { mutableStateOf(action?.colorHex ?: HOTBAR_COLORS.first()) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Título
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isEditing) "Editar Atajo Rápido" else "Nuevo Atajo Rápido",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Cerrar")
                    }
                }

                // Selector de Tipo de Acción
                Text(
                    text = "Tipo de Atajo",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    HotbarTypeTab(
                        title = "Dado",
                        icon = Icons.Default.Casino,
                        isSelected = selectedType == HotbarActionType.DICE_ROLL,
                        onClick = {
                            selectedType = HotbarActionType.DICE_ROLL
                            if (payload.isBlank() || payload == "50_50" || payload == "RANDOM_EVENT") payload = "1d20"
                        },
                        modifier = Modifier.weight(1f)
                    )
                    HotbarTypeTab(
                        title = "Oráculo",
                        icon = Icons.Default.AutoAwesome,
                        isSelected = selectedType == HotbarActionType.ORACLE_QUERY,
                        onClick = {
                            selectedType = HotbarActionType.ORACLE_QUERY
                            payload = "50_50"
                        },
                        modifier = Modifier.weight(1f)
                    )
                    HotbarTypeTab(
                        title = "Tabla",
                        icon = Icons.Default.FormatListNumbered,
                        isSelected = selectedType == HotbarActionType.RANDOM_TABLE,
                        onClick = {
                            selectedType = HotbarActionType.RANDOM_TABLE
                            payload = availableTables.firstOrNull()?.id?.toString() ?: ""
                        },
                        modifier = Modifier.weight(1f)
                    )
                    HotbarTypeTab(
                        title = "Mazo",
                        icon = Icons.Default.Style,
                        isSelected = selectedType == HotbarActionType.DRAW_CARD,
                        onClick = {
                            selectedType = HotbarActionType.DRAW_CARD
                            payload = availableDecks.firstOrNull()?.id?.toString() ?: ""
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Campo Nombre / Etiqueta
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Nombre o Etiqueta") },
                    placeholder = {
                        Text(
                            when (selectedType) {
                                HotbarActionType.DICE_ROLL -> "Ej. Ataque Espada, Sigilo"
                                HotbarActionType.ORACLE_QUERY -> "Ej. Pregunta Rápida, Evento"
                                HotbarActionType.RANDOM_TABLE -> "Ej. Encuentros, Botín"
                                HotbarActionType.DRAW_CARD -> "Ej. Robar Destino"
                            }
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Contenido específico según el tipo
                when (selectedType) {
                    HotbarActionType.DICE_ROLL -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = payload,
                                onValueChange = { payload = it },
                                label = { Text("Fórmula de Dados") },
                                placeholder = { Text("ej. 1d20+5, 2d6+3, d100") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Text(
                                text = "Atajos rápidos:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(listOf("1d20", "1d20+2", "1d20+5", "2d6", "1d8", "1d10", "1d12", "1d100")) { formula ->
                                    SuggestionChip(
                                        onClick = { payload = formula },
                                        label = { Text(formula, fontSize = 11.sp) }
                                    )
                                }
                            }
                        }
                    }

                    HotbarActionType.ORACLE_QUERY -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Probabilidad / Modo de Oráculo",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            val oracleOptions = listOf(
                                Pair("50_50", "Pregunta 50 / 50"),
                                Pair("RANDOM_EVENT", "⚡ Evento Aleatorio (Foco + Significado)"),
                                Pair("NEAR_SURE", "Casi Seguro (+40)"),
                                Pair("UNLIKELY", "Improbable (-20)")
                            )

                            oracleOptions.forEach { (key, optLabel) ->
                                Surface(
                                    onClick = { payload = key },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (payload == key) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    border = BorderStroke(
                                        1.dp,
                                        if (payload == key) MaterialTheme.colorScheme.primary else Color.Transparent
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = optLabel,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = if (payload == key) FontWeight.Bold else FontWeight.Normal
                                            )
                                        )
                                        if (payload == key) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    HotbarActionType.RANDOM_TABLE -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Selecciona la Tabla",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            if (availableTables.isEmpty()) {
                                Text(
                                    text = "No hay tablas registradas en la sesión.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            } else {
                                availableTables.forEach { table ->
                                    val isSelected = payload == table.id.toString()
                                    Surface(
                                        onClick = {
                                            payload = table.id.toString()
                                            if (label.isBlank()) label = table.name
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.secondary else Color.Transparent),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = table.name,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                )
                                            )
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.secondary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    HotbarActionType.DRAW_CARD -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Selecciona el Mazo para Robar",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            if (availableDecks.isEmpty()) {
                                Text(
                                    text = "No hay mazos activos en la sesión.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            } else {
                                availableDecks.forEach { deck ->
                                    val isSelected = payload == deck.id.toString()
                                    Surface(
                                        onClick = {
                                            payload = deck.id.toString()
                                            if (label.isBlank()) label = "Robar ${deck.name}"
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.tertiary else Color.Transparent),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = deck.name,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                )
                                            )
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.tertiary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Selector de Color
                Text(
                    text = "Color del Atajo",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(HOTBAR_COLORS) { colorHex ->
                        val color = Color(colorHex)
                        val isSelected = selectedColor == colorHex
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    BorderStroke(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) Color.White else Color.Transparent
                                    ),
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = colorHex },
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

                // Botones de Acción
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isEditing && onDelete != null && action != null) {
                        TextButton(
                            onClick = {
                                onDelete(action.id)
                                onDismiss()
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Eliminar")
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onDismiss) {
                            Text("Cancelar")
                        }

                        Button(
                            onClick = {
                                val finalLabel = label.ifBlank {
                                    when (selectedType) {
                                        HotbarActionType.DICE_ROLL -> payload
                                        HotbarActionType.ORACLE_QUERY -> "Oráculo"
                                        HotbarActionType.RANDOM_TABLE -> "Tabla"
                                        HotbarActionType.DRAW_CARD -> "Robar"
                                    }
                                }
                                onSave(
                                    SessionHotbarAction(
                                        id = action?.id ?: java.util.UUID.randomUUID().toString(),
                                        label = finalLabel,
                                        type = selectedType,
                                        payload = payload,
                                        colorHex = selectedColor
                                    )
                                )
                                onDismiss()
                            },
                            enabled = payload.isNotBlank()
                        ) {
                            Text("Guardar")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HotbarTypeTab(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent),
        modifier = modifier.height(54.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
