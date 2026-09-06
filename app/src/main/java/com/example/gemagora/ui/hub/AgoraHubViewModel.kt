package com.example.gemagora.ui.hub

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.gemagora.ai.GemmaLocalHelper
import com.example.gemagora.data.datastore.UserPreferenceStore
import com.example.gemagora.data.model.ModelLoadState
import com.example.gemagora.service.tts.SchoolVoiceResolver
import com.example.gemagora.service.tts.TtsManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class DailyQuote(
    val quote: String,
    val author: String,
    val school: String,
    val reflectionQuestion: String
)

class AgoraHubViewModel(
    val gemmaHelper: GemmaLocalHelper,
    val userPreferenceStore: UserPreferenceStore,
    private val ttsManager: TtsManager
) : ViewModel() {

    val loadState: StateFlow<ModelLoadState> = gemmaHelper.loadState
    val loadedModelName: StateFlow<String?> = gemmaHelper.loadedModelName
    val useGpu: StateFlow<Boolean> = userPreferenceStore.useGpuFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val enableThinking: StateFlow<Boolean> = userPreferenceStore.enableThinkingFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isTtsPlaying: StateFlow<Boolean> = ttsManager.isPlaying
    val isTtsPaused: StateFlow<Boolean> = ttsManager.isPaused
    val currentSpeakingUtteranceId: StateFlow<String?> = ttsManager.currentSpeakingUtteranceId

    fun speakQuote(quote: DailyQuote) {
        val utteranceId = "hub_quote_${quote.author.hashCode()}"
        viewModelScope.launch {
            val settings = userPreferenceStore.ttsSettingsFlow.first()
            val schoolSettings = SchoolVoiceResolver.resolveVoiceSettings(
                schoolName = quote.school,
                masterName = quote.author,
                baseSettings = settings,
                availableVoices = ttsManager.availableVoices.value
            )
            val fullText = "每日哲思啟發。${quote.quote}。作者，${quote.author}，${quote.school}。今日反思：${quote.reflectionQuestion}"
            ttsManager.speak(fullText, utteranceId, schoolSettings)
        }
    }

    fun pauseTts() {
        ttsManager.pause()
    }

    fun resumeTts() {
        ttsManager.resume()
    }

    fun toggleSpeakQuote(quote: DailyQuote) {
        val utteranceId = "hub_quote_${quote.author.hashCode()}"
        if (isTtsPlaying.value && currentSpeakingUtteranceId.value == utteranceId) {
            ttsManager.pause()
        } else if (isTtsPaused.value && currentSpeakingUtteranceId.value == utteranceId) {
            ttsManager.resume()
        } else {
            speakQuote(quote)
        }
    }

    fun stopTts() {
        ttsManager.stop()
    }

    override fun onCleared() {
        super.onCleared()
        stopTts()
    }

    private val quotes = listOf(
        DailyQuote(
            quote = "未經審視的人生，是不值得活的。",
            author = "蘇格拉底 (Socrates)",
            school = "古典希臘哲學",
            reflectionQuestion = "今天有哪些行為或信念，是我習以為常卻從未真正檢驗過的？"
        ),
        DailyQuote(
            quote = "我們不能選擇命運的發牌，但能決定如何出牌。",
            author = "愛比克泰德 (Epictetus)",
            school = "斯多葛學派",
            reflectionQuestion = "當前面臨的煩惱中，哪些完全超出我的掌控？哪些完全取決於我的態度？"
        ),
        DailyQuote(
            quote = "人是被判定為自由的——因為一旦被拋入這個世界，他就必須對自己所做的一切負責。",
            author = "薩特 (Jean-Paul Sartre)",
            school = "存在主義",
            reflectionQuestion = "我是否曾用『身不由己』作為藉口，逃避了自由選擇所帶來的責任？"
        ),
        DailyQuote(
            quote = "反抗荒謬的最好方式，是全身心投入生活並帶著微笑。",
            author = "卡繆 (Albert Camus)",
            school = "荒謬主義",
            reflectionQuestion = "如果世界本沒有預設的意義，我今天想賦予自己何種獨特的意義？"
        ),
        DailyQuote(
            quote = "知人者智，自知者明。勝人者有力，自勝者強。",
            author = "老子 (Laozi)",
            school = "道家哲學",
            reflectionQuestion = "我是在與外在環境較勁，還是在直面內在的執念？"
        ),
        DailyQuote(
            quote = "凡殺不死我的，必使我更強大。",
            author = "尼采 (Friedrich Nietzsche)",
            school = "虛無主義與超人哲學",
            reflectionQuestion = "最近經歷的挫折或困頓，正在鍛造我內心的哪一部分？"
        )
    )

    private val _todayQuote = MutableStateFlow(quotes.random())
    val todayQuote: StateFlow<DailyQuote> = _todayQuote.asStateFlow()

    fun refreshQuote() {
        _todayQuote.value = quotes.random()
    }

    class Factory(
        private val gemmaHelper: GemmaLocalHelper,
        private val userPreferenceStore: UserPreferenceStore,
        private val ttsManager: TtsManager
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AgoraHubViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return AgoraHubViewModel(gemmaHelper, userPreferenceStore, ttsManager) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
