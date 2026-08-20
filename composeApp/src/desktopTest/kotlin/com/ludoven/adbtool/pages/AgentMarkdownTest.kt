package com.ludoven.adbtool.pages

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AgentMarkdownTest {
    private val palette = AgentMarkdownPalette(
        text = Color.Black,
        secondaryText = Color.Gray,
        link = Color.Blue,
        codeBackground = Color.LightGray
    )

    @Test
    fun `renders headings lists checkboxes and fenced code without markdown markers`() {
        val result = buildAgentMarkdownAnnotatedString(
            """
            # 状态概览
            - **设备**：小米
            - [x] 已连接

            ```text
            adb devices
            ```
            """.trimIndent(),
            palette
        )

        assertEquals(
            "状态概览\n• 设备：小米\n☑ 已连接\n\nadb devices",
            result.text
        )
        assertTrue(result.spanStyles.any { it.item.fontWeight == FontWeight.Bold })
        assertTrue(result.spanStyles.any { it.item.fontFamily == FontFamily.Monospace })
    }

    @Test
    fun `renders inline emphasis code strike and safe links`() {
        val result = buildAgentMarkdownAnnotatedString(
            "**粗体**、*斜体*、`代码`、~~删除~~、[文档](https://example.com)",
            palette
        )

        assertEquals("粗体、斜体、代码、删除、文档", result.text)
        assertTrue(result.spanStyles.any { it.item.fontWeight == FontWeight.Bold })
        assertTrue(result.spanStyles.any { it.item.fontStyle == FontStyle.Italic })
        assertTrue(result.spanStyles.any { it.item.fontFamily == FontFamily.Monospace })
        assertTrue(result.spanStyles.any { it.item.textDecoration == TextDecoration.LineThrough })
        assertTrue(
            result.getLinkAnnotations(0, result.length).any {
                (it.item as? LinkAnnotation.Url)?.url == "https://example.com"
            }
        )
    }

    @Test
    fun `keeps incomplete streaming markers and does not activate unsafe links`() {
        val incomplete = buildAgentMarkdownAnnotatedString("**正在生成", palette)
        val unsafe = buildAgentMarkdownAnnotatedString("[打开](javascript:alert(1))", palette)

        assertEquals("**正在生成", incomplete.text)
        assertTrue(unsafe.text.startsWith("打开 (javascript:alert(1)"))
        assertFalse(unsafe.hasLinkAnnotations(0, unsafe.length))
    }
}
