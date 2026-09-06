package com.example.gemagora.security

import android.os.SystemClock
import com.example.gemagora.data.datastore.UserPreferenceStore
import com.example.gemagora.data.model.SecuritySettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AppLockManager(
    private val userPreferenceStore: UserPreferenceStore,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main.immediate)
) {

    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private var currentSettings = SecuritySettings()
    private var lastBackgroundTime = 0L
    private var isFirstLaunch = true

    init {
        scope.launch {
            userPreferenceStore.securitySettingsFlow.collectLatest { settings ->
                currentSettings = settings
                // If lock was disabled by user, immediately unlock
                if (!settings.isAppLockEnabled && _isLocked.value) {
                    _isLocked.value = false
                }
            }
        }
    }

    fun onAppForeground() {
        if (currentSettings.isAppLockEnabled) {
            if (isFirstLaunch) {
                _isLocked.value = true
                isFirstLaunch = false
            } else if (lastBackgroundTime > 0L) {
                val elapsedSeconds = (SystemClock.elapsedRealtime() - lastBackgroundTime) / 1000
                if (elapsedSeconds >= currentSettings.autoLockTimeoutSeconds) {
                    _isLocked.value = true
                }
            }
        }
        lastBackgroundTime = 0L
    }

    fun onAppBackground() {
        if (currentSettings.isAppLockEnabled) {
            lastBackgroundTime = SystemClock.elapsedRealtime()
        }
    }

    fun unlock() {
        _isLocked.value = false
        lastBackgroundTime = 0L
    }

    fun lock() {
        if (currentSettings.isAppLockEnabled) {
            _isLocked.value = true
        }
    }

    suspend fun verifyPin(pin: String): Boolean {
        return userPreferenceStore.verifyPin(pin)
    }
}
