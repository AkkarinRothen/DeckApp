package com.deckapp.feature.importdeck.table.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp

@Composable
fun PdfPreviewView(
    bitmap: Bitmap?,
    pageIndex: Int,
    pageCount: Int,
    onPageChange: (Int) -> Unit,
    onConfirm: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Página PDF",
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                CircularProgressIndicator()
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { onPageChange(pageIndex - 1) },
                enabled = pageIndex > 0
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Página anterior")
            }
            
            Text("Página ${pageIndex + 1} de $pageCount")
            
            IconButton(
                onClick = { onPageChange(pageIndex + 1) },
                enabled = pageIndex < pageCount - 1
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, "Página siguiente")
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        Button(
            onClick = onConfirm,
            modifier = Modifier.fillMaxWidth(),
            enabled = bitmap != null
        ) {
            Text("Seleccionar esta página")
        }
    }
}
