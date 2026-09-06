package com.example.gemagora.ui.journal

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.gemagora.data.model.JournalEntry
import com.example.gemagora.ui.components.ThinkingContent
import com.example.gemagora.ui.components.TtsPlayerControl
import com.example.gemagora.ui.components.CopyIconButton
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import java.text.SimpleDateFormat
import java.util.*

private fun formatGroupDateHeader(dateString: String): String {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = sdf.parse(dateString) ?: return dateString

        val todayStr = sdf.format(Date())
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val yesterdayStr = sdf.format(cal.time)

        val dayOfWeekFormat = SimpleDateFormat("EEEE", Locale.TAIWAN)
        val monthDayFormat = SimpleDateFormat("M 月 d 日", Locale.TAIWAN)
        val yearMonthDayFormat = SimpleDateFormat("yyyy 年 M 月 d 日", Locale.TAIWAN)

        val dayOfWeek = dayOfWeekFormat.format(date)

        when (dateString) {
            todayStr -> "今天 · ${monthDayFormat.format(date)} ($dayOfWeek)"
            yesterdayStr -> "昨天 · ${monthDayFormat.format(date)} ($dayOfWeek)"
            else -> "${yearMonthDayFormat.format(date)} ($dayOfWeek)"
        }
    } catch (e: Exception) {
        dateString
    }
}

