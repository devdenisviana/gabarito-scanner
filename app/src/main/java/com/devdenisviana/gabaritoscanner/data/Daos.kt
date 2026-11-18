package com.devdenisviana.gabaritoscanner.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ExamDao {

    @Query("SELECT * FROM exams ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<ExamEntity>>

    @Query("SELECT * FROM exams")
    suspend fun getAll(): List<ExamEntity>

    @Query("SELECT * FROM exams WHERE id = :id")
    suspend fun getById(id: Long): ExamEntity?

    @Insert
    suspend fun insert(exam: ExamEntity): Long

    @Update
    suspend fun update(exam: ExamEntity)

    @Delete
    suspend fun delete(exam: ExamEntity)
}

@Dao
interface ScanResultDao {

    @Query("SELECT * FROM scan_results WHERE examId = :examId ORDER BY scannedAt DESC")
    fun observeByExam(examId: Long): Flow<List<ScanResultEntity>>

    @Query("SELECT * FROM scan_results WHERE examId = :examId")
    suspend fun getByExam(examId: Long): List<ScanResultEntity>

    @Query("SELECT * FROM scan_results WHERE syncedAt IS NULL")
    suspend fun getUnsynced(): List<ScanResultEntity>

    @Query("SELECT COUNT(*) FROM scan_results WHERE syncedAt IS NULL")
    fun observeUnsyncedCount(): Flow<Int>

    @Query("UPDATE scan_results SET syncedAt = :time WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<Long>, time: Long)

    @Insert
    suspend fun insert(result: ScanResultEntity): Long

    @Update
    suspend fun updateAll(results: List<ScanResultEntity>)

    @Delete
    suspend fun delete(result: ScanResultEntity)
}