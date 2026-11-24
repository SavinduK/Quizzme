package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Save
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
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AiRepository
import com.example.ui.components.QuizzMeAlertDialog
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalQuizzMeColors
import com.example.ui.viewmodel.AddContentViewModel
import com.example.ui.viewmodel.AddNotesUiState
import kotlinx.coroutines.launch

@Composable
fun AddNotesScreen(
    viewModel: AddContentViewModel,
    widthSizeClass: WindowWidthSizeClass,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.notesState.collectAsStateWithLifecycle()
    val ext = LocalQuizzMeColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val isTablet = widthSizeClass != WindowWidthSizeClass.Compact

    BackHandler { onBack() }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = resolveDisplayName(context, uri) ?: "document.pdf"
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            if (bytes != null) {
                viewModel.processPdfBytes(fileName, bytes)
            }
        }
    }

    fun copyPdfConvertingPrompt() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(
            ClipData.newPlainText("QuizzMe PDF Converting Prompt", AiRepository.PDF_TO_MARKDOWN_PROMPT)
        )
        scope.launch {
            snackbarHostState.showSnackbar("Converting prompt copied to clipboard")
        }
    }

    val isUploadingOrSaving = uiState.processingPdf || uiState.saving

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
            // Top Bar with Save Button at Top Right
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
                            modifier = Modifier.testTag("add_notes_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Back",
                                tint = ext.title
                            )
                        }
                    }

                    Text(
                        text = "Add Notes",
                        style = MaterialTheme.typography.headlineLarge,
                        color = ext.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Button(
                    onClick = viewModel::saveNotes,
                    enabled = !isUploadingOrSaving,
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
                        .testTag("save_notes_button")
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
                        Text("Save", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
            HorizontalDivider(color = ext.border)

            // Pinned Loading Bar for Lesson PDF Upload & Save
            AnimatedVisibility(visible = isUploadingOrSaving) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = if (uiState.processingPdf) {
                            "Uploading & converting ${uiState.attachedFileName ?: "PDF"}..."
                        } else {
                            "Saving lesson notes..."
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = ext.accent
                    )
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .testTag("lesson_upload_loading_bar"),
                        color = ext.accent,
                        trackColor = ext.border,
                        strokeCap = StrokeCap.Round
                    )
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
                            .weight(0.44f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = 16.dp)
                    ) {
                        NotesMetadataAndUploadSection(
                            uiState = uiState,
                            onSubjectChange = viewModel::setNotesSubject,
                            onTermChange = viewModel::setNotesTerm,
                            onLessonChange = viewModel::setNotesLesson,
                            onUploadPdfClick = { pdfPickerLauncher.launch(arrayOf("application/pdf")) },
                            onCopyPromptClick = ::copyPdfConvertingPrompt
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(0.56f)
                            .fillMaxHeight()
                    ) {
                        MarkdownNotesEditorPanel(
                            notesText = uiState.notesText,
                            onNotesTextChange = viewModel::setNotesText,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
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
                    NotesMetadataAndUploadSection(
                        uiState = uiState,
                        onSubjectChange = viewModel::setNotesSubject,
                        onTermChange = viewModel::setNotesTerm,
                        onLessonChange = viewModel::setNotesLesson,
                        onUploadPdfClick = { pdfPickerLauncher.launch(arrayOf("application/pdf")) },
                        onCopyPromptClick = ::copyPdfConvertingPrompt
                    )

                    Spacer(Modifier.height(22.dp))

                    MarkdownNotesEditorPanel(
                        notesText = uiState.notesText,
                        onNotesTextChange = viewModel::setNotesText,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 240.dp)
                    )
                }
            }
        }
    }

    if (uiState.alertTitle != null && uiState.alertMessage != null) {
        QuizzMeAlertDialog(
            title = uiState.alertTitle!!,
            message = uiState.alertMessage!!,
            isSuccess = uiState.alertIsSuccess,
            onDismiss = { viewModel.dismissNotesAlert(onBack) }
        )
    }
}

