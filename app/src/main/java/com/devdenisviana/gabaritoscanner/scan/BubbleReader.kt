package com.devdenisviana.gabaritoscanner.scan

import com.devdenisviana.gabaritoscanner.domain.sheet.SheetLayout
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/** What was read for a single question. */
sealed interface Answer {
    data class Marked(val optionIndex: Int, val option: Char) : Answer
    data object Blank : Answer
    data class Multiple(val optionIndices: List<Int>) : Answer
}

/** Reading result for one question, with the raw darkness of each option (0..1). */
data class QuestionReading(
    val question: Int,
    val answer: Answer,
    val darkness: List<Float>,
)

/** Grayscale image: [pixels] holds values 0 (black) to 255 (white), row by row. */
class GrayImage(val width: Int, val height: Int, val pixels: IntArray) {
    init {
        require(pixels.size == width * height) { "pixels size must be width * height" }
    }
}

/**
 * Reads which bubbles are filled on an ALIGNED sheet image
 * (normalized sheet coordinates map to x * width, y * height).
 *
 * Darkness of a bubble = (paper - inside) / paper, where "paper" is the mean brightness of a
 * ring just outside the bubble and "inside" is the DARKEST mean brightness of a small disk
 * sampled around the expected center. Searching a small window makes the reader tolerant to
 * slightly curved paper, imperfect alignment and off-center marks, while the small disk stays
 * clear of the printed outline. Being relative to the paper, it also tolerates uneven lighting.
 */
class BubbleReader(
    private val layout: SheetLayout,
    private val markThreshold: Float = DEFAULT_MARK_THRESHOLD,
    private val multipleRatio: Float = DEFAULT_MULTIPLE_RATIO,
) {

    fun read(image: GrayImage): List<QuestionReading> {
        val radiusPx = layout.bubbleRadius * image.width
        val options = layout.profile.optionsPerQuestion

        return (1..layout.profile.questionCount).map { question ->
            val darkness = (0 until options).map { optionIndex ->
                val center = layout.bubbleCenter(question, optionIndex)
                bubbleDarkness(
                    image = image,
                    cx = center.x * image.width,
                    cy = center.y * image.height,
                    radius = radiusPx,
                )
            }
            QuestionReading(question, classify(darkness), darkness)
        }
    }

    private fun classify(darkness: List<Float>): Answer {
        val strongest = darkness.max()
        if (strongest < markThreshold) return Answer.Blank

        val limit = max(markThreshold, strongest * multipleRatio)
        val marked = darkness.indices.filter { darkness[it] >= limit }

        return if (marked.size == 1) {
            Answer.Marked(marked[0], layout.profile.optionLabels[marked[0]])
        } else {
            Answer.Multiple(marked)
        }
    }

    private fun bubbleDarkness(image: GrayImage, cx: Float, cy: Float, radius: Float): Float {
        val paper = meanInRing(image, cx, cy, radius * RING_INNER, radius * RING_OUTER)
        if (paper <= 1f) return 0f

        // Sample the inner disk at several positions within a small circular window.
        val step = radius * SEARCH_STEP
        var darkestInside = Float.MAX_VALUE
        for (iy in -SEARCH_STEPS..SEARCH_STEPS) {
            for (ix in -SEARCH_STEPS..SEARCH_STEPS) {
                if (ix * ix + iy * iy > SEARCH_STEPS * SEARCH_STEPS) continue
                val inside = meanInRing(
                    image,
                    cx + ix * step,
                    cy + iy * step,
                    0f,
                    radius * INNER_FACTOR,
                )
                darkestInside = min(darkestInside, inside)
            }
        }
        return ((paper - darkestInside) / paper).coerceIn(0f, 1f)
    }

    /** Mean brightness of pixels whose distance to (cx, cy) is within [rInner, rOuter]. */
    private fun meanInRing(image: GrayImage, cx: Float, cy: Float, rInner: Float, rOuter: Float): Float {
        val x0 = max(0, floor(cx - rOuter).toInt())
        val x1 = min(image.width - 1, ceil(cx + rOuter).toInt())
        val y0 = max(0, floor(cy - rOuter).toInt())
        val y1 = min(image.height - 1, ceil(cy + rOuter).toInt())

        val inner2 = rInner * rInner
        val outer2 = rOuter * rOuter
        var sum = 0L
        var count = 0

        for (y in y0..y1) {
            val dy = y - cy
            for (x in x0..x1) {
                val dx = x - cx
                val d2 = dx * dx + dy * dy
                if (d2 >= inner2 && d2 <= outer2) {
                    sum += image.pixels[y * image.width + x]
                    count++
                }
            }
        }
        return if (count == 0) 0f else sum.toFloat() / count
    }

    companion object {
        /** Minimum darkness for a bubble to count as marked. */
        const val DEFAULT_MARK_THRESHOLD = 0.25f

        /** A second bubble this fraction as dark as the strongest one makes the answer "multiple". */
        const val DEFAULT_MULTIPLE_RATIO = 0.6f

        /** Radius of the sampled inner disk, as a fraction of the bubble radius. */
        private const val INNER_FACTOR = 0.5f

        private const val RING_INNER = 1.35f
        private const val RING_OUTER = 1.7f

        /** Search window: SEARCH_STEPS steps of SEARCH_STEP * radius in each direction (±25%). */
        private const val SEARCH_STEP = 0.125f
        private const val SEARCH_STEPS = 2
    }
}