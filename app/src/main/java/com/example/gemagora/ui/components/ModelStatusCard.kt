package com.example.gemagora.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.gemagora.data.model.LocalModelInfo
import com.example.gemagora.data.model.ModelDownloadState
import com.example.gemagora.data.model.ModelLoadState

@Composable
fun ModelStatusCard(
    model: LocalModelInfo,
    downloadState: ModelDownloadState,
    loadState: ModelLoadState,
    onDownloadClick: () -> Unit,
    onLoadClick: () -> Unit,
    onImportClick: () -> Unit,
    isActive: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        else MaterialTheme.colorScheme.surfaceContainer,
        border = if (isActive) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = model.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${model.fileSizeBytes / (1024 * 1024)} MB",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (isActive) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text("使用中") },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            labelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            when (downloadState) {
                is ModelDownloadState.Downloading -> {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        LinearProgressIndicator(
                            progress = { downloadState.progress },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${(downloadState.progress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall
                            )
                            Text(
                                text = "${downloadState.downloadedBytes / 1024 / 1024}MB / ${downloadState.totalBytes / 1024 / 1024}MB",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
                is ModelDownloadState.Failed -> {
                    Text(
                        text = "下載失敗: ${downloadState.message}",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                else -> Unit
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (model.isDownloaded) {
                    Button(
                        onClick = onLoadClick,
                        modifier = Modifier.weight(1f),
                        enabled = !isActive && loadState !is ModelLoadState.Loading
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (isActive) "已載入" else "載入模型")
                    }
                } else {
                    Button(
                        onClick = onDownloadClick,
                        modifier = Modifier.weight(1f),
                        enabled = downloadState !is ModelDownloadState.Downloading
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("下載模型")
                    }
                }

                OutlinedButton(
                    onClick = onImportClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("本地匯入")
                }
            }
        }
    }
}
