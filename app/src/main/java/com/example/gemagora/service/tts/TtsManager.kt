package com.example.gemagora.service.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import com.example.gemagora.data.datastore.UserPreferenceStore
import com.example.gemagora.data.model.TtsPreset
import com.example.gemagora.data.model.TtsSettings
import com.example.gemagora.data.model.VoiceGender
import com.example.gemagora.data.model.VoiceInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale

class TtsManager(
    private val context: Context,
    private val userPreferenceStore: UserPreferenceStore
) : TextToSpeech.OnInitListener {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private val _currentSpeakingUtteranceId = MutableStateFlow<String?>(null)
    val currentSpeakingUtteranceId: StateFlow<String?> = _currentSpeakingUtteranceId.asStateFlow()

    // Tracking for Pause / Resume
    private var currentUtteranceId: String? = null
    private var currentFullSanitizedText: String = ""
    private var currentSpeakingCharOffset: Int = 0
    private var currentSettings: TtsSettings? = null
    private var speechStartTimeMs: Long = 0L

    private val _availableVoices = MutableStateFlow<List<VoiceInfo>>(emptyList())
    val availableVoices: StateFlow<List<VoiceInfo>> = _availableVoices.asStateFlow()

    init {
        try {
            tts = TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to instantiate TextToSpeech: ${e.message}", e)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            setupTtsEngine()
            _isReady.value = true
            Log.d(TAG, "TextToSpeech initialized successfully.")
        } else {
            isInitialized = false
            _isReady.value = false
            Log.e(TAG, "TextToSpeech initialization failed with status: $status")
        }
    }

    private fun setupTtsEngine() {
        val engine = tts ?: return

        // Set listener for speech callbacks
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                scope.launch {
                    _isPlaying.value = true
                    _isPaused.value = false
                    _currentSpeakingUtteranceId.value = utteranceId
                }
            }

            override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
                if (utteranceId != null && utteranceId == currentUtteranceId) {
                    currentSpeakingCharOffset = start
                }
            }

            override fun onDone(utteranceId: String?) {
                scope.launch {
                    _isPlaying.value = false
                    _isPaused.value = false
                    _currentSpeakingUtteranceId.value = null
                    resetPlaybackState()
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                scope.launch {
                    _isPlaying.value = false
                    _isPaused.value = false
                    _currentSpeakingUtteranceId.value = null
                    resetPlaybackState()
                }
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                Log.w(TAG, "TTS utterance error: id=$utteranceId, code=$errorCode")
                scope.launch {
                    _isPlaying.value = false
                    _isPaused.value = false
                    _currentSpeakingUtteranceId.value = null
                    resetPlaybackState()
                }
            }
        })

        // Best effort language default
        val twLocale = Locale.TRADITIONAL_CHINESE
        val langResult = engine.isLanguageAvailable(twLocale)
        if (langResult >= TextToSpeech.LANG_AVAILABLE) {
            engine.language = twLocale
        } else {
            engine.language = Locale.getDefault()
        }

        // Query available voices
        queryAvailableVoices()
    }

    private fun queryAvailableVoices() {
        val engine = tts ?: return
        try {
            val allVoices = engine.voices
            if (allVoices != null && allVoices.isNotEmpty()) {
                // Filter relevant languages (Chinese variants, English, or device default)
                val preferredLangs = setOf("zh", "cmn", "en", Locale.getDefault().language)
                val filtered = allVoices.filter { voice ->
                    preferredLangs.contains(voice.locale.language)
                }.sortedWith(
                    compareBy<Voice> { voice ->
                        // Prioritize Traditional Chinese (zh-TW) first, then other Chinese, then device default, then English
                        when {
                            voice.locale.language == "zh" && voice.locale.country.equals("TW", ignoreCase = true) -> 0
                            voice.locale.language == "zh" -> 1
                            voice.locale.language == Locale.getDefault().language -> 2
                            else -> 3
                        }
                    }.thenBy { it.name }
                )

                var femaleCount = 1
                var maleCount = 1
                var otherCount = 1

                val mappedList = filtered.map { voice ->
                    val gender = determineGender(voice)
                    val count = when (gender) {
                        VoiceGender.FEMALE -> femaleCount++
                        VoiceGender.MALE -> maleCount++
                        VoiceGender.NEUTRAL -> otherCount++
                    }
                    val displayName = formatVoiceDisplayName(voice, gender, count)
                    VoiceInfo(
                        name = voice.name,
                        displayName = displayName,
                        locale = voice.locale.toLanguageTag(),
                        isNetworkRequired = voice.isNetworkConnectionRequired,
                        genderHint = gender
                    )
                }

                _availableVoices.value = mappedList
                Log.d(TAG, "Discovered ${mappedList.size} TTS voices.")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error querying TTS voices: ${e.message}")
        }
    }

    private fun determineGender(voice: Voice): VoiceGender {
        val nameLower = voice.name.lowercase()
        val featuresLower = voice.features?.joinToString(" ")?.lowercase() ?: ""
        return when {
            featuresLower.contains("gender=female") || featuresLower.contains("female") ||
                    nameLower.contains("female") || nameLower.contains("cwf") || nameLower.contains("f0") -> VoiceGender.FEMALE

            featuresLower.contains("gender=male") || featuresLower.contains("male") ||
                    nameLower.contains("male") || nameLower.contains("ctd") || nameLower.contains("ctc") || nameLower.contains("m0") -> VoiceGender.MALE

            else -> VoiceGender.NEUTRAL
        }
    }

    private fun formatVoiceDisplayName(voice: Voice, gender: VoiceGender, index: Int): String {
        val langName = when (voice.locale.language) {
            "zh" -> if (voice.locale.country.equals("TW", ignoreCase = true)) "繁中 (台灣)" else "中文"
            "en" -> "英文 (${voice.locale.country})"
            else -> voice.locale.displayLanguage
        }
        val genderStr = when (gender) {
            VoiceGender.MALE -> "男聲"
            VoiceGender.FEMALE -> "女聲"
            VoiceGender.NEUTRAL -> "聲線"
        }
        val netStr = if (voice.isNetworkConnectionRequired) " [網路]" else ""
        return "$langName $genderStr #$index$netStr"
    }

    fun speak(text: String, utteranceId: String, settings: TtsSettings) {
        if (!settings.enabled) return
        val engine = tts ?: return
        if (!isInitialized) return

        val sanitizedText = sanitizeTextForSpeech(text)
        if (sanitizedText.isBlank()) return

        stopInternal()

        currentUtteranceId = utteranceId
        currentFullSanitizedText = sanitizedText
        currentSpeakingCharOffset = 0
        currentSettings = settings
        speechStartTimeMs = System.currentTimeMillis()

        applySettings(engine, settings)

        _isPlaying.value = true
        _isPaused.value = false
        _currentSpeakingUtteranceId.value = utteranceId

        engine.speak(sanitizedText, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun pause() {
        if (!_isPlaying.value) return
        val engine = tts ?: return

        // Fallback offset calculation if onRangeStart wasn't fired or is still 0
        if (currentSpeakingCharOffset == 0 && speechStartTimeMs > 0L && currentSettings != null) {
            val elapsedSec = (System.currentTimeMillis() - speechStartTimeMs) / 1000f
            // Estimate ~4.2 Chinese characters per second at 1.0x speed
            val speedFactor = currentSettings!!.speed.coerceIn(0.5f, 2.0f)
            val estimatedChars = (elapsedSec * 4.2f * speedFactor).toInt()
            currentSpeakingCharOffset = estimatedChars.coerceIn(0, currentFullSanitizedText.length)
        }

        try {
            engine.stop()
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping TTS for pause: ${e.message}")
        }

        _isPlaying.value = false
        _isPaused.value = true
    }

    fun resume() {
        if (!_isPaused.value) return
        val engine = tts ?: return
        val utteranceId = currentUtteranceId ?: return
        val fullText = currentFullSanitizedText
        val settings = currentSettings ?: return

        val resumeOffset = findResumeSentenceStart(fullText, currentSpeakingCharOffset)
        val remainingText = if (resumeOffset < fullText.length) {
            fullText.substring(resumeOffset).trimStart()
        } else {
            ""
        }

        if (remainingText.isBlank()) {
            stop()
            return
        }

        currentSpeakingCharOffset = resumeOffset
        speechStartTimeMs = System.currentTimeMillis()

        applySettings(engine, settings)

        _isPlaying.value = true
        _isPaused.value = false
        _currentSpeakingUtteranceId.value = utteranceId

        engine.speak(remainingText, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun togglePlayPause(text: String, utteranceId: String, settings: TtsSettings) {
        if (_isPlaying.value && _currentSpeakingUtteranceId.value == utteranceId) {
            pause()
        } else if (_isPaused.value && currentUtteranceId == utteranceId) {
            resume()
        } else {
            speak(text, utteranceId, settings)
        }
    }

    private fun findResumeSentenceStart(text: String, targetOffset: Int): Int {
        if (targetOffset <= 0) return 0
        if (targetOffset >= text.length) return text.length

        // Search backwards up to 35 characters for sentence delimiters
        val searchStart = (targetOffset - 35).coerceAtLeast(0)
        val sub = text.substring(searchStart, targetOffset)
        val delimiters = charArrayOf('。', '！', '？', '；', '\n', '，', '、')
        val lastDelimIndex = sub.lastIndexOfAny(delimiters)
        return if (lastDelimIndex != -1) {
            searchStart + lastDelimIndex + 1
        } else {
            targetOffset
        }
    }

    fun preview(settings: TtsSettings) {
        val previewUtteranceId = "tts_preview_sample"
        if (_isPlaying.value && _currentSpeakingUtteranceId.value == previewUtteranceId) {
            stop()
            return
        }

        val sampleText = when (settings.presetId) {
            "socrates" -> "認識你自己。未經審視的人生，是不值得活的。"
            "aspasia" -> "真正的智慧，在於善用語言洞悉靈魂，引導理性的共鳴。"
            "stoic" -> "我們無法掌控外在的世界，但能完全掌控自己的心靈。"
            "youth" -> "探索真理是青年最崇高的追求，讓我們開始辯證吧！"
            else -> "這是 GemAgora 語音朗讀測試。請確認當前音色與語速是否滿意。"
        }

        speak(sampleText, previewUtteranceId, settings)
    }

    fun stop() {
        stopInternal()
        resetPlaybackState()
    }

    private fun stopInternal() {
        try {
            tts?.stop()
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping TTS: ${e.message}")
        }
        _isPlaying.value = false
        _isPaused.value = false
        _currentSpeakingUtteranceId.value = null
    }

    private fun resetPlaybackState() {
        currentUtteranceId = null
        currentFullSanitizedText = ""
        currentSpeakingCharOffset = 0
        currentSettings = null
        speechStartTimeMs = 0L
    }

    private fun applySettings(engine: TextToSpeech, settings: TtsSettings) {
        // Apply Voice if custom voice is selected
        if (!settings.voiceName.isNullOrBlank()) {
            val matchedVoice = engine.voices?.firstOrNull { it.name == settings.voiceName }
            if (matchedVoice != null) {
                engine.voice = matchedVoice
            }
        } else {
            // Pick a matching voice based on preset gender hint if available
            val preset = TtsSettings.findPreset(settings.presetId)
            val matchedVoice = engine.voices?.firstOrNull { voice ->
                voice.locale.language == "zh" && determineGender(voice) == preset.genderHint
            } ?: engine.voices?.firstOrNull { voice ->
                voice.locale.language == "zh"
            }

            if (matchedVoice != null) {
                engine.voice = matchedVoice
            }
        }

        // Apply Pitch (0.5 to 2.0)
        engine.setPitch(settings.pitch.coerceIn(0.5f, 2.0f))

        // Apply Speech Rate (0.5 to 2.0)
        engine.setSpeechRate(settings.speed.coerceIn(0.5f, 2.0f))
    }

    fun sanitizeTextForSpeech(rawText: String): String {
        if (rawText.isBlank()) return ""

        // Normalize newlines
        var cleaned = rawText.replace("\r\n", "\n").replace("\r", "\n")

        // 1. Remove XML/channel style thoughts (Gemma 4 / thinking tags)
        cleaned = cleaned.replace(Regex("<thought>[\\s\\S]*?</thought>", RegexOption.IGNORE_CASE), "")
        cleaned = cleaned.replace(Regex("<think>[\\s\\S]*?</think>", RegexOption.IGNORE_CASE), "")
        cleaned = cleaned.replace(Regex("<\\|?channel\\|?>thought[\\s\\S]*?(?=(<\\|?channel\\|?>|$))", RegexOption.IGNORE_CASE), "")
        cleaned = cleaned.replace(Regex("<\\|?[a-zA-Z0-9_\\|]+>"), "")
        cleaned = cleaned.replace(Regex("<[^>]+>"), "")

        // 2. Remove code blocks and inline code
        cleaned = cleaned.replace(Regex("```[\\s\\S]*?```"), "")
        cleaned = cleaned.replace(Regex("`[^`\n]+`"), "")

        // 3. Remove horizontal divider rules (---, ***, ___)
        cleaned = cleaned.replace(Regex("""^\s*[-*_]{3,}\s*$""", RegexOption.MULTILINE), "")

        // 4. Remove Markdown headers (e.g. #, ##, ###, ####, #####, ######)
        // Matches at line start with any whitespace, whether followed by space or immediately followed by text
        cleaned = cleaned.replace(Regex("""^\s*#{1,6}\s*""", RegexOption.MULTILINE), "")

        // 5. Remove bold / italic / strikethrough markdown markers
        cleaned = cleaned.replace(Regex("""\*\*(.*?)\*\*""", RegexOption.DOT_MATCHES_ALL), "$1")
        cleaned = cleaned.replace(Regex("""\*(.*?)\*""", RegexOption.DOT_MATCHES_ALL), "$1")
        cleaned = cleaned.replace(Regex("""__(.*?)__""", RegexOption.DOT_MATCHES_ALL), "$1")
        cleaned = cleaned.replace(Regex("""_(.*?)_""", RegexOption.DOT_MATCHES_ALL), "$1")
        cleaned = cleaned.replace(Regex("""~~(.*?)~~""", RegexOption.DOT_MATCHES_ALL), "$1")

        // 6. Remove blockquotes and list markers
        cleaned = cleaned.replace(Regex("""^\s*>\s*""", RegexOption.MULTILINE), "")
        cleaned = cleaned.replace(Regex("""^\s*[\*\-\+]\s+""", RegexOption.MULTILINE), "")
        cleaned = cleaned.replace(Regex("""^\s*\d+[\.\)]\s+""", RegexOption.MULTILINE), "")

        // 7. Remove markdown links and images: [text](url) -> text, ![alt](url) -> alt
        cleaned = cleaned.replace(Regex("""!\[([^\]]*)\]\([^\)]+\)"""), "$1")
        cleaned = cleaned.replace(Regex("""\[([^\]]+)\]\([^\)]+\)"""), "$1")

        // 8. Remove markdown tables formatting
        cleaned = cleaned.replace(Regex("""^\s*\|.*\|\s*$""", RegexOption.MULTILINE)) { match ->
            val line = match.value
            if (line.contains("---") || line.contains("===")) ""
            else line.replace("|", " ").trim()
        }

        // 9. Remove any remaining stray markdown characters that TTS shouldn't pronounce
        // In particular, any leftover '#' MUST be removed so TTS never pronounces '井號' or 'hashtag'!
        cleaned = cleaned
            .replace("#", "")
            .replace("*", "")
            .replace("`", "")
            .replace("~", "")
            .replace("|", " ")
            // Replace multiple consecutive dashes with a slight natural Chinese comma pause
            .replace(Regex("-{2,}"), "，")

        // 10. Clean up excessive empty lines
        cleaned = cleaned.replace(Regex("\n{3,}"), "\n\n").trim()

        return cleaned
    }

    fun release() {
        try {
            tts?.stop()
            tts?.shutdown()
            tts = null
        } catch (e: Exception) {
            Log.w(TAG, "Error shutting down TTS: ${e.message}")
        }
        isInitialized = false
        _isReady.value = false
        _isPlaying.value = false
        _isPaused.value = false
        _currentSpeakingUtteranceId.value = null
        resetPlaybackState()
    }

    companion object {
        private const val TAG = "TtsManager"
    }
}
