package com.example.gemagora

import android.app.Application
import com.example.gemagora.ai.GemmaLocalHelper
import com.example.gemagora.ai.GemmaLocalHelperImpl
import com.example.gemagora.ai.ModelManager
import com.example.gemagora.data.datastore.UserPreferenceStore
import com.example.gemagora.data.local.AppDatabase
import com.example.gemagora.data.repository.ChatRepository
import com.example.gemagora.data.repository.HistoryRepository
import com.example.gemagora.data.repository.JournalRepository
import com.example.gemagora.data.repository.ModelRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class GemAgoraApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    lateinit var database: AppDatabase
    lateinit var userPreferenceStore: UserPreferenceStore

    lateinit var modelRepository: ModelRepository
    lateinit var chatRepository: ChatRepository
    lateinit var journalRepository: JournalRepository
    lateinit var historyRepository: HistoryRepository

    lateinit var gemmaHelper: GemmaLocalHelper
    lateinit var modelManager: ModelManager
    lateinit var ttsManager: com.example.gemagora.service.tts.TtsManager

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getDatabase(this)
        userPreferenceStore = UserPreferenceStore(this)

        modelRepository = ModelRepository(database.modelDao())
        chatRepository = ChatRepository(database.chatDao())
        journalRepository = JournalRepository(database.journalDao())
        historyRepository = HistoryRepository(database.historyDao())

        gemmaHelper = GemmaLocalHelperImpl(this)
        modelManager = ModelManager(this, modelRepository, userPreferenceStore, gemmaHelper)
        ttsManager = com.example.gemagora.service.tts.TtsManager(this, userPreferenceStore)
    }

    override fun onTerminate() {
        super.onTerminate()
        if (::ttsManager.isInitialized) {
            ttsManager.release()
        }
    }
}
