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
