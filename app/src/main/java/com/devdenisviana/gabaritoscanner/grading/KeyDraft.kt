package com.devdenisviana.gabaritoscanner.grading

import com.devdenisviana.gabaritoscanner.scan.Answer
import com.devdenisviana.gabaritoscanner.scan.QuestionReading

/** Turns what the camera read on a filled key sheet into text for the answer-key field. */
object KeyDraft {
    /** Placed where the mark was blank or doubled, so the teacher can fix it. */
    const val UNSURE = '?'

    fun fromReadings(readings: List<QuestionReading>): String =
        readings.sortedBy { it.question }.joinToString("") { reading ->
            when (val answer = reading.answer) {
                is Answer.Marked -> answer.option.toString()
                else -> UNSURE.toString()
            }
        }
}