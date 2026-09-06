package com.example.gemagora.data.model

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

data class AppearanceSettings(
    val useWallpaperColors: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val customHue: Int? = null,
    val customSaturation: Float? = null,
    val customHex: String? = null
)
