package com.example.gemagora.ai

import android.graphics.Bitmap
import com.example.gemagora.data.model.ContextTokenLimits
import com.example.gemagora.data.model.ModelLoadState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf

interface GemmaLocalHelper {
    val contextTokenLimit: Flow<Int>
        get() = flowOf(ContextTokenLimits.DEFAULT)
    val loadState: StateFlow<ModelLoadState>
    val loadedModelName: StateFlow<String?>
    val isInferencing: StateFlow<Boolean>
    val visionReady: StateFlow<Boolean>
    val audioReady: StateFlow<Boolean>

    fun loadModel(modelPath: String, modelName: String)
    suspend fun loadModelSync(modelPath: String, modelName: String): Boolean
    suspend fun generateReply(prompt: String): String
    fun generateReplyFlow(prompt: String): Flow<String>
    fun generateReplyWithMediaFlow(
        prompt: String,
        images: List<Bitmap> = emptyList(),
        audioBytes: ByteArray? = null,
    ): Flow<String> = generateReplyFlow(prompt)
    fun cancelCurrentInference(reason: String = "User requested cancellation")
}
