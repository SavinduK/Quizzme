package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AiRepository
import com.example.data.local.LectureEntity
import com.example.model.QuizQuestion
import com.example.ui.components.PastPaperQuestionEditorDialog
import com.example.ui.components.QuizzMeAlertDialog
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalQuizzMeColors
import com.example.ui.viewmodel.AddContentViewModel
import com.example.ui.viewmodel.AddPaperUiState
import com.example.ui.viewmodel.PaperUploadSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

@Composable
fun AddPastPaperScreen(
    viewModel: AddContentViewModel,
    widthSizeClass: WindowWidthSizeClass,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.paperState.collectAsStateWithLifecycle()
    val ext = LocalQuizzMeColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val isTablet = widthSizeClass != WindowWidthSizeClass.Compact

    var preparingImages by remember { mutableStateOf(false) }
    var showLessonPicker by remember { mutableStateOf(false) }
    var editingQuestion by remember { mutableStateOf<Pair<Int?, QuizQuestion?>?>(null) }

    BackHandler { onBack() }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = resolveDisplayName(context, uri) ?: "past_paper.pdf"
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            if (bytes != null) {
                viewModel.processPastPaperPdf(fileName, bytes)
            } else {
                viewModel.setPaperExtractionError("Could not read the selected PDF file.")
            }
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            scope.launch {
                preparingImages = true
                val encoded = withContext(Dispatchers.IO) {
                    uris.mapNotNull { encodeUriToCompressedJpeg(context, it) }
                }
                preparingImages = false
                if (encoded.isNotEmpty()) {
                    viewModel.processPastPaperImages(encoded)
                } else {
                    viewModel.setPaperExtractionError("Could not read the selected image(s).")
                }
            }
        }
    }

    fun copyPastPaperConvertingPrompt() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(
            ClipData.newPlainText("QuizzMe Past Paper Converting Prompt", AiRepository.PAST_PAPER_CONVERT_PROMPT)
        )
        scope.launch {
            snackbarHostState.showSnackbar("Converting prompt copied to clipboard")
        }
    }

    val isBusy = preparingImages || uiState.processing || uiState.saving
    val busyLabel = when {
        preparingImages -> "Preparing past paper images..."
        uiState.processing -> uiState.statusMessage.ifBlank { "Processing past paper..." }
        uiState.saving -> uiState.statusMessage.ifBlank { "Saving past paper..." }
        else -> ""
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
        containerColor = ext.background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = ext.card,
                        border = BorderStroke(1.dp, ext.border)
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("add_paper_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Back",
                                tint = ext.title
                            )
                        }
                    }

                    Text(
                        text = "Add Past Paper",
                        style = MaterialTheme.typography.headlineLarge,
                        color = ext.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Button(
                    onClick = viewModel::savePastPaper,
                    enabled = !isBusy,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ext.accent,
                        contentColor = Color.White,
                        disabledContainerColor = ext.accent.copy(alpha = 0.45f),
                        disabledContentColor = Color.White.copy(alpha = 0.8f)
                    ),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .height(48.dp)
                        .testTag("save_paper_button")
                ) {
                    if (uiState.saving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Saving...", style = MaterialTheme.typography.labelLarge)
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Save,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (uiState.extractedQuestions.isNotEmpty()) {
                                "Save (${uiState.extractedQuestions.size})"
                            } else {
                                "Save"
                            },
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
            HorizontalDivider(color = ext.border)

            // Pinned Loading Bar for Past Paper Upload / Extraction / Save
            AnimatedVisibility(visible = isBusy) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                        .testTag("past_paper_loading_card"),
                    shape = RoundedCornerShape(14.dp),
                    color = ext.card,
                    border = BorderStroke(1.dp, ext.accent)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = busyLabel,
                            style = MaterialTheme.typography.labelLarge,
                            color = ext.accent
                        )
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .testTag("past_paper_upload_loading_bar"),
                            color = ext.accent,
                            trackColor = ext.border,
                            strokeCap = StrokeCap.Round
                        )
                    }
                }
            }

            if (isTablet) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .widthIn(max = 1280.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(0.46f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = 16.dp)
                    ) {
                        PaperMetadataAndUploadPanel(
                            uiState = uiState,
                            isBusy = isBusy,
                            onOpenLessonPicker = { showLessonPicker = true },
                            onYearChange = viewModel::setPaperYear,
                            onSubjectChange = viewModel::setPaperSubject,
                            onTermChange = viewModel::setPaperTerm,
                            onLessonChange = viewModel::setPaperLesson,
                            onSourceChange = viewModel::setPaperUploadSource,
                            onUploadPdfClick = { pdfPickerLauncher.launch(arrayOf("application/pdf")) },
                            onUploadImagesClick = {
                                imagePickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            onRawTextChange = viewModel::setPaperRawText,
                            onExtractRawClick = viewModel::extractFromRawInput,
                            onCopyPromptClick = ::copyPastPaperConvertingPrompt,
                            onAddManualQuestion = { editingQuestion = null to null }
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(0.54f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = 16.dp)
                    ) {
                        ExtractedQuestionsReviewSection(
                            questions = uiState.extractedQuestions,
                            defaultYear = uiState.year,
                            defaultLesson = uiState.lesson,
                            onAddQuestion = { editingQuestion = null to null },
                            onEditQuestion = { idx, q -> editingQuestion = idx to q },
                            onRemoveQuestion = viewModel::removeDraftQuestion
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .widthIn(max = 680.dp)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 18.dp)
                        .padding(bottom = 28.dp)
                ) {
                    PaperMetadataAndUploadPanel(
                        uiState = uiState,
                        isBusy = isBusy,
                        onOpenLessonPicker = { showLessonPicker = true },
                        onYearChange = viewModel::setPaperYear,
                        onSubjectChange = viewModel::setPaperSubject,
                        onTermChange = viewModel::setPaperTerm,
                        onLessonChange = viewModel::setPaperLesson,
                        onSourceChange = viewModel::setPaperUploadSource,
                        onUploadPdfClick = { pdfPickerLauncher.launch(arrayOf("application/pdf")) },
                        onUploadImagesClick = {
                            imagePickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        onRawTextChange = viewModel::setPaperRawText,
                        onExtractRawClick = viewModel::extractFromRawInput,
                        onCopyPromptClick = ::copyPastPaperConvertingPrompt,
                        onAddManualQuestion = { editingQuestion = null to null }
                    )

                    Spacer(Modifier.height(24.dp))

                    ExtractedQuestionsReviewSection(
                        questions = uiState.extractedQuestions,
                        defaultYear = uiState.year,
                        defaultLesson = uiState.lesson,
                        onAddQuestion = { editingQuestion = null to null },
                        onEditQuestion = { idx, q -> editingQuestion = idx to q },
                        onRemoveQuestion = viewModel::removeDraftQuestion
                    )
                }
            }
        }
    }

    if (showLessonPicker) {
        DraftLessonLinkDialog(
            lectures = uiState.availableLectures,
            selectedLectureId = uiState.linkedLectureId,
            onDismiss = { showLessonPicker = false },
            onSelect = { lecture ->
                viewModel.selectLinkedLecture(lecture)
                showLessonPicker = false
            }
        )
    }

    val editPair = editingQuestion
    if (editPair != null) {
        val (idx, q) = editPair
        PastPaperQuestionEditorDialog(
            initialQuestion = q,
            defaultYear = uiState.year,
            defaultLesson = uiState.lesson,
            isTablet = isTablet,
            onDismiss = { editingQuestion = null },
            onSave = { updated ->
                viewModel.addOrUpdateDraftQuestion(idx, updated)
                editingQuestion = null
            }
        )
    }

    if (uiState.alertTitle != null && uiState.alertMessage != null) {
        QuizzMeAlertDialog(
            title = uiState.alertTitle!!,
            message = uiState.alertMessage!!,
            isSuccess = uiState.alertIsSuccess,
            onDismiss = { viewModel.dismissPaperAlert(onBack) }
        )
    }
}

