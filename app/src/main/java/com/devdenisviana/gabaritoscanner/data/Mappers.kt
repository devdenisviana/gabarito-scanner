package com.devdenisviana.gabaritoscanner.data

import com.devdenisviana.gabaritoscanner.domain.sheet.SheetProfile
import com.devdenisviana.gabaritoscanner.grading.AnswerKey

/** The sheet layout this exam was registered with. */
fun ExamEntity.toSheetProfile(): SheetProfile = SheetProfile(
    id = "exam-$id",
    name = name,
    questionCount = questionCount,
    optionsPerQuestion = optionsPerQuestion,
    columns = columns,
)

/** The official answers of this exam. */
fun ExamEntity.toAnswerKey(): AnswerKey = AnswerKey.parse(answerKey, toSheetProfile())