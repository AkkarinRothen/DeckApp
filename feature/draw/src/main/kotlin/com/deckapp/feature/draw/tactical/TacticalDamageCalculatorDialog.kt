package com.deckapp.feature.draw.tactical

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.deckapp.core.model.TacticalToken
import com.deckapp.core.model.TokenFaction
import kotlin.random.Random

@Composable
fun TacticalDamageCalculatorDialog(
    token: TacticalToken,
    onApplyDamage: (amount: Int) -> Unit,
    onApplyHealing: (amount: Int) -> Unit,
    onApplyTempHp: (amount: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var inputExpression by remember { mutableStateOf("") }
    var lastRolledFormula by remember { mutableStateOf<String?>(null) }

    // Función auxiliar para parsear y evaluar dados sencillos (ej: 2d6+3)
    fun rollFormula(formula: String): Int? {
        val cleaned = formula.replace(" ", "").lowercase()
        val regex = Regex("""^(\d*)d(\d+)([+-]\d+)?$""")
        val match = regex.matchEntire(cleaned) ?: return null

        val count = match.groupValues[1].toIntOrNull() ?: 1
        val sides = match.groupValues[2].toIntOrNull() ?: return null
        val mod = match.groupValues[3].toIntOrNull() ?: 0

        var sum = 0
        val clampedCount = count.coerceIn(1, 100)
        for (i in 0 until clampedCount) {
            sum += Random.nextInt(1, sides + 1)
        }
        return (sum + mod).coerceAtLeast(0)
    }

    val parsedNumericValue: Int? = inputExpression.toIntOrNull() ?: rollFormula(inputExpression)
    val displayValue = parsedNumericValue ?: 0

    // Previews de vida resultante
    val damageToTemp = minOf(token.tempHp, displayValue)
    val remainingDamage = (displayValue - damageToTemp).coerceAtLeast(0)
    val previewHpAfterDamage = (token.currentHp - remainingDamage).coerceAtLeast(0)
    val previewTempAfterDamage = (token.tempHp - damageToTemp).coerceAtLeast(0)

    val previewHpAfterHeal = (token.currentHp + displayValue).coerceAtMost(token.maxHp)
    val previewTempHp = maxOf(token.tempHp, displayValue)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 8.dp,
            shadowElevation = 16.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Cabecera: Token y datos actuales
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val factionColor = when (token.faction) {
                            TokenFaction.PLAYER -> Color(0xFF2196F3)
                            TokenFaction.ALLY -> Color(0xFF4CAF50)
                            TokenFaction.ENEMY -> Color(0xFFF44336)
                            TokenFaction.NEUTRAL -> Color(0xFFFFC107)
                        }
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(factionColor)
                        )
                        Column {
                            Text(
                                text = token.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "CA: ${token.armorClass}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Text(
                                    text = "Vel: ${token.speedFt}ft",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar")
                    }
                }

                // Estado actual de HP y Temp HP
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Puntos de Golpe:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "${token.currentHp} / ${token.maxHp} HP",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                if (token.tempHp > 0) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF00E5FF).copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = "+${token.tempHp} Temp",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF00E5FF),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Barra de progreso combinada
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            val hpRatio = if (token.maxHp > 0) (token.currentHp.toFloat() / token.maxHp).coerceIn(0f, 1f) else 1f
                            val hpColor = when {
                                hpRatio > 0.5f -> Color(0xFF4CAF50)
                                hpRatio > 0.2f -> Color(0xFFFFC107)
                                else -> Color(0xFFF44336)
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(hpRatio)
                                    .background(hpColor)
                            )
                        }
                    }
                }

                // Visor de Entrada Numérica / Expresión
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Valor a aplicar:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = if (inputExpression.isEmpty()) "0" else inputExpression,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (inputExpression.isEmpty()) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary
                            )
                        }

                        if (lastRolledFormula != null) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "🎲 $lastRolledFormula = $displayValue",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                // Botones rápidos de dados (+1d6, +1d8, etc.)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("1d4", "1d6", "1d8", "1d10", "1d12", "1d20").forEach { dice ->
                        FilledTonalButton(
                            onClick = {
                                val rolled = rollFormula(dice) ?: 0
                                inputExpression = rolled.toString()
                                lastRolledFormula = dice
                            },
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                        ) {
                            Text(dice, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Atajos de montos fijos rápidos (+5, +10, +20)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(5, 10, 20).forEach { delta ->
                        OutlinedButton(
                            onClick = {
                                val current = inputExpression.toIntOrNull() ?: 0
                                inputExpression = (current + delta).toString()
                                lastRolledFormula = null
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp)
                        ) {
                            Text("+$delta", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        }
                    }
                }

                // Teclado Numérico Táctico (4 filas x 3 columnas)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val keyRows = listOf(
                        listOf("7", "8", "9"),
                        listOf("4", "5", "6"),
                        listOf("1", "2", "3"),
                        listOf("C", "0", "⌫")
                    )

                    keyRows.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            row.forEach { key ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = when (key) {
                                        "C" -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
                                        "⌫" -> MaterialTheme.colorScheme.surfaceVariant
                                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    },
                                    onClick = {
                                        when (key) {
                                            "C" -> {
                                                inputExpression = ""
                                                lastRolledFormula = null
                                            }
                                            "⌫" -> {
                                                if (inputExpression.isNotEmpty()) {
                                                    inputExpression = inputExpression.dropLast(1)
                                                }
                                                lastRolledFormula = null
                                            }
                                            else -> {
                                                inputExpression += key
                                                lastRolledFormula = null
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        if (key == "⌫") {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.Backspace,
                                                contentDescription = "Borrar",
                                                modifier = Modifier.size(18.dp)
                                            )
                                        } else {
                                            Text(
                                                text = key,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

                // Botones Principales de Acción Táctica
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Botón Daño
                    Button(
                        onClick = {
                            if (displayValue > 0) {
                                onApplyDamage(displayValue)
                                onDismiss()
                            }
                        },
                        enabled = displayValue > 0,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                    ) {
                        Icon(Icons.Default.FlashOn, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (displayValue > 0) "Aplicar Daño -$displayValue (HP: $previewHpAfterDamage)" else "Aplicar Daño",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Botón Curar
                        Button(
                            onClick = {
                                if (displayValue > 0) {
                                    onApplyHealing(displayValue)
                                    onDismiss()
                                }
                            },
                            enabled = displayValue > 0,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2E7D32),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                        ) {
                            Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (displayValue > 0) "Curar +$displayValue" else "Curar",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        // Botón Temp HP
                        Button(
                            onClick = {
                                if (displayValue > 0) {
                                    onApplyTempHp(displayValue)
                                    onDismiss()
                                }
                            },
                            enabled = displayValue > 0,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF00838F),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                        ) {
                            Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (displayValue > 0) "+$displayValue Temp" else "PV Temp",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
