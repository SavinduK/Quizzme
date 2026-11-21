package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lectures")
data class LectureEntity(
    @PrimaryKey val id: String,
    val lesson: String,
    val subject: String,
    val term: String,
    val markdownContent: String,
    val summaryMarkdown: String? = null,
    val cachedQuizJson: String? = null,
    val cachedQuizStyle: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "past_papers")
data class PastPaperEntity(
    @PrimaryKey val id: String,
    val lesson: String,
    val subject: String,
    val term: String,
    val questionsJson: String,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "tracker_tasks")
data class TrackerTaskEntity(
    @PrimaryKey val id: String,
    val date: String,
    val time: String,
    val title: String,
    val subject: String,
    val completed: Boolean = false,
    val isCustom: Boolean = false
)
