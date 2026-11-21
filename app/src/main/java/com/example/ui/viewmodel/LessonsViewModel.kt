package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.QuizzMeRepository
import com.example.data.local.LectureEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LessonsUiState(
    val allLessons: List<LectureEntity> = emptyList(),
    val filteredLessons: List<LectureEntity> = emptyList(),
    val uniqueSubjects: List<String> = emptyList(),
    val uniqueTerms: List<String> = emptyList(),
    val searchQuery: String = "",
    val selectedSubject: String? = null,
    val selectedTerm: String? = null
) {
    val hasActiveFilter: Boolean
        get() = selectedSubject != null || selectedTerm != null
}

class LessonsViewModel(
    private val repository: QuizzMeRepository
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val selectedSubject = MutableStateFlow<String?>(null)
    private val selectedTerm = MutableStateFlow<String?>(null)

    val uiState: StateFlow<LessonsUiState> = combine(
        repository.allLectures,
        searchQuery,
        selectedSubject,
        selectedTerm
    ) { lectures, query, subject, term ->
        val subjects = lectures.map { it.subject }.filter { it.isNotBlank() }.distinct().sorted()
        val terms = lectures.map { it.term }.filter { it.isNotBlank() }.distinct().sorted()

        val filtered = lectures.filter { les ->
            val matchesQuery = query.isBlank() ||
                les.lesson.contains(query, ignoreCase = true) ||
                les.subject.contains(query, ignoreCase = true)
            val matchesSubject = subject == null || les.subject.equals(subject, ignoreCase = true)
            val matchesTerm = term == null || les.term.equals(term, ignoreCase = true)
            matchesQuery && matchesSubject && matchesTerm
        }

        LessonsUiState(
            allLessons = lectures,
            filteredLessons = filtered,
            uniqueSubjects = subjects,
            uniqueTerms = terms,
            searchQuery = query,
            selectedSubject = subject,
            selectedTerm = term
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LessonsUiState()
    )

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun setSelectedSubject(subject: String?) {
        selectedSubject.value = subject
    }

    fun setSelectedTerm(term: String?) {
        selectedTerm.value = term
    }

    fun resetFilters() {
        selectedSubject.value = null
        selectedTerm.value = null
    }

    fun deleteLesson(id: String) {
        viewModelScope.launch {
            repository.deleteLecture(id)
        }
    }
}
