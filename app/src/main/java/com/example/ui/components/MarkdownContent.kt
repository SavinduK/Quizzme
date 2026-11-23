package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.core.text.HtmlCompat
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalQuizzMeColors

data class SummaryCardBlock(
    val id: String,
    val title: String,
    val content: String
)

private sealed interface MarkdownBlock {
    data class Heading(val level: Int, val text: String) : MarkdownBlock
    data class Paragraph(val text: String) : MarkdownBlock
    data class BulletList(val items: List<String>) : MarkdownBlock
    data class OrderedList(val items: List<Pair<Int, String>>) : MarkdownBlock
    data class Blockquote(val text: String) : MarkdownBlock
    data class CodeBlock(val code: String) : MarkdownBlock
    data class Table(val headers: List<String>, val rows: List<List<String>>) : MarkdownBlock
    data object Divider : MarkdownBlock
}

fun stripOuterMarkdownFence(raw: String): String {
    var s = raw.trim()
    if (s.startsWith("```")) {
        val firstNewline = s.indexOf('\n')
        if (firstNewline > 0) {
            val header = s.substring(3, firstNewline).trim().lowercase()
            if (header == "markdown" || header == "md" || header == "text") {
                s = s.substring(firstNewline + 1)
                if (s.trimEnd().endsWith("```")) {
                    s = s.trimEnd().removeSuffix("```")
                }
            }
        }
    }
    return s.trim()
}

private fun looksLikeLegacyHtml(raw: String): Boolean {
    return Regex("<(?:h[1-6]|p|ul|ol|li|table|tr|th|td|blockquote|strong|em|div|section|br)\\b[^>]*>", RegexOption.IGNORE_CASE)
        .containsMatchIn(raw)
}

fun legacyHtmlToMarkdown(rawHtml: String): String {
    var text = rawHtml
        .replace(Regex("<!DOCTYPE[^>]*>", RegexOption.IGNORE_CASE), "")
        .replace(Regex("<!--([\\s\\S]*?)-->"), "")
        .replace(Regex("<(script|style)\\b[^>]*>[\\s\\S]*?</\\1>", RegexOption.IGNORE_CASE), "")
        .replace(Regex("</?(?:html|head|body|div|section|thead|tbody)\\b[^>]*>", RegexOption.IGNORE_CASE), "\n")
        .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
        .replace(Regex("<hr\\b[^>]*>", RegexOption.IGNORE_CASE), "\n---\n")
        .replace(Regex("<h1\\b[^>]*>([\\s\\S]*?)</h1>", RegexOption.IGNORE_CASE)) { "\n# ${it.groupValues[1].trim()}\n\n" }
        .replace(Regex("<h2\\b[^>]*>([\\s\\S]*?)</h2>", RegexOption.IGNORE_CASE)) { "\n## ${it.groupValues[1].trim()}\n\n" }
        .replace(Regex("<h3\\b[^>]*>([\\s\\S]*?)</h3>", RegexOption.IGNORE_CASE)) { "\n### ${it.groupValues[1].trim()}\n\n" }
        .replace(Regex("<h[4-6]\\b[^>]*>([\\s\\S]*?)</h[4-6]>", RegexOption.IGNORE_CASE)) { "\n#### ${it.groupValues[1].trim()}\n\n" }
        .replace(Regex("<blockquote\\b[^>]*>([\\s\\S]*?)</blockquote>", RegexOption.IGNORE_CASE)) {
            val inner = it.groupValues[1].replace(Regex("</?p\\b[^>]*>", RegexOption.IGNORE_CASE), " ").trim()
            "\n> $inner\n\n"
        }
        .replace(Regex("<pre\\b[^>]*>([\\s\\S]*?)</pre>", RegexOption.IGNORE_CASE)) {
            val inner = it.groupValues[1].replace(Regex("</?code\\b[^>]*>", RegexOption.IGNORE_CASE), "").trim()
            "\n```\n$inner\n```\n\n"
        }
        .replace(Regex("<li\\b[^>]*>", RegexOption.IGNORE_CASE), "\n- ")
        .replace(Regex("</li>", RegexOption.IGNORE_CASE), "")
        .replace(Regex("</?(?:ul|ol)\\b[^>]*>", RegexOption.IGNORE_CASE), "\n")
        .replace(Regex("<(?:strong|b)\\b[^>]*>([\\s\\S]*?)</(?:strong|b)>", RegexOption.IGNORE_CASE)) { "**${it.groupValues[1].trim()}**" }
        .replace(Regex("<(?:em|i)\\b[^>]*>([\\s\\S]*?)</(?:em|i)>", RegexOption.IGNORE_CASE)) { "*${it.groupValues[1].trim()}*" }
        .replace(Regex("<code\\b[^>]*>([\\s\\S]*?)</code>", RegexOption.IGNORE_CASE)) { "`${it.groupValues[1].trim()}`" }
        .replace(Regex("<p\\b[^>]*>", RegexOption.IGNORE_CASE), "\n")
        .replace(Regex("</p>", RegexOption.IGNORE_CASE), "\n\n")
        .replace(Regex("</t[hd]>", RegexOption.IGNORE_CASE), " | ")
        .replace(Regex("</tr>", RegexOption.IGNORE_CASE), "\n")
        .replace(Regex("<[^>]+>"), "")

    text = decodeEntities(text)
    return text.lines()
        .joinToString("\n") { it.trimEnd() }
        .replace(Regex("\\n{3,}"), "\n\n")
        .trim()
}

