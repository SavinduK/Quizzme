package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        LectureEntity::class,
        PastPaperEntity::class,
        TrackerTaskEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun lectureDao(): LectureDao
    abstract fun pastPaperDao(): PastPaperDao
    abstract fun trackerTaskDao(): TrackerTaskDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "quizzme_db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
