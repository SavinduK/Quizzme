package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.LectureEntity
import com.example.model.QuestionStyle
import com.example.model.QuizQuestion
import com.example.ui.components.DeleteConfirmDialog
import com.example.ui.components.PastPaperQuestionEditorDialog
import com.example.ui.components.QuizCard
import com.example.ui.components.QuizResultsCard
import com.example.ui.components.QuizzMeHeader
import com.example.ui.theme.LocalQuizzMeColors
import com.example.ui.theme.subjectAccentColor
import com.example.ui.viewmodel.LessonLinkFilter
import com.example.ui.viewmodel.PastPaperWithMeta
import com.example.ui.viewmodel.PastPapersUiState
import com.example.ui.viewmodel.PastPapersViewModel

@Composable
fun PastPapersScreen(
    viewModel: PastPapersViewModel,
    widthSizeClass: WindowWidthSizeClass,
    onNavigateToAddPaper: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val ext = LocalQuizzMeColors.current
    val snackbarHostState = remember { SnackbarHostState() }
    val isTablet = widthSizeClass != WindowWidthSizeClass.Compact

    var deletePaperTargetId by remember { mutableStateOf<String?>(null) }
    var editingQuestionState by remember { mutableStateOf<Pair<String, Pair<Int?, QuizQuestion?>>?>(null) }

    LaunchedEffect(uiState.snackbarMessage) {
        val msg = uiState.snackbarMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    val practicingPaper = uiState.practicingPaper
    val inspectingPaper = uiState.inspectingPaper

    Box(modifier = modifier.fillMaxSize()) {
        when {
            practicingPaper != null -> {
                BackHandler { viewModel.stopPractice() }
                PastPaperPracticeView(
                    paper = practicingPaper,
                    uiState = uiState,
                    isTablet = isTablet,
                    onBack = viewModel::stopPractice,
                    onRestart = viewModel::restartPractice,
                    viewModel = viewModel
                )
            }

            inspectingPaper != null -> {
                BackHandler { viewModel.closePaperDetail() }
                PastPaperDetailView(
                    paper = inspectingPaper,
                    isTablet = isTablet,
                    onBack = viewModel::closePaperDetail,
                    onPractice = { viewModel.startPractice(inspectingPaper.entity.id) },
                    onLinkLesson = { viewModel.openLinkDialog(inspectingPaper.entity.id) },
                    onAddQuestion = {
                        editingQuestionState = inspectingPaper.entity.id to (null to null)
                    },
                    onEditQuestion = { index, question ->
                        editingQuestionState = inspectingPaper.entity.id to (index to question)
                    },
                    onDeleteQuestion = { index ->
                        viewModel.deleteQuestion(inspectingPaper.entity.id, index)
                    }
                )
            }

            else -> {
                PastPapersRepositoryView(
                    uiState = uiState,
                    isTablet = isTablet,
                    onAddPaperClick = onNavigateToAddPaper,
                    onSearchChange = viewModel::setSearchQuery,
                    onLinkFilterChange = viewModel::setLinkFilter,
                    onYearSelect = viewModel::setSelectedYear,
                    onResetFilters = viewModel::resetFilters,
                    onInspectPaper = viewModel::openPaperDetail,
                    onPracticePaper = viewModel::startPractice,
                    onLinkPaper = viewModel::openLinkDialog,
                    onDeletePaper = { deletePaperTargetId = it }
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
    }

    val linkingPaper = uiState.linkingPaper
    if (linkingPaper != null) {
        LinkLessonDialog(
            paper = linkingPaper,
            lectures = uiState.allLectures,
            onDismiss = viewModel::dismissLinkDialog,
            onSelectLecture = { lecture ->
                viewModel.linkPaperToLesson(linkingPaper.entity.id, lecture)
            }
        )
    }

    val editSpec = editingQuestionState
    if (editSpec != null) {
        val (paperId, pair) = editSpec
        val (editIdx, initialQ) = pair
        val targetPaper = uiState.allPapers.firstOrNull { it.entity.id == paperId }
        PastPaperQuestionEditorDialog(
            initialQuestion = initialQ,
            defaultYear = targetPaper?.displayYear.orEmpty(),
            defaultLesson = targetPaper?.displayTitle.orEmpty(),
            isTablet = isTablet,
            onDismiss = { editingQuestionState = null },
            onSave = { updatedQ ->
                viewModel.addOrUpdateQuestion(paperId, editIdx, updatedQ)
                editingQuestionState = null
            }
        )
    }

    if (deletePaperTargetId != null) {
        DeleteConfirmDialog(
            title = "Delete Past Paper?",
            message = "Are you sure you want to delete this past paper and its questions? This cannot be undone.",
            onCancel = { deletePaperTargetId = null },
            onConfirm = {
                deletePaperTargetId?.let { viewModel.deletePastPaper(it) }
                deletePaperTargetId = null
            }
        )
    }
}

@Composable
private fun PastPapersRepositoryView(
    uiState: PastPapersUiState,
    isTablet: Boolean,
    onAddPaperClick: () -> Unit,
    onSearchChange: (String) -> Unit,
    onLinkFilterChange: (LessonLinkFilter) -> Unit,
    onYearSelect: (String?) -> Unit,
    onResetFilters: () -> Unit,
    onInspectPaper: (String) -> Unit,
    onPracticePaper: (String) -> Unit,
    onLinkPaper: (String) -> Unit,
    onDeletePaper: (String) -> Unit
) {
    val ext = LocalQuizzMeColors.current

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        QuizzMeHeader(
            title = "Past Papers",
            onAddClick = onAddPaperClick,
            addButtonTag = "past_papers_add_button"
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .widthIn(max = if (isTablet) 1280.dp else 680.dp)
                .fillMaxWidth()
        ) {
            // Search Bar
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Search past papers, years, or lessons...", color = ext.subtext) },
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
                        IconButton(onClick = { onSearchChange("") }) {
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
                    .fillMaxWidth()
                    .padding(horizontal = if (isTablet) 24.dp else 20.dp, vertical = 12.dp)
                    .testTag("past_papers_search_input")
            )

            // Filter chips row (Link Status + Year)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = if (isTablet) 24.dp else 20.dp)
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LessonLinkFilter.entries.forEach { filter ->
                    val selected = uiState.linkFilter == filter
                    Surface(
                        onClick = { onLinkFilterChange(filter) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (selected) ext.accent else ext.card,
                        border = BorderStroke(1.dp, if (selected) ext.accent else ext.border),
                        modifier = Modifier.testTag("paper_filter_${filter.name.lowercase()}")
                    ) {
                        Text(
                            text = filter.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selected) Color.White else ext.title,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }

                if (uiState.availableYears.isNotEmpty()) {
                    uiState.availableYears.forEach { yr ->
                        val selected = uiState.selectedYear == yr
                        Surface(
                            onClick = { onYearSelect(if (selected) null else yr) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected) ext.accentSoft else ext.card,
                            border = BorderStroke(1.dp, if (selected) ext.accent else ext.border)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CalendarToday,
                                    contentDescription = null,
                                    tint = if (selected) ext.accent else ext.subtext,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = yr,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selected) ext.accent else ext.title
                                )
                            }
                        }
                    }
                }

                if (uiState.hasActiveFilter) {
                    TextButton(onClick = onResetFilters) {
                        Text("Reset", color = ext.accent)
                    }
                }
            }

            if (uiState.filteredPapers.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 56.dp, start = 32.dp, end = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Description,
                        contentDescription = null,
                        tint = ext.border,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = if (uiState.allPapers.isEmpty()) {
                            "No past papers yet. Upload a PDF or images, or add questions manually."
                        } else {
                            "No past papers match your current filters."
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = ext.subtext,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(18.dp))
                    if (uiState.allPapers.isEmpty()) {
                        Button(
                            onClick = onAddPaperClick,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ext.accent,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("past_papers_empty_upload_button")
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Upload Past Paper", style = MaterialTheme.typography.labelLarge)
                        }
                    } else {
                        OutlinedButton(
                            onClick = onResetFilters,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, ext.border)
                        ) {
                            Text("Reset Filters", color = ext.accent)
                        }
                    }
                }
            } else if (isTablet) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 320.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    items(uiState.filteredPapers, key = { it.entity.id }) { item ->
                        PastPaperDeckCard(
                            item = item,
                            onInspect = { onInspectPaper(item.entity.id) },
                            onPractice = { onPracticePaper(item.entity.id) },
                            onLinkLesson = { onLinkPaper(item.entity.id) },
                            onDelete = { onDeletePaper(item.entity.id) }
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.filteredPapers, key = { it.entity.id }) { item ->
                        PastPaperDeckCard(
                            item = item,
                            onInspect = { onInspectPaper(item.entity.id) },
                            onPractice = { onPracticePaper(item.entity.id) },
                            onLinkLesson = { onLinkPaper(item.entity.id) },
                            onDelete = { onDeletePaper(item.entity.id) }
                        )
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@Composable
private fun PastPaperDeckCard(
    item: PastPaperWithMeta,
    onInspect: () -> Unit,
    onPractice: () -> Unit,
    onLinkLesson: () -> Unit,
    onDelete: () -> Unit
) {
    val ext = LocalQuizzMeColors.current
    val accentBarColor = subjectAccentColor(item.entity.subject)
    var menuExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onInspect)
            .testTag("past_paper_card_${item.entity.id}"),
        shape = RoundedCornerShape(16.dp),
        color = ext.card,
        border = BorderStroke(1.dp, ext.border)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
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
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.displayTitle.uppercase(),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = ext.title,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(4.dp))
                        val metaLine = listOfNotNull(
                            item.entity.subject.takeIf { it.isNotBlank() },
                            item.entity.term.takeIf { it.isNotBlank() },
                            item.displayYear.takeIf { it.isNotBlank() }?.let { "Year $it" }
                        ).joinToString(" • ")
                        if (metaLine.isNotBlank()) {
                            Text(
                                text = metaLine,
                                style = MaterialTheme.typography.bodySmall,
                                color = ext.subtext,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(
                                imageVector = Icons.Rounded.MoreVert,
                                contentDescription = "Past paper options",
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
                                text = { Text("Link to Lesson", color = ext.title) },
                                leadingIcon = {
                                    Icon(Icons.Rounded.Link, contentDescription = null, tint = ext.accent)
                                },
                                onClick = {
                                    menuExpanded = false
                                    onLinkLesson()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete Past Paper", color = ext.danger) },
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

                Spacer(Modifier.height(10.dp))

                // Linked Lesson & Question Count Badges
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val linked = item.linkedLecture
                    Surface(
                        onClick = onLinkLesson,
                        shape = RoundedCornerShape(10.dp),
                        color = if (linked != null) ext.accentSoft else ext.background,
                        border = BorderStroke(1.dp, if (linked != null) ext.accent else ext.border),
                        modifier = Modifier.testTag("link_lesson_chip_${item.entity.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = if (linked != null) Icons.Rounded.Link else Icons.Rounded.LinkOff,
                                contentDescription = null,
                                tint = if (linked != null) ext.accent else ext.subtext,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = linked?.let { "Linked: ${it.lesson}" } ?: "Link Lesson",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (linked != null) ext.accent else ext.subtext,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = ext.background,
                        border = BorderStroke(1.dp, ext.border)
                    ) {
                        Text(
                            text = "${item.questions.size} Qs",
                            style = MaterialTheme.typography.labelSmall,
                            color = ext.title,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                if (item.styleSummary.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = item.styleSummary,
                        style = MaterialTheme.typography.labelSmall,
                        color = ext.subtext
                    )
                }

                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = ext.border)
                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onInspect,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, ext.border),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Visibility,
                            contentDescription = null,
                            tint = ext.title,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Questions", color = ext.title, style = MaterialTheme.typography.labelMedium)
                    }

                    Button(
                        onClick = onPractice,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ext.accent,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Bolt,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Practice", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun PastPaperDetailView(
    paper: PastPaperWithMeta,
    isTablet: Boolean,
    onBack: () -> Unit,
    onPractice: () -> Unit,
    onLinkLesson: () -> Unit,
    onAddQuestion: () -> Unit,
    onEditQuestion: (Int, QuizQuestion) -> Unit,
    onDeleteQuestion: (Int) -> Unit
) {
    val ext = LocalQuizzMeColors.current

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = ext.card,
                border = BorderStroke(1.dp, ext.border)
            ) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("paper_detail_back_button")) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back to past papers",
                        tint = ext.title
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = paper.displayTitle,
                    style = MaterialTheme.typography.titleLarge,
                    color = ext.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val sub = listOfNotNull(
                    paper.displayYear.takeIf { it.isNotBlank() }?.let { "Year $it" },
                    paper.linkedLecture?.let { "Linked: ${it.lesson}" } ?: "Not linked to lesson"
                ).joinToString(" • ")
                Text(
                    text = sub,
                    style = MaterialTheme.typography.bodySmall,
                    color = ext.subtext,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = onLinkLesson,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Icon(
                    imageVector = if (paper.linkedLecture != null) Icons.Rounded.Link else Icons.Rounded.LinkOff,
                    contentDescription = "Link to lesson",
                    tint = ext.accent
                )
            }

            Button(
                onClick = onPractice,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ext.accent,
                    contentColor = Color.White
                ),
                modifier = Modifier.height(42.dp)
            ) {
                Icon(Icons.Rounded.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Practice", style = MaterialTheme.typography.labelMedium)
            }
        }
        HorizontalDivider(color = ext.border)

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .widthIn(max = if (isTablet) 920.dp else 680.dp)
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${paper.questions.size} QUESTION(S)",
                        style = MaterialTheme.typography.labelMedium,
                        color = ext.accent
                    )
                    OutlinedButton(
                        onClick = onAddQuestion,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, ext.accent),
                        modifier = Modifier.testTag("paper_detail_add_question_button")
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, tint = ext.accent, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add Question", color = ext.accent, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            itemsIndexed(paper.questions) { index, question ->
                PastPaperQuestionInspectorCard(
                    index = index,
                    question = question,
                    fallbackYear = paper.displayYear,
                    fallbackLesson = paper.displayTitle,
                    onEdit = { onEditQuestion(index, question) },
                    onDelete = { onDeleteQuestion(index) }
                )
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun PastPaperQuestionInspectorCard(
    index: Int,
    question: QuizQuestion,
    fallbackYear: String,
    fallbackLesson: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val ext = LocalQuizzMeColors.current
    val style = question.detectedStyle()
    val effectiveYear = question.year?.takeIf { it.isNotBlank() } ?: fallbackYear
    val effectiveLesson = question.lesson?.takeIf { it.isNotBlank() } ?: fallbackLesson
    val correctResponses = question.resolvedCorrectResponses()

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = ext.card,
        border = BorderStroke(1.dp, ext.border)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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
                            text = "Q${index + 1} • ${style.shortLabel}",
                            style = MaterialTheme.typography.labelSmall,
                            color = ext.accent,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    if (effectiveYear.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = ext.background,
                            border = BorderStroke(1.dp, ext.border)
                        ) {
                            Text(
                                text = effectiveYear,
                                style = MaterialTheme.typography.labelSmall,
                                color = ext.title,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    if (effectiveLesson.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = ext.background,
                            border = BorderStroke(1.dp, ext.border)
                        ) {
                            Text(
                                text = effectiveLesson,
                                style = MaterialTheme.typography.labelSmall,
                                color = ext.subtext,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = "Edit question",
                            tint = ext.accent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = "Delete question",
                            tint = ext.danger,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            Text(
                text = question.question,
                style = MaterialTheme.typography.titleMedium,
                color = ext.title
            )
            Spacer(Modifier.height(12.dp))

            when (style) {
                QuestionStyle.MCQ -> {
                    question.options.orEmpty().forEachIndexed { optIdx, opt ->
                        val isCorrect = correctResponses.any { it.equals(opt, ignoreCase = true) }
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = if (isCorrect) ext.successSoft else ext.background,
                            border = BorderStroke(1.dp, if (isCorrect) ext.success else ext.border)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${optIdx + 1}. $opt",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isCorrect) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = ext.title,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isCorrect) {
                                    Icon(
                                        imageVector = Icons.Rounded.CheckCircle,
                                        contentDescription = "Correct response",
                                        tint = ext.success,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                QuestionStyle.TF -> {
                    question.statements.orEmpty().forEachIndexed { sIdx, stmt ->
                        val ans = question.answers?.getOrNull(sIdx)
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = ext.background,
                            border = BorderStroke(1.dp, ext.border)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${sIdx + 1}. $stmt",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = ext.title,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (ans == true) ext.successSoft else ext.dangerSoft
                                ) {
                                    Text(
                                        text = if (ans == true) "TRUE" else "FALSE",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (ans == true) ext.success else ext.danger,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                QuestionStyle.SA -> {
                    if (correctResponses.isNotEmpty()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = ext.successSoft,
                            border = BorderStroke(1.dp, ext.success)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("CORRECT RESPONSE", style = MaterialTheme.typography.labelSmall, color = ext.success)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = correctResponses.joinToString(" / "),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = ext.title
                                )
                            }
                        }
                    }
                }

                QuestionStyle.SEQ -> {
                    question.subQuestions.orEmpty().forEach { sq ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = ext.background,
                            border = BorderStroke(1.dp, ext.border)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(sq.subQuestion, style = MaterialTheme.typography.titleSmall, color = ext.title)
                                if (sq.answerKey.isNotBlank()) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(sq.answerKey, style = MaterialTheme.typography.bodySmall, color = ext.text)
                                }
                            }
                        }
                    }
                    if (!question.modelAnswer.isNullOrBlank()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = ext.successSoft,
                            border = BorderStroke(1.dp, ext.success)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("MODEL ANSWER", style = MaterialTheme.typography.labelSmall, color = ext.success)
                                Spacer(Modifier.height(4.dp))
                                Text(question.modelAnswer, style = MaterialTheme.typography.bodyMedium, color = ext.title)
                            }
                        }
                    }
                }
            }

            if (!question.explanation.isNullOrBlank()) {
                Spacer(Modifier.height(10.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = ext.background,
                    border = BorderStroke(1.dp, ext.accent)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Lightbulb,
                                contentDescription = null,
                                tint = ext.accent,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "ANSWER EXPLAINED",
                                style = MaterialTheme.typography.labelSmall,
                                color = ext.accent
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = question.explanation,
                            style = MaterialTheme.typography.bodyMedium,
                            color = ext.text
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PastPaperPracticeView(
    paper: PastPaperWithMeta,
    uiState: PastPapersUiState,
    isTablet: Boolean,
    onBack: () -> Unit,
    onRestart: () -> Unit,
    viewModel: PastPapersViewModel
) {
    val ext = LocalQuizzMeColors.current
    val session = uiState.practiceSession

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = ext.card,
                border = BorderStroke(1.dp, ext.border)
            ) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("paper_practice_back_button")) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Exit practice",
                        tint = ext.title
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = paper.displayTitle,
                    style = MaterialTheme.typography.titleLarge,
                    color = ext.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = listOfNotNull(
                        paper.displayYear.takeIf { it.isNotBlank() }?.let { "Year $it" },
                        "${paper.questions.size} Past Paper Questions"
                    ).joinToString(" • "),
                    style = MaterialTheme.typography.bodySmall,
                    color = ext.subtext
                )
            }

            IconButton(onClick = onRestart) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = "Restart quiz",
                    tint = ext.accent
                )
            }
        }
        HorizontalDivider(color = ext.border)

        Column(
            modifier = Modifier
                .weight(1f)
                .widthIn(max = if (isTablet) 840.dp else 680.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            if (session.quizFinished) {
                QuizResultsCard(
                    runningScore = session.runningScore,
                    maxPossibleScore = session.maxPossibleScore(uiState.practiceStyle),
                    actionLabel = "Restart Past Paper",
                    onActionClick = onRestart
                )
            } else {
                session.currentQuestion?.let { q ->
                    QuizCard(
                        question = q,
                        sessionState = session,
                        questionStyle = q.detectedStyle(),
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

@Composable
private fun LinkLessonDialog(
    paper: PastPaperWithMeta,
    lectures: List<LectureEntity>,
    onDismiss: () -> Unit,
    onSelectLecture: (LectureEntity?) -> Unit
) {
    val ext = LocalQuizzMeColors.current
    var search by remember { mutableStateOf("") }
    val filtered = remember(lectures, search) {
        if (search.isBlank()) lectures
        else lectures.filter {
            it.lesson.contains(search, ignoreCase = true) ||
                it.subject.contains(search, ignoreCase = true) ||
                it.term.contains(search, ignoreCase = true)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 560.dp),
            shape = RoundedCornerShape(22.dp),
            color = ext.card,
            border = BorderStroke(1.dp, ext.border)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Link Past Paper to Lesson",
                    style = MaterialTheme.typography.titleLarge,
                    color = ext.title
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Linking associates these questions with a lesson for filtered practice and AI quiz alignment.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ext.subtext
                )
                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = { Text("Search lessons...", color = ext.subtext) },
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Rounded.Search, contentDescription = null, tint = ext.subtext, modifier = Modifier.size(18.dp))
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ext.accent,
                        unfocusedBorderColor = ext.border,
                        focusedContainerColor = ext.background,
                        unfocusedContainerColor = ext.background,
                        focusedTextColor = ext.title,
                        unfocusedTextColor = ext.title
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))

                if (filtered.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (lectures.isEmpty()) "No lessons available yet. Add a lesson first." else "No matching lessons.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ext.subtext,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .heightIn(max = 280.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filtered, key = { it.id }) { lecture ->
                            val isSelected = paper.linkedLecture?.id == lecture.id
                            Surface(
                                onClick = { onSelectLecture(lecture) },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) ext.accentSoft else ext.background,
                                border = BorderStroke(1.dp, if (isSelected) ext.accent else ext.border),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("link_lesson_option_${lecture.id}")
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
                                            contentDescription = "Currently linked",
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
                    if (paper.linkedLecture != null) {
                        TextButton(onClick = { onSelectLecture(null) }) {
                            Text("Unlink Lesson", color = ext.danger)
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
