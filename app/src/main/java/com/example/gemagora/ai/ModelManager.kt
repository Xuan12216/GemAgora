package com.example.gemagora.ai

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.gemagora.data.datastore.UserPreferenceStore
import com.example.gemagora.data.model.LocalModelInfo
import com.example.gemagora.data.model.ModelDownloadState
import com.example.gemagora.data.model.ModelType
import com.example.gemagora.data.repository.ModelRepository
import com.example.gemagora.service.ModelDownloadService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

class ModelManager(
    private val context: Context,
    private val modelRepository: ModelRepository,
    private val userPreferenceStore: UserPreferenceStore,
    val gemmaHelper: GemmaLocalHelper
) {
    private val downloader = ModelDownloader()
    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val activeDownloadJobs = mutableMapOf<String, Job>()
    private val initMutex = kotlinx.coroutines.sync.Mutex()
    @Volatile
    private var isInitialized = false

    private val _downloadStates = MutableStateFlow<Map<String, ModelDownloadState>>(emptyMap())
    val downloadStates: StateFlow<Map<String, ModelDownloadState>> = _downloadStates.asStateFlow()

    fun getModelFile(fileName: String): File {
        val dir = File(context.filesDir, "models")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return File(dir, fileName)
    }

    suspend fun initializeDefaultModels() = initMutex.withLock {
        if (isInitialized) return@withLock
        withContext(Dispatchers.IO) {
        val models = listOf(
            LocalModelInfo(
                id = "gemma_4_e2b",
                name = "Gemma 4 E2B Local LLM (2B)",
                type = ModelType.LLM,
                fileName = "gemma-4-E2B-it.litertlm",
                localPath = getModelFile("gemma-4-E2B-it.litertlm").absolutePath,
                sourceUrl = "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm",
                requiresToken = false,
                fileSizeBytes = 2588147712L
            ),
            LocalModelInfo(
                id = "gemma_4_e4b",
                name = "Gemma 4 E4B Local LLM (4B)",
                type = ModelType.LLM,
                fileName = "gemma-4-E4B-it.litertlm",
                localPath = getModelFile("gemma-4-E4B-it.litertlm").absolutePath,
                sourceUrl = "https://huggingface.co/litert-community/gemma-4-E4B-it-litert-lm/resolve/main/gemma-4-E4B-it.litertlm",
                requiresToken = false,
                fileSizeBytes = 3659530240L
            )
        )

        val downloadedLlms = mutableListOf<LocalModelInfo>()
        for (model in models) {
            val file = File(model.localPath)
            if (file.exists()) {
                if (file.length() < 1024) {
                    Log.w("GemAgora_AI", "Model ${model.id} corrupted or empty (<1KB)! Removing...")
                    file.delete()
                }
            }
            val fileExists = file.exists()
            val actualSize = if (fileExists) file.length() else model.fileSizeBytes

            val existing = modelRepository.getModelById(model.id)
            if (existing == null) {
                modelRepository.insertModel(model.copy(isDownloaded = fileExists, fileSizeBytes = actualSize))
            } else {
                modelRepository.updateModel(existing.copy(
                    isDownloaded = fileExists,
                    sourceUrl = model.sourceUrl,
                    requiresToken = model.requiresToken,
                    fileSizeBytes = actualSize
                ))
            }
            if (fileExists) {
                downloadedLlms += model.copy(isDownloaded = true, fileSizeBytes = actualSize)
            }
        }

        val selectedLlmModelId = userPreferenceStore.selectedLlmModelIdFlow.first()
        val selectedLlmPath = userPreferenceStore.selectedLlmPathFlow.first()
        val preferredLlm = downloadedLlms.firstOrNull { it.id == selectedLlmModelId }
            ?: downloadedLlms.firstOrNull { it.localPath == selectedLlmPath }
            ?: downloadedLlms.firstOrNull()
        if (preferredLlm != null) {
            rememberSelectedModel(preferredLlm)
            loadModel(preferredLlm)
        }
        isInitialized = true
        }
    }

    fun downloadModel(modelId: String, token: String?) {
        val intent = Intent(context, ModelDownloadService::class.java).apply {
            putExtra(ModelDownloadService.EXTRA_MODEL_ID, modelId)
            putExtra(ModelDownloadService.EXTRA_TOKEN, token)
        }
        ContextCompat.startForegroundService(context, intent)
    }

    fun downloadModelInBackground(modelId: String, token: String?) {
        synchronized(activeDownloadJobs) {
            if (activeDownloadJobs[modelId]?.isActive == true) return
        }

        _downloadStates.update { it + (modelId to ModelDownloadState.Idle) }

        val job = managerScope.launch(start = CoroutineStart.LAZY) {
            val model = modelRepository.getModelById(modelId) ?: run {
                _downloadStates.update { it + (modelId to ModelDownloadState.Failed("未找到模型中繼資料")) }
                return@launch
            }
            val destinationFile = File(model.localPath)
            var finalState: ModelDownloadState = ModelDownloadState.Idle

            try {
                downloader.downloadModel(
                    url = model.sourceUrl ?: "",
                    token = token,
                    destinationFile = destinationFile,
                    onProgress = { state ->
                        finalState = state
                        _downloadStates.update { it + (modelId to state) }
                    }
                )

                when (finalState) {
                    is ModelDownloadState.Completed -> {
                        val updatedModel = model.copy(isDownloaded = true)
                        modelRepository.updateModel(updatedModel)
                    }
                    is ModelDownloadState.Failed,
                    ModelDownloadState.Cancelled -> {
                        destinationFile.delete()
                        modelRepository.updateModel(model.copy(isDownloaded = false))
                    }
                    else -> Unit
                }
            } finally {
                synchronized(activeDownloadJobs) {
                    activeDownloadJobs.remove(modelId)
                }
            }
        }

        synchronized(activeDownloadJobs) {
            activeDownloadJobs[modelId] = job
        }
        job.start()
    }

    fun loadModel(model: LocalModelInfo) {
        val file = File(model.localPath)
        if (!file.exists() || file.length() < 1024) {
            file.delete()
            managerScope.launch {
                modelRepository.updateModel(model.copy(isDownloaded = false))
            }
            return
        }

        Log.d("GemAgora_AI", "Loading LLM: ${model.name}")
        managerScope.launch {
            rememberSelectedModel(model)
        }
        gemmaHelper.loadModel(model.localPath, model.name)
    }

    suspend fun importLocalModelFile(modelId: String, uri: android.net.Uri) = withContext(Dispatchers.IO) {
        val model = modelRepository.getModelById(modelId) ?: return@withContext
        val destFile = File(model.localPath)
        val tempFile = File(destFile.parentFile, "${destFile.name}.import_tmp")
        destFile.parentFile?.mkdirs()

        try {
            tempFile.delete()
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                java.io.FileOutputStream(tempFile).use { outputStream ->
                    val buffer = ByteArray(16384)
                    var bytesRead: Int
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                    }
                    outputStream.flush()
                }
            } ?: throw java.io.IOException("無法讀取所選檔案 URI")

            if (tempFile.length() < 1024) {
                tempFile.delete()
                throw java.io.IOException("匯入檔案為空或無效 (<1KB)")
            }

            if (destFile.exists()) destFile.delete()
            if (!tempFile.renameTo(destFile)) {
                throw java.io.IOException("無法重命名並儲存模型檔案")
            }

            val updated = model.copy(isDownloaded = true, fileSizeBytes = destFile.length())
            modelRepository.updateModel(updated)
        } catch (e: Exception) {
            tempFile.delete()
            throw e
        }
    }

    private suspend fun rememberSelectedModel(model: LocalModelInfo) {
        userPreferenceStore.saveSelectedLlmModelId(model.id)
        userPreferenceStore.saveSelectedLlmPath(model.localPath)
    }
}
