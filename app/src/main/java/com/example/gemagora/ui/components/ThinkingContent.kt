package com.example.gemagora.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

import androidx.compose.foundation.text.selection.SelectionContainer

@Composable
fun ThinkingContent(
    rawText: String,
    modifier: Modifier = Modifier,
    contentTextStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyMedium,
    cardCornerRadius: Dp = 12.dp,
    isGenerating: Boolean = false
) {
    val regex = Regex("<\\|?channel\\|?>([a-zA-Z0-9_]*)")
    val matches = regex.findAll(rawText).toList()

    if (matches.isEmpty()) {
        SelectionContainer {
            Text(
                text = rawText,
                style = contentTextStyle,
                modifier = modifier
            )
        }
        return
    }

    var thinkingText: String? = null
    var contentText = ""

    for (i in matches.indices) {
        val match = matches[i]
        val channelName = match.groupValues[1]
        val startIdx = match.range.last + 1
        val endIdx = if (i + 1 < matches.size) matches[i + 1].range.first else rawText.length
        val sectionText = rawText.substring(startIdx, endIdx).trim()

        if (channelName == "thought" || channelName == "thinking") {
            thinkingText = sectionText
        } else {
            contentText = sectionText
        }
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (!thinkingText.isNullOrBlank()) {
            var isExpanded by remember { mutableStateOf(false) }

            LaunchedEffect(isGenerating) {
                isExpanded = isGenerating
            }

            val arrowRotation by animateFloatAsState(
                targetValue = if (isExpanded) 180f else 0f,
                animationSpec = tween(durationMillis = 300),
                label = "arrowRotation"
            )

            val borderAlpha by animateFloatAsState(
                targetValue = if (isExpanded) 0.45f else 0.2f,
                animationSpec = tween(durationMillis = 300),
                label = "borderAlpha"
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = borderAlpha),
                        shape = RoundedCornerShape(cardCornerRadius)
                    )
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(cardCornerRadius)
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isExpanded = !isExpanded }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = if (isGenerating) "AI 正在哲學思辨中…" else "查看思考推理過程 (Thinking)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "收起" else "展開",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.rotate(arrowRotation)
                    )
                }

                AnimatedVisibility(
                    visible = isExpanded,
                    enter = expandVertically(animationSpec = tween(durationMillis = 300)) +
                            fadeIn(animationSpec = tween(durationMillis = 250, delayMillis = 50)),
                    exit = shrinkVertically(animationSpec = tween(durationMillis = 300)) +
                            fadeOut(animationSpec = tween(durationMillis = 200))
                ) {
                    Column {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(IntrinsicSize.Max)
                                    .background(
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                        shape = RoundedCornerShape(50)
                                    )
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            SelectionContainer(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = thinkingText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f)
                                )
                            }
                            CopyIconButton(
                                textToCopy = thinkingText,
                                iconSize = 14.dp,
                                buttonSize = 24.dp,
                                cleanReasoningTags = false,
                                contentDescription = "複製思考過程"
                            )
                        }
                    }
                }
            }
        }

        if (contentText.isNotBlank()) {
            SelectionContainer {
                Text(text = contentText, style = contentTextStyle)
            }
        }
    }
}
