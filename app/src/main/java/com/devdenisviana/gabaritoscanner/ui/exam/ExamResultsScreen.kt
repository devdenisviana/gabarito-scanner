package com.devdenisviana.gabaritoscanner.ui.exam

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.devdenisviana.gabaritoscanner.data.ExamEntity
import com.devdenisviana.gabaritoscanner.data.ScanResultEntity
import java.text.DateFormat
import java.util.Date

/** Lists the graded sheets of one exam, with the class average. */
@Composable
fun ExamResultsScreen(
    exam: ExamEntity,
    results: List<ScanResultEntity>,
    onScan: () -> Unit,
    onDelete: (ScanResultEntity) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var resultToDelete by remember { mutableStateOf<ScanResultEntity?>(null) }
    val dateFormat = remember { DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT) }
    val average = if (results.isEmpty()) null else results.map { it.score }.average()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = "Resultados", style = MaterialTheme.typography.headlineSmall)
        Text(text = exam.name, style = MaterialTheme.typography.titleMedium)

        if (average == null) {
            Text("Nenhuma folha corrigida ainda.")
        } else {
            Text("Alunos: ${results.size}   Média da turma: ${"%.1f".format(average)}")
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onScan) { Text("Corrigir folhas") }
            OutlinedButton(onClick = onBack) { Text("Voltar") }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(results, key = { it.id }) { result ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = result.studentName, style = MaterialTheme.typography.titleMedium)
                            if (result.studentCode.isNotBlank()) {
                                Text(text = "Matrícula: ${result.studentCode}")
                            }
                            Text(
                                text = "Nota ${"%.1f".format(result.score)}  •  " +
                                        "${result.correct}/${result.total} acertos",
                            )
                            Text(
                                text = dateFormat.format(Date(result.scannedAt)),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        TextButton(onClick = { resultToDelete = result }) { Text("Apagar") }
                    }
                }
            }
        }
    }

    resultToDelete?.let { result ->
        AlertDialog(
            onDismissRequest = { resultToDelete = null },
            title = { Text("Apagar resultado?") },
            text = { Text("O resultado de ${result.studentName} será apagado.") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(result)
                    resultToDelete = null
                }) { Text("Apagar") }
            },
            dismissButton = {
                TextButton(onClick = { resultToDelete = null }) { Text("Cancelar") }
            },
        )
    }
}