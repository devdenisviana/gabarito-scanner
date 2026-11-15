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

/** Lists the registered exams, with options to create and delete them. */
@Composable
fun ExamListScreen(
    exams: List<ExamEntity>,
    onOpen: (ExamEntity) -> Unit,
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
        Text(text = "Provas", style = MaterialTheme.typography.headlineSmall)

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onNewExam) { Text("Nova prova") }
            OutlinedButton(onClick = onBack) { Text("Voltar") }
        }

        if (exams.isEmpty()) {
            Text("Nenhuma prova cadastrada ainda. Toque em \"Nova prova\" para começar.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(exams, key = { it.id }) { exam ->
                    val lastOption = 'A' + exam.optionsPerQuestion - 1
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = exam.name, style = MaterialTheme.typography.titleMedium)
                                Text(text = "${exam.questionCount} questões (A–$lastOption)")
                            }
                            TextButton(onClick = { onOpen(exam) }) { Text("Resultados") }
                            TextButton(onClick = { examToDelete = exam }) { Text("Apagar") }
                        }
                    }
                }
            }
        }
    }

    examToDelete?.let { exam ->
        AlertDialog(
            onDismissRequest = { examToDelete = null },
            title = { Text("Apagar prova?") },
            text = { Text("\"${exam.name}\" e todos os resultados dela serão apagados.") },
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
 * Form to register an exam. [onSave] returns an error message to display,
 * or null when the exam was created (the screen then goes back).
 */
@Composable
fun ExamFormScreen(
    questionCount: Int,
    onSave: (name: String, answerKey: String) -> String?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var name by remember { mutableStateOf("") }
    var answerKey by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    val typed = answerKey.count { !it.isWhitespace() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = "Nova prova", style = MaterialTheme.typography.headlineSmall)

        OutlinedTextField(
            value = name,
            onValueChange = {
                name = it
                error = null
            },
            label = { Text("Nome da prova") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = answerKey,
            onValueChange = {
                answerKey = it.uppercase()
                error = null
            },
            label = { Text("Gabarito oficial") },
            supportingText = { Text("$typed / $questionCount respostas. Ex.: ABCDE ABCDE ...") },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
            modifier = Modifier.fillMaxWidth(),
        )

        error?.let { Text(text = it, color = MaterialTheme.colorScheme.error) }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = {
                val message = onSave(name, answerKey)
                if (message == null) onBack() else error = message
            }) { Text("Salvar") }
            OutlinedButton(onClick = onBack) { Text("Cancelar") }
        }
    }
}