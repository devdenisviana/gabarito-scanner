package com.devdenisviana.gabaritoscanner.ui.exam

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
import androidx.compose.material3.OutlinedButton
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
import com.devdenisviana.gabaritoscanner.domain.sheet.SheetLayout
import com.devdenisviana.gabaritoscanner.domain.sheet.SheetProfile
import com.devdenisviana.gabaritoscanner.grading.KeyDraft
import com.devdenisviana.gabaritoscanner.scan.AlignResult
import com.devdenisviana.gabaritoscanner.scan.BubbleReader
import com.devdenisviana.gabaritoscanner.scan.ScanOverlay
import com.devdenisviana.gabaritoscanner.scan.SheetAligner
import com.devdenisviana.gabaritoscanner.scan.toGrayImage
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private sealed interface KeyScanState {
    data object Loading : KeyScanState
    data class Done(val overlay: Bitmap, val text: String) : KeyScanState
    data class Error(val message: String) : KeyScanState
}

/**
 * Reads a filled answer-key sheet from [file] using [profile] and offers the
 * detected answers through [onUse]. Blank or doubled marks come back as '?'.
 */
@Composable
fun KeySheetScreen(
    file: File,
    profile: SheetProfile,
    onUse: (answers: String) -> Unit,
    onRetake: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var state by remember(file) { mutableStateOf<KeyScanState>(KeyScanState.Loading) }

    LaunchedEffect(file, profile) {
        state = withContext(Dispatchers.Default) {
            val photo = BitmapFactory.decodeFile(file.absolutePath)
            if (photo == null) {
                KeyScanState.Error("Não foi possível abrir a foto.")
            } else {
                when (val result = SheetAligner.align(photo)) {
                    is AlignResult.Failure -> KeyScanState.Error(result.reason)
                    is AlignResult.Success -> {
                        val layout = SheetLayout(profile)
                        val readings = BubbleReader(layout).read(result.sheet.toGrayImage())
                        KeyScanState.Done(
                            overlay = ScanOverlay.draw(result.sheet, layout, readings),
                            text = KeyDraft.fromReadings(readings),
                        )
                    }
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (val current = state) {
            KeyScanState.Loading -> {
                CircularProgressIndicator()
                Text("Processando a folha...")
            }

            is KeyScanState.Done -> {
                val unsure = current.text.count { it == KeyDraft.UNSURE }

                Text("Folha do gabarito lida")
                Image(
                    bitmap = current.overlay.asImageBitmap(),
                    contentDescription = "Folha lida",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                )
                Text(current.text)
                if (unsure > 0) {
                    Text("$unsure questão(ões) em branco ou com marcação dupla, mostradas como ?. Você poderá corrigir no formulário.")
                }
                Button(onClick = { onUse(current.text) }) { Text("Usar estas respostas") }
            }

            is KeyScanState.Error -> Text(current.message)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedButton(onClick = onCancel) { Text("Cancelar") }
            OutlinedButton(onClick = onRetake) { Text("Tirar outra") }
        }
    }
}