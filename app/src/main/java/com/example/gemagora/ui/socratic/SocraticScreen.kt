package com.example.gemagora.ui.socratic

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.gemagora.data.model.ChatMessage
import com.example.gemagora.data.model.ModelLoadState
import com.example.gemagora.ui.components.ThinkingContent
import com.example.gemagora.ui.components.TtsPlayerControl

import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.example.gemagora.ui.components.HistoryBottomSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocraticScreen(
    viewModel: SocraticViewModel,
    initialPrompt: String? = null,
    onNavigateBack: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val messages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()
    val intensity by viewModel.intensity.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val historySessions by viewModel.historySessions.collectAsStateWithLifecycle()
    val isTtsPlaying by viewModel.isTtsPlaying.collectAsStateWithLifecycle()
    val isTtsPaused by viewModel.isTtsPaused.collectAsStateWithLifecycle()
    val currentSpeakingUtteranceId by viewModel.currentSpeakingUtteranceId.collectAsStateWithLifecycle()

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopTts()
        }
    }

    var showHistorySheet by remember { mutableStateOf(false) }
    var inputText by remember { mutableStateOf(initialPrompt ?: "") }
    val listState = rememberLazyListState()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    val topicStarters = listOf(
        "什麼是真正的正義？",
        "快樂是否就等於善？",
        "自由意志是否存在？",
        "知識是客觀真理還是純粹信念？",
        "如果法律不公義，我們有義務服從嗎？",
        "未經檢驗的傳統道德，具有正當性嗎？"
    )

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            key(statusBarHeight) {
                TopAppBar(
                    title = {
                        Column {
                            Text("蘇格拉底反詰法", fontWeight = FontWeight.Bold)
                            Text(
                                text = "產婆術 · 概念釐清與矛盾解構",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.startNewSession() }) {
                            Icon(Icons.Default.Add, contentDescription = "開啟新對話")
                        }
                        IconButton(onClick = { showHistorySheet = true }) {
                            Icon(Icons.Default.History, contentDescription = "歷史對話")
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
                .padding(top = padding.calculateTopPadding())
        ) {
            // Intensity filter row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "詰問風格:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                listOf("溫和啟發", "標準反詰", "極致解構").forEach { option ->
                    FilterChip(
                        selected = intensity == option,
                        onClick = { viewModel.setIntensity(option) },
                        label = { Text(option) }
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            val gemmaLoadState by viewModel.gemmaLoadState.collectAsStateWithLifecycle()
            if (gemmaLoadState is ModelLoadState.Loading) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "地端推論引擎背景暖機中，可直接發送問題，將於準備完成時自動開始反詰…",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            // Error banner
            if (errorMessage != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(10.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // Message list
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                if (messages.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "🏛️ 雅典公共廣場的對話",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "說出你的主張或困惑，蘇格拉底將檢視其中的前提與定義。",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                items(messages, key = { it.id }) { message ->
                    val utteranceId = message.id.toString()
                    val isPlayingThis = isTtsPlaying && currentSpeakingUtteranceId == utteranceId
                    val isPausedThis = isTtsPaused && currentSpeakingUtteranceId == utteranceId
                    SocraticMessageBubble(
                        message = message,
                        isGenerating = isGenerating,
                        isPlayingThis = isPlayingThis,
                        isPausedThis = isPausedThis,
                        onPlayTts = { viewModel.speak(message.content, utteranceId) },
                        onPauseTts = { viewModel.pauseTts() },
                        onResumeTts = { viewModel.resumeTts() },
                        onStopTts = { viewModel.stopTts() }
                    )
                }
            }

            // Topic starter chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(topicStarters) { topic ->
                    SuggestionChip(
                        onClick = {
                            inputText = topic
                            viewModel.sendMessage(topic)
                            inputText = ""
                        },
                        label = { Text(topic) }
                    )
                }
            }

            // Input Bar
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("陳述你的觀點或提出疑問…") },
                        modifier = Modifier.weight(1f),
                        maxLines = 4,
                        shape = RoundedCornerShape(24.dp)
                    )

                    IconButton(
                        onClick = {
                            if (isGenerating) {
                                viewModel.cancelCurrentGeneration()
                            } else {
                                val text = inputText
                                inputText = ""
                                viewModel.sendMessage(text)
                            }
                        },
                        enabled = isGenerating || inputText.isNotBlank(),
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                if (isGenerating) MaterialTheme.colorScheme.errorContainer
                                else if (inputText.isNotBlank()) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                    ) {
                        Icon(
                            if (isGenerating) Icons.Default.Stop else Icons.AutoMirrored.Filled.Send,
                            contentDescription = if (isGenerating) "停止生成" else "發送",
                            tint = if (isGenerating) MaterialTheme.colorScheme.onErrorContainer
                            else if (inputText.isNotBlank()) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    if (showHistorySheet) {
        HistoryBottomSheet(
            title = "歷史反詰對話",
            sessions = historySessions,
            onSelectSession = { record ->
                viewModel.loadSession(record.sessionId)
                showHistorySheet = false
            },
            onDeleteSession = viewModel::deleteSession,
            onClearAll = viewModel::clearAllHistory,
            onDismissRequest = { showHistorySheet = false },
            extraHeaderAction = {
                IconButton(onClick = {
                    viewModel.startNewSession()
                    showHistorySheet = false
                }) {
                    Icon(Icons.Default.Add, contentDescription = "新對話")
                }
            },
            itemBadges = { record ->
                if (record.metadataJson.isNotBlank()) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text(record.metadataJson, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
        )
    }
}

@Composable
fun SocraticMessageBubble(
    message: ChatMessage,
    isGenerating: Boolean,
    isPlayingThis: Boolean = false,
    isPausedThis: Boolean = false,
    onPlayTts: () -> Unit = {},
    onPauseTts: () -> Unit = {},
    onResumeTts: () -> Unit = {},
    onStopTts: () -> Unit = {}
) {
    val isUser = message.role == "user"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isUser) 18.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 18.dp
            ),
            color = if (isUser) MaterialTheme.colorScheme.primaryContainer
            else if (isPlayingThis || isPausedThis) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            else MaterialTheme.colorScheme.surfaceContainerHigh,
            border = if (isPlayingThis || isPausedThis) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)) else null,
            tonalElevation = if (isUser) 0.dp else 2.dp,
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isUser) "你" else "蘇格拉底 (Socrates)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.primary
                    )

                    if (!isUser && message.content.isNotBlank() && !isGenerating) {
                        TtsPlayerControl(
                            isPlaying = isPlayingThis,
                            isPaused = isPausedThis,
                            onPlay = onPlayTts,
                            onPause = onPauseTts,
                            onResume = onResumeTts,
                            onStop = onStopTts,
                            iconSize = 15.dp,
                            buttonSize = 26.dp
                        )
                    }
                }

                if (isUser) {
                    Text(text = message.content, style = MaterialTheme.typography.bodyMedium)
                } else {
                    ThinkingContent(
                        rawText = message.content,
                        isGenerating = isGenerating
                    )
                }
            }
        }
    }
}
