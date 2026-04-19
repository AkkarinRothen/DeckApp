package com.deckapp.feature.reference.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.deckapp.core.model.ReferenceTable

@Composable
fun ReferenceTableGrid(
    table: ReferenceTable,
    modifier: Modifier = Modifier
) {
    var selectedCell by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    val horizontalScrollState = rememberScrollState()

    Column(modifier = modifier.fillMaxSize()) {
        // Usamos un Box con horizontal scroll para toda la tabla si hay muchas columnas
        Box(modifier = Modifier.horizontalScroll(horizontalScrollState)) {
            Column {
                // Header Row
                Row(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(vertical = 12.dp)
                ) {
                    table.columns.forEach { column ->
                        Text(
                            text = column.header,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier
                                .width(120.dp * column.widthWeight) // Ancho base de 120dp escalado
                                .padding(horizontal = 8.dp)
                        )
                    }
                }

                // Data Rows
                LazyColumn(modifier = Modifier.weight(1f)) {
                    itemsIndexed(table.rows) { rowIndex, row ->
                        Row(
                            modifier = Modifier
                                .background(
                                    if (rowIndex % 2 == 0) MaterialTheme.colorScheme.surface 
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            row.cells.forEachIndexed { colIndex, cellValue ->
                                Box(
                                    modifier = Modifier
                                        .width(120.dp * (table.columns.getOrNull(colIndex)?.widthWeight ?: 1f))
                                        .clickable { selectedCell = rowIndex to colIndex }
                                        .padding(horizontal = 8.dp)
                                ) {
                                    Text(
                                        text = cellValue,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (colIndex < row.cells.size - 1) {
                                    VerticalDivider(modifier = Modifier.height(24.dp))
                                }
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }

    selectedCell?.let { (rowIndex, colIndex) ->
        val header = table.columns.getOrNull(colIndex)?.header ?: ""
        val value = table.rows.getOrNull(rowIndex)?.cells?.getOrNull(colIndex) ?: ""
        
        CellExpandDialog(
            columnHeader = header,
            cellValue = value,
            onDismiss = { selectedCell = null }
        )
    }
}
