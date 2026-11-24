package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.QuizzMeRepository
import com.example.data.local.LectureEntity
import com.example.data.local.PastPaperEntity
import com.example.model.QuestionStyle
import com.example.model.QuizJsonParser
import com.example.model.QuizQuestion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PastPaperWithMeta(
    val entity: PastPaperEntity,
    val questions: List<QuizQuestion>,
    val linkedLecture: LectureEntity?
) {
    val displayTitle: String
        get() = linkedLecture?.lesson?.takeIf { it.isNotBlank() }
            ?: entity.lesson.takeIf { it.isNotBlank() }
            ?: entity.subject.takeIf { it.isNotBlank() }
            ?: "Past Paper"

    val displayYear: String
        get() = entity.year.takeIf { it.isNotBlank() }
            ?: questions.firstOrNull { !it.year.isNullOrBlank() }?.year.orEmpty()

    val styleSummary: String
        get() = questions.groupingBy { it.detectedStyle().shortLabel }
            .eachCount()
            .entries
            .joinToString(" • ") { "${it.key}: ${it.value}" }
}

enum class LessonLinkFilter(val label: String) {
    ALL("All"),
    LINKED("Linked"),
    UNLINKED("Unlinked")
}

data class PastPapersUiState(
    val allPapers: List<PastPaperWithMeta> = emptyList(),
    val filteredPapers: List<PastPaperWithMeta> = emptyList(),
    val allLectures: List<LectureEntity> = emptyList(),
    val availableYears: List<String> = emptyList(),
    val searchQuery: String = "",
    val selectedYear: String? = null,
    val linkFilter: LessonLinkFilter = LessonLinkFilter.ALL,
    val selectedLessonIdFilter: String? = null,
    // Inspection / Detail view
    val inspectingPaperId: String? = null,
    // Practice Quiz view
    val practicingPaperId: String? = null,
    val practiceStyle: QuestionStyle = QuestionStyle.MCQ,
    val practiceSession: QuizSessionState = QuizSessionState(),
    // Link Dialog
    val linkingPaperId: String? = null,
    val snackbarMessage: String? = null
) {
    val inspectingPaper: PastPaperWithMeta?
        get() = inspectingPaperId?.let { id -> allPapers.firstOrNull { it.entity.id == id } }

    val practicingPaper: PastPaperWithMeta?
        get() = practicingPaperId?.let { id -> allPapers.firstOrNull { it.entity.id == id } }

    val linkingPaper: PastPaperWithMeta?
        get() = linkingPaperId?.let { id -> allPapers.firstOrNull { it.entity.id == id } }

    val hasActiveFilter: Boolean
        get() = selectedYear != null || linkFilter != LessonLinkFilter.ALL || selectedLessonIdFilter != null
}

private data class PastPapersFilterSnapshot(
    val query: String = "",
    val selectedYear: String? = null,
    val linkFilter: LessonLinkFilter = LessonLinkFilter.ALL,
    val selectedLessonIdFilter: String? = null
)

private data class PastPapersInteractionSnapshot(
    val inspectingPaperId: String? = null,
    val practicingPaperId: String? = null,
    val practiceStyle: QuestionStyle = QuestionStyle.MCQ,
    val practiceSession: QuizSessionState = QuizSessionState(),
    val linkingPaperId: String? = null,
    val snackbarMessage: String? = null
)

