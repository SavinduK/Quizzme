package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AiRepository
import com.example.ui.components.QuizzMeAlertDialog
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalQuizzMeColors
import com.example.ui.viewmodel.AddContentViewModel
import com.example.ui.viewmodel.AddPaperUiState
import com.example.ui.viewmodel.PaperFormatMode
import kotlinx.coroutines.launch

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

    BackHandler { onBack() }

    fun copyPastPaperConvertingPrompt() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(
            ClipData.newPlainText("QuizzMe Past Paper Converting Prompt", AiRepository.PAST_PAPER_CONVERT_PROMPT)
        )
        scope.launch {
            snackbarHostState.showSnackbar("Converting prompt copied to clipboard")
        }
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
                    enabled = !uiState.processing,
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
                    if (uiState.processing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (uiState.formatMode == PaperFormatMode.JSON) "Saving..." else "Converting...",
                            style = MaterialTheme.typography.labelLarge
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Save,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (uiState.formatMode == PaperFormatMode.JSON) "Save JSON" else "Convert & Save",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
            HorizontalDivider(color = ext.border)

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
                        PaperMetadataSection(
                            uiState = uiState,
                            onSubjectChange = viewModel::setPaperSubject,
                            onTermChange = viewModel::setPaperTerm,
                            onLessonChange = viewModel::setPaperLesson,
                            onModeChange = viewModel::setPaperFormatMode,
                            onCopyPromptClick = ::copyPastPaperConvertingPrompt
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(0.56f)
                            .fillMaxHeight()
                    ) {
                        PaperInputEditorPanel(
                            uiState = uiState,
                            onRawTextChange = viewModel::setPaperRawText,
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
                    PaperMetadataSection(
                        uiState = uiState,
                        onSubjectChange = viewModel::setPaperSubject,
                        onTermChange = viewModel::setPaperTerm,
                        onLessonChange = viewModel::setPaperLesson,
                        onModeChange = viewModel::setPaperFormatMode,
                        onCopyPromptClick = ::copyPastPaperConvertingPrompt
                    )

                    Spacer(Modifier.height(20.dp))

                    PaperInputEditorPanel(
                        uiState = uiState,
                        onRawTextChange = viewModel::setPaperRawText,
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
            onDismiss = { viewModel.dismissPaperAlert(onBack) }
        )
    }
}

@Composable
private fun PaperMetadataSection(
    uiState: AddPaperUiState,
    onSubjectChange: (String) -> Unit,
    onTermChange: (String) -> Unit,
    onLessonChange: (String) -> Unit,
    onModeChange: (PaperFormatMode) -> Unit,
    onCopyPromptClick: () -> Unit
) {
    val ext = LocalQuizzMeColors.current

    Text(
        text = "PAPER DATA",
        style = MaterialTheme.typography.labelMedium,
        color = ext.accent
    )
    Spacer(Modifier.height(12.dp))

    StyledFormField(
        value = uiState.subject,
        onValueChange = onSubjectChange,
        placeholder = "Subject Name (e.g., Physiology)",
        testTag = "paper_subject_input"
    )
    Spacer(Modifier.height(12.dp))

    StyledFormField(
        value = uiState.term,
        onValueChange = onTermChange,
        placeholder = "Term / Year (e.g., 2023 Batch)",
        testTag = "paper_term_input"
    )
    Spacer(Modifier.height(12.dp))

    StyledFormField(
        value = uiState.lesson,
        onValueChange = onLessonChange,
        placeholder = "Lesson / Topic (e.g., Cardiovascular)",
        testTag = "paper_lesson_input"
    )

    Spacer(Modifier.height(18.dp))

    Text(
        text = "FORMAT MODE",
        style = MaterialTheme.typography.labelMedium,
        color = ext.accent
    )
    Spacer(Modifier.height(10.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FormatModeChip(
            label = "AI Convert (Text)",
            selected = uiState.formatMode == PaperFormatMode.AUTO,
            onClick = { onModeChange(PaperFormatMode.AUTO) },
            testTag = "paper_mode_auto",
            modifier = Modifier.weight(1f)
        )
        FormatModeChip(
            label = "Raw JSON",
            selected = uiState.formatMode == PaperFormatMode.JSON,
            onClick = { onModeChange(PaperFormatMode.JSON) },
            testTag = "paper_mode_json",
            modifier = Modifier.weight(1f)
        )
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

    AnimatedVisibility(visible = uiState.processing) {
        LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = ext.accent,
            trackColor = ext.border,
            strokeCap = StrokeCap.Round
        )
    }
}

@Composable
private fun PaperInputEditorPanel(
    uiState: AddPaperUiState,
    onRawTextChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalQuizzMeColors.current
    Column(modifier = modifier) {
        Text(
            text = "QUESTION INPUT",
            style = MaterialTheme.typography.labelMedium,
            color = ext.accent
        )
        Spacer(Modifier.height(10.dp))

        OutlinedTextField(
            value = uiState.rawText,
            onValueChange = onRawTextChange,
            placeholder = {
                Text(
                    text = if (uiState.formatMode == PaperFormatMode.JSON) {
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
                .weight(1f, fill = false)
                .heightIn(min = 240.dp)
                .testTag("paper_raw_input"),
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
private fun FormatModeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val ext = LocalQuizzMeColors.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (selected) ext.accentSoft else ext.card,
        border = BorderStroke(1.dp, if (selected) ext.accent else ext.border),
        modifier = modifier
            .height(48.dp)
            .testTag(testTag)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold
                ),
                color = if (selected) ext.accent else ext.title
            )
        }
    }
}
