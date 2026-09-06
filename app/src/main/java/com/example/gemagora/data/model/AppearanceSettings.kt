package com.example.gemagora.data.model

import kotlin.math.abs

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

enum class FontSizeScale(val scale: Float, val label: String, val percentageText: String) {
    SMALL(0.85f, "小", "85%"),
    NORMAL(1.0f, "標準", "100%"),
    LARGE(1.15f, "大", "115%"),
    EXTRA_LARGE(1.30f, "特大", "130%");

    companion object {
        val DEFAULT = NORMAL

        fun fromScale(scale: Float?): FontSizeScale {
            if (scale == null) return DEFAULT
            return entries.minByOrNull { abs(it.scale - scale) } ?: DEFAULT
        }
    }
}

data class AppearanceSettings(
    val useWallpaperColors: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val customHue: Int? = null,
    val customSaturation: Float? = null,
    val customHex: String? = null,
    val fontScale: Float = 1.0f
)
