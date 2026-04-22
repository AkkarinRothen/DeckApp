package com.deckapp.core.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close

/**
 * Pestaña vertical estilo "Edge Tab" que se ancla al borde de la pantalla.
 */
@Composable
fun EdgeTab(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    alignment: Alignment,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isLeft = alignment == Alignment.CenterStart
    val shape = if (isLeft) {
        RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp)
    } else {
        RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)
    }

    Surface(
        onClick = onClick,
        color = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
        contentColor = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        shape = shape,
        tonalElevation = if (isActive) 8.dp else 2.dp,
        modifier = modifier
            .width(40.dp)
            .height(120.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.height(8.dp))
                
                // Resolver colores antes del block de canvas para evitar error de scope composable
                val primaryColor = MaterialTheme.colorScheme.onPrimaryContainer
                val variantColor = MaterialTheme.colorScheme.onSurfaceVariant
                
                Canvas(modifier = Modifier.size(20.dp, 60.dp)) {
                    rotate(-90f) {
                        drawContext.canvas.nativeCanvas.apply {
                            val paint = android.graphics.Paint().apply {
                                color = if (isActive) {
                                    android.graphics.Color.argb(255, (primaryColor.red * 255).toInt(), (primaryColor.green * 255).toInt(), (primaryColor.blue * 255).toInt())
                                } else {
                                    android.graphics.Color.argb((255 * 0.7f).toInt(), (variantColor.red * 255).toInt(), (variantColor.green * 255).toInt(), (variantColor.blue * 255).toInt())
                                }
                                textSize = 32f
                                textAlign = android.graphics.Paint.Align.CENTER
                                isFakeBoldText = isActive
                            }
                            drawText(label.uppercase(), 0f, 0f, paint)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Panel lateral deslizante con efecto Glassmorphism.
 */
@Composable
fun BentoSidebar(
    isVisible: Boolean,
    alignment: Alignment,
    title: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val isLeft = alignment == Alignment.CenterStart
    val enterTransition = if (isLeft) slideInHorizontally(initialOffsetX = { -it }) + fadeIn()
                         else slideInHorizontally(initialOffsetX = { it }) + fadeIn()
    
    val exitTransition = if (isLeft) slideOutHorizontally(targetOffsetX = { -it }) + fadeOut()
                        else slideOutHorizontally(targetOffsetX = { it }) + fadeOut()

    Box(modifier = Modifier.fillMaxSize()) {
        // Overlay para cerrar al tocar fuera
        if (isVisible) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
                    .clickable { onClose() }
            )
        }

        AnimatedVisibility(
            visible = isVisible,
            enter = enterTransition,
            exit = exitTransition,
            modifier = modifier
                .align(alignment)
                .fillMaxHeight()
                .width(320.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                tonalElevation = 12.dp,
                shadowElevation = 16.dp,
                shape = if (isLeft) RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp) 
                        else RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp),
                modifier = Modifier.fillMaxHeight()
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header del Sidebar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp, 24.dp, 16.dp, 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(onClick = onClose) {
                            Icon(Icons.Default.Close, null)
                        }
                    }

                    // Content Area
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        content()
                    }
                }
            }
        }
    }
}
