package com.devdenisviana.gabaritoscanner.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ExamDao {

    @Query("SELECT * FROM exams ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<ExamEntity>>

    @Query("SELECT * FROM exams WHERE id = :id")
    suspend fun getById(id: Long): ExamEntity?

    @Insert
    suspend fun insert(exam: ExamEntity): Long

    @Delete
    suspend fun delete(exam: ExamEntity)
}

@Dao
interface ScanResultDao {

    @Query("SELECT * FROM scan_results WHERE examId = :examId ORDER BY scannedAt DESC")
    fun observeByExam(examId: Long): Flow<List<ScanResultEntity>>

    @Insert
    suspend fun insert(result: ScanResultEntity): Long

    @Delete
    suspend fun delete(result: ScanResultEntity)
}