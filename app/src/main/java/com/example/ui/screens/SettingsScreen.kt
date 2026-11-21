package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.QuestionStyle
import com.example.ui.components.QuizzMeAlertDialog
import com.example.ui.components.QuizzMeHeader
import com.example.ui.theme.LocalQuizzMeColors
import com.example.ui.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val ext = LocalQuizzMeColors.current

    var aiExpanded by remember { mutableStateOf(false) }
    var quizExpanded by remember { mutableStateOf(true) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        QuizzMeHeader(title = "Configuration Settings")

        Column(
            modifier = Modifier
                .weight(1f)
                .widthIn(max = 680.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            // Section 1: AI Settings (Firebase AI Logic)
            AccordionHeader(
                title = "AI SETTINGS",
                expanded = aiExpanded,
                onClick = { aiExpanded = !aiExpanded },
                tag = "settings_ai_accordion"
            )

            AnimatedVisibility(visible = aiExpanded) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = ext.card,
                    border = BorderStroke(1.dp, ext.border)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = ext.accent,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Firebase AI Logic (Gemini 3.8 Flash)",
                                style = MaterialTheme.typography.titleSmall,
                                color = ext.title
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "Authenticated automatically via App Check. No manual API key is needed.",
                                style = MaterialTheme.typography.bodySmall,
                                color = ext.subtext
                            )
                        }
                    }
                }
            }

            HorizontalDivider(
                color = ext.border,
                modifier = Modifier.padding(vertical = 14.dp)
            )

            // Section 2: Quiz Settings
            AccordionHeader(
                title = "QUIZ SETTINGS",
                expanded = quizExpanded,
                onClick = { quizExpanded = !quizExpanded },
                tag = "settings_quiz_accordion"
            )

            AnimatedVisibility(visible = quizExpanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    Text(
                        text = "Number of Questions",
                        style = MaterialTheme.typography.titleSmall,
                        color = ext.title
                    )
                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(5, 10, 15, 20).forEach { count ->
                            SettingsOptionChip(
                                label = count.toString(),
                                selected = uiState.questionCount == count,
                                onClick = { viewModel.setQuestionCount(count) },
                                tag = "settings_count_$count",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(Modifier.height(18.dp))

                    Text(
                        text = "Question Type",
                        style = MaterialTheme.typography.titleSmall,
                        color = ext.title
                    )
                    Spacer(Modifier.height(8.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SettingsOptionChip(
                                label = "MCQ",
                                selected = uiState.questionStyle == QuestionStyle.MCQ,
                                onClick = { viewModel.setQuestionStyle(QuestionStyle.MCQ) },
                                tag = "settings_style_mcq",
                                modifier = Modifier.weight(1f)
                            )
                            SettingsOptionChip(
                                label = "True / False",
                                selected = uiState.questionStyle == QuestionStyle.TF,
                                onClick = { viewModel.setQuestionStyle(QuestionStyle.TF) },
                                tag = "settings_style_tf",
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SettingsOptionChip(
                                label = "Short Answer",
                                selected = uiState.questionStyle == QuestionStyle.SA,
                                onClick = { viewModel.setQuestionStyle(QuestionStyle.SA) },
                                tag = "settings_style_sa",
                                modifier = Modifier.weight(1f)
                            )
                            SettingsOptionChip(
                                label = "Structured",
                                selected = uiState.questionStyle == QuestionStyle.SEQ,
                                onClick = { viewModel.setQuestionStyle(QuestionStyle.SEQ) },
                                tag = "settings_style_seq",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(Modifier.height(18.dp))

                    Text(
                        text = "Custom Generation Instructions",
                        style = MaterialTheme.typography.titleSmall,
                        color = ext.title
                    )
                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = uiState.customPrompt,
                        onValueChange = viewModel::setCustomPrompt,
                        placeholder = {
                            Text("e.g., Focus heavily on clinical diagnostics...", color = ext.subtext)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 96.dp)
                            .testTag("settings_custom_prompt_input"),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ext.accent,
                            unfocusedBorderColor = ext.border,
                            focusedContainerColor = ext.card,
                            unfocusedContainerColor = ext.card,
                            focusedTextColor = ext.title,
                            unfocusedTextColor = ext.title
                        )
                    )

                    Spacer(Modifier.height(20.dp))

                    OutlinedButton(
                        onClick = viewModel::saveSettings,
                        enabled = !uiState.isSaving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("settings_save_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = ext.accentSoft,
                            contentColor = ext.accent
                        ),
                        border = BorderStroke(1.dp, ext.accent)
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = ext.accent
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Save,
                                contentDescription = null,
                                tint = ext.accent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Save Configurations", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    }

    if (uiState.alertTitle != null && uiState.alertMessage != null) {
        QuizzMeAlertDialog(
            title = uiState.alertTitle!!,
            message = uiState.alertMessage!!,
            isSuccess = uiState.alertIsSuccess,
            onDismiss = viewModel::dismissAlert
        )
    }
}

@Composable
private fun AccordionHeader(
    title: String,
    expanded: Boolean,
    onClick: () -> Unit,
    tag: String
) {
    val ext = LocalQuizzMeColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp)
            .testTag(tag),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = ext.accent
        )
        Icon(
            imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
            contentDescription = if (expanded) "Collapse" else "Expand",
            tint = ext.accent
        )
    }
}

@Composable
private fun SettingsOptionChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    tag: String,
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
            .testTag(tag)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold
                ),
                color = if (selected) ext.accent else ext.title
            )
        }
    }
}
