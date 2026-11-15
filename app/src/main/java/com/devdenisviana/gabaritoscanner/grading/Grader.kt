package com.devdenisviana.gabaritoscanner.grading

import com.devdenisviana.gabaritoscanner.domain.sheet.SheetProfile
import com.devdenisviana.gabaritoscanner.scan.Answer
import com.devdenisviana.gabaritoscanner.scan.QuestionReading

/** The official answer of each question (index 0 = question 1). */
data class AnswerKey(val answers: List<Char>) {

    companion object {
        /**
         * Parses text like "ABCDE ABCDE" (spaces and lowercase are accepted).
         * Throws [IllegalArgumentException] with a user-friendly message if it does not fit [profile].
         */
        fun parse(text: String, profile: SheetProfile): AnswerKey {
            val cleaned = text.filter { !it.isWhitespace() }.uppercase()

            require(cleaned.length == profile.questionCount) {
                "O gabarito precisa ter ${profile.questionCount} respostas (tem ${cleaned.length})."
            }

            val invalid = cleaned.firstOrNull { it !in profile.optionLabels }
            require(invalid == null) {
                "Alternativa inválida: $invalid. Use apenas ${profile.optionLabels.joinToString(", ")}."
            }

            return AnswerKey(cleaned.toList())
        }
    }
}

enum class QuestionOutcome { CORRECT, WRONG, BLANK, MULTIPLE }

/** Outcome of every question, plus the summary numbers derived from it. */
data class GradeResult(val outcomes: List<QuestionOutcome>) {

    val total: Int get() = outcomes.size
    val correct: Int get() = outcomes.count { it == QuestionOutcome.CORRECT }
    val wrong: Int get() = outcomes.count { it == QuestionOutcome.WRONG }
    val blank: Int get() = outcomes.count { it == QuestionOutcome.BLANK }
    val multiple: Int get() = outcomes.count { it == QuestionOutcome.MULTIPLE }

    /** Every question that was not answered correctly (wrong + blank + multiple). */
    val errors: Int get() = total - correct

    /** Score from 0 to 10. */
    val score10: Double get() = if (total == 0) 0.0 else correct * 10.0 / total
}

object Grader {

    fun grade(key: AnswerKey, readings: List<QuestionReading>): GradeResult {
        require(key.answers.size == readings.size) {
            "O gabarito tem ${key.answers.size} respostas, mas foram lidas ${readings.size} questões."
        }

        val outcomes = readings.mapIndexed { index, reading ->
            when (val answer = reading.answer) {
                is Answer.Marked ->
                    if (answer.option == key.answers[index]) QuestionOutcome.CORRECT else QuestionOutcome.WRONG
                Answer.Blank -> QuestionOutcome.BLANK
                is Answer.Multiple -> QuestionOutcome.MULTIPLE
            }
        }
        return GradeResult(outcomes)
    }
}