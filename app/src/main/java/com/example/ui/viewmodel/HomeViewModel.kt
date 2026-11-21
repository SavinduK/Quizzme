package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.QuizzMeRepository
import com.example.model.QuestionStyle
import com.example.model.SourceMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val sourceMode: SourceMode = SourceMode.AI,
    val availableSubjects: List<String> = emptyList(),
    val availableTerms: List<String> = emptyList(),
    val selectedSubject: String? = null,
    val selectedTerm: String? = null,
    val selectedType: QuestionStyle = QuestionStyle.MCQ,
    val maxQuestionsCount: Int = 5,
    val quizState: QuizSessionState = QuizSessionState()
) {
    val hasActiveFilters: Boolean
        get() = selectedSubject != null ||
            selectedTerm != null ||
            selectedType != QuestionStyle.MCQ ||
            sourceMode != SourceMode.AI
}

class HomeViewModel(
    private val repository: QuizzMeRepository
) : ViewModel() {

    private val _sourceMode = MutableStateFlow(SourceMode.AI)
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.allLectures,
                repository.allPastPapers,
                repository.settingsFlow,
                _sourceMode
            ) { lectures, papers, settings, currentMode ->
                val subjects: List<String>
                val terms: List<String>
                if (currentMode == SourceMode.AI) {
                    val withQuizzes = lectures.filter { !it.cachedQuizJson.isNullOrBlank() }
                    subjects = withQuizzes.map { it.subject }.filter { it.isNotBlank() }.distinct().sorted()
                    terms = withQuizzes.map { it.term }.filter { it.isNotBlank() }.distinct().sorted()
                } else {
                    subjects = papers.map { it.subject }.filter { it.isNotBlank() }.distinct().sorted()
                    terms = papers.map { it.term }.filter { it.isNotBlank() }.distinct().sorted()
                }
                Triple(subjects, terms, settings.questionCount)
            }.collect { (subjects, terms, maxCount) ->
                _uiState.update {
                    it.copy(
                        availableSubjects = subjects,
                        availableTerms = terms,
                        maxQuestionsCount = maxCount
                    )
                }
                recompileSessionPool()
            }
        }
    }

    fun setSourceMode(mode: SourceMode) {
        _uiState.update {
            it.copy(
                sourceMode = mode,
                selectedSubject = null,
                selectedTerm = null
            )
        }
        _sourceMode.value = mode
        recompileSessionPool()
    }

    fun setSelectedType(type: QuestionStyle) {
        _uiState.update { it.copy(selectedType = type) }
        recompileSessionPool()
    }

    fun setSelectedSubject(subject: String?) {
        _uiState.update { it.copy(selectedSubject = subject) }
        recompileSessionPool()
    }

    fun setSelectedTerm(term: String?) {
        _uiState.update { it.copy(selectedTerm = term) }
        recompileSessionPool()
    }

    fun resetFilters() {
        _uiState.update {
            it.copy(
                selectedSubject = null,
                selectedTerm = null,
                selectedType = QuestionStyle.MCQ
            )
        }
        recompileSessionPool()
    }

    fun recompileSessionPool() {
        val snapshot = _uiState.value
        viewModelScope.launch {
            val pool = repository.compileHomeSessionPool(
                sourceMode = snapshot.sourceMode,
                selectedSubject = snapshot.selectedSubject,
                selectedTerm = snapshot.selectedTerm,
                selectedType = snapshot.selectedType,
                maxQuestions = snapshot.maxQuestionsCount
            )
            _uiState.update {
                it.copy(quizState = QuizSessionState(deck = pool))
            }
        }
    }

    fun onMcqOptionSelected(option: String) {
        _uiState.update { state ->
            val currentQ = state.quizState.currentQuestion ?: return@update state
            state.copy(quizState = state.quizState.selectMcqOption(option, currentQ.correctAnswer))
        }
    }

    fun onTfToggle(index: Int, value: Boolean) {
        _uiState.update { state ->
            state.copy(quizState = state.quizState.toggleTfStatement(index, value))
        }
    }

    fun onEvaluateTf() {
        _uiState.update { state ->
            state.copy(quizState = state.quizState.evaluateCurrentTf())
        }
    }

    fun onSaInputChange(text: String) {
        _uiState.update { state ->
            state.copy(quizState = state.quizState.copy(saInputText = text))
        }
    }

    fun onRevealSaAnswer() {
        _uiState.update { state ->
            state.copy(quizState = state.quizState.copy(saChecked = true))
        }
    }

    fun onGradeSa(isCorrect: Boolean) {
        _uiState.update { state ->
            val scored = if (isCorrect) {
                state.quizState.copy(runningScore = state.quizState.runningScore + 1)
            } else {
                state.quizState
            }
            state.copy(quizState = scored.advanceToNextQuestion())
        }
    }

    fun onSeqNotesChange(notes: String) {
        _uiState.update { state ->
            state.copy(quizState = state.quizState.copy(seqUserNotes = notes))
        }
    }

    fun onToggleSeqAnswer() {
        _uiState.update { state ->
            state.copy(quizState = state.quizState.copy(showSeqAnswer = !state.quizState.showSeqAnswer))
        }
    }

    fun onGradeSeq(marks: Int) {
        _uiState.update { state ->
            val scored = state.quizState.copy(runningScore = state.quizState.runningScore + marks)
            state.copy(quizState = scored.advanceToNextQuestion())
        }
    }

    fun onNextQuestion() {
        _uiState.update { state ->
            state.copy(quizState = state.quizState.advanceToNextQuestion())
        }
    }
}
