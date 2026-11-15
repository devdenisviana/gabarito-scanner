package com.devdenisviana.gabaritoscanner.ui.scan

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.devdenisviana.gabaritoscanner.scan.AlignResult
import com.devdenisviana.gabaritoscanner.scan.SheetAligner
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private sealed interface ScanUiState {
    data object Loading : ScanUiState
    data class Aligned(val sheet: Bitmap) : ScanUiState
    data class Error(val message: String) : ScanUiState
}

/** Runs the sheet aligner on the captured photo and shows the straightened result. */
@Composable
fun ScanResultScreen(
    file: File,
    onRetake: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var state by remember(file) { mutableStateOf<ScanUiState>(ScanUiState.Loading) }

    LaunchedEffect(file) {
        state = withContext(Dispatchers.Default) {
            val photo = BitmapFactory.decodeFile(file.absolutePath)
            if (photo == null) {
                ScanUiState.Error("Não foi possível abrir a foto.")
            } else {
                when (val result = SheetAligner.align(photo)) {
                    is AlignResult.Success -> ScanUiState.Aligned(result.sheet)
                    is AlignResult.Failure -> ScanUiState.Error(result.reason)
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (val current = state) {
            ScanUiState.Loading -> {
                CircularProgressIndicator()
                Text("Processando a folha...")
            }

            is ScanUiState.Aligned -> {
                Text("Folha alinhada: ${current.sheet.width} x ${current.sheet.height} px")
                Image(
                    bitmap = current.sheet.asImageBitmap(),
                    contentDescription = "Folha alinhada",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                )
            }

            is ScanUiState.Error -> {
                Text(current.message)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(onClick = onClose) { Text("Início") }
            Button(onClick = onRetake) { Text("Tirar outra") }
        }
    }
}