package com.example.gemagora.ui.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.example.gemagora.data.model.AppearanceSettings
import com.example.gemagora.data.model.FontSizeScale
import com.example.gemagora.data.model.ThemeMode
import com.example.gemagora.theme.ColorUtils
import com.example.gemagora.ui.components.SettingsCard
import kotlin.math.roundToInt

private data class ThemePreset(
    val name: String,
    val hue: Int,
    val saturation: Float = 0.52f,
)

private val PresetThemes = listOf(
    ThemePreset("雅典金輝", 45, 0.65f),
    ThemePreset("蘇格拉底橄欖", 95, 0.48f),
    ThemePreset("斯多葛岩灰", 215, 0.22f),
    ThemePreset("柏拉圖洞穴藍", 210, 0.55f),
    ThemePreset("存在主義夜紫", 275, 0.54f),
    ThemePreset("虛無幽黑藍", 230, 0.40f),
    ThemePreset("東方墨韻青", 165, 0.52f),
    ThemePreset("沉思赤陶紅", 12, 0.62f),
    ThemePreset("理性晴空藍", 195, 0.60f),
    ThemePreset("晨曦朝露黃", 35, 0.70f),
    ThemePreset("古典羊皮暖灰", 40, 0.28f),
    ThemePreset("德性翡翠綠", 145, 0.55f),
)

