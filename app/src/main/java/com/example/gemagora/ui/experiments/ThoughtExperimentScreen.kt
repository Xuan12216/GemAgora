package com.example.gemagora.ui.experiments

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.gemagora.ai.PhilosophicalParser
import com.example.gemagora.ui.components.HistoryBottomSheet
import com.example.gemagora.ui.components.ThinkingContent
import com.example.gemagora.ui.components.TtsPlayerControl
import com.example.gemagora.ui.components.CopyIconButton
import androidx.compose.foundation.text.selection.SelectionContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThoughtExperimentScreen(
    viewModel: ThoughtExperimentViewModel,
    onNavigateBack: (() -> Unit)? = null,
    bottomContentPadding: androidx.compose.ui.unit.Dp = 0.dp
) {
    val selectedExp by viewModel.selectedExperiment.collectAsStateWithLifecycle()
    val variableValues by viewModel.variableValues.collectAsStateWithLifecycle()
    val deductionResult by viewModel.deductionResult.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()

    val historySessions by viewModel.historySessions.collectAsStateWithLifecycle()
    val followUpTurns by viewModel.followUpTurns.collectAsStateWithLifecycle()
    val streamingFollowUp by viewModel.streamingFollowUp.collectAsStateWithLifecycle()
    val isGeneratingFollowUp by viewModel.isGeneratingFollowUp.collectAsStateWithLifecycle()

    val isTtsPlaying by viewModel.isTtsPlaying.collectAsStateWithLifecycle()
    val isTtsPaused by viewModel.isTtsPaused.collectAsStateWithLifecycle()
    val currentSpeakingUtteranceId by viewModel.currentSpeakingUtteranceId.collectAsStateWithLifecycle()

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopTts()
        }
    }

    var showHistorySheet by remember { mutableStateOf(false) }
    var followUpInput by remember { mutableStateOf("") }
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    val parsedResult = remember(deductionResult) {
        PhilosophicalParser.parseThoughtExperiment(deductionResult)
    }

    var isConfigExpanded by remember(selectedExp.id, deductionResult.isEmpty()) {
        mutableStateOf(deductionResult.isEmpty())
    }

    var selectedTabIndex by remember { mutableIntStateOf(0) }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            key(statusBarHeight) {
                TopAppBar(
                    title = {
                        Column {
                            Text("思想實驗模擬器", fontWeight = FontWeight.Bold)
                            Text(
                                text = "情境變體 · 跨學派倫理推演",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        if (onNavigateBack != null) {
                            IconButton(onClick = onNavigateBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            viewModel.startNewExperiment()
                            selectedTabIndex = 0
                            isConfigExpanded = true
                        }) {
                            Icon(Icons.Default.Add, contentDescription = "開啟新推演")
                        }
                        IconButton(onClick = { showHistorySheet = true }) {
                            Icon(Icons.Default.History, contentDescription = "歷史紀錄")
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(padding.calculateTopPadding() + 8.dp))
            // 1. Collapsed summary capsule when results are available or generating
            if (deductionResult.isNotBlank() || isGenerating) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    onClick = { isConfigExpanded = !isConfigExpanded }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "🧪 ${selectedExp.title.split(" (").first()}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                                ) {
                                    Text(
                                        text = if (isGenerating) "推演中" else "已設定",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                            val summaryText = remember(selectedExp, variableValues) {
                                selectedExp.variables.joinToString(" · ") { v ->
                                    val cur = variableValues[v.id] ?: v.defaultValue
                                    val valStr = if (v.options != null) {
                                        v.options.getOrNull(cur.toInt()) ?: "${cur.toInt()}"
                                    } else if (v.isToggle) {
                                        if (cur >= 0.5f) "是" else "否"
                                    } else {
                                        "${cur.toInt()}"
                                    }
                                    val shortName = v.name.replace(Regex("（.*）|\\(.*\\)"), "").trim()
                                    "$shortName: $valStr"
                                }
                            }
                            Text(
                                text = summaryText,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(onClick = { isConfigExpanded = !isConfigExpanded }) {
                            Icon(
                                if (isConfigExpanded) Icons.Default.ExpandLess else Icons.Default.Edit,
                                contentDescription = if (isConfigExpanded) "收合設定" else "調整設定",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // 2. Full Configuration Panel (Animated collapsible)
            AnimatedVisibility(
                visible = isConfigExpanded || deductionResult.isBlank(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Experiments Carousel Selector
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(viewModel.experiments) { exp ->
                            FilterChip(
                                selected = exp.id == selectedExp.id,
                                onClick = {
                                    viewModel.selectExperiment(exp)
                                    selectedTabIndex = 0
                                },
                                label = { Text(exp.title.split(" (").first()) }
                            )
                        }
                    }

                    // Experiment Overview Card
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        tonalElevation = 1.dp
                    ) {
                        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            val overviewUtteranceId = "exp_overview_${selectedExp.id}"
                            val isPlayingOverview = isTtsPlaying && currentSpeakingUtteranceId == overviewUtteranceId
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = selectedExp.title,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "哲學淵源：${selectedExp.classicAuthor}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                val overviewText = "${selectedExp.title}。哲學淵源：${selectedExp.classicAuthor}。${selectedExp.description}"
                                TtsPlayerControl(
                                    isPlaying = isPlayingOverview,
                                    isPaused = isTtsPaused && currentSpeakingUtteranceId == overviewUtteranceId,
                                    onPlay = { viewModel.speak(overviewText, overviewUtteranceId) },
                                    onPause = { viewModel.pauseTts() },
                                    onResume = { viewModel.resumeTts() },
                                    onStop = { viewModel.stopTts() }
                                )
                            }
                            Text(
                                text = selectedExp.description,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    // Variables Control Panel
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Text(
                                text = "🎛️ 情境變數調整",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            selectedExp.variables.forEach { variable ->
                                val currentVal = variableValues[variable.id] ?: variable.defaultValue

                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(variable.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                        Text(
                                            if (variable.options != null) {
                                                variable.options.getOrNull(currentVal.toInt()) ?: "${currentVal.toInt()}"
                                            } else if (variable.isToggle) {
                                                if (currentVal >= 0.5f) "開啟 / 是" else "關閉 / 否"
                                            } else "${currentVal.toInt()}",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Text(variable.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                                    if (variable.options != null) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            variable.options.forEachIndexed { optIdx, optName ->
                                                val isOptSelected = currentVal.toInt() == optIdx
                                                FilterChip(
                                                    selected = isOptSelected,
                                                    onClick = { viewModel.updateVariable(variable.id, optIdx.toFloat()) },
                                                    label = {
                                                        Text(
                                                            text = optName,
                                                            fontWeight = if (isOptSelected) FontWeight.Bold else FontWeight.Normal
                                                        )
                                                    }
                                                )
                                            }
                                        }
                                    } else if (variable.isToggle) {
                                        Switch(
                                            checked = currentVal >= 0.5f,
                                            onCheckedChange = { viewModel.updateVariable(variable.id, if (it) 1f else 0f) }
                                        )
                                    } else {
                                        Slider(
                                            value = currentVal,
                                            onValueChange = { viewModel.updateVariable(variable.id, it) },
                                            valueRange = variable.minValue..variable.maxValue,
                                            steps = if (variable.step > 0) ((variable.maxValue - variable.minValue) / variable.step).toInt() - 1 else 0
                                        )
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    if (isGenerating) {
                                        viewModel.cancelDeduction()
                                    } else {
                                        isConfigExpanded = false
                                        viewModel.runEthicalDeduction()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = if (isGenerating) {
                                    ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                } else ButtonDefaults.buttonColors()
                            ) {
                                Icon(
                                    if (isGenerating) Icons.Default.Stop else Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(if (isGenerating) "停止 AI 倫理推演" else "啟動地端 AI 倫理推演")
                            }
                        }
                    }
                }
            }

            // 3. Deduction Result Section (Tabs & Structured Content)
            if (deductionResult.isNotBlank() || isGenerating) {
                val totalTabs = 1 + parsedResult.perspectives.size + 1
                val currentTabIndex = selectedTabIndex.coerceIn(0, (totalTabs - 1).coerceAtLeast(0))
                val followUpTabIdx = 1 + parsedResult.perspectives.size

                // Scrollable Tabs Bar for quick section navigation
                PrimaryScrollableTabRow(
                    selectedTabIndex = currentTabIndex,
                    modifier = Modifier.fillMaxWidth(),
                    edgePadding = 0.dp,
                    divider = {}
                ) {
                    // Tab 0: 總覽全文
                    Tab(
                        selected = currentTabIndex == 0,
                        onClick = { selectedTabIndex = 0 },
                        text = {
                            Text(
                                text = "📋 總覽全文",
                                fontWeight = if (currentTabIndex == 0) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )

                    // Tab 1..N: Each ethical school perspective
                    parsedResult.perspectives.forEachIndexed { idx, p ->
                        val tabIdx = idx + 1
                        Tab(
                            selected = currentTabIndex == tabIdx,
                            onClick = { selectedTabIndex = tabIdx },
                            text = {
                                Text(
                                    text = "${p.iconEmoji} ${p.title}",
                                    fontWeight = if (currentTabIndex == tabIdx) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }

                    // Tab N+1: 延伸思辨
                    Tab(
                        selected = currentTabIndex == followUpTabIdx,
                        onClick = { selectedTabIndex = followUpTabIdx },
                        text = {
                            Text(
                                text = if (followUpTurns.isNotEmpty()) "💬 延伸追問 (${followUpTurns.size / 2})" else "💬 延伸追問",
                                fontWeight = if (currentTabIndex == followUpTabIdx) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }

                // Tab Content Rendering
                when {
                    // Specific Perspective Tab
                    currentTabIndex in 1..parsedResult.perspectives.size -> {
                        val p = parsedResult.perspectives[currentTabIndex - 1]
                        val schoolUtteranceId = "exp_school_${selectedExp.id}_${p.id}"
                        val isSpeakingSchool = isTtsPlaying && currentSpeakingUtteranceId == schoolUtteranceId

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                        ) {
                                            Text(
                                                text = p.iconEmoji,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                        }
                                        Column {
                                            Text(
                                                text = p.title,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = p.fullTitle,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        TtsPlayerControl(
                                            isPlaying = isSpeakingSchool,
                                            isPaused = isTtsPaused && currentSpeakingUtteranceId == schoolUtteranceId,
                                            onPlay = { viewModel.speak("${p.fullTitle}。${p.content}", schoolUtteranceId) },
                                            onPause = { viewModel.pauseTts() },
                                            onResume = { viewModel.resumeTts() },
                                            onStop = { viewModel.stopTts() }
                                        )
                                        CopyIconButton(
                                            textToCopy = p.content,
                                            iconSize = 16.dp,
                                            buttonSize = 28.dp,
                                            contentDescription = "複製${p.title}觀點"
                                        )
                                    }
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                                SelectionContainer {
                                    Text(
                                        text = p.content,
                                        style = MaterialTheme.typography.bodyMedium,
                                        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.25f
                                    )
                                }

                                Spacer(Modifier.height(4.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    if (currentTabIndex < parsedResult.perspectives.size) {
                                        val nextP = parsedResult.perspectives[currentTabIndex]
                                        OutlinedButton(
                                            onClick = { selectedTabIndex = currentTabIndex + 1 },
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text("下一派：${nextP.title} ${nextP.iconEmoji}")
                                            Spacer(Modifier.width(6.dp))
                                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                                        }
                                    } else {
                                        Button(
                                            onClick = { selectedTabIndex = followUpTabIdx },
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text("進入延伸追問 💬")
                                            Spacer(Modifier.width(6.dp))
                                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Follow-Up Interaction Tab
                    currentTabIndex == followUpTabIdx -> {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                modifier = Modifier.fillMaxWidth(),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(
                                        text = "💡 接續追問倫理變體",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "針對此思想實驗結論提出極端條件假設、道德矛盾或親情人倫難題：",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        listOf(
                                            "若軌道上的人是至親摯愛，道德義務如何抉擇？",
                                            "效益主義與康德義務論在極端狀況如何衝突？",
                                            "若當事人具備預知能力，德行論有何建議？",
                                            "在絕對自由下，選擇不作為是否仍須負擔道德責任？"
                                        ).forEach { prompt ->
                                            SuggestionChip(
                                                onClick = { followUpInput = prompt },
                                                label = { Text(prompt, style = MaterialTheme.typography.labelSmall) }
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = followUpInput,
                                            onValueChange = { followUpInput = it },
                                            placeholder = { Text("輸入新的變體情境或倫理質疑...") },
                                            modifier = Modifier.weight(1f),
                                            maxLines = 3,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        if (isGeneratingFollowUp) {
                                            IconButton(onClick = { viewModel.cancelFollowUp() }) {
                                                Icon(Icons.Default.Stop, contentDescription = "停止")
                                            }
                                        } else {
                                            IconButton(
                                                onClick = {
                                                    if (followUpInput.isNotBlank()) {
                                                        viewModel.askFollowUp(followUpInput)
                                                        followUpInput = ""
                                                    }
                                                },
                                                enabled = followUpInput.isNotBlank() && !isGenerating
                                            ) {
                                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "送出")
                                            }
                                        }
                                    }
                                }
                            }

                            // Streaming Follow-Up
                            if (isGeneratingFollowUp) {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                                Text("哲學家深入剖析中...", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                            }
                                            if (!streamingFollowUp.isNullOrBlank()) {
                                                CopyIconButton(
                                                    textToCopy = streamingFollowUp ?: "",
                                                    iconSize = 15.dp,
                                                    buttonSize = 26.dp,
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                        SelectionContainer {
                                            Text(streamingFollowUp ?: "", style = MaterialTheme.typography.bodyMedium)
                                        }
                                    }
                                }
                            }

                            // Follow-up Turns
                            if (followUpTurns.isNotEmpty()) {
                                Text(
                                    text = "💬 歷史追問思辨記錄 (${followUpTurns.size / 2} 輪)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                followUpTurns.chunked(2).forEach { pair ->
                                    val userTurn = pair.firstOrNull { it.role == "user" }
                                    val assistantTurn = pair.firstOrNull { it.role == "assistant" }

                                    if (userTurn != null) {
                                        Surface(
                                            shape = RoundedCornerShape(16.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text("情境追問：", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                                    CopyIconButton(
                                                        textToCopy = userTurn.content,
                                                        iconSize = 15.dp,
                                                        buttonSize = 26.dp,
                                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                                        contentDescription = "複製追問文字"
                                                    )
                                                }
                                                SelectionContainer {
                                                    Text(userTurn.content, style = MaterialTheme.typography.bodyMedium)
                                                }
                                            }
                                        }
                                    }

                                    if (assistantTurn != null) {
                                        val turnUtteranceId = "exp_turn_${assistantTurn.id}"
                                        val isSpeakingTurn = isTtsPlaying && currentSpeakingUtteranceId == turnUtteranceId
                                        Surface(
                                            shape = RoundedCornerShape(16.dp),
                                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text("哲學家推演：", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        TtsPlayerControl(
                                                            isPlaying = isSpeakingTurn,
                                                            isPaused = isTtsPaused && currentSpeakingUtteranceId == turnUtteranceId,
                                                            onPlay = { viewModel.speak(assistantTurn.content, turnUtteranceId) },
                                                            onPause = { viewModel.pauseTts() },
                                                            onResume = { viewModel.resumeTts() },
                                                            onStop = { viewModel.stopTts() },
                                                            iconSize = 16.dp,
                                                            buttonSize = 28.dp
                                                        )
                                                        CopyIconButton(
                                                            textToCopy = assistantTurn.content,
                                                            iconSize = 16.dp,
                                                            buttonSize = 28.dp,
                                                            tint = MaterialTheme.colorScheme.secondary,
                                                            contentDescription = "複製哲學家推演"
                                                        )
                                                    }
                                                }
                                                SelectionContainer {
                                                    Text(assistantTurn.content, style = MaterialTheme.typography.bodyMedium)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Default Tab 0: 總覽全文
                    else -> {
                        val deductionUtteranceId = "exp_deduction_${selectedExp.id}"
                        val isSpeakingDeduction = isTtsPlaying && currentSpeakingUtteranceId == deductionUtteranceId
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                        ) {
                            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "⚖️ 倫理學派推演報告全文",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    if (deductionResult.isNotBlank() && !isGenerating) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            TtsPlayerControl(
                                                isPlaying = isSpeakingDeduction,
                                                isPaused = isTtsPaused && currentSpeakingUtteranceId == deductionUtteranceId,
                                                onPlay = { viewModel.speak(deductionResult, deductionUtteranceId) },
                                                onPause = { viewModel.pauseTts() },
                                                onResume = { viewModel.resumeTts() },
                                                onStop = { viewModel.stopTts() }
                                            )
                                            CopyIconButton(
                                                textToCopy = deductionResult,
                                                iconSize = 16.dp,
                                                buttonSize = 28.dp,
                                                contentDescription = "複製推演報告全文"
                                            )
                                        }
                                    }
                                }

                                ThinkingContent(
                                    rawText = deductionResult,
                                    isGenerating = isGenerating
                                )

                                if (deductionResult.isNotBlank() && !isGenerating) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (parsedResult.perspectives.isNotEmpty()) {
                                            TextButton(onClick = { selectedTabIndex = 1 }) {
                                                Text("逐派精讀 ➔")
                                            }
                                        }
                                        TextButton(onClick = { selectedTabIndex = followUpTabIdx }) {
                                            Text("前往延伸追問 💬 ➔")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp + bottomContentPadding))
        }
    }

    if (showHistorySheet) {
        HistoryBottomSheet(
            title = "歷史思想實驗推演",
            sessions = historySessions,
            onSelectSession = { record ->
                viewModel.loadSession(record)
                showHistorySheet = false
                isConfigExpanded = false
                selectedTabIndex = 0
            },
            onDeleteSession = viewModel::deleteSession,
            onClearAll = viewModel::clearAllHistory,
            onDismissRequest = { showHistorySheet = false },
            itemBadges = { record ->
                val metaParts = record.metadataJson.split(";;")
                val badgeSource = if (metaParts.size > 2 && metaParts[2].isNotBlank()) metaParts[2]
                else if (metaParts.size > 1 && metaParts[1].isNotBlank()) metaParts[1]
                else ""

                if (badgeSource.isNotBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (badge in badgeSource.split(",")) {
                            if (badge.isNotBlank()) {
                                SuggestionChip(
                                    onClick = {},
                                    label = { Text(badge, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                    }
                }
            }
        )
    }
}
