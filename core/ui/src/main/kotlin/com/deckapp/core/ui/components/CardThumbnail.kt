package com.deckapp.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.deckapp.core.model.Card
import com.deckapp.core.model.CardAspectRatio
import java.io.File

/**
 * Miniatura de carta para usar en grillas y en la mano de sesión.
 *
 * - [aspectRatio]: controla el ancho relativo al alto (default STANDARD = naipes 2.5"×3.5")
 * - [showFaceDots]: muestra indicadores de cara activa cuando la carta tiene >1 cara
 * - Imagen con [ContentScale.Fit] + fondo [surfaceVariant] como letterbox (sin recorte)
 */
@Composable
fun CardThumbnail(
    card: Card,
    modifier: Modifier = Modifier,
    height: Dp = 120.dp,
    showTitle: Boolean = true,
    aspectRatio: CardAspectRatio = CardAspectRatio.STANDARD,
    showFaceDots: Boolean = true
) {
    val imagePath = card.activeFace.imagePath
    val cardWidth = height * aspectRatio.ratio

    Column(
        modifier = modifier
            .width(cardWidth)
            .clip(RoundedCornerShape(8.dp)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── Imagen con letterbox ──────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            if (imagePath != null) {
                AsyncImage(
                    model = File(imagePath),
                    contentDescription = card.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = card.title.take(2).uppercase(),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ── Dots de caras activas (solo si hay >1 cara) ──────────
            if (showFaceDots && card.faces.size > 1) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.35f))
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        card.faces.forEachIndexed { index, _ ->
                            val isActive = index == card.currentFaceIndex
                            Box(
                                modifier = Modifier
                                    .size(if (isActive) 6.dp else 5.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isActive) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                    )
                            )
                        }
                    }
                }
            }
        }

        // ── Título ────────────────────────────────────────────────────
        if (showTitle) {
            Text(
                text = card.title,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }
    }
}
