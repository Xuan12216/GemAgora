package com.example.gemagora.ui.setup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.gemagora.data.model.ModelDownloadState
import com.example.gemagora.data.model.ModelLoadState
import com.example.gemagora.ui.components.LoadingDialog
import com.example.gemagora.ui.components.ModelStatusCard
import com.example.gemagora.ui.components.SettingsCard

private data class SettingTabItem(
    val title: String,
    val icon: ImageVector,
    val statusColor: Color? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: ModelSetupViewModel,
    onNavigateBack: () -> Unit,
    initialTab: Int = 0
) {
    val models by viewModel.allModels.collectAsStateWithLifecycle()
    val downloadStates by viewModel.downloadStates.collectAsStateWithLifecycle()
    val gemmaLoadState by viewModel.gemmaLoadState.collectAsStateWithLifecycle()
    val loadedModelName by viewModel.loadedModelName.collectAsStateWithLifecycle()
    val applying by viewModel.isApplyingModelConfig.collectAsStateWithLifecycle()
    val appearance by viewModel.appearance.collectAsStateWithLifecycle()
    val contextTokens by viewModel.maxTokens.collectAsStateWithLifecycle()
    val ttsSettings by viewModel.ttsSettings.collectAsStateWithLifecycle()
    val availableVoices by viewModel.availableVoices.collectAsStateWithLifecycle()
    val isTtsPlaying by viewModel.isTtsPlaying.collectAsStateWithLifecycle()
    val isTtsReady by viewModel.isTtsReady.collectAsStateWithLifecycle()
    val securitySettings by viewModel.securitySettings.collectAsStateWithLifecycle()

    var selectedTab by rememberSaveable { mutableIntStateOf(initialTab.coerceIn(0, 3)) }
    var selectedImportId by rememberSaveable { mutableStateOf<String?>(null) }
    var showConfig by rememberSaveable { mutableStateOf(false) }

    val appearanceScroll = rememberScrollState()
    val modelsScroll = rememberScrollState()
    val ttsScroll = rememberScrollState()
    val securityScroll = rememberScrollState()

    val currentScrollState = when (selectedTab) {
        0 -> appearanceScroll
        1 -> modelsScroll
        2 -> ttsScroll
        else -> securityScroll
    }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val modelId = selectedImportId
        if (uri != null && modelId != null) viewModel.importLocalFile(modelId, uri)
    }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    // 當切換分頁時，若 TopAppBar 為收起狀態，自動展開
    LaunchedEffect(selectedTab) {
        if (scrollBehavior.state.heightOffset != 0f) {
            scrollBehavior.state.heightOffset = 0f
        }
    }

    val tabs = remember(appearance, loadedModelName, gemmaLoadState, isTtsReady, securitySettings) {
        listOf(
            SettingTabItem(
                title = "外觀與顯示",
                icon = Icons.Default.Palette,
                statusColor = appearance?.customHue?.let { Color.hsl(it.toFloat(), 0.65f, 0.45f) }
            ),
            SettingTabItem(
                title = "語言模型",
                icon = Icons.Default.SmartToy,
                statusColor = when {
                    loadedModelName != null -> Color(0xFF10B981)
                    gemmaLoadState is ModelLoadState.Loading -> Color(0xFFF59E0B)
                    else -> null
                }
            ),
            SettingTabItem(
                title = "語音朗讀",
                icon = Icons.Default.RecordVoiceOver,
                statusColor = if (isTtsReady && ttsSettings.enabled) Color(0xFF10B981) else null
            ),
            SettingTabItem(
                title = "隱私與安全",
                icon = Icons.Default.Security,
                statusColor = if (securitySettings.isAppLockEnabled) Color(0xFF10B981) else null
            )
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
                            Text("系統與偏好設定", fontWeight = FontWeight.Bold)
                            Text(
                                text = "外觀 · 模型 · 語音 · 隱私與安全",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
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
                .imePadding()
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // 參考蘇格拉底反詰法：當 TopAppBar 向上收合時，選項列同步收合 (AnimatedVisibility)
                val isTopBarCollapsed = scrollBehavior.state.collapsedFraction > 0.08f
                val showTabs = !isTopBarCollapsed

                AnimatedVisibility(
                    visible = showTabs,
                    enter = fadeIn(tween(180)) + expandVertically(tween(220)),
                    exit = fadeOut(tween(140)) + shrinkVertically(tween(180))
                ) {
                    Column {
                        SecondaryScrollableTabRow(
                            selectedTabIndex = selectedTab,
                            edgePadding = 16.dp,
                            containerColor = MaterialTheme.colorScheme.background,
                            divider = {}
                        ) {
                            tabs.forEachIndexed { index, tabItem ->
                                val isSelected = selectedTab == index
                                Tab(
                                    selected = isSelected,
                                    onClick = { selectedTab = index },
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = tabItem.icon,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = tabItem.title,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                            if (tabItem.statusColor != null) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(7.dp)
                                                        .clip(CircleShape)
                                                        .background(tabItem.statusColor)
                                                )
                                            }
                                        }
                                    }
                                )
                            }
                        }
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                            thickness = 0.5.dp
                        )
                    }
                }

                // 內容滾動區：當頂部收合後，padding.calculateTopPadding() 降為 0，畫面直接延展顯示在 statusBar 之下
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(currentScrollState)
                        .padding(
                            top = 16.dp,
                            bottom = navBarPadding + 28.dp,
                            start = 18.dp,
                            end = 18.dp
                        ),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 頂部模組概況 Hero 摘要卡片
                    SettingsHeroBanner(
                        selectedTab = selectedTab,
                        appearance = appearance,
                        loadedModelName = loadedModelName,
                        contextTokens = contextTokens,
                        ttsSettings = ttsSettings,
                        isTtsReady = isTtsReady,
                        securitySettings = securitySettings
                    )

            // 各分頁內容區
            when (selectedTab) {
                // Tab 0: 外觀與顯示
                0 -> {
                    appearance?.let {
                        AppearanceSettingsCard(it, viewModel::saveAppearance)
                    }
                }

                // Tab 1: 語言模型
                1 -> {
                    SettingsCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "推論引擎參數概況",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "$contextTokens tokens",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Text(
                            text = "大容量（8K-32K）可容納長篇倫理推演與多學派交鋒；小容量可節省設備記憶體消耗。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedButton(
                            onClick = { showConfig = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Tune, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("調整模型參數與蘇格拉底提示詞")
                        }
                    }

                    if (gemmaLoadState is ModelLoadState.Loading) {
                        SettingsCard {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.5.dp,
                                    color = Color(0xFFF59E0B)
                                )
                                Column {
                                    Text(
                                        text = "地端推論引擎背景暖機中…",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "⚡ GPU 運算核心與權重配置中，完成後將自動就緒。",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    val failure = (gemmaLoadState as? ModelLoadState.Failed)?.message
                    if (failure != null) {
                        SettingsCard {
                            Text(
                                text = "模型載入失敗",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = failure,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Storage,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "可用本地端模型庫",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "共 ${models.size} 款",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    models.forEach { model ->
                        ModelStatusCard(
                            model = model,
                            downloadState = downloadStates[model.id] ?: ModelDownloadState.Idle,
                            loadState = gemmaLoadState,
                            onDownloadClick = { viewModel.downloadModel(model.id) },
                            onLoadClick = { viewModel.loadModel(model) },
                            onImportClick = {
                                selectedImportId = model.id
                                filePicker.launch("*/*")
                            },
                            isActive = loadedModelName == model.name
                        )
                    }
                }

                // Tab 2: 語音朗讀
                2 -> {
                    TtsSettingsCard(
                        settings = ttsSettings,
                        availableVoices = availableVoices,
                        isPlaying = isTtsPlaying,
                        isReady = isTtsReady,
                        onSettingsChange = viewModel::saveTtsSettings,
                        onPreviewClick = viewModel::previewTts,
                        onStopClick = viewModel::stopTts
                    )
                }

                // Tab 3: 隱私與安全
                3 -> {
                    SecuritySettingsCard(
                        settings = securitySettings,
                        onVerifyPin = viewModel::verifyPin,
                        onSavePin = viewModel::setPin,
                        onToggleAppLock = viewModel::setAppLockEnabled,
                        onToggleBiometric = viewModel::setBiometricEnabled,
                        onSetAutoLockTimeout = viewModel::setAutoLockTimeout
                    )
                }
            }
        }
    }
}
}

    if (showConfig) ModelConfigDialog(viewModel) { showConfig = false }
    if (applying) LoadingDialog(
        title = "套用模型設定中",
        message = "正在以新的推論參數重新載入地端模型…"
    )
}

@Composable
private fun SettingsHeroBanner(
    selectedTab: Int,
    appearance: com.example.gemagora.data.model.AppearanceSettings?,
    loadedModelName: String?,
    contextTokens: Int,
    ttsSettings: com.example.gemagora.data.model.TtsSettings,
    isTtsReady: Boolean,
    securitySettings: com.example.gemagora.data.model.SecuritySettings
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (selectedTab) {
                            0 -> Icons.Default.Palette
                            1 -> Icons.Default.SmartToy
                            2 -> Icons.Default.RecordVoiceOver
                            else -> Icons.Default.Security
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when (selectedTab) {
                            0 -> "外觀自訂與視覺排版"
                            1 -> "地端 Gemma 4 AI 推論"
                            2 -> "古典哲學語音朗讀"
                            else -> "隱私防護與生物辨識"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = when (selectedTab) {
                            0 -> "精選哲學調色盤、字級縮放與深淺色主題"
                            1 -> "100% 裝置離線運行，保障思辨私密性"
                            2 -> "蘇格拉底與阿斯帕齊婭聲線、音調語速微調"
                            else -> "密碼鎖定、指紋感應解鎖與自動逾時保護"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Quick Status Chips
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                when (selectedTab) {
                    0 -> {
                        val themeLabel = when (appearance?.themeMode) {
                            com.example.gemagora.data.model.ThemeMode.DARK -> "深色模式"
                            com.example.gemagora.data.model.ThemeMode.LIGHT -> "淺色模式"
                            else -> "跟隨系統"
                        }
                        StatusChip(label = themeLabel)
                        appearance?.fontScale?.let {
                            StatusChip(label = "字級 ${(it * 100).toInt()}%")
                        }
                    }
                    1 -> {
                        StatusChip(
                            label = loadedModelName ?: "未載入模型",
                            isAccent = loadedModelName != null
                        )
                        StatusChip(label = "$contextTokens tokens")
                    }
                    2 -> {
                        StatusChip(
                            label = if (ttsSettings.enabled) "語音朗讀中" else "語音已關閉",
                            isAccent = ttsSettings.enabled
                        )
                        val preset = com.example.gemagora.data.model.TtsSettings.findPreset(ttsSettings.presetId)
                        StatusChip(label = "聲線: ${preset.name}")
                    }
                    else -> {
                        if (securitySettings.isAppLockEnabled) {
                            StatusChip(label = "密碼保護中", isAccent = true)
                            if (securitySettings.isBiometricEnabled) {
                                StatusChip(label = "指紋已開啟", isAccent = true)
                            }
                            StatusChip(label = securitySettings.autoLockTimeoutLabel)
                        } else {
                            StatusChip(label = "密碼未啟用")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusChip(
    label: String,
    isAccent: Boolean = false
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isAccent) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            softWrap = false,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            color = if (isAccent) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (isAccent) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
