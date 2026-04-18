package com.deckapp.feature.wiki

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.deckapp.core.ui.components.MarkdownText
import com.deckapp.core.ui.components.MarkdownToolbar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WikiEntryEditorScreen(
    onBack: () -> Unit,
    viewModel: WikiEntryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        viewModel.onImageSelected(uri)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.entryId == null) "Nueva Entrada" else "Editar Entrada") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.togglePreview() }) {
                        Icon(
                            if (uiState.isPreviewMode) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Previsualizar"
                        )
                    }
                    IconButton(onClick = { viewModel.saveEntry() }) {
                        Icon(Icons.Default.Check, contentDescription = "Guardar", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            // Image Selector
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { galleryLauncher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (uiState.imagePath != null) {
                    AsyncImage(
                        model = uiState.imagePath,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(48.dp))
                        Text("Añadir imagen de Lore", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            if (uiState.isPreviewMode) {
                Text(uiState.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                MarkdownText(markdown = uiState.content.text)
            } else {
                OutlinedTextField(
                    value = uiState.title,
                    onValueChange = { viewModel.onTitleChange(it) },
                    label = { Text("Título") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.titleLarge
                )

                Spacer(Modifier.height(16.dp))

                MarkdownToolbar(
                    value = uiState.content,
                    onValueChange = { viewModel.onContentChange(it) },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = uiState.content,
                    onValueChange = { viewModel.onContentChange(it) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 300.dp),
                    placeholder = { Text("Escribe aquí el lore (soporta Markdown)...") }
                )
            }
            
            Spacer(Modifier.height(100.dp))
        }
    }
    
    if (uiState.isSaved) {
        LaunchedEffect(Unit) { onBack() }
    }
}