fun stripHtmlAndCssTags(rawText: String): String {
    var cleaned = rawText
        .replace(Regex("<!--([\\s\\S]*?)-->"), "")
        .replace(Regex("<(script|style)\\b[^>]*>[\\s\\S]*?</\\1>", RegexOption.IGNORE_CASE), "")
        .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
        .replace(Regex("</(?:p|div|section|blockquote|h[1-6]|li|tr)>", RegexOption.IGNORE_CASE), "\n")
        .replace(Regex("<[^>]+>"), "")
    cleaned = decodeEntities(cleaned)
    return cleaned
        .lines()
        .joinToString("\n") { it.trimEnd() }
        .replace(Regex("\\n{3,}"), "\n\n")
        .trim()
}

fun markdownToEditablePlainText(raw: String): String {
    val clean = stripOuterMarkdownFence(raw)
    if (clean.isBlank()) return ""
    return if (looksLikeLegacyHtml(clean)) {
        legacyHtmlToMarkdown(clean)
    } else {
        clean
    }
}

fun markdownToPlainText(raw: String): String {
    val normalized = markdownToEditablePlainText(raw)
    if (normalized.isBlank()) return ""
    return normalized
        .replace(Regex("^#{1,6}\\s+", RegexOption.MULTILINE), "")
        .replace(Regex("\\*\\*([^*]+)\\*\\*"), "$1")
        .replace(Regex("__([^_]+)__"), "$1")
        .replace(Regex("\\*([^*]+)\\*"), "$1")
        .replace(Regex("~~([^~]+)~~"), "$1")
        .replace(Regex("`([^`]+)`"), "$1")
        .replace(Regex("\\[([^]]+)]\\([^)]+\\)"), "$1")
        .replace(Regex("^>\\s?", RegexOption.MULTILINE), "")
        .replace(Regex("\\n{3,}"), "\n\n")
        .trim()
}

fun parseMarkdownToSummaryCards(raw: String): List<SummaryCardBlock> {
    val md = markdownToEditablePlainText(raw)
    if (md.isBlank()) return emptyList()

    val lines = md.lines()
    val hasMajorHeadings = lines.any { Regex("^#{1,2}\\s+(.+)$").matches(it.trim()) }
    val headingRegex = if (hasMajorHeadings) {
        Regex("^#{1,2}\\s+(.+)$")
    } else {
        Regex("^#{1,3}\\s+(.+)$")
    }

    val cards = mutableListOf<SummaryCardBlock>()
    var currentTitle: String? = null
    val currentLines = mutableListOf<String>()

    fun flushSection() {
        val content = currentLines.joinToString("\n").trim()
        if (currentTitle != null || content.isNotBlank()) {
            cards.add(
                SummaryCardBlock(
                    id = "block-${cards.size}",
                    title = currentTitle?.let { cleanHeadingTitle(it) } ?: "Overview",
                    content = content.ifBlank { currentTitle.orEmpty() }
                )
            )
        }
        currentLines.clear()
    }

    var inFence = false
    for (line in lines) {
        val trimmed = line.trim()
        if (trimmed.startsWith("```")) {
            inFence = !inFence
            currentLines.add(line)
            continue
        }
        val headingMatch = if (!inFence) headingRegex.matchEntire(trimmed) else null
        if (headingMatch != null) {
            flushSection()
            currentTitle = headingMatch.groupValues[1].trim()
        } else {
            currentLines.add(line)
        }
    }
    flushSection()

    return if (cards.isEmpty()) {
        listOf(SummaryCardBlock(id = "block-0", title = "Overview", content = md))
    } else {
        cards
    }
}

