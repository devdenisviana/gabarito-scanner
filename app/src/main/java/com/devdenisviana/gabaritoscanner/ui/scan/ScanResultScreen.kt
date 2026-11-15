package com.devdenisviana.gabaritoscanner.ui.scan

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.devdenisviana.gabaritoscanner.data.ExamEntity
import com.devdenisviana.gabaritoscanner.data.toAnswerKey
import com.devdenisviana.gabaritoscanner.data.toSheetProfile
import com.devdenisviana.gabaritoscanner.domain.sheet.SheetLayout
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

private sealed interface ScanUiState {
    data object Loading : ScanUiState
    data class Done(
        val overlay: Bitmap,
        val readings: List<QuestionReading>,
        val grade: GradeResult,
    ) : ScanUiState

    data class Error(val message: String) : ScanUiState
}

/**
 * Aligns the captured photo, reads the bubbles, grades them against [exam]
 * and lets the user save the result with the student's name.
 */
@Composable
fun ScanResultScreen(
    file: File,
    exam: ExamEntity,
    onSave: (
        studentName: String,
        studentCode: String,
        readings: List<QuestionReading>,
        grade: GradeResult,
    ) -> Unit,
    onRetake: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var state by remember(file) { mutableStateOf<ScanUiState>(ScanUiState.Loading) }
    var studentName by remember(file) { mutableStateOf("") }
    var studentCode by remember(file) { mutableStateOf("") }
    var saved by remember(file) { mutableStateOf(false) }

    LaunchedEffect(file, exam.id) {
        state = withContext(Dispatchers.Default) {
            val photo = BitmapFactory.decodeFile(file.absolutePath)
            if (photo == null) {
                ScanUiState.Error("Não foi possível abrir a foto.")
            } else {
                when (val result = SheetAligner.align(photo)) {
                    is AlignResult.Failure -> ScanUiState.Error(result.reason)
                    is AlignResult.Success -> {
                        val layout = SheetLayout(exam.toSheetProfile())
                        val readings = BubbleReader(layout).read(result.sheet.toGrayImage())
                        val overlay = ScanOverlay.draw(result.sheet, layout, readings)
                        ScanUiState.Done(overlay, readings, Grader.grade(exam.toAnswerKey(), readings))
                    }
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (val current = state) {
            ScanUiState.Loading -> {
                CircularProgressIndicator()
                Text("Processando a folha...")
            }

            is ScanUiState.Done -> {
                val grade = current.grade

                Text("Prova: ${exam.name}")
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

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = studentName,
                        onValueChange = { studentName = it },
                        label = { Text("Aluno") },
                        singleLine = true,
                        enabled = !saved,
                        modifier = Modifier.weight(2f),
                    )
                    OutlinedTextField(
                        value = studentCode,
                        onValueChange = { studentCode = it },
                        label = { Text("Matrícula") },
                        singleLine = true,
                        enabled = !saved,
                        modifier = Modifier.weight(1f),
                    )
                }
                Button(
                    onClick = {
                        onSave(studentName, studentCode, current.readings, current.grade)
                        saved = true
                    },
                    enabled = !saved && studentName.isNotBlank(),
                ) {
                    Text(if (saved) "Resultado salvo ✓" else "Salvar resultado")
                }
            }

            is ScanUiState.Error -> Text(current.message)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedButton(onClick = onClose) { Text("Início") }
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