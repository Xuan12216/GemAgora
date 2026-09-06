package com.example.gemagora.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun TtsPlayerControl(
    isPlaying: Boolean,
    isPaused: Boolean,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
    iconSize: Dp = 18.dp,
    buttonSize: Dp = 32.dp,
    activeTint: Color = MaterialTheme.colorScheme.primary,
    idleTint: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        when {
            isPlaying -> {
                // Pause button
                IconButton(
                    onClick = onPause,
                    modifier = Modifier.size(buttonSize)
                ) {
                    Icon(
                        imageVector = Icons.Default.Pause,
                        contentDescription = "暫停朗讀",
                        tint = activeTint,
                        modifier = Modifier.size(iconSize)
                    )
                }

                // Stop & Reset button
                IconButton(
                    onClick = onStop,
                    modifier = Modifier.size(buttonSize)
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "停止朗讀並重置",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.85f),
                        modifier = Modifier.size(iconSize)
                    )
                }
            }

            isPaused -> {
                // Resume button
                IconButton(
                    onClick = onResume,
                    modifier = Modifier.size(buttonSize)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "繼續朗讀",
                        tint = activeTint,
                        modifier = Modifier.size(iconSize)
                    )
                }

                // Stop & Reset button
                IconButton(
                    onClick = onStop,
                    modifier = Modifier.size(buttonSize)
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "停止朗讀並重置",
                        tint = idleTint,
                        modifier = Modifier.size(iconSize)
                    )
                }
            }

            else -> {
                // Play from beginning
                IconButton(
                    onClick = onPlay,
                    modifier = Modifier.size(buttonSize)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "語音朗讀",
                        tint = idleTint,
                        modifier = Modifier.size(iconSize)
                    )
                }
            }
        }
    }
}
