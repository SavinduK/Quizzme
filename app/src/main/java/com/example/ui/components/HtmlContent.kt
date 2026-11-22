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
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.text.HtmlCompat
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalQuizzMeColors

data class SummaryCardBlock(
    val id: String,
    val title: String,
    val content: String
)

private sealed interface HtmlBlock {
    data class Heading(val level: Int, val innerHtml: String) : HtmlBlock
    data class Paragraph(val innerHtml: String) : HtmlBlock
    data class BulletList(val items: List<String>) : HtmlBlock
    data class OrderedList(val items: List<String>) : HtmlBlock
    data class Blockquote(val innerHtml: String) : HtmlBlock
    data class CodeBlock(val code: String) : HtmlBlock
    data class Table(val rows: List<List<TableCell>>) : HtmlBlock
    data object Divider : HtmlBlock
}

private data class TableCell(
    val isHeader: Boolean,
    val innerHtml: String
)

fun sanitizeHtmlFragment(raw: String): String {
    var s = raw.trim()
    if (s.startsWith("```")) {
        s = s.replace(Regex("^```(?:html|xml)?\\s*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s*```$"), "")
            .trim()
    }
    s = s.replace(Regex("<!DOCTYPE[^>]*>", RegexOption.IGNORE_CASE), "")
    s = s.replace(Regex("<!--([\\s\\S]*?)-->"), "")
    s = s.replace(Regex("<(script|style)\\b[^>]*>[\\s\\S]*?</\\1>", RegexOption.IGNORE_CASE), "")
    val bodyMatch = Regex("<body\\b[^>]*>([\\s\\S]*?)</body>", RegexOption.IGNORE_CASE).find(s)
    if (bodyMatch != null) {
        s = bodyMatch.groupValues[1]
    } else {
        s = s.replace(Regex("</?(?:html|head|body)\\b[^>]*>", RegexOption.IGNORE_CASE), "")
    }
    return s.trim()
}

fun htmlToPlainText(rawHtml: String): String {
    val clean = sanitizeHtmlFragment(rawHtml)
    if (clean.isBlank()) return ""
    return HtmlCompat.fromHtml(clean, HtmlCompat.FROM_HTML_MODE_COMPACT)
        .toString()
        .replace(Regex("\\n{3,}"), "\n\n")
        .trim()
}

