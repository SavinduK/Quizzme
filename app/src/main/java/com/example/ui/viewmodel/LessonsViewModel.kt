package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.QuizzMeRepository
import com.example.data.local.LectureEntity
import com.example.ui.components.DrawerFolderItem
import com.example.ui.components.FolderSortOrder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LessonsUiState(
    val allLessons: List<LectureEntity> = emptyList(),
    val filteredLessons: List<LectureEntity> = emptyList(),
    val drawerItems: List<DrawerFolderItem> = emptyList(),
    val searchQuery: String = "",
    val selectedTerm: String? = null,
    val sortOrder: FolderSortOrder = FolderSortOrder.RECENT
) {
    val hasActiveFilter: Boolean
        get() = selectedTerm != null
}

class LessonsViewModel(
    private val repository: QuizzMeRepository
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val selectedTerm = MutableStateFlow<String?>(null)
    private val sortOrder = MutableStateFlow(FolderSortOrder.RECENT)

    val uiState: StateFlow<LessonsUiState> = combine(
        repository.allLectures,
        searchQuery,
        selectedTerm,
        sortOrder
    ) { lectures, query, term, order ->
        val filtered = lectures.filter { les ->
            val matchesQuery = query.isBlank() ||
                les.lesson.contains(query, ignoreCase = true) ||
                les.subject.contains(query, ignoreCase = true) ||
                les.term.contains(query, ignoreCase = true)
            val matchesTerm = term == null ||
                les.term.ifBlank { "Uncategorized" }.equals(term, ignoreCase = true)
            matchesQuery && matchesTerm
        }

        val sorted = when (order) {
            FolderSortOrder.RECENT -> filtered.sortedByDescending { it.updatedAt }
            FolderSortOrder.TITLE_ASC -> filtered.sortedBy { it.lesson.lowercase() }
        }

        val drawerItems = lectures.map { les ->
            DrawerFolderItem(
                id = les.id,
                title = les.lesson,
                subject = les.subject,
                term = les.term,
                updatedAt = les.updatedAt
            )
        }

        LessonsUiState(
            allLessons = lectures,
            filteredLessons = sorted,
            drawerItems = drawerItems,
            searchQuery = query,
            selectedTerm = term,
            sortOrder = order
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LessonsUiState()
    )

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun setSelectedTerm(term: String?) {
        selectedTerm.value = term
    }

    fun setSortOrder(order: FolderSortOrder) {
        sortOrder.value = order
    }

    fun resetFilters() {
        selectedTerm.value = null
    }

    fun deleteLesson(id: String) {
        viewModelScope.launch {
            repository.deleteLecture(id)
        }
    }
}
