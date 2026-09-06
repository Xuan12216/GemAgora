package com.example.gemagora.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

enum class BiometricAvailability(val label: String, val isAvailable: Boolean) {
    AVAILABLE("已就緒並支援生物辨識", true),
    NO_HARDWARE("本機無生物辨識感應硬體", false),
    HARDWARE_UNAVAILABLE("感應硬體目前無法使用", false),
    NONE_ENROLLED("本機尚未註冊任何指紋或生物特徵", false),
    SECURITY_UPDATE_REQUIRED("需進行安全性更新", false),
    UNSUPPORTED("系統不支援生物辨識", false)
}

sealed class BiometricAuthResult {
    data object Success : BiometricAuthResult()
    data object Failed : BiometricAuthResult()
    data class Error(val errorCode: Int, val errString: CharSequence) : BiometricAuthResult()
    data object UserCanceled : BiometricAuthResult()
}

object BiometricHelper {

    private const val AUTHENTICATORS =
        BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK

    fun checkBiometricAvailability(context: Context): BiometricAvailability {
        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(AUTHENTICATORS)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricAvailability.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricAvailability.NO_HARDWARE
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricAvailability.HARDWARE_UNAVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricAvailability.NONE_ENROLLED
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> BiometricAvailability.SECURITY_UPDATE_REQUIRED
            BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> BiometricAvailability.UNSUPPORTED
            else -> BiometricAvailability.UNSUPPORTED
        }
    }

    fun authenticate(
        activity: FragmentActivity,
        title: String = "GemAgora 隱私驗證",
        subtitle: String = "請使用指紋或生物辨識進行解鎖",
        negativeButtonText: String = "使用密碼",
        onResult: (BiometricAuthResult) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onResult(BiometricAuthResult.Success)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                if (errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                    errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON
                ) {
                    onResult(BiometricAuthResult.UserCanceled)
                } else {
                    onResult(BiometricAuthResult.Error(errorCode, errString))
                }
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                onResult(BiometricAuthResult.Failed)
            }
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText(negativeButtonText)
            .setAllowedAuthenticators(AUTHENTICATORS)
            .build()

        val prompt = BiometricPrompt(activity, executor, callback)
        prompt.authenticate(promptInfo)
    }
}
