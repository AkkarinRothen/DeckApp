package com.deckapp.feature.draw.components.playmat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * Menú radial flotante contextual que aparece sobre la carta seleccionada en el tapete.
 */
@Composable
fun PlaymatRadialMenu(
    visible: Boolean,
    onRotate: () -> Unit,
    onFlip: () -> Unit,
    onReturnToHand: () -> Unit,
    onDiscard: () -> Unit,
    onInspect: () -> Unit,
    onSendToTacticalMap: () -> Unit,
    onStartConnection: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(initialScale = 0.8f),
        exit = fadeOut() + scaleOut(targetScale = 0.8f),
        modifier = modifier
    ) {
        Surface(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.95f),
            tonalElevation = 8.dp,
            shadowElevation = 10.dp,
            modifier = Modifier.padding(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadialActionButton(
                    icon = Icons.Default.RotateRight,
                    description = "Girar 90°",
                    onClick = onRotate
                )
                RadialActionButton(
                    icon = Icons.Default.Flip,
                    description = "Voltear",
                    onClick = onFlip
                )
                RadialActionButton(
                    icon = Icons.Default.Share,
                    description = "Conectar Hilo",
                    onClick = onStartConnection
                )
                RadialActionButton(
                    icon = Icons.Default.PanTool,
                    description = "Mandar a Mano",
                    onClick = onReturnToHand
                )
                RadialActionButton(
                    icon = Icons.Default.Explore,
                    description = "A Mapa Táctico",
                    onClick = onSendToTacticalMap
                )
                RadialActionButton(
                    icon = Icons.Default.DeleteOutline,
                    description = "Descartar",
                    onClick = onDiscard,
                    isDestructive = true
                )
                RadialActionButton(
                    icon = Icons.Default.ZoomIn,
                    description = "Detalle",
                    onClick = onInspect
                )
                RadialActionButton(
                    icon = Icons.Default.Close,
                    description = "Cerrar",
                    onClick = onDismiss
                )
            }
        }
    }
}

@Composable
private fun RadialActionButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    FilledTonalIconButton(
        onClick = onClick,
        modifier = Modifier.size(48.dp),
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = if (isDestructive) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
            contentColor = if (isDestructive) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
                modifier = Modifier.size(20.dp)
        )
    }
}
