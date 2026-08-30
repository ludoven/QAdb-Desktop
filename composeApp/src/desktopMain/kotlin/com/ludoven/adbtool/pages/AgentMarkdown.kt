package com.ludoven.adbtool.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ludoven.adbtool.QadbTokens
import com.ludoven.adbtool.UiTokens
import com.ludoven.adbtool.ui.mac.Text

internal data class AgentMarkdownPalette(
    val text: Color,
    val secondaryText: Color,
    val link: Color,
    val codeBackground: Color
)

internal enum class AgentMarkdownColumnAlign { LEFT, CENTER, RIGHT }

internal sealed interface AgentMarkdownBlock {
    data class Paragraph(val source: String) : AgentMarkdownBlock
    data class Table(
        val header: List<String>,
        val rows: List<List<String>>,
        val alignments: List<AgentMarkdownColumnAlign>
    ) : AgentMarkdownBlock
}

@Composable
internal fun AgentMarkdownText(
    text: String,
    modifier: Modifier = Modifier
) {
    val palette = AgentMarkdownPalette(
        text = QadbTokens.textPrimary,
        secondaryText = QadbTokens.textSecondary,
        link = QadbTokens.brand,
        codeBackground = QadbTokens.bg3
    )
    val blocks = remember(text) { buildAgentMarkdownBlocks(text) }

    SelectionContainer {
        Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            blocks.forEach { block ->
                when (block) {
                    is AgentMarkdownBlock.Paragraph -> {
                        val annotated = remember(block.source, palette) {
                            buildAgentMarkdownAnnotatedString(block.source, palette)
                        }
                        Text(
                            text = annotated,
                            color = palette.text,
                            fontSize = UiTokens.TextBodyLarge
                        )
                    }
                    is AgentMarkdownBlock.Table -> AgentMarkdownTable(block, palette)
                }
            }
        }
    }
}

/**
 * Splits the source into paragraph and table blocks. Fenced code blocks are always kept
 * inside paragraphs, even when their content contains `|`, so code is never parsed as a table.
 */
internal fun buildAgentMarkdownBlocks(source: String): List<AgentMarkdownBlock> {
    val normalized = source.replace("\r\n", "\n").replace('\r', '\n')
    val lines = normalized.split('\n')
    val blocks = mutableListOf<AgentMarkdownBlock>()
    val paragraph = mutableListOf<String>()
    var activeFence: String? = null

    fun flushParagraph() {
        if (paragraph.isNotEmpty()) {
            blocks += AgentMarkdownBlock.Paragraph(paragraph.joinToString("\n"))
            paragraph.clear()
        }
    }

    var index = 0
    while (index < lines.size) {
        val raw = lines[index]
        val trimmedStart = raw.trimStart()
        val fence = when {
            trimmedStart.startsWith("```") -> "```"
            trimmedStart.startsWith("~~~") -> "~~~"
            else -> null
        }
        if (activeFence != null) {
            paragraph += raw
            if (fence == activeFence) activeFence = null
        } else if (fence != null) {
            activeFence = fence
            paragraph += raw
        } else {
            val table = parseAgentMarkdownTable(lines, index)
            if (table != null) {
                flushParagraph()
                blocks += table.first
                index = table.second - 1
            } else {
                paragraph += raw
            }
        }
        index++
    }
    flushParagraph()
    return blocks
}

private fun parseAgentMarkdownTable(
    lines: List<String>,
    headerIndex: Int
): Pair<AgentMarkdownBlock.Table, Int>? {
    val headerLine = lines[headerIndex]
    if (headerIndex + 1 >= lines.size || '|' !in headerLine) return null
    val separatorLine = lines[headerIndex + 1]
    val separatorCells = splitTableRow(separatorLine)
    if (separatorCells.isEmpty() || separatorCells.any { !isTableSeparatorCell(it) }) return null
    if (headerLine.trimStart().startsWith("```") || headerLine.trimStart().startsWith("~~~")) return null

    val alignments = separatorCells.map { cell ->
        val trimmed = cell.trim()
        when {
            trimmed.startsWith(":") && trimmed.endsWith(":") -> AgentMarkdownColumnAlign.CENTER
            trimmed.endsWith(":") -> AgentMarkdownColumnAlign.RIGHT
            else -> AgentMarkdownColumnAlign.LEFT
        }
    }
    val header = normalizeRow(splitTableRow(headerLine), alignments.size)

    val rows = mutableListOf<List<String>>()
    var index = headerIndex + 2
    while (index < lines.size) {
        val line = lines[index]
        val trimmedStart = line.trimStart()
        if (trimmedStart.startsWith("```") || trimmedStart.startsWith("~~~")) break
        if ('|' !in line) break
        rows += normalizeRow(splitTableRow(line), alignments.size)
        index++
    }
    return AgentMarkdownBlock.Table(header, rows, alignments) to index
}

