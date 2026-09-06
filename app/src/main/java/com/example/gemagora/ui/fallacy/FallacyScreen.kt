package com.example.gemagora.ui.fallacy

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.gemagora.ai.PhilosophicalParser
import com.example.gemagora.ui.components.ThinkingContent

import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.example.gemagora.ui.components.HistoryBottomSheet
import com.example.gemagora.ui.components.TtsPlayerControl

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FallacyScreen(
    viewModel: FallacyViewModel,
    onNavigateBack: (() -> Unit)? = null,
    bottomContentPadding: Dp = 0.dp
) {
    val argumentInput by viewModel.argumentInput.collectAsStateWithLifecycle()
    val analysisResult by viewModel.analysisResult.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()
    val currentSessionId by viewModel.currentSessionId.collectAsStateWithLifecycle()

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
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    val parsedResult = remember(analysisResult) {
        PhilosophicalParser.parseFallacy(analysisResult)
    }

    val sampleArguments = listOf(
        "如果你不考上頂尖大學，人生就徹底完了，只能一輩子在底層掙扎。" to "滑坡論證與假二分",
        "你批評這部電影拍得爛，那你自己去拍一部啊？你行你上！" to "訴諸人身 / 偷換舉證責任",
        "大家都在網路上搶買這款保健品，而且名人也在推薦，所以它絕對有效。" to "訴諸群眾與權威",
        "只有兩種人：要麼支持我們的立場，要麼就是我們的敵人。" to "非黑即白 (假二分法)"
    )

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            key(statusBarHeight) {
                TopAppBar(
                    title = {
                        Column {
                            Text("邏輯謬誤診斷室", fontWeight = FontWeight.Bold)
                            Text(
                                text = "論點剖析 · 形式與非形式謬誤",
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
                        IconButton(onClick = { viewModel.startNewAnalysis() }) {
                            Icon(Icons.Default.Add, contentDescription = "開啟新檢驗")
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
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("🔍 輸入欲檢驗之論點或爭論言論", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = argumentInput,
                        onValueChange = { viewModel.setArgumentInput(it) },
                        placeholder = { Text("貼上任何新聞言論、網路爭辯或個人推論…") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        maxLines = 6,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Text("經典謬誤範例快速填入：", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        sampleArguments.forEach { (text, tag) ->
                            SuggestionChip(
                                onClick = { viewModel.setArgumentInput(text) },
                                label = { Text(tag) }
                            )
                        }
                    }

                    Button(
                        onClick = {
                            if (isGenerating) {
                                viewModel.cancelAnalysis()
                            } else {
                                viewModel.analyzeArgument()
                            }
                        },
                        enabled = isGenerating || argumentInput.isNotBlank(),
                        colors = if (isGenerating) {
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        } else {
                            ButtonDefaults.buttonColors()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            if (isGenerating) Icons.Default.Stop else Icons.Default.Troubleshoot,
                            contentDescription = null,
                            Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(if (isGenerating) "停止診斷" else "進行結構化邏輯診斷")
                    }
                }
            }

            if (analysisResult.isNotBlank() || isGenerating) {
                if (parsedResult.isStructured) {
                    // Render Structured Breakdown Cards
                    val fallacyUtteranceId = "fallacy_diagnosis_${currentSessionId}"
                    val isPlayingFallacy = isTtsPlaying && currentSpeakingUtteranceId == fallacyUtteranceId
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📊 論證結構地圖與診斷報告",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (!isGenerating) {
                            val spokenText = remember(parsedResult, analysisResult) {
                                buildString {
                                    append("邏輯謬誤診斷報告。")
                                    if (parsedResult.fallacies.isNotEmpty()) {
                                        append("偵測到的謬誤：")
                                        parsedResult.fallacies.forEach {
                                            append("${it.name}，${it.explanation}。")
                                        }
                                    }
                                    if (!parsedResult.evaluation.isNullOrBlank()) {
                                        append("論證健全性總評：${parsedResult.evaluation}。")
                                    }
                                    if (!parsedResult.counter.isNullOrBlank()) {
                                        append("反思建議：${parsedResult.counter}")
                                    }
                                }.ifBlank { analysisResult }
                            }
                            TtsPlayerControl(
                                isPlaying = isPlayingFallacy,
                                isPaused = isTtsPaused && currentSpeakingUtteranceId == fallacyUtteranceId,
                                onPlay = { viewModel.speak(spokenText, fallacyUtteranceId) },
                                onPause = { viewModel.pauseTts() },
                                onResume = { viewModel.resumeTts() },
                                onStop = { viewModel.stopTts() }
                            )
                        }
                    }

                    // 1. Premises & Conclusion Card
                    if (parsedResult.premises.isNotEmpty() || parsedResult.conclusion != null) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            tonalElevation = 1.dp
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("🧩 論證形式拆解", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

                                parsedResult.premises.forEachIndexed { idx, premise ->
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.Top) {
                                            SuggestionChip(
                                                onClick = {},
                                                label = { Text("前提 P${idx + 1}") },
                                                colors = SuggestionChipDefaults.suggestionChipColors(
                                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                                    labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            )
                                            Spacer(Modifier.width(10.dp))
                                            Text(premise, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f).align(Alignment.CenterVertically))
                                        }
                                    }
                                }

                                if (parsedResult.conclusion != null) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.Top) {
                                            SuggestionChip(
                                                onClick = {},
                                                label = { Text("結論 C") },
                                                colors = SuggestionChipDefaults.suggestionChipColors(
                                                    containerColor = MaterialTheme.colorScheme.secondary,
                                                    labelColor = MaterialTheme.colorScheme.onSecondary
                                                )
                                            )
                                            Spacer(Modifier.width(10.dp))
                                            Text(parsedResult.conclusion, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f).align(Alignment.CenterVertically))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 2. Fallacy Findings
                    if (parsedResult.fallacies.isNotEmpty()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            tonalElevation = 1.dp
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("⚠️ 偵測到的謬誤與無效推論", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

                                parsedResult.fallacies.forEach { item ->
                                    val isSound = item.name.contains("無明顯謬誤") || item.type.contains("健全")
                                    Surface(
                                        color = if (isSound) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                                        else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(item.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                                SuggestionChip(
                                                    onClick = {},
                                                    label = { Text(item.type, style = MaterialTheme.typography.labelSmall) }
                                                )
                                            }
                                            if (item.quote.isNotBlank()) {
                                                Text(
                                                    text = "引述原文：「${item.quote}」",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Text(item.explanation, style = MaterialTheme.typography.bodyMedium)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 3. Evaluation & Soundness
                    if (!parsedResult.evaluation.isNullOrBlank()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.Verified, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    Text("論證健全性 (Soundness) 總評", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                }
                                Text(parsedResult.evaluation, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }

                    // 4. Counter / Revision Question
                    if (!parsedResult.counter.isNullOrBlank()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.Psychology, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    Text("反思思考題與論述修正方向", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                                Text(parsedResult.counter, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }

                // Raw / Thinking collapsible container
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = if (parsedResult.isStructured) "🧠 完整推理與原生標籤紀錄" else "📊 診斷分析進行中",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        ThinkingContent(
                            rawText = analysisResult,
                            isGenerating = isGenerating
                        )
                    }
                }

                // Follow-up Turns
                if (followUpTurns.isNotEmpty()) {
                    Text(
                        text = "💬 接續論證諮詢歷程",
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
                                    Text("追問：", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    Text(userTurn.content, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }

                        if (assistantTurn != null) {
                            val turnUtteranceId = "fallacy_turn_${assistantTurn.id}"
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
                                        Text("導師建議：", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
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
                                    }
                                    Text(assistantTurn.content, style = MaterialTheme.typography.bodyMedium)
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
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Text("導師研擬建議中...", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            }
                            Text(streamingFollowUp ?: "", style = MaterialTheme.typography.bodyMedium)
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
                            text = "💡 接續諮詢：修辭防禦與論證改寫",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                "請提供一段得體但一針見血的一句話反駁話術",
                                "如何將此論述重構為邏輯健全的有效論證？",
                                "若對方繼續偷換概念，我應當如何維持對話主導權？"
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
                                placeholder = { Text("輸入反駁話術諮詢或改寫請求...") },
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
            title = "歷史邏輯診斷紀錄",
            sessions = historySessions,
            onSelectSession = { record ->
                viewModel.loadSession(record)
                showHistorySheet = false
            },
            onDeleteSession = viewModel::deleteSession,
            onClearAll = viewModel::clearAllHistory,
            onDismissRequest = { showHistorySheet = false }
        )
    }
}
