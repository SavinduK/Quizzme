package com.example.data

import com.example.data.local.LectureDao
import com.example.data.local.LectureEntity
import com.example.data.local.PastPaperDao
import com.example.data.local.PastPaperEntity
import com.example.data.local.TrackerTaskDao
import com.example.data.local.TrackerTaskEntity
import com.example.model.QuestionStyle
import com.example.model.QuizJsonParser
import com.example.model.QuizQuestion
import com.example.model.QuizSettings
import com.example.model.SourceMode
import com.example.model.TrackerTaskJsonItem
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import java.time.LocalDate

class QuizzMeRepository(
    private val lectureDao: LectureDao,
    private val pastPaperDao: PastPaperDao,
    private val trackerTaskDao: TrackerTaskDao,
    val settingsRepository: SettingsRepository,
    val aiRepository: AiRepository
) {
    val allLectures: Flow<List<LectureEntity>> = lectureDao.observeAll()
    val allPastPapers: Flow<List<PastPaperEntity>> = pastPaperDao.observeAll()
    val allTasks: Flow<List<TrackerTaskEntity>> = trackerTaskDao.observeAll()
    val settingsFlow: Flow<QuizSettings> = settingsRepository.settingsFlow

    fun observeLecture(id: String): Flow<LectureEntity?> = lectureDao.observeById(id)

    fun observePastPaper(id: String): Flow<PastPaperEntity?> = pastPaperDao.observeById(id)

    suspend fun getLecture(id: String): LectureEntity? = lectureDao.getById(id)

    suspend fun getPastPaper(id: String): PastPaperEntity? = pastPaperDao.getById(id)

    suspend fun saveLecture(
        lesson: String,
        subject: String,
        term: String,
        markdownContent: String,
        existingId: String? = null
    ): LectureEntity {
        val cleanLesson = lesson.trim()
        val cleanSubject = subject.trim()
        val cleanTerm = term.trim()
        val id = existingId ?: buildSlugId(cleanLesson, cleanSubject, cleanTerm)
        val existing = lectureDao.getById(id)
        val entity = LectureEntity(
            id = id,
            lesson = cleanLesson,
            subject = cleanSubject,
            term = cleanTerm,
            markdownContent = markdownContent,
            summaryMarkdown = if (existing?.markdownContent == markdownContent) existing.summaryMarkdown else null,
            cachedQuizJson = existing?.cachedQuizJson,
            cachedQuizStyle = existing?.cachedQuizStyle,
            updatedAt = System.currentTimeMillis()
        )
        lectureDao.upsert(entity)
        return entity
    }

    suspend fun updateLectureContent(id: String, newMarkdown: String) {
        val existing = lectureDao.getById(id) ?: return
        lectureDao.upsert(
            existing.copy(
                markdownContent = newMarkdown,
                summaryMarkdown = null,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun cacheLectureSummary(id: String, summaryMarkdown: String) {
        val existing = lectureDao.getById(id) ?: return
        lectureDao.upsert(
            existing.copy(
                summaryMarkdown = summaryMarkdown,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun cacheLectureQuiz(id: String, questions: List<QuizQuestion>, style: QuestionStyle) {
        val existing = lectureDao.getById(id) ?: return
        val encoded = QuizJsonParser.encodeDeck(questions)
        lectureDao.upsert(
            existing.copy(
                cachedQuizJson = encoded,
                cachedQuizStyle = style.name,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteLecture(id: String) {
        lectureDao.deleteById(id)
    }

    suspend fun savePastPaper(
        id: String? = null,
        lesson: String,
        subject: String,
        term: String,
        questions: List<QuizQuestion>
    ): PastPaperEntity {
        val cleanLesson = lesson.trim()
        val cleanSubject = subject.trim()
        val cleanTerm = term.trim()
        val paperId = id ?: buildSlugId(cleanLesson, cleanSubject, cleanTerm)
        val entity = PastPaperEntity(
            id = paperId,
            lesson = cleanLesson,
            subject = cleanSubject,
            term = cleanTerm,
            questionsJson = QuizJsonParser.encodeDeck(questions),
            updatedAt = System.currentTimeMillis()
        )
        pastPaperDao.upsert(entity)
        return entity
    }

    suspend fun deletePastPaper(id: String) {
        pastPaperDao.deleteById(id)
    }

    suspend fun compileHomeSessionPool(
        sourceMode: SourceMode,
        selectedSubject: String?,
        selectedTerm: String?,
        selectedType: QuestionStyle,
        maxQuestions: Int
    ): List<QuizQuestion> {
        val collected = mutableListOf<QuizQuestion>()
        if (sourceMode == SourceMode.AI) {
            val lectures = lectureDao.getAllOnce()
            for (lecture in lectures) {
                val matchSubject = selectedSubject == null ||
                    lecture.subject.equals(selectedSubject, ignoreCase = true)
                val matchTerm = selectedTerm == null ||
                    lecture.term.equals(selectedTerm, ignoreCase = true)
                val rawJson = lecture.cachedQuizJson
                if (matchSubject && matchTerm && !rawJson.isNullOrBlank()) {
                    runCatching { QuizJsonParser.parseQuestions(rawJson) }
                        .getOrNull()
                        ?.filter { it.detectedStyle() == selectedType }
                        ?.let { collected.addAll(it) }
                }
            }
        } else {
            val papers = pastPaperDao.getAllOnce()
            for (paper in papers) {
                val matchSubject = selectedSubject == null ||
                    paper.subject.equals(selectedSubject, ignoreCase = true)
                val matchTerm = selectedTerm == null ||
                    paper.term.equals(selectedTerm, ignoreCase = true)
                if (matchSubject && matchTerm && paper.questionsJson.isNotBlank()) {
                    runCatching { QuizJsonParser.parseQuestions(paper.questionsJson) }
                        .getOrNull()
                        ?.filter { it.detectedStyle() == selectedType }
                        ?.let { collected.addAll(it) }
                }
            }
        }
        return collected.shuffled().take(maxQuestions.coerceAtLeast(1))
    }

    suspend fun ensureInitialTasksSeeded() {
        if (settingsRepository.isInitialTasksSeeded()) return
        if (trackerTaskDao.count() == 0) {
            val today = LocalDate.now().toString()
            trackerTaskDao.upsertAll(
                listOf(
                    TrackerTaskEntity(
                        id = "tt-1",
                        date = today,
                        time = "08:00 AM - 09:30 AM",
                        title = "Cardiovascular System Review",
                        subject = "Physiology",
                        completed = false
                    ),
                    TrackerTaskEntity(
                        id = "tt-2",
                        date = today,
                        time = "10:00 AM - 11:00 AM",
                        title = "Complete Daily MCQ Flash-Quiz",
                        subject = "Physiology",
                        completed = true
                    ),
                    TrackerTaskEntity(
                        id = "tt-3",
                        date = today,
                        time = "02:00 PM - 03:30 PM",
                        title = "Neuroanatomy Diagrams Practice",
                        subject = "Anatomy",
                        completed = false
                    )
                )
            )
        }
        settingsRepository.markInitialTasksSeeded()
    }

    suspend fun toggleTaskCompleted(id: String, currentCompleted: Boolean) {
        trackerTaskDao.setCompleted(id, !currentCompleted)
    }

    suspend fun addTask(date: String, title: String, subject: String, time: String) {
        val task = TrackerTaskEntity(
            id = "custom-${System.currentTimeMillis()}",
            date = date,
            time = time.trim().ifBlank { "Flex Time" },
            title = title.trim(),
            subject = subject.trim().ifBlank { "General Study" },
            completed = false,
            isCustom = true
        )
        trackerTaskDao.upsert(task)
    }

    suspend fun importTasksFromJson(rawJson: String, fallbackDate: String): Int {
        val cleaned = QuizJsonParser.cleanJsonInput(rawJson)
        val element = QuizJsonParser.json.parseToJsonElement(cleaned)
        val items: List<TrackerTaskJsonItem> = when (element) {
            is JsonArray -> QuizJsonParser.json.decodeFromJsonElement(
                kotlinx.serialization.builtins.ListSerializer(TrackerTaskJsonItem.serializer()),
                element
            )
            is JsonObject -> listOf(
                QuizJsonParser.json.decodeFromJsonElement(TrackerTaskJsonItem.serializer(), element)
            )
            else -> emptyList()
        }
        val now = System.currentTimeMillis()
        val entities = items.mapIndexed { idx, item ->
            TrackerTaskEntity(
                id = item.id?.takeIf { it.isNotBlank() } ?: "imported-$now-$idx",
                date = item.date?.takeIf { it.isNotBlank() } ?: fallbackDate,
                time = item.time?.takeIf { it.isNotBlank() } ?: "Flex Time",
                title = item.title?.takeIf { it.isNotBlank() } ?: "Untitled Task",
                subject = item.subject?.takeIf { it.isNotBlank() } ?: "General",
                completed = item.completed ?: false,
                isCustom = item.isCustom ?: true
            )
        }
        trackerTaskDao.upsertAll(entities)
        return entities.size
    }

    suspend fun deleteTask(id: String) {
        trackerTaskDao.deleteById(id)
    }

    companion object {
        fun buildSlugId(lesson: String, subject: String, term: String): String {
            fun slug(s: String) = s.trim().lowercase().replace(Regex("\\s+"), "_")
            return "${slug(lesson)}-${slug(subject)}-${slug(term)}"
        }
    }
}
