package com.devdenisviana.gabaritoscanner.ui.exam

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.devdenisviana.gabaritoscanner.data.ExamEntity
import com.devdenisviana.gabaritoscanner.domain.student.StudentTag
import com.devdenisviana.gabaritoscanner.ui.components.AppHeader

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

    Column(modifier = modifier.fillMaxSize()) {
        AppHeader(
            title = "Gabaritos",
            subtitle = "Cadastre gabaritos e gere as folhas",
            onBack = onBack,
        )

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (exams.isEmpty()) {
                Text(
                    text = "Nenhum gabarito cadastrado ainda.\nToque em \"Novo gabarito\" para começar.",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp),
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(exams, key = { it.id }) { exam ->
                        val lastOption = 'A' + exam.optionsPerQuestion - 1
                        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 16.dp, bottom = 4.dp)) {
                                Text(text = exam.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = "${exam.questionCount} questões · alternativas A–$lastOption",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Row {
                                    TextButton(onClick = { onOpen(exam) }) { Text("Resultados") }
                                    TextButton(onClick = {
                                        sheetName = ""
                                        sheetCode = ""
                                        sheetExam = exam
                                    }) { Text("Folha") }
                                    TextButton(
                                        onClick = { examToDelete = exam },
                                        colors = ButtonDefaults.textButtonColors(
                                            contentColor = MaterialTheme.colorScheme.error,
                                        ),
                                    ) { Text("Apagar") }
                                }
                            }
                        }
                    }
                }
            }

            ExtendedFloatingActionButton(
                onClick = onNewExam,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Novo gabarito") },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(16.dp),
            )
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

    Column(modifier = modifier.fillMaxSize()) {
        AppHeader(
            title = "Novo gabarito",
            subtitle = "Escolha o tamanho e informe as respostas",
            onBack = onBack,
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
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

            Text(text = "Alternativas por questão", style = MaterialTheme.typography.titleSmall)
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

            OutlinedButton(
                onClick = {
                    if (questionCount !in 1..100) {
                        error = "O número de questões deve ficar entre 1 e 100."
                    } else {
                        onReadFromSheet(questionCount, draft.options)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.PhotoCamera, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Ler de uma folha preenchida")
            }

            error?.let { Text(text = it, color = MaterialTheme.colorScheme.error) }

            Button(
                onClick = {
                    val message = onSave(draft.name, draft.answerKey, questionCount, draft.options)
                    if (message == null) {
                        draft.clear()
                        onBack()
                    } else {
                        error = message
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Salvar gabarito") }
        }
    }
}