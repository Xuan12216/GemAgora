package com.example.gemagora.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.gemagora.data.model.AppearanceSettings
import com.example.gemagora.data.model.ContextTokenLimits
import com.example.gemagora.data.model.ThemeMode
import com.example.gemagora.data.model.TtsSettings
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "gemagora_preferences")

class UserPreferenceStore(private val dataStore: DataStore<Preferences>) {
    constructor(context: Context) : this(context.dataStore)

    private val safePreferencesFlow: Flow<Preferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }

    companion object {
        private val KEY_WALLPAPER_COLORS = booleanPreferencesKey("wallpaper_colors")
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_CUSTOM_HUE = intPreferencesKey("custom_theme_hue")
        private val KEY_CUSTOM_SATURATION = floatPreferencesKey("custom_theme_saturation")
        private val KEY_CUSTOM_HEX = stringPreferencesKey("custom_primary_hex")

        val KEY_SELECTED_LLM_PATH = stringPreferencesKey("selected_llm_path")
        val KEY_SELECTED_LLM_MODEL_ID = stringPreferencesKey("selected_llm_model_id")
        val KEY_HF_TOKEN = stringPreferencesKey("hf_token")

        // Local AI model configuration keys (identical to FitAI architecture)
        val KEY_MAX_TOKENS = intPreferencesKey("max_tokens")
        private val KEY_TOKEN_MIGRATED_V2 = booleanPreferencesKey("token_migrated_v2")
        val KEY_TOP_K = intPreferencesKey("top_k")
        val KEY_TOP_P = floatPreferencesKey("top_p")
        val KEY_TEMPERATURE = floatPreferencesKey("temperature")
        val KEY_USE_GPU = booleanPreferencesKey("use_gpu")
        val KEY_ENABLE_THINKING = booleanPreferencesKey("enable_thinking")
        val KEY_ENABLE_SPECULATIVE = booleanPreferencesKey("enable_speculative")
        val KEY_SYSTEM_PROMPT = stringPreferencesKey("system_prompt")

        // Philosophical preferences
        val KEY_PHILOSOPHICAL_TRADITION = stringPreferencesKey("philosophical_tradition")
        val KEY_SOCRATIC_INTENSITY = stringPreferencesKey("socratic_intensity")

        // TTS preferences
        val KEY_TTS_ENABLED = booleanPreferencesKey("tts_enabled")
        val KEY_TTS_AUTO_SPEAK = booleanPreferencesKey("tts_auto_speak")
        val KEY_TTS_VOICE_NAME = stringPreferencesKey("tts_voice_name")
        val KEY_TTS_PITCH = floatPreferencesKey("tts_pitch")
        val KEY_TTS_SPEED = floatPreferencesKey("tts_speed")
        val KEY_TTS_PRESET_ID = stringPreferencesKey("tts_preset_id")
    }

    val appearanceFlow: Flow<AppearanceSettings> = safePreferencesFlow.map { preferences ->
        val themeModeStr = preferences[KEY_THEME_MODE]
        val themeMode = runCatching { ThemeMode.valueOf(themeModeStr ?: "") }.getOrDefault(ThemeMode.SYSTEM)
        AppearanceSettings(
            useWallpaperColors = preferences[KEY_WALLPAPER_COLORS] ?: false,
            themeMode = themeMode,
            customHue = preferences[KEY_CUSTOM_HUE]?.coerceIn(0, 359),
            customSaturation = preferences[KEY_CUSTOM_SATURATION]?.coerceIn(0.1f, 1.0f),
            customHex = preferences[KEY_CUSTOM_HEX],
        )
    }

    suspend fun saveAppearance(settings: AppearanceSettings) {
        dataStore.edit { preferences ->
            preferences[KEY_WALLPAPER_COLORS] = settings.useWallpaperColors
            preferences[KEY_THEME_MODE] = settings.themeMode.name
            if (settings.customHue == null) preferences.remove(KEY_CUSTOM_HUE)
            else preferences[KEY_CUSTOM_HUE] = settings.customHue.coerceIn(0, 359)
            if (settings.customSaturation == null) preferences.remove(KEY_CUSTOM_SATURATION)
            else preferences[KEY_CUSTOM_SATURATION] = settings.customSaturation.coerceIn(0.1f, 1.0f)
            if (settings.customHex.isNullOrBlank()) preferences.remove(KEY_CUSTOM_HEX)
            else preferences[KEY_CUSTOM_HEX] = settings.customHex.trim()
        }
    }

    val selectedLlmPathFlow: Flow<String?> = safePreferencesFlow.map { preferences ->
        preferences[KEY_SELECTED_LLM_PATH]
    }

    val selectedLlmModelIdFlow: Flow<String?> = safePreferencesFlow.map { preferences ->
        preferences[KEY_SELECTED_LLM_MODEL_ID]
    }

    val hfTokenFlow: Flow<String?> = safePreferencesFlow.map { preferences ->
        preferences[KEY_HF_TOKEN]
    }

    // Config flows
    val maxTokensFlow: Flow<Int> = safePreferencesFlow.map { preferences ->
        val stored = preferences[KEY_MAX_TOKENS]
        val isMigrated = preferences[KEY_TOKEN_MIGRATED_V2] ?: false
        val token = if (!isMigrated && (stored == null || stored <= 2048)) {
            ContextTokenLimits.DEFAULT
        } else {
            stored ?: ContextTokenLimits.DEFAULT
        }
        token.coerceIn(ContextTokenLimits.range)
    }
    val topKFlow: Flow<Int> = safePreferencesFlow.map { preferences ->
        preferences[KEY_TOP_K] ?: 64
    }
    val topPFlow: Flow<Float> = safePreferencesFlow.map { preferences ->
        preferences[KEY_TOP_P] ?: 0.95f
    }
    val temperatureFlow: Flow<Float> = safePreferencesFlow.map { preferences ->
        preferences[KEY_TEMPERATURE] ?: 0.85f
    }
    val useGpuFlow: Flow<Boolean> = safePreferencesFlow.map { preferences ->
        preferences[KEY_USE_GPU] ?: true
    }
    val enableThinkingFlow: Flow<Boolean> = safePreferencesFlow.map { preferences ->
        preferences[KEY_ENABLE_THINKING] ?: true
    }
    val enableSpeculativeFlow: Flow<Boolean> = safePreferencesFlow.map { preferences ->
        preferences[KEY_ENABLE_SPECULATIVE] ?: false
    }
    val systemPromptFlow: Flow<String> = safePreferencesFlow.map { preferences ->
        preferences[KEY_SYSTEM_PROMPT]
            ?: "你是一個深諳西方古典哲學與東方思想的智者蘇格拉底。你擅長運用反詰法（Elenchus）與產婆術，不輕易給出武斷結論，而是通過層層追問、概念辨析與邏輯審視，引導對話者自覺發現矛盾、審視前提，逼近真理本質。請始終使用繁體中文。"
    }

    suspend fun saveSelectedLlmPath(path: String) {
        dataStore.edit { preferences ->
            preferences[KEY_SELECTED_LLM_PATH] = path
        }
    }

    suspend fun saveSelectedLlmModelId(modelId: String) {
        dataStore.edit { preferences ->
            preferences[KEY_SELECTED_LLM_MODEL_ID] = modelId
        }
    }

    suspend fun saveHfToken(token: String) {
        dataStore.edit { preferences ->
            preferences[KEY_HF_TOKEN] = token
        }
    }

    suspend fun clearHfToken() {
        dataStore.edit { preferences ->
            preferences.remove(KEY_HF_TOKEN)
        }
    }

    suspend fun saveModelConfig(
        maxTokens: Int,
        topK: Int,
        topP: Float,
        temperature: Float,
        useGpu: Boolean,
        enableThinking: Boolean,
        enableSpeculative: Boolean,
        systemPrompt: String
    ) {
        dataStore.edit { preferences ->
            preferences[KEY_TOKEN_MIGRATED_V2] = true
            preferences[KEY_MAX_TOKENS] = maxTokens.coerceIn(ContextTokenLimits.range)
            preferences[KEY_TOP_K] = topK
            preferences[KEY_TOP_P] = topP
            preferences[KEY_TEMPERATURE] = temperature
            preferences[KEY_USE_GPU] = useGpu
            preferences[KEY_ENABLE_THINKING] = enableThinking
            preferences[KEY_ENABLE_SPECULATIVE] = enableSpeculative
            preferences[KEY_SYSTEM_PROMPT] = systemPrompt
        }
    }

    val ttsSettingsFlow: Flow<TtsSettings> = safePreferencesFlow.map { preferences ->
        TtsSettings(
            enabled = preferences[KEY_TTS_ENABLED] ?: true,
            autoSpeak = preferences[KEY_TTS_AUTO_SPEAK] ?: false,
            voiceName = preferences[KEY_TTS_VOICE_NAME],
            pitch = preferences[KEY_TTS_PITCH] ?: 0.82f,
            speed = preferences[KEY_TTS_SPEED] ?: 0.95f,
            presetId = preferences[KEY_TTS_PRESET_ID] ?: "socrates"
        )
    }

    suspend fun saveTtsSettings(settings: TtsSettings) {
        dataStore.edit { preferences ->
            preferences[KEY_TTS_ENABLED] = settings.enabled
            preferences[KEY_TTS_AUTO_SPEAK] = settings.autoSpeak
            if (settings.voiceName.isNullOrBlank()) {
                preferences.remove(KEY_TTS_VOICE_NAME)
            } else {
                preferences[KEY_TTS_VOICE_NAME] = settings.voiceName
            }
            preferences[KEY_TTS_PITCH] = settings.pitch
            preferences[KEY_TTS_SPEED] = settings.speed
            preferences[KEY_TTS_PRESET_ID] = settings.presetId
        }
    }
}
