package com.example.gemagora.ui.experiments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.gemagora.ai.GemmaLocalHelper
import com.example.gemagora.ai.PromptBuilder
import com.example.gemagora.data.datastore.UserPreferenceStore
import com.example.gemagora.data.model.ExperimentVariable
import com.example.gemagora.data.model.HistoryRecord
import com.example.gemagora.data.model.ModelLoadState
import com.example.gemagora.data.model.ThoughtExperiment
import com.example.gemagora.data.repository.HistoryRepository
import com.example.gemagora.service.tts.TtsManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class ThoughtExperimentViewModel(
    private val gemmaHelper: GemmaLocalHelper,
    private val historyRepository: HistoryRepository,
    private val userPreferenceStore: UserPreferenceStore,
    private val ttsManager: TtsManager
) : ViewModel() {

    val experiments = listOf(
        ThoughtExperiment(
            id = "trolley",
            title = "電車難題 (Trolley Problem)",
            subtitle = "倫理學最著名的道德抉擇困境",
            description = "一輛失控的電車正高速衝向主軌道。若不介入，將撞死主軌道上的群眾；若扳動拉桿，將轉向備用軌道撞死少數人。",
            classicAuthor = "菲莉帕·福特 (Philippa Foot) / 茱蒂絲·湯姆森 (Judith Thomson)",
            defaultPremise = "失控列車逼近分岔路口，拉動轉向桿可改變行徑路線。",
            variables = listOf(
                ExperimentVariable("main_track_count", "主軌道人數", "若不介入將犧牲的人數", 1f, 10f, 5f, step = 1),
                ExperimentVariable("side_track_count", "備用軌道人數", "轉向將犧牲的人數", 1f, 5f, 1f, step = 1),
                ExperimentVariable(
                    id = "variant_mode",
                    name = "情境變體",
                    description = "介入行動的主動性與手段直接性",
                    minValue = 0f,
                    maxValue = 2f,
                    defaultValue = 0f,
                    step = 1,
                    options = listOf("拉桿轉向", "天橋推落", "器官移植")
                )
            )
        ),
        ThoughtExperiment(
            id = "veil_of_ignorance",
            title = "無知之幕 (Veil of Ignorance)",
            subtitle = "正義論與公平社會契約",
            description = "在進入社會前，所有人被置於一堵無知之幕後，無人知曉自己未來的階級、天賦、性別或財富。此時你會制定何種社會分配原則？",
            classicAuthor = "約翰·羅爾斯 (John Rawls)",
            defaultPremise = "所有參與者在原初狀態下，完全抹去個人既得利益的記憶。",
            variables = listOf(
                ExperimentVariable("inequality_level", "社會貧富差距程度", "財富懸殊比率 (1: 均富 ~ 10: 極端寡頭)", 1f, 10f, 6f, step = 1),
                ExperimentVariable("basic_safety_net", "最底層保障水準 (1: 生存底線, 5: 寬裕尊嚴)", "對最弱勢者的資源傾斜與保障指數", 1f, 5f, 4f, step = 1)
            )
        ),
        ThoughtExperiment(
            id = "ship_of_theseus",
            title = "忒修斯之船 (Ship of Theseus)",
            subtitle = "物質置換與同一性悖論",
            description = "忒修斯的木船在航行中每塊老朽的木板逐一被替換成新木板。當每一塊舊木板都被替換過後，這艘船還是原來那艘船嗎？",
            classicAuthor = "普魯塔克 (Plutarch) / 霍布斯 (Thomas Hobbes)",
            defaultPremise = "船隻在長年航行中歷經零件磨損與置換。",
            variables = listOf(
                ExperimentVariable("replacement_pct", "木板替換比例 (%)", "新零件佔總船身之百分比", 10f, 100f, 100f, step = 10),
                ExperimentVariable("rebuild_old_ship", "收集舊木板重組新船 (0: 否, 1: 是)", "舊零件若重新組裝成第二艘船，哪一艘才是真正本尊？", 0f, 1f, 1f, step = 1, isToggle = true)
            )
        ),
        ThoughtExperiment(
            id = "brain_in_vat",
            title = "缸中之腦 (Brain in a Vat)",
            subtitle = "認識論懷疑主義與真實的本質",
            description = "假定你的大腦被瘋狂科學家浸泡在營養液中，神經元連接超級電腦模擬一切感官。你如何確定眼前的現實不是電訊號？",
            classicAuthor = "希拉蕊·普特南 (Hilary Putnam) / 笛卡兒 (René Descartes)",
            defaultPremise = "感官體驗由外部訊號精確模擬。",
            variables = listOf(
                ExperimentVariable("simulation_fidelity", "感官擬真度 (%)", "電腦模擬現實細節與痛覺的完整度", 50f, 100f, 99f, step = 5),
                ExperimentVariable("plug_experience_machine", "是否自願永久接入體驗機器 (0: 拒絕, 1: 接受)", "諾齊克的體驗機器：永遠快樂但並非真實", 0f, 1f, 0f, step = 1, isToggle = true)
            )
        )
    )

    private val _selectedExperiment = MutableStateFlow(experiments.first())
    val selectedExperiment: StateFlow<ThoughtExperiment> = _selectedExperiment.asStateFlow()

    private val _variableValues = MutableStateFlow<Map<String, Float>>(emptyMap())
    val variableValues: StateFlow<Map<String, Float>> = _variableValues.asStateFlow()

    private val _deductionResult = MutableStateFlow("")
    val deductionResult: StateFlow<String> = _deductionResult.asStateFlow()

    private val _currentSessionId = MutableStateFlow(UUID.randomUUID().toString())
    val currentSessionId: StateFlow<String> = _currentSessionId.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    // Follow-up states
    private val _followUpTurns = MutableStateFlow<List<HistoryRecord>>(emptyList())
    val followUpTurns: StateFlow<List<HistoryRecord>> = _followUpTurns.asStateFlow()

    private val _streamingFollowUp = MutableStateFlow<String?>(null)
    val streamingFollowUp: StateFlow<String?> = _streamingFollowUp.asStateFlow()

    private val _isGeneratingFollowUp = MutableStateFlow(false)
    val isGeneratingFollowUp: StateFlow<Boolean> = _isGeneratingFollowUp.asStateFlow()

    val historySessions: StateFlow<List<HistoryRecord>> = historyRepository
        .getSessionsByFeature("EXPERIMENT")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val gemmaLoadState = gemmaHelper.loadState
    private var currentJob: Job? = null
    private var followUpJob: Job? = null

    init {
        selectExperiment(experiments.first())
    }

    fun startNewExperiment(exp: ThoughtExperiment = _selectedExperiment.value) {
        cancelDeduction()
        cancelFollowUp()
        _currentSessionId.value = UUID.randomUUID().toString()
        _selectedExperiment.value = exp
        _variableValues.value = exp.variables.associate { it.id to it.defaultValue }
        _deductionResult.value = ""
        _followUpTurns.value = emptyList()
        _streamingFollowUp.value = null
    }

    fun selectExperiment(exp: ThoughtExperiment) {
        startNewExperiment(exp)
    }

    fun updateVariable(variableId: String, value: Float) {
        _variableValues.value = _variableValues.value + (variableId to value)
    }

    private fun getVariablesSummary(): String {
        val exp = _selectedExperiment.value
        val vars = _variableValues.value
        return exp.variables.joinToString("\n") { v ->
            val cur = vars[v.id] ?: v.defaultValue
            val valStr = if (v.options != null) {
                v.options.getOrNull(cur.toInt()) ?: "${cur.toInt()}"
            } else if (v.isToggle) {
                if (cur >= 0.5f) "是" else "否"
            } else {
                "${cur.toInt()}"
            }
            "- ${v.name}: $valStr (${v.description})"
        }
    }

    fun runEthicalDeduction() {
        if (_isGenerating.value) return
        val session = UUID.randomUUID().toString()
        _currentSessionId.value = session
        _followUpTurns.value = emptyList()

        val exp = _selectedExperiment.value
        val summary = getVariablesSummary()

        val prompt = PromptBuilder.buildThoughtExperimentPrompt(
            experimentTitle = exp.title,
            premise = exp.defaultPremise,
            variablesSummary = summary
        )

        _isGenerating.value = true
        _deductionResult.value = ""

        currentJob = viewModelScope.launch {
            val sb = StringBuilder()
            try {
                gemmaHelper.generateReplyFlow(prompt).collect { token ->
                    sb.append(token)
                    _deductionResult.value = sb.toString()
                }
                val finalOutput = sb.toString().trim()
                if (finalOutput.isNotBlank()) {
                    val meta = buildMetadataString(exp.id, _variableValues.value)
                    historyRepository.saveInitialGeneration(
                        sessionId = session,
                        featureType = "EXPERIMENT",
                        title = exp.title,
                        promptInput = summary,
                        generatedOutput = finalOutput,
                        metadataJson = meta
                    )
                }
            } catch (e: CancellationException) {
                val partial = sb.toString().trim()
                if (partial.isNotBlank()) {
                    val meta = buildMetadataString(exp.id, _variableValues.value)
                    historyRepository.saveInitialGeneration(
                        sessionId = session,
                        featureType = "EXPERIMENT",
                        title = exp.title,
                        promptInput = summary,
                        generatedOutput = partial,
                        metadataJson = meta
                    )
                }
                throw e
            } catch (e: Exception) {
                _deductionResult.value = _deductionResult.value + "\n[推演中斷：${e.localizedMessage}]"
            } finally {
                _isGenerating.value = false
            }
        }
    }

    fun askFollowUp(question: String) {
        val q = question.trim()
        if (q.isBlank() || _isGeneratingFollowUp.value || _deductionResult.value.isBlank()) return

        _isGeneratingFollowUp.value = true
        _streamingFollowUp.value = ""

        val exp = _selectedExperiment.value
        val summary = getVariablesSummary()

        val prompt = PromptBuilder.buildThoughtExperimentFollowUpPrompt(
            experimentTitle = exp.title,
            premise = exp.defaultPremise,
            variablesSummary = summary,
            deductionHistory = _deductionResult.value,
            followUpQuestion = q
        )

        followUpJob = viewModelScope.launch {
            val sb = StringBuilder()
            try {
                gemmaHelper.generateReplyFlow(prompt).collect { token ->
                    sb.append(token)
                    _streamingFollowUp.value = sb.toString()
                }
                val finalReply = sb.toString().trim()
                if (finalReply.isNotBlank()) {
                    val meta = buildMetadataString(exp.id, _variableValues.value)
                    historyRepository.saveFollowUpTurn(
                        sessionId = _currentSessionId.value,
                        featureType = "EXPERIMENT",
                        title = exp.title,
                        userQuestion = q,
                        assistantReply = finalReply,
                        metadataJson = meta
                    )
                    loadSessionTurns(_currentSessionId.value)
                }
            } catch (e: CancellationException) {
                val partial = sb.toString().trim()
                if (partial.isNotBlank()) {
                    val meta = buildMetadataString(exp.id, _variableValues.value)
                    historyRepository.saveFollowUpTurn(
                        sessionId = _currentSessionId.value,
                        featureType = "EXPERIMENT",
                        title = exp.title,
                        userQuestion = q,
                        assistantReply = partial,
                        metadataJson = meta
                    )
                    loadSessionTurns(_currentSessionId.value)
                }
                throw e
            } catch (e: Exception) {
                _streamingFollowUp.value = (_streamingFollowUp.value ?: "") + "\n[追問中斷：${e.localizedMessage}]"
            } finally {
                _streamingFollowUp.value = null
                _isGeneratingFollowUp.value = false
            }
        }
    }

    fun cancelFollowUp() {
        followUpJob?.cancel()
        followUpJob = null
        gemmaHelper.cancelCurrentInference("User cancelled thought experiment follow up")
        _isGeneratingFollowUp.value = false
        _streamingFollowUp.value = null
    }

    fun loadSession(record: HistoryRecord) {
        _currentSessionId.value = record.sessionId
        viewModelScope.launch {
            val allTurns = historyRepository.getRecordsBySessionSync(record.sessionId)
            val firstAssistant = allTurns.firstOrNull { it.role == "assistant" }
            _deductionResult.value = firstAssistant?.content ?: record.content

            val meta = allTurns.firstOrNull { it.metadataJson.isNotBlank() }?.metadataJson ?: record.metadataJson
            val parts = meta.split(";;")
            if (parts.isNotEmpty()) {
                val expId = parts[0]
                val matchedExp = experiments.find { it.id == expId }
                if (matchedExp != null) {
                    _selectedExperiment.value = matchedExp
                    val defaultMap = matchedExp.variables.associate { it.id to it.defaultValue }.toMutableMap()
                    if (parts.size > 1) {
                        parts[1].split(",").forEach { pair ->
                            val kv = pair.split("=")
                            if (kv.size == 2) {
                                kv[1].toFloatOrNull()?.let { v -> defaultMap[kv[0]] = v }
                            }
                        }
                    }
                    _variableValues.value = defaultMap
                }
            }

            val firstAssistantIdx = allTurns.indexOfFirst { it.role == "assistant" }
            _followUpTurns.value = if (firstAssistantIdx != -1) allTurns.drop(firstAssistantIdx + 1) else emptyList()
        }
    }

    private fun loadSessionTurns(sessionId: String) {
        viewModelScope.launch {
            val allTurns = historyRepository.getRecordsBySessionSync(sessionId)
            val firstAssistantIdx = allTurns.indexOfFirst { it.role == "assistant" }
            _followUpTurns.value = if (firstAssistantIdx != -1) allTurns.drop(firstAssistantIdx + 1) else emptyList()
        }
    }

    private fun buildMetadataString(expId: String, vars: Map<String, Float>): String {
        val varsStr = vars.entries.joinToString(",") { "${it.key}=${it.value}" }
        val exp = experiments.find { it.id == expId }
        val friendlyBadges = exp?.variables?.joinToString(",") { v ->
            val cur = vars[v.id] ?: v.defaultValue
            val valStr = if (v.options != null) {
                v.options.getOrNull(cur.toInt()) ?: "${cur.toInt()}"
            } else if (v.isToggle) {
                if (cur >= 0.5f) "是" else "否"
            } else {
                "${cur.toInt()}"
            }
            "${v.name}: $valStr"
        } ?: ""
        return "$expId;;$varsStr;;$friendlyBadges"
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            historyRepository.deleteSession(sessionId)
            if (_currentSessionId.value == sessionId) {
                startNewExperiment()
            }
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            historyRepository.clearFeatureHistory("EXPERIMENT")
            startNewExperiment()
        }
    }

    val isTtsPlaying: StateFlow<Boolean> = ttsManager.isPlaying
    val isTtsPaused: StateFlow<Boolean> = ttsManager.isPaused
    val currentSpeakingUtteranceId: StateFlow<String?> = ttsManager.currentSpeakingUtteranceId

    fun speak(text: String, utteranceId: String) {
        viewModelScope.launch {
            val settings = userPreferenceStore.ttsSettingsFlow.first()
            ttsManager.speak(text, utteranceId, settings)
        }
    }

    fun pauseTts() {
        ttsManager.pause()
    }

    fun resumeTts() {
        ttsManager.resume()
    }

    fun toggleSpeak(text: String, utteranceId: String) {
        viewModelScope.launch {
            val settings = userPreferenceStore.ttsSettingsFlow.first()
            ttsManager.togglePlayPause(text, utteranceId, settings)
        }
    }

    fun stopTts() {
        ttsManager.stop()
    }

    override fun onCleared() {
        super.onCleared()
        stopTts()
    }

    fun cancelDeduction() {
        stopTts()
        currentJob?.cancel()
        currentJob = null
        gemmaHelper.cancelCurrentInference("User cancelled thought experiment")
        _isGenerating.value = false
    }

    class Factory(
        private val gemmaHelper: GemmaLocalHelper,
        private val historyRepository: HistoryRepository,
        private val userPreferenceStore: UserPreferenceStore,
        private val ttsManager: TtsManager
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ThoughtExperimentViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ThoughtExperimentViewModel(
                    gemmaHelper,
                    historyRepository,
                    userPreferenceStore,
                    ttsManager
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
