package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LectureDao {
    @Query("SELECT * FROM lectures ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<LectureEntity>>

    @Query("SELECT * FROM lectures")
    suspend fun getAllOnce(): List<LectureEntity>

    @Query("SELECT * FROM lectures WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<LectureEntity?>

    @Query("SELECT * FROM lectures WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): LectureEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(lecture: LectureEntity)

    @Query("DELETE FROM lectures WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface PastPaperDao {
    @Query("SELECT * FROM past_papers ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<PastPaperEntity>>

    @Query("SELECT * FROM past_papers")
    suspend fun getAllOnce(): List<PastPaperEntity>

    @Query("SELECT * FROM past_papers WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<PastPaperEntity?>

    @Query("SELECT * FROM past_papers WHERE id = :id OR id = :altId LIMIT 1")
    fun observeByIdOrAlt(id: String, altId: String): Flow<PastPaperEntity?>

    @Query("SELECT * FROM past_papers WHERE linkedLectureId = :lectureId ORDER BY updatedAt DESC")
    fun observeByLinkedLectureId(lectureId: String): Flow<List<PastPaperEntity>>

    @Query("SELECT * FROM past_papers WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): PastPaperEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(paper: PastPaperEntity)

    @Query("DELETE FROM past_papers WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM past_papers WHERE id = :id OR id = :altId")
    suspend fun deleteByIdOrAlt(id: String, altId: String)
}

@Dao
interface TrackerTaskDao {
    @Query("SELECT * FROM tracker_tasks ORDER BY date ASC, time ASC")
    fun observeAll(): Flow<List<TrackerTaskEntity>>

    @Query("SELECT COUNT(*) FROM tracker_tasks")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(tasks: List<TrackerTaskEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(task: TrackerTaskEntity)

    @Query("UPDATE tracker_tasks SET completed = :completed WHERE id = :id")
    suspend fun setCompleted(id: String, completed: Boolean)

    @Query("DELETE FROM tracker_tasks WHERE id = :id")
    suspend fun deleteById(id: String)
}
