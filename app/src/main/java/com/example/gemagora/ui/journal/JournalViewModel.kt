package com.example.gemagora.ui.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.gemagora.ai.GemmaLocalHelper
import com.example.gemagora.ai.PromptBuilder
import com.example.gemagora.data.datastore.UserPreferenceStore
import com.example.gemagora.data.model.JournalEntry
import com.example.gemagora.data.repository.JournalRepository
import com.example.gemagora.service.tts.SchoolVoiceResolver
import com.example.gemagora.service.tts.TtsManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class JournalViewModel(
    private val journalRepository: JournalRepository,
    private val gemmaHelper: GemmaLocalHelper,
    private val userPreferenceStore: UserPreferenceStore,
    private val ttsManager: TtsManager
) : ViewModel() {

    val isTtsPlaying: StateFlow<Boolean> = ttsManager.isPlaying
    val isTtsPaused: StateFlow<Boolean> = ttsManager.isPaused
    val currentSpeakingUtteranceId: StateFlow<String?> = ttsManager.currentSpeakingUtteranceId

    fun speakGuidance(guidance: String, entryId: Int) {
        val utteranceId = "journal_guidance_$entryId"
        viewModelScope.launch {
            val settings = userPreferenceStore.ttsSettingsFlow.first()
            val stoicSettings = SchoolVoiceResolver.resolveVoiceSettings(
                schoolName = "斯多葛學派",
                masterName = "馬可·奧理略",
                baseSettings = settings,
                availableVoices = ttsManager.availableVoices.value
            )
            ttsManager.speak(guidance, utteranceId, stoicSettings)
        }
    }

    fun pauseTts() {
        ttsManager.pause()
    }

    fun resumeTts() {
        ttsManager.resume()
    }

    fun toggleSpeakGuidance(guidance: String, entryId: Int) {
        val utteranceId = "journal_guidance_$entryId"
        if (isTtsPlaying.value && currentSpeakingUtteranceId.value == utteranceId) {
            ttsManager.pause()
        } else if (isTtsPaused.value && currentSpeakingUtteranceId.value == utteranceId) {
            ttsManager.resume()
        } else {
            speakGuidance(guidance, entryId)
        }
    }

    fun stopTts() {
        ttsManager.stop()
    }

    override fun onCleared() {
        super.onCleared()
        stopTts()
    }

    val entries: StateFlow<List<JournalEntry>> = journalRepository.allEntries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isGeneratingGuidance = MutableStateFlow(false)
    val isGeneratingGuidance: StateFlow<Boolean> = _isGeneratingGuidance.asStateFlow()

    private val _generatingEntryId = MutableStateFlow<Int?>(null)
    val generatingEntryId: StateFlow<Int?> = _generatingEntryId.asStateFlow()

    private val _currentAiGuidance = MutableStateFlow<String?>(null)
    val currentAiGuidance: StateFlow<String?> = _currentAiGuidance.asStateFlow()

    private var currentJob: kotlinx.coroutines.Job? = null

    private val fallbackMorningPrompts = listOf(
        listOf(
            "今天何事不可控？如何安頓內心？",
            "遇無理之人，如何提醒其源自無知？",
            "今日哪件事最能體現我的實踐美德？"
        ),
        listOf(
            "若今日遭遇阻礙，如何化為基石？",
            "清晨醒來，我對生命抱持何種感恩？",
            "今天該警惕哪些無謂的情緒消耗？"
        )
    )

    private val fallbackEveningPrompts = listOf(
        listOf(
            "今天哪一刻克制？哪刻受情緒牽引？",
            "今天有為討好他人而違背原則嗎？",
            "入睡前，有哪些不可控煩惱應放下？"
        ),
        listOf(
            "今天我對他人展現了足夠包容嗎？",
            "若今天是生命最後一天，我滿意嗎？",
            "今天有哪些微小進步，值得肯定自己？"
        )
    )

    private var morningPoolIndex = 0
    private var eveningPoolIndex = 0

    private val _reflectionPromptSuggestions = MutableStateFlow(fallbackMorningPrompts.first())
    val reflectionPromptSuggestions: StateFlow<List<String>> = _reflectionPromptSuggestions.asStateFlow()

    private val _isPromptAiGenerated = MutableStateFlow(false)
    val isPromptAiGenerated: StateFlow<Boolean> = _isPromptAiGenerated.asStateFlow()

    private val _isRefreshingPrompts = MutableStateFlow(false)
    val isRefreshingPrompts: StateFlow<Boolean> = _isRefreshingPrompts.asStateFlow()

    fun updatePromptType(type: String) {
        val pool = if (type == "morning") fallbackMorningPrompts[morningPoolIndex] else fallbackEveningPrompts[eveningPoolIndex]
        _reflectionPromptSuggestions.value = pool
        _isPromptAiGenerated.value = false
    }

    fun refreshReflectionPrompts(type: String) {
        if (_isRefreshingPrompts.value) return
        val isLoaded = gemmaHelper.loadState.value is com.example.gemagora.data.model.ModelLoadState.Loaded
        if (isLoaded) {
            viewModelScope.launch {
                _isRefreshingPrompts.value = true
                try {
                    val prompt = PromptBuilder.buildJournalPromptSuggestionsPrompt(type, _reflectionPromptSuggestions.value)
                    val reply = gemmaHelper.generateReply(prompt)
                    val parsed = com.example.gemagora.ai.PhilosophicalParser.parseSuggestions(reply)
                    if (parsed.isNotEmpty()) {
                        _reflectionPromptSuggestions.value = parsed.take(3)
                        _isPromptAiGenerated.value = true
                    } else {
                        rotateFallbackPrompts(type)
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    rotateFallbackPrompts(type)
                } finally {
                    _isRefreshingPrompts.value = false
                }
            }
        } else {
            rotateFallbackPrompts(type)
        }
    }

    private fun rotateFallbackPrompts(type: String) {
        if (type == "morning") {
            morningPoolIndex = (morningPoolIndex + 1) % fallbackMorningPrompts.size
            _reflectionPromptSuggestions.value = fallbackMorningPrompts[morningPoolIndex]
        } else {
            eveningPoolIndex = (eveningPoolIndex + 1) % fallbackEveningPrompts.size
            _reflectionPromptSuggestions.value = fallbackEveningPrompts[eveningPoolIndex]
        }
        _isPromptAiGenerated.value = false
    }

    fun addEntry(title: String, content: String, type: String) {
        if (content.isBlank()) return
        viewModelScope.launch {
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val newEntry = JournalEntry(
                title = if (title.isBlank()) (if (type == "morning") "斯多葛晨思" else "斯多葛夕省") else title,
                content = content.trim(),
                entryType = type,
                dateString = date,
                aiGuidance = null
            )
            val id = journalRepository.insertEntry(newEntry)

            // Auto-trigger on-device AI guidance
            generateGuidanceForEntry(id.toInt(), content.trim(), type)
        }
    }

    private fun generateGuidanceForEntry(id: Int, content: String, type: String) {
        _isGeneratingGuidance.value = true
        _generatingEntryId.value = id
        _currentAiGuidance.value = ""

        currentJob?.cancel()
        currentJob = viewModelScope.launch {
            try {
                val prompt = PromptBuilder.buildJournalGuidancePrompt(content, type)
                val responseBuilder = StringBuilder()
                gemmaHelper.generateReplyFlow(prompt).collect { token ->
                    responseBuilder.append(token)
                    _currentAiGuidance.value = responseBuilder.toString()
                }
                val finalGuidance = responseBuilder.toString().trim()
                val entry = journalRepository.getEntryById(id)
                if (entry != null && finalGuidance.isNotBlank()) {
                    journalRepository.updateEntry(entry.copy(aiGuidance = finalGuidance))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // error handled quietly or logged
            } finally {
                _generatingEntryId.value = null
                _isGeneratingGuidance.value = false
            }
        }
    }

    fun cancelGuidance() {
        stopTts()
        currentJob?.cancel()
        gemmaHelper.cancelCurrentInference("User cancelled journal guidance")
        _isGeneratingGuidance.value = false
        _generatingEntryId.value = null
    }

    fun deleteEntry(entry: JournalEntry) {
        viewModelScope.launch {
            journalRepository.deleteEntry(entry)
        }
    }

    class Factory(
        private val journalRepository: JournalRepository,
        private val gemmaHelper: GemmaLocalHelper,
        private val userPreferenceStore: UserPreferenceStore,
        private val ttsManager: TtsManager
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(JournalViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return JournalViewModel(journalRepository, gemmaHelper, userPreferenceStore, ttsManager) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
