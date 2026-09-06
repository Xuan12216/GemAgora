package com.example.gemagora.ui.setup

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gemagora.data.model.TtsPreset
import com.example.gemagora.data.model.TtsSettings
import com.example.gemagora.data.model.VoiceGender
import com.example.gemagora.data.model.VoiceInfo
import com.example.gemagora.ui.components.SettingsCard
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TtsSettingsCard(
    settings: TtsSettings,
    availableVoices: List<VoiceInfo>,
    isPlaying: Boolean,
    isReady: Boolean,
    onSettingsChange: (TtsSettings) -> Unit,
    onPreviewClick: (TtsSettings) -> Unit,
    onStopClick: () -> Unit
) {
    val presetsScroll = rememberScrollState()
    val activePreset = remember(settings.presetId) {
        TtsSettings.findPreset(settings.presetId)
    }

    var showVoiceDropdown by remember { mutableStateOf(false) }

    // 1. 主開關與引擎狀態
    SettingsCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.RecordVoiceOver,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column {
                    Text(
                        text = "語音朗讀 (TTS)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isReady) Color(0xFF10B981) else Color(0xFFF59E0B))
                        )
                        Text(
                            text = if (isReady) "Android 系統 TTS 引擎已就緒" else "TTS 引擎初始化中…",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Switch(
                checked = settings.enabled,
                onCheckedChange = { isChecked ->
                    onSettingsChange(settings.copy(enabled = isChecked))
                }
            )
        }

        Text(
            text = "利用 Android 內建原生語音服務，100% 離線朗讀哲學詰問與思想實驗對話，保護思辨隱私。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    AnimatedVisibility(
        visible = settings.enabled,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

            // 2. 聲線人設切換
            SettingsCard {
                Text(
                    text = "哲學聲線風格 (Presets)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "選擇不同的講述者人設，即時切換男聲、女聲與沈思音律。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(presetsScroll),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TtsSettings.PRESETS.forEach { preset ->
                        val isSelected = settings.presetId == preset.id
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                onSettingsChange(
                                    settings.copy(
                                        presetId = preset.id,
                                        pitch = preset.pitch,
                                        speed = preset.speed
                                    )
                                )
                            },
                            label = {
                                Text(
                                    preset.name,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = when (preset.genderHint) {
                                        VoiceGender.MALE -> Icons.Default.Male
                                        VoiceGender.FEMALE -> Icons.Default.Female
                                        VoiceGender.NEUTRAL -> Icons.Default.GraphicEq
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                }

                // 當前人設資訊卡
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = activePreset.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            SuggestionChip(
                                onClick = {},
                                label = {
                                    Text(
                                        activePreset.subtitle,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                modifier = Modifier.height(24.dp)
                            )
                        }
                        Text(
                            text = activePreset.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 3. 系統語音選擇 (若系統有檢測到語音庫)
            if (availableVoices.isNotEmpty()) {
                SettingsCard {
                    Text(
                        text = "系統底層聲音發音人 (System Voice)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "偵測到本機已安裝 ${availableVoices.size} 款語音庫，可指定底層發音人。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val selectedVoiceInfo = availableVoices.firstOrNull { it.name == settings.voiceName }
                    val currentDisplay = selectedVoiceInfo?.displayName ?: "依人設自動匹配 (預設)"

                    ExposedDropdownMenuBox(
                        expanded = showVoiceDropdown,
                        onExpandedChange = { showVoiceDropdown = it }
                    ) {
                        OutlinedTextField(
                            value = currentDisplay,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("指定發音人") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showVoiceDropdown) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                        )

                        ExposedDropdownMenu(
                            expanded = showVoiceDropdown,
                            onDismissRequest = { showVoiceDropdown = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("依人設自動推薦 (推薦)") },
                                onClick = {
                                    onSettingsChange(settings.copy(voiceName = null))
                                    showVoiceDropdown = false
                                }
                            )
                            HorizontalDivider()
                            availableVoices.forEach { voice ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(voice.displayName, fontWeight = FontWeight.Medium)
                                            Text(voice.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    },
                                    onClick = {
                                        onSettingsChange(
                                            settings.copy(
                                                voiceName = voice.name,
                                                presetId = "custom"
                                            )
                                        )
                                        showVoiceDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // 4. 音調與語速微調
            SettingsCard {
                Text(
                    text = "音色與語速細部調校",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // Pitch Slider
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("音調 (Pitch)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val pitchLabel = when {
                                settings.pitch < 0.85f -> "低沉雄渾 (男聲)"
                                settings.pitch > 1.10f -> "清脆明亮 (女聲)"
                                else -> "標準自然"
                            }
                            Text(
                                text = "${String.format(Locale.US, "%.2f", settings.pitch)}x · $pitchLabel",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            IconButton(
                                onClick = {
                                    onSettingsChange(settings.copy(pitch = activePreset.pitch))
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "還原", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    Slider(
                        value = settings.pitch,
                        onValueChange = { newPitch ->
                            val rounded = (newPitch * 100).roundToInt() / 100f
                            onSettingsChange(settings.copy(pitch = rounded, presetId = "custom"))
                        },
                        valueRange = 0.6f..1.5f,
                        steps = 17
                    )
                }

                // Speed Slider
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("語速 (Speed)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val speedLabel = when {
                                settings.speed < 0.90f -> "沉思緩步"
                                settings.speed > 1.15f -> "明快敏捷"
                                else -> "適中悠然"
                            }
                            Text(
                                text = "${String.format(Locale.US, "%.2f", settings.speed)}x · $speedLabel",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            IconButton(
                                onClick = {
                                    onSettingsChange(settings.copy(speed = activePreset.speed))
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "還原", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    Slider(
                        value = settings.speed,
                        onValueChange = { newSpeed ->
                            val rounded = (newSpeed * 100).roundToInt() / 100f
                            onSettingsChange(settings.copy(speed = rounded, presetId = "custom"))
                        },
                        valueRange = 0.6f..1.8f,
                        steps = 23
                    )
                }
            }

            // 5. 試聽與朗讀時機
            SettingsCard {
                Text(
                    text = "試聽體驗與自動化",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Button(
                    onClick = {
                        if (isPlaying) onStopClick() else onPreviewClick(settings)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = if (isPlaying) {
                        ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    } else {
                        ButtonDefaults.buttonColors()
                    }
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Stop else Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(if (isPlaying) "停止試聽播放" else "即時試聽當前聲音效果")
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("AI 回答完成後自動朗讀", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text("蘇格拉底或學派哲人回答完畢時，立即以當前聲線自動開口。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.width(12.dp))
                    Switch(
                        checked = settings.autoSpeak,
                        onCheckedChange = { isAuto ->
                            onSettingsChange(settings.copy(autoSpeak = isAuto))
                        }
                    )
                }
            }
        }
    }
}