@Composable
private fun NotesMetadataAndUploadSection(
    uiState: AddNotesUiState,
    onSubjectChange: (String) -> Unit,
    onTermChange: (String) -> Unit,
    onLessonChange: (String) -> Unit,
    onUploadPdfClick: () -> Unit,
    onCopyPromptClick: () -> Unit
) {
    val ext = LocalQuizzMeColors.current

    Text(
        text = "NOTE INFO",
        style = MaterialTheme.typography.labelMedium,
        color = ext.accent
    )
    Spacer(Modifier.height(12.dp))

    StyledFormField(
        value = uiState.subject,
        onValueChange = onSubjectChange,
        placeholder = "Subject Name (e.g., Physiology)",
        testTag = "notes_subject_input"
    )
    Spacer(Modifier.height(12.dp))

    StyledFormField(
        value = uiState.term,
        onValueChange = onTermChange,
        placeholder = "Term Name (e.g., Term 2)",
        testTag = "notes_term_input"
    )
    Spacer(Modifier.height(12.dp))

    StyledFormField(
        value = uiState.lesson,
        onValueChange = onLessonChange,
        placeholder = "Lesson / Topic (e.g., Cardiovascular)",
        testTag = "notes_lesson_input"
    )
    Spacer(Modifier.height(14.dp))

    // PDF Convert Button + Copy Converting Prompt Icon Button
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedButton(
            onClick = onUploadPdfClick,
            enabled = !uiState.processingPdf && !uiState.saving,
            modifier = Modifier
                .weight(1f)
                .height(54.dp)
                .testTag("upload_pdf_button"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = ext.accentSoft,
                contentColor = ext.accent
            ),
            border = BorderStroke(1.dp, ext.accent)
        ) {
            if (uiState.processingPdf) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = ext.accent
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Converting PDF to Markdown...",
                    color = ext.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.PictureAsPdf,
                    contentDescription = null,
                    tint = ext.accent,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = uiState.attachedFileName?.let { "Convert PDF: $it" } ?: "Upload & Convert PDF",
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Surface(
            onClick = onCopyPromptClick,
            shape = RoundedCornerShape(16.dp),
            color = ext.card,
            border = BorderStroke(1.dp, ext.border),
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .size(54.dp)
                .testTag("copy_pdf_convert_prompt_button")
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.ContentCopy,
                    contentDescription = "Copy PDF converting prompt",
                    tint = ext.accent,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }

    // Prominent Loading Bar during PDF Conversion
    AnimatedVisibility(visible = uiState.processingPdf) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
                .testTag("pdf_conversion_loading_card"),
            shape = RoundedCornerShape(14.dp),
            color = ext.card,
            border = BorderStroke(1.dp, ext.accent)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Converting PDF to Markdown...",
                        style = MaterialTheme.typography.labelLarge,
                        color = ext.accent
                    )
                    uiState.attachedFileName?.let { name ->
                        Text(
                            text = name,
                            style = MaterialTheme.typography.labelSmall,
                            color = ext.subtext,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 160.dp)
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .testTag("pdf_conversion_loading_bar"),
                    color = ext.accent,
                    trackColor = ext.border,
                    strokeCap = StrokeCap.Round
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Analyzing document structure and writing clean Markdown notes...",
                    style = MaterialTheme.typography.bodySmall,
                    color = ext.subtext
                )
            }
        }
    }
}

@Composable
private fun MarkdownNotesEditorPanel(
    notesText: String,
    onNotesTextChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalQuizzMeColors.current
    Column(modifier = modifier) {
        Text(
            text = "MARKDOWN NOTES",
            style = MaterialTheme.typography.labelMedium,
            color = ext.accent
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = notesText,
            onValueChange = onNotesTextChange,
            placeholder = {
                Text(
                    "Upload PDF to extract Markdown notes, or paste/type Markdown notes manually...",
                    color = ext.subtext
                )
            },
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = JetBrainsMonoFontFamily,
                color = ext.title
            ),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .heightIn(min = 240.dp)
                .testTag("notes_content_input"),
            shape = RoundedCornerShape(20.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ext.accent,
                unfocusedBorderColor = ext.border,
                focusedContainerColor = ext.card,
                unfocusedContainerColor = ext.card
            )
        )
    }
}

@Composable
internal fun StyledFormField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    testTag: String
) {
    val ext = LocalQuizzMeColors.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        placeholder = { Text(placeholder, color = ext.subtext) },
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = ext.accent,
            unfocusedBorderColor = ext.border,
            focusedContainerColor = ext.card,
            unfocusedContainerColor = ext.card,
            focusedTextColor = ext.title,
            unfocusedTextColor = ext.title
        )
    )
}

internal fun resolveDisplayName(context: Context, uri: Uri): String? {
    return context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx) else null
    }
}
