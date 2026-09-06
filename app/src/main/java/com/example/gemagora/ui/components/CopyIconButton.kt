package com.example.gemagora.ui.components

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * 清理文字中的思考鏈標籤 (如 <|channel|>thought 或 <think>)，提取乾淨的對話文字供複製。
 */
fun cleanTextForCopy(rawText: String): String {
    // 1. 處理 <|channel|>thought / <|channel|>thinking 等標籤區塊
    val channelRegex = Regex("<\\|?channel\\|?>([a-zA-Z0-9_]*)")
    val matches = channelRegex.findAll(rawText).toList()
    if (matches.isNotEmpty()) {
        for (i in matches.indices) {
            val match = matches[i]
            val channelName = match.groupValues[1]
            val startIdx = match.range.last + 1
            val endIdx = if (i + 1 < matches.size) matches[i + 1].range.first else rawText.length
            val sectionText = rawText.substring(startIdx, endIdx).trim()
            if (channelName != "thought" && channelName != "thinking" && sectionText.isNotBlank()) {
                return sectionText
            }
        }
    }

    // 2. 處理標準 <think>...</think> 標籤
    var text = rawText.replace(Regex("<think>.*?</think>", RegexOption.DOT_MATCHES_ALL), "")
        .replace(Regex("<think>.*", RegexOption.DOT_MATCHES_ALL), "")
        .trim()

    return text.ifBlank { rawText }
}

@Composable
fun CopyIconButton(
    textToCopy: String,
    modifier: Modifier = Modifier,
    iconSize: Dp = 16.dp,
    buttonSize: Dp = 28.dp,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    cleanReasoningTags: Boolean = true,
    contentDescription: String = "複製文字"
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var justCopied by remember { mutableStateOf(false) }

    LaunchedEffect(justCopied) {
        if (justCopied) {
            delay(1500)
            justCopied = false
        }
    }

    val animatedTint by animateColorAsState(
        targetValue = if (justCopied) MaterialTheme.colorScheme.primary else tint,
        animationSpec = tween(durationMillis = 200),
        label = "copyTint"
    )

    IconButton(
        onClick = {
            val finalContent = if (cleanReasoningTags) cleanTextForCopy(textToCopy) else textToCopy
            if (finalContent.isNotBlank()) {
                clipboardManager.setText(AnnotatedString(finalContent))
                Toast.makeText(context, "已複製到剪貼簿", Toast.LENGTH_SHORT).show()
                justCopied = true
            }
        },
        enabled = textToCopy.isNotBlank(),
        modifier = modifier.size(buttonSize)
    ) {
        Icon(
            imageVector = if (justCopied) Icons.Default.Check else Icons.Default.ContentCopy,
            contentDescription = if (justCopied) "已複製" else contentDescription,
            tint = animatedTint,
            modifier = Modifier.size(iconSize)
        )
    }
}
