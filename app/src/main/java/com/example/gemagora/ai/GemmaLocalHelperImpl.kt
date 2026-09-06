package com.example.gemagora.ai

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.SamplerConfig
import com.example.gemagora.data.datastore.UserPreferenceStore
import com.example.gemagora.data.model.ModelLoadState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.atomic.AtomicLong

class GemmaLocalHelperImpl(private val context: Context) : GemmaLocalHelper {

    private val _loadState = MutableStateFlow<ModelLoadState>(ModelLoadState.NotFound)
    override val loadState: StateFlow<ModelLoadState> = _loadState.asStateFlow()

    private val _loadedModelName = MutableStateFlow<String?>(null)
    override val loadedModelName: StateFlow<String?> = _loadedModelName.asStateFlow()

    private val _isInferencing = MutableStateFlow(false)
    override val isInferencing: StateFlow<Boolean> = _isInferencing.asStateFlow()

    private val _visionReady = MutableStateFlow(false)
    override val visionReady: StateFlow<Boolean> = _visionReady.asStateFlow()

    private val _audioReady = MutableStateFlow(false)
    override val audioReady: StateFlow<Boolean> = _audioReady.asStateFlow()

    private var engine: Engine? = null
    private var conversationConfig: ConversationConfig? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var loadJob: Job? = null
    private val modelMutex = Mutex()
    private val inferenceLock = Any()
    private var activeInferenceJob: Job? = null
    private var activeInferenceId: Long? = null
    private val inferenceIdGenerator = AtomicLong(0L)

    private val userPreferenceStore = UserPreferenceStore(context)
    override val contextTokenLimit = userPreferenceStore.maxTokensFlow

    private data class InferenceRequest(
        val id: Long,
        val type: String,
        val job: Job,
        val startedAt: Long
    )

    override fun loadModel(modelPath: String, modelName: String) {
        synchronized(this) {
            loadJob?.cancel()
            loadJob = scope.launch {
                loadModelSync(modelPath, modelName)
            }
        }
    }

    override suspend fun loadModelSync(modelPath: String, modelName: String): Boolean = withContext(Dispatchers.IO) {
        Log.d("GemAgora_AI", "loadModelSync called with path: $modelPath, name: $modelName")
        cancelActiveInference("Gemma model reloading")
        val file = File(modelPath)
        if (!file.exists()) {
            Log.d("GemAgora_AI", "File does not exist: $modelPath")
            _loadState.value = ModelLoadState.NotFound
            _loadedModelName.value = null
            _visionReady.value = false
            _audioReady.value = false
            return@withContext false
        }

        _loadState.value = ModelLoadState.Loading
        _loadedModelName.value = null
        _visionReady.value = false
        _audioReady.value = false

        return@withContext modelMutex.withLock {
            try {
                Log.d("GemAgora_AI", "Starting model initialization...")
                closeResources()

                val modelInstance = createInitializedConversation(modelPath)

                engine = modelInstance.engine
                _visionReady.value = modelInstance.visionEnabled
                _audioReady.value = modelInstance.audioEnabled
                _loadedModelName.value = modelName
                _loadState.value = ModelLoadState.Loaded
                Log.d(
                    "GemAgora_AI",
                    "Loaded model successfully: $modelName, vision=${modelInstance.visionEnabled}, audio=${modelInstance.audioEnabled}"
                )
                true
            } catch (e: Exception) {
                Log.e("GemAgora_AI", "Model loading failed", e)
                _loadedModelName.value = null
                _visionReady.value = false
                _audioReady.value = false
                _loadState.value = ModelLoadState.Failed(e.localizedMessage ?: "模型載入失敗")
                false
            }
        }
    }

