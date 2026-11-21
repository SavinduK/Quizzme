package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.LectureEntity
import com.example.ui.components.DeleteConfirmDialog
import com.example.ui.components.QuizzMeHeader
import com.example.ui.theme.LocalQuizzMeColors
import com.example.ui.theme.subjectAccentColor
import com.example.ui.viewmodel.LessonsViewModel
import com.example.ui.viewmodel.ReaderTab
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LessonsScreen(
    viewModel: LessonsViewModel,
    onAddNotesClick: () -> Unit,
    onOpenLesson: (lectureId: String, initialTab: ReaderTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val ext = LocalQuizzMeColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var showFilterModal by remember { mutableStateOf(false) }
    var deleteTargetId by remember { mutableStateOf<String?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            QuizzMeHeader(
                title = "Lectures",
                onAddClick = onAddNotesClick,
                addButtonTag = "lessons_add_notes_button"
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .widthIn(max = 680.dp)
                    .fillMaxWidth()
            ) {
                // Search & Filter Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = viewModel::setSearchQuery,
                        placeholder = { Text("Search modules...", color = ext.subtext) },
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = null,
                                tint = ext.subtext,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "Clear search",
                                        tint = ext.subtext,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ext.accent,
                            unfocusedBorderColor = ext.border,
                            focusedContainerColor = ext.card,
                            unfocusedContainerColor = ext.card,
                            focusedTextColor = ext.title,
                            unfocusedTextColor = ext.title
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("lessons_search_input")
                    )

                    Surface(
                        onClick = { showFilterModal = true },
                        shape = RoundedCornerShape(14.dp),
                        color = ext.card,
                        border = BorderStroke(
                            1.dp,
                            if (uiState.hasActiveFilter) ext.accent else ext.border
                        ),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .size(54.dp)
                            .testTag("lessons_filter_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Tune,
                                contentDescription = "Filter lectures",
                                tint = if (uiState.hasActiveFilter) ext.accent else ext.title,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                if (uiState.filteredLessons.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 64.dp, start = 32.dp, end = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FolderOpen,
                            contentDescription = null,
                            tint = ext.border,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = if (uiState.allLessons.isEmpty()) {
                                "No lecture notes yet. Tap + to upload a PDF or paste notes."
                            } else {
                                "No matches found for your current criteria."
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = ext.subtext,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.filteredLessons, key = { it.id }) { lecture ->
                            LessonModuleCard(
                                lecture = lecture,
                                onOpenNotes = { onOpenLesson(lecture.id, ReaderTab.NOTES) },
                                onQuickQuiz = { onOpenLesson(lecture.id, ReaderTab.QUIZ) },
                                onShare = {
                                    shareLessonMarkdown(context, lecture)
                                },
                                onCopy = {
                                    copyLessonToClipboard(context, lecture)
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Copied ${lecture.lesson} to clipboard")
                                    }
                                },
                                onDelete = { deleteTargetId = lecture.id }
                            )
                        }
                        item { Spacer(Modifier.height(16.dp)) }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
    }

    if (deleteTargetId != null) {
        DeleteConfirmDialog(
            title = "Delete Dataset",
            message = "Are you sure you want to permanently delete this lecture and its cached quizzes?",
            onCancel = { deleteTargetId = null },
            onConfirm = {
                deleteTargetId?.let { viewModel.deleteLesson(it) }
                deleteTargetId = null
            }
        )
    }

    if (showFilterModal) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showFilterModal = false },
            sheetState = sheetState,
            containerColor = ext.card,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 28.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Filter Options",
                        style = MaterialTheme.typography.titleLarge,
                        color = ext.title
                    )
                    IconButton(onClick = { showFilterModal = false }) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close", tint = ext.title)
                    }
                }
                HorizontalDivider(color = ext.border)

                Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 18.dp)) {
                    Text("SUBJECT", style = MaterialTheme.typography.labelSmall, color = ext.subtext)
                    Spacer(Modifier.height(10.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LessonFilterChip(
                            label = "All",
                            selected = uiState.selectedSubject == null,
                            onClick = { viewModel.setSelectedSubject(null) }
                        )
                        uiState.uniqueSubjects.forEach { sub ->
                            LessonFilterChip(
                                label = sub,
                                selected = uiState.selectedSubject.equals(sub, ignoreCase = true),
                                onClick = { viewModel.setSelectedSubject(sub) }
                            )
                        }
                    }

                    Spacer(Modifier.height(22.dp))

                    Text("TERM", style = MaterialTheme.typography.labelSmall, color = ext.subtext)
                    Spacer(Modifier.height(10.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LessonFilterChip(
                            label = "All",
                            selected = uiState.selectedTerm == null,
                            onClick = { viewModel.setSelectedTerm(null) }
                        )
                        uiState.uniqueTerms.forEach { term ->
                            LessonFilterChip(
                                label = term,
                                selected = uiState.selectedTerm.equals(term, ignoreCase = true),
                                onClick = { viewModel.setSelectedTerm(term) }
                            )
                        }
                    }

                    Spacer(Modifier.height(28.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = viewModel::resetFilters,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, ext.border)
                        ) {
                            Text("Reset All", color = ext.title)
                        }
                        Button(
                            onClick = { showFilterModal = false },
                            modifier = Modifier
                                .weight(1.6f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ext.accent,
                                contentColor = Color.White
                            )
                        ) {
                            Text("Apply Filters", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LessonModuleCard(
    lecture: LectureEntity,
    onOpenNotes: () -> Unit,
    onQuickQuiz: () -> Unit,
    onShare: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit
) {
    val ext = LocalQuizzMeColors.current
    val accentBarColor = subjectAccentColor(lecture.subject)
    var menuExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onOpenNotes)
            .testTag("lesson_card_${lecture.id}"),
        shape = RoundedCornerShape(16.dp),
        color = ext.card,
        border = BorderStroke(1.dp, ext.border)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(accentBarColor)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 14.dp, top = 16.dp, bottom = 16.dp, end = 8.dp)
            ) {
                Text(
                    text = lecture.lesson.uppercase(),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = ext.title,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = lecture.subject,
                        style = MaterialTheme.typography.bodySmall,
                        color = ext.subtext
                    )
                    Text(text = "|", style = MaterialTheme.typography.bodySmall, color = ext.subtext)
                    Text(
                        text = "Term ${lecture.term}",
                        style = MaterialTheme.typography.bodySmall,
                        color = ext.subtext
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(end = 6.dp)
            ) {
                IconButton(
                    onClick = onQuickQuiz,
                    modifier = Modifier.testTag("lesson_quiz_button_${lecture.id}")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Bolt,
                        contentDescription = "Launch quiz",
                        tint = ext.accent,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.testTag("lesson_menu_button_${lecture.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = "Lesson options",
                            tint = ext.title,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        containerColor = ext.card
                    ) {
                        DropdownMenuItem(
                            text = { Text("Share", color = ext.title) },
                            leadingIcon = {
                                Icon(Icons.Rounded.Share, contentDescription = null, tint = ext.accent)
                            },
                            onClick = {
                                menuExpanded = false
                                onShare()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Copy", color = ext.title) },
                            leadingIcon = {
                                Icon(Icons.Rounded.ContentCopy, contentDescription = null, tint = Color(0xFF007AFF))
                            },
                            onClick = {
                                menuExpanded = false
                                onCopy()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", color = ext.danger) },
                            leadingIcon = {
                                Icon(Icons.Rounded.Delete, contentDescription = null, tint = ext.danger)
                            },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LessonFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val ext = LocalQuizzMeColors.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) ext.accentSoft else Color.Transparent,
        border = BorderStroke(1.dp, if (selected) ext.accent else ext.border),
        modifier = Modifier.minimumInteractiveComponentSize()
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = if (selected) ext.accent else ext.title,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
        )
    }
}

fun copyLessonToClipboard(context: Context, lecture: LectureEntity) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(lecture.lesson, lecture.markdownContent))
}

fun shareLessonMarkdown(context: Context, lecture: LectureEntity) {
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "${lecture.lesson} (${lecture.subject})")
        putExtra(Intent.EXTRA_TEXT, lecture.markdownContent)
    }
    context.startActivity(Intent.createChooser(sendIntent, "Share ${lecture.lesson}"))
}
