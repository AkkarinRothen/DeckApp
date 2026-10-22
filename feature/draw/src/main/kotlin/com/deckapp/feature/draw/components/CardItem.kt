package com.deckapp.feature.draw.components

import com.deckapp.core.ui.components.DeckCard

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.deckapp.core.model.Card
import com.deckapp.core.model.CardAspectRatio
import com.deckapp.core.model.CardContentMode
import java.io.File

@Composable
fun CardItem(
    card: Card,
    aspectRatio: CardAspectRatio,
    showTitle: Boolean,
    backImagePath: String?,
    onClick: () -> Unit,
    onDiscard: () -> Unit,
    onReveal: () -> Unit,
    onRollTable: (Long) -> Unit,
    onRotate: (() -> Unit)? = null
) {
    val rotationAngle by animateFloatAsState(
        targetValue = card.currentRotation.toFloat(),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "card-rotation"
    )

    val mode = card.activeFace.contentMode
    val isMultiOption = mode == CardContentMode.FOUR_EDGE_CUES ||
            mode == CardContentMode.TOP_BOTTOM_SPLIT ||
            mode == CardContentMode.REVERSIBLE

    val orientationBadge = when (mode) {
        CardContentMode.FOUR_EDGE_CUES -> when (((card.currentRotation % 360) + 360) % 360) {
            0 -> "S"
            90 -> "O"
            180 -> "N"
            270 -> "E"
            else -> "S"
        }
        CardContentMode.TOP_BOTTOM_SPLIT,
        CardContentMode.REVERSIBLE -> if (card.currentRotation == 180 || card.isReversed) "2" else "1"
        else -> null
    }

    DeckCard(
        modifier = Modifier
            .width(100.dp)
            .aspectRatio(aspectRatio.ratio)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(Modifier.fillMaxSize()) {
            if (card.isRevealed) {
                // Frente de la carta con rotación activa y animación suave
                AsyncImage(
                    model = com.deckapp.core.ui.util.toCoilModel(card.activeFace.imagePath),
                    contentDescription = card.title,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .graphicsLayer {
                            rotationZ = rotationAngle
                        },
                    contentScale = ContentScale.Fit
                )

                // Chip / Badge de orientación / opción activa
                if (orientationBadge != null) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(4.dp),
                        shape = RoundedCornerShape(4.dp),
                        color = Color.Black.copy(alpha = 0.7f),
                        contentColor = Color.White
                    ) {
                        Text(
                            text = orientationBadge,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                            fontSize = 10.sp
                        )
                    }
                }

                // Botón de rotación rápida para cambiar de opción
                if (onRotate != null && isMultiOption) {
                    IconButton(
                        onClick = onRotate,
                        modifier = Modifier
                            .align(if (card.linkedTableId != null) Alignment.BottomEnd else Alignment.TopEnd)
                            .padding(3.dp)
                            .size(26.dp)
                            .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Rotar orientación ($orientationBadge)",
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
                
                if (showTitle) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.6f),
                        modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                    ) {
                        Text(
                            text = card.title,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(2.dp),
                            maxLines = 1
                        )
                    }
                }

                // Indicador de tabla vinculada
                card.linkedTableId?.let { tableId ->
                    IconButton(
                        onClick = { onRollTable(tableId) },
                        modifier = Modifier.align(Alignment.TopEnd).size(32.dp).padding(4.dp)
                    ) {
                        Icon(Icons.Default.Casino, null, tint = Color.White)
                    }
                }
            } else {
                // Dorso de la carta
                AsyncImage(
                    model = com.deckapp.core.ui.util.toCoilModel(backImagePath),
                    contentDescription = "Dorso",
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceContainerLow),
                    contentScale = ContentScale.Fit
                )
                
                IconButton(
                    onClick = onReveal,
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Icon(Icons.Default.Visibility, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }
        }
    }
}