    override suspend fun generateReply(prompt: String): String = withContext(Dispatchers.IO) {
        cancelActiveInference("Preempted by new inference request")
        val inference = beginInference("generateReply", prompt)
        awaitModelReady(inference)
        if (_loadState.value !is ModelLoadState.Loaded) {
            return@withContext "錯誤：Gemma 模型尚未載入。請先至設定下載或載入模型。"
        }
        var locked = false
        try {
            modelMutex.lock()
            locked = true
            activateInference(inference)
            val currentConversation = createConversationForInference(inference)
                ?: return@withContext "錯誤：Gemma 模型尚未載入。請先至設定下載或載入模型。"
            try {
                val response = StringBuilder()
                currentConversation.sendMessageAsync(prompt).map { it.toString() }.requireAiResponse().collect { token ->
                    response.append(token)
                }
                response.toString()
            } catch (e: CancellationException) {
                if (locked) cancelConversation(inference)
                throw e
            }
        } finally {
            if (locked) {
                closeInferenceConversation(inference)
                modelMutex.unlock()
            }
            finishInference(inference)
        }
    }

    override fun generateReplyFlow(prompt: String): Flow<String> = flow {
        cancelActiveInference("Preempted by new inference request")
        val inference = beginInference("generateReplyFlow", prompt)
        awaitModelReady(inference)
        val state = _loadState.value
        if (state !is ModelLoadState.Loaded) {
            val reason = if (state is ModelLoadState.Failed) "模型載入失敗：${state.message}" else "Gemma 模型尚未載入。請至設定載入模型。"
            throw IllegalStateException("錯誤：$reason")
        }
        var locked = false
        try {
            modelMutex.lock()
            locked = true
            activateInference(inference)
            val currentConversation = createConversationForInference(inference)
                ?: throw IllegalStateException("錯誤：Gemma 引擎尚未就緒。")
            currentConversation.sendMessageAsync(prompt).map { it.toString() }.requireAiResponse().collect { token ->
                emit(token)
            }
        } catch (e: CancellationException) {
            if (locked) cancelConversation(inference)
            throw e
        } finally {
            if (locked) {
                closeInferenceConversation(inference)
                modelMutex.unlock()
            }
            finishInference(inference)
        }
    }.flowOn(Dispatchers.IO)

    override fun generateReplyWithMediaFlow(
        prompt: String,
        images: List<Bitmap>,
        audioBytes: ByteArray?,
    ): Flow<String> = flow {
        cancelActiveInference("Preempted by new inference request")
        val inference = beginInference("generateReplyWithMediaFlow", prompt)
        awaitModelReady(inference)
        val state = _loadState.value
        if (state !is ModelLoadState.Loaded) {
            val reason = if (state is ModelLoadState.Failed) "模型載入失敗：${state.message}" else "Gemma 模型尚未載入。"
            throw IllegalStateException("錯誤：$reason")
        }
        var locked = false
        try {
            modelMutex.lock()
            locked = true
            activateInference(inference)
            val currentConversation = createConversationForInference(inference)
                ?: throw IllegalStateException("錯誤：Gemma 引擎尚未就緒。")
            val responseFlow = if (images.isEmpty() && audioBytes?.isNotEmpty() != true) {
                currentConversation.sendMessageAsync(prompt)
            } else {
                currentConversation.sendMessageAsync(createMediaContents(prompt, images, audioBytes))
            }
            responseFlow.map { it.toString() }.requireAiResponse().collect { token ->
                emit(token)
            }
        } catch (e: CancellationException) {
            if (locked) cancelConversation(inference)
            throw e
        } finally {
            if (locked) {
                closeInferenceConversation(inference)
                modelMutex.unlock()
            }
            finishInference(inference)
        }
    }.flowOn(Dispatchers.IO)

    private fun createMediaContents(
        prompt: String,
        images: List<Bitmap>,
        audioBytes: ByteArray?,
    ): Contents {
        val contents = buildList<Content> {
            images.forEach { add(Content.ImageBytes(bitmapToPngBytes(it))) }
            add(Content.Text(prompt))
            audioBytes?.takeIf { it.isNotEmpty() }?.let { add(Content.AudioBytes(it)) }
        }
        return Contents.of(contents)
    }

    private fun bitmapToPngBytes(bitmap: Bitmap): ByteArray {
        return ByteArrayOutputStream().use { outputStream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.toByteArray()
        }
    }

