package com.deckapp.feature.draw.tactical

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdsClick
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deckapp.core.model.TacticalToken

@Composable
fun TacticalAttackDialog(
    attacker: TacticalToken,
    target: TacticalToken,
    distanceFt: Int,
    onDismiss: () -> Unit,
    onExecuteAttack: (attackName: String, attackBonus: Int, damageFormula: String) -> Unit
) {
    var attackName by remember { mutableStateOf("Ataque Principal") }
    var attackBonusText by remember { mutableStateOf("4") }
    var damageFormula by remember { mutableStateOf("1d8+2") }

    val presetAttacks = listOf(
        Triple("Espada Larga", 5, "1d8+3"),
        Triple("Daga", 4, "1d4+2"),
        Triple("Arco Corto", 5, "1d6+3"),
        Triple("Descarga Arcana", 5, "1d10"),
        Triple("Mandoble", 5, "2d6+3")
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AdsClick,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Text("Resolver Ataque Táctico")
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Tarjeta de Enfrentamiento: Atacante vs Defensor
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Atacante: ${attacker.name}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Distancia: ${distanceFt} ft",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Objetivo: ${target.name}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Text("CA ${target.armorClass}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Presets rápidos de armas
                Text(
                    text = "Ataques Rápidos:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presetAttacks.take(3).forEach { (name, bonus, formula) ->
                        OutlinedButton(
                            onClick = {
                                attackName = name
                                attackBonusText = bonus.toString()
                                damageFormula = formula
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text(name, fontSize = 11.sp)
                        }
                    }
                }

                // Campos editables
                OutlinedTextField(
                    value = attackName,
                    onValueChange = { attackName = it },
                    label = { Text("Nombre del Ataque") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = attackBonusText,
                        onValueChange = { attackBonusText = it.filter { char -> char.isDigit() || char == '-' || char == '+' } },
                        label = { Text("Bono de Ataque (+)") },
                        placeholder = { Text("+4") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = damageFormula,
                        onValueChange = { damageFormula = it },
                        label = { Text("Fórmula de Daño") },
                        placeholder = { Text("1d8+2") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val bonus = attackBonusText.replace("+", "").toIntOrNull() ?: 0
                    onExecuteAttack(attackName, bonus, damageFormula)
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("¡Lanzar Ataque!", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
