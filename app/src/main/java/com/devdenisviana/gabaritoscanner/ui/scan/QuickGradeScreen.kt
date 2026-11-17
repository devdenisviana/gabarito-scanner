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
import androidx.compose.ui.unit.dp
import com.devdenisviana.gabaritoscanner.data.ExamEntity
import com.devdenisviana.gabaritoscanner.data.toAnswerKey
import com.devdenisviana.gabaritoscanner.data.toSheetProfile
import com.devdenisviana.gabaritoscanner.domain.sheet.SheetLayout
import com.devdenisviana.gabaritoscanner.grading.GradeResult
import com.devdenisviana.gabaritoscanner.grading.Grader
import com.devdenisviana.gabaritoscanner.scan.AlignResult
import com.devdenisviana.gabaritoscanner.scan.BubbleReader
import com.devdenisviana.gabaritoscanner.scan.ScanOverlay
import com.devdenisviana.gabaritoscanner.scan.SheetAligner
import com.devdenisviana.gabaritoscanner.scan.toGrayImage
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private sealed interface QuickState {
    data object Loading : QuickState
    data class Done(val overlay: Bitmap, val grade: GradeResult) : QuickState
    data class Error(val message: String) : QuickState
}

/**
 * Quick-correction result: reads the photo, grades it against [exam] and shows
 * the score only. Nothing is saved; [onNext] goes on to the next sheet.
 */
@Composable
fun QuickGradeScreen(
    file: File,
    exam: ExamEntity,
    sheetNumber: Int,
    onNext: () -> Unit,
    onRetake: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var state by remember(file) { mutableStateOf<QuickState>(QuickState.Loading) }

    LaunchedEffect(file, exam.id) {
        state = withContext(Dispatchers.Default) {
            val photo = BitmapFactory.decodeFile(file.absolutePath)
            if (photo == null) {
                QuickState.Error("Não foi possível abrir a foto.")
            } else {
                when (val result = SheetAligner.align(photo)) {
                    is AlignResult.Failure -> QuickState.Error(result.reason)
                    is AlignResult.Success -> {
                        val layout = SheetLayout(exam.toSheetProfile())
                        val readings = BubbleReader(layout).read(result.sheet.toGrayImage())
                        QuickState.Done(
                            overlay = ScanOverlay.draw(result.sheet, layout, readings),
                            grade = Grader.grade(exam.toAnswerKey(), readings),
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
        Text("${exam.name} · Folha $sheetNumber")

        when (val current = state) {
            QuickState.Loading -> {
                CircularProgressIndicator()
                Text("Processando a folha...")
            }

            is QuickState.Done -> {
                val grade = current.grade

                Text(
                    text = "Nota ${"%.1f".format(grade.score10)}",
                    style = MaterialTheme.typography.displayMedium,
                )
                Text("Acertos: ${grade.correct} de ${grade.total}   Erros: ${grade.errors}")
                Text("(${grade.wrong} erradas, ${grade.blank} em branco, ${grade.multiple} duplas)")
                Image(
                    bitmap = current.overlay.asImageBitmap(),
                    contentDescription = "Folha lida",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                )
            }

            is QuickState.Error -> {
                Text(current.message)
                Text("Tente fotografar de novo, com a folha inteira e bem iluminada.")
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onClose) { Text("Terminar") }
            OutlinedButton(onClick = onRetake) { Text("Refazer foto") }
            Button(onClick = onNext) { Text("Próxima folha") }
        }
    }
}