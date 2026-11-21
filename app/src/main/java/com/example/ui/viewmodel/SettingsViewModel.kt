package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.QuizzMeRepository
import com.example.model.QuestionStyle
import com.example.model.QuizSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val questionCount: Int = 5,
    val questionStyle: QuestionStyle = QuestionStyle.MCQ,
    val customPrompt: String = "",
    val isSaving: Boolean = false,
    val alertTitle: String? = null,
    val alertMessage: String? = null,
    val alertIsSuccess: Boolean = true
)

class SettingsViewModel(
    private val repository: QuizzMeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.settingsFlow.collect { saved ->
                _uiState.update {
                    it.copy(
                        questionCount = saved.questionCount,
                        questionStyle = saved.questionStyle,
                        customPrompt = saved.customPrompt
                    )
                }
            }
        }
    }

    fun setQuestionCount(count: Int) {
        _uiState.update { it.copy(questionCount = count) }
    }

    fun setQuestionStyle(style: QuestionStyle) {
        _uiState.update { it.copy(questionStyle = style) }
    }

    fun setCustomPrompt(prompt: String) {
        _uiState.update { it.copy(customPrompt = prompt) }
    }

    fun saveSettings() {
        val current = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                repository.settingsRepository.saveSettings(
                    QuizSettings(
                        questionCount = current.questionCount,
                        questionStyle = current.questionStyle,
                        customPrompt = current.customPrompt
                    )
                )
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        alertTitle = "Success",
                        alertMessage = "Configuration preferences updated successfully.",
                        alertIsSuccess = true
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        alertTitle = "Error",
                        alertMessage = e.localizedMessage ?: "Could not save configurations locally.",
                        alertIsSuccess = false
                    )
                }
            }
        }
    }

    fun dismissAlert() {
        _uiState.update { it.copy(alertTitle = null, alertMessage = null) }
    }
}
