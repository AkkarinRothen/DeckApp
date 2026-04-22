package com.deckapp.feature.npcs

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.deckapp.core.model.Npc
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NpcListScreen(
    onBack: () -> Unit,
    onAddNpc: () -> Unit,
    onEditNpc: (Long) -> Unit,
    viewModel: NpcListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isSimplified by viewModel.isSimplifiedMode
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }
    
    var peekedNpc by remember { mutableStateOf<Npc?>(null) }
    val haptic = LocalHapticFeedback.current

    if (peekedNpc != null) {
        ModalBottomSheet(onDismissRequest = { peekedNpc = null }) {
            NpcPeekContent(npc = peekedNpc!!, onEdit = { 
                onEditNpc(peekedNpc!!.id)
                peekedNpc = null
            })
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Biblioteca de NPCs", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO: Filtros */ }) {
                        Icon(Icons.Default.Search, contentDescription = "Buscar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            LargeFloatingActionButton(
                onClick = {
                    if (isSimplified) {
                        viewModel.quickGenerateNpc { name ->
                            scope.launch {
                                snackbarHostState.showSnackbar("Generado: $name")
                            }
                        }
                    } else {
                        onAddNpc()
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Añadir NPC")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            when (val state = uiState) {
                is NpcListUiState.Loading -> {
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                is NpcListUiState.Success -> {
                    if (state.npcs.isEmpty()) {
                        EmptyNpcsMessage()
                    } else {
                        NpcGrid(
                            npcs = state.npcs,
                            onEditNpc = onEditNpc,
                            onLongPressNpc = { npc ->
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                peekedNpc = npc
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NpcGrid(
    npcs: List<Npc>,
    onEditNpc: (Long) -> Unit,
    onLongPressNpc: (Npc) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(160.dp),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(npcs) { npc ->
            NpcCard(
                npc = npc, 
                onClick = { onEditNpc(npc.id) },
                onLongClick = { onLongPressNpc(npc) }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NpcCard(
    npc: Npc,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.05f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                if (npc.imagePath != null) {
                    AsyncImage(
                        model = npc.imagePath,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = npc.name.take(1).uppercase(),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = npc.name,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                maxLines = 1,
                color = Color.White
            )

            Spacer(Modifier.height(4.dp))

            // Stats resumidas
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatBadge(label = "HP", value = npc.maxHp.toString(), color = Color(0xFFEF5350))
                StatBadge(label = "AC", value = npc.armorClass.toString(), color = Color(0xFF42A5F5))
            }
        }
    }
}

@Composable
fun NpcPeekContent(npc: Npc, onEdit: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .padding(bottom = 32.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                if (npc.imagePath != null) {
                    AsyncImage(model = npc.imagePath, contentDescription = null, contentScale = ContentScale.Crop)
                } else {
                    Text(npc.name.take(1).uppercase(), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(npc.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                if (npc.description.isNotBlank()) {
                    Text(npc.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Button(onClick = onEdit) {
                Text("Editar")
            }
        }

        Spacer(Modifier.height(24.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            StatPeekBox("HP", npc.maxHp.toString(), Color(0xFFEF5350))
            StatPeekBox("AC", npc.armorClass.toString(), Color(0xFF42A5F5))
            StatPeekBox("INIT", if (npc.initiativeBonus >= 0) "+${npc.initiativeBonus}" else npc.initiativeBonus.toString(), Color(0xFFFFB74D))
        }

        if (npc.notes.isNotBlank()) {
            Spacer(Modifier.height(24.dp))
            Text("Notas", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(4.dp))
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    npc.notes, 
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        
        if (npc.tags.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                npc.tags.forEach { tag ->
                    SuggestionChip(onClick = {}, label = { Text(tag.name) })
                }
            }
        }
    }
}

@Composable
fun StatPeekBox(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Bold)
        Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
    }
}

@Composable
fun StatBadge(label: String, value: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.2f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$label ",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = value,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
        }
    }
}

@Composable
fun EmptyNpcsMessage() {
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "La biblioteca está vacía",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White.copy(alpha = 0.6f)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "¡Crea a tus héroes y villanos!",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.4f)
            )
        }
    }
}
