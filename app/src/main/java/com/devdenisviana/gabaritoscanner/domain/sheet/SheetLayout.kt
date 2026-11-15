package com.devdenisviana.gabaritoscanner.domain.sheet

import kotlin.math.min

/** A point in normalized sheet coordinates: (0,0) = top-left, (1,1) = bottom-right. */
data class NormalizedPoint(val x: Float, val y: Float)

/** One printed bubble: which question/option it belongs to and where its center is. */
data class BubbleCell(
    val question: Int,       // 1-based question number
    val optionIndex: Int,    // 0-based index (0 = A, 1 = B, ...)
    val option: Char,        // 'A'..'E'
    val center: NormalizedPoint,
)

/**
 * Computes where every bubble sits on the sheet for a given [SheetProfile].
 * Coordinates are normalized to the area inside the four corner markers.
 */
class SheetLayout(val profile: SheetProfile) {

    private val columnWidth = (GRID_RIGHT - GRID_LEFT) / profile.columns
    private val rowPitch = (GRID_BOTTOM - GRID_TOP) / profile.rowsPerColumn
    private val optionsAreaWidth = columnWidth * (1f - LABEL_FRACTION)
    private val optionPitch = optionsAreaWidth / profile.optionsPerQuestion

    /** Bubble radius as a fraction of the sheet WIDTH. */
    val bubbleRadius: Float =
        min(optionPitch, rowPitch * HEIGHT_TO_WIDTH_RATIO) * RADIUS_FACTOR

    fun bubbleCenter(question: Int, optionIndex: Int): NormalizedPoint {
        require(question in 1..profile.questionCount) { "question out of range: $question" }
        require(optionIndex in 0 until profile.optionsPerQuestion) { "optionIndex out of range: $optionIndex" }

        val index = question - 1
        val column = index / profile.rowsPerColumn
        val row = index % profile.rowsPerColumn

        val columnStart = GRID_LEFT + column * columnWidth
        val optionsStart = columnStart + columnWidth * LABEL_FRACTION

        val x = optionsStart + (optionIndex + 0.5f) * optionPitch
        val y = GRID_TOP + (row + 0.5f) * rowPitch
        return NormalizedPoint(x, y)
    }

    fun allBubbles(): List<BubbleCell> =
        (1..profile.questionCount).flatMap { question ->
            profile.optionLabels.mapIndexed { optionIndex, label ->
                BubbleCell(
                    question = question,
                    optionIndex = optionIndex,
                    option = label,
                    center = bubbleCenter(question, optionIndex),
                )
            }
        }

    companion object {
        const val GRID_LEFT = 0.06f
        const val GRID_RIGHT = 0.94f
        const val GRID_TOP = 0.30f
        const val GRID_BOTTOM = 0.95f

        /** Share of each column's width reserved for the printed question number. */
        const val LABEL_FRACTION = 0.20f

        /** A4 portrait: height / width, used to convert vertical distances to width units. */
        const val HEIGHT_TO_WIDTH_RATIO = 297f / 210f

        const val RADIUS_FACTOR = 0.30f
    }
}