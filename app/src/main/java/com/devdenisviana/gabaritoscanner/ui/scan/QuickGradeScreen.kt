package com.devdenisviana.gabaritoscanner.ui.scan

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.devdenisviana.gabaritoscanner.data.ExamEntity
import com.devdenisviana.gabaritoscanner.data.toAnswerKey
import com.devdenisviana.gabaritoscanner.data.toSheetProfile
import com.devdenisviana.gabaritoscanner.domain.sheet.SheetLayout
import com.devdenisviana.gabaritoscanner.grading.AnswerKey
import com.devdenisviana.gabaritoscanner.grading.GradeResult
import com.devdenisviana.gabaritoscanner.grading.Grader
import com.devdenisviana.gabaritoscanner.scan.AlignResult
import com.devdenisviana.gabaritoscanner.scan.BubbleReader
import com.devdenisviana.gabaritoscanner.scan.QuestionReading
import com.devdenisviana.gabaritoscanner.scan.ScanOverlay
import com.devdenisviana.gabaritoscanner.scan.SheetAligner
import com.devdenisviana.gabaritoscanner.scan.toGrayImage
import com.devdenisviana.gabaritoscanner.ui.components.AppHeader
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private sealed interface QuickState {
    data object Loading : QuickState
    data class Done(
        val overlay: Bitmap,
        val key: AnswerKey,
        val readings: List<QuestionReading>,
        val grade: GradeResult,
    ) : QuickState

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
                        val key = exam.toAnswerKey()
                        QuickState.Done(
                            overlay = ScanOverlay.draw(result.sheet, layout, readings),
                            key = key,
                            readings = readings,
                            grade = Grader.grade(key, readings),
                        )
                    }
                }
            }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        AppHeader(
            title = "Correção rápida",
            subtitle = "${exam.name} · Folha $sheetNumber",
            onBack = onClose,
        )

        when (val current = state) {
            QuickState.Loading -> {
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

            is QuickState.Done -> {
                GradeTabs(
                    key = current.key,
                    readings = current.readings,
                    grade = current.grade,
                    overlay = current.overlay,
                    modifier = Modifier.weight(1f),
                )
            }

            is QuickState.Error -> {
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
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tente fotografar de novo, com a folha inteira e bem iluminada.",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
            Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) {
                Text("Próxima folha")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onRetake, modifier = Modifier.weight(1f)) {
                    Text("Refazer foto")
                }
                OutlinedButton(onClick = onClose, modifier = Modifier.weight(1f)) {
                    Text("Terminar")
                }
            }
        }
    }
}