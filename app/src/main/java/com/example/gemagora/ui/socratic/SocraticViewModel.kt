package com.example.gemagora.ui.socratic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.gemagora.ai.GemmaLocalHelper
import com.example.gemagora.ai.PromptBuilder
import com.example.gemagora.data.datastore.UserPreferenceStore
import com.example.gemagora.data.model.ChatMessage
import com.example.gemagora.data.model.HistoryRecord
import com.example.gemagora.data.model.ModelLoadState
import com.example.gemagora.data.repository.ChatRepository
import com.example.gemagora.data.repository.HistoryRepository
import com.example.gemagora.service.tts.TtsManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class SocraticViewModel(
    private val chatRepository: ChatRepository,
    private val gemmaHelper: GemmaLocalHelper,
    private val historyRepository: HistoryRepository,
    private val userPreferenceStore: UserPreferenceStore,
    private val ttsManager: TtsManager
) : ViewModel() {

    private val _currentSessionId = MutableStateFlow(UUID.randomUUID().toString())
    val currentSessionId: StateFlow<String> = _currentSessionId.asStateFlow()

    private val _streamingMessage = MutableStateFlow<ChatMessage?>(null)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val chatMessages: StateFlow<List<ChatMessage>> = _currentSessionId
        .flatMapLatest { sessionId ->
            historyRepository.getRecordsBySession(sessionId).map { records ->
                records.map { r ->
                    ChatMessage(
                        id = r.id.toInt(),
                        role = r.role,
                        content = r.content,
                        category = "socratic",
                        timestamp = r.timestamp
                    )
                }
            }
        }
        .combine(_streamingMessage) { dbMessages, streamingMsg ->
            if (streamingMsg != null) dbMessages + streamingMsg else dbMessages
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val historySessions: StateFlow<List<HistoryRecord>> = historyRepository
        .getSessionsByFeature("SOCRATIC")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val gemmaLoadState = gemmaHelper.loadState

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _intensity = MutableStateFlow("標準反詰")
    val intensity: StateFlow<String> = _intensity.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var currentJob: Job? = null

    private val fallbackStarterPool = listOf(
        listOf(
            "什麼是真正的正義？法律即正義嗎？",
            "未經審視的人生真的不值得過嗎？",
            "我們追求的是快樂還是生命的意義？"
        ),
        listOf(
            "如果多數人都相信謊言，真理何在？",
            "自由是為所欲為，還是克制慾望？",
            "若善行皆有回報，純粹的善存在嗎？"
        ),
        listOf(
            "知識與智慧的界線究竟在哪裡？",
            "勇氣是無所畏懼還是知恐懼而前行？",
            "自我是一成不變的還是不斷建構的？"
        )
    )

    private var starterPoolIndex = 0

    private val _starterSuggestions = MutableStateFlow(fallbackStarterPool.first())
    val starterSuggestions: StateFlow<List<String>> = _starterSuggestions.asStateFlow()

    private val _isStarterAiGenerated = MutableStateFlow(false)
    val isStarterAiGenerated: StateFlow<Boolean> = _isStarterAiGenerated.asStateFlow()

    private val _isRefreshingStarters = MutableStateFlow(false)
    val isRefreshingStarters: StateFlow<Boolean> = _isRefreshingStarters.asStateFlow()

    fun refreshStarterSuggestions() {
        if (_isRefreshingStarters.value) return
        val isLoaded = gemmaHelper.loadState.value is com.example.gemagora.data.model.ModelLoadState.Loaded
        if (isLoaded) {
            viewModelScope.launch {
                _isRefreshingStarters.value = true
                try {
                    val prompt = PromptBuilder.buildSocraticTopicSuggestionsPrompt(_starterSuggestions.value)
                    val reply = gemmaHelper.generateReply(prompt)
                    val parsed = com.example.gemagora.ai.PhilosophicalParser.parseSuggestions(reply)
                    if (parsed.isNotEmpty()) {
                        _starterSuggestions.value = parsed.take(3)
                        _isStarterAiGenerated.value = true
                    } else {
                        rotateFallbackStarters()
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    rotateFallbackStarters()
                } finally {
                    _isRefreshingStarters.value = false
                }
            }
        } else {
            rotateFallbackStarters()
        }
    }

    private fun rotateFallbackStarters() {
        starterPoolIndex = (starterPoolIndex + 1) % fallbackStarterPool.size
        _starterSuggestions.value = fallbackStarterPool[starterPoolIndex]
        _isStarterAiGenerated.value = false
    }

    private val fallbackInChatPool = listOf(
        listOf(
            "您的主張是否預設所有人追求相同目標？",
            "若在生死存亡情境下，此原則仍適用嗎？",
            "能否為您剛才的核心概念下個精確定義？"
        ),
        listOf(
            "若直覺與理性推導矛盾，該依何者裁決？",
            "依您所言，我們是否陷入了假二分謬誤？",
            "若換作您的反對者，會提出何種質疑？"
        ),
        listOf(
            "這樣的推論是否混淆了事實與價值判斷？",
            "若此規則普遍化，是否會導出荒謬後果？",
            "我們是在討論理想本質還是現實妥協？"
        )
    )

    private var inChatPoolIndex = 0

    private val _followUpSuggestions = MutableStateFlow(fallbackInChatPool.first())
    val followUpSuggestions: StateFlow<List<String>> = _followUpSuggestions.asStateFlow()

    private val _isFollowUpAiGenerated = MutableStateFlow(false)
    val isFollowUpAiGenerated: StateFlow<Boolean> = _isFollowUpAiGenerated.asStateFlow()

    private val _isRefreshingFollowUps = MutableStateFlow(false)
    val isRefreshingFollowUps: StateFlow<Boolean> = _isRefreshingFollowUps.asStateFlow()

    fun refreshFollowUpSuggestions(customHistory: List<ChatMessage>? = null) {
        if (_isRefreshingFollowUps.value) return
        val isLoaded = gemmaHelper.loadState.value is com.example.gemagora.data.model.ModelLoadState.Loaded
        val msgs = customHistory ?: chatMessages.value
        if (isLoaded && msgs.isNotEmpty()) {
            viewModelScope.launch {
                _isRefreshingFollowUps.value = true
                try {
                    val prompt = PromptBuilder.buildSocraticFollowUpSuggestionsPrompt(msgs, _followUpSuggestions.value)
                    val reply = gemmaHelper.generateReply(prompt)
                    val parsed = com.example.gemagora.ai.PhilosophicalParser.parseSuggestions(reply)
                    if (parsed.isNotEmpty()) {
                        _followUpSuggestions.value = parsed.take(3)
                        _isFollowUpAiGenerated.value = true
                    } else {
                        rotateFallbackInChat()
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    rotateFallbackInChat()
                } finally {
                    _isRefreshingFollowUps.value = false
                }
            }
        } else {
            rotateFallbackInChat()
        }
    }

    private fun rotateFallbackInChat() {
        inChatPoolIndex = (inChatPoolIndex + 1) % fallbackInChatPool.size
        _followUpSuggestions.value = fallbackInChatPool[inChatPoolIndex]
        _isFollowUpAiGenerated.value = false
    }

    val isTtsPlaying: StateFlow<Boolean> = ttsManager.isPlaying
    val isTtsPaused: StateFlow<Boolean> = ttsManager.isPaused
    val currentSpeakingUtteranceId: StateFlow<String?> = ttsManager.currentSpeakingUtteranceId

    fun speak(content: String, messageId: String) {
        viewModelScope.launch {
            val settings = userPreferenceStore.ttsSettingsFlow.first()
            ttsManager.speak(content, messageId, settings)
        }
    }

    fun pauseTts() {
        ttsManager.pause()
    }

    fun resumeTts() {
        ttsManager.resume()
    }

    fun toggleSpeakMessage(messageId: String, content: String) {
        if (isTtsPlaying.value && currentSpeakingUtteranceId.value == messageId) {
            ttsManager.stop()
        } else {
            speak(content, messageId)
        }
    }

    fun stopTts() {
        ttsManager.stop()
    }

    fun setIntensity(newIntensity: String) {
        _intensity.value = newIntensity
    }

    fun startNewSession() {
        cancelCurrentGeneration()
        ttsManager.stop()
        _currentSessionId.value = UUID.randomUUID().toString()
    }

    fun loadSession(sessionId: String) {
        cancelCurrentGeneration()
        ttsManager.stop()
        _currentSessionId.value = sessionId
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            historyRepository.deleteSession(sessionId)
            if (_currentSessionId.value == sessionId) {
                startNewSession()
            }
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            historyRepository.clearFeatureHistory("SOCRATIC")
            chatRepository.clearCategory("socratic")
            startNewSession()
        }
    }

    fun cancelCurrentGeneration() {
        currentJob?.cancel()
        currentJob = null
        gemmaHelper.cancelCurrentInference("User cancelled Socratic generation")
        _streamingMessage.value = null
        _isGenerating.value = false
    }

    fun sendMessage(userText: String) {
        if (userText.isBlank() || _isGenerating.value) return
        if (gemmaLoadState.value is ModelLoadState.Failed || gemmaLoadState.value is ModelLoadState.NotFound) {
            val failureMsg = (gemmaLoadState.value as? ModelLoadState.Failed)?.message
            _errorMessage.value = failureMsg ?: "請先至設定載入本地 Gemma 模型。"
            return
        }

        _errorMessage.value = null
        _isGenerating.value = true

        val session = _currentSessionId.value
        val historyList = chatMessages.value
        val title = historyList.firstOrNull()?.content?.take(30) ?: userText.trim().take(30)

        currentJob = viewModelScope.launch {
            val responseBuilder = StringBuilder()
            try {
                // Insert User Turn to History
                historyRepository.insertRecord(
                    HistoryRecord(
                        sessionId = session,
                        featureType = "SOCRATIC",
                        title = title,
                        role = "user",
                        content = userText.trim(),
                        metadataJson = _intensity.value,
                        timestamp = System.currentTimeMillis()
                    )
                )

                val fullPrompt = PromptBuilder.buildSocraticPrompt(
                    history = historyList,
                    currentMessage = userText.trim(),
                    intensity = _intensity.value
                )

                _streamingMessage.value = ChatMessage(
                    role = "assistant",
                    content = "",
                    category = "socratic"
                )

                gemmaHelper.generateReplyFlow(fullPrompt).collect { token ->
                    responseBuilder.append(token)
                    _streamingMessage.value = ChatMessage(
                        role = "assistant",
                        content = responseBuilder.toString(),
                        category = "socratic"
                    )
                }

                val finalContent = responseBuilder.toString().trim()
                if (finalContent.isNotBlank()) {
                    historyRepository.insertRecord(
                        HistoryRecord(
                            sessionId = session,
                            featureType = "SOCRATIC",
                            title = title,
                            role = "assistant",
                            content = finalContent,
                            metadataJson = _intensity.value,
                            timestamp = System.currentTimeMillis()
                        )
                    )

                    // Auto-speak if enabled
                    val ttsSettings = userPreferenceStore.ttsSettingsFlow.first()
                    if (ttsSettings.enabled && ttsSettings.autoSpeak) {
                        ttsManager.speak(finalContent, "auto_${System.currentTimeMillis()}", ttsSettings)
                    }

                    // Auto-generate contextual follow-up inspiration questions
                    val updatedTurnHistory = historyList +
                        ChatMessage(role = "user", content = userText.trim(), category = "socratic") +
                        ChatMessage(role = "assistant", content = finalContent, category = "socratic")
                    refreshFollowUpSuggestions(updatedTurnHistory)
                }
            } catch (e: CancellationException) {
                val partialContent = responseBuilder.toString().trim()
                if (partialContent.isNotBlank()) {
                    historyRepository.insertRecord(
                        HistoryRecord(
                            sessionId = session,
                            featureType = "SOCRATIC",
                            title = title,
                            role = "assistant",
                            content = partialContent,
                            metadataJson = _intensity.value,
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }
                throw e
            } catch (e: Exception) {
                if (responseBuilder.isNotBlank()) {
                    historyRepository.insertRecord(
                        HistoryRecord(
                            sessionId = session,
                            featureType = "SOCRATIC",
                            title = title,
                            role = "assistant",
                            content = responseBuilder.toString().trim(),
                            metadataJson = _intensity.value,
                            timestamp = System.currentTimeMillis()
                        )
                    )
                } else {
                    _errorMessage.value = "推論中斷或發生錯誤：${e.localizedMessage}"
                }
            } finally {
                _streamingMessage.value = null
                _isGenerating.value = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        ttsManager.stop()
    }

    class Factory(
        private val chatRepository: ChatRepository,
        private val gemmaHelper: GemmaLocalHelper,
        private val historyRepository: HistoryRepository,
        private val userPreferenceStore: UserPreferenceStore,
        private val ttsManager: TtsManager
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SocraticViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return SocraticViewModel(
                    chatRepository,
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
