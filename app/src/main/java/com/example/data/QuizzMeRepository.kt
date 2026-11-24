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

    fun observePastPaper(id: String): Flow<PastPaperEntity?> =
        pastPaperDao.observeByIdOrAlt(id, "paper_$id")

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
        year: String = "",
        linkedLectureId: String? = null,
        questions: List<QuizQuestion>
    ): PastPaperEntity {
        val cleanLesson = lesson.trim()
        val cleanSubject = subject.trim()
        val cleanTerm = term.trim()
        val cleanYear = year.trim()
        val normalizedQuestions = questions.map { q ->
            q.copy(
                year = q.year?.takeIf { it.isNotBlank() } ?: cleanYear.ifBlank { null },
                lesson = q.lesson?.takeIf { it.isNotBlank() } ?: cleanLesson.ifBlank { null }
            )
        }
        val paperId = id ?: buildSlugId(
            cleanLesson.ifBlank { cleanSubject },
            cleanSubject.ifBlank { "general" },
            cleanYear.ifBlank { cleanTerm }.ifBlank { System.currentTimeMillis().toString() }
        )
        val entity = PastPaperEntity(
            id = paperId,
            lesson = cleanLesson,
            subject = cleanSubject,
            term = cleanTerm,
            year = cleanYear,
            linkedLectureId = linkedLectureId,
            questionsJson = QuizJsonParser.encodeDeck(normalizedQuestions),
            updatedAt = System.currentTimeMillis()
        )
        pastPaperDao.upsert(entity)
        return entity
    }

    suspend fun linkPastPaperToLesson(paperId: String, lecture: LectureEntity?) {
        val paper = pastPaperDao.getById(paperId) ?: return
        val existingQuestions = runCatching { QuizJsonParser.parseQuestions(paper.questionsJson) }
            .getOrDefault(emptyList())
        val updatedQuestions = if (lecture != null) {
            existingQuestions.map { q -> q.copy(lesson = lecture.lesson) }
        } else {
            existingQuestions
        }
        pastPaperDao.upsert(
            paper.copy(
                linkedLectureId = lecture?.id,
                lesson = lecture?.lesson ?: paper.lesson,
                subject = lecture?.subject ?: paper.subject,
                term = lecture?.term ?: paper.term,
                questionsJson = QuizJsonParser.encodeDeck(updatedQuestions),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun updatePastPaperQuestions(paperId: String, questions: List<QuizQuestion>) {
        val paper = pastPaperDao.getById(paperId) ?: return
        val normalized = questions.map { q ->
            q.copy(
                year = q.year?.takeIf { it.isNotBlank() } ?: paper.year.ifBlank { null },
                lesson = q.lesson?.takeIf { it.isNotBlank() } ?: paper.lesson.ifBlank { null }
            )
        }
        pastPaperDao.upsert(
            paper.copy(
                questionsJson = QuizJsonParser.encodeDeck(normalized),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deletePastPaper(id: String) {
        pastPaperDao.deleteByIdOrAlt(id, "paper_$id")
    }

    suspend fun getReferencePastPapersForLecture(lecture: LectureEntity): List<QuizQuestion> {
        val allPapers = pastPaperDao.getAllOnce()
        if (allPapers.isEmpty()) return emptyList()

        val exactMatches = allPapers.filter { paper ->
            paper.linkedLectureId == lecture.id ||
                paper.id == lecture.id ||
                paper.id == "paper_${lecture.id}" ||
                (paper.lesson.equals(lecture.lesson, ignoreCase = true) &&
                    paper.subject.equals(lecture.subject, ignoreCase = true))
        }
        val targetPapers = if (exactMatches.isNotEmpty()) {
            exactMatches
        } else {
            allPapers.filter { it.subject.equals(lecture.subject, ignoreCase = true) }
        }

        return targetPapers.flatMap { paper ->
            runCatching { QuizJsonParser.parseQuestions(paper.questionsJson) }
                .getOrDefault(emptyList())
        }
    }

    suspend fun compileHomeSessionPool(
        sourceMode: SourceMode,
        selectedSubject: String?,
        selectedTerm: String?,
        selectedType: QuestionStyle,
        maxQuestions: Int,
        selectedLesson: String? = null
    ): List<QuizQuestion> {
        val collected = mutableListOf<QuizQuestion>()
        if (sourceMode == SourceMode.AI) {
            val lectures = lectureDao.getAllOnce()
            for (lecture in lectures) {
                val matchSubject = selectedSubject == null ||
                    lecture.subject.equals(selectedSubject, ignoreCase = true)
                val matchTerm = selectedTerm == null ||
                    lecture.term.equals(selectedTerm, ignoreCase = true)
                val matchLesson = selectedLesson == null ||
                    lecture.lesson.equals(selectedLesson, ignoreCase = true)
                val rawJson = lecture.cachedQuizJson
                if (matchSubject && matchTerm && matchLesson && !rawJson.isNullOrBlank()) {
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
                val matchLesson = selectedLesson == null ||
                    paper.lesson.equals(selectedLesson, ignoreCase = true)
                if (matchSubject && matchTerm && matchLesson && paper.questionsJson.isNotBlank()) {
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
