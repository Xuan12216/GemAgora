package com.example.gemagora.ui.socratic

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.gemagora.data.model.ChatMessage
import com.example.gemagora.data.model.ModelLoadState
import com.example.gemagora.ui.components.CopyIconButton
import com.example.gemagora.ui.components.HistoryBottomSheet
import com.example.gemagora.ui.components.ThinkingContent
import com.example.gemagora.ui.components.TtsPlayerControl
import androidx.compose.foundation.text.selection.SelectionContainer
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

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

    val hazeState = rememberHazeState()
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val glassTint = if (isDark) MaterialTheme.colorScheme.surface.copy(alpha = 0.32f) else Color.White.copy(alpha = 0.12f)

    var isInputCompact by rememberSaveable { mutableStateOf(false) }
    var isInputFocused by remember { mutableStateOf(false) }

    val inputInteractionSource = remember { MutableInteractionSource() }
    LaunchedEffect(inputInteractionSource) {
        inputInteractionSource.interactions.collect { interaction ->
            if (interaction is PressInteraction.Press || interaction is PressInteraction.Release) {
                if (isInputCompact) {
                    isInputCompact = false
                }
            }
        }
    }

    val inputNestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                // Swiping up (reading downwards): delta < -8f -> shrink input bar
                // Swiping down (reading upwards / back to top): delta > 8f -> expand input bar
                if (delta < -8f && !isInputCompact) {
                    isInputCompact = true
                } else if (delta > 8f && isInputCompact) {
                    isInputCompact = false
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                // If user reached top edge and pulled down further
                if (available.y > 0f && isInputCompact) {
                    isInputCompact = false
                } else if (available.y < 0f && !isInputCompact) {
                    isInputCompact = true
                }
                return Offset.Zero
            }
        }
    }

    // Auto expand when soft keyboard opens
    val navBarBottomDp = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val imeBottomDp = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
    val bottomInset = maxOf(navBarBottomDp, imeBottomDp)

    LaunchedEffect(imeBottomDp) {
        if (imeBottomDp > 0.dp) {
            isInputCompact = false
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Dynamic animations for the unified bottom floating capsule
    val animatedHeight by animateDpAsState(
        targetValue = if (isInputCompact) 52.dp else 60.dp,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow),
        label = "inputHeight"
    )
    val animatedCornerRadius by animateDpAsState(
        targetValue = if (isInputCompact) 26.dp else 30.dp,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow),
        label = "cornerRadius"
    )
    val animatedInnerButtonSize by animateDpAsState(
        targetValue = if (isInputCompact) 38.dp else 44.dp,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow),
        label = "innerButtonSize"
    )
    val animatedInnerIconSize by animateDpAsState(
        targetValue = if (isInputCompact) 18.dp else 21.dp,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow),
        label = "innerIconSize"
    )
    val animatedHorizontalPadding by animateDpAsState(
        targetValue = if (isInputCompact) 28.dp else 20.dp,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow),
        label = "horizontalPadding"
    )
    val animatedBottomPadding by animateDpAsState(
        targetValue = if (isInputCompact) 6.dp else 10.dp,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow),
        label = "bottomPadding"
    )
    val animatedDockPadding by animateDpAsState(
        targetValue = bottomInset + (if (isInputCompact) 84.dp else 104.dp),
        animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow),
        label = "dockPadding"
    )

    val pillShape = RoundedCornerShape(animatedCornerRadius)

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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
        ) {
            // Main Content Area
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSource(hazeState)
            ) {
                // Intensity filter: Segmented Pill Control that collapses together with TopAppBar
                val isTopBarCollapsed = scrollBehavior.state.collapsedFraction > 0.08f
                val showIntensityRow = !isInputCompact && !isTopBarCollapsed

                AnimatedVisibility(
                    visible = showIntensityRow,
                    enter = fadeIn(tween(180)) + expandVertically(tween(220)),
                    exit = fadeOut(tween(140)) + shrinkVertically(tween(180))
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isDark) MaterialTheme.colorScheme.surfaceContainerHigh else Color.White,
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isDark) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.50f)
                        ),
                        shadowElevation = 0.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(3.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf("溫和啟發", "標準反詰", "極致解構").forEach { option ->
                                val isSelected = intensity == option
                                val selectedBg = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    Color.Transparent
                                }
                                val contentColor = if (isSelected) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(32.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(selectedBg)
                                        .clickable { viewModel.setIntensity(option) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = option,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = contentColor
                                    )
                                }
                            }
                        }
                    }
                }

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
                        .padding(horizontal = 16.dp)
                        .nestedScroll(inputNestedScrollConnection),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(
                        top = 12.dp,
                        bottom = animatedDockPadding
                    )
                ) {
                    if (messages.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 28.dp, bottom = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
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

                                Spacer(Modifier.height(16.dp))

                                Text(
                                    text = "或從經典哲學命題開始思辨：",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp)
                                )
                            }
                        }

                        // Topic Starter Cards directly on Canvas
                        items(topicStarters) { topic ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isDark) MaterialTheme.colorScheme.surfaceContainerLow else Color.White,
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (isDark) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.30f)
                                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
                                ),
                                shadowElevation = 0.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable {
                                        inputText = topic
                                        viewModel.sendMessage(topic)
                                        inputText = ""
                                        isInputCompact = false
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = topic,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = "探討此題目",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
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
            }

            // Floating Bottom Dock: Unified Frosted Glass Input Capsule (All-in-one NavBar style)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = animatedHorizontalPadding)
                    .padding(bottom = animatedBottomPadding)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = animatedHeight)
                        .shadow(
                            elevation = if (isDark) 0.dp else 4.dp,
                            shape = pillShape,
                            ambientColor = Color.Black.copy(alpha = 0.06f),
                            spotColor = Color.Black.copy(alpha = 0.08f)
                        )
                        .clip(pillShape)
                        .clickable(
                            enabled = isInputCompact,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            isInputCompact = false
                        }
                        .hazeEffect(state = hazeState) {
                            blurRadius = 15.dp
                            tints = listOf(HazeTint(glassTint))
                            noiseFactor = 20f
                        },
                    shape = pillShape,
                    color = if (isDark) MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.38f)
                    else MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.50f),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isDark) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f)
                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.40f)
                    ),
                    shadowElevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                start = 18.dp,
                                end = 6.dp,
                                top = if (isInputCompact) 5.dp else 7.dp,
                                bottom = if (isInputCompact) 5.dp else 7.dp
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Text input field
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(vertical = if (isInputCompact) 6.dp else 8.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            BasicTextField(
                                value = inputText,
                                onValueChange = {
                                    inputText = it
                                    if (isInputCompact) {
                                        isInputCompact = false
                                    }
                                },
                                interactionSource = inputInteractionSource,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onFocusChanged { focusState ->
                                        isInputFocused = focusState.isFocused
                                        if (focusState.isFocused) {
                                            isInputCompact = false
                                        }
                                    },
                                textStyle = (if (isInputCompact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium).copy(
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                maxLines = if (isInputCompact) 1 else 4,
                                decorationBox = { innerTextField ->
                                    Box(
                                        modifier = Modifier.fillMaxWidth(),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        if (inputText.isEmpty()) {
                                            Text(
                                                text = if (isInputCompact) "反詰提問…" else "陳述你的觀點或提出疑問…",
                                                style = if (isInputCompact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        innerTextField()
                                    }
                                }
                            )
                        }

                        Spacer(Modifier.width(8.dp))

                        // Inner Action Button (Send / Stop)
                        val isActionActive = isGenerating || inputText.isNotBlank()
                        Box(
                            modifier = Modifier
                                .size(animatedInnerButtonSize)
                                .clip(CircleShape)
                                .background(
                                    if (isGenerating) MaterialTheme.colorScheme.error
                                    else if (inputText.isNotBlank()) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                                )
                                .clickable(
                                    enabled = isActionActive,
                                    onClick = {
                                        if (isGenerating) {
                                            viewModel.cancelCurrentGeneration()
                                        } else {
                                            val text = inputText
                                            inputText = ""
                                            isInputCompact = false
                                            viewModel.sendMessage(text)
                                        }
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isGenerating) Icons.Default.Stop else Icons.AutoMirrored.Filled.Send,
                                contentDescription = if (isGenerating) "停止生成" else "發送",
                                tint = if (isGenerating) MaterialTheme.colorScheme.onError
                                else if (inputText.isNotBlank()) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(animatedInnerIconSize)
                            )
                        }
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

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
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

                        if (message.content.isNotBlank()) {
                            CopyIconButton(
                                textToCopy = message.content,
                                iconSize = 15.dp,
                                buttonSize = 26.dp,
                                tint = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                contentDescription = if (isUser) "複製我的提問" else "複製蘇格拉底回覆"
                            )
                        }
                    }
                }

                if (isUser) {
                    SelectionContainer {
                        Text(text = message.content, style = MaterialTheme.typography.bodyMedium)
                    }
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
