package com.devdenisviana.gabaritoscanner.domain.sheet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SheetLayoutTest {

    private val layout = SheetLayout(SheetProfile.DEFAULT)

    @Test
    fun defaultProfile_has100Bubbles() {
        assertEquals(100, layout.allBubbles().size)
    }

    @Test
    fun allBubbleCenters_areInsideTheSheet() {
        layout.allBubbles().forEach { cell ->
            assertTrue(cell.center.x in 0f..1f)
            assertTrue(cell.center.y in 0f..1f)
        }
    }

    @Test
    fun optionsOfTheSameQuestion_shareTheRow_andGrowToTheRight() {
        val a = layout.bubbleCenter(1, 0)
        val b = layout.bubbleCenter(1, 1)
        assertEquals(a.y, b.y, 0.0001f)
        assertTrue(b.x > a.x)
    }

    @Test
    fun nextQuestion_isBelow_inTheSameColumn() {
        val q1 = layout.bubbleCenter(1, 0)
        val q2 = layout.bubbleCenter(2, 0)
        assertEquals(q1.x, q2.x, 0.0001f)
        assertTrue(q2.y > q1.y)
    }

    @Test
    fun firstQuestionOfSecondColumn_isToTheRight_andBackAtTheTop() {
        // Default profile: 20 questions, 2 columns -> 10 rows per column
        val q1 = layout.bubbleCenter(1, 0)
        val q11 = layout.bubbleCenter(11, 0)
        assertEquals(q1.y, q11.y, 0.0001f)
        assertTrue(q11.x > q1.x)
    }

    @Test
    fun adjacentBubbles_doNotOverlap() {
        val a = layout.bubbleCenter(1, 0)
        val b = layout.bubbleCenter(1, 1)
        assertTrue(b.x - a.x > 2 * layout.bubbleRadius)
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidQuestion_throws() {
        layout.bubbleCenter(21, 0)
    }
}