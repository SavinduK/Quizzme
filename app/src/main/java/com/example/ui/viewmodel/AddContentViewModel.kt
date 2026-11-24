package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.QuizzMeRepository
import com.example.data.local.LectureEntity
import com.example.model.QuizJsonParser
import com.example.model.QuizQuestion
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class PaperUploadSource(val label: String) {
    PDF("PDF"),
    IMAGES("Images"),
    TEXT("AI Text"),
    JSON("JSON")
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
    val year: String = "",
    val lesson: String = "",
    val linkedLectureId: String? = null,
    val linkedLectureTitle: String? = null,
    val availableLectures: List<LectureEntity> = emptyList(),
    val uploadSource: PaperUploadSource = PaperUploadSource.PDF,
    val attachedPdfName: String? = null,
    val selectedImageCount: Int = 0,
    val rawText: String = "",
    val extractedQuestions: List<QuizQuestion> = emptyList(),
    val processing: Boolean = false,
    val saving: Boolean = false,
    val statusMessage: String = "",
    val extractionError: String? = null,
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

    init {
        viewModelScope.launch {
            repository.allLectures.collect { lectures ->
                _paperState.update { it.copy(availableLectures = lectures) }
            }
        }
    }

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
                        alertMessage = "PDF notes converted to Markdown successfully.",
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
    fun setPaperYear(v: String) = _paperState.update { it.copy(year = v) }
    fun setPaperLesson(v: String) = _paperState.update { it.copy(lesson = v) }
    fun setPaperUploadSource(source: PaperUploadSource) =
        _paperState.update { it.copy(uploadSource = source, extractionError = null) }

    fun setPaperRawText(v: String) = _paperState.update { it.copy(rawText = v, extractionError = null) }

    fun selectLinkedLecture(lecture: LectureEntity?) {
        _paperState.update { state ->
            if (lecture == null) {
                state.copy(
                    linkedLectureId = null,
                    linkedLectureTitle = null
                )
            } else {
                state.copy(
                    linkedLectureId = lecture.id,
                    linkedLectureTitle = lecture.lesson,
                    lesson = lecture.lesson,
                    subject = lecture.subject,
                    term = lecture.term
                )
            }
        }
    }

    fun processPastPaperPdf(fileName: String, pdfBytes: ByteArray) {
        val s = _paperState.value
        viewModelScope.launch {
            _paperState.update {
                it.copy(
                    attachedPdfName = fileName,
                    processing = true,
                    statusMessage = "Extracting questions from $fileName...",
                    extractionError = null
                )
            }
            try {
                val customPrompt = repository.settingsRepository.getSettingsOnce().customPrompt
                val questions = repository.aiRepository.parsePastPaperPdf(
                    pdfBytes = pdfBytes,
                    customPrompt = customPrompt,
                    defaultYear = s.year,
                    defaultLesson = s.lesson
                )
                _paperState.update { state ->
                    state.copy(
                        processing = false,
                        statusMessage = "",
                        extractedQuestions = state.extractedQuestions + questions,
                        extractionError = null
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _paperState.update {
                    it.copy(
                        processing = false,
                        statusMessage = "",
                        extractionError = e.localizedMessage ?: "Failed to extract questions from PDF."
                    )
                }
            }
        }
    }

    fun processPastPaperImages(jpegBytesList: List<ByteArray>) {
        if (jpegBytesList.isEmpty()) return
        val s = _paperState.value
        viewModelScope.launch {
            _paperState.update {
                it.copy(
                    selectedImageCount = jpegBytesList.size,
                    processing = true,
                    statusMessage = "Analyzing ${jpegBytesList.size} past paper image(s)...",
                    extractionError = null
                )
            }
            try {
                val customPrompt = repository.settingsRepository.getSettingsOnce().customPrompt
                val questions = repository.aiRepository.parsePastPaperImages(
                    imageBytesList = jpegBytesList,
                    customPrompt = customPrompt,
                    defaultYear = s.year,
                    defaultLesson = s.lesson
                )
                _paperState.update { state ->
                    state.copy(
                        processing = false,
                        statusMessage = "",
                        extractedQuestions = state.extractedQuestions + questions,
                        extractionError = null
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _paperState.update {
                    it.copy(
                        processing = false,
                        statusMessage = "",
                        extractionError = e.localizedMessage ?: "Failed to analyze past paper images."
                    )
                }
            }
        }
    }

    fun setPaperExtractionError(message: String) {
        _paperState.update {
            it.copy(
                processing = false,
                statusMessage = "",
                extractionError = message
            )
        }
    }

    fun extractFromRawInput() {
        val s = _paperState.value
        if (s.rawText.isBlank()) {
            _paperState.update { it.copy(extractionError = "Please paste question text or JSON first.") }
            return
        }
        viewModelScope.launch {
            _paperState.update {
                it.copy(
                    processing = true,
                    statusMessage = if (s.uploadSource == PaperUploadSource.JSON) {
                        "Parsing structured JSON questions..."
                    } else {
                        "Extracting structured questions with AI..."
                    },
                    extractionError = null
                )
            }
            try {
                val parsed = if (s.uploadSource == PaperUploadSource.JSON) {
                    QuizJsonParser.parseQuestions(s.rawText).also {
                        if (it.isEmpty()) throw IllegalArgumentException("No valid questions found in JSON.")
                    }.map { q ->
                        q.copy(
                            year = q.year?.takeIf { y -> y.isNotBlank() } ?: s.year.ifBlank { null },
                            lesson = q.lesson?.takeIf { l -> l.isNotBlank() } ?: s.lesson.ifBlank { null }
                        )
                    }
                } else {
                    val customPrompt = repository.settingsRepository.getSettingsOnce().customPrompt
                    repository.aiRepository.parsePastPaperText(
                        rawText = s.rawText,
                        customPrompt = customPrompt,
                        defaultYear = s.year,
                        defaultLesson = s.lesson
                    )
                }
                _paperState.update { state ->
                    state.copy(
                        processing = false,
                        statusMessage = "",
                        rawText = "",
                        extractedQuestions = state.extractedQuestions + parsed,
                        extractionError = null
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _paperState.update {
                    it.copy(
                        processing = false,
                        statusMessage = "",
                        extractionError = e.localizedMessage ?: "Failed to parse questions."
                    )
                }
            }
        }
    }

    fun addOrUpdateDraftQuestion(editIndex: Int?, question: QuizQuestion) {
        _paperState.update { state ->
            val list = state.extractedQuestions.toMutableList()
            if (editIndex != null && editIndex in list.indices) {
                list[editIndex] = question
            } else {
                list.add(question)
            }
            state.copy(extractedQuestions = list, extractionError = null)
        }
    }

    fun removeDraftQuestion(index: Int) {
        _paperState.update { state ->
            val list = state.extractedQuestions.toMutableList()
            if (index in list.indices) list.removeAt(index)
            state.copy(extractedQuestions = list)
        }
    }

    fun dismissPaperAlert(onNavigateBack: () -> Unit) {
        val shouldGoBack = _paperState.value.navigateBackOnDismiss
        _paperState.update { it.copy(alertTitle = null, alertMessage = null, navigateBackOnDismiss = false) }
        if (shouldGoBack) onNavigateBack()
    }

    fun savePastPaper() {
        val s = _paperState.value
        val hasQuestions = s.extractedQuestions.isNotEmpty()
        val hasRawFallback = (s.uploadSource == PaperUploadSource.TEXT || s.uploadSource == PaperUploadSource.JSON) &&
            s.rawText.isNotBlank()

        if (!hasQuestions && !hasRawFallback) {
            _paperState.update {
                it.copy(
                    alertTitle = "No Questions Yet",
                    alertMessage = "Upload a past paper PDF or images, extract from text/JSON, or add questions manually before saving.",
                    alertIsSuccess = false
                )
            }
            return
        }

        if (s.lesson.isBlank() && s.subject.isBlank()) {
            _paperState.update {
                it.copy(
                    alertTitle = "Missing Details",
                    alertMessage = "Please enter at least a Lesson or Subject (or link an existing Lesson).",
                    alertIsSuccess = false
                )
            }
            return
        }

        viewModelScope.launch {
            _paperState.update {
                it.copy(
                    saving = true,
                    statusMessage = "Saving past paper to library...",
                    extractionError = null
                )
            }
            try {
                val finalQuestions = if (hasQuestions) {
                    s.extractedQuestions
                } else {
                    if (s.uploadSource == PaperUploadSource.JSON) {
                        QuizJsonParser.parseQuestions(s.rawText).also {
                            if (it.isEmpty()) throw IllegalArgumentException("Invalid JSON input or empty question list.")
                        }
                    } else {
                        val customPrompt = repository.settingsRepository.getSettingsOnce().customPrompt
                        repository.aiRepository.parsePastPaperText(
                            rawText = s.rawText,
                            customPrompt = customPrompt,
                            defaultYear = s.year,
                            defaultLesson = s.lesson
                        )
                    }
                }

                val paperId = "paper_${
                    QuizzMeRepository.buildSlugId(
                        s.lesson.ifBlank { s.subject },
                        s.subject.ifBlank { "general" },
                        s.year.ifBlank { s.term }.ifBlank { System.currentTimeMillis().toString() }
                    )
                }"
                repository.savePastPaper(
                    id = paperId,
                    lesson = s.lesson.ifBlank { s.subject },
                    subject = s.subject.ifBlank { "General" },
                    term = s.term,
                    year = s.year,
                    linkedLectureId = s.linkedLectureId,
                    questions = finalQuestions
                )

                _paperState.update {
                    it.copy(
                        saving = false,
                        statusMessage = "",
                        alertTitle = "Success",
                        alertMessage = "Saved ${finalQuestions.size} past paper question(s) successfully.",
                        alertIsSuccess = true,
                        navigateBackOnDismiss = true
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _paperState.update {
                    it.copy(
                        saving = false,
                        statusMessage = "",
                        alertTitle = "Save Error",
                        alertMessage = e.localizedMessage ?: "Failed to save past paper.",
                        alertIsSuccess = false
                    )
                }
            }
        }
    }
}
