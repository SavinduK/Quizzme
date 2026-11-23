package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.GeminiModelOption
import com.example.model.QuestionStyle
import com.example.ui.components.QuizzMeAlertDialog
import com.example.ui.components.QuizzMeHeader
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalQuizzMeColors
import com.example.ui.viewmodel.SettingsUiState
import com.example.ui.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    widthSizeClass: WindowWidthSizeClass,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val ext = LocalQuizzMeColors.current
    val isTablet = widthSizeClass != WindowWidthSizeClass.Compact

    var aiExpanded by remember { mutableStateOf(true) }
    var quizExpanded by remember { mutableStateOf(true) }
    var showApiKey by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        QuizzMeHeader(title = "Configuration Settings")

        if (isTablet) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .widthIn(max = 1280.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Surface(
                        modifier = Modifier.weight(1.25f),
                        shape = RoundedCornerShape(18.dp),
                        color = ext.card,
                        border = BorderStroke(1.dp, ext.border)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = "AI SETTINGS",
                                style = MaterialTheme.typography.labelMedium,
                                color = ext.accent
                            )
                            Spacer(Modifier.height(12.dp))
                            AiSettingsContent(
                                uiState = uiState,
                                showApiKey = showApiKey,
                                onToggleShowApiKey = { showApiKey = !showApiKey },
                                onApiKeyChange = viewModel::setGeminiApiKey,
                                onModelSelect = viewModel::setSelectedModel,
                                twoColumnModels = true
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.weight(0.95f),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            color = ext.card,
                            border = BorderStroke(1.dp, ext.border)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text(
                                    text = "QUIZ SETTINGS",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ext.accent
                                )
                                Spacer(Modifier.height(12.dp))
                                QuizSettingsContent(
                                    uiState = uiState,
                                    onQuestionCountChange = viewModel::setQuestionCount,
                                    onQuestionStyleChange = viewModel::setQuestionStyle,
                                    onCustomPromptChange = viewModel::setCustomPrompt
                                )
                            }
                        }

                        SaveSettingsButton(
                            isSaving = uiState.isSaving,
                            onSave = viewModel::saveSettings
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
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                AccordionHeader(
                    title = "AI SETTINGS",
                    expanded = aiExpanded,
                    onClick = { aiExpanded = !aiExpanded },
                    tag = "settings_ai_accordion"
                )

                AnimatedVisibility(visible = aiExpanded) {
                    Column(modifier = Modifier.padding(top = 10.dp, bottom = 6.dp)) {
                        AiSettingsContent(
                            uiState = uiState,
                            showApiKey = showApiKey,
                            onToggleShowApiKey = { showApiKey = !showApiKey },
                            onApiKeyChange = viewModel::setGeminiApiKey,
                            onModelSelect = viewModel::setSelectedModel,
                            twoColumnModels = false
                        )
                    }
                }

                HorizontalDivider(
                    color = ext.border,
                    modifier = Modifier.padding(vertical = 14.dp)
                )

                AccordionHeader(
                    title = "QUIZ SETTINGS",
                    expanded = quizExpanded,
                    onClick = { quizExpanded = !quizExpanded },
                    tag = "settings_quiz_accordion"
                )

                AnimatedVisibility(visible = quizExpanded) {
                    Column(modifier = Modifier.padding(top = 12.dp)) {
                        QuizSettingsContent(
                            uiState = uiState,
                            onQuestionCountChange = viewModel::setQuestionCount,
                            onQuestionStyleChange = viewModel::setQuestionStyle,
                            onCustomPromptChange = viewModel::setCustomPrompt
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                SaveSettingsButton(
                    isSaving = uiState.isSaving,
                    onSave = viewModel::saveSettings
                )

                Spacer(Modifier.height(24.dp))
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
private fun AiSettingsContent(
    uiState: SettingsUiState,
    showApiKey: Boolean,
    onToggleShowApiKey: () -> Unit,
    onApiKeyChange: (String) -> Unit,
    onModelSelect: (GeminiModelOption) -> Unit,
    twoColumnModels: Boolean
) {
    val ext = LocalQuizzMeColors.current

    Text(
        text = "Gemini API Key",
        style = MaterialTheme.typography.titleSmall,
        color = ext.title
    )
    Spacer(Modifier.height(6.dp))

    OutlinedTextField(
        value = uiState.geminiApiKey,
        onValueChange = onApiKeyChange,
        singleLine = true,
        placeholder = {
            Text("Paste your Gemini API key (AIza...)", color = ext.subtext)
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Rounded.Key,
                contentDescription = null,
                tint = ext.accent,
                modifier = Modifier.size(18.dp)
            )
        },
        trailingIcon = {
            IconButton(
                onClick = onToggleShowApiKey,
                modifier = Modifier.testTag("settings_toggle_api_key_visibility")
            ) {
                Icon(
                    imageVector = if (showApiKey) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                    contentDescription = if (showApiKey) "Hide API key" else "Show API key",
                    tint = ext.subtext
                )
            }
        },
        visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
        textStyle = MaterialTheme.typography.bodyMedium.copy(
            fontFamily = JetBrainsMonoFontFamily,
            color = ext.title
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("settings_api_key_input"),
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = ext.accent,
            unfocusedBorderColor = ext.border,
            focusedContainerColor = ext.background,
            unfocusedContainerColor = ext.background
        )
    )

    Spacer(Modifier.height(6.dp))
    Text(
        text = "Stored locally on device and sent directly to Gemini Developer API (5-minute timeout for large PDFs).",
        style = MaterialTheme.typography.bodySmall,
        color = ext.subtext
    )

    Spacer(Modifier.height(18.dp))

    Text(
        text = "Gemini Model (${GeminiModelOption.entries.size} Available)",
        style = MaterialTheme.typography.titleSmall,
        color = ext.title
    )
    Spacer(Modifier.height(8.dp))

    val models = GeminiModelOption.entries
    if (twoColumnModels) {
        val pairs = models.chunked(2)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            pairs.forEach { rowModels ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowModels.forEach { option ->
                        GeminiModelOptionCard(
                            option = option,
                            selected = uiState.selectedModel == option,
                            onClick = { onModelSelect(option) },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                    if (rowModels.size == 1) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            models.forEach { option ->
                GeminiModelOptionCard(
                    option = option,
                    selected = uiState.selectedModel == option,
                    onClick = { onModelSelect(option) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun GeminiModelOptionCard(
    option: GeminiModelOption,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalQuizzMeColors.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (selected) ext.accentSoft else ext.background,
        border = BorderStroke(1.dp, if (selected) ext.accent else ext.border),
        modifier = modifier.testTag("settings_model_${option.name.lowercase()}")
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = if (selected) Icons.Rounded.CheckCircle else Icons.Rounded.AutoAwesome,
                contentDescription = null,
                tint = if (selected) ext.accent else ext.subtext,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(18.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = option.displayName,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = if (selected) ext.accent else ext.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (selected) ext.accent else ext.card,
                        border = if (selected) null else BorderStroke(1.dp, ext.border)
                    ) {
                        Text(
                            text = option.tierLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) Color.White else ext.subtext,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = option.modelId,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = JetBrainsMonoFontFamily
                    ),
                    color = ext.subtext,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = option.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = ext.text
                )
            }
        }
    }
}

@Composable
private fun QuizSettingsContent(
    uiState: SettingsUiState,
    onQuestionCountChange: (Int) -> Unit,
    onQuestionStyleChange: (QuestionStyle) -> Unit,
    onCustomPromptChange: (String) -> Unit
) {
    val ext = LocalQuizzMeColors.current

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
                onClick = { onQuestionCountChange(count) },
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
                onClick = { onQuestionStyleChange(QuestionStyle.MCQ) },
                tag = "settings_style_mcq",
                modifier = Modifier.weight(1f)
            )
            SettingsOptionChip(
                label = "True / False",
                selected = uiState.questionStyle == QuestionStyle.TF,
                onClick = { onQuestionStyleChange(QuestionStyle.TF) },
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
                onClick = { onQuestionStyleChange(QuestionStyle.SA) },
                tag = "settings_style_sa",
                modifier = Modifier.weight(1f)
            )
            SettingsOptionChip(
                label = "Structured",
                selected = uiState.questionStyle == QuestionStyle.SEQ,
                onClick = { onQuestionStyleChange(QuestionStyle.SEQ) },
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
        onValueChange = onCustomPromptChange,
        placeholder = {
            Text("e.g., Focus heavily on clinical diagnostics...", color = ext.subtext)
        },
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 104.dp)
            .testTag("settings_custom_prompt_input"),
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = ext.accent,
            unfocusedBorderColor = ext.border,
            focusedContainerColor = ext.background,
            unfocusedContainerColor = ext.background,
            focusedTextColor = ext.title,
            unfocusedTextColor = ext.title
        )
    )
}

@Composable
private fun SaveSettingsButton(
    isSaving: Boolean,
    onSave: () -> Unit
) {
    val ext = LocalQuizzMeColors.current
    Button(
        onClick = onSave,
        enabled = !isSaving,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .testTag("settings_save_button"),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = ext.accent,
            contentColor = Color.White
        )
    ) {
        if (isSaving) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = Color.White
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.Save,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text("Save Configurations", style = MaterialTheme.typography.labelLarge)
        }
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
        color = if (selected) ext.accentSoft else ext.background,
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
