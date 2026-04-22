package com.deckapp.feature.hexploration.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.deckapp.core.model.HexTile
import com.deckapp.feature.hexploration.HexMapSessionUiState

/**
 * Vista central de exploración de Hexcrawl.
 */
@Composable
fun HexcrawlView(
    uiState: HexMapSessionUiState,
    onTileClick: (HexTile) -> Unit,
    onTileLongPress: (HexTile) -> Unit,
    onExploreTile: (HexTile) -> Unit,
    onEmptySpaceClick: (Int, Int) -> Unit,
    onStateChanged: (Float, androidx.compose.ui.geometry.Offset) -> Unit,
    onStartNewDay: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        HexGridCanvas(
            tiles = uiState.tiles,
            pois = uiState.pois,
            mode = if (uiState.isEditMode) HexCanvasMode.DESIGN else HexCanvasMode.SESSION,
            selectedTile = uiState.selectedTile,
            partyLocation = uiState.partyLocation,
            onTileClick = onTileClick,
            onTileLongPress = onTileLongPress,
            onExploreTile = onExploreTile,
            onEmptySpaceClick = onEmptySpaceClick,
            onStateChanged = onStateChanged,
            onTimeSkip = onStartNewDay,
            modifier = Modifier.fillMaxSize()
        )

        // Overlay de indicadores de día (Bento Style)
        uiState.currentDay?.let { day ->
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
                tonalElevation = 6.dp,
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column {
                        Text("Día ${day.dayNumber}", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Actividades: ${uiState.activitiesUsedToday} / ${uiState.maxActivitiesPerDay}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (uiState.activitiesUsedToday >= uiState.maxActivitiesPerDay) MaterialTheme.colorScheme.error else Color.Unspecified
                        )
                    }
                    Button(
                        onClick = onStartNewDay,
                        shape = MaterialTheme.shapes.medium,
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        Icon(Icons.Default.WbSunny, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Siguiente Día")
                    }
                }
            }
        }
    }
}
