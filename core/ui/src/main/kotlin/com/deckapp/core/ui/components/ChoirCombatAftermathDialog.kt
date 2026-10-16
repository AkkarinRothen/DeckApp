package com.deckapp.core.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deckapp.core.domain.usecase.ChoirAftermathRequest
import com.deckapp.core.domain.usecase.ChoirCharacterRules
import com.deckapp.core.model.ChoirCharacter
import kotlin.random.Random

/**
 * Diálogo interactivo para el Cierre de Combate (Aftermath pp. 60-61).
 * Permite resolver tiradas de Inteligencia (médico) o Constitución,
 * supuración de heridas, incremento de Trauma y adjudicación de XP y Botín.
 */
@Composable
fun ChoirCombatAftermathDialog(
    characterName: String,
    character: ChoirCharacter,
    onApplyAftermath: (ChoirAftermathRequest) -> Unit,
    onDismiss: () -> Unit
) {
    val isWounded = character.major > 0 || character.minor > 0
    var spendSupply by remember { mutableStateOf(character.medicalSupplies > 0 && isWounded) }

    val intMod = remember(character.attributes.intelligence) {
        ChoirCharacterRules.modifier(character.attributes.intelligence)
    }
    val conMod = remember(character.attributes.constitution) {
        ChoirCharacterRules.modifier(character.attributes.constitution)
    }

    var intRollText by remember { mutableStateOf("") }
    var conRollText by remember { mutableStateOf("") }
    var xpText by remember { mutableStateOf("1") }
    var lootNotes by remember { mutableStateOf("") }

    val targetNumber = 10

    val intRoll = intRollText.toIntOrNull()
    val conRoll = conRollText.toIntOrNull()

    val treatmentSucceeded: Boolean = when {
        !isWounded -> true
        spendSupply && intRoll != null && intRoll >= targetNumber -> true
        conRoll != null && conRoll >= targetNumber -> true
        else -> false
    }

    val festered = if (!treatmentSucceeded && isWounded) character.minor else 0
    val totalMajor = if (isWounded) (character.major + festered).coerceAtMost(4) else 0
    val traumaGained = totalMajor

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Healing,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text("Cierre de Combate · Aftermath", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(characterName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Estado de Heridas del Inquebrantable
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Mayores", style = MaterialTheme.typography.labelSmall)
                            Text(
                                "${character.major}/4",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (character.major > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Menores", style = MaterialTheme.typography.labelSmall)
                            Text(
                                "${character.minor}/8",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (character.minor > 0) Color(0xFFFF9800) else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Suministros", style = MaterialTheme.typography.labelSmall)
                            Text(
                                "${character.medicalSupplies}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Trauma", style = MaterialTheme.typography.labelSmall)
                            Text(
                                "${character.trauma}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (!isWounded) {
                    Surface(
                        color = Color(0xFF2E7D32).copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, Color(0xFF4CAF50)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "El Inquebrantable no recibió heridas. No requiere tratamiento médico ni sufre supuración.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                } else {
                    // Sección: Tratamiento Médico
                    Text(
                        "Tratamiento Médico (pp. 60-61)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Toggle suministro médico
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("Gastar 1 Suministro Médico", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Text(
                                    if (character.medicalSupplies > 0) "Permite chequeo de INT (NO $targetNumber)" else "Sin suministros médicos disponibles",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = spendSupply,
                                onCheckedChange = { spendSupply = it },
                                enabled = character.medicalSupplies > 0
                            )
                        }
                    }

                    // Chequeo de INT
                    AnimatedVisibility(visible = spendSupply) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Chequeo INT (mod ${if (intMod >= 0) "+$intMod" else "$intMod"})", style = MaterialTheme.typography.bodyMedium)
                                    Button(
                                        onClick = {
                                            val roll = Random.nextInt(1, 21) + intMod
                                            intRollText = roll.toString()
                                        },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Icon(Icons.Default.Casino, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("d20+INT", fontSize = 12.sp)
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = intRollText,
                                        onValueChange = { if (it.all { c -> c.isDigit() || c == '-' }) intRollText = it },
                                        label = { Text("Total INT") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (intRoll != null) {
                                        Surface(
                                            color = if (intRoll >= targetNumber) Color(0xFF2E7D32) else Color(0xFFC62828),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = if (intRoll >= targetNumber) "ÉXITO" else "FALLO",
                                                color = Color.White,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Chequeo de CON si no hay suministro o si falló INT
                    AnimatedVisibility(visible = !spendSupply || (intRoll != null && intRoll < targetNumber)) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Chequeo CON (mod ${if (conMod >= 0) "+$conMod" else "$conMod"})", style = MaterialTheme.typography.bodyMedium)
                                    Button(
                                        onClick = {
                                            val roll = Random.nextInt(1, 21) + conMod
                                            conRollText = roll.toString()
                                        },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Icon(Icons.Default.Casino, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("d20+CON", fontSize = 12.sp)
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = conRollText,
                                        onValueChange = { if (it.all { c -> c.isDigit() || c == '-' }) conRollText = it },
                                        label = { Text("Total CON") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (conRoll != null) {
                                        Surface(
                                            color = if (conRoll >= targetNumber) Color(0xFF2E7D32) else Color(0xFFC62828),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = if (conRoll >= targetNumber) "ÉXITO" else "FALLO",
                                                color = Color.White,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Pronóstico de Consecuencias
                    Surface(
                        color = if (treatmentSucceeded) Color(0xFF1B5E20).copy(alpha = 0.2f) else Color(0xFFB71C1C).copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, if (treatmentSucceeded) Color(0xFF4CAF50) else Color(0xFFEF5350)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    if (treatmentSucceeded) Icons.Default.CheckCircle else Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = if (treatmentSucceeded) Color(0xFF4CAF50) else Color(0xFFEF5350),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    if (treatmentSucceeded) "Consecuencia: Tratamiento Exitoso" else "Consecuencia: Tratamiento Fallido",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (treatmentSucceeded) Color(0xFF81C784) else Color(0xFFE57373)
                                )
                            }
                            if (treatmentSucceeded) {
                                Text("• Las heridas menores son estabilizadas y no supuran.", style = MaterialTheme.typography.bodySmall)
                                if (character.major > 0) {
                                    Text("• Se adquiere +${character.major} Trauma por las heridas mayores.", style = MaterialTheme.typography.bodySmall)
                                }
                            } else {
                                Text("• $festered heridas menores supuran a mayores.", style = MaterialTheme.typography.bodySmall)
                                Text("• Se adquiere +$traumaGained Trauma (Total Trauma: ${character.trauma + traumaGained}).", style = MaterialTheme.typography.bodySmall)
                            }
                            Text("• Todas las heridas de combate serán limpiadas.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                // Sección: Recompensas
                Text(
                    "Recompensas & Botín",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = xpText,
                    onValueChange = { if (it.all { c -> c.isDigit() }) xpText = it },
                    label = { Text("Puntos de Experiencia (XP)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = lootNotes,
                    onValueChange = { lootNotes = it },
                    label = { Text("Botín / Hallazgos") },
                    placeholder = { Text("Ej: 12 chelines, amuleto de hueso") },
                    singleLine = false,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val request = ChoirAftermathRequest(
                        character = character,
                        spendMedicalSupply = spendSupply,
                        intRollResult = intRoll,
                        conRollResult = conRoll,
                        targetNumber = targetNumber,
                        xpGained = xpText.toIntOrNull() ?: 0,
                        lootNotes = lootNotes
                    )
                    onApplyAftermath(request)
                }
            ) {
                Text("APLICAR Y GUARDAR")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCELAR")
            }
        }
    )
}
