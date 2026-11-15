package com.devdenisviana.gabaritoscanner.grading

import com.devdenisviana.gabaritoscanner.domain.sheet.SheetProfile
import com.devdenisviana.gabaritoscanner.scan.Answer
import com.devdenisviana.gabaritoscanner.scan.QuestionReading
import org.junit.Assert.assertEquals
import org.junit.Test

class GraderTest {

    private val profile = SheetProfile.DEFAULT

    private fun marked(option: Char): Answer = Answer.Marked(option - 'A', option)

    private fun readings(answers: List<Answer>) =
        answers.mapIndexed { index, answer -> QuestionReading(index + 1, answer, emptyList()) }

    @Test
    fun allCorrect_scoresTen() {
        val key = AnswerKey.parse("ABCDEABCDEABCDEABCDE", profile)
        val sheet = readings(key.answers.map { marked(it) })

        val result = Grader.grade(key, sheet)

        assertEquals(20, result.correct)
        assertEquals(0, result.errors)
        assertEquals(10.0, result.score10, 0.0001)
    }

    @Test
    fun allBlank_scoresZero() {
        val key = AnswerKey.parse("ABCDEABCDEABCDEABCDE", profile)
        val sheet = readings(List(20) { Answer.Blank })

        val result = Grader.grade(key, sheet)

        assertEquals(0, result.correct)
        assertEquals(20, result.blank)
        assertEquals(20, result.errors)
        assertEquals(0.0, result.score10, 0.0001)
    }

    @Test
    fun realScannedSheet_isGradedCorrectly() {
        // What the scanner read from the real printed sheet used in manual tests.
        val sheet = readings(
            listOf(
                marked('A'), marked('B'), marked('C'), marked('D'), marked('E'),
                Answer.Blank,
                Answer.Multiple(listOf(0, 1)),
                marked('C'), marked('A'), marked('B'), marked('B'), marked('C'), marked('B'),
                marked('D'), marked('B'), marked('A'), marked('E'), marked('E'), marked('B'), marked('D'),
            ),
        )
        val key = AnswerKey.parse("ABCDEABCBBBABDBAEDBD", profile)

        val result = Grader.grade(key, sheet)

        assertEquals(15, result.correct)
        assertEquals(3, result.wrong)
        assertEquals(1, result.blank)
        assertEquals(1, result.multiple)
        assertEquals(5, result.errors)
        assertEquals(7.5, result.score10, 0.0001)

        assertEquals(QuestionOutcome.BLANK, result.outcomes[5])
        assertEquals(QuestionOutcome.MULTIPLE, result.outcomes[6])
        assertEquals(QuestionOutcome.WRONG, result.outcomes[8])
        assertEquals(QuestionOutcome.WRONG, result.outcomes[11])
        assertEquals(QuestionOutcome.WRONG, result.outcomes[17])
    }

    @Test
    fun parse_acceptsLowercaseAndSpaces() {
        val key = AnswerKey.parse("abcde abcde\nabcde abcde", profile)

        assertEquals("ABCDEABCDEABCDEABCDE", key.answers.joinToString(""))
    }

    @Test(expected = IllegalArgumentException::class)
    fun parse_rejectsWrongLength() {
        AnswerKey.parse("ABCDE", profile)
    }

    @Test(expected = IllegalArgumentException::class)
    fun parse_rejectsLetterOutsideTheOptions() {
        // 'F' is not a valid alternative for a sheet with options A-E
        AnswerKey.parse("ABCDEABCDEABCDEABCDF", profile)
    }

    @Test(expected = IllegalArgumentException::class)
    fun grade_rejectsKeyAndReadingsOfDifferentSize() {
        val key = AnswerKey(listOf('A', 'B'))
        Grader.grade(key, readings(List(3) { Answer.Blank }))
    }
}