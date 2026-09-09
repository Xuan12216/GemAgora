package com.example.gemagora.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 區塊級 Markdown 元素結構
 */
sealed class MarkdownBlock {
    data class Heading(val level: Int, val text: String) : MarkdownBlock()
    data object HorizontalRule : MarkdownBlock()
    data class CodeBlock(val language: String, val code: String) : MarkdownBlock()
    data class Blockquote(val text: String) : MarkdownBlock()
    data class BulletItem(val depth: Int, val text: String) : MarkdownBlock()
    data class NumberedItem(val depth: Int, val number: String, val text: String) : MarkdownBlock()
    data class Paragraph(val text: String) : MarkdownBlock()
}

/**
 * Markdown 解析器核心物件，負責將字串解析為區塊與 AnnotatedString
 */
object MarkdownParser {

    private val headerRegex = Regex("""^(#{1,6})\s*(.*)$""")
    private val hrRegex = Regex("""^\s*([*\-_])\s*(\1\s*){2,}$""")
    private val bulletRegex = Regex("""^(\s*)([*+\-•])\s+(.*)$""")
    private val numberedRegex = Regex("""^(\s*)(\d+[\.\)])\s+(.*)$""")
    private val quoteRegex = Regex("""^>\s?(.*)$""")
    private val codeFenceRegex = Regex("""^```([a-zA-Z0-9_\-]*)\s*$""")

    /**
     * 解析整篇 Markdown 文本為區塊列表
     */
    fun parseBlocks(rawText: String): List<MarkdownBlock> {
        if (rawText.isBlank()) return emptyList()

        val normalized = rawText.replace("\r\n", "\n").replace("\r", "\n")
        val lines = normalized.lines()
        val blocks = mutableListOf<MarkdownBlock>()

        var inCodeBlock = false
        var codeBlockLang = ""
        val codeBlockLines = mutableListOf<String>()

        val currentParagraphLines = mutableListOf<String>()

        fun flushParagraph() {
            if (currentParagraphLines.isNotEmpty()) {
                val pText = currentParagraphLines.joinToString("\n").trim()
                if (pText.isNotBlank()) {
                    blocks.add(MarkdownBlock.Paragraph(pText))
                }
                currentParagraphLines.clear()
            }
        }

        for (line in lines) {
            val trimmed = line.trim()

            // 處理程式碼區塊
            if (inCodeBlock) {
                if (trimmed == "```") {
                    blocks.add(MarkdownBlock.CodeBlock(codeBlockLang, codeBlockLines.joinToString("\n")))
                    codeBlockLines.clear()
                    inCodeBlock = false
                } else {
                    codeBlockLines.add(line)
                }
                continue
            }

            val codeFenceMatch = codeFenceRegex.find(trimmed)
            if (codeFenceMatch != null) {
                flushParagraph()
                inCodeBlock = true
                codeBlockLang = codeFenceMatch.groupValues[1]
                codeBlockLines.clear()
                continue
            }

            // 空行：刷新段落
            if (trimmed.isEmpty()) {
                flushParagraph()
                continue
            }

            // 水平分隔線 (如 ---, ***, ___)
            if (hrRegex.matches(trimmed)) {
                flushParagraph()
                blocks.add(MarkdownBlock.HorizontalRule)
                continue
            }

            // 標題 (如 #, ##, ###, ...)
            val headerMatch = headerRegex.find(trimmed)
            if (headerMatch != null && headerMatch.groupValues[2].isNotBlank()) {
                flushParagraph()
                val level = headerMatch.groupValues[1].length
                val title = headerMatch.groupValues[2].trim()
                blocks.add(MarkdownBlock.Heading(level, title))
                continue
            }

            // 區塊引號 (如 > ...)
            val quoteMatch = quoteRegex.find(trimmed)
            if (quoteMatch != null) {
                flushParagraph()
                blocks.add(MarkdownBlock.Blockquote(quoteMatch.groupValues[1].trim()))
                continue
            }

            // 無序清單項目 (如 * item, - item)
            val bulletMatch = bulletRegex.find(line)
            if (bulletMatch != null) {
                flushParagraph()
                val indent = bulletMatch.groupValues[1].length / 2
                val itemText = bulletMatch.groupValues[3].trim()
                blocks.add(MarkdownBlock.BulletItem(indent, itemText))
                continue
            }

            // 有序清單項目 (如 1. item, 2. item)
            val numberedMatch = numberedRegex.find(line)
            if (numberedMatch != null) {
                flushParagraph()
                val indent = numberedMatch.groupValues[1].length / 2
                val num = numberedMatch.groupValues[2]
                val itemText = numberedMatch.groupValues[3].trim()
                blocks.add(MarkdownBlock.NumberedItem(indent, num, itemText))
                continue
            }

            // 一般文字行（併入段落）
            currentParagraphLines.add(line)
        }

        // 刷新剩餘內容
        if (inCodeBlock && codeBlockLines.isNotEmpty()) {
            blocks.add(MarkdownBlock.CodeBlock(codeBlockLang, codeBlockLines.joinToString("\n")))
        }
        flushParagraph()

        return blocks
    }

