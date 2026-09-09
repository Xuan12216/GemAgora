package com.example.gemagora.ui.components

import androidx.compose.ui.text.font.FontWeight
import org.junit.Assert.*
import org.junit.Test

class MarkdownTextTest {

    @Test
    fun testParseBlocksHeadersAndDividers() {
        val markdown = """
            好的，我將以我所學的蘇格拉底的追問之術，來審視這個「電車難題」。
            
            ---
            
            ## 蘇格拉底式的思辨推演：電車難題
            
            ### 一、效益主義 / 功利主義 (Utilitarianism) 的審視
            
            #### 四級子標題
        """.trimIndent()

        val blocks = MarkdownParser.parseBlocks(markdown)
        assertEquals(5, blocks.size)

        assertTrue(blocks[0] is MarkdownBlock.Paragraph)
        assertEquals("好的，我將以我所學的蘇格拉底的追問之術，來審視這個「電車難題」。", (blocks[0] as MarkdownBlock.Paragraph).text)

        assertTrue(blocks[1] is MarkdownBlock.HorizontalRule)

        assertTrue(blocks[2] is MarkdownBlock.Heading)
        val h2 = blocks[2] as MarkdownBlock.Heading
        assertEquals(2, h2.level)
        assertEquals("蘇格拉底式的思辨推演：電車難題", h2.text)

        assertTrue(blocks[3] is MarkdownBlock.Heading)
        val h3 = blocks[3] as MarkdownBlock.Heading
        assertEquals(3, h3.level)
        assertEquals("一、效益主義 / 功利主義 (Utilitarianism) 的審視", h3.text)

        assertTrue(blocks[4] is MarkdownBlock.Heading)
        val h4 = blocks[4] as MarkdownBlock.Heading
        assertEquals(4, h4.level)
        assertEquals("四級子標題", h4.text)
    }

    @Test
    fun testParseBlocksListsAndQuotes() {
        val markdown = """
            * **分析框架：** 在你給定的情境中
            * 第二點清單
              - 縮排無序清單
            1. 第一項步驟
            2. 第二項步驟
            > 這是一段哲學家的至理名言
        """.trimIndent()

        val blocks = MarkdownParser.parseBlocks(markdown)
        assertEquals(6, blocks.size)

        assertTrue(blocks[0] is MarkdownBlock.BulletItem)
        val b1 = blocks[0] as MarkdownBlock.BulletItem
        assertEquals(0, b1.depth)
        assertEquals("**分析框架：** 在你給定的情境中", b1.text)

        assertTrue(blocks[1] is MarkdownBlock.BulletItem)
        assertEquals(0, (blocks[1] as MarkdownBlock.BulletItem).depth)

        assertTrue(blocks[2] is MarkdownBlock.BulletItem)
        assertEquals(1, (blocks[2] as MarkdownBlock.BulletItem).depth)

        assertTrue(blocks[3] is MarkdownBlock.NumberedItem)
        val n1 = blocks[3] as MarkdownBlock.NumberedItem
        assertEquals("1.", n1.number)
        assertEquals("第一項步驟", n1.text)

        assertTrue(blocks[4] is MarkdownBlock.NumberedItem)
        assertEquals("2.", (blocks[4] as MarkdownBlock.NumberedItem).number)

        assertTrue(blocks[5] is MarkdownBlock.Blockquote)
        assertEquals("這是一段哲學家的至理名言", (blocks[5] as MarkdownBlock.Blockquote).text)
    }

    @Test
    fun testParseBlocksCodeBlock() {
        val markdown = """
            引導文字如下：
            ```kotlin
            val thinker = "Socrates"
            println(thinker)
            ```
            結語。
        """.trimIndent()

        val blocks = MarkdownParser.parseBlocks(markdown)
        assertEquals(3, blocks.size)
        assertTrue(blocks[0] is MarkdownBlock.Paragraph)
        assertTrue(blocks[1] is MarkdownBlock.CodeBlock)
        val cb = blocks[1] as MarkdownBlock.CodeBlock
        assertEquals("kotlin", cb.language)
        assertEquals("val thinker = \"Socrates\"\nprintln(thinker)", cb.code)
        assertTrue(blocks[2] is MarkdownBlock.Paragraph)
    }

    @Test
    fun testParseInlineFormatting() {
        val line = "**我將首先問你：** 如果我們將此情境視為一場純粹的數學運算，那麼哪種選擇會帶來「最少痛苦」呢？"
        val annotated = MarkdownParser.parseInline(line)

        assertFalse("Raw asterisks should be stripped from text", annotated.text.contains("**"))
        assertTrue("Text content should be intact", annotated.text.contains("我將首先問你："))
        assertTrue("Text content should be intact", annotated.text.contains("如果我們將此情境視為"))

        // Find bold span
        val boldSpans = annotated.spanStyles.filter { it.item.fontWeight == FontWeight.Bold }
        assertEquals(1, boldSpans.size)
        val boldSpan = boldSpans[0]
        assertEquals(0, boldSpan.start)
        assertEquals("我將首先問你：".length, boldSpan.end)
    }

    @Test
    fun testParseInlineMultipleBoldsAndInlineCode() {
        val line = "選項 A 與選項 B 的比較，是關於**數量化**的權衡，並呼應了`邊沁`的效益原則。"
        val annotated = MarkdownParser.parseInline(line)

        assertFalse(annotated.text.contains("**"))
        assertFalse(annotated.text.contains("`"))

        val boldSpans = annotated.spanStyles.filter { it.item.fontWeight == FontWeight.Bold }
        assertEquals(1, boldSpans.size)
        val boldTarget = annotated.text.substring(boldSpans[0].start, boldSpans[0].end)
        assertEquals("數量化", boldTarget)

        val codeSpans = annotated.spanStyles.filter { it.item.fontFamily != null }
        assertEquals(1, codeSpans.size)
        val codeTarget = annotated.text.substring(codeSpans[0].start, codeSpans[0].end).trim()
        assertEquals("邊沁", codeTarget)
    }

    @Test
    fun testParseInlineStreamingTolerance() {
        // Incomplete trailing bold tag during LLM stream
        val streamingLine = "目前正在進行推論，其核心在於**實踐智慧"
        val annotated = MarkdownParser.parseInline(streamingLine)

        // Must not crash, should gracefully render
        assertTrue(annotated.text.contains("實踐智慧"))
        assertFalse(annotated.text.contains("**"))
    }

    @Test
    fun testStripMarkdown() {
        val raw = """
            ## 蘇格拉底式的思辨推演：電車難題
            
            ---
            
            **我將首先問你：** 是否應當追求「最大效益」？
            * **分析框架：** 選項 A 與選項 B
            > 思考先於行動。
            `println()`
        """.trimIndent()

        val stripped = MarkdownParser.stripMarkdown(raw)
        assertFalse(stripped.contains("##"))
        assertFalse(stripped.contains("---"))
        assertFalse(stripped.contains("**"))
        assertFalse(stripped.contains("`"))
        assertFalse(stripped.contains(">"))
        assertTrue(stripped.contains("蘇格拉底式的思辨推演：電車難題"))
        assertTrue(stripped.contains("我將首先問你： 是否應當追求「最大效益」？"))
        assertTrue(stripped.contains("分析框架： 選項 A 與選項 B"))
        assertTrue(stripped.contains("思考先於行動。"))
    }
}