private fun formatEntryTime(timestamp: Long): String {
    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalScreen(
    viewModel: JournalViewModel,
    onNavigateBack: (() -> Unit)? = null,
    bottomContentPadding: androidx.compose.ui.unit.Dp = 0.dp
) {
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGeneratingGuidance.collectAsStateWithLifecycle()
    val currentGuidance by viewModel.currentAiGuidance.collectAsStateWithLifecycle()

    val isTtsPlaying by viewModel.isTtsPlaying.collectAsStateWithLifecycle()
    val isTtsPaused by viewModel.isTtsPaused.collectAsStateWithLifecycle()
    val currentSpeakingUtteranceId by viewModel.currentSpeakingUtteranceId.collectAsStateWithLifecycle()

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopTts()
        }
    }

    var showAddDialog by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current

    val generatingEntryId by viewModel.generatingEntryId.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    val groupedEntries = remember(entries) {
        entries.groupBy { it.dateString }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            key(statusBarHeight) {
                TopAppBar(
                    title = {
                        Column {
                            Text("哲學沉思日記", fontWeight = FontWeight.Bold)
                            Text(
                                text = "每日自省 · 斯多葛式智慧沉澱",
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
                        if (entries.isNotEmpty()) {
                            IconButton(onClick = {
                                val md = JournalExporter.exportToMarkdown(entries)
                                JournalExporter.shareMarkdown(context, md)
                            }) {
                                Icon(Icons.Default.Share, contentDescription = "匯出 Markdown")
                            }
                        }
                        IconButton(onClick = { showAddDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "撰寫沉思")
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding() + 8.dp,
                bottom = bottomContentPadding + 24.dp
            )
        ) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "🔒 完全離線安全：所有日記均存放於手機本地端資料庫，不會傳送至任何外部伺服器。AI 導引亦完全由本地 Gemma 模型運算。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (entries.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("尚未有沉思記錄", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("點擊右上角「+」按鈕，開啟今日的斯多葛晨思或夕省。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            val groupList = groupedEntries.entries.toList()
            groupList.forEachIndexed { groupIndex, (dateStr, dayEntries) ->
                // Date Section Header - Groups entries on the same date together
                item(key = "header_$dateStr") {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.65f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = if (groupIndex > 0) 10.dp else 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = formatGroupDateHeader(dateStr),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                            ) {
                                Text(
                                    text = "${dayEntries.size} 則沉思",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                // Journal entries for this date
                items(dayEntries, key = { it.id }) { entry ->
                    val isEntryGenerating = generatingEntryId == entry.id
                    val utteranceId = "journal_guidance_${entry.id}"
                    val isSpeakingGuidance = isTtsPlaying && currentSpeakingUtteranceId == utteranceId
                    val isPausedGuidance = isTtsPaused && currentSpeakingUtteranceId == utteranceId
                    JournalCard(
                        entry = entry,
                        isGenerating = isEntryGenerating,
                        liveGuidance = if (isEntryGenerating) currentGuidance else null,
                        isSpeakingGuidance = isSpeakingGuidance,
                        isPausedGuidance = isPausedGuidance,
                        onPlaySpeak = {
                            val textToSpeak = entry.aiGuidance ?: (if (isEntryGenerating) currentGuidance else null)
                            if (!textToSpeak.isNullOrBlank()) {
                                viewModel.speakGuidance(textToSpeak, entry.id)
                            }
                        },
                        onPauseSpeak = { viewModel.pauseTts() },
                        onResumeSpeak = { viewModel.resumeTts() },
                        onStopSpeak = { viewModel.stopTts() },
                        onCancel = { viewModel.cancelGuidance() },
                        onDelete = { viewModel.deleteEntry(entry) }
                    )
                }

                // Elegant separator divider between different days
                if (groupIndex < groupList.size - 1) {
                    item(key = "divider_$dateStr") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            HorizontalDivider(
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                            )
                            Icon(
                                imageVector = Icons.Default.Spa,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.38f),
                                modifier = Modifier.size(14.dp)
                            )
                            HorizontalDivider(
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        NewJournalDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { title, content, type ->
                viewModel.addEntry(title, content, type)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun JournalCard(
    entry: JournalEntry,
    isGenerating: Boolean = false,
    liveGuidance: String? = null,
    isSpeakingGuidance: Boolean = false,
    isPausedGuidance: Boolean = false,
    onPlaySpeak: (() -> Unit)? = null,
    onPauseSpeak: (() -> Unit)? = null,
    onResumeSpeak: (() -> Unit)? = null,
    onStopSpeak: (() -> Unit)? = null,
    onCancel: (() -> Unit)? = null,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val displayGuidance = entry.aiGuidance ?: (if (isGenerating) liveGuidance else null)

    // User's diary content folding state (fold if > 3 lines)
    var isContentExpanded by remember(entry.id) { mutableStateOf(false) }
    var canExpandContent by remember(entry.id) { mutableStateOf(false) }

    // AI Guidance folding state (fold long guidance by default to prevent screen clutter)
    val isGuidanceLong = remember(displayGuidance) {
        (displayGuidance?.length ?: 0) > 130
    }
    var isGuidanceExpanded by remember(entry.id) {
        mutableStateOf(isGenerating || !isGuidanceLong)
    }

    LaunchedEffect(isGenerating) {
        if (isGenerating) {
            isGuidanceExpanded = true
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .padding(18.dp)
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header: Title and delete button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (entry.entryType == "morning") Icons.Default.WbSunny else Icons.Default.Nightlight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = entry.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CopyIconButton(
                        textToCopy = entry.content,
                        iconSize = 16.dp,
                        buttonSize = 28.dp,
                        contentDescription = "複製日記內容"
                    )
                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "刪除",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // Entry Type Chip & Time
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val typeLabel = when (entry.entryType) {
                    "morning" -> "斯多葛晨思"
                    "evening" -> "斯多葛夕省"
                    else -> "哲學省思"
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
                ) {
                    Text(
                        text = typeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
                Text(
                    text = formatEntryTime(entry.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // User Reflection Content with Fold/Expand Toggle
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
            ) {
                SelectionContainer {
                    Text(
                        text = entry.content,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = if (isContentExpanded) Int.MAX_VALUE else 3,
                        overflow = TextOverflow.Ellipsis,
                        onTextLayout = { textLayoutResult ->
                            if (!isContentExpanded && textLayoutResult.hasVisualOverflow) {
                                canExpandContent = true
                            }
                        }
                    )
                }
                if (canExpandContent) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { isContentExpanded = !isContentExpanded }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = if (isContentExpanded) "收起日記全文 ▴" else "展開日記全文 ▾",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // AI Guidance Container with Fold/Expand Toggle
            if (isGenerating && displayGuidance.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "🌿 本地 Gemma 正在沉思斯多葛哲思導引…",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (onCancel != null) {
                            IconButton(onClick = onCancel, modifier = Modifier.size(28.dp)) {
                                Icon(
                                    Icons.Default.Stop,
                                    contentDescription = "停止導引",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            } else if (!displayGuidance.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .padding(12.dp)
                            .animateContentSize(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // AI Guidance Header Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = isGuidanceLong && !isGenerating) {
                                    isGuidanceExpanded = !isGuidanceExpanded
                                },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    "本地 AI 哲思導引",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (!isGenerating && onPlaySpeak != null) {
                                    TtsPlayerControl(
                                        isPlaying = isSpeakingGuidance,
                                        isPaused = isPausedGuidance,
                                        onPlay = onPlaySpeak,
                                        onPause = onPauseSpeak ?: {},
                                        onResume = onResumeSpeak ?: {},
                                        onStop = onStopSpeak ?: {},
                                        iconSize = 16.dp,
                                        buttonSize = 28.dp
                                    )
                                    CopyIconButton(
                                        textToCopy = displayGuidance ?: "",
                                        iconSize = 16.dp,
                                        buttonSize = 28.dp,
                                        contentDescription = "複製哲思導引"
                                    )
                                }
                                if (isGenerating && onCancel != null) {
                                    IconButton(onClick = onCancel, modifier = Modifier.size(24.dp)) {
                                        Icon(
                                            Icons.Default.Stop,
                                            contentDescription = "停止導引",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                                if (isGuidanceLong && !isGenerating) {
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                        modifier = Modifier.clickable {
                                            isGuidanceExpanded = !isGuidanceExpanded
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Text(
                                                text = if (isGuidanceExpanded) "收起" else "展開",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Icon(
                                                imageVector = if (isGuidanceExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Guidance Content: Expanded view vs. Collapsed preview
                        if (isGuidanceExpanded || !isGuidanceLong) {
                            ThinkingContent(
                                rawText = displayGuidance,
                                isGenerating = isGenerating
                            )
                            if (isGuidanceLong && !isGenerating) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(4.dp))
                                        .clickable { isGuidanceExpanded = false }
                                        .padding(top = 4.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "收起哲思導引 ▲",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        } else {
                            val cleanPreview = remember(displayGuidance) {
                                displayGuidance
                                    .replace(Regex("<\\|?channel\\|?>[a-zA-Z0-9_]*"), "")
                                    .trim()
                            }
                            Text(
                                text = cleanPreview,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable { isGuidanceExpanded = true }
                                    .padding(top = 2.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "展開閱讀哲思導引 ▼",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("確定刪除此沉思日記？", fontWeight = FontWeight.Bold) },
            text = { Text("刪除後將無法復原此筆日記及其 AI 哲思導引內容。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("確定刪除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun NewJournalDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    var selectedType by remember { mutableStateOf("morning") }
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("記錄哲學沉思", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = selectedType == "morning",
                        onClick = { selectedType = "morning" },
                        shape = SegmentedButtonDefaults.itemShape(0, 2)
                    ) {
                        Text("晨思")
                    }
                    SegmentedButton(
                        selected = selectedType == "evening",
                        onClick = { selectedType = "evening" },
                        shape = SegmentedButtonDefaults.itemShape(1, 2)
                    ) {
                        Text("夕省")
                    }
                }

                Text(
                    text = if (selectedType == "morning") "斯多葛晨思：今天我可能面臨何種不可控事物？我該如何保有內在德性？"
                    else "斯多葛夕省：今天我何處做到了平靜？何處仍受情緒牽引？",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("標題（可選）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("傾訴你的反思思緒…") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    maxLines = 8
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(title, content, selectedType) },
                enabled = content.isNotBlank()
            ) {
                Text("保存並尋求 AI 哲思指引")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
