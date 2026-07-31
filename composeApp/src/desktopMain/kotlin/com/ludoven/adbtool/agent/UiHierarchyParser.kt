package com.ludoven.adbtool.agent

import java.io.StringReader
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.xml.sax.InputSource

data class ParsedUiHierarchy(
    val nodes: List<UiNodeSnapshot>,
    val compactText: String
)

class UiHierarchyParser {
    fun parse(xml: String, screenWidth: Int, screenHeight: Int): ParsedUiHierarchy {
        if (xml.isBlank() || !xml.trimStart().startsWith("<")) {
            return ParsedUiHierarchy(emptyList(), "<unavailable/>")
        }
        val document = runCatching {
            val factory = DocumentBuilderFactory.newInstance().apply {
                isNamespaceAware = false
                isXIncludeAware = false
                setExpandEntityReferences(false)
                runCatching { setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
                runCatching { setFeature("http://xml.org/sax/features/external-general-entities", false) }
                runCatching { setFeature("http://xml.org/sax/features/external-parameter-entities", false) }
                runCatching { setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "") }
                runCatching { setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "") }
            }
            factory.newDocumentBuilder().parse(InputSource(StringReader(xml)))
        }.getOrNull() ?: return ParsedUiHierarchy(emptyList(), "<unavailable/>")

        val snapshots = mutableListOf<UiNodeSnapshot>()
        walk(document.documentElement) { element ->
            if (snapshots.size >= MAX_UI_NODES) return@walk
            val bounds = parseBounds(element.getAttribute("bounds")) ?: return@walk
            if (!bounds.isVisibleWithin(screenWidth, screenHeight)) return@walk
            val enabled = element.booleanAttribute("enabled", default = true)
            val clickable = element.booleanAttribute("clickable")
            val editable = element.getAttribute("class").contains("EditText", ignoreCase = true) ||
                element.booleanAttribute("focusable") && element.booleanAttribute("focused")
            val password = element.booleanAttribute("password") ||
                element.getAttribute("class").contains("Password", ignoreCase = true)
            val rawText = element.getAttribute("text").cleanUiValue()
            val rawDescription = element.getAttribute("content-desc").cleanUiValue()
            if (!clickable && !editable && rawText.isBlank() && rawDescription.isBlank()) return@walk
            snapshots += UiNodeSnapshot(
                elementId = "e${snapshots.size + 1}",
                text = if (password) "[REDACTED]" else rawText.take(MAX_UI_VALUE_CHARS),
                contentDescription = if (password) "[REDACTED]" else rawDescription.take(MAX_UI_VALUE_CHARS),
                className = element.getAttribute("class").substringAfterLast('.').take(MAX_CLASS_CHARS),
                packageName = element.getAttribute("package").take(MAX_PACKAGE_CHARS),
                bounds = bounds,
                clickable = clickable,
                editable = editable,
                enabled = enabled,
                password = password
            )
        }
        val compact = snapshots.joinToString("\n") { node ->
            buildString {
                append(node.elementId)
                append(" [").append(node.bounds.left).append(',').append(node.bounds.top)
                append("][").append(node.bounds.right).append(',').append(node.bounds.bottom).append(']')
                if (node.text.isNotBlank()) append(" text=\"").append(node.text.escapeCompact()).append('"')
                if (node.contentDescription.isNotBlank()) {
                    append(" desc=\"").append(node.contentDescription.escapeCompact()).append('"')
                }
                if (node.className.isNotBlank()) append(" class=").append(node.className)
                if (node.packageName.isNotBlank()) append(" package=").append(node.packageName)
                if (node.clickable) append(" clickable")
                if (node.editable) append(" editable")
                if (!node.enabled) append(" disabled")
                if (node.password) append(" password")
            }
        }.take(MAX_COMPACT_HIERARCHY_CHARS).ifBlank { "<no-actionable-nodes/>" }
        return ParsedUiHierarchy(snapshots, compact)
    }

    private fun walk(node: Node?, visit: (Element) -> Unit) {
        if (node == null) return
        if (node is Element && node.tagName == "node") visit(node)
        val children = node.childNodes
        for (index in 0 until children.length) walk(children.item(index), visit)
    }
}

private fun Element.booleanAttribute(name: String, default: Boolean = false): Boolean =
    getAttribute(name).let {
        when {
            it.equals("true", ignoreCase = true) -> true
            it.equals("false", ignoreCase = true) -> false
            else -> default
        }
    }

private fun parseBounds(value: String): UiBounds? {
    val match = BOUNDS_PATTERN.matchEntire(value.trim()) ?: return null
    val values = match.groupValues.drop(1).mapNotNull(String::toIntOrNull)
    if (values.size != 4) return null
    return UiBounds(values[0], values[1], values[2], values[3])
}

private fun UiBounds.isVisibleWithin(width: Int, height: Int): Boolean =
    right > left && bottom > top && right > 0 && bottom > 0 && left < width && top < height

private fun String.cleanUiValue(): String =
    replace(Regex("[\\r\\n\\t]+"), " ").replace(Regex("\\s+"), " ").trim()

private fun String.escapeCompact(): String = replace("\\", "\\\\").replace("\"", "\\\"")

private val BOUNDS_PATTERN = Regex("\\[(-?\\d+),(-?\\d+)]\\[(-?\\d+),(-?\\d+)]")
private const val MAX_UI_NODES = 200
private const val MAX_COMPACT_HIERARCHY_CHARS = 8_000
private const val MAX_UI_VALUE_CHARS = 160
private const val MAX_CLASS_CHARS = 80
private const val MAX_PACKAGE_CHARS = 160