    private suspend fun beginInference(type: String, promptOrDetail: String): InferenceRequest {
        val currentJob = currentCoroutineContext()[Job]
            ?: throw IllegalStateException("推論必須在協程中執行")
        val id = inferenceIdGenerator.incrementAndGet()
        val startedAt = System.currentTimeMillis()
        Log.d("GemAgora_AI", "Inference #$id queued: type=$type")
        return InferenceRequest(id = id, type = type, job = currentJob, startedAt = startedAt)
    }

    private fun activateInference(inference: InferenceRequest) {
        synchronized(inferenceLock) {
            activeInferenceJob = inference.job
            activeInferenceId = inference.id
            _isInferencing.value = true
        }
    }

    private fun finishInference(inference: InferenceRequest) {
        synchronized(inferenceLock) {
            if (activeInferenceJob == inference.job) {
                activeInferenceJob = null
                activeInferenceId = null
                _isInferencing.value = false
            }
        }
    }

    override fun cancelCurrentInference(reason: String) {
        cancelActiveInference(reason)
    }

    private fun cancelActiveInference(reason: String) {
        val jobToCancel: Job?
        synchronized(inferenceLock) {
            jobToCancel = activeInferenceJob
            activeInferenceJob = null
            activeInferenceId = null
            _isInferencing.value = false
        }
        try {
            inferenceConversation?.cancelProcess()
            Log.d("GemAgora_AI", "cancelActiveInference: cancelProcess() invoked ($reason)")
        } catch (e: Exception) {
            Log.w("GemAgora_AI", "Failed to cancel native process in cancelActiveInference", e)
        }
        jobToCancel?.cancel(CancellationException(reason))
    }

    private fun cancelConversation(inference: InferenceRequest) {
        try {
            inferenceConversation?.cancelProcess()
            Log.d("GemAgora_AI", "Inference #${inference.id} cancelled native process")
        } catch (e: Exception) {
            Log.w("GemAgora_AI", "Failed to cancel native process", e)
        }
    }

    private var inferenceConversation: Conversation? = null

    private suspend fun awaitModelReady(inference: InferenceRequest) {
        if (_loadState.value is ModelLoadState.Loading) {
            Log.d("GemAgora_AI", "Inference #${inference.id} is waiting for model loading to finish...")
            _loadState.first { it !is ModelLoadState.Loading }
        }
    }

    private suspend fun createConversationForInference(inference: InferenceRequest): Conversation? {
        if (_loadState.value !is ModelLoadState.Loaded) return null
        val currentEngine = engine ?: return null
        val systemPrompt = userPreferenceStore.systemPromptFlow.first()
        val thinking = userPreferenceStore.enableThinkingFlow.first()
        val currentConfig = createConversationConfig(systemPrompt, thinking)
        closeInferenceConversation(inference)
        return currentEngine.createConversation(currentConfig).also {
            inferenceConversation = it
        }
    }

    private fun closeInferenceConversation(inference: InferenceRequest) {
        val current = inferenceConversation ?: return
        try {
            current.close()
        } catch (e: Exception) {
            Log.w("GemAgora_AI", "Error closing conversation", e)
        } finally {
            inferenceConversation = null
        }
    }

    private fun closeResources() {
        try {
            inferenceConversation?.close()
        } catch (_: Exception) {}
        try {
            engine?.close()
        } catch (_: Exception) {}
        inferenceConversation = null
        conversationConfig = null
        engine = null
        _visionReady.value = false
        _audioReady.value = false
    }

    private data class ModelInstance(
        val engine: Engine,
        val visionEnabled: Boolean,
        val audioEnabled: Boolean,
    )

