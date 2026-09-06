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