    private val inlineRegex = Regex(
        """(\*\*\*(.+?)\*\*\*)|""" +                    // 1: ***bold italic*** (g2)
        """(__(.+?)__)|""" +                            // 3: __bold__ (g4)
        """(\*\*(.+?)\*\*)|""" +                        // 5: **bold** (g6)
        """(\*([^\*\n]+?)\*)|""" +                      // 7: *italic* (g8)
        """(_([^_\n]+?)_)|""" +                         // 9: _italic_ (g10)
        """(~~(.+?)~~)|""" +                            // 11: ~~strikethrough~~ (g12)
        """(`([^`\n]+?)`)|""" +                         // 13: `inline code` (g14)
        """(\[([^\]\n]+)\]\(([^\)\n]+)\))"""           // 15: [link](url) (g16: text, g17: url)
    )

    /**
     * 將行內包含 Markdown 語法的字串轉換為 AnnotatedString
     */
    fun parseInline(
        text: String,
        primaryColor: Color = Color(0xFF8A5100),
        codeBgColor: Color = Color(0x1A000000),
        codeTextColor: Color = Color.Unspecified
    ): AnnotatedString {
        if (text.isEmpty()) return AnnotatedString("")

        return buildAnnotatedString {
            var currentIndex = 0
            val matches = inlineRegex.findAll(text)

            for (match in matches) {
                // 追加匹配項之前的普通文字
                if (match.range.first > currentIndex) {
                    append(text.substring(currentIndex, match.range.first))
                }

                when {
                    // ***粗斜體***
                    match.groups[1] != null -> {
                        val content = match.groupValues[2]
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)) {
                            append(content)
                        }
                    }
                    // __粗體__
                    match.groups[3] != null -> {
                        val content = match.groupValues[4]
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(content)
                        }
                    }
                    // **粗體**
                    match.groups[5] != null -> {
                        val content = match.groupValues[6]
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(content)
                        }
                    }
                    // *斜體*
                    match.groups[7] != null -> {
                        val content = match.groupValues[8]
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                            append(content)
                        }
                    }
                    // _斜體_
                    match.groups[9] != null -> {
                        val content = match.groupValues[10]
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                            append(content)
                        }
                    }
                    // ~~刪除線~~
                    match.groups[11] != null -> {
                        val content = match.groupValues[12]
                        withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                            append(content)
                        }
                    }
                    // `行內程式碼`
                    match.groups[13] != null -> {
                        val content = match.groupValues[14]
                        withStyle(
                            SpanStyle(
                                fontFamily = FontFamily.Monospace,
                                background = codeBgColor,
                                color = if (codeTextColor != Color.Unspecified) codeTextColor else primaryColor,
                                fontSize = 0.92.sp * 14 / 14 // proportional
                            )
                        ) {
                            append(" $content ")
                        }
                    }
                    // [連結文字](網址)
                    match.groups[15] != null -> {
                        val linkText = match.groupValues[16]
                        withStyle(
                            SpanStyle(
                                color = primaryColor,
                                textDecoration = TextDecoration.Underline,
                                fontWeight = FontWeight.Medium
                            )
                        ) {
                            append(linkText)
                        }
                    }
                }

                currentIndex = match.range.last + 1
            }

            // 處理末尾剩餘文字（含即時串流容錯）
            if (currentIndex < text.length) {
                val remaining = text.substring(currentIndex)
                // 容錯處理：若地端 AI 串流輸出末尾剛好有未閉合的 ** 標籤
                val unclosedBoldIdx = remaining.indexOf("**")
                if (unclosedBoldIdx != -1 && !remaining.substring(unclosedBoldIdx + 2).contains("**")) {
                    append(remaining.substring(0, unclosedBoldIdx))
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(remaining.substring(unclosedBoldIdx + 2))
                    }
                } else {
                    append(remaining)
                }
            }
        }
    }

    /**
     * 去除所有 Markdown 語法標記，提取純文字（適用於縮略卡片與摘要預覽）
     */
    fun stripMarkdown(text: String): String {
        if (text.isBlank()) return ""
        return text
            // 去除程式碼區塊
            .replace(Regex("```[\\s\\S]*?```"), "")
            .replace(Regex("`[^`\n]+`")) { it.value.trim('`') }
            // 去除水平線
            .replace(Regex("""^\s*[-*_]{3,}\s*$""", RegexOption.MULTILINE), "")
            // 去除標題
            .replace(Regex("""^\s*#{1,6}\s*""", RegexOption.MULTILINE), "")
            // 去除粗體、斜體、刪除線
            .replace(Regex("""\*\*\*(.*?)\*\*\*""", RegexOption.DOT_MATCHES_ALL), "$1")
            .replace(Regex("""\*\*(.*?)\*\*""", RegexOption.DOT_MATCHES_ALL), "$1")
            .replace(Regex("""\*(.*?)\*""", RegexOption.DOT_MATCHES_ALL), "$1")
            .replace(Regex("""__(.*?)__""", RegexOption.DOT_MATCHES_ALL), "$1")
            .replace(Regex("""_(.*?)_""", RegexOption.DOT_MATCHES_ALL), "$1")
            .replace(Regex("""~~(.*?)~~""", RegexOption.DOT_MATCHES_ALL), "$1")
            // 去除引言與列表符號
            .replace(Regex("""^\s*>\s*""", RegexOption.MULTILINE), "")
            .replace(Regex("""^\s*[*+\-•]\s+""", RegexOption.MULTILINE), "")
            .replace(Regex("""^\s*\d+[\.\)]\s+""", RegexOption.MULTILINE), "")
            // 去除超連結
            .replace(Regex("""\[([^\]]+)\]\([^\)]+\)"""), "$1")
            // 清理多餘連續空白與空行
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()
    }
}

