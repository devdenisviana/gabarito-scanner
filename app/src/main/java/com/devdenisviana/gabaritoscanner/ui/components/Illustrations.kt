package com.devdenisviana.gabaritoscanner.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * An answer sheet drawn with code. Its bubbles fill one row after the other,
 * a check mark appears, then it fades and starts again.
 */
@Composable
fun SheetIllustration(modifier: Modifier = Modifier, height: Dp = 150.dp) {
    val transition = rememberInfiniteTransition(label = "sheetIllustration")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4500, easing = LinearEasing), RepeatMode.Restart),
        label = "progress",
    )

    val paper = MaterialTheme.colorScheme.surface
    val border = MaterialTheme.colorScheme.outlineVariant
    val line = MaterialTheme.colorScheme.outline
    val accent = MaterialTheme.colorScheme.primary
    val good = goodColor()

    Canvas(modifier = modifier.size(width = height * 0.78f, height = height)) {
        val w = size.width
        val h = size.height
        val corner = CornerRadius(12.dp.toPx())

        drawRoundRect(color = paper, cornerRadius = corner)
        drawRoundRect(color = border, cornerRadius = corner, style = Stroke(2.dp.toPx()))

        // Title bar and a small square that hints at the student's QR code.
        drawRoundRect(
            color = accent.copy(alpha = 0.85f),
            topLeft = Offset(w * 0.12f, h * 0.08f),
            size = Size(w * 0.5f, h * 0.05f),
            cornerRadius = CornerRadius(4.dp.toPx()),
        )
        drawRoundRect(
            color = line,
            topLeft = Offset(w * 0.72f, h * 0.06f),
            size = Size(w * 0.16f, w * 0.16f),
            cornerRadius = CornerRadius(3.dp.toPx()),
            style = Stroke(1.5.dp.toPx()),
        )

        val picked = intArrayOf(1, 3, 0, 2)
        val fadeOut = if (t > 0.93f) (1f - t) / 0.07f else 1f
        val radius = w * 0.05f

        for (row in 0 until 4) {
            val cy = h * (0.30f + row * 0.13f)
            val fill = (t * 5f - row).coerceIn(0f, 1f) * fadeOut
            for (col in 0 until 5) {
                val center = Offset(w * (0.2f + col * 0.15f), cy)
                drawCircle(color = line, radius = radius, center = center, style = Stroke(1.5.dp.toPx()))
                if (col == picked[row] && fill > 0f) {
                    drawCircle(
                        color = accent.copy(alpha = fill),
                        radius = radius * 0.9f * fill,
                        center = center,
                    )
                }
            }
        }

        val checkAlpha = ((t - 0.8f) / 0.1f).coerceIn(0f, 1f) * fadeOut
        if (checkAlpha > 0f) {
            val color = good.copy(alpha = checkAlpha)
            val stroke = 4.dp.toPx()
            val p1 = Offset(w * 0.36f, h * 0.86f)
            val p2 = Offset(w * 0.46f, h * 0.92f)
            val p3 = Offset(w * 0.66f, h * 0.80f)
            drawLine(color, p1, p2, stroke, StrokeCap.Round)
            drawLine(color, p2, p3, stroke, StrokeCap.Round)
        }
    }
}

/** A tiny static sheet that shows how many columns an answer key has. */
@Composable
fun MiniSheet(questionCount: Int, columns: Int, modifier: Modifier = Modifier) {
    val paper = MaterialTheme.colorScheme.surface
    val border = MaterialTheme.colorScheme.outlineVariant
    val dot = MaterialTheme.colorScheme.outline
    val accent = MaterialTheme.colorScheme.primary

    Canvas(modifier = modifier.size(width = 36.dp, height = 48.dp)) {
        val w = size.width
        val h = size.height
        val corner = CornerRadius(5.dp.toPx())

        drawRoundRect(color = paper, cornerRadius = corner)
        drawRoundRect(color = border, cornerRadius = corner, style = Stroke(1.5.dp.toPx()))
        drawRoundRect(
            color = accent,
            topLeft = Offset(w * 0.15f, h * 0.10f),
            size = Size(w * 0.5f, h * 0.07f),
            cornerRadius = CornerRadius(2.dp.toPx()),
        )

        val cols = columns.coerceIn(1, 4)
        val rows = ((questionCount + cols - 1) / cols).coerceIn(2, 5)
        val colWidth = (w * 0.7f) / cols
        for (c in 0 until cols) {
            for (r in 0 until rows) {
                drawCircle(
                    color = dot,
                    radius = 1.4.dp.toPx(),
                    center = Offset(w * 0.15f + colWidth * (c + 0.5f), h * (0.30f + r * 0.13f)),
                )
            }
        }
    }
}

/** The animated sheet with a message under it, for lists that have nothing yet. */
@Composable
fun EmptyState(message: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SheetIllustration()
        Text(
            text = message,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}