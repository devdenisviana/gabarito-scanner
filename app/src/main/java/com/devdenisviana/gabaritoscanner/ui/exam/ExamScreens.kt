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

/** Lists the registered answer keys, with options to create, print the sheet and delete them. */
@Composable
fun ExamListScreen(
    exams: List<ExamEntity>,
    onOpen: (ExamEntity) -> Unit,
    onSheet: (ExamEntity) -> Unit,
    onNewExam: () -> Unit,
    onDelete: (ExamEntity) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var examToDelete by remember { mutableStateOf<ExamEntity?>(null) }

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
                                TextButton(onClick = { onSheet(exam) }) { Text("Folha") }
                                TextButton(onClick = { examToDelete = exam }) { Text("Apagar") }
                            }
                        }
                    }
                }
            }
        }
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
    onSave: (name: String, answerKey: String, questionCount: Int, optionsPerQuestion: Int) -> String?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var name by remember { mutableStateOf("") }
    var questionCountText by remember { mutableStateOf("20") }
    var options by remember { mutableIntStateOf(5) }
    var answerKey by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    val questionCount = questionCountText.toIntOrNull() ?: 0
    val typed = answerKey.count { !it.isWhitespace() }
    val lastLetter = 'A' + options - 1

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = "Novo gabarito", style = MaterialTheme.typography.headlineSmall)

        OutlinedTextField(
            value = name,
            onValueChange = {
                name = it
                error = null
            },
            label = { Text("Nome do gabarito") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = questionCountText,
            onValueChange = { text ->
                questionCountText = text.filter { it.isDigit() }.take(3)
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
                    selected = options == count,
                    onClick = {
                        options = count
                        error = null
                    },
                    label = { Text("A–${'A' + count - 1}") },
                )
            }
        }

        OutlinedTextField(
            value = answerKey,
            onValueChange = {
                answerKey = it.uppercase()
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
                val message = onSave(name, answerKey, questionCount, options)
                if (message == null) onBack() else error = message
            }) { Text("Salvar") }
            OutlinedButton(onClick = onBack) { Text("Cancelar") }
        }
    }
}