/**
 * 完整的 Jetpack Compose Markdown 渲染組件
 */
@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = Color.Unspecified,
    lineHeight: TextUnit = TextUnit.Unspecified
) {
    val effectiveColor = if (color != Color.Unspecified) color else MaterialTheme.colorScheme.onSurface
    val effectiveLineHeight = if (lineHeight != TextUnit.Unspecified) lineHeight else style.lineHeight

    val blocks = remember(markdown) {
        MarkdownParser.parseBlocks(markdown)
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val codeBgColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.65f)
    val codeTextColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.Heading -> {
                    val (headingStyle, topSpace) = when (block.level) {
                        1 -> MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold) to 14.dp
                        2 -> MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold) to 12.dp
                        3 -> MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold) to 8.dp
                        else -> MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold) to 6.dp
                    }
                    val headingColor = when (block.level) {
                        1 -> MaterialTheme.colorScheme.primary
                        2 -> MaterialTheme.colorScheme.primary
                        3 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                    val inlineAnnotated = remember(block.text, headingColor) {
                        MarkdownParser.parseInline(
                            text = block.text,
                            primaryColor = headingColor,
                            codeBgColor = codeBgColor,
                            codeTextColor = codeTextColor
                        )
                    }

                    Column {
                        if (topSpace > 0.dp) {
                            Spacer(Modifier.height(topSpace))
                        }
                        Text(
                            text = inlineAnnotated,
                            style = headingStyle,
                            color = headingColor
                        )
                    }
                }

                is MarkdownBlock.HorizontalRule -> {
                    HorizontalDivider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                        thickness = 1.dp
                    )
                }

                is MarkdownBlock.BulletItem -> {
                    val indent = (block.depth * 14).dp
                    val inlineAnnotated = remember(block.text, effectiveColor) {
                        MarkdownParser.parseInline(
                            text = block.text,
                            primaryColor = primaryColor,
                            codeBgColor = codeBgColor,
                            codeTextColor = codeTextColor
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = indent),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "•",
                            style = style.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.padding(end = 8.dp, top = 1.dp)
                        )
                        Text(
                            text = inlineAnnotated,
                            style = style,
                            color = effectiveColor,
                            lineHeight = effectiveLineHeight,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                is MarkdownBlock.NumberedItem -> {
                    val indent = (block.depth * 14).dp
                    val inlineAnnotated = remember(block.text, effectiveColor) {
                        MarkdownParser.parseInline(
                            text = block.text,
                            primaryColor = primaryColor,
                            codeBgColor = codeBgColor,
                            codeTextColor = codeTextColor
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = indent),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = block.number,
                            style = style.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.padding(end = 6.dp)
                        )
                        Text(
                            text = inlineAnnotated,
                            style = style,
                            color = effectiveColor,
                            lineHeight = effectiveLineHeight,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                is MarkdownBlock.Blockquote -> {
                    val inlineAnnotated = remember(block.text, effectiveColor) {
                        MarkdownParser.parseInline(
                            text = block.text,
                            primaryColor = primaryColor,
                            codeBgColor = codeBgColor,
                            codeTextColor = codeTextColor
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.5.dp)
                                .height(IntrinsicSize.Max)
                                .background(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(2.dp)
                                )
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = inlineAnnotated,
                            style = style.copy(
                                fontStyle = FontStyle.Italic,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            lineHeight = effectiveLineHeight,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                is MarkdownBlock.CodeBlock -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.7f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            if (block.language.isNotBlank()) {
                                Text(
                                    text = block.language.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                            }
                            Text(
                                text = block.code,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    lineHeight = 18.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                is MarkdownBlock.Paragraph -> {
                    val inlineAnnotated = remember(block.text, effectiveColor) {
                        MarkdownParser.parseInline(
                            text = block.text,
                            primaryColor = primaryColor,
                            codeBgColor = codeBgColor,
                            codeTextColor = codeTextColor
                        )
                    }
                    Text(
                        text = inlineAnnotated,
                        style = style,
                        color = effectiveColor,
                        lineHeight = effectiveLineHeight
                    )
                }
            }
        }
    }
}
