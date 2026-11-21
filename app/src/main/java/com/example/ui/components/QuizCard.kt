package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.model.QuestionStyle
import com.example.model.QuizQuestion
import com.example.ui.theme.LocalQuizzMeColors
import com.example.ui.viewmodel.QuizSessionState

@Composable
fun QuizCard(
    question: QuizQuestion,
    sessionState: QuizSessionState,
    questionStyle: QuestionStyle,
    onMcqOptionClick: (String) -> Unit,
    onTfToggle: (Int, Boolean) -> Unit,
    onEvaluateTf: () -> Unit,
    onSaInputChange: (String) -> Unit,
    onRevealSaAnswer: () -> Unit,
    onGradeSa: (Boolean) -> Unit,
    onSeqNotesChange: (String) -> Unit,
    onToggleSeqAnswer: () -> Unit,
    onGradeSeq: (Int) -> Unit,
    onNextQuestion: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalQuizzMeColors.current
    val resolvedStyle = when {
        !question.subQuestions.isNullOrEmpty() -> QuestionStyle.SEQ
        !question.statements.isNullOrEmpty() -> QuestionStyle.TF
        questionStyle == QuestionStyle.SA -> QuestionStyle.SA
        questionStyle == QuestionStyle.SEQ -> QuestionStyle.SEQ
        else -> QuestionStyle.MCQ
    }

    val totalQuestions = sessionState.deck.size.coerceAtLeast(1)
    val rawProgress = (sessionState.currentIndex + 1).toFloat() / totalQuestions.toFloat()
    val animatedProgress by animateFloatAsState(
        targetValue = rawProgress,
        animationSpec = tween(durationMillis = 250),
        label = "quiz_progress"
    )
    val maxScore = sessionState.maxPossibleScore(resolvedStyle)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("quiz_card"),
        shape = RoundedCornerShape(22.dp),
        color = ext.card,
        border = BorderStroke(1.dp, ext.border),
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Progress Bar + Score Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .weight(1f)
                        .height(7.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = ext.accent,
                    trackColor = ext.border,
                    strokeCap = StrokeCap.Round
                )

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = ext.accentSoft
                ) {
                    Text(
                        text = "${sessionState.runningScore} / $maxScore",
                        style = MaterialTheme.typography.labelLarge,
                        color = ext.accent,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Question Stem
            Text(
                text = "${sessionState.currentIndex + 1}. ${question.question}",
                style = MaterialTheme.typography.titleMedium,
                color = ext.title
            )

            Spacer(modifier = Modifier.height(16.dp))

            when (resolvedStyle) {
                QuestionStyle.MCQ -> {
                    val options = question.options.orEmpty()
                    options.forEachIndexed { idx, option ->
                        val isSelected = sessionState.chosenAnswer == option
                        val isCorrect = option == question.correctAnswer
                        val answered = sessionState.chosenAnswer != null

                        val bgColor = when {
                            answered && isCorrect -> ext.successSoft
                            answered && isSelected -> ext.dangerSoft
                            else -> Color.Transparent
                        }
                        val borderColor = when {
                            answered && isCorrect -> ext.success
                            answered && isSelected -> ext.danger
                            else -> ext.border
                        }

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                                .minimumInteractiveComponentSize()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onMcqOptionClick(option) }
                                .testTag("mcq_option_$idx"),
                            shape = RoundedCornerShape(14.dp),
                            color = bgColor,
                            border = BorderStroke(1.dp, borderColor)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${idx + 1}. $option",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = ext.title,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 10.dp)
                                )
                                if (answered && isCorrect) {
                                    Icon(
                                        imageVector = Icons.Rounded.CheckCircle,
                                        contentDescription = "Correct option",
                                        tint = ext.success,
                                        modifier = Modifier.size(18.dp)
                                    )
                                } else if (answered && isSelected) {
                                    Icon(
                                        imageVector = Icons.Rounded.Cancel,
                                        contentDescription = "Incorrect selection",
                                        tint = ext.danger,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    AnimatedVisibility(visible = sessionState.chosenAnswer != null && !question.explanation.isNullOrBlank()) {
                        ExplanationBox(explanation = question.explanation.orEmpty())
                    }

                    AnimatedVisibility(visible = sessionState.chosenAnswer != null) {
                        NextQuestionButton(
                            isLast = sessionState.currentIndex + 1 == totalQuestions,
                            onClick = onNextQuestion
                        )
                    }
                }

                QuestionStyle.TF -> {
                    val statements = question.statements.orEmpty()
                    statements.forEachIndexed { sIdx, statement ->
                        val selected = sessionState.tfSelections[sIdx]
                        val correctBool = question.answers?.getOrNull(sIdx)
                        val isMatch = selected != null && selected == correctBool

                        val rowBg = if (sessionState.tfChecked) {
                            if (isMatch) ext.successSoft else ext.dangerSoft
                        } else Color.Transparent
                        val rowBorder = if (sessionState.tfChecked) {
                            if (isMatch) ext.success else ext.danger
                        } else ext.border

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp),
                            shape = RoundedCornerShape(14.dp),
                            color = rowBg,
                            border = BorderStroke(1.dp, rowBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${sIdx + 1}. $statement",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = ext.title
                                    )
                                    if (sessionState.tfChecked) {
                                        val selectedStr = when (selected) {
                                            true -> "T"
                                            false -> "F"
                                            null -> "None"
                                        }
                                        val correctStr = if (correctBool == true) "T" else "F"
                                        Text(
                                            text = "Selected: $selectedStr | Correct: $correctStr",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (isMatch) ext.success else ext.danger,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    TfToggleButton(
                                        label = "T",
                                        selected = selected == true,
                                        activeColor = ext.accent,
                                        enabled = !sessionState.tfChecked,
                                        testTag = "tf_true_$sIdx",
                                        onClick = { onTfToggle(sIdx, true) }
                                    )
                                    TfToggleButton(
                                        label = "F",
                                        selected = selected == false,
                                        activeColor = ext.danger,
                                        enabled = !sessionState.tfChecked,
                                        testTag = "tf_false_$sIdx",
                                        onClick = { onTfToggle(sIdx, false) }
                                    )
                                }
                            }
                        }
                    }

                    AnimatedVisibility(visible = sessionState.tfChecked && !question.explanation.isNullOrBlank()) {
                        ExplanationBox(explanation = question.explanation.orEmpty())
                    }

                    if (sessionState.tfChecked) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = ext.background
                        ) {
                            Text(
                                text = "Question Marks: +${sessionState.tfQuestionScore} / ${statements.size.coerceAtLeast(5)}",
                                style = MaterialTheme.typography.labelLarge,
                                color = ext.title,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                        NextQuestionButton(
                            isLast = sessionState.currentIndex + 1 == totalQuestions,
                            onClick = onNextQuestion
                        )
                    } else {
                        OutlinedButton(
                            onClick = onEvaluateTf,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                                .height(48.dp)
                                .testTag("check_tf_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = ext.accentSoft,
                                contentColor = ext.accent
                            ),
                            border = BorderStroke(1.dp, ext.accent)
                        ) {
                            Text("Check Statements", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }

                QuestionStyle.SA -> {
                    OutlinedTextField(
                        value = sessionState.saInputText,
                        onValueChange = onSaInputChange,
                        enabled = !sessionState.saChecked,
                        placeholder = {
                            Text(
                                "Type your answer explanation here...",
                                color = ext.subtext
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 100.dp)
                            .testTag("sa_input"),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ext.accent,
                            unfocusedBorderColor = ext.border,
                            focusedTextColor = ext.title,
                            unfocusedTextColor = ext.title,
                            disabledTextColor = ext.text,
                            focusedContainerColor = ext.background,
                            unfocusedContainerColor = ext.background
                        )
                    )

                    AnimatedVisibility(visible = sessionState.saChecked) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            shape = RoundedCornerShape(14.dp),
                            color = ext.background,
                            border = BorderStroke(1.dp, ext.border)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "CORRECT KEY / TERM",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ext.accent
                                )
                                Text(
                                    text = question.correctAnswer.orEmpty(),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = ext.title,
                                    modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)
                                )
                                if (!question.explanation.isNullOrBlank()) {
                                    Text(
                                        text = "CONTEXT EXPLANATION",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ext.accent
                                    )
                                    Text(
                                        text = question.explanation,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = ext.text,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (!sessionState.saChecked) {
                        OutlinedButton(
                            onClick = onRevealSaAnswer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 14.dp)
                                .height(48.dp)
                                .testTag("reveal_sa_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = ext.accentSoft,
                                contentColor = ext.accent
                            ),
                            border = BorderStroke(1.dp, ext.accent)
                        ) {
                            Text("Reveal Solution", style = MaterialTheme.typography.labelLarge)
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onGradeSa(false) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("sa_incorrect_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = ext.dangerSoft,
                                    contentColor = ext.danger
                                ),
                                border = BorderStroke(1.dp, ext.danger)
                            ) {
                                Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Incorrect", style = MaterialTheme.typography.labelLarge)
                            }

                            OutlinedButton(
                                onClick = { onGradeSa(true) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("sa_correct_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = ext.successSoft,
                                    contentColor = ext.success
                                ),
                                border = BorderStroke(1.dp, ext.success)
                            ) {
                                Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Correct", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }

                QuestionStyle.SEQ -> {
                    question.subQuestions.orEmpty().forEachIndexed { sqIdx, sq ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp),
                            shape = RoundedCornerShape(14.dp),
                            color = ext.background,
                            border = BorderStroke(1.dp, ext.border)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text(
                                        text = sq.subQuestion,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = ext.title,
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(end = 8.dp)
                                    )
                                    if (sq.marks != null) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = ext.accentSoft
                                        ) {
                                            Text(
                                                text = "[${sq.marks} Marks]",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = ext.accent,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }

                                AnimatedVisibility(visible = sessionState.showSeqAnswer) {
                                    Column(modifier = Modifier.padding(top = 10.dp)) {
                                        HorizontalDivider(color = ext.accent.copy(alpha = 0.4f))
                                        Spacer(Modifier.height(8.dp))
                                        Text(
                                            text = "SUB-QUESTION ANSWER KEY",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ext.accent
                                        )
                                        Text(
                                            text = sq.answerKey,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = ext.text,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Text(
                        text = "DRAFT YOUR ESSAY RESPONSE / ROUGH NOTES",
                        style = MaterialTheme.typography.labelSmall,
                        color = ext.subtext,
                        modifier = Modifier.padding(top = 6.dp, bottom = 6.dp)
                    )
                    OutlinedTextField(
                        value = sessionState.seqUserNotes,
                        onValueChange = onSeqNotesChange,
                        placeholder = {
                            Text(
                                "Write key outline points or essay response before toggling the model answer...",
                                color = ext.subtext
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 110.dp)
                            .testTag("seq_notes_input"),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ext.accent,
                            unfocusedBorderColor = ext.border,
                            focusedTextColor = ext.title,
                            unfocusedTextColor = ext.title,
                            focusedContainerColor = ext.background,
                            unfocusedContainerColor = ext.background
                        )
                    )

                    AnimatedVisibility(visible = sessionState.showSeqAnswer && !question.modelAnswer.isNullOrBlank()) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            shape = RoundedCornerShape(14.dp),
                            color = ext.accentSoft,
                            border = BorderStroke(1.dp, ext.accent)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.AutoStories,
                                        contentDescription = null,
                                        tint = ext.accent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "COMPLETE MODEL ANSWER SYNTHESIS",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ext.accent
                                    )
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = question.modelAnswer.orEmpty(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = ext.title
                                )
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = onToggleSeqAnswer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                            .height(48.dp)
                            .testTag("toggle_seq_answer_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (sessionState.showSeqAnswer) ext.background else ext.accentSoft,
                            contentColor = ext.accent
                        ),
                        border = BorderStroke(1.dp, ext.accent)
                    ) {
                        Icon(
                            imageVector = if (sessionState.showSeqAnswer) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (sessionState.showSeqAnswer) "Hide Model Answer" else "Toggle & Show Full Model Answer",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    AnimatedVisibility(visible = sessionState.showSeqAnswer) {
                        Column(modifier = Modifier.padding(top = 16.dp)) {
                            Text(
                                text = "SELF-GRADE YOUR RESPONSE (OUT OF 10 MARKS)",
                                style = MaterialTheme.typography.labelSmall,
                                color = ext.title,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(0, 3, 5, 7, 10).forEach { mark ->
                                    Surface(
                                        onClick = { onGradeSeq(mark) },
                                        shape = RoundedCornerShape(10.dp),
                                        color = ext.background,
                                        border = BorderStroke(1.dp, ext.accent),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .testTag("seq_grade_$mark")
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "+$mark",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = ext.accent
                                            )
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

@Composable
private fun TfToggleButton(
    label: String,
    selected: Boolean,
    activeColor: Color,
    enabled: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    val ext = LocalQuizzMeColors.current
    Surface(
        onClick = { if (enabled) onClick() },
        enabled = enabled,
        shape = RoundedCornerShape(10.dp),
        color = if (selected) activeColor else ext.border.copy(alpha = 0.35f),
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .size(36.dp)
            .testTag(testTag)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = if (selected) Color.White else ext.title
            )
        }
    }
}

@Composable
private fun ExplanationBox(explanation: String) {
    val ext = LocalQuizzMeColors.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
        shape = RoundedCornerShape(14.dp),
        color = ext.background,
        border = BorderStroke(1.dp, ext.accent)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Lightbulb,
                    contentDescription = null,
                    tint = ext.accent,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "EXPLANATION",
                    style = MaterialTheme.typography.labelSmall,
                    color = ext.accent
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = explanation,
                style = MaterialTheme.typography.bodyMedium,
                color = ext.title
            )
        }
    }
}

@Composable
private fun NextQuestionButton(
    isLast: Boolean,
    onClick: () -> Unit
) {
    val ext = LocalQuizzMeColors.current
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp)
            .height(48.dp)
            .testTag("next_question_button"),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = ext.accentSoft,
            contentColor = ext.accent
        ),
        border = BorderStroke(1.dp, ext.accent)
    ) {
        Text(
            text = if (isLast) "View Final Results" else "Next Question",
            style = MaterialTheme.typography.labelLarge
        )
        Spacer(Modifier.width(8.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
        )
    }
}
