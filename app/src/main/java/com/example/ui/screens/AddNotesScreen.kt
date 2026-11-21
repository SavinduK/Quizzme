package com.example.ui.screens

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PictureAsPdf
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.QuizzMeAlertDialog
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalQuizzMeColors
import com.example.ui.viewmodel.AddContentViewModel

@Composable
fun AddNotesScreen(
    viewModel: AddContentViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.notesState.collectAsStateWithLifecycle()
    val ext = LocalQuizzMeColors.current
    val context = LocalContext.current

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

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
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
                text = "NOTE INFO",
                style = MaterialTheme.typography.labelMedium,
                color = ext.accent
            )
            Spacer(Modifier.height(12.dp))

            StyledFormField(
                value = uiState.subject,
                onValueChange = viewModel::setNotesSubject,
                placeholder = "Subject Name (e.g., Physiology)",
                testTag = "notes_subject_input"
            )
            Spacer(Modifier.height(12.dp))

            StyledFormField(
                value = uiState.term,
                onValueChange = viewModel::setNotesTerm,
                placeholder = "Term Name (e.g., Term 2)",
                testTag = "notes_term_input"
            )
            Spacer(Modifier.height(12.dp))

            StyledFormField(
                value = uiState.lesson,
                onValueChange = viewModel::setNotesLesson,
                placeholder = "Lesson / Topic (e.g., Cardiovascular)",
                testTag = "notes_lesson_input"
            )
            Spacer(Modifier.height(14.dp))

            OutlinedButton(
                onClick = { pdfPickerLauncher.launch(arrayOf("application/pdf")) },
                enabled = !uiState.processingPdf,
                modifier = Modifier
                    .fillMaxWidth()
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
                    Text("AI Ingestion Tool Processing...", color = ext.title)
                } else {
                    Icon(
                        imageVector = Icons.Rounded.PictureAsPdf,
                        contentDescription = null,
                        tint = ext.accent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = uiState.attachedFileName?.let { "Change: $it" } ?: "Upload PDF Notes",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            Spacer(Modifier.height(22.dp))

            Text(
                text = "TEXT NOTES",
                style = MaterialTheme.typography.labelMedium,
                color = ext.accent
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = uiState.notesText,
                onValueChange = viewModel::setNotesText,
                placeholder = {
                    Text(
                        "Upload PDF to extract notes, or paste your notes manually...",
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
                    .testTag("notes_content_input"),
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
                onClick = viewModel::saveNotes,
                enabled = !uiState.processingPdf && !uiState.saving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("save_notes_button"),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = ext.accentSoft,
                    contentColor = ext.accent
                ),
                border = BorderStroke(1.dp, ext.accent)
            ) {
                if (uiState.saving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = ext.accent
                    )
                } else {
                    Text("Save Notes", style = MaterialTheme.typography.titleMedium)
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

private fun resolveDisplayName(context: android.content.Context, uri: Uri): String? {
    return context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx) else null
    }
}