class PastPapersViewModel(
    private val repository: QuizzMeRepository
) : ViewModel() {

    private val filterState = MutableStateFlow(PastPapersFilterSnapshot())
    private val interactionState = MutableStateFlow(PastPapersInteractionSnapshot())

    val uiState: StateFlow<PastPapersUiState> = combine(
        repository.allPastPapers,
        repository.allLectures,
        filterState,
        interactionState
    ) { papers, lectures, filters, interaction ->
        val lectureMap = lectures.associateBy { it.id }
        val enriched = papers.map { entity ->
            val questions = runCatching { QuizJsonParser.parseQuestions(entity.questionsJson) }
                .getOrDefault(emptyList())
            val linked = entity.linkedLectureId?.let { lectureMap[it] }
                ?: lectures.firstOrNull {
                    it.id == entity.id ||
                        "paper_${it.id}" == entity.id ||
                        (entity.linkedLectureId == null &&
                            it.lesson.equals(entity.lesson, ignoreCase = true) &&
                            it.subject.equals(entity.subject, ignoreCase = true) &&
                            entity.lesson.isNotBlank())
                }
            PastPaperWithMeta(
                entity = entity,
                questions = questions,
                linkedLecture = linked
            )
        }

        val years = enriched.mapNotNull { it.displayYear.takeIf { y -> y.isNotBlank() } }
            .distinct()
            .sortedDescending()

        val filtered = enriched.filter { item ->
            val q = filters.query.trim()
            val matchesQuery = q.isBlank() ||
                item.displayTitle.contains(q, ignoreCase = true) ||
                item.entity.subject.contains(q, ignoreCase = true) ||
                item.entity.term.contains(q, ignoreCase = true) ||
                item.displayYear.contains(q, ignoreCase = true) ||
                item.questions.any {
                    it.question.contains(q, ignoreCase = true) ||
                        it.lesson.orEmpty().contains(q, ignoreCase = true) ||
                        it.year.orEmpty().contains(q, ignoreCase = true)
                }

            val matchesYear = filters.selectedYear == null ||
                item.displayYear.equals(filters.selectedYear, ignoreCase = true)

            val matchesLink = when (filters.linkFilter) {
                LessonLinkFilter.ALL -> true
                LessonLinkFilter.LINKED -> item.linkedLecture != null
                LessonLinkFilter.UNLINKED -> item.linkedLecture == null
            }

            val matchesLessonFilter = filters.selectedLessonIdFilter == null ||
                item.linkedLecture?.id == filters.selectedLessonIdFilter

            matchesQuery && matchesYear && matchesLink && matchesLessonFilter
        }

        PastPapersUiState(
            allPapers = enriched,
            filteredPapers = filtered,
            allLectures = lectures,
            availableYears = years,
            searchQuery = filters.query,
            selectedYear = filters.selectedYear,
            linkFilter = filters.linkFilter,
            selectedLessonIdFilter = filters.selectedLessonIdFilter,
            inspectingPaperId = interaction.inspectingPaperId,
            practicingPaperId = interaction.practicingPaperId,
            practiceStyle = interaction.practiceStyle,
            practiceSession = interaction.practiceSession,
            linkingPaperId = interaction.linkingPaperId,
            snackbarMessage = interaction.snackbarMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PastPapersUiState()
    )

    fun setSearchQuery(query: String) {
        filterState.update { it.copy(query = query) }
    }

    fun setSelectedYear(year: String?) {
        filterState.update { it.copy(selectedYear = year) }
    }

    fun setLinkFilter(filter: LessonLinkFilter) {
        filterState.update { it.copy(linkFilter = filter) }
    }

    fun setSelectedLessonFilter(lectureId: String?) {
        filterState.update { it.copy(selectedLessonIdFilter = lectureId) }
    }

    fun resetFilters() {
        filterState.value = PastPapersFilterSnapshot()
    }

    fun openPaperDetail(paperId: String) {
        interactionState.update { it.copy(inspectingPaperId = paperId, practicingPaperId = null) }
    }

    fun closePaperDetail() {
        interactionState.update { it.copy(inspectingPaperId = null) }
    }

    fun startPractice(paperId: String) {
        val paper = uiState.value.allPapers.firstOrNull { it.entity.id == paperId } ?: return
        if (paper.questions.isEmpty()) {
            interactionState.update { it.copy(snackbarMessage = "This past paper has no questions yet.") }
            return
        }
        val style = paper.questions.firstOrNull()?.detectedStyle() ?: QuestionStyle.MCQ
        interactionState.update {
            it.copy(
                practicingPaperId = paperId,
                practiceStyle = style,
                practiceSession = QuizSessionState(deck = paper.questions)
            )
        }
    }

    fun stopPractice() {
        interactionState.update {
            it.copy(practicingPaperId = null, practiceSession = QuizSessionState())
        }
    }

    fun restartPractice() {
        interactionState.update { state ->
            state.copy(practiceSession = QuizSessionState(deck = state.practiceSession.deck))
        }
    }

    fun openLinkDialog(paperId: String) {
        interactionState.update { it.copy(linkingPaperId = paperId) }
    }

    fun dismissLinkDialog() {
        interactionState.update { it.copy(linkingPaperId = null) }
    }

    fun linkPaperToLesson(paperId: String, lecture: LectureEntity?) {
        viewModelScope.launch {
            repository.linkPastPaperToLesson(paperId, lecture)
            interactionState.update {
                it.copy(
                    linkingPaperId = null,
                    snackbarMessage = if (lecture != null) {
                        "Linked past paper to \"${lecture.lesson}\""
                    } else {
                        "Unlinked past paper from lesson"
                    }
                )
            }
        }
    }

    fun addOrUpdateQuestion(paperId: String, editIndex: Int?, question: QuizQuestion) {
        val current = uiState.value.allPapers.firstOrNull { it.entity.id == paperId } ?: return
        val mutable = current.questions.toMutableList()
        if (editIndex != null && editIndex in mutable.indices) {
            mutable[editIndex] = question
        } else {
            mutable.add(question)
        }
        viewModelScope.launch {
            repository.updatePastPaperQuestions(paperId, mutable)
            interactionState.update {
                it.copy(
                    snackbarMessage = if (editIndex != null) "Question updated" else "Question added"
                )
            }
        }
    }

    fun deleteQuestion(paperId: String, index: Int) {
        val current = uiState.value.allPapers.firstOrNull { it.entity.id == paperId } ?: return
        val mutable = current.questions.toMutableList()
        if (index !in mutable.indices) return
        mutable.removeAt(index)
        viewModelScope.launch {
            repository.updatePastPaperQuestions(paperId, mutable)
            interactionState.update { it.copy(snackbarMessage = "Question removed") }
        }
    }

    fun deletePastPaper(paperId: String) {
        viewModelScope.launch {
            repository.deletePastPaper(paperId)
            interactionState.update { state ->
                state.copy(
                    inspectingPaperId = if (state.inspectingPaperId == paperId) null else state.inspectingPaperId,
                    practicingPaperId = if (state.practicingPaperId == paperId) null else state.practicingPaperId,
                    snackbarMessage = "Past paper deleted"
                )
            }
        }
    }

    fun clearSnackbar() {
        interactionState.update { it.copy(snackbarMessage = null) }
    }

    // Quiz interaction callbacks
    fun onMcqOptionSelected(option: String) {
        interactionState.update { state ->
            val q = state.practiceSession.currentQuestion ?: return@update state
            state.copy(
                practiceSession = state.practiceSession.selectMcqOption(option, q.resolvedCorrectResponses())
            )
        }
    }

    fun onTfToggle(index: Int, value: Boolean) {
        interactionState.update { state ->
            state.copy(practiceSession = state.practiceSession.toggleTfStatement(index, value))
        }
    }

    fun onEvaluateTf() {
        interactionState.update { state ->
            state.copy(practiceSession = state.practiceSession.evaluateCurrentTf())
        }
    }

    fun onSaInputChange(text: String) {
        interactionState.update { state ->
            state.copy(practiceSession = state.practiceSession.copy(saInputText = text))
        }
    }

    fun onRevealSaAnswer() {
        interactionState.update { state ->
            state.copy(practiceSession = state.practiceSession.copy(saChecked = true))
        }
    }

    fun onGradeSa(isCorrect: Boolean) {
        interactionState.update { state ->
            val scored = if (isCorrect) {
                state.practiceSession.copy(runningScore = state.practiceSession.runningScore + 1)
            } else {
                state.practiceSession
            }
            state.copy(practiceSession = scored.advanceToNextQuestion())
        }
    }

    fun onSeqNotesChange(notes: String) {
        interactionState.update { state ->
            state.copy(practiceSession = state.practiceSession.copy(seqUserNotes = notes))
        }
    }

    fun onToggleSeqAnswer() {
        interactionState.update { state ->
            state.copy(
                practiceSession = state.practiceSession.copy(
                    showSeqAnswer = !state.practiceSession.showSeqAnswer
                )
            )
        }
    }

    fun onGradeSeq(marks: Int) {
        interactionState.update { state ->
            val scored = state.practiceSession.copy(runningScore = state.practiceSession.runningScore + marks)
            state.copy(practiceSession = scored.advanceToNextQuestion())
        }
    }

    fun onNextQuestion() {
        interactionState.update { state ->
            state.copy(practiceSession = state.practiceSession.advanceToNextQuestion())
        }
    }
}
