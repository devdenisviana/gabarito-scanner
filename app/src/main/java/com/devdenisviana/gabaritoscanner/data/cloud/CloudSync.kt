package com.devdenisviana.gabaritoscanner.data.cloud

import com.devdenisviana.gabaritoscanner.data.ExamDao
import com.devdenisviana.gabaritoscanner.data.ExamEntity
import com.devdenisviana.gabaritoscanner.data.ScanResultDao
import com.devdenisviana.gabaritoscanner.data.ScanResultEntity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.WriteBatch
import kotlinx.coroutines.withTimeout


/**
 * Uploads exams and not-yet-synced results to Firestore, under
 * teachers/{userId}/exams and teachers/{userId}/results.
 * Document ids are built from creation times, so sending the same data twice is harmless.
 */
class CloudSync(
    private val examDao: ExamDao,
    private val resultDao: ScanResultDao,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {

    /**
     * Sends everything that is pending and returns how many results were sent.
     * Throws [kotlinx.coroutines.TimeoutCancellationException] when there is no connection.
     */
    suspend fun upload(userId: String): Int {
        val exams = examDao.getAll()
        val examsById = exams.associateBy { it.id }
        val pending = resultDao.getUnsynced()
        val teacher = firestore.collection("teachers").document(userId)

        val operations = mutableListOf<(WriteBatch) -> Unit>()
        for (exam in exams) {
            val document = teacher.collection("exams").document(exam.createdAt.toString())
            operations += { batch -> batch.set(document, examData(exam)) }
        }

        val sentIds = mutableListOf<Long>()
        for (result in pending) {
            val exam = examsById[result.examId] ?: continue
            val document = teacher.collection("results")
                .document("${exam.createdAt}_${result.scannedAt}")
            operations += { batch -> batch.set(document, resultData(exam, result)) }
            sentIds += result.id
        }

        withTimeout(TIMEOUT_MS) {
            for (chunk in operations.chunked(BATCH_SIZE)) {
                val batch = firestore.batch()
                chunk.forEach { it(batch) }
                batch.commit().awaitResult()
            }
        }

        if (sentIds.isNotEmpty()) {
            resultDao.markSynced(sentIds, System.currentTimeMillis())
        }
        return sentIds.size
    }

    private fun examData(exam: ExamEntity): Map<String, Any> = mapOf(
        "name" to exam.name,
        "questionCount" to exam.questionCount,
        "optionsPerQuestion" to exam.optionsPerQuestion,
        "columns" to exam.columns,
        "answerKey" to exam.answerKey,
        "createdAt" to exam.createdAt,
    )

    private fun resultData(exam: ExamEntity, result: ScanResultEntity): Map<String, Any> = mapOf(
        "examId" to exam.createdAt.toString(),
        "examName" to exam.name,
        "studentName" to result.studentName,
        "studentCode" to result.studentCode,
        "readAnswers" to result.readAnswers,
        "correct" to result.correct,
        "wrong" to result.wrong,
        "blank" to result.blank,
        "multiple" to result.multiple,
        "total" to result.total,
        "score" to result.score,
        "scannedAt" to result.scannedAt,
    )

    private companion object {
        const val BATCH_SIZE = 400
        const val TIMEOUT_MS = 20_000L
    }
}