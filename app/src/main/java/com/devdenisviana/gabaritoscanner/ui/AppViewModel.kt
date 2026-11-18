package com.devdenisviana.gabaritoscanner.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.devdenisviana.gabaritoscanner.data.AppDatabase
import com.devdenisviana.gabaritoscanner.data.ExamEntity
import com.devdenisviana.gabaritoscanner.data.ScanResultEntity
import com.devdenisviana.gabaritoscanner.domain.sheet.SheetProfile
import com.devdenisviana.gabaritoscanner.grading.AnswerKey
import com.devdenisviana.gabaritoscanner.grading.GradeResult
import com.devdenisviana.gabaritoscanner.grading.QuestionOutcome
import com.devdenisviana.gabaritoscanner.scan.Answer
import com.devdenisviana.gabaritoscanner.scan.QuestionReading
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.devdenisviana.gabaritoscanner.data.cloud.AuthRepository
import com.devdenisviana.gabaritoscanner.data.cloud.CloudSync
import kotlinx.coroutines.TimeoutCancellationException

/** Holds the app state and talks to the database (MVVM). */
@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.get(application)
    private val examDao = database.examDao()
    private val resultDao = database.scanResultDao()


    private val auth = AuthRepository()

    /** E-mail of the signed-in teacher, or null when signed out. */
    val userEmail: StateFlow<String?> = auth.userEmail
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun signIn(email: String, password: String, onResult: (String?) -> Unit) =
        auth.signIn(email, password, onResult)

    fun signUp(email: String, password: String, onResult: (String?) -> Unit) =
        auth.signUp(email, password, onResult)

    fun signOut() = auth.signOut()

    private val cloud = CloudSync(examDao, resultDao)

    /** Results still waiting to be uploaded. */
    val unsyncedCount: StateFlow<Int> = resultDao.observeUnsyncedCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /** Uploads pending data to the cloud. [onResult] receives a message to show the user. */
    fun sync(onResult: (String) -> Unit) {
        val userId = auth.userId
        if (userId == null) {
            onResult("Entre na sua conta antes de sincronizar.")
            return
        }
        viewModelScope.launch {
            val message = try {
                val sent = cloud.upload(userId)
                if (sent == 0) {
                    "Tudo já estava sincronizado."
                } else {
                    "Sincronizado: $sent resultado(s) enviado(s)."
                }
            } catch (e: TimeoutCancellationException) {
                "Sem conexão com a internet. Tente de novo mais tarde."
            } catch (e: Exception) {
                "Não foi possível sincronizar: ${e.message}"
            }
            onResult(message)
        }
    }

    /** All exams, newest first. Updates automatically when the table changes. */
    val exams: StateFlow<List<ExamEntity>> = examDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val selectedExamId = MutableStateFlow<Long?>(null)

    /** Results of the exam chosen with [selectExam]. */
    val results: StateFlow<List<ScanResultEntity>> = selectedExamId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList()) else resultDao.observeByExam(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun selectExam(examId: Long?) {
        selectedExamId.value = examId
    }

    /** Creates an exam. Returns an error message to show the user, or null on success. */
    fun createExam(
        name: String,
        answerKeyText: String,
        questionCount: Int = SheetProfile.DEFAULT.questionCount,
        optionsPerQuestion: Int = SheetProfile.DEFAULT.optionsPerQuestion,
    ): String? {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) return "Informe o nome do gabarito."
        if (questionCount !in 1..100) return "O número de questões deve ficar entre 1 e 100."
        if (optionsPerQuestion !in 2..5) return "O número de alternativas deve ficar entre 2 e 5."

        val profile = SheetProfile.of(questionCount, optionsPerQuestion)
        val key = try {
            AnswerKey.parse(answerKeyText, profile)
        } catch (e: IllegalArgumentException) {
            return e.message
        }

        viewModelScope.launch {
            examDao.insert(
                ExamEntity(
                    name = trimmedName,
                    questionCount = profile.questionCount,
                    optionsPerQuestion = profile.optionsPerQuestion,
                    columns = profile.columns,
                    answerKey = key.answers.joinToString(""),
                ),
            )
        }
        return null
    }

    /**
     * Changes the name and the answers of a saved exam (the size of the sheet stays the same).
     * Grades already saved for this exam are recalculated and queued to be uploaded again.
     * Returns an error message to show the user, or null on success.
     */
    fun updateExam(exam: ExamEntity, name: String, answerKeyText: String): String? {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) return "Informe o nome do gabarito."

        val profile = SheetProfile.of(exam.questionCount, exam.optionsPerQuestion)
        val key = try {
            AnswerKey.parse(answerKeyText, profile)
        } catch (e: IllegalArgumentException) {
            return e.message
        }

        viewModelScope.launch {
            examDao.update(
                exam.copy(
                    name = trimmedName,
                    answerKey = key.answers.joinToString(""),
                ),
            )
            val regraded = resultDao.getByExam(exam.id).map { regrade(it, key) }
            if (regraded.isNotEmpty()) resultDao.updateAll(regraded)
        }
        return null
    }

    /** Grades a saved result again against [key], using the answers that were read from the sheet. */
    private fun regrade(result: ScanResultEntity, key: AnswerKey): ScanResultEntity {
        val outcomes = key.answers.mapIndexed { index, correctOption ->
            when (result.readAnswers.getOrElse(index) { '-' }) {
                '-' -> QuestionOutcome.BLANK
                '*' -> QuestionOutcome.MULTIPLE
                correctOption -> QuestionOutcome.CORRECT
                else -> QuestionOutcome.WRONG
            }
        }
        val grade = GradeResult(outcomes)
        return result.copy(
            correct = grade.correct,
            wrong = grade.wrong,
            blank = grade.blank,
            multiple = grade.multiple,
            total = grade.total,
            score = grade.score10,
            syncedAt = null,
        )
    }

    fun deleteExam(exam: ExamEntity) {
        viewModelScope.launch { examDao.delete(exam) }
    }

    fun saveResult(
        exam: ExamEntity,
        studentName: String,
        studentCode: String,
        readings: List<QuestionReading>,
        grade: GradeResult,
    ) {
        val readAnswers = readings.joinToString("") { reading ->
            when (val answer = reading.answer) {
                is Answer.Marked -> answer.option.toString()
                Answer.Blank -> "-"
                is Answer.Multiple -> "*"
            }
        }
        viewModelScope.launch {
            resultDao.insert(
                ScanResultEntity(
                    examId = exam.id,
                    studentName = studentName.trim(),
                    studentCode = studentCode.trim(),
                    readAnswers = readAnswers,
                    correct = grade.correct,
                    wrong = grade.wrong,
                    blank = grade.blank,
                    multiple = grade.multiple,
                    total = grade.total,
                    score = grade.score10,
                ),
            )
        }
    }

    fun deleteResult(result: ScanResultEntity) {
        viewModelScope.launch { resultDao.delete(result) }
    }
}