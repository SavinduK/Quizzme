package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.QuizzMeRepository
import com.example.model.QuizJsonParser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class PaperFormatMode {
    AUTO,
    JSON
}

data class AddNotesUiState(
    val subject: String = "",
    val term: String = "",
    val lesson: String = "",
    val notesText: String = "",
    val attachedFileName: String? = null,
    val processingPdf: Boolean = false,
    val saving: Boolean = false,
    val alertTitle: String? = null,
    val alertMessage: String? = null,
    val alertIsSuccess: Boolean = true,
    val navigateBackOnDismiss: Boolean = false
)

data class AddPaperUiState(
    val subject: String = "",
    val term: String = "",
    val lesson: String = "",
    val formatMode: PaperFormatMode = PaperFormatMode.AUTO,
    val rawText: String = "",
    val processing: Boolean = false,
    val alertTitle: String? = null,
    val alertMessage: String? = null,
    val alertIsSuccess: Boolean = true,
    val navigateBackOnDismiss: Boolean = false
)

class AddContentViewModel(
    private val repository: QuizzMeRepository
) : ViewModel() {

    private val _notesState = MutableStateFlow(AddNotesUiState())
    val notesState: StateFlow<AddNotesUiState> = _notesState.asStateFlow()

    private val _paperState = MutableStateFlow(AddPaperUiState())
    val paperState: StateFlow<AddPaperUiState> = _paperState.asStateFlow()

    // --- ADD NOTES ---
    fun setNotesSubject(v: String) = _notesState.update { it.copy(subject = v) }
    fun setNotesTerm(v: String) = _notesState.update { it.copy(term = v) }
    fun setNotesLesson(v: String) = _notesState.update { it.copy(lesson = v) }
    fun setNotesText(v: String) = _notesState.update { it.copy(notesText = v) }

    fun dismissNotesAlert(onNavigateBack: () -> Unit) {
        val shouldGoBack = _notesState.value.navigateBackOnDismiss
        _notesState.update { it.copy(alertTitle = null, alertMessage = null, navigateBackOnDismiss = false) }
        if (shouldGoBack) onNavigateBack()
    }

    fun processPdfBytes(fileName: String, bytes: ByteArray) {
        viewModelScope.launch {
            _notesState.update {
                it.copy(attachedFileName = fileName, processingPdf = true)
            }
            try {
                val extracted = repository.aiRepository.extractNotesFromPdf(bytes)
                _notesState.update {
                    it.copy(
                        processingPdf = false,
                        notesText = extracted,
                        alertTitle = "Success",
                        alertMessage = "PDF text extracted and structured successfully.",
                        alertIsSuccess = true
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _notesState.update {
                    it.copy(
                        processingPdf = false,
                        attachedFileName = null,
                        alertTitle = "Extraction Error",
                        alertMessage = e.localizedMessage ?: "Failed to parse information from the selected PDF.",
                        alertIsSuccess = false
                    )
                }
            }
        }
    }

    fun saveNotes() {
        val s = _notesState.value
        if (s.subject.isBlank() || s.term.isBlank() || s.lesson.isBlank() || s.notesText.isBlank()) {
            _notesState.update {
                it.copy(
                    alertTitle = "Error",
                    alertMessage = "Please fill in all metadata fields and provide the notes content.",
                    alertIsSuccess = false
                )
            }
            return
        }
        viewModelScope.launch {
            _notesState.update { it.copy(saving = true) }
            try {
                repository.saveLecture(
                    lesson = s.lesson,
                    subject = s.subject,
                    term = s.term,
                    markdownContent = s.notesText
                )
                _notesState.update {
                    it.copy(
                        saving = false,
                        alertTitle = "Success",
                        alertMessage = "Lesson notes saved and indexed successfully.",
                        alertIsSuccess = true,
                        navigateBackOnDismiss = true
                    )
                }
            } catch (e: Exception) {
                _notesState.update {
                    it.copy(
                        saving = false,
                        alertTitle = "Save Error",
                        alertMessage = e.localizedMessage ?: "Failed to save notes.",
                        alertIsSuccess = false
                    )
                }
            }
        }
    }

    // --- ADD PAST PAPER ---
    fun setPaperSubject(v: String) = _paperState.update { it.copy(subject = v) }
    fun setPaperTerm(v: String) = _paperState.update { it.copy(term = v) }
    fun setPaperLesson(v: String) = _paperState.update { it.copy(lesson = v) }
    fun setPaperFormatMode(mode: PaperFormatMode) = _paperState.update { it.copy(formatMode = mode) }
    fun setPaperRawText(v: String) = _paperState.update { it.copy(rawText = v) }

    fun dismissPaperAlert(onNavigateBack: () -> Unit) {
        val shouldGoBack = _paperState.value.navigateBackOnDismiss
        _paperState.update { it.copy(alertTitle = null, alertMessage = null, navigateBackOnDismiss = false) }
        if (shouldGoBack) onNavigateBack()
    }

    fun savePastPaper() {
        val s = _paperState.value
        if (s.subject.isBlank() || s.term.isBlank() || s.lesson.isBlank() || s.rawText.isBlank()) {
            _paperState.update {
                it.copy(
                    alertTitle = "Error",
                    alertMessage = "Please fill in all metadata fields and provide the question text.",
                    alertIsSuccess = false
                )
            }
            return
        }

        viewModelScope.launch {
            _paperState.update { it.copy(processing = true) }
            try {
                val questions = if (s.formatMode == PaperFormatMode.JSON) {
                    QuizJsonParser.parseQuestions(s.rawText).also {
                        if (it.isEmpty()) throw IllegalArgumentException("Invalid JSON input or empty question list.")
                    }
                } else {
                    val customPrompt = repository.settingsRepository.getSettingsOnce().customPrompt
                    repository.aiRepository.parsePastPaperText(s.rawText, customPrompt)
                }

                val paperId = "paper_${QuizzMeRepository.buildSlugId(s.lesson, s.subject, s.term)}"
                repository.savePastPaper(
                    id = paperId,
                    lesson = s.lesson,
                    subject = s.subject,
                    term = s.term,
                    questions = questions
                )

                _paperState.update {
                    it.copy(
                        processing = false,
                        alertTitle = "Success",
                        alertMessage = "Past paper questions formatted and saved successfully.",
                        alertIsSuccess = true,
                        navigateBackOnDismiss = true
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _paperState.update {
                    it.copy(
                        processing = false,
                        alertTitle = "Save Error",
                        alertMessage = e.localizedMessage ?: "Failed to process and save past paper questions.",
                        alertIsSuccess = false
                    )
                }
            }
        }
    }
}
