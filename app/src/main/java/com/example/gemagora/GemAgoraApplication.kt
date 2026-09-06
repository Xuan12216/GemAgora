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
    lateinit var appLockManager: com.example.gemagora.security.AppLockManager

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getDatabase(this)
        userPreferenceStore = UserPreferenceStore(this)
        appLockManager = com.example.gemagora.security.AppLockManager(userPreferenceStore)

        var activityCount = 0
        var isChangingConfig = false
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: android.app.Activity, savedInstanceState: android.os.Bundle?) {}
            override fun onActivityStarted(activity: android.app.Activity) {
                if (++activityCount == 1 && !isChangingConfig) {
                    appLockManager.onAppForeground()
                }
            }
            override fun onActivityResumed(activity: android.app.Activity) {}
            override fun onActivityPaused(activity: android.app.Activity) {}
            override fun onActivityStopped(activity: android.app.Activity) {
                isChangingConfig = activity.isChangingConfigurations
                if (--activityCount == 0 && !isChangingConfig) {
                    appLockManager.onAppBackground()
                }
            }
            override fun onActivitySaveInstanceState(activity: android.app.Activity, outState: android.os.Bundle) {}
            override fun onActivityDestroyed(activity: android.app.Activity) {}
        })

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
