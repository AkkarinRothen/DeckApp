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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
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

    val voiceFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        viewModel.onVoiceFileSelected(uri)
    }

    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (npc.id == 0L) "Nuevo NPC" else "Editar NPC", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
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
                .consumeWindowInsets(padding)
                .fillMaxSize()
                .imePadding()
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
                    value = viewModel.hpInput,
                    onValueChange = viewModel::updateHp,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(16.dp))
                StatField(
                    label = "AC (Armadura)",
                    value = viewModel.acInput,
                    onValueChange = viewModel::updateAc,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(16.dp))

            StatField(
                label = "Iniciativa (Bono)",
                value = viewModel.initiativeInput,
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
            
            Spacer(Modifier.height(24.dp))

            // Referencia de Voz
            VoiceSampleSection(
                isRecording = viewModel.isRecording,
                isPlaying = viewModel.isPlaying,
                hasSample = viewModel.hasNewSample || npc.voiceSamplePath != null,
                onRecord = {
                    if (hasPermission) {
                        viewModel.startRecording()
                    } else {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                onStopRecording = viewModel::stopRecording,
                onPlay = viewModel::playSample,
                onStopPlayback = viewModel::stopPlayback,
                onDelete = viewModel::deleteSample,
                onSelectFile = { voiceFileLauncher.launch("audio/*") }
            )
            
            Spacer(Modifier.height(100.dp))
        }
    }
}

@Composable
fun VoiceSampleSection(
    isRecording: Boolean,
    isPlaying: Boolean,
    hasSample: Boolean,
    onRecord: () -> Unit,
    onStopRecording: () -> Unit,
    onPlay: () -> Unit,
    onStopPlayback: () -> Unit,
    onDelete: () -> Unit,
    onSelectFile: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "Referencia de Voz",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(Modifier.height(8.dp))
        
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = Color.White.copy(alpha = 0.05f),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isRecording) {
                    // Recording State
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Color.Red)
                    )
                    Text("Grabando...", color = Color.Red, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    IconButton(
                        onClick = onStopRecording,
                        colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Red.copy(alpha = 0.2f))
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = "Detener", tint = Color.Red)
                    }
                } else {
                    // Idle or Playing State
                    IconButton(
                        onClick = onRecord,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        )
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = "Grabar", tint = MaterialTheme.colorScheme.primary)
                    }
                    
                    IconButton(
                        onClick = onSelectFile,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = Color.White.copy(alpha = 0.05f)
                        )
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = "Seleccionar archivo", tint = Color.White.copy(alpha = 0.6f))
                    }

                    if (hasSample) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.05f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Muestra de voz", fontSize = 14.sp, color = Color.White.copy(alpha = 0.8f))
                            }
                        }

                        IconButton(onClick = if (isPlaying) onStopPlayback else onPlay) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, contentDescription = "Borrar", tint = Color.White.copy(alpha = 0.3f))
                        }
                    } else {
                        Text("No hay muestra grabada", color = Color.White.copy(alpha = 0.3f), fontSize = 14.sp, modifier = Modifier.weight(1f))
                    }
                }
            }
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
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        Text(label, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
            )
        )
    }
}