private fun isTableSeparatorCell(cell: String): Boolean =
    cell.matches(Regex("^\\s*:?-{3,}:?\\s*$"))

/** Splits a `| a | b |` line into trimmed cells, tolerating a missing outer pipe. */
private fun splitTableRow(line: String): List<String> {
    val trimmed = line.trim()
    if ('|' !in trimmed) return emptyList()
    val withoutOuterPipes = trimmed.removePrefix("|").removeSuffix("|")
    return withoutOuterPipes.split('|').map { it.trim() }
}

private fun normalizeRow(cells: List<String>, columnCount: Int): List<String> {
    val safeCount = columnCount.coerceAtLeast(1)
    val padded = cells.take(safeCount).toMutableList()
    while (padded.size < safeCount) padded += ""
    return padded
}

@Composable
private fun AgentMarkdownTable(
    table: AgentMarkdownBlock.Table,
    palette: AgentMarkdownPalette
) {
    val weights = remember(table) { tableColumnWeights(table) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.codeBackground.copy(alpha = 0.35f))
            .padding(vertical = 2.dp)
    ) {
        Row(Modifier.fillMaxWidth()) {
            table.header.forEachIndexed { column, cell ->
                AgentMarkdownTableCell(
                    cell = cell,
                    palette = palette,
                    align = table.alignments[column],
                    modifier = Modifier.weight(weights[column]),
                    bold = true
                )
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .height(1.dp)
                .background(palette.codeBackground)
        )
        table.rows.forEachIndexed { rowIndex, row ->
            Row(Modifier.fillMaxWidth()) {
                row.forEachIndexed { column, cell ->
                    AgentMarkdownTableCell(
                        cell = cell,
                        palette = palette,
                        align = table.alignments[column],
                        modifier = Modifier.weight(weights[column]),
                        bold = false
                    )
                }
            }
            if (rowIndex != table.rows.lastIndex) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .height(1.dp)
                        .background(palette.codeBackground)
                )
            }
        }
    }
}

@Composable
private fun AgentMarkdownTableCell(
    cell: String,
    palette: AgentMarkdownPalette,
    align: AgentMarkdownColumnAlign,
    modifier: Modifier = Modifier,
    bold: Boolean
) {
    val annotated = remember(cell, palette) { buildAgentMarkdownAnnotatedString(cell, palette) }
    Text(
        text = annotated,
        modifier = modifier.padding(horizontal = 8.dp, vertical = 6.dp),
        color = palette.text,
        fontSize = 12.5.sp,
        textAlign = when (align) {
            AgentMarkdownColumnAlign.LEFT -> TextAlign.Left
            AgentMarkdownColumnAlign.CENTER -> TextAlign.Center
            AgentMarkdownColumnAlign.RIGHT -> TextAlign.Right
        },
        fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal
    )
}

/** Column weights follow the widest cell (CJK counted double), clamped to keep narrow columns usable. */
private fun tableColumnWeights(table: AgentMarkdownBlock.Table): List<Float> {
    fun displayWidth(value: String): Int = value.sumOf { char ->
        if (char.code > 0x2E80) 2 else 1
    }
    val widths = List(table.alignments.size) { column ->
        val headerWidth = displayWidth(table.header[column])
        val bodyWidth = table.rows.maxOfOrNull { displayWidth(it[column]) } ?: 0
        maxOf(headerWidth, bodyWidth, 1)
    }
    return widths.map { width -> width.coerceIn(1, 24).toFloat() }
}

