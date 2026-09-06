package com.example.gemagora.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

internal fun customColorScheme(hue: Int, dark: Boolean, saturation: Float = 0.52f): ColorScheme {
    val satRatio = (saturation / 0.52f).coerceIn(0.2f, 2.0f)
    fun tone(baseSaturation: Float, lightness: Float, offset: Int = 0) =
        Color.hsl(
            ((hue + offset) % 360 + 360).rem(360).toFloat(),
            (baseSaturation * satRatio).coerceIn(0f, 1f),
            lightness
        )
    fun foreground(color: Color) = if (color.luminance() > 0.20f) Color(0xFF1B1B18) else Color.White

    val primary = tone(0.65f, if (dark) 0.76f else 0.38f)
    val secondary = tone(0.30f, if (dark) 0.76f else 0.36f, 35)
    val tertiary = tone(0.40f, if (dark) 0.78f else 0.38f, 100)
    val primaryContainer = tone(0.55f, if (dark) 0.22f else 0.82f)
    val onPrimaryContainer = if (dark) tone(0.65f, 0.92f) else tone(0.70f, 0.15f)
    val secondaryContainer = tone(0.32f, if (dark) 0.22f else 0.86f, 35)
    val onSecondaryContainer = if (dark) tone(0.32f, 0.92f, 35) else tone(0.35f, 0.16f, 35)
    val tertiaryContainer = tone(0.32f, if (dark) 0.22f else 0.86f, 100)
    val onTertiaryContainer = if (dark) tone(0.32f, 0.92f, 100) else tone(0.35f, 0.16f, 100)
    val surface = tone(0.12f, if (dark) 0.08f else 0.96f)
    val onSurface = if (dark) tone(0.12f, 0.91f) else Color(0xFF1B1B18)
    val onSurfaceVariant = if (dark) tone(0.10f, 0.76f) else Color(0xFF49463D)

    return (if (dark) darkColorScheme() else lightColorScheme()).copy(
        primary = primary, onPrimary = foreground(primary),
        primaryContainer = primaryContainer, onPrimaryContainer = onPrimaryContainer,
        secondary = secondary, onSecondary = foreground(secondary),
        secondaryContainer = secondaryContainer, onSecondaryContainer = onSecondaryContainer,
        tertiary = tertiary, onTertiary = foreground(tertiary),
        tertiaryContainer = tertiaryContainer, onTertiaryContainer = onTertiaryContainer,
        background = surface, onBackground = onSurface,
        surface = surface, onSurface = onSurface, surfaceTint = primary,
        surfaceVariant = tone(0.14f, if (dark) 0.22f else 0.88f),
        onSurfaceVariant = onSurfaceVariant,
        surfaceContainerLowest = if (dark) tone(0.12f, 0.06f) else Color.White,
        surfaceContainerLow = tone(0.12f, if (dark) 0.12f else 0.93f),
        surfaceContainer = tone(0.14f, if (dark) 0.15f else 0.89f),
        surfaceContainerHigh = tone(0.14f, if (dark) 0.19f else 0.85f),
        surfaceContainerHighest = tone(0.14f, if (dark) 0.23f else 0.81f),
        surfaceBright = tone(0.12f, if (dark) 0.25f else 0.98f),
        surfaceDim = tone(0.12f, if (dark) 0.08f else 0.84f),
        outline = tone(0.14f, if (dark) 0.58f else 0.46f),
        outlineVariant = tone(0.14f, if (dark) 0.30f else 0.72f),
        inverseSurface = tone(0.12f, if (dark) 0.90f else 0.18f),
        inverseOnSurface = tone(0.12f, if (dark) 0.16f else 0.95f),
        inversePrimary = tone(0.52f, if (dark) 0.35f else 0.76f),
    )
}
