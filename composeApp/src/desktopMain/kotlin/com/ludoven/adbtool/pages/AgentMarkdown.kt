package com.ludoven.adbtool.pages

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
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
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
    val annotatedText = remember(text, palette) {
        buildAgentMarkdownAnnotatedString(text, palette)
    }

    SelectionContainer {
        Text(
            text = annotatedText,
            modifier = modifier,
            color = palette.text,
            fontSize = UiTokens.TextBodyLarge
        )
    }
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
