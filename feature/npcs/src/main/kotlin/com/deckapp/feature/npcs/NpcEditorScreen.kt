package com.deckapp.feature.npcs

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NpcEditorScreen(
    onBack: () -> Unit,
    viewModel: NpcEditorViewModel = hiltViewModel()
) {
    val npc = viewModel.npc
    val scrollState = rememberScrollState()
    
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        viewModel.onImageSelected(uri)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (npc.id == 0L) "Nuevo NPC" else "Editar NPC", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.save(onBack) }) {
                        Icon(Icons.Default.Check, contentDescription = "Guardar", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar Selector
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f))
                    .clickable { galleryLauncher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                val displayImage = viewModel.selectedImageUri ?: npc.imagePath
                if (displayImage != null) {
                    AsyncImage(
                        model = displayImage,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(Icons.Default.Image, contentDescription = null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(40.dp))
                }
            }
            
            Spacer(Modifier.height(8.dp))
            Text("Toca para cambiar imagen", fontSize = 12.sp, color = Color.White.copy(alpha = 0.4f))

            Spacer(Modifier.height(24.dp))

            // Secciones del Formulario
            EditorTextField(
                label = "Nombre",
                value = npc.name,
                onValueChange = viewModel::updateName,
                placeholder = "Ej: Lord Silas"
            )

            Spacer(Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                StatField(
                    label = "HP Máx",
                    value = npc.maxHp,
                    onValueChange = viewModel::updateHp,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(16.dp))
                StatField(
                    label = "AC (Armadura)",
                    value = npc.armorClass,
                    onValueChange = viewModel::updateAc,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(16.dp))

            StatField(
                label = "Iniciativa (Bono)",
                value = npc.initiativeBonus,
                onValueChange = viewModel::updateInitiative,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(24.dp))

            // Notas Markdown
            Text(
                "Notas y Lore (Markdown)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = npc.notes,
                onValueChange = viewModel::updateNotes,
                modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                    focusedContainerColor = Color.White.copy(alpha = 0.05f)
                ),
                placeholder = { Text("Describe a tu personaje...", color = Color.White.copy(alpha = 0.3f)) }
            )
            
            Spacer(Modifier.height(100.dp))
        }
    }
}

@Composable
fun EditorTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = ""
) {
    Column {
        Text(label, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            placeholder = { Text(placeholder, color = Color.White.copy(alpha = 0.3f)) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
            )
        )
    }
}

@Composable
fun StatField(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        Text(label, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = value.toString(),
            onValueChange = { onValueChange(it.toIntOrNull() ?: 0) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
            )
        )
    }
}
