package com.devdenisviana.gabaritoscanner.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Ring that fills up to [ratio] (0..1) while the [score] counts up from zero.
 */
@Composable
fun ScoreRing(
    score: Double,
    ratio: Float,
    color: Color,
    modifier: Modifier = Modifier,
    diameter: Dp = 180.dp,
    thickness: Dp = 16.dp,
) {
    val target = ratio.coerceIn(0f, 1f)
    val progress = remember { Animatable(0f) }
    LaunchedEffect(target) {
        progress.snapTo(0f)
        progress.animateTo(target, tween(durationMillis = 1000, easing = FastOutSlowInEasing))
    }
    val shown = if (target > 0f) score * (progress.value / target) else score
    val track = MaterialTheme.colorScheme.surfaceVariant

    Box(modifier = modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = thickness.toPx()
            val topLeft = Offset(stroke / 2, stroke / 2)
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)

            drawArc(
                color = track,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke),
            )
            if (progress.value > 0f) {
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = 360f * progress.value,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "%.1f".format(shown),
                fontSize = 44.sp,
                fontWeight = FontWeight.Bold,
                color = color,
            )
            Text(
                text = "de 10",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}