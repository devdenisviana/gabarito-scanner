package com.devdenisviana.gabaritoscanner.ui.exam

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devdenisviana.gabaritoscanner.data.ExamEntity
import com.devdenisviana.gabaritoscanner.data.ScanResultEntity
import com.devdenisviana.gabaritoscanner.ui.components.AppHeader
import com.devdenisviana.gabaritoscanner.ui.components.EmptyState
import com.devdenisviana.gabaritoscanner.ui.theme.ScoreBadDark
import com.devdenisviana.gabaritoscanner.ui.theme.ScoreBadLight
import com.devdenisviana.gabaritoscanner.ui.theme.ScoreGoodDark
import com.devdenisviana.gabaritoscanner.ui.theme.ScoreGoodLight
import java.text.DateFormat
import java.util.Date

/** Green with 60% of correct answers or more, red below that. */
@Composable
private fun scoreColor(ratio: Double): Color {
    val dark = isSystemInDarkTheme()
    return if (ratio >= 0.6) {
        if (dark) ScoreGoodDark else ScoreGoodLight
    } else {
        if (dark) ScoreBadDark else ScoreBadLight
    }
}

/** Lists the graded sheets of one answer key, with the class summary. */
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
    val averageRatio = if (results.isEmpty()) 0.0 else results.map { ratioOf(it) }.average()

    Column(modifier = modifier.fillMaxSize()) {
        AppHeader(
            title = "Resultados",
            icon = Icons.Filled.Assessment,
            subtitle = exam.name,
            onBack = onBack,
        )

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        SummaryCard(
                            label = "Alunos",
                            value = results.size.toString(),
                            valueColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f),
                        )
                        SummaryCard(
                            label = "Média da turma",
                            value = if (average == null) "–" else "%.1f".format(average),
                            valueColor = if (average == null) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                scoreColor(averageRatio)
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                if (results.isEmpty()) {
                    item {
                        EmptyState(
                            message = "Nenhuma folha corrigida ainda.\nToque em \"Corrigir folhas\" para começar.",
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                items(results, key = { it.id }) { result ->
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "%.1f".format(result.score),
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = scoreColor(ratioOf(result)),
                                modifier = Modifier.width(72.dp),
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = result.studentName, style = MaterialTheme.typography.titleMedium)
                                if (result.studentCode.isNotBlank()) {
                                    Text(
                                        text = "Matrícula: ${result.studentCode}",
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                }
                                Text(
                                    text = "${result.correct}/${result.total} acertos · " +
                                            dateFormat.format(Date(result.scannedAt)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            TextButton(
                                onClick = { resultToDelete = result },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error,
                                ),
                            ) { Text("Apagar") }
                        }
                    }
                }
            }

            ExtendedFloatingActionButton(
                onClick = onScan,
                icon = { Icon(Icons.Filled.PhotoCamera, contentDescription = null) },
                text = { Text("Corrigir folhas") },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(16.dp),
            )
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

private fun ratioOf(result: ScanResultEntity): Double =
    if (result.total == 0) 0.0 else result.correct.toDouble() / result.total

@Composable
private fun SummaryCard(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = value,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = valueColor,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}