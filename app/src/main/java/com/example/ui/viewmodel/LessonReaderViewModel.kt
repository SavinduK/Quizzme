package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.QuizzMeRepository
import com.example.data.local.LectureEntity
import com.example.model.QuestionStyle
import com.example.model.QuizJsonParser
import com.example.ui.components.stripHtmlAndCssTags
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ReaderTab(val title: String) {
    NOTES("Notes"),
    QUIZ("Quiz"),
    SUMMARY("Summary")
}

data class LessonReaderUiState(
    val loading: Boolean = true,
    val lecture: LectureEntity? = null,
    val activeTab: ReaderTab = ReaderTab.NOTES,
    // Quiz Tab
    val loadingQuiz: Boolean = false,
    val quizError: String? = null,
    val quizNotice: String? = null,
    val quizStyle: QuestionStyle = QuestionStyle.MCQ,
    val quizSession: QuizSessionState = QuizSessionState(),
    // Summary Tab
    val loadingSummary: Boolean = false,
    val summaryStatus: String = "Checking context...",
    val summaryText: String = "",
    val summaryError: String? = null,
    // Editor
    val isSavingEdit: Boolean = false,
    val snackbarMessage: String? = null
)

class LessonReaderViewModel(
    private val lectureId: String,
    initialTab: ReaderTab,
    private val repository: QuizzMeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LessonReaderUiState(activeTab = initialTab))
    val uiState: StateFlow<LessonReaderUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeLecture(lectureId).collect { entity ->
                _uiState.update { state ->
                    state.copy(
                        loading = false,
                        lecture = entity,
                        summaryText = entity?.summaryMarkdown ?: state.summaryText
                    )
                }
            }
        }

        if (initialTab == ReaderTab.QUIZ) {
            launchQuiz(forceRegenerate = false)
        } else if (initialTab == ReaderTab.SUMMARY) {
            loadOrGenerateSummary()
        }
    }

    fun selectTab(tab: ReaderTab) {
        _uiState.update { it.copy(activeTab = tab) }
        when (tab) {
            ReaderTab.QUIZ -> {
                if (_uiState.value.quizSession.deck.isEmpty() && !_uiState.value.loadingQuiz) {
                    launchQuiz(forceRegenerate = false)
                }
            }
            ReaderTab.SUMMARY -> {
                if (_uiState.value.summaryText.isBlank() && !_uiState.value.loadingSummary) {
                    loadOrGenerateSummary()
                }
            }
            else -> Unit
        }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun saveEditedNotes(newNotesText: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSavingEdit = true) }
            try {
                val cleanPlainText = stripHtmlAndCssTags(newNotesText)
                repository.updateLectureContent(lectureId, cleanPlainText)
                _uiState.update {
                    it.copy(
                        isSavingEdit = false,
                        summaryText = "",
                        snackbarMessage = "Lesson content updated."
                    )
                }
                onSuccess()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSavingEdit = false,
                        snackbarMessage = e.localizedMessage ?: "Failed to save edits."
                    )
                }
            }
        }
    }

    fun launchQuiz(forceRegenerate: Boolean = true) {
        viewModelScope.launch {
            val lecture = repository.getLecture(lectureId) ?: return@launch
            val settings = repository.settingsRepository.getSettingsOnce()
            _uiState.update {
                it.copy(
                    loadingQuiz = true,
                    quizError = null,
                    quizNotice = null,
                    quizStyle = settings.questionStyle
                )
            }

            try {
                val referenceQuestions = repository.getReferencePastPapersForLecture(lecture)
                val questions = repository.aiRepository.generateQuiz(
                    sourceText = lecture.markdownContent,
                    count = settings.questionCount,
                    style = settings.questionStyle,
                    customPrompt = settings.customPrompt,
                    referencePastPaperQuestions = referenceQuestions
                )
                repository.cacheLectureQuiz(lectureId, questions, settings.questionStyle)
                val notice = if (referenceQuestions.isNotEmpty()) {
                    "Generated using ${referenceQuestions.size} past paper reference question(s)."
                } else {
                    null
                }
                _uiState.update {
                    it.copy(
                        loadingQuiz = false,
                        quizStyle = settings.questionStyle,
                        quizNotice = notice,
                        quizSession = QuizSessionState(deck = questions)
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val cachedJson = lecture.cachedQuizJson
                val cachedStyle = QuestionStyle.fromString(lecture.cachedQuizStyle)
                if (!cachedJson.isNullOrBlank()) {
                    val cachedQuestions = runCatching { QuizJsonParser.parseQuestions(cachedJson) }
                        .getOrDefault(emptyList())
                    if (cachedQuestions.isNotEmpty()) {
                        _uiState.update {
                            it.copy(
                                loadingQuiz = false,
                                quizStyle = cachedStyle,
                                quizNotice = "Offline cache loaded (${e.localizedMessage ?: "Network unavailable"}).",
                                quizSession = QuizSessionState(deck = cachedQuestions)
                            )
                        }
                        return@launch
                    }
                }
                _uiState.update {
                    it.copy(
                        loadingQuiz = false,
                        quizError = e.localizedMessage ?: "Could not generate quiz questions."
                    )
                }
            }
        }
    }

    fun loadOrGenerateSummary(forceRegenerate: Boolean = false) {
        viewModelScope.launch {
            val lecture = repository.getLecture(lectureId) ?: return@launch
            if (!forceRegenerate && !lecture.summaryMarkdown.isNullOrBlank()) {
                _uiState.update {
                    it.copy(
                        loadingSummary = false,
                        summaryText = lecture.summaryMarkdown,
                        summaryError = null
                    )
                }
                return@launch
            }

            _uiState.update {
                it.copy(
                    loadingSummary = true,
                    summaryStatus = "Connecting to AI engine...",
                    summaryError = null,
                    summaryText = ""
                )
            }

            try {
                var latestText = ""
                repository.aiRepository.generateSummaryStream(lecture.markdownContent)
                    .collect { partial ->
                        latestText = partial
                        _uiState.update {
                            it.copy(
                                loadingSummary = false,
                                summaryText = partial
                            )
                        }
                    }
                if (latestText.isNotBlank()) {
                    repository.cacheLectureSummary(lectureId, latestText)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        loadingSummary = false,
                        summaryError = e.localizedMessage ?: "Could not generate summary."
                    )
                }
            }
        }
    }

    // Quiz interactions
    private fun updateActiveSession(transform: (QuizSessionState) -> QuizSessionState) {
        _uiState.update { state ->
            state.copy(quizSession = transform(state.quizSession))
        }
    }

    fun onMcqOptionSelected(option: String) {
        updateActiveSession { session ->
            val q = session.currentQuestion ?: return@updateActiveSession session
            session.selectMcqOption(option, q.resolvedCorrectResponses())
        }
    }

    fun onTfToggle(index: Int, value: Boolean) {
        updateActiveSession { it.toggleTfStatement(index, value) }
    }

    fun onEvaluateTf() {
        updateActiveSession { it.evaluateCurrentTf() }
    }

    fun onSaInputChange(text: String) {
        updateActiveSession { it.copy(saInputText = text) }
    }

    fun onRevealSaAnswer() {
        updateActiveSession { it.copy(saChecked = true) }
    }

    fun onGradeSa(isCorrect: Boolean) {
        updateActiveSession { session ->
            val scored = if (isCorrect) session.copy(runningScore = session.runningScore + 1) else session
            scored.advanceToNextQuestion()
        }
    }

    fun onSeqNotesChange(notes: String) {
        updateActiveSession { it.copy(seqUserNotes = notes) }
    }

    fun onToggleSeqAnswer() {
        updateActiveSession { it.copy(showSeqAnswer = !it.showSeqAnswer) }
    }

    fun onGradeSeq(marks: Int) {
        updateActiveSession { session ->
            session.copy(runningScore = session.runningScore + marks).advanceToNextQuestion()
        }
    }

    fun onNextQuestion() {
        updateActiveSession { it.advanceToNextQuestion() }
    }
}
