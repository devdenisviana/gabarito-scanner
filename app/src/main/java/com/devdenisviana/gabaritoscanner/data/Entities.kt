package com.devdenisviana.gabaritoscanner.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** An exam: its sheet layout and the official answer key. */
@Entity(tableName = "exams")
data class ExamEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val questionCount: Int,
    val optionsPerQuestion: Int,
    val columns: Int,
    /** Official answers, one letter per question, e.g. "ABCDE...". */
    val answerKey: String,
    val createdAt: Long = System.currentTimeMillis(),
)

/** One graded answer sheet of a student. Deleted together with its exam. */
@Entity(
    tableName = "scan_results",
    foreignKeys = [
        ForeignKey(
            entity = ExamEntity::class,
            parentColumns = ["id"],
            childColumns = ["examId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("examId")],
)
data class ScanResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val examId: Long,
    val studentName: String,
    val studentCode: String,
    /** What was read, one character per question: the letter, '-' for blank, '*' for multiple. */
    val readAnswers: String,
    val correct: Int,
    val wrong: Int,
    val blank: Int,
    val multiple: Int,
    val total: Int,
    val score: Double,
    val scannedAt: Long = System.currentTimeMillis(),
    /** When this result was uploaded to the cloud; null means not synced yet. */
    val syncedAt: Long? = null,
)