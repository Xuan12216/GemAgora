package com.example.gemagora.data.model

data class SecuritySettings(
    val isAppLockEnabled: Boolean = false,
    val isBiometricEnabled: Boolean = false,
    val hasPin: Boolean = false,
    val autoLockTimeoutSeconds: Long = 0L,
    val pinLength: Int = 4
) {
    val autoLockTimeoutLabel: String
        get() = when (autoLockTimeoutSeconds) {
            0L -> "立即鎖定"
            60L -> "閒置 1 分鐘"
            300L -> "閒置 5 分鐘"
            900L -> "閒置 15 分鐘"
            else -> "${autoLockTimeoutSeconds} 秒"
        }

    companion object {
        val TIMEOUT_OPTIONS = listOf(
            0L to "立即鎖定",
            60L to "1 分鐘",
            300L to "5 分鐘",
            900L to "15 分鐘"
        )
    }
}
