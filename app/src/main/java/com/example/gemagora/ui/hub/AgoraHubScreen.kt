package com.example.gemagora.ui.hub

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.gemagora.data.model.ModelLoadState
import com.example.gemagora.ui.components.TtsPlayerControl

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgoraHubScreen(
    viewModel: AgoraHubViewModel,
    onNavigateToSocratic: (String?) -> Unit,
    onNavigateToExperiments: () -> Unit,
    onNavigateToRoundTable: () -> Unit,
    onNavigateToFallacy: () -> Unit,
    onNavigateToJournal: () -> Unit,
    onNavigateToSettings: () -> Unit,
    bottomContentPadding: Dp = 0.dp
) {
    val loadState by viewModel.loadState.collectAsStateWithLifecycle()
    val loadedModelName by viewModel.loadedModelName.collectAsStateWithLifecycle()
    val useGpu by viewModel.useGpu.collectAsStateWithLifecycle()
    val enableThinking by viewModel.enableThinking.collectAsStateWithLifecycle()
    val todayQuote by viewModel.todayQuote.collectAsStateWithLifecycle()
    val isTtsPlaying by viewModel.isTtsPlaying.collectAsStateWithLifecycle()
    val isTtsPaused by viewModel.isTtsPaused.collectAsStateWithLifecycle()
    val currentSpeakingUtteranceId by viewModel.currentSpeakingUtteranceId.collectAsStateWithLifecycle()

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopTts()
        }
    }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            key(statusBarHeight) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "GemAgora",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "雅典思想廣場 · 地端哲學 AI",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "設定", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    expandedHeight = TopAppBarDefaults.TopAppBarExpandedHeight,
                    windowInsets = WindowInsets(0, 0, 0, 0),
                    contentPadding = PaddingValues(top = statusBarHeight),
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    scrollBehavior = scrollBehavior
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(Modifier.height(padding.calculateTopPadding() + 8.dp))
            // On-Device Model Status Banner
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToSettings() },
            shape = RoundedCornerShape(18.dp),
            color = when (loadState) {
                is ModelLoadState.Loaded -> MaterialTheme.colorScheme.surfaceContainerHighest
                is ModelLoadState.Loading -> MaterialTheme.colorScheme.surfaceContainerHigh
                else -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
            },
            tonalElevation = 1.dp
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                when (loadState) {
                    is ModelLoadState.Loading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFFF59E0B)
                        )
                    }
                    else -> {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(
                                    color = if (loadState is ModelLoadState.Loaded) Color(0xFF10B981)
                                    else MaterialTheme.colorScheme.error,
                                    shape = CircleShape
                                )
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when (loadState) {
                            is ModelLoadState.Loaded -> "Gemma 4 離線就緒 (${loadedModelName ?: "已載入"})"
                            is ModelLoadState.Loading -> "Gemma 4 地端推論引擎背景暖機中…"
                            else -> "尚未載入本地模型 (點擊前往設定)"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = when (loadState) {
                            is ModelLoadState.Loaded -> "${if (useGpu) "⚡ GPU 加速" else "CPU 模式"} · ${if (enableThinking) "🧠 原生思維鏈開啟" else "思維鏈關閉"}"
                            is ModelLoadState.Loading -> "⚡ GPU 權重與 KV 快取配置中 · 其它功能可正常使用"
                            else -> "離線運行 · 零網路依賴 · 100% 隱私保護"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Daily Philosophical Reflection Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🏛️ 每日哲思啟發 · ${todayQuote.school}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    val quoteUtteranceId = "hub_quote_${todayQuote.author.hashCode()}"
                    val isPlayingQuote = isTtsPlaying && currentSpeakingUtteranceId == quoteUtteranceId
                    val isPausedQuote = isTtsPaused && currentSpeakingUtteranceId == quoteUtteranceId
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TtsPlayerControl(
                            isPlaying = isPlayingQuote,
                            isPaused = isPausedQuote,
                            onPlay = { viewModel.speakQuote(todayQuote) },
                            onPause = { viewModel.pauseTts() },
                            onResume = { viewModel.resumeTts() },
                            onStop = { viewModel.stopTts() },
                            iconSize = 16.dp,
                            buttonSize = 28.dp
                        )
                        IconButton(onClick = { viewModel.refreshQuote() }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Refresh, contentDescription = "更換", modifier = Modifier.size(16.dp))
                        }
                    }
                }
                Text(
                    text = "「${todayQuote.quote}」",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "—— ${todayQuote.author}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                Text(
                    text = "💡 今日審視提問：${todayQuote.reflectionQuestion}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FilledTonalButton(
                    onClick = { onNavigateToSocratic("我想探討蘇格拉底對這句話的反思：「${todayQuote.quote}」。${todayQuote.reflectionQuestion}") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Psychology, contentDescription = null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("以這句話開啟蘇格拉底反詰")
                }
            }
        }

        // Philosophical Modes Grid / List
        Text(
            text = "核心思辨廣場",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        AgoraFeatureCard(
            icon = Icons.Default.QuestionAnswer,
            title = "蘇格拉底詰問對話",
            subtitle = "Socratic Elenchus",
            description = "AI 不直接給出答案，而是透過追問、概念辨析與邏輯審視，揭露矛盾並引導自我發現本質。",
            badge = "產婆術",
            onClick = { onNavigateToSocratic(null) }
        )

        AgoraFeatureCard(
            icon = Icons.Default.Tune,
            title = "思想實驗模擬器",
            subtitle = "Thought Experiments",
            description = "內建電車難題、無知之幕、忒修斯之船、缸中之腦。調整情境變數，即時推演各倫理學派抉擇。",
            badge = "變數推演",
            onClick = onNavigateToExperiments
        )

        AgoraFeatureCard(
            icon = Icons.Default.Groups,
            title = "多學派圓桌思辨",
            subtitle = "Philosophical Round Table",
            description = "召集斯多葛、存在主義、虛無主義與東方道佛學派，就同一人生課題展開正反合交鋒。",
            badge = "正反合",
            onClick = onNavigateToRoundTable
        )

        AgoraFeatureCard(
            icon = Icons.Default.Search,
            title = "邏輯謬誤診斷室",
            subtitle = "Fallacy Inspector",
            description = "輸入爭論陳述，拆解前提與結論，快篩稻草人、滑坡論證與虛假二分，評估論證健全性。",
            badge = "批判思維",
            onClick = onNavigateToFallacy
        )

        AgoraFeatureCard(
            icon = Icons.AutoMirrored.Filled.MenuBook,
            title = "哲學沉思日記",
            subtitle = "Daily Stoic Journal",
            description = "完全離線私密。每日斯多葛晨思與夕省，以控制二分法梳理焦慮，獲取本地 AI 啟發回饋。",
            badge = "100% 隱私",
            onClick = onNavigateToJournal
        )

        Spacer(Modifier.height(16.dp + bottomContentPadding))
    }
    }
}

@Composable
fun AgoraFeatureCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    description: String,
    badge: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    SuggestionChip(
                        onClick = onClick,
                        label = { Text(badge, style = MaterialTheme.typography.labelSmall) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            labelColor = MaterialTheme.colorScheme.primary
                        ),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    )
                }
                Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
