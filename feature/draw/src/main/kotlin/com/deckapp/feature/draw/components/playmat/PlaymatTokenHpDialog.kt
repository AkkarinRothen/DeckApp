package com.deckapp.feature.draw.components.playmat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.deckapp.core.model.PlaymatTokenPlacement

/**
 * Diálogo rápido para ajustar los Puntos de Vida (PV) de un token de personaje.
 */
@Composable
fun PlaymatTokenHpDialog(
    token: PlaymatTokenPlacement,
    onDismiss: () -> Unit,
    onSaveHp: (Int) -> Unit
) {
    var hp by remember(token) { mutableIntStateOf(token.currentHp) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = Color(0xFFE53935)
                    )
                    Text(
                        text = token.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Indicador grande de PV
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "$hp",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (hp <= token.maxHp * 0.25f) Color(0xFFE53935)
                        else if (hp <= token.maxHp * 0.5f) Color(0xFFFFB300)
                        else Color(0xFF4CAF50)
                    )
                    Text(
                        text = " / ${token.maxHp} PV",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
                    )
                }

                // Botones rápidos de ajuste
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    FilledTonalButton(onClick = { hp = (hp - 5).coerceAtLeast(0) }) {
                        Text("-5")
                    }
                    FilledTonalButton(onClick = { hp = (hp - 1).coerceAtLeast(0) }) {
                        Text("-1")
                    }
                    FilledTonalButton(onClick = { hp = (hp + 1).coerceAtMost(token.maxHp) }) {
                        Text("+1")
                    }
                    FilledTonalButton(onClick = { hp = (hp + 5).coerceAtMost(token.maxHp) }) {
                        Text("+5")
                    }
                }

                // Acciones
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onSaveHp(hp)
                            onDismiss()
                        }
                    ) {
                        Text("Guardar")
                    }
                }
            }
        }
    }
}