@Composable
internal fun AppearanceSettingsCard(settings: AppearanceSettings, onChange: (AppearanceSettings) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ThemeColorSettingsCard(settings = settings, onChange = onChange)
        FontSizeSettingsCard(settings = settings, onChange = onChange)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ThemeColorSettingsCard(settings: AppearanceSettings, onChange: (AppearanceSettings) -> Unit) {
    var hue by remember(settings.customHue) { mutableFloatStateOf((settings.customHue ?: 45).toFloat()) }
    var saturation by remember(settings.customSaturation) { mutableFloatStateOf(settings.customSaturation ?: 0.52f) }

    val currentHex = remember(hue, saturation) {
        ColorUtils.hslToHex(hue, saturation, 0.38f)
    }
    var hexInputText by remember(settings.customHex, currentHex) {
        mutableStateOf(settings.customHex ?: currentHex)
    }
    var hexError by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    SettingsCard {
        Text("主題外觀與色彩", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        // 1. 深淺色模式切換
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("外觀模式", style = MaterialTheme.typography.titleSmall)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                ThemeMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = settings.themeMode == mode,
                        onClick = { onChange(settings.copy(themeMode = mode)) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = ThemeMode.entries.size)
                    ) {
                        Text(
                            when (mode) {
                                ThemeMode.SYSTEM -> "跟隨系統"
                                ThemeMode.LIGHT -> "淺色"
                                ThemeMode.DARK -> "深色"
                            }
                        )
                    }
                }
            }
        }

        HorizontalDivider()

        // 2. 桌布動態色彩
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("使用桌布動態配色", style = MaterialTheme.typography.titleSmall)
                Text("跟隨手機桌布色彩，自動搭配深淺色模式 (Android 12+)。", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = settings.useWallpaperColors, onCheckedChange = {
                onChange(settings.copy(useWallpaperColors = it))
            })
        }

        if (!settings.useWallpaperColors) {
            HorizontalDivider()

            // 3. 精選哲學風格主題色
            Text("精選哲學風格調色盤", style = MaterialTheme.typography.titleSmall)

            val row1 = remember { PresetThemes.filterIndexed { index, _ -> index % 2 == 0 } }
            val row2 = remember { PresetThemes.filterIndexed { index, _ -> index % 2 == 1 } }

            @Composable
            fun PresetChip(preset: ThemePreset) {
                val isSelected = settings.customHue == preset.hue &&
                    (settings.customSaturation == null || kotlin.math.abs(settings.customSaturation - preset.saturation) < 0.05f)
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        val hex = ColorUtils.hslToHex(preset.hue.toFloat(), preset.saturation, 0.38f)
                        onChange(
                            settings.copy(
                                customHue = preset.hue,
                                customSaturation = preset.saturation,
                                customHex = hex
                            )
                        )
                    },
                    leadingIcon = {
                        Box(
                            Modifier
                                .size(16.dp)
                                .background(Color.hsl(preset.hue.toFloat(), preset.saturation, 0.45f), CircleShape)
                        )
                    },
                    label = { Text(preset.name) }
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row1.forEach { preset ->
                        PresetChip(preset)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row2.forEach { preset ->
                        PresetChip(preset)
                    }
                }
            }

            HorizontalDivider()

            // 4. 色相滑桿 (Hue Slider)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("色相 (Hue)", style = MaterialTheme.typography.titleSmall)
                    Text("${hue.roundToInt()}°", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        Modifier
                            .size(36.dp)
                            .background(Color.hsl(hue, saturation, 0.45f), CircleShape)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                    )
                    Slider(
                        value = hue,
                        onValueChange = { hue = it },
                        onValueChangeFinished = {
                            val newHex = ColorUtils.hslToHex(hue, saturation, 0.38f)
                            onChange(settings.copy(customHue = hue.roundToInt(), customSaturation = saturation, customHex = newHex))
                        },
                        valueRange = 0f..359f,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 5. 飽和度滑桿 (Saturation Slider)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("鮮豔度 / 飽和度", style = MaterialTheme.typography.titleSmall)
                    Text("${(saturation * 100).roundToInt()}%", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
                Slider(
                    value = saturation,
                    onValueChange = { saturation = it },
                    onValueChangeFinished = {
                        val newHex = ColorUtils.hslToHex(hue, saturation, 0.38f)
                        onChange(settings.copy(customHue = hue.roundToInt(), customSaturation = saturation, customHex = newHex))
                    },
                    valueRange = 0.20f..1.00f,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("向左調整為淡雅古典調，向右調整為現代鮮亮調。", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // 6. HEX 色碼自訂輸入
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("自訂 HEX 色碼", style = MaterialTheme.typography.titleSmall)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = hexInputText,
                        onValueChange = { input ->
                            hexInputText = input
                            hexError = false
                        },
                        label = { Text("色碼 (如 #C59B27)") },
                        singleLine = true,
                        isError = hexError,
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            val parsed = ColorUtils.hexToHsl(hexInputText)
                            if (parsed != null) {
                                focusManager.clearFocus()
                                val (h, s, _) = parsed
                                hue = h.toFloat()
                                saturation = s.coerceIn(0.2f, 1.0f)
                                onChange(
                                    settings.copy(
                                        customHue = h,
                                        customSaturation = saturation,
                                        customHex = hexInputText.trim().let { if (it.startsWith("#")) it else "#$it" }.uppercase()
                                    )
                                )
                            } else {
                                hexError = true
                            }
                        })
                    )
                    Button(
                        onClick = {
                            val parsed = ColorUtils.hexToHsl(hexInputText)
                            if (parsed != null) {
                                focusManager.clearFocus()
                                val (h, s, _) = parsed
                                hue = h.toFloat()
                                saturation = s.coerceIn(0.2f, 1.0f)
                                onChange(
                                    settings.copy(
                                        customHue = h,
                                        customSaturation = saturation,
                                        customHex = hexInputText.trim().let { if (it.startsWith("#")) it else "#$it" }.uppercase()
                                    )
                                )
                            } else {
                                hexError = true
                            }
                        },
                        modifier = Modifier.height(56.dp)
                    ) {
                        Text("套用")
                    }
                }
                if (hexError) {
                    Text("請輸入有效的 6 位元十六進位色碼（例如 #C59B27）", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }

            // 7. 即時預覽卡片
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("配色即時效果預覽", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Text(currentHex, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(onClick = {}, modifier = Modifier.weight(1f)) {
                            Text("主要按鈕")
                        }
                        FilledTonalButton(onClick = {}, modifier = Modifier.weight(1f)) {
                            Text("次要按鈕")
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SuggestionChip(onClick = {}, label = { Text("蘇格拉底詰問") })
                        AssistChip(onClick = {}, label = { Text("思想實驗") })
                    }
                }
            }

            // 8. 恢復預設
            TextButton(
                onClick = {
                    onChange(
                        settings.copy(
                            customHue = null,
                            customSaturation = null,
                            customHex = null,
                            useWallpaperColors = false
                        )
                    )
                },
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(if (settings.customHue == null && settings.customSaturation == null) "目前使用 GemAgora 經典雅典金輝配色" else "恢復 GemAgora 經典雅典金輝配色")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FontSizeSettingsCard(settings: AppearanceSettings, onChange: (AppearanceSettings) -> Unit) {
    var sliderScale by remember(settings.fontScale) { mutableFloatStateOf(settings.fontScale) }
    val currentScale = FontSizeScale.fromScale(sliderScale)
    val percentage = (sliderScale * 100).roundToInt()

    SettingsCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("字體大小與閱讀排版", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = "${currentScale.label} · $percentage%",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Text(
            text = "滑動滑桿或點選預設檔微調全 App 介面與對話文字大小，提升長篇哲學思辨的閱讀舒適度。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // 1. 快速預設檔選鈕
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            FontSizeScale.entries.forEachIndexed { index, scaleOption ->
                val isSelected = kotlin.math.abs(sliderScale - scaleOption.scale) < 0.03f
                SegmentedButton(
                    selected = isSelected,
                    onClick = {
                        sliderScale = scaleOption.scale
                        onChange(settings.copy(fontScale = scaleOption.scale))
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = FontSizeScale.entries.size)
                ) {
                    Text(scaleOption.label)
                }
            }
        }

        // 2. 字體大小精細滑桿
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "A",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Slider(
                    value = sliderScale,
                    onValueChange = {
                        sliderScale = (it * 20f).roundToInt() / 20f
                    },
                    onValueChangeFinished = {
                        val finalScale = (sliderScale * 20f).roundToInt() / 20f
                        sliderScale = finalScale
                        onChange(settings.copy(fontScale = finalScale))
                    },
                    valueRange = 0.85f..1.30f,
                    steps = 8,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "A",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("較小 (85%)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("標準 (100%)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("特大 (130%)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // 3. 即時文字排版效果預覽卡片
        val currentDensity = LocalDensity.current
        val previewDensity = remember(currentDensity.density, currentDensity.fontScale, sliderScale) {
            Density(
                density = currentDensity.density,
                fontScale = currentDensity.fontScale * sliderScale
            )
        }

        CompositionLocalProvider(LocalDensity provides previewDensity) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "即時文字排版效果預覽",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$percentage%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "「未經審視的人生是不值得過的。」",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "—— 蘇格拉底 ·《申辯篇》",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "哲學思辨在於不斷審視前提與概念，讓真理在理性反詰中越辯越明。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

