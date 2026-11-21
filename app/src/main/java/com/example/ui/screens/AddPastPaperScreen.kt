package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.QuizzMeAlertDialog
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalQuizzMeColors
import com.example.ui.viewmodel.AddContentViewModel
import com.example.ui.viewmodel.PaperFormatMode

@Composable
fun AddPastPaperScreen(
    viewModel: AddContentViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.paperState.collectAsStateWithLifecycle()
    val ext = LocalQuizzMeColors.current

    BackHandler { onBack() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
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
                color = ext.title
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .widthIn(max = 680.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
        ) {
            Text(
                text = "PAPER DATA",
                style = MaterialTheme.typography.labelMedium,
                color = ext.accent
            )
            Spacer(Modifier.height(12.dp))

            StyledFormField(
                value = uiState.subject,
                onValueChange = viewModel::setPaperSubject,
                placeholder = "Subject Name (e.g., Physiology)",
                testTag = "paper_subject_input"
            )
            Spacer(Modifier.height(12.dp))

            StyledFormField(
                value = uiState.term,
                onValueChange = viewModel::setPaperTerm,
                placeholder = "Term / Year (e.g., 2023 Batch)",
                testTag = "paper_term_input"
            )
            Spacer(Modifier.height(12.dp))

            StyledFormField(
                value = uiState.lesson,
                onValueChange = viewModel::setPaperLesson,
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
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FormatModeChip(
                    label = "AI Auto-Detect (Text)",
                    selected = uiState.formatMode == PaperFormatMode.AUTO,
                    onClick = { viewModel.setPaperFormatMode(PaperFormatMode.AUTO) },
                    testTag = "paper_mode_auto",
                    modifier = Modifier.weight(1f)
                )
                FormatModeChip(
                    label = "Raw JSON",
                    selected = uiState.formatMode == PaperFormatMode.JSON,
                    onClick = { viewModel.setPaperFormatMode(PaperFormatMode.JSON) },
                    testTag = "paper_mode_json",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(20.dp))

            Text(
                text = "QUESTION INPUT",
                style = MaterialTheme.typography.labelMedium,
                color = ext.accent
            )
            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = uiState.rawText,
                onValueChange = viewModel::setPaperRawText,
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
                    .heightIn(min = 220.dp)
                    .testTag("paper_raw_input"),
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ext.accent,
                    unfocusedBorderColor = ext.border,
                    focusedContainerColor = ext.card,
                    unfocusedContainerColor = ext.card
                )
            )

            Spacer(Modifier.height(24.dp))

            OutlinedButton(
                onClick = viewModel::savePastPaper,
                enabled = !uiState.processing,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("save_paper_button"),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = ext.accentSoft,
                    contentColor = ext.accent
                ),
                border = BorderStroke(1.dp, ext.accent)
            ) {
                if (uiState.processing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = ext.accent
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = if (uiState.formatMode == PaperFormatMode.JSON) "Saving JSON..." else "Detecting & Structuring...",
                        style = MaterialTheme.typography.titleMedium
                    )
                } else {
                    Text(
                        text = if (uiState.formatMode == PaperFormatMode.JSON) "Format & Save JSON" else "Process & Save Paper",
                        style = MaterialTheme.typography.titleMedium
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
