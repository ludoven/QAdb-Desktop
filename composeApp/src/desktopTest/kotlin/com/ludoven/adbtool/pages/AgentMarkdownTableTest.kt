package com.ludoven.adbtool.pages

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AgentMarkdownTableTest {

    @Test
    fun `plain text stays a single paragraph block`() {
        val blocks = buildAgentMarkdownBlocks("# 标题\n\n第一段\n第二段")

        assertEquals(1, blocks.size)
        val paragraph = blocks.single() as AgentMarkdownBlock.Paragraph
        assertEquals("# 标题\n\n第一段\n第二段", paragraph.source)
    }

    @Test
    fun `gfm table is parsed with alignment and body rows`() {
        val blocks = buildAgentMarkdownBlocks(
            "对比结果：\n" +
                "| 项目 | 状态 | 备注 |\n" +
                "|:-----|-----:|:----:|\n" +
                "| 电池 | 正常 | -- |\n" +
                "| 内存 | 偏高 | 后台多 |"
        )

        assertEquals(2, blocks.size)
        val table = blocks[1] as AgentMarkdownBlock.Table
        assertEquals(listOf("项目", "状态", "备注"), table.header)
        assertEquals(2, table.rows.size)
        assertEquals(listOf("电池", "正常", "--"), table.rows[0])
        assertEquals(
            listOf(
                AgentMarkdownColumnAlign.LEFT,
                AgentMarkdownColumnAlign.RIGHT,
                AgentMarkdownColumnAlign.CENTER
            ),
            table.alignments
        )
    }

    @Test
    fun `pipes inside fenced code are not parsed as a table`() {
        val blocks = buildAgentMarkdownBlocks(
            "```bash\n" +
                "input tap 100 | 200\n" +
                "--- | ---\n" +
                "```\n" +
                "后续说明"
        )

        assertEquals(1, blocks.size)
        assertTrue(blocks.single() is AgentMarkdownBlock.Paragraph)
    }

    @Test
    fun `separator without pipes is not a table`() {
        val blocks = buildAgentMarkdownBlocks("标题行\n-----")

        assertTrue(blocks.single() is AgentMarkdownBlock.Paragraph)
    }

    @Test
    fun `ragged rows are normalized to the column count`() {
        val blocks = buildAgentMarkdownBlocks(
            "| A | B |\n" +
                "| --- | --- |\n" +
                "| 1 |\n" +
                "| 2 | 3 | 4 |"
        )

        val table = blocks.single() as AgentMarkdownBlock.Table
        assertEquals(listOf("1", ""), table.rows[0])
        assertEquals(listOf("2", "3"), table.rows[1])
    }
}
