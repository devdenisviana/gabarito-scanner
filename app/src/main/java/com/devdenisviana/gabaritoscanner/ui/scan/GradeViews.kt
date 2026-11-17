package com.devdenisviana.gabaritoscanner.ui.scan

import android.graphics.Bitmap
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devdenisviana.gabaritoscanner.grading.AnswerKey
import com.devdenisviana.gabaritoscanner.grading.GradeResult
import com.devdenisviana.gabaritoscanner.grading.QuestionOutcome
import com.devdenisviana.gabaritoscanner.scan.Answer
import com.devdenisviana.gabaritoscanner.scan.QuestionReading
import com.devdenisviana.gabaritoscanner.ui.components.ScoreRing
import com.devdenisviana.gabaritoscanner.ui.components.Staggered
import com.devdenisviana.gabaritoscanner.ui.components.TabbedContent
import com.devdenisviana.gabaritoscanner.ui.components.badColor
import com.devdenisviana.gabaritoscanner.ui.components.goodColor
import com.devdenisviana.gabaritoscanner.ui.components.scoreColor
import com.devdenisviana.gabaritoscanner.ui.components.warnColor

/** The three tabs of a graded sheet: summary, question table and the photo. */
@Composable
fun GradeTabs(
    key: AnswerKey,
    readings: List<QuestionReading>,
    grade: GradeResult,
    overlay: Bitmap,
    modifier: Modifier = Modifier,
) {
    TabbedContent(titles = listOf("Resumo", "Questões", "Folha"), modifier = modifier) { tab ->
        when (tab) {
            0 -> SummaryTab(grade)
            1 -> QuestionsTab(key, readings, grade)
            else -> SheetTab(overlay)
        }
    }
}

@Composable
private fun SummaryTab(grade: GradeResult, modifier: Modifier = Modifier) {
    val ratio = if (grade.total == 0) 0f else grade.correct.toFloat() / grade.total

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ScoreRing(
            score = grade.score10,
            ratio = ratio,
            color = scoreColor(ratio.toDouble()),
        )
        Text(
            text = "${grade.correct} de ${grade.total} questões certas",
            style = MaterialTheme.typography.titleMedium,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Staggered(0, Modifier.weight(1f)) { StatCard("Acertos", grade.correct, goodColor()) }
            Staggered(1, Modifier.weight(1f)) { StatCard("Erradas", grade.wrong, badColor()) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Staggered(2, Modifier.weight(1f)) { StatCard("Em branco", grade.blank, warnColor()) }
            Staggered(3, Modifier.weight(1f)) { StatCard("Duplas", grade.multiple, warnColor()) }
        }
    }
}

@Composable
private fun StatCard(label: String, value: Int, color: Color) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = value.toString(),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = color,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun QuestionsTab(
    key: AnswerKey,
    readings: List<QuestionReading>,
    grade: GradeResult,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 32.dp, end = 32.dp, top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HeaderCell("Questão", Modifier.weight(1f), TextAlign.Start)
            HeaderCell("Gabarito", Modifier.weight(1f), TextAlign.Center)
            HeaderCell("Resposta", Modifier.weight(1f), TextAlign.Center)
            Spacer(modifier = Modifier.width(32.dp))
        }
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            itemsIndexed(grade.outcomes) { index, outcome ->
                Staggered(index) {
                    QuestionRow(
                        number = index + 1,
                        expected = key.answers[index],
                        answer = readings[index].answer,
                        outcome = outcome,
                    )
                }
            }
        }
    }
}

@Composable
private fun HeaderCell(text: String, modifier: Modifier, align: TextAlign) {
    Text(
        text = text,
        modifier = modifier,
        textAlign = align,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun QuestionRow(
    number: Int,
    expected: Char,
    answer: Answer,
    outcome: QuestionOutcome,
) {
    val color = outcomeColor(outcome)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color.copy(alpha = 0.10f), RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = number.toString(),
            modifier = Modifier.weight(1f),
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = expected.toString(),
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
        )
        Text(
            text = answerSymbol(answer),
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            color = color,
            fontWeight = FontWeight.Bold,
        )
        OutcomeIcon(outcome, color)
    }
}

@Composable
private fun OutcomeIcon(outcome: QuestionOutcome, color: Color) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val scale by animateFloatAsState(
        targetValue = if (shown) 1f else 0.5f,
        animationSpec = tween(350),
        label = "pop",
    )
    val icon: ImageVector = when (outcome) {
        QuestionOutcome.CORRECT -> Icons.Filled.Check
        QuestionOutcome.WRONG -> Icons.Filled.Close
        QuestionOutcome.BLANK -> Icons.Filled.Remove
        QuestionOutcome.MULTIPLE -> Icons.Filled.Warning
    }
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = color,
        modifier = Modifier
            .size(32.dp)
            .padding(4.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
    )
}

@Composable
private fun outcomeColor(outcome: QuestionOutcome): Color = when (outcome) {
    QuestionOutcome.CORRECT -> goodColor()
    QuestionOutcome.WRONG -> badColor()
    QuestionOutcome.BLANK, QuestionOutcome.MULTIPLE -> warnColor()
}

private fun answerSymbol(answer: Answer): String = when (answer) {
    is Answer.Marked -> answer.option.toString()
    Answer.Blank -> "–"
    is Answer.Multiple -> "*"
}

@Composable
private fun SheetTab(overlay: Bitmap, modifier: Modifier = Modifier) {
    val image = remember(overlay) { overlay.asImageBitmap() }
    Image(
        bitmap = image,
        contentDescription = "Folha lida",
        contentScale = ContentScale.Fit,
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
    )
}