package com.deckapp.feature.settings.components

import com.deckapp.core.ui.components.DeckCard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.deckapp.core.model.TableAppearance
import com.deckapp.core.model.TableDensity
import com.deckapp.core.model.TextureIntensity
import com.deckapp.core.ui.components.PaperSurface
import com.deckapp.core.ui.components.TableReferenceHeader
import com.deckapp.core.ui.components.TableReferenceRow
import com.deckapp.core.ui.components.paperBackground
import com.deckapp.core.ui.components.ThemeChapterHeader
import com.deckapp.core.ui.components.categoryEmblem
import com.deckapp.core.ui.theme.LocalTableAppearance

@Composable
fun TableAppearanceCard(
    appearance: TableAppearance,
    onTextureSelected: (TextureIntensity) -> Unit,
    onDensitySelected: (TableDensity) -> Unit
) {
    DeckCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Textura y lectura", style = MaterialTheme.typography.titleSmall)
            Text("Ajusta el papel de las tablas y el espacio de sus filas. Los cambios se guardan automáticamente.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Textura", style = MaterialTheme.typography.labelLarge)
            AppearanceChoices(
                labels = listOf("Apagada", "Suave", "Marcada"),
                selectedIndex = TextureIntensity.entries.indexOf(appearance.texture),
                onSelect = { onTextureSelected(TextureIntensity.entries[it]) }
            )
            Text("Densidad", style = MaterialTheme.typography.labelLarge)
            AppearanceChoices(
                labels = listOf("Cómoda", "Compacta"),
                selectedIndex = TableDensity.entries.indexOf(appearance.density),
                onSelect = { onDensitySelected(TableDensity.entries[it]) }
            )
            Text("Vista previa · Encuentros del camino · 1d6", style = MaterialTheme.typography.labelMedium)
            CompositionLocalProvider(LocalTableAppearance provides appearance) {
                PaperSurface(shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), textureAlpha = 0.04f) {
                    Column {
                        Box(Modifier.fillMaxWidth().paperBackground().padding(12.dp)) {
                            ThemeChapterHeader("Encuentros del camino", emblem = categoryEmblem("Encuentros"))
                        }
                        TableReferenceHeader("Rango")
                        TableReferenceRow("1–2", "Un mercader ofrece provisiones.", alternate = false)
                        TableReferenceRow("3–4", "Huellas recientes conducen hacia el bosque.", alternate = true)
                        TableReferenceRow("5–6", "Una caravana pide escolta hasta el próximo pueblo.", alternate = false, selected = true)
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun AppearanceChoices(labels: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit) {
    // Wrap on narrow screens and with large fonts while preserving 48 dp targets.
    FlowRow(Modifier.fillMaxWidth().selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        labels.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Surface(shape = MaterialTheme.shapes.small,
                color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                border = BorderStroke(if (selected) 2.dp else 1.dp,
                    if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)) {
                Box(Modifier.heightIn(min = 48.dp)
                    .selectable(selected = selected, role = Role.RadioButton, onClick = { onSelect(index) })
                    .padding(horizontal = 16.dp, vertical = 12.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    Text(label, style = MaterialTheme.typography.bodyMedium,
                        color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}