internal fun buildAgentMarkdownAnnotatedString(
    source: String,
    palette: AgentMarkdownPalette
): AnnotatedString = buildAnnotatedString {
    val normalized = source.replace("\r\n", "\n").replace('\r', '\n')
    var activeFence: String? = null
    var hasOutputLine = false

    fun beginOutputLine() {
        if (hasOutputLine) append('\n')
        hasOutputLine = true
    }

    normalized.split('\n').forEach { rawLine ->
        val trimmedStart = rawLine.trimStart()
        val fence = when {
            trimmedStart.startsWith("```") -> "```"
            trimmedStart.startsWith("~~~") -> "~~~"
            else -> null
        }

        if (activeFence != null) {
            if (fence == activeFence) {
                activeFence = null
            } else {
                beginOutputLine()
                withStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        background = palette.codeBackground
                    )
                ) {
                    append(rawLine)
                }
            }
            return@forEach
        }

        if (fence != null) {
            activeFence = fence
            return@forEach
        }

        beginOutputLine()
        appendAgentMarkdownLine(rawLine, palette)
    }
}

private fun AnnotatedString.Builder.appendAgentMarkdownLine(
    rawLine: String,
    palette: AgentMarkdownPalette
) {
    val leadingWhitespace = rawLine.takeWhile(Char::isWhitespace)
    val line = rawLine.drop(leadingWhitespace.length)
    val heading = HEADING_PATTERN.matchEntire(line)
    val checklist = CHECKLIST_PATTERN.matchEntire(line)
    val unorderedList = UNORDERED_LIST_PATTERN.matchEntire(line)
    val orderedList = ORDERED_LIST_PATTERN.matchEntire(line)
    val quote = BLOCKQUOTE_PATTERN.matchEntire(line)

    when {
        line.matches(HORIZONTAL_RULE_PATTERN) -> {
            withStyle(SpanStyle(color = palette.secondaryText)) {
                append("────────────────")
            }
        }

        heading != null -> {
            val level = heading.groupValues[1].length
            withStyle(
                SpanStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = HEADING_SIZES[level - 1].sp
                )
            ) {
                appendInlineMarkdown(heading.groupValues[2], palette)
            }
        }

        checklist != null -> {
            append(leadingWhitespace)
            append(if (checklist.groupValues[1].equals("x", ignoreCase = true)) "☑ " else "☐ ")
            appendInlineMarkdown(checklist.groupValues[2], palette)
        }

        unorderedList != null -> {
            append(leadingWhitespace)
            append("• ")
            appendInlineMarkdown(unorderedList.groupValues[1], palette)
        }

        orderedList != null -> {
            append(leadingWhitespace)
            append(orderedList.groupValues[1])
            append(". ")
            appendInlineMarkdown(orderedList.groupValues[2], palette)
        }

        quote != null -> {
            append(leadingWhitespace)
            withStyle(SpanStyle(color = palette.secondaryText, fontStyle = FontStyle.Italic)) {
                append("│ ")
                appendInlineMarkdown(quote.groupValues[1], palette)
            }
        }

        else -> {
            append(leadingWhitespace)
            appendInlineMarkdown(line, palette)
        }
    }
}

