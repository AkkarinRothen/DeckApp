package com.deckapp.feature.draw.components.playmat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deckapp.core.model.SessionLogEntry
import com.deckapp.core.model.SessionLogEventType
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaymatSessionDrawer(
    isOpen: Boolean,
    selectedTab: Int,
    logs: List<SessionLogEntry>,
    notesMarkdown: String,
    isFilterCurrentScene: Boolean,
    currentSceneTitle: String?,
    onClose: () -> Unit,
    onTabSelected: (Int) -> Unit,
    onToggleFilterCurrentScene: () -> Unit,
    onAddManualLog: (String) -> Unit,
    onSaveNotes: (String) -> Unit,
    onClearLogs: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isOpen) return

    val context = LocalContext.current
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

    var manualLogInput by remember { mutableStateOf("") }
    var notesText by remember(notesMarkdown) { mutableStateOf(notesMarkdown) }
    var isSavingNotes by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    // Debounce continuo de guardado automático (400ms tras dejar de escribir)
    LaunchedEffect(notesText) {
        if (notesText != notesMarkdown) {
            isSavingNotes = true
            kotlinx.coroutines.delay(400)
            onSaveNotes(notesText)
            isSavingNotes = false
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (notesText != notesMarkdown) {
                onSaveNotes(notesText)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(360.dp)
            .background(Color(0xFF14151B).copy(alpha = 0.96f))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
            )
            .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Cabecera del Drawer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (selectedTab == 0) Icons.Default.AutoStories else Icons.Default.EditNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (selectedTab == 0) "Bitácora en Vivo" else "Notas de la Mesa",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (selectedTab == 1) {
                        if (isSavingNotes) {
                            Text(
                                text = "Guardando...",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Text(
                                text = "Guardado ✓",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF4CAF50)
                            )
                        }
                        Spacer(Modifier.width(4.dp))
                        IconButton(
                            onClick = {
                                keyboardController?.hide()
                                focusManager.clearFocus()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.KeyboardHide,
                                contentDescription = "Ocultar teclado",
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(Modifier.width(4.dp))
                    }

                    IconButton(
                        onClick = {
                            if (notesText != notesMarkdown) {
                                onSaveNotes(notesText)
                            }
                            onClose()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar panel",
                            tint = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // Pestañas Superiores
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFF1B1D26),
                contentColor = MaterialTheme.colorScheme.primary,
                divider = {}
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { onTabSelected(0) },
                    text = { Text("Bitácora (${logs.size})", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { onTabSelected(1) },
                    text = { Text("Notas", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }

            Divider(color = Color.White.copy(alpha = 0.1f), thickness = 1.dp)

            if (selectedTab == 0) {
                // ── PESTAÑA 0: BITÁCORA EN VIVO ─────────────────────────────────────
                Column(modifier = Modifier.weight(1f)) {
                    // Barra de Filtros y Acciones
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = isFilterCurrentScene,
                            onClick = onToggleFilterCurrentScene,
                            label = {
                                Text(
                                    text = if (isFilterCurrentScene) currentSceneTitle ?: "Escena actual" else "Toda la sesión",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontSize = 12.sp
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (isFilterCurrentScene) Icons.Default.FilterAlt else Icons.Default.FilterAltOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )

                        IconButton(
                            onClick = { showClearConfirmDialog = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Limpiar bitácora",
                                tint = Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Lista de Eventos
                    if (logs.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.MenuBook,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.25f),
                                    modifier = Modifier.size(44.dp)
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "La bitácora está vacía.\nLas tiradas, cartas y notas aparecerán aquí.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.45f),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(logs, key = { it.id }) { entry ->
                                LogEntryCard(entry = entry, timeFormatter = timeFormatter)
                            }
                        }
                    }

                    // Input de Nota Manual Rápida
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1B1D26))
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = manualLogInput,
                            onValueChange = { manualLogInput = it },
                            placeholder = { Text("Escribe una nota rápida...", fontSize = 13.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(20.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                focusedContainerColor = Color(0xFF121318),
                                unfocusedContainerColor = Color(0xFF121318)
                            )
                        )
                        Spacer(Modifier.width(6.dp))
                        IconButton(
                            onClick = {
                                if (manualLogInput.isNotBlank()) {
                                    onAddManualLog(manualLogInput.trim())
                                    manualLogInput = ""
                                }
                            },
                            enabled = manualLogInput.isNotBlank()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Enviar nota",
                                tint = if (manualLogInput.isNotBlank()) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.3f)
                            )
                        }
                    }
                }
            } else {
                // ── PESTAÑA 1: NOTAS DE LA MESA (MARKDOWN) ──────────────────────────
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(12.dp)
                ) {
                    // Quick Markdown Helpers Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SuggestionChip(
                            onClick = {
                                notesText = "$notesText\n- [ ] "
                                onSaveNotes(notesText)
                            },
                            label = { Text("[ ] Tarea", fontSize = 11.sp) },
                            icon = { Icon(Icons.Default.CheckCircleOutline, null, modifier = Modifier.size(12.dp)) }
                        )

                        SuggestionChip(
                            onClick = {
                                val time = timeFormatter.format(Date())
                                notesText = "$notesText\n**[$time]** "
                                onSaveNotes(notesText)
                            },
                            label = { Text("⏰ Hora", fontSize = 11.sp) },
                            icon = { Icon(Icons.Default.AccessTime, null, modifier = Modifier.size(12.dp)) }
                        )

                        SuggestionChip(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("DeckApp Notes", notesText)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Notas copiadas al portapapeles", Toast.LENGTH_SHORT).show()
                            },
                            label = { Text("Copiar", fontSize = 11.sp) },
                            icon = { Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(12.dp)) }
                        )
                    }

                    // Editor de Markdown
                    OutlinedTextField(
                        value = notesText,
                        onValueChange = {
                            notesText = it
                        },
                        placeholder = {
                            Text(
                                text = "Escribe aquí tus notas, pistas, objetivos de la misión o consecuencias...",
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.35f)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedContainerColor = Color(0xFF101116),
                            unfocusedContainerColor = Color(0xFF101116)
                        ),
                        textStyle = LocalTextStyle.current.copy(
                            fontFamily = FontFamily.Default,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = Color.White
                        )
                    )
                }
            }
        }
    }

    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("¿Vaciar la bitácora?") },
            text = { Text("Esta acción eliminará todos los registros del historial de esta sesión.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearLogs()
                        showClearConfirmDialog = false
                    }
                ) {
                    Text("Vaciar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun LogEntryCard(
    entry: SessionLogEntry,
    timeFormatter: SimpleDateFormat,
    modifier: Modifier = Modifier
) {
    val (icon, iconColor, bgBadgeColor) = remember(entry.eventType) {
        when (entry.eventType) {
            SessionLogEventType.DICE -> Triple(Icons.Default.Casino, Color(0xFF00E5FF), Color(0xFF00E5FF).copy(alpha = 0.15f))
            SessionLogEventType.CARD -> Triple(Icons.Default.Style, Color(0xFFFFB300), Color(0xFFFFB300).copy(alpha = 0.15f))
            SessionLogEventType.TOKEN -> Triple(Icons.Default.Shield, Color(0xFFE53935), Color(0xFFE53935).copy(alpha = 0.15f))
            SessionLogEventType.ORACLE -> Triple(Icons.Default.AutoAwesome, Color(0xFFD500F9), Color(0xFFD500F9).copy(alpha = 0.15f))
            SessionLogEventType.CLOCK -> Triple(Icons.Default.Schedule, Color(0xFF4CAF50), Color(0xFF4CAF50).copy(alpha = 0.15f))
            SessionLogEventType.MANUAL -> Triple(Icons.Default.EditNote, Color(0xFFEEEEEE), Color.White.copy(alpha = 0.10f))
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF1E202B),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Icono del Evento con Badge
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(28.dp)
                    .background(bgBadgeColor, CircleShape)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(Modifier.width(8.dp))

            // Contenido
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = entry.title,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = timeFormatter.format(Date(entry.timestamp)),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 10.sp
                    )
                }

                if (entry.details.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = entry.details,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}
