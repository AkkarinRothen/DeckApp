package com.deckapp.core.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.deckapp.core.domain.usecase.ChoirCharacterRules
import com.deckapp.core.model.*

/** Same typed fields in creation and the persistent library editor. Parent owns the draft. */
@Composable
fun ChoirCharacterFields(sheet: ChoirCharacter, onChange: (ChoirCharacter) -> Unit, creation: Boolean = false) {
    val a = sheet.attributes
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Atributos", style = MaterialTheme.typography.titleMedium)
        val labels = listOf("FUE", "CON", "DES", "INT", "VOL", "PRE")
        labels.zip(a.values()).forEachIndexed { index, (label, score) ->
            ChoirNumberField("$label · modificador ${ChoirCharacterRules.modifier(score)}", score,
                if (creation) 3 else 1, if (creation) 18 else 99) { value ->
                val changed = when (index) {
                    0 -> a.copy(strength = value); 1 -> a.copy(constitution = value)
                    2 -> a.copy(dexterity = value); 3 -> a.copy(intelligence = value)
                    4 -> a.copy(willpower = value); else -> a.copy(presence = value)
                }
                onChange(sheet.copy(attributes = changed))
            }
        }
        Text("Defensa ${ChoirCharacterRules.defense(sheet)} · Iniciativa ${ChoirCharacterRules.initiative(sheet)} · Carga ${a.strength}")
        Text("Vigor máximo utilizable: ${ChoirCharacterRules.vigorMaximum(sheet)} · Heridas: 4 mayores / 8 menores")
        ChoirNumberField("Armadura (bonificación de Defensa)", sheet.armor, 0, 99) { onChange(sheet.copy(armor = it)) }
        ChoirNumberField("Otros modificadores de Defensa", sheet.defenseOther, -99, 99) { onChange(sheet.copy(defenseOther = it)) }
        ChoirNumberField("Otros modificadores de Iniciativa", sheet.initiativeOther, -99, 99) { onChange(sheet.copy(initiativeOther = it)) }
        ChoirNumberField("Movimiento (casillas)", sheet.movement, 0, 99) { onChange(sheet.copy(movement = it)) }
        if (!creation) ChoirCharacterResources(sheet, onChange)
        ChoirNumberField("Raciones", sheet.rations) { onChange(sheet.copy(rations = it)) }
        ChoirNumberField("Suministros médicos", sheet.medicalSupplies) { onChange(sheet.copy(medicalSupplies = it)) }
    }
}

@Composable
private fun ChoirCharacterResources(s: ChoirCharacter, onChange: (ChoirCharacter) -> Unit) {
    Text("Estado de juego", style = MaterialTheme.typography.titleMedium)
    ChoirNumberField("Vigor actual", s.vigor, 0, ChoirCharacterRules.vigorMaximum(s)) { onChange(s.copy(vigor = it)) }
    ChoirNumberField("Humanidad", s.humanity, 0, 20) { onChange(s.copy(humanity = it)) }
    ChoirNumberField("Angustia", s.anguish) { onChange(s.copy(anguish = it)) }
    ChoirNumberField("Trauma", s.trauma) { onChange(s.copy(trauma = it)) }
    ChoirNumberField("Heridas mayores", s.major, 0, 4) { onChange(s.copy(major = it)) }
    ChoirNumberField("Heridas menores", s.minor, 0, 8) { onChange(s.copy(minor = it)) }
    ChoirNumberField("Nivel", s.level, 1) { onChange(s.copy(level = it)) }
    ChoirNumberField("XP", s.xp) { onChange(s.copy(xp = it)) }
    ChoirNumberField("Rumores", s.rumors) { onChange(s.copy(rumors = it)) }
    if (ChoirCharacterRules.isDead(s)) Text("Registro de heridas completo: muerte según las reglas.", color = MaterialTheme.colorScheme.error)
}

@Composable
fun ChoirNumberField(label: String, value: Int, min: Int = 0, max: Int = Int.MAX_VALUE, onChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("$label: $value", modifier = Modifier.weight(1f).padding(top = 12.dp))
        OutlinedButton(onClick = { onChange(value - 1) }, enabled = value > min,
            modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)) { Text("−") }
        OutlinedButton(onClick = { onChange(value + 1) }, enabled = value < max,
            modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)) { Text("+") }
    }
}

@Composable
fun ChoirCharacterDetails(s: ChoirCharacter, onChange: (ChoirCharacter) -> Unit, backgrounds: Boolean = true) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (backgrounds) {
            ChoirTextField("Ocupación y problema", s.occupation) { onChange(s.copy(occupation = it)) }
            ChoirTextField("Pecado", s.sin) { onChange(s.copy(sin = it)) }
            ChoirTextField("Fragmento", s.fragment) { onChange(s.copy(fragment = it)) }
            ChoirTextField("Susurro", s.whisper) { onChange(s.copy(whisper = it)) }
        }
        Text("Elige 4 habilidades base; revisa las adicionales por atributos 16+ y ocupación. Puedes sustituir una por un Don sin Estigma.")
        ChoirTextField("Habilidades elegidas", s.skills) { onChange(s.copy(skills = it)) }
        ChoirTextField("Dotes elegidas", s.feats) { onChange(s.copy(feats = it)) }
        ChoirTextField("Defectos", s.drawbacks) { onChange(s.copy(drawbacks = it)) }
        ChoirTextField("Competencia de arma", s.weaponProficiency) { onChange(s.copy(weaponProficiency = it)) }
        ChoirTextField("Competencia de armadura", s.armorProficiency) { onChange(s.copy(armorProficiency = it)) }
        ChoirTextField("Dones", s.gifts) { onChange(s.copy(gifts = it)) }
        ChoirTextField("Estigmas", s.stigmata) { onChange(s.copy(stigmata = it)) }
        ChoirTextField("Equipo y armas", s.equipment) { onChange(s.copy(equipment = it)) }
        ChoirTextField("Notas personales", s.personalNotes) { onChange(s.copy(personalNotes = it)) }
    }
}

@Composable
fun ChoirTextField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = onChange, label = { Text(label) }, modifier = Modifier.fillMaxWidth())
}
