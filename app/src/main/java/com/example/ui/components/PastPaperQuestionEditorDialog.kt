package com.example.ui.components

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.QuestionStyle
import com.example.model.QuizQuestion
import com.example.model.SeqSubQuestion
import com.example.ui.theme.LocalQuizzMeColors

@Composable
fun PastPaperQuestionEditorDialog(
    initialQuestion: QuizQuestion?,
    defaultYear: String,
    defaultLesson: String,
    isTablet: Boolean,
    onDismiss: () -> Unit,
    onSave: (QuizQuestion) -> Unit
) {
    val ext = LocalQuizzMeColors.current

    var style by remember(initialQuestion) {
        mutableStateOf(initialQuestion?.detectedStyle() ?: QuestionStyle.MCQ)
    }
    var questionText by remember(initialQuestion) {
        mutableStateOf(initialQuestion?.question.orEmpty())
    }
    var yearText by remember(initialQuestion) {
        mutableStateOf(initialQuestion?.year?.takeIf { it.isNotBlank() } ?: defaultYear)
    }
    var lessonText by remember(initialQuestion) {
        mutableStateOf(initialQuestion?.lesson?.takeIf { it.isNotBlank() } ?: defaultLesson)
    }
    var explanationText by remember(initialQuestion) {
        mutableStateOf(initialQuestion?.explanation.orEmpty())
    }

    // MCQ options + correct response indices
    val mcqOptions = remember(initialQuestion) {
        mutableStateListOf<String>().apply {
            val existing = initialQuestion?.options.orEmpty()
            if (existing.isNotEmpty()) {
                addAll(existing)
            } else {
                addAll(listOf("", "", "", ""))
            }
        }
    }
    val selectedCorrectOptions = remember(initialQuestion) {
        mutableStateListOf<Int>().apply {
            val responses = initialQuestion?.resolvedCorrectResponses().orEmpty()
            mcqOptions.forEachIndexed { idx, opt ->
                if (opt.isNotBlank() && responses.any { it.equals(opt, ignoreCase = true) }) {
                    add(idx)
                }
            }
            if (isEmpty() && mcqOptions.isNotEmpty()) {
                add(0)
            }
        }
    }

    // TF statements + answers
    val tfStatements = remember(initialQuestion) {
        mutableStateListOf<String>().apply {
            val existing = initialQuestion?.statements.orEmpty()
            if (existing.isNotEmpty()) addAll(existing) else addAll(listOf("", "", ""))
        }
    }
    val tfAnswers = remember(initialQuestion) {
        mutableStateListOf<Boolean>().apply {
            val existing = initialQuestion?.answers.orEmpty()
            for (i in tfStatements.indices) {
                add(existing.getOrElse(i) { true })
            }
        }
    }

    // SA correct answer
    var saCorrectText by remember(initialQuestion) {
        mutableStateOf(initialQuestion?.primaryCorrectAnswer().orEmpty())
    }

    // SEQ sub-questions + model answer
    val seqPrompts = remember(initialQuestion) {
        mutableStateListOf<String>().apply {
            val existing = initialQuestion?.subQuestions.orEmpty()
            if (existing.isNotEmpty()) addAll(existing.map { it.subQuestion }) else add("")
        }
    }
    val seqKeys = remember(initialQuestion) {
        mutableStateListOf<String>().apply {
            val existing = initialQuestion?.subQuestions.orEmpty()
            if (existing.isNotEmpty()) addAll(existing.map { it.answerKey }) else add("")
        }
    }
    var seqModelAnswer by remember(initialQuestion) {
        mutableStateOf(initialQuestion?.modelAnswer.orEmpty())
    }

    var validationError by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = !isTablet)
    ) {
        Surface(
            modifier = if (isTablet) {
                Modifier
                    .widthIn(min = 600.dp, max = 820.dp)
                    .fillMaxWidth()
                    .heightIn(min = 520.dp, max = 780.dp)
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
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = ext.subtext, style = MaterialTheme.typography.labelLarge)
                    }
                    Text(
                        text = if (initialQuestion == null) "Add Question" else "Edit Question",
                        style = MaterialTheme.typography.titleLarge,
                        color = ext.title
                    )
                    TextButton(
                        onClick = {
                            val cleanQ = questionText.trim()
                            if (cleanQ.isBlank()) {
                                validationError = "Please enter the question stem."
                                return@TextButton
                            }
                            val built = when (style) {
                                QuestionStyle.MCQ -> {
                                    val validEntries = mcqOptions.mapIndexedNotNull { idx, text ->
                                        val clean = text.trim()
                                        if (clean.isNotBlank()) idx to clean else null
                                    }
                                    if (validEntries.size < 2) {
                                        validationError = "Add at least 2 answer options."
                                        return@TextButton
                                    }
                                    val correctList = validEntries
                                        .filter { (idx, _) -> selectedCorrectOptions.contains(idx) }
                                        .map { it.second }
                                    if (correctList.isEmpty()) {
                                        validationError = "Mark at least 1 option as a correct response."
                                        return@TextButton
                                    }
                                    QuizQuestion(
                                        type = "MCQ",
                                        question = cleanQ,
                                        options = validEntries.map { it.second },
                                        correctAnswer = correctList.first(),
                                        correctAnswers = if (correctList.size > 1) correctList else null,
                                        explanation = explanationText.trim().ifBlank { null },
                                        year = yearText.trim().ifBlank { null },
                                        lesson = lessonText.trim().ifBlank { null }
                                    )
                                }
                                QuestionStyle.TF -> {
                                    val pairs = tfStatements.indices.mapNotNull { i ->
                                        val s = tfStatements[i].trim()
                                        if (s.isNotBlank()) s to tfAnswers.getOrElse(i) { true } else null
                                    }
                                    if (pairs.isEmpty()) {
                                        validationError = "Add at least 1 True/False statement."
                                        return@TextButton
                                    }
                                    QuizQuestion(
                                        type = "TF",
                                        question = cleanQ,
                                        statements = pairs.map { it.first },
                                        answers = pairs.map { it.second },
                                        explanation = explanationText.trim().ifBlank { null },
                                        year = yearText.trim().ifBlank { null },
                                        lesson = lessonText.trim().ifBlank { null }
                                    )
                                }
                                QuestionStyle.SA -> {
                                    val cleanAns = saCorrectText.trim()
                                    if (cleanAns.isBlank()) {
                                        validationError = "Enter the correct response for this short answer."
                                        return@TextButton
                                    }
                                    QuizQuestion(
                                        type = "SA",
                                        question = cleanQ,
                                        correctAnswer = cleanAns,
                                        explanation = explanationText.trim().ifBlank { null },
                                        year = yearText.trim().ifBlank { null },
                                        lesson = lessonText.trim().ifBlank { null }
                                    )
                                }
                                QuestionStyle.SEQ -> {
                                    val subs = seqPrompts.indices.mapNotNull { i ->
                                        val p = seqPrompts[i].trim()
                                        val k = seqKeys.getOrElse(i) { "" }.trim()
                                        if (p.isNotBlank()) SeqSubQuestion(subQuestion = p, answerKey = k) else null
                                    }
                                    if (subs.isEmpty() && seqModelAnswer.isBlank()) {
                                        validationError = "Add at least 1 sub-question or model answer."
                                        return@TextButton
                                    }
                                    QuizQuestion(
                                        type = "SEQ",
                                        question = cleanQ,
                                        subQuestions = subs.ifEmpty { null },
                                        modelAnswer = seqModelAnswer.trim().ifBlank { null },
                                        explanation = explanationText.trim().ifBlank { null },
                                        year = yearText.trim().ifBlank { null },
                                        lesson = lessonText.trim().ifBlank { null }
                                    )
                                }
                            }
                            onSave(built)
                        },
                        modifier = Modifier.testTag("question_editor_save_button")
                    ) {
                        Text("Save", color = ext.accent, style = MaterialTheme.typography.labelLarge)
                    }
                }
                HorizontalDivider(color = ext.border)

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (validationError != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = ext.dangerSoft,
                            border = BorderStroke(1.dp, ext.danger),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = validationError!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = ext.danger,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    // Format Selector
                    Text("QUESTION FORMAT", style = MaterialTheme.typography.labelSmall, color = ext.accent)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuestionStyle.entries.forEach { option ->
                            val selected = style == option
                            Surface(
                                onClick = {
                                    style = option
                                    validationError = null
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = if (selected) ext.accentSoft else ext.card,
                                border = BorderStroke(1.dp, if (selected) ext.accent else ext.border),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = option.shortLabel,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold
                                        ),
                                        color = if (selected) ext.accent else ext.title
                                    )
                                }
                            }
                        }
                    }

                    // Year & Lesson row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(modifier = Modifier.weight(0.4f)) {
                            Text("YEAR", style = MaterialTheme.typography.labelSmall, color = ext.accent)
                            Spacer(Modifier.height(6.dp))
                            EditorTextField(
                                value = yearText,
                                onValueChange = { yearText = it },
                                placeholder = "e.g., 2024",
                                singleLine = true,
                                testTag = "question_editor_year_input"
                            )
                        }
                        Column(modifier = Modifier.weight(0.6f)) {
                            Text("LESSON", style = MaterialTheme.typography.labelSmall, color = ext.accent)
                            Spacer(Modifier.height(6.dp))
                            EditorTextField(
                                value = lessonText,
                                onValueChange = { lessonText = it },
                                placeholder = "e.g., Cardiovascular",
                                singleLine = true,
                                testTag = "question_editor_lesson_input"
                            )
                        }
                    }

                    // Question Stem
                    Text("QUESTION", style = MaterialTheme.typography.labelSmall, color = ext.accent)
                    EditorTextField(
                        value = questionText,
                        onValueChange = {
                            questionText = it
                            validationError = null
                        },
                        placeholder = "Enter the past paper question...",
                        minHeight = 90,
                        testTag = "question_editor_stem_input"
                    )

                    // Style-specific options / responses
                    when (style) {
                        QuestionStyle.MCQ -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "OPTIONS & CORRECT RESPONSE(S)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ext.accent
                                )
                                Text(
                                    text = "Tap circle to mark correct",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ext.subtext
                                )
                            }

                            mcqOptions.forEachIndexed { idx, opt ->
                                val isCorrect = selectedCorrectOptions.contains(idx)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            if (isCorrect) {
                                                selectedCorrectOptions.remove(idx)
                                            } else {
                                                selectedCorrectOptions.add(idx)
                                            }
                                            validationError = null
                                        },
                                        modifier = Modifier.minimumInteractiveComponentSize()
                                    ) {
                                        Icon(
                                            imageVector = if (isCorrect) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                                            contentDescription = if (isCorrect) "Correct option" else "Mark as correct",
                                            tint = if (isCorrect) ext.success else ext.subtext
                                        )
                                    }

                                    OutlinedTextField(
                                        value = opt,
                                        onValueChange = {
                                            mcqOptions[idx] = it
                                            validationError = null
                                        },
                                        placeholder = { Text("Option ${idx + 1}", color = ext.subtext) },
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = if (isCorrect) ext.success else ext.accent,
                                            unfocusedBorderColor = if (isCorrect) ext.success else ext.border,
                                            focusedContainerColor = if (isCorrect) ext.successSoft else ext.card,
                                            unfocusedContainerColor = if (isCorrect) ext.successSoft else ext.card,
                                            focusedTextColor = ext.title,
                                            unfocusedTextColor = ext.title
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )

                                    if (mcqOptions.size > 2) {
                                        IconButton(
                                            onClick = {
                                                mcqOptions.removeAt(idx)
                                                val updated = selectedCorrectOptions.mapNotNull {
                                                    when {
                                                        it == idx -> null
                                                        it > idx -> it - 1
                                                        else -> it
                                                    }
                                                }
                                                selectedCorrectOptions.clear()
                                                selectedCorrectOptions.addAll(updated)
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Delete,
                                                contentDescription = "Remove option",
                                                tint = ext.danger,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            OutlinedButton(
                                onClick = { mcqOptions.add("") },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, ext.border),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = null, tint = ext.accent, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Add Option", color = ext.accent)
                            }
                        }

                        QuestionStyle.TF -> {
                            Text("STATEMENTS & CORRECT RESPONSES", style = MaterialTheme.typography.labelSmall, color = ext.accent)
                            tfStatements.forEachIndexed { idx, stmt ->
                                val isTrue = tfAnswers.getOrElse(idx) { true }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = stmt,
                                        onValueChange = { tfStatements[idx] = it },
                                        placeholder = { Text("Statement ${idx + 1}", color = ext.subtext) },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = ext.accent,
                                            unfocusedBorderColor = ext.border,
                                            focusedContainerColor = ext.card,
                                            unfocusedContainerColor = ext.card,
                                            focusedTextColor = ext.title,
                                            unfocusedTextColor = ext.title
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                    Surface(
                                        onClick = { tfAnswers[idx] = !isTrue },
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isTrue) ext.successSoft else ext.dangerSoft,
                                        border = BorderStroke(1.dp, if (isTrue) ext.success else ext.danger),
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = if (isTrue) "T" else "F",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = if (isTrue) ext.success else ext.danger
                                            )
                                        }
                                    }
                                    if (tfStatements.size > 1) {
                                        IconButton(
                                            onClick = {
                                                tfStatements.removeAt(idx)
                                                tfAnswers.removeAt(idx)
                                            }
                                        ) {
                                            Icon(Icons.Rounded.Delete, contentDescription = "Remove", tint = ext.danger, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                            OutlinedButton(
                                onClick = {
                                    tfStatements.add("")
                                    tfAnswers.add(true)
                                },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, ext.border),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = null, tint = ext.accent, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Add Statement", color = ext.accent)
                            }
                        }

                        QuestionStyle.SA -> {
                            Text("CORRECT RESPONSE", style = MaterialTheme.typography.labelSmall, color = ext.accent)
                            EditorTextField(
                                value = saCorrectText,
                                onValueChange = {
                                    saCorrectText = it
                                    validationError = null
                                },
                                placeholder = "Enter the concise correct answer...",
                                minHeight = 72,
                                testTag = "question_editor_sa_answer"
                            )
                        }

                        QuestionStyle.SEQ -> {
                            Text("SUB-QUESTIONS & ANSWER KEYS", style = MaterialTheme.typography.labelSmall, color = ext.accent)
                            seqPrompts.forEachIndexed { idx, prompt ->
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = ext.card,
                                    border = BorderStroke(1.dp, ext.border),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        EditorTextField(
                                            value = prompt,
                                            onValueChange = { seqPrompts[idx] = it },
                                            placeholder = "Sub-question ${idx + 1} (e.g., (a) Describe...)",
                                            singleLine = false
                                        )
                                        EditorTextField(
                                            value = seqKeys.getOrElse(idx) { "" },
                                            onValueChange = {
                                                if (idx < seqKeys.size) seqKeys[idx] = it
                                            },
                                            placeholder = "Marking key / points...",
                                            singleLine = false
                                        )
                                    }
                                }
                            }
                            OutlinedButton(
                                onClick = {
                                    seqPrompts.add("")
                                    seqKeys.add("")
                                },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, ext.border),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = null, tint = ext.accent, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Add Sub-question", color = ext.accent)
                            }

                            Text("MODEL ANSWER", style = MaterialTheme.typography.labelSmall, color = ext.accent)
                            EditorTextField(
                                value = seqModelAnswer,
                                onValueChange = { seqModelAnswer = it },
                                placeholder = "Enter complete model essay answer...",
                                minHeight = 96
                            )
                        }
                    }

                    // Answer Explained
                    Text("ANSWER EXPLAINED", style = MaterialTheme.typography.labelSmall, color = ext.accent)
                    EditorTextField(
                        value = explanationText,
                        onValueChange = { explanationText = it },
                        placeholder = "Explain why the correct response is right...",
                        minHeight = 96,
                        testTag = "question_editor_explanation_input"
                    )

                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun EditorTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    singleLine: Boolean = false,
    minHeight: Int = 52,
    testTag: String? = null
) {
    val ext = LocalQuizzMeColors.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = singleLine,
        placeholder = { Text(placeholder, color = ext.subtext) },
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
            .then(if (!singleLine) Modifier.heightIn(min = minHeight.dp) else Modifier)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
    )
}
