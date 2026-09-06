package com.example.gemagora

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.gemagora.data.model.AppearanceSettings
import com.example.gemagora.data.model.SecuritySettings
import com.example.gemagora.data.model.ThemeMode
import com.example.gemagora.security.BiometricAuthResult
import com.example.gemagora.security.BiometricHelper
import com.example.gemagora.theme.GemAgoraTheme
import com.example.gemagora.ui.security.LockScreen

class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as GemAgoraApplication

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT)
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.statusBarColor = Color.TRANSPARENT
            window.navigationBarColor = Color.TRANSPARENT
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        }

        setContent {
            val appearance by app.userPreferenceStore.appearanceFlow
                .collectAsStateWithLifecycle(initialValue = AppearanceSettings())
            val isLocked by app.appLockManager.isLocked
                .collectAsStateWithLifecycle()
            val securitySettings by app.userPreferenceStore.securitySettingsFlow
                .collectAsStateWithLifecycle(initialValue = SecuritySettings())

            val systemDark = isSystemInDarkTheme()
            val isDark = when (appearance.themeMode) {
                ThemeMode.SYSTEM -> systemDark
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            GemAgoraTheme(
                darkTheme = isDark,
                dynamicColor = appearance.useWallpaperColors,
                customHue = appearance.customHue,
                customSaturation = appearance.customSaturation,
                fontScale = appearance.fontScale
            ) {
                LaunchedEffect(Unit) {
                    app.modelManager.initializeDefaultModels()
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (isLocked) {
                        LockScreen(
                            onVerifyPin = { pin ->
                                val valid = app.appLockManager.verifyPin(pin)
                                if (valid) {
                                    app.appLockManager.unlock()
                                }
                                valid
                            },
                            onUnlockSuccess = {
                                app.appLockManager.unlock()
                            },
                            isBiometricEnabled = securitySettings.isBiometricEnabled,
                            pinLength = securitySettings.pinLength,
                            onBiometricRequest = {
                                if (securitySettings.isBiometricEnabled) {
                                    BiometricHelper.authenticate(
                                        activity = this@MainActivity,
                                        title = "GemAgora 隱私驗證",
                                        subtitle = "請使用指紋或生物辨識進行解鎖",
                                        negativeButtonText = "使用密碼"
                                    ) { result ->
                                        if (result is BiometricAuthResult.Success) {
                                            app.appLockManager.unlock()
                                        }
                                    }
                                }
                            }
                        )
                    } else {
                        MainNavigation()
                    }
                }
            }
        }
    }
}