@Composable
private fun PaperMetadataAndUploadPanel(
    uiState: AddPaperUiState,
    isBusy: Boolean,
    onOpenLessonPicker: () -> Unit,
    onYearChange: (String) -> Unit,
    onSubjectChange: (String) -> Unit,
    onTermChange: (String) -> Unit,
    onLessonChange: (String) -> Unit,
    onSourceChange: (PaperUploadSource) -> Unit,
    onUploadPdfClick: () -> Unit,
    onUploadImagesClick: () -> Unit,
    onRawTextChange: (String) -> Unit,
    onExtractRawClick: () -> Unit,
    onCopyPromptClick: () -> Unit,
    onAddManualQuestion: () -> Unit
) {
    val ext = LocalQuizzMeColors.current

    Text(
        text = "LINK LESSON & PAPER METADATA",
        style = MaterialTheme.typography.labelMedium,
        color = ext.accent
    )
    Spacer(Modifier.height(10.dp))

    // Link to Lesson Picker Button
    Surface(
        onClick = onOpenLessonPicker,
        shape = RoundedCornerShape(16.dp),
        color = if (uiState.linkedLectureId != null) ext.accentSoft else ext.card,
        border = BorderStroke(1.dp, if (uiState.linkedLectureId != null) ext.accent else ext.border),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("add_paper_link_lesson_button")
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Link,
                    contentDescription = null,
                    tint = ext.accent,
                    modifier = Modifier.size(20.dp)
                )
                Column {
                    Text(
                        text = uiState.linkedLectureTitle?.let { "Linked to: $it" }
                            ?: "Link to Existing Lesson (Optional)",
                        style = MaterialTheme.typography.titleSmall,
                        color = if (uiState.linkedLectureId != null) ext.accent else ext.title
                    )
                    Text(
                        text = if (uiState.linkedLectureId != null) {
                            "Tap to change or unlink lesson"
                        } else {
                            "Auto-fills lesson, subject & term"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = ext.subtext
                    )
                }
            }
            Text(
                text = if (uiState.linkedLectureId != null) "Change" else "Select",
                style = MaterialTheme.typography.labelMedium,
                color = ext.accent
            )
        }
    }

    Spacer(Modifier.height(12.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(modifier = Modifier.weight(0.42f)) {
            StyledFormField(
                value = uiState.year,
                onValueChange = onYearChange,
                placeholder = "Year (e.g. 2024)",
                testTag = "paper_year_input"
            )
        }
        Box(modifier = Modifier.weight(0.58f)) {
            StyledFormField(
                value = uiState.lesson,
                onValueChange = onLessonChange,
                placeholder = "Lesson / Topic",
                testTag = "paper_lesson_input"
            )
        }
    }
    Spacer(Modifier.height(12.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(modifier = Modifier.weight(1f)) {
            StyledFormField(
                value = uiState.subject,
                onValueChange = onSubjectChange,
                placeholder = "Subject (e.g. Physiology)",
                testTag = "paper_subject_input"
            )
        }
        Box(modifier = Modifier.weight(1f)) {
            StyledFormField(
                value = uiState.term,
                onValueChange = onTermChange,
                placeholder = "Term / Batch",
                testTag = "paper_term_input"
            )
        }
    }

    Spacer(Modifier.height(20.dp))

    Text(
        text = "UPLOAD PAST PAPER SOURCE",
        style = MaterialTheme.typography.labelMedium,
        color = ext.accent
    )
    Spacer(Modifier.height(10.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PaperUploadSource.entries.forEach { source ->
            val selected = uiState.uploadSource == source
            Surface(
                onClick = { onSourceChange(source) },
                shape = RoundedCornerShape(14.dp),
                color = if (selected) ext.accentSoft else ext.card,
                border = BorderStroke(1.dp, if (selected) ext.accent else ext.border),
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .testTag("paper_source_${source.name.lowercase()}")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = source.label,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold
                        ),
                        color = if (selected) ext.accent else ext.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }

    Spacer(Modifier.height(14.dp))

    when (uiState.uploadSource) {
        PaperUploadSource.PDF -> {
            OutlinedButton(
                onClick = onUploadPdfClick,
                enabled = !isBusy,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("upload_past_paper_pdf_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = ext.accentSoft,
                    contentColor = ext.accent
                ),
                border = BorderStroke(1.dp, ext.accent)
            ) {
                Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    text = uiState.attachedPdfName?.let { "Upload Another PDF ($it)" }
                        ?: "Upload Past Paper PDF",
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Extracts question, options, correct responses, explanation, year, and lesson from PDF.",
                style = MaterialTheme.typography.bodySmall,
                color = ext.subtext
            )
        }

        PaperUploadSource.IMAGES -> {
            OutlinedButton(
                onClick = onUploadImagesClick,
                enabled = !isBusy,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("upload_past_paper_images_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = ext.accentSoft,
                    contentColor = ext.accent
                ),
                border = BorderStroke(1.dp, ext.accent)
            ) {
                Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    text = if (uiState.selectedImageCount > 0) {
                        "Upload More Images (${uiState.selectedImageCount} analyzed)"
                    } else {
                        "Upload Past Paper Images"
                    },
                    style = MaterialTheme.typography.labelLarge
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Select up to 10 photos/screenshots of past paper pages for AI extraction.",
                style = MaterialTheme.typography.bodySmall,
                color = ext.subtext
            )
        }

        PaperUploadSource.TEXT, PaperUploadSource.JSON -> {
            OutlinedTextField(
                value = uiState.rawText,
                onValueChange = onRawTextChange,
                placeholder = {
                    Text(
                        text = if (uiState.uploadSource == PaperUploadSource.JSON) {
                            "Paste structured JSON directly here..."
                        } else {
                            "Paste raw past paper text here (MCQs, T/F, Short Answers, or SEQs)..."
                        },
                        color = ext.subtext
                    )
                },
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = JetBrainsMonoFontFamily,
                    color = ext.title
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 170.dp)
                    .testTag("paper_raw_input"),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ext.accent,
                    unfocusedBorderColor = ext.border,
                    focusedContainerColor = ext.card,
                    unfocusedContainerColor = ext.card
                )
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onExtractRawClick,
                    enabled = !isBusy,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ext.accent,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("extract_raw_paper_button")
                ) {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (uiState.uploadSource == PaperUploadSource.JSON) "Parse JSON" else "Extract Questions",
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                Surface(
                    onClick = onCopyPromptClick,
                    shape = RoundedCornerShape(14.dp),
                    color = ext.card,
                    border = BorderStroke(1.dp, ext.border),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .size(48.dp)
                        .testTag("copy_add_paper_prompt_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.ContentCopy,
                            contentDescription = "Copy converting prompt",
                            tint = ext.accent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }

    if (uiState.extractionError != null) {
        Spacer(Modifier.height(12.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = ext.dangerSoft,
            border = BorderStroke(1.dp, ext.danger)
        ) {
            Text(
                text = uiState.extractionError,
                style = MaterialTheme.typography.bodySmall,
                color = ext.danger,
                modifier = Modifier.padding(12.dp)
            )
        }
    }

    Spacer(Modifier.height(14.dp))
    OutlinedButton(
        onClick = onAddManualQuestion,
        enabled = !isBusy,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, ext.border),
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("add_manual_question_button")
    ) {
        Icon(Icons.Rounded.Add, contentDescription = null, tint = ext.accent, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("Add Question Manually", color = ext.accent, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun ExtractedQuestionsReviewSection(
    questions: List<QuizQuestion>,
    defaultYear: String,
    defaultLesson: String,
    onAddQuestion: () -> Unit,
    onEditQuestion: (Int, QuizQuestion) -> Unit,
    onRemoveQuestion: (Int) -> Unit
) {
    val ext = LocalQuizzMeColors.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "QUESTIONS READY (${questions.size})",
            style = MaterialTheme.typography.labelMedium,
            color = ext.accent
        )
        if (questions.isNotEmpty()) {
            TextButton(onClick = onAddQuestion) {
                Icon(Icons.Rounded.Add, contentDescription = null, tint = ext.accent, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Add More", color = ext.accent)
            }
        }
    }
    Spacer(Modifier.height(8.dp))

    if (questions.isEmpty()) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = ext.card,
            border = BorderStroke(1.dp, ext.border)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "No questions added yet",
                    style = MaterialTheme.typography.titleSmall,
                    color = ext.title
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Upload a PDF or images, paste text/JSON, or add questions manually with options, correct responses, explanation, year, and lesson.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ext.subtext
                )
            }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            questions.forEachIndexed { idx, q ->
                val yearLabel = q.year?.takeIf { it.isNotBlank() } ?: defaultYear
                val lessonLabel = q.lesson?.takeIf { it.isNotBlank() } ?: defaultLesson
                val correctList = q.resolvedCorrectResponses()

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = ext.card,
                    border = BorderStroke(1.dp, ext.border)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(shape = RoundedCornerShape(8.dp), color = ext.accentSoft) {
                                    Text(
                                        text = "Q${idx + 1} • ${q.detectedStyle().shortLabel}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ext.accent,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                if (yearLabel.isNotBlank()) {
                                    Text(
                                        text = yearLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ext.subtext
                                    )
                                }
                                if (lessonLabel.isNotBlank()) {
                                    Text(
                                        text = "• $lessonLabel",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ext.subtext,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Row {
                                IconButton(
                                    onClick = { onEditQuestion(idx, q) },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(Icons.Rounded.Edit, contentDescription = "Edit", tint = ext.accent, modifier = Modifier.size(18.dp))
                                }
                                IconButton(
                                    onClick = { onRemoveQuestion(idx) },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(Icons.Rounded.Delete, contentDescription = "Remove", tint = ext.danger, modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = q.question,
                            style = MaterialTheme.typography.titleSmall,
                            color = ext.title
                        )

                        if (!q.options.isNullOrEmpty()) {
                            Spacer(Modifier.height(6.dp))
                            q.options.forEachIndexed { optIdx, opt ->
                                val isCorrect = correctList.any { it.equals(opt, ignoreCase = true) }
                                Text(
                                    text = "${optIdx + 1}. $opt" + if (isCorrect) " ✓" else "",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = if (isCorrect) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = if (isCorrect) ext.success else ext.text
                                )
                            }
                        } else if (correctList.isNotEmpty()) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "Correct: ${correctList.joinToString(" / ")}",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = ext.success
                            )
                        }

                        if (!q.explanation.isNullOrBlank()) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "Explained: ${q.explanation}",
                                style = MaterialTheme.typography.bodySmall,
                                color = ext.subtext,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DraftLessonLinkDialog(
    lectures: List<LectureEntity>,
    selectedLectureId: String?,
    onDismiss: () -> Unit,
    onSelect: (LectureEntity?) -> Unit
) {
    val ext = LocalQuizzMeColors.current
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 520.dp),
            shape = RoundedCornerShape(22.dp),
            color = ext.card,
            border = BorderStroke(1.dp, ext.border)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Link to Lesson",
                    style = MaterialTheme.typography.titleLarge,
                    color = ext.title
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Select a lesson to link with this past paper.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ext.subtext
                )
                Spacer(Modifier.height(12.dp))

                if (lectures.isEmpty()) {
                    Text(
                        text = "No lessons saved yet. You can still enter a lesson name manually.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ext.subtext,
                        modifier = Modifier.padding(vertical = 20.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(lectures, key = { it.id }) { lecture ->
                            val isSelected = lecture.id == selectedLectureId
                            Surface(
                                onClick = { onSelect(lecture) },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) ext.accentSoft else ext.background,
                                border = BorderStroke(1.dp, if (isSelected) ext.accent else ext.border),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.School,
                                            contentDescription = null,
                                            tint = if (isSelected) ext.accent else ext.subtext,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Column {
                                            Text(
                                                text = lecture.lesson,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = if (isSelected) ext.accent else ext.title
                                            )
                                            Text(
                                                text = "${lecture.subject} • ${lecture.term}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = ext.subtext
                                            )
                                        }
                                    }
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Rounded.CheckCircle,
                                            contentDescription = "Selected",
                                            tint = ext.accent,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (selectedLectureId != null) {
                        TextButton(onClick = { onSelect(null) }) {
                            Text("Unlink", color = ext.danger)
                        }
                    } else {
                        Spacer(Modifier.width(1.dp))
                    }
                    TextButton(onClick = onDismiss) {
                        Text("Close", color = ext.subtext)
                    }
                }
            }
        }
    }
}

private fun encodeUriToCompressedJpeg(context: Context, uri: Uri): ByteArray? {
    return runCatching {
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, boundsOptions)
        }
        val maxDim = 1600
        var sampleSize = 1
        val width = boundsOptions.outWidth
        val height = boundsOptions.outHeight
        if (width > maxDim || height > maxDim) {
            var halfW = width / 2
            var halfH = height / 2
            while ((halfW / sampleSize) >= maxDim || (halfH / sampleSize) >= maxDim) {
                sampleSize *= 2
            }
        }
        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val decoded = context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOptions)
        } ?: return null

        val scaled = if (decoded.width > maxDim || decoded.height > maxDim) {
            val scale = minOf(maxDim.toFloat() / decoded.width, maxDim.toFloat() / decoded.height)
            val targetW = (decoded.width * scale).toInt().coerceAtLeast(1)
            val targetH = (decoded.height * scale).toInt().coerceAtLeast(1)
            Bitmap.createScaledBitmap(decoded, targetW, targetH, true).also {
                if (it !== decoded) decoded.recycle()
            }
        } else {
            decoded
        }

        ByteArrayOutputStream().use { baos ->
            scaled.compress(Bitmap.CompressFormat.JPEG, 82, baos)
            scaled.recycle()
            baos.toByteArray()
        }
    }.getOrNull()
}
