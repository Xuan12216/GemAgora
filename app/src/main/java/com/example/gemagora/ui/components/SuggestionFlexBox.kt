package com.example.gemagora.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SuggestionFlexBox(
    suggestions: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    selectedText: String? = null,
    isRefreshing: Boolean = false,
    onRefresh: (() -> Unit)? = null,
    isAiGenerated: Boolean = false,
    refreshButtonText: String = "換一批",
    useScrollableRow: Boolean = false,
    maxLines: Int = Int.MAX_VALUE
) {
    if (suggestions.isEmpty() && onRefresh == null) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (title != null || onRefresh != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .padding(end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (title != null) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (isAiGenerated) {
                        AiGeneratedBadge()
                    }
                }

                if (onRefresh != null) {
                    RefreshSuggestionsButton(
                        isRefreshing = isRefreshing,
                        onClick = onRefresh,
                        buttonText = refreshButtonText
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = suggestions.isNotEmpty(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            if (useScrollableRow) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    suggestions.forEach { text ->
                        val isSelected = selectedText == text
                        SuggestionChip(
                            onClick = { onSelect(text) },
                            icon = if (isAiGenerated) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = "AI 建議",
                                        modifier = Modifier.size(12.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            } else null,
                            label = {
                                Text(
                                    text = text,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = if (isSelected) {
                                SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            } else {
                                SuggestionChipDefaults.suggestionChipColors()
                            }
                        )
                    }
                }
            } else {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    maxLines = maxLines
                ) {
                    suggestions.forEach { text ->
                        val isSelected = selectedText == text
                        SuggestionChip(
                            onClick = { onSelect(text) },
                            icon = if (isAiGenerated) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = "AI 建議",
                                        modifier = Modifier.size(12.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            } else null,
                            label = {
                                Text(
                                    text = text,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = if (isSelected) {
                                SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            } else {
                                SuggestionChipDefaults.suggestionChipColors()
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SuggestionTaggedFlexBox(
    suggestions: List<Pair<String, String>>, // text to tag
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    isRefreshing: Boolean = false,
    onRefresh: (() -> Unit)? = null,
    isAiGenerated: Boolean = false,
    refreshButtonText: String = "換一批"
) {
    if (suggestions.isEmpty() && onRefresh == null) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (title != null || onRefresh != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .padding(end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (title != null) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (isAiGenerated) {
                        AiGeneratedBadge()
                    }
                }

                if (onRefresh != null) {
                    RefreshSuggestionsButton(
                        isRefreshing = isRefreshing,
                        onClick = onRefresh,
                        buttonText = refreshButtonText
                    )
                }
            }
        }

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            suggestions.forEach { (text, tag) ->
                SuggestionChip(
                    onClick = { onSelect(text) },
                    icon = if (isAiGenerated) {
                        {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "AI 生成範例",
                                modifier = Modifier.size(11.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else null,
                    label = {
                        Text(
                            text = tag,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun AiGeneratedBadge(
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
        shape = RoundedCornerShape(4.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(10.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "AI 生成",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
fun RefreshSuggestionsButton(
    isRefreshing: Boolean,
    onClick: () -> Unit,
    buttonText: String = "換一批",
    icon: ImageVector = Icons.Default.AutoAwesome
) {
    TextButton(
        onClick = onClick,
        enabled = !isRefreshing,
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
        modifier = Modifier.height(28.dp)
    ) {
        if (isRefreshing) {
            CircularProgressIndicator(
                modifier = Modifier.size(12.dp),
                strokeWidth = 1.5.dp,
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            Icon(
                imageVector = icon,
                contentDescription = "請 AI 提供更多建議",
                modifier = Modifier.size(13.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.width(4.dp))
        Text(
            text = buttonText,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            softWrap = false,
            color = if (isRefreshing) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            else MaterialTheme.colorScheme.primary
        )
    }
}