private fun cleanHeadingTitle(rawTitle: String): String {
    return rawTitle
        .replace(Regex("\\*\\*([^*]+)\\*\\*"), "$1")
        .replace(Regex("__([^_]+)__"), "$1")
        .replace(Regex("`([^`]+)`"), "$1")
        .trim()
}

@Composable
fun MarkdownContent(
    markdown: String,
    modifier: Modifier = Modifier
) {
    val ext = LocalQuizzMeColors.current
    val normalized = remember(markdown) { markdownToEditablePlainText(markdown) }
    val blocks = remember(normalized) { parseMarkdownToBlocks(normalized) }

    Column(modifier = modifier.fillMaxWidth()) {
        blocks.forEachIndexed { index, block ->
            when (block) {
                is MarkdownBlock.Heading -> {
                    val style = when (block.level) {
                        1 -> MaterialTheme.typography.headlineMedium
                        2 -> MaterialTheme.typography.titleLarge
                        3 -> MaterialTheme.typography.titleMedium
                        else -> MaterialTheme.typography.titleSmall
                    }
                    val topPad = when (block.level) {
                        1 -> if (index == 0) 2.dp else 18.dp
                        2 -> if (index == 0) 2.dp else 14.dp
                        else -> if (index == 0) 2.dp else 10.dp
                    }
                    Text(
                        text = renderInlineMarkdown(block.text, ext.accent),
                        style = style,
                        color = ext.title,
                        modifier = Modifier.padding(top = topPad, bottom = 6.dp)
                    )
                }

                is MarkdownBlock.Paragraph -> {
                    val rendered = renderInlineMarkdown(block.text, ext.accent)
                    if (rendered.text.isNotBlank()) {
                        Text(
                            text = rendered,
                            style = MaterialTheme.typography.bodyLarge,
                            color = ext.text,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }

                is MarkdownBlock.BulletList -> {
                    Column(modifier = Modifier.padding(vertical = 3.dp)) {
                        block.items.forEach { itemText ->
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
                                    text = renderInlineMarkdown(itemText, ext.accent),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = ext.text,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                is MarkdownBlock.OrderedList -> {
                    Column(modifier = Modifier.padding(vertical = 3.dp)) {
                        block.items.forEach { (num, itemText) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 6.dp, top = 3.dp, bottom = 3.dp)
                            ) {
                                Text(
                                    text = "$num.",
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
                    }
                }

                is MarkdownBlock.Blockquote -> {
                    val accentColor = ext.accent
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
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
                            text = renderInlineMarkdown(block.text, ext.accent),
                            style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                            color = ext.text
                        )
                    }
                }

                is MarkdownBlock.CodeBlock -> {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = ext.card,
                        border = BorderStroke(1.dp, ext.border)
                    ) {
                        Text(
                            text = block.code,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = JetBrainsMonoFontFamily
                            ),
                            color = ext.text,
                            modifier = Modifier
                                .horizontalScroll(rememberScrollState())
                                .padding(12.dp)
                        )
                    }
                }

                is MarkdownBlock.Table -> {
                    val allRows = buildList {
                        if (block.headers.isNotEmpty()) add(true to block.headers)
                        block.rows.forEach { add(false to it) }
                    }
                    if (allRows.isNotEmpty()) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = ext.card,
                            border = BorderStroke(1.dp, ext.border)
                        ) {
                            Column(
                                modifier = Modifier.horizontalScroll(rememberScrollState())
                            ) {
                                allRows.forEachIndexed { rowIdx, (isHeader, rowCells) ->
                                    Row(
                                        modifier = Modifier
                                            .height(IntrinsicSize.Min)
                                            .background(if (isHeader) ext.accentSoft else Color.Transparent)
                                    ) {
                                        rowCells.forEachIndexed { colIdx, cellText ->
                                            if (colIdx > 0) {
                                                VerticalDivider(
                                                    color = ext.border,
                                                    modifier = Modifier.fillMaxHeight()
                                                )
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .widthIn(min = 120.dp, max = 280.dp)
                                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                                            ) {
                                                Text(
                                                    text = renderInlineMarkdown(cellText, ext.accent),
                                                    style = if (isHeader) {
                                                        MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                                    } else {
                                                        MaterialTheme.typography.bodyMedium
                                                    },
                                                    color = if (isHeader) ext.title else ext.text
                                                )
                                            }
                                        }
                                    }
                                    if (rowIdx < allRows.lastIndex) {
                                        HorizontalDivider(color = ext.border)
                                    }
                                }
                            }
                        }
                    }
                }

                MarkdownBlock.Divider -> {
                    HorizontalDivider(
                        color = ext.border,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                }
            }
        }
    }
}

private val HEADING_REGEX = Regex("^(#{1,6})\\s+(.*)$")
private val BULLET_REGEX = Regex("^\\s*[-*•+]\\s+(.*)$")
private val ORDERED_REGEX = Regex("^\\s*(\\d+)[.)]\\s+(.*)$")
private val HR_REGEX = Regex("^\\s*(?:[-*_]){3,}\\s*$")
private val TABLE_SEP_REGEX = Regex("^\\|?\\s*:?-+:?\\s*(\\|\\s*:?-+:?\\s*)+\\|?$")

private fun parseMarkdownToBlocks(markdown: String): List<MarkdownBlock> {
    if (markdown.isBlank()) return emptyList()
    val lines = markdown.lines()
    val blocks = mutableListOf<MarkdownBlock>()
    var i = 0

    while (i < lines.size) {
        val rawLine = lines[i]
        val trimmed = rawLine.trim()

        if (trimmed.isEmpty()) {
            i++
            continue
        }

        // Fenced code block
        if (trimmed.startsWith("```")) {
            val codeLines = mutableListOf<String>()
            i++
            while (i < lines.size && !lines[i].trim().startsWith("```")) {
                codeLines.add(lines[i])
                i++
            }
            if (i < lines.size) i++
            blocks.add(MarkdownBlock.CodeBlock(codeLines.joinToString("\n")))
            continue
        }

        // Horizontal rule
        if (HR_REGEX.matches(trimmed)) {
            blocks.add(MarkdownBlock.Divider)
            i++
            continue
        }

        // Heading
        val headingMatch = HEADING_REGEX.matchEntire(trimmed)
        if (headingMatch != null) {
            val level = headingMatch.groupValues[1].length.coerceIn(1, 6)
            val text = headingMatch.groupValues[2].trim().removeSuffix("#").trim()
            blocks.add(MarkdownBlock.Heading(level, text))
            i++
            continue
        }

        // Markdown Table (header | separator | body)
        if (trimmed.contains('|') && i + 1 < lines.size && TABLE_SEP_REGEX.matches(lines[i + 1].trim())) {
            val headers = splitTableRow(trimmed)
            i += 2
            val dataRows = mutableListOf<List<String>>()
            while (i < lines.size) {
                val rowTrimmed = lines[i].trim()
                if (rowTrimmed.isEmpty() || !rowTrimmed.contains('|')) break
                dataRows.add(splitTableRow(rowTrimmed))
                i++
            }
            blocks.add(MarkdownBlock.Table(headers, dataRows))
            continue
        }

        // Blockquote
        if (trimmed.startsWith(">")) {
            val quoteLines = mutableListOf<String>()
            while (i < lines.size && lines[i].trim().startsWith(">")) {
                quoteLines.add(lines[i].trim().removePrefix(">").trim())
                i++
            }
            blocks.add(MarkdownBlock.Blockquote(quoteLines.joinToString("\n")))
            continue
        }

        // Bullet list
        val firstBullet = BULLET_REGEX.matchEntire(rawLine)
        if (firstBullet != null) {
            val items = mutableListOf<String>()
            while (i < lines.size) {
                val m = BULLET_REGEX.matchEntire(lines[i]) ?: break
                items.add(m.groupValues[1].trim())
                i++
            }
            blocks.add(MarkdownBlock.BulletList(items))
            continue
        }

        // Ordered list
        val firstOrdered = ORDERED_REGEX.matchEntire(rawLine)
        if (firstOrdered != null) {
            val items = mutableListOf<Pair<Int, String>>()
            while (i < lines.size) {
                val m = ORDERED_REGEX.matchEntire(lines[i]) ?: break
                val num = m.groupValues[1].toIntOrNull() ?: (items.size + 1)
                items.add(num to m.groupValues[2].trim())
                i++
            }
            blocks.add(MarkdownBlock.OrderedList(items))
            continue
        }

        // Paragraph (consumes consecutive non-special lines)
        val paraLines = mutableListOf<String>()
        while (i < lines.size) {
            val cur = lines[i]
            val curTrim = cur.trim()
            if (curTrim.isEmpty() ||
                curTrim.startsWith("```") ||
                curTrim.startsWith(">") ||
                HR_REGEX.matches(curTrim) ||
                HEADING_REGEX.matches(curTrim) ||
                BULLET_REGEX.matches(cur) ||
                ORDERED_REGEX.matches(cur) ||
                (curTrim.contains('|') && i + 1 < lines.size && TABLE_SEP_REGEX.matches(lines[i + 1].trim()))
            ) {
                break
            }
            paraLines.add(curTrim)
            i++
        }
        if (paraLines.isNotEmpty()) {
            blocks.add(MarkdownBlock.Paragraph(paraLines.joinToString("\n")))
        }
    }
    return blocks
}

private fun splitTableRow(line: String): List<String> {
    return line.trim()
        .removePrefix("|")
        .removeSuffix("|")
        .split('|')
        .map { it.trim() }
}

private val INLINE_TOKEN_REGEX = Regex(
    """(\*\*[^*]+?\*\*|__[^_]+?__|`[^`]+?`|~~[^~]+?~~|\[[^\]]+?]\([^)]+?\)|\*[^*\n]+?\*|_[^_\n]+?_)"""
)

private fun renderInlineMarkdown(text: String, accentColor: Color): AnnotatedString {
    return buildAnnotatedString {
        var cursor = 0
        for (match in INLINE_TOKEN_REGEX.findAll(text)) {
            if (match.range.first > cursor) {
                append(text.substring(cursor, match.range.first))
            }
            val token = match.value
            when {
                token.startsWith("**") && token.endsWith("**") && token.length > 4 -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(renderInlineMarkdown(token.substring(2, token.length - 2), accentColor))
                    }
                }
                token.startsWith("__") && token.endsWith("__") && token.length > 4 -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(renderInlineMarkdown(token.substring(2, token.length - 2), accentColor))
                    }
                }
                token.startsWith("`") && token.endsWith("`") && token.length > 2 -> {
                    withStyle(
                        SpanStyle(
                            fontFamily = JetBrainsMonoFontFamily,
                            color = accentColor,
                            background = accentColor.copy(alpha = 0.12f)
                        )
                    ) {
                        append(token.substring(1, token.length - 1))
                    }
                }
                token.startsWith("~~") && token.endsWith("~~") && token.length > 4 -> {
                    withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                        append(token.substring(2, token.length - 2))
                    }
                }
                token.startsWith("[") && token.contains("](") && token.endsWith(")") -> {
                    val splitIdx = token.indexOf("](")
                    val label = token.substring(1, splitIdx)
                    withStyle(SpanStyle(color = accentColor, textDecoration = TextDecoration.Underline)) {
                        append(label)
                    }
                }
                (token.startsWith("*") && token.endsWith("*") && token.length > 2) ||
                    (token.startsWith("_") && token.endsWith("_") && token.length > 2) -> {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(token.substring(1, token.length - 1))
                    }
                }
                else -> append(token)
            }
            cursor = match.range.last + 1
        }
        if (cursor < text.length) {
            append(text.substring(cursor))
        }
    }
}

private fun decodeEntities(text: String): String {
    if (!text.contains('&')) return text
    return HtmlCompat.fromHtml(text, HtmlCompat.FROM_HTML_MODE_LEGACY).toString()
}
