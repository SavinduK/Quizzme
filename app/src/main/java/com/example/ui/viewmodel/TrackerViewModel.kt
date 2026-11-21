package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.QuizzMeRepository
import com.example.data.local.TrackerTaskEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

data class CalendarDayItem(
    val dateStr: String,
    val dayName: String,
    val dayNum: Int,
    val isToday: Boolean,
    val hasTasks: Boolean
)

data class TrackerUiState(
    val selectedDate: String = LocalDate.now().toString(),
    val todayDate: String = LocalDate.now().toString(),
    val calendarDays: List<CalendarDayItem> = emptyList(),
    val dayTasks: List<TrackerTaskEntity> = emptyList(),
    val completedCount: Int = 0,
    val feedbackMessage: String? = null
)

class TrackerViewModel(
    private val repository: QuizzMeRepository
) : ViewModel() {

    private val today = LocalDate.now()
    private val todayStr = today.toString()
    private val selectedDate = MutableStateFlow(todayStr)
    private val feedbackMessage = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch {
            repository.ensureInitialTasksSeeded()
        }
    }

    val uiState: StateFlow<TrackerUiState> = combine(
        repository.allTasks,
        selectedDate,
        feedbackMessage
    ) { allTasks, selected, message ->
        val taskDates = allTasks.map { it.date }.toSet()
        val days = (-30..30).map { offset ->
            val d = today.plusDays(offset.toLong())
            val dStr = d.toString()
            CalendarDayItem(
                dateStr = dStr,
                dayName = d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.US),
                dayNum = d.dayOfMonth,
                isToday = dStr == todayStr,
                hasTasks = dStr in taskDates
            )
        }
        val forDay = allTasks.filter { it.date == selected }
        TrackerUiState(
            selectedDate = selected,
            todayDate = todayStr,
            calendarDays = days,
            dayTasks = forDay,
            completedCount = forDay.count { it.completed },
            feedbackMessage = message
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TrackerUiState()
    )

    fun selectDate(dateStr: String) {
        selectedDate.value = dateStr
    }

    fun toggleTask(task: TrackerTaskEntity) {
        viewModelScope.launch {
            repository.toggleTaskCompleted(task.id, task.completed)
        }
    }

    fun addTask(title: String, subject: String, time: String) {
        if (title.isBlank()) {
            feedbackMessage.value = "Please enter a task title."
            return
        }
        viewModelScope.launch {
            repository.addTask(
                date = selectedDate.value,
                title = title,
                subject = subject,
                time = time
            )
        }
    }

    fun importJson(rawJson: String, onSuccess: () -> Unit) {
        if (rawJson.isBlank()) {
            feedbackMessage.value = "Please paste a valid JSON schedule."
            return
        }
        viewModelScope.launch {
            try {
                val count = repository.importTasksFromJson(rawJson, selectedDate.value)
                feedbackMessage.value = "Imported $count task(s)."
                onSuccess()
            } catch (e: Exception) {
                feedbackMessage.value = "Invalid JSON format: ${e.localizedMessage ?: "Please check syntax."}"
            }
        }
    }

    fun deleteTask(id: String) {
        viewModelScope.launch {
            repository.deleteTask(id)
        }
    }

    fun clearFeedback() {
        feedbackMessage.value = null
    }
}
