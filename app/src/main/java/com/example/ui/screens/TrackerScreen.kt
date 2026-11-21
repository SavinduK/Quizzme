package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.TrackerTaskEntity
import com.example.ui.components.DeleteConfirmDialog
import com.example.ui.components.QuizzMeHeader
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalQuizzMeColors
import com.example.ui.viewmodel.TrackerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackerScreen(
    viewModel: TrackerViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val ext = LocalQuizzMeColors.current
    val snackbarHostState = remember { SnackbarHostState() }
    val calendarListState = rememberLazyListState(initialFirstVisibleItemIndex = 28)

    var showAddModal by remember { mutableStateOf(false) }
    var newTitle by remember { mutableStateOf("") }
    var newSubject by remember { mutableStateOf("") }
    var newTime by remember { mutableStateOf("04:00 PM") }

    var showImportModal by remember { mutableStateOf(false) }
    var jsonInput by remember { mutableStateOf("") }

    var deleteTaskId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uiState.feedbackMessage) {
        val msg = uiState.feedbackMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearFeedback()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            QuizzMeHeader(title = "Work Tracker")

            Column(
                modifier = Modifier
                    .weight(1f)
                    .widthIn(max = 680.dp)
                    .fillMaxWidth()
            ) {
                // Horizontal 61-Day Calendar Strip
                LazyRow(
                    state = calendarListState,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(uiState.calendarDays, key = { it.dateStr }) { day ->
                        val selected = day.dateStr == uiState.selectedDate
                        Surface(
                            onClick = { viewModel.selectDate(day.dateStr) },
                            shape = RoundedCornerShape(16.dp),
                            color = if (selected) ext.accent else ext.card,
                            border = BorderStroke(1.dp, if (selected) ext.accent else ext.border),
                            modifier = Modifier
                                .size(width = 60.dp, height = 74.dp)
                                .testTag("calendar_day_${day.dateStr}")
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = day.dayName.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (selected) Color.White else ext.subtext
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = day.dayNum.toString(),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = if (selected) Color.White else ext.title
                                )
                                if (day.hasTasks) {
                                    Spacer(Modifier.height(3.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(5.dp)
                                            .clip(CircleShape)
                                            .background(if (selected) Color.White else ext.accent)
                                    )
                                }
                            }
                        }
                    }
                }
                HorizontalDivider(color = ext.border)

                // Daily Progress Summary + Action Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (uiState.selectedDate == uiState.todayDate) {
                                "TODAY'S SCHEDULE"
                            } else {
                                "SCHEDULE FOR ${uiState.selectedDate}"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = ext.subtext
                        )
                        if (uiState.dayTasks.isNotEmpty()) {
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "${uiState.completedCount} of ${uiState.dayTasks.size} tasks completed",
                                style = MaterialTheme.typography.titleLarge,
                                color = ext.title
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            onClick = { showImportModal = true },
                            shape = RoundedCornerShape(10.dp),
                            color = ext.accentSoft,
                            border = BorderStroke(1.dp, ext.accent),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .size(40.dp)
                                .testTag("tracker_import_json_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.UploadFile,
                                contentDescription = "Import JSON tasks",
                                tint = ext.accent,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                        Surface(
                            onClick = { showAddModal = true },
                            shape = RoundedCornerShape(10.dp),
                            color = ext.accentSoft,
                            border = BorderStroke(1.dp, ext.accent),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .size(40.dp)
                                .testTag("tracker_add_task_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = "Add task",
                                tint = ext.accent,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }

                // Task List
                if (uiState.dayTasks.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 56.dp, start = 24.dp, end = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.EventAvailable,
                            contentDescription = null,
                            tint = ext.border,
                            modifier = Modifier.size(52.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "No planned tasks or timetable entries for this date.",
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
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(uiState.dayTasks, key = { it.id }) { task ->
                            TaskRowCard(
                                task = task,
                                onToggle = { viewModel.toggleTask(task) },
                                onDelete = { deleteTaskId = task.id }
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

    // Add Task Bottom Sheet
    if (showAddModal) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showAddModal = false },
            sheetState = sheetState,
            containerColor = ext.card,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 28.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Add Planned Task", style = MaterialTheme.typography.titleLarge, color = ext.title)
                    IconButton(onClick = { showAddModal = false }) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close", tint = ext.title)
                    }
                }
                HorizontalDivider(color = ext.border, modifier = Modifier.padding(vertical = 8.dp))

                Text("TITLE", style = MaterialTheme.typography.labelSmall, color = ext.subtext)
                Spacer(Modifier.height(6.dp))
                StyledFormField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    placeholder = "e.g. Read Anatomy Chapter 4",
                    testTag = "task_title_input"
                )

                Spacer(Modifier.height(14.dp))
                Text("SUBJECT / TAG", style = MaterialTheme.typography.labelSmall, color = ext.subtext)
                Spacer(Modifier.height(6.dp))
                StyledFormField(
                    value = newSubject,
                    onValueChange = { newSubject = it },
                    placeholder = "e.g. Pathology",
                    testTag = "task_subject_input"
                )

                Spacer(Modifier.height(14.dp))
                Text("TIME WINDOW", style = MaterialTheme.typography.labelSmall, color = ext.subtext)
                Spacer(Modifier.height(6.dp))
                StyledFormField(
                    value = newTime,
                    onValueChange = { newTime = it },
                    placeholder = "e.g. 03:00 PM - 04:30 PM",
                    testTag = "task_time_input"
                )

                Spacer(Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { showAddModal = false },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, ext.border)
                    ) {
                        Text("Cancel", color = ext.title)
                    }
                    Button(
                        onClick = {
                            if (newTitle.isNotBlank()) {
                                viewModel.addTask(newTitle, newSubject, newTime)
                                newTitle = ""
                                newSubject = ""
                                showAddModal = false
                            }
                        },
                        modifier = Modifier
                            .weight(1.6f)
                            .height(48.dp)
                            .testTag("save_task_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ext.accent,
                            contentColor = Color.White
                        )
                    ) {
                        Text("Save Task", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }

    // Import JSON Bottom Sheet
    if (showImportModal) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showImportModal = false },
            sheetState = sheetState,
            containerColor = ext.card,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 28.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Import Tasks via JSON", style = MaterialTheme.typography.titleLarge, color = ext.title)
                    IconButton(onClick = { showImportModal = false }) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close", tint = ext.title)
                    }
                }
                HorizontalDivider(color = ext.border, modifier = Modifier.padding(vertical = 8.dp))

                Text("JSON SCHEDULE PAYLOAD", style = MaterialTheme.typography.labelSmall, color = ext.subtext)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = jsonInput,
                    onValueChange = { jsonInput = it },
                    placeholder = {
                        Text(
                            "Paste JSON array, e.g. [{\"title\": \"Study\", \"subject\": \"Math\", \"time\": \"10:00 AM\"}]",
                            color = ext.subtext
                        )
                    },
                    textStyle = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = JetBrainsMonoFontFamily,
                        color = ext.title
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 130.dp)
                        .testTag("tracker_json_input"),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ext.accent,
                        unfocusedBorderColor = ext.border,
                        focusedContainerColor = ext.background,
                        unfocusedContainerColor = ext.background
                    )
                )

                Spacer(Modifier.height(22.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { showImportModal = false },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, ext.border)
                    ) {
                        Text("Cancel", color = ext.title)
                    }
                    Button(
                        onClick = {
                            viewModel.importJson(jsonInput) {
                                jsonInput = ""
                                showImportModal = false
                            }
                        },
                        modifier = Modifier
                            .weight(1.6f)
                            .height(48.dp)
                            .testTag("confirm_import_json_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ext.accent,
                            contentColor = Color.White
                        )
                    ) {
                        Text("Import JSON", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }

    if (deleteTaskId != null) {
        DeleteConfirmDialog(
            title = "Delete Task",
            message = "Are you sure you want to remove this task? This action cannot be undone.",
            onCancel = { deleteTaskId = null },
            onConfirm = {
                deleteTaskId?.let { viewModel.deleteTask(it) }
                deleteTaskId = null
            }
        )
    }
}

@Composable
private fun TaskRowCard(
    task: TrackerTaskEntity,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val ext = LocalQuizzMeColors.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (task.completed) 0.65f else 1f)
            .testTag("task_card_${task.id}"),
        shape = RoundedCornerShape(16.dp),
        color = ext.card,
        border = BorderStroke(1.dp, ext.border)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (task.completed) ext.accent else Color.Transparent)
                    .clickable(onClick = onToggle)
                    .testTag("task_checkbox_${task.id}"),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (task.completed) ext.accent else Color.Transparent,
                    border = BorderStroke(2.dp, if (task.completed) ext.accent else ext.border),
                    modifier = Modifier.size(24.dp)
                ) {
                    if (task.completed) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = "Completed",
                            tint = Color.White,
                            modifier = Modifier.padding(3.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None
                    ),
                    color = ext.title
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = task.subject,
                    style = MaterialTheme.typography.bodySmall,
                    color = ext.subtext
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Schedule,
                        contentDescription = null,
                        tint = ext.subtext,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = task.time,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                        color = ext.subtext
                    )
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.testTag("task_delete_${task.id}")
            ) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = "Delete task",
                    tint = ext.danger,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
