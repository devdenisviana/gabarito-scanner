package com.devdenisviana.gabaritoscanner.ui.exam

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.devdenisviana.gabaritoscanner.data.ExamEntity
import com.devdenisviana.gabaritoscanner.ui.components.AppHeader
import com.devdenisviana.gabaritoscanner.ui.components.EmptyState

/** Lets the user choose which answer key the next scanned sheets will be graded against. */
@Composable
fun ExamPickerScreen(
    exams: List<ExamEntity>,
    onPick: (ExamEntity) -> Unit,
    onNewExam: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        AppHeader(
            title = "Escolha o gabarito",
            icon = Icons.Filled.Checklist,
            subtitle = "Contra qual gabarito corrigir?",
            onBack = onBack,
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .navigationBarsPadding(),
        ) {
            if (exams.isEmpty()) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    EmptyState(message = "Cadastre um gabarito antes de ler as folhas.")
                    Button(onClick = onNewExam) { Text("Novo gabarito") }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(exams, key = { it.id }) { exam ->
                        val lastOption = 'A' + exam.optionsPerQuestion - 1
                        ElevatedCard(
                            onClick = { onPick(exam) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(text = exam.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = "${exam.questionCount} questões · alternativas A–$lastOption",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}