package com.example.gemagora.ui.setup

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.gemagora.ai.ModelManager
import com.example.gemagora.data.datastore.UserPreferenceStore
import com.example.gemagora.data.model.AppearanceSettings
import com.example.gemagora.data.model.ContextTokenLimits
import com.example.gemagora.data.model.LocalModelInfo
import com.example.gemagora.data.model.ModelType
import com.example.gemagora.data.model.SecuritySettings
import com.example.gemagora.data.model.TtsSettings
import com.example.gemagora.data.model.VoiceInfo
import com.example.gemagora.data.repository.ModelRepository
import com.example.gemagora.service.tts.TtsManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ModelSetupViewModel(
    private val modelRepository: ModelRepository,
    private val userPreferenceStore: UserPreferenceStore,
    private val modelManager: ModelManager,
    private val ttsManager: TtsManager
) : ViewModel() {
    private val _isApplyingModelConfig = MutableStateFlow(false)
    val isApplyingModelConfig: StateFlow<Boolean> = _isApplyingModelConfig.asStateFlow()

    val allModels: StateFlow<List<LocalModelInfo>> = modelRepository.allModels
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val downloadStates = modelManager.downloadStates

    val hfToken: StateFlow<String> = userPreferenceStore.hfTokenFlow
        .map { it ?: "" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val gemmaLoadState = modelManager.gemmaHelper.loadState
    val loadedModelName = modelManager.gemmaHelper.loadedModelName

    val appearance: StateFlow<AppearanceSettings?> = userPreferenceStore.appearanceFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun saveAppearance(settings: AppearanceSettings) {
        viewModelScope.launch { userPreferenceStore.saveAppearance(settings) }
    }

    val ttsSettings: StateFlow<TtsSettings> = userPreferenceStore.ttsSettingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TtsSettings())

    val availableVoices: StateFlow<List<VoiceInfo>> = ttsManager.availableVoices
    val isTtsPlaying: StateFlow<Boolean> = ttsManager.isPlaying
    val isTtsReady: StateFlow<Boolean> = ttsManager.isReady

    fun saveTtsSettings(settings: TtsSettings) {
        viewModelScope.launch {
            userPreferenceStore.saveTtsSettings(settings)
        }
    }

    fun previewTts(settings: TtsSettings) {
        ttsManager.preview(settings)
    }

    fun stopTts() {
        ttsManager.stop()
    }

    val securitySettings: StateFlow<SecuritySettings> = userPreferenceStore.securitySettingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SecuritySettings())

    suspend fun verifyPin(pin: String): Boolean = userPreferenceStore.verifyPin(pin)

    fun setPin(pin: String) {
        viewModelScope.launch {
            userPreferenceStore.setPin(pin)
        }
    }

    fun setAppLockEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferenceStore.setAppLockEnabled(enabled)
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferenceStore.setBiometricEnabled(enabled)
        }
    }

    fun setAutoLockTimeout(seconds: Long) {
        viewModelScope.launch {
            userPreferenceStore.setAutoLockTimeout(seconds)
        }
    }

    fun clearPin() {
        viewModelScope.launch {
            userPreferenceStore.clearPin()
        }
    }

    val maxTokens: StateFlow<Int> = userPreferenceStore.maxTokensFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, ContextTokenLimits.DEFAULT)

    val topK: StateFlow<Int> = userPreferenceStore.topKFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, 64)

    val topP: StateFlow<Float> = userPreferenceStore.topPFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0.95f)

    val temperature: StateFlow<Float> = userPreferenceStore.temperatureFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0.85f)

    val useGpu: StateFlow<Boolean> = userPreferenceStore.useGpuFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val enableThinking: StateFlow<Boolean> = userPreferenceStore.enableThinkingFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val enableSpeculative: StateFlow<Boolean> = userPreferenceStore.enableSpeculativeFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val systemPrompt: StateFlow<String> = userPreferenceStore.systemPromptFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    fun saveToken(token: String) {
        viewModelScope.launch {
            userPreferenceStore.saveHfToken(token)
        }
    }

    fun clearToken() {
        viewModelScope.launch {
            userPreferenceStore.clearHfToken()
        }
    }

    fun downloadModel(modelId: String) {
        modelManager.downloadModel(modelId, hfToken.value.takeIf { it.isNotBlank() })
    }

    fun loadModel(model: LocalModelInfo) {
        viewModelScope.launch {
            rememberSelectedModel(model)
            modelManager.loadModel(model)
        }
    }

    fun importLocalFile(modelId: String, uri: Uri) {
        viewModelScope.launch {
            modelManager.importLocalModelFile(modelId, uri)
            val model = modelRepository.getModelById(modelId)
            if (model != null) {
                rememberSelectedModel(model)
                modelManager.loadModel(model)
            }
        }
    }

    fun saveModelConfig(
        maxTokens: Int,
        topK: Int,
        topP: Float,
        temperature: Float,
        useGpu: Boolean,
        enableThinking: Boolean,
        enableSpeculative: Boolean,
        systemPrompt: String
    ) {
        viewModelScope.launch {
            _isApplyingModelConfig.value = true
            try {
                userPreferenceStore.saveModelConfig(
                    maxTokens, topK, topP, temperature, useGpu, enableThinking, enableSpeculative, systemPrompt
                )

                val selectedLlmModelId = userPreferenceStore.selectedLlmModelIdFlow.first()
                val selectedLlmPath = userPreferenceStore.selectedLlmPathFlow.first()
                val currentLoadedModelName = modelManager.gemmaHelper.loadedModelName.value
                val models = modelRepository.allModels.first()
                val selectedModel = selectedLlmModelId?.let { modelRepository.getModelById(it) }
                    ?: models.firstOrNull { model ->
                        model.type == ModelType.LLM && model.localPath == selectedLlmPath
                    }
                    ?: models.firstOrNull { model ->
                        model.type == ModelType.LLM && model.name == currentLoadedModelName
                    }
                    ?: models.firstOrNull { model ->
                        model.type == ModelType.LLM && model.isDownloaded
                    }

                if (selectedModel != null && selectedModel.type == ModelType.LLM && selectedModel.isDownloaded) {
                    rememberSelectedModel(selectedModel)
                    modelManager.gemmaHelper.loadModelSync(selectedModel.localPath, selectedModel.name)
                }
            } finally {
                _isApplyingModelConfig.value = false
            }
        }
    }

    private suspend fun rememberSelectedModel(model: LocalModelInfo) {
        userPreferenceStore.saveSelectedLlmModelId(model.id)
        userPreferenceStore.saveSelectedLlmPath(model.localPath)
    }

    class Factory(
        private val modelRepository: ModelRepository,
        private val userPreferenceStore: UserPreferenceStore,
        private val modelManager: ModelManager,
        private val ttsManager: TtsManager
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ModelSetupViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ModelSetupViewModel(modelRepository, userPreferenceStore, modelManager, ttsManager) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
