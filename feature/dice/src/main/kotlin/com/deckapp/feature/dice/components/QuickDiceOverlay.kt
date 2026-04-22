package com.deckapp.feature.dice.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.random.Random

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import android.content.ClipData

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun QuickDiceOverlay(
    modifier: Modifier = Modifier,
    onOpenFullRoller: () -> Unit,
    externalRollTrigger: Boolean = false, // Permite disparar desde fuera (ej: Shake)
    onExternalRollHandled: () -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    
    var isExpanded by remember { mutableStateOf(false) }
    var lastResult by remember { mutableStateOf<Pair<Int, Int>?>(null) } // Result to Sides
    var resultVisible by remember { mutableStateOf(false) }

    fun rollDie(sides: Int) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        val result = Random.nextInt(1, sides + 1)
        lastResult = result to sides
        resultVisible = true
        isExpanded = false
        
        scope.launch {
            delay(5000)
            resultVisible = false
        }
    }

    // Efecto para disparar desde sensor
    LaunchedEffect(externalRollTrigger) {
        if (externalRollTrigger) {
            rollDie(20) // d20 por defecto para el shake en manuales
            onExternalRollHandled()
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomEnd
    ) {
        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.padding(16.dp).padding(bottom = 16.dp)
        ) {
            // Burbuja de resultado (AHORA ARRASTRABLE)
            AnimatedVisibility(
                visible = resultVisible,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                lastResult?.let { (res, sides) ->
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        shape = RoundedCornerShape(16.dp),
                        tonalElevation = 8.dp,
                        modifier = Modifier
                            .padding(bottom = 8.dp)
                            .dragAndDropSource {
                                detectTapGestures(
                                    onLongPress = {
                                        startTransfer(
                                            DragAndDropTransferData(
                                                clipData = ClipData.newPlainText(
                                                    "dice_result",
                                                    res.toString()
                                                )
                                            )
                                        )
                                    }
                                )
                            }
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = res.toString(),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "d$sides",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }

            // Menú expandible de dados
            AnimatedVisibility(
                visible = isExpanded,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut()
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(24.dp),
                    tonalElevation = 4.dp,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuickDiceButton("d20") { rollDie(20) }
                        QuickDiceButton("d100") { rollDie(100) }
                        QuickDiceButton("d6") { rollDie(6) }
                        IconButton(
                            onClick = { onOpenFullRoller(); isExpanded = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
                        ) {
                            Icon(Icons.Default.Casino, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    }
                }
            }

            // Botón principal
            FloatingActionButton(
                onClick = { isExpanded = !isExpanded },
                containerColor = if (isExpanded) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primary,
                contentColor = if (isExpanded) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.Close else Icons.Default.Casino,
                    contentDescription = "Dados rápidos"
                )
            }
        }
    }
}

@Composable
private fun QuickDiceButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}
