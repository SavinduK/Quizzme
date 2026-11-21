package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalQuizzMeColors

data class SummaryCardBlock(
    val id: String,
    val title: String,
    val content: String
)

fun parseMarkdownToSummaryCards(markdown: String): List<SummaryCardBlock> {
    if (markdown.isBlank()) return emptyList()
    val sections = markdown.split(Regex("\\n(?=# |\\n)")).filter { it.isNotBlank() }
    val acc = mutableListOf<SummaryCardBlock>()

    for (sec in sections) {
        val trimmed = sec.trim()
        val lines = trimmed.lines()
        val firstLine = lines.firstOrNull().orEmpty()
        when {
            firstLine.startsWith("#") -> {
                val title = firstLine.replace(Regex("^#+\\s*"), "")
                val body = lines.drop(1).joinToString("\n").trim().ifBlank { title }
                acc.add(SummaryCardBlock("block-${acc.size}", title, body))
            }
            firstLine.startsWith(">") -> {
                acc.add(SummaryCardBlock("block-${acc.size}", "Key Note", trimmed))
            }
            else -> {
                if (acc.isNotEmpty()) {
                    val last = acc.removeAt(acc.lastIndex)
                    acc.add(last.copy(content = "${last.content}\n\n$trimmed"))
                } else {
                    acc.add(SummaryCardBlock("block-${acc.size}", "Overview", trimmed))
                }
            }
        }
    }
    return acc
}

@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier
) {
    val ext = LocalQuizzMeColors.current
    val lines = remember(markdown) { markdown.lines() }

    Column(modifier = modifier.fillMaxWidth()) {
        var idx = 0
        while (idx < lines.size) {
            val raw = lines[idx]
            val trimmed = raw.trim()

            when {
                trimmed.isEmpty() -> {
                    Spacer(Modifier.height(6.dp))
                }
                trimmed.startsWith("```") -> {
                    val codeLines = mutableListOf<String>()
                    idx++
                    while (idx < lines.size && !lines[idx].trim().startsWith("```")) {
                        codeLines.add(lines[idx])
                        idx++
                    }
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = ext.background
                    ) {
                        Text(
                            text = codeLines.joinToString("\n"),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = JetBrainsMonoFontFamily
                            ),
                            color = ext.text,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
                trimmed == "---" || trimmed == "***" -> {
                    HorizontalDivider(
                        color = ext.border,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                }
                trimmed.startsWith("### ") -> {
                    Text(
                        text = renderInlineMarkdown(trimmed.removePrefix("### "), ext.accent),
                        style = MaterialTheme.typography.titleMedium,
                        color = ext.title,
                        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                    )
                }
                trimmed.startsWith("## ") -> {
                    Text(
                        text = renderInlineMarkdown(trimmed.removePrefix("## "), ext.accent),
                        style = MaterialTheme.typography.titleLarge,
                        color = ext.title,
                        modifier = Modifier.padding(top = 14.dp, bottom = 6.dp)
                    )
                }
                trimmed.startsWith("# ") -> {
                    Text(
                        text = renderInlineMarkdown(trimmed.removePrefix("# "), ext.accent),
                        style = MaterialTheme.typography.headlineMedium,
                        color = ext.title,
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                    )
                }
                trimmed.startsWith("> ") -> {
                    val quoteText = trimmed.removePrefix("> ")
                    val accentColor = ext.accent
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .background(ext.accentSoft, RoundedCornerShape(8.dp))
                            .drawBehind {
                                drawLine(
                                    color = accentColor,
                                    start = Offset(0f, 0f),
                                    end = Offset(0f, size.height),
                                    strokeWidth = 4.dp.toPx()
                                )
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = renderInlineMarkdown(quoteText, ext.accent),
                            style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                            color = ext.text
                        )
                    }
                }
                trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                    val bulletContent = trimmed.substring(2)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 6.dp, top = 3.dp, bottom = 3.dp)
                    ) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            color = ext.accent,
                            modifier = Modifier.width(16.dp)
                        )
                        Text(
                            text = renderInlineMarkdown(bulletContent, ext.accent),
                            style = MaterialTheme.typography.bodyLarge,
                            color = ext.text,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Regex("^\\d+\\.\\s+.*").matches(trimmed) -> {
                    val dotIndex = trimmed.indexOf('.')
                    val prefix = trimmed.substring(0, dotIndex + 1)
                    val itemText = trimmed.substring(dotIndex + 1).trimStart()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 6.dp, top = 3.dp, bottom = 3.dp)
                    ) {
                        Text(
                            text = prefix,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            color = ext.accent,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = renderInlineMarkdown(itemText, ext.accent),
                            style = MaterialTheme.typography.bodyLarge,
                            color = ext.text,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                else -> {
                    Text(
                        text = renderInlineMarkdown(trimmed, ext.accent),
                        style = MaterialTheme.typography.bodyLarge,
                        color = ext.text,
                        modifier = Modifier.padding(vertical = 3.dp)
                    )
                }
            }
            idx++
        }
    }
}

private fun renderInlineMarkdown(text: String, accentColor: androidx.compose.ui.graphics.Color): AnnotatedString {
    return buildAnnotatedString {
        var i = 0
        while (i < text.length) {
            when {
                text.startsWith("**", i) -> {
                    val end = text.indexOf("**", i + 2)
                    if (end != -1) {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(text.substring(i + 2, end))
                        }
                        i = end + 2
                    } else {
                        append(text[i])
                        i++
                    }
                }
                text.startsWith("`", i) -> {
                    val end = text.indexOf("`", i + 1)
                    if (end != -1) {
                        withStyle(
                            SpanStyle(
                                fontFamily = JetBrainsMonoFontFamily,
                                color = accentColor,
                                fontWeight = FontWeight.Medium
                            )
                        ) {
                            append(text.substring(i + 1, end))
                        }
                        i = end + 1
                    } else {
                        append(text[i])
                        i++
                    }
                }
                text.startsWith("*", i) && !text.startsWith("**", i) -> {
                    val end = text.indexOf("*", i + 1)
                    if (end != -1 && end > i + 1) {
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                            append(text.substring(i + 1, end))
                        }
                        i = end + 1
                    } else {
                        append(text[i])
                        i++
                    }
                }
                else -> {
                    append(text[i])
                    i++
                }
            }
        }
    }
}
