package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
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
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.QuizzMeAlertDialog
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalQuizzMeColors
import com.example.ui.viewmodel.AddContentViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

@Composable
fun AddNotesScreen(
    viewModel: AddContentViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.notesState.collectAsStateWithLifecycle()
    val ext = LocalQuizzMeColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var preparingImages by remember { mutableStateOf(false) }

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

    val paperImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            scope.launch {
                preparingImages = true
                val encodedImages = withContext(Dispatchers.IO) {
                    uris.mapNotNull { encodeUriToCompressedJpeg(context, it) }
                }
                preparingImages = false
                if (encodedImages.isNotEmpty()) {
                    viewModel.processPastPaperImages(encodedImages)
                } else {
                    viewModel.setPaperImagesError("Could not read the selected image(s).")
                }
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
                    Text("Extracting HTML Notes...", color = ext.title)
                } else {
                    Icon(
                        imageVector = Icons.Rounded.PictureAsPdf,
                        contentDescription = null,
                        tint = ext.accent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = uiState.attachedFileName?.let { "Change PDF: $it" } ?: "Upload PDF Notes",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            Spacer(Modifier.height(22.dp))

            Text(
                text = "PAST PAPER IMAGES (OPTIONAL)",
                style = MaterialTheme.typography.labelMedium,
                color = ext.accent
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Upload past paper photos or screenshots to extract questions for the Past Paper section and quiz generation.",
                style = MaterialTheme.typography.bodySmall,
                color = ext.subtext
            )
            Spacer(Modifier.height(10.dp))

            val isAnalyzingImages = preparingImages || uiState.processingPaperImages
            OutlinedButton(
                onClick = {
                    paperImagePickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                enabled = !isAnalyzingImages && !uiState.saving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("upload_past_paper_images_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = ext.card,
                    contentColor = ext.accent
                ),
                border = BorderStroke(1.dp, ext.border)
            ) {
                if (isAnalyzingImages) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = ext.accent
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = if (preparingImages) {
                            "Preparing Images..."
                        } else {
                            "Analyzing ${uiState.selectedPaperImageCount} Image(s)..."
                        },
                        color = ext.title
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.AddPhotoAlternate,
                        contentDescription = null,
                        tint = ext.accent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = if (uiState.extractedPaperQuestions.isNotEmpty()) {
                            "Replace Past Paper Images (${uiState.selectedPaperImageCount})"
                        } else {
                            "Upload Past Paper Images"
                        },
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            if (uiState.paperImagesError != null) {
                Spacer(Modifier.height(10.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = ext.card,
                    border = BorderStroke(1.dp, ext.danger)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = uiState.paperImagesError!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = ext.danger,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = viewModel::clearPastPaperImages) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Dismiss image error",
                                tint = ext.danger,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            if (uiState.extractedPaperQuestions.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                val questions = uiState.extractedPaperQuestions
                val breakdown = questions.groupingBy { it.detectedStyle().shortLabel }
                    .eachCount()
                    .entries
                    .joinToString(" • ") { "${it.key}: ${it.value}" }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("extracted_paper_preview_card"),
                    shape = RoundedCornerShape(16.dp),
                    color = ext.accentSoft,
                    border = BorderStroke(1.dp, ext.accent)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    tint = ext.accent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "${questions.size} Past Paper Question(s) Ready",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = ext.title
                                )
                            }
                            TextButton(
                                onClick = viewModel::clearPastPaperImages,
                                modifier = Modifier.testTag("clear_paper_images_button")
                            ) {
                                Text("Clear", color = ext.danger)
                            }
                        }

                        if (breakdown.isNotBlank()) {
                            Text(
                                text = breakdown,
                                style = MaterialTheme.typography.labelSmall,
                                color = ext.accent
                            )
                            Spacer(Modifier.height(8.dp))
                        }

                        questions.take(3).forEachIndexed { idx, q ->
                            Text(
                                text = "${idx + 1}. [${q.detectedStyle().shortLabel}] ${q.question}",
                                style = MaterialTheme.typography.bodySmall,
                                color = ext.text,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                        if (questions.size > 3) {
                            Text(
                                text = "+${questions.size - 3} more question(s)",
                                style = MaterialTheme.typography.labelSmall,
                                color = ext.subtext,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(22.dp))

            Text(
                text = "HTML NOTES",
                style = MaterialTheme.typography.labelMedium,
                color = ext.accent
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = uiState.notesText,
                onValueChange = viewModel::setNotesText,
                placeholder = {
                    Text(
                        "Upload PDF to extract HTML notes, or paste/type HTML notes manually...",
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
                enabled = !uiState.processingPdf && !isAnalyzingImages && !uiState.saving,
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
                    Text(
                        text = if (uiState.extractedPaperQuestions.isNotEmpty()) {
                            "Save Notes & Past Paper"
                        } else {
                            "Save Notes"
                        },
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

private fun resolveDisplayName(context: Context, uri: Uri): String? {
    return context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx) else null
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
