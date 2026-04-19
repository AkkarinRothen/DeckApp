package com.deckapp.feature.draw.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.deckapp.core.ui.components.GameSystemsSelector

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionConfigSheet(
    selectedSystems: List<String>,
    onSystemsChanged: (List<String>) -> Unit,
    availableSystems: List<String>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp)
        ) {
            Text(
                "Configuración de la Sesión",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(16.dp))
            
            Text(
                "Sistemas de juego",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "Configura qué sistemas se usan en esta sesión para filtrar las tablas de referencia y reglas.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))

            GameSystemsSelector(
                selectedSystems = selectedSystems,
                onSystemsChanged = onSystemsChanged,
                availableSystems = availableSystems
            )

            Spacer(Modifier.height(24.dp))
            
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Listo")
            }
        }
    }
}
