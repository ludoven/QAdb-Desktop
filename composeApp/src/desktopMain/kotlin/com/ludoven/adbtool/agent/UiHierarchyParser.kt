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
        walk(document.documentElement, emptyList()) { element, ancestors ->
            if (snapshots.size >= MAX_UI_NODES) return@walk
            val bounds = parseBounds(element.getAttribute("bounds")) ?: return@walk
            if (!bounds.isVisibleWithin(screenWidth, screenHeight)) return@walk
            val enabled = element.booleanAttribute("enabled", default = true)
            val clickable = element.booleanAttribute("clickable")
            val editable = element.isEditableControl()
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
                password = password,
                resourceId = element.getAttribute("resource-id").cleanUiValue().take(MAX_RESOURCE_ID_CHARS),
                role = element.agentRole(clickable, editable),
                selected = element.booleanAttribute("selected"),
                checked = element.booleanAttribute("checked"),
                ancestorResourceIds = ancestors.mapNotNull { it.resourceId.takeIf(String::isNotBlank) }.takeLast(MAX_ANCESTOR_DEPTH),
                ancestorRoles = ancestors.mapNotNull { it.role.takeIf(String::isNotBlank) }.takeLast(MAX_ANCESTOR_DEPTH)
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
                if (node.resourceId.isNotBlank()) append(" id=").append(node.resourceId.escapeCompact())
                if (node.role.isNotBlank()) append(" role=").append(node.role)
                if (node.packageName.isNotBlank()) append(" package=").append(node.packageName)
                if (node.clickable) append(" clickable")
                if (node.editable) append(" editable")
                if (!node.enabled) append(" disabled")
                if (node.selected) append(" selected")
                if (node.checked) append(" checked")
                if (node.password) append(" password")
            }
        }.take(MAX_COMPACT_HIERARCHY_CHARS).ifBlank { "<no-actionable-nodes/>" }
        return ParsedUiHierarchy(snapshots, compact)
    }

    private fun walk(
        node: Node?,
        ancestors: List<UiAncestor>,
        visit: (Element, List<UiAncestor>) -> Unit
    ) {
        if (node == null) return
        val nextAncestors = if (node is Element && node.tagName == "node") {
            visit(node, ancestors)
            val clickable = node.booleanAttribute("clickable")
            val editable = node.isEditableControl()
            ancestors + UiAncestor(
                resourceId = node.getAttribute("resource-id").cleanUiValue().take(MAX_RESOURCE_ID_CHARS),
                role = node.agentRole(clickable, editable)
            )
        } else {
            ancestors
        }
        val children = node.childNodes
        for (index in 0 until children.length) walk(children.item(index), nextAncestors, visit)
    }
}

private data class UiAncestor(val resourceId: String, val role: String)

private val EDITABLE_CLASS_MARKERS = listOf("EditText", "AutoCompleteTextView", "TextInputEditText")

private fun Element.booleanAttribute(name: String, default: Boolean = false): Boolean =
    getAttribute(name).let {
        when {
            it.equals("true", ignoreCase = true) -> true
            it.equals("false", ignoreCase = true) -> false
            else -> default
        }
    }

private fun Element.isEditableControl(): Boolean {
    if (booleanAttribute("editable")) return true
    val className = getAttribute("class")
    return EDITABLE_CLASS_MARKERS.any { marker -> className.contains(marker, ignoreCase = true) }
}

private fun Element.agentRole(clickable: Boolean, editable: Boolean): String = when {
    editable -> "text_field"
    getAttribute("class").contains("Switch", true) -> "switch"
    getAttribute("class").contains("CheckBox", true) -> "checkbox"
    getAttribute("class").contains("SeekBar", true) -> "slider"
    getAttribute("class").contains("ImageButton", true) -> "icon_button"
    getAttribute("class").contains("Button", true) -> "button"
    clickable && getAttribute("class").contains("Recycler", true) -> "list_item"
    clickable -> "text_button"
    else -> "text"
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
private const val MAX_RESOURCE_ID_CHARS = 200
private const val MAX_PACKAGE_CHARS = 160
private const val MAX_ANCESTOR_DEPTH = 6
