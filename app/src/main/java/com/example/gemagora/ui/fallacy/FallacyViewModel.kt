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
