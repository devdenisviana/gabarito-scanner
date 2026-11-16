package com.devdenisviana.gabaritoscanner.grading

import com.devdenisviana.gabaritoscanner.scan.Answer
import com.devdenisviana.gabaritoscanner.scan.QuestionReading
import org.junit.Assert.assertEquals
import org.junit.Test

class KeyDraftTest {

    private fun reading(question: Int, answer: Answer) =
        QuestionReading(question = question, answer = answer, darkness = emptyList())

    @Test
    fun `marcacoes viram letras`() {
        val readings = listOf(
            reading(0, Answer.Marked(0, 'A')),
            reading(1, Answer.Marked(2, 'C')),
            reading(2, Answer.Marked(1, 'B')),
        )
        assertEquals("ACB", KeyDraft.fromReadings(readings))
    }

    @Test
    fun `em branco e duplas viram interrogacao`() {
        val readings = listOf(
            reading(0, Answer.Marked(3, 'D')),
            reading(1, Answer.Blank),
            reading(2, Answer.Multiple(listOf(0, 1))),
        )
        assertEquals("D??", KeyDraft.fromReadings(readings))
    }

    @Test
    fun `respeita a ordem das questoes`() {
        val readings = listOf(
            reading(1, Answer.Marked(1, 'B')),
            reading(0, Answer.Marked(0, 'A')),
        )
        assertEquals("AB", KeyDraft.fromReadings(readings))
    }
}