    private suspend fun createInitializedConversation(modelPath: String): ModelInstance {
        val maxTokens = userPreferenceStore.maxTokensFlow.first()
        val useGpu = userPreferenceStore.useGpuFlow.first()
        val systemPrompt = userPreferenceStore.systemPromptFlow.first()
        val speculative = userPreferenceStore.enableSpeculativeFlow.first()
        val thinking = userPreferenceStore.enableThinkingFlow.first()

        try {
            @OptIn(com.google.ai.edge.litertlm.ExperimentalApi::class)
            com.google.ai.edge.litertlm.ExperimentalFlags.enableSpeculativeDecoding = speculative
            Log.d("GemAgora_AI", "Applied enableSpeculativeDecoding = $speculative")
        } catch (e: Exception) {
            Log.w("GemAgora_AI", "Failed to apply speculative decoding flag", e)
        }

        val conversationConfig = createConversationConfig(systemPrompt, thinking)

        val configs = if (useGpu) {
            listOf(
                Triple(
                    EngineConfig(
                        modelPath = modelPath,
                        backend = Backend.GPU(),
                        visionBackend = Backend.GPU(),
                        audioBackend = Backend.CPU(),
                        maxNumTokens = maxTokens,
                    ),
                    true,
                    true,
                ),
                Triple(
                    EngineConfig(modelPath = modelPath, backend = Backend.GPU(), visionBackend = Backend.GPU(), maxNumTokens = maxTokens),
                    true,
                    false,
                ),
                Triple(
                    EngineConfig(modelPath = modelPath, backend = Backend.GPU(), maxNumTokens = maxTokens),
                    false,
                    false,
                ),
                Triple(
                    EngineConfig(modelPath = modelPath, backend = Backend.CPU(), maxNumTokens = maxTokens),
                    false,
                    false,
                ),
            )
        } else {
            listOf(
                Triple(
                    EngineConfig(
                        modelPath = modelPath,
                        backend = Backend.CPU(),
                        visionBackend = Backend.GPU(),
                        audioBackend = Backend.CPU(),
                        maxNumTokens = maxTokens,
                    ),
                    true,
                    true,
                ),
                Triple(
                    EngineConfig(
                        modelPath = modelPath,
                        backend = Backend.CPU(),
                        visionBackend = Backend.CPU(),
                        audioBackend = Backend.CPU(),
                        maxNumTokens = maxTokens,
                    ),
                    true,
                    true,
                ),
                Triple(
                    EngineConfig(modelPath = modelPath, backend = Backend.CPU(), visionBackend = Backend.GPU(), maxNumTokens = maxTokens),
                    true,
                    false,
                ),
                Triple(
                    EngineConfig(modelPath = modelPath, backend = Backend.CPU(), maxNumTokens = maxTokens),
                    false,
                    false,
                ),
            )
        }

        var lastError: Exception? = null
        for ((config, visionEnabled, audioEnabled) in configs) {
            var candidateEngine: Engine? = null
            try {
                Log.d("GemAgora_AI", "Attempting engine init with backend=${config.backend}, maxTokens=${config.maxNumTokens}")
                candidateEngine = Engine(config).also { it.initialize() }
                candidateEngine.createConversation(conversationConfig).close()
                this.conversationConfig = conversationConfig
                return ModelInstance(candidateEngine, visionEnabled, audioEnabled)
            } catch (e: Exception) {
                lastError = e
                Log.w("GemAgora_AI", "Engine config failed, trying fallback...", e)
                try {
                    candidateEngine?.close()
                } catch (_: Exception) {}
            }
        }
        throw lastError ?: IllegalStateException("所有 LiteRT 引擎初始化設定皆失敗")
    }

    private suspend fun createConversationConfig(systemPrompt: String, thinking: Boolean): ConversationConfig {
        val systemInstructionContents = Contents.of(listOf<Content>(Content.Text(systemPrompt)))

        val topK = userPreferenceStore.topKFlow.first()
        val topP = userPreferenceStore.topPFlow.first()
        val temp = userPreferenceStore.temperatureFlow.first()
        val samplerConfig = SamplerConfig(
            topK = topK,
            topP = topP.toDouble(),
            temperature = temp.toDouble(),
            seed = 0
        )

        val extra = mapOf<String, Any>(
            "enable_thinking" to thinking,
            "thinking_token_budget" to (if (thinking) 512 else 0)
        )

        return ConversationConfig(
            systemInstructionContents,
            emptyList(),
            emptyList(),
            samplerConfig,
            false,
            emptyList(),
            extra
        )
    }
}
