package com.devdenisviana.gabaritoscanner.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ExamEntity::class, ScanResultEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun examDao(): ExamDao
    abstract fun scanResultDao(): ScanResultDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        /** Returns the single database instance, creating it on first use. */
        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gabarito.db",
                ).build().also { instance = it }
            }
    }
}