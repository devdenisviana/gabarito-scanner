package com.devdenisviana.gabaritoscanner.ui.exam

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.devdenisviana.gabaritoscanner.data.ExamEntity
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.text.input.KeyboardType
import com.devdenisviana.gabaritoscanner.domain.student.StudentTag



/** Form fields kept outside the screen, so they survive a trip to the camera. */
class ExamDraft {
    var name by mutableStateOf("")
    var questionCountText by mutableStateOf("20")
    var options by mutableIntStateOf(5)
    var answerKey by mutableStateOf("")

    fun clear() {
        name = ""
        questionCountText = "20"
        options = 5
        answerKey = ""
    }
}

/** Lists the registered answer keys, with options to create, print the sheet and delete them. */
@Composable
fun ExamListScreen(
    exams: List<ExamEntity>,
    onOpen: (ExamEntity) -> Unit,
    onSheet: (ExamEntity, StudentTag?) -> Unit,
    onNewExam: () -> Unit,
    onDelete: (ExamEntity) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var examToDelete by remember { mutableStateOf<ExamEntity?>(null) }
    var sheetExam by remember { mutableStateOf<ExamEntity?>(null) }
    var sheetName by remember { mutableStateOf("") }
    var sheetCode by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = "Gabaritos", style = MaterialTheme.typography.headlineSmall)

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onNewExam) { Text("Novo gabarito") }
            OutlinedButton(onClick = onBack) { Text("Voltar") }
        }

        if (exams.isEmpty()) {
            Text("Nenhum gabarito cadastrado ainda. Toque em \"Novo gabarito\" para começar.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(exams, key = { it.id }) { exam ->
                    val lastOption = 'A' + exam.optionsPerQuestion - 1
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(text = exam.name, style = MaterialTheme.typography.titleMedium)
                            Text(text = "${exam.questionCount} questões (A–$lastOption)")
                            Row {
                                TextButton(onClick = { onOpen(exam) }) { Text("Resultados") }
                                TextButton(onClick = {
                                    sheetName = ""
                                    sheetCode = ""
                                    sheetExam = exam
                                }) { Text("Folha") }
                                TextButton(onClick = { examToDelete = exam }) { Text("Apagar") }
                            }
                        }
                    }
                }
            }
        }
    }

    sheetExam?.let { exam ->
        val filled = sheetName.isNotBlank() && sheetCode.isNotBlank()
        val empty = sheetName.isBlank() && sheetCode.isBlank()
        AlertDialog(
            onDismissRequest = { sheetExam = null },
            title = { Text("Folha de respostas") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Deixe em branco para uma folha sem aluno, ou preencha nome e matrícula para gerar a folha com o QR code do aluno.")
                    OutlinedTextField(
                        value = sheetName,
                        onValueChange = { sheetName = it },
                        label = { Text("Nome do aluno") },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = sheetCode,
                        onValueChange = { sheetCode = it },
                        label = { Text("Matrícula") },
                        singleLine = true,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = filled || empty,
                    onClick = {
                        val student = if (filled) StudentTag(sheetCode.trim(), sheetName.trim()) else null
                        onSheet(exam, student)
                        sheetExam = null
                    },
                ) { Text("Gerar PDF") }
            },
            dismissButton = {
                TextButton(onClick = { sheetExam = null }) { Text("Cancelar") }
            },
        )
    }

    examToDelete?.let { exam ->
        AlertDialog(
            onDismissRequest = { examToDelete = null },
            title = { Text("Apagar gabarito?") },
            text = { Text("\"${exam.name}\" e todos os resultados dele serão apagados.") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(exam)
                    examToDelete = null
                }) { Text("Apagar") }
            },
            dismissButton = {
                TextButton(onClick = { examToDelete = null }) { Text("Cancelar") }
            },
        )
    }
}
/**
 * Form to register an answer key. [onSave] returns an error message to display,
 * or null when it was created (the screen then goes back).
 */
@Composable
fun ExamFormScreen(
    draft: ExamDraft,
    onSave: (name: String, answerKey: String, questionCount: Int, optionsPerQuestion: Int) -> String?,
    onReadFromSheet: (questionCount: Int, optionsPerQuestion: Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var error by remember { mutableStateOf<String?>(null) }

    val questionCount = draft.questionCountText.toIntOrNull() ?: 0
    val typed = draft.answerKey.count { !it.isWhitespace() }
    val lastLetter = 'A' + draft.options - 1

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = "Novo gabarito", style = MaterialTheme.typography.headlineSmall)

        OutlinedTextField(
            value = draft.name,
            onValueChange = {
                draft.name = it
                error = null
            },
            label = { Text("Nome do gabarito") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = draft.questionCountText,
            onValueChange = { text ->
                draft.questionCountText = text.filter { it.isDigit() }.take(3)
                error = null
            },
            label = { Text("Quantidade de questões") },
            supportingText = { Text("De 1 a 100") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )

        Text(text = "Alternativas por questão")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (count in 2..5) {
                FilterChip(
                    selected = draft.options == count,
                    onClick = {
                        draft.options = count
                        error = null
                    },
                    label = { Text("A–${'A' + count - 1}") },
                )
            }
        }

        OutlinedButton(onClick = {
            if (questionCount !in 1..100) {
                error = "O número de questões deve ficar entre 1 e 100."
            } else {
                onReadFromSheet(questionCount, draft.options)
            }
        }) { Text("Ler de uma folha preenchida") }

        OutlinedTextField(
            value = draft.answerKey,
            onValueChange = {
                draft.answerKey = it.uppercase()
                error = null
            },
            label = { Text("Respostas do gabarito") },
            supportingText = {
                Text("$typed / $questionCount respostas, letras de A a $lastLetter. Ex.: ABCD ABCD")
            },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
            modifier = Modifier.fillMaxWidth(),
        )

        error?.let { Text(text = it, color = MaterialTheme.colorScheme.error) }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = {
                val message = onSave(draft.name, draft.answerKey, questionCount, draft.options)
                if (message == null) {
                    draft.clear()
                    onBack()
                } else {
                    error = message
                }
            }) { Text("Salvar") }
            OutlinedButton(onClick = {
                draft.clear()
                onBack()
            }) { Text("Cancelar") }
        }
    }
}