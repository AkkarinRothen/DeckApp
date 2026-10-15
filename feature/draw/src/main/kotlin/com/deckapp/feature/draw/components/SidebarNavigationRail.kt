package com.deckapp.feature.draw.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.deckapp.feature.draw.SidebarTool

@Composable
fun SidebarNavigationRail(
    tools: List<Pair<SidebarTool, ImageVector>>,
    activeTool: SidebarTool,
    onToolClick: (SidebarTool) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = modifier.fillMaxHeight().width(64.dp),
        tonalElevation = 1.dp
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(vertical = 16.dp)
        ) {
            tools.forEach { (tool, icon) ->
                val isActive = tool == activeTool
                IconButton(
                    onClick = { onToolClick(tool) },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                        contentColor = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(icon, contentDescription = null)
                }
            }
        }
    }
}
