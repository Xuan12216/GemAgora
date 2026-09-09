package com.example.gemagora.ui.roundtable

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.gemagora.ai.CrossExaminationItem
import com.example.gemagora.ai.GemmaLocalHelper
import com.example.gemagora.ai.PromptBuilder
import com.example.gemagora.ai.RoundTableResult
import com.example.gemagora.ai.SchoolSpeech
import com.example.gemagora.data.datastore.UserPreferenceStore
import com.example.gemagora.data.model.HistoryRecord
import com.example.gemagora.data.repository.HistoryRepository
import com.example.gemagora.service.tts.SchoolVoiceResolver
import com.example.gemagora.service.tts.TtsManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class RoundTableViewModel(
    private val gemmaHelper: GemmaLocalHelper,
    private val historyRepository: HistoryRepository,
    private val userPreferenceStore: UserPreferenceStore,
    private val ttsManager: TtsManager
) : ViewModel() {

    val availableSchools = listOf(
        "斯多葛學派 (馬可·奧理略 / 愛比克泰德)",
        "存在主義 (薩特 / 卡繆)",
        "虛無主義與超人意志 (尼采)",
        "效益主義 (邊沁 / 密爾)",
        "東方道家思想 (老子 / 莊子)",
        "佛家緣起性空 (釋迦牟尼)"
    )

    private val _selectedSchools = MutableStateFlow(
        setOf("斯多葛學派 (馬可·奧理略 / 愛比克泰德)", "存在主義 (薩特 / 卡繆)", "東方道家思想 (老子 / 莊子)")
    )
    val selectedSchools: StateFlow<Set<String>> = _selectedSchools.asStateFlow()

    private val _topic = MutableStateFlow("面對人生的焦慮、無常與困境，我們應當如何自處？")
    val topic: StateFlow<String> = _topic.asStateFlow()

    private val _debateResult = MutableStateFlow("")
    val debateResult: StateFlow<String> = _debateResult.asStateFlow()

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
        .getSessionsByFeature("ROUNDTABLE")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val gemmaLoadState = gemmaHelper.loadState
    private var currentJob: Job? = null
    private var followUpJob: Job? = null

    private val fallbackTopicPool = listOf(
        listOf(
            "如何以哲學面對現代精神內耗？",
            "追求內心寧靜是否等於逃避責任？",
            "在荒謬的世界中，意義由誰定義？"
        ),
        listOf(
            "科技演算法是否正在剝奪自由意志？",
            "追求多數人幸福，可以犧牲少數嗎？",
            "順應自然無為，能否在競爭中立足？"
        ),
        listOf(
            "痛苦是生命的本質還是靈魂的磨練？",
            "面對無法改變的逆境，臣服還是反抗？",
            "真正的道德是純粹理性還是同理心？"
        )
    )

    private var topicPoolIndex = 0

    private val _topicSuggestions = MutableStateFlow(fallbackTopicPool.first())
    val topicSuggestions: StateFlow<List<String>> = _topicSuggestions.asStateFlow()

    private val _isTopicAiGenerated = MutableStateFlow(false)
    val isTopicAiGenerated: StateFlow<Boolean> = _isTopicAiGenerated.asStateFlow()

    private val _isRefreshingTopics = MutableStateFlow(false)
    val isRefreshingTopics: StateFlow<Boolean> = _isRefreshingTopics.asStateFlow()

    fun refreshTopicSuggestions() {
        if (_isRefreshingTopics.value) return
        val isLoaded = gemmaHelper.loadState.value is com.example.gemagora.data.model.ModelLoadState.Loaded
        if (isLoaded) {
            viewModelScope.launch {
                _isRefreshingTopics.value = true
                try {
                    val prompt = PromptBuilder.buildRoundTableTopicSuggestionsPrompt(_topicSuggestions.value)
                    val reply = gemmaHelper.generateReply(prompt)
                    val parsed = com.example.gemagora.ai.PhilosophicalParser.parseSuggestions(reply)
                    if (parsed.isNotEmpty()) {
                        _topicSuggestions.value = parsed.take(3)
                        _isTopicAiGenerated.value = true
                    } else {
                        rotateFallbackTopics()
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    rotateFallbackTopics()
                } finally {
                    _isRefreshingTopics.value = false
                }
            }
        } else {
            rotateFallbackTopics()
        }
    }

    private fun rotateFallbackTopics() {
        topicPoolIndex = (topicPoolIndex + 1) % fallbackTopicPool.size
        _topicSuggestions.value = fallbackTopicPool[topicPoolIndex]
        _isTopicAiGenerated.value = false
    }

    private val fallbackFollowUpPool = listOf(
        listOf(
            "請讓斯多葛學派針對此結論提出反思",
            "如何將此辯證轉化為日常生活實踐？",
            "面對無常，存在主義與道家如何調和？"
        ),
        listOf(
            "虛無主義在此處是否低估了意義韌性？",
            "若將結論應用於職場，大師有何指引？",
            "效益主義推行時會付出何種道德代價？"
        ),
        listOf(
            "佛家緣起性空如何化解各派執念？",
            "主持人能否給予更具包容性的解方？",
            "意志軟弱時，崇高的哲學如何著陸？"
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
        val currentTopic = _topic.value.trim()
        val currentDebate = _debateResult.value.trim()
        if (isLoaded && currentDebate.isNotBlank()) {
            viewModelScope.launch {
                _isRefreshingFollowUpSuggestions.value = true
                try {
                    val prompt = PromptBuilder.buildRoundTableFollowUpSuggestionsPrompt(currentTopic, currentDebate, _followUpSuggestions.value)
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

    fun setTopic(newTopic: String) {
        _topic.value = newTopic
    }

    fun startNewTopic() {
        cancelRoundTable()
        cancelFollowUp()
        _currentSessionId.value = UUID.randomUUID().toString()
        _topic.value = ""
        _selectedSchools.value = setOf("斯多葛學派 (馬可·奧理略 / 愛比克泰德)", "存在主義 (薩特 / 卡繆)", "東方道家思想 (老子 / 莊子)")
        _debateResult.value = ""
        _followUpTurns.value = emptyList()
        _streamingFollowUp.value = null
    }

    fun toggleSchool(school: String) {
        val current = _selectedSchools.value
        if (current.contains(school)) {
            if (current.size > 1) {
                _selectedSchools.value = current - school
            }
        } else {
            _selectedSchools.value = current + school
        }
    }

    fun startRoundTable() {
        if (_isGenerating.value || _topic.value.isBlank()) return
        val session = UUID.randomUUID().toString()
        _currentSessionId.value = session
        _followUpTurns.value = emptyList()

        val prompt = PromptBuilder.buildRoundTablePrompt(
            topic = _topic.value.trim(),
            selectedSchools = _selectedSchools.value.toList()
        )

        _isGenerating.value = true
        _debateResult.value = ""

        currentJob = viewModelScope.launch {
            val sb = StringBuilder()
            try {
                gemmaHelper.generateReplyFlow(prompt).collect { token ->
                    sb.append(token)
                    _debateResult.value = sb.toString()
                }
                val finalOutput = sb.toString().trim()
                if (finalOutput.isNotBlank()) {
                    val schoolsJson = _selectedSchools.value.joinToString(";;")
                    historyRepository.saveInitialGeneration(
                        sessionId = session,
                        featureType = "ROUNDTABLE",
                        title = _topic.value.trim(),
                        promptInput = _topic.value.trim(),
                        generatedOutput = finalOutput,
                        metadataJson = schoolsJson
                    )
                }
            } catch (e: CancellationException) {
                val partial = sb.toString().trim()
                if (partial.isNotBlank()) {
                    val schoolsJson = _selectedSchools.value.joinToString(";;")
                    historyRepository.saveInitialGeneration(
                        sessionId = session,
                        featureType = "ROUNDTABLE",
                        title = _topic.value.trim(),
                        promptInput = _topic.value.trim(),
                        generatedOutput = partial,
                        metadataJson = schoolsJson
                    )
                }
                throw e
            } catch (e: Exception) {
                _debateResult.value = _debateResult.value + "\n[圓桌思辨中斷：${e.localizedMessage}]"
            } finally {
                _isGenerating.value = false
            }
        }
    }

    fun askFollowUp(question: String) {
        val q = question.trim()
        if (q.isBlank() || _isGeneratingFollowUp.value || _debateResult.value.isBlank()) return

        _isGeneratingFollowUp.value = true
        _streamingFollowUp.value = ""

        val prompt = PromptBuilder.buildRoundTableFollowUpPrompt(
            topic = _topic.value,
            selectedSchools = _selectedSchools.value.toList(),
            debateHistory = _debateResult.value,
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
                        featureType = "ROUNDTABLE",
                        title = _topic.value,
                        userQuestion = q,
                        assistantReply = finalReply,
                        metadataJson = _selectedSchools.value.joinToString(";;")
                    )
                    loadSessionTurns(_currentSessionId.value)
                }
            } catch (e: CancellationException) {
                val partial = sb.toString().trim()
                if (partial.isNotBlank()) {
                    historyRepository.saveFollowUpTurn(
                        sessionId = _currentSessionId.value,
                        featureType = "ROUNDTABLE",
                        title = _topic.value,
                        userQuestion = q,
                        assistantReply = partial,
                        metadataJson = _selectedSchools.value.joinToString(";;")
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
        gemmaHelper.cancelCurrentInference("User cancelled round table follow up")
        _isGeneratingFollowUp.value = false
        _streamingFollowUp.value = null
    }

    fun loadSession(record: HistoryRecord) {
        _currentSessionId.value = record.sessionId
        _topic.value = record.title
        viewModelScope.launch {
            val allTurns = historyRepository.getRecordsBySessionSync(record.sessionId)
            val firstAssistant = allTurns.firstOrNull { it.role == "assistant" }
            _debateResult.value = firstAssistant?.content ?: record.content

            val meta = allTurns.firstOrNull { it.metadataJson.isNotBlank() }?.metadataJson ?: record.metadataJson
            if (meta.isNotBlank()) {
                val schools = meta.split(";;").filter { it.isNotBlank() }.toSet()
                if (schools.isNotEmpty()) {
                    _selectedSchools.value = schools
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

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            historyRepository.deleteSession(sessionId)
            if (_currentSessionId.value == sessionId) {
                startNewTopic()
            }
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            historyRepository.clearFeatureHistory("ROUNDTABLE")
            startNewTopic()
        }
    }

    val isTtsPlaying: StateFlow<Boolean> = ttsManager.isPlaying
    val isTtsPaused: StateFlow<Boolean> = ttsManager.isPaused
    val currentSpeakingUtteranceId: StateFlow<String?> = ttsManager.currentSpeakingUtteranceId

    private val _isDebateSequencerActive = MutableStateFlow(false)
    val isDebateSequencerActive: StateFlow<Boolean> = _isDebateSequencerActive.asStateFlow()

    private var debateSequencerJob: Job? = null

    fun speakSchool(speech: SchoolSpeech) {
        stopSequencer()
        val utteranceId = "school_${speech.schoolName.hashCode()}"
        viewModelScope.launch {
            val baseSettings = userPreferenceStore.ttsSettingsFlow.first()
            val voices = ttsManager.availableVoices.value
            val resolvedSettings = SchoolVoiceResolver.resolveVoiceSettings(
                speech.schoolName,
                speech.masterName,
                baseSettings,
                voices
            )
            val intro = "【${speech.schoolName}代表 ${speech.masterName}】：${speech.content}"
            ttsManager.speak(intro, utteranceId, resolvedSettings)
        }
    }

    fun toggleSpeakSchool(speech: SchoolSpeech) {
        val utteranceId = "school_${speech.schoolName.hashCode()}"
        if (isTtsPlaying.value && currentSpeakingUtteranceId.value == utteranceId) {
            ttsManager.stop()
        } else {
            speakSchool(speech)
        }
    }

    fun speakCrossExam(item: CrossExaminationItem, index: Int) {
        stopSequencer()
        val utteranceId = "cross_$index"
        viewModelScope.launch {
            val baseSettings = userPreferenceStore.ttsSettingsFlow.first()
            val voices = ttsManager.availableVoices.value
            val resolvedSettings = SchoolVoiceResolver.resolveVoiceSettings(
                item.challenger,
                item.challenger,
                baseSettings,
                voices
            )
            val header = "${item.challenger} 質疑 ${item.target ?: "各派"}：${item.content}"
            ttsManager.speak(header, utteranceId, resolvedSettings)
        }
    }

    fun toggleSpeakCrossExam(item: CrossExaminationItem, index: Int) {
        val utteranceId = "cross_$index"
        if (isTtsPlaying.value && currentSpeakingUtteranceId.value == utteranceId) {
            ttsManager.stop()
        } else {
            speakCrossExam(item, index)
        }
    }

    fun speakSynthesis(synthesisText: String) {
        stopSequencer()
        val utteranceId = "synthesis_summary"
        viewModelScope.launch {
            val baseSettings = userPreferenceStore.ttsSettingsFlow.first()
            val socratesSettings = baseSettings.copy(pitch = 0.82f, speed = 0.95f)
            ttsManager.speak("【辯證合一與總結】：$synthesisText", utteranceId, socratesSettings)
        }
    }

    fun toggleSpeakSynthesis(synthesisText: String) {
        val utteranceId = "synthesis_summary"
        if (isTtsPlaying.value && currentSpeakingUtteranceId.value == utteranceId) {
            ttsManager.stop()
        } else {
            speakSynthesis(synthesisText)
        }
    }

    fun speakFollowUpTurn(turnId: Long, content: String) {
        stopSequencer()
        val utteranceId = "followup_$turnId"
        viewModelScope.launch {
            val baseSettings = userPreferenceStore.ttsSettingsFlow.first()
            ttsManager.speak(content, utteranceId, baseSettings)
        }
    }

    fun pauseTts() {
        ttsManager.pause()
    }

    fun resumeTts() {
        ttsManager.resume()
    }

    fun playFullDebate(result: RoundTableResult) {
        if (_isDebateSequencerActive.value) {
            stopSequencer()
            ttsManager.stop()
            return
        }

        stopSequencer()
        ttsManager.stop()

        debateSequencerJob = viewModelScope.launch {
            _isDebateSequencerActive.value = true
            try {
                val baseSettings = userPreferenceStore.ttsSettingsFlow.first()
                val voices = ttsManager.availableVoices.value

                // 1. Speeches
                for (speech in result.speeches) {
                    val utteranceId = "school_${speech.schoolName.hashCode()}"
                    val resolvedSettings = SchoolVoiceResolver.resolveVoiceSettings(
                        speech.schoolName,
                        speech.masterName,
                        baseSettings,
                        voices
                    )
                    val textToSpeak = "【${speech.schoolName}代表 ${speech.masterName}】：${speech.content}"
                    ttsManager.speak(textToSpeak, utteranceId, resolvedSettings)

                    ttsManager.currentSpeakingUtteranceId.first { it == utteranceId }
                    combine(ttsManager.isPlaying, ttsManager.isPaused) { playing, paused ->
                        !playing && !paused
                    }.first { it }
                    kotlinx.coroutines.delay(650)
                }

                // 2. Cross examination
                for ((idx, item) in result.crossExamItems.withIndex()) {
                    val utteranceId = "cross_$idx"
                    val resolvedSettings = SchoolVoiceResolver.resolveVoiceSettings(
                        item.challenger,
                        item.challenger,
                        baseSettings,
                        voices
                    )
                    val textToSpeak = "${item.challenger} 質疑 ${item.target ?: "各派"}：${item.content}"
                    ttsManager.speak(textToSpeak, utteranceId, resolvedSettings)

                    ttsManager.currentSpeakingUtteranceId.first { it == utteranceId }
                    combine(ttsManager.isPlaying, ttsManager.isPaused) { playing, paused ->
                        !playing && !paused
                    }.first { it }
                    kotlinx.coroutines.delay(650)
                }

                // 3. Synthesis
                if (!result.synthesis.isNullOrBlank()) {
                    val utteranceId = "synthesis_summary"
                    val socratesSettings = baseSettings.copy(pitch = 0.82f, speed = 0.95f)
                    ttsManager.speak("【辯證合一與總結】：${result.synthesis}", utteranceId, socratesSettings)

                    ttsManager.currentSpeakingUtteranceId.first { it == utteranceId }
                    combine(ttsManager.isPlaying, ttsManager.isPaused) { playing, paused ->
                        !playing && !paused
                    }.first { it }
                }
            } catch (e: CancellationException) {
                // Cancelled normally
            } finally {
                _isDebateSequencerActive.value = false
            }
        }
    }

    fun stopSequencer() {
        debateSequencerJob?.cancel()
        debateSequencerJob = null
        _isDebateSequencerActive.value = false
    }

    fun stopTts() {
        stopSequencer()
        ttsManager.stop()
    }

    override fun onCleared() {
        super.onCleared()
        stopTts()
    }

    fun cancelRoundTable() {
        stopTts()
        currentJob?.cancel()
        currentJob = null
        gemmaHelper.cancelCurrentInference("User cancelled round table")
        _isGenerating.value = false
    }

    class Factory(
        private val gemmaHelper: GemmaLocalHelper,
        private val historyRepository: HistoryRepository,
        private val userPreferenceStore: UserPreferenceStore,
        private val ttsManager: TtsManager
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(RoundTableViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return RoundTableViewModel(
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
