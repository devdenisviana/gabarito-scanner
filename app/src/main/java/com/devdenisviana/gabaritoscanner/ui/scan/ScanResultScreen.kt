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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.devdenisviana.gabaritoscanner.data.ExamEntity
import com.devdenisviana.gabaritoscanner.data.toAnswerKey
import com.devdenisviana.gabaritoscanner.data.toSheetProfile
import com.devdenisviana.gabaritoscanner.domain.sheet.SheetLayout
import com.devdenisviana.gabaritoscanner.domain.student.StudentTag
import com.devdenisviana.gabaritoscanner.grading.AnswerKey
import com.devdenisviana.gabaritoscanner.grading.GradeResult
import com.devdenisviana.gabaritoscanner.grading.Grader
import com.devdenisviana.gabaritoscanner.scan.AlignResult
import com.devdenisviana.gabaritoscanner.scan.BubbleReader
import com.devdenisviana.gabaritoscanner.scan.QuestionReading
import com.devdenisviana.gabaritoscanner.scan.ScanOverlay
import com.devdenisviana.gabaritoscanner.scan.SheetAligner
import com.devdenisviana.gabaritoscanner.scan.StudentQrReader
import com.devdenisviana.gabaritoscanner.scan.toGrayImage
import com.devdenisviana.gabaritoscanner.ui.components.AppHeader
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private sealed interface ScanUiState {
    data object Loading : ScanUiState
    data class Done(
        val overlay: Bitmap,
        val key: AnswerKey,
        val readings: List<QuestionReading>,
        val grade: GradeResult,
        val student: StudentTag?,
    ) : ScanUiState

    data class Error(val message: String) : ScanUiState
}

/**
 * Aligns the captured photo, reads the bubbles, grades them against [exam]
 * and lets the user save the result with the student's name.
 * When the sheet carries a student QR code, name and code are filled in automatically.
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
        val newState = withContext(Dispatchers.Default) {
            val photo = BitmapFactory.decodeFile(file.absolutePath)
            if (photo == null) {
                ScanUiState.Error("Não foi possível abrir a foto.")
            } else {
                when (val result = SheetAligner.align(photo)) {
                    is AlignResult.Failure -> ScanUiState.Error(result.reason)
                    is AlignResult.Success -> {
                        val layout = SheetLayout(exam.toSheetProfile())
                        val readings = BubbleReader(layout).read(result.sheet.toGrayImage())
                        val key = exam.toAnswerKey()
                        ScanUiState.Done(
                            overlay = ScanOverlay.draw(result.sheet, layout, readings),
                            key = key,
                            readings = readings,
                            grade = Grader.grade(key, readings),
                            student = StudentQrReader.read(result.sheet),
                        )
                    }
                }
            }
        }
        state = newState
        if (newState is ScanUiState.Done) {
            newState.student?.let {
                studentName = it.name
                studentCode = it.code
            }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        AppHeader(
            title = "Correção",
            icon = Icons.Filled.TaskAlt,
            subtitle = exam.name,
            onBack = onClose,
        )

        when (val current = state) {
            ScanUiState.Loading -> {
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

            is ScanUiState.Done -> {
                GradeTabs(
                    key = current.key,
                    readings = current.readings,
                    grade = current.grade,
                    overlay = current.overlay,
                    modifier = Modifier.weight(1f),
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding()
                        .navigationBarsPadding()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (current.student != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.QrCode,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Aluno identificado pelo QR code",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
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
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onRetake, modifier = Modifier.weight(1f)) {
                            Text("Tirar outra")
                        }
                        Button(
                            onClick = {
                                onSave(studentName, studentCode, current.readings, current.grade)
                                saved = true
                            },
                            enabled = !saved && studentName.isNotBlank(),
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(if (saved) "Resultado salvo ✓" else "Salvar resultado")
                        }
                    }
                }
            }

            is ScanUiState.Error -> {
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
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onRetake) { Text("Tirar outra") }
                }
            }
        }
    }
}