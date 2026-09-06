package com.example.gemagora.ui.setup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.gemagora.data.model.ModelDownloadState
import com.example.gemagora.data.model.ModelLoadState
import com.example.gemagora.data.model.ModelType
import com.example.gemagora.ui.components.LoadingDialog
import com.example.gemagora.ui.components.ModelStatusCard
import com.example.gemagora.ui.components.SettingsCard

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
    var selectedTab by rememberSaveable { mutableIntStateOf(initialTab.coerceIn(0, 2)) }
    var selectedImportId by rememberSaveable { mutableStateOf<String?>(null) }
    var showConfig by rememberSaveable { mutableStateOf(false) }
    val appearanceScroll = rememberScrollState()
    val modelsScroll = rememberScrollState()
    val ttsScroll = rememberScrollState()
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val modelId = selectedImportId
        if (uri != null && modelId != null) viewModel.importLocalFile(modelId, uri)
    }
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Column {
                key(statusBarHeight) {
                    TopAppBar(
                        title = {
                            Column {
                                Text("系統與模型設定", fontWeight = FontWeight.Bold)
                                Text(
                                    text = "外觀與顯示 · 地端模型 · 朗讀語音",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") }
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
            SecondaryTabRow(selectedTabIndex = selectedTab, containerColor = MaterialTheme.colorScheme.background) {
                listOf(
                    "外觀與顯示" to Icons.Default.Palette,
                    "語言模型" to Icons.Default.SmartToy,
                    "語音朗讀" to Icons.Default.RecordVoiceOver
                ).forEachIndexed { index, (label, icon) ->
                    Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(icon, null, Modifier.size(20.dp))
                            Text(label)
                        }
                    })
                }
            }
        }
    }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(
                    when (selectedTab) {
                        0 -> appearanceScroll
                        1 -> modelsScroll
                        else -> ttsScroll
                    }
                )
                .padding(
                    top = padding.calculateTopPadding() + 16.dp,
                    bottom = navBarPadding + 24.dp,
                    start = 20.dp,
                    end = 20.dp
                ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SettingsPageHeading(
                title = when (selectedTab) {
                    0 -> "定制 GemAgora 外觀與顯示"
                    1 -> "管理本地端 Gemma 4 AI"
                    else -> "哲學語音朗讀與聲線"
                },
                description = when (selectedTab) {
                    0 -> "選擇古典色彩風格與最適字體大小，陪伴每一場深刻對話。"
                    1 -> "直接於裝置離線運行 Gemma 4，保障 100% 哲學隱私。"
                    else -> "設定蘇格拉底沉穩男聲、阿斯帕齊婭女聲或調諧音調語速，賦予思辨靈動聲音。"
                }
            )
            when (selectedTab) {
                0 -> {
                    appearance?.let { AppearanceSettingsCard(it, viewModel::saveAppearance) }
                }
                1 -> {
                    SettingsCard {
                    Text("推論參數概況", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("上下文容量", style = MaterialTheme.typography.bodyMedium)
                        Text("$contextTokens tokens", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    }
                    Text("大容量（8K-32K）可容納長篇倫理推演與多學派交鋒；小容量可節約記憶體。", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedButton(onClick = { showConfig = true }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Tune, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("調整模型參數與提示詞")
                    }
                }
                if (gemmaLoadState is ModelLoadState.Loading) {
                    SettingsCard {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = Color(0xFFF59E0B)
                            )
                            Column {
                                Text("地端推論引擎背景暖機中…", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text("⚡ GPU 運算核心與權重分配中，完成後將自動就緒。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                val failure = (gemmaLoadState as? ModelLoadState.Failed)?.message
                if (failure != null) {
                    SettingsCard {
                        Text("模型載入失敗", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                        Text(failure, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Text("可用本地端模型庫", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("下載完成後或從裝置檔案匯入即可完全離線使用。", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                models.forEach { model ->
                    ModelStatusCard(
                        model = model,
                        downloadState = downloadStates[model.id] ?: ModelDownloadState.Idle,
                        loadState = gemmaLoadState,
                        onDownloadClick = { viewModel.downloadModel(model.id) },
                        onLoadClick = { viewModel.loadModel(model) },
                        onImportClick = { selectedImportId = model.id; filePicker.launch("*/*") },
                        isActive = loadedModelName == model.name
                    )
                }
            }
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
private fun SettingsPageHeading(title: String, description: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
