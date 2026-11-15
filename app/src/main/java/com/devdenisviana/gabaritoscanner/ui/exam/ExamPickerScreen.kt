package com.devdenisviana.gabaritoscanner.ui.exam

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.devdenisviana.gabaritoscanner.data.ExamEntity

/** Lets the user choose which exam the next scanned sheets will be graded against. */
@Composable
fun ExamPickerScreen(
    exams: List<ExamEntity>,
    onPick: (ExamEntity) -> Unit,
    onNewExam: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = "Qual prova você vai corrigir?", style = MaterialTheme.typography.headlineSmall)
        OutlinedButton(onClick = onBack) { Text("Voltar") }

        if (exams.isEmpty()) {
            Text("Cadastre uma prova com o gabarito oficial antes de ler as folhas.")
            Button(onClick = onNewExam) { Text("Nova prova") }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(exams, key = { it.id }) { exam ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(exam) },
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = exam.name, style = MaterialTheme.typography.titleMedium)
                            Text(text = "${exam.questionCount} questões")
                        }
                    }
                }
            }
        }
    }
}