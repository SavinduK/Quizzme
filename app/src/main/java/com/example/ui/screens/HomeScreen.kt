package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.QuestionStyle
import com.example.ui.components.QuizCard
import com.example.ui.components.QuizResultsCard
import com.example.ui.components.QuizzMeHeader
import com.example.ui.theme.LocalQuizzMeColors
import com.example.ui.viewmodel.HomeUiState
import com.example.ui.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    widthSizeClass: WindowWidthSizeClass,
    onOpenDrawer: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val ext = LocalQuizzMeColors.current
    var showFilterSheet by remember { mutableStateOf(false) }
    val isTablet = widthSizeClass != WindowWidthSizeClass.Compact

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        QuizzMeHeader(
            title = "Questions",
            onMenuClick = onOpenDrawer,
            menuButtonTag = "home_drawer_button",
            hasActiveDrawerFilter = uiState.selectedTerm != null || uiState.selectedLesson != null
        )

        if (isTablet) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .widthIn(max = 1280.dp)
                    .fillMaxWidth()
            ) {
                // Main Quiz Canvas
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    HomeStatusRow(
                        uiState = uiState,
                        showFilterButton = false,
                        onOpenFilters = {}
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 24.dp)
                            .padding(bottom = 28.dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Box(modifier = Modifier.widthIn(max = 760.dp).fillMaxWidth()) {
                            HomeQuizFeedContent(
                                uiState = uiState,
                                viewModel = viewModel
                            )
                        }
                    }
                }

                VerticalDivider(color = ext.border)

                // Supporting Session Controls Pane on Tablet
                Surface(
                    color = ext.card,
                    modifier = Modifier
                        .width(330.dp)
                        .fillMaxHeight()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp)
                    ) {
                        Text(
                            text = "Quiz Settings",
                            style = MaterialTheme.typography.titleLarge,
                            color = ext.title
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Filter your study pool by format, subject, or term.",
                            style = MaterialTheme.typography.bodySmall,
                            color = ext.subtext
                        )
                        HorizontalDivider(
                            color = ext.border,
                            modifier = Modifier.padding(vertical = 14.dp)
                        )

                        HomeFiltersContent(
                            uiState = uiState,
                            onTypeChange = viewModel::setSelectedType,
                            onSubjectChange = viewModel::setSelectedSubject,
                            onTermChange = viewModel::setSelectedTerm,
                            onReset = viewModel::resetFilters,
                            onRecompile = viewModel::recompileSessionPool
                        )
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .widthIn(max = 680.dp)
                    .fillMaxWidth()
            ) {
                HomeStatusRow(
                    uiState = uiState,
                    showFilterButton = true,
                    onOpenFilters = { showFilterSheet = true }
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 24.dp)
                ) {
                    HomeQuizFeedContent(
                        uiState = uiState,
                        viewModel = viewModel
                    )
                }
            }
        }
    }

    if (showFilterSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
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
                        text = "Quiz Settings",
                        style = MaterialTheme.typography.titleLarge,
                        color = ext.title
                    )
                    IconButton(onClick = { showFilterSheet = false }) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = ext.title
                        )
                    }
                }
                HorizontalDivider(color = ext.border)

                Box(modifier = Modifier.padding(horizontal = 24.dp, vertical = 18.dp)) {
                    HomeFiltersContent(
                        uiState = uiState,
                        onTypeChange = viewModel::setSelectedType,
                        onSubjectChange = viewModel::setSelectedSubject,
                        onTermChange = viewModel::setSelectedTerm,
                        onReset = viewModel::resetFilters,
                        onRecompile = {
                            viewModel.recompileSessionPool()
                            showFilterSheet = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeStatusRow(
    uiState: HomeUiState,
    showFilterButton: Boolean,
    onOpenFilters: () -> Unit
) {
    val ext = LocalQuizzMeColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Bolt,
                    contentDescription = null,
                    tint = ext.accent,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "AI GENERATED • ${uiState.selectedType.shortLabel.uppercase()}",
                    style = MaterialTheme.typography.labelMedium,
                    color = ext.subtext
                )
            }
            val folderPath = listOfNotNull(
                uiState.selectedTerm?.let {
                    if (it.startsWith("Term", ignoreCase = true) || it == "Uncategorized") it else "Term $it"
                },
                uiState.selectedLesson
            ).joinToString(" / ")
            if (folderPath.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = folderPath,
                    style = MaterialTheme.typography.labelSmall,
                    color = ext.accent
                )
            }
        }

        if (showFilterButton) {
            Surface(
                onClick = onOpenFilters,
                shape = RoundedCornerShape(12.dp),
                color = ext.background,
                border = BorderStroke(
                    1.dp,
                    if (uiState.hasActiveFilters) ext.accent else ext.border
                ),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .size(44.dp)
                    .testTag("home_filter_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Tune,
                        contentDescription = "Quiz settings and filters",
                        tint = if (uiState.hasActiveFilters) ext.accent else ext.title,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeQuizFeedContent(
    uiState: HomeUiState,
    viewModel: HomeViewModel
) {
    val ext = LocalQuizzMeColors.current
    val quiz = uiState.quizState
    when {
        quiz.deck.isEmpty() -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 72.dp, start = 20.dp, end = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Rounded.School,
                    contentDescription = null,
                    tint = ext.border,
                    modifier = Modifier.size(56.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "No AI generated questions found for this criteria. Open a lesson and generate a quiz to populate your pool.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = ext.subtext,
                    textAlign = TextAlign.Center
                )
                if (uiState.hasActiveFilters) {
                    Spacer(Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = viewModel::resetFilters,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, ext.border),
                        modifier = Modifier.testTag("home_reset_filters_button")
                    ) {
                        Text("Reset Filters", color = ext.accent)
                    }
                }
            }
        }

        quiz.quizFinished -> {
            QuizResultsCard(
                runningScore = quiz.runningScore,
                maxPossibleScore = quiz.maxPossibleScore(uiState.selectedType),
                actionLabel = "Start New Session",
                onActionClick = viewModel::recompileSessionPool
            )
        }

        else -> {
            val currentQ = quiz.currentQuestion
            if (currentQ != null) {
                QuizCard(
                    question = currentQ,
                    sessionState = quiz,
                    questionStyle = uiState.selectedType,
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HomeFiltersContent(
    uiState: HomeUiState,
    onTypeChange: (QuestionStyle) -> Unit,
    onSubjectChange: (String?) -> Unit,
    onTermChange: (String?) -> Unit,
    onReset: () -> Unit,
    onRecompile: () -> Unit
) {
    val ext = LocalQuizzMeColors.current
    Column {

        Text(
            text = "QUESTION FORMAT",
            style = MaterialTheme.typography.labelSmall,
            color = ext.subtext
        )
        Spacer(Modifier.height(10.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuestionStyle.entries.forEach { style ->
                FilterPillChip(
                    label = style.label,
                    selected = uiState.selectedType == style,
                    onClick = { onTypeChange(style) },
                    tag = "format_${style.name.lowercase()}_chip"
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        Text(
            text = "SUBJECT",
            style = MaterialTheme.typography.labelSmall,
            color = ext.subtext
        )
        Spacer(Modifier.height(10.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterPillChip(
                label = "All Subjects",
                selected = uiState.selectedSubject == null,
                onClick = { onSubjectChange(null) }
            )
            uiState.availableSubjects.forEach { sub ->
                FilterPillChip(
                    label = sub,
                    selected = uiState.selectedSubject == sub,
                    onClick = { onSubjectChange(sub) }
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        Text(
            text = "TERM / BATCH",
            style = MaterialTheme.typography.labelSmall,
            color = ext.subtext
        )
        Spacer(Modifier.height(10.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterPillChip(
                label = "All Terms",
                selected = uiState.selectedTerm == null,
                onClick = { onTermChange(null) }
            )
            uiState.availableTerms.forEach { term ->
                FilterPillChip(
                    label = if (term.startsWith("Term", ignoreCase = true)) term else "Term $term",
                    selected = uiState.selectedTerm == term,
                    onClick = { onTermChange(term) }
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onReset,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("drawer_reset_button"),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, ext.border)
            ) {
                Text("Reset", color = ext.title)
            }
            Button(
                onClick = onRecompile,
                modifier = Modifier
                    .weight(1.5f)
                    .height(48.dp)
                    .testTag("drawer_recompile_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ext.accent,
                    contentColor = Color.White
                )
            ) {
                Text("Recompile", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun FilterPillChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    tag: String? = null
) {
    val ext = LocalQuizzMeColors.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = if (selected) ext.accent else Color.Transparent,
        border = BorderStroke(1.dp, if (selected) ext.accent else ext.border),
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .then(if (tag != null) Modifier.testTag(tag) else Modifier)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = if (selected) Color.White else ext.title,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
        )
    }
}