private fun AnnotatedString.Builder.appendInlineMarkdown(
    text: String,
    palette: AgentMarkdownPalette,
    depth: Int = 0
) {
    if (depth >= MAX_INLINE_DEPTH) {
        append(text)
        return
    }

    var index = 0
    while (index < text.length) {
        when {
            text[index] == '\\' && index + 1 < text.length && text[index + 1] in MARKDOWN_ESCAPABLE -> {
                append(text[index + 1])
                index += 2
            }

            text[index] == '`' -> {
                val markerLength = text.countMarkerLength(index, '`')
                val marker = "`".repeat(markerLength)
                val closing = text.indexOf(marker, index + markerLength)
                if (closing >= 0) {
                    withStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = palette.codeBackground
                        )
                    ) {
                        append(text.substring(index + markerLength, closing))
                    }
                    index = closing + markerLength
                } else {
                    append(marker)
                    index += markerLength
                }
            }

            text.startsWith("**", index) || text.startsWith("__", index) -> {
                val marker = text.substring(index, index + 2)
                val closing = text.indexOf(marker, index + 2)
                if (closing > index + 2) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        appendInlineMarkdown(text.substring(index + 2, closing), palette, depth + 1)
                    }
                    index = closing + 2
                } else {
                    append(marker)
                    index += 2
                }
            }

            text.startsWith("~~", index) -> {
                val closing = text.indexOf("~~", index + 2)
                if (closing > index + 2) {
                    withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                        appendInlineMarkdown(text.substring(index + 2, closing), palette, depth + 1)
                    }
                    index = closing + 2
                } else {
                    append("~~")
                    index += 2
                }
            }

            text[index] == '[' -> {
                val labelEnd = text.indexOf("](", index + 1)
                val urlEnd = if (labelEnd >= 0) text.indexOf(')', labelEnd + 2) else -1
                if (labelEnd > index + 1 && urlEnd > labelEnd + 2) {
                    val label = text.substring(index + 1, labelEnd)
                    val url = text.substring(labelEnd + 2, urlEnd).trim()
                    val start = length
                    appendInlineMarkdown(label, palette, depth + 1)
                    val end = length
                    if (url.isSafeAgentMarkdownUrl()) {
                        addLink(
                            LinkAnnotation.Url(
                                url = url,
                                styles = TextLinkStyles(
                                    style = SpanStyle(
                                        color = palette.link,
                                        textDecoration = TextDecoration.Underline
                                    )
                                )
                            ),
                            start = start,
                            end = end
                        )
                    } else {
                        append(" ($url)")
                    }
                    index = urlEnd + 1
                } else {
                    append('[')
                    index++
                }
            }

            text[index] == '<' -> {
                val end = text.indexOf('>', index + 1)
                val url = if (end > index + 1) text.substring(index + 1, end).trim() else ""
                if (url.isSafeAgentMarkdownUrl()) {
                    val start = length
                    append(url)
                    addLink(
                        LinkAnnotation.Url(
                            url = url,
                            styles = TextLinkStyles(
                                style = SpanStyle(
                                    color = palette.link,
                                    textDecoration = TextDecoration.Underline
                                )
                            )
                        ),
                        start = start,
                        end = length
                    )
                    index = end + 1
                } else {
                    append('<')
                    index++
                }
            }

            text[index] == '*' || text[index] == '_' -> {
                val marker = text[index]
                val closing = text.indexOf(marker, index + 1)
                val isWordUnderscore = marker == '_' &&
                    index > 0 &&
                    text[index - 1].isLetterOrDigit()
                if (!isWordUnderscore && closing > index + 1) {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        appendInlineMarkdown(text.substring(index + 1, closing), palette, depth + 1)
                    }
                    index = closing + 1
                } else {
                    append(marker)
                    index++
                }
            }

            else -> {
                append(text[index])
                index++
            }
        }
    }
}

private fun String.countMarkerLength(start: Int, marker: Char): Int {
    var end = start
    while (end < length && this[end] == marker) end++
    return end - start
}

private fun String.isSafeAgentMarkdownUrl(): Boolean {
    val scheme = substringBefore(':', missingDelimiterValue = "").lowercase()
    return scheme == "http" || scheme == "https" || scheme == "mailto"
}

private val HEADING_PATTERN = Regex("^(#{1,6})\\s+(.+?)\\s*#*\\s*$")
private val CHECKLIST_PATTERN = Regex("^[-*+]\\s+\\[([ xX])]\\s+(.+)$")
private val UNORDERED_LIST_PATTERN = Regex("^[-*+]\\s+(.+)$")
private val ORDERED_LIST_PATTERN = Regex("^(\\d+)[.)]\\s+(.+)$")
private val BLOCKQUOTE_PATTERN = Regex("^>\\s?(.*)$")
private val HORIZONTAL_RULE_PATTERN = Regex("^((\\*\\s*){3,}|(-\\s*){3,}|(_\\s*){3,})$")
private val HEADING_SIZES = listOf(22, 20, 18, 16, 15, 14)
private val MARKDOWN_ESCAPABLE = setOf('\\', '`', '*', '_', '{', '}', '[', ']', '<', '>', '(', ')', '#', '+', '-', '.', '!', '|')
private const val MAX_INLINE_DEPTH = 8
