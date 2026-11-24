package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.MarkdownContent
import com.example.ui.components.QuizCard
import com.example.ui.components.QuizResultsCard
import com.example.ui.components.SummaryCardBlock
import com.example.ui.components.markdownToEditablePlainText
import com.example.ui.components.parseMarkdownToSummaryCards
import com.example.ui.theme.LocalQuizzMeColors
import com.example.ui.viewmodel.LessonReaderViewModel
import com.example.ui.viewmodel.ReaderTab
import kotlinx.coroutines.launch

@Composable
fun LessonReaderScreen(
    viewModel: LessonReaderViewModel,
    widthSizeClass: WindowWidthSizeClass,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val ext = LocalQuizzMeColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val isTablet = widthSizeClass != WindowWidthSizeClass.Compact

    var showEditModal by remember { mutableStateOf(false) }
    var editableText by remember { mutableStateOf("") }

    BackHandler { onBack() }

    LaunchedEffect(uiState.snackbarMessage) {
        val msg = uiState.snackbarMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        containerColor = ext.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (uiState.activeTab == ReaderTab.NOTES && uiState.lecture != null) {
                FloatingActionButton(
                    onClick = {
                        editableText = markdownToEditablePlainText(uiState.lecture?.markdownContent.orEmpty())
                        showEditModal = true
                    },
                    containerColor = ext.accent,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.testTag("reader_edit_fab")
                ) {
                    Icon(Icons.Rounded.Edit, contentDescription = "Edit lesson notes")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("reader_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = ext.text
                    )
                }

                Text(
                    text = (uiState.lecture?.lesson ?: "View Lesson").uppercase(),
                    style = MaterialTheme.typography.titleLarge,
                    color = ext.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                )

                val lecture = uiState.lecture
                if (lecture != null) {
                    IconButton(
                        onClick = {
                            copyLessonToClipboard(context, lecture)
                            scope.launch {
                                snackbarHostState.showSnackbar("Notes copied to clipboard")
                            }
                        },
                        modifier = Modifier.testTag("reader_copy_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ContentCopy,
                            contentDescription = "Copy notes",
                            tint = ext.accent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = { shareLessonContent(context, lecture) },
                        modifier = Modifier.testTag("reader_share_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Share,
                            contentDescription = "Share notes",
                            tint = ext.accent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            HorizontalDivider(color = ext.border)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .widthIn(max = if (isTablet) 1140.dp else 680.dp)
                    .fillMaxWidth()
            ) {
                // Segmented 3-Tab Bar
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = if (isTablet) 24.dp else 16.dp, vertical = 12.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = ext.accentSoft
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        ReaderTabButton(
                            label = "Notes",
                            icon = Icons.AutoMirrored.Rounded.MenuBook,
                            selected = uiState.activeTab == ReaderTab.NOTES,
                            onClick = { viewModel.selectTab(ReaderTab.NOTES) },
                            tag = "tab_notes",
                            modifier = Modifier.weight(1f)
                        )
                        ReaderTabButton(
                            label = "Quiz",
                            icon = Icons.Rounded.Bolt,
                            selected = uiState.activeTab == ReaderTab.QUIZ,
                            onClick = { viewModel.selectTab(ReaderTab.QUIZ) },
                            tag = "tab_quiz",
                            modifier = Modifier.weight(1f)
                        )
                        ReaderTabButton(
                            label = "Summary",
                            icon = Icons.Rounded.Description,
                            selected = uiState.activeTab == ReaderTab.SUMMARY,
                            onClick = { viewModel.selectTab(ReaderTab.SUMMARY) },
                            tag = "tab_summary",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Body
                if (uiState.loading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = ext.accent)
                            Spacer(Modifier.height(12.dp))
                            Text("Loading Content...", color = ext.subtext)
                        }
                    }
                } else if (uiState.lecture == null) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Lesson not found.", color = ext.subtext)
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = if (isTablet) 28.dp else 20.dp)
                            .padding(bottom = 96.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        when (uiState.activeTab) {
                            ReaderTab.NOTES -> {
                                Surface(
                                    modifier = Modifier
                                        .widthIn(max = 880.dp)
                                        .fillMaxWidth()
                                        .padding(top = 4.dp),
                                    shape = RoundedCornerShape(18.dp),
                                    color = if (isTablet) ext.card else Color.Transparent,
                                    border = if (isTablet) BorderStroke(1.dp, ext.border) else null
                                ) {
                                    MarkdownContent(
                                        markdown = uiState.lecture!!.markdownContent,
                                        modifier = Modifier.padding(if (isTablet) 24.dp else 0.dp)
                                    )
                                }
                            }

                            ReaderTab.QUIZ -> {
                                Column(
                                    modifier = Modifier
                                        .widthIn(max = 840.dp)
                                        .fillMaxWidth()
                                ) {
                                    when {
                                        uiState.loadingQuiz -> {
                                            CenteredLoadingState("Compiling Questions & Answer Keys...")
                                        }
                                        uiState.quizError != null && uiState.quizSession.deck.isEmpty() -> {
                                            ErrorRetryCard(
                                                message = uiState.quizError!!,
                                                onRetry = { viewModel.launchQuiz(forceRegenerate = true) }
                                            )
                                        }
                                        uiState.quizSession.deck.isNotEmpty() -> {
                                            AnimatedVisibility(visible = uiState.quizNotice != null) {
                                                Surface(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(bottom = 10.dp),
                                                    shape = RoundedCornerShape(12.dp),
                                                    color = ext.accentSoft,
                                                    border = BorderStroke(1.dp, ext.accent)
                                                ) {
                                                    Text(
                                                        text = uiState.quizNotice.orEmpty(),
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = ext.accent,
                                                        modifier = Modifier.padding(12.dp)
                                                    )
                                                }
                                            }

                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(bottom = 6.dp),
                                                horizontalArrangement = Arrangement.End
                                            ) {
                                                TextButton(
                                                    onClick = { viewModel.launchQuiz(forceRegenerate = true) },
                                                    modifier = Modifier.testTag("regenerate_quiz_button")
                                                ) {
                                                    Icon(
                                                        Icons.Rounded.Refresh,
                                                        contentDescription = null,
                                                        tint = ext.accent,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(Modifier.width(6.dp))
                                                    Text("Regenerate Quiz", color = ext.accent)
                                                }
                                            }

                                            if (uiState.quizSession.quizFinished) {
                                                QuizResultsCard(
                                                    runningScore = uiState.quizSession.runningScore,
                                                    maxPossibleScore = uiState.quizSession.maxPossibleScore(uiState.quizStyle),
                                                    actionLabel = "Generate New Quiz",
                                                    onActionClick = { viewModel.launchQuiz(forceRegenerate = true) }
                                                )
                                            } else {
                                                uiState.quizSession.currentQuestion?.let { q ->
                                                    QuizCard(
                                                        question = q,
                                                        sessionState = uiState.quizSession,
                                                        questionStyle = uiState.quizStyle,
                                                        onMcqOptionClick = viewModel::onMcqOptionSelected,
                                                        onTfToggle = viewModel::onTfToggle,
                                                        onEvaluateTf = viewModel::onEvaluateTf,
                                                        onSaInputChange = viewModel::onSaInputChange,
                                                        onRevealSaAnswer = viewModel::onRevealSaAnswer,
                                                        onGradeSa = viewModel::onGradeSa,
                                                        onSeqNotesChange = viewModel::onSeqNotesChange,
                                                        onToggleSeqAnswer = viewModel::onToggleSeqAnswer,
                                                        onGradeSeq = viewModel::onGradeSeq,
                                                        onNextQuestion = viewModel::onNextQuestion
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            ReaderTab.SUMMARY -> {
                                when {
                                    uiState.loadingSummary -> {
                                        CenteredLoadingState(uiState.summaryStatus)
                                    }
                                    uiState.summaryError != null && uiState.summaryText.isBlank() -> {
                                        ErrorRetryCard(
                                            message = uiState.summaryError!!,
                                            onRetry = { viewModel.loadOrGenerateSummary(forceRegenerate = true) }
                                        )
                                    }
                                    else -> {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            TextButton(
                                                onClick = { viewModel.loadOrGenerateSummary(forceRegenerate = true) },
                                                modifier = Modifier.testTag("regenerate_summary_button")
                                            ) {
                                                Icon(
                                                    Icons.Rounded.AutoAwesome,
                                                    contentDescription = null,
                                                    tint = ext.accent,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(Modifier.width(6.dp))
                                                Text("Regenerate Summary", color = ext.accent)
                                            }
                                        }

                                        val blocks = remember(uiState.summaryText) {
                                            parseMarkdownToSummaryCards(uiState.summaryText)
                                        }
                                        if (isTablet && blocks.size > 1) {
                                            val chunked = blocks.chunked(2)
                                            Column(
                                                verticalArrangement = Arrangement.spacedBy(14.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                chunked.forEach { pair ->
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .height(IntrinsicSize.Min),
                                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                                    ) {
                                                        pair.forEach { block ->
                                                            SummaryBlockCard(
                                                                block = block,
                                                                modifier = Modifier
                                                                    .weight(1f)
                                                                    .fillMaxHeight()
                                                            )
                                                        }
                                                        if (pair.size == 1) {
                                                            Spacer(Modifier.weight(1f))
                                                        }
                                                    }
                                                }
                                            }
                                        } else {
                                            Column(
                                                verticalArrangement = Arrangement.spacedBy(14.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                blocks.forEach { block ->
                                                    SummaryBlockCard(block = block, modifier = Modifier.fillMaxWidth())
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Markdown Notes Editor Dialog (tag-free, clean text)
    if (showEditModal) {
        Dialog(
            onDismissRequest = { showEditModal = false },
            properties = DialogProperties(usePlatformDefaultWidth = !isTablet)
        ) {
            Surface(
                modifier = if (isTablet) {
                    Modifier
                        .widthIn(min = 640.dp, max = 900.dp)
                        .fillMaxWidth()
                        .heightIn(min = 520.dp, max = 760.dp)
                } else {
                    Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                },
                shape = if (isTablet) RoundedCornerShape(24.dp) else RoundedCornerShape(0.dp),
                color = ext.background,
                border = if (isTablet) BorderStroke(1.dp, ext.border) else null
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showEditModal = false }) {
                            Text("Cancel", color = ext.subtext, style = MaterialTheme.typography.labelLarge)
                        }
                        Text(
                            text = "Edit Lesson",
                            style = MaterialTheme.typography.titleLarge,
                            color = ext.title
                        )
                        TextButton(
                            onClick = {
                                viewModel.saveEditedNotes(editableText) {
                                    showEditModal = false
                                }
                            },
                            enabled = !uiState.isSavingEdit,
                            modifier = Modifier.testTag("editor_save_button")
                        ) {
                            if (uiState.isSavingEdit) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Text("Save", color = ext.accent, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                    HorizontalDivider(color = ext.border)

                    OutlinedTextField(
                        value = editableText,
                        onValueChange = { editableText = it },
                        placeholder = { Text("Edit lesson Markdown notes...", color = ext.subtext) },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = ext.title
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(16.dp)
                            .testTag("editor_html_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryBlockCard(
    block: SummaryCardBlock,
    modifier: Modifier = Modifier
) {
    val ext = LocalQuizzMeColors.current
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = ext.card,
        border = BorderStroke(1.dp, ext.border),
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = block.title,
                style = MaterialTheme.typography.titleMedium,
                color = ext.accent
            )
            HorizontalDivider(
                color = ext.border,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            MarkdownContent(markdown = block.content)
        }
    }
}

@Composable
private fun ReaderTabButton(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    tag: String,
    modifier: Modifier = Modifier
) {
    val ext = LocalQuizzMeColors.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (selected) ext.background else Color.Transparent,
        tonalElevation = if (selected) 2.dp else 0.dp,
        modifier = modifier
            .minimumInteractiveComponentSize()
            .height(40.dp)
            .testTag(tag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) ext.accent else ext.subtext,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) ext.accent else ext.subtext,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CenteredLoadingState(message: String) {
    val ext = LocalQuizzMeColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(color = ext.accent)
        Spacer(Modifier.height(14.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = ext.subtext,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ErrorRetryCard(
    message: String,
    onRetry: () -> Unit
) {
    val ext = LocalQuizzMeColors.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp),
        shape = RoundedCornerShape(16.dp),
        color = ext.card,
        border = BorderStroke(1.dp, ext.danger)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = ext.danger,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(14.dp))
            OutlinedButton(
                onClick = onRetry,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, ext.accent)
            ) {
                Text("Retry", color = ext.accent)
            }
        }
    }
}
