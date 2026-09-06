package com.example.gemagora.ui.roundtable

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.gemagora.ai.PhilosophicalParser
import com.example.gemagora.ui.components.ThinkingContent
import com.example.gemagora.ui.components.TtsPlayerControl
import com.example.gemagora.ui.components.CopyIconButton
import androidx.compose.foundation.text.selection.SelectionContainer

import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.example.gemagora.ui.components.HistoryBottomSheet

enum class RoundTableSection(val label: String, val icon: ImageVector) {
    SPEECHES("各派立論", Icons.Default.RecordVoiceOver),
    CROSS_EXAM("針鋒交鋒", Icons.AutoMirrored.Filled.CompareArrows),
    SYNTHESIS("辯證合一", Icons.Default.Balance)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoundTableScreen(
    viewModel: RoundTableViewModel,
    onNavigateBack: (() -> Unit)? = null,
    bottomContentPadding: androidx.compose.ui.unit.Dp = 0.dp
) {
    val topic by viewModel.topic.collectAsStateWithLifecycle()
    val selectedSchools by viewModel.selectedSchools.collectAsStateWithLifecycle()
    val debateResult by viewModel.debateResult.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()

    val historySessions by viewModel.historySessions.collectAsStateWithLifecycle()
    val followUpTurns by viewModel.followUpTurns.collectAsStateWithLifecycle()
    val streamingFollowUp by viewModel.streamingFollowUp.collectAsStateWithLifecycle()
    val isGeneratingFollowUp by viewModel.isGeneratingFollowUp.collectAsStateWithLifecycle()
    val isTtsPlaying by viewModel.isTtsPlaying.collectAsStateWithLifecycle()
    val isTtsPaused by viewModel.isTtsPaused.collectAsStateWithLifecycle()
    val currentSpeakingUtteranceId by viewModel.currentSpeakingUtteranceId.collectAsStateWithLifecycle()
    val isDebateSequencerActive by viewModel.isDebateSequencerActive.collectAsStateWithLifecycle()

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopTts()
        }
    }

    var showHistorySheet by remember { mutableStateOf(false) }
    var followUpInput by remember { mutableStateOf("") }
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    val parsedResult = remember(debateResult) {
        PhilosophicalParser.parseRoundTable(debateResult)
    }

    var topicInput by remember(topic) { mutableStateOf(topic) }
    var isConfigExpanded by remember(debateResult.isEmpty()) { mutableStateOf(debateResult.isEmpty()) }
    var selectedSection by remember { mutableStateOf(RoundTableSection.SPEECHES) }
    var isThinkingExpanded by remember { mutableStateOf(false) }

    // Auto switch to corresponding section during streaming if user hasn't manually navigated
    LaunchedEffect(parsedResult.isStructured, parsedResult.crossExamItems.size, parsedResult.synthesis) {
        if (isGenerating) {
            when {
                parsedResult.synthesis != null -> selectedSection = RoundTableSection.SYNTHESIS
                parsedResult.crossExamItems.isNotEmpty() || !parsedResult.crossExamination.isNullOrBlank() ->
                    selectedSection = RoundTableSection.CROSS_EXAM
            }
        }
    }

    val presetTopics = remember {
        listOf(
            "面對人生的焦慮、無常與困境，我們應當如何自處？",
            "痛苦是否具有正面價值？還是純粹該被消滅？",
            "科技與 AI 發展是否正在異化人的自由與本質？",
            "人生的終極意義是客觀存在的，還是主觀賦予的？",
            "愛是一種美德，還是一種脆弱的執念？",
            "如果沒有絕對的自由意志，道德責任還能成立嗎？",
            "為了追求群體多數的幸福，少數人的權益可以被犧牲嗎？",
            "在荒謬的世界中，死亡是解脫還是終極的反抗？"
        )
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            key(statusBarHeight) {
                TopAppBar(
                    title = {
                        Column {
                            Text("多學派圓桌思辨", fontWeight = FontWeight.Bold)
                            Text(
                                text = "大師交鋒 · 正反合辯證",
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
                            viewModel.startNewTopic()
                            isConfigExpanded = true
                        }) {
                            Icon(Icons.Default.Add, contentDescription = "開啟新話題")
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
            // Collapsed summary capsule when results are available
            if (debateResult.isNotBlank()) {
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
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "💡 議題：$topicInput",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "出席論壇（${selectedSchools.size}派）：" + selectedSchools.joinToString("、") { it.split(" ").first() },
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

            // Full Configuration Panel (Animated collapsible)
            AnimatedVisibility(
                visible = isConfigExpanded || debateResult.isBlank(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Topic Card
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer
                    ) {
                        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("💡 探討核心議題", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            OutlinedTextField(
                                value = topicInput,
                                onValueChange = {
                                    topicInput = it
                                    viewModel.setTopic(it)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("請輸入思辨主題，或於下方點選推薦主題...") },
                                maxLines = 3,
                                shape = RoundedCornerShape(12.dp)
                            )

                            Text("推薦思辨主題（左右滑動挑選）：", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            val row1Topics = remember(presetTopics) { presetTopics.filterIndexed { idx, _ -> idx % 2 == 0 } }
                            val row2Topics = remember(presetTopics) { presetTopics.filterIndexed { idx, _ -> idx % 2 != 0 } }
                            val topicScrollState = rememberScrollState()

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(topicScrollState),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    row1Topics.forEach { p ->
                                        val isSelected = topicInput == p
                                        SuggestionChip(
                                            onClick = {
                                                topicInput = p
                                                viewModel.setTopic(p)
                                            },
                                            label = {
                                                Text(
                                                    text = p,
                                                    style = MaterialTheme.typography.labelMedium,
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
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    row2Topics.forEach { p ->
                                        val isSelected = topicInput == p
                                        SuggestionChip(
                                            onClick = {
                                                topicInput = p
                                                viewModel.setTopic(p)
                                            },
                                            label = {
                                                Text(
                                                    text = p,
                                                    style = MaterialTheme.typography.labelMedium,
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

                    // Schools Selector
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("👥 邀請出席論壇之學派", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("勾選 2 個以上學派，觀察大師間的觀點交鋒與批判：", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                viewModel.availableSchools.forEach { school ->
                                    val isSelected = selectedSchools.contains(school)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { viewModel.toggleSchool(school) },
                                        label = { Text(school) }
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    if (isGenerating) {
                                        viewModel.cancelRoundTable()
                                    } else {
                                        isConfigExpanded = false
                                        viewModel.startRoundTable()
                                    }
                                },
                                enabled = isGenerating || topicInput.isNotBlank(),
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
                                    if (isGenerating) Icons.Default.Stop else Icons.Default.Forum,
                                    contentDescription = null,
                                    Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(if (isGenerating) "停止圓桌思辨生成" else "召開思想大會與正反合辯證")
                            }
                        }
                    }
                }
            }

            // Debate Result Presentation
            if (debateResult.isNotBlank() || isGenerating) {
                if (parsedResult.isStructured) {
                    // Segmented Tabs Header
                    PrimaryTabRow(
                        selectedTabIndex = selectedSection.ordinal,
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        contentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clip(RoundedCornerShape(14.dp))
                    ) {
                        RoundTableSection.entries.forEach { section ->
                            val countBadge = when (section) {
                                RoundTableSection.SPEECHES -> parsedResult.speeches.size.takeIf { it > 0 }
                                RoundTableSection.CROSS_EXAM -> (parsedResult.crossExamItems.size.takeIf { it > 0 }
                                    ?: if (!parsedResult.crossExamination.isNullOrBlank()) 1 else null)
                                RoundTableSection.SYNTHESIS -> if (parsedResult.synthesis != null) 1 else null
                            }
                            Tab(
                                selected = selectedSection == section,
                                onClick = { selectedSection = section },
                                modifier = Modifier.height(52.dp)
                            ) {
                                Icon(
                                    imageVector = section.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = section.label,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (selectedSection == section) FontWeight.Bold else FontWeight.Normal
                                    )
                                    if (countBadge != null) {
                                        Badge(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                                            Text(countBadge.toString())
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (parsedResult.speeches.isNotEmpty() && !isGenerating) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isDebateSequencerActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                    else MaterialTheme.colorScheme.surfaceContainerHigh,
                            border = BorderStroke(1.dp, if (isDebateSequencerActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = if (isDebateSequencerActive) Icons.Default.SpatialAudio else Icons.AutoMirrored.Filled.VolumeUp,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            text = if (isDebateSequencerActive) {
                                                if (isTtsPaused) "圓桌廣播暫停中…" else "圓桌連環廣播進行中…"
                                            } else {
                                                "連續聆聽整場圓桌思辨"
                                            },
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "各哲學學派以不同聲線依序立論、針鋒交鋒與辯證合一",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                if (isDebateSequencerActive) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        if (isTtsPaused) {
                                            IconButton(onClick = { viewModel.resumeTts() }, modifier = Modifier.size(32.dp)) {
                                                Icon(Icons.Default.PlayArrow, contentDescription = "繼續廣播", tint = MaterialTheme.colorScheme.primary)
                                            }
                                        } else {
                                            IconButton(onClick = { viewModel.pauseTts() }, modifier = Modifier.size(32.dp)) {
                                                Icon(Icons.Default.Pause, contentDescription = "暫停廣播", tint = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                        IconButton(onClick = { viewModel.stopTts() }, modifier = Modifier.size(32.dp)) {
                                            Icon(Icons.Default.Stop, contentDescription = "停止廣播", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                } else {
                                    Button(
                                        onClick = { viewModel.playFullDebate(parsedResult) },
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("開始廣播", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }
                    }

                    // Section Content Display
                    when (selectedSection) {
                        RoundTableSection.SPEECHES -> {
                            if (parsedResult.speeches.isEmpty()) {
                                CardPlaceholder("各大派大師正在就座並醞釀立論發言…")
                            } else {
                                parsedResult.speeches.forEach { speech ->
                                    val speechId = "school_${speech.schoolName.hashCode()}"
                                    val isThisSpeaking = isTtsPlaying && currentSpeakingUtteranceId == speechId
                                    val isThisPaused = isTtsPaused && currentSpeakingUtteranceId == speechId
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(18.dp),
                                        color = if (isThisSpeaking || isThisPaused) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceContainer,
                                        border = if (isThisSpeaking || isThisPaused) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                                        tonalElevation = 1.dp
                                    ) {
                                        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    Surface(
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = MaterialTheme.colorScheme.primaryContainer
                                                    ) {
                                                        Text(
                                                            text = speech.schoolName,
                                                            style = MaterialTheme.typography.labelMedium,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                                        )
                                                    }
                                                    val displayMaster = remember(speech.masterName) {
                                                        formatMasterName(speech.masterName)
                                                    }
                                                    Text(
                                                        text = "主講代表：$displayMaster",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }

                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    TtsPlayerControl(
                                                        isPlaying = isThisSpeaking,
                                                        isPaused = isThisPaused,
                                                        onPlay = { viewModel.speakSchool(speech) },
                                                        onPause = { viewModel.pauseTts() },
                                                        onResume = { viewModel.resumeTts() },
                                                        onStop = { viewModel.stopTts() },
                                                        iconSize = 18.dp,
                                                        buttonSize = 32.dp
                                                    )
                                                    CopyIconButton(
                                                        textToCopy = speech.content,
                                                        iconSize = 18.dp,
                                                        buttonSize = 32.dp,
                                                        contentDescription = "複製${speech.schoolName}立論"
                                                    )
                                                }
                                            }
                                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                            SelectionContainer {
                                                Text(speech.content, style = MaterialTheme.typography.bodyMedium)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        RoundTableSection.CROSS_EXAM -> {
                            if (parsedResult.crossExamItems.isNotEmpty()) {
                                parsedResult.crossExamItems.forEachIndexed { idx, item ->
                                    val crossId = "cross_$idx"
                                    val isCrossSpeaking = isTtsPlaying && currentSpeakingUtteranceId == crossId
                                    val isCrossPaused = isTtsPaused && currentSpeakingUtteranceId == crossId
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(18.dp),
                                        color = if (isCrossSpeaking || isCrossPaused) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceContainer,
                                        border = BorderStroke(if (isCrossSpeaking || isCrossPaused) 1.5.dp else 1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = if (isCrossSpeaking || isCrossPaused) 0.8f else 0.35f)),
                                        tonalElevation = 1.dp
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Icon(
                                                        Icons.AutoMirrored.Filled.CompareArrows,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.secondary,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                    val cleanChallenger = item.challenger.replace(Regex("\\(.*\\)"), "").removePrefix("質疑").removePrefix("發言").trim()
                                                    val cleanTarget = item.target?.replace(Regex("\\(.*\\)"), "")?.removePrefix("被質疑")?.removePrefix("質疑")?.trim()
                                                    Text(
                                                        text = buildString {
                                                             append(cleanChallenger)
                                                            if (!cleanTarget.isNullOrBlank()) {
                                                                append(" ⚔️ 質疑 ")
                                                                append(cleanTarget)
                                                            } else {
                                                                append(" ⚔️ 觀點交鋒")
                                                            }
                                                        },
                                                        style = MaterialTheme.typography.titleSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.secondary
                                                    )
                                                }

                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    TtsPlayerControl(
                                                        isPlaying = isCrossSpeaking,
                                                        isPaused = isCrossPaused,
                                                        onPlay = { viewModel.speakCrossExam(item, idx) },
                                                        onPause = { viewModel.pauseTts() },
                                                        onResume = { viewModel.resumeTts() },
                                                        onStop = { viewModel.stopTts() },
                                                        iconSize = 18.dp,
                                                        buttonSize = 32.dp
                                                    )
                                                    CopyIconButton(
                                                        textToCopy = item.content,
                                                        iconSize = 18.dp,
                                                        buttonSize = 32.dp,
                                                        contentDescription = "複製交鋒質疑"
                                                    )
                                                }
                                            }
                                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                                            SelectionContainer {
                                                Text(item.content, style = MaterialTheme.typography.bodyMedium)
                                            }
                                        }
                                    }
                                }
                            } else if (!parsedResult.crossExamination.isNullOrBlank()) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(18.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f))
                                ) {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Icon(Icons.AutoMirrored.Filled.CompareArrows, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                                                Text("⚔️ 各派交鋒與盲點質疑", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                            }
                                            CopyIconButton(
                                                textToCopy = parsedResult.crossExamination,
                                                iconSize = 18.dp,
                                                buttonSize = 32.dp,
                                                contentDescription = "複製交鋒總覽"
                                            )
                                        }
                                        SelectionContainer {
                                            Text(parsedResult.crossExamination, style = MaterialTheme.typography.bodyMedium)
                                        }
                                    }
                                }
                            } else {
                                CardPlaceholder("各大派思想交鋒與盲點詰問尚未展開…")
                            }
                        }

                        RoundTableSection.SYNTHESIS -> {
                            if (!parsedResult.synthesis.isNullOrBlank()) {
                                val isSynthesisSpeaking = isTtsPlaying && currentSpeakingUtteranceId == "synthesis_summary"
                                val isSynthesisPaused = isTtsPaused && currentSpeakingUtteranceId == "synthesis_summary"
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(18.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (isSynthesisSpeaking || isSynthesisPaused) 0.65f else 0.45f),
                                    border = BorderStroke(if (isSynthesisSpeaking || isSynthesisPaused) 2.dp else 1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = if (isSynthesisSpeaking || isSynthesisPaused) 0.9f else 0.6f))
                                ) {
                                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(Icons.Default.Balance, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                                                Text("⚖️ 辯證正反合 (Synthesis) 與生活智慧", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                            }

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                TtsPlayerControl(
                                                    isPlaying = isSynthesisSpeaking,
                                                    isPaused = isSynthesisPaused,
                                                    onPlay = { viewModel.speakSynthesis(parsedResult.synthesis) },
                                                    onPause = { viewModel.pauseTts() },
                                                    onResume = { viewModel.resumeTts() },
                                                    onStop = { viewModel.stopTts() },
                                                    iconSize = 18.dp,
                                                    buttonSize = 32.dp
                                                )
                                                CopyIconButton(
                                                    textToCopy = parsedResult.synthesis,
                                                    iconSize = 18.dp,
                                                    buttonSize = 32.dp,
                                                    contentDescription = "複製辯證結論"
                                                )
                                            }
                                        }
                                        HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                                        SelectionContainer {
                                            Text(parsedResult.synthesis, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Normal)
                                        }
                                    }
                                }
                            } else {
                                CardPlaceholder("主持人尚未提煉最終辯證智慧…")
                            }
                        }
                    }
                } else {
                    // Ongoing initial streaming before first structured block completes
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        tonalElevation = 1.dp
                    ) {
                        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                Text("📜 哲學大師正在就座並展開深層推演…", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            }
                            ThinkingContent(
                                rawText = debateResult,
                                isGenerating = isGenerating
                            )
                        }
                    }
                }

                // Expandable Raw / Thinking Container (Drawer Style, default collapsed)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                    onClick = { isThinkingExpanded = !isThinkingExpanded }
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Psychology, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                                Text(
                                    text = if (parsedResult.isStructured) "完整推理推論與原生標籤 (Debug)" else "📜 原生思辨輸出串流",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                if (isThinkingExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (isThinkingExpanded) "收合" else "展開",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (isThinkingExpanded) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                            ThinkingContent(
                                rawText = debateResult,
                                isGenerating = isGenerating
                            )
                        }
                    }
                }

                // Follow-up Inquiry History & Continuation
                if (followUpTurns.isNotEmpty()) {
                    Text(
                        text = "💬 接續思辨討論歷程",
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
                                        Text("追問：", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
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
                            val turnUtteranceId = "followup_${assistantTurn.id}"
                            val isTurnSpeaking = isTtsPlaying && currentSpeakingUtteranceId == turnUtteranceId
                            val isTurnPaused = isTtsPaused && currentSpeakingUtteranceId == turnUtteranceId
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isTurnSpeaking || isTurnPaused) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                                border = if (isTurnSpeaking || isTurnPaused) BorderStroke(1.dp, MaterialTheme.colorScheme.secondary) else null,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("大師回覆：", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            TtsPlayerControl(
                                                isPlaying = isTurnSpeaking,
                                                isPaused = isTurnPaused,
                                                onPlay = { viewModel.speakFollowUpTurn(assistantTurn.id, assistantTurn.content) },
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
                                                contentDescription = "複製大師回覆"
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
                                    Text("大師思辨研擬中...", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
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

                // Follow-up Input Card
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "💡 繼續向圓桌追問",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )

                        // Quick suggestions
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                "請讓斯多葛學派針對此結論提出反思",
                                "如何將此辯證轉化為現代日常實踐？",
                                "面對不可抗力之無常，存在主義與道家如何妥協？"
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
                                placeholder = { Text("輸入進一步追問或假設情境...") },
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
            }
            Spacer(Modifier.height(16.dp + bottomContentPadding))
        }
    }

    if (showHistorySheet) {
        HistoryBottomSheet(
            title = "歷史圓桌思辨紀錄",
            sessions = historySessions,
            onSelectSession = { record ->
                viewModel.loadSession(record)
                showHistorySheet = false
            },
            onDeleteSession = viewModel::deleteSession,
            onClearAll = viewModel::clearAllHistory,
            onDismissRequest = { showHistorySheet = false },
            itemBadges = { record ->
                if (record.metadataJson.isNotBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (s in record.metadataJson.split(";;")) {
                            if (s.isNotBlank()) {
                                SuggestionChip(
                                    onClick = {},
                                    label = { Text(s.split(" (").first(), style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                    }
                }
            }
        )
    }
}

@Composable
private fun CardPlaceholder(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
    ) {
        Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
            Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun formatMasterName(masterName: String): String {
    // 1. 如果包含括號中的代表人物 (例如 "斯多葛學派 (馬可·奧理略 / 愛比克泰德)代表大師")，優先提取括號內大師姓名
    val base = Regex("\\((.*?)\\)").find(masterName)?.groupValues?.get(1)?.trim()
        ?: masterName.replace("代表大師", "大師").trim().ifBlank { masterName }
    // 2. 將模型偶爾輸出之底線符號替換為規範之間隔號或斜線
    return base.replace("_或_", " / ")
        .replace("_", "·")
}

