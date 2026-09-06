package com.example.gemagora.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.gemagora.GemAgoraApplication
import com.example.gemagora.MainActivity
import com.example.gemagora.R
import com.example.gemagora.data.model.ModelDownloadState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect

class ModelDownloadService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val monitorJobs = mutableMapOf<String, Job>()
    private var hasForegroundNotification = false
    private lateinit var notificationManager: NotificationManager

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val modelId = intent?.getStringExtra(EXTRA_MODEL_ID) ?: return START_NOT_STICKY
        val token = intent.getStringExtra(EXTRA_TOKEN)
        val notificationId = notificationIdFor(modelId)
        val app = application as GemAgoraApplication

        val initialNotification = buildNotification(
            title = "GemAgora 模型下載",
            text = "$modelId: 正在準備下載...",
            progress = 0,
            indeterminate = true,
            ongoing = true
        )

        if (!hasForegroundNotification) {
            val foregroundServiceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            } else {
                0
            }
            ServiceCompat.startForeground(this, notificationId, initialNotification, foregroundServiceType)
            hasForegroundNotification = true
        } else {
            notificationManager.notify(notificationId, initialNotification)
        }

        if (monitorJobs[modelId]?.isActive != true) {
            monitorJobs[modelId] = serviceScope.launch {
                app.modelManager.downloadStates.collect { states ->
                    val state = states[modelId] ?: return@collect
                    when (state) {
                        is ModelDownloadState.Downloading -> {
                            val progress = (state.progress * 100).toInt().coerceIn(0, 100)
                            notificationManager.notify(
                                notificationId,
                                buildNotification(
                                    title = "GemAgora 模型下載中",
                                    text = "$modelId: $progress% (${state.downloadedBytes.toMb()}MB / ${state.totalBytes.toMb()}MB)",
                                    progress = progress,
                                    indeterminate = state.totalBytes <= 0,
                                    ongoing = true
                                )
                            )
                        }
                        ModelDownloadState.Completed -> {
                            notificationManager.notify(
                                notificationId,
                                buildNotification(
                                    title = "模型下載完成",
                                    text = "$modelId 已下載完成，可於模型管理中載入使用。",
                                    progress = 100,
                                    indeterminate = false,
                                    ongoing = false
                                )
                            )
                            delay(1500)
                            finishMonitoring(modelId)
                            currentCoroutineContext().cancel()
                        }
                        is ModelDownloadState.Failed -> {
                            notificationManager.notify(
                                notificationId,
                                buildNotification(
                                    title = "模型下載失敗",
                                    text = "$modelId: ${state.message}",
                                    progress = 0,
                                    indeterminate = false,
                                    ongoing = false
                                )
                            )
                            delay(3000)
                            finishMonitoring(modelId)
                            currentCoroutineContext().cancel()
                        }
                        ModelDownloadState.Cancelled -> {
                            notificationManager.notify(
                                notificationId,
                                buildNotification(
                                    title = "模型下載已取消",
                                    text = "$modelId 下載已中止。",
                                    progress = 0,
                                    indeterminate = false,
                                    ongoing = false
                                )
                            )
                            finishMonitoring(modelId)
                            currentCoroutineContext().cancel()
                        }
                        ModelDownloadState.Idle -> Unit
                    }
                }
            }
        }

        app.modelManager.downloadModelInBackground(modelId, token)
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        monitorJobs.values.forEach { it.cancel() }
        monitorJobs.clear()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun finishMonitoring(modelId: String) {
        monitorJobs.remove(modelId)
        if (monitorJobs.isEmpty()) {
            hasForegroundNotification = false
            stopSelf()
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "模型下載進度",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "顯示 GemAgora 本地端 AI 模型下載進度"
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun buildNotification(
        title: String,
        text: String,
        progress: Int,
        indeterminate: Boolean,
        ongoing: Boolean
    ) = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(R.mipmap.ic_launcher)
        .setContentTitle(title)
        .setContentText(text)
        .setContentIntent(contentIntent())
        .setOnlyAlertOnce(true)
        .setOngoing(ongoing)
        .setProgress(100, progress, indeterminate)
        .setGroup(NOTIFICATION_GROUP)
        .build()

    private fun contentIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getActivity(this, 0, intent, flags)
    }

    private fun notificationIdFor(modelId: String): Int = 1000 + (modelId.hashCode() and 0x7fffffff) % 9000

    private fun Long.toMb(): Long = if (this > 0) this / 1024 / 1024 else 0

    companion object {
        const val EXTRA_MODEL_ID = "extra_model_id"
        const val EXTRA_TOKEN = "extra_token"
        private const val CHANNEL_ID = "gemagora_model_downloads"
        private const val NOTIFICATION_GROUP = "gemagora_model_download_group"
    }
}
