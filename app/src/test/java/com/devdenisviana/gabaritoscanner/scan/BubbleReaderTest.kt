package com.devdenisviana.gabaritoscanner.scan

import com.devdenisviana.gabaritoscanner.domain.sheet.SheetLayout
import com.devdenisviana.gabaritoscanner.domain.sheet.SheetProfile
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.roundToInt
import kotlin.math.sqrt

class BubbleReaderTest {

    private val layout = SheetLayout(SheetProfile.DEFAULT)
    private val reader = BubbleReader(layout)
    private val width = 1000
    private val height = (width * SheetLayout.HEIGHT_TO_WIDTH_RATIO).roundToInt()
    private val radius = layout.bubbleRadius * width

    private fun sheet(paper: Int = 255) = IntArray(width * height) { paper }

    /** Paints every pixel whose distance to (cx, cy) is within [rInner, rOuter]. */
    private fun paint(pixels: IntArray, cx: Float, cy: Float, rInner: Float, rOuter: Float, gray: Int) {
        for (y in (cy - rOuter).toInt()..(cy + rOuter).toInt() + 1) {
            for (x in (cx - rOuter).toInt()..(cx + rOuter).toInt() + 1) {
                if (x !in 0 until width || y !in 0 until height) continue
                val dx = x - cx
                val dy = y - cy
                val distance = sqrt(dx * dx + dy * dy)
                if (distance in rInner..rOuter) pixels[y * width + x] = gray
            }
        }
    }

    private fun fill(pixels: IntArray, question: Int, optionIndex: Int, gray: Int) {
        val c = layout.bubbleCenter(question, optionIndex)
        paint(pixels, c.x * width, c.y * height, 0f, radius * 0.9f, gray)
    }

    private fun outline(pixels: IntArray, question: Int, optionIndex: Int) {
        val c = layout.bubbleCenter(question, optionIndex)
        paint(pixels, c.x * width, c.y * height, radius - 1.2f, radius + 1.2f, 40)
    }

    private fun read(pixels: IntArray) = reader.read(GrayImage(width, height, pixels))

    @Test
    fun blankSheet_readsAllBlank() {
        val readings = read(sheet())
        assertEquals(20, readings.size)
        readings.forEach { assertEquals(Answer.Blank, it.answer) }
    }

    @Test
    fun singleMark_isReadAndOthersStayBlank() {
        val pixels = sheet()
        fill(pixels, question = 3, optionIndex = 2, gray = 30)

        val readings = read(pixels)

        assertEquals(Answer.Marked(2, 'C'), readings[2].answer)
        readings.filter { it.question != 3 }.forEach { assertEquals(Answer.Blank, it.answer) }
    }

    @Test
    fun everyQuestionMarked_isReadCorrectly() {
        val pixels = sheet()
        for (question in 1..20) fill(pixels, question, question % 5, 30)

        val readings = read(pixels)

        readings.forEach {
            val expected = it.question % 5
            assertEquals(Answer.Marked(expected, 'A' + expected), it.answer)
        }
    }

    @Test
    fun twoMarksInTheSameQuestion_isMultiple() {
        val pixels = sheet()
        fill(pixels, question = 5, optionIndex = 1, gray = 30)
        fill(pixels, question = 5, optionIndex = 3, gray = 30)

        assertEquals(Answer.Multiple(listOf(1, 3)), read(pixels)[4].answer)
    }

    @Test
    fun lightPencilMark_isStillRead() {
        val pixels = sheet()
        fill(pixels, question = 7, optionIndex = 0, gray = 150)

        assertEquals(Answer.Marked(0, 'A'), read(pixels)[6].answer)
    }

    @Test
    fun veryFaintSmudge_isIgnored() {
        val pixels = sheet()
        fill(pixels, question = 7, optionIndex = 0, gray = 240)

        assertEquals(Answer.Blank, read(pixels)[6].answer)
    }

    @Test
    fun unevenLighting_stillWorks() {
        // Darker paper overall (as in a dim photo): marks are judged relative to the paper.
        val pixels = sheet(paper = 170)
        fill(pixels, question = 12, optionIndex = 4, gray = 50)

        val readings = read(pixels)

        assertEquals(Answer.Marked(4, 'E'), readings[11].answer)
        readings.filter { it.question != 12 }.forEach { assertEquals(Answer.Blank, it.answer) }
    }

    @Test
    fun printedOutlinesAlone_areNotMistakenForMarks() {
        val pixels = sheet()
        for (question in 1..20) {
            for (option in 0 until 5) outline(pixels, question, option)
        }

        read(pixels).forEach { assertEquals(Answer.Blank, it.answer) }
    }

    @Test
    fun smallOffCenterPencilMark_isStillRead() {
        val pixels = sheet()
        val c = layout.bubbleCenter(9, 3)
        // A small pencil mark (60% of the bubble radius), drawn 9 px off-center.
        paint(pixels, c.x * width + 9f, c.y * height, 0f, radius * 0.6f, 120)

        assertEquals(Answer.Marked(3, 'D'), read(pixels)[8].answer)
    }
}