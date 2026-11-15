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
import com.devdenisviana.gabaritoscanner.domain.sheet.SheetLayout
import com.devdenisviana.gabaritoscanner.domain.sheet.SheetProfile
import com.devdenisviana.gabaritoscanner.grading.AnswerKey
import com.devdenisviana.gabaritoscanner.grading.GradeResult
import com.devdenisviana.gabaritoscanner.grading.Grader
import com.devdenisviana.gabaritoscanner.scan.AlignResult
import com.devdenisviana.gabaritoscanner.scan.Answer
import com.devdenisviana.gabaritoscanner.scan.BubbleReader
import com.devdenisviana.gabaritoscanner.scan.QuestionReading
import com.devdenisviana.gabaritoscanner.scan.ScanOverlay
import com.devdenisviana.gabaritoscanner.scan.SheetAligner
import com.devdenisviana.gabaritoscanner.scan.toGrayImage
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** TEMPORARY answer key used until exams can be registered (next step). */
private const val DEMO_ANSWER_KEY = "ABCDEABCBBBABDBAEDBD"

private sealed interface ScanUiState {
    data object Loading : ScanUiState
    data class Done(
        val overlay: Bitmap,
        val readings: List<QuestionReading>,
        val grade: GradeResult,
    ) : ScanUiState

    data class Error(val message: String) : ScanUiState
}

/** Aligns the captured photo, reads the bubbles, grades them and shows the result. */
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
                    is AlignResult.Failure -> ScanUiState.Error(result.reason)
                    is AlignResult.Success -> {
                        val profile = SheetProfile.DEFAULT
                        val layout = SheetLayout(profile)
                        val readings = BubbleReader(layout).read(result.sheet.toGrayImage())
                        val overlay = ScanOverlay.draw(result.sheet, layout, readings)
                        val key = AnswerKey.parse(DEMO_ANSWER_KEY, profile)
                        ScanUiState.Done(overlay, readings, Grader.grade(key, readings))
                    }
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

            is ScanUiState.Done -> {
                val grade = current.grade

                Text("Acertos: ${grade.correct}   Erros: ${grade.errors}   Nota: ${"%.1f".format(grade.score10)}")
                Text("(erros: ${grade.wrong} erradas, ${grade.blank} em branco, ${grade.multiple} duplas)")
                Image(
                    bitmap = current.overlay.asImageBitmap(),
                    contentDescription = "Folha lida",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                )
                Text(answersLine(current.readings))
                Text("– = em branco   * = marcação dupla")
            }

            is ScanUiState.Error -> Text(current.message)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(onClick = onClose) { Text("Início") }
            Button(onClick = onRetake) { Text("Tirar outra") }
        }
    }
}

private fun answersLine(readings: List<QuestionReading>): String =
    readings.joinToString("  ") { reading ->
        val symbol = when (val answer = reading.answer) {
            is Answer.Marked -> answer.option.toString()
            Answer.Blank -> "–"
            is Answer.Multiple -> "*"
        }
        "${reading.question}:$symbol"
    }