fun htmlToEditablePlainText(rawHtml: String): String {
    var text = sanitizeHtmlFragment(rawHtml)
    if (text.isBlank()) return ""
    text = text
        .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
        .replace(Regex("</(?:p|div|section|blockquote|h[1-6]|pre)>", RegexOption.IGNORE_CASE), "\n\n")
        .replace(Regex("<li\\b[^>]*>", RegexOption.IGNORE_CASE), "• ")
        .replace(Regex("</li>", RegexOption.IGNORE_CASE), "\n")
        .replace(Regex("</t[hd]>", RegexOption.IGNORE_CASE), "  ")
        .replace(Regex("</tr>", RegexOption.IGNORE_CASE), "\n")
        .replace(Regex("<hr\\b[^>]*>", RegexOption.IGNORE_CASE), "\n---\n")
        .replace(Regex("<[^>]+>"), "")
    text = decodeEntities(text)
    return text
        .lines()
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

fun parseHtmlToSummaryCards(rawHtml: String): List<SummaryCardBlock> {
    val html = sanitizeHtmlFragment(rawHtml)
    if (html.isBlank()) return emptyList()

    // Split into logical cards by <h1> or <h2> sections; fall back to <h3> if no h1/h2 exist.
    val hasMajorHeadings = Regex("<h[12]\\b[^>]*>", RegexOption.IGNORE_CASE).containsMatchIn(html)
    val headingPattern = if (hasMajorHeadings) {
        Regex("<(h[12])\\b[^>]*>([\\s\\S]*?)</\\1>", RegexOption.IGNORE_CASE)
    } else {
        Regex("<(h3)\\b[^>]*>([\\s\\S]*?)</\\1>", RegexOption.IGNORE_CASE)
    }

    val matches = headingPattern.findAll(html).toList()
    if (matches.isEmpty()) {
        return listOf(SummaryCardBlock(id = "block-0", title = "Overview", content = html))
    }

    val cards = mutableListOf<SummaryCardBlock>()
    val leading = html.substring(0, matches.first().range.first).trim()
    if (leading.isNotBlank() && htmlToPlainText(leading).isNotBlank()) {
        cards.add(SummaryCardBlock(id = "block-0", title = "Overview", content = leading))
    }

    matches.forEachIndexed { index, match ->
        val title = htmlToPlainText(match.groupValues[2]).ifBlank { "Section ${index + 1}" }
        val bodyStart = match.range.last + 1
        val bodyEnd = if (index + 1 < matches.size) matches[index + 1].range.first else html.length
        val bodyHtml = html.substring(bodyStart, bodyEnd).trim()
        val effectiveBody = bodyHtml.ifBlank { "<p>${match.groupValues[2]}</p>" }
        cards.add(
            SummaryCardBlock(
                id = "block-${cards.size}",
                title = title,
                content = effectiveBody
            )
        )
    }
    return cards
}

@Composable
fun HtmlContent(
    html: String,
    modifier: Modifier = Modifier
) {
    val ext = LocalQuizzMeColors.current
    val blocks = remember(html) { parseHtmlToBlocks(html) }

    Column(modifier = modifier.fillMaxWidth()) {
        blocks.forEachIndexed { index, block ->
            when (block) {
                is HtmlBlock.Heading -> {
                    val style = when (block.level) {
                        1 -> MaterialTheme.typography.headlineMedium
                        2 -> MaterialTheme.typography.titleLarge
                        3 -> MaterialTheme.typography.titleMedium
                        else -> MaterialTheme.typography.titleSmall
                    }
                    val topPad = when (block.level) {
                        1 -> if (index == 0) 2.dp else 16.dp
                        2 -> if (index == 0) 2.dp else 14.dp
                        else -> if (index == 0) 2.dp else 10.dp
                    }
                    Text(
                        text = renderInlineHtml(block.innerHtml, ext.accent),
                        style = style,
                        color = ext.title,
                        modifier = Modifier.padding(top = topPad, bottom = 6.dp)
                    )
                }

                is HtmlBlock.Paragraph -> {
                    val rendered = renderInlineHtml(block.innerHtml, ext.accent)
                    if (rendered.text.isNotBlank()) {
                        Text(
                            text = rendered,
                            style = MaterialTheme.typography.bodyLarge,
                            color = ext.text,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }

                is HtmlBlock.BulletList -> {
                    Column(modifier = Modifier.padding(vertical = 3.dp)) {
                        block.items.forEach { itemHtml ->
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
                                    text = renderInlineHtml(itemHtml, ext.accent),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = ext.text,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                is HtmlBlock.OrderedList -> {
                    Column(modifier = Modifier.padding(vertical = 3.dp)) {
                        block.items.forEachIndexed { itemIndex, itemHtml ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 6.dp, top = 3.dp, bottom = 3.dp)
                            ) {
                                Text(
                                    text = "${itemIndex + 1}.",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                    color = ext.accent,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Text(
                                    text = renderInlineHtml(itemHtml, ext.accent),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = ext.text,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                is HtmlBlock.Blockquote -> {
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
                            text = renderInlineHtml(block.innerHtml, ext.accent),
                            style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                            color = ext.text
                        )
                    }
                }

                is HtmlBlock.CodeBlock -> {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = ext.card,
                        border = BorderStroke(1.dp, ext.border)
                    ) {
                        Text(
                            text = decodeEntities(block.code),
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

                is HtmlBlock.Table -> {
                    if (block.rows.isNotEmpty()) {
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
                                block.rows.forEachIndexed { rowIdx, row ->
                                    val isHeaderRow = row.any { it.isHeader }
                                    Row(
                                        modifier = Modifier
                                            .height(IntrinsicSize.Min)
                                            .background(if (isHeaderRow) ext.accentSoft else Color.Transparent)
                                    ) {
                                        row.forEachIndexed { colIdx, cell ->
                                            if (colIdx > 0) {
                                                VerticalDivider(
                                                    color = ext.border,
                                                    modifier = Modifier.fillMaxHeight()
                                                )
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .widthIn(min = 110.dp, max = 260.dp)
                                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                                            ) {
                                                Text(
                                                    text = renderInlineHtml(cell.innerHtml, ext.accent),
                                                    style = if (cell.isHeader) {
                                                        MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                                    } else {
                                                        MaterialTheme.typography.bodyMedium
                                                    },
                                                    color = if (cell.isHeader) ext.title else ext.text
                                                )
                                            }
                                        }
                                    }
                                    if (rowIdx < block.rows.lastIndex) {
                                        HorizontalDivider(color = ext.border)
                                    }
                                }
                            }
                        }
                    }
                }

                HtmlBlock.Divider -> {
                    HorizontalDivider(
                        color = ext.border,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                }
            }
        }
    }
}

private val BLOCK_TAG_REGEX = Regex(
    """<(h[1-6]|p|ul|ol|blockquote|pre|table|hr|div|section)\b([^>]*)>""",
    RegexOption.IGNORE_CASE
)

private fun parseHtmlToBlocks(raw: String): List<HtmlBlock> {
    val html = sanitizeHtmlFragment(raw)
    if (html.isBlank()) return emptyList()

    val blocks = mutableListOf<HtmlBlock>()
    var cursor = 0

    while (cursor < html.length) {
        val nextMatch = BLOCK_TAG_REGEX.find(html, cursor)
        if (nextMatch == null) {
            appendLooseTextAsParagraphs(html.substring(cursor), blocks)
            break
        }

        if (nextMatch.range.first > cursor) {
            appendLooseTextAsParagraphs(html.substring(cursor, nextMatch.range.first), blocks)
        }

        val tagName = nextMatch.groupValues[1].lowercase()
        if (tagName == "hr") {
            blocks.add(HtmlBlock.Divider)
            cursor = nextMatch.range.last + 1
            continue
        }

        val contentStart = nextMatch.range.last + 1
        val closeTagRange = findMatchingCloseTag(html, tagName, contentStart)
        val innerHtml = if (closeTagRange != null) {
            html.substring(contentStart, closeTagRange.first)
        } else {
            // Unclosed tag while streaming: consume remainder cleanly
            html.substring(contentStart)
        }
        val nextCursor = if (closeTagRange != null) closeTagRange.last + 1 else html.length

        when {
            tagName.startsWith("h") && tagName.length == 2 -> {
                val level = tagName[1].digitToIntOrNull()?.coerceIn(1, 6) ?: 2
                blocks.add(HtmlBlock.Heading(level, innerHtml))
            }
            tagName == "p" -> {
                blocks.add(HtmlBlock.Paragraph(innerHtml))
            }
            tagName == "ul" -> {
                val items = extractListItems(innerHtml)
                if (items.isNotEmpty()) blocks.add(HtmlBlock.BulletList(items))
            }
            tagName == "ol" -> {
                val items = extractListItems(innerHtml)
                if (items.isNotEmpty()) blocks.add(HtmlBlock.OrderedList(items))
            }
            tagName == "blockquote" -> {
                val cleaned = innerHtml.replace(Regex("</?p\\b[^>]*>", RegexOption.IGNORE_CASE), " ").trim()
                if (cleaned.isNotBlank()) blocks.add(HtmlBlock.Blockquote(cleaned))
            }
            tagName == "pre" -> {
                val stripped = innerHtml.replace(Regex("</?code\\b[^>]*>", RegexOption.IGNORE_CASE), "")
                blocks.add(HtmlBlock.CodeBlock(stripped.trim()))
            }
            tagName == "table" -> {
                val rows = extractTableRows(innerHtml)
                if (rows.isNotEmpty()) blocks.add(HtmlBlock.Table(rows))
            }
            tagName == "div" || tagName == "section" -> {
                blocks.addAll(parseHtmlToBlocks(innerHtml))
            }
        }

        cursor = nextCursor
    }

    return blocks
}

private fun appendLooseTextAsParagraphs(fragment: String, out: MutableList<HtmlBlock>) {
    val trimmed = fragment.trim()
    if (trimmed.isEmpty()) return
    // Split on blank lines into paragraphs while preserving single line breaks
    trimmed.split(Regex("(?:\\r?\\n){2,}")).forEach { chunk ->
        val clean = chunk.trim()
        if (clean.isNotEmpty()) {
            out.add(HtmlBlock.Paragraph(clean.replace(Regex("\\r?\\n"), "<br/>")))
        }
    }
}

private fun findMatchingCloseTag(html: String, tagName: String, searchFrom: Int): IntRange? {
    val pattern = Regex("<(/?)$tagName\\b[^>]*>", RegexOption.IGNORE_CASE)
    var depth = 1
    var pos = searchFrom
    while (pos < html.length) {
        val m = pattern.find(html, pos) ?: return null
        if (m.groupValues[1] == "/") {
            depth--
            if (depth == 0) return m.range
        } else if (!m.value.endsWith("/>")) {
            depth++
        }
        pos = m.range.last + 1
    }
    return null
}

private fun extractListItems(listInnerHtml: String): List<String> {
    val items = mutableListOf<String>()
    val liOpenRegex = Regex("<li\\b[^>]*>", RegexOption.IGNORE_CASE)
    var pos = 0
    while (pos < listInnerHtml.length) {
        val openMatch = liOpenRegex.find(listInnerHtml, pos) ?: break
        val contentStart = openMatch.range.last + 1
        val closeRange = findMatchingCloseTag(listInnerHtml, "li", contentStart)
        val rawItem = if (closeRange != null) {
            listInnerHtml.substring(contentStart, closeRange.first)
        } else {
            listInnerHtml.substring(contentStart)
        }
        val normalized = rawItem
            .replace(Regex("</?p\\b[^>]*>", RegexOption.IGNORE_CASE), " ")
            .trim()
        if (normalized.isNotBlank()) items.add(normalized)
        pos = if (closeRange != null) closeRange.last + 1 else listInnerHtml.length
    }
    return items
}

private fun extractTableRows(tableInnerHtml: String): List<List<TableCell>> {
    val rows = mutableListOf<List<TableCell>>()
    val trRegex = Regex("<tr\\b[^>]*>([\\s\\S]*?)</tr>", RegexOption.IGNORE_CASE)
    val cellRegex = Regex("<(th|td)\\b[^>]*>([\\s\\S]*?)</\\1>", RegexOption.IGNORE_CASE)
    for (trMatch in trRegex.findAll(tableInnerHtml)) {
        val rowCells = cellRegex.findAll(trMatch.groupValues[1]).map { cellMatch ->
            TableCell(
                isHeader = cellMatch.groupValues[1].equals("th", ignoreCase = true),
                innerHtml = cellMatch.groupValues[2].trim()
            )
        }.toList()
        if (rowCells.isNotEmpty()) {
            rows.add(rowCells)
        }
    }
    return rows
}

private fun renderInlineHtml(inlineHtml: String, accentColor: Color): AnnotatedString {
    val normalized = inlineHtml
        .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
        .replace(Regex("\\s+"), " ")
        .replace(Regex(" ?\\n ?"), "\n")
        .trim()

    return buildAnnotatedString {
        val tagRegex = Regex("<(/?)([a-zA-Z0-9]+)(?:\\s+[^>]*)?>")
        val openTags = mutableListOf<String>()
        var cursor = 0

        fun activeSpanStyle(): SpanStyle {
            var bold = false
            var italic = false
            var underline = false
            var strike = false
            var code = false
            var mark = false
            var link = false
            var sup = false
            var sub = false
            for (tag in openTags) {
                when (tag) {
                    "strong", "b" -> bold = true
                    "em", "i" -> italic = true
                    "u", "ins" -> underline = true
                    "s", "del", "strike" -> strike = true
                    "code", "kbd", "samp" -> code = true
                    "mark" -> mark = true
                    "a" -> link = true
                    "sup" -> sup = true
                    "sub" -> sub = true
                }
            }
            val decorations = buildList {
                if (underline || link) add(TextDecoration.Underline)
                if (strike) add(TextDecoration.LineThrough)
            }
            return SpanStyle(
                fontWeight = if (bold) FontWeight.Bold else null,
                fontStyle = if (italic) FontStyle.Italic else null,
                textDecoration = if (decorations.isNotEmpty()) TextDecoration.combine(decorations) else null,
                fontFamily = if (code) JetBrainsMonoFontFamily else null,
                color = when {
                    code || link -> accentColor
                    else -> Color.Unspecified
                },
                background = when {
                    mark -> accentColor.copy(alpha = 0.22f)
                    code -> accentColor.copy(alpha = 0.12f)
                    else -> Color.Unspecified
                },
                fontSize = if (sup || sub) 11.sp else androidx.compose.ui.unit.TextUnit.Unspecified,
                baselineShift = when {
                    sup -> BaselineShift.Superscript
                    sub -> BaselineShift.Subscript
                    else -> null
                }
            )
        }

        while (cursor < normalized.length) {
            val match = tagRegex.find(normalized, cursor)
            if (match == null) {
                val text = decodeEntities(normalized.substring(cursor))
                if (text.isNotEmpty()) {
                    withStyle(activeSpanStyle()) { append(text) }
                }
                break
            }

            if (match.range.first > cursor) {
                val text = decodeEntities(normalized.substring(cursor, match.range.first))
                if (text.isNotEmpty()) {
                    withStyle(activeSpanStyle()) { append(text) }
                }
            }

            val isClosing = match.groupValues[1] == "/"
            val tagName = match.groupValues[2].lowercase()
            if (!match.value.endsWith("/>")) {
                if (isClosing) {
                    val idx = openTags.lastIndexOf(tagName)
                    if (idx >= 0) openTags.removeAt(idx)
                } else {
                    openTags.add(tagName)
                }
            }
            cursor = match.range.last + 1
        }
    }
}

private fun decodeEntities(text: String): String {
    if (!text.contains('&')) return text
    return HtmlCompat.fromHtml(text, HtmlCompat.FROM_HTML_MODE_LEGACY).toString()
}
