package com.deckapp.feature.reference

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.deckapp.core.ui.components.MarkdownText
import com.deckapp.core.ui.components.MarkdownToolbar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RuleEditorScreen(
    ruleId: Long,
    prefilledSystem: String = "",
    onNavigateBack: () -> Unit,
    viewModel: RuleEditorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    var contentValue by remember { mutableStateOf(TextFieldValue("")) }

    LaunchedEffect(ruleId) {
        viewModel.loadRule(ruleId, prefilledSystem)
    }

    // Sync state from VM to local TextFieldValue (only once or when title changes/loaded)
    LaunchedEffect(uiState.id) {
        if (uiState.content != contentValue.text) {
            contentValue = TextFieldValue(uiState.content)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (ruleId == -1L) "Nueva regla" else "Editar regla") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.onContentChanged(contentValue.text)
                            viewModel.save()
                            onNavigateBack()
                        },
                        enabled = uiState.title.isNotBlank() && !uiState.isSaving
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Guardar")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                OutlinedTextField(
                    value = uiState.title,
                    onValueChange = viewModel::onTitleChanged,
                    label = { Text("Título *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.gameSystem,
                        onValueChange = viewModel::onSystemChanged,
                        label = { Text("Sistema") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = uiState.category,
                        onValueChange = viewModel::onCategoryChanged,
                        label = { Text("Categoría") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
            }

            item {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = !uiState.isPreviewMode,
                        onClick = { if (uiState.isPreviewMode) viewModel.togglePreviewMode() },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) {
                        Text("Editar")
                    }
                    SegmentedButton(
                        selected = uiState.isPreviewMode,
                        onClick = { if (!uiState.isPreviewMode) viewModel.togglePreviewMode() },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) {
                        Text("Vista previa")
                    }
                }
            }

            item {
                if (uiState.isPreviewMode) {
                    Card(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        MarkdownText(
                            markdown = contentValue.text.ifBlank { "_Sin contenido_" },
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        MarkdownToolbar(
                            value = contentValue,
                            onValueChange = { contentValue = it },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = contentValue,
                            onValueChange = { contentValue = it },
                            label = { Text("Contenido (Markdown)") },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 300.dp),
                            placeholder = { Text("Escribe las reglas aquí...") }
                        )
                    }
                }
            }
        }
    }
}
