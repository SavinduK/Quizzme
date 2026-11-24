package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.QuizzMeRepository
import com.example.model.QuestionStyle
import com.example.model.SourceMode
import com.example.ui.components.DrawerFolderItem
import com.example.ui.components.FolderSortOrder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val availableSubjects: List<String> = emptyList(),
    val availableTerms: List<String> = emptyList(),
    val drawerItems: List<DrawerFolderItem> = emptyList(),
    val sortOrder: FolderSortOrder = FolderSortOrder.RECENT,
    val selectedSubject: String? = null,
    val selectedTerm: String? = null,
    val selectedLesson: String? = null,
    val selectedDrawerItemId: String? = null,
    val selectedType: QuestionStyle = QuestionStyle.MCQ,
    val maxQuestionsCount: Int = 5,
    val quizState: QuizSessionState = QuizSessionState()
) {
    val hasActiveFilters: Boolean
        get() = selectedSubject != null ||
            selectedTerm != null ||
            selectedLesson != null ||
            selectedType != QuestionStyle.MCQ
}

class HomeViewModel(
    private val repository: QuizzMeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.allLectures,
                repository.settingsFlow
            ) { lectures, settings ->
                val withQuizzes = lectures.filter { !it.cachedQuizJson.isNullOrBlank() }
                val subjects = withQuizzes.map { it.subject }.filter { it.isNotBlank() }.distinct().sorted()
                val terms = withQuizzes.map { it.term }.filter { it.isNotBlank() }.distinct().sorted()
                val folderItems = withQuizzes.map {
                    DrawerFolderItem(
                        id = it.id,
                        title = it.lesson,
                        subject = it.subject,
                        term = it.term,
                        updatedAt = it.updatedAt
                    )
                }
                HomeMetaSnapshot(subjects, terms, folderItems, settings.questionCount)
            }.collect { meta ->
                _uiState.update {
                    it.copy(
                        availableSubjects = meta.subjects,
                        availableTerms = meta.terms,
                        drawerItems = meta.folderItems,
                        maxQuestionsCount = meta.maxCount
                    )
                }
                recompileSessionPool()
            }
        }
    }

    fun setSortOrder(order: FolderSortOrder) {
        _uiState.update { it.copy(sortOrder = order) }
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
        _uiState.update {
            it.copy(
                selectedTerm = term,
                selectedLesson = null,
                selectedDrawerItemId = null
            )
        }
        recompileSessionPool()
    }

    fun selectDrawerItem(item: DrawerFolderItem) {
        _uiState.update {
            it.copy(
                selectedTerm = item.term.ifBlank { "Uncategorized" },
                selectedLesson = item.title,
                selectedDrawerItemId = item.id
            )
        }
        recompileSessionPool()
    }

    fun clearDrawerSelection() {
        _uiState.update {
            it.copy(
                selectedTerm = null,
                selectedLesson = null,
                selectedDrawerItemId = null
            )
        }
        recompileSessionPool()
    }

    fun resetFilters() {
        _uiState.update {
            it.copy(
                selectedSubject = null,
                selectedTerm = null,
                selectedLesson = null,
                selectedDrawerItemId = null,
                selectedType = QuestionStyle.MCQ
            )
        }
        recompileSessionPool()
    }

    fun recompileSessionPool() {
        val snapshot = _uiState.value
        viewModelScope.launch {
            val effectiveTerm = snapshot.selectedTerm?.takeUnless { it == "Uncategorized" }
            val pool = repository.compileHomeSessionPool(
                sourceMode = SourceMode.AI,
                selectedSubject = snapshot.selectedSubject,
                selectedTerm = effectiveTerm,
                selectedType = snapshot.selectedType,
                maxQuestions = snapshot.maxQuestionsCount,
                selectedLesson = snapshot.selectedLesson
            )
            _uiState.update {
                it.copy(quizState = QuizSessionState(deck = pool))
            }
        }
    }

    fun onMcqOptionSelected(option: String) {
        _uiState.update { state ->
            val currentQ = state.quizState.currentQuestion ?: return@update state
            state.copy(
                quizState = state.quizState.selectMcqOption(
                    option = option,
                    correctResponses = currentQ.resolvedCorrectResponses()
                )
            )
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

    private data class HomeMetaSnapshot(
        val subjects: List<String>,
        val terms: List<String>,
        val folderItems: List<DrawerFolderItem>,
        val maxCount: Int
    )
}
