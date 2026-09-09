package com.example.gemagora.ui.fallacy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.gemagora.ai.GemmaLocalHelper
import com.example.gemagora.ai.PromptBuilder
import com.example.gemagora.data.datastore.UserPreferenceStore
import com.example.gemagora.data.model.HistoryRecord
import com.example.gemagora.data.repository.HistoryRepository
import com.example.gemagora.service.tts.TtsManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class FallacyViewModel(
    private val gemmaHelper: GemmaLocalHelper,
    private val historyRepository: HistoryRepository,
    private val userPreferenceStore: UserPreferenceStore,
    private val ttsManager: TtsManager
) : ViewModel() {

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

    private val _argumentInput = MutableStateFlow("")
    val argumentInput: StateFlow<String> = _argumentInput.asStateFlow()

    private val _analysisResult = MutableStateFlow("")
    val analysisResult: StateFlow<String> = _analysisResult.asStateFlow()

    private val _currentSessionId = MutableStateFlow(UUID.randomUUID().toString())
    val currentSessionId: StateFlow<String> = _currentSessionId.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    // Follow-up state
    private val _followUpTurns = MutableStateFlow<List<HistoryRecord>>(emptyList())
    val followUpTurns: StateFlow<List<HistoryRecord>> = _followUpTurns.asStateFlow()

    private val _streamingFollowUp = MutableStateFlow<String?>(null)
    val streamingFollowUp: StateFlow<String?> = _streamingFollowUp.asStateFlow()

    private val _isGeneratingFollowUp = MutableStateFlow(false)
    val isGeneratingFollowUp: StateFlow<Boolean> = _isGeneratingFollowUp.asStateFlow()

    val historySessions: StateFlow<List<HistoryRecord>> = historyRepository
        .getSessionsByFeature("FALLACY")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val gemmaLoadState = gemmaHelper.loadState
    private var currentJob: Job? = null
    private var followUpJob: Job? = null

    private val fallbackSamplePool = listOf(
        listOf(
            "不考上頂尖大學，人生就徹底完了" to "滑坡謬誤",
            "你批評這部電影，那你自己去拍一部啊" to "訴諸人身",
            "非黑即白：不支持我們就是敵人" to "假二分法"
        ),
        listOf(
            "大家都搶買這款保健品，肯定有效" to "訴諸群眾",
            "科學無法證明沒外星人，所以必然有" to "訴諸無知",
            "你天天熬夜，憑什麼勸我早睡" to "你也一樣"
        ),
        listOf(
            "古人用草藥活了幾千年，西藥都是毒" to "訴諸傳統",
            "新法上路後景氣變差，全是法規的錯" to "後此謬誤",
            "這本古籍必然正確，因它是神聖的" to "循環論證"
        )
    )

    private var samplePoolIndex = 0

    private val _sampleArguments = MutableStateFlow(fallbackSamplePool.first())
    val sampleArguments: StateFlow<List<Pair<String, String>>> = _sampleArguments.asStateFlow()

    private val _isSampleAiGenerated = MutableStateFlow(false)
    val isSampleAiGenerated: StateFlow<Boolean> = _isSampleAiGenerated.asStateFlow()

    private val _isRefreshingSamples = MutableStateFlow(false)
    val isRefreshingSamples: StateFlow<Boolean> = _isRefreshingSamples.asStateFlow()

    fun refreshSampleArguments() {
        if (_isRefreshingSamples.value) return
        val isLoaded = gemmaHelper.loadState.value is com.example.gemagora.data.model.ModelLoadState.Loaded
        if (isLoaded) {
            viewModelScope.launch {
                _isRefreshingSamples.value = true
                try {
                    val prompt = PromptBuilder.buildFallacySampleSuggestionsPrompt(_sampleArguments.value.map { it.first })
                    val reply = gemmaHelper.generateReply(prompt)
                    val parsed = com.example.gemagora.ai.PhilosophicalParser.parseTaggedSuggestions(reply)
                    if (parsed.isNotEmpty()) {
                        _sampleArguments.value = parsed.take(3)
                        _isSampleAiGenerated.value = true
                    } else {
                        rotateFallbackSamples()
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    rotateFallbackSamples()
                } finally {
                    _isRefreshingSamples.value = false
                }
            }
        } else {
            rotateFallbackSamples()
        }
    }

    private fun rotateFallbackSamples() {
        samplePoolIndex = (samplePoolIndex + 1) % fallbackSamplePool.size
        _sampleArguments.value = fallbackSamplePool[samplePoolIndex]
        _isSampleAiGenerated.value = false
    }

    private val fallbackFollowUpPool = listOf(
        listOf(
            "請提供一句得體但一針見血的反駁話術",
            "如何將此論述重構為健全論證？",
            "若對方偷換概念，該如何維持主導權？"
        ),
        listOf(
            "該如何指出其隱含的無效前提？",
            "如何用反詰法讓對方發現自身矛盾？",
            "此論點是否有可取之處？如何善意理解？"
        ),
        listOf(
            "如何用類比論證凸顯其邏輯荒謬？",
            "如何依同情理解原則進行理性改寫？",
            "正式辯論中如何進行定點技術拆解？"
        )
    )

    private var followUpPoolIndex = 0

    private val _followUpSuggestions = MutableStateFlow(fallbackFollowUpPool.first())
    val followUpSuggestions: StateFlow<List<String>> = _followUpSuggestions.asStateFlow()

    private val _isFollowUpAiGenerated = MutableStateFlow(false)
    val isFollowUpAiGenerated: StateFlow<Boolean> = _isFollowUpAiGenerated.asStateFlow()

    private val _isRefreshingFollowUpSuggestions = MutableStateFlow(false)
    val isRefreshingFollowUpSuggestions: StateFlow<Boolean> = _isRefreshingFollowUpSuggestions.asStateFlow()

    fun refreshFollowUpSuggestions() {
        if (_isRefreshingFollowUpSuggestions.value) return
        val isLoaded = gemmaHelper.loadState.value is com.example.gemagora.data.model.ModelLoadState.Loaded
        val arg = _argumentInput.value.trim()
        if (isLoaded && arg.isNotBlank()) {
            viewModelScope.launch {
                _isRefreshingFollowUpSuggestions.value = true
                try {
                    val prompt = PromptBuilder.buildFallacyFollowUpSuggestionsPrompt(arg, _analysisResult.value, _followUpSuggestions.value)
                    val reply = gemmaHelper.generateReply(prompt)
                    val parsed = com.example.gemagora.ai.PhilosophicalParser.parseSuggestions(reply)
                    if (parsed.isNotEmpty()) {
                        _followUpSuggestions.value = parsed.take(3)
                        _isFollowUpAiGenerated.value = true
                    } else {
                        rotateFallbackFollowUps()
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    rotateFallbackFollowUps()
                } finally {
                    _isRefreshingFollowUpSuggestions.value = false
                }
            }
        } else {
            rotateFallbackFollowUps()
        }
    }

    private fun rotateFallbackFollowUps() {
        followUpPoolIndex = (followUpPoolIndex + 1) % fallbackFollowUpPool.size
        _followUpSuggestions.value = fallbackFollowUpPool[followUpPoolIndex]
        _isFollowUpAiGenerated.value = false
    }

    fun setArgumentInput(text: String) {
        _argumentInput.value = text
    }

    fun startNewAnalysis() {
        cancelAnalysis()
        cancelFollowUp()
        _currentSessionId.value = UUID.randomUUID().toString()
        _argumentInput.value = ""
        _analysisResult.value = ""
        _followUpTurns.value = emptyList()
        _streamingFollowUp.value = null
    }

    fun analyzeArgument() {
        val text = _argumentInput.value.trim()
        if (text.isBlank() || _isGenerating.value) return

        val session = UUID.randomUUID().toString()
        _currentSessionId.value = session
        _followUpTurns.value = emptyList()

        val prompt = PromptBuilder.buildFallacyAnalysisPrompt(text)
        _isGenerating.value = true
        _analysisResult.value = ""

        currentJob = viewModelScope.launch {
            val sb = StringBuilder()
            try {
                gemmaHelper.generateReplyFlow(prompt).collect { token ->
                    sb.append(token)
                    _analysisResult.value = sb.toString()
                }
                val finalOutput = sb.toString().trim()
                if (finalOutput.isNotBlank()) {
                    historyRepository.saveInitialGeneration(
                        sessionId = session,
                        featureType = "FALLACY",
                        title = text,
                        promptInput = text,
                        generatedOutput = finalOutput,
                        metadataJson = ""
                    )
                }
            } catch (e: CancellationException) {
                val partial = sb.toString().trim()
                if (partial.isNotBlank()) {
                    historyRepository.saveInitialGeneration(
                        sessionId = session,
                        featureType = "FALLACY",
                        title = text,
                        promptInput = text,
                        generatedOutput = partial,
                        metadataJson = ""
                    )
                }
                throw e
            } catch (e: Exception) {
                _analysisResult.value = _analysisResult.value + "\n[分析中斷：${e.localizedMessage}]"
            } finally {
                _isGenerating.value = false
            }
        }
    }

    fun askFollowUp(question: String) {
        val q = question.trim()
        if (q.isBlank() || _isGeneratingFollowUp.value || _analysisResult.value.isBlank()) return

        _isGeneratingFollowUp.value = true
        _streamingFollowUp.value = ""

        val prompt = PromptBuilder.buildFallacyFollowUpPrompt(
            argumentText = _argumentInput.value,
            analysisHistory = _analysisResult.value,
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
                    historyRepository.saveFollowUpTurn(
                        sessionId = _currentSessionId.value,
                        featureType = "FALLACY",
                        title = _argumentInput.value,
                        userQuestion = q,
                        assistantReply = finalReply,
                        metadataJson = ""
                    )
                    loadSessionTurns(_currentSessionId.value)
                }
            } catch (e: CancellationException) {
                val partial = sb.toString().trim()
                if (partial.isNotBlank()) {
                    historyRepository.saveFollowUpTurn(
                        sessionId = _currentSessionId.value,
                        featureType = "FALLACY",
                        title = _argumentInput.value,
                        userQuestion = q,
                        assistantReply = partial,
                        metadataJson = ""
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
        gemmaHelper.cancelCurrentInference("User cancelled fallacy follow up")
        _isGeneratingFollowUp.value = false
        _streamingFollowUp.value = null
    }

    fun loadSession(record: HistoryRecord) {
        _currentSessionId.value = record.sessionId
        viewModelScope.launch {
            val allTurns = historyRepository.getRecordsBySessionSync(record.sessionId)
            val firstUser = allTurns.firstOrNull { it.role == "user" }
            val firstAssistant = allTurns.firstOrNull { it.role == "assistant" }

            _argumentInput.value = firstUser?.content ?: record.title
            _analysisResult.value = firstAssistant?.content ?: record.content

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

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            historyRepository.deleteSession(sessionId)
            if (_currentSessionId.value == sessionId) {
                startNewAnalysis()
            }
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            historyRepository.clearFeatureHistory("FALLACY")
            startNewAnalysis()
        }
    }

    fun cancelAnalysis() {
        stopTts()
        currentJob?.cancel()
        currentJob = null
        gemmaHelper.cancelCurrentInference("User cancelled fallacy analysis")
        _isGenerating.value = false
    }

    class Factory(
        private val gemmaHelper: GemmaLocalHelper,
        private val historyRepository: HistoryRepository,
        private val userPreferenceStore: UserPreferenceStore,
        private val ttsManager: TtsManager
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(FallacyViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return FallacyViewModel(gemmaHelper, historyRepository, userPreferenceStore, ttsManager) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
