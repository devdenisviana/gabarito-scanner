package com.devdenisviana.gabaritoscanner.ui.exam

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.devdenisviana.gabaritoscanner.domain.sheet.SheetLayout
import com.devdenisviana.gabaritoscanner.domain.sheet.SheetProfile
import com.devdenisviana.gabaritoscanner.grading.KeyDraft
import com.devdenisviana.gabaritoscanner.scan.AlignResult
import com.devdenisviana.gabaritoscanner.scan.BubbleReader
import com.devdenisviana.gabaritoscanner.scan.ScanOverlay
import com.devdenisviana.gabaritoscanner.scan.SheetAligner
import com.devdenisviana.gabaritoscanner.scan.toGrayImage
import com.devdenisviana.gabaritoscanner.ui.components.AppHeader
import com.devdenisviana.gabaritoscanner.ui.components.Staggered
import com.devdenisviana.gabaritoscanner.ui.components.TabbedContent
import com.devdenisviana.gabaritoscanner.ui.components.warnColor
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

    Column(modifier = modifier.fillMaxSize()) {
        AppHeader(
            title = "Folha do gabarito",
            subtitle = "Confira as respostas lidas",
            onBack = onCancel,
        )

        when (val current = state) {
            KeyScanState.Loading -> {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Processando a folha...")
                }
            }

            is KeyScanState.Done -> {
                TabbedContent(
                    titles = listOf("Respostas", "Folha"),
                    modifier = Modifier.weight(1f),
                ) { tab ->
                    if (tab == 0) {
                        KeyAnswersTab(current.text)
                    } else {
                        val image = remember(current.overlay) { current.overlay.asImageBitmap() }
                        Image(
                            bitmap = image,
                            contentDescription = "Folha lida",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                        )
                    }
                }
            }

            is KeyScanState.Error -> {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = current.message,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            (state as? KeyScanState.Done)?.let { done ->
                Button(onClick = { onUse(done.text) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Usar estas respostas")
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onRetake, modifier = Modifier.weight(1f)) {
                    Text("Tirar outra")
                }
                OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                    Text("Cancelar")
                }
            }
        }
    }
}

@Composable
private fun KeyAnswersTab(text: String, modifier: Modifier = Modifier) {
    val letters = text.filter { !it.isWhitespace() }.toList()
    val unsure = letters.count { it == KeyDraft.UNSURE }
    val amber = warnColor()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (unsure > 0) {
            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "$unsure questão(ões) em branco ou com marcação dupla, mostradas como ?. " +
                                "Você poderá corrigir no formulário.",
                        modifier = Modifier.padding(16.dp),
                        color = amber,
                    )
                }
            }
        }
        itemsIndexed(letters) { index, letter ->
            val isUnsure = letter == KeyDraft.UNSURE
            val color = if (isUnsure) amber else MaterialTheme.colorScheme.primary
            Staggered(index) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(color.copy(alpha = 0.10f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Questão ${index + 1}",
                        modifier = Modifier.weight(1f),
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = letter.toString(),
                        color = color,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}